/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.api.distributions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import eu.stratosphere.api.distributions.SimpleDistribution;
import eu.stratosphere.types.Key;
import eu.stratosphere.types.PactDouble;
import eu.stratosphere.types.PactInteger;
import eu.stratosphere.types.PactString;

public class SimpleDataDistributionTest {

	@Test
	public void testConstructorSingleKey() {

		// check correct data distribution
		try {
			SimpleDistribution dd = new SimpleDistribution(new Key[] {new PactInteger(1), new PactInteger(2), new PactInteger(3)});
			Assert.assertEquals(1, dd.getNumberOfFields());
		}
		catch (Throwable t) {
			Assert.fail();
		}
		
		// check incorrect key types
		try {
			new SimpleDistribution(new Key[] {new PactInteger(1), new PactString("ABC"), new PactInteger(3)});
			Assert.fail("Data distribution accepts inconsistent key types");
		} catch(IllegalArgumentException iae) {
			// do nothing
		}
		
		// check inconsistent number of keys
		try {
			new SimpleDistribution(new Key[][] {{new PactInteger(1)}, {new PactInteger(2), new PactInteger(2)}, {new PactInteger(3)}});
			Assert.fail("Data distribution accepts inconsistent many keys");
		} catch(IllegalArgumentException iae) {
			// do nothing
		}
	}
	
	@Test 
	public void testConstructorMultiKey() {
		
		// check correct data distribution
		SimpleDistribution dd = new SimpleDistribution(
				new Key[][] {{new PactInteger(1), new PactString("A"), new PactInteger(1)}, 
				             {new PactInteger(2), new PactString("A"), new PactInteger(1)}, 
				             {new PactInteger(3), new PactString("A"), new PactInteger(1)}});
		Assert.assertEquals(3, dd.getNumberOfFields());
		
		// check inconsistent key types
		try {
			new SimpleDistribution( 
					new Key[][] {{new PactInteger(1), new PactString("A"), new PactDouble(1.3d)}, 
								 {new PactInteger(2), new PactString("B"), new PactInteger(1)}});
			Assert.fail("Data distribution accepts incorrect key types");
		} catch(IllegalArgumentException iae) {
			// do nothing
		}
		
		// check inconsistent number of keys
		try {
			dd = new SimpleDistribution(
					new Key[][] {{new PactInteger(1), new PactInteger(2)}, 
					             {new PactInteger(2), new PactInteger(2)}, 
					             {new PactInteger(3)}});
			Assert.fail("Data distribution accepts bucket boundaries with inconsistent many keys");
		} catch(IllegalArgumentException iae) {
			// do nothing
		}
		
	}
	
	@Test
	public void testWriteRead() {
		
		SimpleDistribution ddWrite = new SimpleDistribution(
				new Key[][] {{new PactInteger(1), new PactString("A"), new PactInteger(1)}, 
				             {new PactInteger(2), new PactString("A"), new PactInteger(1)}, 
				             {new PactInteger(2), new PactString("B"), new PactInteger(4)},
				             {new PactInteger(2), new PactString("B"), new PactInteger(3)},
				             {new PactInteger(2), new PactString("B"), new PactInteger(2)}});
		Assert.assertEquals(3, ddWrite.getNumberOfFields());
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);
		try {
			ddWrite.write(dos);
		} catch (IOException e) {
			Assert.fail("Error serializing the DataDistribution: " + e.getMessage());
		}

		byte[] seralizedDD = baos.toByteArray();
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(seralizedDD);
		final DataInputStream in = new DataInputStream(bais);
		
		SimpleDistribution ddRead = new SimpleDistribution();
		
		try {
			ddRead.read(in);
		} catch (Exception ex) {
			Assert.fail("The deserialization of the encoded data distribution caused an error");
		}
		
		Assert.assertEquals(3, ddRead.getNumberOfFields());
		
		// compare written and read distributions
		for(int i=0;i<6;i++) {
			Key[] recW = ddWrite.getBucketBoundary(0, 6);
			Key[] recR = ddWrite.getBucketBoundary(0, 6);
			
			Assert.assertEquals(recW[0], recR[0]);
			Assert.assertEquals(recW[1], recR[1]);
			Assert.assertEquals(recW[2], recR[2]);
		}
	}
	
	@Test
	public void testGetBucketBoundary() {
		
		SimpleDistribution dd = new SimpleDistribution(
				new Key[][] {{new PactInteger(1), new PactString("A")}, 
				             {new PactInteger(2), new PactString("B")}, 
				             {new PactInteger(3), new PactString("C")},
				             {new PactInteger(4), new PactString("D")},
				             {new PactInteger(5), new PactString("E")},
				             {new PactInteger(6), new PactString("F")},
				             {new PactInteger(7), new PactString("G")}});
		
		Key[] boundRec = dd.getBucketBoundary(0, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 1);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("A"));
		
		boundRec = dd.getBucketBoundary(1, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 2);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("B"));
		
		boundRec = dd.getBucketBoundary(2, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 3);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("C"));
		
		boundRec = dd.getBucketBoundary(3, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 4);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("D"));
		
		boundRec = dd.getBucketBoundary(4, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 5);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("E"));
		
		boundRec = dd.getBucketBoundary(5, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 6);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("F"));
		
		boundRec = dd.getBucketBoundary(6, 8);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 7);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("G"));
		
		boundRec = dd.getBucketBoundary(0, 4);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 2);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("B"));
		
		boundRec = dd.getBucketBoundary(1, 4);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 4);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("D"));
		
		boundRec = dd.getBucketBoundary(2, 4);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 6);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("F"));
		
		boundRec = dd.getBucketBoundary(0, 2);
		Assert.assertEquals(((PactInteger) boundRec[0]).getValue(), 4);
		Assert.assertTrue(((PactString) boundRec[1]).getValue().equals("D"));
		
		try {
			boundRec = dd.getBucketBoundary(0, 7);
			Assert.fail();
		} catch(IllegalArgumentException iae) {
			// nothing to do
		}
		
		try {
			boundRec = dd.getBucketBoundary(3, 4);
			Assert.fail();
		} catch(IllegalArgumentException iae) {
			// nothing to do
		}
		
		try {
			boundRec = dd.getBucketBoundary(-1, 4);
			Assert.fail();
		} catch(IllegalArgumentException iae) {
			// nothing to do
		}
		
		try {
			boundRec = dd.getBucketBoundary(0, 0);
			Assert.fail();
		} catch(IllegalArgumentException iae) {
			// nothing to do
		}
	}

}
