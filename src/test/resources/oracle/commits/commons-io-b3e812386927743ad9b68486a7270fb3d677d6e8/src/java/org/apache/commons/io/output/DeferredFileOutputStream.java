/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.apache.commons.io.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;


/**
 * <p>An output stream which will retain data in memory until a specified
 * threshold is reached, and only then commit it to disk. If the stream is
 * closed before the threshold is reached, the data will not be written to
 * disk at all.</p>
 * 
 * <p>This class originated in FileUpload processing. In this use case, you do
 * not know in advance the size of the file being uploaded. If the file is small
 * you want to store it in memory (for speed), but if the file is large you want
 * to store it to file (to avoid memory issues).</p>
 *
 * @author <a href="mailto:martinc@apache.org">Martin Cooper</a>
 * @author gaxzerow
 *
 * @version $Id$
 */
public class DeferredFileOutputStream
    extends ThresholdingOutputStream
{

    // ----------------------------------------------------------- Data members


    /**
     * The output stream to which data will be written prior to the theshold
     * being reached.
     */
    private ByteArrayOutputStream memoryOutputStream;


    /**
     * The output stream to which data will be written at any given time. This
     * will always be one of <code>memoryOutputStream</code> or
     * <code>diskOutputStream</code>.
     */
    private OutputStream currentOutputStream;


    /**
     * The file to which output will be directed if the threshold is exceeded.
     */
    private File outputFile;

    
    /**
     * True when close() has been called successfully.
     */
    private boolean closed = false;

    // ----------------------------------------------------------- Constructors


    /**
     * Constructs an instance of this class which will trigger an event at the
     * specified threshold, and save data to a file beyond that point.
     *
     * @param threshold  The number of bytes at which to trigger an event.
     * @param outputFile The file to which data is saved beyond the threshold.
     */
    public DeferredFileOutputStream(int threshold, File outputFile)
    {
        super(threshold);
        this.outputFile = outputFile;

        memoryOutputStream = new ByteArrayOutputStream();
        currentOutputStream = memoryOutputStream;
    }


    // --------------------------------------- ThresholdingOutputStream methods


    /**
     * Returns the current output stream. This may be memory based or disk
     * based, depending on the current state with respect to the threshold.
     *
     * @return The underlying output stream.
     *
     * @exception IOException if an error occurs.
     */
    protected OutputStream getStream() throws IOException
    {
        return currentOutputStream;
    }


    /**
     * Switches the underlying output stream from a memory based stream to one
     * that is backed by disk. This is the point at which we realise that too
     * much data is being written to keep in memory, so we elect to switch to
     * disk-based storage.
     *
     * @exception IOException if an error occurs.
     */
    protected void thresholdReached() throws IOException
    {
        FileOutputStream fos = new FileOutputStream(outputFile);
        memoryOutputStream.writeTo(fos);
        currentOutputStream = fos;
        memoryOutputStream = null;
    }


    // --------------------------------------------------------- Public methods


    /**
     * Determines whether or not the data for this output stream has been
     * retained in memory.
     *
     * @return <code>true</code> if the data is available in memory;
     *         <code>false</code> otherwise.
     */
    public boolean isInMemory()
    {
        return (!isThresholdExceeded());
    }


    /**
     * Returns the data for this output stream as an array of bytes, assuming
     * that the data has been retained in memory. If the data was written to
     * disk, this method returns <code>null</code>.
     *
     * @return The data for this output stream, or <code>null</code> if no such
     *         data is available.
     */
    public byte[] getData()
    {
        if (memoryOutputStream != null)
        {
            return memoryOutputStream.toByteArray();
        }
        return null;
    }


    /**
     * Returns the same output file specified in the constructor, even when
     * threashold has not been reached.
     *
     * @return The file for this output stream, or <code>null</code> if no such
     *         file exists.
     */
    public File getFile()
    {
        return outputFile;
    }
    
        
    /**
     * Closes underlying output stream, and mark this as closed
     *
     * @exception IOException if an error occurs.
     */
    public void close() throws IOException
    {
        super.close();
        closed = true;
    }
    
    
    /**
     * Writes the data from this output stream to the specified output stream,
     * after it has been closed.
     *
     * @param out output stream to write to.
     * @exception IOException if this stream is not yet closed or an error occurs.
     */
    public void writeTo(OutputStream out) throws IOException 
    {
        // we may only need to check if this is closed if we are working with a file
        // but we should force the habit of closing wether we are working with
        // a file or memory.
        if (!closed)
        {
            throw new IOException("Stream not closed");
        }
        
        if(isInMemory())
        {
            memoryOutputStream.writeTo(out);
        }
        else
        {
            FileInputStream fis = new FileInputStream(outputFile);
            try {
                IOUtils.copy(fis, out);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
    }
}
