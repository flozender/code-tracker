////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.CommonHiddenStreamToken;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;
import antlr.TokenStreamHiddenTokenFilter;
import antlr.TokenStreamRecognitionException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Context;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaLexer;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaRecognizer;

import static com.puppycrawl.tools.checkstyle.Utils.fileExtensionMatches;

/**
 * Responsible for walking an abstract syntax tree and notifying interested
 * checks at each each node.
 *
 * @author Oliver Burn
 */
public final class TreeWalker
    extends AbstractFileSetCheck
{
    /**
     * State of AST.
     * Indicates whether tree contains certain nodes.
     */
    private static enum AstState {
        /**
         * Ordinary tree.
         */
        ORDINARY,

        /**
         * AST contains comment nodes.
         */
        WITH_COMMENTS
    }

    /** default distance between tab stops */
    private static final int DEFAULT_TAB_WIDTH = 8;

    /** maps from token name to ordinary checks */
    private final Multimap<String, Check> tokenToOrdinaryChecks =
        HashMultimap.create();

    /** maps from token name to comment checks */
    private final Multimap<String, Check> tokenToCommentChecks =
            HashMultimap.create();

    /** registered ordinary checks, that don't use comment nodes */
    private final Set<Check> ordinaryChecks = Sets.newHashSet();

    /** registered comment checks */
    private final Set<Check> commentChecks = Sets.newHashSet();

    /** the distance between tab stops */
    private int tabWidth = DEFAULT_TAB_WIDTH;

    /** cache file **/
    private PropertyCacheFile cache = new PropertyCacheFile(null, null);

    /** class loader to resolve classes with. **/
    private ClassLoader classLoader;

    /** context of child components */
    private Context childContext;

    /** a factory for creating submodules (i.e. the Checks) */
    private ModuleFactory moduleFactory;

    /** logger for debug purpose */
    private static final Log LOG = LogFactory.getLog(TreeWalker.class);

    /**
     * Creates a new <code>TreeWalker</code> instance.
     */
    public TreeWalker()
    {
        setFileExtensions(new String[]{"java"});
    }

    /** @param tabWidth the distance between tab stops */
    public void setTabWidth(int tabWidth)
    {
        this.tabWidth = tabWidth;
    }

    /** @param fileName the cache file */
    public void setCacheFile(String fileName)
    {
        final Configuration configuration = getConfiguration();
        cache = new PropertyCacheFile(configuration, fileName);
    }

    /** @param classLoader class loader to resolve classes with. */
    public void setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    /**
     * Sets the module factory for creating child modules (Checks).
     * @param moduleFactory the factory
     */
    public void setModuleFactory(ModuleFactory moduleFactory)
    {
        this.moduleFactory = moduleFactory;
    }

    @Override
    public void finishLocalSetup()
    {
        final DefaultContext checkContext = new DefaultContext();
        checkContext.add("classLoader", classLoader);
        checkContext.add("messages", getMessageCollector());
        checkContext.add("severity", getSeverity());
        checkContext.add("tabWidth", String.valueOf(tabWidth));

        childContext = checkContext;
    }

    @Override
    public void setupChild(Configuration childConf)
        throws CheckstyleException
    {
        final String name = childConf.getName();
        final Object module = moduleFactory.createModule(name);
        if (!(module instanceof Check)) {
            throw new CheckstyleException(
                "TreeWalker is not allowed as a parent of " + name);
        }
        final Check c = (Check) module;
        c.contextualize(childContext);
        c.configure(childConf);
        c.init();

        registerCheck(c);
    }

    @Override
    protected void processFiltered(File file, List<String> lines)
    {
        // check if already checked and passed the file
        final String fileName = file.getPath();
        final long timestamp = file.lastModified();
        if (cache.alreadyChecked(fileName, timestamp)
                 || !fileExtensionMatches(file, getFileExtensions()))
        {
            return;
        }

        final String msg = "%s occurred during the analysis of file %s .";

        try {
            final FileText text = FileText.fromLines(file, lines);
            final FileContents contents = new FileContents(text);
            final DetailAST rootAST = TreeWalker.parse(contents);

            getMessageCollector().reset();

            walk(rootAST, contents, AstState.ORDINARY);

            final DetailAST astWithComments = appendHiddenCommentNodes(rootAST);

            walk(astWithComments, contents, AstState.WITH_COMMENTS);
        }
        catch (final TokenStreamRecognitionException tre) {
            final String exceptionMsg = String.format(msg, "TokenStreamRecognitionException",
                     fileName);
            LOG.error(exceptionMsg);
            final RecognitionException re = tre.recog;
            String message = "TokenStreamRecognitionException occured";
            if (re != null) {
                message = re.getMessage();
            }
            getMessageCollector().add(createLocalizedMessage(message));
        }
        // RecognitionException and any other (need to check if needed)
        catch (Throwable ex) {
            final String exceptionMsg = String.format(msg, ex.getClass().getSimpleName(), fileName);
            LOG.error(exceptionMsg);
            getMessageCollector().add(createLocalizedMessage(ex.getMessage()));
        }

        if (getMessageCollector().size() == 0) {
            cache.checkedOk(fileName, timestamp);
        }
    }

    /**
     * Creates {@link LocalizedMessage} object using default attributes.
     * @param message
     *        message that will be used for created object
     * @return instance of created object
     */
    private LocalizedMessage createLocalizedMessage(String message)
    {
        return new LocalizedMessage(
                0,
                Defn.CHECKSTYLE_BUNDLE,
                "general.exception",
                new String[] {message },
                getId(),
                this.getClass(), null);
    }

    /**
     * Register a check for a given configuration.
     * @param check the check to register
     * @throws CheckstyleException if an error occurs
     */
    private void registerCheck(Check check)
        throws CheckstyleException
    {
        final int[] tokens;
        final Set<String> checkTokens = check.getTokenNames();
        if (!checkTokens.isEmpty()) {
            tokens = check.getRequiredTokens();

            //register configured tokens
            final int[] acceptableTokens = check.getAcceptableTokens();
            Arrays.sort(acceptableTokens);
            for (String token : checkTokens) {
                final int tokenId = TokenTypes.getTokenId(token);
                if (Arrays.binarySearch(acceptableTokens, tokenId) >= 0) {
                    registerCheck(token, check);
                }
                else {
                    throw new CheckstyleException("Token \""
                        + token + "\" was not found in Acceptable tokens list"
                                + " in check " + check);
                }
            }
        }
        else {
            tokens = check.getDefaultTokens();
        }
        for (int element : tokens) {
            registerCheck(element, check);
        }
        if (check.isCommentNodesRequired()) {
            commentChecks.add(check);
        }
        else {
            ordinaryChecks.add(check);
        }
    }

    /**
     * Register a check for a specified token id.
     * @param tokenID the id of the token
     * @param check the check to register
     */
    private void registerCheck(int tokenID, Check check)
    {
        registerCheck(TokenTypes.getTokenName(tokenID), check);
    }

    /**
     * Register a check for a specified token name
     * @param token the name of the token
     * @param check the check to register
     */
    private void registerCheck(String token, Check check)
    {
        if (check.isCommentNodesRequired()) {
            tokenToCommentChecks.put(token, check);
        }
        else if (TokenTypes.isCommentType(token)) {
            final String message = String.format("Check '%s' waits for comment type "
                    + "token ('%s') and should override 'isCommentNodesRequred()' "
                    + "method to return 'true'", check.getClass().getName(), token);
            LOG.warn(message);
        }
        else {
            tokenToOrdinaryChecks.put(token, check);
        }
    }

    /**
     * Initiates the walk of an AST.
     * @param ast the root AST
     * @param contents the contents of the file the AST was generated from.
     * @param astState state of AST.
     */
    private void walk(DetailAST ast, FileContents contents
            , AstState astState)
    {
        notifyBegin(ast, contents, astState);

        // empty files are not flagged by javac, will yield ast == null
        if (ast != null) {
            processIter(ast, astState);
        }
        notifyEnd(ast, astState);
    }

    /**
     * Notify checks that we are about to begin walking a tree.
     * @param rootAST the root of the tree.
     * @param contents the contents of the file the AST was generated from.
     * @param astState state of AST.
     */
    private void notifyBegin(DetailAST rootAST, FileContents contents
            , AstState astState)
    {
        Set<Check> checks;

        if (astState == AstState.WITH_COMMENTS) {
            checks = commentChecks;
        }
        else {
            checks = ordinaryChecks;
        }

        for (Check ch : checks) {
            ch.setFileContents(contents);
            ch.beginTree(rootAST);
        }
    }

    /**
     * Notify checks that we have finished walking a tree.
     * @param rootAST the root of the tree.
     * @param astState state of AST.
     */
    private void notifyEnd(DetailAST rootAST, AstState astState)
    {
        Set<Check> checks;

        if (astState == AstState.WITH_COMMENTS) {
            checks = commentChecks;
        }
        else {
            checks = ordinaryChecks;
        }

        for (Check ch : checks) {
            ch.finishTree(rootAST);
        }
    }

    /**
     * Notify checks that visiting a node.
     * @param ast the node to notify for.
     * @param astState state of AST.
     */
    private void notifyVisit(DetailAST ast, AstState astState)
    {
        Collection<Check> visitors;
        final String tokenType = TokenTypes.getTokenName(ast.getType());

        if (astState == AstState.WITH_COMMENTS) {
            if (!tokenToCommentChecks.containsKey(tokenType)) {
                return;
            }
            visitors = tokenToCommentChecks.get(tokenType);
        }
        else {
            if (!tokenToOrdinaryChecks.containsKey(tokenType)) {
                return;
            }
            visitors = tokenToOrdinaryChecks.get(tokenType);
        }

        for (Check c : visitors) {
            c.visitToken(ast);
        }
    }

    /**
     * Notify checks that leaving a node.
     * @param ast
     *        the node to notify for
     * @param astState state of AST.
     */
    private void notifyLeave(DetailAST ast, AstState astState)
    {
        Collection<Check> visitors;
        final String tokenType = TokenTypes.getTokenName(ast.getType());

        if (astState == AstState.WITH_COMMENTS) {
            if (!tokenToCommentChecks.containsKey(tokenType)) {
                return;
            }
            visitors = tokenToCommentChecks.get(tokenType);
        }
        else {
            if (!tokenToOrdinaryChecks.containsKey(tokenType)) {
                return;
            }
            visitors = tokenToOrdinaryChecks.get(tokenType);
        }

        for (Check ch : visitors) {
            ch.leaveToken(ast);
        }
    }

    /**
     * Static helper method to parses a Java source file.
     *
     * @param contents
     *                contains the contents of the file
     * @throws TokenStreamException
     *                 if lexing failed
     * @throws RecognitionException
     *                 if parsing failed
     * @return the root of the AST
     */
    public static DetailAST parse(FileContents contents)
        throws RecognitionException, TokenStreamException
    {
        final String fullText = contents.getText().getFullText().toString();
        final Reader sr = new StringReader(fullText);
        final GeneratedJavaLexer lexer = new GeneratedJavaLexer(sr);
        lexer.setFilename(contents.getFileName());
        lexer.setCommentListener(contents);
        lexer.setTreatAssertAsKeyword(true);
        lexer.setTreatEnumAsKeyword(true);
        lexer.setTokenObjectClass("antlr.CommonHiddenStreamToken");

        final TokenStreamHiddenTokenFilter filter =
                new TokenStreamHiddenTokenFilter(lexer);
        filter.hide(TokenTypes.SINGLE_LINE_COMMENT);
        filter.hide(TokenTypes.BLOCK_COMMENT_BEGIN);

        final GeneratedJavaRecognizer parser =
            new GeneratedJavaRecognizer(filter);
        parser.setFilename(contents.getFileName());
        parser.setASTNodeClass(DetailAST.class.getName());
        parser.compilationUnit();

        return (DetailAST) parser.getAST();
    }

    @Override
    public void destroy()
    {
        for (Check c : ordinaryChecks) {
            c.destroy();
        }
        for (Check c : commentChecks) {
            c.destroy();
        }
        cache.destroy();
        super.destroy();
    }

    /**
     * Processes a node calling interested checks at each node.
     * Uses iterative algorithm.
     * @param root the root of tree for process
     * @param astState state of AST.
     */
    private void processIter(DetailAST root, AstState astState)
    {
        DetailAST curNode = root;
        while (curNode != null) {
            notifyVisit(curNode, astState);
            DetailAST toVisit = curNode.getFirstChild();
            while (curNode != null && toVisit == null) {
                notifyLeave(curNode, astState);
                toVisit = curNode.getNextSibling();
                if (toVisit == null) {
                    curNode = curNode.getParent();
                }
            }
            curNode = toVisit;
        }
    }

    /**
     * Appends comment nodes to existing AST.
     * It traverses each node in AST, looks for hidden comment tokens
     * and appends found comment tokens as nodes in AST.
     * @param root
     *        root of AST.
     * @return root of AST with comment nodes.
     */
    private static DetailAST appendHiddenCommentNodes(DetailAST root)
    {
        DetailAST result = root;
        DetailAST curNode = root;
        DetailAST lastNode = root;

        while (curNode != null) {
            if (isPositionGreater(curNode, lastNode)) {
                lastNode = curNode;
            }

            CommonHiddenStreamToken tokenBefore = curNode.getHiddenBefore();
            DetailAST currentSibling = curNode;
            while (tokenBefore != null) { // threat multiple comments
                final DetailAST newCommentNode =
                         createCommentAstFromToken(tokenBefore);

                currentSibling.addPreviousSibling(newCommentNode);

                if (currentSibling == result) {
                    result = newCommentNode;
                }

                currentSibling = newCommentNode;
                tokenBefore = tokenBefore.getHiddenBefore();
            }

            DetailAST toVisit = curNode.getFirstChild();
            while (curNode != null && toVisit == null) {
                toVisit = curNode.getNextSibling();
                if (toVisit == null) {
                    curNode = curNode.getParent();
                }
            }
            curNode = toVisit;
        }
        if (lastNode != null) {
            CommonHiddenStreamToken tokenAfter = lastNode.getHiddenAfter();
            DetailAST currentSibling = lastNode;
            while (tokenAfter != null) {
                final DetailAST newCommentNode =
                        createCommentAstFromToken(tokenAfter);

                currentSibling.addNextSibling(newCommentNode);

                currentSibling = newCommentNode;
                tokenAfter = tokenAfter.getHiddenAfter();
            }
        }
        return result;
    }

    /**
     * Checks if position of first DetailAST is greater than position of
     * second DetailAST. Position is line number and column number in source
     * file.
     * @param ast1
     *        first DetailAST node.
     * @param ast2
     *        second DetailAST node.
     * @return true if position of ast1 is greater than position of ast2.
     */
    private static boolean isPositionGreater(DetailAST ast1, DetailAST ast2)
    {
        if (ast1.getLineNo() > ast2.getLineNo()) {
            return true;
        }
        else if (ast1.getLineNo() < ast2.getLineNo()) {
            return false;
        }
        else {
            if (ast1.getColumnNo() > ast2.getColumnNo()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create comment AST from token. Depending on token type
     * SINGLE_LINE_COMMENT or BLOCK_COMMENT_BEGIN is created.
     * @param token
     *        Token object.
     * @return DetailAST of comment node.
     */
    private static DetailAST createCommentAstFromToken(Token token)
    {
        switch (token.getType()) {
            case TokenTypes.SINGLE_LINE_COMMENT:
                return createSlCommentNode(token);
            case TokenTypes.BLOCK_COMMENT_BEGIN:
                return createBlockCommentNode(token);
            default:
                throw new IllegalArgumentException("Unknown comment type");
        }
    }

    /**
     * Create single-line comment from token.
     * @param token
     *        Token object.
     * @return DetailAST with SINGLE_LINE_COMMENT type.
     */
    private static DetailAST createSlCommentNode(Token token)
    {
        final DetailAST slComment = new DetailAST();
        slComment.setType(TokenTypes.SINGLE_LINE_COMMENT);
        slComment.setText("//");

        // column counting begins from 0
        slComment.setColumnNo(token.getColumn() - 1);
        slComment.setLineNo(token.getLine());

        final DetailAST slCommentContent = new DetailAST();
        slCommentContent.initialize(token);
        slCommentContent.setType(TokenTypes.COMMENT_CONTENT);

        // column counting begins from 0
        // plus length of '//'
        slCommentContent.setColumnNo(token.getColumn() - 1 + 2);
        slCommentContent.setLineNo(token.getLine());
        slCommentContent.setText(token.getText());

        slComment.addChild(slCommentContent);
        return slComment;
    }

    /**
     * Create block comment from token.
     * @param token
     *        Token object.
     * @return DetailAST with BLOCK_COMMENT type.
     */
    private static DetailAST createBlockCommentNode(Token token)
    {
        final DetailAST blockComment = new DetailAST();
        blockComment.initialize(TokenTypes.BLOCK_COMMENT_BEGIN, "/*");

        // column counting begins from 0
        blockComment.setColumnNo(token.getColumn() - 1);
        blockComment.setLineNo(token.getLine());

        final DetailAST blockCommentContent = new DetailAST();
        blockCommentContent.initialize(token);
        blockCommentContent.setType(TokenTypes.COMMENT_CONTENT);

        // column counting begins from 0
        // plus length of '/*'
        blockCommentContent.setColumnNo(token.getColumn() - 1 + 2);
        blockCommentContent.setLineNo(token.getLine());
        blockCommentContent.setText(token.getText());

        final DetailAST blockCommentClose = new DetailAST();
        blockCommentClose.initialize(TokenTypes.BLOCK_COMMENT_END, "*/");

        final Entry<Integer, Integer> linesColumns = countLinesColumns(
                token.getText(), token.getLine(), token.getColumn());
        blockCommentClose.setLineNo(linesColumns.getKey());
        blockCommentClose.setColumnNo(linesColumns.getValue());

        blockComment.addChild(blockCommentContent);
        blockComment.addChild(blockCommentClose);
        return blockComment;
    }

    /**
     * Count lines and columns (in last line) in text.
     * @param text
     *        String.
     * @param initialLinesCnt
     *        initial value of lines counter.
     * @param initialColumnsCnt
     *        initial value of columns counter.
     * @return entry(pair), first element is lines counter, second - columns
     *         counter.
     */
    private static Entry<Integer, Integer> countLinesColumns(
            String text, int initialLinesCnt, int initialColumnsCnt)
    {
        int lines = initialLinesCnt;
        int columns = initialColumnsCnt;
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\n':
                    lines++;
                    columns = 0;
                    break;
                default:
                    columns++;
            }
        }
        return new SimpleEntry<>(lines, columns);
    }

}
