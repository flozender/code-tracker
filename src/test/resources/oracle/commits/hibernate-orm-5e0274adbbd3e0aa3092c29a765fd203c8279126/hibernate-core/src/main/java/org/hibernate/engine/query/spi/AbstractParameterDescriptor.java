/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.query.spi;

import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameterDescriptor implements QueryParameter {
	private final int[] sourceLocations;

	private Type expectedType;

	public AbstractParameterDescriptor(int[] sourceLocations, Type expectedType) {
		this.sourceLocations = sourceLocations;
		this.expectedType = expectedType;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public Class getParameterType() {
		return expectedType == null ? null : expectedType.getReturnedClass();
	}

	@Override
	public Type getType() {
		return getExpectedType();
	}

	@Override
	public int[] getSourceLocations() {
		return sourceLocations;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public void resetExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}
}
