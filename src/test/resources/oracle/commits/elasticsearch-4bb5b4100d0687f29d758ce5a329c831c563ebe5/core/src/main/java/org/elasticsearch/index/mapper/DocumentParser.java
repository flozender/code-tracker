/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.elasticsearch.index.mapper;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.CloseableThreadLocal;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.mapper.core.BinaryFieldMapper;
import org.elasticsearch.index.mapper.core.BooleanFieldMapper;
import org.elasticsearch.index.mapper.core.DateFieldMapper;
import org.elasticsearch.index.mapper.core.DateFieldMapper.DateFieldType;
import org.elasticsearch.index.mapper.core.DoubleFieldMapper;
import org.elasticsearch.index.mapper.core.FloatFieldMapper;
import org.elasticsearch.index.mapper.core.IntegerFieldMapper;
import org.elasticsearch.index.mapper.core.LongFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper.StringFieldType;
import org.elasticsearch.index.mapper.internal.TypeFieldMapper;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;
import org.elasticsearch.index.mapper.object.ArrayValueMapperParser;
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.elasticsearch.index.mapper.object.RootObjectMapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A parser for documents, given mappings from a DocumentMapper */
class DocumentParser implements Closeable {

    private CloseableThreadLocal<ParseContext.InternalParseContext> cache = new CloseableThreadLocal<ParseContext.InternalParseContext>() {
        @Override
        protected ParseContext.InternalParseContext initialValue() {
            return new ParseContext.InternalParseContext(indexSettings.getSettings(), docMapperParser, docMapper, new ContentPath(0));
        }
    };

    private final IndexSettings indexSettings;
    private final DocumentMapperParser docMapperParser;
    private final DocumentMapper docMapper;

    public DocumentParser(IndexSettings indexSettings, DocumentMapperParser docMapperParser, DocumentMapper docMapper) {
        this.indexSettings = indexSettings;
        this.docMapperParser = docMapperParser;
        this.docMapper = docMapper;
    }

    public ParsedDocument parseDocument(SourceToParse source) throws MapperParsingException {
        if (docMapper.type().equals(MapperService.DEFAULT_MAPPING)) {
            throw new IllegalArgumentException("It is forbidden to index into the default mapping [" + MapperService.DEFAULT_MAPPING + "]");
        }

        ParseContext.InternalParseContext context = cache.get();

        final Mapping mapping = docMapper.mapping();
        if (source.type() != null && !source.type().equals(docMapper.type())) {
            throw new MapperParsingException("Type mismatch, provide type [" + source.type() + "] but mapper is of type [" + docMapper.type() + "]");
        }
        source.type(docMapper.type());

        XContentParser parser = source.parser();
        try {
            if (parser == null) {
                parser = XContentHelper.createParser(source.source());
            }
            context.reset(parser, new ParseContext.Document(), source);

            // will result in START_OBJECT
            XContentParser.Token token = parser.nextToken();
            if (token != XContentParser.Token.START_OBJECT) {
                throw new MapperParsingException("Malformed content, must start with an object");
            }

            boolean emptyDoc = false;
            if (mapping.root.isEnabled()) {
                token = parser.nextToken();
                if (token == XContentParser.Token.END_OBJECT) {
                    // empty doc, we can handle it...
                    emptyDoc = true;
                } else if (token != XContentParser.Token.FIELD_NAME) {
                    throw new MapperParsingException("Malformed content, after first object, either the type field or the actual properties should exist");
                }
            }

            for (MetadataFieldMapper metadataMapper : mapping.metadataMappers) {
                metadataMapper.preParse(context);
            }

            if (mapping.root.isEnabled() == false) {
                // entire type is disabled
                parser.skipChildren();
            } else if (emptyDoc == false) {
                Mapper update = parseObject(context, mapping.root, true);
                if (update != null) {
                    context.addDynamicMappingsUpdate(update);
                }
            }

            for (MetadataFieldMapper metadataMapper : mapping.metadataMappers) {
                metadataMapper.postParse(context);
            }

            // try to parse the next token, this should be null if the object is ended properly
            // but will throw a JSON exception if the extra tokens is not valid JSON (this will be handled by the catch)
            if (source.parser() == null && parser != null) {
                // only check for end of tokens if we created the parser here
                token = parser.nextToken();
                if (token != null) {
                    throw new IllegalArgumentException("Malformed content, found extra data after parsing: " + token);
                }
            }

        } catch (Throwable e) {
            // if its already a mapper parsing exception, no need to wrap it...
            if (e instanceof MapperParsingException) {
                throw (MapperParsingException) e;
            }

            // Throw a more meaningful message if the document is empty.
            if (source.source() != null && source.source().length() == 0) {
                throw new MapperParsingException("failed to parse, document is empty");
            }

            throw new MapperParsingException("failed to parse", e);
        } finally {
            // only close the parser when its not provided externally
            if (source.parser() == null && parser != null) {
                parser.close();
            }
        }
        // reverse the order of docs for nested docs support, parent should be last
        if (context.docs().size() > 1) {
            Collections.reverse(context.docs());
        }
        // apply doc boost
        if (context.docBoost() != 1.0f) {
            Set<String> encounteredFields = new HashSet<>();
            for (ParseContext.Document doc : context.docs()) {
                encounteredFields.clear();
                for (IndexableField field : doc) {
                    if (field.fieldType().indexOptions() != IndexOptions.NONE && !field.fieldType().omitNorms()) {
                        if (!encounteredFields.contains(field.name())) {
                            ((Field) field).setBoost(context.docBoost() * field.boost());
                            encounteredFields.add(field.name());
                        }
                    }
                }
            }
        }

        Mapper rootDynamicUpdate = context.dynamicMappingsUpdate();
        Mapping update = null;
        if (rootDynamicUpdate != null) {
            update = mapping.mappingUpdate(rootDynamicUpdate);
        }

        ParsedDocument doc = new ParsedDocument(context.uid(), context.version(), context.seqNo(), context.id(), context.type(), source.routing(), source.timestamp(), source.ttl(), context.docs(),
            context.source(), update).parent(source.parent());
        // reset the context to free up memory
        context.reset(null, null, null);
        return doc;
    }

    static ObjectMapper parseObject(ParseContext context, ObjectMapper mapper, boolean atRoot) throws IOException {
        if (mapper.isEnabled() == false) {
            context.parser().skipChildren();
            return null;
        }
        XContentParser parser = context.parser();

        String currentFieldName = parser.currentName();
        if (atRoot && MapperService.isMetadataField(currentFieldName)) {
            throw new MapperParsingException("Field [" + currentFieldName + "] is a metadata field and cannot be added inside a document. Use the index API request parameters.");
        }
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_NULL) {
            // the object is null ("obj1" : null), simply bail
            return null;
        }

        if (token.isValue()) {
            throw new MapperParsingException("object mapping for [" + mapper.name() + "] tried to parse field [" + currentFieldName + "] as object, but found a concrete value");
        }

        ObjectMapper.Nested nested = mapper.nested();
        if (nested.isNested()) {
            context = context.createNestedContext(mapper.fullPath());
            ParseContext.Document nestedDoc = context.doc();
            ParseContext.Document parentDoc = nestedDoc.getParent();
            // pre add the uid field if possible (id was already provided)
            IndexableField uidField = parentDoc.getField(UidFieldMapper.NAME);
            if (uidField != null) {
                // we don't need to add it as a full uid field in nested docs, since we don't need versioning
                // we also rely on this for UidField#loadVersion

                // this is a deeply nested field
                nestedDoc.add(new Field(UidFieldMapper.NAME, uidField.stringValue(), UidFieldMapper.Defaults.NESTED_FIELD_TYPE));
            }
            // the type of the nested doc starts with __, so we can identify that its a nested one in filters
            // note, we don't prefix it with the type of the doc since it allows us to execute a nested query
            // across types (for example, with similar nested objects)
            nestedDoc.add(new Field(TypeFieldMapper.NAME, mapper.nestedTypePathAsString(), TypeFieldMapper.Defaults.FIELD_TYPE));
        }

        // if we are at the end of the previous object, advance
        if (token == XContentParser.Token.END_OBJECT) {
            token = parser.nextToken();
        }
        if (token == XContentParser.Token.START_OBJECT) {
            // if we are just starting an OBJECT, advance, this is the object we are parsing, we need the name first
            token = parser.nextToken();
        }

        ObjectMapper update = null;
        while (token != XContentParser.Token.END_OBJECT) {
            ObjectMapper newUpdate = null;
            if (token == XContentParser.Token.START_OBJECT) {
                newUpdate = parseObject(context, mapper, currentFieldName);
            } else if (token == XContentParser.Token.START_ARRAY) {
                newUpdate = parseArray(context, mapper, currentFieldName);
            } else if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_NULL) {
                parseNullValue(context, mapper, currentFieldName);
            } else if (token == null) {
                throw new MapperParsingException("object mapping for [" + mapper.name() + "] tried to parse field [" + currentFieldName + "] as object, but got EOF, has a concrete value been provided to it?");
            } else if (token.isValue()) {
                newUpdate = parseValue(context, mapper, currentFieldName, token);
            }
            token = parser.nextToken();
            if (newUpdate != null) {
                if (update == null) {
                    update = newUpdate;
                } else {
                    update = update.merge(newUpdate, false);
                }
            }
        }
        // restore the enable path flag
        if (nested.isNested()) {
            ParseContext.Document nestedDoc = context.doc();
            ParseContext.Document parentDoc = nestedDoc.getParent();
            if (nested.isIncludeInParent()) {
                for (IndexableField field : nestedDoc.getFields()) {
                    if (field.name().equals(UidFieldMapper.NAME) || field.name().equals(TypeFieldMapper.NAME)) {
                        continue;
                    } else {
                        parentDoc.add(field);
                    }
                }
            }
            if (nested.isIncludeInRoot()) {
                ParseContext.Document rootDoc = context.rootDoc();
                // don't add it twice, if its included in parent, and we are handling the master doc...
                if (!nested.isIncludeInParent() || parentDoc != rootDoc) {
                    for (IndexableField field : nestedDoc.getFields()) {
                        if (field.name().equals(UidFieldMapper.NAME) || field.name().equals(TypeFieldMapper.NAME)) {
                            continue;
                        } else {
                            rootDoc.add(field);
                        }
                    }
                }
            }
        }
        return update;
    }

    private static Mapper parseObjectOrField(ParseContext context, Mapper mapper) throws IOException {
        if (mapper instanceof ObjectMapper) {
            return parseObject(context, (ObjectMapper) mapper, false);
        } else {
            FieldMapper fieldMapper = (FieldMapper)mapper;
            Mapper update = fieldMapper.parse(context);
            if (fieldMapper.copyTo() != null) {
                parseCopyFields(context, fieldMapper, fieldMapper.copyTo().copyToFields());
            }
            return update;
        }
    }

    private static ObjectMapper parseObject(final ParseContext context, ObjectMapper mapper, String currentFieldName) throws IOException {
        if (currentFieldName == null) {
            throw new MapperParsingException("object mapping [" + mapper.name() + "] trying to serialize an object with no field associated with it, current value [" + context.parser().textOrNull() + "]");
        }
        context.path().add(currentFieldName);

        ObjectMapper update = null;
        Mapper objectMapper = mapper.getMapper(currentFieldName);
        if (objectMapper != null) {
            final Mapper subUpdate = parseObjectOrField(context, objectMapper);
            if (subUpdate != null) {
                // propagate mapping update
                update = mapper.mappingUpdate(subUpdate);
            }
        } else {
            ObjectMapper.Dynamic dynamic = mapper.dynamic();
            if (dynamic == null) {
                dynamic = dynamicOrDefault(context.root().dynamic());
            }
            if (dynamic == ObjectMapper.Dynamic.STRICT) {
                throw new StrictDynamicMappingException(mapper.fullPath(), currentFieldName);
            } else if (dynamic == ObjectMapper.Dynamic.TRUE) {
                // remove the current field name from path, since template search and the object builder add it as well...
                context.path().remove();
                Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "object");
                if (builder == null) {
                    builder = new ObjectMapper.Builder(currentFieldName).enabled(true);
                    // if this is a non root object, then explicitly set the dynamic behavior if set
                    if (!(mapper instanceof RootObjectMapper) && mapper.dynamic() != ObjectMapper.Defaults.DYNAMIC) {
                        ((ObjectMapper.Builder) builder).dynamic(mapper.dynamic());
                    }
                }
                Mapper.BuilderContext builderContext = new Mapper.BuilderContext(context.indexSettings(), context.path());
                objectMapper = builder.build(builderContext);
                context.path().add(currentFieldName);
                update = mapper.mappingUpdate(parseAndMergeUpdate(objectMapper, context));
            } else {
                // not dynamic, read everything up to end object
                context.parser().skipChildren();
            }
        }

        context.path().remove();
        return update;
    }

    private static ObjectMapper parseArray(ParseContext context, ObjectMapper parentMapper, String lastFieldName) throws IOException {
        String arrayFieldName = lastFieldName;
        Mapper mapper = parentMapper.getMapper(lastFieldName);
        if (mapper != null) {
            // There is a concrete mapper for this field already. Need to check if the mapper
            // expects an array, if so we pass the context straight to the mapper and if not
            // we serialize the array components
            if (mapper instanceof ArrayValueMapperParser) {
                final Mapper subUpdate = parseObjectOrField(context, mapper);
                if (subUpdate != null) {
                    // propagate the mapping update
                    return parentMapper.mappingUpdate(subUpdate);
                } else {
                    return null;
                }
            } else {
                return parseNonDynamicArray(context, parentMapper, lastFieldName, arrayFieldName);
            }
        } else {

            ObjectMapper.Dynamic dynamic = parentMapper.dynamic();
            if (dynamic == null) {
                dynamic = dynamicOrDefault(context.root().dynamic());
            }
            if (dynamic == ObjectMapper.Dynamic.STRICT) {
                throw new StrictDynamicMappingException(parentMapper.fullPath(), arrayFieldName);
            } else if (dynamic == ObjectMapper.Dynamic.TRUE) {
                Mapper.Builder builder = context.root().findTemplateBuilder(context, arrayFieldName, "object");
                if (builder == null) {
                    return parseNonDynamicArray(context, parentMapper, lastFieldName, arrayFieldName);
                }
                Mapper.BuilderContext builderContext = new Mapper.BuilderContext(context.indexSettings(), context.path());
                mapper = builder.build(builderContext);
                if (mapper != null && mapper instanceof ArrayValueMapperParser) {
                    context.path().add(arrayFieldName);
                    mapper = parseAndMergeUpdate(mapper, context);
                    return parentMapper.mappingUpdate(mapper);
                } else {
                    return parseNonDynamicArray(context, parentMapper, lastFieldName, arrayFieldName);
                }
            } else {
                return parseNonDynamicArray(context, parentMapper, lastFieldName, arrayFieldName);
            }
        }
    }

    private static ObjectMapper parseNonDynamicArray(ParseContext context, ObjectMapper mapper, String lastFieldName, String arrayFieldName) throws IOException {
        XContentParser parser = context.parser();
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
            if (token == XContentParser.Token.START_OBJECT) {
                return parseObject(context, mapper, lastFieldName);
            } else if (token == XContentParser.Token.START_ARRAY) {
                return parseArray(context, mapper, lastFieldName);
            } else if (token == XContentParser.Token.FIELD_NAME) {
                lastFieldName = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_NULL) {
                parseNullValue(context, mapper, lastFieldName);
            } else if (token == null) {
                throw new MapperParsingException("object mapping for [" + mapper.name() + "] with array for [" + arrayFieldName + "] tried to parse as array, but got EOF, is there a mismatch in types for the same field?");
            } else {
                return parseValue(context, mapper, lastFieldName, token);
            }
        }
        return null;
    }

    private static ObjectMapper parseValue(final ParseContext context, ObjectMapper parentMapper, String currentFieldName, XContentParser.Token token) throws IOException {
        if (currentFieldName == null) {
            throw new MapperParsingException("object mapping [" + parentMapper.name() + "] trying to serialize a value with no field associated with it, current value [" + context.parser().textOrNull() + "]");
        }
        Mapper mapper = parentMapper.getMapper(currentFieldName);
        if (mapper != null) {
            Mapper subUpdate = parseObjectOrField(context, mapper);
            if (subUpdate == null) {
                return null;
            }
            return parentMapper.mappingUpdate(subUpdate);
        } else {
            return parseDynamicValue(context, parentMapper, currentFieldName, token);
        }
    }

    private static void parseNullValue(ParseContext context, ObjectMapper parentMapper, String lastFieldName) throws IOException {
        // we can only handle null values if we have mappings for them
        Mapper mapper = parentMapper.getMapper(lastFieldName);
        if (mapper != null) {
            // TODO: passing null to an object seems bogus?
            parseObjectOrField(context, mapper);
        } else if (parentMapper.dynamic() == ObjectMapper.Dynamic.STRICT) {
            throw new StrictDynamicMappingException(parentMapper.fullPath(), lastFieldName);
        }
    }

    private static Mapper.Builder<?,?> createBuilderFromFieldType(final ParseContext context, MappedFieldType fieldType, String currentFieldName) {
        Mapper.Builder builder = null;
        if (fieldType instanceof StringFieldType) {
            builder = context.root().findTemplateBuilder(context, currentFieldName, "string");
            if (builder == null) {
                builder = new StringFieldMapper.Builder(currentFieldName);
            }
        } else if (fieldType instanceof DateFieldType) {
            builder = context.root().findTemplateBuilder(context, currentFieldName, "date");
            if (builder == null) {
                builder = new DateFieldMapper.Builder(currentFieldName);
            }
        } else if (fieldType.numericType() != null) {
            switch (fieldType.numericType()) {
            case LONG:
                builder = context.root().findTemplateBuilder(context, currentFieldName, "long");
                if (builder == null) {
                    builder = new LongFieldMapper.Builder(currentFieldName);
                }
                break;
            case DOUBLE:
                builder = context.root().findTemplateBuilder(context, currentFieldName, "double");
                if (builder == null) {
                    builder = new DoubleFieldMapper.Builder(currentFieldName);
                }
                break;
            case INT:
                builder = context.root().findTemplateBuilder(context, currentFieldName, "integer");
                if (builder == null) {
                    builder = new IntegerFieldMapper.Builder(currentFieldName);
                }
                break;
            case FLOAT:
                builder = context.root().findTemplateBuilder(context, currentFieldName, "float");
                if (builder == null) {
                    builder = new FloatFieldMapper.Builder(currentFieldName);
                }
                break;
            default:
                throw new AssertionError("Unexpected numeric type " + fieldType.numericType());
            }
        }
        return builder;
    }

    private static Mapper.Builder<?,?> createBuilderFromDynamicValue(final ParseContext context, XContentParser.Token token, String currentFieldName) throws IOException {
        if (token == XContentParser.Token.VALUE_STRING) {
            // do a quick test to see if its fits a dynamic template, if so, use it.
            // we need to do it here so we can handle things like attachment templates, where calling
            // text (to see if its a date) causes the binary value to be cleared
            {
                Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "string", null);
                if (builder != null) {
                    return builder;
                }
            }

            if (context.root().dateDetection()) {
                String text = context.parser().text();
                // a safe check since "1" gets parsed as well
                if (Strings.countOccurrencesOf(text, ":") > 1 || Strings.countOccurrencesOf(text, "-") > 1 || Strings.countOccurrencesOf(text, "/") > 1) {
                    for (FormatDateTimeFormatter dateTimeFormatter : context.root().dynamicDateTimeFormatters()) {
                        try {
                            dateTimeFormatter.parser().parseMillis(text);
                            Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "date");
                            if (builder == null) {
                                builder = new DateFieldMapper.Builder(currentFieldName).dateTimeFormatter(dateTimeFormatter);
                            }
                            return builder;
                        } catch (Exception e) {
                            // failure to parse this, continue
                        }
                    }
                }
            }
            if (context.root().numericDetection()) {
                String text = context.parser().text();
                try {
                    Long.parseLong(text);
                    Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "long");
                    if (builder == null) {
                        builder = new LongFieldMapper.Builder(currentFieldName);
                    }
                    return builder;
                } catch (NumberFormatException e) {
                    // not a long number
                }
                try {
                    Double.parseDouble(text);
                    Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "double");
                    if (builder == null) {
                        builder = new DoubleFieldMapper.Builder(currentFieldName);
                    }
                    return builder;
                } catch (NumberFormatException e) {
                    // not a long number
                }
            }
            Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "string");
            if (builder == null) {
                builder = new StringFieldMapper.Builder(currentFieldName);
            }
            return builder;
        } else if (token == XContentParser.Token.VALUE_NUMBER) {
            XContentParser.NumberType numberType = context.parser().numberType();
            if (numberType == XContentParser.NumberType.INT || numberType == XContentParser.NumberType.LONG) {
                Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "long");
                if (builder == null) {
                    builder = new LongFieldMapper.Builder(currentFieldName);
                }
                return builder;
            } else if (numberType == XContentParser.NumberType.FLOAT || numberType == XContentParser.NumberType.DOUBLE) {
                Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "double");
                if (builder == null) {
                    // no templates are defined, we use float by default instead of double
                    // since this is much more space-efficient and should be enough most of
                    // the time
                    builder = new FloatFieldMapper.Builder(currentFieldName);
                }
                return builder;
            }
        } else if (token == XContentParser.Token.VALUE_BOOLEAN) {
            Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "boolean");
            if (builder == null) {
                builder = new BooleanFieldMapper.Builder(currentFieldName);
            }
            return builder;
        } else if (token == XContentParser.Token.VALUE_EMBEDDED_OBJECT) {
            Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, "binary");
            if (builder == null) {
                builder = new BinaryFieldMapper.Builder(currentFieldName);
            }
            return builder;
        } else {
            Mapper.Builder builder = context.root().findTemplateBuilder(context, currentFieldName, null);
            if (builder != null) {
                return builder;
            }
        }
        // TODO how do we identify dynamically that its a binary value?
        throw new IllegalStateException("Can't handle serializing a dynamic type with content token [" + token + "] and field name [" + currentFieldName + "]");
    }

    private static ObjectMapper parseDynamicValue(final ParseContext context, ObjectMapper parentMapper, String currentFieldName, XContentParser.Token token) throws IOException {
        ObjectMapper.Dynamic dynamic = parentMapper.dynamic();
        if (dynamic == null) {
            dynamic = dynamicOrDefault(context.root().dynamic());
        }
        if (dynamic == ObjectMapper.Dynamic.STRICT) {
            throw new StrictDynamicMappingException(parentMapper.fullPath(), currentFieldName);
        }
        if (dynamic == ObjectMapper.Dynamic.FALSE) {
            return null;
        }
        final String path = context.path().pathAsText(currentFieldName);
        final Mapper.BuilderContext builderContext = new Mapper.BuilderContext(context.indexSettings(), context.path());
        final MappedFieldType existingFieldType = context.mapperService().fullName(path);
        Mapper.Builder builder = null;
        if (existingFieldType != null) {
            // create a builder of the same type
            builder = createBuilderFromFieldType(context, existingFieldType, currentFieldName);
        }
        if (builder == null) {
            builder = createBuilderFromDynamicValue(context, token, currentFieldName);
        }
        Mapper mapper = builder.build(builderContext);
        if (existingFieldType != null) {
            // try to not introduce a conflict
            mapper = mapper.updateFieldType(Collections.singletonMap(path, existingFieldType));
        }

        mapper = parseAndMergeUpdate(mapper, context);

        ObjectMapper update = null;
        if (mapper != null) {
            update = parentMapper.mappingUpdate(mapper);
        }
        return update;
    }

    /** Creates instances of the fields that the current field should be copied to */
    private static void parseCopyFields(ParseContext context, FieldMapper fieldMapper, List<String> copyToFields) throws IOException {
        if (!context.isWithinCopyTo() && copyToFields.isEmpty() == false) {
            context = context.createCopyToContext();
            for (String field : copyToFields) {
                // In case of a hierarchy of nested documents, we need to figure out
                // which document the field should go to
                ParseContext.Document targetDoc = null;
                for (ParseContext.Document doc = context.doc(); doc != null; doc = doc.getParent()) {
                    if (field.startsWith(doc.getPrefix())) {
                        targetDoc = doc;
                        break;
                    }
                }
                assert targetDoc != null;
                final ParseContext copyToContext;
                if (targetDoc == context.doc()) {
                    copyToContext = context;
                } else {
                    copyToContext = context.switchDoc(targetDoc);
                }
                parseCopy(field, copyToContext);
            }
        }
    }

    /** Creates an copy of the current field with given field name and boost */
    private static void parseCopy(String field, ParseContext context) throws IOException {
        FieldMapper fieldMapper = context.docMapper().mappers().getMapper(field);
        if (fieldMapper != null) {
            fieldMapper.parse(context);
        } else {
            // The path of the dest field might be completely different from the current one so we need to reset it
            context = context.overridePath(new ContentPath(0));

            String[] paths = Strings.splitStringToArray(field, '.');
            String fieldName = paths[paths.length-1];
            ObjectMapper mapper = context.root();
            ObjectMapper[] mappers = new ObjectMapper[paths.length-1];
            if (paths.length > 1) {
                ObjectMapper parent = context.root();
                for (int i = 0; i < paths.length-1; i++) {
                    mapper = context.docMapper().objectMappers().get(context.path().pathAsText(paths[i]));
                    if (mapper == null) {
                        // One mapping is missing, check if we are allowed to create a dynamic one.
                        ObjectMapper.Dynamic dynamic = parent.dynamic();
                        if (dynamic == null) {
                            dynamic = dynamicOrDefault(context.root().dynamic());
                        }

                        switch (dynamic) {
                            case STRICT:
                                throw new StrictDynamicMappingException(parent.fullPath(), paths[i]);
                            case TRUE:
                                Mapper.Builder builder = context.root().findTemplateBuilder(context, paths[i], "object");
                                if (builder == null) {
                                    // if this is a non root object, then explicitly set the dynamic behavior if set
                                    if (!(parent instanceof RootObjectMapper) && parent.dynamic() != ObjectMapper.Defaults.DYNAMIC) {
                                        ((ObjectMapper.Builder) builder).dynamic(parent.dynamic());
                                    }
                                    builder = new ObjectMapper.Builder(paths[i]).enabled(true);
                                }
                                Mapper.BuilderContext builderContext = new Mapper.BuilderContext(context.indexSettings(), context.path());
                                mapper = (ObjectMapper) builder.build(builderContext);
                                if (mapper.nested() != ObjectMapper.Nested.NO) {
                                    throw new MapperParsingException("It is forbidden to create dynamic nested objects ([" + context.path().pathAsText(paths[i]) + "]) through `copy_to`");
                                }
                                break;
                            case FALSE:
                              // Maybe we should log something to tell the user that the copy_to is ignored in this case.
                              break;
                            default:
                                throw new AssertionError("Unexpected dynamic type " + dynamic);

                        }
                    }
                    context.path().add(paths[i]);
                    mappers[i] = mapper;
                    parent = mapper;
                }
            }
            ObjectMapper update = parseDynamicValue(context, mapper, fieldName, context.parser().currentToken());
            assert update != null; // we are parsing a dynamic value so we necessarily created a new mapping

            if (paths.length > 1) {
                for (int i = paths.length - 2; i >= 0; i--) {
                    ObjectMapper parent = context.root();
                    if (i > 0) {
                        parent = mappers[i-1];
                    }
                    assert parent != null;
                    update = parent.mappingUpdate(update);
                }
            }
            context.addDynamicMappingsUpdate(update);
        }
    }

    /**
     * Parse the given {@code context} with the given {@code mapper} and apply
     * the potential mapping update in-place. This method is useful when
     * composing mapping updates.
     */
    private static <M extends Mapper> M parseAndMergeUpdate(M mapper, ParseContext context) throws IOException {
        final Mapper update = parseObjectOrField(context, mapper);
        if (update != null) {
            mapper = (M) mapper.merge(update, false);
        }
        return mapper;
    }

    private static ObjectMapper.Dynamic dynamicOrDefault(ObjectMapper.Dynamic dynamic) {
        return dynamic == null ? ObjectMapper.Dynamic.TRUE : dynamic;
    }

    @Override
    public void close() {
        cache.close();
    }
}
