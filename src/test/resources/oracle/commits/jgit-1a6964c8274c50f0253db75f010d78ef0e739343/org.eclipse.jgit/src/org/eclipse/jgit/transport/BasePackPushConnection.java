/*
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
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

package org.eclipse.jgit.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.PackProtocolException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PackWriter;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

/**
 * Push implementation using the native Git pack transfer service.
 * <p>
 * This is the canonical implementation for transferring objects to the remote
 * repository from the local repository by talking to the 'git-receive-pack'
 * service. Objects are packed on the local side into a pack file and then sent
 * to the remote repository.
 * <p>
 * This connection requires only a bi-directional pipe or socket, and thus is
 * easily wrapped up into a local process pipe, anonymous TCP socket, or a
 * command executed through an SSH tunnel.
 * <p>
 * This implementation honors {@link Transport#isPushThin()} option.
 * <p>
 * Concrete implementations should just call
 * {@link #init(java.io.InputStream, java.io.OutputStream)} and
 * {@link #readAdvertisedRefs()} methods in constructor or before any use. They
 * should also handle resources releasing in {@link #close()} method if needed.
 */
class BasePackPushConnection extends BasePackConnection implements
		PushConnection {
	static final String CAPABILITY_REPORT_STATUS = "report-status";

	static final String CAPABILITY_DELETE_REFS = "delete-refs";

	static final String CAPABILITY_OFS_DELTA = "ofs-delta";

	private final boolean thinPack;

	private boolean capableDeleteRefs;

	private boolean capableReport;

	private boolean capableOfsDelta;

	private boolean sentCommand;

	private boolean writePack;

	/** Time in milliseconds spent transferring the pack data. */
	private long packTransferTime;

	BasePackPushConnection(final PackTransport packTransport) {
		super(packTransport);
		thinPack = transport.isPushThin();
	}

	public void push(final ProgressMonitor monitor,
			final Map<String, RemoteRefUpdate> refUpdates)
			throws TransportException {
		markStartedOperation();
		doPush(monitor, refUpdates);
	}

	@Override
	protected TransportException noRepository() {
		// Sadly we cannot tell the "invalid URI" case from "push not allowed".
		// Opening a fetch connection can help us tell the difference, as any
		// useful repository is going to support fetch if it also would allow
		// push. So if fetch throws NoRemoteRepositoryException we know the
		// URI is wrong. Otherwise we can correctly state push isn't allowed
		// as the fetch connection opened successfully.
		//
		try {
			transport.openFetch().close();
		} catch (NotSupportedException e) {
			// Fall through.
		} catch (NoRemoteRepositoryException e) {
			// Fetch concluded the repository doesn't exist.
			//
			return e;
		} catch (TransportException e) {
			// Fall through.
		}
		return new TransportException(uri, "push not permitted");
	}

	protected void doPush(final ProgressMonitor monitor,
			final Map<String, RemoteRefUpdate> refUpdates)
			throws TransportException {
		try {
			writeCommands(refUpdates.values(), monitor);
			if (writePack)
				writePack(refUpdates, monitor);
			if (sentCommand && capableReport)
				readStatusReport(refUpdates);
		} catch (TransportException e) {
			throw e;
		} catch (Exception e) {
			throw new TransportException(uri, e.getMessage(), e);
		} finally {
			close();
		}
	}

	private void writeCommands(final Collection<RemoteRefUpdate> refUpdates,
			final ProgressMonitor monitor) throws IOException {
		final String capabilities = enableCapabilities();
		for (final RemoteRefUpdate rru : refUpdates) {
			if (!capableDeleteRefs && rru.isDelete()) {
				rru.setStatus(Status.REJECTED_NODELETE);
				continue;
			}

			final StringBuilder sb = new StringBuilder();
			final Ref advertisedRef = getRef(rru.getRemoteName());
			final ObjectId oldId = (advertisedRef == null ? ObjectId.zeroId()
					: advertisedRef.getObjectId());
			sb.append(oldId.name());
			sb.append(' ');
			sb.append(rru.getNewObjectId().name());
			sb.append(' ');
			sb.append(rru.getRemoteName());
			if (!sentCommand) {
				sentCommand = true;
				sb.append(capabilities);
			}

			pckOut.writeString(sb.toString());
			rru.setStatus(sentCommand ? Status.AWAITING_REPORT : Status.OK);
			if (!rru.isDelete())
				writePack = true;
		}

		if (monitor.isCancelled())
			throw new TransportException(uri, "push cancelled");
		pckOut.end();
		outNeedsEnd = false;
	}

	private String enableCapabilities() {
		final StringBuilder line = new StringBuilder();
		capableReport = wantCapability(line, CAPABILITY_REPORT_STATUS);
		capableDeleteRefs = wantCapability(line, CAPABILITY_DELETE_REFS);
		capableOfsDelta = wantCapability(line, CAPABILITY_OFS_DELTA);
		if (line.length() > 0)
			line.setCharAt(0, '\0');
		return line.toString();
	}

	private void writePack(final Map<String, RemoteRefUpdate> refUpdates,
			final ProgressMonitor monitor) throws IOException {
		final PackWriter writer = new PackWriter(local, monitor);
		final ArrayList<ObjectId> remoteObjects = new ArrayList<ObjectId>(
				getRefs().size());
		final ArrayList<ObjectId> newObjects = new ArrayList<ObjectId>(
				refUpdates.size());

		for (final Ref r : getRefs())
			remoteObjects.add(r.getObjectId());
		remoteObjects.addAll(additionalHaves);
		for (final RemoteRefUpdate r : refUpdates.values()) {
			if (!ObjectId.zeroId().equals(r.getNewObjectId()))
				newObjects.add(r.getNewObjectId());
		}

		writer.setThin(thinPack);
		writer.setDeltaBaseAsOffset(capableOfsDelta);
		writer.preparePack(newObjects, remoteObjects);
		final long start = System.currentTimeMillis();
		writer.writePack(out);
		packTransferTime = System.currentTimeMillis() - start;
	}

	private void readStatusReport(final Map<String, RemoteRefUpdate> refUpdates)
			throws IOException {
		final String unpackLine = readStringLongTimeout();
		if (!unpackLine.startsWith("unpack "))
			throw new PackProtocolException(uri, "unexpected report line: "
					+ unpackLine);
		final String unpackStatus = unpackLine.substring("unpack ".length());
		if (!unpackStatus.equals("ok"))
			throw new TransportException(uri,
					"error occurred during unpacking on the remote end: "
							+ unpackStatus);

		String refLine;
		while ((refLine = pckIn.readString()) != PacketLineIn.END) {
			boolean ok = false;
			int refNameEnd = -1;
			if (refLine.startsWith("ok ")) {
				ok = true;
				refNameEnd = refLine.length();
			} else if (refLine.startsWith("ng ")) {
				ok = false;
				refNameEnd = refLine.indexOf(" ", 3);
			}
			if (refNameEnd == -1)
				throw new PackProtocolException(uri
						+ ": unexpected report line: " + refLine);
			final String refName = refLine.substring(3, refNameEnd);
			final String message = (ok ? null : refLine
					.substring(refNameEnd + 1));

			final RemoteRefUpdate rru = refUpdates.get(refName);
			if (rru == null)
				throw new PackProtocolException(uri
						+ ": unexpected ref report: " + refName);
			if (ok) {
				rru.setStatus(Status.OK);
			} else {
				rru.setStatus(Status.REJECTED_OTHER_REASON);
				rru.setMessage(message);
			}
		}
		for (final RemoteRefUpdate rru : refUpdates.values()) {
			if (rru.getStatus() == Status.AWAITING_REPORT)
				throw new PackProtocolException(uri
						+ ": expected report for ref " + rru.getRemoteName()
						+ " not received");
		}
	}

	private String readStringLongTimeout() throws IOException {
		if (timeoutIn == null)
			return pckIn.readString();

		// The remote side may need a lot of time to choke down the pack
		// we just sent them. There may be many deltas that need to be
		// resolved by the remote. Its hard to say how long the other
		// end is going to be silent. Taking 10x the configured timeout
		// or the time spent transferring the pack, whichever is larger,
		// gives the other side some reasonable window to process the data,
		// but this is just a wild guess.
		//
		final int oldTimeout = timeoutIn.getTimeout();
		final int sendTime = (int) Math.min(packTransferTime, 28800000L);
		try {
			timeoutIn.setTimeout(10 * Math.max(sendTime, oldTimeout));
			return pckIn.readString();
		} finally {
			timeoutIn.setTimeout(oldTimeout);
		}
	}
}
