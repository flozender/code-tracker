//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class IteratingCallbackTest
{
    static Scheduler scheduler = new ScheduledExecutorScheduler();
   
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        scheduler.start();
    }
    
    @AfterClass
    public static void afterClass() throws Exception
    {
        scheduler.stop();
    }
    
    @Test
    public void testNonWaitingProcess() throws Exception
    {
        
        TestCB cb=new TestCB()
        {
            int i=10;
            
            @Override
            protected Action process() throws Exception
            {
                processed++;
                if (i-->1)      
                {
                    succeeded(); // fake a completed IO operation
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };
        
        cb.iterate();
        Assert.assertTrue(cb.waitForComplete()); 
        Assert.assertEquals(10,cb.processed);
    }



    @Test
    public void testWaitingProcess() throws Exception
    {
        TestCB cb=new TestCB()
        {
            int i=4;
            
            @Override
            protected Action process() throws Exception
            {
                processed++;
                if (i-->1)      
                {
                    scheduler.schedule(successTask,50,TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };
        
        cb.iterate();
        
        Assert.assertTrue(cb.waitForComplete());
         
        Assert.assertEquals(4,cb.processed);
    }

    @Test
    public void testWaitingProcessSpuriousInterate() throws Exception
    {
        final TestCB cb=new TestCB()
        {
            int i=4;
            
            @Override
            protected Action process() throws Exception
            {
                processed++;
                if (i-->1)      
                {
                    scheduler.schedule(successTask,50,TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };
        
        cb.iterate();
        scheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                cb.iterate();
                if (!cb.isSucceeded())
                    scheduler.schedule(this,50,TimeUnit.MILLISECONDS);
            }
        },49,TimeUnit.MILLISECONDS);
        
        Assert.assertTrue(cb.waitForComplete());
         
        Assert.assertEquals(4,cb.processed);
    }

    @Test
    public void testNonWaitingProcessFailure() throws Exception
    {
        TestCB cb=new TestCB()
        {
            int i=10;
            
            @Override
            protected Action process() throws Exception
            {
                processed++;
                if (i-->1)      
                {
                    if (i>5)
                        succeeded(); // fake a completed IO operation
                    else
                        failed(new Exception("testing"));
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };
        
        cb.iterate();
        Assert.assertFalse(cb.waitForComplete()); 
        Assert.assertEquals(5,cb.processed);
    }

    @Test
    public void testWaitingProcessFailure() throws Exception
    {
        TestCB cb=new TestCB()
        {
            int i=4;
            
            @Override
            protected Action process() throws Exception
            {
                processed++;
                if (i-->1)      
                {
                    scheduler.schedule(i>2?successTask:failTask,50,TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };
        
        cb.iterate();
        
        Assert.assertFalse(cb.waitForComplete());
        Assert.assertEquals(2,cb.processed);
    }
    

    @Test
    public void testIdleWaiting() throws Exception
    {
        final CountDownLatch idle = new CountDownLatch(1);
        
        TestCB cb=new TestCB()
        {
            int i=5;
            
            @Override
            protected Action process()
            {
                processed++;
                
                switch(i--)
                {
                    case 5:
                        succeeded();
                        return Action.SCHEDULED;
                        
                    case 4:
                        scheduler.schedule(successTask,5,TimeUnit.MILLISECONDS);
                        return Action.SCHEDULED;
                        
                    case 3:
                        scheduler.schedule(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                idle.countDown();
                            }
                        },5,TimeUnit.MILLISECONDS);
                        return Action.IDLE;

                    case 2:
                        succeeded();
                        return Action.SCHEDULED;
                        
                    case 1:
                        scheduler.schedule(successTask,5,TimeUnit.MILLISECONDS);
                        return Action.SCHEDULED;
                        
                    case 0:
                        return Action.SUCCEEDED;
                        
                    default: 
                        throw new IllegalStateException();
                    
                }
            }
        };
        
        cb.iterate();
        idle.await(10,TimeUnit.SECONDS);
        Assert.assertTrue(cb.isIdle());
        
        cb.iterate();
        Assert.assertTrue(cb.waitForComplete());
        Assert.assertEquals(6,cb.processed);
    }
    
    
    
    private abstract static class TestCB extends IteratingCallback
    {
        CountDownLatch completed = new CountDownLatch(1);
        int processed=0;

        @Override
        protected void onCompleteSuccess()
        {
            completed.countDown();
        }
        
        @Override
        public void onCompleteFailure(Throwable x)
        {
            completed.countDown();
        }

        boolean waitForComplete() throws InterruptedException
        {
            completed.await(10,TimeUnit.SECONDS);
            return isSucceeded();
        }
        
        Runnable successTask = new Runnable()
        {
            @Override
            public void run()
            {
                succeeded();
            }
        };
        Runnable failTask = new Runnable()
        {
            @Override
            public void run()
            {
                failed(new Exception("testing failure"));
            }
        };
    }

}
