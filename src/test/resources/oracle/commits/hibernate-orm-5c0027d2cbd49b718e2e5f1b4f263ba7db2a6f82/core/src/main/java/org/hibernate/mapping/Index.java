//$Id: Index.java 10661 2006-10-31 02:19:13Z epbernard $
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.util.StringHelper;

/**
 * A relational table index
 *
 * @author Gavin King
 */
public class Index implements RelationalModel, Serializable {

	private Table table;
	private List columns = new ArrayList();
	private String name;

	public String sqlCreateString(Dialect dialect, Mapping mapping, String defaultCatalog, String defaultSchema)
			throws HibernateException {
		return buildSqlCreateIndexString(
				dialect,
				getName(),
				getTable(),
				getColumnIterator(),
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
			String defaultSchema
	) {
		return "drop index " +
				StringHelper.qualify(
						table.getQualifiedName( dialect, defaultCatalog, defaultSchema ),
						name
				);
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			Table table,
			Iterator columns,
			boolean unique,
			String defaultCatalog,
			String defaultSchema
	) {
		//TODO handle supportsNotNullUnique=false, but such a case does not exist in the wild so far
		StringBuffer buf = new StringBuffer( "create" )
				.append( unique ?
						" unique" :
						"" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ?
						name :
						StringHelper.unqualify( name ) )
				.append( " on " )
				.append( table.getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
				.append( " (" );
		Iterator iter = columns;
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		buf.append( ")" );
		return buf.toString();
	}


	// Used only in Table for sqlCreateString (but commented out at the moment)
	public String sqlConstraintString(Dialect dialect) {
		StringBuffer buf = new StringBuffer( " index (" );
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return buf.append( ')' ).toString();
	}

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

	public Iterator getColumnIterator() {
		return columns.iterator();
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) columns.add( column );
	}

	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) addColumn( (Column) extraColumns.next() );
	}

	/**
	 * @param column
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return getClass().getName() + "(" + getName() + ")";
	}
}
