/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.event;

import org.hibernate.LockMode;
import org.hibernate.LockRequest;

/**
 *  Defines an event class for the refreshing of an object.
 *
 * @author Steve Ebersole
 */
public class RefreshEvent extends AbstractEvent {

	private Object object;
	private LockRequest lockRequest = new LockRequest().setLockMode(LockMode.READ);

	public RefreshEvent(Object object, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null object");
		}
		this.object = object;
	}

	public RefreshEvent(Object object, LockMode lockMode, EventSource source) {
		this(object, source);
		if (lockMode == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null lock mode");
		}
		this.lockRequest.setLockMode(lockMode);
	}

	public RefreshEvent(Object object, LockRequest lockRequest, EventSource source) {
		this(object, source);
		if (lockRequest == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null lock request");
		}
		this.lockRequest = lockRequest;
	}

	public Object getObject() {
		return object;
	}

	public LockRequest getLockRequest() {
		return lockRequest;
	}

	public LockMode getLockMode() {
		return lockRequest.getLockMode();
	}

	public int getLockTimeout() {
		return this.lockRequest.getTimeOut();
	}

	public boolean getLockScope() {
		return this.lockRequest.getScope();
	}
}
