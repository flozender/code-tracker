// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.util.log;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class LogTest
{
    static PrintStream _orig= System.err;
    static ByteArrayOutputStream _out = new ByteArrayOutputStream();
    static PrintStream _pout = new PrintStream(_out);
    

    @BeforeClass
    public static  void setUp()
    {
        System.setErr(_pout);
    }
    
    @AfterClass
    public static void tearDown()
    {
        System.setErr(_orig);
    }
    
    private void logNotContains(String text)
    {
        _pout.flush();
        String err = _out.toString();
        _out.reset();
        
        if (err.indexOf(text)<0)
            return;
        
        _orig.println("FAIL '"+text+"' in '"+err+"'");
        
        assertTrue(false);
    }
    
    private void logContains(String text)
    {
        _pout.flush();
        String err = _out.toString();
        _out.reset();
        
        err = err.replaceAll("\r\n","\n");
        text = text.replaceAll("\r\n","\n");
        
        if (err.indexOf(text)>=0)
            return;
        
        _orig.println("FAIL '"+text+"' not in '"+err+"'");
        assertTrue(false);
    }
    
    @Test
    public void testStdErrLogFormat()
    {
        StdErrLog log = new StdErrLog(LogTest.class.getName());

        log.info("testing:{},{}","test","format");
        logContains("INFO:oejul.LogTest:testing:test,format");
        
        log.info("testing:{}","test","format");
        logContains("INFO:oejul.LogTest:testing:test format");
        
        log.info("testing","test","format");
        logContains("INFO:oejul.LogTest:testing test format");
       
        log.info("testing:{},{}","test",null);
        logContains("INFO:oejul.LogTest:testing:test,null");
       
        log.info("testing {} {}",null,null);
        logContains("INFO:oejul.LogTest:testing null null");
        
        log.info("testing:{}",null,null);
        logContains("INFO:oejul.LogTest:testing:null");
        
        log.info("testing",null,null);
        logContains("INFO:oejul.LogTest:testing");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testStdErrLogDebug()
    {
        StdErrLog log = new StdErrLog("xxx");
        
        log.setLevel(StdErrLog.LEVEL_DEBUG);
        log.debug("testing {} {}","test","debug");
        logContains("DBUG:xxx:testing test debug");
        
        log.info("testing {} {}","test","info");
        logContains("INFO:xxx:testing test info");
        
        log.warn("testing {} {}","test","warn");
        logContains("WARN:xxx:testing test warn");
        
        log.setLevel(StdErrLog.LEVEL_INFO);
        log.debug("YOU SHOULD NOT SEE THIS!",null,null);
        logNotContains("YOU SHOULD NOT SEE THIS!");
        
        // Test for backward compat with old (now deprecated) method
        log.setDebugEnabled(true);
        log.debug("testing {} {}","test","debug-deprecated");
        logContains("DBUG:xxx:testing test debug-deprecated");

        log.setDebugEnabled(false);
        log.debug("testing {} {}","test","debug-deprecated-false");
        logNotContains("DBUG:xxx:testing test debug-depdeprecated-false");
    }
    
    @Test
    public void testStdErrLogName()
    {
        StdErrLog log = new StdErrLog("test");
        log.setPrintLongNames(true);
        Assert.assertEquals("test",log.getName());
        
        Logger next=log.getLogger("next");
        
        Assert.assertEquals("test.next",next.getName());
        
        next.info("testing {} {}","next","info");
        logContains(":test.next:testing next info");
    }
    
    @Test
    public void testStdErrThrowable()
    {
        Throwable th = new Throwable("Message");
        
        th.printStackTrace();
        _pout.flush();
        String ths = _out.toString();
        _out.reset();
        

        StdErrLog log = new StdErrLog("test");
        log.warn("ex",th);

        logContains(ths);
        
        th = new Throwable("Message with \033 escape");

        log.warn("ex",th);
        logNotContains("Message with \033 escape");
        log.info(th.toString());
        logNotContains("Message with \033 escape");
        
        log.warn("ex",th);
        logContains("Message with ? escape");
        log.info(th.toString());
        logContains("Message with ? escape");
    }
}
