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
package com.puppycrawl.tools.checkstyle.checks.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Checks the order of at-clauses.
 * </p>
 *
 * <p>
 * The check allows to configure itself by using the following properties:
 * </p>
 * <ul>
 * <li>
 * target - allows to specify targets to check at-clauses.
 * </li>
 * <li>
 * tagOrder - allows to specify the order by tags.
 * </li>
 * </ul>
 * <p>
 * Default configuration:
 * </p>
 * <pre>
 * &lt;module name=&quot;AtclauseOrderCheck&quot;&gt;
 *     &lt;property name=&quot;tagOrder&quot; value=&quot;&#64;author, &#64;version, &#64;param,
 *     &#64;return, &#64;throws, &#64;exception, &#64;see, &#64;since, &#64;serial,
 *     &#64;serialField, &#64;serialData, &#64;deprecated&quot;/&gt;
 *     &lt;property name=&quot;target&quot; value=&quot;CLASS_DEF, INTERFACE_DEF, ENUM_DEF,
 *     METHOD_DEF, CTOR_DEF, VARIABLE_DEF&quot;/&gt;
 * &lt;/module>
 * </pre>
 *
 * @author max
 *
 */
public class AtclauseOrderCheck extends AbstractJavadocCheck
{

    /**
     * Default order of atclauses.
     */
    private static final String[] DEFAULT_ORDER = {
        "@author", "@version",
        "@param", "@return",
        "@throws", "@exception",
        "@see", "@since",
        "@serial", "@serialField",
        "@serialData", "@deprecated",
    };

    /**
     * Default target of checking atclauses.
     */
    private List<Integer> target = Arrays.asList(
        TokenTypes.CLASS_DEF,
        TokenTypes.INTERFACE_DEF,
        TokenTypes.ENUM_DEF,
        TokenTypes.METHOD_DEF,
        TokenTypes.CTOR_DEF,
        TokenTypes.VARIABLE_DEF
    );

    /**
     * Order of atclauses.
     */
    private List<String> tagOrder = Arrays.asList(DEFAULT_ORDER);

    /**
     * Sets custom targets.
     * @param target user's targets.
     */
    public void setTarget(String target)
    {
        final List<Integer> customTarget = new ArrayList<>();
        for (String type : target.split(", ")) {
            customTarget.add(TokenTypes.getTokenId(type));
        }
        this.target = customTarget;
    }

    /**
     * Sets custom order of atclauses.
     * @param order user's order.
     */
    public void setTagOrder(String order)
    {
        final List<String> customOrder = new ArrayList<>();
        Collections.addAll(customOrder, order.split(", "));
        tagOrder = customOrder;
    }

    @Override
    public int[] getDefaultJavadocTokens()
    {
        return new int[] {
            JavadocTokenTypes.JAVADOC,
        };
    }

    @Override
    public void visitJavadocToken(DetailNode ast)
    {
        final int parentType = getParentType(getBlockCommentAst());

        if (target.contains(parentType)) {
            checkOrderInTagSection(ast);
        }
    }

    /**
     * Checks order of atclauses in tag section node.
     * @param javadoc Javadoc root node.
     */
    private void checkOrderInTagSection(DetailNode javadoc)
    {
        int indexOrderOfPreviousTag = 0;
        int indexOrderOfCurrentTag = 0;

        for (DetailNode node : javadoc.getChildren()) {
            if (node.getType() == JavadocTokenTypes.JAVADOC_TAG) {
                final String tagText = JavadocUtils.getFirstChild(node).getText();
                indexOrderOfCurrentTag = tagOrder.indexOf(tagText);

                if (tagOrder.contains(tagText)
                        && indexOrderOfCurrentTag < indexOrderOfPreviousTag)
                {
                    log(node.getLineNumber(), "at.clause.order", tagOrder.toString());
                }
                indexOrderOfPreviousTag = indexOrderOfCurrentTag;
            }
        }
    }

    /**
     * Returns type of parent node.
     * @param commentBlock child node.
     * @return parent type.
     */
    private int getParentType(DetailAST commentBlock)
    {
        int type = 0;
        final DetailAST parentNode = commentBlock.getParent();
        if (parentNode != null) {
            type = parentNode.getType();
            if (type == TokenTypes.TYPE || type == TokenTypes.MODIFIERS) {
                type = parentNode.getParent().getType();
            }
        }
        return type;
    }
}
