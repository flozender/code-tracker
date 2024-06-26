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
import java.nio.ByteBuffer;
import java.util.Objects;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.util.ArrayQueue;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * {@link HttpInput} provides an implementation of {@link ServletInputStream} for {@link HttpChannel}.
 * <p/>
 * Content may arrive in patterns such as [content(), content(), messageComplete()] so that this class
 * maintains two states: the content state that tells whether there is content to consume and the EOF
 * state that tells whether an EOF has arrived.
 * Only once the content has been consumed the content state is moved to the EOF state.
 */
public abstract class HttpInput extends ServletInputStream implements Runnable
{
    private final static Logger LOG = Log.getLogger(HttpInput.class);
    private final static Content EOF_CONTENT = new PoisonPillContent("EOF");
    private final static Content EARLY_EOF_CONTENT = new PoisonPillContent("EARLY_EOF");
    
    private final byte[] _oneByteBuffer = new byte[1];
    private final ArrayQueue<Content> _inputQ = new ArrayQueue<>();
    private ReadListener _listener;
    private boolean _unready;
    private State _state = STREAM;
    private long _contentConsumed;

    public HttpInput()
    {
    }

    protected abstract void onReadPossible();

    public Object lock()
    {
        return _inputQ;
    }
    
    public void recycle()
    {
        synchronized (_inputQ)
        {
            Content item = _inputQ.pollUnsafe();
            while (item != null)
            {
                item.failed(null);
                item = _inputQ.pollUnsafe();
            }
            _listener = null;
            _unready = false;
            _state = STREAM;
            _contentConsumed = 0;
        }
    }

    @Override
    public int available()
    {
        try
        {
            synchronized (_inputQ)
            {
                Content item = nextContent();
                return item == null ? 0 : remaining(item);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public int read() throws IOException
    {
        int read = read(_oneByteBuffer, 0, 1);
        if (read==0)
            throw new IllegalStateException("unready");
        return read < 0 ? -1 : _oneByteBuffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        synchronized (_inputQ)
        {
            Content item = nextContent();
            if (item == null && _state.blockForContent(this))
                item = nextContent();
            if (item == null)
                return _state.noContent();
            
            int l = get(item, b, off, len);
            return l;
        }
    }

    /**
     * Called when derived implementations should attempt to 
     * produce more Content and add it via {@link #content(Content)}.
     * For protocols that are constantly producing (eg HTTP2) this can
     * be left as a noop;
     * @throws IOException
     */
    protected void produceContent() throws IOException
    {
    }
    
    /**
     * Access the next content to be consumed from.   Returning the next item does not consume it
     * and it may be returned multiple times until it is consumed.
     * <p/>
     * Calls to {@link #get(Content, byte[], int, int)}
     * or {@link #skip(Content, int)} are required to consume data from the content.
     *
     * @return the content or null if none available.
     * @throws IOException if retrieving the content fails
     */
    protected Content nextContent() throws IOException
    {
        if (!Thread.holdsLock(_inputQ))
            throw new IllegalStateException();
        Content content = pollInputQ();
        if (content==null && !isFinished())
        {
            produceContent();
            content = pollInputQ();
        }
        return content;
    }
    
    protected Content pollInputQ()
    {
        if (!Thread.holdsLock(_inputQ))
            throw new IllegalStateException();

        // Items are removed only when they are fully consumed.
        Content content = _inputQ.peekUnsafe();
        // Skip consumed items at the head of the queue.
        while (content != null && remaining(content) == 0)
        {
            _inputQ.pollUnsafe();
            content.succeeded();
            if (LOG.isDebugEnabled())
                LOG.debug("{} consumed {}", this, content);

            if (content==EOF_CONTENT)
                _state=EOF;
            else if (content==EARLY_EOF_CONTENT)
                _state=EARLY_EOF;

            content = _inputQ.peekUnsafe();
        }
        
        return content;
    }
    

    /**
     * @param item the content
     * @return how many bytes remain in the given content
     */
    protected int remaining(Content item)
    {
        return item.remaining();
    }


    /**
     * Copies the given content into the given byte buffer.
     *
     * @param content   the content to copy from
     * @param buffer the buffer to copy into
     * @param offset the buffer offset to start copying from
     * @param length the space available in the buffer
     * @return the number of bytes actually copied
     */
    protected int get(Content content, byte[] buffer, int offset, int length)
    {
        int l = Math.min(content.remaining(), length);
        content.getContent().get(buffer, offset, l);
        _contentConsumed+=l;
        if (l>0 && !content.hasContent())
            pollInputQ(); // hungry succeed
        return l;
    }

    /**
     * Consumes the given content.
     * Calls the content succeeded if all content consumed.
     *
     * @param content   the content to consume
     * @param length the number of bytes to consume
     */
    protected void skip(Content content, int length)
    {
        int l = Math.min(content.remaining(), length);
        ByteBuffer buffer = content.getContent();
        buffer.position(buffer.position()+l);
        _contentConsumed+=l;
        if (l>0 && !content.hasContent())
            pollInputQ(); // hungry succeed

    }

    /**
     * Blocks until some content or some end-of-file event arrives.
     *
     * @throws IOException if the wait is interrupted
     */
    protected void blockForContent() throws IOException
    {
        if (!Thread.holdsLock(_inputQ))
            throw new IllegalStateException();
        while (_inputQ.isEmpty() && !isFinished())
        {
            try
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("{} waiting for content", this);
                _inputQ.wait();
            }
            catch (InterruptedException e)
            {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
    }

    /**
     * Adds some content to this input stream.
     *
     * @param content the content to add
     */
    public void content(Content item)
    {
        synchronized (_inputQ)
        {
            boolean wasEmpty = _inputQ.isEmpty();
            _inputQ.add(item);
            if (LOG.isDebugEnabled())
                LOG.debug("{} queued {}", this, item);
            if (wasEmpty)
            {
                if (!onAsyncRead())
                    _inputQ.notify();
            }
        }
    }

    protected boolean onAsyncRead()
    {
        boolean needReadCB=false;
        synchronized (_inputQ)
        {
            if (_listener == null)
                return false;
            needReadCB=_unready;
        }
        if (needReadCB)
            onReadPossible();
        return true;
    }

    public long getContentConsumed()
    {
        synchronized (_inputQ)
        {
            return _contentConsumed;
        }
    }

    /**
     * This method should be called to signal that an EOF has been
     * detected before all the expected content arrived.
     * <p/>
     * Typically this will result in an EOFException being thrown
     * from a subsequent read rather than a -1 return.
     */
    public void earlyEOF()
    {
        synchronized (_inputQ)
        {
            if (!isFinished())
            {
                if (_inputQ.isEmpty())
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Early EOF", this);
                    _state=EARLY_EOF;
                }
                else
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Early EOF pending", this);
                    _inputQ.add(EARLY_EOF_CONTENT);
                }
                
                if (_listener == null)
                {
                    _inputQ.notify();
                    return;
                }
            }
        }
        onReadPossible();
    }

    /**
     * This method should be called to signal that all the expected
     * content arrived.
     */
    public void eof()
    {
        synchronized (_inputQ)
        {
            if (!isFinished())
            {
                if (_inputQ.isEmpty())
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} EOF", this);
                    _state=EOF;
                }
                else
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} EOF pending", this);
                    _inputQ.add(EOF_CONTENT);
                }
                
                if (_listener == null)
                {
                    _inputQ.notify();
                    return;
                }
            }
        }
        onReadPossible();
    }

    public boolean consumeAll()
    {
        synchronized (_inputQ)
        {
            try
            {
                while (!isFinished())
                {
                    Content item = nextContent();
                    if (item == null)
                        break; // Let's not bother blocking
                    
                    skip(item, remaining(item));
                }
                return isFinished() && !isError();
            }
            catch (IOException e)
            {
                LOG.debug(e);
                return false;
            }
        }
    }

    public boolean isError()
    {
        synchronized (_inputQ)
        {
            return _state instanceof ErrorState;
        }
    }
    
    public boolean isAsync()
    {
        synchronized (_inputQ)
        {
            return _state==ASYNC;
        }
    }

    @Override
    public boolean isFinished()
    {
        synchronized (_inputQ)
        {
            return _state.isEOF();
        }
    }
    

    @Override
    public boolean isReady()
    {
        boolean finished;
        synchronized (_inputQ)
        {
            if (available() > 0)
                return true;
            if (_state.isEOF())
                return true;
            if (_listener == null )
                return true;
            if (_unready)
                return false;
            _unready = true;
            finished = isFinished();
        }
        if (finished)
            onReadPossible();
        else
            unready();
        return false;
    }

    protected void unready()
    {
    }

    @Override
    public void setReadListener(ReadListener readListener)
    {
        readListener = Objects.requireNonNull(readListener);
        synchronized (_inputQ)
        {
            if (_state != STREAM)
                throw new IllegalStateException("state=" + _state);
            _state = ASYNC;
            _listener = readListener;
            _unready = true;
        }
        onReadPossible();
    }

    public void failed(Throwable x)
    {
        synchronized (_inputQ)
        {
            if (_state instanceof ErrorState)
                LOG.warn(x);
            else
                _state = new ErrorState(x);
        }
    }

    @Override
    public void run()
    {
        final Throwable error;
        final ReadListener listener;
        boolean available = false;
        final boolean eof;

        synchronized (_inputQ)
        {
            if (!_unready || _listener == null)
                return;

            error = _state instanceof ErrorState?((ErrorState)_state).getError():null;
            listener = _listener;

            try
            {
                Content item = nextContent();
                available = item != null && remaining(item) > 0;
            }
            catch (Exception e)
            {
                failed(e);
            }

            eof = !available && isFinished();
            _unready = !available && !eof;
        }

        try
        {
            if (error != null)
                listener.onError(error);
            else if (available)
                listener.onDataAvailable();
            else if (eof)
                listener.onAllDataRead();
            else
                unready();
        }
        catch (Throwable e)
        {
            LOG.warn(e.toString());
            LOG.debug(e);
            listener.onError(e);
        }
    }

    public static class PoisonPillContent extends Content
    {
        private final String _name;
        public PoisonPillContent(String name)
        {
            super(BufferUtil.EMPTY_BUFFER);
            _name=name;
        }
        
        @Override
        public String toString()
        {
            return _name;
        }
    }
    
    public static class Content extends Callback.Adapter
    {
        private final ByteBuffer _content;
        
        public Content(ByteBuffer content)
        {
            _content=content;
        }
        
        public ByteBuffer getContent()
        {
            return _content;
        }
        
        public boolean hasContent()
        {
            return _content.hasRemaining();
        }
        
        public int remaining()
        {
            return _content.remaining();
        }
    }
    
    
    protected static abstract class State
    {
        public boolean blockForContent(HttpInput in) throws IOException
        {
            return false;
        }

        public int noContent() throws IOException
        {
            return -1;
        }

        public boolean isEOF()
        {
            return false;
        }
    }
    
    protected static class ErrorState extends State
    {
        final Throwable _error;
        ErrorState(Throwable error)
        {
            _error=error;
        }
        
        public Throwable getError()
        {
            return _error;
        }

        @Override
        public int noContent() throws IOException
        {
            throw new IOException(_error);
        }

        @Override
        public boolean isEOF()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return "ERROR:"+_error;
        }
    }

    protected static final State STREAM = new State()
    {
        @Override
        public boolean blockForContent(HttpInput input) throws IOException
        {
            input.blockForContent();
            return true;
        }

        @Override
        public String toString()
        {
            return "STREAM";
        }
    };

    protected static final State ASYNC = new State()
    {
        @Override
        public int noContent() throws IOException
        {
            return 0;
        }

        @Override
        public String toString()
        {
            return "ASYNC";
        }
    };

    protected static final State EARLY_EOF = new State()
    {
        @Override
        public int noContent() throws IOException
        {
            throw new EofException("Early EOF");
        }

        @Override
        public boolean isEOF()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return "EARLY_EOF";
        }
    };

    protected static final State EOF = new State()
    {
        @Override
        public boolean isEOF()
        {
            return true;
        }

        @Override
        public String toString()
        {
            return "EOF";
        }
    };
}
