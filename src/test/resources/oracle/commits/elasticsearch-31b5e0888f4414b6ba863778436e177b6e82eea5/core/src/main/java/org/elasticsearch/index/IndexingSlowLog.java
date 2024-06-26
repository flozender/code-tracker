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

import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.SettingsProperty;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.elasticsearch.index.shard.IndexingOperationListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 */
public final class IndexingSlowLog implements IndexingOperationListener {
    private boolean reformat;
    private long indexWarnThreshold;
    private long indexInfoThreshold;
    private long indexDebugThreshold;
    private long indexTraceThreshold;
    /**
     * How much of the source to log in the slowlog - 0 means log none and
     * anything greater than 0 means log at least that many <em>characters</em>
     * of the source.
     */
    private int maxSourceCharsToLog;

    private SlowLogLevel level;

    private final ESLogger indexLogger;
    private final ESLogger deleteLogger;

    private static final String INDEX_INDEXING_SLOWLOG_PREFIX = "index.indexing.slowlog";
    public static final Setting<TimeValue> INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING =
        Setting.timeSetting(INDEX_INDEXING_SLOWLOG_PREFIX +".threshold.index.warn", TimeValue.timeValueNanos(-1),
            TimeValue.timeValueMillis(-1), true, SettingsProperty.IndexScope);
    public static final Setting<TimeValue> INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING =
        Setting.timeSetting(INDEX_INDEXING_SLOWLOG_PREFIX +".threshold.index.info", TimeValue.timeValueNanos(-1),
            TimeValue.timeValueMillis(-1), true, SettingsProperty.IndexScope);
    public static final Setting<TimeValue> INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING =
        Setting.timeSetting(INDEX_INDEXING_SLOWLOG_PREFIX +".threshold.index.debug", TimeValue.timeValueNanos(-1),
            TimeValue.timeValueMillis(-1), true, SettingsProperty.IndexScope);
    public static final Setting<TimeValue> INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING =
        Setting.timeSetting(INDEX_INDEXING_SLOWLOG_PREFIX +".threshold.index.trace", TimeValue.timeValueNanos(-1),
            TimeValue.timeValueMillis(-1), true, SettingsProperty.IndexScope);
    public static final Setting<Boolean> INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING =
        Setting.boolSetting(INDEX_INDEXING_SLOWLOG_PREFIX +".reformat", true, true, SettingsProperty.IndexScope);
    public static final Setting<SlowLogLevel> INDEX_INDEXING_SLOWLOG_LEVEL_SETTING =
        new Setting<>(INDEX_INDEXING_SLOWLOG_PREFIX +".level", SlowLogLevel.TRACE.name(), SlowLogLevel::parse, true,
            SettingsProperty.IndexScope);
    /**
     * Reads how much of the source to log. The user can specify any value they
     * like and numbers are interpreted the maximum number of characters to log
     * and everything else is interpreted as Elasticsearch interprets booleans
     * which is then converted to 0 for false and Integer.MAX_VALUE for true.
     */
    public static final Setting<Integer> INDEX_INDEXING_SLOWLOG_MAX_SOURCE_CHARS_TO_LOG_SETTING = new Setting<>(INDEX_INDEXING_SLOWLOG_PREFIX + ".source", "1000", (value) -> {
        try {
            return Integer.parseInt(value, 10);
        } catch (NumberFormatException e) {
            return Booleans.parseBoolean(value, true) ? Integer.MAX_VALUE : 0;
        }
    }, true, SettingsProperty.IndexScope);

    IndexingSlowLog(IndexSettings indexSettings) {
        this(indexSettings, Loggers.getLogger(INDEX_INDEXING_SLOWLOG_PREFIX + ".index"),
                Loggers.getLogger(INDEX_INDEXING_SLOWLOG_PREFIX + ".delete"));
    }

    /**
     * Build with the specified loggers. Only used to testing.
     */
    IndexingSlowLog(IndexSettings indexSettings, ESLogger indexLogger, ESLogger deleteLogger) {
        this.indexLogger = indexLogger;
        this.deleteLogger = deleteLogger;

        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING, this::setReformat);
        this.reformat = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_REFORMAT_SETTING);
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING, this::setWarnThreshold);
        this.indexWarnThreshold = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_WARN_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING, this::setInfoThreshold);
        this.indexInfoThreshold = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_INFO_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING, this::setDebugThreshold);
        this.indexDebugThreshold = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_DEBUG_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING, this::setTraceThreshold);
        this.indexTraceThreshold = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_THRESHOLD_INDEX_TRACE_SETTING).nanos();
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_LEVEL_SETTING, this::setLevel);
        setLevel(indexSettings.getValue(INDEX_INDEXING_SLOWLOG_LEVEL_SETTING));
        indexSettings.getScopedSettings().addSettingsUpdateConsumer(INDEX_INDEXING_SLOWLOG_MAX_SOURCE_CHARS_TO_LOG_SETTING, this::setMaxSourceCharsToLog);
        this.maxSourceCharsToLog = indexSettings.getValue(INDEX_INDEXING_SLOWLOG_MAX_SOURCE_CHARS_TO_LOG_SETTING);
    }

    private void setMaxSourceCharsToLog(int maxSourceCharsToLog) {
        this.maxSourceCharsToLog = maxSourceCharsToLog;
    }

    private void setLevel(SlowLogLevel level) {
        this.level = level;
        this.indexLogger.setLevel(level.name());
        this.deleteLogger.setLevel(level.name());
    }

    private void setWarnThreshold(TimeValue warnThreshold) {
        this.indexWarnThreshold = warnThreshold.nanos();
    }

    private void setInfoThreshold(TimeValue infoThreshold) {
        this.indexInfoThreshold = infoThreshold.nanos();
    }

    private void setDebugThreshold(TimeValue debugThreshold) {
        this.indexDebugThreshold = debugThreshold.nanos();
    }

    private void setTraceThreshold(TimeValue traceThreshold) {
        this.indexTraceThreshold = traceThreshold.nanos();
    }

    private void setReformat(boolean reformat) {
        this.reformat = reformat;
    }


    public void postIndex(Engine.Index index) {
        final long took = index.endTime() - index.startTime();
        postIndexing(index.parsedDoc(), took);
    }


    private void postIndexing(ParsedDocument doc, long tookInNanos) {
        if (indexWarnThreshold >= 0 && tookInNanos > indexWarnThreshold) {
            indexLogger.warn("{}", new SlowLogParsedDocumentPrinter(doc, tookInNanos, reformat, maxSourceCharsToLog));
        } else if (indexInfoThreshold >= 0 && tookInNanos > indexInfoThreshold) {
            indexLogger.info("{}", new SlowLogParsedDocumentPrinter(doc, tookInNanos, reformat, maxSourceCharsToLog));
        } else if (indexDebugThreshold >= 0 && tookInNanos > indexDebugThreshold) {
            indexLogger.debug("{}", new SlowLogParsedDocumentPrinter(doc, tookInNanos, reformat, maxSourceCharsToLog));
        } else if (indexTraceThreshold >= 0 && tookInNanos > indexTraceThreshold) {
            indexLogger.trace("{}", new SlowLogParsedDocumentPrinter(doc, tookInNanos, reformat, maxSourceCharsToLog));
        }
    }

    static final class SlowLogParsedDocumentPrinter {
        private final ParsedDocument doc;
        private final long tookInNanos;
        private final boolean reformat;
        private final int maxSourceCharsToLog;

        SlowLogParsedDocumentPrinter(ParsedDocument doc, long tookInNanos, boolean reformat, int maxSourceCharsToLog) {
            this.doc = doc;
            this.tookInNanos = tookInNanos;
            this.reformat = reformat;
            this.maxSourceCharsToLog = maxSourceCharsToLog;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("took[").append(TimeValue.timeValueNanos(tookInNanos)).append("], took_millis[").append(TimeUnit.NANOSECONDS.toMillis(tookInNanos)).append("], ");
            sb.append("type[").append(doc.type()).append("], ");
            sb.append("id[").append(doc.id()).append("], ");
            if (doc.routing() == null) {
                sb.append("routing[] ");
            } else {
                sb.append("routing[").append(doc.routing()).append("] ");
            }

            if (maxSourceCharsToLog == 0 || doc.source() == null || doc.source().length() == 0) {
                return sb.toString();
            }
            try {
                String source = XContentHelper.convertToJson(doc.source(), reformat);
                sb.append(", source[").append(Strings.cleanTruncate(source, maxSourceCharsToLog)).append("]");
            } catch (IOException e) {
                sb.append(", source[_failed_to_convert_]");
            }
            return sb.toString();
        }
    }

    boolean isReformat() {
        return reformat;
    }

    long getIndexWarnThreshold() {
        return indexWarnThreshold;
    }

    long getIndexInfoThreshold() {
        return indexInfoThreshold;
    }

    long getIndexTraceThreshold() {
        return indexTraceThreshold;
    }

    long getIndexDebugThreshold() {
        return indexDebugThreshold;
    }

    int getMaxSourceCharsToLog() {
        return maxSourceCharsToLog;
    }

    SlowLogLevel getLevel() {
        return level;
    }

}
