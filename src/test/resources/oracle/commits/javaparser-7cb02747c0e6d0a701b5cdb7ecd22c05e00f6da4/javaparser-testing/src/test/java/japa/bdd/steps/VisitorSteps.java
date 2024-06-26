package japa.bdd.steps;

import japa.bdd.visitors.PositionTestVisitor;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.CloneVisitor;
import japa.parser.ast.visitor.GenericVisitorAdapter;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class VisitorSteps {

    /* Fields used to maintain step state within this step class */
    private VoidVisitorAdapter<AtomicReference<String>> toUpperCaseVariableNameVisitor;
    private VoidVisitorAdapter<AtomicReference<String>> collectVariableNameVisitor;
    private PositionTestVisitor positionTestVisitor;
    private GenericVisitorAdapter<String, Void> nameReturningVisitor;
    private AtomicReference<String> collectedVariableName;
    private String returnedVariableName;

    /* Map that maintains shares state across step classes.  If manipulating the objects in the map you must update the state */
    private Map<String, Object> state;

    public VisitorSteps(Map<String, Object> state){
        this.state = state;
    }

    @Given("a VoidVisitorAdapter with a visit method that changes variable names to uppercase")
    public void givenAVoidVisitorAdapterWithAVisitMethodThatChangesVariableNamesToUppercase() {
        toUpperCaseVariableNameVisitor = new VoidVisitorAdapter<AtomicReference<String>>() {
            @Override
            public void visit(VariableDeclaratorId n, AtomicReference<String> arg) {
                n.setName(n.getName().toUpperCase());
            }
        };
    }

    @Given("a VoidVisitorAdapter with a visit method and collects the variable names")
    public void givenAVoidVisitorAdapterWithAVisitMethodThatCollectsTheVariableName() {
        collectVariableNameVisitor = new VoidVisitorAdapter<AtomicReference<String>>() {
            @Override
            public void visit(VariableDeclaratorId n, AtomicReference<String> arg) {
                arg.set(arg.get() + n.getName() + ";");
            }
        };
    }

    @Given("a GenericVisitorAdapter with a visit method that returns variable names")
    public void givenAGenericVisitorAdapterWithAVisitMethodThatReturnsVariableNames() {
        nameReturningVisitor = new GenericVisitorAdapter<String, Void>() {
            @Override
            public String visit(VariableDeclaratorId n, Void arg) {
                return n.getName();
            }
        };
    }

    @Given("a VoidVisitorAdapter with a visit method that asserts sensible line positions")
    public void givenAVoidVisitorAdapterWithAVisitMethodThatAssertsSensibleLinePositions() {
        positionTestVisitor = new PositionTestVisitor();
    }

    @When("the CompilationUnit is cloned to the second CompilationUnit")
    public void whenTheSecondCompilationUnitIsCloned() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit)compilationUnit.accept(new CloneVisitor(), null);
        state.put("cu2", compilationUnit2);
    }

    @When("the CompilationUnit is visited by the to uppercase visitor")
    public void whenTheCompilationUnitIsVisitedByTheVistor() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        toUpperCaseVariableNameVisitor.visit(compilationUnit, null);
        state.put("cu1", compilationUnit);
    }

    @When("the CompilationUnit is visited by the variable name collector visitor")
    public void whenTheCompilationUnitIsVisitedByTheVariableNameCollectorVisitor() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        collectedVariableName = new AtomicReference<String>("");
        collectVariableNameVisitor.visit(compilationUnit, collectedVariableName);
    }

    @When("the CompilationUnit is visited by the visitor that returns variable names")
    public void whenTheCompilationUnitIsVisitedByTheVisitorThatReturnsVariableNames() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        returnedVariableName = nameReturningVisitor.visit(compilationUnit, null);
    }

    @When("the CompilationUnit is visited by the PositionTestVisitor")
    public void whenTheCompilationUnitIsVisitedByThePositionTestVisitor() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        compilationUnit.accept(positionTestVisitor, null);
    }

    @Then("the collected variable name is \"$nameUnderTest\"")
    public void thenTheCollectedVariableNameIs(String nameUnderTest) {
        assertThat(collectedVariableName.get(), is(nameUnderTest));
    }

    @Then("the return variable name is \"$nameUnderTest\"")
    public void thenTheReturnVariableNameIs(String nameUnderTest) {
        assertThat(returnedVariableName, is(nameUnderTest));
    }

    @Then("the total number of nodes visited is $expectedCount")
    public void thenTheTotalNumberOfNodesVisitedIs(int expectedCount) {
        assertThat(positionTestVisitor.getNumberOfNodesVisited(), is(expectedCount));
    }
}
