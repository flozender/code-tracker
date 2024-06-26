/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.search.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.BatchCoordinator;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.
 * The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 * 
 * @author Sanne Grinovero
 */
public class MassIndexerImpl implements MassIndexer {
	
	private static final Logger log = LoggerFactory.make();
	
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final SessionFactory sessionFactory;

	protected Set<Class<?>> rootEntities = new HashSet<Class<?>>();
	
	// default settings defined here:
	private int objectLoadingThreads = 2; //loading the main entity
	private int collectionLoadingThreads = 4; //also responsible for loading of lazy @IndexedEmbedded collections
//	private int writerThreads = 1; //also running the Analyzers
	private int objectLoadingBatchSize = 10;
	private int objectsLimit = 0; //means no limit at all
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeAtEnd = true;
	private boolean purgeAtStart = true;
	private boolean optimizeAfterPurge = true;
	private MassIndexerProgressMonitor monitor = new SimpleIndexingProgressMonitor();

	protected MassIndexerImpl(SearchFactoryImplementor searchFactory, SessionFactory sessionFactory, Class<?>...entities) {
		this.searchFactoryImplementor = searchFactory;
		this.sessionFactory = sessionFactory;
		rootEntities = toRootEntities( searchFactoryImplementor, entities );
	}

	/**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 * @param selection
	 * @return a new set of entities
	 */
	private static Set<Class<?>> toRootEntities(SearchFactoryImplementor searchFactoryImplementor, Class<?>... selection) {
		Set<Class<?>> entities = new HashSet<Class<?>>();
		//first build the "entities" set containing all indexed subtypes of "selection".
		for (Class<?> entityType : selection) {
			Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic( new Class[] {entityType} );
			if ( targetedClasses.isEmpty() ) {
				String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
				throw new IllegalArgumentException( msg );
			}
			entities.addAll( targetedClasses );
		}
		Set<Class<?>> cleaned = new HashSet<Class<?>>();
		Set<Class<?>> toRemove = new HashSet<Class<?>>();
		//now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for (Class<?> type : entities) {
			boolean typeIsOk = true;
			for (Class<?> existing : cleaned) {
				if ( existing.isAssignableFrom( type ) ) {
					typeIsOk = false;
					break;
				}
				if ( type.isAssignableFrom( existing ) ) {
					toRemove.add( existing );
				}
			}
			if ( typeIsOk ) {
				cleaned.add( type );
			}
		}
		cleaned.removeAll( toRemove );
		log.debug( "Targets for indexing job: {}", cleaned );
		return cleaned;
	}

	public MassIndexer cacheMode(CacheMode cacheMode) {
		if ( cacheMode == null )
			throw new IllegalArgumentException( "cacheMode must not be null" );
		this.cacheMode = cacheMode;
		return this;
	}

	public MassIndexer threadsToLoadObjects(int numberOfThreads) {
		if ( numberOfThreads < 1 )
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		this.objectLoadingThreads = numberOfThreads;
		return this;
	}
	
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		if ( batchSize < 1 )
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		this.objectLoadingBatchSize = batchSize;
		return this;
	}
	
	public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
		if ( numberOfThreads < 1 )
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		this.collectionLoadingThreads = numberOfThreads;
		return this;
	}
	
	//TODO see MassIndexer interface
//	public MassIndexer threadsForIndexWriter(int numberOfThreads) {
//		if ( numberOfThreads < 1 )
//			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
//		this.writerThreads = numberOfThreads;
//		return this;
//	}
	
	public MassIndexer optimizeOnFinish(boolean optimize) {
		this.optimizeAtEnd = optimize;
		return this;
	}

	public MassIndexer optimizeAfterPurge(boolean optimize) {
		this.optimizeAfterPurge = optimize;
		return this;
	}

	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		this.purgeAtStart = purgeAll;
		return this;
	}
	
	public Future<?> start() {
		BatchCoordinator coordinator = createCoordinator();
		ExecutorService executor = Executors.newFixedThreadPool( 1, "batch coordinator" );
		try {
			Future<?> submit = executor.submit( coordinator );
			return submit;
		}
		finally {
			executor.shutdown();
		}
	}
	
	public void startAndWait() throws InterruptedException {
		BatchCoordinator coordinator = createCoordinator();
		coordinator.run();
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}
	}
	
	protected BatchCoordinator createCoordinator() {
		return new BatchCoordinator( rootEntities, searchFactoryImplementor, sessionFactory,
				objectLoadingThreads, collectionLoadingThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				monitor );
	}

	public MassIndexer limitIndexedObjectsTo(int maximum) {
		this.objectsLimit = maximum;
		return this;
	}

}
