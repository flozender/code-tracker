/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import java.lang.reflect.Field;

import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.ast.And;
import org.aspectj.weaver.ast.Call;
import org.aspectj.weaver.ast.FieldGetCall;
import org.aspectj.weaver.ast.HasAnnotation;
import org.aspectj.weaver.ast.ITestVisitor;
import org.aspectj.weaver.ast.Instanceof;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Not;
import org.aspectj.weaver.ast.Or;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.internal.tools.MatchingContextBasedTest;
import org.aspectj.weaver.reflect.ReflectionVar;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ShadowMatch;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * This class encapsulates some AspectJ internal knowledge that should be
 * pushed back into the AspectJ project in a future release.
 *
 * <p>It relies on implementation specific knowledge in AspectJ to break
 * encapsulation and do something AspectJ was not designed to do: query
 * the types of runtime tests that will be performed. The code here should
 * migrate to <code>ShadowMatch.getVariablesInvolvedInRuntimeTest()</code>
 * or some similar operation.
 *
 * <p>See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=151593"/>.
 *
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 */
class RuntimeTestWalker {

	private final Test runtimeTest;


	public RuntimeTestWalker(ShadowMatch shadowMatch) {
		ShadowMatchImpl shadowMatchImplementation = (ShadowMatchImpl) shadowMatch;
		try {
			Field testField = shadowMatchImplementation.getClass().getDeclaredField("residualTest");
			ReflectionUtils.makeAccessible(testField);
			this.runtimeTest = (Test) testField.get(shadowMatch);
		}
		catch (NoSuchFieldException noSuchFieldEx) {
			throw new IllegalStateException("The version of aspectjtools.jar / aspectjweaver.jar " +
					"on the classpath is incompatible with this version of Spring: Expected field " +
					"'runtimeTest' is not present on ShadowMatchImpl class.");
		}
		catch (IllegalAccessException illegalAccessEx) {
			// Famous last words... but I don't see how this can happen given the
			// makeAccessible call above
			throw new IllegalStateException("Unable to access ShadowMatchImpl.residualTest field");
		}
	}


	/**
	 * If the test uses any of the this, target, at_this, at_target, and at_annotation vars,
	 * then it tests subtype sensitive vars.
	 */
	public boolean testsSubtypeSensitiveVars() {
		return (this.runtimeTest != null &&
				new SubtypeSensitiveVarTypeTestVisitor().testsSubtypeSensitiveVars(this.runtimeTest));
	}

	public boolean testThisInstanceOfResidue(Class thisClass) {
		return (this.runtimeTest != null &&
				new ThisInstanceOfResidueTestVisitor(thisClass).thisInstanceOfMatches(this.runtimeTest));
	}

	public boolean testTargetInstanceOfResidue(Class targetClass) {
		return (this.runtimeTest != null &&
				new TargetInstanceOfResidueTestVisitor(targetClass).targetInstanceOfMatches(this.runtimeTest));
	}


	private static class TestVisitorAdapter implements ITestVisitor {

		protected static final int THIS_VAR = 0;
		protected static final int TARGET_VAR = 1;
		protected static final int AT_THIS_VAR = 3;
		protected static final int AT_TARGET_VAR = 4;
		protected static final int AT_ANNOTATION_VAR = 8;

		public void visit(And e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		public void visit(Or e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		public void visit(Not e) {
			e.getBody().accept(this);
		}

		public void visit(Instanceof i) {
		}

		public void visit(Literal literal) {
		}

		public void visit(Call call) {
		}

		public void visit(FieldGetCall fieldGetCall) {
		}

		public void visit(HasAnnotation hasAnnotation) {
		}

		public void visit(MatchingContextBasedTest matchingContextTest) {
		}

		protected int getVarType(ReflectionVar v) {
			try {
				Field varTypeField = ReflectionVar.class.getDeclaredField("varType");
				ReflectionUtils.makeAccessible(varTypeField);
				return (Integer) varTypeField.get(v);
			}
			catch (NoSuchFieldException noSuchFieldEx) {
				throw new IllegalStateException("the version of aspectjtools.jar / aspectjweaver.jar " +
						"on the classpath is incompatible with this version of Spring:- expected field " +
						"'varType' is not present on ReflectionVar class");
			}
			catch (IllegalAccessException illegalAccessEx) {
				// Famous last words... but I don't see how this can happen given the
				// makeAccessible call above
				throw new IllegalStateException("Unable to access ReflectionVar.varType field");
			}
		}
	}


	private static abstract class InstanceOfResidueTestVisitor extends TestVisitorAdapter {

		private Class matchClass;
		private boolean matches;
		private int matchVarType;

		public InstanceOfResidueTestVisitor(Class matchClass, boolean defaultMatches, int matchVarType) {
			this.matchClass = matchClass;
			this.matches = defaultMatches;
			this.matchVarType = matchVarType;
		}

		public boolean instanceOfMatches(Test test) {
			test.accept(this);
			return matches;
		}

		@Override
		public void visit(Instanceof i) {
			ResolvedType type = (ResolvedType) i.getType();
			int varType = getVarType((ReflectionVar) i.getVar());
			if (varType != this.matchVarType) {
				return;
			}
			try {
				Class typeClass = ClassUtils.forName(type.getName(), this.matchClass.getClassLoader());
				// Don't use ReflectionType.isAssignableFrom() as it won't be aware of (Spring) mixins
				this.matches = typeClass.isAssignableFrom(this.matchClass);
			}
			catch (ClassNotFoundException ex) {
				this.matches = false;
			}
		}
	}


	/**
	 * Check if residue of target(TYPE) kind. See SPR-3783 for more details.
	 */
	private static class TargetInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public TargetInstanceOfResidueTestVisitor(Class targetClass) {
			super(targetClass, false, TARGET_VAR);
		}

		public boolean targetInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	/**
	 * Check if residue of this(TYPE) kind. See SPR-2979 for more details.
	 */
	private static class ThisInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public ThisInstanceOfResidueTestVisitor(Class thisClass) {
			super(thisClass, true, THIS_VAR);
		}

		// TODO: Optimization: Process only if this() specifies a type and not an identifier.
		public boolean thisInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	private static class SubtypeSensitiveVarTypeTestVisitor extends TestVisitorAdapter {

		private final Object thisObj = new Object();
		private final Object targetObj = new Object();
		private final Object[] argsObjs = new Object[0];
		private boolean testsSubtypeSensitiveVars = false;

		public boolean testsSubtypeSensitiveVars(Test aTest) {
			aTest.accept(this);
			return this.testsSubtypeSensitiveVars;
		}

		@Override
		public void visit(Instanceof i) {
			ReflectionVar v = (ReflectionVar) i.getVar();
			Object varUnderTest = v.getBindingAtJoinPoint(thisObj,targetObj,argsObjs);
			if ((varUnderTest == thisObj) || (varUnderTest == targetObj)) {
				this.testsSubtypeSensitiveVars = true;
			}
		}

		@Override
		public void visit(HasAnnotation hasAnn) {
			// If you thought things were bad before, now we sink to new levels of horror...
			ReflectionVar v = (ReflectionVar) hasAnn.getVar();
			int varType = getVarType(v);
				if ((varType == AT_THIS_VAR) || (varType == AT_TARGET_VAR) || (varType == AT_ANNOTATION_VAR)) {
				this.testsSubtypeSensitiveVars = true;
			}
		}
	}

}
