package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Useful APIs for working with Java types, including resolving type variables and generic types.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TypeUtilities {
    /**
     * Extracts the raw Class from a given Type.
     * For example, for List<String> it returns List.class.
     *
     * @param type the type to inspect. If type is null, the return is null.
     * @return the raw class behind the type
     */
    public static Class<?> getRawClass(Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class<?>) {
            // Simple non-generic type.
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            // For something like List<String>, return List.class.
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            } else {
                throw new IllegalArgumentException("Unexpected raw type: " + rawType);
            }
        } else if (type instanceof GenericArrayType) {
            // For a generic array type (e.g., T[] or List<String>[]),
            // first get the component type, then build an array class.
            GenericArrayType arrayType = (GenericArrayType) type;
            Type componentType = arrayType.getGenericComponentType();
            Class<?> componentClass = getRawClass(componentType);
            return Array.newInstance(componentClass, 0).getClass();
        } else if (type instanceof WildcardType) {
            // For wildcard types like "? extends Number", use the first upper bound.
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                return getRawClass(upperBounds[0]);
            }
            return Object.class; // safe default
        } else if (type instanceof TypeVariable) {
            // For type variables (like T), pick the first bound.
            TypeVariable<?> typeVar = (TypeVariable<?>) type;
            Type[] bounds = typeVar.getBounds();
            if (bounds.length > 0) {
                return getRawClass(bounds[0]);
            }
            return Object.class;
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Extracts the component type of an array type.
     *
     * @param type the array type (can be a Class or GenericArrayType)
     * @return the component type, or null if not an array
     */
    public static Type extractArrayComponentType(Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else if (type instanceof Class<?>) {
            Class<?> cls = (Class<?>) type;
            if (cls.isArray()) {
                return cls.getComponentType();
            }
        }
        return null;
    }

    /**
     * Determines whether the provided type (including its nested types)
     * contains an unresolved type variable.
     *
     * @param type the type to inspect
     * @return true if an unresolved type variable is found; false otherwise
     */
    public static boolean containsUnresolvedType(Type type) {
        if (type == null) {
            return false;
        }
        if (type instanceof TypeVariable) {
            return true;
        }
        if (type instanceof ParameterizedType) {
            for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
                if (containsUnresolvedType(arg)) {
                    return true;
                }
            }
        }
        if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            for (Type bound : wt.getUpperBounds()) {
                if (containsUnresolvedType(bound)) {
                    return true;
                }
            }
            for (Type bound : wt.getLowerBounds()) {
                if (containsUnresolvedType(bound)) {
                    return true;
                }
            }
        }
        if (type instanceof GenericArrayType) {
            return containsUnresolvedType(((GenericArrayType) type).getGenericComponentType());
        }
        return false;
    }

    /**
     * Resolves a generic field type using the actual class of the target instance.
     * It handles type variables, parameterized types, generic array types, and wildcards.
     *
     * @param target    the target instance that holds the field
     * @param typeToResolve the declared generic type of the field
     * @return the resolved type
     */
    public static Type resolveTypeUsingInstance(Object target, Type typeToResolve) {
        Convention.throwIfNull(target, "target cannot be null");
        Type resolved = resolveType(target.getClass(), typeToResolve);
        // For raw instance resolution, if no concrete substitution was found,
        // use the first bound (which for an unbounded type variable defaults to Object.class).
        if (resolved instanceof TypeVariable) {
            resolved = firstBound((TypeVariable<?>) resolved);
        }
        return resolved;
    }

    /**
     * Public API: Resolves type variables in typeToResolve using the rootContext,
     * which should be the most concrete type (for example, Child.class).
     */
    public static Type resolveType(Type rootContext, Type typeToResolve) {
        return resolveType(rootContext, rootContext, typeToResolve, new HashSet<Type>());
    }

    /**
     * Recursively resolves typeToResolve using:
     * - rootContext: the most concrete type (never changes)
     * - currentContext: the immediate context (may change as we climb the hierarchy)
     * - visited: to avoid cycles
     */
    private static Type resolveType(Type rootContext, Type currentContext, Type typeToResolve, Set<Type> visited) {
        System.out.println("resolveType() called with rootContext: " + rootContext +
                ", currentContext: " + currentContext +
                ", typeToResolve: " + typeToResolve);
        if (typeToResolve == null) {
            return null;
        }
        if (typeToResolve instanceof TypeVariable) {
            return processTypeVariable(rootContext, currentContext, (TypeVariable<?>) typeToResolve, visited);
        }
        if (visited.contains(typeToResolve)) {
            System.out.println("Cycle detected for type: " + typeToResolve + ", returning it");
            return typeToResolve;
        }
        visited.add(typeToResolve);
        try {
            if (typeToResolve instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) typeToResolve;
                System.out.println("Processing ParameterizedType: " + pt);
                Type[] args = pt.getActualTypeArguments();
                Type[] resolvedArgs = new Type[args.length];
                // Use the current ParameterizedType (pt) as the new context for its type arguments.
                for (int i = 0; i < args.length; i++) {
                    System.out.println("Resolving argument " + i + ": " + args[i]);
                    resolvedArgs[i] = resolveType(rootContext, pt, args[i], visited);
                    System.out.println("Resolved argument " + i + " to: " + resolvedArgs[i]);
                }
                Type ownerType = pt.getOwnerType();
                if (ownerType != null) {
                    System.out.println("Resolving owner type: " + ownerType);
                    ownerType = resolveType(rootContext, pt, ownerType, visited);
                    System.out.println("Resolved owner type to: " + ownerType);
                }
                ParameterizedTypeImpl result = new ParameterizedTypeImpl((Class<?>) pt.getRawType(), resolvedArgs, ownerType);
                System.out.println("Returning ParameterizedType: " + result);
                return result;
            } else if (typeToResolve instanceof GenericArrayType) {
                System.out.println("Processing GenericArrayType: " + typeToResolve);
                GenericArrayType gat = (GenericArrayType) typeToResolve;
                Type resolvedComp = resolveType(rootContext, currentContext, gat.getGenericComponentType(), visited);
                GenericArrayTypeImpl result = new GenericArrayTypeImpl(resolvedComp);
                System.out.println("Returning GenericArrayType: " + result);
                return result;
            } else if (typeToResolve instanceof WildcardType) {
                System.out.println("Processing WildcardType: " + typeToResolve);
                WildcardType wt = (WildcardType) typeToResolve;
                Type[] upperBounds = wt.getUpperBounds();
                Type[] lowerBounds = wt.getLowerBounds();
                for (int i = 0; i < upperBounds.length; i++) {
                    System.out.println("Resolving upper bound " + i + ": " + upperBounds[i]);
                    upperBounds[i] = resolveType(rootContext, currentContext, upperBounds[i], visited);
                    System.out.println("Resolved upper bound " + i + " to: " + upperBounds[i]);
                }
                for (int i = 0; i < lowerBounds.length; i++) {
                    System.out.println("Resolving lower bound " + i + ": " + lowerBounds[i]);
                    lowerBounds[i] = resolveType(rootContext, currentContext, lowerBounds[i], visited);
                    System.out.println("Resolved lower bound " + i + " to: " + lowerBounds[i]);
                }
                WildcardTypeImpl result = new WildcardTypeImpl(upperBounds, lowerBounds);
                System.out.println("Returning WildcardType: " + result);
                return result;
            } else {
                System.out.println("Returning raw type: " + typeToResolve);
                return typeToResolve;
            }
        } finally {
            visited.remove(typeToResolve);
        }
    }

    private static Type processTypeVariable(Type rootContext, Type currentContext, TypeVariable<?> typeVar, Set<Type> visited) {
        System.out.println("\n=== Entering processTypeVariable() ===");
        System.out.println("rootContext: " + rootContext + " (Class? " + (rootContext instanceof Class) + ")");
        System.out.println("currentContext: " + currentContext + " (Class? " + (currentContext instanceof Class) + ")");
        System.out.println("typeVar: " + typeVar);

        if (visited.contains(typeVar)) {
            System.out.println("Cycle detected for type variable: " + typeVar + ", returning it");
            return typeVar;
        }
        visited.add(typeVar);

        try {
            Type resolved = null;

            System.out.println("Checking if currentContext is a ParameterizedType...");
            if (currentContext instanceof ParameterizedType) {
                resolved = resolveTypeVariableFromParentType(currentContext, typeVar);
                System.out.println("Resolved from ParameterizedType context: " + resolved);
                if (resolved == typeVar) {
                    System.out.println("No substitution found, setting resolved to null.");
                    resolved = null;
                }
            }

            System.out.println("Checking if unresolved and raw class is not the declaring class...");
            Class<?> declaringClass = (Class<?>) typeVar.getGenericDeclaration();
            Class<?> currentRaw = getRawClass(currentContext);
            System.out.println("Declaring class: " + declaringClass + ", Current raw class: " + currentRaw);
            if (resolved == null && (currentRaw == null || !declaringClass.equals(currentRaw))) {
                System.out.println("Attempting to locate parameterized binding in root context...");
                ParameterizedType pType = findParameterizedType(rootContext, declaringClass);
                if (pType != null) {
                    System.out.println("Located parameterized binding: " + pType);
                    TypeVariable<?>[] declaredVars = declaringClass.getTypeParameters();
                    for (int i = 0; i < declaredVars.length; i++) {
                        if (declaredVars[i].getName().equals(typeVar.getName())) {
                            resolved = pType.getActualTypeArguments()[i];
                            System.out.println("Resolved from root context: " + resolved);
                            break;
                        }
                    }
                }
            }

            System.out.println("Checking if unresolved and currentContext is a Class...");
            if (resolved == null && currentContext instanceof Class) {
                System.out.println("Attempting to climb the type hierarchy...");
                resolved = climbGenericHierarchy(rootContext, currentContext, typeVar, visited);
                System.out.println("Resolved by climbing hierarchy: " + resolved);
            }

            System.out.println("Checking if resolved type is still a TypeVariable...");
            if (resolved != null && resolved instanceof TypeVariable) {
                System.out.println("Further resolving intermediate type variable: " + resolved);
                resolved = resolveType(rootContext, rootContext, resolved, visited);
                System.out.println("After further resolution: " + resolved);
            }

            // **Final Fallback**
            System.out.println("\n=== Final Resolution Decision ===");
            System.out.println("rootContext instance of Class: " + (rootContext instanceof Class));
            System.out.println("currentContext instance of Class: " + (currentContext instanceof Class));
            System.out.println("rootContext == currentContext: " + (rootContext == currentContext));
            System.out.println("firstBound(typeVar): " + firstBound(typeVar));
            System.out.println("resolved (before final fallback): " + resolved);

            if (resolved == null) {
                // If the resolution was invoked with a raw class as parent,
                // then leave the type variable unchanged.
                if (rootContext instanceof Class && rootContext == currentContext && !(rootContext instanceof ParameterizedType)) {
                    System.out.println("Detected raw class resolution...");
                    resolved = typeVar;
                    System.out.println("Returning original type variable (raw class definition): " + typeVar);
                } else {
                    resolved = firstBound(typeVar);
                    System.out.println("Using firstBound as last fallback for " + typeVar + ": " + resolved);
                }
            }

            System.out.println("Final resolved TypeVariable: " + resolved);
            System.out.println("=== Exiting processTypeVariable() ===\n");

            return resolved;
        } finally {
            visited.remove(typeVar);
        }
    }

    /**
     * Climb up the generic inheritance chain (superclass then interfaces) starting from currentContext,
     * using rootContext for full resolution.
     */
    private static Type climbGenericHierarchy(Type rootContext, Type currentContext, TypeVariable<?> typeVar, Set<Type> visited) {
        System.out.println("Climbing hierarchy for type variable: " + typeVar + " with currentContext: " + currentContext);
        Class<?> declaringClass = (Class<?>) typeVar.getGenericDeclaration();
        Class<?> contextClass = getRawClass(currentContext);
        if (contextClass != null && declaringClass.equals(contextClass)) {
            // Found the declaring class; try to locate its parameterized type in the rootContext.
            ParameterizedType pType = findParameterizedType(rootContext, declaringClass);
            if (pType != null) {
                TypeVariable<?>[] declaredVars = declaringClass.getTypeParameters();
                for (int i = 0; i < declaredVars.length; i++) {
                    if (declaredVars[i].getName().equals(typeVar.getName())) {
                        System.out.println("Found declaring class " + declaringClass + " with parameterized type " + pType);
                        return pType.getActualTypeArguments()[i];
                    }
                }
            }
        }
        if (currentContext instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) currentContext;
            Type resolved = climbGenericHierarchy(rootContext, pt.getRawType(), typeVar, visited);
            if (resolved != null && !(resolved instanceof TypeVariable)) {
                System.out.println("Resolved in ParameterizedType raw: " + resolved);
                return resolved;
            }
        }
        if (contextClass == null) {
            System.out.println("Cannot extract raw class from currentContext: " + currentContext);
            return null;
        }
        // Try the generic superclass.
        Type superType = contextClass.getGenericSuperclass();
        if (superType != null && !superType.equals(Object.class)) {
            System.out.println("Climbing generic superclass: " + superType);
            Type resolved = resolveType(rootContext, superType, superType, visited);
            if (resolved != null && !(resolved instanceof TypeVariable)) {
                System.out.println("Resolved in superclass: " + resolved);
                return resolved;
            }
            resolved = climbGenericHierarchy(rootContext, superType, typeVar, visited);
            if (resolved != null && !(resolved instanceof TypeVariable)) {
                System.out.println("Resolved by climbing further in superclass: " + resolved);
                return resolved;
            }
        }
        // Then try each generic interface.
        for (Type iface : contextClass.getGenericInterfaces()) {
            System.out.println("Climbing generic interface: " + iface);
            Type resolved = resolveType(rootContext, iface, iface, visited);
            if (resolved != null && !(resolved instanceof TypeVariable)) {
                System.out.println("Resolved in interface: " + resolved);
                return resolved;
            }
            resolved = climbGenericHierarchy(rootContext, iface, typeVar, visited);
            if (resolved != null && !(resolved instanceof TypeVariable)) {
                System.out.println("Resolved by climbing further in interface: " + resolved);
                return resolved;
            }
        }
        System.out.println("Unable to resolve type variable " + typeVar + " in currentContext " + currentContext);
        return null;
    }

    /**
     * Recursively searches the hierarchy of 'context' for a ParameterizedType whose raw type equals target.
     */
    private static ParameterizedType findParameterizedType(Type context, Class<?> target) {
        if (context instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) context;
            if (target.equals(pt.getRawType())) {
                return pt;
            }
        }
        Class<?> clazz = getRawClass(context);
        if (clazz != null) {
            for (Type iface : clazz.getGenericInterfaces()) {
                ParameterizedType pt = findParameterizedType(iface, target);
                if (pt != null) {
                    return pt;
                }
            }
            Type superType = clazz.getGenericSuperclass();
            if (superType != null) {
                return findParameterizedType(superType, target);
            }
        }
        return null;
    }
    
    /**
     * Resolves a field’s declared generic type by substituting type variables
     * using the actual type arguments from the parent type.
     *
     * @param parentType the full parent type
     * @param typeToResolve the declared generic type of the field (e.g., T)
     * @return the resolved type (e.g., Point) if substitution is possible;
     *         otherwise, returns fieldType.
     */
    public static Type resolveFieldTypeUsingParent(Type parentType, Type typeToResolve) {
        if (typeToResolve instanceof TypeVariable && parentType instanceof ParameterizedType) {
            ParameterizedType parameterizedParentType = (ParameterizedType) parentType;
            TypeVariable<?> typeVar = (TypeVariable<?>) typeToResolve;
            // Get the type parameters declared on the raw parent class.
            TypeVariable<?>[] typeParams = ((Class<?>) parameterizedParentType.getRawType()).getTypeParameters();
            for (int i = 0; i < typeParams.length; i++) {
                if (typeParams[i].getName().equals(typeVar.getName())) {
                    return parameterizedParentType.getActualTypeArguments()[i];
                }
            }
        }
        return typeToResolve;
    }

    /**
     * Helper method that, given a type (if it is parameterized), checks whether it
     * maps the given type variable to a concrete type.
     *
     * @param parentType the type to inspect (may be null)
     * @param typeToResolve   the type variable to resolve
     * @return the resolved type if found, or null otherwise
     */
    private static Type resolveTypeVariableFromParentType(Type parentType, TypeVariable<?> typeToResolve) {
        if (parentType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) parentType;
            // Get the type parameters declared on the raw type.
            TypeVariable<?>[] typeParams = ((Class<?>) pt.getRawType()).getTypeParameters();
            Type[] actualTypes = pt.getActualTypeArguments();
            for (int i = 0; i < typeParams.length; i++) {
                if (typeParams[i].getName().equals(typeToResolve.getName())) {
                    return actualTypes[i];
                }
            }
        }
        return null;
    }

    /**
     * Returns the first bound of the type variable, or Object.class if none exists.
     *
     * @param tv the type variable
     * @return the first bound
     */
    private static Type firstBound(TypeVariable<?> tv) {
        Type[] bounds = tv.getBounds();
        return bounds.length > 0 ? bounds[0] : Object.class;
    }

    /**
     * Resolves a suggested type against a field's generic type.
     * Useful for collections, maps, and arrays.
     *
     * @param suggestedType the full parent type (e.g., ThreeType&lt;Point, String, Point&gt;)
     * @param fieldGenericType the declared generic type of the field
     * @return the resolved type based on the suggested type
     */
    public static Type resolveSuggestedType(Type suggestedType, Type fieldGenericType) {
        if (suggestedType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) suggestedType;
            Type[] typeArgs = pt.getActualTypeArguments();
            Class<?> raw = getRawClass(pt.getRawType());
            if (Map.class.isAssignableFrom(raw)) {
                // For maps, expect two type arguments; the value type is at index 1.
                if (typeArgs.length >= 2) {
                    fieldGenericType = typeArgs[1];
                }
            } else if (Collection.class.isAssignableFrom(raw)) {
                // For collections, expect one type argument.
                if (typeArgs.length >= 1) {
                    fieldGenericType = typeArgs[0];
                }
            } else if (raw.isArray()) {
                // For arrays, expect one type argument.
                if (typeArgs.length >= 1) {
                    fieldGenericType = typeArgs[0];
                }
            } else {
                // For other types, default to Object.class.
                fieldGenericType = Object.class;
            }
        }
        return fieldGenericType;
    }

    // --- Internal implementations of Type interfaces ---

    /**
     * A simple implementation of ParameterizedType.
     */
    private static class ParameterizedTypeImpl implements ParameterizedType {
        private final Class<?> raw;
        private final Type[] args;
        private final Type owner;

        public ParameterizedTypeImpl(Class<?> raw, Type[] args, Type owner) {
            this.raw = raw;
            this.args = args.clone();
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(raw.getName());
            if (args != null && args.length > 0) {
                sb.append("<");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(args[i].getTypeName());
                }
                sb.append(">");
            }
            return sb.toString();
        }
    }

    /**
     * A simple implementation of GenericArrayType.
     */
    private static class GenericArrayTypeImpl implements GenericArrayType {
        private final Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public String toString() {
            return componentType.getTypeName() + "[]";
        }
    }

    /**
     * A simple implementation of WildcardType.
     */
    private static class WildcardTypeImpl implements WildcardType {
        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds != null ? upperBounds.clone() : new Type[]{Object.class};
            this.lowerBounds = lowerBounds != null ? lowerBounds.clone() : new Type[0];
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("?");
            if (upperBounds.length > 0 && !(upperBounds.length == 1 && upperBounds[0] == Object.class)) {
                sb.append(" extends ");
                for (int i = 0; i < upperBounds.length; i++) {
                    if (i > 0) sb.append(" & ");
                    sb.append(upperBounds[i].getTypeName());
                }
            }
            if (lowerBounds.length > 0) {
                sb.append(" super ");
                for (int i = 0; i < lowerBounds.length; i++) {
                    if (i > 0) sb.append(" & ");
                    sb.append(lowerBounds[i].getTypeName());
                }
            }
            return sb.toString();
        }
    }
}
