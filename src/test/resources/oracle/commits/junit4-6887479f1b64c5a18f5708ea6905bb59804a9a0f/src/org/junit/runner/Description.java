package org.junit.runner;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * <p>A <code>Description</code> describes a test which is to be run or has been run. <code>Descriptions</code> 
 * can be atomic (a single test) or compound (containing children tests). <code>Descriptions</code> are used
 * to provide feedback about the tests that are about to run (for example, the tree view
 * visible in many IDEs) or tests that have been run (for example, the failures view).</p>
 * 
 * <p><code>Descriptions</code> are implemented as a single class rather than a Composite because
 * they are entirely informational. They contain no logic aside from counting their tests.</p>
 * 
 * <p>In the past, we used the raw {@link junit.framework.TestCase}s and {@link junit.framework.TestSuite}s
 * to display the tree of tests. This was no longer viable in JUnit 4 because atomic tests no longer have 
 * a superclass below {@link Object}. We needed a way to pass a class and name together. Description 
 * emerged from this.</p>
 * 
 * @see org.junit.runner.Request
 * @see org.junit.runner.Runner
 */
public class Description {
	
	/**
	 * Create a <code>Description</code> named <code>name</code>.
	 * Generally, you will add children to this <code>Description</code>.
	 * @param name the name of the <code>Description</code> 
	 * @param annotations 
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createSuiteDescription(String name, Annotation... annotations) {
		return new Description(name, null, annotations);
	}

	/**
	 * Create a <code>Description</code> of a single test named <code>name</code> in the class <code>clazz</code>.
	 * Generally, this will be a leaf <code>Description</code>.
	 * @param clazz the class of the test
	 * @param name the name of the test (a method name for test annotated with {@link org.junit.Test})
	 * @param annotations meta-data about the test, for downstream interpreters
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createTestDescription(Class<?> clazz, String name, Annotation... annotations) {
		return new Description(String.format("%s(%s)", name, clazz.getName()), clazz.getAnnotations(), annotations);
	}

	/**
	 * Create a <code>Description</code> of a single test named <code>name</code> in the class <code>clazz</code>.
	 * Generally, this will be a leaf <code>Description</code>.  
	 * (This remains for binary compatibility with clients of JUnit 4.3)
	 * @param clazz the class of the test
	 * @param name the name of the test (a method name for test annotated with {@link org.junit.Test})
	 * @return a <code>Description</code> named <code>name</code>
	 */
	public static Description createTestDescription(Class<?> clazz, String name) {
		return createTestDescription(clazz, name, new Annotation[0]);
	}

	/**
	 * Create a <code>Description</code> named after <code>testClass</code>
	 * @param testClass A {@link Class} containing tests 
	 * @return a <code>Description</code> of <code>testClass</code>
	 */
	public static Description createSuiteDescription(Class<?> testClass) {
		return new Description(testClass.getName(), null, testClass.getAnnotations());
	}
	
	public static final Description EMPTY= new Description("No Tests", null);
	public static final Description TEST_MECHANISM= new Description("Test mechanism", null);
	
	private final ArrayList<Description> fChildren= new ArrayList<Description>();
	private final String fDisplayName;
	
	private final Annotation[] fAnnotations;
	private final Annotation[] fParentAnnotations;
	
	private Description(final String displayName, Annotation[] parentAnnotations, Annotation... annotations) {
		fDisplayName= displayName;
		fParentAnnotations= parentAnnotations != null ? parentAnnotations : new Annotation[0];
		fAnnotations= annotations;
	}

	/**
	 * @return a user-understandable label
	 */
	public String getDisplayName() {
		return fDisplayName;
	}

	/**
	 * Add <code>Description</code> as a child of the receiver.
	 * @param description the soon-to-be child.
	 */
	public void addChild(Description description) {
		getChildren().add(description);
	}

	/**
	 * @return the receiver's children, if any
	 */
	public ArrayList<Description> getChildren() {
		return fChildren;
	}

	/**
	 * @return <code>true</code> if the receiver is a suite
	 */
	public boolean isSuite() {
		return !isTest();
	}

	/**
	 * @return <code>true</code> if the receiver is an atomic test
	 */
	public boolean isTest() {
		return getChildren().isEmpty();
	}

	/**
	 * @return the total number of atomic tests in the receiver
	 */
	public int testCount() {
		if (isTest())
			return 1;
		int result= 0;
		for (Description child : getChildren())
			result+= child.testCount();
		return result;
	}

	@Override
	public int hashCode() {
		return getDisplayName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Description))
			return false;
		Description d = (Description) obj;
		return getDisplayName().equals(d.getDisplayName())
				&& getChildren().equals(d.getChildren());
	}
	
	@Override
	public String toString() {
		return getDisplayName();
	}

	public boolean isEmpty() {
		return equals(EMPTY);
	}

	public Description childlessCopy() {
		return new Description(fDisplayName, fAnnotations);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		for (Annotation each : fAnnotations)
			if (each.annotationType().equals(annotationType))
				return annotationType.cast(each);
		return null;
	}

	// TODO: (Aug 6, 2007 5:10:13 PM) DUP
	public <T extends Annotation> T getParentAnnotation(Class<T> annotationType) {
		for (Annotation each : fParentAnnotations)
			if (each.annotationType().equals(annotationType))
				return annotationType.cast(each);
		return null;
	}

	public Collection<Annotation> getAnnotations() {
		return Arrays.asList(fAnnotations);
	}
}