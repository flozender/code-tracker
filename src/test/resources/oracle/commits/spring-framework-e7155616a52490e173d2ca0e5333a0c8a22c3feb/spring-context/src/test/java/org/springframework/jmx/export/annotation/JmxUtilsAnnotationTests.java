/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export.annotation;

import javax.management.MXBean;

import junit.framework.TestCase;

import org.springframework.core.JdkVersion;
import org.springframework.jmx.support.JmxUtils;

/**
 * @author Juergen Hoeller
 */
public class JmxUtilsAnnotationTests extends TestCase {

	public void testNotMXBean() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_16) {
			return;
		}
		FooNotX foo = new FooNotX();
		assertFalse("MXBean annotation not detected correctly", JmxUtils.isMBean(foo.getClass()));
	}

	public void testAnnotatedMXBean() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_16) {
			return;
		}
		FooX foo = new FooX();
		assertTrue("MXBean annotation not detected correctly", JmxUtils.isMBean(foo.getClass()));
	}


	@MXBean(false)
	public static interface FooNotMXBean {

		String getName();
	}


	public static class FooNotX implements FooNotMXBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}


	@MXBean(true)
	public static interface FooIfc {

		String getName();
	}


	public static class FooX implements FooIfc {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}

}
