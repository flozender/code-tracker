//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

import static org.hamcrest.Matchers.*;
import org.junit.Assert;
import org.junit.Test;

public class CommandLineBuilderTest
{
    @Test
    public void testSimpleCommandline()
    {
        CommandLineBuilder cmd = new CommandLineBuilder("java");
        cmd.addEqualsArg("-Djava.io.tmpdir","/home/java/temp dir/");
        cmd.addArg("--version");
        
        Assert.assertThat(cmd.toString(), is("java -Djava.io.tmpdir=/home/java/temp\\ dir/ --version"));
    }
    
    @Test
    public void testQuotingSimple()
    {
        assertQuoting("/opt/jetty","/opt/jetty");
    }

    @Test
    public void testQuotingSpaceInPath()
    {
        assertQuoting("/opt/jetty 7/home","/opt/jetty\\ 7/home");
    }

    @Test
    public void testQuotingSpaceAndQuotesInPath()
    {
        assertQuoting("/opt/jetty 7 \"special\"/home","/opt/jetty\\ 7\\ \\\"special\\\"/home");
    }

    private void assertQuoting(String raw, String expected)
    {
        String actual = CommandLineBuilder.quote(raw);
        Assert.assertThat("Quoted version of [" + raw + "]",actual,is(expected));
    }
}
