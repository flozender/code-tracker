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

package org.springframework.web.jsf;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import junit.framework.TestCase;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 */
public class DelegatingPhaseListenerTests extends TestCase {

	private MockFacesContext facesContext;
	private StaticListableBeanFactory beanFactory;
	private DelegatingPhaseListenerMulticaster delPhaseListener;

	@Override
	@SuppressWarnings("serial")
	protected void setUp() {
		facesContext = new MockFacesContext();
		beanFactory = new StaticListableBeanFactory();

		delPhaseListener = new DelegatingPhaseListenerMulticaster() {
			@Override
			protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
				return beanFactory;
			}
		};
	}

	public void testBeforeAndAfterPhaseWithSingleTarget() {
		TestListener target = new TestListener();
		beanFactory.addBean("testListener", target);

		assertEquals(delPhaseListener.getPhaseId(), PhaseId.ANY_PHASE);
		PhaseEvent event = new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, new MockLifecycle());

		delPhaseListener.beforePhase(event);
		assertTrue(target.beforeCalled);

		delPhaseListener.afterPhase(event);
		assertTrue(target.afterCalled);
	}

	public void testBeforeAndAfterPhaseWithMultipleTargets() {
		TestListener target1 = new TestListener();
		TestListener target2 = new TestListener();
		beanFactory.addBean("testListener1", target1);
		beanFactory.addBean("testListener2", target2);

		assertEquals(delPhaseListener.getPhaseId(), PhaseId.ANY_PHASE);
		PhaseEvent event = new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, new MockLifecycle());

		delPhaseListener.beforePhase(event);
		assertTrue(target1.beforeCalled);
		assertTrue(target2.beforeCalled);

		delPhaseListener.afterPhase(event);
		assertTrue(target1.afterCalled);
		assertTrue(target2.afterCalled);
	}


	@SuppressWarnings("serial")
	public static class TestListener implements PhaseListener {

		boolean beforeCalled = false;
		boolean afterCalled = false;

		@Override
		public PhaseId getPhaseId() {
			return PhaseId.ANY_PHASE;
		}

		@Override
		public void beforePhase(PhaseEvent arg0) {
			beforeCalled = true;
		}

		@Override
		public void afterPhase(PhaseEvent arg0) {
			afterCalled = true;
		}
	}

}
