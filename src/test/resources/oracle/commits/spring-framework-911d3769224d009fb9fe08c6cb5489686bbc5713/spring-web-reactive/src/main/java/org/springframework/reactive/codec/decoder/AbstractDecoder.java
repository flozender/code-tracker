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

package org.springframework.reactive.codec.decoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;

/**
 * @author Sebastien Deleuze
 */
public abstract class AbstractDecoder<T> implements Decoder<T> {

	private List<MimeType> supportedMimeTypes = Collections.emptyList();


	public AbstractDecoder(MimeType... supportedMimeTypes) {
		this.supportedMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	@Override
	public List<MimeType> getSupportedMimeTypes() {
		return this.supportedMimeTypes;
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		for (MimeType supportedMimeType : this.supportedMimeTypes) {
			if (supportedMimeType.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

}
