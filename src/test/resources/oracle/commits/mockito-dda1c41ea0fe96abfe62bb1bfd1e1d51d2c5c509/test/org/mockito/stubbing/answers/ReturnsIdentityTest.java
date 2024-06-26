package org.mockito.stubbing.answers;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockitousage.IMethods;

import java.lang.reflect.Method;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ReturnsIdentityTest {
	@Test
	public void should_be_able_to_return_the_first_parameter() throws Throwable {
		assertThat(new ReturnsIdentity(0).answer(invocationWith("A", "B"))).isEqualTo("A");
	}

	@Test
	public void should_be_able_to_return_the_second_parameter()
			throws Throwable {
		assertThat(new ReturnsIdentity(1).answer(invocationWith("A", "B", "C"))).isEqualTo("B");
	}

	@Test
	public void should_be_able_to_return_the_last_parameter() throws Throwable {
		assertThat(new ReturnsIdentity(-1).answer(invocationWith("A"))).isEqualTo("A");
		assertThat(new ReturnsIdentity(-1).answer(invocationWith("A", "B"))).isEqualTo("B");
	}

	@Test
	public void should_be_able_to_return_the_specified_parameter() throws Throwable {
		assertThat(new ReturnsIdentity(0).answer(invocationWith("A", "B", "C"))).isEqualTo("A");
		assertThat(new ReturnsIdentity(1).answer(invocationWith("A", "B", "C"))).isEqualTo("B");
		assertThat(new ReturnsIdentity(2).answer(invocationWith("A", "B", "C"))).isEqualTo("C");
	}

	@Test
	public void should_raise_an_exception_if_index_is_not_in_allowed_range_at_creation_time() throws Throwable {
        try {
            new ReturnsIdentity(-30);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage())
                    .containsIgnoringCase("argument index")
                    .containsIgnoringCase("positive number")
                    .contains("1")
                    .containsIgnoringCase("last argument");
        }
    }

	private static InvocationOnMock invocationWith(final String... parameters) {
        return new InvocationOnMock() {

            public Object getMock() {
                return null;
            }

            public Method getMethod() {
                try {
                    return IMethods.class.getDeclaredMethod("varargsReturningString", Object[].class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            public Object[] getArguments() {
                return parameters;
            }

            public Object callRealMethod() throws Throwable {
                return null;
            }
        };
    }

}
