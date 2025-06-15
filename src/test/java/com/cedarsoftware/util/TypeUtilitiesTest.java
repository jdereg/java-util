package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
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
public class TypeUtilitiesTest {

    // --- Helper Classes for Testing ---

    /**
     * A generic class with various generic fields.
     */
    public static class TestGeneric<T> {
        public T field;
        public T[] arrayField;
        public Collection<T> collectionField;
        public Map<String, T> mapField;
    }

    /**
     * A concrete subclass of TestGeneric that fixes T to Integer.
     */
    public static class TestConcrete extends TestGeneric<Integer> {
    }

    /**
     * A class with a field using a wildcard type.
     */
    public static class TestWildcard {
        public Collection<? extends Number> numbers;
    }

    /**
     * A class with a parameterized field.
     */
    public static class TestParameterized {
        public List<String> strings;
    }

    /**
     * A class with a Map field.
     */
    public static class TestMap {
        public Map<String, Double> map;
    }

    /**
     * A class with a Collection field.
     */
    public static class TestCollection {
        public Collection<String> collection;
    }

    /**
     * A custom implementation of ParameterizedType used in tests.
     */
    private static class CustomParameterizedType implements ParameterizedType {
        private final Type rawType;
        private final Type[] typeArguments;
        private final Type ownerType;

        public CustomParameterizedType(Type rawType, Type[] typeArguments, Type ownerType) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
            this.ownerType = ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }
    }

    /**
     * A helper class to capture a generic type using anonymous subclassing.
     */
    abstract static class TypeReference<T> {
        private final Type type;
        protected TypeReference() {
            ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
            this.type = superClass.getActualTypeArguments()[0];
        }
        public Type getType() {
            return this.type;
        }
    }

    // --- Tests for getRawClass ---

    @Test
    public void testGetRawClassWithNull() {
        assertNull(TypeUtilities.getRawClass(null));
    }

    @Test
    public void testGetRawClassWithClass() {
        assertEquals(String.class, TypeUtilities.getRawClass(String.class));
    }

    @Test
    public void testGetRawClassWithParameterizedType() throws Exception {
        Field field = TestParameterized.class.getField("strings");
        Type genericType = field.getGenericType();
        Class<?> raw = TypeUtilities.getRawClass(genericType);
        assertEquals(List.class, raw);
    }

    @Test
    public void testGetRawClassWithGenericArrayType() throws Exception {
        Field field = TestGeneric.class.getField("arrayField");
        Type genericType = field.getGenericType();
        assertTrue(genericType instanceof GenericArrayType);
        Class<?> raw = TypeUtilities.getRawClass(genericType);
        // Since TestGeneric<T> has an unbounded T, the first bound is Object,
        // so T[] becomes effectively Object[].
        assertEquals(Object[].class, raw);
    }

    @Test
    public void testGetRawClassWithWildcardType() throws Exception {
        Field field = TestWildcard.class.getField("numbers");
        ParameterizedType pType = (ParameterizedType) field.getGenericType();
        Type wildcard = pType.getActualTypeArguments()[0];
        assertTrue(wildcard instanceof WildcardType);
        Class<?> raw = TypeUtilities.getRawClass(wildcard);
        // For ? extends Number, the first upper bound is Number.
        assertEquals(Number.class, raw);
    }

    @Test
    public void testGetRawClassWithTypeVariable() throws Exception {
        Field field = TestGeneric.class.getField("field");
        Type typeVariable = field.getGenericType();
        assertTrue(typeVariable instanceof TypeVariable);
        // T is unbounded so its first bound is Object.
        Class<?> raw = TypeUtilities.getRawClass(typeVariable);
        assertEquals(Object.class, raw);
    }

    // --- Tests for extractArrayComponentType ---

    @Test
    public void testExtractArrayComponentTypeWithNull() {
        assertNull(TypeUtilities.extractArrayComponentType(null));
    }

    @Test
    public void testExtractArrayComponentTypeWithGenericArrayType() throws Exception {
        Field field = TestGeneric.class.getField("arrayField");
        Type genericType = field.getGenericType();
        Type componentType = TypeUtilities.extractArrayComponentType(genericType);
        // The component type of T[] is T, which is a TypeVariable.
        assertTrue(componentType instanceof TypeVariable);
    }

    @Test
    public void testExtractArrayComponentTypeWithClassArray() {
        Type componentType = TypeUtilities.extractArrayComponentType(String[].class);
        assertEquals(String.class, componentType);
    }

    @Test
    public void testExtractArrayComponentTypeWithNonArray() {
        assertNull(TypeUtilities.extractArrayComponentType(Integer.class));
    }

    // --- Tests for containsUnresolvedType ---

    @Test
    public void testHasUnresolvedTypeWithNull() {
        assertFalse(TypeUtilities.hasUnresolvedType(null));
    }

    @Test
    public void testHasUnresolvedTypeWithResolvedType() throws Exception {
        Field field = TestParameterized.class.getField("strings");
        Type type = field.getGenericType();
        // List<String> is fully resolved.
        assertFalse(TypeUtilities.hasUnresolvedType(type));
    }

    @Test
    public void testHasUnresolvedTypeWithUnresolvedType() throws Exception {
        Field field = TestGeneric.class.getField("field");
        Type type = field.getGenericType();
        // T is unresolved.
        assertTrue(TypeUtilities.hasUnresolvedType(type));
    }

    @Test
    public void testHasUnresolvedTypeWithGenericArrayType() throws Exception {
        Field field = TestGeneric.class.getField("arrayField");
        Type type = field.getGenericType();
        // The component type T is unresolved.
        assertTrue(TypeUtilities.hasUnresolvedType(type));
    }

    // --- Tests for resolveTypeUsingInstance ---

    @Test
    public void testResolveTypeUsingInstanceWithTypeVariable() throws Exception {
        TestConcrete instance = new TestConcrete();
        Field field = TestGeneric.class.getField("field");
        Type type = field.getGenericType(); // T
        // For a TestConcrete instance, T resolves to Integer.
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, type);
        assertEquals(Integer.class, resolved);
    }

    @Test
    public void testResolveTypeUsingInstanceWithParameterizedType() throws Exception {
        TestConcrete instance = new TestConcrete();
        Field field = TestGeneric.class.getField("collectionField");
        Type type = field.getGenericType(); // Collection<T>
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, type);
        assertTrue(resolved instanceof ParameterizedType);
        ParameterizedType pt = (ParameterizedType) resolved;
        assertEquals(Collection.class, TypeUtilities.getRawClass(pt.getRawType()));
        assertEquals(Integer.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    public void testResolveTypeUsingInstanceWithGenericArrayType() throws Exception {
        TestConcrete instance = new TestConcrete();
        Field field = TestGeneric.class.getField("arrayField");
        Type type = field.getGenericType(); // T[]
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, type);
        assertEquals("java.lang.Integer[]", resolved.getTypeName());
        // Expect a Class representing Integer[].
        Class<?> resolvedClass = TypeUtilities.getRawClass(resolved);
        assertTrue(resolvedClass instanceof Class);
        assertTrue(resolvedClass.isArray());
        assertEquals(Integer.class, resolvedClass.getComponentType());
    }

    @Test
    public void testResolveTypeUsingInstanceWithWildcardType() throws Exception {
        TestWildcard instance = new TestWildcard();
        Field field = TestWildcard.class.getField("numbers");
        ParameterizedType pType = (ParameterizedType) field.getGenericType();
        Type wildcard = pType.getActualTypeArguments()[0];
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, wildcard);
        // The wildcard should remain as ? extends Number.
        assertTrue(resolved instanceof WildcardType);
        assertTrue(resolved.toString().contains("extends " + Number.class.getName()));
    }

    @Test
    public void testResolveTypeUsingInstanceWithClass() {
        Type resolved = TypeUtilities.resolveTypeUsingInstance(new Object(), String.class);
        assertEquals(String.class, resolved);
    }

    // --- Tests for resolveTypeRecursivelyUsingParent ---

    @Test
    public void testResolveTypeRecursivelyUsingParentWithTypeVariable() throws Exception {
        // Using TestConcrete's generic superclass: TestGeneric<Integer>
        Type parentType = TestConcrete.class.getGenericSuperclass();
        Field field = TestGeneric.class.getField("field");
        Type type = field.getGenericType(); // T
        Type resolved = TypeUtilities.resolveType(parentType, type);
        assertEquals(Integer.class, resolved);
    }

    @Test
    public void testResolveTypeRecursivelyUsingParentWithParameterizedType() throws Exception {
        Type parentType = TestConcrete.class.getGenericSuperclass();
        Field field = TestGeneric.class.getField("collectionField");
        Type type = field.getGenericType(); // Collection<T>
        Type resolved = TypeUtilities.resolveType(parentType, type);
        assertTrue(resolved instanceof ParameterizedType);
        ParameterizedType pt = (ParameterizedType) resolved;
        assertEquals(Collection.class, TypeUtilities.getRawClass(pt.getRawType()));
        assertEquals(Integer.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    public void testResolveTypeRecursivelyUsingParentWithGenericArrayType() throws Exception {
        Type parentType = TestConcrete.class.getGenericSuperclass();
        Field field = TestGeneric.class.getField("arrayField");
        Type type = field.getGenericType(); // T[]
        Type resolved = TypeUtilities.resolveType(parentType, type);
        // Should resolve to Integer[].
        assertTrue("java.lang.Integer[]".equals(resolved.getTypeName()));
        Class<?> arrayClass = (Class<?>) TypeUtilities.getRawClass(resolved);
        assertTrue(arrayClass.isArray());
        assertEquals(Integer.class, arrayClass.getComponentType());
    }

    @Test
    public void testResolveTypeRecursivelyUsingParentWithWildcardType() throws Exception {
        Type parentType = TestWildcard.class.getGenericSuperclass();
        Field field = TestWildcard.class.getField("numbers");
        ParameterizedType pType = (ParameterizedType) field.getGenericType();
        Type wildcard = pType.getActualTypeArguments()[0];
        Type resolved = TypeUtilities.resolveType(parentType, wildcard);
        // Should remain as ? extends Number.
        assertTrue(resolved instanceof WildcardType);
        assertTrue(resolved.toString().contains("extends " + Number.class.getName()));
    }

    // --- Test for resolveFieldTypeUsingParent ---

    @Test
    public void testResolveFieldTypeUsingParent() throws Exception {
        Type parentType = TestConcrete.class.getGenericSuperclass();
        Field field = TestGeneric.class.getField("field");
        Type type = field.getGenericType(); // T
        Type resolved = TypeUtilities.resolveType(parentType, type);
        assertEquals(Integer.class, resolved);
    }

    // --- Tests for resolveSuggestedType ---

    @Test
    public void testInferElementTypeForMap() throws Exception {
        Field field = TestMap.class.getField("map");
        Type suggestedType = field.getGenericType(); // Map<String, Double>
        // For a Map, the method should select the second type argument (the value type).
        Type resolved = TypeUtilities.inferElementType(suggestedType, Object.class);
        assertEquals(Double.class, resolved);
    }

    @Test
    public void testInferElementTypeForCollection() throws Exception {
        Field field = TestCollection.class.getField("collection");
        Type suggestedType = field.getGenericType(); // Collection<String>
        // For a Collection, the method should select the first (and only) type argument.
        Type resolved = TypeUtilities.inferElementType(suggestedType, Object.class);
        assertEquals(String.class, resolved);
    }

    @Test
    public void testInferElementTypeForArray() throws Exception {
        // Create a custom ParameterizedType whose raw type is an array.
        ParameterizedType arrayType = new CustomParameterizedType(String[].class, new Type[]{String.class}, null);
        Type resolved = TypeUtilities.inferElementType(arrayType, Object.class);
        assertEquals(String.class, resolved);
    }

    @Test
    public void testInferElementTypeForNonParameterizedType() {
        // If suggestedType is not a ParameterizedType, the fieldGenericType should be returned as-is.
        Type resolved = TypeUtilities.inferElementType(String.class, Integer.class);
        assertEquals(Integer.class, resolved);
    }

    @Test
    public void testInferElementTypeForOther() throws Exception {
        // For a ParameterizedType that is neither a Map, Collection, nor an array, the method returns Object.class.
        ParameterizedType optionalType = (ParameterizedType) new TypeReference<Optional<String>>(){}.getType();
        Type resolved = TypeUtilities.inferElementType(optionalType, Object.class);
        assertEquals(Object.class, resolved);
    }

    @Test
    public void testGetRawClassElseClause() {
        // A simple implementation of Type that is not a Class.
        class NonClassType implements Type {
            @Override
            public String getTypeName() {
                return "NonClassType";
            }
        }

        // Create a custom ParameterizedType that returns a NonClassType from getRawType().
        ParameterizedType dummyParameterizedType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[0];
            }
            @Override
            public Type getRawType() {
                return new NonClassType();
            }
            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            TypeUtilities.getRawClass(dummyParameterizedType);
        });
        assertTrue(exception.getMessage().contains("Unexpected raw type:"));
    }

    @Test
    public void testGetRawClassWildcardEmptyUpperBounds() {
        // Create a custom WildcardType with empty upper bounds.
        WildcardType customWildcard = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[0];  // empty upper bounds to trigger the default
            }
            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };

        // When upper bounds is empty, getRawClass() should return Object.class.
        Class<?> result = TypeUtilities.getRawClass(customWildcard);
        assertEquals(Object.class, result);
    }

    @Test
    public void testGetRawClassTypeVariableNoBounds() {
        // Create a dummy GenericDeclaration for our dummy TypeVariable.
        GenericDeclaration dummyDeclaration = new GenericDeclaration() {
            @Override
            public TypeVariable<?>[] getTypeParameters() {
                return new TypeVariable[0];
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }
        };

        // Create a dummy TypeVariable with an empty bounds array.
        TypeVariable<GenericDeclaration> dummyTypeVariable = new TypeVariable<GenericDeclaration>() {
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Type[] getBounds() {
                return new Type[0]; // No bounds, so the safe default should trigger.
            }
            @Override
            public GenericDeclaration getGenericDeclaration() {
                return dummyDeclaration;
            }
            @Override
            public String getName() {
                return "DummyTypeVariable";
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return new AnnotatedType[0];
            }

            @Override
            public String toString() {
                return getName();
            }
        };

        // When the bounds array is empty, getRawClass() should return Object.class.
        Class<?> result = TypeUtilities.getRawClass(dummyTypeVariable);
        assertEquals(Object.class, result);
    }

    @Test
    public void testGetRawClassUnknownType() {
        // Create an anonymous implementation of Type that is not one of the known types.
        Type unknownType = new Type() {
            @Override
            public String toString() {
                return "UnknownType";
            }
        };

        // Expect an IllegalArgumentException when calling getRawClass with this unknown type.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            TypeUtilities.getRawClass(unknownType);
        });
        assertTrue(thrown.getMessage().contains("Unknown type:"));
    }
    
    @Test
    public void testHasUnresolvedTypeReturnsTrueForParameterizedTypeWithUnresolvedArg() throws Exception {
        // Obtain the ParameterizedType representing Collection<T>
        Field field = TestGeneric.class.getField("collectionField");
        Type type = field.getGenericType();

        // The type argument T is unresolved, so containsUnresolvedType should return true.
        assertTrue(TypeUtilities.hasUnresolvedType(type));
    }

    @Test
    public void testHasUnresolvedTypeForWildcardWithUnresolvedUpperBound() {
        // Create a dummy GenericDeclaration required by the TypeVariable interface.
        GenericDeclaration dummyDeclaration = new GenericDeclaration() {
            @Override
            public TypeVariable<?>[] getTypeParameters() {
                return new TypeVariable[0];
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }
        };

        // Create a dummy TypeVariable to simulate an unresolved type.
        TypeVariable<GenericDeclaration> dummyTypeVariable = new TypeVariable<GenericDeclaration>() {
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Type[] getBounds() {
                // Even if a bound is provided, being a TypeVariable makes it unresolved.
                return new Type[]{ Object.class };
            }
            @Override
            public GenericDeclaration getGenericDeclaration() {
                return dummyDeclaration;
            }
            @Override
            public String getName() {
                return "T";
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return new AnnotatedType[0];
            }

            @Override
            public String toString() {
                return getName();
            }
        };

        // Create a custom WildcardType whose upper bound is the dummy TypeVariable.
        WildcardType customWildcard = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[]{ dummyTypeVariable };
            }
            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };

        // When the wildcard's upper bound is unresolved (i.e. a TypeVariable),
        // containsUnresolvedType should return true.
        assertTrue(TypeUtilities.hasUnresolvedType(customWildcard));
    }

    @Test
    public void testHasUnresolvedTypeForWildcardWithUnresolvedLowerBound() {
        // Create a dummy GenericDeclaration required by the TypeVariable interface.
        GenericDeclaration dummyDeclaration = new GenericDeclaration() {
            @Override
            public TypeVariable<?>[] getTypeParameters() {
                return new TypeVariable[0];
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }
        };

        // Create a dummy TypeVariable to simulate an unresolved type.
        TypeVariable<GenericDeclaration> dummyTypeVariable = new TypeVariable<GenericDeclaration>() {
            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Type[] getBounds() {
                // Although a bound is provided, the mere fact that this is a TypeVariable makes it unresolved.
                return new Type[]{ Object.class };
            }
            @Override
            public GenericDeclaration getGenericDeclaration() {
                return dummyDeclaration;
            }
            @Override
            public String getName() {
                return "T";
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return new AnnotatedType[0];
            }

            @Override
            public String toString() {
                return getName();
            }
        };

        // Create a custom WildcardType whose lower bounds array includes the dummy TypeVariable.
        WildcardType customWildcard = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[0];
            }
            @Override
            public Type[] getLowerBounds() {
                return new Type[]{ dummyTypeVariable };
            }
        };

        // The lower bounds contain an unresolved type variable, so containsUnresolvedType should return true.
        assertTrue(TypeUtilities.hasUnresolvedType(customWildcard));
    }

    @Test
    void testResolveTypeUsingInstanceWithNullNull() {
        assertThrows(IllegalArgumentException.class, () -> TypeUtilities.resolveTypeUsingInstance(null, null));
    }

    @Test
    public void testResolveTypeUsingInstanceWildcardLowerBounds() {
        // Create a custom WildcardType with a non-empty lower bounds array.
        WildcardType customWildcard = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                // For this test, the upper bound can be a concrete type.
                return new Type[] { Object.class };
            }
            @Override
            public Type[] getLowerBounds() {
                // The lower bounds array is non-empty to force execution of the lower bounds loop.
                return new Type[] { String.class };
            }
        };

        Object target = new Object();
        Type resolved = TypeUtilities.resolveTypeUsingInstance(target, customWildcard);

        // Verify that the resolved type is a WildcardType (specifically, an instance of WildcardTypeImpl)
        assertTrue(resolved instanceof WildcardType, "Resolved type should be a WildcardType");

        WildcardType resolvedWildcard = (WildcardType) resolved;
        // Verify that the lower bounds were processed and remain String.class.
        Type[] lowerBounds = resolvedWildcard.getLowerBounds();
        assertEquals(1, lowerBounds.length, "Expected one lower bound");
        assertEquals(String.class, lowerBounds[0], "Lower bound should resolve to String.class");
    }

    @Test
    public void testResolveTypeRecursivelyUsingParentLowerBoundsLoop() {
        // Use the generic superclass of TestConcrete as the parent type.
        // This should be TestGeneric<Integer>, where T is resolved to Integer.
        Type parentType = TestConcrete.class.getGenericSuperclass();

        // Obtain the type variable T from TestGeneric.
        TypeVariable<?> typeVariable = TestGeneric.class.getTypeParameters()[0];

        // Create a custom WildcardType whose lower bounds array contains the type variable T.
        WildcardType customWildcard = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                // Provide a simple upper bound.
                return new Type[]{ Object.class };
            }

            @Override
            public Type[] getLowerBounds() {
                // Return a non-empty lower bounds array to force the loop.
                return new Type[]{ typeVariable };
            }
        };

        // Call resolveTypeRecursivelyUsingParent. The method will recursively resolve the lower bound T
        // using the parent type, replacing T with Integer.
        Type resolved = TypeUtilities.resolveType(parentType, customWildcard);

        // The resolved type should be a WildcardType with its lower bound resolved to Integer.
        assertTrue(resolved instanceof WildcardType, "Resolved type should be a WildcardType");
        WildcardType resolvedWildcard = (WildcardType) resolved;
        Type[] lowerBounds = resolvedWildcard.getLowerBounds();
        assertEquals(1, lowerBounds.length, "Expected one lower bound");
        assertEquals(Integer.class, lowerBounds[0], "The lower bound should be resolved to Integer");
    }

    @Test
    public void testResolveFieldTypeUsingParentReturnsOriginalType() throws Exception {
        // Obtain the type variable T from the field "field" in TestGeneric.
        Field field = TestGeneric.class.getField("field");
        Type typeToResolve = field.getGenericType();  // This is a TypeVariable representing T.

        // Use the raw class (TestGeneric.class) as the parent type,
        // which is not a ParameterizedType.
        Type parentType = TestGeneric.class;

        // Since parentType is not a ParameterizedType, the method should fall through
        // and return typeToResolve unchanged.
        Type resolved = TypeUtilities.resolveType(parentType, typeToResolve);

        // Verify that the returned type is the same as the original typeToResolve.
        assertEquals(typeToResolve, resolved);
    }

    // Define a generic interface with a type parameter.
    public interface MyInterface<T> { }

    // A base class that implements MyInterface with a concrete type (String).
    public static class Base implements MyInterface<String> { }

    // A subclass of Base that does not add any new generic parameters.
    public static class Sub extends Base { }

    @Test
    public void testResolveTypeVariableThroughGenericInterface() {
        // Retrieve the type variable declared on MyInterface.
        TypeVariable<?> typeVariable = MyInterface.class.getTypeParameters()[0];

        // Create an instance of Sub.
        Sub instance = new Sub();

        // Call resolveTypeUsingInstance on the type variable.
        // This will eventually call resolveTypeVariable() which will iterate over
        // the generic interfaces of the supertypes (Base implements MyInterface<String>).
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, typeVariable);

        // Since Base implements MyInterface<String>, the type variable T should be resolved to String.
        assertEquals(String.class, resolved);
    }

    // A dummy generic class with an unresolved type variable.
    public static class Dummy<T> { }
    
    @Test
    public void testFirstBoundPathInResolveTypeUsingInstance() throws Exception {
        // Retrieve the generic type of the field "field" from TestGeneric (this is a TypeVariable T).
        Field field = TestGeneric.class.getField("field");
        Type typeVariable = field.getGenericType();

        // Create an instance of TestGeneric using the raw type.
        // This instance does not provide any concrete type for T.
        TestGeneric rawInstance = new TestGeneric();

        // When we call resolveTypeUsingInstance with a raw instance, no resolution occurs,
        // so resolveTypeVariable returns null and the fallback (firstBound) is used.
        Type resolved = TypeUtilities.resolveTypeUsingInstance(rawInstance, typeVariable);

        // For an unbounded type variable, firstBound(tv) returns the first bound,
        // which defaults to Object.class.
        assertEquals(Object.class, resolved);
    }

    @Test
    public void testParameterizedTypeImplToString() throws Exception {
        // Create an instance of TestParameterized.
        TestParameterized instance = new TestParameterized();

        // Use reflection to obtain the field 'strings', declared as List<String>.
        Field field = TestParameterized.class.getField("strings");
        Type genericType = field.getGenericType();

        // Resolve the type using the instance.
        // This should return an instance of ParameterizedTypeImpl.
        Type resolved = TypeUtilities.resolveTypeUsingInstance(instance, genericType);

        // Call toString() on the resolved type.
        String typeString = resolved.toString();

        // For List<String>, the expected string is "java.util.List<java.lang.String>"
        assertEquals("java.util.List<java.lang.String>", typeString, "The toString() output is not as expected.");
    }

    // A generic interface declaring a type variable T.
    public interface AnInterface<T> {
        T get();
    }

    // Grandparent implements the generic interface.
    public static class Grandparent<T> implements AnInterface<T> {
        public T value;

        @Override
        public T get() {
            return value;
        }
    }

    // Parent extends Grandparent, preserving the type variable.
    public static class Parent<U> extends Grandparent<U> { }

    // Child concretely binds the type variable (via Parent) to Double.
    public static class Child extends Parent<Double> { }

    @Test
    public void testResolveTypeUsingGrandparentInterface() throws Exception {
        // Retrieve the generic return type from AnInterface.get(), which is T.
        Method getMethod = AnInterface.class.getMethod("get");
        Type interfaceReturnType = getMethod.getGenericReturnType(); // This is the TypeVariable from AnInterface

        // Use Child.class as the resolution context.
        // Since Child extends Parent<Double> and Parent extends Grandparent<Double> (which implements AnInterface<Double>),
        // the type variable T should resolve to Double.
        Type startingType = Child.class;

        Type resolved = TypeUtilities.resolveType(startingType, interfaceReturnType);

        // The expected resolved type is Double.
        assertEquals(Double.class, resolved,
                "Expected the type variable declared in AnInterface (implemented by Grandparent) to resolve to Double");
    }

    @Test
    public void testGetGenericComponentTypeFromResolveType() throws Exception {
        Type parentType = TestConcrete.class.getGenericSuperclass();
        Field field = TestGeneric.class.getField("arrayField");
        Type arrayType = field.getGenericType();

        Type resolved = TypeUtilities.resolveType(parentType, arrayType);

        assertTrue(resolved instanceof GenericArrayType, "Should be GenericArrayType");
        GenericArrayType gat = (GenericArrayType) resolved;
        assertEquals(Integer.class, gat.getGenericComponentType(), "Component should resolve to Integer.class");
    }

    @Test
    public void testSetTypeResolveCacheWithNull() {
        assertThrows(IllegalArgumentException.class, () -> TypeUtilities.setTypeResolveCache(null));
    }

    @Test
    public void testSetTypeResolveCacheUsesProvidedMap() throws Exception {
        Map<Map.Entry<Type, Type>, Type> customCache = new ConcurrentHashMap<>();
        TypeUtilities.setTypeResolveCache(customCache);

        Field field = TestGeneric.class.getField("field");
        TypeUtilities.resolveType(TestConcrete.class, field.getGenericType());

        assertFalse(customCache.isEmpty(), "Cache should contain resolved entry");

        TypeUtilities.setTypeResolveCache(new LRUCache<>(2000));
    }
}