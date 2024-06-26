package net.sourceforge.pmd;

import java.io.FileReader;
import java.util.Iterator;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.rule.XPathRule;

/**
 * To use this, do this:
 *
 * $ cat ~/tmp/Test.java
 * package foo;
 * public class Test {
 *  private int x;
 * }
 * $ java net.sourceforge.pmd.util.XPathTest -xpath "//FieldDeclaration" -filename "/home/tom/tmp/Test.java"
 * Match at line 3 column 11; package name 'foo'; variable name 'x'
 */
public class XPathCLI {

    public static void main(String[] args) throws Exception {
        String xpath;
        if (args[0].equals("-xpath")) {
            xpath = args[1];
        } else {
            xpath = args[3];
        }
        String filename;
        if (args[0].equals("-file")) {
            filename = args[1];
        } else {
            filename = args[3];
        }
        PMD pmd = new PMD();
        Rule rule = new XPathRule();
     //   rule.addProperty("xpath", xpath);
        rule.setProperty(XPathRule.XPATH_DESCRIPTOR, xpath);
        rule.setMessage("Got one!");
        RuleSet ruleSet = new RuleSet();
        ruleSet.addRule(rule);

        Report report = new Report();
        RuleContext ctx = new RuleContext();
        ctx.setReport(report);
        ctx.setSourceCodeFilename(filename);
        ctx.setLanguageVersion(Language.JAVA.getDefaultVersion());

        pmd.getSourceCodeProcessor().processSourceCode(new FileReader(filename), new RuleSets(ruleSet), ctx);

        for (Iterator<RuleViolation> i = report.iterator(); i.hasNext();) {
            RuleViolation rv = i.next();
            String res = "Match at line " + rv.getBeginLine() + " column " + rv.getBeginColumn();
            if (rv.getPackageName() != null && !rv.getPackageName().equals("")) {
                res += "; package name '" + rv.getPackageName() + "'";
            }
            if (rv.getMethodName() != null && !rv.getMethodName().equals("")) {
                res += "; method name '" + rv.getMethodName() + "'";
            }
            if (rv.getVariableName() != null && !rv.getVariableName().equals("")) {
                res += "; variable name '" + rv.getVariableName() + "'";
            }
            System.out.println(res);
        }
    }
}
