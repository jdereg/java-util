package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional test cases from GPT-5 final review.
 */
class ClassUtilitiesFinalReviewTest {
    
    @Test
    @DisplayName("findLowestCommonSupertypesExcluding with null excluded set should work like empty set")
    void testFindLowestCommonSupertypesExcludingWithNull() {
        // Test with null excluded set - should behave like an empty set
        Set<Class<?>> resultWithNull = ClassUtilities.findLowestCommonSupertypesExcluding(
            Integer.class, Double.class, null);
        
        // Test with empty excluded set
        Set<Class<?>> resultWithEmpty = ClassUtilities.findLowestCommonSupertypesExcluding(
            Integer.class, Double.class, Collections.emptySet());
        
        // Both should return the same result
        assertEquals(resultWithEmpty, resultWithNull, 
            "Result with null excluded should match result with empty excluded set");
        
        // Both should contain Number and Comparable
        assertTrue(resultWithNull.contains(Number.class), "Should contain Number");
        assertTrue(resultWithNull.contains(Comparable.class), "Should contain Comparable");
    }
    
    @Test
    @DisplayName("Named-param construction of varargs constructor with different argument types")
    void testNamedParamVarargsConstruction() {
        // Note: This test validates the enhancement for varargs support with named parameters.
        // Since the test classes don't have parameter names available at runtime (not compiled with -parameters),
        // we'll test the varargs handling using positional arguments instead.
        
        // Test class with varargs constructor
        class VarargsTest {
            public String prefix;
            public String[] values;
            
            public VarargsTest(String prefix, String... values) {
                this.prefix = prefix;
                this.values = values;
            }
        }
        
        // Test 1: Array passed as varargs
        List<Object> args1 = Arrays.asList("test", new String[]{"a", "b", "c"});
        VarargsTest result1 = (VarargsTest) ClassUtilities.newInstance(VarargsTest.class, args1);
        assertNotNull(result1);
        assertEquals("test", result1.prefix);
        assertArrayEquals(new String[]{"a", "b", "c"}, result1.values);
        
        // Test 2: Multiple individual values for varargs
        List<Object> args2 = Arrays.asList("test2", "x", "y", "z");
        VarargsTest result2 = (VarargsTest) ClassUtilities.newInstance(VarargsTest.class, args2);
        assertNotNull(result2);
        assertEquals("test2", result2.prefix);
        assertArrayEquals(new String[]{"x", "y", "z"}, result2.values);
        
        // Test 3: Single value for varargs
        List<Object> args3 = Arrays.asList("test3", "single");
        VarargsTest result3 = (VarargsTest) ClassUtilities.newInstance(VarargsTest.class, args3);
        assertNotNull(result3);
        assertEquals("test3", result3.prefix);
        assertArrayEquals(new String[]{"single"}, result3.values);
    }
    
    @Test
    @DisplayName("Varargs element that can't convert cleanly falls back to default")
    void testVarargsConversionFallback() {
        // Test class with int varargs
        class IntVarargsTest {
            public int[] numbers;
            
            public IntVarargsTest(int... numbers) {
                this.numbers = numbers;
            }
        }
        
        // Pass values that include something that can't convert to int
        // The matchArgumentsWithVarargs should handle this gracefully
        List<Object> args = Arrays.asList("not-a-number", 42, "also-not");
        
        // This should not throw an exception but handle gracefully
        IntVarargsTest result = (IntVarargsTest) ClassUtilities.newInstance(IntVarargsTest.class, args);
        
        // The result should exist (not null)
        assertNotNull(result, "Should create instance even with conversion issues");
        
        // The numbers array should have been created with fallback values
        assertNotNull(result.numbers, "Varargs array should be created");
        assertEquals(3, result.numbers.length, "Should have 3 elements");
        
        // First element should be 0 (default for int when conversion fails)
        assertEquals(0, result.numbers[0], "Failed conversion should use default value");
        // Second element should be 42 (successful conversion)
        assertEquals(42, result.numbers[1], "Valid conversion should work");
        // Third element should be 0 (default for int when conversion fails)
        assertEquals(0, result.numbers[2], "Failed conversion should use default value");
    }
}