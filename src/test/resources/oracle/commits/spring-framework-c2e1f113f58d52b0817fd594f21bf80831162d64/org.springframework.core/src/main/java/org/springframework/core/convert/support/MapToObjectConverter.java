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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Map to an Object by returning the first Map entry value after converting it to the desired targetType.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToObjectConverter implements ConditionalGenericConverter {

	private final GenericConversionService conversionService;

	public MapToObjectConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Object.class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType.getMapValueTypeDescriptor(), targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return this.conversionService.convertNullSource(sourceType, targetType);
		}
		Map<?, ?> sourceMap = (Map<?, ?>) source;
		if (sourceMap.size() == 0) {
			return null;
		} else {
			Object firstValue = sourceMap.values().iterator().next();
			TypeDescriptor sourceValueType = sourceType.getMapValueTypeDescriptor();
			if (sourceValueType == TypeDescriptor.NULL) {
				sourceValueType = TypeDescriptor.forObject(firstValue);
			}
			boolean valuesCompatible = false;
			if (sourceValueType != TypeDescriptor.NULL && sourceValueType.isAssignableTo(targetType)) {
				valuesCompatible = true;
			}
			MapEntryConverter converter = new MapEntryConverter(sourceValueType, sourceValueType, targetType,
					targetType, true, valuesCompatible, this.conversionService);
			return converter.convertValue(firstValue);
		}
	}

}
