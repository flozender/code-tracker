package com.github.javaparser.junit.builders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.javaparser.ast.type.PrimitiveType.*;
import static org.junit.Assert.assertEquals;

public class FieldDeclarationBuildersTest {
    CompilationUnit cu;
    private ClassOrInterfaceDeclaration testClass;
    private EnumDeclaration testEnum;

    @Before
    public void setup() {
        cu = new CompilationUnit();
        testClass = cu.addClass("testClass");
        testEnum = cu.addEnum("testEnum");
    }

    @After
    public void teardown() {
        cu = null;
        testClass = null;
        testEnum = null;
    }

    @Test(expected = IllegalStateException.class)
    public void testOrphanFieldGetter() {
        new FieldDeclaration().createGetter();
    }

    @Test(expected = IllegalStateException.class)
    public void testOrphanFieldSetter() {
        new FieldDeclaration().createSetter();
    }

    @Test
    public void testCreateGetterInAClass() {
        testClass.addPrivateField(int.class, "myField").createGetter();
        assertEquals(2, testClass.getMembers().size());
        assertEquals(MethodDeclaration.class, testClass.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testClass.getMethodsByName("getMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration getter = methodsWithName.get(0);
        assertEquals("getMyField", getter.getNameAsString());
        assertEquals("int", getter.getType().toString());
        assertEquals(ReturnStmt.class, getter.getBody().get().getStatement(0).getClass());
    }

    @Test
    public void testCreateSetterInAClass() {
        testClass.addPrivateField(int.class, "myField").createSetter();
        assertEquals(2, testClass.getMembers().size());
        assertEquals(MethodDeclaration.class, testClass.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testClass.getMethodsByName("setMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration setter = methodsWithName.get(0);
        assertEquals("setMyField", setter.getNameAsString());
        assertEquals("int", setter.getParameter(0).getType().toString());
        assertEquals(ExpressionStmt.class, setter.getBody().get().getStatement(0).getClass());
        assertEquals("this.myField = myField;", setter.getBody().get().getStatement(0).toString());
    }

    @Test
    public void testCreateGetterInEnum() {
        testEnum.addPrivateField(int.class, "myField").createGetter();
        assertEquals(2, testEnum.getMembers().size());
        assertEquals(MethodDeclaration.class, testEnum.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testEnum.getMethodsByName("getMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration getter = methodsWithName.get(0);
        assertEquals("getMyField", getter.getNameAsString());
        assertEquals("int", getter.getType().toString());
        assertEquals(ReturnStmt.class, getter.getBody().get().getStatement(0).getClass());
    }

    @Test
    public void testCreateSetterInEnum() {
        testEnum.addPrivateField(int.class, "myField").createSetter();
        assertEquals(2, testEnum.getMembers().size());
        assertEquals(MethodDeclaration.class, testEnum.getMember(1).getClass());
        List<MethodDeclaration> methodsWithName = testEnum.getMethodsByName("setMyField");
        assertEquals(1, methodsWithName.size());
        MethodDeclaration setter = methodsWithName.get(0);
        assertEquals("setMyField", setter.getNameAsString());
        assertEquals("int", setter.getParameter(0).getType().toString());
        assertEquals(ExpressionStmt.class, setter.getBody().get().getStatement(0).getClass());
        assertEquals("this.myField = myField;", setter.getBody().get().getStatement(0).toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateGetterWithANonValidField() {
        FieldDeclaration myPrivateField = testClass.addPrivateField(int.class, "myField");
        myPrivateField.getVariables().add(new VariableDeclarator(INT_TYPE, new VariableDeclaratorId("secondField")));
        myPrivateField.createGetter();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateSetterWithANonValidField() {
        FieldDeclaration myPrivateField = testClass.addPrivateField(int.class, "myField");
        myPrivateField.getVariables().add(new VariableDeclarator(INT_TYPE, new VariableDeclaratorId("secondField")));
        myPrivateField.createSetter();
    }

}
