//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simple common abstraction for Text files, that consist of a series of lines.
 * <p>
 * Ignoring lines that are empty, deemed to be comments, or are duplicates of prior lines.
 */
public class TextFile implements Iterable<String>
{
    private final File file;
    private final List<String> lines = new ArrayList<>();

    public TextFile(File file) throws FileNotFoundException, IOException
    {
        this.file = file;
        init();
        
        try (FileReader reader = new FileReader(file))
        {
            try (BufferedReader buf = new BufferedReader(reader))
            {
                String line;
                while ((line = buf.readLine()) != null)
                {
                    if (line.length() == 0)
                    {
                        continue;
                    }

                    if (line.charAt(0) == '#')
                    {
                        continue;
                    }

                    process(line.trim());
                }
            }
        }
    }

    public void init()
    {
    }

    public void addUniqueLine(String line)
    {
        if (lines.contains(line))
        {
            // skip
            return;
        }
        lines.add(line);
    }

    public File getFile()
    {
        return file;
    }

    public List<String> getLineMatches(Pattern pattern)
    {
        List<String> ret = new ArrayList<>();
        for (String line : lines)
        {
            if (pattern.matcher(line).matches())
            {
                ret.add(line);
            }
        }
        return ret;
    }

    public List<String> getLines()
    {
        return lines;
    }

    @Override
    public Iterator<String> iterator()
    {
        return lines.iterator();
    }

    public void process(String line)
    {
        addUniqueLine(line);
    }
}
