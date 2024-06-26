/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jmx.export.naming;

import java.io.IOException;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.CollectionUtils;

/**
 * <code>ObjectNamingStrategy</code> implementation that builds
 * <code>ObjectName</code> instances from the key used in the
 * "beans" map passed to <code>MBeanExporter</code>.
 *
 * <p>Can also check object name mappings, given as <code>Properties</code>
 * or as <code>mappingLocations</code> of properties files. The key used
 * to look up is the key used in <code>MBeanExporter</code>'s "beans" map.
 * If no mapping is found for a given key, the key itself is used to
 * build an <code>ObjectName</code>.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setMappings
 * @see #setMappingLocation
 * @see #setMappingLocations
 * @see org.springframework.jmx.export.MBeanExporter#setBeans
 */
public class KeyNamingStrategy implements ObjectNamingStrategy, InitializingBean {

	/**
	 * <code>Log</code> instance for this class.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Stores the mappings of bean key to <code>ObjectName</code>.
	 */
	private Properties mappings;

	/**
	 * Stores the <code>Resource</code>s containing properties that should be loaded
	 * into the final merged set of <code>Properties</code> used for <code>ObjectName</code>
	 * resolution.
	 */
	private Resource[] mappingLocations;

	/**
	 * Stores the result of merging the <code>mappings</code> <code>Properties</code>
	 * with the the properties stored in the resources defined by <code>mappingLocations</code>.
	 */
	private Properties mergedMappings;


	/**
	 * Set local properties, containing object name mappings, e.g. via
	 * the "props" tag in XML bean definitions. These can be considered
	 * defaults, to be overridden by properties loaded from files.
	 */
	public void setMappings(Properties mappings) {
		this.mappings = mappings;
	}

	/**
	 * Set a location of a properties file to be loaded,
	 * containing object name mappings.
	 */
	public void setMappingLocation(Resource location) {
		this.mappingLocations = new Resource[]{location};
	}

	/**
	 * Set location of properties files to be loaded,
	 * containing object name mappings.
	 */
	public void setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
	}


	/**
	 * Merges the <code>Properties</code> configured in the <code>mappings</code> and
	 * <code>mappingLocations</code> into the final <code>Properties</code> instance
	 * used for <code>ObjectName</code> resolution.
	 * @throws IOException
	 */
	public void afterPropertiesSet() throws IOException {
		this.mergedMappings = new Properties();

		CollectionUtils.mergePropertiesIntoMap(this.mappings, this.mergedMappings);

		if (this.mappingLocations != null) {
			for (int i = 0; i < this.mappingLocations.length; i++) {
				Resource location = this.mappingLocations[i];
				if (logger.isInfoEnabled()) {
					logger.info("Loading JMX object name mappings file from " + location);
				}
				PropertiesLoaderUtils.fillProperties(this.mergedMappings, location);
			}
		}
	}


	/**
	 * Attempts to retrieve the <code>ObjectName</code> via the given key, trying to
	 * find a mapped value in the mappings first.
	 */
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		String objectName = null;
		if (this.mergedMappings != null) {
			objectName = this.mergedMappings.getProperty(beanKey);
		}
		if (objectName == null) {
			objectName = beanKey;
		}
		return ObjectNameManager.getInstance(objectName);
	}

}
