/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.matchers;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.mockito.MockitoMatcher;

public class Find implements MockitoMatcher<String>, Serializable {

    private final String regex;

    public Find(String regex) {
        this.regex = regex;
    }

    public boolean matches(Object actual) {
        return actual != null && Pattern.compile(regex).matcher((String) actual).find();
    }

    public String toString() {
        return "find(\"" + regex.replaceAll("\\\\", "\\\\\\\\") + "\")";
    }
}