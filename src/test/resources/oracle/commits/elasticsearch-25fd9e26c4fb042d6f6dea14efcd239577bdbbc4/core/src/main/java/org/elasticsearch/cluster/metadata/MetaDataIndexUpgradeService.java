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
package org.elasticsearch.cluster.metadata;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.Version;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.indices.mapper.MapperRegistry;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This service is responsible for upgrading legacy index metadata to the current version
 * <p>
 * Every time an existing index is introduced into cluster this service should be used
 * to upgrade the existing index metadata to the latest version of the cluster. It typically
 * occurs during cluster upgrade, when dangling indices are imported into the cluster or indices
 * are restored from a repository.
 */
public class MetaDataIndexUpgradeService extends AbstractComponent {

    private final MapperRegistry mapperRegistry;
    private final IndexScopedSettings indexScopedSettings;

    @Inject
    public MetaDataIndexUpgradeService(Settings settings, MapperRegistry mapperRegistry, IndexScopedSettings indexScopedSettings) {
        super(settings);
        this.mapperRegistry = mapperRegistry;
        this.indexScopedSettings = indexScopedSettings;
    }

    /**
     * Checks that the index can be upgraded to the current version of the master node.
     *
     * <p>
     * If the index does not need upgrade it returns the index metadata unchanged, otherwise it returns a modified index metadata. If index
     * cannot be updated the method throws an exception.
     */
    public IndexMetaData upgradeIndexMetaData(IndexMetaData indexMetaData) {
        // Throws an exception if there are too-old segments:
        if (isUpgraded(indexMetaData)) {
            assert indexMetaData == archiveBrokenIndexSettings(indexMetaData) : "all settings must have been upgraded before";
            return indexMetaData;
        }
        checkSupportedVersion(indexMetaData);
        IndexMetaData newMetaData = indexMetaData;
        // we have to run this first otherwise in we try to create IndexSettings
        // with broken settings and fail in checkMappingsCompatibility
        newMetaData = archiveBrokenIndexSettings(newMetaData);
        // only run the check with the upgraded settings!!
        checkMappingsCompatibility(newMetaData);
        return markAsUpgraded(newMetaData);
    }


    /**
     * Checks if the index was already opened by this version of Elasticsearch and doesn't require any additional checks.
     */
    boolean isUpgraded(IndexMetaData indexMetaData) {
        return indexMetaData.getUpgradedVersion().onOrAfter(Version.CURRENT);
    }

    /**
     * Elasticsearch 5.0 no longer supports indices with pre Lucene v5.0 (Elasticsearch v2.0.0.beta1) segments. All indices
     * that were created before Elasticsearch v2.0.0.beta1 should be reindexed in Elasticsearch 2.x
     * before they can be opened by this version of elasticsearch.     */
    private void checkSupportedVersion(IndexMetaData indexMetaData) {
        if (indexMetaData.getState() == IndexMetaData.State.OPEN && isSupportedVersion(indexMetaData) == false) {
            throw new IllegalStateException("The index [" + indexMetaData.getIndex() + "] was created before v2.0.0.beta1."
                    + " It should be reindexed in Elasticsearch 2.x before upgrading to " + Version.CURRENT + ".");
        }
    }

    /*
     * Returns true if this index can be supported by the current version of elasticsearch
     */
    private static boolean isSupportedVersion(IndexMetaData indexMetaData) {
        return indexMetaData.getCreationVersion().onOrAfter(Version.V_2_0_0_beta1);
    }

    /**
     * Checks the mappings for compatibility with the current version
     */
    private void checkMappingsCompatibility(IndexMetaData indexMetaData) {
        try {
            // We cannot instantiate real analysis server at this point because the node might not have
            // been started yet. However, we don't really need real analyzers at this stage - so we can fake it
            IndexSettings indexSettings = new IndexSettings(indexMetaData, this.settings);
            SimilarityService similarityService = new SimilarityService(indexSettings, Collections.emptyMap());
            final NamedAnalyzer fakeDefault = new NamedAnalyzer("fake_default", new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName) {
                    throw new UnsupportedOperationException("shouldn't be here");
                }
            });
            // this is just a fake map that always returns the same value for any possible string key
            // also the entrySet impl isn't fully correct but we implement it since internally
            // IndexAnalyzers will iterate over all analyzers to close them.
            final Map<String, NamedAnalyzer> analyzerMap = new AbstractMap<String, NamedAnalyzer>() {
                @Override
                public NamedAnalyzer get(Object key) {
                    assert key instanceof String : "key must be a string but was: " + key.getClass();
                    return new NamedAnalyzer((String)key, fakeDefault.analyzer());
                }

                @Override
                public Set<Entry<String, NamedAnalyzer>> entrySet() {
                    // just to ensure we can iterate over this single analzyer
                    return Collections.singletonMap(fakeDefault.name(), fakeDefault).entrySet();
                }
            };
            try (IndexAnalyzers fakeIndexAnalzyers = new IndexAnalyzers(indexSettings, fakeDefault, fakeDefault, fakeDefault, analyzerMap)) {
                MapperService mapperService = new MapperService(indexSettings, fakeIndexAnalzyers, similarityService, mapperRegistry, () -> null);
                for (ObjectCursor<MappingMetaData> cursor : indexMetaData.getMappings().values()) {
                    MappingMetaData mappingMetaData = cursor.value;
                    mapperService.merge(mappingMetaData.type(), mappingMetaData.source(), MapperService.MergeReason.MAPPING_RECOVERY, false);
                }
            }
        } catch (Exception ex) {
            // Wrap the inner exception so we have the index name in the exception message
            throw new IllegalStateException("unable to upgrade the mappings for the index [" + indexMetaData.getIndex() + "]", ex);
        }
    }

    /**
     * Marks index as upgraded so we don't have to test it again
     */
    private IndexMetaData markAsUpgraded(IndexMetaData indexMetaData) {
        Settings settings = Settings.builder().put(indexMetaData.getSettings()).put(IndexMetaData.SETTING_VERSION_UPGRADED, Version.CURRENT).build();
        return IndexMetaData.builder(indexMetaData).settings(settings).build();
    }

    IndexMetaData archiveBrokenIndexSettings(IndexMetaData indexMetaData) {
        final Settings settings = indexMetaData.getSettings();
        final Settings upgrade = indexScopedSettings.archiveUnknownOrBrokenSettings(settings);
        if (upgrade != settings) {
            return IndexMetaData.builder(indexMetaData).settings(upgrade).build();
        } else {
            return indexMetaData;
        }
    }
}