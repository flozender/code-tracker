package org.junit.tests.running.classes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.runners.model.TestClass;

public class TestClassTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	public static class TwoConstructors {
		public TwoConstructors() {
		}

		public TwoConstructors(int x) {
		}
	}

	@Test(expected= IllegalArgumentException.class)
	public void complainIfMultipleConstructors() {
		new TestClass(TwoConstructors.class);
	}

	public static class ManyMethods {
		@Test
		public void a() {
		}

		@Before
		public void b() {
		}

		@Ignore
		@Test
		public void c() {
		}

		@Ignore
		@After
		public void d() {
		}

		public void e() {
		}

		@BeforeClass
		public void f() {
		}

		public void g() {
		}

		@AfterClass
		public void h() {
		}

		@Test
		public void i() {
		}

		@Test
		public void j() {
		}
	}

	public static class SuperclassWithField {
		@Rule
		public TestRule x;
	}

	public static class SubclassWithField extends SuperclassWithField {
		@Rule
		public TestRule x;
	}

	@Test
	public void fieldsOnSubclassesShadowSuperclasses() {
		assertThat(new TestClass(SubclassWithField.class).getAnnotatedFields(
				Rule.class).size(), is(1));
	}
}
