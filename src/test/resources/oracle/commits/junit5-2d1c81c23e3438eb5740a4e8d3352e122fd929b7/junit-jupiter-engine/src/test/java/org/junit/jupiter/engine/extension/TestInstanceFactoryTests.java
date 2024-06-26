/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.assertRecordedExecutionEventsContainsExactly;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.container;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.engine;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.event;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.finishedSuccessfully;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.finishedWithFailure;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.started;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.test;
import static org.junit.platform.engine.test.event.TestExecutionResultConditions.isA;
import static org.junit.platform.engine.test.event.TestExecutionResultConditions.message;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;

/**
 * Integration tests that verify support for {@link TestInstanceFactory}.
 *
 * @since 5.3
 */
class TestInstanceFactoryTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
	}

	@Test
	void multipleFactoriesRegisteredOnSingleTestClass() {
		Class<?> testClass = MultipleFactoriesRegisteredTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(allOf(isA(ExtensionConfigurationException.class),
					message("The following TestInstanceFactory extensions were registered for test class ["
							+ testClass.getName() + "], but only one is permitted: "
							+ nullSafeToString(FooInstanceFactory.class, BarInstanceFactory.class))))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nullTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = NullTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + NullTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [null]."))))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nullTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleNullTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + NullTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [null]."))))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void bogusTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = BogusTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + BogusTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [java.lang.String]."))))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void bogusTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleBogusTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message(m -> m.equals("TestInstanceFactory [" + BogusTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [java.lang.String]."))))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void explosiveTestInstanceFactoryWithPerMethodLifecycle() {
		Class<?> testClass = ExplosiveTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(test("testShouldNotBeCalled"), started()), //
			event(test("testShouldNotBeCalled"),
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message("TestInstanceFactory [" + ExplosiveTestInstanceFactory.class.getName()
							+ "] failed to instantiate test class [" + testClass.getName() + "]: boom!")))), //
			event(container(testClass), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void explosiveTestInstanceFactoryWithPerClassLifecycle() {
		Class<?> testClass = PerClassLifecycleExplosiveTestInstanceFactoryTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), //
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message("TestInstanceFactory [" + ExplosiveTestInstanceFactory.class.getName()
							+ "] failed to instantiate test class [" + testClass.getName() + "]: boom!")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void proxyTestInstanceFactoryFailsDueToUseOfDifferentClassLoader() {
		Class<?> testClass = ProxiedTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests aborted");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass), //
				// NOTE: the test class name is the same even though the objects are instantiated using different ClassLoaders.
				finishedWithFailure(allOf(isA(TestInstantiationException.class),
					message("TestInstanceFactory [" + ProxyTestInstanceFactory.class.getName()
							+ "] failed to return an instance of [" + testClass.getName()
							+ "] and instead returned an instance of [" + testClass.getName() + "].")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void instanceFactoryOnToplevelTestClass() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(ParentTestCase.class);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: ParentTestCase",
				"parentTest"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryInTestClassHierarchy() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(ChildTestCase.class);

		assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: ChildTestCase",
				"parentTest",
			"FooInstanceFactory instantiated: ChildTestCase",
				"childTest"
		);
		// @formatter:on
	}

	@Test
	void multipleFactoriesRegisteredInTestClassHierarchy() {
		Class<?> testClass = MultipleFactoriesRegisteredChildTestCase.class;
		ExecutionEventRecorder eventRecorder = executeTestsForClass(testClass);

		assertEquals(0, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		assertRecordedExecutionEventsContainsExactly(eventRecorder.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(testClass), started()), //
			event(container(testClass),
				finishedWithFailure(allOf(isA(ExtensionConfigurationException.class),
					message("The following TestInstanceFactory extensions were registered for test class ["
							+ testClass.getName() + "], but only one is permitted: "
							+ nullSafeToString(FooInstanceFactory.class, BarInstanceFactory.class))))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void instanceFactoriesInNestedClassHierarchy() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(OuterTestCase.class);

		assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(

			// OuterTestCase
			"FooInstanceFactory instantiated: OuterTestCase",
				"beforeOuterMethod",
					"testOuter",

			// InnerTestCase
			"FooInstanceFactory instantiated: OuterTestCase",
				"NestedInstanceFactory instantiated: InnerTestCase",
					"beforeOuterMethod",
						"beforeInnerMethod",
							"testInner"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryRegisteredAsLambdaExpression() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(LambdaFactoryTestCase.class);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"beforeEach: lambda",
				"test: lambda"
		);
		// @formatter:on
	}

	@Test
	void instanceFactoryWithPerClassLifecycle() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(PerClassLifecycleTestCase.class);

		assertEquals(1, PerClassLifecycleTestCase.counter.get());

		assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");

		// @formatter:off
		assertThat(callSequence).containsExactly(
			"FooInstanceFactory instantiated: PerClassLifecycleTestCase",
				"@BeforeAll",
					"@BeforeEach",
						"test1",
					"@BeforeEach",
						"test2",
				"@AfterAll"
		);
		// @formatter:on
	}

	// -------------------------------------------------------------------------

	@ExtendWith({ FooInstanceFactory.class, BarInstanceFactory.class })
	static class MultipleFactoriesRegisteredTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@ExtendWith(NullTestInstanceFactory.class)
	static class NullTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleNullTestInstanceFactoryTestCase extends NullTestInstanceFactoryTestCase {
	}

	@ExtendWith(BogusTestInstanceFactory.class)
	static class BogusTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleBogusTestInstanceFactoryTestCase extends BogusTestInstanceFactoryTestCase {
	}

	@ExtendWith(ExplosiveTestInstanceFactory.class)
	static class ExplosiveTestInstanceFactoryTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@TestInstance(PER_CLASS)
	static class PerClassLifecycleExplosiveTestInstanceFactoryTestCase extends ExplosiveTestInstanceFactoryTestCase {
	}

	@ExtendWith(FooInstanceFactory.class)
	static class ParentTestCase {

		@Test
		void parentTest() {
			callSequence.add("parentTest");
		}
	}

	static class ChildTestCase extends ParentTestCase {

		@Test
		void childTest() {
			callSequence.add("childTest");
		}
	}

	@ExtendWith(BarInstanceFactory.class)
	static class MultipleFactoriesRegisteredChildTestCase extends ParentTestCase {

		@Test
		void testShouldNotBeCalled() {
			callSequence.add("testShouldNotBeCalled");
		}
	}

	@ExtendWith(FooInstanceFactory.class)
	static class OuterTestCase {

		@BeforeEach
		void beforeOuterMethod() {
			callSequence.add("beforeOuterMethod");
		}

		@Test
		void testOuter() {
			callSequence.add("testOuter");
		}

		@Nested
		@ExtendWith(NestedInstanceFactory.class)
		class InnerTestCase {

			@BeforeEach
			void beforeInnerMethod() {
				callSequence.add("beforeInnerMethod");
			}

			@Test
			void testInner() {
				callSequence.add("testInner");
			}
		}
	}

	static class LambdaFactoryTestCase {

		private final String text;

		@RegisterExtension
		static final TestInstanceFactory factory = (__, ___) -> new LambdaFactoryTestCase("lambda");

		LambdaFactoryTestCase(String text) {
			this.text = text;
		}

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEach: " + this.text);
		}

		@Test
		void test() {
			callSequence.add("test: " + this.text);
		}
	}

	@ExtendWith(FooInstanceFactory.class)
	@TestInstance(PER_CLASS)
	static class PerClassLifecycleTestCase {

		static final AtomicInteger counter = new AtomicInteger();

		PerClassLifecycleTestCase() {
			counter.incrementAndGet();
		}

		@BeforeAll
		void beforeAll() {
			callSequence.add("@BeforeAll");
		}

		@BeforeEach
		void beforeEach() {
			callSequence.add("@BeforeEach");
		}

		@Test
		void test1() {
			callSequence.add("test1");
		}

		@Test
		void test2() {
			callSequence.add("test2");
		}

		@AfterAll
		void afterAll() {
			callSequence.add("@AfterAll");
		}
	}

	@ExtendWith(ProxyTestInstanceFactory.class)
	@TestInstance(PER_CLASS)
	static class ProxiedTestCase {

		@Test
		void test1() {
			callSequence.add("test1");
		}

		@Test
		void test2() {
			callSequence.add("test2");
		}

	}

	// -------------------------------------------------------------------------

	private static abstract class AbstractTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			Class<?> testClass = factoryContext.getTestClass();
			instantiated(getClass(), testClass);
			return ReflectionUtils.newInstance(testClass);
		}
	}

	private static class FooInstanceFactory extends AbstractTestInstanceFactory {
	}

	private static class BarInstanceFactory extends AbstractTestInstanceFactory {
	}

	private static class NestedInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			Class<?> testClass = factoryContext.getTestClass();
			Object outerInstance = factoryContext.getOuterInstance().get();
			instantiated(getClass(), testClass);
			return ReflectionUtils.newInstance(testClass, outerInstance);
		}
	}

	/**
	 * {@link TestInstanceFactory} that returns null.
	 */
	private static class NullTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			return null;
		}
	}

	/**
	 * {@link TestInstanceFactory} that returns an object of a type that does
	 * not match the supplied test class.
	 */
	private static class BogusTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			return "bogus";
		}
	}

	/**
	 * {@link TestInstanceFactory} that always throws an exception.
	 */
	private static class ExplosiveTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			throw new RuntimeException("boom!");
		}
	}

	/**
	 * This does not actually create a proxy. Rather, it simply simulates what
	 * a proxy-based implementation might do, by loading the class from a
	 * different {@link ClassLoader}.
	 */
	private static class ProxyTestInstanceFactory implements TestInstanceFactory {

		@Override
		public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) {
			Class<?> testClass = factoryContext.getTestClass();
			String className = testClass.getName();
			instantiated(getClass(), testClass);

			try (ProxyClassLoader enigmaClassLoader = new ProxyClassLoader()) {
				// Load test class from different class loader
				Class<?> clazz = enigmaClassLoader.loadClass(className);
				return ReflectionUtils.newInstance(clazz);
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to load class [" + className + "]", ex);
			}
		}
	}

	private static class ProxyClassLoader extends URLClassLoader {

		ProxyClassLoader() {
			super(new URL[] { ProxyClassLoader.class.getProtectionDomain().getCodeSource().getLocation() },
				getSystemClassLoader());
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			return (name.startsWith(TestInstanceFactoryTests.class.getName()) ? findClass(name)
					: super.loadClass(name));
		}
	}

	private static boolean instantiated(Class<? extends TestInstanceFactory> factoryClass, Class<?> testClass) {
		return callSequence.add(factoryClass.getSimpleName() + " instantiated: " + testClass.getSimpleName());
	}

}
