/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.typeresolution;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTMemberSelector;
import net.sourceforge.pmd.lang.java.ast.ASTTypeArguments;
import net.sourceforge.pmd.lang.java.ast.TypeNode;
import net.sourceforge.pmd.lang.java.typeresolution.typedefinition.JavaTypeDefinition;
import net.sourceforge.pmd.lang.java.typeresolution.typeinference.Bound;
import net.sourceforge.pmd.lang.java.typeresolution.typeinference.InferenceRuleType;
import net.sourceforge.pmd.lang.java.typeresolution.typeinference.TypeInferenceResolver;
import net.sourceforge.pmd.lang.java.typeresolution.typeinference.TypeInferenceResolver.ResolutionFailedException;
import net.sourceforge.pmd.lang.java.typeresolution.typeinference.Variable;

import static net.sourceforge.pmd.lang.java.typeresolution.typeinference.InferenceRuleType.SUBTYPE;

public final class MethodTypeResolution {
    private MethodTypeResolution() {}

    private static final List<Class<?>> PRIMITIVE_SUBTYPE_ORDER;
    private static final List<Class<?>> BOXED_PRIMITIVE_SUBTYPE_ORDER;

    static {
        List<Class<?>> primitiveList = new ArrayList<>();

        primitiveList.add(double.class);
        primitiveList.add(float.class);
        primitiveList.add(long.class);
        primitiveList.add(int.class);
        primitiveList.add(short.class);
        primitiveList.add(byte.class);
        primitiveList.add(char.class); // this is here for convenience, not really in order

        PRIMITIVE_SUBTYPE_ORDER = Collections.unmodifiableList(primitiveList);

        List<Class<?>> boxedList = new ArrayList<>();

        boxedList.add(Double.class);
        boxedList.add(Float.class);
        boxedList.add(Long.class);
        boxedList.add(Integer.class);
        boxedList.add(Short.class);
        boxedList.add(Byte.class);
        boxedList.add(Character.class);

        BOXED_PRIMITIVE_SUBTYPE_ORDER = Collections.unmodifiableList(boxedList);
    }

    public static boolean checkSubtypeability(MethodType method, MethodType subtypeableMethod) {
        List<JavaTypeDefinition> subtypeableParams = subtypeableMethod.getParameterTypes();
        List<JavaTypeDefinition> methodParams = method.getParameterTypes();


        if (!method.getMethod().isVarArgs() && !subtypeableMethod.getMethod().isVarArgs()) {
            for (int index = 0; index < subtypeableParams.size(); ++index) {
                if (!isSubtypeable(methodParams.get(index), subtypeableParams.get(index))) {
                    return false;
                }
            }
        } else if (method.getMethod().isVarArgs() && subtypeableMethod.getMethod().isVarArgs()) {

            if (methodParams.size() < subtypeableParams.size()) {
                for (int index = 0; index < subtypeableParams.size(); ++index) {
                    if (!isSubtypeable(method.getArgTypeIncludingVararg(index),
                                       subtypeableMethod.getArgTypeIncludingVararg(index))) {
                        return false;
                    }
                }
            } else {
                for (int index = 0; index < methodParams.size(); ++index) {
                    if (!isSubtypeable(method.getArgTypeIncludingVararg(index),
                                       subtypeableMethod.getArgTypeIncludingVararg(index))) {
                        return false;
                    }
                }
            }

        } else {
            throw new IllegalStateException("These methods can only be vararg at the same time:\n"
                                                    + method.toString() + "\n" + subtypeableMethod.toString());
        }

        return true;
    }

    /**
     * Look for methods be subtypeability.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.2
     */
    public static List<MethodType> selectMethodsFirstPhase(JavaTypeDefinition context,
                                                           List<MethodType> methodsToSearch, ASTArgumentList argList) {
        // TODO: check if explicit type arguments are applicable to the type parameter bounds
        List<MethodType> selectedMethods = new ArrayList<>();

        for (int methodIndex = 0; methodIndex < methodsToSearch.size(); ++methodIndex) {
            MethodType methodType = methodsToSearch.get(methodIndex);
            if (!methodType.isParameterized()) {
                methodType = parameterizeStrictInvocation(context, methodType.getMethod(), argList);
            }

            if (argList == null) {
                selectedMethods.add(methodType);

                // vararg methods are considered fixed arity here
            } else if (getArity(methodType.getMethod()) == argList.jjtGetNumChildren()) {
                // check subtypeability of each argument to the corresponding parameter
                boolean methodIsApplicable = true;

                // try each arguments if it's subtypeable
                for (int argIndex = 0; argIndex < argList.jjtGetNumChildren(); ++argIndex) {
                    if (!isSubtypeable(methodType.getParameterTypes().get(argIndex),
                                       (ASTExpression) argList.jjtGetChild(argIndex))) {
                        methodIsApplicable = false;
                        break;
                    }
                    // TODO: add unchecked conversion in an else if branch
                }

                if (methodIsApplicable) {
                    selectedMethods.add(methodType);
                }

            }

        }

        return selectedMethods;
    }


    public static MethodType parameterizeStrictInvocation(JavaTypeDefinition context, Method method,
                                                          ASTArgumentList argList) {

        // variables are set up by the call to getInitialBounds
        List<Variable> variables = new ArrayList<>();
        List<Bound> initialBound = getInitialBounds(method, context, variables);

        return null;
    }


    public static List<Bound> getInitialBounds(Method method, JavaTypeDefinition context, List<Variable> variables) {
        // https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.1.3
        // When inference begins, a bound set is typically generated from a list of type parameter declarations P1,
        // ..., Pp and associated inference variables α1, ..., αp. Such a bound set is constructed as follows. For
        // each l (1 ≤ l ≤ p):
        
        TypeVariable<Method>[] typeVariables = method.getTypeParameters();

        variables.clear();
        for (int i = 0; i < typeVariables.length; ++i) {
            variables.add(new Variable());
        }

        List<Bound> result = new ArrayList<>();

        for (int currVarIndex = 0; currVarIndex < typeVariables.length; ++currVarIndex) {
            Type[] bounds = typeVariables[currVarIndex].getBounds();
            boolean currVarHasNoProperUpperBound = true;

            for (Type bound : bounds) {
                // Otherwise, for each type T delimited by & in the TypeBound, the bound αl <: T[P1:=α1, ..., Pp:=αp]
                // appears in the set; if this results in no proper upper bounds for αl (only dependencies), then the
                // bound α <: Object also appears in the set.

                int boundVarIndex;
                if (bound instanceof TypeVariable
                        && (boundVarIndex = JavaTypeDefinition
                        .getGenericTypeIndex(typeVariables, ((TypeVariable) bound).getName())) != -1) {
                    result.add(new Bound(variables.get(currVarIndex), variables.get(boundVarIndex), SUBTYPE));
                } else {
                    currVarHasNoProperUpperBound = false;
                    result.add(new Bound(variables.get(currVarIndex), context.resolveTypeDefinition(bound), SUBTYPE));
                }
            }

            // If Pl has no TypeBound, the bound αl <: Object appears in the set.
            if (currVarHasNoProperUpperBound) {
                result.add(new Bound(variables.get(currVarIndex), JavaTypeDefinition.forClass(Object.class), SUBTYPE));
            }
        }

        return result;
    }


    /**
     * Look for methods be method conversion.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.3
     */
    public static List<MethodType> selectMethodsSecondPhase(List<MethodType> methodsToSearch, ASTArgumentList argList) {
        // TODO: check if explicit type arguments are applicable to the type parameter bounds
        List<MethodType> selectedMethods = new ArrayList<>();

        for (int methodIndex = 0; methodIndex < methodsToSearch.size(); ++methodIndex) {
            MethodType methodType = methodsToSearch.get(methodIndex);
            if (!methodType.isParameterized()) {
                throw new ResolutionFailedException();
            }

            if (argList == null) {
                selectedMethods.add(methodType);

                // vararg methods are considered fixed arity here
            } else if (getArity(methodType.getMethod()) == argList.jjtGetNumChildren()) {
                // check method convertability of each argument to the corresponding parameter
                boolean methodIsApplicable = true;

                // try each arguments if it's method convertible
                for (int argIndex = 0; argIndex < argList.jjtGetNumChildren(); ++argIndex) {
                    if (!isMethodConvertible(methodType.getParameterTypes().get(argIndex),
                                             (ASTExpression) argList.jjtGetChild(argIndex))) {
                        methodIsApplicable = false;
                        break;
                    }
                    // TODO: add unchecked conversion in an else if branch
                }

                if (methodIsApplicable) {
                    selectedMethods.add(methodType);
                }
            }

        }

        return selectedMethods;
    }


    /**
     * Look for methods considering varargs as well.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.4
     */
    public static List<MethodType> selectMethodsThirdPhase(List<MethodType> methodsToSearch, ASTArgumentList argList) {
        // TODO: check if explicit type arguments are applicable to the type parameter bounds
        List<MethodType> selectedMethods = new ArrayList<>();

        for (int methodIndex = 0; methodIndex < methodsToSearch.size(); ++methodIndex) {
            MethodType methodType = methodsToSearch.get(methodIndex);
            if (!methodType.isParameterized()) {
                throw new ResolutionFailedException();
            }

            if (argList == null) {
                selectedMethods.add(methodType);

                // now we consider varargs as not fixed arity
            } else { // check subtypeability of each argument to the corresponding parameter
                boolean methodIsApplicable = true;

                List<JavaTypeDefinition> methodParameters = methodType.getParameterTypes();
                JavaTypeDefinition varargComponentType = methodType.getVarargComponentType();

                // try each arguments if it's method convertible
                for (int argIndex = 0; argIndex < argList.jjtGetNumChildren(); ++argIndex) {
                    JavaTypeDefinition parameterType = argIndex < methodParameters.size() - 1
                            ? methodParameters.get(argIndex) : varargComponentType;

                    if (!isMethodConvertible(parameterType, (ASTExpression) argList.jjtGetChild(argIndex))) {
                        methodIsApplicable = false;
                        break;
                    }

                    // TODO: If k != n, or if k = n and An cannot be converted by method invocation conversion to
                    // Sn[], then the type which is the erasure (§4.6) of Sn is accessible at the point of invocation.

                    // TODO: add unchecked conversion in an else if branch
                }

                if (methodIsApplicable) {
                    selectedMethods.add(methodType);
                }
            }
        }

        return selectedMethods;
    }


    /**
     * Searches a list of methods by trying the three phases of method overload resolution.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2
     */
    public static JavaTypeDefinition getBestMethodReturnType(JavaTypeDefinition context, List<MethodType> methods,
                                                             ASTArgumentList arguments) {

        try {
            List<MethodType> selectedMethods = selectMethodsFirstPhase(context, methods, arguments);
            if (!selectedMethods.isEmpty()) {
                return selectMostSpecificMethod(selectedMethods).getReturnType();
            }

            selectedMethods = selectMethodsSecondPhase(methods, arguments);
            if (!selectedMethods.isEmpty()) {
                return selectMostSpecificMethod(selectedMethods).getReturnType();
            }

            selectedMethods = selectMethodsThirdPhase(methods, arguments);
            if (!selectedMethods.isEmpty()) {
                return selectMostSpecificMethod(selectedMethods).getReturnType();
            }

            return null;
        } catch (ResolutionFailedException e) {
            return null;
        }
    }

    /**
     * Most specific method selection.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.5
     */
    public static MethodType selectMostSpecificMethod(List<MethodType> selectedMethods) {

        MethodType mostSpecific = selectedMethods.get(0);

        for (int methodIndex = 1; methodIndex < selectedMethods.size(); ++methodIndex) {
            MethodType nextMethod = selectedMethods.get(methodIndex);

            if (checkSubtypeability(mostSpecific, nextMethod)) {
                if (checkSubtypeability(nextMethod, mostSpecific)) { // both are maximally specific
                    mostSpecific = selectAmongMaximallySpecific(mostSpecific, nextMethod);
                } else {
                    mostSpecific = nextMethod;
                }
            }
        }

        return mostSpecific;
    }


    /**
     * Select maximally specific method.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.5
     */
    public static MethodType selectAmongMaximallySpecific(MethodType first, MethodType second) {
        if (first.isAbstract()) {
            if (second.isAbstract()) {
                // https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.5
                // the bottom of the section is relevant here, we can't resolve this type
                // TODO: resolve this

                return null;
            } else { // second one isn't abstract
                return second;
            }
        } else if (second.isAbstract()) {
            return first; // first isn't abstract, second one is
        } else {
            return null; // TODO: once shadowing and overriding methods is done, add exception back
            // throw new IllegalStateException("None of the maximally specific methods are abstract.\n"
            //                                        + first.toString() + "\n" + second.toString());
        }
    }


    /**
     * Looks for potentially applicable methods in a given type definition.
     */
    public static List<MethodType> getApplicableMethods(JavaTypeDefinition context,
                                                        String methodName,
                                                        List<JavaTypeDefinition> typeArguments,
                                                        int argArity,
                                                        Class<?> accessingClass) {
        List<MethodType> result = new ArrayList<>();

        if (context == null) {
            return result;
        }

        // TODO: shadowing, overriding
        // TODO: add multiple upper bounds

        Class<?> contextClass = context.getType();

        // search the class
        for (Method method : contextClass.getDeclaredMethods()) {
            if (isMethodApplicable(method, methodName, argArity, accessingClass, typeArguments)) {
                if (isGeneric(method) && typeArguments.size() == 0) {
                    // TODO: do generic implicit methods
                    // this disables invocations which could match generic methods and have no explicit type args
                    result.clear();
                    return result;
                }

                result.add(getTypeDefOfMethod(context, method, typeArguments));
            }
        }

        // search it's supertype
        if (!contextClass.equals(Object.class)) {
            result.addAll(getApplicableMethods(context.resolveTypeDefinition(contextClass.getGenericSuperclass()),
                                               methodName, typeArguments, argArity, accessingClass));
        }

        // search it's interfaces
        for (Type interfaceType : contextClass.getGenericInterfaces()) {
            result.addAll(getApplicableMethods(context.resolveTypeDefinition(interfaceType),
                                               methodName, typeArguments, argArity, accessingClass));
        }

        return result;
    }


    public static MethodType getTypeDefOfMethod(JavaTypeDefinition context, Method method,
                                                List<JavaTypeDefinition> typeArguments) {
        if (typeArguments.isEmpty() && isGeneric(method)) {
            return MethodType.build(method);
        }

        JavaTypeDefinition returnType = context.resolveTypeDefinition(method.getGenericReturnType(),
                                                                      method, typeArguments);
        List<JavaTypeDefinition> argTypes = new ArrayList<>();

        for (Type argType : method.getGenericParameterTypes()) {
            argTypes.add(context.resolveTypeDefinition(argType, method, typeArguments));
        }

        return MethodType.build(returnType, argTypes, method);
    }


    /**
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.1
     * Potential applicability.
     */
    public static boolean isMethodApplicable(Method method, String methodName, int argArity,
                                             Class<?> accessingClass, List<JavaTypeDefinition> typeArguments) {

        if (method.getName().equals(methodName) // name matches
                // is visible
                && isMemberVisibleFromClass(method.getDeclaringClass(), method.getModifiers(), accessingClass)
                // if method is vararg with arity n, then the invocation's arity >= n - 1
                && (!method.isVarArgs() || (argArity >= getArity(method) - 1))
                // if the method isn't vararg, then arity matches
                && (method.isVarArgs() || (argArity == getArity(method)))
                // isn't generic or arity of type arguments matches that of parameters
                && (!isGeneric(method) || typeArguments == null
                || method.getTypeParameters().length == typeArguments.size())) {

            return true;
        }

        return false;
    }


    /**
     * Given a class, the modifiers of on of it's member and the class that is trying to access that member,
     * returns true is the member is accessible from the accessingClass Class.
     *
     * @param classWithMember The Class with the member.
     * @param modifiers       The modifiers of that member.
     * @param accessingClass  The Class trying to access the member.
     * @return True if the member is visible from the accessingClass Class.
     */
    public static boolean isMemberVisibleFromClass(Class<?> classWithMember, int modifiers, Class<?> accessingClass) {
        if (accessingClass == null) {
            return false;
        }

        // public members
        if (Modifier.isPublic(modifiers)) {
            return true;
        }

        boolean areInTheSamePackage = false;

        if (accessingClass.getPackage() != null) { // if null, then it's in the default package
            // if calssWithMember.getPackage() is null, result will be false
            areInTheSamePackage = accessingClass.getPackage().getName().equals(
                    classWithMember.getPackage().getName());
        }

        // protected members
        if (Modifier.isProtected(modifiers)) {
            if (areInTheSamePackage || classWithMember.isAssignableFrom(accessingClass)) {
                return true;
            }
            // private members
        } else if (Modifier.isPrivate(modifiers)) {
            if (classWithMember.equals(accessingClass)) {
                return true;
            }
            // package private members
        } else if (areInTheSamePackage) {
            return true;
        }

        return false;
    }

    public static boolean isGeneric(Method method) {
        return method.getTypeParameters().length != 0;
    }

    public static boolean isGeneric(Class<?> clazz) {
        return clazz.getTypeParameters().length != 0;
    }

    public static int getArity(Method method) {
        return method.getParameterTypes().length;
    }

    public static boolean isMethodConvertible(JavaTypeDefinition parameter, ASTExpression argument) {
        return isMethodConvertible(parameter, argument.getTypeDefinition());
    }

    /**
     * Method invocation conversion rules.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.3
     */
    public static boolean isMethodConvertible(JavaTypeDefinition parameter, JavaTypeDefinition argument) {
        // covers identity conversion, widening primitive conversion, widening reference conversion, null
        // covers if both are primitive or bot are boxed primitive
        if (isSubtypeable(parameter, argument)) {
            return true;
        }

        // covers boxing
        int indexInPrimitive = PRIMITIVE_SUBTYPE_ORDER.indexOf(argument.getType());

        if (indexInPrimitive != -1 // arg is primitive
                && isSubtypeable(parameter,
                                 JavaTypeDefinition.forClass(BOXED_PRIMITIVE_SUBTYPE_ORDER.get(indexInPrimitive)))) {
            return true;
        }

        // covers unboxing
        int indexInBoxed = BOXED_PRIMITIVE_SUBTYPE_ORDER.indexOf(argument.getType());

        if (indexInBoxed != -1 // arg is boxed primitive
                && isSubtypeable(parameter,
                                 JavaTypeDefinition.forClass(PRIMITIVE_SUBTYPE_ORDER.get(indexInBoxed)))) {
            return true;
        }

        // TODO: add raw unchecked conversion part

        return false;
    }


    public static boolean isSubtypeable(JavaTypeDefinition parameter, ASTExpression argument) {
        return isSubtypeable(parameter, argument.getTypeDefinition());
    }

    public static boolean isSubtypeable(Class<?> parameter, Class<?> argument) {
        return isSubtypeable(JavaTypeDefinition.forClass(parameter), JavaTypeDefinition.forClass(argument));
    }

    /**
     * Subtypeability rules.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.10
     */
    public static boolean isSubtypeable(JavaTypeDefinition parameter, JavaTypeDefinition argument) {
        // null types are always applicable
        if (argument.getType() == null) {
            return true;
        }

        // this covers arrays, simple class/interface cases
        if (parameter.getType().isAssignableFrom(argument.getType())) {
            if (!parameter.isGeneric() || parameter.isRawType() || argument.isRawType()) {
                return true;
            }

            // parameter is a non-raw generic type
            // argument is a non-generic or a non-raw generic type

            // example result: List<String>.getAsSuper(Collection) becomes Collection<String>
            JavaTypeDefinition argSuper = argument.getAsSuper(parameter.getType());
            // argSuper can't be null because isAssignableFrom check above returned true

            // right now we only check if generic arguments are the same
            // TODO: add support for wildcard types
            // (future note: can't call subtype as it is recursively, infinite types)
            return parameter.equals(argSuper);
        }

        int indexOfParameter = PRIMITIVE_SUBTYPE_ORDER.indexOf(parameter.getType());

        if (indexOfParameter != -1) {
            if (argument.getType() == char.class) {
                if (indexOfParameter <= 3) { // <= 3 because short and byte are not compatible with char
                    return true;
                }
            } else {
                int indexOfArg = PRIMITIVE_SUBTYPE_ORDER.indexOf(argument.getType());
                if (indexOfArg != -1 && indexOfParameter <= indexOfArg) {
                    return true;
                }
            }
        }

        return false;
    }

    public static JavaTypeDefinition boxPrimitive(JavaTypeDefinition def) {
        if (!def.isPrimitive()) {
            return null;
        }

        return JavaTypeDefinition.forClass(BOXED_PRIMITIVE_SUBTYPE_ORDER.get(PRIMITIVE_SUBTYPE_ORDER.indexOf(def.getType())));
    }

    public static List<JavaTypeDefinition> getMethodExplicitTypeArugments(Node node) {
        ASTMemberSelector memberSelector = node.getFirstChildOfType(ASTMemberSelector.class);
        if (memberSelector == null) {
            return Collections.emptyList();
        }

        ASTTypeArguments typeArguments = memberSelector.getFirstChildOfType(ASTTypeArguments.class);
        if (typeArguments == null) {
            return Collections.emptyList();
        }

        List<JavaTypeDefinition> result = new ArrayList<>();

        for (int childIndex = 0; childIndex < typeArguments.jjtGetNumChildren(); ++childIndex) {
            result.add(((TypeNode) typeArguments.jjtGetChild(childIndex)).getTypeDefinition());
        }

        return result;
    }
}
