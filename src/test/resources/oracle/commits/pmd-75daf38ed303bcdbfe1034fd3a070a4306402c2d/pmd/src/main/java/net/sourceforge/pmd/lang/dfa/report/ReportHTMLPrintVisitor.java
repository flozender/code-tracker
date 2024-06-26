package net.sourceforge.pmd.lang.dfa.report;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author raik
 *         <p/>
 *         * Uses the generated result tree instead of the result list. The visitor
 *         * traverses the tree and creates several html files. The "package view" file
 *         * (index.html) displays an overview of packages, classes and the number of
 *         * rule violations they contain. All the other html files represent a class
 *         * and show detailed information about the violations.
 */
public class ReportHTMLPrintVisitor extends ReportVisitor {

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private StringBuilder packageBuf = new StringBuilder();
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private StringBuilder classBuf = new StringBuilder();
    private int length;
    private String baseDir;

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public ReportHTMLPrintVisitor(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Writes the buffer to file.
     */
    private void write(String filename, StringBuilder buf) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(baseDir + FILE_SEPARATOR + filename)));
        bw.write(buf.toString(), 0, buf.length());
        IOUtil.closeQuietly(bw);
    }
    
    /**
     * Generates a html table with violation information.
     */
    private String displayRuleViolation(RuleViolation vio) {
 
    	StringBuilder sb = new StringBuilder(200);
        sb.append("<table border=\"0\">");
        renderViolationRow(sb, "Rule:", vio.getRule().getName());
        renderViolationRow(sb, "Description:", vio.getDescription());

        if (StringUtil.isNotEmpty(vio.getVariableName())) {
        	renderViolationRow(sb, "Variable:", vio.getVariableName());
        }

        if (vio.getEndLine() > 0) {
        	renderViolationRow(sb, "Line:", vio.getEndLine() + " and " + vio.getBeginLine());
        } else {
        	renderViolationRow(sb, "Line:", Integer.toString(vio.getBeginLine()));
        }

        sb.append("</table>");
        return sb.toString();
    }

    // TODO - join the 21st century, include CSS attributes :)
    private void renderViolationRow(StringBuilder sb, String fieldName, String fieldData) {
    	sb.append("<tr><td><b>").append(fieldName).append("</b></td><td>").append(fieldData).append("</td></tr>");
    }
    
    /**
     * The visit method (Visitor Pattern). There are 3 types of ReportNodes:
     * RuleViolation - contains a RuleViolation, Class - represents a class and
     * contains the name of the class, Package - represents a package and
     * contains the name(s) of the package.
     */
    public void visit(AbstractReportNode node) {

        /*
         * The first node of result tree.
         */
        if (node.getParent() == null) {
            packageBuf.insert(0,
                    "<html>" +
                    " <head>" +
                    "   <title>PMD</title>" +
                    " </head>" +
                    " <body>" + PMD.EOL + 
                    "<h2>Package View</h2>" +
                    "<table border=\"1\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\">" +
                    " <tr>" + PMD.EOL + 
                    "<th>Package</th>" +
                    "<th>Class</th>" +
                    "<th>#</th>" +
                    " </tr>" + PMD.EOL);

            length = packageBuf.length();
        }


        super.visit(node);


        if (node instanceof ViolationNode) {
            ViolationNode vnode = (ViolationNode) node;
            vnode.getParent().addNumberOfViolation(1);
            RuleViolation vio = vnode.getRuleViolation();
            classBuf.append("<tr>" +
                    " <td>" + vio.getMethodName() + "</td>" +
                    " <td>" + this.displayRuleViolation(vio) + "</td>" +
                    "</tr>");
        }
        if (node instanceof ClassNode) {
            ClassNode cnode = (ClassNode) node;
            String str = cnode.getClassName();

            classBuf.insert(0,
                    "<html><head><title>PMD - " + str + "</title></head><body>" + PMD.EOL + 
                    "<h2>Class View</h2>" +
                    "<h3 align=\"center\">Class: " + str + "</h3>" +
                    "<table border=\"\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\">" +
                    " <tr>" + PMD.EOL + 
                    "<th>Method</th>" +
                    "<th>Violation</th>" +
                    " </tr>" + PMD.EOL);

            classBuf.append("</table>" +
                    " </body>" +
                    "</html>");


            try {
                write(str + ".html", classBuf);
            } catch (Exception e) {
                throw new RuntimeException("Error while writing HTML report: " + e.getMessage());
            }
            classBuf = new StringBuilder();


            packageBuf.insert(this.length,
                    "<tr>" +
                    " <td>-</td>" +
                    " <td><a href=\"" + str + ".html\">" + str + "</a></td>" +
                    " <td>" + node.getNumberOfViolations() + "</td>" +
                    "</tr>" + PMD.EOL);
            node.getParent().addNumberOfViolation(node.getNumberOfViolations());
        }
        if (node instanceof PackageNode) {
            PackageNode pnode = (PackageNode) node;
            String str;

            // rootNode
            if (node.getParent() == null) {
                str = "Aggregate";
            } else {           // all the other nodes
                str = pnode.getPackageName();
                node.getParent().addNumberOfViolation(node.getNumberOfViolations());
            }

            packageBuf.insert(length,
                    "<tr><td><b>" + str + "</b></td>" +
                    " <td>-</td>" +
                    " <td>" + node.getNumberOfViolations() + "</td>" +
                    "</tr>" + PMD.EOL);
        }
        // The first node of result tree.
        if (node.getParent() == null) {
            packageBuf.append("</table> </body></html>");
            try {
                write("index.html", packageBuf);
            } catch (Exception e) {
                throw new RuntimeException("Error while writing HTML report: " + e.getMessage());
            }
        }
    }
}
