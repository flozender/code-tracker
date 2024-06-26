/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Indicates that a class declares one or more @{@link Bean} methods and may be processed
 * by the Spring container to generate bean definitions and service requests for those
 * beans at runtime, for example:
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate, configure and return bean ...
 *     }
 * }</pre>
 *
 * <h2>Bootstrapping {@code @Configuration} classes</h2>
 * <h3>Via {@code AnnotationConfigApplicationContext}</h3>
 * {@code @Configuration} classes are typically bootstrapped using either
 * {@link AnnotationConfigApplicationContext} or its web-capable variant,
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext}.
 * A simple example with the former follows:
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx =
 *     new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // use myBean ...</pre>
 *
 * See {@link AnnotationConfigApplicationContext} Javadoc for further details and see
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} for {@code web.xml} configuration instructions.
 *
 * <h3>Via Spring {@code <beans>} XML</h3>
 * <p>As an alternative to registering {@code @Configuration} classes directly against an
 * {@code AnnotationConfigApplicationContext}, {@code @Configuration} classes may be
 * declared as normal {@code <bean>} definitions within Spring XML files:
 * <pre class="code">
 * {@code
 * <beans>
 *    <context:annotation-config/>
 *    <bean class="com.acme.AppConfig"/>
 * </beans>
 * }</pre>
 *
 * In the example above, {@code <context:annotation-config/>} is required in order to
 * enable {@link ConfigurationClassPostProcessor} and other annotation-related
 * post processors that facilitate handling {@code @Configuration} classes.
 *
 * <h3>Via component scanning</h3>
 * <p>{@code @Configuration} is meta-annotated with @{@link Component}, therefore
 * {@code @Configuration} classes are candidates for component scanning (typically using
 * Spring XML's {@code <context:component-scan/>} element) and therefore may also take
 * advantage of @{@link Autowired}/@{@link Inject} at the field and method level (but not
 * at the constructor level).
 * <p>{@code @Configuration} classes may not only be bootstrapped using
 * component scanning, but may also themselves <em>configure</em> component scanning using
 * the @{@link ComponentScan} annotation:
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // various &#064;Bean definitions ...
 * }</pre>
 *
 * See @{@link ComponentScan} Javadoc for details.
 * 
 *
 * <h2>Working with externalized values</h2>
 * <h3>Using the {@code Environment} API</h3>
 * Externalized values may be looked up by injecting the Spring
 * {@link org.springframework.core.env.Environment Environment} into a
 * {@code @Configuration} class:
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *     &#064Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * Properties resolved through the {@code Environment} reside in one or more "property
 * source" objects, and {@code @Configuration} classes may contribute property sources to
 * the {@code Environment} object using the @{@link PropertySources} annotation:
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *     &#064Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * See {@link org.springframework.core.env.Environment Environment}
 * and @{@link PropertySource} Javadoc for further details.
 * 
 * <h3>Using the {@code @Value} annotation</h3>
 * Externalized values may be 'wired into' {@code @Configuration} classes using
 * the @{@link Value} annotation:
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *     &#064Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * This approach is most useful when using Spring's
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, usually enabled via XML with
 * {@code <context:property-placeholder/>}.  See the section below on composing
 * {@code @Configuration} classes with Spring XML using {@code @ImportResource},
 * see @{@link Value} Javadoc, and see @{@link Bean} Javadoc for details on working with
 * {@code BeanFactoryPostProcessor} types such as
 * {@code PropertySourcesPlaceholderConfigurer}.
 *
 * <h2>Composing {@code @Configuration} classes</h2>
 * <h3>With the {@code @Import} annotation</h3>
 * <p>{@code @Configuration} classes may be composed using the @{@link Import} annotation,
 * not unlike the way that {@code <import>} works in Spring XML. Because
 * {@code @Configuration} objects are managed as Spring beans within the container,
 * imported configurations may be injected using {@code @Autowired} or {@code @Inject}:
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *     &#064Inject DatabaseConfig dataConfig;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // reference the dataSource() bean method
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * Now both {@code AppConfig} and the imported {@code DatabaseConfig} can be bootstrapped
 * by registering only {@code AppConfig} against the Spring context:
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>With the {@code @Profile} annotation</h3>
 * {@code @Configuration} classes may be marked with the @{@link Profile} annotation to 
 * indicate they should be processed only if a given profile or profiles are
 * <em>active</em>:
 * <pre class="code">
 * &#064;Profile("embedded")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return embedded DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return production DataSource
 *     }
 * }</pre>
 *
 * See @{@link Profile} and {@link org.springframework.core.env.Environment} Javadoc for
 * further details.
 *
 * <h3>With Spring XML using the {@code @ImportResource} annotation</h3>
 * As mentioned above, {@code @Configuration} classes may be declared as regular Spring
 * {@code <bean>} definitions within Spring XML files. It is also possible to
 * import Spring XML configuration files into {@code @Configuration} classes using
 * the @{@link ImportResource} annotation. Bean definitions imported from XML can be
 * injected using {@code @Autowired} or {@code @Import}:
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *     &#064Inject DataSource dataSource; // from XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // inject the XML-defined dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h2>Configuring lazy initialization</h2>
 * <p>By default, {@code @Bean} methods will be <em>eagerly instantiated</em> at container
 * bootstrap time.  To avoid this, {@code @Configuration} may be used in conjunction with
 * the @{@link Lazy} annotation to indicate that all {@code @Bean} methods declared within
 * the class are by default lazily initialized. Note that {@code @Lazy} may be used on
 * individual {@code @Bean} methods as well.
 *
 * <h2>Testing support for {@code @Configuration} classes</h2>
 * The Spring <em>TestContext framework</em> available in the {@code spring-test} module
 * provides the
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}
 * annotation, which as of Spring 3.1 can accept an array of {@code @Configuration}
 * {@code Class} objects:
 * <pre class="code">
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * &#064;ContextConfiguration(classes={AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // assertions against myBean ...
 *     }
 * }</pre>
 *
 * See TestContext framework reference documentation for details.
 * 
 * <h2>Enabling built-in Spring features using {@code @Enable} annotations</h2>
 * Spring features such as asynchronous method execution, scheduled task execution,
 * annotation driven transaction management, and even Spring MVC can be enabled and
 * configured from {@code @Configuration}
 * classes using their respective "{@code @Enable}" annotations. See
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync},
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling},
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement}, and
 * {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * for details.
 *
 * <h2>Constraints when authoring {@code @Configuration} classes</h2>
 * <ul>
 *    <li>&#064;Configuration classes must be non-final
 *    <li>&#064;Configuration classes must be non-local (may not be declared within a method)
 *    <li>&#064;Configuration classes must have a default/no-arg constructor and may not
 *        use @{@link Autowired} constructor parameters. Any nested configuration classes
 *        must be {@code static}
 * </ul>
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 3.0
 * @see Bean
 * @see Profile
 * @see Import
 * @see ImportResource
 * @see ComponentScan
 * @see Lazy
 * @see PropertySource
 * @see AnnotationConfigApplicationContext
 * @see ConfigurationClassPostProcessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.test.context.ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * Explicitly specify the name of the Spring bean definition associated
	 * with this Configuration class.  If left unspecified (the common case),
	 * a bean name will be automatically generated.
	 *
	 * <p>The custom name applies only if the Configuration class is picked up via
	 * component scanning or supplied directly to a {@link AnnotationConfigApplicationContext}.
	 * If the Configuration class is registered as a traditional XML bean definition,
	 * the name/id of the bean element will take precedence.
	 *
	 * @return the specified bean name, if any
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
	 */
	String value() default "";

}
