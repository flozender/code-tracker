/*
 * Copyright (C) 2008, Mike Ralphson <mike@abacus.co.uk>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg.lists@dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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

package org.eclipse.jgit.lib;

import org.eclipse.jgit.JGitText;
/**
 * Important state of the repository that affects what can and cannot bed
 * done. This is things like unhandled conflicted merges and unfinished rebase.
 *
 * The granularity and set of states are somewhat arbitrary. The methods
 * on the state are the only supported means of deciding what to do.
 */
public enum RepositoryState {
	/**
	 * A safe state for working normally
	 * */
	SAFE {
		public boolean canCheckout() { return true; }
		public boolean canResetHead() { return true; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_normal; }
	},

	/** An unfinished merge. Must resole or reset before continuing normally
	 */
	MERGING {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return false; }
		public String getDescription() { return JGitText.get().repositoryState_conflicts; }
	},

	/**
	 * An merge where all conflicts have been resolved. The index does not
	 * contain any unmerged paths.
	 */
	MERGING_RESOLVED {
		public boolean canCheckout() { return true; }
		public boolean canResetHead() { return true; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_merged; }
	},

	/**
	 * An unfinished rebase or am. Must resolve, skip or abort before normal work can take place
	 */
	REBASING {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_rebaseOrApplyMailbox; }
	},

	/**
	 * An unfinished rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_REBASING {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_rebase; }
	},

	/**
	 * An unfinished apply. Must resolve, skip or abort before normal work can take place
	 */
	APPLY {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_applyMailbox; }
	},

	/**
	 * An unfinished rebase with merge. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_MERGE {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_rebaseWithMerge; }
	},

	/**
	 * An unfinished interactive rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_INTERACTIVE {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return JGitText.get().repositoryState_rebaseInteractive; }
	},

	/**
	 * Bisecting being done. Normal work may continue but is discouraged
	 */
	BISECTING {
		/* Changing head is a normal operation when bisecting */
		public boolean canCheckout() { return true; }

		/* Do not reset, checkout instead */
		public boolean canResetHead() { return false; }

		/* Commit during bisect is useful */
		public boolean canCommit() { return true; }

		public String getDescription() { return JGitText.get().repositoryState_bisecting; }
	};

	/**
	 * @return true if changing HEAD is sane.
	 */
	public abstract boolean canCheckout();

	/**
	 * @return true if we can commit
	 */
	public abstract boolean canCommit();

	/**
	 * @return true if reset to another HEAD is considered SAFE
	 */
	public abstract boolean canResetHead();

	/**
	 * @return a human readable description of the state.
	 */
	public abstract String getDescription();
}
