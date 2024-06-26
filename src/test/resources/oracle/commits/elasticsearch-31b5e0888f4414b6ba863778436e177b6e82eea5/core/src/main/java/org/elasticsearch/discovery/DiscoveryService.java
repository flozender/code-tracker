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

package org.elasticsearch.discovery;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.block.ClusterBlock;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Randomness;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.SettingsProperty;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DiscoveryService extends AbstractLifecycleComponent<DiscoveryService> {

    public static final Setting<TimeValue> INITIAL_STATE_TIMEOUT_SETTING =
        Setting.positiveTimeSetting("discovery.initial_state_timeout", TimeValue.timeValueSeconds(30), false,
            SettingsProperty.ClusterScope);
    public static final Setting<Long> DISCOVERY_SEED_SETTING =
        Setting.longSetting("discovery.id.seed", 0L, Long.MIN_VALUE, false, SettingsProperty.ClusterScope);

    private static class InitialStateListener implements InitialStateDiscoveryListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean initialStateReceived;

        @Override
        public void initialStateProcessed() {
            initialStateReceived = true;
            latch.countDown();
        }

        public boolean waitForInitialState(TimeValue timeValue) throws InterruptedException {
            if (timeValue.millis() > 0) {
                latch.await(timeValue.millis(), TimeUnit.MILLISECONDS);
            }
            return initialStateReceived;
        }
    }

    private final TimeValue initialStateTimeout;
    private final Discovery discovery;
    private InitialStateListener initialStateListener;
    private final DiscoverySettings discoverySettings;

    @Inject
    public DiscoveryService(Settings settings, DiscoverySettings discoverySettings, Discovery discovery) {
        super(settings);
        this.discoverySettings = discoverySettings;
        this.discovery = discovery;
        this.initialStateTimeout = INITIAL_STATE_TIMEOUT_SETTING.get(settings);
    }

    public ClusterBlock getNoMasterBlock() {
        return discoverySettings.getNoMasterBlock();
    }

    @Override
    protected void doStart() {
        initialStateListener = new InitialStateListener();
        discovery.addListener(initialStateListener);
        discovery.start();
        logger.info(discovery.nodeDescription());
    }

    public void waitForInitialState() {
        try {
            if (!initialStateListener.waitForInitialState(initialStateTimeout)) {
                logger.warn("waited for {} and no initial state was set by the discovery", initialStateTimeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ElasticsearchTimeoutException("Interrupted while waiting for initial discovery state");
        }
    }

    @Override
    protected void doStop() {
        if (initialStateListener != null) {
            discovery.removeListener(initialStateListener);
        }
        discovery.stop();
    }

    @Override
    protected void doClose() {
        discovery.close();
    }

    public DiscoveryNode localNode() {
        return discovery.localNode();
    }

    public String nodeDescription() {
        return discovery.nodeDescription();
    }

    /**
     * Publish all the changes to the cluster from the master (can be called just by the master). The publish
     * process should not publish this state to the master as well! (the master is sending it...).
     * <p>
     * The {@link org.elasticsearch.discovery.Discovery.AckListener} allows to acknowledge the publish
     * event based on the response gotten from all nodes
     */
    public void publish(ClusterChangedEvent clusterChangedEvent, Discovery.AckListener ackListener) {
        if (lifecycle.started()) {
            discovery.publish(clusterChangedEvent, ackListener);
        }
    }

    public static String generateNodeId(Settings settings) {
        Random random = Randomness.get(settings, DiscoveryService.DISCOVERY_SEED_SETTING);
        return Strings.randomBase64UUID(random);
    }
}
