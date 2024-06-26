package org.apache.lucene.index;

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

import java.util.List;
import java.io.IOException;

/**
 * <p>Expert: implement this interface, and pass it to one
 * of the {@link IndexWriter} or {@link IndexReader}
 * constructors, to customize when "point in time" commits
 * are deleted from an index.  The default deletion policy
 * is {@link KeepOnlyLastCommitDeletionPolicy}, which always
 * removes old commits as soon as a new commit is done (this
 * matches the behavior before 2.2).</p>
 *
 * <p>One expected use case for this (and the reason why it
 * was first created) is to work around problems with an
 * index directory accessed via filesystems like NFS because
 * NFS does not provide the "delete on last close" semantics
 * that Lucene's "point in time" search normally relies on.
 * By implementing a custom deletion policy, such as "a
 * commit is only removed once it has been stale for more
 * than X minutes", you can give your readers time to
 * refresh to the new commit before {@link IndexWriter}
 * removes the old commits.  Note that doing so will
 * increase the storage requirements of the index.  See <a
 * target="top"
 * href="http://issues.apache.org/jira/browse/LUCENE-710">LUCENE-710</a>
 * for details.</p>
 */

public interface IndexDeletionPolicy {

  /**
   * <p>This is called once when a writer is first
   * instantiated to give the policy a chance to remove old
   * commit points.</p>
   * 
   * <p>The writer locates all commits present in the index
   * and calls this method.  The policy may choose to delete
   * commit points.  To delete a commit point, call the
   * {@link IndexCommitPoint#delete} method.</p>
   *
   * @param commits List of {@link IndexCommitPoint},
   *  sorted by age (the 0th one is the oldest commit).
   */
  public void onInit(List commits) throws IOException;

  /**
   * <p>This is called each time the writer commits.  This
   * gives the policy a chance to remove old commit points
   * with each commit.</p>
   *
   * <p>If writer has <code>autoCommit = true</code> then
   * this method will in general be called many times during
   * one instance of {@link IndexWriter}.  If
   * <code>autoCommit = false</code> then this method is
   * only called once when {@link IndexWriter#close} is
   * called, or not at all if the {@link IndexWriter#abort}
   * is called.  The policy may now choose to delete old
   * commit points by calling {@link IndexCommitPoint#delete}.
   *
   * @param commits List of {@link IndexCommitPoint}>,
   *  sorted by age (the 0th one is the oldest commit).
   */
  public void onCommit(List commits) throws IOException;
}
