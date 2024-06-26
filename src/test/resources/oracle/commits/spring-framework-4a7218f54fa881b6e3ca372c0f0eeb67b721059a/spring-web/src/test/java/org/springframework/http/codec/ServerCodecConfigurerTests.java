/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.http.codec;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ServerCodecConfigurer}.
 * @author Rossen Stoyanchev
 */
public class ServerCodecConfigurerTests {

	private final ServerCodecConfigurer configurer = new ServerCodecConfigurer();

	private final AtomicInteger index = new AtomicInteger(0);


	@Test
	public void defaultReaders() throws Exception {
		List<ServerHttpMessageReader<?>> readers = this.configurer.getReaders();
		assertEquals(8, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), true);
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertStringDecoder(getNextDecoder(readers), false);
	}

	@Test
	public void defaultWriters() throws Exception {
		List<ServerHttpMessageWriter<?>> writers = this.configurer.getWriters();
		assertEquals(9, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertStringEncoder(getNextEncoder(writers), true);
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertSseWriter(writers);
		assertStringEncoder(getNextEncoder(writers), false);
	}

	@Test
	public void defaultAndCustomReaders() throws Exception {

		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		ServerHttpMessageReader<?> customReader1 = mock(ServerHttpMessageReader.class);
		ServerHttpMessageReader<?> customReader2 = mock(ServerHttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodec().decoder(customDecoder1);
		this.configurer.customCodec().decoder(customDecoder2);

		this.configurer.customCodec().reader(customReader1);
		this.configurer.customCodec().reader(customReader2);

		List<ServerHttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(12, readers.size());
		assertEquals(ByteArrayDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ByteBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(DataBufferDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(ResourceDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertEquals(Jaxb2XmlDecoder.class, getNextDecoder(readers).getClass());
		assertEquals(Jackson2JsonDecoder.class, getNextDecoder(readers).getClass());
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
		assertEquals(StringDecoder.class, getNextDecoder(readers).getClass());
	}

	@Test
	public void defaultAndCustomWriters() throws Exception {

		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		ServerHttpMessageWriter<?> customWriter1 = mock(ServerHttpMessageWriter.class);
		ServerHttpMessageWriter<?> customWriter2 = mock(ServerHttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodec().encoder(customEncoder1);
		this.configurer.customCodec().encoder(customEncoder2);

		this.configurer.customCodec().writer(customWriter1);
		this.configurer.customCodec().writer(customWriter2);

		List<ServerHttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(13, writers.size());
		assertEquals(ByteArrayEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ByteBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(DataBufferEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ResourceHttpMessageWriter.class, writers.get(index.getAndIncrement()).getClass());
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertEquals(Jaxb2XmlEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(Jackson2JsonEncoder.class, getNextEncoder(writers).getClass());
		assertEquals(ServerSentEventHttpMessageWriter.class, writers.get(this.index.getAndIncrement()).getClass());
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
		assertEquals(CharSequenceEncoder.class, getNextEncoder(writers).getClass());
	}

	@Test
	public void defaultsOffCustomReaders() throws Exception {

		Decoder<?> customDecoder1 = mock(Decoder.class);
		Decoder<?> customDecoder2 = mock(Decoder.class);

		when(customDecoder1.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customDecoder2.canDecode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		ServerHttpMessageReader<?> customReader1 = mock(ServerHttpMessageReader.class);
		ServerHttpMessageReader<?> customReader2 = mock(ServerHttpMessageReader.class);

		when(customReader1.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customReader2.canRead(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodec().decoder(customDecoder1);
		this.configurer.customCodec().decoder(customDecoder2);

		this.configurer.customCodec().reader(customReader1);
		this.configurer.customCodec().reader(customReader2);

		this.configurer.registerDefaults(false);

		List<ServerHttpMessageReader<?>> readers = this.configurer.getReaders();

		assertEquals(4, readers.size());
		assertSame(customDecoder1, getNextDecoder(readers));
		assertSame(customReader1, readers.get(this.index.getAndIncrement()));
		assertSame(customDecoder2, getNextDecoder(readers));
		assertSame(customReader2, readers.get(this.index.getAndIncrement()));
	}

	@Test
	public void defaultsOffWithCustomWriters() throws Exception {

		Encoder<?> customEncoder1 = mock(Encoder.class);
		Encoder<?> customEncoder2 = mock(Encoder.class);

		when(customEncoder1.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customEncoder2.canEncode(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		ServerHttpMessageWriter<?> customWriter1 = mock(ServerHttpMessageWriter.class);
		ServerHttpMessageWriter<?> customWriter2 = mock(ServerHttpMessageWriter.class);

		when(customWriter1.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(false);
		when(customWriter2.canWrite(ResolvableType.forClass(Object.class), null)).thenReturn(true);

		this.configurer.customCodec().encoder(customEncoder1);
		this.configurer.customCodec().encoder(customEncoder2);

		this.configurer.customCodec().writer(customWriter1);
		this.configurer.customCodec().writer(customWriter2);

		this.configurer.registerDefaults(false);

		List<ServerHttpMessageWriter<?>> writers = this.configurer.getWriters();

		assertEquals(4, writers.size());
		assertSame(customEncoder1, getNextEncoder(writers));
		assertSame(customWriter1, writers.get(this.index.getAndIncrement()));
		assertSame(customEncoder2, getNextEncoder(writers));
		assertSame(customWriter2, writers.get(this.index.getAndIncrement()));
	}

	@Test
	public void jackson2DecoderOverride() throws Exception {

		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		this.configurer.defaultCodec().jackson2Decoder(decoder);

		assertSame(decoder, this.configurer.getReaders().stream()
				.filter(writer -> writer instanceof DecoderHttpMessageReader)
				.map(writer -> ((DecoderHttpMessageReader<?>) writer).getDecoder())
				.filter(e -> Jackson2JsonDecoder.class.equals(e.getClass()))
				.findFirst()
				.filter(e -> e == decoder).orElse(null));
	}

	@Test
	public void jackson2EncoderOverride() throws Exception {

		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		this.configurer.defaultCodec().jackson2Encoder(encoder);

		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> writer instanceof EncoderHttpMessageWriter)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder())
				.filter(e -> Jackson2JsonEncoder.class.equals(e.getClass()))
				.findFirst()
				.filter(e -> e == encoder).orElse(null));

		assertSame(encoder, this.configurer.getWriters().stream()
				.filter(writer -> ServerSentEventHttpMessageWriter.class.equals(writer.getClass()))
				.map(writer -> (ServerSentEventHttpMessageWriter) writer)
				.findFirst()
				.map(ServerSentEventHttpMessageWriter::getEncoder)
				.filter(e -> e == encoder).orElse(null));
	}


	private Decoder<?> getNextDecoder(List<ServerHttpMessageReader<?>> readers) {
		HttpMessageReader<?> reader = readers.get(this.index.getAndIncrement());
		assertEquals(DecoderHttpMessageReader.class, reader.getClass());
		return ((DecoderHttpMessageReader) reader).getDecoder();
	}

	private Encoder<?> getNextEncoder(List<ServerHttpMessageWriter<?>> writers) {
		HttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertEquals(EncoderHttpMessageWriter.class, writer.getClass());
		return ((EncoderHttpMessageWriter) writer).getEncoder();
	}

	private void assertStringDecoder(Decoder<?> decoder, boolean textOnly) {
		assertEquals(StringDecoder.class, decoder.getClass());
		assertTrue(decoder.canDecode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, decoder.canDecode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertStringEncoder(Encoder<?> encoder, boolean textOnly) {
		assertEquals(CharSequenceEncoder.class, encoder.getClass());
		assertTrue(encoder.canEncode(forClass(String.class), MimeTypeUtils.TEXT_PLAIN));
		assertEquals(!textOnly, encoder.canEncode(forClass(String.class), MediaType.TEXT_EVENT_STREAM));
	}

	private void assertSseWriter(List<ServerHttpMessageWriter<?>> writers) {
		ServerHttpMessageWriter<?> writer = writers.get(this.index.getAndIncrement());
		assertEquals(ServerSentEventHttpMessageWriter.class, writer.getClass());
		Encoder<?> encoder = ((ServerSentEventHttpMessageWriter) writer).getEncoder();
		assertNotNull(encoder);
		assertEquals(Jackson2JsonEncoder.class, encoder.getClass());
	}

}
