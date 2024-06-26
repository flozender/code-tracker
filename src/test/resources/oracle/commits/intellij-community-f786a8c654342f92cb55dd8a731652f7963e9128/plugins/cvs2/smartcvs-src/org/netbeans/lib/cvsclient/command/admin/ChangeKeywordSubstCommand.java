/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.lib.cvsclient.command.admin;

import org.netbeans.lib.cvsclient.IClientEnvironment;
import org.netbeans.lib.cvsclient.IRequestProcessor;
import org.netbeans.lib.cvsclient.command.*;
import org.netbeans.lib.cvsclient.event.ICvsListenerRegistry;
import org.netbeans.lib.cvsclient.event.IEventSender;
import org.netbeans.lib.cvsclient.progress.IProgressViewer;
import org.netbeans.lib.cvsclient.progress.sending.FileStateRequestsProgressHandler;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.Requests;
import org.netbeans.lib.cvsclient.util.BugLog;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

/**
 * @author  Thomas Singer
 */
public class ChangeKeywordSubstCommand extends AbstractCommand {

	// Fields =================================================================

	private KeywordSubstitution keywordSubstitution;

	// Setup ==================================================================

	public ChangeKeywordSubstCommand() {
	}

	// Implemented ============================================================

	protected void resetCvsCommand() {
		super.resetCvsCommand();
		setKeywordSubstitution(null);
	}

	public boolean execute(IRequestProcessor requestProcessor, IEventSender eventManager, ICvsListenerRegistry listenerRegistry, IClientEnvironment clientEnvironment, IProgressViewer progressViewer) throws CommandException {
		BugLog.getInstance().assertNotNull(keywordSubstitution);

		final ICvsFiles cvsFiles;
		try {
			cvsFiles = scanFileSystem(getFileObjects(), clientEnvironment);
		}
		catch (IOException ex) {
			throw new IOCommandException(ex);
		}

		final Requests requests = new Requests(CommandRequest.ADMIN, clientEnvironment);
		requests.addArgumentRequest(keywordSubstitution, "-k");
		addFileRequests(cvsFiles, requests, clientEnvironment);
		requests.addLocalPathDirectoryRequest();
		addArgumentRequests(requests);

		return requestProcessor.processRequests(requests, FileStateRequestsProgressHandler.create(progressViewer, cvsFiles));
	}

	public String getCvsCommandLine() {
		@NonNls final StringBuffer cvsCommandLine = new StringBuffer("admin ");
		cvsCommandLine.append(getCvsArguments());
		appendFileArguments(cvsCommandLine);
		return cvsCommandLine.toString();
	}

	// Accessing ==============================================================

	public void setKeywordSubstitution(KeywordSubstitution keywordSubstitution) {
		this.keywordSubstitution = keywordSubstitution;
	}

	// Utils ==================================================================

	private String getCvsArguments() {
		@NonNls final StringBuffer arguments = new StringBuffer();
		arguments.append("-k" + keywordSubstitution + " ");
		return arguments.toString();
	}
}
