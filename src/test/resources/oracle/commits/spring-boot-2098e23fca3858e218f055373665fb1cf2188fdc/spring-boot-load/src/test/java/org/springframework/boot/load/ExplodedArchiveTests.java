/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.load;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.load.Archive;
import org.springframework.boot.load.ExplodedArchive;
import org.springframework.boot.load.Archive.Entry;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ExplodedArchive}.
 * 
 * @author Phillip Webb
 */
public class ExplodedArchiveTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File rootFolder;

	private ExplodedArchive archive;

	@Before
	public void setup() throws Exception {
		File file = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(file);

		this.rootFolder = this.temporaryFolder.newFolder();
		JarFile jarFile = new JarFile(file);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File destination = new File(this.rootFolder.getAbsolutePath()
					+ File.separator + entry.getName());
			destination.getParentFile().mkdirs();
			if (entry.isDirectory()) {
				destination.mkdir();
			}
			else {
				copy(jarFile.getInputStream(entry), new FileOutputStream(destination));
			}
		}
		this.archive = new ExplodedArchive(this.rootFolder);
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len = in.read(buffer);
		while (len != -1) {
			out.write(buffer, 0, len);
			len = in.read(buffer);
		}
	}

	@Test
	public void getManifest() throws Exception {
		assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By"),
				equalTo("j1"));
	}

	@Test
	public void getEntries() throws Exception {
		Map<String, Archive.Entry> entries = getEntriesMap(this.archive);
		assertThat(entries.size(), equalTo(7));
	}

	@Test
	public void getUrl() throws Exception {
		URL url = this.archive.getUrl();
		assertThat(url, equalTo(this.rootFolder.toURI().toURL()));
	}

	@Test
	public void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl().toString(),
				equalTo("jar:file:" + this.rootFolder.getPath() + "/nested.jar!/"));
	}

	@Test
	public void nestedDirArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("d/");
		Archive nested = this.archive.getNestedArchive(entry);
		Map<String, Entry> nestedEntries = getEntriesMap(nested);
		assertThat(nestedEntries.size(), equalTo(1));
		assertThat(nested.getUrl().toString(),
				equalTo("file:" + this.rootFolder.getPath() + "/d/"));
	}

	@Test
	public void getFilteredArchive() throws Exception {
		Archive filteredArchive = this.archive
				.getFilteredArchive(new Archive.EntryFilter() {
					@Override
					public String apply(String entryName, Entry entry) {
						if (entryName.equals("1.dat")) {
							return entryName;
						}
						return null;
					}
				});
		Map<String, Entry> entries = getEntriesMap(filteredArchive);
		assertThat(entries.size(), equalTo(1));
		URLClassLoader classLoader = new URLClassLoader(
				new URL[] { filteredArchive.getUrl() });
		assertThat(classLoader.getResourceAsStream("1.dat").read(), equalTo(1));
		assertThat(classLoader.getResourceAsStream("2.dat"), nullValue());
	}

	private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
		Map<String, Archive.Entry> entries = new HashMap<String, Archive.Entry>();
		for (Archive.Entry entry : archive.getEntries()) {
			entries.put(entry.getName(), entry);
		}
		return entries;
	}
}
