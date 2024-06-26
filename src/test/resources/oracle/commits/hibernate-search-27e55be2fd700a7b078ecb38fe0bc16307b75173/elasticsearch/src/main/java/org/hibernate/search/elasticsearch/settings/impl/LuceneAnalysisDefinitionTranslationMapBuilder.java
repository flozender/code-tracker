/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A utility that helps {@link DefaultElasticsearchAnalyzerDefinitionTranslator} build its translation maps.
 *
 * @author Yoann Rodiere
 */
class LuceneAnalysisDefinitionTranslationMapBuilder<D extends AnalysisDefinition> {

	private final Class<D> targetClass;
	private final Map<String, AnalysisDefinitionFactory<D>> result = new HashMap<>();

	public LuceneAnalysisDefinitionTranslationMapBuilder(Class<D> targetClass) {
		this.targetClass = targetClass;
	}

	public SimpleAnalysisDefinitionFactoryBuilder<D> builder(Class<? extends AbstractAnalysisFactory> luceneClass, String typeName) {
		return new SimpleAnalysisDefinitionFactoryBuilder<>( this, luceneClass, typeName );
	}

	public LuceneAnalysisDefinitionTranslationMapBuilder<D> add(Class<? extends AbstractAnalysisFactory> luceneClass,
			AnalysisDefinitionFactory<D> definitionFactory) {
		result.put( luceneClass.getName(), definitionFactory );
		return this;
	}

	public Map<String, AnalysisDefinitionFactory<D>> build() {
		return Collections.unmodifiableMap( result );
	}

	static class SimpleAnalysisDefinitionFactoryBuilder<D extends AnalysisDefinition> {

		private final LuceneAnalysisDefinitionTranslationMapBuilder<D> parent;
		private final Class<? extends AbstractAnalysisFactory> luceneClass;
		private final String typeName;

		private Map<String, String> parameterNameTranslations;
		private Map<String, ParameterValueTransformer> parameterValueTranslations;
		private Map<String, JsonElement> staticParameters;

		private SimpleAnalysisDefinitionFactoryBuilder(LuceneAnalysisDefinitionTranslationMapBuilder<D> parent, Class<? extends AbstractAnalysisFactory> luceneClass, String typeName) {
			super();
			this.parent = parent;
			this.luceneClass = luceneClass;
			this.typeName = typeName;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> add(String elasticsearchParam, String value) {
			if ( staticParameters == null ) {
				staticParameters = new LinkedHashMap<>();
			}
			staticParameters.put( elasticsearchParam, new JsonPrimitive( value ) );
			return this;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> disallow(String luceneParam) {
			return transform( luceneParam, new ThrowingUnsupportedParameterValueTransformer( luceneClass, luceneParam ) );
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> rename(String luceneParam, String elasticsearchParam) {
			if ( parameterNameTranslations == null ) {
				parameterNameTranslations = new HashMap<>();
			}
			parameterNameTranslations.put( luceneParam, elasticsearchParam );
			return this;
		}

		public MapParameterValueTransformerBuilder<D> transform(String luceneParam) {
			return new MapParameterValueTransformerBuilder<>( this, luceneClass, luceneParam );
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> transform(String luceneParam, ParameterValueTransformer transformer) {
			if ( parameterValueTranslations == null ) {
				parameterValueTranslations = new HashMap<>();
			}
			parameterValueTranslations.put( luceneParam, transformer );
			return this;
		}

		public LuceneAnalysisDefinitionTranslationMapBuilder<D> end() {
			if ( parameterNameTranslations == null ) {
				parameterNameTranslations = Collections.emptyMap();
			}
			if ( parameterValueTranslations == null ) {
				parameterValueTranslations = Collections.emptyMap();
			}
			if ( staticParameters == null ) {
				staticParameters = Collections.emptyMap();
			}
			return parent.add( luceneClass, new SimpleAnalysisDefinitionFactory<>(
					parent.targetClass, typeName,
					parameterNameTranslations, parameterValueTranslations, staticParameters ) );
		}

	}

	static class MapParameterValueTransformerBuilder<D extends AnalysisDefinition> {
		private final SimpleAnalysisDefinitionFactoryBuilder<D> parent;

		private final Class<?> factoryClass;
		private final String parameterName;

		private final Map<String, JsonElement> translations = new HashMap<>();

		private MapParameterValueTransformerBuilder(SimpleAnalysisDefinitionFactoryBuilder<D> parent, Class<?> factoryClass, String parameterName) {
			super();
			this.parent = parent;
			this.factoryClass = factoryClass;
			this.parameterName = parameterName;
		}

		public MapParameterValueTransformerBuilder<D> add(String luceneValue, String elasticsearchValue) {
			return add( luceneValue, new JsonPrimitive( elasticsearchValue ) );
		}

		public MapParameterValueTransformerBuilder<D> add(String luceneValue, JsonElement elasticsearchValue) {
			translations.put( luceneValue, elasticsearchValue );
			return this;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> end() {
			return parent.transform( parameterName, new MapParameterValueTransformer( factoryClass, parameterName, translations ) );
		}

	}
}
