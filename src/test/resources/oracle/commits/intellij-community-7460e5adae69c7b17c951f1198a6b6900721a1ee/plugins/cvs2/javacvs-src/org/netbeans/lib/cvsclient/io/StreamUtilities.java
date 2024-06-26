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
package org.netbeans.lib.cvsclient.io;

import java.io.IOException;
import java.io.Reader;

/**
 * @author  Thomas Singer
 */
public final class StreamUtilities {

	public static String readLine(Reader reader) throws IOException {
		final StringBuffer buffer = new StringBuffer(512);
		for (;;) {
			int value = reader.read();
			if (value < 0) {
				if (buffer.length() == 0) {
					continue;
				}

				break;
			}

			final char chr = (char)value;
			if (chr == '\n') {
				break;
			}

			buffer.append(chr);
		}

		return buffer.toString();
	}
}
