package org.codetracker.util;

public class ProcessingInfo {

  private long processingTime;
  private boolean move;
  private String fileName;
  private String commitId;

  public ProcessingInfo(
    String commitId,
    String fileName,
    long processingTime,
    boolean move
  ) {
    this.commitId = commitId;
    this.fileName = fileName;
    this.processingTime = processingTime;
    this.move = move;
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

  public ProcessingInfo() {}
}
