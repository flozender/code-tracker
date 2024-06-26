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

package org.eclipse.jetty.plus.annotation;

import javax.servlet.ServletException;

import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * RunAs
 * <p/>
 * Represents a &lt;run-as&gt; element in web.xml, or a runAs annotation.
 */
public class RunAs
{
    private Class _targetClass;
    private String _roleName;

    public RunAs()
    {}


    public void setTargetClass (Class clazz)
    {
        _targetClass=clazz;
    }

    public Class getTargetClass ()
    {
        return _targetClass;
    }

    public void setRoleName (String roleName)
    {
        _roleName = roleName;
    }

    public String getRoleName ()
    {
        return _roleName;
    }


    public void setRunAs (ServletHolder holder, SecurityHandler securityHandler)
    throws ServletException
    {
        if (holder == null)
            return;
        String className = getServletClassNameForHolder(holder);

        if (className.equals(_targetClass.getName()))
            holder.setRunAsRole(_roleName);
    }

    public static String getServletClassNameForHolder (ServletHolder holder)
    throws ServletException
    {
        if (PojoServlet.class.getName().equals(holder.getClassName()))
            return ((PojoWrapper)holder.getServlet()).getPojo().getClass().getName();
        return holder.getClassName();
    }

}
