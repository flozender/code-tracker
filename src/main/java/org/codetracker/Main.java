package org.codetracker;

import org.codetracker.api.*;
import org.codetracker.change.Change;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.codetracker.util.CodeElementLocator;
import gr.uom.java.xmi.LocationInfo;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Main {
    private final static String FOLDER_TO_CLONE = "tmp/";

    public static void main(String args[]) throws Exception {
        GitService gitService = new GitServiceImpl();
        
        // BLOCK TRACKING EXAMPLE
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")) {

            BlockTracker blockTracker = CodeTracker.blockTracker()
            .repository(repository)
            .filePath("src/checkstyle/com/puppycrawl/tools/checkstyle/Checker.java")
            .startCommitId("f55fcd5462624e611212b0b6c41e842d1adfa9f2")
            .methodName("setupChild")
            .methodDeclarationLineNumber(179)
            .codeElementType(LocationInfo.CodeElementType.IF_STATEMENT)
            .blockStartLineNumber(190)
            .blockEndLineNumber(205)
            .build();

            History<Block> blockHistory = blockTracker.track();

            for (History.HistoryInfo<Block> historyInfo : blockHistory.getHistoryInfoList()) {
                System.out.println("======================================================");
                System.out.println("Commit ID: " + historyInfo.getCommitId());
                System.out.println("Date: " +
                        LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
                System.out.println("Before: " + historyInfo.getElementBefore().getName());
                System.out.println("After: " + historyInfo.getElementAfter().getName());

                for (Change change : historyInfo.getChangeList()) {
                    System.out.println(change.getType().getTitle() + ": " + change);
                }
            }
            System.out.println("======================================================");
        }
    }
}

