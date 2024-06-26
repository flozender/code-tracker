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

package org.eclipse.jetty.osgi.boot.utils.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipFile;

import org.eclipse.jetty.osgi.boot.utils.BundleFileLocatorHelper;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.FileResource;
import org.osgi.framework.Bundle;

/**
 * From a bundle to its location on the filesystem. Assumes the bundle is not a
 * jar.
 * 
 * @author hmalphettes
 */
public class DefaultFileLocatorHelper implements BundleFileLocatorHelper
{

    // hack to locate the file-system directly from the bundle.
    // support equinox, felix and nuxeo's osgi implementations.
    // not tested on nuxeo and felix just yet.
    // The url nuxeo and felix return is created directly from the File so it
    // should work.
    private static Field BUNDLE_ENTRY_FIELD = null;

    private static Field FILE_FIELD = null;

    private static Field BUNDLE_FILE_FIELD_FOR_DIR_ZIP_BUNDLE_ENTRY = null;// ZipBundleFile

    // inside
    // DirZipBundleEntry

    private static Field ZIP_FILE_FILED_FOR_ZIP_BUNDLE_FILE = null;// ZipFile

    /**
     * Works with equinox, felix, nuxeo and probably more. Not exactly in the
     * spirit of OSGi but quite necessary to support self-contained webapps and
     * other situations.
     * 
     * @param bundle The bundle
     * @return Its installation location as a file.
     * @throws Exception
     */
    public File getBundleInstallLocation(Bundle bundle) throws Exception
    {
        // String installedBundles = System.getProperty("osgi.bundles");
        // grab the MANIFEST.MF's url
        // and then do what it takes.
        URL url = bundle.getEntry("/META-INF/MANIFEST.MF");

        if (url.getProtocol().equals("file"))
        {
            // some osgi frameworks do use the file protocole directly in some
            // situations. Do use the FileResource to transform the URL into a
            // File: URL#toURI is broken
            return new FileResource(url).getFile().getParentFile().getParentFile();
        }
        else if (url.getProtocol().equals("bundleentry"))
        {
            // say hello to equinox who has its own protocol.
            // we use introspection like there is no tomorrow to get access to
            // the File

            URLConnection con = url.openConnection();
            con.setUseCaches(Resource.getDefaultUseCaches()); // work around
            // problems where
            // url connections
            // cache
            // references to
            // jars

            if (BUNDLE_ENTRY_FIELD == null)
            {
                BUNDLE_ENTRY_FIELD = con.getClass().getDeclaredField("bundleEntry");
                BUNDLE_ENTRY_FIELD.setAccessible(true);
            }
            Object bundleEntry = BUNDLE_ENTRY_FIELD.get(con);
            if (bundleEntry.getClass().getName().equals("org.eclipse.osgi.baseadaptor.bundlefile.FileBundleEntry"))
            {
                if (FILE_FIELD == null)
                {
                    FILE_FIELD = bundleEntry.getClass().getDeclaredField("file");
                    FILE_FIELD.setAccessible(true);
                }
                File f = (File) FILE_FIELD.get(bundleEntry);
                return f.getParentFile().getParentFile();
            }
            else if (bundleEntry.getClass().getName().equals("org.eclipse.osgi.baseadaptor.bundlefile.ZipBundleEntry"))
            {
                url = bundle.getEntry("/");

                con = url.openConnection();
                con.setDefaultUseCaches(Resource.getDefaultUseCaches());

                if (BUNDLE_ENTRY_FIELD == null)
                {// this one will be a DirZipBundleEntry
                    BUNDLE_ENTRY_FIELD = con.getClass().getDeclaredField("bundleEntry");
                    BUNDLE_ENTRY_FIELD.setAccessible(true);
                }
                bundleEntry = BUNDLE_ENTRY_FIELD.get(con);
                if (BUNDLE_FILE_FIELD_FOR_DIR_ZIP_BUNDLE_ENTRY == null)
                {
                    BUNDLE_FILE_FIELD_FOR_DIR_ZIP_BUNDLE_ENTRY = bundleEntry.getClass().getDeclaredField("bundleFile");
                    BUNDLE_FILE_FIELD_FOR_DIR_ZIP_BUNDLE_ENTRY.setAccessible(true);
                }
                Object zipBundleFile = BUNDLE_FILE_FIELD_FOR_DIR_ZIP_BUNDLE_ENTRY.get(bundleEntry);
                if (ZIP_FILE_FILED_FOR_ZIP_BUNDLE_FILE == null)
                {
                    ZIP_FILE_FILED_FOR_ZIP_BUNDLE_FILE = zipBundleFile.getClass().getDeclaredField("zipFile");
                    ZIP_FILE_FILED_FOR_ZIP_BUNDLE_FILE.setAccessible(true);
                }
                ZipFile zipFile = (ZipFile) ZIP_FILE_FILED_FOR_ZIP_BUNDLE_FILE.get(zipBundleFile);
                return new File(zipFile.getName());
            }
            else if (bundleEntry.getClass().getName().equals("org.eclipse.osgi.baseadaptor.bundlefile.DirZipBundleEntry"))
            {
                // that will not happen as we did ask for the manifest not a
                // directory.
            }
        }
        else if ("bundle".equals(url.getProtocol()))
        {
            // observed this on felix-2.0.0
            String location = bundle.getLocation();
            // System.err.println("location  " + location);
            if (location.startsWith("file:/"))
            {
                URI uri = new URI(URIUtil.encodePath(location));
                return new File(uri);
            }
            else if (location.startsWith("file:"))
            {
                // location defined in the BundleArchive m_bundleArchive
                // it is relative to relative to the BundleArchive's
                // m_archiveRootDir
                File res = new File(location.substring("file:".length()));
                if (!res.exists()) { return null;
                // Object bundleArchive = getFelixBundleArchive(bundle);
                // File archiveRoot =
                // getFelixBundleArchiveRootDir(bundleArchive);
                // String currentLocation =
                // getFelixBundleArchiveCurrentLocation(bundleArchive);
                // System.err.println("Got the archive root " +
                // archiveRoot.getAbsolutePath()
                // + " current location " + currentLocation +
                // " is directory ?");
                // res = new File(archiveRoot, currentLocation != null
                // ? currentLocation : location.substring("file:".length()));
                }
                return res;
            }
            else if (location.startsWith("reference:file:"))
            {
                location = URLDecoder.decode(location.substring("reference:".length()), "UTF-8");
                File file = new File(location.substring("file:".length()));
                return file;
            }
        }
        return null;
    }

    /**
     * Locate a file inside a bundle.
     * 
     * @param bundle
     * @param path
     * @return file object
     * @throws Exception
     */
    public File getFileInBundle(Bundle bundle, String path) throws Exception
    {
        if (path != null && path.length() > 0 && path.charAt(0) == '/')
        {
            path = path.substring(1);
        }
        File bundleInstall = getBundleInstallLocation(bundle);
        File webapp = path != null && path.length() != 0 ? new File(bundleInstall, path) : bundleInstall;
        if (!webapp.exists()) { throw new IllegalArgumentException("Unable to locate " + path
                                                                   + " inside "
                                                                   + bundle.getSymbolicName()
                                                                   + " ("
                                                                   + (bundleInstall != null ? bundleInstall.getAbsolutePath() : " no_bundle_location ")
                                                                   + ")"); }
        return webapp;
    }

    /**
     * Helper method equivalent to Bundle#getEntry(String entryPath) except that
     * it searches for entries in the fragments by using the Bundle#findEntries
     * method.
     * 
     * @param bundle
     * @param entryPath
     * @return null or all the entries found for that path.
     */
    public Enumeration<URL> findEntries(Bundle bundle, String entryPath)
    {
        int last = entryPath.lastIndexOf('/');
        String path = last != -1 && last < entryPath.length() - 2 ? entryPath.substring(0, last) : "/";
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        String pattern = last != -1 && last < entryPath.length() - 2 ? entryPath.substring(last + 1) : entryPath;
        Enumeration<URL> enUrls = bundle.findEntries(path, pattern, false);
        return enUrls;
    }

    /**
     * If the bundle is a jar, returns the jar. If the bundle is a folder, look
     * inside it and search for jars that it returns.
     * <p>
     * Good enough for our purpose (TldLocationsCache when it scans for tld
     * files inside jars alone. In fact we only support the second situation for
     * development purpose where the bundle was imported in pde and the classes
     * kept in a jar.
     * </p>
     * 
     * @param bundle
     * @return The jar(s) file that is either the bundle itself, either the jars
     *         embedded inside it.
     */
    public File[] locateJarsInsideBundle(Bundle bundle) throws Exception
    {
        File jasperLocation = getBundleInstallLocation(bundle);
        if (jasperLocation.isDirectory())
        {
            // try to find the jar files inside this folder
            ArrayList<File> urls = new ArrayList<File>();
            for (File f : jasperLocation.listFiles())
            {
                if (f.getName().endsWith(".jar") && f.isFile())
                {
                    urls.add(f);
                }
                else if (f.isDirectory() && f.getName().equals("lib"))
                {
                    for (File f2 : jasperLocation.listFiles())
                    {
                        if (f2.getName().endsWith(".jar") && f2.isFile())
                        {
                            urls.add(f2);
                        }
                    }
                }
            }
            return urls.toArray(new File[urls.size()]);
        }
        else
        {
            return new File[] { jasperLocation };
        }
    }

    // introspection on equinox to invoke the getLocalURL method on
    // BundleURLConnection
    // equivalent to using the FileLocator without depending on an equinox
    // class.
    private static Method BUNDLE_URL_CONNECTION_getLocalURL = null;

    private static Method BUNDLE_URL_CONNECTION_getFileURL = null;

    /**
     * Only useful for equinox: on felix we get the file:// or jar:// url
     * already. Other OSGi implementations have not been tested
     * <p>
     * Get a URL to the bundle entry that uses a common protocol (i.e. file:
     * jar: or http: etc.).
     * </p>
     * 
     * @return a URL to the bundle entry that uses a common protocol
     */
    public static URL getLocalURL(URL url)
    {
        if ("bundleresource".equals(url.getProtocol()) || "bundleentry".equals(url.getProtocol()))
        {
            try
            {
                URLConnection conn = url.openConnection();
                conn.setDefaultUseCaches(Resource.getDefaultUseCaches());
                if (BUNDLE_URL_CONNECTION_getLocalURL == null && conn.getClass().getName().equals("org.eclipse.osgi.framework.internal.core.BundleURLConnection"))
                {
                    BUNDLE_URL_CONNECTION_getLocalURL = conn.getClass().getMethod("getLocalURL", null);
                    BUNDLE_URL_CONNECTION_getLocalURL.setAccessible(true);
                }
                if (BUNDLE_URL_CONNECTION_getLocalURL != null) { return (URL) BUNDLE_URL_CONNECTION_getLocalURL.invoke(conn, null); }
            }
            catch (Throwable t)
            {
                System.err.println("Unable to locate the OSGi url: '" + url + "'.");
                t.printStackTrace();
            }
        }
        return url;
    }

    /**
     * Only useful for equinox: on felix we get the file:// url already. Other
     * OSGi implementations have not been tested
     * <p>
     * Get a URL to the content of the bundle entry that uses the file:
     * protocol. The content of the bundle entry may be downloaded or extracted
     * to the local file system in order to create a file: URL.
     * 
     * @return a URL to the content of the bundle entry that uses the file:
     *         protocol
     *         </p>
     */
    public static URL getFileURL(URL url)
    {
        if ("bundleresource".equals(url.getProtocol()) || "bundleentry".equals(url.getProtocol()))
        {
            try
            {
                URLConnection conn = url.openConnection();
                conn.setDefaultUseCaches(Resource.getDefaultUseCaches());
                if (BUNDLE_URL_CONNECTION_getFileURL == null && conn.getClass().getName().equals("org.eclipse.osgi.framework.internal.core.BundleURLConnection"))
                {
                    BUNDLE_URL_CONNECTION_getFileURL = conn.getClass().getMethod("getFileURL", null);
                    BUNDLE_URL_CONNECTION_getFileURL.setAccessible(true);
                }
                if (BUNDLE_URL_CONNECTION_getFileURL != null) { return (URL) BUNDLE_URL_CONNECTION_getFileURL.invoke(conn, null); }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
        return url;
    }

}
