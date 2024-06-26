/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Test the Encoding Utilities part of {@link XmlStreamReader}.
 */
public class XmlStreamReaderUtilitiesTest extends TestCase {

    private static String RAWMGS1 = "encoding mismatch";
    private static String RAWMGS2 = "unknown BOM";
    private static String HTTPMGS1 = "BOM must be NULL";
    private static String HTTPMGS2 = "encoding mismatch";
    private static String HTTPMGS3 = "Invalid MIME";

    private static String APPXML         = "application/xml";
    private static String APPXML_UTF8    = "application/xml;charset=UTF-8";
    private static String APPXML_UTF16   = "application/xml;charset=UTF-16";
    private static String APPXML_UTF16BE = "application/xml;charset=UTF-16BE";
    private static String APPXML_UTF16LE = "application/xml;charset=UTF-16LE";
    private static String TXTXML = "text/xml";

    @Override
    protected void setUp() {
    }

    /** Test for {@link XmlStreamReader#getContentTypeEncoding(String)}. */
    public void testContentTypeEncoding() throws IOException {
        checkContentTypeEncoding(null, null);
        checkContentTypeEncoding(null, "");
        checkContentTypeEncoding(null, "application/xml");
        checkContentTypeEncoding(null, "application/xml;");
        checkContentTypeEncoding(null, "multipart/mixed;boundary=frontier");
        checkContentTypeEncoding(null, "multipart/mixed;boundary='frontier'");
        checkContentTypeEncoding(null, "multipart/mixed;boundary=\"frontier\"");
        checkContentTypeEncoding("UTF-16", "application/xml;charset=utf-16");
        checkContentTypeEncoding("UTF-16", "application/xml;charset=UTF-16");
        checkContentTypeEncoding("UTF-16", "application/xml;charset='UTF-16'");
        checkContentTypeEncoding("UTF-16", "application/xml;charset=\"UTF-16\"");
    }
    private void checkContentTypeEncoding(String expected, String httpContentType) {
        assertEquals("ContentTypeEncoding=[" + httpContentType + "]", expected, XmlStreamReader.getContentTypeEncoding(httpContentType));
    }

    /** Test for {@link XmlStreamReader#getContentTypeEncoding(String)}. */
    public void testContentTypeMime() throws IOException {
        checkContentTypeMime(null, null);
        checkContentTypeMime("", "");
        checkContentTypeMime("application/xml", "application/xml");
        checkContentTypeMime("application/xml", "application/xml;");
        checkContentTypeMime("application/xml", "application/xml;charset=utf-16");
    }
    private void checkContentTypeMime(String expected, String httpContentType) {
        assertEquals("ContentTypeMime=[" + httpContentType + "]", expected, XmlStreamReader.getContentTypeMime(httpContentType));
    }

    /** Test for {@link XmlStreamReader#isAppXml(String)}. */
    public void testAppXml() throws IOException {
        checkAppXml(false, null);
        checkAppXml(false, "");
        checkAppXml(true,  "application/xml");
        checkAppXml(true,  "application/xml-dtd");
        checkAppXml(true,  "application/xml-external-parsed-entity");
        checkAppXml(true,  "application/soap+xml");
        checkAppXml(true,  "application/atom+xml");
        checkAppXml(false, "application/atomxml");
        checkAppXml(false, "text/xml");
        checkAppXml(false, "text/atom+xml");
        checkAppXml(true,  "application/xml-dtd");
        checkAppXml(true,  "application/xml-external-parsed-entity");
    }
    private void checkAppXml(boolean expected, String mime) {
        assertEquals("Mime=[" + mime + "]", expected, XmlStreamReader.isAppXml(mime));
    }

    /** Test for {@link XmlStreamReader#isTextXml(String)}. */
    public void testTextXml() throws IOException {
        checkTextXml(false, null);
        checkTextXml(false, "");
        checkTextXml(true,  "text/xml");
        checkTextXml(true,  "text/xml-external-parsed-entity");
        checkTextXml(true,  "text/soap+xml");
        checkTextXml(true,  "text/atom+xml");
        checkTextXml(false, "text/atomxml");
        checkTextXml(false, "application/xml");
        checkTextXml(false, "application/atom+xml");
    }
    private void checkTextXml(boolean expected, String mime) {
        assertEquals("Mime=[" + mime + "]", expected, XmlStreamReader.isTextXml(mime));
    }

    /** No BOM calculateRawEncoding() Test */
    public void testCalculateRawEncodingNoBOM() throws IOException {
        // No BOM        Expected    BOM         Guess       XML         Default
        checkRawError(RAWMGS2,       "UTF-32",   null,       null,       null);
        checkRawEncoding("UTF-8",    null,       null,       null,       null);
        checkRawEncoding("UTF-8",    null,       "UTF-16BE", null,       null); /* why default & not Guess? */
        checkRawEncoding("UTF-8",    null,       null,       "UTF-16BE", null); /* why default & not XMLEnc? */
        checkRawEncoding("UTF-16BE", null,       "UTF-16BE", "UTF-16BE", null);
        checkRawEncoding("UTF-16BE", null,       null,       null,       "UTF-16BE");
        checkRawEncoding("UTF-16BE", null,       "UTF-8",    null,       "UTF-16BE"); /* why default & not Guess? */
        checkRawEncoding("UTF-16BE", null,       null,       "UTF-8",    "UTF-16BE"); /* why default & not Guess? */
        checkRawEncoding("UTF-8",    null,       "UTF-8",    "UTF-8",    "UTF-16BE");
        checkRawEncoding("UTF-16BE", null,       "UTF-16BE", "UTF-16",   null);
        checkRawEncoding("UTF-16LE", null,       "UTF-16LE", "UTF-16",   null);
    }

    /** BOM calculateRawEncoding() Test */
    public void testCalculateRawEncodingStandard() throws IOException {
        // Standard BOM Checks           BOM         Other       Default
        testCalculateRawEncodingStandard("UTF-8",    "UTF-16BE", "UTF-16LE");
        testCalculateRawEncodingStandard("UTF-16BE", "UTF-8",    "UTF-16LE");
        testCalculateRawEncodingStandard("UTF-16LE", "UTF-8",    "UTF-16BE");
    }
    private void testCalculateRawEncodingStandard(String bomEnc, String otherEnc, String defaultEnc) throws IOException {
        //               Expected   BOM        Guess     XMLEnc    Default
        checkRawEncoding(bomEnc,    bomEnc,    null,     null,     defaultEnc);
        checkRawEncoding(bomEnc,    bomEnc,    bomEnc,   null,     defaultEnc);
        checkRawError(RAWMGS1,      bomEnc,    otherEnc, null,     defaultEnc);
        checkRawEncoding(bomEnc,    bomEnc,    null,     bomEnc,   defaultEnc);
        checkRawError(RAWMGS1,      bomEnc,    null,     otherEnc, defaultEnc);
        checkRawEncoding(bomEnc,    bomEnc,    bomEnc,   bomEnc,   defaultEnc);
        checkRawError(RAWMGS1,      bomEnc,    bomEnc,   otherEnc, defaultEnc);
        checkRawError(RAWMGS1,      bomEnc,    otherEnc, bomEnc,   defaultEnc);

    }

    /** Additional UTF-16 calculateRawEncoding() Test */
    public void testCalculateRawEncodingAdditonalkUTF16() throws IOException {
        //                           BOM         Guess       XML         Default
        checkRawError(RAWMGS1,       "UTF-16BE", "UTF-16",   null,       null);
        checkRawEncoding("UTF-16BE", "UTF-16BE", null,       "UTF-16",   null);
        checkRawEncoding("UTF-16BE", "UTF-16BE", "UTF-16BE", "UTF-16",   null);
        checkRawError(RAWMGS1,       "UTF-16BE", null,       "UTF-16LE", null);
        checkRawError(RAWMGS1,       "UTF-16BE", "UTF-16BE", "UTF-16LE", null);
        checkRawError(RAWMGS1,       "UTF-16LE", "UTF-16",   null,       null);
        checkRawEncoding("UTF-16LE", "UTF-16LE", null,       "UTF-16",   null);
        checkRawEncoding("UTF-16LE", "UTF-16LE", "UTF-16LE", "UTF-16",   null);
        checkRawError(RAWMGS1,       "UTF-16LE", null,       "UTF-16BE", null);
        checkRawError(RAWMGS1,       "UTF-16LE", "UTF-16LE", "UTF-16BE", null);
    }
    private void checkRawEncoding(String expected,
            String bomEnc, String xmlGuessEnc, String xmlEnc, String defaultEncoding) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("RawEncoding: ").append(bomEnc).append("], ");
        builder.append("bomEnc=[").append(bomEnc).append("], ");
        builder.append("xmlGuessEnc=[").append(xmlGuessEnc).append("], ");
        builder.append("xmlEnc=[").append(xmlEnc).append("], ");
        builder.append("defaultEncoding=[").append(defaultEncoding).append("],");
        MockXmlStreamReader mock = new MockXmlStreamReader(defaultEncoding);
        String encoding = mock.calculateRawEncoding(bomEnc,xmlGuessEnc,xmlEnc);
        assertEquals(builder.toString(), expected, encoding);
    }
    private void checkRawError(String msgSuffix,
            String bomEnc, String xmlGuessEnc, String xmlEnc, String defaultEncoding) {
        try {
            checkRawEncoding("XmlStreamReaderException", bomEnc, xmlGuessEnc, xmlEnc, defaultEncoding);
            fail("Expected XmlStreamReaderException");
        } catch (XmlStreamReaderException e) {
            assertTrue("Msg Start: " + e.getMessage(), e.getMessage().startsWith("Invalid encoding"));
            assertTrue("Msg End: "   + e.getMessage(), e.getMessage().endsWith(msgSuffix));
            assertEquals("bomEnc",      bomEnc,      e.getBomEncoding());
            assertEquals("xmlGuessEnc", xmlGuessEnc, e.getXmlGuessEncoding());
            assertEquals("xmlEnc",      xmlEnc,      e.getXmlEncoding());
            assertNull("ContentTypeEncoding", e.getContentTypeEncoding());
            assertNull("ContentTypeMime",     e.getContentTypeMime());
        } catch (Exception e) {
            fail("Expected XmlStreamReaderException, but threw " + e);
        }
    }

    /** Test calculate HTTP Encoding */
    public void testCalculateHttpEncoding() throws IOException {
        // No BOM        Expected     Lenient cType           BOM         Guess       XML         Default
        checkHttpError(HTTPMGS3,      true,   null,           null,       null,       null,       null);
        checkHttpError(HTTPMGS3,      false,  null,           null,       null,       "UTF-8",    null);
        checkHttpEncoding("UTF-8",    true,   null,           null,       null,       "UTF-8",    null);
        checkHttpEncoding("UTF-16LE", true,   null,           null,       null,       "UTF-16LE", null);
        checkHttpError(HTTPMGS3,      false,  "text/css",     null,       null,       null,       null);
        checkHttpEncoding("US-ASCII", false,  TXTXML,         null,       null,       null,       null);
        checkHttpEncoding("UTF-16BE", false,  TXTXML,         null,       null,       null,       "UTF-16BE");
        checkHttpEncoding("UTF-8",    false,  APPXML,         null,       null,       null,       null);
        checkHttpEncoding("UTF-16BE", false,  APPXML,         null,       null,       null,       "UTF-16BE");
        checkHttpEncoding("UTF-8",    false,  APPXML,         "UTF-8",    null,       null,       "UTF-16BE");
        checkHttpEncoding("UTF-16LE", false,  APPXML_UTF16LE, null,       null,       null,       null);
        checkHttpEncoding("UTF-16BE", false,  APPXML_UTF16BE, null,       null,       null,       null);
        checkHttpError(HTTPMGS1,      false,  APPXML_UTF16LE, "UTF-16LE", null,       null,       null);
        checkHttpError(HTTPMGS1,      false,  APPXML_UTF16BE, "UTF-16BE", null,       null,       null);
        checkHttpError(HTTPMGS2,      false,  APPXML_UTF16,   null,       null,       null,       null);
        checkHttpError(HTTPMGS2,      false,  APPXML_UTF16,   "UTF-8",    null,       null,       null);
        checkHttpEncoding("UTF-16LE", false,  APPXML_UTF16,   "UTF-16LE", null,       null,       null);
        checkHttpEncoding("UTF-16BE", false,  APPXML_UTF16,   "UTF-16BE", null,       null,       null);
        checkHttpEncoding("UTF-8",    false,  APPXML_UTF8,    null,       null,       null,       null);
        checkHttpEncoding("UTF-8",    false,  APPXML_UTF8,    "UTF-16BE", "UTF-16BE", "UTF-16BE", "UTF-16BE");
    }
    private void checkHttpEncoding(String expected, boolean lenienet, String httpContentType,
            String bomEnc, String xmlGuessEnc, String xmlEnc, String defaultEncoding) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("HttpEncoding: ").append(bomEnc).append("], ");
        builder.append("lenienet=[").append(lenienet).append("], ");
        builder.append("httpContentType=[").append(httpContentType).append("], ");
        builder.append("bomEnc=[").append(bomEnc).append("], ");
        builder.append("xmlGuessEnc=[").append(xmlGuessEnc).append("], ");
        builder.append("xmlEnc=[").append(xmlEnc).append("], ");
        builder.append("defaultEncoding=[").append(defaultEncoding).append("],");
        MockXmlStreamReader mock = new MockXmlStreamReader(defaultEncoding);
        String encoding = mock.calculateHttpEncoding(httpContentType, bomEnc, xmlGuessEnc, xmlEnc, lenienet);
        assertEquals(builder.toString(), expected, encoding);
    }
    private void checkHttpError(String msgSuffix, boolean lenienet, String httpContentType,
            String bomEnc, String xmlGuessEnc, String xmlEnc, String defaultEncoding) {
        try {
            checkHttpEncoding("XmlStreamReaderException", lenienet, httpContentType, bomEnc, xmlGuessEnc, xmlEnc, defaultEncoding);
            fail("Expected XmlStreamReaderException");
        } catch (XmlStreamReaderException e) {
            assertTrue("Msg Start: " + e.getMessage(), e.getMessage().startsWith("Invalid encoding"));
            assertTrue("Msg End: "   + e.getMessage(), e.getMessage().endsWith(msgSuffix));
            assertEquals("bomEnc",      bomEnc,      e.getBomEncoding());
            assertEquals("xmlGuessEnc", xmlGuessEnc, e.getXmlGuessEncoding());
            assertEquals("xmlEnc",      xmlEnc,      e.getXmlEncoding());
            assertEquals("ContentTypeEncoding", XmlStreamReader.getContentTypeEncoding(httpContentType),
                                                e.getContentTypeEncoding());
            assertEquals("ContentTypeMime", XmlStreamReader.getContentTypeMime(httpContentType),
                                            e.getContentTypeMime());
        } catch (Exception e) {
            fail("Expected XmlStreamReaderException, but threw " + e);
        }
    }

    /** Mock {@link XmlStreamReader} implementation */
    private static class MockXmlStreamReader extends XmlStreamReader {
        MockXmlStreamReader(String defaultEncoding) throws IOException {
            super(new ByteArrayInputStream("".getBytes()), null, true, defaultEncoding);
        }
    }
}
