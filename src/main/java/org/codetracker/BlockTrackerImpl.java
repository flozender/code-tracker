package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import java.util.*;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class BlockTrackerImpl extends BaseTracker implements BlockTracker {

  private final BlockTrackerChangeHistory changeHistory;

  public BlockTrackerImpl(
    Repository repository,
    String startCommitId,
    String filePath,
    String methodName,
    int methodDeclarationLineNumber,
    CodeElementType blockType,
    int blockStartLineNumber,
    int blockEndLineNumber
  ) {
    super(repository, startCommitId, filePath);
    this.changeHistory =
      new BlockTrackerChangeHistory(
        methodName,
        methodDeclarationLineNumber,
        blockType,
        blockStartLineNumber,
        blockEndLineNumber
      );
  }

  @Override
  public History<Block> track() throws Exception {
    String prevCommit = null;
    long startTime = 0;
    boolean move = false;

    HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
    try (Git git = new Git(repository)) {
      Version startVersion = gitRepository.getVersion(startCommitId);
      UMLModel umlModel = getUMLModel(
        startCommitId,
        Collections.singleton(filePath)
      );
      Method startMethod = getMethod(
        umlModel,
        startVersion,
        changeHistory::isStartMethod
      );
      if (startMethod == null) {
        throw new CodeElementNotFoundException(
          filePath,
          changeHistory.getMethodName(),
          changeHistory.getMethodDeclarationLineNumber()
        );
      }
      Block startBlock = startMethod.findBlock(changeHistory::isStartBlock);
      if (startBlock == null) {
        throw new CodeElementNotFoundException(
          filePath,
          changeHistory.getBlockType().getName(),
          changeHistory.getBlockStartLineNumber()
        );
      }
      changeHistory.get().addNode(startBlock);

      ArrayDeque<Block> blocks = new ArrayDeque<>();
      blocks.addFirst(startBlock);
      HashSet<String> analysedCommits = new HashSet<>();
      List<String> commits = null;
      String lastFileName = null;
      while (!blocks.isEmpty()) {
        Block currentBlock = blocks.poll();
        if (currentBlock.isAdded()) {
          commits = null;
          continue;
        }
        if (
          commits == null || !currentBlock.getFilePath().equals(lastFileName)
        ) {
          lastFileName = currentBlock.getFilePath();
          commits =
            getCommits(
              repository,
              currentBlock.getVersion().getId(),
              currentBlock.getFilePath(),
              git
            );
          historyReport.gitLogCommandCallsPlusPlus();
          analysedCommits.clear();
        }
        if (analysedCommits.containsAll(commits)) break;
        for (String commitId : commits) {
          if (analysedCommits.contains(commitId)) continue;
          //System.out.println("processing " + commitId);
          long commitProcessingTime = (System.nanoTime() - startTime) / 1000000;
          historyReport.addProcessingInfo(
            prevCommit,
            filePath,
            commitProcessingTime,
            move
          );
          prevCommit = commitId;
          startTime = System.nanoTime();
          move = false;
          analysedCommits.add(commitId);
          Version currentVersion = gitRepository.getVersion(commitId);
          String parentCommitId = gitRepository.getParentId(commitId);
          Version parentVersion = gitRepository.getVersion(parentCommitId);
          Method currentMethod = Method.of(
            currentBlock.getOperation(),
            currentVersion
          );
          UMLModel rightModel = getUMLModel(
            commitId,
            Collections.singleton(currentMethod.getFilePath())
          );
          Method rightMethod = getMethod(
            rightModel,
            currentVersion,
            currentMethod::equalIdentifierIgnoringVersion
          );
          if (rightMethod == null) {
            continue;
          }
          String rightMethodClassName = rightMethod
            .getUmlOperation()
            .getClassName();
          Block rightBlock = rightMethod.findBlock(
            currentBlock::equalIdentifierIgnoringVersion
          );
          if (rightBlock == null) {
            continue;
          }
          Predicate<Method> equalMethod =
            rightMethod::equalIdentifierIgnoringVersion;
          Predicate<Block> equalBlock =
            rightBlock::equalIdentifierIgnoringVersion;
          historyReport.analysedCommitsPlusPlus();
          if ("0".equals(parentCommitId)) {
            Method leftMethod = Method.of(
              rightMethod.getUmlOperation(),
              parentVersion
            );
            Block leftBlock = Block.of(rightBlock.getComposite(), leftMethod);
            changeHistory
              .get()
              .handleAdd(leftBlock, rightBlock, "Initial commit!");
            changeHistory.get().connectRelatedNodes();
            blocks.add(leftBlock);
            commitProcessingTime = (System.nanoTime() - startTime) / 1000000;
            historyReport.addProcessingInfo(
              prevCommit,
              filePath,
              commitProcessingTime,
              false
            );
            break;
          }
          UMLModel leftModel = getUMLModel(
            parentCommitId,
            Collections.singleton(currentMethod.getFilePath())
          );
          //NO CHANGE
          Method leftMethod = getMethod(
            leftModel,
            parentVersion,
            rightMethod::equalIdentifierIgnoringVersion
          );
          if (leftMethod != null) {
            historyReport.step2PlusPlus();
            continue;
          }
          //CHANGE BODY OR DOCUMENT
          leftMethod =
            getMethod(
              leftModel,
              parentVersion,
              rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody
            );
          //check if there is another method in leftModel with identical bodyHashCode to the rightMethod
          boolean otherExactMatchFound = false;
          if (leftMethod != null) {
            for (UMLClass leftClass : leftModel.getClassList()) {
              for (UMLOperation leftOperation : leftClass.getOperations()) {
                if (
                  leftOperation.getBodyHashCode() ==
                  rightMethod.getUmlOperation().getBodyHashCode() &&
                  !leftOperation.equals(leftMethod.getUmlOperation())
                ) {
                  otherExactMatchFound = true;
                  break;
                }
              }
              if (otherExactMatchFound) {
                break;
              }
            }
          }
          if (leftMethod != null && !otherExactMatchFound) {
            VariableDeclarationContainer leftOperation = leftMethod.getUmlOperation();
            VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
            UMLOperationBodyMapper bodyMapper = null;
            if (
              leftOperation instanceof UMLOperation &&
              rightOperation instanceof UMLOperation
            ) {
              UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(
                leftModel,
                rightModel,
                leftOperation,
                rightOperation
              );
              bodyMapper =
                new UMLOperationBodyMapper(
                  (UMLOperation) leftOperation,
                  (UMLOperation) rightOperation,
                  lightweightClassDiff
                );
              if (
                containsCallToExtractedMethod(
                  bodyMapper,
                  bodyMapper.getClassDiff()
                )
              ) {
                bodyMapper = null;
              }
            } else if (
              leftOperation instanceof UMLInitializer &&
              rightOperation instanceof UMLInitializer
            ) {
              UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(
                leftModel,
                rightModel,
                leftOperation,
                rightOperation
              );
              bodyMapper =
                new UMLOperationBodyMapper(
                  (UMLInitializer) leftOperation,
                  (UMLInitializer) rightOperation,
                  lightweightClassDiff
                );
              if (
                containsCallToExtractedMethod(
                  bodyMapper,
                  bodyMapper.getClassDiff()
                )
              ) {
                bodyMapper = null;
              }
            }
            if (
              changeHistory.checkBodyOfMatchedOperations(
                blocks,
                currentVersion,
                parentVersion,
                rightBlock::equalIdentifierIgnoringVersion,
                bodyMapper
              )
            ) {
              historyReport.step3PlusPlus();
              break;
            }
          }
          UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
          {
            //Local Refactoring
            List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
            boolean found = changeHistory.checkForExtractionOrInline(
              blocks,
              currentVersion,
              parentVersion,
              equalMethod,
              rightBlock,
              refactorings
            );
            if (found) {
              historyReport.step4PlusPlus();
              break;
            }
            found =
              changeHistory.checkRefactoredMethod(
                blocks,
                currentVersion,
                parentVersion,
                equalMethod,
                rightBlock,
                refactorings
              );
            if (found) {
              historyReport.step4PlusPlus();
              break;
            }
            found =
              changeHistory.checkBodyOfMatchedOperations(
                blocks,
                currentVersion,
                parentVersion,
                rightBlock::equalIdentifierIgnoringVersion,
                findBodyMapper(
                  umlModelDiffLocal,
                  rightMethod,
                  currentVersion,
                  parentVersion
                )
              );
            if (found) {
              historyReport.step4PlusPlus();
              break;
            }
          }
          //All refactorings
          {
            move = true;
            CommitModel commitModel = getCommitModel(commitId);
            if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
              String leftFilePath = null;
              for (MoveSourceFolderRefactoring ref : commitModel.moveSourceFolderRefactorings) {
                if (
                  ref
                    .getIdenticalFilePaths()
                    .containsValue(currentBlock.getFilePath())
                ) {
                  for (Map.Entry<String, String> entry : ref
                    .getIdenticalFilePaths()
                    .entrySet()) {
                    if (entry.getValue().equals(currentBlock.getFilePath())) {
                      leftFilePath = entry.getKey();
                      break;
                    }
                  }
                  if (leftFilePath != null) {
                    break;
                  }
                }
              }
              Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(
                commitModel,
                currentMethod.getFilePath(),
                s -> true,
                true
              );
              if (leftFilePath != null) {
                boolean found = false;
                for (UMLClass umlClass : umlModelPairPartial
                  .getLeft()
                  .getClassList()) {
                  if (umlClass.getSourceFile().equals(leftFilePath)) {
                    for (UMLOperation operation : umlClass.getOperations()) {
                      if (operation.equals(rightMethod.getUmlOperation())) {
                        VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(
                          umlModelPairPartial.getLeft(),
                          umlModelPairPartial.getRight(),
                          operation,
                          rightOperation
                        );
                        UMLOperationBodyMapper bodyMapper = new UMLOperationBodyMapper(
                          operation,
                          (UMLOperation) rightOperation,
                          lightweightClassDiff
                        );
                        found =
                          changeHistory.isMatched(
                            bodyMapper,
                            blocks,
                            currentVersion,
                            parentVersion,
                            rightBlock::equalIdentifierIgnoringVersion
                          );
                        if (found) {
                          break;
                        }
                      }
                    }
                    if (found) {
                      break;
                    }
                  }
                }
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }
              } else {
                UMLModelDiff umlModelDiffPartial = umlModelPairPartial
                  .getLeft()
                  .diff(umlModelPairPartial.getRight());
                //List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();

                boolean found;
                UMLOperationBodyMapper bodyMapper = findBodyMapper(
                  umlModelDiffPartial,
                  rightMethod,
                  currentVersion,
                  parentVersion
                );
                found =
                  changeHistory.checkBodyOfMatchedOperations(
                    blocks,
                    currentVersion,
                    parentVersion,
                    rightBlock::equalIdentifierIgnoringVersion,
                    bodyMapper
                  );
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }
              }
            }
            {
              Set<String> fileNames = getRightSideFileNames(
                currentMethod,
                commitModel,
                umlModelDiffLocal
              );
              Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(
                commitModel,
                currentMethod.getFilePath(),
                fileNames::contains,
                false
              );
              UMLModelDiff umlModelDiffAll = umlModelPairAll
                .getLeft()
                .diff(umlModelPairAll.getRight());

              Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
              UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(
                rightMethodClassName
              );
              if (classDiff != null) {
                List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
                boolean found = changeHistory.checkForExtractionOrInline(
                  blocks,
                  currentVersion,
                  parentVersion,
                  equalMethod,
                  rightBlock,
                  classLevelRefactorings
                );
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }

                found =
                  changeHistory.isBlockRefactored(
                    classLevelRefactorings,
                    blocks,
                    currentVersion,
                    parentVersion,
                    rightBlock::equalIdentifierIgnoringVersion
                  );
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }

                found =
                  changeHistory.checkRefactoredMethod(
                    blocks,
                    currentVersion,
                    parentVersion,
                    equalMethod,
                    rightBlock,
                    classLevelRefactorings
                  );
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }

                found =
                  changeHistory.checkClassDiffForBlockChange(
                    blocks,
                    currentVersion,
                    parentVersion,
                    equalMethod,
                    equalBlock,
                    classDiff
                  );
                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }
              }
              List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
              boolean flag = false;
              for (Refactoring refactoring : refactorings) {
                if (
                  RefactoringType.MOVE_AND_RENAME_OPERATION.equals(
                    refactoring.getRefactoringType()
                  ) ||
                  RefactoringType.MOVE_OPERATION.equals(
                    refactoring.getRefactoringType()
                  )
                ) {
                  MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                  Method movedOperation = Method.of(
                    moveOperationRefactoring.getMovedOperation(),
                    currentVersion
                  );
                  if (
                    rightMethod.equalIdentifierIgnoringVersion(movedOperation)
                  ) {
                    fileNames.add(
                      moveOperationRefactoring
                        .getOriginalOperation()
                        .getLocationInfo()
                        .getFilePath()
                    );
                    flag = true;
                  }
                }
              }
              if (flag) {
                umlModelPairAll =
                  getUMLModelPair(
                    commitModel,
                    currentMethod.getFilePath(),
                    fileNames::contains,
                    false
                  );
                umlModelDiffAll =
                  umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                refactorings = umlModelDiffAll.getRefactorings();
              }

              boolean found = changeHistory.checkForExtractionOrInline(
                blocks,
                currentVersion,
                parentVersion,
                equalMethod,
                rightBlock,
                refactorings
              );
              if (found) {
                historyReport.step5PlusPlus();
                break;
              }

              found =
                changeHistory.isBlockRefactored(
                  refactorings,
                  blocks,
                  currentVersion,
                  parentVersion,
                  rightBlock::equalIdentifierIgnoringVersion
                );
              if (found) {
                historyReport.step5PlusPlus();
                break;
              }

              found =
                changeHistory.checkRefactoredMethod(
                  blocks,
                  currentVersion,
                  parentVersion,
                  equalMethod,
                  rightBlock,
                  refactorings
                );
              if (found) {
                historyReport.step5PlusPlus();
                break;
              }

              UMLClassBaseDiff umlClassDiff = getUMLClassDiff(
                umlModelDiffAll,
                rightMethodClassName
              );
              if (umlClassDiff != null) {
                found =
                  changeHistory.checkClassDiffForBlockChange(
                    blocks,
                    currentVersion,
                    parentVersion,
                    equalMethod,
                    equalBlock,
                    umlClassDiff
                  );

                if (found) {
                  historyReport.step5PlusPlus();
                  break;
                }
              }

              if (
                isMethodAdded(
                  umlModelDiffAll,
                  rightMethod.getUmlOperation().getClassName(),
                  rightMethod::equalIdentifierIgnoringVersion,
                  method -> {},
                  currentVersion
                )
              ) {
                Block blockBefore = Block.of(
                  rightBlock.getComposite(),
                  rightBlock.getOperation(),
                  parentVersion
                );
                changeHistory
                  .get()
                  .handleAdd(blockBefore, rightBlock, "added with method");
                blocks.add(blockBefore);
                changeHistory.get().connectRelatedNodes();
                historyReport.step5PlusPlus();
                commitProcessingTime =
                  (System.nanoTime() - startTime) / 1000000;
                historyReport.addProcessingInfo(
                  prevCommit,
                  filePath,
                  commitProcessingTime,
                  false
                );
                break;
              }
            }
          }
        }
      }
      return new HistoryImpl<>(
        changeHistory.get().getCompleteGraph(),
        historyReport
      );
    }
  }
}
