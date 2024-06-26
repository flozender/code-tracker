package org.apache.solr.cloud;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public abstract class ElectionContext {
  
  final String electionPath;
  final ZkNodeProps leaderProps;
  final String id;
  final String leaderPath;
  String leaderSeqPath;
  private SolrZkClient zkClient;
  
  public ElectionContext(final String shardZkNodeName,
      final String electionPath, final String leaderPath, final ZkNodeProps leaderProps, final SolrZkClient zkClient) {
    this.id = shardZkNodeName;
    this.electionPath = electionPath;
    this.leaderPath = leaderPath;
    this.leaderProps = leaderProps;
    this.zkClient = zkClient;
  }
  
  public void cancelElection() throws InterruptedException, KeeperException {
    zkClient.delete(leaderSeqPath, -1, true);
  }
  // the given core may or may not be null - if you need access to the current core, you must pass
  // the core container and core name to your context impl - then use this core ref if it is not null
  // else access it from the core container
  abstract void runLeaderProcess(boolean weAreReplacement) throws KeeperException, InterruptedException, IOException;
}

class ShardLeaderElectionContextBase extends ElectionContext {
  private static Logger log = LoggerFactory.getLogger(ShardLeaderElectionContextBase.class);
  protected final SolrZkClient zkClient;
  protected String shardId;
  protected String collection;
  protected LeaderElector leaderElector;

  public ShardLeaderElectionContextBase(LeaderElector leaderElector, final String shardId,
      final String collection, final String shardZkNodeName, ZkNodeProps props, ZkStateReader zkStateReader) {
    super(shardZkNodeName, ZkStateReader.COLLECTIONS_ZKNODE + "/" + collection + "/leader_elect/"
        + shardId, ZkStateReader.getShardLeadersPath(collection, shardId),
        props, zkStateReader.getZkClient());
    this.leaderElector = leaderElector;
    this.zkClient = zkStateReader.getZkClient();
    this.shardId = shardId;
    this.collection = collection;
  }

  @Override
  void runLeaderProcess(boolean weAreReplacement)
      throws KeeperException, InterruptedException, IOException {

    try {
      zkClient.makePath(leaderPath,
          leaderProps == null ? null : ZkStateReader.toJSON(leaderProps),
          CreateMode.EPHEMERAL, true);
    } catch (NodeExistsException e) {
      // if a previous leader ephemeral still exists for some reason, try and
      // remove it
      zkClient.delete(leaderPath, -1, true);
      zkClient.makePath(leaderPath,
          leaderProps == null ? null : ZkStateReader.toJSON(leaderProps),
          CreateMode.EPHEMERAL, true);
    }
    
    // TODO: above we make it looks like leaderProps could be true, but here
    // you would get an NPE if it was.
    ZkNodeProps m = new ZkNodeProps(Overseer.QUEUE_OPERATION,
        "leader", ZkStateReader.SHARD_ID_PROP, shardId,
        ZkStateReader.COLLECTION_PROP, collection, ZkStateReader.BASE_URL_PROP,
        leaderProps.getProperties().get(ZkStateReader.BASE_URL_PROP),
        ZkStateReader.CORE_NAME_PROP, leaderProps.getProperties().get(ZkStateReader.CORE_NAME_PROP));
    Overseer.getInQueue(zkClient).offer(ZkStateReader.toJSON(m));
  } 

}

// add core container and stop passing core around...
final class ShardLeaderElectionContext extends ShardLeaderElectionContextBase {
  private static Logger log = LoggerFactory.getLogger(ShardLeaderElectionContext.class);
  
  private ZkController zkController;
  private CoreContainer cc;
  private SyncStrategy syncStrategy = new SyncStrategy();
  
  public ShardLeaderElectionContext(LeaderElector leaderElector, 
      final String shardId, final String collection,
      final String shardZkNodeName, ZkNodeProps props, ZkController zkController, CoreContainer cc) {
    super(leaderElector, shardId, collection, shardZkNodeName, props,
        zkController.getZkStateReader());
    this.zkController = zkController;
    this.cc = cc;
  }
  
  @Override
  void runLeaderProcess(boolean weAreReplacement)
      throws KeeperException, InterruptedException, IOException {
    if (cc != null) {
      String coreName = leaderProps.get(ZkStateReader.CORE_NAME_PROP);
      SolrCore core = null;
      try {
     
        core = cc.getCore(coreName);

        if (core == null) {
          cancelElection();
          throw new SolrException(ErrorCode.SERVER_ERROR, "Fatal Error, SolrCore not found:" + coreName + " in " + cc.getCoreNames());
        }
        // should I be leader?
        if (weAreReplacement && !shouldIBeLeader(leaderProps)) {
          // System.out.println("there is a better leader candidate it appears");
          rejoinLeaderElection(leaderSeqPath, core);
          return;
        }

        if (weAreReplacement) {
          if (zkClient.exists(leaderPath, true)) {
            zkClient.delete(leaderPath, -1, true);
          }
          log.info("I may be the new leader - try and sync");
          // we are going to attempt to be the leader
          // first cancel any current recovery
          core.getUpdateHandler().getSolrCoreState().cancelRecovery();
          boolean success = syncStrategy.sync(zkController, core, leaderProps);
          if (!success && anyoneElseActive()) {
            rejoinLeaderElection(leaderSeqPath, core);
            return;
          } 
        }
        log.info("I am the new leader: " + ZkCoreNodeProps.getCoreUrl(leaderProps));
        
        // If I am going to be the leader I have to be active
        core.getUpdateHandler().getSolrCoreState().cancelRecovery();
        zkController.publish(core.getCoreDescriptor(), ZkStateReader.ACTIVE);
        
      } finally {
        if (core != null ) {
          core.close();
        }
      }
      
    }
    
    super.runLeaderProcess(weAreReplacement);
  }

  private void rejoinLeaderElection(String leaderSeqPath, SolrCore core)
      throws InterruptedException, KeeperException, IOException {
    // remove our ephemeral and re join the election
    // System.out.println("sync failed, delete our election node:"
    // + leaderSeqPath);
    log.info("There is a better leader candidate than us - going back into recovery");
    
    zkController.publish(core.getCoreDescriptor(), ZkStateReader.DOWN);
    
    cancelElection();
    
    core.getUpdateHandler().getSolrCoreState().doRecovery(cc, core.getName());
    
    leaderElector.joinElection(this);
  }
  
  private boolean shouldIBeLeader(ZkNodeProps leaderProps) {
    ClusterState clusterState = zkController.getZkStateReader().getClusterState();
    Map<String,Slice> slices = clusterState.getSlices(this.collection);
    Slice slice = slices.get(shardId);
    Map<String,ZkNodeProps> shards = slice.getShards();
    boolean foundSomeoneElseActive = false;
    for (Map.Entry<String,ZkNodeProps> shard : shards.entrySet()) {
      String state = shard.getValue().get(ZkStateReader.STATE_PROP);

      if (new ZkCoreNodeProps(shard.getValue()).getCoreUrl().equals(
              new ZkCoreNodeProps(leaderProps).getCoreUrl())) {
        if (state.equals(ZkStateReader.ACTIVE)
          && clusterState.liveNodesContain(shard.getValue().get(
              ZkStateReader.NODE_NAME_PROP))) {
          // we are alive
          return true;
        }
      }
      
      if ((state.equals(ZkStateReader.ACTIVE))
          && clusterState.liveNodesContain(shard.getValue().get(
              ZkStateReader.NODE_NAME_PROP))
          && !new ZkCoreNodeProps(shard.getValue()).getCoreUrl().equals(
              new ZkCoreNodeProps(leaderProps).getCoreUrl())) {
        foundSomeoneElseActive = true;
      }
    }
    
    return !foundSomeoneElseActive;
  }
  
  private boolean anyoneElseActive() {
    ClusterState clusterState = zkController.getZkStateReader().getClusterState();
    Map<String,Slice> slices = clusterState.getSlices(this.collection);
    Slice slice = slices.get(shardId);
    Map<String,ZkNodeProps> shards = slice.getShards();

    for (Map.Entry<String,ZkNodeProps> shard : shards.entrySet()) {
      String state = shard.getValue().get(ZkStateReader.STATE_PROP);

      
      if ((state.equals(ZkStateReader.ACTIVE))
          && clusterState.liveNodesContain(shard.getValue().get(
              ZkStateReader.NODE_NAME_PROP))) {
        return true;
      }
    }
    
    return false;
  }
  
}

final class OverseerElectionContext extends ElectionContext {
  
  private final SolrZkClient zkClient;
  private Overseer overseer;


  public OverseerElectionContext(SolrZkClient zkClient, Overseer overseer, final String zkNodeName) {
    super(zkNodeName, "/overseer_elect", "/overseer_elect/leader", null, zkClient);
    this.overseer = overseer;
    this.zkClient = zkClient;
  }

  @Override
  void runLeaderProcess(boolean weAreReplacement) throws KeeperException, InterruptedException {
    
    final String id = leaderSeqPath.substring(leaderSeqPath.lastIndexOf("/")+1);
    ZkNodeProps myProps = new ZkNodeProps("id", id);

    try {
      zkClient.makePath(leaderPath,
          ZkStateReader.toJSON(myProps),
          CreateMode.EPHEMERAL, true);
    } catch (NodeExistsException e) {
      // if a previous leader ephemeral still exists for some reason, try and
      // remove it
      zkClient.delete(leaderPath, -1, true);
      zkClient.makePath(leaderPath,
          ZkStateReader.toJSON(myProps),
          CreateMode.EPHEMERAL, true);
    }
  
    overseer.start(id);
  }
  
}
