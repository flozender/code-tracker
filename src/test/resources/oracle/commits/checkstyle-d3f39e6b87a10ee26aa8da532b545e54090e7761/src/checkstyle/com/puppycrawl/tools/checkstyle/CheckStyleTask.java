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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.regexp.RESyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An implementation of a ANT task for calling checkstyle. See the documentation
 * of the task for usage.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 **/
public class CheckStyleTask
    extends Task
{
    /** name of file to check **/
    private String mFileName;
    /** contains the filesets to process **/
    private final List mFileSets = new ArrayList();
    /** the configuration to pass to the checker **/
    private final Configuration mConfig = new Configuration();

    ////////////////////////////////////////////////////////////////////////////
    // Setters for attributes
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Adds a set of files (nested fileset attribute).
     * @param aFS the file set to add
     */
    public void addFileset(FileSet aFS)
    {
        mFileSets.add(aFS);
    }

    /** @param aFile the file to be checked **/
    public void setFile(File aFile)
    {
        mFileName = aFile.getAbsolutePath();
    }

    /** @param aAllowed whether tabs are allowed **/
    public void setAllowTabs(boolean aAllowed)
    {
        mConfig.setAllowTabs(aAllowed);
    }

    /** @param aAllowed whether protected data is allowed **/
    public void setAllowProtected(boolean aAllowed)
    {
        mConfig.setAllowProtected(aAllowed);
    }

    /** @param aAllowed whether allow having no author **/
    public void setAllowNoAuthor(boolean aAllowed)
    {
        mConfig.setAllowNoAuthor(aAllowed);
    }

    /** @param aLen max allowed line length **/
    public void setMaxLineLen(int aLen)
    {
        mConfig.setMaxLineLength(aLen);
    }

    /** @param aPat pattern for member variables **/
    public void setMemberPattern(String aPat)
    {
        try {
            mConfig.setMemberPat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse memberpattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aPat pattern for public member variables **/
    public void setPublicMemberPattern(String aPat)
    {
        try {
            mConfig.setPublicMemberPat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse publicmemberpattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aPat pattern for parameters **/
    public void setParamPattern(String aPat)
    {
        try {
            mConfig.setParamPat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse parampattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aPat pattern for constant variables **/
    public void setConstPattern(String aPat)
    {
        try {
            mConfig.setStaticFinalPat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse constpattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aPat pattern for static variables **/
    public void setStaticPattern(String aPat)
    {
        try {
            mConfig.setStaticPat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse staticpattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aPat pattern for type names **/
    public void setTypePattern(String aPat)
    {
        try {
            mConfig.setTypePat(aPat);
        }
        catch (RESyntaxException ex) {
            throw new BuildException("Unable to parse typepattern - " +
                                     ex.getMessage());
        }
    }

    /** @param aName header file name **/
    public void setHeaderFile(File aName)
    {
        try {
            mConfig.setHeaderFile(aName.getAbsolutePath());
        }
        catch (IOException ex) {
            throw new BuildException("Unable to read headerfile - " +
                                     ex.getMessage());
        }
    }

    /** @param aNum **/
    public void setHeaderIgnoreLine(int aNum)
    {
        mConfig.setHeaderIgnoreLineNo(aNum);
    }

    /** @param aRelax whether to be relaxed on Javadoc **/
    public void setRelaxJavadoc(boolean aRelax)
    {
        mConfig.setRelaxJavadoc(aRelax);
    }

    /** @param aIgnore whether to ignore import statements **/
    public void setIgnoreImports(boolean aIgnore)
    {
        mConfig.setIgnoreImports(aIgnore);
    }

    /** @param aIgnore whether to ignore whitespace **/
    public void setIgnoreWhitespace(boolean aIgnore)
    {
        mConfig.setIgnoreWhitespace(aIgnore);
    }

    /** @param aIgnore whether to ignore braces **/
    public void setIgnoreBraces(boolean aIgnore)
    {
        mConfig.setIgnoreBraces(aIgnore);
    }

    ////////////////////////////////////////////////////////////////////////////
    // The doers
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Actually checks the files specified. All errors are reported to
     * System.out. Will fail if any errors occurred.
     * @throws BuildException an error occurred
     **/
    public void execute()
        throws BuildException
    {
        // Check for no arguments
        if ((mFileName == null) && (mFileSets.size() == 0)) {
            throw new BuildException("Must specify atleast one of 'file' " +
                                     "or nested 'fileset'.", location);
        }

        // Create the checker
        Checker c;
        try {
            c = new Checker(mConfig, System.out);
        }
        catch (RESyntaxException e){
            e.printStackTrace();
            throw new BuildException("Unable to create a Checker", location);
        }

        // Process the files
        int numErrs = 0;
        if (mFileName != null) {
            numErrs += c.process(mFileName);
        }

        final Iterator it = mFileSets.iterator();
        while (it.hasNext()) {
            final FileSet fs = (FileSet) it.next();
            final DirectoryScanner ds = fs.getDirectoryScanner(project);
            numErrs += process(fs.getDir(project).getAbsolutePath(),
                               ds.getIncludedFiles(),
                               c);
        }

        if (numErrs > 0) {
            throw new BuildException("Got " + numErrs + " errors.", location);
        }
    }

    /**
     * Processes the list of files.
     * @return the number of errors found
     * @param aDir absolute path to directory containing files
     * @param aFiles the files to process
     * @param aChecker the checker to process the files with
     **/
    private int process(String aDir, String[] aFiles, Checker aChecker)
    {
        int retVal = 0;
        for (int i = 0; i < aFiles.length; i++) {
            retVal += aChecker.process(aDir + File.separator + aFiles[i]);
        }
        return retVal;
    }
}
