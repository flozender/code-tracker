/*
 * Copyright (C) 2009, Christian Halstrick <christian.halstrick@sap.com>
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.merge.MergeChunk.ConflictState;

/**
 * Provides the merge algorithm which does a three-way merge on content provided
 * as RawText. Makes use of {@link MyersDiff} to compute the diffs.
 */
public final class MergeAlgorithm {

	/**
	 * Since this class provides only static methods I add a private default
	 * constructor to prevent instantiation.
	 */
	private MergeAlgorithm() {
	}

	// An special edit which acts as a sentinel value by marking the end the
	// list of edits
	private final static Edit END_EDIT = new Edit(Integer.MAX_VALUE,
			Integer.MAX_VALUE);

	/**
	 * Does the three way merge between a common base and two sequences.
	 *
	 * @param base the common base sequence
	 * @param ours the first sequence to be merged
	 * @param theirs the second sequence to be merged
	 * @return the resulting content
	 */
	public static MergeResult merge(Sequence base, Sequence ours,
			Sequence theirs) {
		List<Sequence> sequences = new ArrayList<Sequence>(3);
		sequences.add(base);
		sequences.add(ours);
		sequences.add(theirs);
		MergeResult result = new MergeResult(sequences);
		EditList oursEdits = new MyersDiff(base, ours).getEdits();
		Iterator<Edit> baseToOurs = oursEdits.iterator();
		EditList theirsEdits = new MyersDiff(base, theirs).getEdits();
		Iterator<Edit> baseToTheirs = theirsEdits.iterator();
		int current = 0; // points to the next line (first line is 0) of base
		                 // which was not handled yet
		Edit oursEdit = nextEdit(baseToOurs);
		Edit theirsEdit = nextEdit(baseToTheirs);

		// iterate over all edits from base to ours and from base to theirs
		// leave the loop when there are no edits more for ours or for theirs
		// (or both)
		while (theirsEdit != END_EDIT || oursEdit != END_EDIT) {
			if (oursEdit.getEndA() <= theirsEdit.getBeginA()) {
				// something was changed in ours not overlapping with any change
				// from theirs. First add the common part in front of the edit
				// then the edit.
				if (current != oursEdit.getBeginA()) {
					result.add(0, current, oursEdit.getBeginA(),
							ConflictState.NO_CONFLICT);
				}
				result.add(1, oursEdit.getBeginB(), oursEdit.getEndB(),
						ConflictState.NO_CONFLICT);
				current = oursEdit.getEndA();
				oursEdit = nextEdit(baseToOurs);
			} else if (theirsEdit.getEndA() <= oursEdit.getBeginA()) {
				// something was changed in theirs not overlapping with any
				// from ours. First add the common part in front of the edit
				// then the edit.
				if (current != theirsEdit.getBeginA()) {
					result.add(0, current, theirsEdit.getBeginA(),
							ConflictState.NO_CONFLICT);
				}
				result.add(2, theirsEdit.getBeginB(), theirsEdit.getEndB(),
						ConflictState.NO_CONFLICT);
				current = theirsEdit.getEndA();
				theirsEdit = nextEdit(baseToTheirs);
			} else {
				// here we found a real overlapping modification

				// if there is a common part in front of the conflict add it
				if (oursEdit.getBeginA() != current
						&& theirsEdit.getBeginA() != current) {
					result.add(0, current, Math.min(oursEdit.getBeginA(),
							theirsEdit.getBeginA()), ConflictState.NO_CONFLICT);
				}

				// set some initial values for the ranges in A and B which we
				// want to handle
				int oursBeginB = oursEdit.getBeginB();
				int theirsBeginB = theirsEdit.getBeginB();
				// harmonize the start of the ranges in A and B
				if (oursEdit.getBeginA() < theirsEdit.getBeginA()) {
					theirsBeginB -= theirsEdit.getBeginA()
							- oursEdit.getBeginA();
				} else {
					oursBeginB -= oursEdit.getBeginA() - theirsEdit.getBeginA();
				}

				// combine edits:
				// Maybe an Edit on one side corresponds to multiple Edits on
				// the other side. Then we have to combine the Edits of the
				// other side - so in the end we can merge together two single
				// edits.
				//
				// It is important to notice that this combining will extend the
				// ranges of our conflict always downwards (towards the end of
				// the content). The starts of the conflicting ranges in ours
				// and theirs are not touched here.
				//
				// This combining is an iterative process: after we have
				// combined some edits we have to do the check again. The
				// combined edits could now correspond to multiple edits on the
				// other side.
				//
				// Example: when this combining algorithm works on the following
				// edits
				// oursEdits=((0-5,0-5),(6-8,6-8),(10-11,10-11)) and
				// theirsEdits=((0-1,0-1),(2-3,2-3),(5-7,5-7))
				// it will merge them into
				// oursEdits=((0-8,0-8),(10-11,10-11)) and
				// theirsEdits=((0-7,0-7))
				//
				// Since the only interesting thing to us is how in ours and
				// theirs the end of the conflicting range is changing we let
				// oursEdit and theirsEdit point to the last conflicting edit
				Edit nextOursEdit = nextEdit(baseToOurs);
				Edit nextTheirsEdit = nextEdit(baseToTheirs);
				for (;;) {
					if (oursEdit.getEndA() > nextTheirsEdit.getBeginA()) {
						theirsEdit = nextTheirsEdit;
						nextTheirsEdit = nextEdit(baseToTheirs);
					} else if (theirsEdit.getEndA() > nextOursEdit.getBeginA()) {
						oursEdit = nextOursEdit;
						nextOursEdit = nextEdit(baseToOurs);
					} else {
						break;
					}
				}

				// harmonize the end of the ranges in A and B
				int oursEndB = oursEdit.getEndB();
				int theirsEndB = theirsEdit.getEndB();
				if (oursEdit.getEndA() < theirsEdit.getEndA()) {
					oursEndB += theirsEdit.getEndA() - oursEdit.getEndA();
				} else {
					theirsEndB += oursEdit.getEndA() - theirsEdit.getEndA();
				}

				// A conflicting region is found. Strip off common lines in
				// in the beginning and the end of the conflicting region
				int conflictLen = Math.min(oursEndB - oursBeginB, theirsEndB
						- theirsBeginB);
				int commonPrefix = 0;
				while (commonPrefix < conflictLen
						&& ours.equals(oursBeginB + commonPrefix, theirs,
								theirsBeginB + commonPrefix))
					commonPrefix++;
				conflictLen -= commonPrefix;
				int commonSuffix = 0;
				while (commonSuffix < conflictLen
						&& ours.equals(oursEndB - commonSuffix - 1, theirs,
								theirsEndB - commonSuffix - 1))
					commonSuffix++;
				conflictLen -= commonSuffix;

				// Add the common lines at start of conflict
				if (commonPrefix > 0)
					result.add(1, oursBeginB, oursBeginB + commonPrefix,
							ConflictState.NO_CONFLICT);

				// Add the conflict
				if (conflictLen > 0) {
					result.add(1, oursBeginB + commonPrefix, oursEndB
							- commonSuffix,
							ConflictState.FIRST_CONFLICTING_RANGE);
					result.add(2, theirsBeginB + commonPrefix, theirsEndB
							- commonSuffix,
							ConflictState.NEXT_CONFLICTING_RANGE);
				}

				// Add the common lines at end of conflict
				if (commonSuffix > 0)
					result.add(1, oursEndB - commonSuffix, oursEndB,
							ConflictState.NO_CONFLICT);

				current = Math.max(oursEdit.getEndA(), theirsEdit.getEndA());
				oursEdit = nextOursEdit;
				theirsEdit = nextTheirsEdit;
			}
		}
		// maybe we have a common part behind the last edit: copy it to the
		// result
		if (current < base.size()) {
			result.add(0, current, base.size(), ConflictState.NO_CONFLICT);
		}
		return result;
	}

	/**
	 * Helper method which returns the next Edit for an Iterator over Edits.
	 * When there are no more edits left this method will return the constant
	 * END_EDIT.
	 *
	 * @param it
	 *            the iterator for which the next edit should be returned
	 * @return the next edit from the iterator or END_EDIT if there no more
	 *         edits
	 */
	private static Edit nextEdit(Iterator<Edit> it) {
		return (it.hasNext() ? it.next() : END_EDIT);
	}
}
