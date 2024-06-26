/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.cache.spi;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be
 * stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * @author Gavin King
 */
public class CacheKey implements Serializable {
	private final Serializable key;
	private final Type type;
	private final String entityOrRoleName;
	private final EntityMode entityMode;
	private final String tenantId;
	private final int hashCode;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param type The Hibernate type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param entityMode The entity mode of the originating session
	 * @param tenantId The tenant identifier associated this data.
	 * @param factory The session factory for which we are caching
	 */
	public CacheKey(
			final Serializable id,
			final Type type,
			final String entityOrRoleName,
			final EntityMode entityMode,
			final String tenantId,
			final SessionFactoryImplementor factory) {
		this.key = id;
		this.type = type;
		this.entityOrRoleName = entityOrRoleName;
		this.entityMode = entityMode;
		this.tenantId = tenantId;
		this.hashCode = type.getHashCode( key, entityMode, factory );
	}

	@Override
	public String toString() {
		// Mainly for OSCache
		return entityOrRoleName + '#' + key.toString();//"CacheKey#" + type.toString(key, sf);
	}

	@Override
	public boolean equals(Object other) {
		if ( !(other instanceof CacheKey) ) {
			return false;
		}
		CacheKey that = (CacheKey) other;
		return entityOrRoleName.equals( that.entityOrRoleName ) &&
				type.isEqual( key, that.key, entityMode ) &&
				EqualsHelper.equals( tenantId, that.tenantId );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public Serializable getKey() {
		return key;
	}

	public String getEntityOrRoleName() {
		return entityOrRoleName;
	}

}
