package com.intellij.refactoring.util.duplicates;

import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.controlFlow.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author dsl
 */
public final class Match {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.util.duplicates.Match");
  private final PsiElement myMatchStart;
  private final PsiElement myMatchEnd;
  private final Map<PsiVariable, List<PsiElement>> myParameterValues = new HashMap<PsiVariable, List<PsiElement>>();
  private final Map<PsiVariable, ArrayList<PsiElement>> myParameterOccurences = new HashMap<PsiVariable, ArrayList<PsiElement>>();
  private final Map<PsiElement, PsiElement> myDeclarationCorrespondence = new HashMap<PsiElement, PsiElement>();
  private ReturnValue myReturnValue = null;
  private Ref<PsiExpression> myInstanceExpression = null;
  private final Map<PsiVariable, PsiType> myChangedParams = new HashMap<PsiVariable, PsiType>();

  Match(PsiElement start, PsiElement end) {
    LOG.assertTrue(start.getParent() == end.getParent());
    myMatchStart = start;
    myMatchEnd = end;
  }


  public PsiElement getMatchStart() {
    return myMatchStart;
  }

  public PsiElement getMatchEnd() {
    return myMatchEnd;
  }

  public List<PsiElement> getParameterValues(PsiVariable parameter) {
    return myParameterValues.get(parameter);
  }

  /**
   * Returns either local variable declaration or expression
   * @param outputParameter
   * @return
   */
  public ReturnValue getOutputVariableValue(PsiVariable outputParameter) {
    final PsiElement decl = myDeclarationCorrespondence.get(outputParameter);
    if (decl instanceof PsiVariable) {
      return new VariableReturnValue((PsiVariable)decl);
    }
    final List<PsiElement> parameterValue = getParameterValues(outputParameter);
    if (parameterValue != null && parameterValue.size() == 1 && parameterValue.get(0) instanceof PsiExpression) {
      return new ExpressionReturnValue((PsiExpression) parameterValue.get(0));
    }
    else {
      return null;
    }
  }


  boolean putParameter(Pair<PsiVariable, PsiType> parameter, PsiElement value) {
    final PsiVariable psiVariable = parameter.first;

    if (myDeclarationCorrespondence.get(psiVariable) == null) {
      final boolean [] valueDependsOnReplacedScope = new boolean[1];
      value.accept(new JavaRecursiveElementWalkingVisitor() {
        @Override
        public void visitReferenceExpression(final PsiReferenceExpression expression) {
          super.visitReferenceExpression(expression);
          final PsiElement resolved = expression.resolve();
          if (resolved != null && Comparing.equal(resolved.getContainingFile(), getMatchEnd().getContainingFile())) {
            final TextRange range = resolved.getTextRange();
            if (getMatchStart().getTextOffset() <= range.getStartOffset() &&
                range.getEndOffset() <= getMatchEnd().getTextRange().getEndOffset()) {
              valueDependsOnReplacedScope[0] = true;
            }
          }
        }
      });
      if (valueDependsOnReplacedScope[0]) return false;
    }

    final List<PsiElement> currentValue = myParameterValues.get(psiVariable);
    final boolean isVararg = psiVariable instanceof PsiParameter && ((PsiParameter)psiVariable).isVarArgs();
    if (!(value instanceof PsiExpression)) return false;
    final PsiType type = ((PsiExpression)value).getType();
    final PsiType parameterType = parameter.second;
    if (type == null) return false;
    if (currentValue == null) {
      if (parameterType instanceof PsiClassType && ((PsiClassType)parameterType).resolve() instanceof PsiTypeParameter) {
        final PsiTypeParameter typeParameter = (PsiTypeParameter)((PsiClassType)parameterType).resolve();
        LOG.assertTrue(typeParameter != null);
        for (PsiClassType classType : typeParameter.getExtendsListTypes()) {
          if (!classType.isAssignableFrom(type)) return false;
        }
      }
      else {
        if (isVararg) {
          if (!((PsiEllipsisType)psiVariable.getType()).getComponentType().isAssignableFrom(type) && !((PsiEllipsisType)psiVariable.getType()).toArrayType().equals(type)) {
            myChangedParams.put(psiVariable, new PsiEllipsisType(parameterType));
          }
        } else {
          if (!parameterType.isAssignableFrom(type)) return false;
          if (!psiVariable.getType().isAssignableFrom(type)) {
            myChangedParams.put(psiVariable, parameterType);
          }
        }
      }
      final List<PsiElement> values = new ArrayList<PsiElement>();
      values.add(value);
      myParameterValues.put(psiVariable, values);
      final ArrayList<PsiElement> elements = new ArrayList<PsiElement>();
      myParameterOccurences.put(psiVariable, elements);
      return true;
    }
    else {
      for (PsiElement val : currentValue) {
        if (!isVararg && !PsiEquivalenceUtil.areElementsEquivalent(val, value)) {
          return false;
        }
      }
      if (isVararg) {
        if (!parameterType.isAssignableFrom(type)) return false;
        currentValue.add(value);
      }
      myParameterOccurences.get(psiVariable).add(value);
      return true;
    }
  }

  public ReturnValue getReturnValue() {
    return myReturnValue;
  }

  boolean registerReturnValue(ReturnValue returnValue) {
    if (myReturnValue == null) {
      myReturnValue = returnValue;
      return true;
    }
    else {
      return myReturnValue.isEquivalent(returnValue);
    }
  }

  boolean registerInstanceExpression(PsiExpression instanceExpression, final PsiClass contextClass) {
    if (myInstanceExpression == null) {
      if (instanceExpression != null) {
        final PsiType type = instanceExpression.getType();
        if (!(type instanceof PsiClassType)) return false;
        final PsiClass hisClass = ((PsiClassType) type).resolve();
        if (hisClass == null || !InheritanceUtil.isInheritorOrSelf(hisClass, contextClass, true)) return false;
      }
      myInstanceExpression = Ref.create(instanceExpression);
      return true;
    }
    else {
      if (myInstanceExpression.get() == null) {
        myInstanceExpression.set(instanceExpression);

        return instanceExpression == null;
      }
      else {
        if (instanceExpression != null) {
          return PsiEquivalenceUtil.areElementsEquivalent(instanceExpression, myInstanceExpression.get());
        }
        else {
          return myInstanceExpression.get() == null || myInstanceExpression.get() instanceof PsiThisExpression;
        }
      }
    }
  }

  boolean putDeclarationCorrespondence(PsiElement patternDeclaration, @NotNull PsiElement matchDeclaration) {
    PsiElement originalValue = myDeclarationCorrespondence.get(patternDeclaration);
    if (originalValue == null) {
      myDeclarationCorrespondence.put(patternDeclaration, matchDeclaration);
      return true;
    }
    else {
      return originalValue == matchDeclaration;
    }
  }

  private PsiElement replaceWith(final PsiStatement statement) throws IncorrectOperationException {
    final PsiElement matchStart = getMatchStart();
    final PsiElement matchEnd = getMatchEnd();
    final PsiElement element = matchStart.getParent().addBefore(statement, matchStart);
    matchStart.getParent().deleteChildRange(matchStart, matchEnd);
    return element;
  }

  public PsiElement replaceByStatement(final PsiMethod extractedMethod, final PsiMethodCallExpression methodCallExpression, final PsiVariable outputVariable) throws IncorrectOperationException {
    PsiStatement statement = null;
    if (outputVariable != null) {
      ReturnValue returnValue = getOutputVariableValue(outputVariable);
      if (returnValue == null && outputVariable instanceof PsiField) {
        returnValue = new FieldReturnValue((PsiField)outputVariable);
      }
      if (returnValue == null) return null;
      statement = returnValue.createReplacement(extractedMethod, methodCallExpression);
    }
    else if (getReturnValue() != null) {
      statement = getReturnValue().createReplacement(extractedMethod, methodCallExpression);
    }
    if (statement == null) {
      final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(methodCallExpression.getProject()).getElementFactory();
      PsiExpressionStatement expressionStatement = (PsiExpressionStatement) elementFactory.createStatementFromText("x();", null);
      final CodeStyleManager styleManager = CodeStyleManager.getInstance(methodCallExpression.getManager());
      expressionStatement = (PsiExpressionStatement)styleManager.reformat(expressionStatement);
      expressionStatement.getExpression().replace(methodCallExpression);
      statement = expressionStatement;
    }
    return replaceWith(statement);
  }

  public PsiExpression getInstanceExpression() {
    if (myInstanceExpression == null) {
      return null;
    }
    else {
      return myInstanceExpression.get();
    }
  }

  public PsiElement replace(final PsiMethod extractedMethod, final PsiMethodCallExpression methodCallExpression, PsiVariable outputVariable) throws IncorrectOperationException {
    declareLocalVariables();
    if (getMatchStart() == getMatchEnd() && getMatchStart() instanceof PsiExpression) {
      return replaceWithExpression(methodCallExpression);
    }
    else {
      return replaceByStatement(extractedMethod, methodCallExpression, outputVariable);
    }
  }

  private void declareLocalVariables() throws IncorrectOperationException {
    final PsiElement codeFragment = ControlFlowUtil.findCodeFragment(getMatchStart());
    try {
      final Project project = getMatchStart().getProject();
      final ControlFlow controlFlow = ControlFlowFactory.getInstance(project)
          .getControlFlow(codeFragment, new LocalsControlFlowPolicy(codeFragment));
      final int endOffset = controlFlow.getEndOffset(getMatchEnd());
      final int startOffset = controlFlow.getStartOffset(getMatchStart());
      final List<PsiVariable> usedVariables = ControlFlowUtil.getUsedVariables(controlFlow, endOffset, controlFlow.getSize());
      Collection<ControlFlowUtil.VariableInfo> reassigned = ControlFlowUtil.getInitializedTwice(controlFlow, endOffset, controlFlow.getSize());
      final Collection<PsiVariable> outVariables = ControlFlowUtil.getWrittenVariables(controlFlow, startOffset, endOffset, false);
      for (PsiVariable variable : usedVariables) {
        if (!outVariables.contains(variable)) {
          final PsiIdentifier identifier = variable.getNameIdentifier();
          if (identifier != null) {
            if (identifier.getTextRange().getStartOffset() >= getMatchStart().getTextRange().getStartOffset() &&
                identifier.getTextRange().getEndOffset() <= getMatchEnd().getTextRange().getEndOffset()) {
              final String name = variable.getName();
              LOG.assertTrue(name != null);
              PsiDeclarationStatement statement =
                  JavaPsiFacade.getInstance(project).getElementFactory().createVariableDeclarationStatement(name, variable.getType(), null);
              if (reassigned.contains(new ControlFlowUtil.VariableInfo(variable, null))) {
                final PsiElement[] psiElements = statement.getDeclaredElements();
                final PsiModifierList modifierList = ((PsiVariable)psiElements[0]).getModifierList();
                LOG.assertTrue(modifierList != null);
                modifierList.setModifierProperty(PsiModifier.FINAL, false);
              }
              getMatchStart().getParent().addBefore(statement, getMatchStart());
            }
          }
        }
      }
    }
    catch (AnalysisCanceledException e) {
      //skip match
    }
  }

  private PsiElement replaceWithExpression(final PsiMethodCallExpression methodCallExpression) throws IncorrectOperationException {
    LOG.assertTrue(getMatchStart() == getMatchEnd());
    return getMatchStart().replace(methodCallExpression);
  }

  TextRange getTextRange() {
    return new TextRange(getMatchStart().getTextRange().getStartOffset(),
                  getMatchEnd().getTextRange().getEndOffset());
  }

  @Nullable
  public String getChangedSignature(final PsiMethod method, final boolean shouldBeStatic, @Modifier String visibility) {
    final PsiType returnType = getChangedReturnType(method);
    if (!myChangedParams.isEmpty() || returnType != null) {
      @NonNls StringBuilder buffer = new StringBuilder();
      buffer.append(visibility);
      if (buffer.length() > 0) {
        buffer.append(" ");
      }
      if (shouldBeStatic) {
        buffer.append("static ");
      }
      final PsiTypeParameterList typeParameterList = method.getTypeParameterList();
      if (typeParameterList != null) {
        buffer.append(typeParameterList.getText());
        buffer.append(" ");
      }

      buffer.append(PsiFormatUtil.formatType(returnType != null ? returnType : method.getReturnType(), 0, PsiSubstitutor.EMPTY));
      buffer.append(" ");
      buffer.append(method.getName());
      buffer.append("(");
      int count = 0;
      final String INDENT = "    ";
      final ArrayList<ParameterInfoImpl> params = patchParams(method);
      for (ParameterInfoImpl param : params) {
        String typeText = param.getTypeText();
        if (count > 0) {
          buffer.append(",");
        }
        buffer.append("\n");
        buffer.append(INDENT);
        buffer.append(typeText);
        buffer.append(" ");
        buffer.append(param.getName());
        count++;
      }

      if (count > 0) {
        buffer.append("\n");
      }
      buffer.append(")");
      final PsiClassType[] exceptions = method.getThrowsList().getReferencedTypes();
      if (exceptions.length > 0) {
        buffer.append("\n");
        buffer.append("throws\n");
        for (PsiType exception : exceptions) {
          buffer.append(INDENT);
          buffer.append(PsiFormatUtil.formatType(exception, 0, PsiSubstitutor.EMPTY));
          buffer.append("\n");
        }
      }
      return buffer.toString();
    }
    return null;
  }

  public void changeSignature(final PsiMethod psiMethod) {
    final ArrayList<ParameterInfoImpl> newParameters = patchParams(psiMethod);
    final PsiType expressionType = getChangedReturnType(psiMethod);
    final ChangeSignatureProcessor csp = new ChangeSignatureProcessor(psiMethod.getProject(), psiMethod, false, null, psiMethod.getName(),
                                                                      expressionType != null ? expressionType : psiMethod.getReturnType(),
                                                                      newParameters.toArray(new ParameterInfoImpl[newParameters.size()]));

    csp.run();
  }

  @Nullable
  private PsiType getChangedReturnType(final PsiMethod psiMethod) {
    final PsiType returnType = psiMethod.getReturnType();
    if (returnType != null) {
      final PsiElement parent = getMatchEnd().getParent();
      if (parent instanceof PsiExpression) {
        JavaResolveResult result = null;
        if (parent instanceof PsiMethodCallExpression) {
          result = ((PsiMethodCallExpression)parent).resolveMethodGenerics();
        }
        else if (parent instanceof PsiReferenceExpression) {
          result = ((PsiReferenceExpression)parent).advancedResolve(false);
        }
        if (result != null) {
          final PsiElement element = result.getElement();
          if (element instanceof PsiMember) {
            final PsiClass psiClass = ((PsiMember)element).getContainingClass();
            if (psiClass != null) {
              final JavaPsiFacade facade = JavaPsiFacade.getInstance(parent.getProject());
              final PsiClassType expressionType = facade.getElementFactory().createType(psiClass, result.getSubstitutor());
              if (weakerType(psiMethod, returnType, expressionType)) {
                return expressionType;
              }
            }
          }
        }
      }
      else if (parent instanceof PsiExpressionList) {
        final PsiExpression[] expressions = ((PsiExpressionList)parent).getExpressions();
        final PsiElement call = parent.getParent();
        if (call instanceof PsiMethodCallExpression) {
          final PsiMethod method = ((PsiMethodCallExpression)call).resolveMethod();
          if (method != null) {
            final int idx = ArrayUtil.find(expressions, getMatchEnd());
            final PsiParameter[] psiParameters = method.getParameterList().getParameters();
            if (idx >= 0 && idx < psiParameters.length) {
              final PsiType type = psiParameters[idx].getType();
              if (weakerType(psiMethod, returnType, type)){
                return type;
              }
            }
          }
        }
      }
      else if (parent instanceof PsiLocalVariable) {
        final PsiType localVariableType = ((PsiLocalVariable)parent).getType();
        if (weakerType(psiMethod, returnType, localVariableType)) return localVariableType;
      }
    }
    return null;
  }

  private static boolean weakerType(final PsiMethod psiMethod, final PsiType returnType, final PsiType currentType) {
    final PsiTypeParameter[] typeParameters = psiMethod.getTypeParameters();
    final PsiSubstitutor substitutor =
        JavaPsiFacade.getInstance(psiMethod.getProject()).getResolveHelper().inferTypeArguments(typeParameters, new PsiType[]{returnType}, new PsiType[]{currentType}, PsiUtil.getLanguageLevel(psiMethod));

    return !TypeConversionUtil.isAssignable(currentType, substitutor.substitute(returnType));
  }

  private ArrayList<ParameterInfoImpl> patchParams(final PsiMethod psiMethod) {
    final ArrayList<ParameterInfoImpl> newParameters = new ArrayList<ParameterInfoImpl>();
    final PsiParameter[] oldParameters = psiMethod.getParameterList().getParameters();
    for (int i = 0; i < oldParameters.length; i++) {
      final PsiParameter oldParameter = oldParameters[i];
      PsiType type = oldParameter.getType();
      for (PsiVariable variable : myChangedParams.keySet()) {
        if (PsiEquivalenceUtil.areElementsEquivalent(variable, oldParameter)) {
          type = myChangedParams.get(variable);
          break;
        }
      }
      newParameters.add(new ParameterInfoImpl(i, oldParameter.getName(), type));
    }
    return newParameters;
  }

  public PsiFile getFile() {
    return getMatchStart().getContainingFile();
  }
}
