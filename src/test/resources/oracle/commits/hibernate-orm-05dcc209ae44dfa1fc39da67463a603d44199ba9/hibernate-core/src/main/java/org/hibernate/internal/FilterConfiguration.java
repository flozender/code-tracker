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
package org.hibernate.internal;

import java.util.Collections;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PersistentClass;

/**
 *
 * @author Rob Worsnop
 */
public class FilterConfiguration {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap;
	private final PersistentClass persistentClass;
	
	public FilterConfiguration(String name, String condition, boolean autoAliasInjection, Map<String, String> aliasTableMap, PersistentClass persistentClass) {
		this.name = name;
		this.condition = condition;
		this.autoAliasInjection = autoAliasInjection;
		this.aliasTableMap = aliasTableMap;
		this.persistentClass = persistentClass;
	}

	public String getName() {
		return name;
	}

	public String getCondition() {
		return condition;
	}

	public boolean useAutoAliasInjection() {
		return autoAliasInjection;
	}

	public Map<String, String> getAliasTableMap(SessionFactoryImplementor factory) {
		if (!CollectionHelper.isEmpty(aliasTableMap)){
			return aliasTableMap;
		} else if (persistentClass != null){
			String table = persistentClass.getTable().getQualifiedName(factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName());
			return Collections.singletonMap(null, table);
		} else{
			return Collections.emptyMap();
		}
		
	}
}