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

package org.springframework.boot.strap;

import org.junit.Test;
import org.springframework.boot.strap.Banner;

/**
 * Tests for {@link Banner}.
 * 
 * @author Phillip Webb
 */
public class BannerTests {

	@Test
	public void visualBannder() throws Exception {
		Banner.write(System.out);
	}

}
