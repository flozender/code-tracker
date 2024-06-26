/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.cache.id.simple;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.UTF8SortedAsUnicodeComparator;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.cache.id.IdCache;
import org.elasticsearch.index.cache.id.IdReaderCache;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentTypeListener;
import org.elasticsearch.index.mapper.Uid;
import org.elasticsearch.index.mapper.internal.ParentFieldMapper;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.ShardUtils;
import org.elasticsearch.index.shard.service.IndexShard;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class SimpleIdCache extends AbstractIndexComponent implements IdCache, SegmentReader.CoreClosedListener, DocumentTypeListener {

    private final boolean reuse;
    private final ConcurrentMap<Object, SimpleIdReaderCache> idReaders;
    private final NavigableSet<HashedBytesArray> parentTypes;

    IndexService indexService;

    @Inject
    public SimpleIdCache(Index index, @IndexSettings Settings indexSettings) {
        super(index, indexSettings);
        reuse = componentSettings.getAsBoolean("reuse", false);
        idReaders = ConcurrentCollections.newConcurrentMap();
        parentTypes = new TreeSet<HashedBytesArray>(UTF8SortedAsUnicodeComparator.utf8SortedAsUnicodeSortOrder);
    }

    @Override
    public void setIndexService(IndexService indexService) {
        this.indexService = indexService;
        indexService.mapperService().addTypeListener(this);
    }

    @Override
    public void close() throws ElasticsearchException {
        indexService.mapperService().removeTypeListener(this);
        clear();
    }

    @Override
    public void clear() {
        // Make a copy of the live id readers...
        Map<Object, SimpleIdReaderCache> copy = new HashMap<Object, SimpleIdReaderCache>(idReaders);
        for (Map.Entry<Object, SimpleIdReaderCache> entry : copy.entrySet()) {
            SimpleIdReaderCache removed = idReaders.remove(entry.getKey());
            // ... and only if the id reader still exists in live readers we decrement stats,
            // this will prevent double onRemoval calls
            if (removed != null) {
                onRemoval(removed);
            }
        }
    }

    @Override
    public void onClose(Object coreCacheKey) {
        clear(coreCacheKey);
    }

    @Override
    public void clear(Object coreCacheKey) {
        SimpleIdReaderCache removed = idReaders.remove(coreCacheKey);
        if (removed != null) onRemoval(removed);
    }

    @Override
    public IdReaderCache reader(AtomicReader reader) {
        return idReaders.get(reader.getCoreCacheKey());
    }

    @SuppressWarnings({"StringEquality"})
    @Override
    public void refresh(List<AtomicReaderContext> atomicReaderContexts) throws IOException {
        // do a quick check for the common case, that all are there
        if (refreshNeeded(atomicReaderContexts)) {
            synchronized (idReaders) {
                if (!refreshNeeded(atomicReaderContexts)) {
                    return;
                }

                // do the refresh
                Map<Object, Map<String, TypeBuilder>> builders = new HashMap<Object, Map<String, TypeBuilder>>();
                Map<Object, IndexReader> cacheToReader = new HashMap<Object, IndexReader>();

                // first, go over and load all the id->doc map for all types
                for (AtomicReaderContext context : atomicReaderContexts) {
                    AtomicReader reader = context.reader();
                    if (!refreshNeeded(context)) {
                        // no need, continue
                        continue;
                    }

                    if (reader instanceof SegmentReader) {
                        ((SegmentReader) reader).addCoreClosedListener(this);
                    }
                    Map<String, TypeBuilder> readerBuilder = new HashMap<String, TypeBuilder>();
                    builders.put(reader.getCoreCacheKey(), readerBuilder);
                    cacheToReader.put(reader.getCoreCacheKey(), context.reader());


                    Terms terms = reader.terms(UidFieldMapper.NAME);
                    if (terms != null) {
                        TermsEnum termsEnum = terms.iterator(null);
                        DocsEnum docsEnum = null;
                        uid: for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {
                            HashedBytesArray[] typeAndId = Uid.splitUidIntoTypeAndId(term);
                            // We don't want to load uid of child documents, this allows us to not load uids of child types.
                            if (!parentTypes.contains(typeAndId[0])) {
                                do {
                                    HashedBytesArray nextParent = parentTypes.ceiling(typeAndId[0]);
                                    if (nextParent == null) {
                                        break uid;
                                    }

                                    TermsEnum.SeekStatus status = termsEnum.seekCeil(nextParent.toBytesRef());
                                    if (status == TermsEnum.SeekStatus.END) {
                                        break uid;
                                    } else if (status == TermsEnum.SeekStatus.NOT_FOUND) {
                                        term = termsEnum.term();
                                        typeAndId = Uid.splitUidIntoTypeAndId(term);
                                    } else if (status == TermsEnum.SeekStatus.FOUND) {
                                        assert false : "Seek status should never be FOUND, because we seek only the type part";
                                        term = termsEnum.term();
                                        typeAndId = Uid.splitUidIntoTypeAndId(term);
                                    }
                                } while (!parentTypes.contains(typeAndId[0]));
                            }

                            String type = typeAndId[0].toUtf8();
                            TypeBuilder typeBuilder = readerBuilder.get(type);
                            if (typeBuilder == null) {
                                typeBuilder = new TypeBuilder(reader);
                                readerBuilder.put(type, typeBuilder);
                            }

                            HashedBytesArray idAsBytes = checkIfCanReuse(builders, typeAndId[1]);
                            docsEnum = termsEnum.docs(null, docsEnum, 0);
                            for (int docId = docsEnum.nextDoc(); docId != DocsEnum.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                                typeBuilder.idToDoc.put(idAsBytes, docId);
                                typeBuilder.docToId[docId] = idAsBytes;
                            }
                        }
                    }
                }

                // now, go and load the docId->parentId map
                for (AtomicReaderContext context : atomicReaderContexts) {
                    AtomicReader reader = context.reader();
                    if (!refreshNeeded(context)) {
                        // no need, continue
                        continue;
                    }

                    Map<String, TypeBuilder> readerBuilder = builders.get(reader.getCoreCacheKey());

                    Terms terms = reader.terms(ParentFieldMapper.NAME);
                    if (terms != null) {
                        TermsEnum termsEnum = terms.iterator(null);
                        DocsEnum docsEnum = null;
                        for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {
                            HashedBytesArray[] typeAndId = Uid.splitUidIntoTypeAndId(term);

                            TypeBuilder typeBuilder = readerBuilder.get(typeAndId[0].toUtf8());
                            if (typeBuilder == null) {
                                typeBuilder = new TypeBuilder(reader);
                                readerBuilder.put(typeAndId[0].toUtf8(), typeBuilder);
                            }

                            HashedBytesArray idAsBytes = checkIfCanReuse(builders, typeAndId[1]);
                            boolean added = false; // optimize for when all the docs are deleted for this id

                            docsEnum = termsEnum.docs(null, docsEnum, 0);
                            for (int docId = docsEnum.nextDoc(); docId != DocsEnum.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                                if (!added) {
                                    typeBuilder.parentIdsValues.add(idAsBytes);
                                    added = true;
                                }
                                typeBuilder.parentIdsOrdinals[docId] = typeBuilder.t;
                            }

                            if (added) {
                                typeBuilder.t++;
                            }
                        }
                    }
                }


                // now, build it back
                for (Map.Entry<Object, Map<String, TypeBuilder>> entry : builders.entrySet()) {
                    Object readerKey = entry.getKey();
                    MapBuilder<String, SimpleIdReaderTypeCache> types = MapBuilder.newMapBuilder();
                    for (Map.Entry<String, TypeBuilder> typeBuilderEntry : entry.getValue().entrySet()) {
                        types.put(typeBuilderEntry.getKey(), new SimpleIdReaderTypeCache(typeBuilderEntry.getKey(),
                                typeBuilderEntry.getValue().idToDoc,
                                typeBuilderEntry.getValue().docToId,
                                typeBuilderEntry.getValue().parentIdsValues.toArray(new HashedBytesArray[typeBuilderEntry.getValue().parentIdsValues.size()]),
                                typeBuilderEntry.getValue().parentIdsOrdinals));
                    }
                    IndexReader indexReader = cacheToReader.get(readerKey);
                    SimpleIdReaderCache readerCache = new SimpleIdReaderCache(types.immutableMap(), ShardUtils.extractShardId(indexReader));
                    idReaders.put(readerKey, readerCache);
                    onCached(readerCache);
                }
            }
        }
    }

    void onCached(SimpleIdReaderCache readerCache) {
        if (readerCache.shardId != null) {
            IndexShard shard = indexService.shard(readerCache.shardId.id());
            if (shard != null) {
                shard.idCache().onCached(readerCache.sizeInBytes());
            }
        }
    }

    void onRemoval(SimpleIdReaderCache readerCache) {
        if (readerCache.shardId != null) {
            IndexShard shard = indexService.shard(readerCache.shardId.id());
            if (shard != null) {
                shard.idCache().onRemoval(readerCache.sizeInBytes());
            }
        }
    }

    private HashedBytesArray checkIfCanReuse(Map<Object, Map<String, TypeBuilder>> builders, HashedBytesArray idAsBytes) {
        HashedBytesArray finalIdAsBytes;
        // go over and see if we can reuse this id
        if (reuse) {
            for (SimpleIdReaderCache idReaderCache : idReaders.values()) {
                finalIdAsBytes = idReaderCache.canReuse(idAsBytes);
                if (finalIdAsBytes != null) {
                    return finalIdAsBytes;
                }
            }
        }
        // even if we don't enable reuse, at least check on the current "live" builders that we are handling
        for (Map<String, TypeBuilder> map : builders.values()) {
            for (TypeBuilder typeBuilder : map.values()) {
                finalIdAsBytes = typeBuilder.canReuse(idAsBytes);
                if (finalIdAsBytes != null) {
                    return finalIdAsBytes;
                }
            }
        }
        return idAsBytes;
    }

    private boolean refreshNeeded(List<AtomicReaderContext> atomicReaderContexts) {
        for (AtomicReaderContext atomicReaderContext : atomicReaderContexts) {
            if (refreshNeeded(atomicReaderContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean refreshNeeded(AtomicReaderContext atomicReaderContext) {
        return !idReaders.containsKey(atomicReaderContext.reader().getCoreCacheKey());
    }

    @Override
    public void beforeCreate(DocumentMapper mapper) {
        synchronized (idReaders) {
            ParentFieldMapper parentFieldMapper = mapper.parentFieldMapper();
            if (parentFieldMapper.active()) {
                // A _parent field can never be added to an existing mapping, so a _parent field either exists on
                // a new created or doesn't exists. This is why we can update the known parent types via DocumentTypeListener
                if (parentTypes.add(new HashedBytesArray(Strings.toUTF8Bytes(parentFieldMapper.type(), new BytesRef())))) {
                    clear();
                }
            }
        }
    }

    @Override
    public void afterRemove(DocumentMapper mapper) {
        synchronized (idReaders) {
            ParentFieldMapper parentFieldMapper = mapper.parentFieldMapper();
            if (parentFieldMapper.active()) {
                parentTypes.remove(new HashedBytesArray(Strings.toUTF8Bytes(parentFieldMapper.type(), new BytesRef())));
            }
        }
    }

    static class TypeBuilder {
        final ObjectIntOpenHashMap<HashedBytesArray> idToDoc = new ObjectIntOpenHashMap<HashedBytesArray>();
        final HashedBytesArray[] docToId;
        final ArrayList<HashedBytesArray> parentIdsValues = new ArrayList<HashedBytesArray>();
        final int[] parentIdsOrdinals;
        int t = 1;  // current term number (0 indicated null value)

        TypeBuilder(IndexReader reader) {
            parentIdsOrdinals = new int[reader.maxDoc()];
            // the first one indicates null value
            parentIdsValues.add(null);
            docToId = new HashedBytesArray[reader.maxDoc()];
        }

        /**
         * Returns an already stored instance if exists, if not, returns null;
         */
        public HashedBytesArray canReuse(HashedBytesArray id) {
            if (idToDoc.containsKey(id)) {
                // we can use #lkey() since this is called from a synchronized block
                return idToDoc.lkey();
            } else {
                return id;
            }
        }
    }
}
