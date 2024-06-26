/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdef;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.engine.impl.AnalyzerRegistry;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Test that the {@link org.hibernate.search.annotations.AnalyzerDef} annotation can be read by the engine
 * in all the valid locations.
 *
 * @author Davide D'Alto
 */
public class AnalyzerDefAnnotationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void shouldBePossibleToAnnotatePackage() throws Exception {
		assertAnalyzerExists( "package-analyzer" );
	}

	@Test
	public void shouldBePossibleToAnnotateClass() throws Exception {
		assertAnalyzerExists( "class-analyzer" );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // Unused AnalyzerDefs are always bound to the Lucene registry, making this test fail on ES
	public void shouldContainOnlyTheDefinedAnalyzers() throws Exception {
		ExtendedSearchIntegrator factory = sfHolder.getSearchFactory();
		IndexManagerType indexManagerType = factory.getIndexBinding( Sample.class ).getIndexManagerType();
		Map<String, AnalyzerReference> analyzerReferences =
				factory.getIntegration( indexManagerType ).getAnalyzerRegistry().getNamedAnalyzerReferences();
		assertThat( analyzerReferences.keySet() ).containsOnly( "package-analyzer", "class-analyzer" );
	}

	@Test
	public void shouldNotBePossibleToHaveTwoAnalyzerDefsWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );

		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithError.class );
		integratorResource.create( cfg );
	}

	private void assertAnalyzerExists(String analyzerName) {
		for ( SearchIntegration integration : sfHolder.getSearchFactory().getIntegrations().values() ) {
			AnalyzerRegistry registry = integration.getAnalyzerRegistry();
			AnalyzerReference analyzerReference = registry.getAnalyzerReference( analyzerName );
			if ( analyzerReference != null ) {
				if ( analyzerReference.is( LuceneAnalyzerReference.class ) ) {
					assertThat( analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer() ).isNotNull();
				}
				return;
			}
		}
		Assert.fail( "Analyzer does not exist: " + analyzerName );
	}

	@Indexed
	@AnalyzerDef(
			name = "class-analyzer",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
	)
	static class Sample {

		@DocumentId
		long id;

		@Field
		String description;
	}

	@Indexed
	@AnalyzerDef(
			name = "package-analyzer",
			tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
	)
	static class SampleWithError {

		@DocumentId
		final long id = 1;

		@Field
		final String description = "";
	}
}
