package com.puppycrawl.tools.checkstyle.checks;

import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class ClassResolverTest
    extends TestCase
{
    public void testMisc() throws ClassNotFoundException
    {
        final Set<String> imps = new HashSet<String>();
        imps.add("java.io.File");
        imps.add("nothing.will.match.*");
        imps.add("java.applet.*");
        ClassResolver cr =
            new ClassResolver(Thread.currentThread().getContextClassLoader(),
                              null, imps);
        assertNotNull(cr);
        try {
            cr.resolve("who.will.win.the.world.cup", "");
            fail("Should not resolve class");
        }
        catch (ClassNotFoundException e) {
        }
        cr.resolve("java.lang.String", "");
        cr.resolve("StringBuffer", "");
        cr.resolve("AppletContext", "");

        try {
            cr.resolve("ChoiceFormat", "");
            fail();
        }
        catch (ClassNotFoundException e) {
        }

        imps.add("java.text.ChoiceFormat");
        cr.resolve("ChoiceFormat", "");

        cr = new ClassResolver(Thread.currentThread().getContextClassLoader(),
                               "java.util", imps);
        cr.resolve("List", "");
        try {
            cr.resolve("two.nil.england", "");
            fail();
        }
        catch (ClassNotFoundException e) {
        }
    }
}
