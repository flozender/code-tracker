/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import static org.jboss.logging.Logger.Level.TRACE;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.VersionType;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Utilities for dealing with optimisitic locking values.
 *
 * @author Gavin King
 */
public final class Versioning {
	/**
	 * Apply no optimistic locking
	 */
	public static final int OPTIMISTIC_LOCK_NONE = -1;

	/**
	 * Apply optimisitc locking based on the defined version or timestamp
	 * property.
	 */
	public static final int OPTIMISTIC_LOCK_VERSION = 0;

	/**
	 * Apply optimisitc locking based on the a current vs. snapshot comparison
	 * of <b>all</b> properties.
	 */
	public static final int OPTIMISTIC_LOCK_ALL = 2;

	/**
	 * Apply optimisitc locking based on the a current vs. snapshot comparison
	 * of <b>dirty</b> properties.
	 */
	public static final int OPTIMISTIC_LOCK_DIRTY = 1;

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                Versioning.class.getPackage().getName());

	/**
	 * Private constructor disallowing instantiation.
	 */
	private Versioning() {}

	/**
	 * Create an initial optimisitc locking value according the {@link VersionType}
	 * contract for the version property.
	 *
	 * @param versionType The version type.
	 * @param session The originating session
	 * @return The initial optimisitc locking value
	 */
	private static Object seed(VersionType versionType, SessionImplementor session) {
		Object seed = versionType.seed( session );
        LOG.seeding(seed);
		return seed;
	}

	/**
	 * Create an initial optimisitc locking value according the {@link VersionType}
	 * contract for the version property <b>if required</b> and inject it into
	 * the snapshot state.
	 *
	 * @param fields The current snapshot state
	 * @param versionProperty The index of the version property
	 * @param versionType The version type
	 * @param session The orginating session
	 * @return True if we injected a new version value into the fields array; false
	 * otherwise.
	 */
	public static boolean seedVersion(
	        Object[] fields,
	        int versionProperty,
	        VersionType versionType,
	        SessionImplementor session) {
		Object initialVersion = fields[versionProperty];
		if (
			initialVersion==null ||
			// This next bit is to allow for both unsaved-value="negative"
			// and for "older" behavior where version number did not get
			// seeded if it was already set in the object
			// TODO: shift it into unsaved-value strategy
			( (initialVersion instanceof Number) && ( (Number) initialVersion ).longValue()<0 )
		) {
			fields[versionProperty] = seed( versionType, session );
			return true;
		}
		else {
            LOG.usingInitialVersion(initialVersion);
			return false;
		}
	}


	/**
	 * Generate the next increment in the optimisitc locking value according
	 * the {@link VersionType} contract for the version property.
	 *
	 * @param version The current version
	 * @param versionType The version type
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	public static Object increment(Object version, VersionType versionType, SessionImplementor session) {
		Object next = versionType.next( version, session );
        if (LOG.isTraceEnabled()) LOG.incrementing(versionType.toLoggableString(version, session.getFactory()),
                                                   versionType.toLoggableString(next, session.getFactory()));
		return next;
	}

	/**
	 * Inject the optimisitc locking value into the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param version The optimisitc locking value
	 * @param persister The entity persister
	 */
	public static void setVersion(Object[] fields, Object version, EntityPersister persister) {
		if ( !persister.isVersioned() ) {
			return;
		}
		fields[ persister.getVersionProperty() ] = version;
	}

	/**
	 * Extract the optimisitc locking value out of the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param persister The entity persister
	 * @return The extracted optimisitc locking value
	 */
	public static Object getVersion(Object[] fields, EntityPersister persister) {
		if ( !persister.isVersioned() ) {
			return null;
		}
		return fields[ persister.getVersionProperty() ];
	}

	/**
	 * Do we need to increment the version number, given the dirty properties?
	 *
	 * @param dirtyProperties The array of property indexes which were deemed dirty
	 * @param hasDirtyCollections Were any collections found to be dirty (structurally changed)
	 * @param propertyVersionability An array indicating versionability of each property.
	 * @return True if a version increment is required; false otherwise.
	 */
	public static boolean isVersionIncrementRequired(
			final int[] dirtyProperties,
			final boolean hasDirtyCollections,
			final boolean[] propertyVersionability) {
		if ( hasDirtyCollections ) {
			return true;
		}
		for ( int i = 0; i < dirtyProperties.length; i++ ) {
			if ( propertyVersionability[ dirtyProperties[i] ] ) {
				return true;
			}
		}
	    return false;
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "Incrementing: %s to %s" )
        void incrementing( String loggableString,
                           String loggableString2 );

        @LogMessage( level = TRACE )
        @Message( value = "Seeding: %s" )
        void seeding( Object seed );

        @LogMessage( level = TRACE )
        @Message( value = "Using initial version: %s" )
        void usingInitialVersion( Object initialVersion );
    }
}
