/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * @author Steve Ebersole
 */
public interface TimestampsRegion extends DirectAccessRegion {
	@Override
	default void clear() {
		getAccess().clearCache();
	}
}
