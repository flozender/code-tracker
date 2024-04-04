/*
 * Copyright 2002-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang.time;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * TestCase for DurationFormatUtils.
 *
 * @author Apache Ant - DateUtilsTest
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Stephen Colebourne
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 */
public class DurationFormatUtilsTest extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
    	TestSuite suite = new TestSuite(DurationFormatUtilsTest.class);
    	suite.setName("DurationFormatUtils Tests");
        return suite;
    }

    public DurationFormatUtilsTest(String s) {
        super(s);
    }

    //-----------------------------------------------------------------------
    public void testConstructor() {
        assertNotNull(new DurationFormatUtils());
        Constructor[] cons = DurationFormatUtils.class.getDeclaredConstructors();
        assertEquals(1, cons.length);
        assertEquals(true, Modifier.isPublic(cons[0].getModifiers()));
        assertEquals(false, Modifier.isPublic(DurationFormatUtils.class.getModifiers()));
        assertEquals(false, Modifier.isFinal(DurationFormatUtils.class.getModifiers()));
    }
    
    //-----------------------------------------------------------------------
    public void testFormatWords(){
        String text = null;
        
        text = DurationFormatUtils.formatWords(50*1000, true, false);
        assertEquals("50 seconds", text);
        text = DurationFormatUtils.formatWords(65*1000, true, false);
        assertEquals("1 minute 5 seconds", text);
        text = DurationFormatUtils.formatWords(120*1000, true, false);
        assertEquals("2 minutes 0 seconds", text);
        text = DurationFormatUtils.formatWords(121*1000, true, false);
        assertEquals("2 minutes 1 second", text);
        text = DurationFormatUtils.formatWords(72*60*1000, true, false);
        assertEquals("1 hour 12 minutes 0 seconds", text);
        text = DurationFormatUtils.formatWords(24*60*60*1000, true, false);
        assertEquals("1 day 0 hours 0 minutes 0 seconds", text);
        
        text = DurationFormatUtils.formatWords(50*1000, true, true);
        assertEquals("50 seconds", text);
        text = DurationFormatUtils.formatWords(65*1000, true, true);
        assertEquals("1 minute 5 seconds", text);
        text = DurationFormatUtils.formatWords(120*1000, true, true);
        assertEquals("2 minutes", text);
        text = DurationFormatUtils.formatWords(121*1000, true, true);
        assertEquals("2 minutes 1 second", text);
        text = DurationFormatUtils.formatWords(72*60*1000, true, true);
        assertEquals("1 hour 12 minutes", text);
        text = DurationFormatUtils.formatWords(24*60*60*1000, true, true);
        assertEquals("1 day", text);
        
        text = DurationFormatUtils.formatWords(50*1000, false, true);
        assertEquals("0 days 0 hours 0 minutes 50 seconds", text);
        text = DurationFormatUtils.formatWords(65*1000, false, true);
        assertEquals("0 days 0 hours 1 minute 5 seconds", text);
        text = DurationFormatUtils.formatWords(120*1000, false, true);
        assertEquals("0 days 0 hours 2 minutes", text);
        text = DurationFormatUtils.formatWords(121*1000, false, true);
        assertEquals("0 days 0 hours 2 minutes 1 second", text);
        text = DurationFormatUtils.formatWords(72*60*1000, false, true);
        assertEquals("0 days 1 hour 12 minutes", text);
        text = DurationFormatUtils.formatWords(24*60*60*1000, false, true);
        assertEquals("1 day", text);
        
        text = DurationFormatUtils.formatWords(50*1000, false, false);
        assertEquals("0 days 0 hours 0 minutes 50 seconds", text);
        text = DurationFormatUtils.formatWords(65*1000, false, false);
        assertEquals("0 days 0 hours 1 minute 5 seconds", text);
        text = DurationFormatUtils.formatWords(120*1000, false, false);
        assertEquals("0 days 0 hours 2 minutes 0 seconds", text);
        text = DurationFormatUtils.formatWords(121*1000, false, false);
        assertEquals("0 days 0 hours 2 minutes 1 second", text);
        text = DurationFormatUtils.formatWords(72*60*1000, false, false);
        assertEquals("0 days 1 hour 12 minutes 0 seconds", text);
        text = DurationFormatUtils.formatWords(48*60*60*1000 + 72*60*1000 , false, false);
        assertEquals("2 days 1 hour 12 minutes 0 seconds", text);
    }

    public void testFormatISOStyle(){
        long time = 0;
        assertEquals("0:00:00.000", DurationFormatUtils.formatISO(time));
        
        time = 1;
        assertEquals("0:00:00.001", DurationFormatUtils.formatISO(time));
        
        time = 15;
        assertEquals("0:00:00.015", DurationFormatUtils.formatISO(time));
        
        time = 165;
        assertEquals("0:00:00.165", DurationFormatUtils.formatISO(time));
        
        time = 1675;
        assertEquals("0:00:01.675", DurationFormatUtils.formatISO(time));
        
        time = 13465;
        assertEquals("0:00:13.465", DurationFormatUtils.formatISO(time));
        
        time = 72789;
        assertEquals("0:01:12.789", DurationFormatUtils.formatISO(time));
        
        time = 12789 + 32 * 60000;
        assertEquals("0:32:12.789", DurationFormatUtils.formatISO(time));
        
        time = 12789 + 62 * 60000;
        assertEquals("1:02:12.789", DurationFormatUtils.formatISO(time));
    }

    public void testISODurationFormat(){
        TimeZone timeZone = TimeZone.getTimeZone("GMT-3");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.set(2002, 1, 23, 9, 11, 12);
        cal.set(Calendar.MILLISECOND, 1);
        String text;
        // repeat a test from testDateTimeISO to compare extended and not extended.
        text = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(cal);
        assertEquals("2002-02-23T09:11:12-03:00", text);
        // test fixture is the same as above, but now with extended format.
        text = DurationFormatUtils.ISO_EXTENDED_FORMAT.format(cal);
        assertEquals("P2002Y2M23DT9H11M12.1S", text);
        // test fixture from example in http://www.w3.org/TR/xmlschema-2/#duration
        cal.set(1, 1, 3, 10, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        text = DurationFormatUtils.ISO_EXTENDED_FORMAT.format(cal);
        assertEquals("P1Y2M3DT10H30M0.0S", text);
        // want a way to say 'don't print the seconds in format()' or other fields for that matter:
        //assertEquals("P1Y2M3DT10H30M", text);
    }

    
}
