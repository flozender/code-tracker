/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.io.PathUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Simple check for duplicate class files across the classpath */
class JarHell {

    /**
     * Checks the current classloader for duplicate classes
     * @throws IllegalStateException if jar hell was found
     */
    @SuppressForbidden(reason = "needs JarFile for speed, just reading entries")
    static void checkJarHell() throws Exception {
        ClassLoader loader = JarHell.class.getClassLoader();
        if (loader instanceof URLClassLoader == false) {
           return;
        }
        final Map<String,URL> clazzes = new HashMap<>(32768);
        Set<String> seenJars = new HashSet<>();
        for (final URL url : ((URLClassLoader)loader).getURLs()) {
            String path = url.getPath();
            if (path.endsWith(".jar")) {
                if (!seenJars.add(path)) {
                    continue; // we can't fail because of sheistiness with joda-time
                }
                try (JarFile file = new JarFile(url.getPath())) {
                    Enumeration<JarEntry> elements = file.entries();
                    while (elements.hasMoreElements()) {
                        String entry = elements.nextElement().getName();
                        if (entry.endsWith(".class")) {
                            checkClass(clazzes, entry, url);
                        }
                    }
                }
            } else {
                Files.walkFileTree(PathUtils.get(url.toURI()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String entry = file.toString();
                        if (entry.endsWith(".class")) {
                            checkClass(clazzes, entry, url);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            }
        }
    }
    
    @SuppressForbidden(reason = "proper use of URL to reduce noise")
    static void checkClass(Map<String,URL> clazzes, String clazz, URL url) {
        if (clazz.startsWith("org/apache/log4j")) {
            return; // go figure, jar hell for what should be System.out.println...
        }
        if (clazz.equals("org/joda/time/base/BaseDateTime.class")) {
            return; // apparently this is intentional... clean this up
        }
        URL previous = clazzes.put(clazz, url);
        if (previous != null) {
            throw new IllegalStateException("jar hell!" + System.lineSeparator() +
                    "class: " + clazz + System.lineSeparator() +
                    "jar1: " + previous.getPath() + System.lineSeparator() +
                    "jar2: " + url.getPath());
        }
    }
}
