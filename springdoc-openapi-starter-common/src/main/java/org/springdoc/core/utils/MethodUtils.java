package org.springdoc.core.utils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Utility methods for reflection operations.
 * <p>
 * Replaces methods from {@code org.apache.commons.lang3.reflect.MethodUtils}.
 * Delegates to Spring's {@code org.springframework.util.ReflectionUtils}
 * internally where appropriate.
 * </p>
 */
public class MethodUtils {

    /**
     * Invokes a named method on an object.
     *
     * @param obj        the object to invoke the method on
     * @param methodName the method name
     * @return the return value of the method invocation, or null if method not found
     */
    public static Object invokeMethod(final Object obj, final String methodName) {
        try {
            Method method = ReflectionUtils.findMethod(obj.getClass(), methodName);
            if (method == null) {
                return null;
            }
            ReflectionUtils.makeAccessible(method);
            return ReflectionUtils.invokeMethod(method, obj);
        }
        catch (IllegalStateException e) {
            return null;
        }
    }
}
