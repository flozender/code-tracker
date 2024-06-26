/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.util.xcontent.support;

import org.elasticsearch.util.xcontent.XContentGenerator;
import org.elasticsearch.util.xcontent.XContentParser;
import org.elasticsearch.util.xcontent.builder.XContentBuilder;

import java.io.IOException;
import java.util.*;

/**
 * @author kimchy (shay.banon)
 */
public class XContentMapConverter {

    public static Map<String, Object> readMap(XContentParser parser) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        XContentParser.Token t = parser.currentToken();
        if (t == null) {
            t = parser.nextToken();
        }
        if (t == XContentParser.Token.START_OBJECT) {
            t = parser.nextToken();
        }
        for (; t == XContentParser.Token.FIELD_NAME; t = parser.nextToken()) {
            // Must point to field name
            String fieldName = parser.currentName();
            // And then the value...
            t = parser.nextToken();
            Object value = readValue(parser, t);
            map.put(fieldName, value);
        }
        return map;
    }

    private static List<Object> readList(XContentParser parser, XContentParser.Token t) throws IOException {
        ArrayList<Object> list = new ArrayList<Object>();
        while ((t = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
            list.add(readValue(parser, t));
        }
        return list;
    }

    private static Object readValue(XContentParser parser, XContentParser.Token t) throws IOException {
        if (t == XContentParser.Token.VALUE_NULL) {
            return null;
        } else if (t == XContentParser.Token.VALUE_STRING) {
            return parser.text();
        } else if (t == XContentParser.Token.VALUE_NUMBER) {
            XContentParser.NumberType numberType = parser.numberType();
            if (numberType == XContentParser.NumberType.INT) {
                return parser.intValue();
            } else if (numberType == XContentParser.NumberType.LONG) {
                return parser.longValue();
            } else if (numberType == XContentParser.NumberType.FLOAT) {
                return parser.floatValue();
            } else if (numberType == XContentParser.NumberType.DOUBLE) {
                return parser.doubleValue();
            }
        } else if (t == XContentParser.Token.VALUE_BOOLEAN) {
            return parser.booleanValue();
        } else if (t == XContentParser.Token.START_OBJECT) {
            return readMap(parser);
        } else if (t == XContentParser.Token.START_ARRAY) {
            return readList(parser, t);
        }
        return null;
    }

    public static void writeMap(XContentGenerator gen, Map<String, Object> map) throws IOException {
        gen.writeStartObject();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            gen.writeFieldName(entry.getKey());
            Object value = entry.getValue();
            if (value == null) {
                gen.writeNull();
            } else {
                writeValue(gen, value);
            }
        }

        gen.writeEndObject();
    }

    private static void writeIterable(XContentGenerator gen, Iterable iterable) throws IOException {
        gen.writeStartArray();
        for (Object value : iterable) {
            writeValue(gen, value);
        }
        gen.writeEndArray();
    }

    private static void writeObjectArray(XContentGenerator gen, Object[] array) throws IOException {
        gen.writeStartArray();
        for (Object value : array) {
            writeValue(gen, value);
        }
        gen.writeEndArray();
    }

    private static void writeValue(XContentGenerator gen, Object value) throws IOException {
        Class type = value.getClass();
        if (type == String.class) {
            gen.writeString((String) value);
        } else if (type == Integer.class) {
            gen.writeNumber(((Integer) value).intValue());
        } else if (type == Long.class) {
            gen.writeNumber(((Long) value).longValue());
        } else if (type == Float.class) {
            gen.writeNumber(((Float) value).floatValue());
        } else if (type == Double.class) {
            gen.writeNumber(((Double) value).doubleValue());
        } else if (type == Short.class) {
            gen.writeNumber(((Short) value).shortValue());
        } else if (type == Boolean.class) {
            gen.writeBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Map) {
            writeMap(gen, (Map) value);
        } else if (value instanceof Iterable) {
            writeIterable(gen, (Iterable) value);
        } else if (value instanceof Object[]) {
            writeObjectArray(gen, (Object[]) value);
        } else if (type == byte[].class) {
            gen.writeBinary((byte[]) value);
        } else if (type == Date.class) {
            gen.writeString(XContentBuilder.defaultDatePrinter.print(((Date) value).getTime()));
        } else if (type == java.sql.Date.class) {
            gen.writeString(XContentBuilder.defaultDatePrinter.print(((java.sql.Date) value).getTime()));
        } else {
            gen.writeString(value.toString());
        }
    }
}
