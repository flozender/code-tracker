/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.querycache;
import junit.framework.Test;
import org.hibernate.CacheMode;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCacheIgnoreResultTransformerTest extends AbstractQueryCacheResultTransformerTest {

	public CriteriaQueryCacheIgnoreResultTransformerTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CriteriaQueryCacheIgnoreResultTransformerTest.class );
	}

	protected CacheMode getQueryCacheMode() {
		return CacheMode.IGNORE;
	}

	protected void runTest(HqlExecutor hqlExecutor, CriteriaExecutor criteriaExecutor, ResultChecker checker, boolean isSingleResult)
		throws Exception {
		createData();
		try {
			if ( criteriaExecutor != null ) {
				runTest( criteriaExecutor, checker, isSingleResult );
			}
		}
		finally {
			deleteData();
		}
	}
}
