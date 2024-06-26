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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;

/**
 * A relational table index
 *
 * @author Gavin King
 */
public class Index implements RelationalModel, Exportable, Serializable {
	private Table table;
	private java.util.List<Column> columns = new ArrayList<Column>();
	private java.util.Map<Column, String> columnOrderMap = new HashMap<Column, String>(  );
	private String name;

	public String sqlCreateString(Dialect dialect, Mapping mapping, String defaultCatalog, String defaultSchema)
			throws HibernateException {
		return buildSqlCreateIndexString(
				dialect,
				getName(),
				getTable(),
				getColumnIterator(),
				columnOrderMap,
				false,
				defaultCatalog,
				defaultSchema
		);
	}

	public static String buildSqlDropIndexString(
			Dialect dialect,
			Table table,
			String name,
			String defaultCatalog,
			String defaultSchema) {
		return buildSqlDropIndexString( name, table.getQualifiedName( dialect, defaultCatalog, defaultSchema ) );
	}

	public static String buildSqlDropIndexString(
			String name,
			String tableName) {
		return "drop index " + StringHelper.qualify( tableName, name );
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			Table table,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique,
			String defaultCatalog,
			String defaultSchema) {
		return buildSqlCreateIndexString(
				dialect,
				name,
				table.getQualifiedName( dialect, defaultCatalog, defaultSchema ),
				columns,
				columnOrderMap,
				unique
		);
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			String tableName,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique) {
		StringBuilder buf = new StringBuilder( "create" )
				.append( unique ? " unique" : "" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ? name : StringHelper.unqualify( name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );
		while ( columns.hasNext() ) {
			Column column = columns.next();
			buf.append( column.getQuotedName( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				buf.append( " " ).append( columnOrderMap.get( column ) );
			}
			if ( columns.hasNext() ) {
				buf.append( ", " );
			}
		}
		buf.append( ")" );
		return buf.toString();
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			Table table,
			Iterator<Column> columns,
			boolean unique,
			String defaultCatalog,
			String defaultSchema) {
		return buildSqlCreateIndexString(
				dialect,
				name,
				table,
				columns,
				Collections.EMPTY_MAP,
				unique,
				defaultCatalog,
				defaultSchema
		);
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			Table table,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique,
			Metadata metadata) {
		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				table.getQualifiedTableName(),
				dialect
		);

		return buildSqlCreateIndexString(
				dialect,
				name,
				tableName,
				columns,
				columnOrderMap,
				unique
		);
	}


	// Used only in Table for sqlCreateString (but commented out at the moment)
	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder( " index (" );
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

	@Override
	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return "drop index " +
				StringHelper.qualify(
						table.getQualifiedName( dialect, defaultCatalog, defaultSchema ),
						name
				);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) {
			addColumn( (Column) extraColumns.next() );
		}
	}

	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getClass().getName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "IDX-" + getName() );
	}
}
