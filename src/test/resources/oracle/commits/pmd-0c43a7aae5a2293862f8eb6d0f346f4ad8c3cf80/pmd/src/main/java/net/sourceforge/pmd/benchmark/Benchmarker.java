/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.benchmark;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.SourceCodeProcessor;
import net.sourceforge.pmd.lang.*;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.util.FileUtil;
import net.sourceforge.pmd.util.StringUtil;
import net.sourceforge.pmd.util.datasource.DataSource;

import org.apache.commons.io.IOUtils;

/**
 *
 *
 */
public class Benchmarker {

    /**
     * @param args String[]
     * @param name String
     * @return boolean
     */
    private static boolean findBooleanSwitch(String[] args, String name) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param args String[]
     * @param name String
     * @param defaultValue String
     * @return String
     */
    private static String findOptionalStringValue(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    /**
     *
     * @param args String[]
     * @throws RuleSetNotFoundException
     * @throws IOException
     * @throws PMDException
     */
    public static void main(String[] args) throws RuleSetNotFoundException, IOException, PMDException {

        String targetjdk = findOptionalStringValue(args, "--targetjdk", "1.4");
        LanguageModule language = LanguageRegistry.getLanguage(JavaLanguageModule.NAME);
        LanguageVersionModule languageVersion = language.getVersion(targetjdk);
        if (languageVersion == null) {
        	languageVersion = language.getDefaultVersion();
        }

        String srcDir = findOptionalStringValue(args, "--source-directory", "/usr/local/java/src/java/lang/");
        List<DataSource> dataSources = FileUtil.collectFiles(srcDir, new LanguageFilenameFilter(language));

        boolean debug = findBooleanSwitch(args, "--debug");
        boolean parseOnly = findBooleanSwitch(args, "--parse-only");

        if (debug) {
            System.out.println("Using " +language.getName() + " " + languageVersion.getVersion());
        }
        if (parseOnly) {
        	Parser parser = PMD.parserFor(languageVersion, null);
            parseStress(parser, dataSources, debug);
        } else {
            String ruleset = findOptionalStringValue(args, "--ruleset", "");
            if (debug) {
        		System.out.println("Checking directory " + srcDir);
            }
            Set<RuleDuration> results = new TreeSet<RuleDuration>();
            RuleSetFactory factory = new RuleSetFactory();
            if (StringUtil.isNotEmpty(ruleset)) {
                stress(languageVersion, factory.createRuleSet(ruleset), dataSources, results, debug);
            } else {
                Iterator<RuleSet> i = factory.getRegisteredRuleSets();
                while (i.hasNext()) {
                    stress(languageVersion, i.next(), dataSources, results, debug);
                }
            }

            TextReport report = new TextReport();
			report.generate(results, System.err);
        }
    }

    /**
     * @param parser Parser
     * @param dataSources List<DataSource>
     * @param debug boolean
     * @throws IOException
     */
    private static void parseStress(Parser parser, List<DataSource> dataSources, boolean debug) throws IOException {

        long start = System.currentTimeMillis();

        for (DataSource dataSource: dataSources) {
            parser.parse(
            	dataSource.getNiceFileName(false, null),
            	new InputStreamReader(dataSource.getInputStream()
            	)
            );
        }

        if (debug) {
        	long end = System.currentTimeMillis();
        	long elapsed = end - start;
        	System.out.println("That took " + elapsed + " ms");
        }
    }

    /**
     * @param languageVersion LanguageVersion
     * @param ruleSet RuleSet
     * @param dataSources List<DataSource>
     * @param results Set<RuleDuration>
     * @param debug boolean
     * @throws PMDException
     * @throws IOException
     */
    private static void stress(LanguageVersionModule languageVersion, RuleSet ruleSet, List<DataSource> dataSources, Set<RuleDuration> results, boolean debug) throws PMDException, IOException {

        for (Rule rule: ruleSet.getRules()) {
            if (debug) {
            	System.out.println("Starting " + rule.getName());
            }

            RuleSet working = new RuleSet();
            working.addRule(rule);
            RuleSets ruleSets = new RuleSets(working);

            PMDConfiguration config = new PMDConfiguration();
            config.setDefaultLanguageVersion(languageVersion);

            RuleContext ctx = new RuleContext();
            long start = System.currentTimeMillis();
            Reader reader = null;
            for (DataSource dataSource: dataSources) {
            	reader = new InputStreamReader(dataSource.getInputStream());
            	ctx.setSourceCodeFilename(dataSource.getNiceFileName(false, null));
            	new SourceCodeProcessor(config).processSourceCode(reader, ruleSets, ctx);
            	IOUtils.closeQuietly(reader);
            	}
            long end = System.currentTimeMillis();
            long elapsed = end - start;
            results.add(new RuleDuration(elapsed, rule));
            if (debug) {
            	System.out.println("Done timing " + rule.getName() + "; elapsed time was " + elapsed);
            }
        }
    }

    private static final Map<String, BenchmarkResult> BenchmarksByName = new HashMap<String, BenchmarkResult>();

    /**
     * @param type Benchmark
     * @param time long
     * @param count long
     */
    public static void mark(Benchmark type, long time, long count) {
        mark(type, null, time, count);
    }

    /**
     *
     * @param type Benchmark
     * @param name String
     * @param time long
     * @param count long
     */
    public synchronized static void mark(Benchmark type, String name, long time, long count) {
        String typeName = type.name;
        if (typeName != null && name != null) {
            throw new IllegalArgumentException("Name cannot be given for type: " + type);
        } else if (typeName == null && name == null) {
            throw new IllegalArgumentException("Name is required for type: " + type);
        } else if (typeName == null) {
            typeName = name;
        }
        BenchmarkResult benchmarkResult = BenchmarksByName.get(typeName);
        if (benchmarkResult == null) {
            benchmarkResult = new BenchmarkResult(type, typeName);
            BenchmarksByName.put(typeName, benchmarkResult);
        }
        benchmarkResult.update(time, count);
    }

    public static void reset() {
        BenchmarksByName.clear();
    }

    /**
     *
     * @return Map<String,BenchmarkResult>
     */
    public static Map<String, BenchmarkResult> values() {
    	return BenchmarksByName;
    }
}
