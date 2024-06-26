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

package org.elasticsearch.index;

import org.apache.lucene.document.NumericDocValuesField;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.IndexingSlowLog.SlowLogParsedDocumentPrinter;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class IndexingSlowLogTests extends ESTestCase {
    public void testSlowLogParsedDocumentPrinterSourceToLog() throws IOException {
        BytesReference source = JsonXContent.contentBuilder().startObject().field("foo", "bar").endObject().bytes();
        ParsedDocument pd = new ParsedDocument(new NumericDocValuesField("version", 1), new NumericDocValuesField("seqNo", 1), "id",
                "test", null, 0, -1, null, source, null);
        Index index = new Index("foo", "123");
        // Turning off document logging doesn't log source[]
        SlowLogParsedDocumentPrinter p = new SlowLogParsedDocumentPrinter(index, pd, 10, true, 0);
        assertThat(p.toString(), not(containsString("source[")));

        // Turning on document logging logs the whole thing
        p = new SlowLogParsedDocumentPrinter(index, pd, 10, true, Integer.MAX_VALUE);
        assertThat(p.toString(), containsString("source[{\"foo\":\"bar\"}]"));

        // And you can truncate the source
        p = new SlowLogParsedDocumentPrinter(index, pd, 10, true, 3);
        assertThat(p.toString(), containsString("source[{\"f]"));

        // And you can truncate the source
        p = new SlowLogParsedDocumentPrinter(index, pd, 10, true, 3);
        assertThat(p.toString(), containsString("source[{\"f]"));
        assertThat(p.toString(), startsWith("[foo/123] took"));
    }

    public void testReformatSetting() {
        IndexMetaData metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING.getKey(), false)
            .build());
        IndexSettings settings = new IndexSettings(metaData, Settings.EMPTY);
        IndexingSlowLog log = new IndexingSlowLog(settings);
        assertFalse(log.isReformat());
        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING.getKey(), "true").build()));
        assertTrue(log.isReformat());

        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING.getKey(), "false").build()));
        assertFalse(log.isReformat());

        settings.updateIndexMetaData(newIndexMeta("index", Settings.EMPTY));
        assertTrue(log.isReformat());

        metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .build());
        settings = new IndexSettings(metaData, Settings.EMPTY);
        log = new IndexingSlowLog(settings);
        assertTrue(log.isReformat());
        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING.getKey(), "NOT A BOOLEAN").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Failed to parse value [NOT A BOOLEAN] cannot be parsed to boolean [ true/1/on/yes OR false/0/off/no ]");
        }
        assertTrue(log.isReformat());
    }

    public void testLevelSetting() {
        SlowLogLevel level = randomFrom(SlowLogLevel.values());
        IndexMetaData metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL_SETTING.getKey(), level)
            .build());
        IndexSettings settings = new IndexSettings(metaData, Settings.EMPTY);
        IndexingSlowLog log = new IndexingSlowLog(settings);
        assertEquals(level, log.getLevel());
        level = randomFrom(SlowLogLevel.values());
        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL_SETTING.getKey(), level).build()));
        assertEquals(level, log.getLevel());
        level = randomFrom(SlowLogLevel.values());
        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL_SETTING.getKey(), level).build()));
        assertEquals(level, log.getLevel());


        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL_SETTING.getKey(), level).build()));
        assertEquals(level, log.getLevel());

        settings.updateIndexMetaData(newIndexMeta("index", Settings.EMPTY));
        assertEquals(SlowLogLevel.TRACE, log.getLevel());

        metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .build());
        settings = new IndexSettings(metaData, Settings.EMPTY);
        log = new IndexingSlowLog(settings);
        assertTrue(log.isReformat());
        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_LEVEL_SETTING.getKey(), "NOT A LEVEL").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "No enum constant org.elasticsearch.index.SlowLogLevel.NOT A LEVEL");
        }
        assertEquals(SlowLogLevel.TRACE, log.getLevel());
    }

    public void testSetLevels() {
        IndexMetaData metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING.getKey(), "100ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING.getKey(), "200ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING.getKey(), "300ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING.getKey(), "400ms")
            .build());
        IndexSettings settings = new IndexSettings(metaData, Settings.EMPTY);
        IndexingSlowLog log = new IndexingSlowLog(settings);
        assertEquals(TimeValue.timeValueMillis(100).nanos(), log.getIndexTraceThreshold());
        assertEquals(TimeValue.timeValueMillis(200).nanos(), log.getIndexDebugThreshold());
        assertEquals(TimeValue.timeValueMillis(300).nanos(), log.getIndexInfoThreshold());
        assertEquals(TimeValue.timeValueMillis(400).nanos(), log.getIndexWarnThreshold());

        settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING.getKey(), "120ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING.getKey(), "220ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING.getKey(), "320ms")
            .put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING.getKey(), "420ms").build()));


        assertEquals(TimeValue.timeValueMillis(120).nanos(), log.getIndexTraceThreshold());
        assertEquals(TimeValue.timeValueMillis(220).nanos(), log.getIndexDebugThreshold());
        assertEquals(TimeValue.timeValueMillis(320).nanos(), log.getIndexInfoThreshold());
        assertEquals(TimeValue.timeValueMillis(420).nanos(), log.getIndexWarnThreshold());

        metaData = newIndexMeta("index", Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .build());
        settings.updateIndexMetaData(metaData);
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexTraceThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexDebugThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexInfoThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexWarnThreshold());

        settings = new IndexSettings(metaData, Settings.EMPTY);
        log = new IndexingSlowLog(settings);

        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexTraceThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexDebugThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexInfoThreshold());
        assertEquals(TimeValue.timeValueMillis(-1).nanos(), log.getIndexWarnThreshold());
        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING.getKey(), "NOT A TIME VALUE").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Failed to parse setting [index.indexing.slowlog.threshold.index.trace] with value [NOT A TIME VALUE] as a time value: unit is missing or unrecognized");
        }

        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING.getKey(), "NOT A TIME VALUE").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Failed to parse setting [index.indexing.slowlog.threshold.index.debug] with value [NOT A TIME VALUE] as a time value: unit is missing or unrecognized");
        }

        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING.getKey(), "NOT A TIME VALUE").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Failed to parse setting [index.indexing.slowlog.threshold.index.info] with value [NOT A TIME VALUE] as a time value: unit is missing or unrecognized");
        }

        try {
            settings.updateIndexMetaData(newIndexMeta("index", Settings.builder().put(IndexingSlowLog.INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING.getKey(), "NOT A TIME VALUE").build()));
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "Failed to parse setting [index.indexing.slowlog.threshold.index.warn] with value [NOT A TIME VALUE] as a time value: unit is missing or unrecognized");
        }
    }

    private IndexMetaData newIndexMeta(String name, Settings indexSettings) {
        Settings build = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
            .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
            .put(indexSettings)
            .build();
        IndexMetaData metaData = IndexMetaData.builder(name).settings(build).build();
        return metaData;
    }
}
