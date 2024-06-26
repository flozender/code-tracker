package org.mockitousage.bugs;

import org.junit.Test;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IOOBExceptionShouldNotBeThrownWhenNotCodingFluentlyTest {

    @Test
    public void second_stubbing_throws_IndexOutOfBoundsException() throws Exception {
        Map<String, String> map = mock(Map.class);

        OngoingStubbing<String> mapOngoingStubbing = when(map.get(anyString()));

        mapOngoingStubbing.thenReturn("first stubbing");

        try {
            mapOngoingStubbing.thenReturn("second stubbing");
        } catch (MockitoException e) {
            assertThat(e.getMessage())
                    .contains("Incorrect use of API detected here")
                    .contains(this.getClass().getSimpleName());
        }
    }
}
