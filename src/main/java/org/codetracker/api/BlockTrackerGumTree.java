package org.codetracker.api;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import org.codetracker.element.Block;
import org.codetracker.BlockTrackerGumTreeImpl;
import org.eclipse.jgit.lib.Repository;

public interface BlockTrackerGumTree extends CodeTracker {

    History<Block> track() throws Exception;

    class Builder {
        private Repository repository;
        private String startCommitId;
        private String filePath;
        private String methodName;
        private int methodDeclarationLineNumber;
        private CodeElementType codeElementType;
        private int blockStartLineNumber;
        private int blockEndLineNumber;

        public BlockTrackerGumTree.Builder repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public BlockTrackerGumTree.Builder startCommitId(String startCommitId) {
            this.startCommitId = startCommitId;
            return this;
        }

        public BlockTrackerGumTree.Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public BlockTrackerGumTree.Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public BlockTrackerGumTree.Builder methodDeclarationLineNumber(int methodDeclarationLineNumber) {
            this.methodDeclarationLineNumber = methodDeclarationLineNumber;
            return this;
        }

        public Builder codeElementType(CodeElementType codeElementType) {
            this.codeElementType = codeElementType;
            return this;
        }

        public BlockTrackerGumTree.Builder blockStartLineNumber(int blockStartLineNumber) {
            this.blockStartLineNumber = blockStartLineNumber;
            return this;
        }

        public BlockTrackerGumTree.Builder blockEndLineNumber(int blockEndLineNumber) {
            this.blockEndLineNumber = blockEndLineNumber;
            return this;
        }

        private void checkInput() {

        }

        public BlockTrackerGumTree build() {
            checkInput();
            return new BlockTrackerGumTreeImpl(repository, startCommitId, filePath, methodName, methodDeclarationLineNumber,
                    codeElementType, blockStartLineNumber, blockEndLineNumber);
        }
    }
}
