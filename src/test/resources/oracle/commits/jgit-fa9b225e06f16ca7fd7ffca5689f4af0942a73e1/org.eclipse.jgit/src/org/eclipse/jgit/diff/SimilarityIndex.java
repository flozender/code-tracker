/*
 * Copyright (C) 2010, Google Inc.
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

package org.eclipse.jgit.diff;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;

/**
 * Index structure of lines/blocks in one file.
 * <p>
 * This structure can be used to compute an approximation of the similarity
 * between two files. The index is used by {@link SimilarityRenameDetector} to
 * compute scores between files.
 * <p>
 * To save space in memory, this index uses a space efficient encoding which
 * will not exceed 1 MiB per instance. The index starts out at a smaller size
 * (closer to 2 KiB), but may grow as more distinct blocks within the scanned
 * file are discovered.
 */
class SimilarityIndex {
	/** The {@link #idHash} table stops growing at {@code 1 << MAX_HASH_BITS}. */
	private static final int MAX_HASH_BITS = 17;

	/** The {@link #idHash} table will not grow bigger than this, ever. */
	private static final int MAX_HASH_SIZE = 1 << MAX_HASH_BITS;

	/** Prime just before {@link #MAX_HASH_SIZE}. */
	private static final int P = 131071;

	/**
	 * Shift to apply before storing a key.
	 * <p>
	 * Within the 64 bit table record space, we leave the highest bit unset so
	 * all values are positive, and we need {@link #MAX_HASH_BITS} bits for the
	 * keys. The lower 32 bits are used to count bytes impacted.
	 */
	private static final int KEY_SHIFT = 64 - 1 - MAX_HASH_BITS;

	/** Total size of the file we hashed into the structure. */
	private long fileSize;

	/** Number of non-zero entries in {@link #idHash}. */
	private int idSize;

	/**
	 * Pairings of content keys and counters.
	 * <p>
	 * Slots in the table are actually two ints wedged into a single long. The
	 * upper {@link #MAX_HASH_BITS} bits stores the content key, and the
	 * remaining lower bits stores the number of bytes associated with that key.
	 * Empty slots are denoted by 0, which cannot occur because the count cannot
	 * be 0. Values can only be positive, which we enforce during key addition.
	 */
	private long[] idHash;

	SimilarityIndex() {
		idHash = new long[256];
	}

	long getFileSize() {
		return fileSize;
	}

	void setFileSize(long size) {
		fileSize = size;
	}

	void hash(ObjectLoader obj) throws MissingObjectException, IOException {
		if (obj.isLarge()) {
			ObjectStream in = obj.openStream();
			try {
				setFileSize(in.getSize());
				hash(in, fileSize);
			} finally {
				in.close();
			}
		} else {
			byte[] raw = obj.getCachedBytes();
			setFileSize(raw.length);
			hash(raw, 0, raw.length);
		}
	}

	void hash(byte[] raw, int ptr, final int end) {
		while (ptr < end) {
			int hash = 5381;
			int start = ptr;

			// Hash one line, or one block, whichever occurs first.
			do {
				int c = raw[ptr++] & 0xff;
				if (c == '\n')
					break;
				hash = (hash << 5) ^ c;
			} while (ptr < end && ptr - start < 64);
			add(hash, ptr - start);
		}
	}

	void hash(InputStream in, long remaining) throws IOException {
		byte[] buf = new byte[4096];
		int ptr = 0;
		int cnt = 0;

		while (0 < remaining) {
			int hash = 5381;

			// Hash one line, or one block, whichever occurs first.
			int n = 0;
			do {
				if (ptr == cnt) {
					ptr = 0;
					cnt = in.read(buf, 0, buf.length);
					if (cnt <= 0)
						throw new EOFException();
				}

				n++;
				int c = buf[ptr++] & 0xff;
				if (c == '\n')
					break;
				hash = (hash << 5) ^ c;
			} while (n < 64 && n < remaining);
			add(hash, n);
			remaining -= n;
		}
	}

	/**
	 * Sort the internal table so it can be used for efficient scoring.
	 * <p>
	 * Once sorted, additional lines/blocks cannot be added to the index.
	 */
	void sort() {
		// Sort the array. All of the empty space will wind up at the front,
		// because we forced all of the keys to always be positive. Later
		// we only work with the back half of the array.
		//
		Arrays.sort(idHash);
	}

	int score(SimilarityIndex dst, int maxScore) {
		long max = Math.max(fileSize, dst.fileSize);
		if (max == 0)
			return maxScore;
		return (int) ((common(dst) * maxScore) / max);
	}

	int common(SimilarityIndex dst) {
		return common(this, dst);
	}

	private static int common(SimilarityIndex src, SimilarityIndex dst) {
		int srcIdx = src.packedIndex(0);
		int dstIdx = dst.packedIndex(0);
		long[] srcHash = src.idHash;
		long[] dstHash = dst.idHash;
		return common(srcHash, srcIdx, dstHash, dstIdx);
	}

	private static int common(long[] srcHash, int srcIdx, //
			long[] dstHash, int dstIdx) {
		if (srcIdx == srcHash.length || dstIdx == dstHash.length)
			return 0;

		int common = 0;
		int srcKey = keyOf(srcHash[srcIdx]);
		int dstKey = keyOf(dstHash[dstIdx]);

		for (;;) {
			if (srcKey == dstKey) {
				common += countOf(dstHash[dstIdx]);

				if (++srcIdx == srcHash.length)
					break;
				srcKey = keyOf(srcHash[srcIdx]);

				if (++dstIdx == dstHash.length)
					break;
				dstKey = keyOf(dstHash[dstIdx]);

			} else if (srcKey < dstKey) {
				// Regions of src which do not appear in dst.
				if (++srcIdx == srcHash.length)
					break;
				srcKey = keyOf(srcHash[srcIdx]);

			} else /* if (srcKey > dstKey) */{
				// Regions of dst which do not appear in dst.
				if (++dstIdx == dstHash.length)
					break;
				dstKey = keyOf(dstHash[dstIdx]);
			}
		}

		return common;
	}

	// Testing only
	int size() {
		return idSize;
	}

	// Testing only
	int key(int idx) {
		return keyOf(idHash[packedIndex(idx)]);
	}

	// Testing only
	long count(int idx) {
		return countOf(idHash[packedIndex(idx)]);
	}

	// Brute force approach only for testing.
	int findIndex(int key) {
		for (int i = 0; i < idSize; i++)
			if (key(i) == key)
				return i;
		return -1;
	}

	private int packedIndex(int idx) {
		return (idHash.length - idSize) + idx;
	}

	void add(int key, int cnt) {
		key = hash(key);
		int j = slot(key);
		for (;;) {
			long v = idHash[j];
			if (v == 0) {
				// Empty slot in the table, store here.
				if (shouldGrow()) {
					grow();
					j = slot(key);
					continue;
				}
				idHash[j] = (((long) key) << KEY_SHIFT) | cnt;
				idSize++;
				return;

			} else if (keyOf(v) == key) {
				// Same key, increment the counter.
				idHash[j] = v + cnt;
				return;

			} else if (++j >= idHash.length) {
				j = 0;
			}
		}
	}

	private static int hash(int key) {
		// Make the key fit into our table. Since we have a maximum size
		// that we cap the table at, all keys get squashed before going
		// into the table. This prevents overflow.
		//
		return (key >>> 1) % P;
	}

	private int slot(int key) {
		return key % idHash.length;
	}

	private boolean shouldGrow() {
		int n = idHash.length;
		return n < MAX_HASH_SIZE && n <= idSize * 2;
	}

	private void grow() {
		long[] oldHash = idHash;
		int oldSize = idHash.length;

		idHash = new long[2 * oldSize];
		for (int i = 0; i < oldSize; i++) {
			long v = oldHash[i];
			if (v != 0) {
				int j = slot(keyOf(v));
				while (idHash[j] != 0)
					if (++j >= idHash.length)
						j = 0;
				idHash[j] = v;
			}
		}
	}

	private static int keyOf(long v) {
		return (int) (v >>> KEY_SHIFT);
	}

	private static int countOf(long v) {
		return (int) v;
	}
}
