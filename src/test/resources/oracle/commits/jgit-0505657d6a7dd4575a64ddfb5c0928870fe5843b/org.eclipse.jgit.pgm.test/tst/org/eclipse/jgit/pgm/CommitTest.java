/*
 * Copyright (C) 2015, Andrey Loskutov <loskutov@gmx.de>
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
package org.eclipse.jgit.pgm;

import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.lib.CLIRepositoryTestCase;
import org.junit.Test;

public class CommitTest extends CLIRepositoryTestCase {

	@Test
	public void testCommitPath() throws Exception {
		writeTrashFile("a", "a");
		writeTrashFile("b", "a");
		String result = toString(execute("git add a"));
		assertTrue("Unexpected output: " + result, result.isEmpty());

		result = toString(execute("git status -- a"));
		assertTrue("Unexpected output: " + result,
				result.contains("new file:   a"));

		result = toString(execute("git status -- b"));
		assertTrue("Unexpected output: " + result,
				result.trim().contains("Untracked files:\n	b"));

		result = toString(execute("git commit a -m 'added a'"));
		assertTrue("Unexpected output: " + result, result.contains("added a"));

		result = toString(execute("git status -- a"));
		assertTrue("Unexpected output: " + result,
				result.trim().equals("On branch master"));

		result = toString(execute("git status -- b"));
		assertTrue("Unexpected output: " + result,
				result.trim().contains("Untracked files:\n	b"));
	}

	@Test
	public void testCommitAll() throws Exception {
		writeTrashFile("a", "a");
		writeTrashFile("b", "a");
		String result = toString(execute("git add a b"));
		assertTrue("Unexpected output: " + result, result.isEmpty());

		result = toString(execute("git status -- a b"));
		assertTrue("Unexpected output: " + result,
				result.contains("new file:   a"));
		assertTrue("Unexpected output: " + result,
				result.contains("new file:   b"));

		result = toString(execute("git commit -m 'added a b'"));
		assertTrue("Unexpected output: " + result,
				result.contains("added a b"));

		result = toString(execute("git status -- a b"));
		assertTrue("Unexpected output: " + result,
				result.trim().equals("On branch master"));
	}

	String toString(String[] arr) {
		StringBuilder sb = new StringBuilder();
		for (String s : arr) {
			if (s != null && !s.isEmpty()) {
				sb.append(s);
				if (!s.endsWith("\n")) {
					sb.append('\n');
				}
			}
		}
		return sb.toString();
	}
}
