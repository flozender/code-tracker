/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec.support;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Encode from an {@code Object} stream to a byte stream of XML elements.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jaxb2Decoder
 */
public class Jaxb2Encoder extends AbstractSingleValueEncoder<Object> {

	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	public Jaxb2Encoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML);
	}

	@Override
	public boolean canEncode(ResolvableType type, MimeType mimeType, Object... hints) {
		if (super.canEncode(type, mimeType, hints)) {
			Class<?> outputClass = type.getRawClass();
			return outputClass.isAnnotationPresent(XmlRootElement.class) ||
					outputClass.isAnnotationPresent(XmlType.class);
		}
		else {
			return false;
		}

	}

	@Override
	protected Flux<DataBuffer> encode(Object value, DataBufferAllocator allocator,
			ResolvableType type, MimeType mimeType, Object... hints) {
		try {
			DataBuffer buffer = allocator.allocateBuffer(1024);
			OutputStream outputStream = buffer.asOutputStream();
			Class<?> clazz = ClassUtils.getUserClass(value);
			Marshaller marshaller = jaxbContexts.createMarshaller(clazz);
			marshaller
					.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
			marshaller.marshal(value, outputStream);
			return Flux.just(buffer);
		}
		catch (JAXBException ex) {
			return Flux.error(ex);
		}
	}



}

