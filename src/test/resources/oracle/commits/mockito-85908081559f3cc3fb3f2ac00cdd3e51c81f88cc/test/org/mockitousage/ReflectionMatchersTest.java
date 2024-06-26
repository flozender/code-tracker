/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.TestBase;
import org.mockito.exceptions.verification.InvocationDiffersFromActual;

@SuppressWarnings("all")
public class ReflectionMatchersTest extends TestBase {

    class Parent {
        private int parentField;
        protected String protectedParentField;
        public Parent(int parentField, String protectedParentField) {
            this.parentField = parentField;
            this.protectedParentField = protectedParentField;
        }
    }
    
    class Child extends Parent {
        private int childFieldOne;
        private Object childFieldTwo;
        public Child(int parentField, String protectedParentField, int childFieldOne, Object childFieldTwo) {
            super(parentField, protectedParentField);
            this.childFieldOne = childFieldOne;
            this.childFieldTwo = childFieldTwo;
        } 
    }
    
    interface MockMe {
        void run(Child child);
    }
    
    MockMe mock;
    
    @Before
    public void setup() {
        mock = mock(MockMe.class);
        
        Child actual = new Child(1, "foo", 2, "bar");
        mock.run(actual);
    }
    
    @Test
    public void shouldMatchWhenFieldValuesEqual() throws Exception {
        Child wanted = new Child(1, "foo", 2, "bar");
        verify(mock).run(refEq(wanted));
    }
    
    @Test(expected=InvocationDiffersFromActual.class)
    public void shouldNotMatchWhenFieldValuesDiffer() throws Exception {
        Child wanted = new Child(1, "foo", 2, "bar XXX");
        verify(mock).run(refEq(wanted));
    }
    
    @Test(expected=InvocationDiffersFromActual.class)
    public void shouldNotMatchAgain() throws Exception {
        Child wanted = new Child(1, "foo", 999, "bar");
        verify(mock).run(refEq(wanted));
    }
    
    @Test(expected=InvocationDiffersFromActual.class)
    public void shouldNotMatchYetAgain() throws Exception {
        Child wanted = new Child(1, "XXXXX", 2, "bar");
        verify(mock).run(refEq(wanted));
    }
    
    @Test(expected=InvocationDiffersFromActual.class)
    public void shouldNotMatch() throws Exception {
        Child wanted = new Child(234234, "foo", 2, "bar");
        verify(mock).run(refEq(wanted));
    }
}