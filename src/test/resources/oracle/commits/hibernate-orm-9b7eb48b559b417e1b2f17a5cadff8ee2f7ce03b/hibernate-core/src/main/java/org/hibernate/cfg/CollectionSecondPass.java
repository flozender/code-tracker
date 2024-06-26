/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;

import static org.jboss.logging.Logger.Level.DEBUG;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.hibernate.MappingException;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Collection second pass
 *
 * @author Emmanuel Bernard
 */
public abstract class CollectionSecondPass implements SecondPass {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                CollectionSecondPass.class.getPackage().getName());

	Mappings mappings;
	Collection collection;
	private Map localInheritedMetas;

	public CollectionSecondPass(Mappings mappings, Collection collection, java.util.Map inheritedMetas) {
		this.collection = collection;
		this.mappings = mappings;
		this.localInheritedMetas = inheritedMetas;
	}

	public CollectionSecondPass(Mappings mappings, Collection collection) {
		this(mappings, collection, Collections.EMPTY_MAP);
	}

	public void doSecondPass(java.util.Map persistentClasses)
			throws MappingException {
        LOG.secondPass(collection.getRole());

		secondPass( persistentClasses, localInheritedMetas ); // using local since the inheritedMetas at this point is not the correct map since it is always the empty map
		collection.createAllKeys();

        if (LOG.isDebugEnabled()) {
			String msg = "Mapped collection key: " + columns( collection.getKey() );
			if ( collection.isIndexed() )
				msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
			if ( collection.isOneToMany() ) {
				msg += ", one-to-many: "
					+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			}
			else {
				msg += ", element: " + columns( collection.getElement() );
			}
            LOG.mappedCollection(msg);
		}
	}

	abstract public void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
			throws MappingException;

	private static String columns(Value val) {
		StringBuffer columns = new StringBuffer();
		Iterator iter = val.getColumnIterator();
		while ( iter.hasNext() ) {
			columns.append( ( (Selectable) iter.next() ).getText() );
			if ( iter.hasNext() ) columns.append( ", " );
		}
		return columns.toString();
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "%s" )
        void mappedCollection( String message );

        @LogMessage( level = DEBUG )
        @Message( value = "Second pass for collection: %s" )
        void secondPass( String role );
    }
}
