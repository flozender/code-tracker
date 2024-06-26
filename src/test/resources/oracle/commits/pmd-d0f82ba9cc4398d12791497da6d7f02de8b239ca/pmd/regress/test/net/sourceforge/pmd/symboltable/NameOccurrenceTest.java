/*
 * User: tom
 * Date: Oct 2, 2002
 * Time: 9:15:27 AM
 */
package test.net.sourceforge.pmd.symboltable;

import junit.framework.TestCase;

import java.util.List;

import net.sourceforge.pmd.ast.SimpleNode;
import net.sourceforge.pmd.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.symboltable.LocalScope;
import net.sourceforge.pmd.symboltable.NameOccurrence;

public class NameOccurrenceTest extends TestCase {

    public void testConstructor() {
        SimpleNode node = new ASTPrimaryExpression(1);
        node.testingOnly__setBeginLine(10);
        LocalScope lclScope = new LocalScope();
        node.setScope(lclScope);
        NameOccurrence occ = new NameOccurrence(node, "foo");
        assertEquals("foo", occ.getImage());
        assertTrue(!occ.isThisOrSuper());
        assertEquals(new NameOccurrence(null, "foo"), occ);
        assertEquals(10, occ.getBeginLine());
    }
}
