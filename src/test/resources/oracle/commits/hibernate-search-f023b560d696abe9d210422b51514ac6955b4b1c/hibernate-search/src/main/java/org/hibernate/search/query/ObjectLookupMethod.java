/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.query;

/**
 * Define whether or not to check if objects are already present in:
 *  - the second level cache
 *  - the persistence context
 *
 * In most cases, no presence check is necessary.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public enum ObjectLookupMethod {
	/**
	 * check whether an object is already in the second level cache
	 * before initializing it
	 */
	SECOND_LEVEL_CACHE,

	/**
	 * check whether an object is already in the persistence context
	 * before initializing it
	 */
	PERSISTENCE_CONTEXT,

	/**
	 * check whether an object is already either :
	 *  - in the second level cache
	 *  - in the persistence context
	 * before loading it.
	 */
	PERSISTENCE_CONTEXT_AND_SECOND_LEVEL_CACHE,

	/**
	 * skip checking (default)
	 */
	SKIP
}
