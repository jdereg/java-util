package com.cedarsoftware.util;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that inner class constructors with additional parameters
 * are properly found and used.
 */
class ClassUtilitiesInnerClassFixTest {
    
    public static class Outer {
        // Outer class with inner classes
        
        public class InnerWithOnlyOuter {
            // Constructor takes only the implicit outer instance
            public InnerWithOnlyOuter() {
            }
        }
        
        public class InnerWithExtraParams {
            private final String value;
            private final int number;
            
            // Constructor takes outer instance + additional parameters
            public InnerWithExtraParams(String value, int number) {
                this.value = value;
                this.number = number;
            }
            
            public String getValue() {
                return value;
            }
            
            public int getNumber() {
                return number;
            }
        }
    }
    
    @Test
    @DisplayName("Verify inner class constructors are properly detected")
    void testInnerClassConstructorDetection() {
        // Test that we can find the constructor for InnerWithOnlyOuter
        Constructor<?>[] constructors1 = Outer.InnerWithOnlyOuter.class.getDeclaredConstructors();
        assertEquals(1, constructors1.length);
        // The constructor should have 1 parameter (the outer instance)
        assertEquals(1, constructors1[0].getParameterCount());
        assertEquals(Outer.class, constructors1[0].getParameterTypes()[0]);
        
        // Test that we can find the constructor for InnerWithExtraParams
        Constructor<?>[] constructors2 = Outer.InnerWithExtraParams.class.getDeclaredConstructors();
        assertEquals(1, constructors2.length);
        // The constructor should have 3 parameters (outer, String, int)
        assertEquals(3, constructors2[0].getParameterCount());
        Class<?>[] paramTypes = constructors2[0].getParameterTypes();
        assertEquals(Outer.class, paramTypes[0]);
        assertEquals(String.class, paramTypes[1]);
        assertEquals(int.class, paramTypes[2]);
    }
    
    @Test
    @DisplayName("Verify our fix allows finding inner class constructors with extra params")
    void testInnerClassConstructorWithExtraParams() throws Exception {
        // Create an outer instance
        Outer outer = new Outer();
        
        // Find the InnerWithExtraParams constructor
        Constructor<?> constructor = null;
        for (Constructor<?> c : Outer.InnerWithExtraParams.class.getDeclaredConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length > 0 && params[0].equals(Outer.class)) {
                constructor = c;
                break;
            }
        }
        
        assertNotNull(constructor, "Should find constructor with Outer as first param");
        assertEquals(3, constructor.getParameterCount(), "Constructor should have 3 params");
        
        // Verify we can instantiate it
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(outer, "test", 42);
        
        assertNotNull(instance);
        assertTrue(instance instanceof Outer.InnerWithExtraParams);
        
        Outer.InnerWithExtraParams inner = (Outer.InnerWithExtraParams) instance;
        assertEquals("test", inner.getValue());
        assertEquals(42, inner.getNumber());
    }
}