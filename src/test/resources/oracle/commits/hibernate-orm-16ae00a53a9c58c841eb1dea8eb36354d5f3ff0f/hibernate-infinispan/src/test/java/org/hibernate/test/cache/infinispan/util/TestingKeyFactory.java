/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import java.io.Serializable;

import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.EntityCacheKey;

public class TestingKeyFactory {

	private TestingKeyFactory() {
		//Not to be constructed
	}

	public static EntityCacheKey generateEntityCacheKey(String id) {
		return new TestingEntityCacheKey( id );
	}

	public static CollectionCacheKey generateCollectionCacheKey(String id) {
		return new TestingEntityCacheKey( id );
	}

	//For convenience implement both interfaces.
	private static class TestingEntityCacheKey implements EntityCacheKey, CollectionCacheKey, Serializable {

		private final String id;

		public TestingEntityCacheKey(String id) {
			this.id = id;
		}

		@Override
		public Serializable getKey() {
			return null;
		}

		@Override
		public String getEntityName() {
			return null;
		}

		@Override
		public String getCollectionRole() {
			return null;
		}

		@Override
		public String getTenantId() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestingEntityCacheKey other = (TestingEntityCacheKey) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

	}

}
