/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityListeners;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.java.JavaMetadataProvider;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;

import org.dom4j.Element;

/**
 * MetadataProvider aware of the JPA Deployment descriptor
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class JPAMetadataProvider implements MetadataProvider {
	private final MetadataProvider delegate = new JavaMetadataProvider();

	private final BootstrapContext bootstrapContext;
	private final XMLContext xmlContext;

	private Map<Object, Object> defaults;
	private Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>(100);


	public JPAMetadataProvider(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;

		xmlContext = new XMLContext( bootstrapContext );

	}

	//all of the above can be safely rebuilt from XMLContext: only XMLContext this object is serialized
	@Override
	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get( annotatedElement );
		if (reader == null) {
			if ( xmlContext.hasContext() ) {
				reader = new JPAOverriddenAnnotationReader( annotatedElement, xmlContext, bootstrapContext );
			}
			else {
				reader = delegate.getAnnotationReader( annotatedElement );
			}
			cache.put(annotatedElement, reader);
		}
		return reader;
	}
	@Override
	public Map<Object, Object> getDefaults() {
		if ( defaults == null ) {
			defaults = new HashMap<>();
			XMLContext.Default xmlDefaults = xmlContext.getDefault( null );

			defaults.put( "schema", xmlDefaults.getSchema() );
			defaults.put( "catalog", xmlDefaults.getCatalog() );
			defaults.put( "delimited-identifier", xmlDefaults.getDelimitedIdentifier() );
			defaults.put( "cascade-persist", xmlDefaults.getCascadePersist() );
			List<Class> entityListeners = new ArrayList<Class>();
			for ( String className : xmlContext.getDefaultEntityListeners() ) {
				try {
					entityListeners.add( bootstrapContext.getClassLoaderAccess().classForName( className ) );
				}
				catch ( ClassLoadingException e ) {
					throw new IllegalStateException( "Default entity listener class not found: " + className );
				}
			}
			defaults.put( EntityListeners.class, entityListeners );
			for ( Element element : xmlContext.getAllDocuments() ) {
				@SuppressWarnings( "unchecked" )
				List<Element> elements = element.elements( "sequence-generator" );
				List<SequenceGenerator> sequenceGenerators = ( List<SequenceGenerator> ) defaults.get( SequenceGenerator.class );
				if ( sequenceGenerators == null ) {
					sequenceGenerators = new ArrayList<>();
					defaults.put( SequenceGenerator.class, sequenceGenerators );
				}
				for ( Element subelement : elements ) {
					sequenceGenerators.add( JPAOverriddenAnnotationReader.buildSequenceGeneratorAnnotation( subelement ) );
				}

				elements = element.elements( "table-generator" );
				List<TableGenerator> tableGenerators = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
				if ( tableGenerators == null ) {
					tableGenerators = new ArrayList<>();
					defaults.put( TableGenerator.class, tableGenerators );
				}
				for ( Element subelement : elements ) {
					tableGenerators.add(
							JPAOverriddenAnnotationReader.buildTableGeneratorAnnotation(
									subelement, xmlDefaults
							)
					);
				}

				List<NamedQuery> namedQueries = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
				if ( namedQueries == null ) {
					namedQueries = new ArrayList<>();
					defaults.put( NamedQuery.class, namedQueries );
				}
				List<NamedQuery> currentNamedQueries = JPAOverriddenAnnotationReader.buildNamedQueries(
						element,
						false,
						xmlDefaults,
						bootstrapContext.getClassLoaderAccess()
				);
				namedQueries.addAll( currentNamedQueries );

				List<NamedNativeQuery> namedNativeQueries = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
				if ( namedNativeQueries == null ) {
					namedNativeQueries = new ArrayList<>();
					defaults.put( NamedNativeQuery.class, namedNativeQueries );
				}
				List<NamedNativeQuery> currentNamedNativeQueries = JPAOverriddenAnnotationReader.buildNamedQueries(
						element,
						true,
						xmlDefaults,
						bootstrapContext.getClassLoaderAccess()
				);
				namedNativeQueries.addAll( currentNamedNativeQueries );

				List<SqlResultSetMapping> sqlResultSetMappings = ( List<SqlResultSetMapping> ) defaults.get(
						SqlResultSetMapping.class
				);
				if ( sqlResultSetMappings == null ) {
					sqlResultSetMappings = new ArrayList<>();
					defaults.put( SqlResultSetMapping.class, sqlResultSetMappings );
				}
				List<SqlResultSetMapping> currentSqlResultSetMappings = JPAOverriddenAnnotationReader.buildSqlResultsetMappings(
						element,
						xmlDefaults,
						bootstrapContext.getClassLoaderAccess()
				);
				sqlResultSetMappings.addAll( currentSqlResultSetMappings );

				List<NamedStoredProcedureQuery> namedStoredProcedureQueries = (List<NamedStoredProcedureQuery>)defaults.get( NamedStoredProcedureQuery.class );
				if(namedStoredProcedureQueries==null){
					namedStoredProcedureQueries = new ArrayList<>(  );
					defaults.put( NamedStoredProcedureQuery.class, namedStoredProcedureQueries );
				}
				List<NamedStoredProcedureQuery> currentNamedStoredProcedureQueries = JPAOverriddenAnnotationReader.buildNamedStoreProcedureQueries(
						element,
						xmlDefaults,
						bootstrapContext.getClassLoaderAccess()
				);
				namedStoredProcedureQueries.addAll( currentNamedStoredProcedureQueries );
			}
		}
		return defaults;
	}

	public XMLContext getXMLContext() {
		return xmlContext;
	}
}
