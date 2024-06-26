////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.security.MessageDigest;

/**
 * This class maintains a persistent store of the files that have
 * checked ok and their associated timestamp. It uses a property file
 * for storage.  A hashcode of the Configuration is stored in the
 * cache file to ensure the cache is invalidated when the
 * configuration has changed.
 *
 * @author <a href="mailto:oliver-work@puppycrawl.com">Oliver Burn</a>
 */
class PropertyCacheFile
{
    /**
     * The property key to use for storing the hashcode of the
     * configuration. To avoid nameclashes with the files that are
     * checked the key is chosen in such a way that it cannot be a
     * valid file name.
     */
    private static final String CONFIG_HASH_KEY = "configuration*?";

    /** name of file to store details **/
    private final String mDetailsFile;
    /** the details on files **/
    private final Properties mDetails = new Properties();

    /**
     * Creates a new <code>PropertyCacheFile</code> instance.
     *
     * @param aCurrentConfig the current configuration, not null
     */
    PropertyCacheFile(Configuration aCurrentConfig)
    {
        boolean setInActive = true;
        final String fileName = aCurrentConfig.getCacheFile();
        if (fileName != null) {
            try {
                mDetails.load(new FileInputStream(fileName));
                String cachedConfigHash = mDetails.getProperty(CONFIG_HASH_KEY);
                String currentConfigHash = getConfigHashCode(aCurrentConfig);
                setInActive = false;
                if (cachedConfigHash == null ||
                    !cachedConfigHash.equals(currentConfigHash))
                {
                    // Detected configuration change - clear cache
                    mDetails.clear();
                    mDetails.put(CONFIG_HASH_KEY, currentConfigHash);
                }
            }
            catch (FileNotFoundException e) {
                // Ignore, the cache does not exist
                setInActive = false;
            }
            catch (IOException e) {
                System.out.println("Unable to open cache file, ignoring.");
                e.printStackTrace(System.out);
            }
        }
        mDetailsFile = (setInActive) ? null : fileName;
    }

    /** Cleans up the object and updates the cache file. **/
    void destroy()
    {
        if (mDetailsFile != null) {
            try {
                mDetails.store(new FileOutputStream(mDetailsFile), null);
            }
            catch (IOException e) {
                System.out.println("Unable to save cache file");
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * @return whether the specified file has already been checked ok
     * @param aFileName the file to check
     * @param aTimestamp the timestamp of the file to check
     */
    boolean alreadyChecked(String aFileName, long aTimestamp)
    {
        final String lastChecked = mDetails.getProperty(aFileName);
        return (lastChecked != null) &&
            (lastChecked.equals(Long.toString(aTimestamp)));
    }

    /**
     * Records that a file checked ok.
     * @param aFileName name of the file that checked ok
     * @param aTimestamp the timestamp of the file
     */
    void checkedOk(String aFileName, long aTimestamp)
    {
        mDetails.put(aFileName, Long.toString(aTimestamp));
    }

    /**
     * Calculates the hashcode for a Configuration.
     *
     * @param aConfiguration the Configuration
     * @return the hashcode for <code>aConfiguration</code>
     */
    private String getConfigHashCode(Configuration aConfiguration)
    {
        try {
            // im-memory serialization of Configuration

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(aConfiguration);
            oos.flush();
            oos.close();

            // Instead of hexEncoding baos.toByteArray() directly we
            // use a message digest here to keep the length of the
            // hashcode reasonable

            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(baos.toByteArray());

            return hexEncode(md.digest());
        }
        catch (Exception ex) { // IO, NoSuchAlgorithm
            ex.printStackTrace();
            return "ALWAYS FRESH: " + System.currentTimeMillis();
        }
    }

    /** hex digits */
    private static char[] sHexChars = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Hex-encodes a byte array.
     * @param aByteArray the byte array
     * @return hex encoding of <code>aByteArray</code>
     */
    private static String hexEncode(byte[] aByteArray)
    {
        final StringBuffer buf = new StringBuffer(2 * aByteArray.length);
        for (int i = 0; i < aByteArray.length; i++) {
            final int b = aByteArray[i];
            final int low = b & 0x0F;
            final int high = (b >> 4) & 0x0F;
            buf.append(sHexChars[high]);
            buf.append(sHexChars[low]);
        }
        return buf.toString();
    }
}
