/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;
import java.util.ArrayList;
import java.util.List;

/**
 * Support for writing {@link Constraint} implementations
 *
 * @todo do we need to support defining these on particular schemas/catalogs?
 *
 * @author Steve Ebersole
 */
public abstract class AbstractConstraint implements Constraint {
	private final TableSpecification table;
	private final String name;
	private List<Column> columns = new ArrayList<Column>();

	protected AbstractConstraint(TableSpecification table, String name) {
		this.table = table;
		this.name = name;
	}

	public TableSpecification getTable() {
		return table;
	}

	public String getName() {
		return name;
	}

	public Iterable<Column> getColumns() {
		return columns;
	}

	protected List<Column> internalColumnAccess() {
		return columns;
	}

	public void addColumn(Column column) {
		if ( column.getValueContainer() != getTable() ) {
			throw new IllegalArgumentException( "Unable to add column to constraint; tables did not match" );
		}
		columns.add( column );
	}
}
