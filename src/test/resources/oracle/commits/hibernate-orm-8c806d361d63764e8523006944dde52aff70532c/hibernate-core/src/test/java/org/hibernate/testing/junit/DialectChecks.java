/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

package org.hibernate.testing.junit;
import org.hibernate.dialect.Dialect;

/**
 * Container class for different implementation of the {@code DialectCheck} interface.
 *
 * @author Hardy Ferentschik
 */
abstract public class DialectChecks {

	abstract public boolean include(Dialect dialect);

	public static class SupportsSequences extends DialectChecks {
		public boolean include(Dialect dialect) {
			return dialect.supportsSequences();
		}
	}

	public static class SupportsExpectedLobUsagePattern extends DialectChecks {
		public boolean include(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern();
		}
	}

	public static class SupportsIdentityColumns extends DialectChecks {
		public boolean include(Dialect dialect) {
			return dialect.supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck extends DialectChecks {
		public boolean include(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}
}


