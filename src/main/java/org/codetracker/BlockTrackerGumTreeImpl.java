package org.codetracker;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLModel;
import org.codetracker.api.BlockTrackerGumTree;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.util.*;

import static java.util.Map.entry;
import static org.codetracker.util.Util.*;

public class BlockTrackerGumTreeImpl extends BaseTracker implements BlockTrackerGumTree {
    private final ChangeHistory<Block> blockChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType blockType;
    private final int blockStartLineNumber;
    private final int blockEndLineNumber;
    //    attributes for block predicate
    private CodeElementType treeType;
    private int blockStartLineNumberTree;
    private int blockEndLineNumberTree;
    // attributes for method predicate
    private int methodStartLineNumberTree;
    private int methodEndLineNumberTree;

    public BlockTrackerGumTreeImpl(Repository repository, String startCommitId, String filePath,
                                   String methodName, int methodDeclarationLineNumber,
                                   CodeElementType blockType, int blockStartLineNumber, int blockEndLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.blockType = blockType;
        this.blockStartLineNumber = blockStartLineNumber;
        this.blockEndLineNumber = blockEndLineNumber;
    }

    // Convert CodeTracker Method to GumTree Tree
    private Tree methodToGumTree(Method method, Tree sourceTree, LineReader lr) {
        Tree methodGT = null;
        int methodStartLine = method.getLocation().getStartLine();
        int methodEndLine = method.getLocation().getEndLine();
        for (Tree descendant : sourceTree.getDescendants()) {
            if (descendant.getType().toString().equals("MethodDeclaration")) {
                int descendantStartLine = startLine(descendant, lr);
                int descendantEndLine = endLine(descendant, lr);
                if (descendantStartLine == methodStartLine && descendantEndLine == methodEndLine) {
                    methodGT = descendant;
                    break;
                }
            }
        }
        return methodGT;
    }

    // Convert CodeTracker Block to GumTree Tree
    private Tree blockToGumTree(Block block, Tree sourceTree, LineReader lr) {
        Tree blockGT = null;
        for (Tree descendant : sourceTree.getDescendants()) {
            if (descendant.getType().toString().contains("Statement") ||
                    descendant.getType().toString().equals("CatchClause") ||
                    (
                            block.getLocation().getCodeElementType().toString().equals("FINALLY_BLOCK") &&
                                    descendant.getType().toString().equals("Block")
                    )
            ) {
                int descendantStartLine = startLine(descendant, lr);
                int descendantEndLine = endLine(descendant, lr);
                if (descendantStartLine == block.getLocation().getStartLine()
                        && descendantEndLine == block.getLocation().getEndLine()) {
                    blockGT = descendant;
                    break;
                }
            }
        }
        return blockGT;
    }

    private boolean isStartBlock(Block block) {
        return block.getComposite().getLocationInfo().getCodeElementType().equals(blockType) &&
                block.getComposite().getLocationInfo().getStartLine() == blockStartLineNumber &&
                block.getComposite().getLocationInfo().getEndLine() == blockEndLineNumber;
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    private boolean isEqualToBlockTree(Block block) {
        return block.getComposite().getLocationInfo().getCodeElementType().equals(treeType) &&
                block.getComposite().getLocationInfo().getStartLine() == blockStartLineNumberTree &&
                block.getComposite().getLocationInfo().getEndLine() == blockEndLineNumberTree;
    }

    private boolean isEqualToMethodTree(Method method) {
        return method.getUmlOperation().getLocationInfo().getStartLine() == methodStartLineNumberTree &&
                method.getUmlOperation().getLocationInfo().getEndLine() == methodEndLineNumberTree;
    }


    @Override
    public History<Block> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method startMethod = getMethod(umlModel, startVersion, this::isStartMethod);
            if (startMethod == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }
            Block startBlock = startMethod.findBlock(this::isStartBlock);
            if (startBlock == null) {
                throw new CodeElementNotFoundException(filePath, blockType.getName(), blockStartLineNumber);
            }
            blockChangeHistory.addNode(startBlock);

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
                if (commits == null || !currentBlock.getFilePath().equals(lastFileName)) {
                    lastFileName = currentBlock.getFilePath();
                    commits = getCommits(repository, currentBlock.getVersion().getId(), currentBlock.getFilePath(), git);
                    analysedCommits.clear();
                }
                if (analysedCommits.containsAll(commits))
                    break;
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);
                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);
                    Method currentMethod = Method.of(currentBlock.getOperation(), currentVersion);
                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);

                    if (rightMethod == null) {
                        continue;
                    }

                    Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
                    if (rightBlock == null) {
                        continue;
                    }

                    if ("0".equals(parentCommitId)) {
                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                        Block leftBlock = Block.of(rightBlock.getComposite(), leftMethod);
                        blockChangeHistory.handleAdd(leftBlock, rightBlock, "Initial commit!");
                        blockChangeHistory.connectRelatedNodes();
                        blocks.add(leftBlock);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));
                    // NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    // If the method has stayed the same between both commits
                    if (leftMethod != null) {
                        continue;
                    }

                    // CHANGE BODY OR DOCUMENT
                    // If the method body has changed between commits but signature remains the same
                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);

                    GumTreeSource source = new GumTreeSource(repository, commitId, rightBlock.getFilePath());

                    LineReader lrSource = getLineReader(source.fileContent);

                    Tree rightMethodGT = null;
                    String newLeftFilePath = null;
                    // check if file name has changed
                    if (leftMethod == null) {
                        //  Here handle cases where method signature has changed, get new method mapping from gumtree
                        DiffEntry diff = diffFile(repository,
                                parentCommitId,
                                commitId,
                                rightMethod.getFilePath());
                        if (diff != null
                                && !diff.getOldPath().equals(diff.getNewPath())
                                && !diff.getOldPath().equals("/dev/null")
                        ) {
                            // Obtain the new file name from git in case of rename
                            newLeftFilePath = diff.getOldPath();
                        }
                        rightMethodGT = methodToGumTree(rightMethod, source.tree, lrSource);
                    }

                    Tree rightBlockGT = blockToGumTree(rightBlock, source.tree, lrSource);

                    GumTreeSource destination = null;

                    if (leftMethod != null) {
                        destination = new GumTreeSource(repository, parentCommitId, leftMethod.getFilePath());
                    }

                    if (destination == null || destination.tree == null) {
                        destination = new GumTreeSource(repository, parentCommitId, rightMethod.getFilePath());
                    }

                    if (destination == null || destination.tree == null) {
                        destination = new GumTreeSource(repository, parentCommitId, newLeftFilePath);
                    }

                    // find all the files that were modified in this commit
                    List<DiffEntry> changedFiles = listDiff(repository, git, commitId, parentCommitId);
                    Matcher defaultMatcher = Matchers.getInstance().getMatcher();

                    Tree leftMethodGT = null;
                    // if no file was found by git (newLeftFilePath), 
                    // we try all the other files in the commit
                    if (destination == null || destination.tree == null) {
                        for (DiffEntry file : changedFiles) {
                            String additionalFilePath = file.getOldPath();
                            GumTreeSource additionalDestination = new GumTreeSource(repository, parentCommitId, additionalFilePath);

                            if (additionalDestination == null || additionalDestination.tree == null) {
                                continue;
                            }
                            MappingStore additionalMappings = defaultMatcher.match(source.tree, additionalDestination.tree);
                            leftMethodGT = additionalMappings.getDstForSrc(rightMethodGT);
                            if (leftMethodGT != null) {
                                destination = additionalDestination;
                                break;
                            }
                        }
                    }

                    // If the file is still not found, it can't be found by GumTree anymore
                    if (destination.tree == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    MappingStore mappings = defaultMatcher.match(source.tree, destination.tree);

                    EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                    EditScript actions = editScriptGenerator.computeActions(mappings);
                    LineReader lrDestination = getLineReader(destination.fileContent);

                    Tree leftBlockGT = mappings.getDstForSrc(rightBlockGT);

                    if (leftMethod == null && rightMethodGT != null) {
                        leftMethodGT = mappings.getDstForSrc(rightMethodGT);

                        // if a block was mapped but the parent method was unmapped
                        // (the block moved another method wasn't mapped to the original method)
                        if (leftMethodGT == null && leftBlockGT != null) {
                            // find and map the parent method of the block that did match
                            for (Tree blockParent : leftBlockGT.getParents()) {
                                if (blockParent.getType().toString().equals("MethodDeclaration")) {
                                    leftMethodGT = blockParent;
                                    break;
                                }
                            }
                        }
                        // if method was not found and the block was not mapped
                        // try to see if the method moved to another file
                        if (leftMethodGT == null) {
                            for (DiffEntry file : changedFiles) {
                                String additionalFilePath = file.getOldPath();
                                GumTreeSource additionalDestination = new GumTreeSource(repository, parentCommitId, additionalFilePath);
                                if (additionalDestination == null || additionalDestination.tree == null) {
                                    continue;
                                }
                                MappingStore additionalMappings = defaultMatcher.match(source.tree, additionalDestination.tree);
                                leftMethodGT = additionalMappings.getDstForSrc(rightMethodGT);
                                if (leftMethodGT != null) {
                                    destination = additionalDestination;
                                    lrDestination = getLineReader(destination.fileContent);
                                    leftBlockGT = additionalMappings.getDstForSrc(rightBlockGT);
                                    break;
                                }
                            }
                        }

                        // if both left method and block are unmapped
                        if (leftMethodGT == null) {
                            Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                            blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                            blocks.add(blockBefore);
                            blockChangeHistory.connectRelatedNodes();
                            break;
                        }

                        // Set attributes for matching method predicate
                        this.methodStartLineNumberTree = startLine(leftMethodGT, lrDestination);
                        this.methodEndLineNumberTree = endLine(leftMethodGT, lrDestination);

                        leftModel = getUMLModel(parentCommitId, Collections.singleton(destination.filePath));
                        leftMethod = getMethod(leftModel, parentVersion, this::isEqualToMethodTree);
                    }

                    Map<String, CodeElementType> treeToBlockType = Map.ofEntries(
                            entry("ForStatement", CodeElementType.FOR_STATEMENT),
                            entry("EnhancedForStatement", CodeElementType.ENHANCED_FOR_STATEMENT),
                            entry("WhileStatement", CodeElementType.WHILE_STATEMENT),
                            entry("DoStatement", CodeElementType.DO_STATEMENT),
                            entry("TryStatement", CodeElementType.TRY_STATEMENT),
                            entry("IfStatement", CodeElementType.IF_STATEMENT),
                            entry("CatchClause", CodeElementType.CATCH_CLAUSE),
                            entry("SwitchStatement", CodeElementType.SWITCH_STATEMENT),
                            entry("SynchronizedStatement", CodeElementType.SYNCHRONIZED_STATEMENT),
                            entry("Block", CodeElementType.FINALLY_BLOCK)
                    );
                    // The method exists but the block doesn't, so it's newly introduced
                    if (leftBlockGT == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "new block");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    // Set attributes for matching block predicate
                    this.treeType = treeToBlockType.get(leftBlockGT.getType().name);

                    this.blockStartLineNumberTree = startLine(leftBlockGT, lrDestination);
                    this.blockEndLineNumberTree = endLine(leftBlockGT, lrDestination);

                    Block leftBlock = leftMethod.findBlock(this::isEqualToBlockTree);
                    CodeElementRange leftBlockRange = new CodeElementRange(leftBlockGT, lrDestination);
                    CodeElementRange rightBlockRange = new CodeElementRange(rightBlockGT, lrSource);

                    boolean bodyChange = false;
                    boolean expressionChange = false;
                    boolean catchClauseChange = false;
                    boolean finallyBlockChange = false;

                    for (Action action : actions.asList()) {
                        CodeElementRange blockRange;
                        LineReader lrFile;
                        if (action.getName().contains("insert")) {
                            blockRange = leftBlockRange;
                            lrFile = lrDestination;
                        } else {
                            blockRange = rightBlockRange;
                            lrFile = lrSource;
                        }

                        // Here check each action and derive the change made
                        CodeElementRange actionRange = new CodeElementRange(action.getNode(), lrFile);
                        if (actionOverlapsElement(actionRange, blockRange)) {
                            Tree expression = null;
                            Tree body = null;
                            ArrayList<Tree> catchClauses = new ArrayList<>();
                            Tree finallyBlock = null;
                            for (Tree parent : action.getNode().getParents()) {
                                CodeElementRange parentRange = new CodeElementRange(parent, lrFile);
                                // if the action parent start line matches our block's start line
                                // we have our element
                                // the last condition handles cases of multiple if/else blocks
                                // where the start line doesn't match block start line
                                if (
                                        (parentRange.startPosition == blockRange.startPosition &&
                                                (parent.getType().toString().contains("Statement") ||
                                                        parent.getType().toString().equals("CatchClause") ||
                                                        (parent.getType().toString().equals("Block") &&
                                                                this.treeType == CodeElementType.FINALLY_BLOCK))
                                        ) ||
                                                (parentRange.endPosition == blockRange.endPosition &&
                                                        parent.getType().toString().equals("IfStatement"))
                                ) {
                                    // obtain statement body, expression, and catch/finally positions (if any)
                                    for (Tree child : parent.getChildren()) {
                                        String childType = child.getType().toString();
                                        switch (childType) {
                                            case "InstanceofExpression":
                                                expression = child;
                                                break;
                                            case "Block":
                                                if (body == null) {
                                                    body = child;
                                                } else if (this.treeType == CodeElementType.TRY_STATEMENT) {
                                                    finallyBlock = child;
                                                }
                                                break;
                                            case "CatchClause":
                                                catchClauses.add(child);
                                                break;
                                        }
                                    }
                                }
                            }

                            CodeElementRange bodyRange = new CodeElementRange(body, lrFile);
                            CodeElementRange expressionRange = new CodeElementRange(expression, lrFile);
                            CodeElementRange catchClauseRange = new CodeElementRange(catchClauses, lrFile);
                            CodeElementRange finallyBlockRange = new CodeElementRange(finallyBlock, lrFile);

                            // check if a change was made within an expression
                            if (actionOverlapsElement(actionRange, expressionRange)) {
                                expressionChange = true;
                            }
                            // check if a change was made within the body
                            if (actionOverlapsElement(actionRange, bodyRange)) {
                                bodyChange = true;
                            }
                            // check if a change was made within a 'catch' clause
                            if (actionOverlapsElement(actionRange, catchClauseRange)) {
                                catchClauseChange = true;
                            }

                            // check if a change was made within the 'finally' clause
                            if (actionOverlapsElement(actionRange, finallyBlockRange)) {
                                finallyBlockChange = true;
                            }

                            // if all types of changes are found, break loop
                            // && catchClauseChange
                            if (bodyChange && expressionChange && catchClauseChange && finallyBlockChange) {
                                break;
                            }
                        }
                    }

                    if (expressionChange) {
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                    }

                    if (bodyChange) {
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                    }

                    if (catchClauseChange) {
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                    }

                    if (finallyBlockChange) {
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_CHANGE));
                    }

                    if (!(bodyChange || expressionChange || catchClauseChange || finallyBlockChange)) {
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    }

                    blockChangeHistory.connectRelatedNodes();

                    // add the parent block to continue tracking
                    if (leftBlock != null) {
                        blocks.add(leftBlock);
                        break;
                    }
                }
            }

            return new HistoryImpl<>(blockChangeHistory.getCompleteGraph(), historyReport);
        }
    }

    public class GumTreeSource {
        // filePaths within the repo
        String filePath = null;
        // File content
        String fileContent = null;
        // GumTree JDT Tree object
        Tree tree = null;

        public GumTreeSource(Repository repository, String commitId, String filePath) {
            try {
                this.filePath = filePath;
                this.fileContent = getFileContent(repository, commitId, filePath);
                this.tree = new JdtTreeGenerator().generateFrom().string(this.fileContent).getRoot();
            } catch (Exception e) {
            }
        }
    }

    public class CodeElementRange {
        public int startLine = -1;
        public int endLine = -1;
        public int startPosition = -1;
        public int endPosition = -1;

        public CodeElementRange(Tree codeElement, LineReader lr) {
            if (codeElement == null) {
                return;
            }
            this.startPosition = codeElement.getPos();
            this.endPosition = codeElement.getEndPos();
            this.startLine = startLine(codeElement, lr);
            this.endLine = endLine(codeElement, lr);
        }

        public CodeElementRange(ArrayList<Tree> codeElements, LineReader lr) {
            if (codeElements == null || codeElements.size() == 0) {
                return;
            }
            int startPosition = Integer.MAX_VALUE;
            int endPosition = 0;

            for (Tree codeElement : codeElements) {
                startPosition = Math.min(startPosition, codeElement.getPos());
                endPosition = Math.max(endPosition, codeElement.getEndPos());
            }
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.startLine = startLine(this.startPosition, lr);
            this.endLine = endLine(this.endPosition, lr);
        }

        @Override
        public String toString() {
            return "Lines: (" + startLine + ", " + endLine + "), Positions: (" + startPosition + ", " + endPosition + ")";
        }
    }

}
