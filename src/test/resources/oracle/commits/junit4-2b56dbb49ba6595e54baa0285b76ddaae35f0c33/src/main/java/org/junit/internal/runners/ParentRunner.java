package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assume.AssumptionViolatedException;
import org.junit.internal.runners.links.RunAfters;
import org.junit.internal.runners.links.RunBefores;
import org.junit.internal.runners.links.Statement;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.InitializationError;
import org.junit.internal.runners.model.TestClass;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

public abstract class ParentRunner<T> extends Runner implements Filterable, Sortable {
	protected TestClass fTestClass;
	private List<T> fChildren = null;
	private Filter fFilter = null;
	private Sorter fSorter = null;

	public ParentRunner(Class<?> testClass) {
		fTestClass = new TestClass(testClass);
	}

	protected abstract List<T> getChildren();
	
	protected abstract Description describeChild(T child);

	// TODO: (Nov 24, 2007 11:50:17 PM) can I avoid RunNotifier?

	protected abstract void runChild(T child, RunNotifier notifier);

	private Statement classBlock(final RunNotifier notifier) {
		return new Statement() {
			@Override
			public void evaluate() {
				for (T each : getFilteredChildren())
					runChild(each, notifier);
			}
		};
	}
	
	@Override
	public void run(final RunNotifier notifier) {
		EachTestNotifier testNotifier= new EachTestNotifier(notifier,
				getDescription());
		try {
			Statement statement= new RunBefores(classBlock(notifier), fTestClass, null);
			statement= new RunAfters(statement, fTestClass, null);
			statement.evaluate();
		} catch (AssumptionViolatedException e) {
			testNotifier.addIgnorance(e);
		} catch (StoppedByUserException e) {
			throw e;
		} catch (Throwable e) {
			testNotifier.addFailure(e);
		}
	}

	@Override
	public Description getDescription() {
		Description description= Description.createSuiteDescription(getName(), classAnnotations());
		for (T child : getFilteredChildren())
			description.addChild(describeChild(child));
		return description;
	}

	private List<T> getFilteredChildren() {
		if (fChildren == null)
			fChildren = computeFilteredChildren();
		return fChildren;
	}

	private List<T> computeFilteredChildren() {
		ArrayList<T> filtered= new ArrayList<T>();
		for (T each : getChildren())
			if (shouldRun(each)) {
				try {
					filtered.add(sortChild(filterChild(each, fFilter), fSorter));
				} catch (NoTestsRemainException e) {
					// don't add it
				}
			}
		if (fSorter != null)
			Collections.sort(filtered, comparator());
		return filtered;
	}

	protected T sortChild(T child, Sorter sorter) {
		return child;
	}

	/**
	 * @throws NoTestsRemainException 
	 */
	protected T filterChild(T child, Filter filter) throws NoTestsRemainException {
		return child;
	}

	private Comparator<? super T> comparator() {
		return new Comparator<T>() {
			public int compare(T o1, T o2) {
				return fSorter.compare(describeChild(o1), describeChild(o2));
			}
		};
	}

	private boolean shouldRun(T each) {
		return fFilter == null || fFilter.shouldRun(describeChild(each));
	}

	protected TestClass getTestClass() {
		return fTestClass;
	}

	protected Annotation[] classAnnotations() {
		return fTestClass.getJavaClass().getAnnotations();
	}

	protected String getName() {
		return fTestClass.getName();
	}

	// TODO: (Nov 14, 2007 11:04:54 AM) sort methods

	protected void assertValid(List<Throwable> errors) throws InitializationError {
		if (!errors.isEmpty())
			throw new InitializationError(errors);
	}
	
	public void filter(Filter filter) throws NoTestsRemainException {
		fFilter= filter;
	}
	
	public void sort(Sorter sorter) {
		fSorter= sorter;
	}
}