/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.id;

import org.jboss.envers.tools.query.Parameters;

import java.util.Map;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface IdMapper {
    void mapToMapFromId(Map<String, Object> data, Object obj);

    void mapToMapFromEntity(Map<String, Object> data, Object obj);

    void mapToEntityFromMap(Object obj, Map data);

    Object mapToIdFromEntity(Object data);

    Object mapToIdFromMap(Map data);

    /**
     * Creates a mapper with all mapped properties prefixed. A mapped property is a property which
     * is directly mapped to values (not composite).
     * @param prefix Prefix to add to mapped properties
     * @return A copy of the current property mapper, with mapped properties prefixed.
     */
    IdMapper prefixMappedProperties(String prefix);

    /**
     * @param obj Id from which to map.
     * @return A set parameter data, needed to build a query basing on the given id.
     */
    List<QueryParameterData> mapToQueryParametersFromId(Object obj);

    /**
     * Adds query statements, which contains restrictions, which express the property that the id of the entity
     * with alias prefix1, is equal to the id of the entity with alias prefix2 (the entity is the same).
     * @param parameters Parameters, to which to add the statements.
     * @param prefix1 First alias of the entity + prefix to add to the properties.
     * @param prefix2 Second alias of the entity + prefix to add to the properties.
     */
    void addIdsEqualToQuery(Parameters parameters, String prefix1, String prefix2);

    /**
     * Adds query statements, which contains restrictions, which express the property that the id of the entity
     * with alias prefix1, is equal to the id of the entity with alias prefix2 mapped by the second mapper
     * (the second mapper must be for the same entity, but it can have, for example, prefixed properties).
     * @param parameters Parameters, to which to add the statements.
     * @param prefix1 First alias of the entity + prefix to add to the properties.
     * @param mapper2 Second mapper for the same entity, which will be used to get properties for the right side
     * of the equation.
     * @param prefix2 Second alias of the entity + prefix to add to the properties.
     */
    void addIdsEqualToQuery(Parameters parameters, String prefix1, IdMapper mapper2, String prefix2);

    /**
     * Adds query statements, which contains restrictions, which express the property that the id of the entity
     * with alias prefix, is equal to the given object.
     * @param parameters Parameters, to which to add the statements.
     * @param id Value of id.
     * @param prefix Prefix to add to the properties (may be null).
     * @param equals Should this query express the "=" relation or the "<>" relation.
     */
    void addIdEqualsToQuery(Parameters parameters, Object id, String prefix, boolean equals);

    /**
     * Adds query statements, which contains named parameters, which express the property that the id of the entity
     * with alias prefix, is equal to the given object. It is the responsibility of the using method to read
     * parameter values from the id and specify them on the final query object.
     * @param parameters Parameters, to which to add the statements.
     * @param prefix Prefix to add to the properties (may be null).
     * @param equals Should this query express the "=" relation or the "<>" relation.
     */
    void addNamedIdEqualsToQuery(Parameters parameters, String prefix, boolean equals);
}
