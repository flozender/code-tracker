package test.net.sourceforge.pmd.symboltable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.DummyJavaNode;
import net.sourceforge.pmd.lang.java.symboltable.NameOccurrence;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.java.symboltable.VariableUsageFinderFunction;
import net.sourceforge.pmd.lang.java.symboltable.Applier;

import org.junit.Test;
public class VariableUsageFinderFunctionTest {

    @Test
    public void testLookingForUsed() {
        ASTVariableDeclaratorId variableDeclarationIdNode = new ASTVariableDeclaratorId(1);
        variableDeclarationIdNode.setImage("x");
        VariableNameDeclaration nameDeclaration = new VariableNameDeclaration(variableDeclarationIdNode);
        List<NameOccurrence> nameOccurrences = new ArrayList<NameOccurrence>();
        nameOccurrences.add(new NameOccurrence(new DummyJavaNode(2), "x"));

        Map<VariableNameDeclaration, List<NameOccurrence>> declarations = new HashMap<VariableNameDeclaration, List<NameOccurrence>>();
        declarations.put(nameDeclaration, nameOccurrences);

        List<VariableNameDeclaration> vars = new ArrayList<VariableNameDeclaration>();
        vars.add(nameDeclaration);

        VariableUsageFinderFunction f = new VariableUsageFinderFunction(declarations);
        Applier.apply(f, vars.iterator());
        Map p = f.getUsed();
        assertEquals(1, p.size());
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(VariableUsageFinderFunctionTest.class);
    }
}
