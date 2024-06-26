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

import java.text.NumberFormat;

import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.stats.Points;
import org.apache.lucene.benchmark.byTask.stats.TaskStats;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.byTask.utils.Format;

/**
 * An abstract task to be tested for performance. <br>
 * Every performance task extends this class, and provides its own
 * {@link #doLogic()} method, which performss the actual task. <br>
 * Tasks performing some work that should be measured for the task, can overide
 * {@link #setup()} and/or {@link #tearDown()} and place that work there. <br>
 * Relevant properties: <code>task.max.depth.log</code>.<br>
 * Also supports the following logging attributes:
 * <ul>
 * <li>log.step - specifies how often to log messages about the current running
 * task. Default is 1000 {@link #doLogic()} invocations. Set to -1 to disable
 * logging.
 * <li>log.step.[class Task Name] - specifies the same as 'log.step', only for a
 * particular task name. For example, log.step.AddDoc will be applied only for
 * {@link AddDocTask}, but not for {@link DeleteDocTask}. It's a way to control
 * per task logging settings. If you want to ommit logging for any other task,
 * include log.step=-1. The syntax is "log.step." together with the Task's
 * 'short' name (i.e., without the 'Task' part).
 * </ul>
 */
public abstract class PerfTask implements Cloneable {

  static final int DEFAULT_LOG_STEP = 1000;
  
  private PerfRunData runData;
  
  // propeties that all tasks have
  private String name;
  private int depth = 0;
  protected int logStep;
  private int logStepCount = 0;
  private int maxDepthLogStart = 0;
  private boolean disableCounting = false;
  protected String params = null;
  
  protected static final String NEW_LINE = System.getProperty("line.separator");

  /** Should not be used externally */
  private PerfTask() {
    name = Format.simpleName(getClass());
    if (name.endsWith("Task")) {
      name = name.substring(0, name.length() - 4);
    }
  }

  /**
   * @deprecated will be removed in 3.0. checks if there are any obsolete
   *             settings, like doc.add.log.step and doc.delete.log.step and
   *             alerts the user.
   */
  private void checkObsoleteSettings(Config config) {
    if (config.get("doc.add.log.step", null) != null) {
      throw new RuntimeException("doc.add.log.step is not supported anymore. " +
      		"Use log.step.AddDoc and refer to CHANGES to read on the recent " +
      		"API changes done to Benchmark's DocMaker and Task-based logging.");
    }
    
    if (config.get("doc.delete.log.step", null) != null) {
      throw new RuntimeException("doc.delete.log.step is not supported anymore. " +
          "Use log.step.DeleteDoc and refer to CHANGES to read on the recent " +
          "API changes done to Benchmark's DocMaker and Task-based logging.");
    }
  }
  
  public PerfTask(PerfRunData runData) {
    this();
    this.runData = runData;
    Config config = runData.getConfig();
    this.maxDepthLogStart = config.get("task.max.depth.log",0);

    String logStepAtt = "log.step";
    // TODO (1.5): call getClass().getSimpleName() instead.
    String taskName = getClass().getName();
    int idx = taskName.lastIndexOf('.');
    // To support test internal classes. when we move to getSimpleName, this can be removed.
    int idx2 = taskName.indexOf('$', idx);
    if (idx2 != -1) idx = idx2;
    String taskLogStepAtt = "log.step." + taskName.substring(idx + 1, taskName.length() - 4 /* w/o the 'Task' part */);
    if (config.get(taskLogStepAtt, null) != null) {
      logStepAtt = taskLogStepAtt;
    }

    // It's important to read this from Config, to support vals-by-round.
    logStep = config.get(logStepAtt, DEFAULT_LOG_STEP);
    // To avoid the check 'if (logStep > 0)' in tearDown(). This effectively
    // turns logging off.
    if (logStep <= 0) {
      logStep = Integer.MAX_VALUE;
    }
    checkObsoleteSettings(config);
  }
  
  protected Object clone() throws CloneNotSupportedException {
    // tasks having non primitive data structures should overide this.
    // otherwise parallel running of a task sequence might not run crrectly. 
    return super.clone();
  }

  public void close() throws Exception {
  }

  /**
   * Run the task, record statistics.
   * @return number of work items done by this task.
   */
  public final int runAndMaybeStats(boolean reportStats) throws Exception {
    if (reportStats && depth <= maxDepthLogStart && !shouldNeverLogAtStart()) {
      System.out.println("------------> starting task: " + getName());
    }
    if (!reportStats || shouldNotRecordStats()) {
      setup();
      int count = doLogic();
      count = disableCounting ? 0 : count;
      tearDown();
      return count;
    }
    setup();
    Points pnts = runData.getPoints();
    TaskStats ts = pnts.markTaskStart(this,runData.getConfig().getRoundNumber());
    int count = doLogic();
    count = disableCounting ? 0 : count;
    pnts.markTaskEnd(ts, count);
    tearDown();
    return count;
  }

  /**
   * Perform the task once (ignoring repetions specification)
   * Return number of work items done by this task.
   * For indexing that can be number of docs added.
   * For warming that can be number of scanned items, etc.
   * @return number of work items done by this task.
   */
  public abstract int doLogic() throws Exception;
  
  /**
   * @return Returns the name.
   */
  public String getName() {
    if (params==null) {
      return name;
    } 
    return new StringBuffer(name).append('(').append(params).append(')').toString();
  }

  /**
   * @param name The name to set.
   */
  protected void setName(String name) {
    this.name = name;
  }

  /**
   * @return Returns the run data.
   */
  public PerfRunData getRunData() {
    return runData;
  }

  /**
   * @return Returns the depth.
   */
  public int getDepth() {
    return depth;
  }

  /**
   * @param depth The depth to set.
   */
  public void setDepth(int depth) {
    this.depth = depth;
  }
  
  // compute a blank string padding for printing this task indented by its depth  
  String getPadding () {
    char c[] = new char[4*getDepth()];
    for (int i = 0; i < c.length; i++) c[i] = ' ';
    return new String(c);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String padd = getPadding();
    StringBuffer sb = new StringBuffer(padd);
    if (disableCounting) {
      sb.append('-');
    }
    sb.append(getName());
    return sb.toString();
  }

  /**
   * @return Returns the maxDepthLogStart.
   */
  int getMaxDepthLogStart() {
    return maxDepthLogStart;
  }

  protected String getLogMessage(int recsCount) {
    return "processed " + recsCount + " records";
  }
  
  /**
   * Tasks that should never log at start can overide this.  
   * @return true if this task should never log when it start.
   */
  protected boolean shouldNeverLogAtStart () {
    return false;
  }
  
  /**
   * Tasks that should not record statistics can overide this.  
   * @return true if this task should never record its statistics.
   */
  protected boolean shouldNotRecordStats () {
    return false;
  }

  /**
   * Task setup work that should not be measured for that specific task.
   * By default it does nothing, but tasks can implement this, moving work from 
   * doLogic() to this method. Only the work done in doLogicis measured for this task.
   * Notice that higher level (sequence) tasks containing this task would then 
   * measure larger time than the sum of their contained tasks.
   * @throws Exception 
   */
  public void setup () throws Exception {
  }
  
  /**
   * Task tearDown work that should not be measured for that specific task.
   * By default it does nothing, but tasks can implement this, moving work from 
   * doLogic() to this method. Only the work done in doLogicis measured for this task.
   * Notice that higher level (sequence) tasks containing this task would then 
   * measure larger time than the sum of their contained tasks.
   */
  public void tearDown() throws Exception {
    if (++logStepCount % logStep == 0) {
      double time = (System.currentTimeMillis() - runData.getStartTimeMillis()) / 1000.0;
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(2);
      System.out.println(nf.format(time) + " sec --> "
          + Thread.currentThread().getName() + " " + getLogMessage(logStepCount));
    }
  }

  /**
   * Sub classes that supports parameters must overide this method to return true.
   * @return true iff this task supports command line params.
   */
  public boolean supportsParams () {
    return false;
  }
  
  /**
   * Set the params of this task.
   * @exception UnsupportedOperationException for tasks supporting command line parameters.
   */
  public void setParams(String params) {
    if (!supportsParams()) {
      throw new UnsupportedOperationException(getName()+" does not support command line parameters.");
    }
    this.params = params;
  }
  
  /**
   * @return Returns the Params.
   */
  public String getParams() {
    return params;
  }

  /**
   * Return true if counting is disabled for this task.
   */
  public boolean isDisableCounting() {
    return disableCounting;
  }

  /**
   * See {@link #isDisableCounting()}
   */
  public void setDisableCounting(boolean disableCounting) {
    this.disableCounting = disableCounting;
  }

}
