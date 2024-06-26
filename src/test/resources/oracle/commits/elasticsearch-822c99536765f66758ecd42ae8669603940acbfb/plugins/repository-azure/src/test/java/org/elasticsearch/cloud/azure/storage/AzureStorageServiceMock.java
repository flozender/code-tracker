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

package org.elasticsearch.cloud.azure.storage;

import com.microsoft.azure.storage.LocationMode;
import com.microsoft.azure.storage.StorageException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.support.PlainBlobMetaData;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory storage for unit tests
 */
public class AzureStorageServiceMock extends AbstractLifecycleComponent
        implements AzureStorageService {

    protected Map<String, ByteArrayOutputStream> blobs = new ConcurrentHashMap<>();

    @Inject
    public AzureStorageServiceMock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean doesContainerExist(String account, LocationMode mode, String container) {
        return true;
    }

    @Override
    public void removeContainer(String account, LocationMode mode, String container) {
    }

    @Override
    public void createContainer(String account, LocationMode mode, String container) {
    }

    @Override
    public void deleteFiles(String account, LocationMode mode, String container, String path) {
    }

    @Override
    public boolean blobExists(String account, LocationMode mode, String container, String blob) {
        return blobs.containsKey(blob);
    }

    @Override
    public void deleteBlob(String account, LocationMode mode, String container, String blob) {
        blobs.remove(blob);
    }

    @Override
    public InputStream getInputStream(String account, LocationMode mode, String container, String blob) {
        return new ByteArrayInputStream(blobs.get(blob).toByteArray());
    }

    @Override
    public OutputStream getOutputStream(String account, LocationMode mode, String container, String blob) throws URISyntaxException, StorageException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobs.put(blob, outputStream);
        return outputStream;
    }

    @Override
    public Map<String, BlobMetaData> listBlobsByPrefix(String account, LocationMode mode, String container, String keyPath, String prefix) {
        MapBuilder<String, BlobMetaData> blobsBuilder = MapBuilder.newMapBuilder();
        for (String blobName : blobs.keySet()) {
            final String checkBlob;
            if (keyPath != null) {
                // strip off key path from the beginning of the blob name
                checkBlob = blobName.replace(keyPath, "");
            } else {
                checkBlob = blobName;
            }
            if (startsWithIgnoreCase(checkBlob, prefix)) {
                blobsBuilder.put(blobName, new PlainBlobMetaData(checkBlob, blobs.get(blobName).size()));
            }
        }
        return blobsBuilder.immutableMap();
    }

    @Override
    public void moveBlob(String account, LocationMode mode, String container, String sourceBlob, String targetBlob) throws URISyntaxException, StorageException {
        for (String blobName : blobs.keySet()) {
            if (endsWithIgnoreCase(blobName, sourceBlob)) {
                ByteArrayOutputStream outputStream = blobs.get(blobName);
                blobs.put(blobName.replace(sourceBlob, targetBlob), outputStream);
                blobs.remove(blobName);
            }
        }
    }

    @Override
    protected void doStart() throws ElasticsearchException {
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    /**
     * Test if the given String starts with the specified prefix,
     * ignoring upper/lower case.
     *
     * @param str    the String to check
     * @param prefix the prefix to look for
     * @see java.lang.String#startsWith
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        if (str.startsWith(prefix)) {
            return true;
        }
        if (str.length() < prefix.length()) {
            return false;
        }
        String lcStr = str.substring(0, prefix.length()).toLowerCase(Locale.ROOT);
        String lcPrefix = prefix.toLowerCase(Locale.ROOT);
        return lcStr.equals(lcPrefix);
    }

    /**
     * Test if the given String ends with the specified suffix,
     * ignoring upper/lower case.
     *
     * @param str    the String to check
     * @param suffix the suffix to look for
     * @see java.lang.String#startsWith
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        if (str.endsWith(suffix)) {
            return true;
        }
        if (str.length() < suffix.length()) {
            return false;
        }
        String lcStr = str.substring(0, suffix.length()).toLowerCase(Locale.ROOT);
        String lcPrefix = suffix.toLowerCase(Locale.ROOT);
        return lcStr.equals(lcPrefix);
    }
}
