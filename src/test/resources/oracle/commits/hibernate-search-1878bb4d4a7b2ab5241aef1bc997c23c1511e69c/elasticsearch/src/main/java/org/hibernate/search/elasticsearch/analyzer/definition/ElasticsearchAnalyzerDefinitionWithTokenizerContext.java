/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition;


/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchAnalyzerDefinitionWithTokenizerContext {

	/**
	 * Set the char filters that the analyzer will use.
	 *
	 * @param names The name of each char filters to use, in order.
	 * There must be a corresponding char filter definition on the Elasticsearch server.
	 * This can be achieved by defining the char filter
	 * {@link ElasticsearchAnalysisDefinitionRegistryBuilder#charFilter(String) from Hibernate Search},
	 * by configuring the Elasticsearch server directly, or by using built-in tokenizers.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchAnalyzerDefinitionWithTokenizerContext withCharFilters(String... names);

	/**
	 * Set the token filters that the analyzer will use.
	 *
	 * @param names The name of the token filters to use, in order.
	 * There must be a corresponding token filter definition on the Elasticsearch server.
	 * This can be achieved by defining the token filter
	 * {@link ElasticsearchAnalysisDefinitionRegistryBuilder#tokenFilter(String) from Hibernate Search},
	 * by configuring the Elasticsearch server, or by using built-in tokenizers.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchAnalyzerDefinitionWithTokenizerContext withTokenFilters(String... names);

}
