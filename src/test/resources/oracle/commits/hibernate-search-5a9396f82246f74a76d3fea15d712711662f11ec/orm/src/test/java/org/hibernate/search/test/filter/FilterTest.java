/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import java.util.Calendar;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeFilter;

import org.hibernate.Session;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class FilterTest extends SearchTestBase {
	private BooleanQuery query;
	private FullTextSession fullTextSession;

	@Test
	public void testNamedFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.disableFullTextFilter( "bestDriver" ); //was not enabled, but should be harmless
		ftQuery.enableFullTextFilter( "bestDriver" );
		assertEquals( "Should filter out Gavin", 2, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		assertEquals( "Should filter to limit to Emmanuel", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		ftQuery.disableFullTextFilter( "security" );
		ftQuery.disableFullTextFilter( "bestDriver" );
		assertEquals( "Should not filter anymore", 3, ftQuery.getResultSize() );
	}

	@Test
	public void testCache() {
		InstanceBasedExcludeAllFilter.assertConstructorInvoked( 1 ); // SearchFactory tests filter construction once
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		assertEquals( "No filter should happen", 3, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheresultstest" );
		assertEquals( "Should filter out all", 0, ftQuery.getResultSize() );

		// HSEARCH-174 - we call System.gc() to force a garbage collection.
		// Prior to the fix for HSEARCH-174 this would cause the filter to be
		// garbage collected since Lucene used weak references.
		System.gc();

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheresultstest" );
		try {
			ftQuery.getResultSize();
		}
		catch (IllegalStateException e) {
			fail( "Cache results does not work" );
		}

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancetest" );
		InstanceBasedExcludeAllFilter.assertConstructorInvoked( 1 );
		assertEquals( "Should filter out all", 0, ftQuery.getResultSize() );
		InstanceBasedExcludeAllFilter.assertConstructorInvoked( 2 ); // HSEARCH-818 : would be even better if it was still at 1 here, reusing what was created at SearchFactory build time

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "cacheinstancetest" );
		ftQuery.getResultSize();
//		InstanceBasedExcludeAllFilter.assertConstructorInvoked( 2 ); //uncomment this when solving HSEARCH-818
	}

	@Test
	public void testStraightFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		Filter dateFilter = TermRangeFilter.newStringRange( "delivery", "2001", "2005", true, true );
		ftQuery.setFilter( dateFilter );
		assertEquals( "Should select only liz", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "empty" );
		assertEquals( "two filters, one is empty, should not match anything", 0, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.setFilter( dateFilter );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "security" ).setParameter( "login", "andre" );
		ftQuery.disableFullTextFilter( "security" );
		ftQuery.disableFullTextFilter( "bestDriver" );
		ftQuery.setFilter( null );
		assertEquals( "Should not filter anymore", 3, ftQuery.getResultSize() );
	}

	@TestForIssue(jiraKey = "HSEARCH-1513")
	@Test
	public void testCachedEmptyFilters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		Filter dateFilter = TermRangeFilter.newStringRange( "delivery", "2001", "2005", true, true );
		ftQuery.setFilter( dateFilter );
		assertEquals( "Should select only liz", 1, ftQuery.getResultSize() );

		ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "bestDriver" );
		ftQuery.enableFullTextFilter( "cached_empty" );
		assertEquals( "two filters, one is empty, should not match anything", 0, ftQuery.getResultSize() );
	}

	@Test
	public void testMultipleFiltersOfSameTypeWithDifferentParameters() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		ftQuery.enableFullTextFilter( "fieldConstraintFilter-1" )
				.setParameter( "field", "teacher" )
				.setParameter( "value", "andre" );
		ftQuery.enableFullTextFilter( "fieldConstraintFilter-2" )
				.setParameter( "field", "teacher" )
				.setParameter( "value", "aaron" );
		assertEquals( "Should apply both filters resulting in 0 results", 0, ftQuery.getResultSize() );
	}

	@Test
	public void testUnknownFilterNameThrowsException() {
		FullTextQuery ftQuery = fullTextSession.createFullTextQuery( query, Driver.class );
		try {
			ftQuery.enableFullTextFilter( "foo" );
			fail( "Retrieving an unknown filter should throw a SearchException" );
		}
		catch (SearchException e) {
			assertEquals( "Wrong message", "HSEARCH000115: Unknown @FullTextFilter: 'foo'", e.getMessage() );
		}
	}

	private void createData() {
		Session s = openSession();
		s.getTransaction().begin();
		Calendar cal = Calendar.getInstance();
		cal.set( 2006, 10, 11 );
		Driver driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 1 );
		driver.setName( "Emmanuel" );
		driver.setScore( 5 );
		driver.setTeacher( "andre" );
		s.persist( driver );

		cal.set( 2007, 10, 11 );
		driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 2 );
		driver.setName( "Gavin" );
		driver.setScore( 3 );
		driver.setTeacher( "aaron" );
		s.persist( driver );

		cal.set( 2004, 10, 11 );
		driver = new Driver();
		driver.setDelivery( cal.getTime() );
		driver.setId( 3 );
		driver.setName( "Liz" );
		driver.setScore( 5 );
		driver.setTeacher( "max" );
		s.persist( driver );
		s.getTransaction().commit();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		createData();
		query = createQuery();
		fullTextSession = Search.getFullTextSession( openSession() );
		fullTextSession.getTransaction().begin();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		fullTextSession.getTransaction().commit();
		fullTextSession.close();
		super.tearDown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Driver.class,
				Soap.class
		};
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.filter.cache_docidresults.size", "10" );
		InstanceBasedExcludeAllFilter.reset();
	}

	private BooleanQuery createQuery() {
		BooleanQuery query = new BooleanQuery();
		query.add( new TermQuery( new Term( "teacher", "andre" ) ), BooleanClause.Occur.SHOULD );
		query.add( new TermQuery( new Term( "teacher", "max" ) ), BooleanClause.Occur.SHOULD );
		query.add( new TermQuery( new Term( "teacher", "aaron" ) ), BooleanClause.Occur.SHOULD );
		return query;
	}
}
