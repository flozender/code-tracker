/*
 * Copyright (C) 2010, Robin Stocker <robin@nibor.org>
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
package org.eclipse.jgit.merge;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;

/**
 * Formatter for constructing the commit message for a merge commit.
 * <p>
 * The format should be the same as C Git does it, for compatibility.
 */
public class MergeMessageFormatter {
	/**
	 * Construct the merge commit message.
	 *
	 * @param refsToMerge
	 *            the refs which will be merged
	 * @param target
	 *            the branch ref which will be merged into
	 * @return merge commit message
	 */
	public String format(List<Ref> refsToMerge, Ref target) {
		StringBuilder sb = new StringBuilder();
		sb.append("Merge ");

		List<String> branches = new ArrayList<String>();
		List<String> remoteBranches = new ArrayList<String>();
		List<String> tags = new ArrayList<String>();
		List<String> commits = new ArrayList<String>();
		List<String> others = new ArrayList<String>();
		for (Ref ref : refsToMerge) {
			if (ref.getName().startsWith(Constants.R_HEADS))
				branches.add("'" + Repository.shortenRefName(ref.getName())
						+ "'");

			else if (ref.getName().startsWith(Constants.R_REMOTES))
				remoteBranches.add("'"
						+ Repository.shortenRefName(ref.getName()) + "'");

			else if (ref.getName().startsWith(Constants.R_TAGS))
				tags.add("'" + Repository.shortenRefName(ref.getName()) + "'");

			else if (ref.getName().equals(ref.getObjectId().getName()))
				commits.add("'" + ref.getName() + "'");

			else
				others.add(ref.getName());
		}

		List<String> listings = new ArrayList<String>();

		if (!branches.isEmpty())
			listings.add(joinNames(branches, "branch", "branches"));

		if (!remoteBranches.isEmpty())
			listings.add(joinNames(remoteBranches, "remote branch",
					"remote branches"));

		if (!tags.isEmpty())
			listings.add(joinNames(tags, "tag", "tags"));

		if (!commits.isEmpty())
			listings.add(joinNames(commits, "commit", "commits"));

		if (!others.isEmpty())
			listings.add(StringUtils.join(others, ", ", " and "));

		sb.append(StringUtils.join(listings, ", "));

		if (!target.getName().equals(Constants.R_HEADS + Constants.MASTER)) {
			String targetShortName = Repository
					.shortenRefName(target.getName());
			sb.append(" into " + targetShortName);
		}

		return sb.toString();
	}

	private static String joinNames(List<String> names, String singular,
			String plural) {
		if (names.size() == 1)
			return singular + " " + names.get(0);
		else
			return plural + " " + StringUtils.join(names, ", ", " and ");
	}
}
