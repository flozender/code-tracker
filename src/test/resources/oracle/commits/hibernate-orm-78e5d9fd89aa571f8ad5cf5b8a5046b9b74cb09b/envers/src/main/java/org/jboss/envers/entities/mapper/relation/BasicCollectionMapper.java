/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.PropertyMapper;
import org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.jboss.envers.entities.mapper.relation.lazy.initializor.BasicCollectionInitializor;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.hibernate.collection.PersistentCollection;

import java.util.Map;
import java.util.Collection;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class BasicCollectionMapper<T extends Collection> extends AbstractCollectionMapper<T> implements PropertyMapper {
    private final MiddleComponentData elementComponentData;

    public BasicCollectionMapper(CommonCollectionMapperData commonCollectionMapperData,
                                 Class<? extends T> collectionClass, Class<? extends T> proxyClass,
                                 MiddleComponentData elementComponentData) {
        super(commonCollectionMapperData, collectionClass, proxyClass);
        this.elementComponentData = elementComponentData;
    }

    protected Initializor<T> getInitializor(VersionsConfiguration verCfg, VersionsReaderImplementor versionsReader,
                                            Object primaryKey, Number revision) {
        return new BasicCollectionInitializor<T>(verCfg, versionsReader, commonCollectionMapperData.getQueryGenerator(),
                primaryKey, revision, collectionClass, elementComponentData);
    }

    protected Collection getNewCollectionContent(PersistentCollection newCollection) {
        return (Collection) newCollection;
    }

    protected Collection getOldCollectionContent(Serializable oldCollection) {
        if (oldCollection == null) {
            return null;
        } else if (oldCollection instanceof Map) {
            return ((Map) oldCollection).keySet();
        } else {
            return (Collection) oldCollection;
        }
    }

    protected void mapToMapFromObject(Map<String, Object> data, Object changed) {
        elementComponentData.getComponentMapper().mapToMapFromObject(data, changed);
    }

    protected Object getElement(Object changedObject) {
        return changedObject;
    }
}
