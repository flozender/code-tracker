package org.apache.commons.io;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Turbine" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.File;
import java.io.FilenameFilter;

/**
 * Accepts a selection if it is acceptable to both of two {@link FilenameFilter}s.
 * This takes two {@link FilenameFilter}s as input.
 *
 * <p>Eg., to print all files beginning with <code>A</code> and ending with <code>.java</code>:</p>
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( new AndFileFilter(
 *         new PrefixFileFilter("A"),
 *         new ExtensionFileFilter(".java")
 *         )
 *     );
 * for ( int i=0; i&lt;files.length; i++ )
 * {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 * @version $Revision: 1.1 $ $Date: 2002/07/08 22:14:46 $
 * @since 4.0
 */
public class AndFileFilter
    implements FilenameFilter
{
    private final FilenameFilter m_filter1;
    private final FilenameFilter m_filter2;

    public AndFileFilter( final FilenameFilter filter1, final FilenameFilter filter2 )
    {
        m_filter1 = filter1;
        m_filter2 = filter2;
    }

    public boolean accept( final File file, final String name )
    {
        return m_filter1.accept( file, name ) && m_filter2.accept( file, name );
    }
}


