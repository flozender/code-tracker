package org.elasticsearch.index.fielddata;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.settings.IndexSettings;

import java.io.IOException;

/**
 */
public abstract class AbstractIndexFieldData<FD extends AtomicFieldData> extends AbstractIndexComponent implements IndexFieldData<FD> {

    private final FieldMapper.Names fieldNames;
    protected final FieldDataType fieldDataType;
    protected final IndexFieldDataCache cache;

    public AbstractIndexFieldData(Index index, @IndexSettings Settings indexSettings, FieldMapper.Names fieldNames, FieldDataType fieldDataType, IndexFieldDataCache cache) {
        super(index, indexSettings);
        this.fieldNames = fieldNames;
        this.fieldDataType = fieldDataType;
        this.cache = cache;
    }

    @Override
    public FieldMapper.Names getFieldNames() {
        return this.fieldNames;
    }

    @Override
    public void clear() {
        cache.clear(fieldNames.indexName());
    }

    @Override
    public void clear(IndexReader reader) {
        cache.clear(reader);
    }

    @Override
    public final FD load(AtomicReaderContext context) {
        try {
            FD fd = cache.load(context, this);
            return fd;
        } catch (Throwable e) {
            if (e instanceof ElasticsearchException) {
                throw (ElasticsearchException) e;
            } else {
                throw new ElasticsearchException(e.getMessage(), e);
            }
        }
    }

    /**
     * A {@code PerValueEstimator} is a sub-class that can be used to estimate
     * the memory overhead for loading the data. Each field data
     * implementation should implement its own {@code PerValueEstimator} if it
     * intends to take advantage of the MemoryCircuitBreaker.
     * <p/>
     * Note that the .beforeLoad(...) and .afterLoad(...) methods must be
     * manually called.
     */
    public interface PerValueEstimator {

        /**
         * @return the number of bytes for the given term
         */
        public long bytesPerValue(BytesRef term);

        /**
         * Execute any pre-loading estimations for the terms. May also
         * optionally wrap a {@link TermsEnum} in a
         * {@link RamAccountingTermsEnum}
         * which will estimate the memory on a per-term basis.
         *
         * @param terms terms to be estimated
         * @return A TermsEnum for the given terms
         * @throws IOException
         */
        public TermsEnum beforeLoad(Terms terms) throws IOException;

        /**
         * Possibly adjust a circuit breaker after field data has been loaded,
         * now that the actual amount of memory used by the field data is known
         *
         * @param termsEnum  terms that were loaded
         * @param actualUsed actual field data memory usage
         */
        public void afterLoad(TermsEnum termsEnum, long actualUsed);
    }
}
