package test.net.sourceforge.pmd.ast;

import java.util.Set;
import java.util.Iterator;

import net.sourceforge.pmd.ast.*;

public class FieldDeclTest
    extends ParserTst
{
    public String makeAccessJavaCode( String access[] ) {
	String RC = 
	    "public class Test { ";
	for (int i = 0; i < access.length; i++) {
	    RC += access[i] + " ";
	}

	RC += " int j;  }";
	return RC;
    }

    public ASTFieldDeclaration getFieldDecl( String access[] )
	throws Throwable
    {
	Set fields = getNodes( ASTFieldDeclaration.class,
			       makeAccessJavaCode( access ) );
	
	assertEquals( "Wrong number of fields",
		      1, fields.size());
	Iterator i = fields.iterator();
	return (ASTFieldDeclaration) i.next();
    }
	
    public void testPublic() 
	throws Throwable
    {
	String access[] = { "public" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be public.",
		    afd.isPublic() );
    }

    public void testProtected() 
	throws Throwable
    {
	String access[] = { "protected" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be protected.",
		    afd.isProtected() );
    }

    public void testPrivate() 
	throws Throwable
    {
	String access[] = { "private" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be private.",
		    afd.isPrivate() );
    }

    public void testStatic() 
	throws Throwable
    {
	String access[] = { "private", "static" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be static.",
		    afd.isStatic() );
	assertTrue( "Expecting field to be private.",
		    afd.isPrivate() );
    }

    public void testFinal() 
	throws Throwable
    {
	String access[] = { "public", "final" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be final.",
		    afd.isFinal() );
	assertTrue( "Expecting field to be public.",
		    afd.isPublic() );
    }

    public void testTransient() 
	throws Throwable
    {
	String access[] = { "private", "transient" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be private.",
		    afd.isPrivate() );
	assertTrue( "Expecting field to be transient.",
		    afd.isTransient() );
    }

    public void testVolatile() 
	throws Throwable
    {
	String access[] = { "private", "volatile" };
	ASTFieldDeclaration afd = getFieldDecl( access );
	assertTrue( "Expecting field to be volatile.",
		    afd.isVolatile() );
	assertTrue( "Expecting field to be private.",
		    afd.isPrivate() );
    }
}
