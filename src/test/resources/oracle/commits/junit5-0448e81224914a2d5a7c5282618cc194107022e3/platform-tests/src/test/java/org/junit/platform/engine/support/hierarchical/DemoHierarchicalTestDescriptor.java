/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.doNotSkip;
import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.skip;

import java.util.function.Consumer;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * @since 1.0
 */
public class DemoHierarchicalTestDescriptor extends AbstractTestDescriptor implements Node<DemoEngineExecutionContext> {

	private final Runnable executeBlock;
	private String skippedReason;
	private boolean skipped;

	DemoHierarchicalTestDescriptor(UniqueId uniqueId, String displayName, Runnable executeBlock) {
		this(uniqueId, displayName, null, executeBlock);
	}

	public DemoHierarchicalTestDescriptor(UniqueId uniqueId, String displayName, TestSource source,
			Runnable executeBlock) {
		super(uniqueId, displayName);
		if (source != null) {
			setSource(source);
		}
		this.executeBlock = executeBlock;
	}

	@Override
	public boolean isTest() {
		return !isContainer();
	}

	@Override
	public boolean isContainer() {
		return this.executeBlock == null;
	}

	@Override
	public boolean isLeaf() {
		return isTest();
	}

	public void markSkipped(String reason) {
		this.skipped = true;
		this.skippedReason = reason;
	}

	@Override
	public SkipResult shouldBeSkipped(DemoEngineExecutionContext context) throws Exception {
		return skipped ? skip(skippedReason) : doNotSkip();
	}

	@Override
	public DemoEngineExecutionContext execute(DemoEngineExecutionContext context,
			Consumer<TestDescriptor> dynamicTestExecutor) {
		if (this.executeBlock != null) {
			this.executeBlock.run();
		}
		return context;
	}

}
