/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.matchers;

public class GreaterThan<T extends Comparable<T>> extends CompareTo<T> {

    public GreaterThan(Comparable<T> value) {
        super(value);
    }

    @Override
    protected String getName() {
        return "gt";
    }

    @Override
    protected boolean matchResult(int result) {
        return result > 0;
    }    
}
