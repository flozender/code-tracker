/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.SimpleJsonParser;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link SimpleJsonParser}.
 * 
 * @author Dave Syer
 */
public class SimpleJsonParserTests {

	private final JsonParser parser = getParser();

	protected JsonParser getParser() {
		return new SimpleJsonParser();
	}

	@Test
	public void testSimpleMap() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1}");
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals(1L, ((Number) map.get("spam")).longValue());
	}

	@Test
	public void testDoubleValie() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1.23}");
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals(1.23d, map.get("spam"));
	}

	@Test
	public void testEmptyMap() {
		Map<String, Object> map = this.parser.parseMap("{}");
		assertEquals(0, map.size());
	}

	@Test
	public void testSimpleList() {
		List<Object> list = this.parser.parseList("[\"foo\",\"bar\",1]");
		assertEquals(3, list.size());
		assertEquals("bar", list.get(1));
	}

	@Test
	public void testEmptyList() {
		List<Object> list = this.parser.parseList("[]");
		assertEquals(0, list.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testListOfMaps() {
		List<Object> list = this.parser
				.parseList("[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]");
		assertEquals(2, list.size());
		assertEquals(2, ((Map<String, Object>) list.get(1)).size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMapOfLists() {
		Map<String, Object> map = this.parser
				.parseMap("{\"foo\":[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]}");
		assertEquals(1, map.size());
		assertEquals(2, ((List<Object>) map.get("foo")).size());
	}

}
