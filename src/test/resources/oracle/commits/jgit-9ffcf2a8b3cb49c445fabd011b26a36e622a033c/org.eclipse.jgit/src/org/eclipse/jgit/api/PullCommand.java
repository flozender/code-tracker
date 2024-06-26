/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
package org.eclipse.jgit.api;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;

/**
 * The Pull command
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-pull.html"
 *      >Git documentation about Pull</a>
 */
public class PullCommand extends GitCommand<PullResult> {
	private int timeout = 0;

	private final static String DOT = ".";

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	private CredentialsProvider credentialsProvider;

	/**
	 * @param repo
	 */
	protected PullCommand(Repository repo) {
		super(repo);
	}

	/**
	 * @param timeout
	 *            in seconds
	 * @return this instance
	 */
	public PullCommand setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * @param monitor
	 *            a progress monitor
	 * @return this instance
	 */
	public PullCommand setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	/**
	 * @param credentialsProvider
	 *            the {@link CredentialsProvider} to use
	 * @return this instance
	 */
	public PullCommand setCredentialsProvider(
			CredentialsProvider credentialsProvider) {
		checkCallable();
		this.credentialsProvider = credentialsProvider;
		return this;
	}

	/**
	 * Executes the {@code Pull} command with all the options and parameters
	 * collected by the setter methods (e.g.
	 * {@link #setProgressMonitor(ProgressMonitor)}) of this class. Each
	 * instance of this class should only be used for one invocation of the
	 * command. Don't call this method twice on an instance.
	 *
	 * @return the result of the pull
	 */
	public PullResult call() throws WrongRepositoryStateException,
			InvalidConfigurationException, DetachedHeadException,
			InvalidRemoteException, CanceledException {
		checkCallable();

		monitor.beginTask(JGitText.get().pullTaskName, 2);

		String branchName;
		try {
			String fullBranch = repo.getFullBranch();
			if (!fullBranch.startsWith(Constants.R_HEADS)) {
				// we can not pull if HEAD is detached and branch is not
				// specified explicitly
				throw new DetachedHeadException();
			}
			branchName = fullBranch.substring(Constants.R_HEADS.length());
		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
					e);
		}

		if (!repo.getRepositoryState().equals(RepositoryState.SAFE))
			throw new WrongRepositoryStateException(MessageFormat.format(
					JGitText.get().cannotPullOnARepoWithState, repo
							.getRepositoryState().name()));

		// get the configured remote for the currently checked out branch
		// stored in configuration key branch.<branch name>.remote
		Config repoConfig = repo.getConfig();
		final String remote = repoConfig.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REMOTE);
		if (remote == null) {
			String missingKey = ConfigConstants.CONFIG_BRANCH_SECTION + DOT
					+ branchName + DOT + ConfigConstants.CONFIG_KEY_REMOTE;
			throw new InvalidConfigurationException(MessageFormat.format(
					JGitText.get().missingConfigurationForKey, missingKey));
		}

		// get the name of the branch in the remote repository
		// stored in configuration key branch.<branch name>.merge
		String remoteBranchName = repoConfig.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_MERGE);
		boolean doRebase = false;
		if (remoteBranchName == null) {
			// check if the branch is configured for pull-rebase
			remoteBranchName = repoConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REBASE);
			if (remoteBranchName != null) {
				doRebase = true;
			}
		}

		if (remoteBranchName == null) {
			String missingKey = ConfigConstants.CONFIG_BRANCH_SECTION + DOT
					+ branchName + DOT + ConfigConstants.CONFIG_KEY_MERGE;
			throw new InvalidConfigurationException(MessageFormat.format(
					JGitText.get().missingConfigurationForKey, missingKey));
		}

		final boolean isRemote = !remote.equals(".");
		String remoteUri;
		FetchResult fetchRes;
		if (isRemote) {
			remoteUri = repoConfig.getString("remote", remote,
					ConfigConstants.CONFIG_KEY_URL);
			if (remoteUri == null) {
				String missingKey = ConfigConstants.CONFIG_REMOTE_SECTION + DOT
						+ remote + DOT + ConfigConstants.CONFIG_KEY_URL;
				throw new InvalidConfigurationException(MessageFormat.format(
						JGitText.get().missingConfigurationForKey, missingKey));
			}

			if (monitor.isCancelled())
				throw new CanceledException(MessageFormat.format(
						JGitText.get().operationCanceled,
						JGitText.get().pullTaskName));

			FetchCommand fetch = new FetchCommand(repo);
			fetch.setRemote(remote);
			fetch.setProgressMonitor(monitor);
			fetch.setTimeout(this.timeout);
			fetch.setCredentialsProvider(credentialsProvider);

			fetchRes = fetch.call();
		} else {
			// we can skip the fetch altogether
			remoteUri = "local repository";
			fetchRes = null;
		}

		monitor.update(1);

		if (monitor.isCancelled())
			throw new CanceledException(MessageFormat.format(
					JGitText.get().operationCanceled,
					JGitText.get().pullTaskName));

		PullResult result;
		if (doRebase) {
			RebaseCommand rebase = new RebaseCommand(repo);
			try {
				RebaseResult rebaseRes = rebase.setUpstream(remoteBranchName)
						.setProgressMonitor(monitor).setOperation(
								Operation.BEGIN).call();
				result = new PullResult(fetchRes, remote, rebaseRes);
			} catch (NoHeadException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (RefNotFoundException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (JGitInternalException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (GitAPIException e) {
				throw new JGitInternalException(e.getMessage(), e);
			}
		} else {
			// we check the updates to see which of the updated branches
			// corresponds
			// to the remote branch name
			AnyObjectId commitToMerge;

			if (isRemote) {
				Ref r = null;
				if (fetchRes != null) {
					r = fetchRes.getAdvertisedRef(remoteBranchName);
					if (r == null)
						r = fetchRes.getAdvertisedRef(Constants.R_HEADS
								+ remoteBranchName);
				}
				if (r == null)
					throw new JGitInternalException(MessageFormat.format(
							JGitText.get().couldNotGetAdvertisedRef,
							remoteBranchName));
				else
					commitToMerge = r.getObjectId();
			} else {
				try {
					commitToMerge = repo.resolve(remoteBranchName);
				} catch (IOException e) {
					throw new JGitInternalException(
							JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
							e);
				}
			}
			MergeCommand merge = new MergeCommand(repo);
			merge.include(
					"branch \'" + remoteBranchName + "\' of " + remoteUri,
					commitToMerge);
			MergeResult mergeRes;
			try {
				mergeRes = merge.call();
				monitor.update(1);
				result = new PullResult(fetchRes, remote, mergeRes);
			} catch (NoHeadException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (ConcurrentRefUpdateException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (CheckoutConflictException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (InvalidMergeHeadsException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (WrongRepositoryStateException e) {
				throw new JGitInternalException(e.getMessage(), e);
			} catch (NoMessageException e) {
				throw new JGitInternalException(e.getMessage(), e);
			}
		}
		monitor.endTask();
		return result;
	}

}
