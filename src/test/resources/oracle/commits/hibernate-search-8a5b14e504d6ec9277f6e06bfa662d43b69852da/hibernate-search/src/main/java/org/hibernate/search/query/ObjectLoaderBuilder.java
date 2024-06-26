package org.hibernate.search.query;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.Loader;
import org.hibernate.search.engine.MultiClassesQueryLoader;
import org.hibernate.search.engine.QueryLoader;
import org.hibernate.search.engine.SearchFactoryImplementor;

import java.util.List;
import java.util.Set;

/**
 * @author Emmanuel Bernard
 */
public class ObjectLoaderBuilder {
	private Criteria criteria;
	private List<Class<?>> targetedEntities;
	private SessionImplementor session;
	private SearchFactoryImplementor searchFactoryImplementor;
	private Set<Class<?>> indexedTargetedEntities;

	public ObjectLoaderBuilder criteria(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	public ObjectLoaderBuilder targetedEntities(List<Class<?>> targetedEntities) {
		this.targetedEntities = targetedEntities;
		return this;
	}

	public Loader buildLoader() {
		if ( criteria != null ) {
			return getCriteriaLoader();
		}
		else if ( targetedEntities.size() == 1 ) {
			return getSingleEntityLoader();
		}
		else {
			return getMultipleEntitiesLoader();
		}
	}

	private Loader getMultipleEntitiesLoader() {
		final MultiClassesQueryLoader multiClassesLoader = new MultiClassesQueryLoader();
		multiClassesLoader.init( (Session) session, searchFactoryImplementor );
		multiClassesLoader.setEntityTypes( indexedTargetedEntities );
		return multiClassesLoader;
	}

	private Loader getSingleEntityLoader() {
		final QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( ( Session ) session, searchFactoryImplementor );
		queryLoader.setEntityType( targetedEntities.iterator().next() );
		return queryLoader;
	}

	private Loader getCriteriaLoader() {
		if ( targetedEntities.size() > 1 ) {
			throw new SearchException( "Cannot mix criteria and multiple entity types" );
		}
		Class entityType = targetedEntities.size() == 0 ? null : targetedEntities.iterator().next();
		if ( criteria instanceof CriteriaImpl) {
			String targetEntity = ( ( CriteriaImpl ) criteria ).getEntityOrClassName();
			if ( entityType != null && !entityType.getName().equals( targetEntity ) ) {
				throw new SearchException( "Criteria query entity should match query entity" );
			}
			else {
				try {
					entityType = ReflectHelper.classForName( targetEntity );
				}
				catch ( ClassNotFoundException e ) {
					throw new SearchException( "Unable to load entity class from criteria: " + targetEntity, e );
				}
			}
		}
		QueryLoader queryLoader = new QueryLoader();
		queryLoader.init( ( Session ) session, searchFactoryImplementor );
		queryLoader.setEntityType( entityType );
		queryLoader.setCriteria( criteria );
		return queryLoader;
	}

	public ObjectLoaderBuilder session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public ObjectLoaderBuilder searchFactory(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		return this;
	}

	public ObjectLoaderBuilder indexedTargetedEntities(Set<Class<?>> indexedTargetedEntities) {
		this.indexedTargetedEntities = indexedTargetedEntities;
		return this;
	}
}
