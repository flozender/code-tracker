/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.console.tasks;

import static org.junit.gen5.console.tasks.ColoredPrintingTestListener.INDENTATION;
import static org.junit.gen5.engine.TestExecutionResult.failed;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.reporting.ReportEntry;
import org.junit.gen5.engine.test.TestDescriptorStub;
import org.junit.gen5.launcher.TestIdentifier;
import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
public class ColoredPrintingTestListenerTests {

	@Test
	public void executionSkippedMessageIndented() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		ColoredPrintingTestListener listener = new ColoredPrintingTestListener(out, true);

		listener.executionSkipped(newTestIdentifier(), "Test\n\tdisabled");
		String output = stringWriter.toString();

		String expected = "Skipped:     failingTest [[engine:junit]]\n" + INDENTATION + "=> Reason: Test\n"
				+ INDENTATION + "\tdisabled\n";

		assertEquals(expected, output);
	}

	@Test
	public void reportingEntryPublishedMessageIndented() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		ColoredPrintingTestListener listener = new ColoredPrintingTestListener(out, true);

		listener.reportingEntryPublished(newTestIdentifier(), ReportEntry.from("foo", "bar"));
		String[] split = stringWriter.toString().split(System.lineSeparator());

		assertEquals(2, split.length);
		assertAll("lines in the message", () -> assertEquals("Reported:    failingTest [[engine:junit]]", split[0]),
			() -> assertTrue(split[1].startsWith(INDENTATION + "=> Reported values: ReportEntry [timestamp =")),
			() -> assertTrue(split[1].endsWith(", foo = 'bar']")));
	}

	@Test
	public void exceptionMessageIndented() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		ColoredPrintingTestListener listener = new ColoredPrintingTestListener(out, true);

		listener.executionFinished(newTestIdentifier(),
			failed(new Throwable("Fail\n\texpected: <foo> but was: <bar>")));
		String output = stringWriter.toString();

		String expected = "Finished:    failingTest [[engine:junit]]\n" + INDENTATION + "=> Exception: Fail\n"
				+ INDENTATION + "\texpected: <foo> but was: <bar>\n";

		assertEquals(expected, output);
	}

	private static TestIdentifier newTestIdentifier() {
		TestDescriptorStub testDescriptor = new TestDescriptorStub(UniqueId.forEngine("junit"), "failingTest");
		return TestIdentifier.from(testDescriptor);
	}

}
