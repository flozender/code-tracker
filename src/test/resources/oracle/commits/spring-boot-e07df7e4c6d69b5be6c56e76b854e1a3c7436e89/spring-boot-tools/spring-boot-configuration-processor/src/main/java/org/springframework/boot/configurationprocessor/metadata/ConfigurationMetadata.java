/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ItemMetadata
 */
public class ConfigurationMetadata {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final MultiValueMap<String, ItemMetadata> items;

	private final MultiValueMap<String, ItemHint> hints;

	public ConfigurationMetadata() {
		this.items = new LinkedMultiValueMap<String, ItemMetadata>();
		this.hints = new LinkedMultiValueMap<String, ItemHint>();
	}

	public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.items = new LinkedMultiValueMap<String, ItemMetadata>(metadata.items);
		this.hints = new LinkedMultiValueMap<String, ItemHint>(metadata.hints);
	}

	/**
	 * Add item meta-data.
	 * @param itemMetadata the meta-data to add
	 */
	public void add(ItemMetadata itemMetadata) {
		this.items.add(itemMetadata.getName(), itemMetadata);
	}

	/**
	 * Add item hint.
	 * @param itemHint the item hint to add
	 */
	public void add(ItemHint itemHint) {
		this.hints.add(itemHint.getName(), itemHint);
	}

	/**
	 * Merge the content from another {@link ConfigurationMetadata}.
	 * @param metadata the {@link ConfigurationMetadata} instance to merge
	 */
	public void merge(ConfigurationMetadata metadata) {
		for (ItemMetadata additionalItem : metadata.getItems()) {
			mergeItemMetadata(additionalItem);
		}
		for (ItemHint itemHint : metadata.getHints()) {
			add(itemHint);
		}
	}

	/**
	 * @return the meta-data properties.
	 */
	public List<ItemMetadata> getItems() {
		return flattenValues(this.items);
	}

	/**
	 * @return the meta-data hints.
	 */
	public List<ItemHint> getHints() {
		return flattenValues(this.hints);
	}

	protected void mergeItemMetadata(ItemMetadata metadata) {
		ItemMetadata matching = findMatchingItemMetadata(metadata);
		if (matching != null) {
			if (metadata.getDescription() != null) {
				matching.setDescription(metadata.getDescription());
			}
			if (metadata.getDefaultValue() != null) {
				matching.setDefaultValue(metadata.getDefaultValue());
			}
			ItemDeprecation deprecation = metadata.getDeprecation();
			ItemDeprecation matchingDeprecation = matching.getDeprecation();
			if (deprecation != null) {
				if (matchingDeprecation == null) {
					matching.setDeprecation(deprecation);
				}
				else {
					if (deprecation.getReason() != null) {
						matchingDeprecation.setReason(deprecation.getReason());
					}
					if (deprecation.getReplacement() != null) {
						matchingDeprecation.setReplacement(deprecation.getReplacement());
					}
				}
			}
		}
		else {
			this.items.add(metadata.getName(), metadata);
		}
	}

	private ItemMetadata findMatchingItemMetadata(ItemMetadata metadata) {
		List<ItemMetadata> candidates = this.items.get(metadata.getName());
		if (CollectionUtils.isEmpty(candidates)) {
			return null;
		}
		ListIterator<ItemMetadata> it = candidates.listIterator();
		while (it.hasNext()) {
			if (!it.next().hasSameType(metadata)) {
				it.remove();
			}
		}
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		for (ItemMetadata candidate : candidates) {
			if (ObjectUtils.nullSafeEquals(candidate.getSourceType(),
					metadata.getSourceType())) {
				return candidate;
			}
		}
		return null;
	}

	public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix == null ? "" : prefix);
		String dashedName = toDashedCase(name);
		nestedPrefix += ("".equals(nestedPrefix) ? dashedName : "." + dashedName);
		return nestedPrefix;
	}

	static String toDashedCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result, getDashed(matcher));
		}
		matcher.appendTail(result);
		return result.toString().toLowerCase();
	}

	private static String getDashed(Matcher matcher) {
		String first = matcher.group(1);
		String second = matcher.group(2);
		if (first.equals("_")) {
			// not a word for the binder
			return first + second;
		}
		return first + "-" + second;
	}

	private static <T extends Comparable> List<T> flattenValues(MultiValueMap<?, T> map) {
		List<T> content = new ArrayList<T>();
		for (List<T> values : map.values()) {
			content.addAll(values);
		}
		Collections.sort(content);
		return content;
	}

}
