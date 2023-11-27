package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Utility class for handling operations related to annotations.
 * This class provides static methods to retrieve annotations from classes and methods
 * throughout the complete inheritance hierarchy. The extraction of these methods from the
 * ReflectionUtils class was necessary for improved code organization, separation of concerns,
 * and enhanced modularity. By isolating annotation-specific logic in this class,
 * the ReflectionUtils class can maintain a clearer focus on reflective code, caching mechanisms,
 * and other utility methods. This separation improves overall code readability and maintainability.
 *
 * @author Zeel Ravalani
 */
public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    /**
     * Determine if the passed in class (classToCheck) has the annotation (annoClass) on itself,
     * any of its super classes, any of it's interfaces, or any of it's super interfaces.
     * This is a exhaustive check throughout the complete inheritance hierarchy.
     *
     * @return the Annotation if found, null otherwise.
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk)) {
                continue;
            }
            visited.add(classToChk);
            T a = (T) classToChk.getAnnotation(annoClass);
            if (a != null) {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }
        return null;
    }

    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack) {
        for (Class<?> interFace : classToCheck.getInterfaces()) {
            stack.push(interFace);
        }
    }

    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass) {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(method.getDeclaringClass());

        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk)) {
                continue;
            }
            visited.add(classToChk);
            Method m = ReflectionUtils.getMethod(classToChk, method.getName(), method.getParameterTypes());
            if (m == null) {
                continue;
            }
            T a = m.getAnnotation(annoClass);
            if (a != null) {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(method.getDeclaringClass(), stack);
        }
        return null;
    }
}

//public static class DeltaError extends Delta {
//    private static final long serialVersionUID = 6248596026486571238L;
//
//    public DeltaError(String error, Delta delta) {
//        super(delta.getId(), delta.getFieldName(), delta.getSrcPtr(), delta.getSourceValue(),
//                delta.getTargetValue(), delta.getOptionalKey());
//        setError(error);
//    }
//
//    public String toString() {
//        return String.format("%s (%s)", getError(), super.toString());
//    }
//}