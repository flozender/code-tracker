//$Id$
package org.hibernate.search.test.query.boost;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.LoggerFactory;

public class DynamicBoostingTest extends SearchTestCase {

	private static final Logger log = LoggerFactory.make();

	public void testDynamicBoosts() throws Exception {

		Session session = openSession();
		session.beginTransaction();

		DynamicBoostedDescriptionLibrary lib1 = new DynamicBoostedDescriptionLibrary();
		lib1.setName( "one" );
		session.persist( lib1 );

		DynamicBoostedDescriptionLibrary lib2 = new DynamicBoostedDescriptionLibrary();
		lib2.setName( "two" );
		session.persist( lib2 );

		session.getTransaction().commit();
		session.close();

		float lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		float lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertEquals( "The scores should be equal", lib1Score, lib2Score );

		// set dynamic score and reindex!
		session = openSession();
		session.beginTransaction();

		session.refresh( lib2 );
		lib2.setDynScore( 2.0f );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib2score should be greater than lib1score", lib1Score < lib2Score );



		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		assertEquals( "lib1score should be 0 since term is not yet indexed.", 0.0f, lib1Score );

		// index foobar
		session = openSession();
		session.beginTransaction();

		session.refresh( lib1 );
		lib1.setName( "foobar" );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib1score should be greater than lib2score", lib1Score > lib2Score );
	}

	private float getScore(Query query) {
		Session session = openSession();
		Object[] queryResult;
		float score;
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			List resultList = fullTextSession
					.createFullTextQuery( query, DynamicBoostedDescriptionLibrary.class )
					.setProjection( ProjectionConstants.SCORE, ProjectionConstants.EXPLANATION )
					.setMaxResults( 1 )
					.list();

			if ( resultList.size() == 0 ) {
				score = 0.0f;
			}
			else {
				queryResult = ( Object[] ) resultList.get( 0 );
				score = ( Float ) queryResult[0];
				String explanation = queryResult[1].toString();
				log.debug( "score: " + score + " explanation: " + explanation );
			}
		}
		finally {
			session.close();
		}
		return score;
	}

	protected Class[] getMappings() {
		return new Class[] {
				DynamicBoostedDescriptionLibrary.class
		};
	}
}
