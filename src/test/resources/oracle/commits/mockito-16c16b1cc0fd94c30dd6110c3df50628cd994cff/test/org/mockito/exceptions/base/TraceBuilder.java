/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.exceptions.base;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TraceBuilder {

    private String[] methods = {};
    private String[] classes = {};

    public HasStackTrace toTrace() {
        List<StackTraceElement> trace = toTraceList();
        return new HasStackTraceStub(trace.toArray(new StackTraceElement[methods.length]));
    }

    public List<StackTraceElement> toTraceList() {
        assert methods.length == 0 || classes.length == 0;
        
        List<StackTraceElement> trace = new LinkedList<StackTraceElement>();
        for (String method : methods) {
            trace.add(new StackTraceElement("SomeClass", method, "SomeClass.java", 50));
        }
        for (String clazz : classes) {
            trace.add(new StackTraceElement(clazz, "someMethod", clazz + ".java", 50));
        }
        
        Collections.reverse(trace);
        return trace;
    }

    public TraceBuilder classes(String ... classes) {
        this.classes = classes;
        return this;
    }
    
    public TraceBuilder methods(String ... methods) {
        this.methods = methods;
        return this;
    }
}