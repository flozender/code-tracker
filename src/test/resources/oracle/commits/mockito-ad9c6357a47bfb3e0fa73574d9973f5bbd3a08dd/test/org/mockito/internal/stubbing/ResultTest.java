package org.mockito.internal.stubbing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.util.ExtraMatchers.*;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.RequiresValidState;
import org.mockito.exceptions.parents.HasStackTrace;
import org.mockito.exceptions.parents.StackTraceFilter;

public class ResultTest extends RequiresValidState {

    @Test
    public void shouldCreateReturnResult() throws Throwable {
        Result result = Result.createReturnResult("lol");
        assertEquals("lol", result.answer());
    }
    
    @Test(expected=RuntimeException.class)
    public void shouldCreateThrowResult() throws Throwable {
        Result.createThrowResult(new RuntimeException(), new StackTraceFilter()).answer();
    }
    
    @Test
    public void shouldFilterStackTraceWhenCreatingThrowResult() throws Throwable {
        StackTraceFilterStub filterStub = new StackTraceFilterStub();
        Result result = Result.createThrowResult(new RuntimeException(), filterStub);
        try {
            result.answer(); 
            fail();
        } catch (RuntimeException e) {
            assertTrue(Arrays.equals(filterStub.hasStackTrace.getStackTrace(), e.getStackTrace()));
            assertThat("should fill in stack trace", e, hasFirstMethodInStackTrace("answer"));
        }
    }
    
    class StackTraceFilterStub extends StackTraceFilter {
        HasStackTrace hasStackTrace;
        @Override public void filterStackTrace(HasStackTrace hasStackTrace) {
            this.hasStackTrace = hasStackTrace;
        }
    }
}
