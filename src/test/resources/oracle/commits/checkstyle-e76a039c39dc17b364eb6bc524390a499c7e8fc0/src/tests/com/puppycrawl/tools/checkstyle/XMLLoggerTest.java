package com.puppycrawl.tools.checkstyle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

import junit.framework.TestCase;

/**
 * Enter a description of class XMLLoggerTest.java.
 * @author Rick Giles
 * @version 11-Dec-2002
 */
public class XMLLoggerTest extends TestCase
{
    private ByteArrayOutputStream outStream;

    public void setUp()
        throws Exception
    {
        outStream = new ByteArrayOutputStream();
    }

    public void testEncode()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, false);
        final String[][] encodings = {
            {"<", "&lt;"},
            {">", "&gt;"},
            {"'", "&apos;"},
            {"\"", "&quot;"},
            {"&", "&amp;"},
            {"&lt;", "&lt;"},
            {"abc;", "abc;"},
        };
        for (int i = 0; i < encodings.length; i++) {
            final String encoded = logger.encode(encodings[i][0]);
            assertEquals("\"" + encodings[i][0] + "\"", encodings[i][1], encoded);
        }
        outStream.close();
    }

    public void testIsReference()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, false);
        final String[] reference = {
            "&#0;",
            "&#x0;",
        };
        for (int i = 0; i < reference.length; i++) {
            assertTrue("reference: " + reference[i],
                       logger.isReference(reference[i]));
        }
        final String[] noReference = {
            "&",
            "&;",
            "&#;",
            "&#a;",
            "&#X0;",
            "&#x;",
            "&#xg;",
        };
        for (int i = 0; i < noReference.length; i++) {
            assertFalse("no reference: " + noReference[i],
                       logger.isReference(noReference[i]));
        }

        outStream.close();
    }

     public void testCloseStream()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, true);
        logger.auditStarted(null);
        logger.auditFinished(null);
        final String[] expectedLines = {};
        verifyLines(expectedLines);
    }

    public void testNoCloseStream()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, false);
        logger.auditStarted(null);
        logger.auditFinished(null);
        outStream.close();
        final String[] expectedLines = {};
        verifyLines(expectedLines);
    }

    public void testFileStarted()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, true);
        logger.auditStarted(null);
        final AuditEvent ev = new AuditEvent(this, "Test.java");
        logger.fileStarted(ev);
        logger.auditFinished(null);
        final String[] expectedLines = {"<file name=\"Test.java\">"};
        verifyLines(expectedLines);
    }

    public void testFileFinished()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, true);
        logger.auditStarted(null);
        final AuditEvent ev = new AuditEvent(this, "Test.java");
        logger.fileFinished(ev);
        logger.auditFinished(null);
        final String[] expectedLines = {"</file>"};
        verifyLines(expectedLines);
    }

    public void testAddError() throws IOException {
        final XMLLogger logger = new XMLLogger(outStream, true);
        logger.auditStarted(null);
        final LocalizedMessage message =
            new LocalizedMessage(
                1,
                1,
                "messages.properties",
                "key",
                null,
                SeverityLevel.ERROR,
                "aSource");
        final AuditEvent ev = new AuditEvent(this, "Test.java", message);
        logger.addError(ev);
        logger.auditFinished(null);
        final String[] expectedLines =
            {"<error line=\"1\" column=\"1\" severity=\"error\" message=\"key\" source=\"aSource\"/>"};
        verifyLines(expectedLines);
    }

    public void testAddException()
        throws IOException
    {
        final XMLLogger logger = new XMLLogger(outStream, true);
        logger.auditStarted(null);
        final LocalizedMessage message =
            new LocalizedMessage( 1, 1, "messages.properties", null, null);
        final AuditEvent ev = new AuditEvent(this, "Test.java", message);
        logger.addException(ev, new TestThrowable());
        logger.auditFinished(null);
        final String[] expectedLines = {
            "&lt;exception&gt;",
            "&lt;![CDATA[",
            "stackTrace]]&gt;",
            "&lt;/exception&gt;",
            "",
        };
        verifyLines(expectedLines);
    }

    private String[] getOutStreamLines()
        throws IOException
    {
        final byte[] bytes = outStream.toByteArray();
        final ByteArrayInputStream inStream =
            new ByteArrayInputStream(bytes);
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(inStream));
        final ArrayList lineList = new ArrayList();
        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            lineList.add(line);
        }
        reader.close();
        return (String[])lineList.toArray(new String[lineList.size()]);
    }

    /**
     * Verify output lines from auditStart to auditEnd.
     * Take into consideration checkstyle element (first and last lines).
     * @param strings
     * @param lines
     */
    private void verifyLines(String[] aExpectedLines)
        throws IOException
    {
        final String[] lines = getOutStreamLines();
        assertEquals("length.", aExpectedLines.length + 3, lines.length);
        assertEquals("first line.",
                     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                     lines[0]);
        assertEquals("second line.", "<checkstyle>", lines[1]);
        for (int i = 0; i < aExpectedLines.length; i++) {
            assertEquals("line " + i + ".", aExpectedLines[i], lines[i + 2]);
        }
        assertEquals("last line.", "</checkstyle>", lines[lines.length - 1]);
    }

    private class TestThrowable extends Exception
    {
        public void printStackTrace(PrintWriter s)
        {
            s.print("stackTrace");
        }
    }
}
