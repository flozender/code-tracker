/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;

import static org.springframework.core.MethodIntrospector.*;

/**
 * Package-private class to assist {@link RequestMappingHandlerAdapter} with
 * resolving, initializing, and caching annotated methods declared in
 * {@code @Controller} and {@code @ControllerAdvice} components. Assists with
 * the following annotations:
 * <ul>
 * <li>{@code @InitBinder}
 * <li>{@code @ModelAttribute}
 * <li>{@code @RequestMapping}
 * <li>{@code @ExceptionHandler}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ControllerMethodResolver {

	private static Log logger = LogFactory.getLog(ControllerMethodResolver.class);


	private final List<SyncHandlerMethodArgumentResolver> initBinderResolvers;

	private final List<HandlerMethodArgumentResolver> modelAttributeResolvers;

	private final List<HandlerMethodArgumentResolver> requestMappingResolvers;

	private final List<HandlerMethodArgumentResolver> exceptionHandlerResolvers;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	private final Map<Class<?>, Set<Method>> initBinderMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Set<Method>> modelAttributeMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);


	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);

	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);


	ControllerMethodResolver(ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext context, List<HttpMessageReader<?>> readers) {

		Assert.notNull(customResolvers, "ArgumentResolverConfigurer is required");
		Assert.notNull(readers, "'messageReaders' is required");
		Assert.notNull(reactiveRegistry, "ReactiveAdapterRegistry is required");
		Assert.notNull(context, "ApplicationContext is required");

		this.initBinderResolvers = initBinderResolvers(customResolvers, reactiveRegistry, context);
		this.modelAttributeResolvers = modelMethodResolvers(customResolvers, reactiveRegistry, context);
		this.requestMappingResolvers = requestMappingResolvers(customResolvers, reactiveRegistry, context, readers);
		this.exceptionHandlerResolvers = exceptionHandlerResolvers(customResolvers, reactiveRegistry, context);
		this.reactiveAdapterRegistry = reactiveRegistry;

		initControllerAdviceCaches(context);
	}

	private List<SyncHandlerMethodArgumentResolver> initBinderResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, reactiveRegistry, context, false, Collections.emptyList()).stream()
				.filter(resolver -> resolver instanceof SyncHandlerMethodArgumentResolver)
				.map(resolver -> (SyncHandlerMethodArgumentResolver) resolver)
				.collect(Collectors.toList());
	}

	private static List<HandlerMethodArgumentResolver> modelMethodResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, reactiveRegistry, context, true, Collections.emptyList());
	}

	private static List<HandlerMethodArgumentResolver> requestMappingResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext context, List<HttpMessageReader<?>> readers) {

		return initResolvers(customResolvers, reactiveRegistry, context, true, readers);
	}

	private static List<HandlerMethodArgumentResolver> exceptionHandlerResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, reactiveRegistry, context, false, Collections.emptyList());
	}

	private static List<HandlerMethodArgumentResolver> initResolvers(ArgumentResolverConfigurer customResolvers,
			ReactiveAdapterRegistry reactiveRegistry, ConfigurableApplicationContext context,
			boolean supportDataBinding, List<HttpMessageReader<?>> readers) {

		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean requestMappingMethod = !readers.isEmpty() && supportDataBinding;

		// Annotation-based...
		List<HandlerMethodArgumentResolver> result = new ArrayList<>();
		result.add(new RequestParamMethodArgumentResolver(beanFactory, reactiveRegistry, false));
		result.add(new RequestParamMapMethodArgumentResolver(reactiveRegistry));
		result.add(new PathVariableMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new PathVariableMapMethodArgumentResolver(reactiveRegistry));
		result.add(new MatrixVariableMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new MatrixVariableMapMethodArgumentResolver(reactiveRegistry));
		if (!readers.isEmpty()) {
			result.add(new RequestBodyArgumentResolver(readers, reactiveRegistry));
			result.add(new RequestPartMethodArgumentResolver(readers, reactiveRegistry));
		}
		if (supportDataBinding) {
			result.add(new ModelAttributeMethodArgumentResolver(reactiveRegistry, false));
		}
		result.add(new RequestHeaderMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new RequestHeaderMapMethodArgumentResolver(reactiveRegistry));
		result.add(new CookieValueMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new ExpressionValueMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new SessionAttributeMethodArgumentResolver(beanFactory, reactiveRegistry));
		result.add(new RequestAttributeMethodArgumentResolver(beanFactory, reactiveRegistry));

		// Type-based...
		if (!readers.isEmpty()) {
			result.add(new HttpEntityArgumentResolver(readers, reactiveRegistry));
		}
		result.add(new ModelArgumentResolver(reactiveRegistry));
		if (supportDataBinding) {
			result.add(new ErrorsMethodArgumentResolver(reactiveRegistry));
		}
		result.add(new ServerWebExchangeArgumentResolver(reactiveRegistry));
		result.add(new PrincipalArgumentResolver(reactiveRegistry));
		if (requestMappingMethod) {
			result.add(new SessionStatusMethodArgumentResolver());
		}
		result.add(new WebSessionArgumentResolver(reactiveRegistry));

		// Custom...
		result.addAll(customResolvers.getCustomResolvers());

		// Catch-all...
		result.add(new RequestParamMethodArgumentResolver(beanFactory, reactiveRegistry, true));
		if (supportDataBinding) {
			result.add(new ModelAttributeMethodArgumentResolver(reactiveRegistry, true));
		}

		return result;
	}

	private void initControllerAdviceCaches(ApplicationContext applicationContext) {

		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
		AnnotationAwareOrderComparator.sort(beans);

		for (ControllerAdviceBean bean : beans) {
			Class<?> beanType = bean.getBeanType();
			if (beanType != null) {
				Set<Method> attrMethods = selectMethods(beanType, ATTRIBUTE_METHODS);
				if (!attrMethods.isEmpty()) {
					this.modelAttributeAdviceCache.put(bean, attrMethods);
				}
				Set<Method> binderMethods = selectMethods(beanType, BINDER_METHODS);
				if (!binderMethods.isEmpty()) {
					this.initBinderAdviceCache.put(bean, binderMethods);
				}
				ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
				if (resolver.hasExceptionMappings()) {
					this.exceptionHandlerAdviceCache.put(bean, resolver);
				}
			}
		}

		if (logger.isDebugEnabled()) {
			int modelSize = this.modelAttributeAdviceCache.size();
			int binderSize = this.initBinderAdviceCache.size();
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			if (modelSize == 0 && binderSize == 0 && handlerSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			}
			else {
				logger.debug("ControllerAdvice beans: " + modelSize + " @ModelAttribute, " + binderSize +
						" @InitBinder, " + handlerSize + " @ExceptionHandler");
			}
		}
	}


	/**
	 * Return an {@link InvocableHandlerMethod} for the given
	 * {@code @RequestMapping} method initialized with argument resolvers.
	 */
	public InvocableHandlerMethod getRequestMappingMethod(HandlerMethod handlerMethod) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setArgumentResolvers(this.requestMappingResolvers);
		invocable.setReactiveAdapterRegistry(this.reactiveAdapterRegistry);
		return invocable;
	}

	/**
	 * Find {@code @InitBinder} methods in {@code @ControllerAdvice} components
	 * or in the controller of the given {@code @RequestMapping} method.
	 */
	public List<SyncInvocableHandlerMethod> getInitBinderMethods(HandlerMethod handlerMethod) {
		List<SyncInvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.initBinderAdviceCache.forEach((adviceBean, methods) -> {
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(getInitBinderMethod(bean, method)));
			}
		});

		this.initBinderMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, BINDER_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(getInitBinderMethod(bean, method));
				});

		return result;
	}

	private SyncInvocableHandlerMethod getInitBinderMethod(Object bean, Method method) {
		SyncInvocableHandlerMethod invocable = new SyncInvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.initBinderResolvers);
		return invocable;
	}

	/**
	 * Find {@code @ModelAttribute} methods in {@code @ControllerAdvice}
	 * components or in the controller of the given {@code @RequestMapping} method.
	 */
	public List<InvocableHandlerMethod> getModelAttributeMethods(HandlerMethod handlerMethod) {
		List<InvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.modelAttributeAdviceCache.forEach((adviceBean, methods) -> {
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(createAttributeMethod(bean, method)));
			}
		});

		this.modelAttributeMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, ATTRIBUTE_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(createAttributeMethod(bean, method));
				});

		return result;
	}

	private InvocableHandlerMethod createAttributeMethod(Object bean, Method method) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.modelAttributeResolvers);
		return invocable;
	}

	/**
	 * Find an {@code @ExceptionHandler} method in {@code @ControllerAdvice}
	 * components or in the controller of the given {@code @RequestMapping} method.
	 */
	@Nullable
	public InvocableHandlerMethod getExceptionHandlerMethod(Throwable ex, HandlerMethod handlerMethod) {

		Class<?> handlerType = handlerMethod.getBeanType();

		// Controller-local first...
		Object targetBean = handlerMethod.getBean();
		Method targetMethod = this.exceptionHandlerCache
				.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new)
				.resolveMethodByThrowable(ex);

		if (targetMethod == null) {
			// Global exception handlers...
			for (ControllerAdviceBean advice : this.exceptionHandlerAdviceCache.keySet()) {
				if (advice.isApplicableToBeanType(handlerType)) {
					targetBean = advice.resolveBean();
					targetMethod = this.exceptionHandlerAdviceCache.get(advice).resolveMethodByThrowable(ex);
					if (targetMethod != null) {
						break;
					}
				}
			}
		}

		if (targetMethod == null) {
			return null;
		}

		InvocableHandlerMethod invocable = new InvocableHandlerMethod(targetBean, targetMethod);
		invocable.setArgumentResolvers(this.exceptionHandlerResolvers);
		return invocable;
	}

	/**
	 * Return the handler for the type-level {@code @SessionAttributes} annotation
	 * based on the given controller method.
	 */
	public SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
		Class<?> handlerType = handlerMethod.getBeanType();
		SessionAttributesHandler result = this.sessionAttributesHandlerCache.get(handlerType);
		if (result == null) {
			synchronized (this.sessionAttributesHandlerCache) {
				result = this.sessionAttributesHandlerCache.get(handlerType);
				if (result == null) {
					result = new SessionAttributesHandler(handlerType);
					this.sessionAttributesHandlerCache.put(handlerType, result);
				}
			}
		}
		return result;
	}


	/** Filter for {@link InitBinder @InitBinder} methods. */
	private static final ReflectionUtils.MethodFilter BINDER_METHODS = method ->
			AnnotationUtils.findAnnotation(method, InitBinder.class) != null;

	/** Filter for {@link ModelAttribute @ModelAttribute} methods. */
	private static final ReflectionUtils.MethodFilter ATTRIBUTE_METHODS = method ->
			(AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) &&
					(AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null);

}
