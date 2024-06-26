/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.searchfactory;

import java.util.Set;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class SearchFactoryTest {

	@Test
	public void testTypeWithNoDocumentIdThrowsException() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
		;
		cfg.setProgrammaticMapping( mapping );

		try {
			new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
			fail( "Invalid configuration should have thrown an exception" );
		}
		catch (SearchException e) {
			assertTrue( e.getMessage().startsWith( "HSEARCH000177" ) );
		}
	}

	@Test
	public void testGetIndexedTypesNoTypeIndexed() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		Set<Class<?>> indexedClasses = sf.getIndexedTypes();
		assertEquals( "Wrong number of indexed entities", 0, indexedClasses.size() );
	}

	@Test
	public void testGetIndexedTypeSingleIndexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		Set<Class<?>> indexedClasses = sf.getIndexedTypes();
		assertEquals( "Wrong number of indexed entities", 1, indexedClasses.size() );
		assertTrue( indexedClasses.iterator().next().equals( Foo.class ) );
	}

	@Test
	public void testGetIndexedTypesMultipleTypes() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
				.entity( Bar.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		Set<Class<?>> indexedClasses = sf.getIndexedTypes();
		assertEquals( "Wrong number of indexed entities", 2, indexedClasses.size() );
	}

	@Test
	public void testGetTypeDescriptorForUnindexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		IndexedTypeDescriptor indexedTypeDescriptor = sf.getIndexedTypeDescriptor( Foo.class);
		assertNotNull( indexedTypeDescriptor );
		assertFalse( indexedTypeDescriptor.isIndexed() );
	}

	@Test
	public void testGetTypeDescriptorForIndexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		IndexedTypeDescriptor indexedTypeDescriptor = sf.getIndexedTypeDescriptor( Foo.class);
		assertNotNull( indexedTypeDescriptor );
		assertTrue( indexedTypeDescriptor.isIndexed() );
	}

	private SearchConfigurationForTest getManualConfiguration() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Foo.class );
		cfg.addClass( Bar.class );
		return cfg;
	}

	public static class Foo {
		private long id;
	}

	public static class Bar {
		private long id;
	}
}


