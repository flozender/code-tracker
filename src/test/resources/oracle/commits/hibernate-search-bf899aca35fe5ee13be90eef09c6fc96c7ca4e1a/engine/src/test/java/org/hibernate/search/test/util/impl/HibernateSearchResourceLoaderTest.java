/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.search.test.util.impl;

import java.io.InputStream;

import org.hibernate.search.SearchException;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.HibernateSearchResourceLoader;
import org.hibernate.search.util.impl.StreamHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class HibernateSearchResourceLoaderTest {

	private HibernateSearchResourceLoader resourceLoader;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		BuildContext buildContext = new BuildContextForTest( searchConfiguration );
		ServiceManager serviceManager = new StandardServiceManager( searchConfiguration, buildContext );
		resourceLoader = new HibernateSearchResourceLoader( serviceManager );
	}

	@Test
	public void testOpenKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "log4j.properties";
		InputStream in = resourceLoader.openResource( resource );
		String resourceContent = StreamHelper.readInputStream( in );
		assertNotNull( resourceContent );
		assertFalse( resourceContent.isEmpty() );
	}

	@Test
	public void testUnKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "foo";
		try {
			resourceLoader.openResource( resource );
		}
		catch (SearchException e) {
			assertEquals( "Wrong error message", "HSEARCH000114: Could not load resource: 'foo'", e.getMessage() );
		}
	}
}
