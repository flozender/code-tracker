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
package org.hibernate.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.sql.Template;

/**
 * Implementation of FilterHelper.
 *
 * @author Steve Ebersole
 * @author Rob Worsnop
 */
public class FilterHelper {

	private final String[] filterNames;
	private final String[] filterConditions;
	private final boolean[] filterAutoAliasFlags;
	private final Map<String,String>[] filterAliasTableMaps;

	/**
	 * The map of defined filters.  This is expected to be in format
	 * where the filter names are the map keys, and the defined
	 * conditions are the values.
	 *
	 * @param filters The map of defined filters.
	 * @param dialect The sql dialect
	 * @param factory The session factory
	 */
	public FilterHelper(List filters, SessionFactoryImplementor factory) {
		int filterCount = filters.size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		filterAutoAliasFlags = new boolean[filterCount];
		filterAliasTableMaps = new Map[filterCount];
		Iterator iter = filters.iterator();
		filterCount = 0;
		while ( iter.hasNext() ) {
			filterAutoAliasFlags[filterCount] = false;
			final FilterConfiguration filter = (FilterConfiguration) iter.next();
			filterNames[filterCount] = (String) filter.getName();
			filterConditions[filterCount] = filter.getCondition();
			filterAliasTableMaps[filterCount] = filter.getAliasTableMap(factory);
			if ((filterAliasTableMaps[filterCount].isEmpty() || isTableFromPersistentClass(filterAliasTableMaps[filterCount])) && filter.useAutoAliasInjection()){
				filterConditions[filterCount] = Template.renderWhereStringTemplate(
						filter.getCondition(),
						FilterImpl.MARKER,
						factory.getDialect(),
						factory.getSqlFunctionRegistry()
					);
				filterAutoAliasFlags[filterCount] = true;
			}
			filterConditions[filterCount] = StringHelper.replace(
					filterConditions[filterCount],
					":",
					":" + filterNames[filterCount] + "."
			);
			filterCount++;
		}
	}
	
	private static boolean isTableFromPersistentClass(Map<String,String> aliasTableMap){
		return aliasTableMap.size() == 1 && aliasTableMap.containsKey(null);
	}

	public boolean isAffectedBy(Map enabledFilters) {
		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				return true;
			}
		}
		return false;
	}

	public String render(FilterAliasGenerator aliasGenerator, Map enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		render( buffer, aliasGenerator, enabledFilters );
		return buffer.toString();
	}

	public void render(StringBuilder buffer, FilterAliasGenerator aliasGenerator, Map enabledFilters) {
		if ( filterNames != null && filterNames.length > 0 ) {
			for ( int i = 0, max = filterNames.length; i < max; i++ ) {
				if ( enabledFilters.containsKey( filterNames[i] ) ) {
					final String condition = filterConditions[i];
					if ( StringHelper.isNotEmpty( condition ) ) {
						buffer.append(" and " ).append(render(aliasGenerator, i));
					}
				}
			}
		}
	}
	
	private String render(FilterAliasGenerator aliasGenerator, int filterIndex){
		Map<String,String> aliasTableMap = filterAliasTableMaps[filterIndex];
		if (filterAutoAliasFlags[filterIndex]){
			return StringHelper.replace(filterConditions[filterIndex], FilterImpl.MARKER, aliasGenerator.getAlias(aliasTableMap.get(null)));
		} else{
			String condition = filterConditions[filterIndex];
			for (Map.Entry<String, String> entry : aliasTableMap.entrySet()){
				condition = condition.replace("{"+entry.getKey()+"}", aliasGenerator.getAlias(entry.getValue()));
			}
			return condition;
		}
	}
}
