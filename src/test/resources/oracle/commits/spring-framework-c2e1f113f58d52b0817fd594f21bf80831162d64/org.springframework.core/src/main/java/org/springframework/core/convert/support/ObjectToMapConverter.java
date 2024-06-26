/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.convert.support;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts an Object to a single-entry Map containing the Object.
 * The Object is put as both the entry key and value.
 * Will convert the Object to the target Map's parameterized types K,V if necessary.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ObjectToMapConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public ObjectToMapConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Map.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType, targetType.getMapKeyTypeDescriptor())
				&& this.conversionService.canConvert(sourceType, targetType.getMapValueTypeDescriptor());
	}

	@SuppressWarnings("unchecked")
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map target = CollectionFactory.createMap(targetType.getType(), 1);
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		boolean keysCompatible = false;
		if (sourceType != TypeDescriptor.NULL && sourceType.isAssignableTo(targetKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceType != TypeDescriptor.NULL && sourceType.isAssignableTo(targetValueType)) {
			valuesCompatible = true;
		}
		MapEntryConverter converter = new MapEntryConverter(sourceType, sourceType, targetKeyType, targetValueType,
				keysCompatible, valuesCompatible, this.conversionService);
		Object key = converter.convertKey(source);
		Object value = converter.convertValue(source);
		target.put(key, value);
		return target;
	}

}
