//$Id: DB2Dialect.java 10963 2006-12-08 16:06:14Z steve.ebersole@jboss.com $
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.function.AnsiTrimEmulationFunction;

/**
 * An SQL dialect for DB2.
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {

	public DB2Dialect() {
		super();
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "varchar($l) for bit data" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "blob($l)" );
		registerColumnType( Types.CLOB, "clob($l)" );

		registerFunction("abs", new StandardSQLFunction("abs") );
		registerFunction("absval", new StandardSQLFunction("absval") );
		registerFunction("sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction("ceiling", new StandardSQLFunction("ceiling") );
		registerFunction("ceil", new StandardSQLFunction("ceil") );
		registerFunction("floor", new StandardSQLFunction("floor") );
		registerFunction("round", new StandardSQLFunction("round") );

		registerFunction("acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction("asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction("atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction("cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction("cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );
		registerFunction("exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction("float", new StandardSQLFunction("float", Hibernate.DOUBLE) );
		registerFunction("hex", new StandardSQLFunction("hex", Hibernate.STRING) );
		registerFunction("ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction("log", new StandardSQLFunction("log", Hibernate.DOUBLE) );
		registerFunction("log10", new StandardSQLFunction("log10", Hibernate.DOUBLE) );
		registerFunction("radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction("rand", new NoArgSQLFunction("rand", Hibernate.DOUBLE) );
		registerFunction("sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction("soundex", new StandardSQLFunction("soundex", Hibernate.STRING) );
		registerFunction("sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction("stddev", new StandardSQLFunction("stddev", Hibernate.DOUBLE) );
		registerFunction("tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction("variance", new StandardSQLFunction("variance", Hibernate.DOUBLE) );

		registerFunction("julian_day", new StandardSQLFunction("julian_day", Hibernate.INTEGER) );
		registerFunction("microsecond", new StandardSQLFunction("microsecond", Hibernate.INTEGER) );
		registerFunction("midnight_seconds", new StandardSQLFunction("midnight_seconds", Hibernate.INTEGER) );
		registerFunction("minute", new StandardSQLFunction("minute", Hibernate.INTEGER) );
		registerFunction("month", new StandardSQLFunction("month", Hibernate.INTEGER) );
		registerFunction("monthname", new StandardSQLFunction("monthname", Hibernate.STRING) );
		registerFunction("quarter", new StandardSQLFunction("quarter", Hibernate.INTEGER) );
		registerFunction("hour", new StandardSQLFunction("hour", Hibernate.INTEGER) );
		registerFunction("second", new StandardSQLFunction("second", Hibernate.INTEGER) );
		registerFunction("current_date", new NoArgSQLFunction("current date", Hibernate.DATE, false) );
		registerFunction("date", new StandardSQLFunction("date", Hibernate.DATE) );
		registerFunction("day", new StandardSQLFunction("day", Hibernate.INTEGER) );
		registerFunction("dayname", new StandardSQLFunction("dayname", Hibernate.STRING) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek", Hibernate.INTEGER) );
		registerFunction("dayofweek_iso", new StandardSQLFunction("dayofweek_iso", Hibernate.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear", Hibernate.INTEGER) );
		registerFunction("days", new StandardSQLFunction("days", Hibernate.LONG) );
		registerFunction("current_time", new NoArgSQLFunction("current time", Hibernate.TIME, false) );
		registerFunction("time", new StandardSQLFunction("time", Hibernate.TIME) );
		registerFunction("current_timestamp", new NoArgSQLFunction("current timestamp", Hibernate.TIMESTAMP, false) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp", Hibernate.TIMESTAMP) );
		registerFunction("timestamp_iso", new StandardSQLFunction("timestamp_iso", Hibernate.TIMESTAMP) );
		registerFunction("week", new StandardSQLFunction("week", Hibernate.INTEGER) );
		registerFunction("week_iso", new StandardSQLFunction("week_iso", Hibernate.INTEGER) );
		registerFunction("year", new StandardSQLFunction("year", Hibernate.INTEGER) );

		registerFunction("double", new StandardSQLFunction("double", Hibernate.DOUBLE) );
		registerFunction("varchar", new StandardSQLFunction("varchar", Hibernate.STRING) );
		registerFunction("real", new StandardSQLFunction("real", Hibernate.FLOAT) );
		registerFunction("bigint", new StandardSQLFunction("bigint", Hibernate.LONG) );
		registerFunction("char", new StandardSQLFunction("char", Hibernate.CHARACTER) );
		registerFunction("integer", new StandardSQLFunction("integer", Hibernate.INTEGER) );
		registerFunction("smallint", new StandardSQLFunction("smallint", Hibernate.SHORT) );

		registerFunction("digits", new StandardSQLFunction("digits", Hibernate.STRING) );
		registerFunction("chr", new StandardSQLFunction("chr", Hibernate.CHARACTER) );
		registerFunction("upper", new StandardSQLFunction("upper") );
		registerFunction("lower", new StandardSQLFunction("lower") );
		registerFunction("ucase", new StandardSQLFunction("ucase") );
		registerFunction("lcase", new StandardSQLFunction("lcase") );
		registerFunction("length", new StandardSQLFunction("length", Hibernate.LONG) );
		registerFunction("ltrim", new StandardSQLFunction("ltrim") );
		registerFunction("rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "substr", new StandardSQLFunction( "substr", Hibernate.STRING ) );
		registerFunction( "posstr", new StandardSQLFunction( "posstr", Hibernate.INTEGER ) );

		registerFunction( "substring", new StandardSQLFunction( "substr", Hibernate.STRING ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( Hibernate.INTEGER, "length(?1)*8" ) );
		registerFunction( "trim", new AnsiTrimEmulationFunction() );

		registerFunction( "concat", new VarArgsSQLFunction(Hibernate.STRING, "", "||", "") );

		registerFunction( "str", new SQLFunctionTemplate( Hibernate.STRING, "rtrim(char(?1))" ) );

		registerKeyword("current");
		registerKeyword("date");
		registerKeyword("time");
		registerKeyword("timestamp");
		registerKeyword("fetch");
		registerKeyword("first");
		registerKeyword("rows");
		registerKeyword("only");

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);
	}

	public String getLowercaseFunction() {
		return "lcase";
	}

	public String getAddColumnString() {
		return "add column";
	}
	public boolean dropConstraints() {
		return false;
	}
	public boolean supportsIdentityColumns() {
		return true;
	}
	public String getIdentitySelectString() {
		return "values identity_val_local()";
	}
	public String getIdentityColumnString() {
		return "generated by default as identity"; //not null ... (start with 1) is implicit
	}
	public String getIdentityInsertString() {
		return "default";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "values nextval for " + sequenceName;
	}
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select seqname from sysibm.syssequences";
	}

	public boolean supportsLimit() {
		return true;
	}

	/*public String getLimitString(String sql, boolean hasOffset) {
		StringBuffer rownumber = new StringBuffer(50)
			.append(" rownumber() over(");
		int orderByIndex = sql.toLowerCase().indexOf("order by");
		if (orderByIndex>0) rownumber.append( sql.substring(orderByIndex) );
		rownumber.append(") as row_,");
		StringBuffer pagingSelect = new StringBuffer( sql.length()+100 )
			.append("select * from ( ")
			.append(sql)
			.insert( getAfterSelectInsertPoint(sql)+16, rownumber.toString() )
			.append(" ) as temp_ where row_ ");
		if (hasOffset) {
			pagingSelect.append("between ?+1 and ?");
		}
		else {
			pagingSelect.append("<= ?");
		}
		return pagingSelect.toString();
	}*/

	/**
	 * Render the <tt>rownumber() over ( .... ) as rownumber_,</tt> 
	 * bit, that goes in the select list
	 */
	private String getRowNumber(String sql) {
		StringBuffer rownumber = new StringBuffer(50)
			.append("rownumber() over(");

		int orderByIndex = sql.toLowerCase().indexOf("order by");

		if ( orderByIndex>0 && !hasDistinct(sql) ) {
			rownumber.append( sql.substring(orderByIndex) );
		}

		rownumber.append(") as rownumber_,");

		return rownumber.toString();
	}

	public String getLimitString(String sql, boolean hasOffset) {

		int startOfSelect = sql.toLowerCase().indexOf("select");

		StringBuffer pagingSelect = new StringBuffer( sql.length()+100 )
					.append( sql.substring(0, startOfSelect) ) //add the comment
					.append("select * from ( select ") //nest the main query in an outer select
					.append( getRowNumber(sql) ); //add the rownnumber bit into the outer query select list

		if ( hasDistinct(sql) ) {
			pagingSelect.append(" row_.* from ( ") //add another (inner) nested select
				.append( sql.substring(startOfSelect) ) //add the main query
				.append(" ) as row_"); //close off the inner nested select
		}
		else {
			pagingSelect.append( sql.substring( startOfSelect + 6 ) ); //add the main query
		}

		pagingSelect.append(" ) as temp_ where rownumber_ ");

		//add the restriction to the outer select
		if (hasOffset) {
			pagingSelect.append("between ?+1 and ?");
		}
		else {
			pagingSelect.append("<= ?");
		}

		return pagingSelect.toString();
	}

	private static boolean hasDistinct(String sql) {
		return sql.toLowerCase().indexOf("select distinct")>=0;
	}

	public String getForUpdateString() {
		return " for read only with rs";
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	public boolean supportsNotNullUnique() {
		return false;
	}

	public String getSelectClauseNullString(int sqlType) {
		String literal;
		switch(sqlType) {
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "'x'";
				break;
			case Types.DATE:
				literal = "'2000-1-1'";
				break;
			case Types.TIMESTAMP:
				literal = "'2000-1-1 00:00:00'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
				break;
			default:
				literal = "0";
		}
		return "nullif(" + literal + ',' + literal + ')';
	}

	public static void main(String[] args) {
		System.out.println( new DB2Dialect().getLimitString("/*foo*/ select * from foos", true) );
		System.out.println( new DB2Dialect().getLimitString("/*foo*/ select distinct * from foos", true) );
		System.out.println( new DB2Dialect().getLimitString("/*foo*/ select * from foos foo order by foo.bar, foo.baz", true) );
		System.out.println( new DB2Dialect().getLimitString("/*foo*/ select distinct * from foos foo order by foo.bar, foo.baz", true) );
	}

	public boolean supportsUnionAll() {
		return true;
	}

	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts 
		while (!isResultSet && ps.getUpdateCount() != -1) {
		    isResultSet = ps.getMoreResults();
		}
		ResultSet rs = ps.getResultSet();
		// You may still have other ResultSets or update counts left to process here 
		// but you can't do it now or the ResultSet you just got will be closed 
		return rs;
	}

	public boolean supportsCommentOn() {
		return true;
	}

	public boolean supportsTemporaryTables() {
		return true;
	}

	public String getCreateTemporaryTableString() {
		return "declare global temporary table";
	}

	public String getCreateTemporaryTablePostfix() {
		return "not logged";
	}

	public String generateTemporaryTableName(String baseTableName) {
		return "session." + super.generateTemporaryTableName(baseTableName);
	}

	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	public String getCurrentTimestampSelectString() {
		return "values current timestamp";
	}

	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	public boolean supportsParametersInInsertSelect() {
		// DB2 known to not support parameters within the select
		// clause of an SQL INSERT ... SELECT ... statement
		return false;
	}

	public String getCurrentTimestampSQLFunctionName() {
		return "sysdate";
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsEmptyInList() {
		return false;
	}
}
