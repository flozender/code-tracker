////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.regexp.RESyntaxException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

// TODO: Fix the loader so it doesn't validate the document
/**
 * Describe class <code>ConfigurationLoader</code> here.
 *
 * @author <a href="mailto:checkstyle@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
class ConfigurationLoader
    extends DefaultHandler
{
    /** overriding properties **/
    private Properties mOverrideProps = new Properties();
    /** parser to read XML files **/
    private final XMLReader mParser;
    /** the loaded global properties **/
    private final Properties mProps = new Properties();
    /** the loaded configurations **/
    private final ArrayList mCheckConfigs = new ArrayList();
    /** the loaded configuration **/
    private Configuration mConfig = null;
    /** the current check configuration being created **/
    private CheckConfiguration mCurrent;
    /** buffer for collecting text **/
    private final StringBuffer mBuf = new StringBuffer();
    /** in global element **/
    private boolean mIsInGlobalElement = false;
    /** started processing check configurations **/
    private boolean mIsInCheckMode = false;

    /**
     * Creates a new <code>ConfigurationLoader</code> instance.
     * @throws ParserConfigurationException if an error occurs
     * @throws SAXException if an error occurs
     */
    private ConfigurationLoader()
        throws ParserConfigurationException, SAXException
    {
        mParser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        mParser.setContentHandler(this);
    }

    /**
     * Parses the specified file loading the configuration information.
     * @param aFilename the file to parse
     * @throws FileNotFoundException if an error occurs
     * @throws IOException if an error occurs
     * @throws SAXException if an error occurs
     */
    void parseFile(String aFilename)
        throws FileNotFoundException, IOException, SAXException
    {
        mParser.parse(new InputSource(new FileReader(aFilename)));
    }

    /**
     * Returns the configuration information in the last file parsed.
     * @return list of CheckConfiguration objects
     */
    CheckConfiguration[] getConfigs()
    {
        return (CheckConfiguration[]) mCheckConfigs.toArray(
            new CheckConfiguration[mCheckConfigs.size()]);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Document handler methods
    ///////////////////////////////////////////////////////////////////////////

    /** @see org.xml.sax.helpers.DefaultHandler **/
    public void characters(char[] aChars, int aStart, int aLength)
    {
        mBuf.append(String.valueOf(aChars, aStart, aLength));
    }

    /** @see org.xml.sax.helpers.DefaultHandler **/
    public void startElement(String aNamespaceURI,
                             String aLocalName,
                             String aQName,
                             Attributes aAtts)
    {
        mBuf.setLength(0);
        if ("global".equals(aQName)) {
            mIsInGlobalElement = true;
        }
        else if ("check".equals(aQName)) {
            //first apply overriding properties
            if (!mIsInCheckMode) {
                mIsInCheckMode = true;
                for (Enumeration enum = mOverrideProps.keys();
                      enum.hasMoreElements();)
                {
                    final String key = (String) enum.nextElement();
                    final String value = (String) mOverrideProps.get(key);
                    mProps.setProperty(key, value);
                }
            }

            mCurrent = new CheckConfiguration();
            mCurrent.setClassname(aAtts.getValue("classname"));
        }
        else if ("property".equals(aQName)) {
            final String name = aAtts.getValue("name");
            String value = aAtts.getValue("value");

            if (value == null) {
                //try global
                String globalKey = aAtts.getValue("from-global");
                value = (String) mProps.get(globalKey);
            }

            if (mIsInGlobalElement) {
                mProps.setProperty(name, value);
            }
            else {
                mCurrent.addProperty(name, value);
            }
        }

    }

    /** @see org.xml.sax.helpers.DefaultHandler **/
    public void endElement(String aNamespaceURI,
                           String aLocalName,
                           String aQName)
    {
        if ("global".equals(aQName)) {
            mIsInGlobalElement = false;
        }
        else if ("check".equals(aQName)) {
            mCheckConfigs.add(mCurrent);
            mCurrent = null;
        }
        else if ("tokens".equals(aQName)) {
            mCurrent.addTokens(mBuf.toString());
        }
    }

    /**
     * Returns the check configurations in a specified file.
     * @param aConfigFname name of config file
     * @return the check configurations
     * @throws CheckstyleException if an error occurs
     */
    public static CheckConfiguration[] loadConfigs(String aConfigFname)
        throws CheckstyleException
    {
        try {
            final ConfigurationLoader loader = new ConfigurationLoader();
            loader.parseFile(aConfigFname);
            return loader.getConfigs();
        }
        catch (FileNotFoundException e) {
            throw new CheckstyleException("unable to find " + aConfigFname);
        }
        catch (ParserConfigurationException e) {
            throw new CheckstyleException("unable to parse " + aConfigFname);
        }
        catch (SAXException e) {
            throw new CheckstyleException("unable to parse " + aConfigFname);
        }
        catch (IOException e) {
            throw new CheckstyleException("unable to read " + aConfigFname);
        }
    }

    /**
     * Returns the check configurations in a specified file.
     * @param aConfigFname name of config file
     * @param aOverrideProps overriding properties
     * @return the check configurations
     * @throws CheckstyleException if an error occurs
     */
    public static Configuration loadConfiguration(String aConfigFname,
                                                   Properties aOverrideProps)
        throws CheckstyleException
    {
        try {
            final ConfigurationLoader loader = new ConfigurationLoader();
            loader.mOverrideProps = aOverrideProps;
            loader.parseFile(aConfigFname);
            return loader.getConfiguration();
        }
        catch (FileNotFoundException e) {
            throw new CheckstyleException("unable to find " + aConfigFname);
        }
        catch (ParserConfigurationException e) {
            throw new CheckstyleException("unable to parse " + aConfigFname);
        }
        catch (SAXException e) {
            throw new CheckstyleException("unable to parse " + aConfigFname);
        }
        catch (IOException e) {
            throw new CheckstyleException("unable to read " + aConfigFname);
        }
        catch (RESyntaxException e) {
            throw new CheckstyleException(
                "A regular expression error exists in " + aConfigFname);
        }
    }

    /**
     * Returns the configuration in the last file parsed.
     * @return Configuration object
     * @throws RESyntaxException if an error occurs
     * @throws FileNotFoundException if an error occurs
     * @throws IOException if an error occurs
     */
    private Configuration getConfiguration()
        throws IOException, FileNotFoundException, RESyntaxException
    {
        final GlobalProperties globalProps =
            new GlobalProperties(mProps, System.out);
        final CheckConfiguration[] checkConfigs =
            (CheckConfiguration[]) mCheckConfigs.toArray(
                new CheckConfiguration[mCheckConfigs.size()]);
        return new Configuration(globalProps, checkConfigs);
    }
}
