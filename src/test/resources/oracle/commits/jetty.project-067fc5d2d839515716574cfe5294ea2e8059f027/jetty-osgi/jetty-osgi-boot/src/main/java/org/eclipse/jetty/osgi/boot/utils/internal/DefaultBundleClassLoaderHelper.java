//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.osgi.boot.utils.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jetty.osgi.boot.utils.BundleClassLoaderHelper;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.osgi.framework.Bundle;

/**
 * DefaultBundleClassLoaderHelper
 * 
 * 
 * Default implementation of the BundleClassLoaderHelper. Uses introspection to
 * support equinox-3.5 and felix-2.0.0
 */
public class DefaultBundleClassLoaderHelper implements BundleClassLoaderHelper
{
    private static final Logger LOG = Log.getLogger(BundleClassLoaderHelper.class);
    private static enum OSGiContainerType {EquinoxOld, EquinoxLuna, FelixOld, Felix403};
    private static OSGiContainerType osgiContainer;
    private static Class Equinox_BundleHost_Class;
    private static Class Equinox_EquinoxBundle_Class;
    private static Class Felix_BundleImpl_Class;
    private static Class Felix_BundleWiring_Class;
    //old equinox
    private static Method Equinox_BundleHost_getBundleLoader_method;
    private static Method Equinox_BundleLoader_createClassLoader_method;
    //new equinox
    private static Method Equinox_EquinoxBundle_getModuleClassLoader_Method;
  
    //new felix
    private static Method Felix_BundleImpl_Adapt_Method;
    //old felix
    private static Field Felix_BundleImpl_m_Modules_Field;
    private static Field Felix_ModuleImpl_m_ClassLoader_Field;
    private static Method Felix_BundleWiring_getClassLoader_Method;
    
    
    private static void checkContainerType (Bundle bundle)
    {
        if (osgiContainer != null)
            return;
        
        try
        {
            Equinox_BundleHost_Class = bundle.getClass().getClassLoader().loadClass("org.eclipse.osgi.framework.internal.core.BundleHost");
            osgiContainer = OSGiContainerType.EquinoxOld;
            return;
        }
        catch (ClassNotFoundException e)
        {
            LOG.ignore(e);
        }

        try
        {
            Equinox_EquinoxBundle_Class = bundle.getClass().getClassLoader().loadClass("org.eclipse.osgi.internal.framework.EquinoxBundle");
            osgiContainer = OSGiContainerType.EquinoxLuna;
            return;
        }
        catch (ClassNotFoundException e)
        {
            LOG.ignore(e);
        }
        
        try
        {       
            //old felix or new felix?
            Felix_BundleImpl_Class = bundle.getClass().getClassLoader().loadClass("org.apache.felix.framework.BundleImpl");  
            try
            {
                Felix_BundleImpl_Adapt_Method = Felix_BundleImpl_Class.getDeclaredMethod("adapt", new Class[] {Class.class});
                osgiContainer = OSGiContainerType.Felix403;
                return;
            }
            catch (NoSuchMethodException e)
            {
                osgiContainer = OSGiContainerType.FelixOld;
                return;
            }
        }
        catch (ClassNotFoundException e)
        {
            LOG.warn("Unknown OSGi container type");
            return;
        }
        
    }

  
    
    

    /**
     * Assuming the bundle is started.
     * 
     * @param bundle
     * @return classloader object
     */
    public ClassLoader getBundleClassLoader(Bundle bundle)
    {
        String bundleActivator = (String) bundle.getHeaders().get("Bundle-Activator");
   
        if (bundleActivator == null)
        {
            bundleActivator = (String) bundle.getHeaders().get("Jetty-ClassInBundle");
        }
        if (bundleActivator != null)
        {
            try
            {
                return bundle.loadClass(bundleActivator).getClassLoader();
            }
            catch (ClassNotFoundException e)
            {
                LOG.warn(e);
            }
        }
        
        // resort to introspection     
        return getBundleClassLoaderForContainer(bundle);
    }
    
    /**
     * @param bundle
     * @return
     */
    private ClassLoader getBundleClassLoaderForContainer (Bundle bundle)
    {
        checkContainerType (bundle);
        if (osgiContainer == null)
        {
            LOG.warn("No classloader for unknown OSGi container type");
            return null;
        }
        
        switch (osgiContainer)
        {
            case EquinoxOld:
            case EquinoxLuna:
            {
                return internalGetEquinoxBundleClassLoader(bundle);
            }

            case FelixOld:
            case Felix403:
            {
                return internalGetFelixBundleClassLoader(bundle); 
            }
            default:
            {
                LOG.warn("No classloader found for bundle "+bundle.getSymbolicName());
                return null;

            }
        }
    }
    
    

    /**
     * @param bundle
     * @return
     */
    private static ClassLoader internalGetEquinoxBundleClassLoader(Bundle bundle)
    {
        if (osgiContainer == OSGiContainerType.EquinoxOld)
        {
            try
            {
                if (Equinox_BundleHost_getBundleLoader_method == null)
                {
                    Equinox_BundleHost_getBundleLoader_method = 
                            Equinox_BundleHost_Class.getDeclaredMethod("getBundleLoader", new Class[] {});
                    Equinox_BundleHost_getBundleLoader_method.setAccessible(true);
                }
                Object bundleLoader = Equinox_BundleHost_getBundleLoader_method.invoke(bundle, new Object[] {});
                if (Equinox_BundleLoader_createClassLoader_method == null && bundleLoader != null)
                {
                    Equinox_BundleLoader_createClassLoader_method = 
                            bundleLoader.getClass().getClassLoader().loadClass("org.eclipse.osgi.internal.loader.BundleLoader").getDeclaredMethod("createClassLoader", new Class[] {});
                    Equinox_BundleLoader_createClassLoader_method.setAccessible(true);
                }
                return (ClassLoader) Equinox_BundleLoader_createClassLoader_method.invoke(bundleLoader, new Object[] {});
            }
            catch (ClassNotFoundException t)
            {
                LOG.warn(t);
                return null;
            }
            catch (Throwable t)
            {
                LOG.warn(t);
                return null;
            }
        }
        
        if (osgiContainer == OSGiContainerType.EquinoxLuna)
        {
            try
            {
                if (Equinox_EquinoxBundle_getModuleClassLoader_Method == null)
                    Equinox_EquinoxBundle_getModuleClassLoader_Method = Equinox_EquinoxBundle_Class.getDeclaredMethod("getModuleClassLoader", new Class[] {Boolean.TYPE});

                Equinox_EquinoxBundle_getModuleClassLoader_Method.setAccessible(true);
                return (ClassLoader)Equinox_EquinoxBundle_getModuleClassLoader_Method.invoke(bundle, new Object[] {Boolean.FALSE});
            }
            catch (Exception e)
            {
                LOG.warn(e);
                return null;
            }
        }
        
        LOG.warn("No classloader for equinox platform for bundle "+bundle.getSymbolicName());
        return null;
    }

   


    /**
     * @param bundle
     * @return
     */
    private static ClassLoader internalGetFelixBundleClassLoader(Bundle bundle)
    {
        
        if (osgiContainer == OSGiContainerType.Felix403)
        {
            try
            {
                if (Felix_BundleWiring_Class == null)
                    Felix_BundleWiring_Class = bundle.getClass().getClassLoader().loadClass("org.osgi.framework.wiring.BundleWiring");


                Felix_BundleImpl_Adapt_Method.setAccessible(true);

                if (Felix_BundleWiring_getClassLoader_Method == null)
                {
                    Felix_BundleWiring_getClassLoader_Method = Felix_BundleWiring_Class.getDeclaredMethod("getClassLoader");
                    Felix_BundleWiring_getClassLoader_Method.setAccessible(true);
                }


                Object wiring = Felix_BundleImpl_Adapt_Method.invoke(bundle, new Object[] {Felix_BundleWiring_Class});
                return (ClassLoader)Felix_BundleWiring_getClassLoader_Method.invoke(wiring);
            }
            catch (Exception e)
            {
                LOG.warn(e);
                return null;
            }
        }


        if (osgiContainer == OSGiContainerType.FelixOld)
        {     
            try
            {
                if (Felix_BundleImpl_m_Modules_Field == null)
                {
                    Felix_BundleImpl_m_Modules_Field = Felix_BundleImpl_Class.getDeclaredField("m_modules");
                    Felix_BundleImpl_m_Modules_Field.setAccessible(true);
                }

                // Figure out which version of the modules is exported
                Object currentModuleImpl;

                try
                {
                    Object[] moduleArray = (Object[]) Felix_BundleImpl_m_Modules_Field.get(bundle);
                    currentModuleImpl = moduleArray[moduleArray.length - 1];
                }
                catch (Throwable t2)
                {
                    try
                    {
                        List<Object> moduleArray = (List<Object>) Felix_BundleImpl_m_Modules_Field.get(bundle);
                        currentModuleImpl = moduleArray.get(moduleArray.size() - 1);
                    }
                    catch (Exception e)
                    {
                        LOG.warn(e);
                        return null;
                    }
                }

                if (Felix_ModuleImpl_m_ClassLoader_Field == null && currentModuleImpl != null)
                {
                    try
                    {
                        Felix_ModuleImpl_m_ClassLoader_Field = bundle.getClass().getClassLoader().loadClass("org.apache.felix.framework.ModuleImpl").getDeclaredField("m_classLoader");
                        Felix_ModuleImpl_m_ClassLoader_Field.setAccessible(true);
                    }
                    catch (Exception e)
                    {
                        LOG.warn(e);
                        return null;
                    }
                }

                // first make sure that the classloader is ready:
                // the m_classLoader field must be initialized by the
                // ModuleImpl.getClassLoader() private method.
                ClassLoader cl = null;
                try
                {
                    cl = (ClassLoader) Felix_ModuleImpl_m_ClassLoader_Field.get(currentModuleImpl);
                    if (cl != null)
                        return cl;
                }
                catch (Exception e)
                {
                    LOG.warn(e);
                    return null;
                }

                // looks like it was not ready:
                // the m_classLoader field must be initialized by the
                // ModuleImpl.getClassLoader() private method.
                // this call will do that.
                try
                {
                    bundle.loadClass("java.lang.Object");
                    cl = (ClassLoader) Felix_ModuleImpl_m_ClassLoader_Field.get(currentModuleImpl);
                    return cl;
                }
                catch (Exception e)
                {
                    LOG.warn(e);
                    return null;
                }
            }  
            catch (Exception e)
            {
                LOG.warn(e);
                return null;
            }
        }
        
        LOG.warn("No classloader for felix platform for bundle "+bundle.getSymbolicName());
        return null;
    }
}
