/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.impl;

import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.FlagsAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.KeywordAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PayloadAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.TypeAttributeImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorksBuilder;
import org.hibernate.search.indexes.serialization.spi.SerializableIndex;
import org.hibernate.search.indexes.serialization.spi.SerializableStore;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.impl.SerializationHelper.toSerializable;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneWorkHydrator implements LuceneWorksBuilder {
	private static final Log log = LoggerFactory.make();

	private SearchFactoryImplementor searchFactory;
	private List<LuceneWork> results;
	private ClassLoader loader;
	private Document luceneDocument;
	private List<AttributeImpl> attributes;
	private List<List<AttributeImpl>> tokens;
	private Serializable id;

	public LuceneWorkHydrator(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
		this.results = new ArrayList<LuceneWork>();
		this.loader = Thread.currentThread().getContextClassLoader();
	}

	public List<LuceneWork> getLuceneWorks() {
		return results;
	}

	@Override
	public void addOptimizeAll() {
		results.add( OptimizeLuceneWork.INSTANCE );
	}

	@Override
	public void addFlush() {
		results.add( FlushLuceneWork.INSTANCE );
	}

	@Override
	public void addPurgeAllLuceneWork(String entityClassName) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		results.add( new PurgeAllLuceneWork( entityClass ) );
	}

	@Override
	public void addIdAsJavaSerialized(byte[] idAsByte) {
		this.id = toSerializable( idAsByte, loader );
	}

	@Override
	public void addId(Serializable id) {
		this.id = id;
	}

	@Override
	public void addDeleteLuceneWork(String entityClassName, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new DeleteLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass
		);
		results.add( result );
		id = null;
	}

	@Override
	public void addAddLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new AddLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
		id = null;
	}

	@Override
	public void addUpdateLuceneWork(String entityClassName, Map<String, String> fieldToAnalyzerMap, ConversionContext conversionContext) {
		Class<?> entityClass = ClassLoaderHelper.classForName(
				entityClassName,
				"entity class",
				searchFactory.getServiceManager()
		);
		LuceneWork result = new UpdateLuceneWork(
				id,
				objectIdInString( entityClass, id, conversionContext ),
				entityClass,
				getLuceneDocument(),
				fieldToAnalyzerMap
		);
		results.add( result );
		clearDocument();
		id = null;
	}

	private void clearDocument() {
		luceneDocument = null;
	}

	@Override
	public void defineDocument() {
		//Document level boost is not available anymore: is this method still needed?
		getLuceneDocument();
	}

	@Override
	public void addFieldable(byte[] instanceAsByte) {
		//FIXME implementors of IndexableField ARE NOT SERIALIZABLE :-(
		getLuceneDocument().add( (IndexableField) toSerializable( instanceAsByte, loader ) );
	}

	@Override
	public void addIntNumericField(int value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final IntField numField = new IntField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addLongNumericField(long value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final LongField numField = new LongField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addFloatNumericField(float value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final FloatField numField = new FloatField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addDoubleNumericField(double value, String name, int precisionStep, SerializableStore store, boolean indexed, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final DoubleField numField = new DoubleField( name, value, getStore( store ) );
		numField.setBoost( boost );
		getLuceneDocument().add( numField );
	}

	@Override
	public void addFieldWithBinaryData(String name, byte[] value, int offset, int length) {
		Field luceneField = new StoredField( name, value, offset, length );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addFieldWithStringData(String name, String value, SerializableStore store, SerializableIndex index, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = identifyFieldType( store == SerializableStore.YES, //if stored
				index != SerializableIndex.NO, //if indexed
				index == SerializableIndex.ANALYZED || index == SerializableIndex.ANALYZED_NO_NORMS, //if analyzed
				termVector, omitNorms, omitTermFreqAndPositions );
		Field luceneField = new Field( name, value, type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addFieldWithTokenStreamData(String name, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = identifyFieldType( false, true, true, termVector, omitNorms, omitTermFreqAndPositions );
		Field luceneField = new Field( name, new CopyTokenStream( tokens ), type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
		clearTokens();
	}

	private void clearTokens() {
		tokens = new ArrayList<List<AttributeImpl>>();
	}

	@Override
	public void addFieldWithSerializableReaderData(String name, byte[] valueAsByte, SerializableTermVector termVector, float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
		FieldType type = identifyFieldType( false, true, true, termVector, omitNorms, omitTermFreqAndPositions );
		Reader value = (Reader) toSerializable( valueAsByte, loader );
		Field luceneField = new Field( name, value, type );
		luceneField.setBoost( boost );
		getLuceneDocument().add( luceneField );
	}

	@Override
	public void addSerializedAttribute(byte[] bytes) {
		getAttributes().add( (AttributeImpl) toSerializable( bytes, loader ) );
	}

	@Override
	public void addAttributeInstance(AttributeImpl attribute) {
		getAttributes().add( attribute );
	}

	@Override
	public void addTokenTrackingAttribute(List<Integer> positions) {
		//TokenTrackingAttribute is no longer available
		throw new SearchException( "Serialization of TokenTrackingAttribute is no longer supported" );
	}

	@Override
	public void addCharTermAttribute(CharSequence sequence) {
		CharTermAttributeImpl attr = new CharTermAttributeImpl();
		attr.append( sequence );
		getAttributes().add( attr );
	}

	@Override
	public void addPayloadAttribute(byte[] payloads) {
		PayloadAttributeImpl attr = new PayloadAttributeImpl();
		attr.setPayload( new BytesRef( payloads ) );
		getAttributes().add( attr );
	}

	@Override
	public void addKeywordAttribute(boolean isKeyword) {
		KeywordAttributeImpl attr = new KeywordAttributeImpl();
		attr.setKeyword( isKeyword );
		getAttributes().add( attr );
	}

	@Override
	public void addPositionIncrementAttribute(int positionIncrement) {
		PositionIncrementAttributeImpl attr = new PositionIncrementAttributeImpl();
		attr.setPositionIncrement( positionIncrement );
		getAttributes().add( attr );
	}

	@Override
	public void addFlagsAttribute(int flags) {
		FlagsAttributeImpl attr = new FlagsAttributeImpl();
		attr.setFlags( flags );
		getAttributes().add( attr );
	}

	@Override
	public void addTypeAttribute(String type) {
		TypeAttributeImpl attr = new TypeAttributeImpl();
		attr.setType( type );
		getAttributes().add( attr );
	}

	@Override
	public void addOffsetAttribute(int startOffset, int endOffset) {
		OffsetAttributeImpl attr = new OffsetAttributeImpl();
		attr.setOffset( startOffset, endOffset );
		getAttributes().add( attr );
	}

	@Override
	public void addToken() {
		getTokens().add( getAttributes() );
		clearAttributes();
	}

	private void clearAttributes() {
		attributes = new ArrayList<AttributeImpl>();
	}

	private Document getLuceneDocument() {
		if ( luceneDocument == null ) {
			luceneDocument = new Document();
		}
		return luceneDocument;
	}

	private String objectIdInString(Class<?> entityClass, Serializable id, ConversionContext conversionContext) {
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( entityClass );
		if ( indexBindingForEntity == null ) {
			throw new SearchException( "Unable to find entity type metadata while deserializing: " + entityClass );
		}
		DocumentBuilderIndexedEntity documentBuilder = indexBindingForEntity.getDocumentBuilder();
		return documentBuilder.objectToString( documentBuilder.getIdKeywordName(), id, conversionContext );
	}

	private FieldType identifyFieldType(boolean stored, boolean indexed, boolean analyzed, SerializableTermVector termVector, boolean omitNorms, boolean omitTermFreqAndPositions) {
		final FieldType type = new FieldType();
		type.setStored( stored );
		type.setIndexed( indexed );
		type.setTokenized( analyzed );
		type.setStoreTermVectors( termVector != SerializableTermVector.NO );
		type.setStoreTermVectorOffsets( termVector == SerializableTermVector.WITH_OFFSETS || termVector == SerializableTermVector.WITH_POSITIONS_OFFSETS );
		type.setStoreTermVectorPayloads( termVector == SerializableTermVector.YES );
		type.setStoreTermVectorPositions( termVector == SerializableTermVector.WITH_POSITIONS || termVector == SerializableTermVector.WITH_POSITIONS_OFFSETS );
		type.setOmitNorms( omitNorms );
		type.setIndexOptions( omitTermFreqAndPositions ? IndexOptions.DOCS_ONLY : IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
		return type	;
	}

	private Field.Store getStore(SerializableStore store) {
		switch ( store ) {
			case NO:
				return Field.Store.NO;
			case YES:
				return Field.Store.YES;
			default:
				throw log.unableToConvertSerializableStoreToLuceneStore( store.toString() );
		}
	}

	public List<AttributeImpl> getAttributes() {
		if ( attributes == null ) {
			attributes = new ArrayList<AttributeImpl>();
		}
		return attributes;
	}

	public List<List<AttributeImpl>> getTokens() {
		if ( tokens == null ) {
			tokens = new ArrayList<List<AttributeImpl>>();
		}
		return tokens;
	}
}
