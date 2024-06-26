/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.env;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringUtils;

import static java.lang.String.*;
import static org.springframework.util.StringUtils.*;
import static org.springframework.util.SystemPropertyUtils.*;


/**
 * TODO SPR-7508: document
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profile.active";
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profile.default";

	protected final Log logger = LogFactory.getLog(getClass());

	private Set<String> activeProfiles = new LinkedHashSet<String>();
	private Set<String> defaultProfiles = new LinkedHashSet<String>();

	private LinkedList<PropertySource<?>> propertySources = new LinkedList<PropertySource<?>>();
	private ConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();

	private final PropertyPlaceholderHelper nonStrictHelper =
		new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);

	private final PropertyPlaceholderHelper strictHelper =
		new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);


	public ConversionService getConversionService() {
		return this.conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public void addPropertySource(PropertySource<?> propertySource) {
		propertySources.addFirst(propertySource);
	}

	public void addPropertySource(String name, Properties properties) {
		addPropertySource(new PropertiesPropertySource(name, properties));
	}

	public void addPropertySource(String name, Map<String, String> propertiesMap) {
		addPropertySource(new MapPropertySource(name, propertiesMap));
	}

	public LinkedList<PropertySource<?>> getPropertySources() {
		return propertySources;
	}

	public boolean containsProperty(String key) {
		for (PropertySource<?> propertySource : propertySources) {
			if (propertySource.containsProperty(key)) {
				return true;
			}
		}
		return false;
	}

	public String getProperty(String key) {
		if (logger.isTraceEnabled()) {
			logger.trace(format("getProperty(\"%s\") (implicit targetType [String])", key));
		}
		return getProperty(key, String.class);
	}

	public String getRequiredProperty(String key) {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException(format("required key [%s] not found", key));
		}
		return value;
	}

	public <T> T getProperty(String key, Class<T> targetValueType) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (logger.isTraceEnabled()) {
			logger.trace(format("getProperty(\"%s\", %s)", key, targetValueType.getSimpleName()));
		}

		for (PropertySource<?> propertySource : propertySources) {
			if (debugEnabled) {
				logger.debug(format("Searching for key '%s' in [%s]", key, propertySource.getName()));
			}
			if (propertySource.containsProperty(key)) {
				Object value = propertySource.getProperty(key);
				Class<?> valueType = value == null ? null : value.getClass();
				if (debugEnabled) {
					logger.debug(
							format("Found key '%s' in [%s] with type [%s] and value '%s'",
									key, propertySource.getName(),
									valueType == null ? "" : valueType.getSimpleName(), value));
				}
				if (value == null) {
					return null;
				}
				if (!conversionService.canConvert(valueType, targetValueType)) {
					throw new IllegalArgumentException(
							format("Cannot convert value [%s] from source type [%s] to target type [%s]",
									value, valueType.getSimpleName(), targetValueType.getSimpleName()));
				}
				return conversionService.convert(value, targetValueType);
			}
		}

		if (debugEnabled) {
			logger.debug(format("Could not find key '%s' in any property source. Returning [null]", key));
		}
		return null;
	}

	public <T> T getRequiredProperty(String key, Class<T> valueType) {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalArgumentException(format("required key [%s] not found", key));
		}
		return value;
	}

	public int getPropertyCount() {
		return asProperties().size();
	}

	public Properties asProperties() {
		// TODO SPR-7508: refactor, simplify. only handles map-based propertysources right now.
		Properties mergedProps = new Properties();
		for (int i = propertySources.size() -1; i >= 0; i--) {
			PropertySource<?> propertySource = propertySources.get(i);
			Object object = propertySource.getSource();
			if (object instanceof Map) {
				for (Entry<?, ?> entry : ((Map<?, ?>)object).entrySet()) {
					mergedProps.put(entry.getKey(), entry.getValue());
				}
			} else {
				throw new IllegalArgumentException("unknown PropertySource source type: " + object.getClass().getName());
			}
		}
		return mergedProps;
	}

	public Set<String> getActiveProfiles() {
		if (this.activeProfiles.isEmpty()) {
			String profiles = getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			if (StringUtils.hasText(profiles)) {
				this.activeProfiles = commaDelimitedListToSet(trimAllWhitespace(profiles));
			}
		}
		return Collections.unmodifiableSet(activeProfiles);
	}

	public void setActiveProfiles(String... profiles) {
		this.activeProfiles.clear();
		this.activeProfiles.addAll(Arrays.asList(profiles));
	}

	public Set<String> getDefaultProfiles() {
		if (this.defaultProfiles.isEmpty()) {
			String profiles = getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
			if (StringUtils.hasText(profiles)) {
				this.defaultProfiles = commaDelimitedListToSet(profiles);
			}
		}
		return Collections.unmodifiableSet(this.defaultProfiles);
	}

	public void setDefaultProfiles(String... profiles) {
		this.defaultProfiles.clear();
		this.defaultProfiles.addAll(Arrays.asList(profiles));
	}

	public Map<String, String> getSystemEnvironment() {
		Map<String,String> systemEnvironment;
		try {
			systemEnvironment = System.getenv();
		}
		catch (AccessControlException ex) {
			systemEnvironment = new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String variableName) {
					try {
						return System.getenv(variableName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info(format("Caught AccessControlException when accessing system environment variable " +
									"[%s]; its value will be returned [null]. Reason: %s", variableName, ex.getMessage()));
						}
						return null;
					}
				}
			};
		}
		return systemEnvironment;
	}

	/**
	 * TODO SPR-7508: document
	 *
	 * Returns a string, string map even though the underlying system properties
	 * are a properties object that can technically contain non-string keys and values.
	 * Thus, the unchecked conversions and raw map type being used.  In practice, it will
	 * always be 'safe' to interact with the properties map as if it contains only strings,
	 * because Properties copes with this in its getProperty method.  We never access the
	 * properties object via its Hashtable.get() method, so any non-string keys/values
	 * get effectively ignored.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, String> getSystemProperties() {
		Map systemProperties;
		try {
			systemProperties = System.getProperties();
		}
		catch (AccessControlException ex) {
			systemProperties = new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String propertyName) {
					try {
						return System.getProperty(propertyName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info(format("Caught AccessControlException when accessing system property " +
									"[%s]; its value will be returned [null]. Reason: %s", propertyName, ex.getMessage()));
						}
						return null;
					}
				}
			};
		}
		return systemProperties;
	}

	public String resolvePlaceholders(String text) {
		return doResolvePlaceholders(text, nonStrictHelper);
	}

	public String resolveRequiredPlaceholders(String text) {
		return doResolvePlaceholders(text, strictHelper);
	}

	public boolean acceptsProfiles(String[] specifiedProfiles) {
		boolean activeProfileFound = false;
		Set<String> activeProfiles = this.getActiveProfiles();
		Set<String> defaultProfiles = this.getDefaultProfiles();
		for (String profile : specifiedProfiles) {
			if (activeProfiles.contains(profile)
					|| (activeProfiles.isEmpty() && defaultProfiles.contains(profile))) {
				activeProfileFound = true;
				break;
			}
		}
		return activeProfileFound;
	}

	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		return helper.replacePlaceholders(text, new PlaceholderResolver() {
			public String resolvePlaceholder(String placeholderName) {
				return AbstractEnvironment.this.getProperty(placeholderName);
			}
		});
	}

	@Override
	public String toString() {
		return String.format("%s [activeProfiles=%s, defaultProfiles=%s, propertySources=%s]",
				getClass().getSimpleName(), activeProfiles, defaultProfiles, propertySources);
	}

}
