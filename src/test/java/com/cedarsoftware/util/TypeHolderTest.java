package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

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
class TypeHolderTest {

    // A raw subclass of TypeHolder that does not provide a type parameter.
    private static class RawHolder extends TypeHolder { }

    @Test
    void testAnonymousSubclassCapturesGenericType() {
        // Create an anonymous subclass capturing List<String>
        TypeHolder<List<String>> holder = new TypeHolder<List<String>>() {};
        Type type = holder.getType();

        // Ensure that the captured type is a ParameterizedType
        assertTrue(type instanceof ParameterizedType, "Captured type should be a ParameterizedType");
        ParameterizedType pType = (ParameterizedType) type;

        // Check that the raw type is List.class
        assertEquals(List.class, pType.getRawType(), "Raw type should be java.util.List");

        // Check that the actual type argument is String.class
        Type[] typeArgs = pType.getActualTypeArguments();
        assertEquals(1, typeArgs.length, "There should be one type argument");
        assertEquals(String.class, typeArgs[0], "Type argument should be java.lang.String");
    }

    @Test
    void testStaticOfMethodWithRawClass() {
        // Use the static of() method with a raw class (String.class)
        TypeHolder<String> holder = TypeHolder.of(String.class);
        Type type = holder.getType();

        // The type should be exactly String.class
        assertEquals(String.class, type, "The type should be java.lang.String");
    }

    @Test
    void testStaticOfMethodWithParameterizedType() {
        // Create a TypeHolder via anonymous subclass to capture a parameterized type (List<Integer>)
        TypeHolder<List<Integer>> holder = new TypeHolder<List<Integer>>() {};
        Type capturedType = holder.getType();

        // Use the static of() method to wrap the captured type
        TypeHolder<List<Integer>> holder2 = TypeHolder.of(capturedType);
        Type type2 = holder2.getType();

        // The type from holder2 should equal the captured type
        assertEquals(capturedType, type2, "The type from the of() method should match the captured type");
    }

    @Test
    void testToStringMethod() {
        // Create a TypeHolder using the of() method with a raw class
        TypeHolder<Integer> holder = TypeHolder.of(Integer.class);
        String typeString = holder.toString();

        // For a raw class, toString() returns the class name prefixed with "class "
        assertEquals("class java.lang.Integer", typeString, "toString() should return the underlying type's toString()");
    }

    @Test
    void testNoTypeParameterThrowsException() {
        // Creating a raw subclass (without a generic type parameter) should trigger an exception.
        assertThrows(IllegalArgumentException.class, () -> {
            new RawHolder();
        });
    }

    @Test
    void testNull() {
        assertThrows(IllegalArgumentException.class, () -> new TypeHolder<>(null));
    }
}
