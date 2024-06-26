/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.global;

import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.HashMap;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.QueryHints;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

public class QueryBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			QueryBinder.class.getName()
	);

	/**
	 * Binds all {@link NamedQuery}, {@link NamedQueries}, {@link NamedNativeQuery}, {{@link NamedNativeQueries},
	 * {@link org.hibernate.annotations.NamedQuery} , {@link org.hibernate.annotations.NamedQueries},
	 * {@link org.hibernate.annotations.NamedNativeQuery}, and {@link org.hibernate.annotations.NamedNativeQueries} annotations to
	 * the supplied metadata.
	 *
	 * @param metadata the global metadata
	 * @param jandex the jandex index
	 */
	public static void bind(MetadataImplementor metadata, Index jandex) {
		for ( AnnotationInstance query : jandex.getAnnotations( JPADotNames.NAMED_QUERY ) ) {
			bindNamedQuery( metadata, query );
		}
		for ( AnnotationInstance queries : jandex.getAnnotations( JPADotNames.NAMED_QUERIES ) ) {
			for ( AnnotationInstance query : JandexHelper.getValueAsArray( queries, "value" ) ) {
				bindNamedQuery( metadata, query );
			}
		}
		for ( AnnotationInstance query : jandex.getAnnotations( JPADotNames.NAMED_NATIVE_QUERY ) ) {
			bindNamedNativeQuery( metadata, query );
		}
		for ( AnnotationInstance queries : jandex.getAnnotations( JPADotNames.NAMED_NATIVE_QUERIES ) ) {
			for ( AnnotationInstance query : JandexHelper.getValueAsArray( queries, "value" ) ) {
				bindNamedNativeQuery( metadata, query );
			}
		}
		for ( AnnotationInstance query : jandex.getAnnotations( HibernateDotNames.NAMED_QUERY ) ) {
			bindNamedQuery( metadata, query );
		}
		for ( AnnotationInstance queries : jandex.getAnnotations( HibernateDotNames.NAMED_QUERIES ) ) {
			for ( AnnotationInstance query : JandexHelper.getValueAsArray( queries, "value" ) ) {
				bindNamedQuery( metadata, query );
			}
		}
		for ( AnnotationInstance query : jandex.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY ) ) {
			bindNamedNativeQuery( metadata, query );
		}
		for ( AnnotationInstance queries : jandex.getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERIES ) ) {
			for ( AnnotationInstance query : JandexHelper.getValueAsArray( queries, "value" ) ) {
				bindNamedNativeQuery( metadata, query );
			}
		}
	}

	private static void bindNamedQuery(MetadataImplementor metadata, AnnotationInstance annotation) {
		String name = JandexHelper.getValueAsString( annotation, "name" );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		String query = JandexHelper.getValueAsString( annotation, "query" );
		AnnotationInstance[] hints = JandexHelper.getValueAsArray( annotation, "hints" );
		String cacheRegion = getString( hints, QueryHints.CACHE_REGION );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}
		Integer timeout = getTimeout( hints, query );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}
		Integer fetchSize = getInteger( hints, QueryHints.FETCH_SIZE, name );
		if ( fetchSize != null && fetchSize < 0 ) {
			fetchSize = null;
		}
		String comment = getString( hints, QueryHints.COMMENT );
		if ( StringHelper.isEmpty( comment ) ) {
			comment = null;
		}
		metadata.addNamedQuery(
				name,
				new NamedQueryDefinition(
						query, getBoolean( hints, QueryHints.CACHEABLE, name ), cacheRegion,
						timeout, fetchSize, getFlushMode( hints, QueryHints.FLUSH_MODE, name ),
						getCacheMode( hints, QueryHints.CACHE_MODE, name ),
						getBoolean( hints, QueryHints.READ_ONLY, name ), comment, null
				)
		);
		LOG.debugf( "Binding named query: %s => %s", name, query );
	}

	private static void bindNamedNativeQuery(MetadataImplementor metadata, AnnotationInstance annotation) {
		String name = JandexHelper.getValueAsString( annotation, "name" );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named native query must have a name when used in class or package level" );
		}
		String query = JandexHelper.getValueAsString( annotation, "query" );
		String resultSetMapping = JandexHelper.getValueAsString( annotation, "resultSetMapping" );
		AnnotationInstance[] hints = JandexHelper.getValueAsArray( annotation, "hints" );
		boolean cacheable = getBoolean( hints, "org.hibernate.cacheable", name );
		String cacheRegion = getString( hints, QueryHints.CACHE_REGION );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}
		Integer timeout = getTimeout( hints, query );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}
		Integer fetchSize = getInteger( hints, QueryHints.FETCH_SIZE, name );
		if ( fetchSize != null && fetchSize < 0 ) {
			fetchSize = null;
		}
		FlushMode flushMode = getFlushMode( hints, QueryHints.FLUSH_MODE, name );
		CacheMode cacheMode = getCacheMode( hints, QueryHints.CACHE_MODE, name );
		boolean readOnly = getBoolean( hints, QueryHints.READ_ONLY, name );
		String comment = getString( hints, QueryHints.COMMENT );
		if ( StringHelper.isEmpty( comment ) ) {
			comment = null;
		}
		boolean callable = getBoolean( hints, QueryHints.CALLABLE, name );
		NamedSQLQueryDefinition def;
		if ( StringHelper.isNotEmpty( resultSetMapping ) ) {
			def = new NamedSQLQueryDefinition(
					query, resultSetMapping, null, cacheable,
					cacheRegion, timeout, fetchSize,
					flushMode, cacheMode, readOnly, comment,
					null, callable
			);
		}
		else {
			String resultClass = JandexHelper.getValueAsString( annotation, "resultClass" );
			if ( void.class.equals( resultClass ) ) {
				throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
			}
			def = new NamedSQLQueryDefinition(
					query, new NativeSQLQueryRootReturn[] {
					new NativeSQLQueryRootReturn(
							"alias1",
							resultClass,
							new HashMap(),
							LockMode.READ
					)
			},
					null, cacheable, cacheRegion, timeout, fetchSize, flushMode, cacheMode, readOnly,
					comment, null, callable
			);

		}
		metadata.addNamedNativeQuery( name, def );
		LOG.debugf( "Binding named native query: %s => %s", name, query );
	}

	private static boolean getBoolean(AnnotationInstance[] hints,
									  String element,
									  String query) {
		String val = getString( hints, element );
		if ( val == null || val.equalsIgnoreCase( "false" ) ) {
			return false;
		}
		if ( val.equalsIgnoreCase( "true" ) ) {
			return true;
		}
		throw new AnnotationException( "Not a boolean in hint: " + query + ":" + element );
	}

	private static CacheMode getCacheMode(AnnotationInstance[] hints,
										  String element,
										  String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		if ( val.equalsIgnoreCase( CacheMode.GET.toString() ) ) {
			return CacheMode.GET;
		}
		if ( val.equalsIgnoreCase( CacheMode.IGNORE.toString() ) ) {
			return CacheMode.IGNORE;
		}
		if ( val.equalsIgnoreCase( CacheMode.NORMAL.toString() ) ) {
			return CacheMode.NORMAL;
		}
		if ( val.equalsIgnoreCase( CacheMode.PUT.toString() ) ) {
			return CacheMode.PUT;
		}
		if ( val.equalsIgnoreCase( CacheMode.REFRESH.toString() ) ) {
			return CacheMode.REFRESH;
		}
		throw new AnnotationException( "Unknown CacheMode in hint: " + query + ":" + element );
	}

	private static FlushMode getFlushMode(AnnotationInstance[] hints,
										  String element,
										  String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		if ( val.equalsIgnoreCase( FlushMode.ALWAYS.toString() ) ) {
			return FlushMode.ALWAYS;
		}
		else if ( val.equalsIgnoreCase( FlushMode.AUTO.toString() ) ) {
			return FlushMode.AUTO;
		}
		else if ( val.equalsIgnoreCase( FlushMode.COMMIT.toString() ) ) {
			return FlushMode.COMMIT;
		}
		else if ( val.equalsIgnoreCase( FlushMode.NEVER.toString() ) ) {
			return FlushMode.MANUAL;
		}
		else if ( val.equalsIgnoreCase( FlushMode.MANUAL.toString() ) ) {
			return FlushMode.MANUAL;
		}
		else {
			throw new AnnotationException( "Unknown FlushMode in hint: " + query + ":" + element );
		}

	}

	private static Integer getInteger(AnnotationInstance[] hints,
									  String element,
									  String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		try {
			return Integer.decode( val );
		}
		catch ( NumberFormatException nfe ) {
			throw new AnnotationException( "Not an integer in hint: " + query + ":" + element, nfe );
		}
	}

	private static String getString(AnnotationInstance[] hints,
									String element) {
		for ( AnnotationInstance hint : hints ) {
			if ( element.equals( JandexHelper.getValue( hint, "name" ) ) ) {
				return JandexHelper.getValueAsString( hint, "value" );
			}
		}
		return null;
	}

	private static Integer getTimeout(AnnotationInstance[] hints,
									  String query) {
		Integer timeout = getInteger( hints, QueryHints.TIMEOUT_JPA, query );
		if ( timeout == null ) {
			return getInteger( hints, QueryHints.TIMEOUT_HIBERNATE, query ); // timeout is already in seconds
		}
		return new Integer( (int) Math.round( timeout.doubleValue() / 1000.0 ) ); // convert milliseconds to seconds
	}

	private QueryBinder() {
	}
}
