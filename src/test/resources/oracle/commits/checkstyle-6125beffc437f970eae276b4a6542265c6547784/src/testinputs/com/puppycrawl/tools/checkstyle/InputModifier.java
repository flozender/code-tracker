////////////////////////////////////////////////////////////////////////////////
// Test case file for checkstyle.
// Created: 2001
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle;

/**
 * Test case for Modifier checks:
 * - order of modifiers
 * - use of 'public' in interface definition
 * @author lkuehne
 */
strictfp final class InputModifier // illegal order of modifiers for class
{

    /** Illegal order of modifiers for variables */
    static private boolean sModifierOrderVar = false;

    /**
     * Illegal order of modifiers for methods. Make sure that the
     * first and last modifier from the JLS sequence is used.
     */
    strictfp private void doStuff()
    {
    }

    /** Single annotation without other modifiers */
    @MyAnnotation void someMethod()
    {
    }

    /** Illegal order of annotation - must come first */
    private @MyAnnotation void someMethod2()
    {
    }

    /** Annotation in middle of other modifiers otherwise in correct order */
    private @MyAnnotation strictfp void someMethod3()
    {
    }

    /** Correct order */
    @MyAnnotation private strictfp void someMethod4()
    {
    }

    /** Annotation in middle of other modifiers otherwise in correct order */
    @MyAnnotation private static @MyAnnotation2 strictfp void someMethod5()
    {
    }

    /** holder for redundant 'public' modifier check. */
    public interface InputRedundantPublicModifier
    {
        /** redundant 'public' modifier */
        public void a();

        /** all OK */
        void b();

        /** redundant abstract modifier */
        abstract void c();

        /** redundant 'public' modifier */
        public float PI_PUBLIC = 3.14;

        /** redundant 'abstract' modifier */
        abstract float PI_ABSTRACT = 3.14;

        /** redundant 'final' modifier */
        final float PI_FINAL = 3.14;

        /** all OK */
        float PI_OK = 3.14;
    }
    
    /** redundant 'final' modifier */
    private final void method()
    {
    }
}

/** Holder for redundant 'final' check. */
final class RedundantFinalClass
{
    /** redundant 'final' modifier */
    public final void finalMethod()
    {
    }

    /** OK */
    public void method()
    {
    }
}

/** Holder for redundant modifiers of inner implementation */
interface InnerImplementation
{
    InnerImplementation inner =
        new InnerImplementation()
        {
            /** compiler requires 'public' modifier */
            public void method()
            {
            }
        };
    
    void method();
}

/** Holder for redundant modifiers of annotation fields/variables */
@interface Annotation
{
    public String s1 = "";
    final String s2 = "";
    static String s3 = "";
    String s4 = "";
    public String blah();
    abstract String blah2();
}
