package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

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
        if (typeToResolve instanceof TypeVariable) {
            // Attempt to resolve the type variable using the target's class.
            TypeVariable<?> tv = (TypeVariable<?>) typeToResolve;
            Class<?> targetClass = target.getClass();
            Type resolved = resolveTypeVariable(targetClass, tv);
            return resolved != null ? resolved : firstBound(tv);
        } else if (typeToResolve instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) typeToResolve;
            Type[] actualArgs = pt.getActualTypeArguments();
            Type[] resolvedArgs = new Type[actualArgs.length];
            for (int i = 0; i < actualArgs.length; i++) {
                resolvedArgs[i] = resolveTypeUsingInstance(target, actualArgs[i]);
            }
            return new ParameterizedTypeImpl((Class<?>) pt.getRawType(), resolvedArgs, pt.getOwnerType());
        } else if (typeToResolve instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) typeToResolve;
            Type compType = gat.getGenericComponentType();
            Type resolvedCompType = resolveTypeUsingInstance(target, compType);
            return new GenericArrayTypeImpl(resolvedCompType);
        } else if (typeToResolve instanceof WildcardType) {
            WildcardType wt = (WildcardType) typeToResolve;
            Type[] upperBounds = wt.getUpperBounds();
            Type[] lowerBounds = wt.getLowerBounds();
            // Resolve bounds recursively.
            for (int i = 0; i < upperBounds.length; i++) {
                upperBounds[i] = resolveTypeUsingInstance(target, upperBounds[i]);
            }
            for (int i = 0; i < lowerBounds.length; i++) {
                lowerBounds[i] = resolveTypeUsingInstance(target, lowerBounds[i]);
            }
            return new WildcardTypeImpl(upperBounds, lowerBounds);
        } else {
            return typeToResolve;
        }
    }

    /**
     * Recursively resolves the declared generic type using the type information from its parent.
     * <p>
     * This method examines the supplied {@code typeToResolve} and, if it is a parameterized type,
     * generic array type, wildcard type, or type variable, it recursively substitutes any type variables
     * with the corresponding actual type arguments as defined in the {@code parentType}. For parameterized
     * types, each actual type argument is recursively resolved; for generic array types, the component
     * type is resolved; for wildcard types, both upper and lower bounds are resolved; and for type variables,
     * the {@code resolveTypeUsingParent(parentType, typeToResolve)} helper is used.
     * </p>
     * <p>
     * If the {@code typeToResolve} is a simple (non-generic) type or is already fully resolved, the original
     * {@code typeToResolve} is returned.
     * </p>
     *
     * @param parentType the full generic type of the parent object (e.g. the type of the enclosing class)
     *                   which provides context for resolving type variables in {@code typeToResolve}.
     * @param typeToResolve the declared generic type of the field or argument that may contain type variables, wildcards,
     *                  parameterized types, or generic array types.
     * @return the fully resolved type with all type variables replaced by their actual type arguments as
     *         determined by the {@code parentType}. If resolution is not necessary, returns {@code typeToResolve} unchanged.
     * @see #resolveFieldTypeUsingParent(Type, Type)
     * @see TypeUtilities#getRawClass(Type)
     */
    public static Type resolveTypeRecursivelyUsingParent(Type parentType, Type typeToResolve) {
        if (typeToResolve instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) typeToResolve;
            Type[] args = pt.getActualTypeArguments();
            Type[] resolvedArgs = new Type[args.length];
            for (int i = 0; i < args.length; i++) {
                resolvedArgs[i] = resolveTypeRecursivelyUsingParent(parentType, args[i]);
            }
            return new ParameterizedTypeImpl((Class<?>) pt.getRawType(), resolvedArgs, pt.getOwnerType());
        } else if (typeToResolve instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) typeToResolve;
            Type compType = gat.getGenericComponentType();
            Type resolvedCompType = resolveTypeRecursivelyUsingParent(parentType, compType);
            return new GenericArrayTypeImpl(resolvedCompType);
        } else if (typeToResolve instanceof WildcardType) {
            WildcardType wt = (WildcardType) typeToResolve;
            Type[] upperBounds = wt.getUpperBounds();
            Type[] lowerBounds = wt.getLowerBounds();
            for (int i = 0; i < upperBounds.length; i++) {
                upperBounds[i] = resolveTypeRecursivelyUsingParent(parentType, upperBounds[i]);
            }
            for (int i = 0; i < lowerBounds.length; i++) {
                lowerBounds[i] = resolveTypeRecursivelyUsingParent(parentType, lowerBounds[i]);
            }
            return new WildcardTypeImpl(upperBounds, lowerBounds);
        } else if (typeToResolve instanceof TypeVariable) {
            return resolveFieldTypeUsingParent(parentType, typeToResolve);
        } else {
            return typeToResolve;
        }
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
     * Attempts to resolve a type variable by inspecting the target class's generic superclass
     * and generic interfaces. This method recursively inspects all supertypes (including interfaces)
     * of the target class.
     *
     * @param targetClass the class in which to resolve the type variable
     * @param typeToResolve          the type variable to resolve
     * @return the resolved type, or null if resolution fails
     */
    private static Type resolveTypeVariable(Class<?> targetClass, TypeVariable<?> typeToResolve) {
        // Use getAllSupertypes() to traverse the full hierarchy
        for (Class<?> supertype : ClassUtilities.getAllSupertypes(targetClass)) {
            // Check the generic superclass of the current supertype.
            Type genericSuper = supertype.getGenericSuperclass();
            Type resolved = resolveTypeVariableFromParentType(genericSuper, typeToResolve);
            if (resolved != null) {
                return resolved;
            }
            // Check each generic interface of the current supertype.
            for (Type genericInterface : supertype.getGenericInterfaces()) {
                resolved = resolveTypeVariableFromParentType(genericInterface, typeToResolve);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
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
