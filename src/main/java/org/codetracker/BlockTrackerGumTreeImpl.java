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
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.SplitClassRefactoring;
import org.codetracker.api.*;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.util.MethodCache;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private MethodCache cache;

    public BlockTrackerGumTreeImpl(Repository repository, String startCommitId, String filePath,
                                   String methodName, int methodDeclarationLineNumber,
                                   CodeElementType blockType, int blockStartLineNumber, int blockEndLineNumber,
                                   MethodCache cache) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.blockType = blockType;
        this.blockStartLineNumber = blockStartLineNumber;
        this.blockEndLineNumber = blockEndLineNumber;
        try {
            this.cache = cache;
        } catch (Exception ignored){}
    }

    // Convert CodeTracker Method to GumTree Tree
    private Tree methodToGumTree(Method method, GumTreeSource source) {
        Tree methodGT = null;
        int methodStartLine = method.getLocation().getStartLine();
        int methodEndLine = method.getLocation().getEndLine();
        for (Tree descendant : source.tree.getDescendants()) {
            if (descendant.getType().toString().equals("MethodDeclaration")) {
                int descendantStartLine = startLine(descendant, source.lineReader);
                int descendantEndLine = endLine(descendant, source.lineReader);
                if (descendantStartLine == methodStartLine && descendantEndLine == methodEndLine) {
                    methodGT = descendant;
                    break;
                }
            }
        }
        return methodGT;
    }

    // Convert CodeTracker Block to GumTree Tree
    private Tree blockToGumTree(Block block, GumTreeSource source) {
        Tree blockGT = null;
        for (Tree descendant : source.tree.getDescendants()) {
            if (descendant.getType().toString().contains("Statement") ||
                    descendant.getType().toString().equals("CatchClause") ||
                    (
                            block.getLocation().getCodeElementType().toString().equals("FINALLY_BLOCK") &&
                                    descendant.getType().toString().equals("Block")
                    )
            ) {
                int descendantStartLine = startLine(descendant, source.lineReader);
                int descendantEndLine = endLine(descendant, source.lineReader);
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
                    // leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
                    Matcher defaultMatcher = Matchers.getInstance().getMatcher();

                    // in the perspective of drilling down from the newest to the oldest commit,
                    // source is the current commit, destination is the parent commit
                    GumTreeSource source = new GumTreeSource(repository, commitId, rightBlock.getFilePath());
                    GumTreeSource destination = new GumTreeSource(repository, parentCommitId, rightMethod.getFilePath());


                    // GT suffix to variables indicates that it is a GumTree Tree
                    Tree rightMethodGT = methodToGumTree(rightMethod, source);
                    Tree rightBlockGT = blockToGumTree(rightBlock, source);

                    // if destination is null, then the file doesn't exist anymore
                    if (destination == null || destination.tree == null) {
                        // check if file name has changed (via git)
                        DiffEntry diff = diffFile(repository, parentCommitId, commitId, rightMethod.getFilePath());
                        if (diff != null && !diff.getOldPath().equals(diff.getNewPath()) && !diff.getOldPath().equals("/dev/null")) {
                            // Obtain the new file name from git in case of rename
                            destination = new GumTreeSource(repository, parentCommitId, diff.getOldPath());
                        }
                    }

                    // check if the method was moved using RMiner and set the new destination
                    String movedFilePath = getMovedFilePathFromRMiner(currentVersion.getId(), rightMethod);
                    if (movedFilePath != null) {
                        destination = new GumTreeSource(repository, parentCommitId, movedFilePath);
                    }

                    // If the file is still not found, it can't be found by GumTree or RMiner anymore
                    if (destination.tree == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    MappingStore mappings;
                    // postSource is the file the method was moved from,
                    // but in the parent commit
                    GumTreeSource postSource = new GumTreeSource(repository, parentCommitId, source.filePath);
                    // check if the postSource file exists in the parent commit
                    // TODO: handle cases where postSource was renamed
                    if (movedFilePath == null || (postSource == null || postSource.tree == null)) {
                        mappings = defaultMatcher.match(source.tree, destination.tree);
                    } else {
                        // postSource does exist, so first create mappings for postSource w/ source
                        MappingStore preMappings = defaultMatcher.match(source.tree, postSource.tree);
                        // and find mappings (for the method?) with the destination among unmapped nodes
                        mappings = defaultMatcher.match(source.tree, destination.tree, preMappings);
                    }


                    Tree leftMethodGT = null;
                    Tree leftBlockGT = mappings.getDstForSrc(rightBlockGT);

                    if (leftBlockGT != null) {
                        // find and map the parent method of the block that matched
                        for (Tree blockParent : leftBlockGT.getParents()) {
                            if (blockParent.getType().toString().equals("MethodDeclaration")) {
                                leftMethodGT = blockParent;
                                break;
                            }
                        }
                    } else if (rightMethodGT != null) {
                        // we reach here if left block was unmapped by gumtree
                        // now, check if the method can be mapped
                        // (cases where block was added to an existing method)
                        leftMethodGT = mappings.getDstForSrc(rightMethodGT);
                    }

                    // report cases where method was added with the block
                    // since here, the method and block are both unmapped
                    if (leftMethodGT == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    // The method exists but the block doesn't, so it's newly introduced
                    if (leftBlockGT == null) {
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        blockChangeHistory.handleAdd(blockBefore, rightBlock, "new block");
                        blocks.add(blockBefore);
                        blockChangeHistory.connectRelatedNodes();
                        break;
                    }

                    // Set attributes for matching method predicate
                    this.methodStartLineNumberTree = startLine(leftMethodGT, destination.lineReader);
                    this.methodEndLineNumberTree = endLine(leftMethodGT, destination.lineReader);

                    leftModel = getUMLModel(parentCommitId, Collections.singleton(destination.filePath));
                    leftMethod = getMethod(leftModel, parentVersion, this::isEqualToMethodTree);

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

                    // Set attributes for matching block predicate
                    this.treeType = treeToBlockType.get(leftBlockGT.getType().name);

                    this.blockStartLineNumberTree = startLine(leftBlockGT, destination.lineReader);
                    this.blockEndLineNumberTree = endLine(leftBlockGT, destination.lineReader);

                    Block leftBlock = leftMethod.findBlock(this::isEqualToBlockTree);

                    addChanges(leftBlock, rightBlock, rightMethod, mappings, source, destination);

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

    public String getMovedFilePathFromRMiner(String commitId, Method rightMethod) throws IOException {
        // Create a unique key for the cache entry based on the method signature and commit ID
        String cacheKey = "getMovedFilePathFromRMiner:" + commitId + ":" + rightMethod.toString();

        // Check if the cache contains the result for the given inputs
        if (cache != null && cache.hasKey(cacheKey)) {
            // Return the cached result if available
            System.out.println("Path found in cache: " + cacheKey);
            return cache.get(cacheKey);
        }

        // Compute the result if it's not in the cache
        final String[] movedFilePath = {null};
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit(repository, commitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                for (Refactoring ref : refactorings) {
                    if (ref instanceof MoveOperationRefactoring) {
                        MoveOperationRefactoring ref1 = (MoveOperationRefactoring) ref;
                        if (ref1.getOriginalOperation().equalSignature((UMLOperation) rightMethod.getUmlOperation())) {
                            movedFilePath[0] = ref1.getOriginalOperation().getLocationInfo().getFilePath();
                        }
                    } else if (ref instanceof SplitClassRefactoring){
                        SplitClassRefactoring ref1 = (SplitClassRefactoring) ref;
                        for (Object clazz : ref1.getSplitClasses().toArray()){
                            UMLClass umlClass = (UMLClass) clazz;
                            if (umlClass.getSourceFile().equals(rightMethod.getFilePath())){
                                movedFilePath[0] = ref1.getOriginalClass().getSourceFile();
                                break;
                            }
                        }
                    }
                }
            }
        });

        if (cache != null){
            // Cache the result for future use
            cache.put(cacheKey, movedFilePath[0]);
        }

        // Return the computed result
        return movedFilePath[0];
    }

    private void addChanges(Block blockBefore, Block blockAfter, Method methodAfter, MappingStore mappings, GumTreeSource source, GumTreeSource destination){
        boolean bodyChange = false;
        boolean catchOrFinallyChange = false;
        List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
        List<String> stringRepresentationAfter = blockAfter.getComposite().stringRepresentation();
        if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
            if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
            }
            List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
            List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
            if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
            }
            bodyChange = true;
        }
        if (blockBefore.getComposite() instanceof TryStatementObject && blockAfter.getComposite() instanceof TryStatementObject) {
            TryStatementObject tryBefore = (TryStatementObject) blockBefore.getComposite();
            TryStatementObject tryAfter = (TryStatementObject) blockAfter.getComposite();
            ArrayList<CompositeStatementObject> catchBlocksBefore = new ArrayList<>(tryBefore.getCatchClauses());
            ArrayList<CompositeStatementObject> catchBlocksAfter = new ArrayList<>(tryAfter.getCatchClauses());
            ArrayList<CompositeStatementObject> catchBlocksBeforeClone = (ArrayList<CompositeStatementObject>) catchBlocksAfter.clone();
            for (CompositeStatementObject catchBlockAfter : catchBlocksBeforeClone) {
                CompositeStatementObject fragment2 = catchBlockAfter;
                Tree catchBlockAfterGT = blockToGumTree(Block.of(fragment2, methodAfter), source);
                Tree catchBlockBeforeGT = mappings.getDstForSrc(catchBlockAfterGT);
                CodeElementRange catchBlockAfterRange = new CodeElementRange(catchBlockAfterGT, source.lineReader);
                CodeElementRange catchBlockBeforeRange = new CodeElementRange(catchBlockBeforeGT, destination.lineReader);

                List<CompositeStatementObject> potentialFragment1 = catchBlocksBefore.stream().filter(
                        c -> (c.getLocationInfo().getStartLine() == catchBlockBeforeRange.startLine && c.getLocationInfo().getEndLine()==catchBlockBeforeRange.endLine)
                ).collect(Collectors.toList());

                CompositeStatementObject fragment1 = null;
                if (potentialFragment1.size() > 0){
                    fragment1 = potentialFragment1.get(0);
                }

                if (fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                        fragment1 != null && fragment1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                        tryBefore.getCatchClauses().contains(fragment1) &&
                        tryAfter.getCatchClauses().contains(fragment2)) {
                    List<String> catchStringRepresentationBefore = fragment1.stringRepresentation();
                    List<String> catchStringRepresentationAfter = fragment2.stringRepresentation();
                    catchBlocksBefore.remove(fragment1);
                    catchBlocksAfter.remove(fragment2);
                    if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                        catchOrFinallyChange = true;
                    }
                }
            }
            Set<CompositeStatementObject> catchBlocksBeforeToRemove = new LinkedHashSet<>();
            Set<CompositeStatementObject> catchBlocksAfterToRemove = new LinkedHashSet<>();
            for (int i=0; i<Math.min(catchBlocksBefore.size(), catchBlocksAfter.size()); i++) {
                List<UMLType> typesBefore = new ArrayList<>();
                for (VariableDeclaration variableDeclaration : catchBlocksBefore.get(i).getVariableDeclarations()) {
                    typesBefore.add(variableDeclaration.getType());
                }
                List<UMLType> typesAfter = new ArrayList<>();
                for (VariableDeclaration variableDeclaration : catchBlocksAfter.get(i).getVariableDeclarations()) {
                    typesAfter.add(variableDeclaration.getType());
                }
                if (typesBefore.equals(typesAfter)) {
                    List<String> catchStringRepresentationBefore = catchBlocksBefore.get(i).stringRepresentation();
                    List<String> catchStringRepresentationAfter = catchBlocksAfter.get(i).stringRepresentation();
                    catchBlocksBeforeToRemove.add(catchBlocksBefore.get(i));
                    catchBlocksAfterToRemove.add(catchBlocksAfter.get(i));
                    if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                        catchOrFinallyChange = true;
                    }
                }
            }
            catchBlocksBefore.removeAll(catchBlocksBeforeToRemove);
            catchBlocksAfter.removeAll(catchBlocksAfterToRemove);
            for (CompositeStatementObject catchBlockBefore : catchBlocksBefore) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_REMOVED));
                catchOrFinallyChange = true;
            }
            for (CompositeStatementObject catchBlockAfter : catchBlocksAfter) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_ADDED));
                catchOrFinallyChange = true;
            }
            if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() != null) {
                List<String> finallyStringRepresentationBefore = tryBefore.getFinallyClause().stringRepresentation();
                List<String> finallyStringRepresentationAfter = tryAfter.getFinallyClause().stringRepresentation();
                if (!finallyStringRepresentationBefore.equals(finallyStringRepresentationAfter)) {
                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_CHANGE));
                    catchOrFinallyChange = true;
                }
            }
            else if (tryBefore.getFinallyClause() == null && tryAfter.getFinallyClause() != null) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_ADDED));
                catchOrFinallyChange = true;
            }
            else if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() == null) {
                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_REMOVED));
                catchOrFinallyChange = true;
            }
        }
        if (!bodyChange && !catchOrFinallyChange) {
            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
        }
    }

    public static class GumTreeSource {
        // filePaths within the repo
        String filePath = null;
        // File content
        String fileContent = null;
        // Commit ID
        String commitId = null;
        // GumTree JDT Tree object
        Tree tree = null;
        LineReader lineReader;

        public GumTreeSource(Repository repository, String commitId, String filePath) {
            try {
                this.filePath = filePath;
                this.commitId = commitId;
                this.fileContent = getFileContent(repository, commitId, filePath);
                this.tree = new JdtTreeGenerator().generateFrom().string(this.fileContent).getRoot();
                this.lineReader = getLineReader(fileContent);
            } catch (Exception e) {
            }
        }
    }

    public static class CodeElementRange {
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
