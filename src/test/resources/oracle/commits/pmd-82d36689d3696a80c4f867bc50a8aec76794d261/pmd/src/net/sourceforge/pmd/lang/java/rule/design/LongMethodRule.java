/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.lang.java.rule.design;

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;

/**
 * This rule detects when a method exceeds a certain
 * threshold.  i.e. if a method has more than x lines
 * of code.
 */
public class LongMethodRule extends ExcessiveLengthRule {
    public LongMethodRule() {
        super(ASTMethodDeclaration.class);
    }
}
