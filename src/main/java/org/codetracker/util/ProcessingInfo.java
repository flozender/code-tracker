package org.codetracker.util;

public class ProcessingInfo {

  private String fileName;
  private String commitId;
  private long processingTime;
  private boolean move;
  private boolean change;

  public ProcessingInfo(
    String commitId,
    String fileName,
    long processingTime,
    boolean move,
    boolean change
  ) {
    this.commitId = commitId;
    this.fileName = fileName;
    this.processingTime = processingTime;
    this.move = move;
    this.change = change;
  }

  public String getCommitId() {
    return this.commitId;
  }

  public String getFileName() {
    return this.fileName;
  }

  public long getProcessingTime() {
    return this.processingTime;
  }

  public boolean getMove() {
    return this.move;
  }

  public boolean getChange() {
    return this.change;
  }

  @Override
  public String toString() {
    return (
      getFileName() +
      " " +
      getCommitId() +
      " " +
      getProcessingTime() +
      " " +
      getMove() +
      " " +
      getChange()
    );
  }

  public ProcessingInfo() {}
}
