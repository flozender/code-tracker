/*
 * Copyright 2002-2004,2006 The Apache Software Foundation.
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
package org.apache.commons.io.filefilter;

import java.io.File;

/**
 * A file filter that always returns false.
 *
 * @since Commons IO 1.0
 * @version $Revision$ $Date$
 *
 * @author Henri Yandell
 * @author Stephen Colebourne
 */
public class FalseFileFilter implements IOFileFilter {

    /**
     * Singleton instance of false filter.
     * @since Commons IO 1.3
     */
    public static final IOFileFilter FALSE = new FalseFileFilter();
    /**
     * Singleton instance of false filter.
     * Please use the identical FalseFileFilter.FALSE constant.
     * The new name is more JDK 1.5 friendly as it doesn't clash with other
     * values when using static imports.
     */
    public static final IOFileFilter INSTANCE = FALSE;

    /**
     * Restrictive consructor.
     */
    protected FalseFileFilter() {
    }

    /**
     * Returns false.
     *
     * @param file  the file to check
     * @return false
     */
    public boolean accept(File file) {
        return false;
    }

    /**
     * Returns false.
     *
     * @param dir  the directory to check
     * @param name  the filename
     * @return false
     */
    public boolean accept(File dir, String name) {
        return false;
    }

}
