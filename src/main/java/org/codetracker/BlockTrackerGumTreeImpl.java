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
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.util.*;

import static java.util.Map.entry;
import static org.codetracker.util.Util.*;

public class BlockTrackerGumTreeImpl extends BaseTracker implements BlockTracker {
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
                int descendantStartLine = startLine(lr, descendant);
                int descendantEndLine = endLine(lr, descendant);
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
            if (descendant.getType().toString().equalsIgnoreCase(blockType.getName() + "Statement")) {
                int descendantStartLine = startLine(lr, descendant);
                int descendantEndLine = endLine(lr, descendant);
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
                    // Checkout commit to get file contents at the current commit
                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commitId).call();
                    GumTreeSource source = new GumTreeSource(repository, rightBlock.getFilePath());

                    LineReader lrSource = getLineReader(source.completeFilePath);

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

                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef(parentCommitId).call();


                    GumTreeSource destination = null;

                    if (leftMethod != null) {
                        destination = new GumTreeSource(repository, leftMethod.getFilePath());
                    }

                    if (destination == null || destination.tree == null) {
                        destination = new GumTreeSource(repository, rightMethod.getFilePath());
                    }

                    if (destination == null || destination.tree == null) {
                        destination = new GumTreeSource(repository, newLeftFilePath);
                    }

                    // If the file is still not found, it can't be found by GumTree anymore
                    if (destination.tree == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                    MappingStore mappings = defaultMatcher.match(source.tree, destination.tree);

                    EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                    EditScript actions = editScriptGenerator.computeActions(mappings);
                    LineReader lrDestination = getLineReader(destination.completeFilePath);

                    Tree leftBlockGT = mappings.getDstForSrc(rightBlockGT);

                    if (leftMethod == null && rightMethodGT != null) {
                        Tree leftMethodGT = mappings.getDstForSrc(rightMethodGT);

                        // if a block was mapped but the parent method was unmapped
                        // (the block moved another method wasn't mapped to the original method)
                        if (leftMethodGT == null && leftBlockGT != null){
                            // find and map the parent method of the block that did match
                            for (Tree blockParent : leftBlockGT.getParents()){
                                if (blockParent.getType().toString().equals("MethodDeclaration")){
                                    leftMethodGT = blockParent;
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
                        this.methodStartLineNumberTree = startLine(lrDestination, leftMethodGT);
                        this.methodEndLineNumberTree = endLine(lrDestination, leftMethodGT);

                        leftModel = getUMLModel(parentCommitId, Collections.singleton(destination.filePath));
                        leftMethod = getMethod(leftModel, parentVersion, this::isEqualToMethodTree);
                    }

                    Map<String, CodeElementType> treeToBlockType = Map.ofEntries(
                            entry("ForStatement", CodeElementType.FOR_STATEMENT),
                            entry("TryStatement", CodeElementType.TRY_STATEMENT),
                            entry("IfStatement", CodeElementType.IF_STATEMENT)
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

                    this.blockStartLineNumberTree = startLine(lrDestination, leftBlockGT);
                    this.blockEndLineNumberTree = endLine(lrDestination, leftBlockGT);

                    Block leftBlock = leftMethod.findBlock(this::isEqualToBlockTree);

                    boolean bodyChange = false;
                    boolean expressionChange = false;
                    for (Action action : actions.asList()) {
                        // Here check each action and derive the change made

                        int actionStartLine = startLine(lrSource, action.getNode());
                        int actionEndLine = endLine(lrSource, action.getNode());
                        int blockStartLine = startLine(lrSource, rightBlockGT);
                        int blockEndLine = endLine(lrSource, rightBlockGT);
                        if (actionStartLine <= blockEndLine
                                && actionEndLine >= blockStartLine) {
                            Tree expression = null;
                            for (Tree parent: action.getNode().getParents()){
                                if (startLine(lrSource, parent) == blockStartLine && parent.getType().toString().contains("Expression")){
                                    expression = parent;
                                }
                            }

                            int expressionStartPos = -1;
                            int expressionEndPos = -1;

                            if (expression != null){
                                expressionStartPos = expression.getPos();
                                expressionEndPos = expression.getEndPos();
                            }

                            int actionStartPos = action.getNode().getPos();
                            int actionEndPos = action.getNode().getEndPos();

                            // check if a change was made within an expression
                            if (actionStartPos <= expressionEndPos && actionEndPos >= expressionStartPos){
                                expressionChange = true;
                            }
                            // check if a change was made within the body
                            if (actionEndPos > expressionEndPos){
                                bodyChange = true;
                            }
                            // if both types of changes are found, break loop
                            if (bodyChange && expressionChange){
                                break;
                            }
                        }
                    }

                    if (expressionChange){
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                    }

                    if (bodyChange){
                        blockChangeHistory.addChange(leftBlock, rightBlock, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                    }

                    if (!bodyChange && !expressionChange) {
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

            return new HistoryImpl<>(blockChangeHistory.findSubGraph(startBlock), historyReport);
        }
    }

    public class GumTreeSource {
        // filePath within the repo
        String filePath = null;
        // filePath from the source folder
        String completeFilePath = null;
        // GumTree JDT Tree object
        Tree tree = null;

        public GumTreeSource(Repository repository, String relativeFilePath) {
            try {
                filePath = relativeFilePath;
                completeFilePath = repository.getDirectory().getParent() + "/" + filePath;
                tree = new JdtTreeGenerator().generateFrom().file(completeFilePath).getRoot();
            } catch (Exception e) {
            }
        }
    }

}
