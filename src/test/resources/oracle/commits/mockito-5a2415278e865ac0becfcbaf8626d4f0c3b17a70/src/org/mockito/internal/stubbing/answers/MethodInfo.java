package org.mockito.internal.stubbing.answers;

import org.mockito.internal.util.Primitives;
import org.mockito.invocation.Invocation;

import java.lang.reflect.Method;

/**
 * by Szczepan Faber, created at: 3/31/12
 */
public class MethodInfo {

    private Method method;

    public MethodInfo(Invocation theInvocation) {
        this.method = theInvocation.getMethod();
    }

    public boolean isValidException(Throwable throwable) {
        Class<?>[] exceptions = method.getExceptionTypes();
        Class<?> throwableClass = throwable.getClass();
        for (Class<?> exception : exceptions) {
            if (exception.isAssignableFrom(throwableClass)) {
                return true;
            }
        }

        return false;
    }

    public boolean isValidReturnType(Class clazz) {
        if (method.getReturnType().isPrimitive() || clazz.isPrimitive()) {
            return Primitives.primitiveTypeOf(clazz) == Primitives.primitiveTypeOf(method.getReturnType());
        } else {
            return method.getReturnType().isAssignableFrom(clazz);
        }
    }

    public boolean isVoid() {
        return this.method.getReturnType() == Void.TYPE;
    }

    public String printMethodReturnType() {
        return method.getReturnType().getSimpleName();
    }

    public String getMethodName() {
        return method.getName();
    }

    public boolean returnsPrimitive() {
        return method.getReturnType().isPrimitive();
    }

    public Method getMethod() {
        return method;
    }

    public boolean isDeclaredOnInterface() {
        return method.getDeclaringClass().isInterface();
    }
}
