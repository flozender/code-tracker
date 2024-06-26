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
package com.puppycrawl.tools.checkstyle;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * Simple XML logger.
 * It outputs everything in UTF-8 (default XML encoding is UTF-8) in case
 * we want to localize error messages or simply that filenames are
 * localized and takes care about escaping as well.

 * @author <a href="mailto:stephane.bailliez@wanadoo.fr">Stephane Bailliez</a>
 */
public class XMLLogger
    extends AutomaticBean
    implements AuditListener
{
    /** decimal radix */
    private static final int BASE_10 = 10;

    /** hex radix */
    private static final int BASE_16 = 16;

    /** close output stream in auditFinished */
    private boolean closeStream;

    /** helper writer that allows easy encoding and printing */
    private PrintWriter writer;

    /** some known entities to detect */
    private static final String[] ENTITIES = {"gt", "amp", "lt", "apos",
                                              "quot", };

    /**
     * Creates a new <code>XMLLogger</code> instance.
     * Sets the output to a defined stream.
     * @param os the stream to write logs to.
     * @param closeStream close oS in auditFinished
     */
    public XMLLogger(OutputStream os, boolean closeStream)
    {
        setOutputStream(os);
        this.closeStream = closeStream;
    }

    /**
     * sets the OutputStream
     * @param oS the OutputStream to use
     **/
    private void setOutputStream(OutputStream oS)
    {
        try {
            final OutputStreamWriter osw = new OutputStreamWriter(oS, "UTF-8");
            writer = new PrintWriter(osw);
        }
        catch (final UnsupportedEncodingException e) {
            // unlikely to happen...
            throw new ExceptionInInitializerError(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void auditStarted(AuditEvent evt)
    {
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        final ResourceBundle compilationProperties =
            ResourceBundle.getBundle("checkstylecompilation");
        final String version =
            compilationProperties.getString("checkstyle.compile.version");

        writer.println("<checkstyle version=\"" + version + "\">");
    }

    /** {@inheritDoc} */
    @Override
    public void auditFinished(AuditEvent evt)
    {
        writer.println("</checkstyle>");
        if (closeStream) {
            writer.close();
        }
        else {
            writer.flush();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fileStarted(AuditEvent evt)
    {
        writer.println("<file name=\"" + encode(evt.getFileName()) + "\">");
    }

    /** {@inheritDoc} */
    @Override
    public void fileFinished(AuditEvent evt)
    {
        writer.println("</file>");
    }

    /** {@inheritDoc} */
    @Override
    public void addError(AuditEvent evt)
    {
        if (SeverityLevel.IGNORE != evt.getSeverityLevel()) {
            writer.print("<error" + " line=\"" + evt.getLine() + "\"");
            if (evt.getColumn() > 0) {
                writer.print(" column=\"" + evt.getColumn() + "\"");
            }
            writer.print(" severity=\""
                + evt.getSeverityLevel().getName()
                + "\"");
            writer.print(" message=\""
                + encode(evt.getMessage())
                + "\"");
            writer.println(" source=\""
                + encode(evt.getSourceName())
                + "\"/>");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addException(AuditEvent evt, Throwable throwable)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        pw.println("<exception>");
        pw.println("<![CDATA[");
        throwable.printStackTrace(pw);
        pw.println("]]>");
        pw.println("</exception>");
        pw.flush();
        writer.println(encode(sw.toString()));
    }

    /**
     * Escape &lt;, &gt; &amp; &#39; and &quot; as their entities.
     * @param value the value to escape.
     * @return the escaped value if necessary.
     */
    public String encode(String value)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                case '\"':
                    sb.append("&quot;");
                    break;
                case '&':
                    final int nextSemi = value.indexOf(';', i);
                    if (nextSemi < 0
                        || !isReference(value.substring(i, nextSemi + 1)))
                    {
                        sb.append("&amp;");
                    }
                    else {
                        sb.append('&');
                    }
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * @param ent the possible entity to look for.
     * @return whether the given argument a character or entity reference
     */
    public boolean isReference(String ent)
    {
        if (!(ent.charAt(0) == '&') || !ent.endsWith(";")) {
            return false;
        }

        if (ent.charAt(1) == '#') {
            int prefixLength = 2; // "&#"
            int radix = BASE_10;
            if (ent.charAt(2) == 'x') {
                prefixLength++;
                radix = BASE_16;
            }
            try {
                Integer.parseInt(
                    ent.substring(prefixLength, ent.length() - 1), radix);
                return true;
            }
            catch (final NumberFormatException nfe) {
                return false;
            }
        }

        final String name = ent.substring(1, ent.length() - 1);
        for (String element : ENTITIES) {
            if (name.equals(element)) {
                return true;
            }
        }
        return false;
    }
}
