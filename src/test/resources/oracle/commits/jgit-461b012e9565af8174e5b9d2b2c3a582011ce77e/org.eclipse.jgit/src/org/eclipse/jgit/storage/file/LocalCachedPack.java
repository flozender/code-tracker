/*
 * Copyright (C) 2011, Google Inc.
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

package org.eclipse.jgit.storage.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.pack.CachedPack;
import org.eclipse.jgit.storage.pack.PackOutputStream;

class LocalCachedPack extends CachedPack {
	private final ObjectDirectory odb;

	private final Set<ObjectId> tips;

	private final String[] packNames;

	LocalCachedPack(ObjectDirectory odb, Set<ObjectId> tips,
			List<String> packNames) {
		this.odb = odb;

		if (tips.size() == 1)
			this.tips = Collections.singleton(tips.iterator().next());
		else
			this.tips = Collections.unmodifiableSet(tips);

		this.packNames = packNames.toArray(new String[packNames.size()]);
	}

	@Override
	public Set<ObjectId> getTips() {
		return tips;
	}

	@Override
	public long getObjectCount() throws IOException {
		long cnt = 0;
		for (String packName : packNames)
			cnt += getPackFile(packName).getObjectCount();
		return cnt;
	}

	void copyAsIs(PackOutputStream out, WindowCursor wc) throws IOException {
		for (String packName : packNames)
			getPackFile(packName).copyPackAsIs(out, wc);
	}

	@Override
	public <T extends ObjectId> Set<ObjectId> hasObject(Iterable<T> toFind)
			throws IOException {
		PackFile[] packs = new PackFile[packNames.length];
		for (int i = 0; i < packNames.length; i++)
			packs[i] = getPackFile(packNames[i]);

		Set<ObjectId> have = new HashSet<ObjectId>();
		for (ObjectId id : toFind) {
			for (PackFile pack : packs) {
				if (pack.hasObject(id)) {
					have.add(id);
					break;
				}
			}
		}
		return have;
	}

	private PackFile getPackFile(String packName) throws FileNotFoundException {
		for (PackFile pack : odb.getPacks()) {
			if (packName.equals(pack.getPackName()))
				return pack;
		}
		throw new FileNotFoundException(getPackFilePath(packName));
	}

	private String getPackFilePath(String packName) {
		final File packDir = new File(odb.getDirectory(), "pack");
		return new File(packDir, "pack-" + packName + ".pack").getPath();
	}
}
