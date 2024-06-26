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
package org.netbeans.lib.cvsclient.command.remove;

import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.DefaultEntryParser;
import org.netbeans.lib.cvsclient.event.IEventSender;
import org.netbeans.lib.cvsclient.file.FileObject;
import org.netbeans.lib.cvsclient.file.ICvsFileSystem;

/**
 * @author  Thomas Singer
 */
final class RemoveParser extends DefaultEntryParser {

	// Setup ==================================================================

	public RemoveParser(IEventSender eventManager, ICvsFileSystem cvsFileSystem) {
		super(eventManager, cvsFileSystem);
	}

	// Implemented ============================================================

	public void gotEntry(FileObject fileObject, Entry entry) {
		if (!entry.isRemoved()) {
			return;
		}

		super.gotEntry(fileObject, entry);
	}
}

