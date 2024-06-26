package com.intellij.codeInspection.unneededThrows;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.ExceptionUtil;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefMethod;
import com.intellij.codeInspection.reference.RefVisitor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllOverridingMethodsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.containers.BidirectionalMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class RedundantThrows extends GlobalInspectionTool {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.unneededThrows.RedundantThrows");
  private static final String DISPLAY_NAME = InspectionsBundle.message("inspection.redundant.throws.display.name");
  private final BidirectionalMap<String, QuickFix> myQuickFixes = new BidirectionalMap<String, QuickFix>();
  @NonNls private static final String SHORT_NAME = "RedundantThrows";

  @Nullable
  public CommonProblemDescriptor[] checkElement(RefEntity refEntity,
                                                AnalysisScope scope,
                                                InspectionManager manager,
                                                GlobalInspectionContext globalContext,
                                                ProblemDescriptionsProcessor processor) {
    if (refEntity instanceof RefMethod) {
      final RefMethod refMethod = (RefMethod)refEntity;
      if (refMethod.isSyntheticJSP()) return null;

      if (refMethod.hasSuperMethods()) return null;

      if (refMethod.isEntry()) return null;

      PsiClass[] unThrown = refMethod.getUnThrownExceptions();
      if (unThrown == null) return null;

      PsiMethod psiMethod = (PsiMethod)refMethod.getElement();
      PsiClassType[] throwsList = psiMethod.getThrowsList().getReferencedTypes();
      PsiJavaCodeReferenceElement[] throwsRefs = psiMethod.getThrowsList().getReferenceElements();
      ArrayList<ProblemDescriptor> problems = null;

      for (int i = 0; i < throwsList.length; i++) {
        final PsiClassType throwsType = throwsList[i];
        final String throwsClassName = throwsType.getClassName();
        final PsiJavaCodeReferenceElement throwsRef = throwsRefs[i];
        if (ExceptionUtil.isUncheckedException(throwsType)) continue;
        if (declaredInRemotableMethod(psiMethod, throwsType)) continue;

        for (PsiClass s : unThrown) {
          final PsiClass throwsResolvedType = throwsType.resolve();
          if (Comparing.equal(s, throwsResolvedType)) {
            if (problems == null) problems = new ArrayList<ProblemDescriptor>(1);

            if (refMethod.isAbstract() || refMethod.getOwnerClass().isInterface()) {
              problems.add(manager.createProblemDescriptor(throwsRef, InspectionsBundle.message(
                "inspection.redundant.throws.problem.descriptor", "<code>#ref</code>"), getFix(processor, throwsClassName), ProblemHighlightType.LIKE_UNUSED_SYMBOL));
            }
            else if (!refMethod.getDerivedMethods().isEmpty()) {
              problems.add(manager.createProblemDescriptor(throwsRef, InspectionsBundle.message(
                "inspection.redundant.throws.problem.descriptor1", "<code>#ref</code>"), getFix(processor, throwsClassName), ProblemHighlightType.LIKE_UNUSED_SYMBOL));
            }
            else {
              problems.add(manager.createProblemDescriptor(throwsRef, InspectionsBundle.message(
                "inspection.redundant.throws.problem.descriptor2", "<code>#ref</code>"), getFix(processor, throwsClassName), ProblemHighlightType.LIKE_UNUSED_SYMBOL));
            }
          }
        }
      }

      if (problems != null) {
        return problems.toArray(new ProblemDescriptorImpl[problems.size()]);
      }
    }

    return null;
  }

  private static boolean declaredInRemotableMethod(final PsiMethod psiMethod, final PsiClassType throwsType) {
    if (!throwsType.equalsToText("java.rmi.RemoteException")) return false;
    PsiClass aClass = psiMethod.getContainingClass();
    if (aClass == null) return false;
    PsiClass remote = aClass.getManager().findClass("java.rmi.Remote", GlobalSearchScope.allScope(aClass.getProject()));
    return remote != null && aClass.isInheritor(remote, true);
  }


  public boolean queryExternalUsagesRequests(final InspectionManager manager,
                                             final GlobalInspectionContext globalContext,
                                             final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
    globalContext.getRefManager().iterate(new RefVisitor() {
      @Override public void visitElement(RefEntity refEntity) {
        if (problemDescriptionsProcessor.getDescriptions(refEntity) != null) {
          refEntity.accept(new RefVisitor() {
            @Override public void visitMethod(final RefMethod refMethod) {
              globalContext.enqueueDerivedMethodsProcessor(refMethod, new GlobalInspectionContextImpl.DerivedMethodsProcessor() {
                public boolean process(PsiMethod derivedMethod) {
                  problemDescriptionsProcessor.ignoreElement(refMethod);
                  return true;
                }
              });
            }
          });
        }
      }
    });

    return false;
  }

  @NotNull
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.DECLARATION_REDUNDANCY;
  }

  @NotNull
  public String getShortName() {
    return SHORT_NAME;
  }

  private LocalQuickFix getFix(final ProblemDescriptionsProcessor processor, final String hint) {
    QuickFix fix = myQuickFixes.get(hint);
    if (fix == null) {
      fix = new MyQuickFix(processor, hint);
      if (hint != null) { 
        myQuickFixes.put(hint, fix);
      }
    }
    return (LocalQuickFix)fix;
  }


  @Nullable
  public QuickFix getQuickFix(String hint) {
    return getFix(null, hint);
  }

  @Nullable
  public String getHint(final QuickFix fix) {
    final List<String> hints = myQuickFixes.getKeysByValue(fix);
    LOG.assertTrue(hints != null && hints.size() == 1);
    return hints.get(0);
  }

  private static class MyQuickFix implements LocalQuickFix {
    private ProblemDescriptionsProcessor myProcessor;
    private String myHint;

    public MyQuickFix(final ProblemDescriptionsProcessor processor, final String hint) {
      myProcessor = processor;
      myHint = hint;
    }

    @NotNull
    public String getName() {
      return InspectionsBundle.message("inspection.redundant.throws.remove.quickfix");
    }

    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      if (myProcessor != null) {
        RefElement refElement = (RefElement)myProcessor.getElement(descriptor);
        if (refElement.isValid() && refElement instanceof RefMethod) {
          RefMethod refMethod = (RefMethod)refElement;
          final ProblemDescriptor[] problems = (ProblemDescriptor[])myProcessor.getDescriptions(refMethod);
          if (problems != null) {
            removeExcessiveThrows(refMethod, null, problems);
          }
        }
      }
      else {
        final PsiMethod psiMethod = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiMethod.class);
        if (psiMethod != null) {
          removeExcessiveThrows(null, psiMethod, new ProblemDescriptor[]{descriptor});
        }
      }
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }

    private void removeExcessiveThrows(@Nullable RefMethod refMethod, @Nullable final PsiModifierListOwner element, final ProblemDescriptor[] problems) {
      try {
        @Nullable final PsiMethod psiMethod;
        if (element == null) {
          LOG.assertTrue(refMethod != null);
          psiMethod = (PsiMethod)refMethod.getElement();
        } else {
          psiMethod = (PsiMethod)element;
        }
        if (psiMethod == null) return; //invalid refMethod
        final Project project = psiMethod.getProject();
        final PsiManager psiManager = PsiManager.getInstance(project);
        final List<PsiJavaCodeReferenceElement> refsToDelete = new ArrayList<PsiJavaCodeReferenceElement>();
        for (ProblemDescriptor problem : problems) {
          final PsiElement psiElement = problem.getPsiElement();
          if (psiElement instanceof PsiJavaCodeReferenceElement) {
            final PsiJavaCodeReferenceElement classRef = (PsiJavaCodeReferenceElement)psiElement;
            final PsiType psiType = psiManager.getElementFactory().createType(classRef);
            removeException(refMethod, psiType, refsToDelete, psiMethod);
          } else {
            final PsiReferenceList throwsList = psiMethod.getThrowsList();
            final PsiClassType[] classTypes = throwsList.getReferencedTypes();
            for (PsiClassType classType : classTypes) {
              final String text = classType.getClassName();
              if (Comparing.strEqual(myHint, text)) {
                removeException(refMethod, classType, refsToDelete, psiMethod);
                break;
              }
            }
          }
        }

        for (final PsiJavaCodeReferenceElement aRefsToDelete : refsToDelete) {
          aRefsToDelete.delete();
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    private static void removeException(final RefMethod refMethod,
                                        final PsiType exceptionType,
                                        final List<PsiJavaCodeReferenceElement> refsToDelete,
                                        final PsiMethod psiMethod) {
      PsiManager psiManager = psiMethod.getManager();

      PsiJavaCodeReferenceElement[] refs = psiMethod.getThrowsList().getReferenceElements();
      for (PsiJavaCodeReferenceElement ref : refs) {
        PsiType refType = psiManager.getElementFactory().createType(ref);
        if (exceptionType.isAssignableFrom(refType)) {
          refsToDelete.add(ref);
        }
      }

      if (refMethod != null) {
        for (RefMethod refDerived : refMethod.getDerivedMethods()) {
          removeException(refDerived, exceptionType, refsToDelete, (PsiMethod)refDerived.getElement());
        }
      } else {
        final Query<Pair<PsiMethod,PsiMethod>> query = AllOverridingMethodsSearch.search(psiMethod.getContainingClass());
        query.forEach(new Processor<Pair<PsiMethod, PsiMethod>>(){
          public boolean process(final Pair<PsiMethod, PsiMethod> pair) {
            if (pair.first == psiMethod) {
              removeException(null, exceptionType, refsToDelete, pair.second);
            }
            return true;
          }
        });
      }
    }
  }
}
