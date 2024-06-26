package org.junit.tests.experimental.rules;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.experimental.results.PrintableResult.testResult;
import static org.junit.experimental.results.ResultMatchers.hasSingleFailureContaining;
import static org.junit.experimental.results.ResultMatchers.isSuccessful;
import static org.junit.matchers.JUnitMatchers.containsString;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestRuleTestWatchman;
import org.junit.rules.TestWatchman;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class TestRuleTest {
	private static boolean wasRun;

	public static class ExampleTest {
		@Rule
		public TestRule example= new TestRule() {
			public Statement apply(final Statement base, Description description) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						wasRun= true;
						base.evaluate();
					};
				};
			}
		};

		@Test
		public void nothing() {

		}
	}

	@Test
	public void ruleIsIntroducedAndEvaluated() {
		wasRun= false;
		JUnitCore.runClasses(ExampleTest.class);
		assertTrue(wasRun);
	}

	public static class SonOfExampleTest extends ExampleTest {
		
	}

	@Test
	public void ruleIsIntroducedAndEvaluatedOnSubclass() {
		wasRun= false;
		JUnitCore.runClasses(SonOfExampleTest.class);
		assertTrue(wasRun);
	}
	
	private static int runCount;

	public static class MultipleRuleTest {
		private static class Increment implements TestRule {
			public Statement apply(final Statement base, Description description) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						runCount++;
						base.evaluate();
					};
				};
			}
		}

		@Rule
		public TestRule incrementor1= new Increment();

		@Rule
		public TestRule incrementor2= new Increment();

		@Test
		public void nothing() {

		}
	}

	@Test
	public void multipleRulesAreRun() {
		runCount= 0;
		JUnitCore.runClasses(MultipleRuleTest.class);
		assertEquals(2, runCount);
	}

	public static class NoRulesTest {
		public int x;

		@Test
		public void nothing() {

		}
	}

	@Test
	public void ignoreNonRules() {
		Result result= JUnitCore.runClasses(NoRulesTest.class);
		assertEquals(0, result.getFailureCount());
	}

	private static String log;

	public static class OnFailureTest {
		@Rule
		// TODO: better name for TestRuleTestWatchman
		public TestRule watchman= new TestRuleTestWatchman() {
			@Override
			public void failed(Throwable e, Description description) {
				log+= description + " " + e.getClass().getSimpleName();
			}
		};

		@Test
		public void nothing() {
			fail();
		}
	}

	@Test
	public void onFailure() {
		log= "";
		Result result= JUnitCore.runClasses(OnFailureTest.class);
		assertEquals(String.format("nothing(%s) AssertionError", OnFailureTest.class.getName()), log);
		assertEquals(1, result.getFailureCount());
	}

	public static class WatchmanTest {
		private static String watchedLog;

		@Rule
		public TestRule watchman= new TestRuleTestWatchman() {
			@Override
			public void failed(Throwable e, Description description) {
				watchedLog+= description + " "
						+ e.getClass().getSimpleName() + "\n";
			}

			@Override
			public void succeeded(Description description) {
				watchedLog+= description + " " + "success!\n";
			}
		};

		@Test
		public void fails() {
			fail();
		}

		@Test
		public void succeeds() {
		}
	}

	@Test
	public void succeeded() {
		WatchmanTest.watchedLog= "";
		JUnitCore.runClasses(WatchmanTest.class);
		assertThat(WatchmanTest.watchedLog, containsString(String.format("fails(%s) AssertionError", WatchmanTest.class.getName())));
		assertThat(WatchmanTest.watchedLog, containsString(String.format("succeeds(%s) success!", WatchmanTest.class.getName())));
	}

	public static class BeforesAndAfters {
		private static String watchedLog;

		@Before public void before() {
			watchedLog+= "before ";
		}
		
		@Rule
		public MethodRule watchman= new TestWatchman() {
			@Override
			public void starting(FrameworkMethod method) {
				watchedLog+= "starting ";
			}
			
			@Override
			public void finished(FrameworkMethod method) {
				watchedLog+= "finished ";
			}
			
			@Override
			public void succeeded(FrameworkMethod method) {
				watchedLog+= "succeeded ";
			}
		};
		
		@After public void after() {
			watchedLog+= "after ";
		}

		@Test
		public void succeeds() {
			watchedLog+= "test ";
		}
	}

	@Test
	public void beforesAndAfters() {
		BeforesAndAfters.watchedLog= "";
		JUnitCore.runClasses(BeforesAndAfters.class);
		assertThat(BeforesAndAfters.watchedLog, is("starting before test after succeeded finished "));
	}
	
	public static class WrongTypedField {
		@Rule public int x = 5;
		@Test public void foo() {}
	}
	
	@Test public void validateWrongTypedField() {
		assertThat(testResult(WrongTypedField.class), 
				hasSingleFailureContaining("must implement MethodRule"));
	}
	
	public static class SonOfWrongTypedField extends WrongTypedField {
		
	}

	@Test public void validateWrongTypedFieldInSuperclass() {
		assertThat(testResult(SonOfWrongTypedField.class), 
				hasSingleFailureContaining("must implement MethodRule"));
	}

	public static class PrivateRule {
		@SuppressWarnings("unused")
		@Rule private MethodRule rule = new TestName();
		@Test public void foo() {}
	}
	
	@Test public void validatePrivateRule() {
		assertThat(testResult(PrivateRule.class), 
				hasSingleFailureContaining("must be public"));
	}
	
	public static class CustomTestName implements TestRule {
		public String name = null;
			
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {				
				@Override
				public void evaluate() throws Throwable {
					name = description.getMethodName();
					base.evaluate();
				}
			};
		}		
	}
	
	public static class UsesCustomMethodRule {
		@Rule public CustomTestName counter = new CustomTestName();
		@Test public void foo() {
			assertEquals("foo", counter.name);
		}
	}
	
	@Test public void useCustomMethodRule() {
		assertThat(testResult(UsesCustomMethodRule.class), isSuccessful());
	}
}
