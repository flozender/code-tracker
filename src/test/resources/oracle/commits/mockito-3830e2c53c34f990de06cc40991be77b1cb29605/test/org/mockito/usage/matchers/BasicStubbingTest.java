package org.mockito.usage.matchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.util.ExtraMatchers.collectionContaining;

import java.util.*;

import org.junit.*;
import org.mockito.Mockito;
import org.mockito.exceptions.VerificationAssertionError;

@SuppressWarnings("unchecked")
public class BasicStubbingTest {

    private DummyInterface mock;

    @Before
    public void setup() {
        mock = Mockito.mock(DummyInterface.class);
    }
    
    private interface DummyInterface {
        int getInt(String value);
        String getString(int argumentOne, String argumentTwo);
        List<String> getList();
    }
    
    @Test
    public void shouldStubAllMethodsByDefault() throws Exception {
        assertEquals(0, mock.getInt("test"));
        assertEquals(0, mock.getInt("testTwo"));
        
        assertNull(mock.getString(0, null));
        assertNull(mock.getString(100, null));
        
        assertEquals(0, mock.getList().size());
        assertEquals(0, mock.getList().size());
    }
    
    @Test
    public void shouldStubAndLetBeCalledAnyTimes() throws Exception {
        stub(mock.getInt("14")).andReturn(14);
        
        assertThat(mock.getInt("14"), equalTo(14));
        assertThat(mock.getInt("14"), equalTo(14));
        
        stub(mock.getList()).andReturn(Arrays.asList("elementOne", "elementTwo"));
        
        assertThat(mock.getList(), collectionContaining("elementOne", "elementTwo"));
        assertThat(mock.getList(), collectionContaining("elementOne", "elementTwo"));
        
        stub(mock.getString(10, "test")).andReturn("test");
        
        assertThat(mock.getString(10, "test"), equalTo("test"));
        assertThat(mock.getString(10, "test"), equalTo("test"));
    }
    
    @Test
    public void shouldStubbingBeTreatedAsInteraction() throws Exception {
        mock = Mockito.mock(DummyInterface.class);
        
        stub(mock.getInt("blah"));
        
        try {
            verifyNoMoreInteractions(mock);
            fail();
        } catch (VerificationAssertionError e) {}
    }
}
