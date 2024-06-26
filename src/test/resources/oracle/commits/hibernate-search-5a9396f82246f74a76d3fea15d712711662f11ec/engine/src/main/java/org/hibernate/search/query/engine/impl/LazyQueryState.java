/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
* @author Sanne Grinovero
*/
public final class LazyQueryState implements Closeable {

	private static final Log log = LoggerFactory.make();

	private final Query userQuery;
	private final IndexSearcher searcher;
	private final boolean fieldSortDoTrackScores;
	private final boolean fieldSortDoMaxScore;

	private Query rewrittenQuery;
	private Weight queryWeight;

	public LazyQueryState(Query userQuery, IndexReader reader, Similarity searcherSimilarity, boolean fieldSortDoTrackScores, boolean fieldSortDoMaxScore) {
		this.userQuery = userQuery;
		this.fieldSortDoTrackScores = fieldSortDoTrackScores;
		this.fieldSortDoMaxScore = fieldSortDoMaxScore;
		this.searcher = new IndexSearcher( reader );
		this.searcher.setSimilarity( searcherSimilarity );
	}

	public boolean isFieldSortDoTrackScores() {
		return fieldSortDoTrackScores;
	}

	public boolean isFieldSortDoMaxScore() {
		return fieldSortDoMaxScore;
	}

	public boolean scoresDocsOutOfOrder() throws IOException {
		return getWeight().scoresDocsOutOfOrder();
	}

	public Explanation explain(int documentId) throws IOException {
		return searcher.explain( rewrittenQuery(), documentId );
	}

	public Explanation explain(Query filteredQuery, int documentId) throws IOException {
		return searcher.explain( filteredQuery, documentId );
	}

	public Document doc(final int docId) throws IOException {
		return searcher.doc( docId );
	}

	public void doc(final int docId, final StoredFieldVisitor fieldVisitor) throws IOException {
		searcher.doc( docId, fieldVisitor );
	}

	public int maxDoc() {
		//pointless to try caching this one
		return searcher.getIndexReader().maxDoc();
	}

	public void search(final Filter filter, final Collector collector) throws IOException {
		searcher.search( rewrittenQuery(), filter, collector );
	}

	private Weight getWeight() throws IOException {
		if ( queryWeight == null ) {
			queryWeight = rewrittenQuery().createWeight( searcher );
		}
		return queryWeight;
	}

	private Query rewrittenQuery() throws IOException {
		if ( rewrittenQuery == null ) {
			rewrittenQuery = userQuery.rewrite( searcher.getIndexReader() );
		}
		return rewrittenQuery;
	}

	@Override
	public void close() {
		final IndexReader indexReader = searcher.getIndexReader();
		try {
			MultiReaderFactory.closeReader( indexReader );
		}
		catch (SearchException e) {
			log.unableToCloseSearcherDuringQuery( userQuery.toString(), e );
		}
	}

	public String describeQuery() {
		return userQuery.toString();
	}

}
