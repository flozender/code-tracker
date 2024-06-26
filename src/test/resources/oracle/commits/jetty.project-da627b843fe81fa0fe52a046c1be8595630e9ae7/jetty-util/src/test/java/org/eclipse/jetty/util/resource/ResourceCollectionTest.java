// ========================================================================
// Copyright (c) 2007-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.util.resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

public class ResourceCollectionTest extends TestCase
{
    
    public void testMutlipleSources1() throws Exception
    {
        ResourceCollection rc1 = new ResourceCollection(new String[]{
                "src/test/resources/org/eclipse/jetty/util/resource/one/",
                "src/test/resources/org/eclipse/jetty/util/resource/two/",
                "src/test/resources/org/eclipse/jetty/util/resource/three/"
        });
        assertEquals("1 - one", getContent(rc1, "1.txt"));
        assertEquals("2 - two", getContent(rc1, "2.txt"));
        assertEquals("3 - three", getContent(rc1, "3.txt"));        
        
        
        ResourceCollection rc2 = new ResourceCollection(
                "src/test/resources/org/eclipse/jetty/util/resource/one/," +
                "src/test/resources/org/eclipse/jetty/util/resource/two/," +
                "src/test/resources/org/eclipse/jetty/util/resource/three/"
        );
        assertEquals("1 - one", getContent(rc2, "1.txt"));
        assertEquals("2 - two", getContent(rc2, "2.txt"));
        assertEquals("3 - three", getContent(rc2, "3.txt"));
        
        for(String s : rc1.list())
            System.err.println(s);        
    }
    
    public void testMergedDir() throws Exception
    {
        ResourceCollection rc = new ResourceCollection(new String[]{
                "src/test/resources/org/eclipse/jetty/util/resource/one/",
                "src/test/resources/org/eclipse/jetty/util/resource/two/",
                "src/test/resources/org/eclipse/jetty/util/resource/three/"
        });
        
        Resource r = rc.addPath("dir");
        assertTrue(r instanceof ResourceCollection);
        rc=(ResourceCollection)r;
        assertEquals("1 - one", getContent(rc, "1.txt"));
        assertEquals("2 - two", getContent(rc, "2.txt"));
        assertEquals("3 - three", getContent(rc, "3.txt"));  
    }
    
    static String getContent(ResourceCollection rc, String path) throws Exception
    {
        StringBuilder buffer = new StringBuilder();
        String line = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(rc.addPath(path).getURL().openStream()));
        while((line=br.readLine())!=null)
            buffer.append(line);
        br.close();        
        return buffer.toString();
    }
    
}
