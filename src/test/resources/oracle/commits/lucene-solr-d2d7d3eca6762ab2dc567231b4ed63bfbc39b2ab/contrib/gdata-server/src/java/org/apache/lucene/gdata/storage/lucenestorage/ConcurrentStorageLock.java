/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.gdata.storage.lucenestorage;

/**
 * 
 * @author Simon Willnauer
 *
 */
public interface ConcurrentStorageLock {
    
    /**
     * @param key
     * @return
     */
    public abstract boolean setLock(final String key);
    /**
     * @param key
     * @return
     */
    public abstract boolean releaseLock(final String key);
    /**
     * @return
     */
    public abstract boolean releaseThreadLocks();
    /**
     * @param key
     * @return
     */
    public abstract boolean isKeyLocked(final String key);
    /**
     * 
     */
    public abstract void close();

}
