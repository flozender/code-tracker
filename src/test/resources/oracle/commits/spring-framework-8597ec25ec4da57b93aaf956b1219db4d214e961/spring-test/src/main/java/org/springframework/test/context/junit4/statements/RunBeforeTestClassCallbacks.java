/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4.statements;

import org.junit.runners.model.Statement;
import org.springframework.test.context.TestContextManager;

/**
 * <code>RunBeforeTestClassCallbacks</code> is a custom JUnit 4.5+
 * {@link Statement} which allows the <em>Spring TestContext Framework</em> to
 * be plugged into the JUnit execution chain by calling
 * {@link TestContextManager#beforeTestClass() beforeTestClass()} on the
 * supplied {@link TestContextManager}.
 *
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 * @author Sam Brannen
 * @since 3.0
 */
public class RunBeforeTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * Constructs a new <code>RunBeforeTestClassCallbacks</code> statement.
	 *
	 * @param next the next <code>Statement</code> in the execution chain
	 * @param testContextManager the TestContextManager upon which to call
	 * <code>beforeTestClass()</code>
	 */
	public RunBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}

	/**
	 * Calls {@link TestContextManager#beforeTestClass()} and then invokes the
	 * next {@link Statement} in the execution chain (typically an instance of
	 * {@link org.junit.internal.runners.statements.RunBefores RunBefores}).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestClass();
		this.next.evaluate();
	}

}
