package org.codetracker.experiment;

import static org.codetracker.util.FileUtil.createDirectory;

import gr.uom.java.xmi.LocationInfo;
import java.io.IOException;
import java.util.List;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.BlockTrackerGumTree;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.codetracker.experiment.oracle.BlockOracle;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.codetracker.util.MethodCache;
import org.eclipse.jgit.lib.Repository;

public class BlockExperimentStarter extends AbstractExperimentStarter {

  private static final String TOOL_NAME = "gumtree";
  private static final String CODE_ELEMENT_NAME = "block";
  private static final MethodCache cache = new MethodCache("src/main/resources/oracle/cache.json");;

  public static void main(String[] args) throws IOException {
    new BlockExperimentStarter().start();
    cache.saveCacheToFile();
  }

  @Override
  protected String getCodeElementName() {
    return CODE_ELEMENT_NAME;
  }

  @Override
  protected String getToolName() {
    return TOOL_NAME;
  }

  public void start() throws IOException {
    createDirectory(
      "experiments",
      "experiments/tracking-accuracy",
      "experiments/tracking-accuracy/block",
      "experiments/tracking-accuracy/block/gumtree"
    );
    List<BlockOracle> oracles = BlockOracle.all();

    for (BlockOracle oracle : oracles) {
      codeTracker(oracle);
      calculateFinalResults(oracle.getName());
    }
  }

  private History<Block> blockTracker(
    BlockHistoryInfo blockHistoryInfo,
    Repository repository
  ) throws Exception {
    BlockTrackerGumTree blockTracker = CodeTracker
      .blockTrackerGumTree()
      .repository(repository)
      .filePath(blockHistoryInfo.getFilePath())
      .startCommitId(blockHistoryInfo.getStartCommitId())
      .methodName(blockHistoryInfo.getFunctionName())
      .methodDeclarationLineNumber(blockHistoryInfo.getFunctionStartLine())
      .codeElementType(
              LocationInfo.CodeElementType.valueOf(blockHistoryInfo.getBlockType())
      )
      .blockStartLineNumber(blockHistoryInfo.getBlockStartLine())
      .blockEndLineNumber(blockHistoryInfo.getBlockEndLine())
      .cache(cache)
      .build();
    return blockTracker.track();
  }

  private void codeTracker(BlockOracle blockOracle) throws IOException {
    codeTracker(blockOracle, this::blockTracker);
  }
}
