/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public class SimpleLuceneAnalyzerReference extends LuceneAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private Analyzer analyzer;

	public SimpleLuceneAnalyzerReference(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public Analyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyLuceneAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize(Analyzer analyzer) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "An analyzer reference has been initialized more than once:" + this );
		}
		this.analyzer = analyzer;
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( SimpleLuceneAnalyzerReference.class );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( analyzer );
		sb.append( ">" );
		return sb.toString();
	}
}
