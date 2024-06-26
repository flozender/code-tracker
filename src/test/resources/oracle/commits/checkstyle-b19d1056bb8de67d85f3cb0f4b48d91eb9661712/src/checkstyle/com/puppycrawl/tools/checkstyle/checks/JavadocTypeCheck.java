package com.puppycrawl.tools.checkstyle.checks;

import org.apache.regexp.RE;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.Utils;

/**
 * Checks that a type has Javadoc comment
 *
 * @author <a href="mailto:checkstyle@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
public class JavadocTypeCheck
    extends Check
{
    /** the pattern to match author tag **/
    private static final String MATCH_JAVADOC_AUTHOR_PAT = "@author\\s+\\S";
    /** compiled regexp to match author tag **/
    private static final RE MATCH_JAVADOC_AUTHOR =
        Utils.createRE(MATCH_JAVADOC_AUTHOR_PAT);

    /** the pattern to match version tag **/
    private static final String MATCH_JAVADOC_VERSION_PAT = "@version\\s+\\S";
    /** compiled regexp to match version tag **/
    private static final RE MATCH_JAVADOC_VERSION
        = Utils.createRE(MATCH_JAVADOC_VERSION_PAT);

    /** the scope to check for */
    private Scope mScope = Scope.PRIVATE;
    /** whether to allow no author tag */
    private boolean mAllowNoAuthor = false;
    /** whether to require version tag */
    private boolean mRequireVersion = false;

    /**
     * Sets the scope to check.
     * @param aFrom string to set scope from
     */
    public void setScope(String aFrom)
    {
        mScope = Scope.getInstance(aFrom);
    }

    /**
     * Sets whether to allow no author tag
     * @param aAllowNoAuthor a <code>boolean</code> value
     */
    public void setAllowNoAuthor(boolean aAllowNoAuthor)
    {
        mAllowNoAuthor = aAllowNoAuthor;
    }

    /**
     * Sets whether to require a version tag.
     * @param aRequireVersion a <code>boolean</code> value
     */
    public void setRequireVersion(boolean aRequireVersion)
    {
        mRequireVersion = aRequireVersion;
    }


    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.INTERFACE_DEF, TokenTypes.CLASS_DEF};
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public void visitToken(DetailAST aAST)
    {
        final DetailAST mods = aAST.findFirstToken(TokenTypes.MODIFIERS);
        final Scope declaredScope = ScopeUtils.getScopeFromMods(mods);
        final Scope typeScope =
            ScopeUtils.inInterfaceBlock(aAST) ? Scope.PUBLIC : declaredScope;
        if (typeScope.isIn(mScope)) {
            final Scope surroundingScope = ScopeUtils.getSurroundingScope(aAST);
            if ((surroundingScope == null) || surroundingScope.isIn(mScope)) {
                final FileContents contents = getFileContents();
                final String[] cmt =
                    contents.getJavadocBefore(aAST.getLineNo());
                if (cmt == null) {
                    log(aAST.getLineNo(), "javadoc.missing");
                }
                else if (ScopeUtils.isOuterMostType(aAST)) {
                    // don't check author/version for inner classes
                    if (!mAllowNoAuthor
                        && (MATCH_JAVADOC_AUTHOR.grep(cmt).length == 0))
                    {
                        log(aAST.getLineNo(), "type.missingTag", "@author");
                    }

                    if (mRequireVersion
                        && (MATCH_JAVADOC_VERSION.grep(cmt).length == 0))
                    {
                        log(aAST.getLineNo(), "type.missingTag", "@version");
                    }
                }
            }
        }
    }

}
