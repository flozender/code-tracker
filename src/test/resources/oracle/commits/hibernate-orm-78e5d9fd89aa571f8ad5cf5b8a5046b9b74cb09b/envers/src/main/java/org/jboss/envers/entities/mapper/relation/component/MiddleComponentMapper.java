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
package org.jboss.envers.entities.mapper.relation.component;

import org.jboss.envers.entities.EntityInstantiator;
import org.jboss.envers.tools.query.Parameters;

import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface MiddleComponentMapper {
    /**
     * Maps from full object data, contained in the given map (or object representation of the map, if
     * available), to an object.
     * @param entityInstantiator An entity instatiator bound with an open versions reader.
     * @param data Full object data.
     * @param dataObject An optional object representation of the data.
     * @param revision Revision at which the data is read.
     * @return An object with data corresponding to the one found in the given map.
     */
    Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                  Object dataObject, Number revision);

    /**
     * Maps from an object to the object's map representation (for an entity - only its id).
     * @param data Map to which data should be added.
     * @param obj Object to map from.
     */
    void mapToMapFromObject(Map<String, Object> data, Object obj);

    /**
     * Adds query statements, which contains restrictions, which express the property that part of the middle
     * entity with alias prefix1, is equal to part of the middle entity with alias prefix2 (the entity is the same).
     * The part is the component's representation in the middle entity.
     * @param parameters Parameters, to which to add the statements.
     * @param prefix1 First alias of the entity + prefix to add to the properties.
     * @param prefix2 Second alias of the entity + prefix to add to the properties.
     */
    void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2);
}
