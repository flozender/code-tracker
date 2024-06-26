// ========================================================================
// Copyright (c) Webtide LLC
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
//
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
//
// The Apache License v2.0 is available at
// http://www.apache.org/licenses/LICENSE-2.0.txt
//
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================
package org.eclipse.jetty.deploy.bindings;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.graph.Node;
import org.eclipse.jetty.util.log.Log;

public class DebugBinding implements AppLifeCycle.Binding
{
    final String[] _targets;
    
    public DebugBinding(String target)
    {
        _targets=new String[]{target};
    }
    
    public DebugBinding(final String... targets)
    {
        _targets=targets;
    }
    
    public String[] getBindingTargets()
    {
        return _targets;
    }

    public void processBinding(Node node, App app) throws Exception
    {
        Log.info("processBinding {} {}",node,app.getContextHandler());
    }
}
