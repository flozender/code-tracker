// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.webapp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.xml.XmlParser;

/**
 * IterativeDescriptorProcessor
 *
 *
 */
public abstract class IterativeDescriptorProcessor implements DescriptorProcessor
{
    public static final Class[] __signature = new Class[]{Descriptor.class, XmlParser.Node.class};
    protected Map<String, Method> _visitors = new HashMap<String, Method>();
    public abstract void start(Descriptor descriptor);
    public abstract void end(Descriptor descriptor);

    /**
     * Register a method to be called back when visiting the node with the given name.
     * The method must exist on a subclass of this class, and must have the signature:
     * public void method (Descriptor descriptor, XmlParser.Node node)
     * @param nodeName
     * @param m
     */
    public void registerVisitor(String nodeName, Method m)
    {
        _visitors.put(nodeName, m);
    }

    
    /** 
     * @see org.eclipse.jetty.webapp.DescriptorProcessor#process(org.eclipse.jetty.webapp.Descriptor)
     */
    public void process(Descriptor descriptor)
    throws Exception
    {
        if (descriptor == null)
            return;

        start(descriptor);

        XmlParser.Node root = descriptor.getRoot();
        Iterator iter = root.iterator();
        XmlParser.Node node = null;
        while (iter.hasNext())
        {
            Object o = iter.next();
            if (!(o instanceof XmlParser.Node)) continue;
            node = (XmlParser.Node) o;
            visit(descriptor, node);
        }

        end(descriptor);
    }


    protected void visit (final Descriptor descriptor, final XmlParser.Node node)
    throws Exception
    {
        String name = node.getTag();
        Method m =  _visitors.get(name);
        if (m != null)
            m.invoke(this, new Object[]{descriptor, node});
    }
}
