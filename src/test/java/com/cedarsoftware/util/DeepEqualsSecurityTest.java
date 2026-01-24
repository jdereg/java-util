package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for DeepEquals class.
 * Tests configurable security controls to prevent resource exhaustion and information disclosure attacks.
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
public class DeepEqualsSecurityTest {
    
    private String originalSecureErrors;
    private String originalMaxCollectionSize;
    private String originalMaxArraySize;
    private String originalMaxMapSize;
    private String originalMaxObjectFields;
    private String originalMaxRecursionDepth;
    
    @BeforeEach
    void setUp() {
        // Save original system property values
        originalSecureErrors = System.getProperty("deepequals.secure.errors");
        originalMaxCollectionSize = System.getProperty("deepequals.max.collection.size");
        originalMaxArraySize = System.getProperty("deepequals.max.array.size");
        originalMaxMapSize = System.getProperty("deepequals.max.map.size");
        originalMaxObjectFields = System.getProperty("deepequals.max.object.fields");
        originalMaxRecursionDepth = System.getProperty("deepequals.max.recursion.depth");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property values
        restoreProperty("deepequals.secure.errors", originalSecureErrors);
        restoreProperty("deepequals.max.collection.size", originalMaxCollectionSize);
        restoreProperty("deepequals.max.array.size", originalMaxArraySize);
        restoreProperty("deepequals.max.map.size", originalMaxMapSize);
        restoreProperty("deepequals.max.object.fields", originalMaxObjectFields);
        restoreProperty("deepequals.max.recursion.depth", originalMaxRecursionDepth);
        // Reload cached values so other tests see defaults
        DeepEquals.reloadSecurityProperties();
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    void testSecurityFeaturesDisabledByDefault() {
        // Security should be disabled by default for backward compatibility
        System.clearProperty("deepequals.secure.errors");
        System.clearProperty("deepequals.max.collection.size");
        System.clearProperty("deepequals.max.array.size");
        System.clearProperty("deepequals.max.map.size");
        System.clearProperty("deepequals.max.object.fields");
        System.clearProperty("deepequals.max.recursion.depth");
        DeepEquals.reloadSecurityProperties();

        // Create large structures that would normally trigger limits
        List<Integer> largeList1 = createLargeList(1000);
        List<Integer> largeList2 = createLargeList(1000);
        
        // Should work without throwing SecurityException when security disabled
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(largeList1, largeList2);
            assertTrue(result, "Large lists should be equal when security disabled");
        }, "DeepEquals should work without security limits by default");
    }
    
    @Test
    void testCollectionSizeLimiting() {
        // Enable collection size limit
        System.setProperty("deepequals.max.collection.size", "10");
        DeepEquals.reloadSecurityProperties();

        // Create collections that exceed the limit
        List<Integer> largeList1 = createLargeList(15);
        List<Integer> largeList2 = createLargeList(15);
        
        // Should throw SecurityException for oversized collections
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(largeList1, largeList2);
        }, "Should throw SecurityException when collection size exceeded");
        
        assertTrue(e.getMessage().contains("Collection size exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("10"));
    }
    
    @Test
    void testArraySizeLimiting() {
        // Enable array size limit
        System.setProperty("deepequals.max.array.size", "5");
        DeepEquals.reloadSecurityProperties();

        // Create arrays that exceed the limit
        int[] largeArray1 = createLargeArray(10);
        int[] largeArray2 = createLargeArray(10);
        
        // Should throw SecurityException for oversized arrays
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(largeArray1, largeArray2);
        }, "Should throw SecurityException when array size exceeded");
        
        assertTrue(e.getMessage().contains("Array size exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("5"));
    }
    
    @Test
    void testMapSizeLimiting() {
        // Enable map size limit
        System.setProperty("deepequals.max.map.size", "3");
        DeepEquals.reloadSecurityProperties();

        // Create maps that exceed the limit
        Map<String, Integer> largeMap1 = createLargeMap(5);
        Map<String, Integer> largeMap2 = createLargeMap(5);
        
        // Should throw SecurityException for oversized maps
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(largeMap1, largeMap2);
        }, "Should throw SecurityException when map size exceeded");
        
        assertTrue(e.getMessage().contains("Map size exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("3"));
    }
    
    @Test
    void testObjectFieldCountLimiting() {
        // Enable object field count limit
        System.setProperty("deepequals.max.object.fields", "2");
        DeepEquals.reloadSecurityProperties();

        // Create objects with many fields that exceed the limit
        LargeFieldObject obj1 = new LargeFieldObject();
        LargeFieldObject obj2 = new LargeFieldObject();
        
        // Should throw SecurityException for objects with too many fields
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(obj1, obj2);
        }, "Should throw SecurityException when object field count exceeded");
        
        assertTrue(e.getMessage().contains("Object field count exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("2"));
    }
    
    @Test
    void testRecursionDepthLimitingConfiguration() {
        // Enable recursion depth limit
        System.setProperty("deepequals.max.recursion.depth", "5");
        DeepEquals.reloadSecurityProperties();

        // Note: The current DeepEquals implementation uses an iterative algorithm 
        // rather than true recursion, so the depth limiting is checked only at 
        // the entry point. This test verifies the configuration is available.
        
        // Create simple objects that should work fine
        SimpleObject obj1 = new SimpleObject();
        obj1.name = "test";
        SimpleObject obj2 = new SimpleObject();
        obj2.name = "test";
        
        // Should work normally for simple comparisons
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(obj1, obj2);
            assertTrue(result, "Simple objects should compare successfully");
        }, "Should work for objects within any reasonable depth");
        
        // The recursion depth limit is primarily for entry-point protection
        // rather than deep traversal protection in the current implementation
    }
    
    @Test
    void testZeroLimitsDisableChecks() {
        // Set all limits to 0 (disabled)
        System.setProperty("deepequals.max.collection.size", "0");
        System.setProperty("deepequals.max.array.size", "0");
        System.setProperty("deepequals.max.map.size", "0");
        System.setProperty("deepequals.max.object.fields", "0");
        System.setProperty("deepequals.max.recursion.depth", "0");
        DeepEquals.reloadSecurityProperties();

        // Create large structures that would normally trigger limits
        List<Integer> largeList1 = createLargeList(1000);
        List<Integer> largeList2 = createLargeList(1000);
        
        // Should NOT throw SecurityException when limits set to 0
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(largeList1, largeList2);
            assertTrue(result, "Should compare successfully when limits disabled");
        }, "Should not enforce limits when set to 0");
    }
    
    @Test
    void testNegativeLimitsDisableChecks() {
        // Set all limits to negative values (disabled)
        System.setProperty("deepequals.max.collection.size", "-1");
        System.setProperty("deepequals.max.array.size", "-5");
        System.setProperty("deepequals.max.map.size", "-10");
        DeepEquals.reloadSecurityProperties();

        // Create structures that would trigger positive limits
        List<Integer> list1 = createLargeList(100);
        List<Integer> list2 = createLargeList(100);
        
        // Should NOT throw SecurityException when limits are negative
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(list1, list2);
            assertTrue(result, "Should compare successfully when limits negative");
        }, "Should not enforce negative limits");
    }
    
    @Test
    void testInvalidLimitValuesDefaultToDefaults() {
        // Set invalid limit values
        System.setProperty("deepequals.max.collection.size", "invalid");
        System.setProperty("deepequals.max.array.size", "not_a_number");
        System.setProperty("deepequals.max.map.size", "");
        DeepEquals.reloadSecurityProperties();

        // Create structures that are small and should work with defaults
        List<Integer> list1 = createLargeList(10);
        List<Integer> list2 = createLargeList(10);
        
        // Should work normally (using default values when parsing fails)
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(list1, list2);
            assertTrue(result, "Should work with small structures when invalid limits provided");
        }, "Should use default values when invalid property values provided");
    }
    
    @Test
    void testMultipleLimitsCanBeTriggered() {
        // Enable multiple restrictive limits
        System.setProperty("deepequals.max.collection.size", "100");
        System.setProperty("deepequals.max.array.size", "50");
        System.setProperty("deepequals.max.map.size", "20");
        DeepEquals.reloadSecurityProperties();

        // Create structure that could trigger multiple limits
        ComplexObject obj1 = new ComplexObject();
        ComplexObject obj2 = new ComplexObject();
        
        // Should throw SecurityException when any limit exceeded
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(obj1, obj2);
        }, "Should throw SecurityException when any limit exceeded");
        
        assertTrue(e.getMessage().contains("exceeds maximum allowed"));
    }
    
    @Test
    void testSmallStructuresStillWork() {
        // Enable all security limits with reasonable values
        System.setProperty("deepequals.max.collection.size", "100");
        System.setProperty("deepequals.max.array.size", "100");
        System.setProperty("deepequals.max.map.size", "100");
        System.setProperty("deepequals.max.object.fields", "50");
        System.setProperty("deepequals.max.recursion.depth", "20");
        DeepEquals.reloadSecurityProperties();

        // Create small structures that are well within limits
        List<String> smallList1 = Arrays.asList("a", "b", "c");
        List<String> smallList2 = Arrays.asList("a", "b", "c");
        
        // Should work normally for small structures
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(smallList1, smallList2);
            assertTrue(result, "Small structures should compare successfully");
        }, "Should work normally for structures within limits");
    }
    
    @Test
    void testBackwardCompatibilityPreserved() {
        // Clear all security properties to test default behavior
        System.clearProperty("deepequals.secure.errors");
        System.clearProperty("deepequals.max.collection.size");
        System.clearProperty("deepequals.max.array.size");
        System.clearProperty("deepequals.max.map.size");
        System.clearProperty("deepequals.max.object.fields");
        System.clearProperty("deepequals.max.recursion.depth");
        DeepEquals.reloadSecurityProperties();

        // Create reasonably large structures
        List<Integer> list1 = createLargeList(1000);
        List<Integer> list2 = createLargeList(1000);
        
        // Should work normally without any security restrictions
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(list1, list2);
            assertTrue(result, "Should preserve backward compatibility");
        }, "Should preserve backward compatibility");
    }
    
    @Test
    void testSecureErrorMessagesWhenEnabled() {
        // Enable secure error messages
        System.setProperty("deepequals.secure.errors", "true");
        System.setProperty("deepequals.max.collection.size", "5");
        DeepEquals.reloadSecurityProperties();

        // Create object with sensitive field names that would appear in error
        SensitiveObject obj1 = new SensitiveObject();
        obj1.password = "secret123";
        obj1.data = createLargeList(10); // Will trigger size limit
        
        SensitiveObject obj2 = new SensitiveObject();
        obj2.password = "secret456";
        obj2.data = createLargeList(10); // Will trigger size limit
        
        // Should throw SecurityException and error message should be sanitized
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(obj1, obj2);
        }, "Should throw SecurityException for oversized collection");
        
        // Error message should not contain sensitive values when secure errors enabled
        String message = e.getMessage();
        assertFalse(message.contains("secret123"), "Error message should not contain sensitive password");
        assertFalse(message.contains("secret456"), "Error message should not contain sensitive password");
        assertTrue(message.contains("Collection size exceeds maximum allowed"), "Should contain security limit message");
    }
    
    @Test
    void testRegularErrorMessagesWhenDisabled() {
        // Disable secure error messages (default)
        System.setProperty("deepequals.secure.errors", "false");
        DeepEquals.reloadSecurityProperties();

        // Create objects with different values
        SimpleObject obj1 = new SimpleObject();
        obj1.name = "test1";
        SimpleObject obj2 = new SimpleObject();
        obj2.name = "test2";
        
        Map<String, Object> options = new HashMap<>();
        
        // Should provide detailed difference information when secure errors disabled
        boolean result = DeepEquals.deepEquals(obj1, obj2, options);
        assertFalse(result, "Objects should not be equal");
        
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff, "Should provide difference information");
        // Normal detailed error messages should contain actual values
        assertTrue(diff.contains("test") || diff.contains("name"), "Should contain field information in regular mode");
    }
    
    @Test
    void testRecursionDepthLimitingWithDeeplyNestedObjects() {
        // Enable recursion depth limit to 1000
        System.setProperty("deepequals.max.recursion.depth", "1000");
        DeepEquals.reloadSecurityProperties();

        // Create deeply nested objects that exceed the limit
        NestedObject obj1 = createDeeplyNestedObject(1001);
        NestedObject obj2 = createDeeplyNestedObject(1001);
        
        // Should throw SecurityException for excessive depth
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(obj1, obj2);
        }, "Should throw SecurityException when recursion depth exceeded");
        
        assertTrue(e.getMessage().contains("Maximum recursion depth exceeded"));
        assertTrue(e.getMessage().contains("1000"));
    }
    
    @Test
    void testOneMillionDepthLimitForHeapBasedTraversal() {
        // Test with 1 million depth limit (should work fine for heap-based traversal)
        System.setProperty("deepequals.max.recursion.depth", "1000000");
        DeepEquals.reloadSecurityProperties();

        // Create objects that are just within the limit
        NestedObject obj1 = createDeeplyNestedObject(1000);
        NestedObject obj2 = createDeeplyNestedObject(1000);
        
        // Should work fine for reasonable depth
        assertDoesNotThrow(() -> {
            boolean result = DeepEquals.deepEquals(obj1, obj2);
            assertTrue(result, "Deeply nested identical objects should be equal");
        }, "Should handle reasonable depth (1000) without issues");
        
        // Create objects that exceed 1M depth - this would be too expensive to test in practice
        // but we can test the validation logic with a smaller limit
        System.setProperty("deepequals.max.recursion.depth", "500");
        DeepEquals.reloadSecurityProperties();

        NestedObject obj3 = createDeeplyNestedObject(501);
        NestedObject obj4 = createDeeplyNestedObject(501);
        
        SecurityException e = assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(obj3, obj4);
        }, "Should throw SecurityException when depth exceeds configured limit");
        
        assertTrue(e.getMessage().contains("Maximum recursion depth exceeded"));
        assertTrue(e.getMessage().contains("500"));
    }
    
    // Helper classes for testing
    
    private static class LargeFieldObject {
        public String field1 = "value1";
        public String field2 = "value2";
        public String field3 = "value3";
        public String field4 = "value4";
        public String field5 = "value5";
    }
    
    private static class NestedObject {
        public NestedObject child;
        public int level;
        
        public NestedObject(int level) {
            this.level = level;
        }
    }
    
    private static class ComplexObject {
        public List<Integer> largeList;
        public int[] largeArray;
        public Map<String, Integer> largeMap;
        
        public ComplexObject() {
            largeList = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                largeList.add(i);
            }
            
            largeArray = new int[75];
            for (int i = 0; i < 75; i++) {
                largeArray[i] = i;
            }
            
            largeMap = new HashMap<>();
            for (int i = 0; i < 30; i++) {
                largeMap.put("key" + i, i);
            }
        }
    }
    
    private static class SensitiveObject {
        public String password;
        public String secret;
        public String token;
        public List<Integer> data;
    }
    
    private static class SimpleObject {
        public String name;
        public int value;
    }
    
    // Helper methods
    
    private List<Integer> createLargeList(int size) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }
    
    private int[] createLargeArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        return array;
    }
    
    private Map<String, Integer> createLargeMap(int size) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put("key" + i, i);
        }
        return map;
    }
    
    private NestedObject createDeeplyNestedObject(int depth) {
        NestedObject root = new NestedObject(0);
        NestedObject current = root;
        
        for (int i = 1; i < depth; i++) {
            current.child = new NestedObject(i);
            current = current.child;
        }
        
        return root;
    }
}