/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.AdviceBindingTestAspect.AdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class AfterAdviceBindingTests {

	private AdviceBindingCollaborator mockCollaborator;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	@Before
	public void setUp() throws Exception {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		AdviceBindingTestAspect afterAdviceAspect = (AdviceBindingTestAspect) ctx.getBean("testAspect");

		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(testBeanProxy));

		// we need the real target too, not just the proxy...
		testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

		mockCollaborator = createNiceMock(AdviceBindingCollaborator.class);
		afterAdviceAspect.setCollaborator(mockCollaborator);
	}

	@Test
	public void testOneIntArg() {
		mockCollaborator.oneIntArg(5);
		replay(mockCollaborator);
		testBeanProxy.setAge(5);
		verify(mockCollaborator);
	}

	@Test
	public void testOneObjectArgBindingProxyWithThis() {
		mockCollaborator.oneObjectArg(this.testBeanProxy);
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

	@Test
	public void testOneObjectArgBindingTarget() {
		mockCollaborator.oneObjectArg(this.testBeanTarget);
		replay(mockCollaborator);
		testBeanProxy.getDoctor();
		verify(mockCollaborator);
	}

	@Test
	public void testOneIntAndOneObjectArgs() {
		mockCollaborator.oneIntAndOneObject(5,this.testBeanProxy);
		replay(mockCollaborator);
		testBeanProxy.setAge(5);
		verify(mockCollaborator);
	}

	@Test
	public void testNeedsJoinPoint() {
		mockCollaborator.needsJoinPoint("getAge");
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

	@Test
	public void testNeedsJoinPointStaticPart() {
		mockCollaborator.needsJoinPointStaticPart("getAge");
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

}
