//$Id$
package org.hibernate.test.sql.hand.custom.oracle;
import junit.framework.Test;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Custom SQL tests for Oracle
 * 
 * @author Gavin King
 */
public class OracleCustomSQLTest extends CustomStoredProcTestSupport {

	public OracleCustomSQLTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/custom/oracle/Mappings.hbm.xml", "sql/hand/custom/oracle/StoredProcedures.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OracleCustomSQLTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof Oracle9iDialect );
	}

}

