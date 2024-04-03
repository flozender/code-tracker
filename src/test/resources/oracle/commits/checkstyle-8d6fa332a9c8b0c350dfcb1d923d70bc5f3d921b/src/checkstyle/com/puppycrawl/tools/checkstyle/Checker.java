////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001  Oliver Burn
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.regexp.RESyntaxException;

/**
 * This class provides the functionality to check a file.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 * @author <a href="mailto:stephane.bailliez@wanadoo.fr">Stephane Bailliez</a>
 */
public class Checker
    implements Defn
{
    /** cache file **/
    private final PropertyCacheFile mCache;

    /** vector of listeners */
    private final ArrayList mListeners = new ArrayList();

    /**
     * Constructs the object.
     * @param aConfig contains the configuration to check with
     * @param aLog the PrintStream to log messages to
     * @throws RESyntaxException unable to create a regexp object
     **/
    public Checker(Configuration aConfig, PrintStream aLog)
        throws RESyntaxException
    {
        mCache = new PropertyCacheFile(aConfig.getCacheFile(), aLog);
        final Verifier v = new Verifier(aConfig);
        VerifierSingleton.setInstance(v);
    }

    /** Cleans up the object **/
    public void destroy()
    {
        mCache.destroy();

        // close all streamable listeners
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final Object obj = it.next();
            if (obj instanceof Streamable) {
                final Streamable str = (Streamable) obj;
                final OutputStream os = str.getOutputStream();
                // close only those that can be closed...
                if ((os != System.out) && (os != System.err) && (os != null)) {
                    try  {
                        os.flush();
                        os.close();
                    }
                    catch (IOException ignored) {
                    }
                }
            }
            it.remove();
        }
    }

    /**
     * Add the listener that will be used to receive events from the audit
     * @param aListener the nosy thing
     */
    public void addListener(AuditListener aListener)
    {
        mListeners.add(aListener);
    }

    /**
     * Processes a set of files.
     * Once this is done, it is highly recommended to call for
     * the destroy method to close and remove the listeners.
     * @param aFiles the list of files to be audited.
     * @return the total number of errors found
     * @see destroy()
     */
    public int process(String[] aFiles)
    {
        int total = 0;
        fireAuditStarted();
        for (int i = 0; i < aFiles.length; i++) {
            total += process(aFiles[i]);
        }
        fireAuditFinished();
        return total;
    }

    /**
     * Processes a specified file and prints out all errors found.
     * @return the number of errors found
     * @param aFileName the name of the file to process
     **/
    private int process(String aFileName)
    {
        final File f = new File(aFileName);
        final long timestamp = f.lastModified();
        if (mCache.alreadyChecked(aFileName, timestamp)) {
            return 0;
        }

        LineText[] errors;
        try {
            fireFileStarted(aFileName);
            final String[] lines = getLines(aFileName);
            VerifierSingleton.getInstance().clearMessages();
            VerifierSingleton.getInstance().setLines(lines);
            final Reader sar = new StringArrayReader(lines);
            final GeneratedJavaLexer jl = new GeneratedJavaLexer(sar);
            jl.setFilename(aFileName);
            final GeneratedJavaRecognizer jr = new GeneratedJavaRecognizer(jl);
            jr.setFilename(aFileName);
            jr.setASTNodeClass(MyCommonAST.class.getName());
            jr.compilationUnit();
            errors = VerifierSingleton.getInstance().getMessages();
        }
        catch (FileNotFoundException fnfe) {
            errors = new LineText[] {new LineText(0, "File not found!")};
        }
        catch (IOException ioe) {
            errors = new LineText[] {
                new LineText(0, "Got an IOException -" + ioe.getMessage())};
        }
        catch (RecognitionException re) {
            errors = new LineText[] {
                new LineText(0,
                             "Got a RecognitionException -" + re.getMessage())};
        }
        catch (TokenStreamException te) {
            errors = new LineText[] {
                new LineText(0,
                             "Got a TokenStreamException -" + te.getMessage())};
        }

        if (errors.length == 0) {
            mCache.checkedOk(aFileName, timestamp);
        }
        else {
            fireErrors(aFileName, errors);
        }

        fireFileFinished(aFileName);
        return errors.length;
    }

    /**
     * Loads the contents of a file in a String array.
     * @return the lines in the file
     * @param aFileName the name of the file to load
     * @throws IOException error occurred
     **/
    private String[] getLines(String aFileName)
        throws IOException
    {
        final LineNumberReader lnr =
            new LineNumberReader(new FileReader(aFileName));
        final ArrayList lines = new ArrayList();
        while (true) {
            final String l = lnr.readLine();
            if (l == null) {
                break;
            }
            lines.add(l);
        }

        return (String[]) lines.toArray(new String[0]);
    }

    /** notify all listeners about the audit start */
    protected void fireAuditStarted()
    {
        final AuditEvent evt = new AuditEvent(this);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.auditStarted(evt);
        }
    }

    /** notify all listeners about the audit end */
    protected void fireAuditFinished()
    {
        final AuditEvent evt = new AuditEvent(this);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.auditFinished(evt);
        }
    }

    /**
     * notify all listeners about the beginning of a file audit
     * @param aFileName the file to be audited
     */
    protected void fireFileStarted(String aFileName)
    {
        final AuditEvent evt = new AuditEvent(this, aFileName);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.fileStarted(evt);
        }
    }

    /**
     * notify all listeners about the end of a file audit
     * @param aFileName the audited file
     */
    protected void fireFileFinished(String aFileName)
    {
        final AuditEvent evt = new AuditEvent(this, aFileName);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.fileFinished(evt);
        }
    }

    /**
     * notify all listeners about the errors in a file.
     * @param aFileName the audited file
     * @param aErrors the audit errors from the file
     */
    protected void fireErrors(String aFileName, LineText[] aErrors)
    {
        for (int i = 0; i < aErrors.length; i++) {
            final AuditEvent evt =
                new AuditEvent(this, aFileName, aErrors[i].getLineNo(),
                               aErrors[i].getText());
            final Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                final AuditListener listener = (AuditListener) it.next();
                listener.addError(evt);
            }
        }
    }
}
