//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.eclipse.jetty.util.ArrayQueue;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * {@link QueuedHttpInput} holds a queue of items passed to it by calls to {@link #content(HttpInput.Content)}.
 * <p/>
 * {@link QueuedHttpInput} stores the items directly; if the items contain byte buffers, it does not copy them
 * but simply holds references to the item, thus the caller must organize for those buffers to valid while
 * held by this class.
 * <p/>
 * To assist the caller, subclasses may override methods such as  {@link #onAsyncRead()},
 * {@link #consume(HttpInput.Content, int)}, etc.
 * that can be implemented so that the caller will know when buffers are queued and consumed.
 */
public class QueuedHttpInput extends HttpInput
{
    private final static Logger LOG = Log.getLogger(QueuedHttpInput.class);

    private final ArrayQueue<Content> _inputQ = new ArrayQueue<>(lock());

    public QueuedHttpInput()
    {
    }

    public void content(Content item)
    {
        // The buffer is not copied here.  This relies on the caller not recycling the buffer
        // until the it is consumed.  The onContentConsumed and onAllContentConsumed() callbacks are
        // the signals to the caller that the buffers can be recycled.

        synchronized (lock())
        {
            boolean wasEmpty = _inputQ.isEmpty();
            _inputQ.add(item);
            LOG.debug("{} queued {}", this, item);
            if (wasEmpty)
            {
                if (!onAsyncRead())
                    lock().notify();
            }
        }
    }

    public void recycle()
    {
        synchronized (lock())
        {
            Content item = _inputQ.pollUnsafe();
            while (item != null)
            {
                item.failed(null);
                item = _inputQ.pollUnsafe();
            }
            super.recycle();
        }
    }

    @Override
    protected Content nextContent()
    {
        synchronized (lock())
        {
            // Items are removed only when they are fully consumed.
            Content item = _inputQ.peekUnsafe();
            // Skip consumed items at the head of the queue.
            while (item != null && remaining(item) == 0)
            {
                _inputQ.pollUnsafe();
                LOG.debug("{} consumed {}", this, item);
                item = _inputQ.peekUnsafe();
            }
            return item;
        }
    }

    protected void blockForContent() throws IOException
    {
        synchronized (lock())
        {
            while (_inputQ.isEmpty() && !isFinished() && !isEOF())
            {
                try
                {
                    LOG.debug("{} waiting for content", this);
                    lock().wait();
                }
                catch (InterruptedException e)
                {
                    throw (IOException)new InterruptedIOException().initCause(e);
                }
            }
        }
    }

    public void earlyEOF()
    {
        synchronized (lock())
        {
            super.earlyEOF();
            lock().notify();
        }
    }

    public void messageComplete()
    {
        synchronized (lock())
        {
            super.messageComplete();
            lock().notify();
        }
    }
}
