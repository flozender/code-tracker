package com.intellij.execution.junit;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.*;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.execution.testframework.SourceScope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.containers.Convertor;
import gnu.trove.THashSet;
import junit.runner.BaseTestRunner;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.junit.runners.Parameterized;

import java.util.*;

public class JUnitUtil {
  @NonNls private static final String TESTCASE_CLASS = "junit.framework.TestCase";
  @NonNls private static final String TEST_INTERFACE = "junit.framework.Test";
  @NonNls private static final String TESTSUITE_CLASS = "junit.framework.TestSuite";
  @NonNls public static final String RUN_WITH = "org.junit.runner.RunWith";

  public static boolean isSuiteMethod(final PsiMethod psiMethod) {
    if (psiMethod == null) return false;
    if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) return false;
    if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) return false;
    if (psiMethod.isConstructor()) return false;
    final PsiType returnType = psiMethod.getReturnType();
    if (returnType != null) {
      if (!returnType.equalsToText(TEST_INTERFACE) && !returnType.equalsToText(TESTSUITE_CLASS)) {
        final PsiType testType =
          JavaPsiFacade.getInstance(psiMethod.getProject()).getElementFactory().createTypeFromText(TEST_INTERFACE, null);
        if (!TypeConversionUtil.isAssignable(testType, returnType)) {
          return false;
        }
      }
    }
    final PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
    return parameters.length == 0;
  }

  public static boolean isTestMethod(final Location<? extends PsiMethod> location) {
    final PsiMethod psiMethod = location.getPsiElement();
    final PsiClass aClass = location instanceof MethodLocation ? ((MethodLocation)location).getContainingClass() : psiMethod.getContainingClass();
    if (aClass == null || !isTestClass(aClass)) return false;
    if (isTestAnnotated(psiMethod)) return true;
    if (psiMethod.isConstructor()) return false;
    if (!psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) return false;
    if (psiMethod.hasModifierProperty(PsiModifier.ABSTRACT)) return false;
    if (AnnotationUtil.isAnnotated(aClass, RUN_WITH, true)) return true;
    if (psiMethod.getParameterList().getParametersCount() > 0) return false;
    if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && BaseTestRunner.SUITE_METHODNAME.equals(psiMethod.getName())) return false;
    if (!psiMethod.getName().startsWith("test")) return false;
    PsiClass testCaseClass = getTestCaseClassOrNull(location);
    return testCaseClass != null && psiMethod.getContainingClass().isInheritor(testCaseClass, true);
  }

  private static boolean isTestCaseInheritor(final PsiClass aClass) {
    if (!aClass.isValid()) return false;
    Location<PsiClass> location = PsiLocation.fromPsiElement(aClass);
    PsiClass testCaseClass = getTestCaseClassOrNull(location);
    return testCaseClass != null && aClass.isInheritor(testCaseClass, true);
  }

  /**
   *
   * @param aClassLocation
   * @return true iff aClassLocation can be used as JUnit test class.
   */
  private static boolean isTestClass(final Location<? extends PsiClass> aClassLocation) {
    return isTestClass(aClassLocation.getPsiElement());
  }

  public static boolean isTestClass(final PsiClass psiClass) {
    return isTestClass(psiClass, true, null, true);
  }
  private static boolean isTestClass(final PsiClass psiClass, boolean checkAbstract, @Nullable Set<PsiClass> visited, boolean checkForTestCaseInheritance) {
    if (!PsiClassUtil.isRunnableClass(psiClass, true, checkAbstract)) return false;
    if (checkForTestCaseInheritance && isTestCaseInheritor(psiClass)) return true;
    final PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) return false;
    if (AnnotationUtil.isAnnotated(psiClass, RUN_WITH, true)) return true;

    for (final PsiMethod method : psiClass.getMethods()) {
      if (isSuiteMethod(method)) return true;
      if (isTestAnnotated(method)) return true;
    }

    PsiClass superClass = psiClass.getSuperClass();
    if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName()) && !superClass.isInterface()) {
      if (visited != null && visited.contains(psiClass)) return false;
      if (visited == null) visited = new THashSet<PsiClass>();
      visited.add(psiClass);
      return isTestClass(superClass, false, visited, false);
    }
    return false;
  }

  public static boolean isJUnit3TestClass(final PsiClass clazz) {
    return isTestCaseInheritor(clazz);
  }

  public static boolean isJUnit4TestClass(final PsiClass psiClass) {
    return isJUnit4TestClass(psiClass, true,null);
  }
  private static boolean isJUnit4TestClass(final PsiClass psiClass, boolean checkAbstract, @Nullable Set<PsiClass> visited) {
    if (!PsiClassUtil.isRunnableClass(psiClass, true, checkAbstract)) return false;

    final PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) return false;
    if (AnnotationUtil.isAnnotated(psiClass, RUN_WITH, true)) return true;
    for (final PsiMethod method : psiClass.getMethods()) {
      if (isTestAnnotated(method)) return true;
    }
    PsiClass superClass = psiClass.getSuperClass();
    if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName()) && !superClass.isInterface()) {
      if (visited != null && visited.contains(psiClass)) return false;
      if (visited == null) visited = new THashSet<PsiClass>();
      visited.add(psiClass);
      return isJUnit4TestClass(superClass, false, visited);
    }
    return false;
  }

  public static boolean isTestAnnotated(final PsiMethod method) {
    if (AnnotationUtil.isAnnotated(method, "org.junit.Test", false)) {
      final PsiAnnotation annotation = AnnotationUtil.findAnnotationInHierarchy(method.getContainingClass(), Collections.singleton(RUN_WITH));
      if (annotation != null) {
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
          final PsiAnnotationMemberValue value = attribute.getValue();
          if (value instanceof PsiClassObjectAccessExpression ) {
            final PsiTypeElement typeElement = ((PsiClassObjectAccessExpression)value).getOperand();
            if (typeElement.getType().getCanonicalText().equals(Parameterized.class.getName())) {
              return false;
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  private static PsiClass getTestCaseClassOrNull(final Location<?> location) {
    final Location<PsiClass> ancestorOrSelf = location.getAncestorOrSelf(PsiClass.class);
    final PsiClass aClass = ancestorOrSelf.getPsiElement();
    Module module = JavaExecutionUtil.findModule(aClass);
    if (module == null) return null;
    GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, true);
    return getTestCaseClassOrNull(scope, module.getProject());
  }

  public static PsiClass getTestCaseClass(final Module module) throws NoJUnitException {
    if (module == null) throw new NoJUnitException();
    final GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, true);
    return getTestCaseClass(scope, module.getProject());
  }

  public static PsiClass getTestCaseClass(final SourceScope scope) throws NoJUnitException {
    if (scope == null) throw new NoJUnitException();
    return getTestCaseClass(scope.getLibrariesScope(), scope.getProject());
  }

  private static PsiClass getTestCaseClass(final GlobalSearchScope scope, final Project project) throws NoJUnitException {
    PsiClass testCaseClass = getTestCaseClassOrNull(scope, project);
    if (testCaseClass == null) throw new NoJUnitException(scope.getDisplayName());
    return testCaseClass;
  }
  private static PsiClass getTestCaseClassOrNull(final GlobalSearchScope scope, final Project project) {
    return JavaPsiFacade.getInstance(project).findClass(TESTCASE_CLASS, scope);
  }

  public static class  TestMethodFilter implements Condition<PsiMethod> {
    private final PsiClass myClass;

    public TestMethodFilter(final PsiClass aClass) {
      myClass = aClass;
    }

    public boolean value(final PsiMethod method) {
      return isTestMethod(MethodLocation.elementInClass(method, myClass));
    }
  }

  public static PsiClass findPsiClass(final String qualifiedName, final Module module, final Project project) {
    final GlobalSearchScope scope = module == null ? GlobalSearchScope.projectScope(project) : GlobalSearchScope.moduleWithDependenciesScope(module);
    return JavaPsiFacade.getInstance(project).findClass(qualifiedName, scope);
  }

  public static PsiPackage getContainingPackage(final PsiClass psiClass) {
    return JavaDirectoryService.getInstance().getPackage(psiClass.getContainingFile().getContainingDirectory());
  }

  public static PsiClass getTestClass(final PsiElement element) {
    return getTestClass(PsiLocation.fromPsiElement(element));
  }

  public static PsiClass getTestClass(final Location<?> location) {
    for (Iterator<Location<PsiClass>> iterator = location.getAncestors(PsiClass.class, false); iterator.hasNext();) {
      final Location<PsiClass> classLocation = iterator.next();
      if (isTestClass(classLocation)) return classLocation.getPsiElement();
    }
    PsiElement element = location.getPsiElement();
    if (element instanceof PsiJavaFile) {
      PsiClass[] classes = ((PsiJavaFile)element).getClasses();
      if (classes.length == 1) return classes[0];
    }
    return null;
  }

  public static PsiMethod getTestMethod(final PsiElement element) {
    final PsiManager manager = element.getManager();
    final Location<PsiElement> location = PsiLocation.fromPsiElement(manager.getProject(), element);
    for (Iterator<Location<PsiMethod>> iterator = location.getAncestors(PsiMethod.class, false); iterator.hasNext();) {
      final Location<? extends PsiMethod> methodLocation = iterator.next();
      if (isTestMethod(methodLocation)) return methodLocation.getPsiElement();
    }
    return null;
  }

  /**
   * @param collection
   * @param comparator returns 0 iff elemets are incomparable.
   * @return maximum elements
   */
  public static <T> Collection<T> findMaximums(final Collection<T> collection, final Comparator<T> comparator) {
    final ArrayList<T> maximums = new ArrayList<T>();
    loop:
    for (final T candidate : collection) {
      for (final T element : collection) {
        if (comparator.compare(element, candidate) > 0) continue loop;
      }
      maximums.add(candidate);
    }
    return maximums;
  }

  /*public static Map<Module, Collection<Module>> buildAllDependencies(final Project project) {
    final Module[] modules = ModuleManager.getInstance(project).getSortedModules();
    final HashMap<Module, Collection<Module>> lessers = new HashMap<Module, Collection<Module>>();
    int prevProcessedCount = 0;
    while (modules.length > lessers.size()) {
      for (int i = 0; i < modules.length; i++) {
        final Module module = modules[i];
        if (lessers.containsKey(module)) continue;
        final Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
        if (lessers.keySet().containsAll(Arrays.asList(dependencies))) {
          final HashSet<Module> allDependencies = new HashSet<Module>();
          for (int j = 0; j < dependencies.length; j++) {
            final Module dependency = dependencies[j];
            allDependencies.add(dependency);
            allDependencies.addAll(lessers.get(dependency));
          }
          lessers.put(module, allDependencies);
        }
      }
      if (lessers.size() == prevProcessedCount) return null;
      prevProcessedCount = lessers.size();
    }
    return lessers;
  }*/

  public static class ModuleOfClass implements Convertor<PsiClass, Module> {
    public Module convert(final PsiClass psiClass) {
      if (psiClass == null || !psiClass.isValid()) return null;
      return ModuleUtil.findModuleForPsiElement(psiClass);
    }
  }

  public static class NoJUnitException extends CantRunException {
    public NoJUnitException() {
      super(ExecutionBundle.message("no.junit.error.message"));
    }

    public NoJUnitException(final String message) {
      super(ExecutionBundle.message("no.junit.in.scope.error.message", message));
    }
  }
}
