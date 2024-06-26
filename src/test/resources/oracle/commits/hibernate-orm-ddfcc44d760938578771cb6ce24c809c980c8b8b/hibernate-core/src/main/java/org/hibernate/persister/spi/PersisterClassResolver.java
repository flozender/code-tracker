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
package org.hibernate.persister.spi;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.Service;

/**
 * Provides persister classes based on the entity or collection role.
 * The persister class is chosen according to the following rules in decreasing priority:
 *  - the persister class defined explicitly via annotation or XML
 *  - the persister class returned by the PersisterClassResolver implementation (if not null)
 *  - the default provider as chosen by Hibernate Core (best choice most of the time)
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Steve Ebersole
 */
public interface PersisterClassResolver extends Service {
	/**
	 * Returns the entity persister class for a given entityName or null
	 * if the entity persister class should be the default.
	 */
	Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata);

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 */
	Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata);
}
