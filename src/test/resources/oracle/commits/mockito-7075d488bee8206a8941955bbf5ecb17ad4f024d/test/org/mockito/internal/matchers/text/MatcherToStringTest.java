package org.mockito.internal.matchers.text;

import org.junit.Test;
import org.mockito.MockitoMatcher;
import org.mockitoutil.TestBase;

public class MatcherToStringTest extends TestBase {

    static class MatcherWithoutDescription implements MockitoMatcher<Object> {
        public boolean matches(Object argument) {
            return false;
        }
    }

    static class MatcherWithDescription implements MockitoMatcher<Object> {
        public boolean matches(Object argument) {
            return false;
        }
        public String toString() {
            return "*my custom description*";
        }
    }

    static class MatcherWithInheritedDescription extends MatcherWithDescription {
        public boolean matches(Object argument) {
            return false;
        }
    }

    @Test
    public void better_toString_for_matchers() {
        assertEquals("<Matcher without description>", MatcherToString.toString(new MatcherWithoutDescription()));
        assertEquals("*my custom description*", MatcherToString.toString(new MatcherWithDescription()));
        assertEquals("*my custom description*", MatcherToString.toString(new MatcherWithInheritedDescription()));
    }
}