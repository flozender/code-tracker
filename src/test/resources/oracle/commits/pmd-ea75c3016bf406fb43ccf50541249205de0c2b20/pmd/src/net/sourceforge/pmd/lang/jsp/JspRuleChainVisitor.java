package net.sourceforge.pmd.lang.jsp;

import java.util.List;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.AbstractRuleChainVisitor;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.jsp.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.jsp.ast.JspNode;
import net.sourceforge.pmd.lang.jsp.ast.JspParserVisitor;
import net.sourceforge.pmd.lang.jsp.ast.JspParserVisitorAdapter;
import net.sourceforge.pmd.rules.XPathRule;

public class JspRuleChainVisitor extends AbstractRuleChainVisitor {

    protected void indexNodes(List<Node> astCompilationUnits, RuleContext ctx) {
        JspParserVisitor jspParserVisitor = new JspParserVisitorAdapter() {
            // Perform a visitation of the AST to index nodes which need
            // visiting by type
            public Object visit(JspNode node, Object data) {
                indexNode(node);
                return super.visit(node, data);
            }
        };

        for (int i = 0; i < astCompilationUnits.size(); i++) {
            jspParserVisitor.visit((ASTCompilationUnit)astCompilationUnits.get(i), ctx);
        }
    }

    protected void visit(Rule rule, Node node, RuleContext ctx) {
        // Rule better either be a JspParserVisitor, or a XPathRule
        if (rule instanceof JspParserVisitor) {
            ((JspNode) node).jjtAccept((JspParserVisitor) rule, ctx);
        } else {
            ((XPathRule) rule).evaluate(node, ctx);
        }
    }
}
