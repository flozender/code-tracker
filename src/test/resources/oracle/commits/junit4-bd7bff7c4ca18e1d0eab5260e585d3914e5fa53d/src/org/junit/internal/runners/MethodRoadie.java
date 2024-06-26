package org.junit.internal.runners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class MethodRoadie {
	private final Object fTest;
	private final Method fMethod;
	private final RunNotifier fNotifier;
	private final Description fDescription;
	private TestMethod fTestMethod;

	public MethodRoadie(Object test, Method method, RunNotifier notifier, Description description, TestClass testClass) {
		fTest= test;
		fMethod= method;
		fNotifier= notifier;
		fDescription= description;
		fTestMethod= new TestMethod(method, testClass);
	}

	public void run() {
		if (fTestMethod.isIgnored()) {
			fNotifier.fireTestIgnored(fDescription);
			return;
		}
		fNotifier.fireTestStarted(fDescription);
		try {
			long timeout= fTestMethod.getTimeout();
			if (timeout > 0)
				runWithTimeout(timeout);
			else
				runTest();
		} finally {
			fNotifier.fireTestFinished(fDescription);
		}
	}

	private void runWithTimeout(long timeout) {
		ExecutorService service= Executors.newSingleThreadExecutor();
		Callable<Object> callable= new Callable<Object>() {
			public Object call() throws Exception {
				runTest();
				return null;
			}
		};
		Future<Object> result= service.submit(callable);
		service.shutdown();
		try {
			boolean terminated= service.awaitTermination(timeout,
					TimeUnit.MILLISECONDS);
			if (!terminated)
				service.shutdownNow();
			result.get(0, TimeUnit.MILLISECONDS); // throws the exception if one occurred during the invocation
		} catch (TimeoutException e) {
			addFailure(new Exception(String.format("test timed out after %d milliseconds", timeout)));
		} catch (Exception e) {
			addFailure(e);
		}		
	}
	
	public void runTest() {
		try {
			runBefores();
			runTestMethod();
		} catch (FailedBefore e) {
		} finally {
			runAfters();
		}
	}

	protected void runTestMethod() {
		try {
			fMethod.invoke(fTest);
			if (fTestMethod.expectsException())
				addFailure(new AssertionError("Expected exception: " + fTestMethod.getExpectedException().getName()));
		} catch (InvocationTargetException e) {
			Throwable actual= e.getTargetException();
			if (!fTestMethod.expectsException())
				addFailure(actual);
			else if (fTestMethod.isUnexpected(actual)) {
				String message= "Unexpected exception, expected<" + fTestMethod.getExpectedException().getName() + "> but was<"
					+ actual.getClass().getName() + ">";
				addFailure(new Exception(message, actual));
			}
		} catch (Throwable e) {
			addFailure(e);
		}
	}
	
	private void runBefores() throws FailedBefore {
		try {
			List<Method> befores= fTestMethod.getBefores();
			for (Method before : befores)
				before.invoke(fTest);
		} catch (InvocationTargetException e) {
			addFailure(e.getTargetException());
			throw new FailedBefore();
		} catch (Throwable e) {
			addFailure(e);
			throw new FailedBefore();
		}
	}

	private void runAfters() {
		List<Method> afters= fTestMethod.getAfters();
		for (Method after : afters)
			try {
				after.invoke(fTest);
			} catch (InvocationTargetException e) {
				addFailure(e.getTargetException());
			} catch (Throwable e) {
				addFailure(e); // Untested, but seems impossible
			}
	}

	protected void addFailure(Throwable e) {
		fNotifier.fireTestFailure(new Failure(fDescription, e));
	}
}

