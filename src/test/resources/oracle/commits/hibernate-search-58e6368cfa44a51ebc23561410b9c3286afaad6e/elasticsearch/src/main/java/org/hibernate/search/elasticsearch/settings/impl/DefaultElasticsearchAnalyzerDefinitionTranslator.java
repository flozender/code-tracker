/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilterFactory;
import org.apache.lucene.analysis.ar.ArabicStemFilterFactory;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianStemFilterFactory;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.br.BrazilianStemFilterFactory;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cjk.CJKBigramFilterFactory;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.ckb.SoraniAnalyzer;
import org.apache.lucene.analysis.ckb.SoraniNormalizationFilterFactory;
import org.apache.lucene.analysis.ckb.SoraniStemFilterFactory;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.compound.DictionaryCompoundWordTokenFilterFactory;
import org.apache.lucene.analysis.compound.HyphenationCompoundWordTokenFilterFactory;
import org.apache.lucene.analysis.core.DecimalDigitFilterFactory;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LetterTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseTokenizerFactory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.UpperCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.cz.CzechStemFilterFactory;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.de.GermanLightStemFilterFactory;
import org.apache.lucene.analysis.de.GermanMinimalStemFilterFactory;
import org.apache.lucene.analysis.de.GermanNormalizationFilterFactory;
import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.el.GreekStemFilterFactory;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.es.SpanishLightStemFilterFactory;
import org.apache.lucene.analysis.eu.BasqueAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fa.PersianNormalizationFilterFactory;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fi.FinnishLightStemFilterFactory;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchLightStemFilterFactory;
import org.apache.lucene.analysis.fr.FrenchMinimalStemFilterFactory;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.gl.GalicianMinimalStemFilterFactory;
import org.apache.lucene.analysis.gl.GalicianStemFilterFactory;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hi.HindiNormalizationFilterFactory;
import org.apache.lucene.analysis.hi.HindiStemFilterFactory;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.hu.HungarianLightStemFilterFactory;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianStemFilterFactory;
import org.apache.lucene.analysis.in.IndicNormalizationFilterFactory;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.it.ItalianLightStemFilterFactory;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeepWordFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilterFactory;
import org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ScandinavianFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ScandinavianNormalizationFilterFactory;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TruncateTokenFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenizerFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.no.NorwegianLightStemFilterFactory;
import org.apache.lucene.analysis.no.NorwegianMinimalStemFilterFactory;
import org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternCaptureGroupFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilterFactory;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilterFactory;
import org.apache.lucene.analysis.pt.PortugueseMinimalStemFilterFactory;
import org.apache.lucene.analysis.pt.PortugueseStemFilterFactory;
import org.apache.lucene.analysis.reverse.ReverseStringFilterFactory;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.ru.RussianLightStemFilterFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.sr.SerbianNormalizationFilterFactory;
import org.apache.lucene.analysis.standard.ClassicFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.sv.SwedishLightStemFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.th.ThaiTokenizerFactory;
import org.apache.lucene.analysis.tr.ApostropheFilterFactory;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.ElisionFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchAnalyzerDefinitionTranslator implements ElasticsearchAnalyzerDefinitionTranslator, Startable, Stoppable {

	static final Log LOG = LoggerFactory.make( Log.class );

	private Map<String, String> luceneAnalyzers;

	private Map<String, AnalysisDefinitionFactory<CharFilterDefinition>> luceneCharFilters;

	private Map<String, AnalysisDefinitionFactory<TokenizerDefinition>> luceneTokenizers;

	private Map<String, AnalysisDefinitionFactory<TokenFilterDefinition>> luceneTokenFilters;

	@Override
	public void start(Properties properties, BuildContext context) {
		luceneAnalyzers = new LuceneAnalyzerImplementationTranslationMapBuilder()
				.add( StandardAnalyzer.class, "standard" )
				.add( SimpleAnalyzer.class, "simple" )
				.add( WhitespaceAnalyzer.class, "whitespace" )
				.add( StopAnalyzer.class, "stop" )
				.add( KeywordAnalyzer.class, "keyword" )
				.add( ArabicAnalyzer.class, "arabic" )
				.add( ArmenianAnalyzer.class, "armenian" )
				.add( BasqueAnalyzer.class, "basque" )
				.add( BrazilianAnalyzer.class, "brazilian" )
				.add( BulgarianAnalyzer.class, "bulgarian" )
				.add( CatalanAnalyzer.class, "catalan" )
				.add( CJKAnalyzer.class, "cjk" )
				.add( CzechAnalyzer.class, "czech" )
				.add( DanishAnalyzer.class, "danish" )
				.add( DutchAnalyzer.class, "dutch" )
				.add( EnglishAnalyzer.class, "english" )
				.add( FinnishAnalyzer.class, "finnish" )
				.add( FrenchAnalyzer.class, "french" )
				.add( GalicianAnalyzer.class, "galician" )
				.add( GermanAnalyzer.class, "german" )
				.add( GreekAnalyzer.class, "greek" )
				.add( HindiAnalyzer.class, "hindi" )
				.add( HungarianAnalyzer.class, "hungarian" )
				.add( IndonesianAnalyzer.class, "indonesian" )
				.add( IrishAnalyzer.class, "irish" )
				.add( ItalianAnalyzer.class, "italian" )
				.add( LatvianAnalyzer.class, "latvian" )
				.add( LithuanianAnalyzer.class, "lithuanian" )
				.add( NorwegianAnalyzer.class, "norwegian" )
				.add( PersianAnalyzer.class, "persian" )
				.add( PortugueseAnalyzer.class, "portuguese" )
				.add( RomanianAnalyzer.class, "romanian" )
				.add( RussianAnalyzer.class, "russian" )
				.add( SoraniAnalyzer.class, "sorani" )
				.add( SpanishAnalyzer.class, "spanish" )
				.add( SwedishAnalyzer.class, "swedish" )
				.add( TurkishAnalyzer.class, "turkish" )
				.add( ThaiAnalyzer.class, "thai" )
				.build();

		luceneCharFilters = new LuceneAnalysisDefinitionTranslationMapBuilder<>( CharFilterDefinition.class )
				.builder( MappingCharFilterFactory.class, "mapping" ) // "mappings" (unmapped) is an array
						.rename( "mapping", "mappings_path" ) // Only accepts one file, no support for comma-separated list
				.end()
				.builder( HTMLStripCharFilterFactory.class, "html_strip" )
						.rename( "escapedTags", "escaped_tags" )
						.transform( "escapedTags", new SplitArrayParameterValueTransformer( "[\\s,]+", StringParameterValueTransformer.INSTANCE ) )
				.end()
				.builder( PatternReplaceCharFilterFactory.class, "pattern_replace" ).end()
				.build();

		luceneTokenizers = new LuceneAnalysisDefinitionTranslationMapBuilder<>( TokenizerDefinition.class )
				.builder( StandardTokenizerFactory.class, "standard" )
						.rename( "maxTokenLength", "max_token_length" )
				.end()
				.builder( EdgeNGramTokenizerFactory.class, "edgeNGram" ) // "token_chars" is an array of strings
						.rename( "minGramSize", "min_gram" )
						.rename( "maxGramSize", "max_gram" )
				.end()
				.builder( KeywordTokenizerFactory.class, "keyword" ).end()
				.builder( LetterTokenizerFactory.class, "letter" ).end()
				.builder( LowerCaseTokenizerFactory.class, "lowercase" ).end()
				.builder( NGramTokenizerFactory.class, "nGram" ) // "token_chars" is an array of strings
						.rename( "minGramSize", "min_gram" )
						.rename( "maxGramSize", "max_gram" )
				.end()
				.builder( WhitespaceTokenizerFactory.class, "whitespace" )
						.disallow( "rule" )
				.end()
				.builder( PatternTokenizerFactory.class, "pattern" ).end()
				.builder( UAX29URLEmailTokenizerFactory.class, "uax_url_email" )
						.rename( "maxTokenLength", "max_token_length" )
				.end()
				.builder( PathHierarchyTokenizerFactory.class, "path_hierarchy" )
						.rename( "replace", "replacement" )
				.end()
				.builder( ClassicTokenizerFactory.class, "classic" )
						.rename( "maxTokenLength", "max_token_length" )
				.end()
				.builder( ThaiTokenizerFactory.class, "thai" ).end()
				.build();

		luceneTokenFilters = new LuceneAnalysisDefinitionTranslationMapBuilder<>( TokenFilterDefinition.class )
				.builder( StandardFilterFactory.class, "standard" ).end()
				.builder( ASCIIFoldingFilterFactory.class, "asciifolding" )
						.rename( "preserveOriginal", "preserve_original" )
				.end()
				.builder( LengthFilterFactory.class, "length" ).end()
				.builder( LowerCaseFilterFactory.class, "lowercase" ).end()
				.builder( UpperCaseFilterFactory.class, "uppercase" ).end()
				.builder( NGramFilterFactory.class, "nGram" )
						.rename( "minGramSize", "min_gram" )
						.rename( "maxGramSize", "max_gram" )
				.end()
				.builder( EdgeNGramFilterFactory.class, "edgeNGram" )
						.rename( "minGramSize", "min_gram" )
						.rename( "maxGramSize", "max_gram" )
				.end()
				.builder( PorterStemFilterFactory.class, "porter_stem" ).end()
				.builder( ShingleFilterFactory.class, "shingle" )
						.rename( "minShingleSize", "max_shingle_size" )
						.rename( "maxShingleSize", "min_shingle_size" )
						.rename( "outputUnigrams", "output_unigrams" )
						.rename( "outputUnigramsIfNoShingles", "output_unigrams_if_no_shingles" )
						.rename( "tokenSeparator", "token_separator" )
						.rename( "fillerToken", "filler_token" )
				.end()
				.builder( StopFilterFactory.class, "stop" ) // "stopwords" array (or string), "stopwords_path" file path
						.rename( "words", "stopwords_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "ignoreCase", "ignore_case" )
						.disallow( "format" )
						.disallow( "enablePositionIncrements" )
				.end()
				.builder( WordDelimiterFilterFactory.class, "word_delimiter" ) // "protected_words" array, "protected_words_path" file path, "type_table" array, "type_table_path" file path
						.rename( "generateWordParts", "generate_word_parts" )
						.rename( "generateNumberParts", "generate_number_parts" )
						.rename( "catenateWords", "catenate_words" )
						.rename( "catenateNumbers", "catenate_numbers" )
						.rename( "catenateAll", "catenate_all" )
						.rename( "splitOnCaseChange", "split_on_case_change" )
						.rename( "splitOnNumerics", "split_on_numerics" )
						.rename( "preserveOriginal", "preserve_original" )
						.rename( "stemEnglishPossessive", "stem_english_possessive" )
						.rename( "protected", "protected_words_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "types", "type_table_path" ) // Only accepts one file, no support for comma-separated list
				.end()
				.builder( ArabicStemFilterFactory.class, "stemmer" ).add( "name", "armenian" ).end()
				.builder( BrazilianStemFilterFactory.class, "stemmer" ).add( "name", "brazilian" ).end()
				.builder( BulgarianStemFilterFactory.class, "stemmer" ).add( "name", "bulgarian" ).end()
				.builder( CzechStemFilterFactory.class, "stemmer" ).add( "name", "czech" ).end()
				.builder( EnglishMinimalStemFilterFactory.class, "stemmer" ).add( "name", "minimal_english" ).end()
				.builder( EnglishPossessiveFilterFactory.class, "stemmer" ).add( "name", "possessive_english" ).end()
				.builder( FinnishLightStemFilterFactory.class, "stemmer" ).add( "name", "light_finnish" ).end()
				.builder( FrenchLightStemFilterFactory.class, "stemmer" ).add( "name", "light_french" ).end()
				.builder( FrenchMinimalStemFilterFactory.class, "stemmer" ).add( "name", "minimal_french" ).end()
				.builder( GalicianStemFilterFactory.class, "stemmer" ).add( "name", "galician" ).end()
				.builder( GalicianMinimalStemFilterFactory.class, "stemmer" ).add( "name", "minimal_galician" ).end()
				.builder( GermanStemFilterFactory.class, "stemmer" ).add( "name", "german" ).end()
				.builder( GermanMinimalStemFilterFactory.class, "stemmer" ).add( "name", "minimal_german" ).end()
				.builder( GermanLightStemFilterFactory.class, "stemmer" ).add( "name", "light_german" ).end()
				.builder( GreekStemFilterFactory.class, "stemmer" ).add( "name", "greek" ).end()
				.builder( HindiStemFilterFactory.class, "stemmer" ).add( "name", "hindi" ).end()
				.builder( HungarianLightStemFilterFactory.class, "stemmer" ).add( "name", "light_hungarian" ).end()
				.builder( IndonesianStemFilterFactory.class, "stemmer" ).add( "name", "indonesian" ).end()
				.builder( ItalianLightStemFilterFactory.class, "stemmer" ).add( "name", "light_italian" ).end()
				.builder( SoraniStemFilterFactory.class, "stemmer" ).add( "name", "sorani" ).end()
				.builder( LatvianStemFilterFactory.class, "stemmer" ).add( "name", "latvian" ).end()
				.builder( NorwegianLightStemFilterFactory.class, "stemmer" )
						.add( "name", "light_norwegian" )
						.rename( "variant", "name" )
						.transform( "variant" )
								.add( "nb", "light_norwegian" )
								.add( "nn", "light_nynorsk" )
						.end()
				.end()
				.builder( NorwegianMinimalStemFilterFactory.class, "stemmer" )
						.add( "name", "minimal_norwegian" )
						.rename( "variant", "name" )
						.transform( "variant" )
								.add( "nb", "minimal_norwegian" )
								.add( "nn", "minimal_nynorsk" )
						.end()
				.end()
				.builder( PortugueseStemFilterFactory.class, "stemmer" ).add( "name", "portuguese_rslp" ).end()
				.builder( PortugueseLightStemFilterFactory.class, "stemmer" ).add( "name", "light_portuguese" ).end()
				.builder( PortugueseMinimalStemFilterFactory.class, "stemmer" ).add( "name", "minimal_portuguese" ).end()
				.builder( RussianLightStemFilterFactory.class, "stemmer" ).add( "name", "light_russian" ).end()
				.builder( SpanishLightStemFilterFactory.class, "stemmer" ).add( "name", "light_spanish" ).end()
				.builder( SwedishLightStemFilterFactory.class, "stemmer" ).add( "name", "light_swedish" ).end()
				.builder( StemmerOverrideFilterFactory.class, "stemmer_override" ) // "rules" array, "rules_path" file path
						.rename( "dictionary", "rules_path" ) // Only accepts one file, no support for comma-separated list
						.disallow( "ignoreCase" )
				.end()
				.builder( KeywordMarkerFilterFactory.class, "keyword_marker" ) // "keywords" array, "keywords_path" file path
						.rename( "protected", "keywords_path" ) // Only accepts one file, no support for comma-separated list
						.disallow( "pattern" )
						.rename( "ignoreCase", "ignore_case" )
				.end()
				.builder( KeywordRepeatFilterFactory.class, "keyword_repeat" ).end()
				.builder( KStemFilterFactory.class, "kstem" ).end()
				.builder( SnowballPorterFilterFactory.class, "snowball" )
						.disallow( "protected" )
				.end()
				.builder( SynonymFilterFactory.class, "synonym" ) // "synonyms" array, "synonyms_path" file path
						.rename( "ignoreCase", "ignore_case" )
						.rename( "synonyms", "synonyms_path" ) // Only accepts one file, no support for comma-separated list
						.disallow( "analyzer" )
						.rename( "tokenizerFactory", "tokenizer" )
						.transform( "tokenizerFactory",
								new TokenizerClassNameToElasticsearchTypeNameTransformer( SynonymFilterFactory.class, "tokenizerFactory" ) )
				.end()
				.builder( HyphenationCompoundWordTokenFilterFactory.class, "hyphenation_decompounder" )
						.rename( "dictionary", "word_list_path" ) // Only accepts one file, no support for comma-separated list
						.disallow( "encoding" )
						.rename( "hyphenator", "hyphenation_patterns_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "minWordSize", "min_word_size" )
						.rename( "minSubwordSize", "min_subword_size" )
						.rename( "maxSubwordSize", "max_subword_size" )
						.rename( "onlyLongestMatch", "only_longest_match" )
				.end()
				.builder( DictionaryCompoundWordTokenFilterFactory.class, "dictionary_decompounder" )
						.rename( "dictionary", "word_list_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "minWordSize", "min_word_size" )
						.rename( "minSubwordSize", "min_subword_size" )
						.rename( "maxSubwordSize", "max_subword_size" )
						.rename( "onlyLongestMatch", "only_longest_match" )
				.end()
				.builder( ReverseStringFilterFactory.class, "reverse" ).end()
				.builder( ElisionFilterFactory.class, "elision" )
						.rename( "articles", "articles_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "ignoreCase", "articles_case" )
				.end()
				.builder( TruncateTokenFilterFactory.class, "truncate" )
						.rename( "prefixLength", "length" )
				.end()
				.builder( PatternCaptureGroupFilterFactory.class, "pattern_capture" ) // "patterns" array
						.rename( "pattern", "patterns" )
						.transform( "pattern", new SingleElementArrayParameterValueTransformer( StringParameterValueTransformer.INSTANCE ) )
				.end()
				.builder( PatternReplaceFilterFactory.class, "pattern_replace" )
						.rename( "replace", "all" )
						.transform( "replace" )
								.add( "all", "true" )
								.add( "first", "false" )
						.end()
				.end()
				.builder( TrimFilterFactory.class, "trim" )
						.disallow( "updateOffsets" )
				.end()
				.builder( LimitTokenCountFilterFactory.class, "limit" )
						.rename( "maxTokenCount", "max_token_count" )
						.rename( "consumeAllTokens", "consume_all_tokens" )
				.end()
				.builder( CommonGramsFilterFactory.class, "common_grams" ) // "common_words" array, "common_words_path" file path
						.rename( "words", "common_words_path" ) // Only accepts one file, no support for comma-separated list
						.disallow( "format" )
						.rename( "ignoreCase", "ignore_case" )
				.end()
				.builder( ArabicNormalizationFilterFactory.class, "arabic_normalization" ).end()
				.builder( GermanNormalizationFilterFactory.class, "german_normalization" ).end()
				.builder( HindiNormalizationFilterFactory.class, "hindi_normalization" ).end()
				.builder( IndicNormalizationFilterFactory.class, "indic_normalization" ).end()
				.builder( SoraniNormalizationFilterFactory.class, "sorani_normalization" ).end()
				.builder( PersianNormalizationFilterFactory.class, "persian_normalization" ).end()
				.builder( ScandinavianNormalizationFilterFactory.class, "scandinavian_normalization" ).end()
				.builder( ScandinavianFoldingFilterFactory.class, "scandinavian_folding" ).end()
				.builder( SerbianNormalizationFilterFactory.class, "serbian_normalization" ).end()
				.builder( CJKWidthFilterFactory.class, "cjk_width" ).end()
				.add( CJKBigramFilterFactory.class, new CjkBigramTokenFilterDefinitionFactory() ) // "ignored_scripts" array
				.builder( DelimitedPayloadTokenFilterFactory.class, "delimited_payload_filter" )
						.rename( "encoder", "encoding" ) // custom class name is not supported
				.end()
				.builder( KeepWordFilterFactory.class, "keep" )
						.rename( "words", "keep_words_path" ) // Only accepts one file, no support for comma-separated list
						.rename( "ignoreCase", "keep_words_case" )
						.disallow( "enablePositionIncrements" )
				.end()
				.builder( ClassicFilterFactory.class, "classic" ).end()
				.builder( ApostropheFilterFactory.class, "apostrophe" ).end()
				.builder( DecimalDigitFilterFactory.class, "decimal_digit" ).end()
				.build();
	}

	@Override
	public void stop() {
		luceneAnalyzers = null;
		luceneCharFilters = null;
		luceneTokenizers = null;
		luceneTokenFilters = null;
	}

	@Override
	public String translate(Class<?> luceneClass) {
		String elasticsearchName = luceneAnalyzers.get( luceneClass.getName() );
		if ( elasticsearchName == null ) {
			throw LOG.unsupportedAnalyzerImplementation( luceneClass );
		}
		return elasticsearchName;
	}

	@Override
	public CharFilterDefinition translate(CharFilterDef hibernateSearchDef) {
		Class<? extends CharFilterFactory> factoryType = hibernateSearchDef.factory();
		AnalysisDefinitionFactory<CharFilterDefinition> factory = luceneCharFilters.get( factoryType.getName() );
		if ( factory == null ) {
			throw LOG.unsupportedCharFilterFactory( factoryType );
		}
		return factory.create( hibernateSearchDef.params() );
	}

	@Override
	public TokenizerDefinition translate(TokenizerDef hibernateSearchDef) {
		Class<? extends TokenizerFactory> factoryType = hibernateSearchDef.factory();
		AnalysisDefinitionFactory<TokenizerDefinition> factory = luceneTokenizers.get( factoryType.getName() );
		if ( factory == null ) {
			throw LOG.unsupportedTokenizerFactory( factoryType );
		}
		return factory.create( hibernateSearchDef.params() );
	}

	@Override
	public TokenFilterDefinition translate(TokenFilterDef hibernateSearchDef) {
		Class<? extends TokenFilterFactory> factoryType = hibernateSearchDef.factory();
		AnalysisDefinitionFactory<TokenFilterDefinition> factory = luceneTokenFilters.get( factoryType.getName() );
		if ( factory == null ) {
			throw LOG.unsupportedTokenFilterFactory( factoryType );
		}
		return factory.create( hibernateSearchDef.params() );
	}

	private class TokenizerClassNameToElasticsearchTypeNameTransformer implements ParameterValueTransformer {
		private final Class<?> factoryClass;
		private final String parameterName;

		public TokenizerClassNameToElasticsearchTypeNameTransformer(Class<?> factoryClass, String parameterName) {
			this.factoryClass = factoryClass;
			this.parameterName = parameterName;
		}

		@Override
		public JsonElement transform(String parameterValue) {
			AnalysisDefinitionFactory<?> factory =
					DefaultElasticsearchAnalyzerDefinitionTranslator.this.luceneTokenizers.get( parameterValue );

			if ( factory == null ) {
				throw LOG.unsupportedAnalysisFactoryTokenizerClassNameParameter( factoryClass, parameterName, parameterValue );
			}

			return new JsonPrimitive( factory.getType() );
		}

		@Override
		public String toString() {
			return new StringBuilder( getClass().getSimpleName() )
					.append( "[" )
					.append( factoryClass )
					.append( "," )
					.append( parameterName )
					.append( "]" )
					.toString();
		}
	}

}
