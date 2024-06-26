//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.plugins.util;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RepositoryParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testParseLinksInDirectoryListing() throws IOException {
		String listing = StreamUtils.inputStreamToString(this.getClass().getClassLoader().getResourceAsStream("mavenRepoJettyDirectoryListing.html"));
		List<String> modules = RepositoryParser.parseLinksInDirectoryListing(listing);
		assertThat("At least ten jetty modules expected",modules.size(), greaterThan(10));
		assertThat("jetty-jmx module expected", modules.contains("jetty-jmx"), is(true));
	}

	@Test
	public void testIsPlugin() throws IOException{
		String listing = StreamUtils.inputStreamToString(this.getClass().getClassLoader().getResourceAsStream("mavenRepoJettyJMXDirectoryListing.html"));
		assertThat("listing describes a plugin", RepositoryParser.isModuleAPlugin(listing), is(true));
		String nonPluginListing = StreamUtils.inputStreamToString(this.getClass().getClassLoader().getResourceAsStream("mavenRepoJettyJNDIDirectoryListing.html"));
		assertThat("listing doesn't describe a plugin", RepositoryParser.isModuleAPlugin(nonPluginListing), is(false));
	}

}
