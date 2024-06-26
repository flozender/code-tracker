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

package org.springframework.boot.strap.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Strategy to load '.properties' files into a {@link PropertySource}.
 * 
 * @author Dave Syer
 */
public class PropertiesPropertySourceLoader implements PropertySourceLoader {

	@Override
	public boolean supports(Resource resource) {
		return resource.getFilename().endsWith(".properties");
	}

	@Override
	public PropertySource<?> load(Resource resource) {
		try {
			Properties properties = loadProperties(resource);
			return new PropertiesPropertySource(resource.getDescription(), properties);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load properties from " + resource,
					ex);
		}
	}

	protected Properties loadProperties(Resource resource) throws IOException {
		return PropertiesLoaderUtils.loadProperties(resource);
	}
}