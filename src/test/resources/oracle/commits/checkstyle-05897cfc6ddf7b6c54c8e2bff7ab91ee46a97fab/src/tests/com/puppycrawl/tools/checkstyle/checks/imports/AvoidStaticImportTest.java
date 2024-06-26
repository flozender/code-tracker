package com.puppycrawl.tools.checkstyle.checks.imports;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import java.io.File;
import org.junit.Test;

public class AvoidStaticImportTest
    extends BaseCheckTestSupport
{
    @Test
    public void testDefaultOperation()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(AvoidStaticImportCheck.class);
        final String[] expected = {
            "23: Using a static member import should be avoided - java.io.File.listRoots.",
            "25: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
            "26: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
            "27: Using a static member import should be avoided - java.io.File.createTempFile.",
            "28: Using a static member import should be avoided - sun.net.ftpclient.FtpClient.*.",
        };

        verify(checkConfig, getPath("imports" + File.separator + "InputImport.java"), expected);
    }

    @Test
    public void testStarExcludes()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(AvoidStaticImportCheck.class);
        checkConfig.addAttribute("excludes", "java.io.File.*,sun.net.ftpclient.FtpClient.*");
        // allow the java.io.File.*/sun.net.ftpclient.FtpClient.* star imports
        final String[] expected = {
                "25: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
                "26: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
            };
        verify(checkConfig, getPath("imports" + File.separator + "InputImport.java"), expected);
    }
    
    @Test
    public void testMemberExcludes()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(AvoidStaticImportCheck.class);
        checkConfig.addAttribute("excludes", "java.io.File.listRoots");
        // allow the java.io.File.listRoots member imports
        final String[] expected = {
                "25: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
                "26: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
                "27: Using a static member import should be avoided - java.io.File.createTempFile.",
                "28: Using a static member import should be avoided - sun.net.ftpclient.FtpClient.*.",
            };
        verify(checkConfig, getPath("imports" + File.separator + "InputImport.java"), expected);
    }
    
    @Test
    public void testBogusMemberExcludes()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(AvoidStaticImportCheck.class);
        checkConfig.addAttribute("excludes", "java.io.File.listRoots.listRoots, javax.swing.WindowConstants," +
        		"sun.net.ftpclient.FtpClient.*FtpClient, sun.net.ftpclient.FtpClientjunk, java.io.File.listRootsmorejunk");
        // allow the java.io.File.listRoots member imports
        final String[] expected = {
                "23: Using a static member import should be avoided - java.io.File.listRoots.",
                "25: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
                "26: Using a static member import should be avoided - javax.swing.WindowConstants.*.",
                "27: Using a static member import should be avoided - java.io.File.createTempFile.",
                "28: Using a static member import should be avoided - sun.net.ftpclient.FtpClient.*.",
        };
        verify(checkConfig, getPath("imports" + File.separator + "InputImport.java"), expected);
    }
}
