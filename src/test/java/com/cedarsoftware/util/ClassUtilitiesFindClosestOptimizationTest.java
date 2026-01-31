package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for findClosest() optimization.
 * Verifies that the method correctly finds the closest matching class using cached distance maps.
 */
class ClassUtilitiesFindClosestOptimizationTest {
    
    @Test
    @DisplayName("findClosest should return exact match when available")
    void testFindClosestExactMatch() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(String.class, "String");
        candidates.put(Integer.class, "Integer");
        candidates.put(Object.class, "Object");
        
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("String", result);
    }
    
    @Test
    @DisplayName("findClosest should find closest parent class")
    void testFindClosestParentClass() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Number.class, "Number");
        candidates.put(Object.class, "Object");
        candidates.put(Comparable.class, "Comparable");
        
        // Integer extends Number which is closer than Object
        String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
        assertEquals("Number", result);
    }
    
    @Test
    @DisplayName("findClosest should find closest interface")
    void testFindClosestInterface() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Comparable.class, "Comparable");
        candidates.put(Object.class, "Object");
        
        // Object is the direct superclass of String (distance 1)
        // Comparable is an interface implemented by String (different distance calculation)
        // Object wins as the closest match
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("Object", result);
    }
    
    @Test
    @DisplayName("findClosest should return default when no match found")
    void testFindClosestNoMatch() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Number.class, "Number");
        candidates.put(CharSequence.class, "CharSequence");
        
        // Thread has no inheritance relationship with Number or CharSequence
        String result = ClassUtilities.findClosest(Thread.class, candidates, "default");
        assertEquals("default", result);
    }
    
    @Test
    @DisplayName("findClosest should handle multiple candidates at same distance")
    void testFindClosestEqualDistance() {
        Map<Class<?>, String> candidates = new LinkedHashMap<>();  // Use LinkedHashMap for predictable order
        candidates.put(Comparable.class, "Comparable");
        candidates.put(CharSequence.class, "CharSequence");
        candidates.put(Object.class, "Object");
        
        // Object is the direct superclass with distance 1
        // Comparable and CharSequence are interfaces
        // Object wins as the closest match
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("Object", result);
    }
    
    @Test
    @DisplayName("findClosest should handle empty candidate map")
    void testFindClosestEmptyMap() {
        Map<Class<?>, String> candidates = new HashMap<>();
        
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("default", result);
    }
    
    @Test
    @DisplayName("findClosest should handle null default value")
    void testFindClosestNullDefault() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Number.class, "Number");
        
        // No match for String, should return null default
        String result = ClassUtilities.findClosest(String.class, candidates, null);
        assertNull(result);
    }
    
    @Test
    @DisplayName("findClosest should throw on null source class")
    void testFindClosestNullSource() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(String.class, "String");
        
        assertThrows(IllegalArgumentException.class, () ->
            ClassUtilities.findClosest(null, candidates, "default")
        );
    }
    
    @Test
    @DisplayName("findClosest should throw on null candidate map")
    void testFindClosestNullCandidates() {
        assertThrows(IllegalArgumentException.class, () ->
            ClassUtilities.findClosest(String.class, null, "default")
        );
    }
    
    @Test
    @DisplayName("findClosest performance with large candidate map")
    void testFindClosestPerformance() {
        // Create a large candidate map
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Object.class, "Object");
        candidates.put(Number.class, "Number");
        candidates.put(Integer.class, "Integer");
        candidates.put(Double.class, "Double");
        candidates.put(Float.class, "Float");
        candidates.put(Long.class, "Long");
        candidates.put(Short.class, "Short");
        candidates.put(Byte.class, "Byte");
        candidates.put(String.class, "String");
        candidates.put(StringBuilder.class, "StringBuilder");
        candidates.put(StringBuffer.class, "StringBuffer");
        candidates.put(CharSequence.class, "CharSequence");
        candidates.put(Comparable.class, "Comparable");
        candidates.put(Cloneable.class, "Cloneable");
        candidates.put(java.io.Serializable.class, "Serializable");
        
        // Test multiple lookups - the optimized version pulls the distance map once
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
            assertEquals("Integer", result);  // Exact match
        }
        long exactTime = System.nanoTime() - start;
        
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            String result = ClassUtilities.findClosest(BigInteger.class, candidates, "default");
            assertEquals("Number", result);  // Closest match
        }
        long inheritanceTime = System.nanoTime() - start;
        
        // The optimization should make both cases fast
        // Just verify they complete in reasonable time (not hanging)
        assertTrue(exactTime < 100_000_000);  // Less than 100ms for 1000 iterations
        assertTrue(inheritanceTime < 100_000_000);  // Less than 100ms for 1000 iterations
    }
    
    private static class BigInteger extends Number {
        @Override
        public int intValue() { return 0; }
        @Override
        public long longValue() { return 0; }
        @Override
        public float floatValue() { return 0; }
        @Override
        public double doubleValue() { return 0; }
    }
}