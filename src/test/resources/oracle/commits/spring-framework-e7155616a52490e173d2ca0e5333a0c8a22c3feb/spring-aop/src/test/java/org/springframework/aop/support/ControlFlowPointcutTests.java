/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;

import test.aop.NopInterceptor;
import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class ControlFlowPointcutTests {

	@Test
	public void testMatches() {
		TestBean target = new TestBean();
		target.setAge(27);
		NopInterceptor nop = new NopInterceptor();
		ControlFlowPointcut cflow = new ControlFlowPointcut(One.class, "getAge");
		ProxyFactory pf = new ProxyFactory(target);
		ITestBean proxied = (ITestBean) pf.getProxy();
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));

		// Not advised, not under One
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(0, nop.getCount());

		// Will be advised
		assertEquals(target.getAge(), new One().getAge(proxied));
		assertEquals(1, nop.getCount());

		// Won't be advised
		assertEquals(target.getAge(), new One().nomatch(proxied));
		assertEquals(1, nop.getCount());
		assertEquals(3, cflow.getEvaluations());
	}

	/**
	 * Check that we can use a cflow pointcut only in conjunction with
	 * a static pointcut: e.g. all setter methods that are invoked under
	 * a particular class. This greatly reduces the number of calls
	 * to the cflow pointcut, meaning that it's not so prohibitively
	 * expensive.
	 */
	@Test
	public void testSelectiveApplication() {
		TestBean target = new TestBean();
		target.setAge(27);
		NopInterceptor nop = new NopInterceptor();
		ControlFlowPointcut cflow = new ControlFlowPointcut(One.class);
		Pointcut settersUnderOne = Pointcuts.intersection(Pointcuts.SETTERS, cflow);
		ProxyFactory pf = new ProxyFactory(target);
		ITestBean proxied = (ITestBean) pf.getProxy();
		pf.addAdvisor(new DefaultPointcutAdvisor(settersUnderOne, nop));

		// Not advised, not under One
		target.setAge(16);
		assertEquals(0, nop.getCount());

		// Not advised; under One but not a setter
		assertEquals(16, new One().getAge(proxied));
		assertEquals(0, nop.getCount());

		// Won't be advised
		new One().set(proxied);
		assertEquals(1, nop.getCount());

		// We saved most evaluations
		assertEquals(1, cflow.getEvaluations());
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		assertEquals(new ControlFlowPointcut(One.class), new ControlFlowPointcut(One.class));
		assertEquals(new ControlFlowPointcut(One.class, "getAge"), new ControlFlowPointcut(One.class, "getAge"));
		assertFalse(new ControlFlowPointcut(One.class, "getAge").equals(new ControlFlowPointcut(One.class)));
		assertEquals(new ControlFlowPointcut(One.class).hashCode(), new ControlFlowPointcut(One.class).hashCode());
		assertEquals(new ControlFlowPointcut(One.class, "getAge").hashCode(), new ControlFlowPointcut(One.class, "getAge").hashCode());
		assertFalse(new ControlFlowPointcut(One.class, "getAge").hashCode() == new ControlFlowPointcut(One.class).hashCode());
	}

	public class One {
		int getAge(ITestBean proxied) {
			return proxied.getAge();
		}
		int nomatch(ITestBean proxied) {
			return proxied.getAge();
		}
		void set(ITestBean proxied) {
			proxied.setAge(5);
		}
	}

}
