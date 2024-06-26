/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
 *
 * This file is part of JavaParser.
 * 
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License 
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
 
package com.github.javaparser.bdd.steps;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class SharedSteps {

    /* Map that maintains shares state across step classes.  If manipulating the objects in the map you must update the state */
    private Map<String, Object> state;

    public SharedSteps(Map<String, Object> state){
        this.state = state;
    }

    /*
     * Given steps
     */

    @Given("a CompilationUnit")
    public void givenACompilationUnit() {
        state.put("cu1", new CompilationUnit());
    }

    @Given("a second CompilationUnit")
    public void givenASecondCompilationUnit() {
        state.put("cu2", new CompilationUnit());
    }

    /*
     * When steps
     */

    @When("the following source is parsed:$classSrc")
    public void whenTheFollowingSourceIsParsed(String classSrc) throws ParseException {
        state.put("cu1", JavaParser.parse(new StringReader(classSrc.trim()), true));
    }

    @When("the following source is parsed (trimming space):$classSrc")
    public void whenTheFollowingSourceIsParsedTrimmingSpace(String classSrc) throws ParseException {
        state.put("cu1", JavaParser.parse(new StringReader(classSrc.trim()), true));
    }

    @When("the following sources is parsed by the second CompilationUnit:$classSrc")
    public void whenTheFollowingSourcesIsParsedBytTheSecondCompilationUnit(String classSrc) throws ParseException {
        state.put("cu2", JavaParser.parse(new StringReader(classSrc.trim()), true));
    }

    @When("file \"$fileName\" is parsed")
    public void whenTheJavaFileIsParsed(String fileName) throws IOException, ParseException, URISyntaxException {
        URL url = getClass().getResource("../samples/" + fileName);
        CompilationUnit compilationUnit = JavaParser.parse(new File(url.toURI()));
        state.put("cu1", compilationUnit);
    }

    @Then("the CompilationUnit is equal to the second CompilationUnit")
    public void thenTheCompilationUnitIsEqualToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit, is(equalTo(compilationUnit2)));
    }

    @Then("the CompilationUnit has the same hashcode to the second CompilationUnit")
    public void thenTheCompilationUnitHasTheSameHashcodeToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit.hashCode(), is(equalTo(compilationUnit2.hashCode())));
    }

    @Then("the CompilationUnit is not equal to the second CompilationUnit")
    public void thenTheCompilationUnitIsNotEqualToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit, not(equalTo(compilationUnit2)));
    }

    @Then("the CompilationUnit has a different hashcode to the second CompilationUnit")
    public void thenTheCompilationUnitHasADifferentHashcodeToTheSecondCompilationUnit() {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        CompilationUnit compilationUnit2 = (CompilationUnit) state.get("cu2");

        assertThat(compilationUnit.hashCode(), not(equalTo(compilationUnit2.hashCode())));
    }

    @Then("the expected source should be:$classSrc")
    public void thenTheExpectedSourcesShouldBe(String classSrc) {
        CompilationUnit compilationUnit = (CompilationUnit) state.get("cu1");
        assertThat(compilationUnit.toString(), CoreMatchers.is(equalToIgnoringWhiteSpace(classSrc)));
    }

	public static BodyDeclaration<?> getMemberByTypeAndPosition(TypeDeclaration<?> typeDeclaration, int position, Class<? extends BodyDeclaration<?>> typeClass) {
        int typeCount = 0;
		for (BodyDeclaration<?> declaration : typeDeclaration.getMembers()) {
            if(declaration.getClass().equals(typeClass)){
                if(typeCount == position){
                    return declaration;
                }
                typeCount++;
            }
        }
        throw new IllegalArgumentException("No member " + typeClass + " at position: " + position );
    }

    public static MethodDeclaration getMethodByPositionAndClassPosition(CompilationUnit compilationUnit,
                                                                         int methodPosition, int classPosition) {
		TypeDeclaration<?> type = compilationUnit.getTypes().get(classPosition - 1);

        int memberCount = 0;
        int methodCount = 0;
		for (BodyDeclaration<?> bodyDeclaration : type.getMembers()) {
            if(bodyDeclaration instanceof MethodDeclaration){
                if(methodCount == methodPosition -1) {
                    return (MethodDeclaration) type.getMembers().get(memberCount);
                }
                methodCount++;
            }
            memberCount++;
        }
        throw new IllegalArgumentException("Method not found at position " +methodPosition+ "in class " + classPosition);
    }
}
