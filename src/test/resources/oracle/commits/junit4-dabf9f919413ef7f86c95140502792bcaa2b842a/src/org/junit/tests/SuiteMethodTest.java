package org.junit.tests;

import static org.junit.Assert.*;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class SuiteMethodTest {
	public static boolean wasRun;

	static public class OldTest extends TestCase {
		public OldTest(String name) {
			super(name);
		}
		
		public static junit.framework.Test suite() {
			TestSuite result= new TestSuite();
			result.addTest(new OldTest("notObviouslyATest"));
			return result;
		}
		
		public void notObviouslyATest() {
			wasRun= true;
		}
	}
	
	@Test public void makeSureSuiteIsCalled() {
		wasRun= false;
		JUnitCore.runClasses(OldTest.class);
		assertTrue(wasRun);
	}
	
	static public class NewTest {
		@Test public void sample() {
			wasRun= true;
		}

		public static junit.framework.Test suite() {
			return new JUnit4TestAdapter(NewTest.class);
		}
	}
	
	@Test public void makeSureSuiteWorksWithJUnit4Classes() {
		wasRun= false;
		JUnitCore.runClasses(NewTest.class);
		assertTrue(wasRun);
	}
	

	public static class CompatibilityTest {
		@Ignore	@Test
		public void ignored() {
		}
		
		public static junit.framework.Test suite() {
			return new JUnit4TestAdapter(CompatibilityTest.class);
		}
	}
	
	@Test public void descriptionAndRunNotificationsAreConsistent() {
		Result result= JUnitCore.runClasses(CompatibilityTest.class);
		assertEquals(0, result.getIgnoreCount());
		
		Description description= Request.aClass(CompatibilityTest.class).getRunner().getDescription();
		assertEquals(0, description.getChildren().size());
	}
}
