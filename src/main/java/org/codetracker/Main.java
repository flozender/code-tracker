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
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "apache\\commons-lang",
                "https://github.com/apache/commons-lang.git")) {

            BlockTrackerGumTree blockTracker = CodeTracker.blockTrackerGumTree()
                    .repository(repository)
                    .filePath("src/main/java/org/apache/commons/lang3/time/FastDatePrinter.java")
                    .startCommitId("1002c9e243883654f9c1e6beae643fb4e72fc172")
                    .methodName("parsePattern")
                    .methodDeclarationLineNumber(182)
                    .codeElementType(LocationInfo.CodeElementType.FOR_STATEMENT)
                    .blockStartLineNumber(196)
                    .blockEndLineNumber(308)
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

