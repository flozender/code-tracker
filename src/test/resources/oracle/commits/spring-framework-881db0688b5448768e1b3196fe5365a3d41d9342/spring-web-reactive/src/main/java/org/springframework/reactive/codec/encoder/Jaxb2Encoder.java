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

package org.springframework.reactive.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;

import org.reactivestreams.Publisher;
import reactor.io.buffer.Buffer;
import reactor.rx.Streams;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.CodecException;
import org.springframework.reactive.codec.decoder.Jaxb2Decoder;
import org.springframework.reactive.io.BufferOutputStream;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Encode from an {@code Object} stream to a byte stream of XML elements.
 *
 * @author Sebastien Deleuze
 * @see Jaxb2Decoder
 */
public class Jaxb2Encoder implements MessageToByteEncoder<Object> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<Class<?>, JAXBContext>(64);


	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_XML) || mediaType.isCompatibleWith(MediaType.TEXT_XML);
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends Object> messageStream, ResolvableType type, MediaType mediaType, Object... hints) {
		return Streams.wrap(messageStream).map(value -> {
			try {
				Buffer buffer = new Buffer();
				BufferOutputStream outputStream = new BufferOutputStream(buffer);
				Class<?> clazz = ClassUtils.getUserClass(value);
				Marshaller marshaller = createMarshaller(clazz);
				marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
				marshaller.marshal(value, outputStream);
				buffer.flip();
				return buffer.byteBuffer();
			}
			catch (MarshalException ex) {
				throw new CodecException(
						"Could not marshal [" + value + "]: " + ex.getMessage(), ex);
			}
			catch (JAXBException ex) {
				throw new CodecException(
						"Could not instantiate JAXBContext: " + ex.getMessage(), ex);
			}
		});
	}

	protected final Marshaller createMarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Marshaller marshaller = jaxbContext.createMarshaller();
			return marshaller;
		}
		catch (JAXBException ex) {
			throw new CodecException(
					"Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		Assert.notNull(clazz, "'clazz' must not be null");
		JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(clazz);
				this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
			}
			catch (JAXBException ex) {
				throw new CodecException(
						"Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		}
		return jaxbContext;
	}

}
