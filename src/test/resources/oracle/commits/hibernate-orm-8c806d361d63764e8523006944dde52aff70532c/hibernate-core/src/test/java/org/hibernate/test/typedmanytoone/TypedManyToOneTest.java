//$Id: TypedManyToOneTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.typedmanytoone;
import java.util.List;
import junit.framework.Test;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class TypedManyToOneTest extends FunctionalTestCase {
	
	public TypedManyToOneTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "typedmanytoone/Customer.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TypedManyToOneTest.class );
	}
	
	public void testCreateQuery() {
		Customer cust = new Customer();
		cust.setCustomerId("abc123");
		cust.setName("Matt");
		
		Address ship = new Address();
		ship.setStreet("peachtree rd");
		ship.setState("GA");
		ship.setCity("ATL");
		ship.setZip("30326");
		ship.setAddressId( new AddressId("SHIPPING", "xyz123") );
		ship.setCustomer(cust);
		
		Address bill = new Address();
		bill.setStreet("peachtree rd");
		bill.setState("GA");
		bill.setCity("ATL");
		bill.setZip("30326");
		bill.setAddressId( new AddressId("BILLING", "xyz123") );
		bill.setCustomer(cust);
		
		cust.setBillingAddress(bill);
		cust.setShippingAddress(ship);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cust);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='abc123'").list();
		//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
		cust = (Customer) results.get(0);
		assertFalse( Hibernate.isInitialized( cust.getShippingAddress() ) );
		assertTrue( Hibernate.isInitialized( cust.getBillingAddress() ) );
		assertEquals( "30326", cust.getBillingAddress().getZip() );
		assertEquals( "30326", cust.getShippingAddress().getZip() );
		assertEquals( "BILLING", cust.getBillingAddress().getAddressId().getType() );
		assertEquals( "SHIPPING", cust.getShippingAddress().getAddressId().getType() );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.saveOrUpdate(cust);
		ship = cust.getShippingAddress();
		cust.setShippingAddress(null);
		s.delete("ShippingAddress", ship);
		s.flush();
		assertNull( s.get( "ShippingAddress", ship.getAddressId() ) );
		s.delete( cust );
		t.commit();
		s.close();
	}
	
	public void testCreateQueryNull() {
		Customer cust = new Customer();
		cust.setCustomerId("xyz123");
		cust.setName("Matt");
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cust);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='xyz123'").list();
		//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
		cust = (Customer) results.get(0);
		assertNull( cust.getShippingAddress() );
		assertNull( cust.getBillingAddress() );
		s.delete( cust );
		t.commit();
		s.close();
		
	}

}

