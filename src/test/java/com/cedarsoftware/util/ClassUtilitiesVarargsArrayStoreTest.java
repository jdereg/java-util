package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for varargs ArrayStoreException prevention.
 * Verifies that incompatible types are properly handled when packing into varargs arrays.
 */
class ClassUtilitiesVarargsArrayStoreTest {
    
    private final Converter converter = new Converter(new DefaultConverterOptions());
    
    // Test class with varargs methods
    public static class VarargsTestClass {
        public String result;
        
        public VarargsTestClass(String... args) {
            result = "strings:" + String.join(",", args);
        }
        
        public VarargsTestClass(int fixed, String... args) {
            result = "int-strings:" + fixed + ":" + String.join(",", args);
        }
        
        public VarargsTestClass(Integer... numbers) {
            StringBuilder sb = new StringBuilder("integers:");
            for (Integer n : numbers) {
                sb.append(n).append(",");
            }
            result = sb.toString();
        }
    }
    
    @Test
    @DisplayName("Varargs with incompatible types should handle gracefully")
    void testVarargsIncompatibleTypes() {
        // Try to pass incompatible types - converter will try to convert them
        Map<String, Object> args = new HashMap<>();
        args.put("args", Arrays.asList("hello", 123, true));
        
        // Should convert numbers and booleans to strings
        VarargsTestClass instance = (VarargsTestClass)
            ClassUtilities.newInstance(converter, VarargsTestClass.class, args);
        
        assertNotNull(instance);
        assertTrue(instance.result.contains("hello"));
        assertTrue(instance.result.contains("123"));
        assertTrue(instance.result.contains("true"));
    }
    
    @Test
    @DisplayName("Varargs with convertible types should work")
    void testVarargsConvertibleTypes() {
        // Pass numbers that can be converted to strings
        VarargsTestClass instance = (VarargsTestClass)
            ClassUtilities.newInstance(converter, VarargsTestClass.class, 
                Arrays.asList("hello", 123, 45.6, true));
        
        assertNotNull(instance);
        // The numbers should be converted to strings
        assertTrue(instance.result.contains("hello"));
        assertTrue(instance.result.contains("123"));
        assertTrue(instance.result.contains("45.6"));
        assertTrue(instance.result.contains("true"));
    }
    
    @Test
    @DisplayName("Varargs with fixed params and mixed types")
    void testVarargsWithFixedParamsIncompatible() {
        // Try constructor with (int, String...)
        Map<String, Object> args = new HashMap<>();
        args.put("fixed", 42);
        args.put("args", Arrays.asList("hello", 123));
        
        // Should convert the number to string
        VarargsTestClass instance = (VarargsTestClass)
            ClassUtilities.newInstance(converter, VarargsTestClass.class, args);
        
        assertNotNull(instance);
        assertTrue(instance.result.contains("42"));
        assertTrue(instance.result.contains("hello"));
        assertTrue(instance.result.contains("123"));
    }
    
    @Test
    @DisplayName("Varargs with primitive component type")
    void testVarargsPrimitiveComponentType() {
        // Test class with primitive varargs
        class PrimitiveVarargs {
            public int sum;
            public PrimitiveVarargs(int... values) {
                sum = 0;
                for (int v : values) {
                    sum += v;
                }
            }
        }
        
        // Pass Integer objects that should be unboxed to int
        PrimitiveVarargs instance = (PrimitiveVarargs)
            ClassUtilities.newInstance(converter, PrimitiveVarargs.class,
                Arrays.asList(10, 20, 30));
        
        assertNotNull(instance);
        assertEquals(60, instance.sum);
    }
    
    @Test
    @DisplayName("Varargs with null values should be handled")
    void testVarargsWithNulls() {
        // Nulls in varargs should be handled gracefully
        VarargsTestClass instance = (VarargsTestClass)
            ClassUtilities.newInstance(converter, VarargsTestClass.class,
                Arrays.asList("hello", null, "world"));
        
        assertNotNull(instance);
        assertTrue(instance.result.contains("hello"));
        assertTrue(instance.result.contains("world"));
    }
    
    @Test
    @DisplayName("Empty varargs should create empty array")
    void testEmptyVarargs() {
        // No arguments for varargs should create empty array
        VarargsTestClass instance = (VarargsTestClass)
            ClassUtilities.newInstance(converter, VarargsTestClass.class,
                Arrays.asList());
        
        assertNotNull(instance);
        // May pick either constructor - both are valid for empty args
        assertTrue(instance.result.equals("strings:") || instance.result.equals("int-strings:0:"),
                "Expected 'strings:' or 'int-strings:0:' but got: " + instance.result);
    }
}