/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
												QueryCustomizer queryCustomizer, List<FieldContext> fieldContexts,
												QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
		this.queryContext = queryContext;
	}

	@Override
	public RangeTerminationExcludable excludeLimit() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeFrom( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	@Override
	public Query createQuery() {
		final int size = fieldContexts.size();
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ), conversionContext ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery( );
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext, conversionContext ), BooleanClause.Occur.SHOULD );
			}
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();
		final Analyzer queryAnalyzer = queryContext.getQueryAnalyzer();

		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );

		final FieldBridge fieldBridge = fieldContext.getFieldBridge() != null ? fieldContext.getFieldBridge() : documentBuilder.getBridge( fieldContext.getField() );

		final Object fromObject = rangeContext.getFrom();
		final Object toObject = rangeContext.getTo();

		if ( fieldBridge != null && NumericFieldBridge.class.isAssignableFrom( fieldBridge.getClass() ) ) {
			perFieldQuery = NumericFieldUtils.createNumericRangeQuery(
					fieldName,
					fromObject,
					toObject,
					!rangeContext.isExcludeFrom(),
					!rangeContext.isExcludeTo()
			);
		}
		else {
			final String fromString = fieldContext.objectToString( documentBuilder, fromObject, conversionContext );
			final String lowerTerm = fromString == null ?
					null :
					Helper.getAnalyzedTerm( fieldName, fromString, "from", queryAnalyzer, fieldContext );

			final String toString = fieldContext.objectToString( documentBuilder, toObject, conversionContext );
			final String upperTerm = toString == null ?
					null :
					Helper.getAnalyzedTerm( fieldName, toString, "to", queryAnalyzer, fieldContext );

			perFieldQuery = TermRangeQuery.newStringRange( fieldName, lowerTerm, upperTerm, !rangeContext.isExcludeFrom(), !rangeContext.isExcludeTo() );
		}

		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}
}
