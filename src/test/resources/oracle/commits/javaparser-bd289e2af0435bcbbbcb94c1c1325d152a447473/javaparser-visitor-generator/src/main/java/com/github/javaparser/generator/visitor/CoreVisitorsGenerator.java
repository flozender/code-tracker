package com.github.javaparser.generator.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.generator.utils.SourceRoot;
import com.github.javaparser.metamodel.JavaParserMetaModel;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CoreVisitorsGenerator {
    private static JavaParserMetaModel javaParserMetaModel = new JavaParserMetaModel();

    public static void main(String[] args) throws IOException {
        final JavaParserMetaModel javaParserMetaModel = new JavaParserMetaModel();

        final Path root = Paths.get(VisitorGenerator.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "..", "..", "..", "javaparser-core", "src", "main", "java");

        final JavaParser javaParser = new JavaParser();

        final SourceRoot sourceRoot = new SourceRoot(root);

        new VoidVisitorGenerator(javaParser, sourceRoot, javaParserMetaModel).generate();
        new HashCodeVisitorGenerator(javaParser, sourceRoot, javaParserMetaModel).generate();

        sourceRoot.saveAll();
    }
}
