package net.sourceforge.pmd.lang.java.rule.junit;

import java.util.List;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTExtendsList;
import net.sourceforge.pmd.lang.java.ast.ASTMarkerAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTResultType;
import net.sourceforge.pmd.lang.java.ast.ASTTypeParameters;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.typeresolution.TypeHelper;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
// Don't think we can otherwise here...
public abstract class AbstractJUnitRule extends AbstractJavaRule {

    public static final Class<?> junit4Class;

    public static final Class<?> junit3Class;

    private boolean isJUnit3Class;
    private boolean isJUnit4Class;

    static {
	Class<?> c;
	try {
	    c = Class.forName("org.junit.Test");
	} catch (Throwable t) {
	    c = null;
	}
	junit4Class = c;

	try {
	    c = Class.forName("junit.framework.TestCase");
	} catch (Throwable t) {
	    c = null;
	}
	junit3Class = c;
    }

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {

	isJUnit3Class = isJUnit4Class = false;

	isJUnit3Class = isJUnit3Class(node);
	if (!isJUnit3Class) {
	    isJUnit4Class = isJUnit4Class(node);
	}

	if (isJUnit3Class || isJUnit4Class) {
	    return super.visit(node, data);
	}
	return data;
    }

    public boolean isJUnitMethod(ASTMethodDeclaration method, Object data) {

	if (!method.isPublic() || method.isAbstract() || method.isNative() || method.isStatic()) {
	    return false; // skip various inapplicable method variations
	}

	if (isJUnit3Class) {
	    return isJUnit3Method(method);
	} else {
	    return isJUnit4Method(method);
	}
    }

    private boolean isJUnit4Method(ASTMethodDeclaration method) {
	return doesNodeContainJUnitAnnotation(method.jjtGetParent());
    }

    private boolean isJUnit3Method(ASTMethodDeclaration method) {
	Node node = method.jjtGetChild(0);
	if (node instanceof ASTTypeParameters) {
	    node = method.jjtGetChild(1);
	}
	return ((ASTResultType) node).isVoid() && method.getMethodName().startsWith("test");
    }

    private boolean isJUnit3Class(ASTCompilationUnit node) {
	if (node.getType() != null && TypeHelper.isA(node, junit3Class)) {
	    return true;

	} else if (node.getType() == null) {
	    ASTClassOrInterfaceDeclaration cid = node.getFirstChildOfType(ASTClassOrInterfaceDeclaration.class);
	    if (cid == null) {
		return false;
	    }
	    ASTExtendsList extendsList = cid.getFirstChildOfType(ASTExtendsList.class);
	    if (extendsList == null) {
		return false;
	    }
	    if (((ASTClassOrInterfaceType) extendsList.jjtGetChild(0)).getImage().endsWith("TestCase")) {
		return true;
	    }
	    String className = cid.getImage();
	    return className.endsWith("Test");
	}
	return false;
    }

    private boolean isJUnit4Class(ASTCompilationUnit node) {
	return doesNodeContainJUnitAnnotation(node);
    }

    private boolean doesNodeContainJUnitAnnotation(Node node) {
	List<ASTMarkerAnnotation> lstAnnotations = node.findChildrenOfType(ASTMarkerAnnotation.class);
	for (ASTMarkerAnnotation annotation : lstAnnotations) {
	    if (annotation.getType() == null) {
		ASTName name = (ASTName) annotation.jjtGetChild(0);
		if ("Test".equals(name.getImage())) {
		    return true;
		}
	    } else if (annotation.getType().equals(junit4Class)) {
		return true;
	    }
	}
	return false;
    }

}
