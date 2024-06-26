/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//io/src/test/org/apache/commons/io/HexDumpTest.java,v 1.1 2002/02/22 06:13:44 bayard Exp $
 * $Revision: 1.1 $
 * $Date: 2002/02/22 06:13:44 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */


package org.apache.commons.io;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;


/**
 * @author Scott Sanders (sanders at apache dot org)
 * @author Marc Johnson (mjohnson at apache dot org)
 * @version $Revision: 1.1 $ $Date: 2002/02/22 06:13:44 $
 */

public class HexDumpTest extends TestCase {

    /**
     * Creates new HexDumpTest
     *
     * @param name
     */

    public HexDumpTest(String name) {
        super(name);
    }

    private char toHex(final int n) {
        char[] hexChars =
                {
                    '0', '1', '2', '3', '4', '5', '6', '7',
                    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
                };

        return hexChars[n % 16];
    }

    /**
     * test dump method
     *
     * @exception IOException
     */

    public void testDump()
            throws IOException {
        byte[] testArray = new byte[256];

        for (int j = 0; j < 256; j++) {
            testArray[j] = (byte) j;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        HexDump.dump(testArray, 0, stream, 0);
        byte[] outputArray = new byte[16 * (73 + HexDump.EOL.length())];

        for (int j = 0; j < 16; j++) {
            int offset = (73 + HexDump.EOL.length()) * j;

            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) toHex(j);
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) ' ';
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toHex(j);
                outputArray[offset++] = (byte) toHex(k);
                outputArray[offset++] = (byte) ' ';
            }
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toAscii((j * 16) + k);
            }
            System.arraycopy(HexDump.EOL.getBytes(), 0, outputArray, offset,
                    HexDump.EOL.getBytes().length);
        }
        byte[] actualOutput = stream.toByteArray();

        assertEquals("array size mismatch", outputArray.length,
                actualOutput.length);
        for (int j = 0; j < outputArray.length; j++) {
            assertEquals("array[ " + j + "] mismatch", outputArray[j],
                    actualOutput[j]);
        }

        // verify proper behavior with non-zero offset
        stream = new ByteArrayOutputStream();
        HexDump.dump(testArray, 0x10000000, stream, 0);
        outputArray = new byte[16 * (73 + HexDump.EOL.length())];
        for (int j = 0; j < 16; j++) {
            int offset = (73 + HexDump.EOL.length()) * j;

            outputArray[offset++] = (byte) '1';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) toHex(j);
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) ' ';
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toHex(j);
                outputArray[offset++] = (byte) toHex(k);
                outputArray[offset++] = (byte) ' ';
            }
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toAscii((j * 16) + k);
            }
            System.arraycopy(HexDump.EOL.getBytes(), 0, outputArray, offset,
                    HexDump.EOL.getBytes().length);
        }
        actualOutput = stream.toByteArray();
        assertEquals("array size mismatch", outputArray.length,
                actualOutput.length);
        for (int j = 0; j < outputArray.length; j++) {
            assertEquals("array[ " + j + "] mismatch", outputArray[j],
                    actualOutput[j]);
        }

        // verify proper behavior with negative offset
        stream = new ByteArrayOutputStream();
        HexDump.dump(testArray, 0xFF000000, stream, 0);
        outputArray = new byte[16 * (73 + HexDump.EOL.length())];
        for (int j = 0; j < 16; j++) {
            int offset = (73 + HexDump.EOL.length()) * j;

            outputArray[offset++] = (byte) 'F';
            outputArray[offset++] = (byte) 'F';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) toHex(j);
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) ' ';
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toHex(j);
                outputArray[offset++] = (byte) toHex(k);
                outputArray[offset++] = (byte) ' ';
            }
            for (int k = 0; k < 16; k++) {
                outputArray[offset++] = (byte) toAscii((j * 16) + k);
            }
            System.arraycopy(HexDump.EOL.getBytes(), 0, outputArray, offset,
                    HexDump.EOL.getBytes().length);
        }
        actualOutput = stream.toByteArray();
        assertEquals("array size mismatch", outputArray.length,
                actualOutput.length);
        for (int j = 0; j < outputArray.length; j++) {
            assertEquals("array[ " + j + "] mismatch", outputArray[j],
                    actualOutput[j]);
        }

        // verify proper behavior with non-zero index
        stream = new ByteArrayOutputStream();
        HexDump.dump(testArray, 0x10000000, stream, 0x81);
        outputArray = new byte[(8 * (73 + HexDump.EOL.length())) - 1];
        for (int j = 0; j < 8; j++) {
            int offset = (73 + HexDump.EOL.length()) * j;

            outputArray[offset++] = (byte) '1';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) '0';
            outputArray[offset++] = (byte) toHex(j + 8);
            outputArray[offset++] = (byte) '1';
            outputArray[offset++] = (byte) ' ';
            for (int k = 0; k < 16; k++) {
                int index = 0x81 + (j * 16) + k;

                if (index < 0x100) {
                    outputArray[offset++] = (byte) toHex(index / 16);
                    outputArray[offset++] = (byte) toHex(index);
                } else {
                    outputArray[offset++] = (byte) ' ';
                    outputArray[offset++] = (byte) ' ';
                }
                outputArray[offset++] = (byte) ' ';
            }
            for (int k = 0; k < 16; k++) {
                int index = 0x81 + (j * 16) + k;

                if (index < 0x100) {
                    outputArray[offset++] = (byte) toAscii(index);
                }
            }
            System.arraycopy(HexDump.EOL.getBytes(), 0, outputArray, offset,
                    HexDump.EOL.getBytes().length);
        }
        actualOutput = stream.toByteArray();
        assertEquals("array size mismatch", outputArray.length,
                actualOutput.length);
        for (int j = 0; j < outputArray.length; j++) {
            assertEquals("array[ " + j + "] mismatch", outputArray[j],
                    actualOutput[j]);
        }

        // verify proper behavior with negative index
        try {
            HexDump.dump(testArray, 0x10000000, new ByteArrayOutputStream(),
                    -1);
            fail("should have caught ArrayIndexOutOfBoundsException on negative index");
        } catch (ArrayIndexOutOfBoundsException ignored_exception) {

            // as expected
        }

        // verify proper behavior with index that is too large
        try {
            HexDump.dump(testArray, 0x10000000, new ByteArrayOutputStream(),
                    testArray.length);
            fail("should have caught ArrayIndexOutOfBoundsException on large index");
        } catch (ArrayIndexOutOfBoundsException ignored_exception) {

            // as expected
        }

        // verify proper behavior with null stream
        try {
            HexDump.dump(testArray, 0x10000000, null, 0);
            fail("should have caught IllegalArgumentException on negative index");
        } catch (IllegalArgumentException ignored_exception) {

            // as expected
        }
    }

    private char toAscii(final int c) {
        char rval = '.';

        if ((c >= 32) && (c <= 126)) {
            rval = (char) c;
        }
        return rval;
    }

    /**
     * main method to run the unit tests
     *
     * @param ignored_args
     */

    public static void main(String[] ignored_args) {
        System.out.println("Testing io.HexDump functionality");
        junit.textui.TestRunner.run(HexDumpTest.class);
    }
}
