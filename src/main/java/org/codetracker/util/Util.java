package org.codetracker.util;

import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class Util {
    private Util() {
    }

    public static String getPath(String filePath, String className) {
        String srcFile = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        CharSequence charSequence = longestSubstring(srcFile, className.replace(".", "/"));
        if (className.startsWith(charSequence.toString().replace("/", "."))) {
            srcFile = filePath.toLowerCase().replace(charSequence, "$");
            srcFile = srcFile.substring(0, srcFile.lastIndexOf("$"));
            return srcFile;
        }
        return srcFile;
    }

    public static String longestSubstring(String str1, String str2) {

        StringBuilder sb = new StringBuilder();
        if (str1 == null || str1.isEmpty() || str2 == null || str2.isEmpty())
            return "";

// ignore case
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

// java initializes them already with 0
        int[][] num = new int[str1.length()][str2.length()];
        int maxLength = 0;
        int lastSubsBegin = 0;

        for (int i = 0; i < str1.length(); i++) {
            for (int j = 0; j < str2.length(); j++) {
                if (str1.charAt(i) == str2.charAt(j)) {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];

                    if (num[i][j] > maxLength) {
                        maxLength = num[i][j];
                        // generate substring from str1 => i
                        int thisSubsBegin = i - num[i][j] + 1;
                        if (lastSubsBegin == thisSubsBegin) {
                            //if the current LCS is the same as the last time this block ran
                            sb.append(str1.charAt(i));
                        } else {
                            //this block resets the string builder if a different LCS is found
                            lastSubsBegin = thisSubsBegin;
                            sb = new StringBuilder();
                            sb.append(str1, lastSubsBegin, i + 1);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    protected static String getPackage(String filePath, String className) {
        try {
            String replace = className.replace(filePath.substring(filePath.lastIndexOf("/") + 1).replace(".java", ""), "$");
            String packageName = replace.substring(0, replace.lastIndexOf("$") - 1);
            packageName = getPath(filePath, className) + packageName;
            return packageName;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String annotationsToString(List<UMLAnnotation> umlAnnotations) {
        return umlAnnotations != null && !umlAnnotations.isEmpty()
                ? String.format("[%s]", umlAnnotations.stream().map(UMLAnnotation::toString).sorted().collect(Collectors.joining(";")))
                : "";
    }


    public static String getSHA512(String input) {
        String toReturn = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            toReturn = String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    // https://github.com/centic9/jgit-cookbook/blob/209b4d2d747af6e032d9b2c86cb82b1e7b2ca793/src/main/java/org/dstadler/jgit/porcelain/DiffRenamedFile.java
    public static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    public static DiffEntry diffFile(Repository repo, String oldCommit, String newCommit, String path) throws IOException, GitAPIException {
        Config config = new Config();
        config.setBoolean("diff", null, "renames", true);
        DiffConfig diffConfig = config.get(DiffConfig.KEY);
        try (Git git = new Git(repo)) {
            List<DiffEntry> diffList = git.diff().
                    setOldTree(prepareTreeParser(repo, oldCommit)).
                    setNewTree(prepareTreeParser(repo, newCommit)).
                    setPathFilter(FollowFilter.create(path, diffConfig)).
                    call();
            if (diffList.size() == 0)
                return null;
            if (diffList.size() > 1)
                throw new RuntimeException("invalid diff");
            return diffList.get(0);
        }
    }

    public static String getFileContent(Repository repository, String commitId, String fileName) throws Exception {
        if (fileName == null)
            return null;
        Set<String> repositoryDirectories = new LinkedHashSet<>();
        Map<String, String> fileContents = new LinkedHashMap<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, revCommit, Collections.singleton(fileName), fileContents, repositoryDirectories);
            return fileContents.get(fileName);
        }
    }

    public static LineReader getLineReader(String fileContent) throws IOException {
        LineReader lr = new LineReader(new BufferedReader(new StringReader(fileContent)));
        // wait till LineReader is done reading
        while ((lr.read()) != -1) {}
        lr.close();
        return lr;
    }

    public static int startLine(LineReader lr, Tree tree){
        return lr.positionFor(tree.getPos())[0];
    }

    public static int endLine(LineReader lr, Tree tree){
        return lr.positionFor(tree.getEndPos())[0];
    }
}
