package org.junit.tests.experimental.theories.runner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.junit.runners.Enclosed;

@RunWith(Enclosed.class)
public class SuccessfulWithDataPointFields {
	@RunWith(Theories.class)
	public static class HasATwoParameterTheory {
		public static int ONE= 1;

		@Theory
		public void everythingIsZero(int x, int y) {
			assertThat(x, is(y));
		}
	}

	@RunWith(Theories.class)
	public static class BeforeAndAfterOnSameInstance {
		@DataPoint
		public static String A= "A";

		private int befores= 0;

		@Before
		public void incrementBefore() {
			befores++;
		}

		@Theory
		public void stringsAreOK(String string) {
			assertTrue(befores == 1);
		}
	}

	@RunWith(Theories.class)
	public static class NewObjectEachTime {
		@DataPoint
		public static String A= "A";

		@DataPoint
		public static String B= "B";

		private List<String> list= new ArrayList<String>();

		@Theory
		public void addToEmptyList(String string) {
			list.add(string);
			assertThat(list.size(), is(1));
		}
	}

	@RunWith(Theories.class)
	public static class PositiveInts {
		@DataPoint
		public static final int ONE= 1;

		private int x;

		public PositiveInts(int x) {
			assumeTrue(x > 0);
			this.x= x;
		}

		@Theory
		public void haveAPostiveSquare() {
			assertTrue(x * x > 0);
		}
	}

	@RunWith(Theories.class)
	public static class PositiveIntsWithNegativeField {
		@DataPoint
		public static final int ONE= 1;
		@DataPoint
		public static final int NEGONE= -1;

		private int x;

		public PositiveIntsWithNegativeField(int x) {
			assumeTrue(x > 0);
			this.x= x;
		}

		@Theory
		public void haveAPostiveSquare() {
			assertTrue(x > 0);
		}
	}
	
	@RunWith(Theories.class)
	public static class PositiveIntsWithMethodParams {
		@DataPoint
		public static final int ONE= 1;

		private int x;

		public PositiveIntsWithMethodParams(int x) {
			assumeTrue(x > 0);
			this.x= x;
		}

		@Theory
		public void haveAPostiveSquare(int y) {
			assumeTrue(y > 0);
			assertTrue(x * y > 0);
		}
	}
	
	@RunWith(Theories.class)
	public static class DifferentTypesInConstructor {
		@DataPoint
		public static final int ONE= 1;

		@DataPoint public static final String A = "A";
		
		public DifferentTypesInConstructor(int x) {
		}

		@Theory
		public void yesIndeed(String a) {
		}
	}
	


	@RunWith(Theories.class)
	public static class BeforeAndAfterEachTime {
		public static int befores= 0;

		@DataPoint
		public static String A= "A";

		@DataPoint
		public static String B= "B";

		@Before
		public void incrementBefore() {
			befores++;
		}

		@BeforeClass public static void resetCalls() {
			befores = 0;
		}
		
		@Theory
		public void stringsAreOK(String string) {
		}
		
		@AfterClass public static void calledTwice() {
			assertEquals(2, befores);
		}
	}
}