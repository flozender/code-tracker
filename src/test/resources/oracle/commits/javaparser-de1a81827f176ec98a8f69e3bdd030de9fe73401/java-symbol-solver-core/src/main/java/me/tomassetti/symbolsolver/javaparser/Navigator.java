package me.tomassetti.symbolsolver.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;

import java.util.Optional;

/**
 * This class can be used to easily retrieve nodes from a JavaParser AST.
 */
public final class Navigator {

    private Navigator() {
        // prevent instantiation
    }

    private static String getOuterTypeName(String qualifiedName) {
        return qualifiedName.split("\\.", 2)[0];
    }
    
    private static String getInnerTypeName(String qualifiedName) {
        if (qualifiedName.contains(".")) {
            return qualifiedName.split("\\.", 2)[1];
        }
        return "";
    }
    
    public static Optional<TypeDeclaration> findType(CompilationUnit cu, String qualifiedName) {
        if (cu.getTypes() == null) {
            return Optional.empty();
        }
        
        final String typeName = getOuterTypeName(qualifiedName);
        Optional<TypeDeclaration> type = cu.getTypes().stream().filter((t) -> t.getName().equals(typeName)).findFirst();

        final String innerTypeName = getInnerTypeName(qualifiedName);
        if (type.isPresent() && !innerTypeName.isEmpty()) {
            return findType(type.get(), innerTypeName);
        } 
        return type;
    }
    
    public static Optional<TypeDeclaration> findType(TypeDeclaration td, String qualifiedName) {
        final String typeName = getOuterTypeName(qualifiedName);
        
        Optional<TypeDeclaration> type = Optional.empty();
        for (Node n: td.getChildrenNodes()) {
            if (n instanceof TypeDeclaration && ((TypeDeclaration)n).getName().equals(typeName)) {
                type = Optional.of((TypeDeclaration)n);
                break;
            }
        }
        final String innerTypeName = getInnerTypeName(qualifiedName);
        if (type.isPresent() && !innerTypeName.isEmpty()) {
            return findType(type.get(), innerTypeName);
        } 
        return type;
    }
    

    public static ClassOrInterfaceDeclaration demandClass(CompilationUnit cu, String qualifiedName) {
        ClassOrInterfaceDeclaration cd = demandClassOrInterface(cu, qualifiedName);
        if (cd.isInterface()) {
            throw new IllegalStateException("Type is not a class");
        }
        return cd;
    }

    public static EnumDeclaration demandEnum(CompilationUnit cu, String qualifiedName) {
        Optional<TypeDeclaration> res = findType(cu, qualifiedName);
        if (!res.isPresent()) {
            throw new IllegalStateException("No type found");
        }
        if (!(res.get() instanceof EnumDeclaration)) {
            throw new IllegalStateException("Type is not an enum");
        }
        return (EnumDeclaration) res.get();
    }

    public static MethodDeclaration demandMethod(ClassOrInterfaceDeclaration cd, String name) {
        MethodDeclaration found = null;
        for (BodyDeclaration bd : cd.getMembers()) {
            if (bd instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) bd;
                if (md.getName().equals(name)) {
                    if (found != null) {
                        throw new IllegalStateException("Ambiguous getName");
                    }
                    found = md;
                }
            }
        }
        if (found == null) {
            throw new IllegalStateException("No method with given name");
        }
        return found;
    }

    public static VariableDeclarator demandField(ClassOrInterfaceDeclaration cd, String name) {
        for (BodyDeclaration bd : cd.getMembers()) {
            if (bd instanceof FieldDeclaration) {
                FieldDeclaration fd = (FieldDeclaration) bd;
                for (VariableDeclarator vd : fd.getVariables()) {
                    if (vd.getId().getName().equals(name)) {
                        return vd;
                    }
                }
            }
        }
        throw new IllegalStateException("No field with given name");
    }

    public static NameExpr findNameExpression(Node node, String name) {
        if (node instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) node;
            if (nameExpr.getName().equals(name)) {
                return nameExpr;
            }
        }
        for (Node child : node.getChildrenNodes()) {
            NameExpr res = findNameExpression(child, name);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public static MethodCallExpr findMethodCall(Node node, String methodName) {
        if (node instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            if (methodCallExpr.getName().equals(methodName)) {
                return methodCallExpr;
            }
        }
        for (Node child : node.getChildrenNodes()) {
            MethodCallExpr res = findMethodCall(child, methodName);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public static VariableDeclarator demandVariableDeclaration(Node node, String name) {
        if (node instanceof VariableDeclarator) {
            VariableDeclarator variableDeclarator = (VariableDeclarator) node;
            if (variableDeclarator.getId().getName().equals(name)) {
                return variableDeclarator;
            }
        }
        for (Node child : node.getChildrenNodes()) {
            VariableDeclarator res = demandVariableDeclaration(child, name);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public static ClassOrInterfaceDeclaration demandClassOrInterface(CompilationUnit compilationUnit, String qualifiedName) {
        Optional<TypeDeclaration> res = findType(compilationUnit, qualifiedName);
        if (!res.isPresent()) {
            throw new IllegalStateException("No type named '" + qualifiedName + "'found");
        }
        if (!(res.get() instanceof ClassOrInterfaceDeclaration)) {
            throw new IllegalStateException("Type is not a class or an interface, it is " + res.get().getClass().getCanonicalName());
        }
        ClassOrInterfaceDeclaration cd = (ClassOrInterfaceDeclaration) res.get();
        return cd;
    }

    public static SwitchStmt findSwitch(Node node) {
        SwitchStmt res = findSwitchHelper(node);
        if (res == null) {
            throw new IllegalArgumentException();
        } else {
            return res;
        }
    }

    private static SwitchStmt findSwitchHelper(Node node) {
        if (node instanceof SwitchStmt) {
            return (SwitchStmt) node;
        }
        for (Node child : node.getChildrenNodes()) {
            SwitchStmt resChild = findSwitchHelper(child);
            if (resChild != null) {
                return resChild;
            }
        }
        return null;
    }

    private static <N> N findNodeOfGivenClasshHelper(Node node, Class<N> clazz) {
        if (clazz.isInstance(node)) {
            return clazz.cast(node);
        }
        for (Node child : node.getChildrenNodes()) {
            N resChild = findNodeOfGivenClasshHelper(child, clazz);
            if (resChild != null) {
                return resChild;
            }
        }
        return null;
    }

    public static <N> N findNodeOfGivenClass(Node node, Class<N> clazz) {
        N res = findNodeOfGivenClasshHelper(node, clazz);
        if (res == null) {
            throw new IllegalArgumentException();
        } else {
            return res;
        }
    }

    public static ReturnStmt findReturnStmt(MethodDeclaration method) {
        return findNodeOfGivenClass(method, ReturnStmt.class);
    }
}
