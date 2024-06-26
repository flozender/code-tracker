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
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;

/**
 * A relational unique key constraint
 *
 * @author Brett Meyer
 */
public class UniqueKey extends Constraint {
	private java.util.Map<Column, String> columnOrderMap = new HashMap<Column, String>();

	@Override
	public String sqlConstraintString(
			Dialect dialect,
			String constraintName,
			String defaultCatalog,
			String defaultSchema) {
//		return dialect.getUniqueDelegate().uniqueConstraintSql( this );
		// Not used.
		return "";
	}

	@Override
	public String sqlCreateString(
			Dialect dialect,
			Mapping p,
			String defaultCatalog,
			String defaultSchema) {
		return null;
//		return dialect.getUniqueDelegate().getAlterTableToAddUniqueKeyCommand(
//				this, defaultCatalog, defaultSchema
//		);
	}

	@Override
	public String sqlDropString(
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema) {
		return null;
//		return dialect.getUniqueDelegate().getAlterTableToDropUniqueKeyCommand(
//				this, defaultCatalog, defaultSchema
//		);
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public Map<Column, String> getColumnOrderMap() {
		return columnOrderMap;
	}

	public String generatedConstraintNamePrefix() {
		return "UK_";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "UK-" + getName() );
	}
}
