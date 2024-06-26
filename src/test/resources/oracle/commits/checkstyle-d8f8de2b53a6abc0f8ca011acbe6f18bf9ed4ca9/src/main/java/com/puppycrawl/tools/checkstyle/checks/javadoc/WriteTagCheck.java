////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
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
package com.puppycrawl.tools.checkstyle.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.Utils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.beanutils.ConversionException;

/**
 * <p>
 * Outputs a JavaDoc tag as information. Can be used e.g. with the stylesheets
 * that sort the report by author name.
 * To define the format for a tag, set property tagFormat to a
 * regular expression.
 * This check uses two different severity levels. The normal one is used for
 * reporting when the tag is missing. The additional one (tagSeverity) is used
 * for the level of reporting when the tag exists. The default value for
 * tagSeverity is info.
 * </p>
 * <p> An example of how to configure the check for printing author name is:
 *</p>
 * <pre>
 * &lt;module name="WriteTag"&gt;
 *    &lt;property name="tag" value="@author"/&gt;
 *    &lt;property name="tagFormat" value="\S"/&gt;
 * &lt;/module&gt;
 * </pre>
 * <p> An example of how to configure the check to print warnings if an
 * "@incomplete" tag is found, and not print anything if it is not found:
 *</p>
 * <pre>
 * &lt;module name="WriteTag"&gt;
 *    &lt;property name="tag" value="@incomplete"/&gt;
 *    &lt;property name="tagFormat" value="\S"/&gt;
 *    &lt;property name="severity" value="ignore"/&gt;
 *    &lt;property name="tagSeverity" value="warning"/&gt;
 * &lt;/module&gt;
 * </pre>
 *
 * @author Daniel Grenner
 * @version 1.0
 */
public class WriteTagCheck
    extends Check
{
    /** compiled regexp to match tag **/
    private Pattern tagRE;
    /** compiled regexp to match tag content **/
    private Pattern tagFormatRE;

    /** regexp to match tag */
    private String tag;
    /** regexp to match tag content */
    private String tagFormat;
    /** the severity level of found tag reports */
    private SeverityLevel tagSeverityLevel = SeverityLevel.INFO;

    /**
     * Sets the tag to check.
     * @param tag tag to check
     * @throws ConversionException If the tag is not a valid regular exception.
     */
    public void setTag(String tag)
        throws ConversionException
    {
        try {
            this.tag = tag;
            tagRE = Utils.getPattern(tag + "\\s*(.*$)");
        }
        catch (final PatternSyntaxException e) {
            throw new ConversionException("unable to parse " + tag, e);
        }
    }

    /**
     * Set the tag format.
     * @param format a <code>String</code> value
     * @throws ConversionException unable to parse format
     */
    public void setTagFormat(String format)
        throws ConversionException
    {
        try {
            tagFormat = format;
            tagFormatRE = Utils.getPattern(format);
        }
        catch (final PatternSyntaxException e) {
            throw new ConversionException("unable to parse " + format, e);
        }
    }

    /**
     * Sets the tag severity level.  The string should be one of the names
     * defined in the <code>SeverityLevel</code> class.
     *
     * @param severity  The new severity level
     * @see SeverityLevel
     */
    public final void setTagSeverity(String severity)
    {
        tagSeverityLevel = SeverityLevel.getInstance(severity);
    }

    @Override
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.INTERFACE_DEF,
                          TokenTypes.CLASS_DEF,
                          TokenTypes.ENUM_DEF,
                          TokenTypes.ANNOTATION_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens()
    {
        return new int[] {TokenTypes.INTERFACE_DEF,
                          TokenTypes.CLASS_DEF,
                          TokenTypes.ENUM_DEF,
                          TokenTypes.ANNOTATION_DEF,
                          TokenTypes.METHOD_DEF,
                          TokenTypes.CTOR_DEF,
                          TokenTypes.ENUM_CONSTANT_DEF,
                          TokenTypes.ANNOTATION_FIELD_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST ast)
    {
        final FileContents contents = getFileContents();
        final int lineNo = ast.getLineNo();
        final TextBlock cmt =
            contents.getJavadocBefore(lineNo);
        if (cmt == null) {
            log(lineNo, "type.missingTag", tag);
        }
        else {
            checkTag(lineNo, cmt.getText(), tag, tagRE, tagFormatRE,
                tagFormat);
        }
    }

    /**
     * Verifies that a type definition has a required tag.
     * @param lineNo the line number for the type definition.
     * @param comment the Javadoc comment for the type definition.
     * @param tag the required tag name.
     * @param tagRE regexp for the full tag.
     * @param formatRE regexp for the tag value.
     * @param format pattern for the tag value.
     */
    private void checkTag(
            int lineNo,
            String[] comment,
            String tag,
            Pattern tagRE,
            Pattern formatRE,
            String format)
    {
        if (tagRE == null) {
            return;
        }

        int tagCount = 0;
        for (int i = 0; i < comment.length; i++) {
            final String s = comment[i];
            final Matcher matcher = tagRE.matcher(s);
            if (matcher.find()) {
                tagCount += 1;
                final int contentStart = matcher.start(1);
                final String content = s.substring(contentStart);
                if ((formatRE != null) && !formatRE.matcher(content).find()) {
                    log(lineNo + i - comment.length, "type.tagFormat", tag,
                        format);
                }
                else {
                    logTag(lineNo + i - comment.length, tag, content);
                }

            }
        }
        if (tagCount == 0) {
            log(lineNo, "type.missingTag", tag);
        }

    }


    /**
     * Log a message.
     *
     * @param line the line number where the error was found
     * @param tag the javadoc tag to be logged
     * @param tagValue the contents of the tag
     *
     * @see java.text.MessageFormat
     */
    protected final void logTag(int line, String tag, String tagValue)
    {
        final String originalSeverity = getSeverity();
        setSeverity(tagSeverityLevel.getName());

        log(line, "javadoc.writeTag", tag, tagValue);

        setSeverity(originalSeverity);
    }
}
