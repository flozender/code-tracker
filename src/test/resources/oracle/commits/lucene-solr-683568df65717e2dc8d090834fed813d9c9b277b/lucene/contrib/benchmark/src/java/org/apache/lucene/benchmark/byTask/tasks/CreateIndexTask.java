package org.apache.lucene.benchmark.byTask.tasks;

/**
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

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.NoDeletionPolicy;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.NoMergeScheduler;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.util.Version;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Create an index. <br>
 * Other side effects: index writer object in perfRunData is set. <br>
 * Relevant properties: <code>merge.factor (default 10),
 * max.buffered (default no flush), max.field.length (default
 * 10,000 tokens), max.field.length, compound (default true), ram.flush.mb [default 0],
 * merge.policy (default org.apache.lucene.index.LogByteSizeMergePolicy),
 * merge.scheduler (default
 * org.apache.lucene.index.ConcurrentMergeScheduler),
 * concurrent.merge.scheduler.max.thread.count and
 * concurrent.merge.scheduler.max.merge.count (defaults per
 * ConcurrentMergeScheduler) </code>.
 * <p>
 * This task also supports a "writer.info.stream" property with the following
 * values:
 * <ul>
 * <li>SystemOut - sets {@link IndexWriter#setInfoStream(java.io.PrintStream)}
 * to {@link System#out}.
 * <li>SystemErr - sets {@link IndexWriter#setInfoStream(java.io.PrintStream)}
 * to {@link System#err}.
 * <li>&lt;file_name&gt; - attempts to create a file given that name and sets
 * {@link IndexWriter#setInfoStream(java.io.PrintStream)} to that file. If this
 * denotes an invalid file name, or some error occurs, an exception will be
 * thrown.
 * </ul>
 */
public class CreateIndexTask extends PerfTask {

  public CreateIndexTask(PerfRunData runData) {
    super(runData);
  }

  public static void setIndexWriterConfig(IndexWriter writer, Config config) throws IOException {

    final String mergeScheduler = config.get("merge.scheduler",
                                             "org.apache.lucene.index.ConcurrentMergeScheduler");
    if (mergeScheduler.equals(NoMergeScheduler.class.getName())) {
      writer.getConfig().setMergeScheduler(NoMergeScheduler.INSTANCE);
    } else {
      try {
        writer.getConfig().setMergeScheduler(Class.forName(mergeScheduler).asSubclass(MergeScheduler.class).newInstance());
      } catch (Exception e) {
        throw new RuntimeException("unable to instantiate class '" + mergeScheduler + "' as merge scheduler", e);
      }
      
      if (mergeScheduler.equals("org.apache.lucene.index.ConcurrentMergeScheduler")) {
        ConcurrentMergeScheduler cms = (ConcurrentMergeScheduler) writer.getConfig().getMergeScheduler();
        int v = config.get("concurrent.merge.scheduler.max.thread.count", -1);
        if (v != -1) {
          cms.setMaxThreadCount(v);
        }
        v = config.get("concurrent.merge.scheduler.max.merge.count", -1);
        if (v != -1) {
          cms.setMaxMergeCount(v);
        }
      }
    }

    final String mergePolicy = config.get("merge.policy",
                                          "org.apache.lucene.index.LogByteSizeMergePolicy");
    boolean isCompound = config.get("compound", true);
    if (mergePolicy.equals(NoMergePolicy.class.getName())) {
      writer.getConfig().setMergePolicy(isCompound ? NoMergePolicy.COMPOUND_FILES : NoMergePolicy.NO_COMPOUND_FILES);
    } else {
      try {
        writer.getConfig().setMergePolicy(Class.forName(mergePolicy).asSubclass(MergePolicy.class).newInstance());
      } catch (Exception e) {
        throw new RuntimeException("unable to instantiate class '" + mergePolicy + "' as merge policy", e);
      }
      if (writer.getConfig().getMergePolicy() instanceof LogMergePolicy) {
        ((LogMergePolicy) writer.getConfig().getMergePolicy()).setUseCompoundFile(isCompound);
        ((LogMergePolicy) writer.getConfig().getMergePolicy()).setMergeFactor(config.get("merge.factor",OpenIndexTask.DEFAULT_MERGE_PFACTOR));
      }
    }
    writer.getConfig().setMaxFieldLength(config.get("max.field.length",OpenIndexTask.DEFAULT_MAX_FIELD_LENGTH));

    final double ramBuffer = config.get("ram.flush.mb",OpenIndexTask.DEFAULT_RAM_FLUSH_MB);
    final int maxBuffered = config.get("max.buffered",OpenIndexTask.DEFAULT_MAX_BUFFERED);
    if (maxBuffered == IndexWriterConfig.DISABLE_AUTO_FLUSH) {
      writer.setRAMBufferSizeMB(ramBuffer);
      writer.setMaxBufferedDocs(maxBuffered);
    } else {
      writer.setMaxBufferedDocs(maxBuffered);
      writer.setRAMBufferSizeMB(ramBuffer);
    }
    
    String infoStreamVal = config.get("writer.info.stream", null);
    if (infoStreamVal != null) {
      if (infoStreamVal.equals("SystemOut")) {
        writer.setInfoStream(System.out);
      } else if (infoStreamVal.equals("SystemErr")) {
        writer.setInfoStream(System.err);
      } else {
        File f = new File(infoStreamVal).getAbsoluteFile();
        writer.setInfoStream(new PrintStream(new BufferedOutputStream(new FileOutputStream(f))));
      }
    }
  }
  
  public static IndexDeletionPolicy getIndexDeletionPolicy(Config config) {
    String deletionPolicyName = config.get("deletion.policy", "org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy");
    if (deletionPolicyName.equals(NoDeletionPolicy.class.getName())) {
      return NoDeletionPolicy.INSTANCE;
    } else {
      try {
        return Class.forName(deletionPolicyName).asSubclass(IndexDeletionPolicy.class).newInstance();
      } catch (Exception e) {
        throw new RuntimeException("unable to instantiate class '" + deletionPolicyName + "' as IndexDeletionPolicy", e);
      }
    }
  }
  
  @Override
  public int doLogic() throws IOException {
    PerfRunData runData = getRunData();
    Config config = runData.getConfig();
    
    IndexWriter writer = new IndexWriter(runData.getDirectory(),
        new IndexWriterConfig(Version.LUCENE_31, runData.getAnalyzer())
            .setOpenMode(OpenMode.CREATE).setIndexDeletionPolicy(
                getIndexDeletionPolicy(config)));
    setIndexWriterConfig(writer, config);
    runData.setIndexWriter(writer);
    return 1;
  }
}
