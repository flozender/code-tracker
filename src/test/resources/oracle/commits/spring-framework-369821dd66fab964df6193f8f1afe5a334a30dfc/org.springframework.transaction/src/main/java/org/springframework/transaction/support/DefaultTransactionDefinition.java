/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;

/**
 * Default implementation of the {@link TransactionDefinition} interface,
 * offering bean-style configuration and sensible default values
 * (PROPAGATION_REQUIRED, ISOLATION_DEFAULT, TIMEOUT_DEFAULT, readOnly=false).
 *
 * <p>Base class for both {@link TransactionTemplate} and
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}.
 *
 * @author Juergen Hoeller
 * @since 08.05.2003
 */
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {

	/** Prefix for the propagation constants defined in TransactionDefinition */
	public static final String PREFIX_PROPAGATION = "PROPAGATION_";

	/** Prefix for the isolation constants defined in TransactionDefinition */
	public static final String PREFIX_ISOLATION = "ISOLATION_";

	/** Prefix for transaction timeout values in description strings */
	public static final String PREFIX_TIMEOUT = "timeout_";

	/** Marker for read-only transactions in description strings */
	public static final String READ_ONLY_MARKER = "readOnly";


	/** Constants instance for TransactionDefinition */
	static final Constants constants = new Constants(TransactionDefinition.class);

	private int propagationBehavior = PROPAGATION_REQUIRED;

	private int isolationLevel = ISOLATION_DEFAULT;

	private int timeout = TIMEOUT_DEFAULT;

	private boolean readOnly = false;

	private String name;


	/**
	 * Create a new DefaultTransactionDefinition, with default settings.
	 * Can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition() {
	}

	/**
	 * Copy constructor. Definition can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition(TransactionDefinition other) {
		this.propagationBehavior = other.getPropagationBehavior();
		this.isolationLevel = other.getIsolationLevel();
		this.timeout = other.getTimeout();
		this.readOnly = other.isReadOnly();
		this.name = other.getName();
	}

	/**
	 * Create a new DefaultTransactionDefinition with the the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionDefinition(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}


	/**
	 * Set the propagation behavior by the name of the corresponding constant in
	 * TransactionDefinition, e.g. "PROPAGATION_REQUIRED".
	 * @param constantName name of the constant
	 * @exception IllegalArgumentException if the supplied value is not resolvable
	 * to one of the <code>PROPAGATION_</code> constants or is <code>null</code>
	 * @see #setPropagationBehavior
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehaviorName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(PREFIX_PROPAGATION)) {
			throw new IllegalArgumentException("Only propagation constants allowed");
		}
		setPropagationBehavior(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set the propagation behavior. Must be one of the propagation constants
	 * in the TransactionDefinition interface. Default is PROPAGATION_REQUIRED.
	 * @exception IllegalArgumentException if the supplied value is not
	 * one of the <code>PROPAGATION_</code> constants
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehavior(int propagationBehavior) {
		if (!constants.getValues(PREFIX_PROPAGATION).contains(new Integer(propagationBehavior))) {
			throw new IllegalArgumentException("Only values of propagation constants allowed");
		}
		this.propagationBehavior = propagationBehavior;
	}

	public final int getPropagationBehavior() {
		return this.propagationBehavior;
	}

	/**
	 * Set the isolation level by the name of the corresponding constant in
	 * TransactionDefinition, e.g. "ISOLATION_DEFAULT".
	 * @param constantName name of the constant
	 * @exception IllegalArgumentException if the supplied value is not resolvable
	 * to one of the <code>ISOLATION_</code> constants or is <code>null</code>
	 * @see #setIsolationLevel
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set the isolation level. Must be one of the isolation constants
	 * in the TransactionDefinition interface. Default is ISOLATION_DEFAULT.
	 * @exception IllegalArgumentException if the supplied value is not
	 * one of the <code>ISOLATION_</code> constants
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(PREFIX_ISOLATION).contains(new Integer(isolationLevel))) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = isolationLevel;
	}

	public final int getIsolationLevel() {
		return this.isolationLevel;
	}

	/**
	 * Set the timeout to apply, as number of seconds.
	 * Default is TIMEOUT_DEFAULT (-1).
	 * @see #TIMEOUT_DEFAULT
	 */
	public final void setTimeout(int timeout) {
		if (timeout < TIMEOUT_DEFAULT) {
			throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
		}
		this.timeout = timeout;
	}

	public final int getTimeout() {
		return this.timeout;
	}

	/**
	 * Set whether to optimize as read-only transaction.
	 * Default is "false".
	 */
	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public final boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Set the name of this transaction. Default is none.
	 * <p>This will be used as transaction name to be shown in a
	 * transaction monitor, if applicable (for example, WebLogic's).
	 */
	public final void setName(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}


	/**
	 * This implementation compares the <code>toString()</code> results.
	 * @see #toString()
	 */
	@Override
	public boolean equals(Object other) {
		return (other instanceof TransactionDefinition && toString().equals(other.toString()));
	}

	/**
	 * This implementation returns <code>toString()</code>'s hash code.
	 * @see #toString()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Return an identifying description for this transaction definition.
	 * <p>The format matches the one used by
	 * {@link org.springframework.transaction.interceptor.TransactionAttributeEditor},
	 * to be able to feed <code>toString</code> results into bean properties of type
	 * {@link org.springframework.transaction.interceptor.TransactionAttribute}.
	 * <p>Has to be overridden in subclasses for correct <code>equals</code>
	 * and <code>hashCode</code> behavior. Alternatively, {@link #equals}
	 * and {@link #hashCode} can be overridden themselves.
	 * @see #getDefinitionDescription()
	 * @see org.springframework.transaction.interceptor.TransactionAttributeEditor
	 */
	@Override
	public String toString() {
		return getDefinitionDescription().toString();
	}

	/**
	 * Return an identifying description for this transaction definition.
	 * <p>Available to subclasses, for inclusion in their <code>toString()</code> result.
	 */
	protected final StringBuffer getDefinitionDescription() {
		StringBuffer desc = new StringBuffer();
		desc.append(constants.toCode(new Integer(this.propagationBehavior), PREFIX_PROPAGATION));
		desc.append(',');
		desc.append(constants.toCode(new Integer(this.isolationLevel), PREFIX_ISOLATION));
		if (this.timeout != TIMEOUT_DEFAULT) {
			desc.append(',');
			desc.append(PREFIX_TIMEOUT + this.timeout);
		}
		if (this.readOnly) {
			desc.append(',');
			desc.append(READ_ONLY_MARKER);
		}
		return desc;
	}

}
