/*
 * Copyright (C) 2009, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;

// Note, test vectors created with:
//
// perl -e 'printf "%4.4x%s\n", 4+length($ARGV[0]),$ARGV[0]'

public class PacketLineInTest extends TestCase {
	private ByteArrayInputStream rawIn;

	private PacketLineIn in;

	// readString

	public void testReadString1() throws IOException {
		init("0006a\n0007bc\n");
		assertEquals("a", in.readString());
		assertEquals("bc", in.readString());
		assertEOF();
	}

	public void testReadString2() throws IOException {
		init("0032want fcfcfb1fd94829c1a1704f894fc111d14770d34e\n");
		final String act = in.readString();
		assertEquals("want fcfcfb1fd94829c1a1704f894fc111d14770d34e", act);
		assertEOF();
	}

	public void testReadString4() throws IOException {
		init("0005a0006bc");
		assertEquals("a", in.readString());
		assertEquals("bc", in.readString());
		assertEOF();
	}

	public void testReadString5() throws IOException {
		// accept both upper and lower case
		init("000Fhi i am a s");
		assertEquals("hi i am a s", in.readString());
		assertEOF();

		init("000fhi i am a s");
		assertEquals("hi i am a s", in.readString());
		assertEOF();
	}

	public void testReadString_LenHELO() {
		init("HELO");
		try {
			in.readString();
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: HELO", e.getMessage());
		}
	}

	public void testReadString_Len0001() {
		init("0001");
		try {
			in.readString();
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: 0001", e.getMessage());
		}
	}

	public void testReadString_Len0002() {
		init("0002");
		try {
			in.readString();
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: 0002", e.getMessage());
		}
	}

	public void testReadString_Len0003() {
		init("0003");
		try {
			in.readString();
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: 0003", e.getMessage());
		}
	}

	public void testReadString_Len0004() throws IOException {
		init("0004");
		final String act = in.readString();
		assertEquals("", act);
		assertNotSame(PacketLineIn.END, act);
		assertEOF();
	}

	public void testReadString_End() throws IOException {
		init("0000");
		assertSame(PacketLineIn.END, in.readString());
		assertEOF();
	}

	// readStringNoLF

	public void testReadStringRaw1() throws IOException {
		init("0005a0006bc");
		assertEquals("a", in.readStringRaw());
		assertEquals("bc", in.readStringRaw());
		assertEOF();
	}

	public void testReadStringRaw2() throws IOException {
		init("0031want fcfcfb1fd94829c1a1704f894fc111d14770d34e");
		final String act = in.readStringRaw();
		assertEquals("want fcfcfb1fd94829c1a1704f894fc111d14770d34e", act);
		assertEOF();
	}

	public void testReadStringRaw3() throws IOException {
		init("0004");
		final String act = in.readStringRaw();
		assertEquals("", act);
		assertNotSame(PacketLineIn.END, act);
		assertEOF();
	}

	public void testReadStringRaw_End() throws IOException {
		init("0000");
		assertSame(PacketLineIn.END, in.readStringRaw());
		assertEOF();
	}

	public void testReadStringRaw4() {
		init("HELO");
		try {
			in.readStringRaw();
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: HELO", e.getMessage());
		}
	}

	// readACK

	public void testReadACK_NAK() throws IOException {
		final ObjectId expid = ObjectId
				.fromString("fcfcfb1fd94829c1a1704f894fc111d14770d34e");
		final MutableObjectId actid = new MutableObjectId();
		actid.fromString(expid.name());

		init("0008NAK\n");
		assertSame(PacketLineIn.AckNackResult.NAK, in.readACK(actid));
		assertTrue(actid.equals(expid));
		assertEOF();
	}

	public void testReadACK_ACK1() throws IOException {
		final ObjectId expid = ObjectId
				.fromString("fcfcfb1fd94829c1a1704f894fc111d14770d34e");
		final MutableObjectId actid = new MutableObjectId();

		init("0031ACK fcfcfb1fd94829c1a1704f894fc111d14770d34e\n");
		assertSame(PacketLineIn.AckNackResult.ACK, in.readACK(actid));
		assertTrue(actid.equals(expid));
		assertEOF();
	}

	public void testReadACK_ACKcontinue1() throws IOException {
		final ObjectId expid = ObjectId
				.fromString("fcfcfb1fd94829c1a1704f894fc111d14770d34e");
		final MutableObjectId actid = new MutableObjectId();

		init("003aACK fcfcfb1fd94829c1a1704f894fc111d14770d34e continue\n");
		assertSame(PacketLineIn.AckNackResult.ACK_CONTINUE, in.readACK(actid));
		assertTrue(actid.equals(expid));
		assertEOF();
	}

	public void testReadACK_Invalid1() {
		init("HELO");
		try {
			in.readACK(new MutableObjectId());
			fail("incorrectly accepted invalid packet header");
		} catch (IOException e) {
			assertEquals("Invalid packet line header: HELO", e.getMessage());
		}
	}

	public void testReadACK_Invalid2() {
		init("0009HELO\n");
		try {
			in.readACK(new MutableObjectId());
			fail("incorrectly accepted invalid ACK/NAK");
		} catch (IOException e) {
			assertEquals("Expected ACK/NAK, got: HELO", e.getMessage());
		}
	}

	public void testReadACK_Invalid3() {
		init("0000");
		try {
			in.readACK(new MutableObjectId());
			fail("incorrectly accepted no ACK/NAK");
		} catch (IOException e) {
			assertEquals("Expected ACK/NAK, found EOF", e.getMessage());
		}
	}

	// test support

	private void init(final String msg) {
		rawIn = new ByteArrayInputStream(Constants.encodeASCII(msg));
		in = new PacketLineIn(rawIn);
	}

	private void assertEOF() {
		assertEquals(-1, rawIn.read());
	}
}
