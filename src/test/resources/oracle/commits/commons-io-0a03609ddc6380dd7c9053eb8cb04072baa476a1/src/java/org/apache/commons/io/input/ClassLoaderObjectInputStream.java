/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
package org.apache.commons.io.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

/**
 * A special ObjectInputStream that loads a class based on a specified
 * <code>ClassLoader</code> rather than the system default.
 * <p>
 * This is useful in dynamic container environments.
 *
 * @author Paul Hammant
 * @version $Id$
 * @since Commons IO 1.1
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

    /** The class loader to use. */
    private ClassLoader classLoader;

    /**
     * Constructs a new ClassLoaderObjectInputStream.
     *
     * @param classLoader  the ClassLoader from which classes should be loaded
     * @param inputStream  the InputStream to work on
     * @throws IOException in case of an I/O error
     * @throws StreamCorruptedException if the stream is corrupted
     */
    public ClassLoaderObjectInputStream(
            ClassLoader classLoader, InputStream inputStream)
            throws IOException, StreamCorruptedException {
        super(inputStream);
        this.classLoader = classLoader;
    }

    /**
     * Resolve a class specified by the descriptor using the
     * specified ClassLoader or the super ClassLoader.
     *
     * @param objectStreamClass  descriptor of the class
     * @return the Class object described by the ObjectStreamClass
     * @throws IOException in case of an I/O error
     * @throws ClassNotFoundException if the Class cannot be found
     */
    protected Class resolveClass(ObjectStreamClass objectStreamClass)
            throws IOException, ClassNotFoundException {
        
        Class clazz = Class.forName(objectStreamClass.getName(), false, classLoader);

        if (clazz != null) {
            // the classloader knows of the class
            return clazz;
        } else {
            // classloader knows not of class, let the super classloader do it
            return super.resolveClass(objectStreamClass);
        }
    }
}
