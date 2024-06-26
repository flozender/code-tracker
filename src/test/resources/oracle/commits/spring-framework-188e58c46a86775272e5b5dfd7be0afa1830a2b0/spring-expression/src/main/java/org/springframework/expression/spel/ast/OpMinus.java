/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.math.BigDecimal;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.NumberUtils;

/**
 * The minus operator supports:
 * <ul>
 * <li>subtraction of {@code BigDecimal}
 * <li>subtraction of doubles (floats are represented as doubles)
 * <li>subtraction of longs
 * <li>subtraction of integers
 * <li>subtraction of an int from a string of one character (effectively decreasing that
 * character), so 'd'-3='a'
 * </ul>
 * It can be used as a unary operator for numbers ({@code BigDecimal}/double/long/int).
 * The standard promotions are performed when the operand types vary (double-int=double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpMinus extends Operator {


	public OpMinus(int pos, SpelNodeImpl... operands) {
		super("-", pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {

		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		if (rightOp == null) {// If only one operand, then this is unary minus
			Object operand = leftOp.getValueInternal(state).getValue();
			if (operand instanceof Number) {
				Number n = (Number) operand;

				if (operand instanceof BigDecimal) {
					BigDecimal bdn = (BigDecimal) n;
					return new TypedValue(bdn.negate());
				}

				if (operand instanceof Double) {
					this.exitTypeDescriptor = "D";
					return new TypedValue(0 - n.doubleValue());
				}

				if (operand instanceof Float) {
					this.exitTypeDescriptor = "F";
					return new TypedValue(0 - n.floatValue());
				}

				if (operand instanceof Long) {
					this.exitTypeDescriptor = "J";
					return new TypedValue(0 - n.longValue());
				}
				this.exitTypeDescriptor = "I";
				return new TypedValue(0 - n.intValue());
			}

			return state.operate(Operation.SUBTRACT, operand, null);
		}

		Object left = leftOp.getValueInternal(state).getValue();
		Object right = rightOp.getValueInternal(state).getValue();

		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.subtract(rightBigDecimal));
			}
			
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				if (leftNumber instanceof Double && rightNumber instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				return new TypedValue(leftNumber.doubleValue() - rightNumber.doubleValue());
			}

			if (leftNumber instanceof Float || rightNumber instanceof Float) {
				if (leftNumber instanceof Float && rightNumber instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				return new TypedValue(leftNumber.floatValue() - rightNumber.floatValue());
			}

			if (leftNumber instanceof Long || rightNumber instanceof Long) {
				if (leftNumber instanceof Long && rightNumber instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				return new TypedValue(leftNumber.longValue() - rightNumber.longValue());
			}
			this.exitTypeDescriptor = "I";
			return new TypedValue(leftNumber.intValue() - rightNumber.intValue());
		}
		else if (left instanceof String && right instanceof Integer
				&& ((String) left).length() == 1) {
			String theString = (String) left;
			Integer theInteger = (Integer) right;
			// implements character - int (ie. b - 1 = a)
			return new TypedValue(Character.toString((char)
					(theString.charAt(0) - theInteger)));
		}

		return state.operate(Operation.SUBTRACT, left, right);
	}

	@Override
	public String toStringAST() {
		if (getRightOperand() == null) { // unary minus
			return new StringBuilder().append("-").append(getLeftOperand().toStringAST()).toString();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length<2) {return null;}
		return this.children[1];
	}
	
	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length>1) {
			 if (!getRightOperand().isCompilable()) {
				 return false;
			 }
		}
		return this.exitTypeDescriptor!=null;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		getLeftOperand().generateCode(mv, codeflow);
		String leftdesc = getLeftOperand().getExitDescriptor();
		if (!CodeFlow.isPrimitive(leftdesc)) {
			CodeFlow.insertUnboxInsns(mv, this.exitTypeDescriptor.charAt(0), false);
		}	
		if (this.children.length>1) {
			getRightOperand().generateCode(mv, codeflow);
			String rightdesc = getRightOperand().getExitDescriptor();
			if (!CodeFlow.isPrimitive(rightdesc)) {
				CodeFlow.insertUnboxInsns(mv, this.exitTypeDescriptor.charAt(0), false);
			}
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(ISUB);
					break;
				case 'J':
					mv.visitInsn(LSUB);
					break;
				case 'F': 
					mv.visitInsn(FSUB);
					break;
				case 'D':
					mv.visitInsn(DSUB);
					break;				
				default:
					throw new IllegalStateException("Unrecognized exit descriptor: '"+this.exitTypeDescriptor+"'");
			}
		} else {
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(INEG);
					break;
				case 'J':
					mv.visitInsn(LNEG);
					break;
				case 'F': 
					mv.visitInsn(FNEG);
					break;
				case 'D':
					mv.visitInsn(DNEG);
					break;				
				default:
					throw new IllegalStateException("Unrecognized exit descriptor: '"+this.exitTypeDescriptor+"'");
			}			
		}
		codeflow.pushDescriptor(this.exitTypeDescriptor);
	}

}
