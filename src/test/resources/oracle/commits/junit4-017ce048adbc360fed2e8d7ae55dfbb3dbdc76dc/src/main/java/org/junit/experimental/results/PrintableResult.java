package org.junit.experimental.results;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.internal.TextListener;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * A test result that prints nicely in error messages.
 * This is only intended to be used in JUnit self-tests.
 * For example:
 * 
 * <pre>
 *    assertThat(testResult(HasExpectedException.class), isSuccessful());
 * </pre>
 */
public class PrintableResult {
	public static PrintableResult testResult(Class<?> type) {
		return testResult(type, new Computer());
	}
	
	public static PrintableResult testResult(Class<?> type, Computer computer) {
		return new PrintableResult(type, computer);
	}
	
	private Result result;

	public PrintableResult(List<Failure> failures) {
		this(new FailureList(failures).result());
	}

	public PrintableResult(Result result) {
		this.result = result;
	}
	
	public PrintableResult(Class<?> type, Computer computer) {
		this(JUnitCore.runClasses(computer, type));
	}

	@Override
	public String toString() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		new TextListener(new PrintStream(stream)).testRunFinished(result);
		return stream.toString();
	}

	public List<Failure> getFailures() {
		return result.getFailures();
	}
}