// $Id$
package org.hibernate.search.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.test.SerializationTestHelper.Foo.TestInnerClass;

import junit.framework.TestCase;

/**
 * @author Sanne Grinovero
 */
public class SerializationTestHelper extends TestCase {
	
	/**
	 * Duplicates an object using Serialization, it moves
	 * state to and from a buffer. Should be used to test
	 * correct serializability.
	 * @param o The object to "clone"
	 * @return the clone.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object duplicateBySerialization(Object o) throws IOException, ClassNotFoundException {
		//Serialize to buffer:
		java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
	    ObjectOutputStream objectOutStream = new ObjectOutputStream( outStream );
	    objectOutStream.writeObject( o );
	    objectOutStream.flush();
	    objectOutStream.close();
	    //buffer version of Object:
	    byte[] objectBuffer = outStream.toByteArray();
	    //deserialize to new instance:
	    java.io.ByteArrayInputStream inStream = new ByteArrayInputStream( objectBuffer );
	    ObjectInputStream objectInStream = new ObjectInputStream( inStream );
	    Object copy = objectInStream.readObject();
	    return copy;
	}
	
	public void testSelf() throws IOException, ClassNotFoundException {
		Foo a = new Foo();
		a.list.add( new TestInnerClass(30) );
		Foo b = (Foo) duplicateBySerialization( a );
		assertEquals( Integer.valueOf(6), a.integer);
		assertEquals( Integer.valueOf(7), b.integer);
		assertEquals( a.list, b.list );
	}
	
	static class Foo implements Serializable {
		
		List<TestInnerClass> list = new ArrayList<TestInnerClass>();
		transient Integer integer = Integer.valueOf( 6 );

		static class TestInnerClass implements Serializable {
			private final int v;

			public TestInnerClass(int i) {
				v = i;
			}
			
			public void print() {
				System.out.println(v);
			}
			
			public String toString(){
				return ""+v;
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + v;
				return result;
			}
			
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				final TestInnerClass other = (TestInnerClass) obj;
				if (v != other.v)
					return false;
				return true;
			}
		}

		private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
			aInputStream.defaultReadObject();
			integer = Integer.valueOf( 7 );
		}

		private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
			aOutputStream.defaultWriteObject();
		}
	}

}
