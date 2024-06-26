/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * @author Sebastien Deleuze
 */
public class ReactiveStreamsToCompletableFutureConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<GenericConverter.ConvertiblePair> convertibleTypes = new LinkedHashSet<>();
		convertibleTypes.add(new GenericConverter.ConvertiblePair(Publisher.class, CompletableFuture.class));
		convertibleTypes.add(new GenericConverter.ConvertiblePair(CompletableFuture.class, Publisher.class));
		return convertibleTypes;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source != null) {
			if (CompletableFuture.class.isAssignableFrom(source.getClass())) {
				return reactor.core.publisher.convert.CompletableFutureConverter.from((CompletableFuture)source);
			} else if (CompletableFuture.class.isAssignableFrom(targetType.getResolvableType().getRawClass())) {
				return reactor.core.publisher.convert.CompletableFutureConverter.fromSingle((Publisher)source);
			}
		}
		return null;
	}

}
