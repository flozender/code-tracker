/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.engine;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiStatement;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 *         Date: 10/26/13
 */
public class LambdaMethodFilter implements BreakpointStepMethodFilter{
  private static final String LAMBDA_METHOD_PREFIX = "lambda$";
  private final int myLambdaOrdinal;
  @Nullable
  private final SourcePosition myFirstStatementPosition;
  private final int myLastStatementLine;

  public LambdaMethodFilter(PsiLambdaExpression lambda, int expressionOrdinal) {
    myLambdaOrdinal = expressionOrdinal;

    SourcePosition firstStatementPosition = null;
    SourcePosition lastStatementPosition = null;
    final PsiElement body = lambda.getBody();
    if (body instanceof PsiCodeBlock) {
      final PsiStatement[] statements = ((PsiCodeBlock)body).getStatements();
      final int statementCount = statements.length;
      if (statementCount > 0) {
        firstStatementPosition = SourcePosition.createFromElement(statements[0]);
        if (statementCount > 1) {
          lastStatementPosition = SourcePosition.createFromElement(statements[statementCount - 1]);
        }
      }
    }
    else if (body != null){
      firstStatementPosition = SourcePosition.createFromElement(body);
    }
    myFirstStatementPosition = firstStatementPosition;
    myLastStatementLine = lastStatementPosition != null? lastStatementPosition.getLine() : -1;
  }

  public int getLambdaOrdinal() {
    return myLambdaOrdinal;
  }

  @Nullable
  public SourcePosition getBreakpointPosition() {
    return myFirstStatementPosition;
  }

  /**
   * @return a zero-based line number of the last lambda statement, or -1 if not available
   */
  public int getLastStatementLine() {
    return myLastStatementLine;
  }

  public boolean locationMatches(DebugProcessImpl process, Location location) throws EvaluateException {
    final VirtualMachineProxyImpl vm = process.getVirtualMachineProxy();
    final Method method = location.method();
    return method.name().startsWith(LAMBDA_METHOD_PREFIX) && (!vm.canGetSyntheticAttribute() || method.isSynthetic());
  }
}
