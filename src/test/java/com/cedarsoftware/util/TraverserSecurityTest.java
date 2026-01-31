package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security tests for Traverser class.
 * Tests configurable security controls to prevent resource exhaustion and stack overflow attacks.
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
public class TraverserSecurityTest {
    
    private String originalSecurityEnabled;
    private String originalMaxStackDepth;
    private String originalMaxObjectsVisited;
    private String originalMaxCollectionSize;
    private String originalMaxArrayLength;
    
    @BeforeEach
    void setUp() {
        // Save original system property values
        originalSecurityEnabled = System.getProperty("traverser.security.enabled");
        originalMaxStackDepth = System.getProperty("traverser.max.stack.depth");
        originalMaxObjectsVisited = System.getProperty("traverser.max.objects.visited");
        originalMaxCollectionSize = System.getProperty("traverser.max.collection.size");
        originalMaxArrayLength = System.getProperty("traverser.max.array.length");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property values
        restoreProperty("traverser.security.enabled", originalSecurityEnabled);
        restoreProperty("traverser.max.stack.depth", originalMaxStackDepth);
        restoreProperty("traverser.max.objects.visited", originalMaxObjectsVisited);
        restoreProperty("traverser.max.collection.size", originalMaxCollectionSize);
        restoreProperty("traverser.max.array.length", originalMaxArrayLength);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    void testSecurityDisabledByDefault() {
        // Security should be disabled by default for backward compatibility
        System.clearProperty("traverser.security.enabled");
        
        // Create deeply nested object that would normally trigger limits
        DeepObject deep = createDeeplyNestedObject(100);
        
        // Should traverse without throwing SecurityException when security disabled
        assertDoesNotThrow(() -> {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(deep, visit -> visited.add(visit.getNode()), null);
            assertTrue(visited.size() > 50, "Should visit many objects when security disabled");
        }, "Traverser should work without security limits by default");
    }
    
    @Test
    void testStackDepthLimiting() {
        // Enable security with stack depth limit
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.stack.depth", "10");
        
        // Create deeply nested object that exceeds limit
        DeepObject deep = createDeeplyNestedObject(15);
        
        // Should throw SecurityException for stack depth
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(deep, visit -> {}, null);
        }, "Should throw SecurityException when stack depth exceeded");
        
        assertTrue(e.getMessage().contains("Stack depth exceeded limit"));
        assertTrue(e.getMessage().contains("max 10"));
    }
    
    @Test
    void testObjectCountLimiting() {
        // Enable security with object count limit
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.objects.visited", "5");
        
        // Create object graph with many objects
        WideObject wide = createWideObject(10);
        
        // Should throw SecurityException for object count
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(wide, visit -> {}, null);
        }, "Should throw SecurityException when object count exceeded");
        
        assertTrue(e.getMessage().contains("Objects visited exceeded limit"));
        assertTrue(e.getMessage().contains("max 5"));
    }
    
    @Test
    void testCollectionSizeLimiting() {
        // Enable security with collection size limit
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.collection.size", "3");
        
        // Create object with large collection
        CollectionContainer container = new CollectionContainer();
        for (int i = 0; i < 5; i++) {
            container.items.add("item" + i);
        }
        
        // Should throw SecurityException for collection size
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(container, visit -> {}, null);
        }, "Should throw SecurityException when collection size exceeded");
        
        assertTrue(e.getMessage().contains("Collection size exceeded limit"));
        assertTrue(e.getMessage().contains("max 3"));
    }
    
    @Test
    void testArrayLengthLimiting() {
        // Enable security with array length limit
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.array.length", "3");
        
        // Create object with large array
        ArrayContainer container = new ArrayContainer();
        container.values = new String[]{"a", "b", "c", "d", "e"};
        
        // Should throw SecurityException for array length
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(container, visit -> {}, null);
        }, "Should throw SecurityException when array length exceeded");
        
        assertTrue(e.getMessage().contains("Array length exceeded limit"));
        assertTrue(e.getMessage().contains("max 3"));
    }
    
    @Test
    void testMapSizeLimiting() {
        // Enable security with collection size limit (maps use same limit)
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.collection.size", "2");
        
        // Create object with large map
        MapContainer container = new MapContainer();
        container.data.put("key1", "value1");
        container.data.put("key2", "value2");
        container.data.put("key3", "value3");
        
        // Should throw SecurityException for map size
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(container, visit -> {}, null);
        }, "Should throw SecurityException when map size exceeded");
        
        assertTrue(e.getMessage().contains("Collection size exceeded limit"));
        assertTrue(e.getMessage().contains("max 2"));
    }
    
    @Test
    void testSecurityLimitsOnlyEnforcedWhenEnabled() {
        // Disable security
        System.setProperty("traverser.security.enabled", "false");
        System.setProperty("traverser.max.stack.depth", "5");
        System.setProperty("traverser.max.objects.visited", "3");
        System.setProperty("traverser.max.collection.size", "2");
        System.setProperty("traverser.max.array.length", "2");
        
        // Create objects that would exceed all limits
        DeepObject deep = createDeeplyNestedObject(10);
        
        // Should NOT throw SecurityException when security disabled
        assertDoesNotThrow(() -> {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(deep, visit -> visited.add(visit.getNode()), null);
            assertTrue(visited.size() > 5, "Should visit objects when security disabled");
        }, "Should not enforce limits when security disabled");
    }
    
    @Test
    void testZeroLimitsDisableIndividualChecks() {
        // Enable security but set individual limits to 0 (disabled)
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.stack.depth", "0");
        System.setProperty("traverser.max.objects.visited", "0");
        System.setProperty("traverser.max.collection.size", "0");
        System.setProperty("traverser.max.array.length", "0");
        
        // Create objects that would normally trigger limits
        DeepObject deep = createDeeplyNestedObject(100);
        
        // Should NOT throw SecurityException when limits set to 0
        assertDoesNotThrow(() -> {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(deep, visit -> visited.add(visit.getNode()), null);
            assertTrue(visited.size() > 50, "Should visit objects when limits set to 0");
        }, "Should not enforce limits when set to 0");
    }
    
    @Test
    void testInvalidLimitValuesIgnored() {
        // Enable security with invalid limit values
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.stack.depth", "invalid");
        System.setProperty("traverser.max.objects.visited", "not_a_number");
        System.setProperty("traverser.max.collection.size", "");
        System.setProperty("traverser.max.array.length", "-5"); // Negative treated as 0
        
        // Create objects that would exceed default object count limit (100000)
        WideObject wide = createWideObject(150000);
        
        // Should use default limits when invalid values provided
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(wide, visit -> {}, null);
        }, "Should use default limits when invalid values provided");
        
        // Should hit default stack depth limit (1000000) or object count limit (100000)
        assertTrue(e.getMessage().contains("exceeded limit"));
    }
    
    @Test
    void testMultipleLimitsCanBeTriggered() {
        // Enable security with multiple restrictive limits
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.stack.depth", "100");
        System.setProperty("traverser.max.objects.visited", "50");
        System.setProperty("traverser.max.collection.size", "10");
        
        // Create deep object that could trigger multiple limits
        DeepObject deep = createDeeplyNestedObject(200);
        
        // Should throw SecurityException (might be any of the limits)
        SecurityException e = assertThrows(SecurityException.class, () -> {
            Traverser.traverse(deep, visit -> {}, null);
        }, "Should throw SecurityException when any limit exceeded");
        
        assertTrue(e.getMessage().contains("exceeded limit"));
    }
    
    @Test
    void testPrimitiveArraysNotLimited() {
        // Enable security with array length limit
        System.setProperty("traverser.security.enabled", "true");
        System.setProperty("traverser.max.array.length", "3");
        
        // Create object with large primitive array (should not be limited)
        PrimitiveArrayContainer container = new PrimitiveArrayContainer();
        container.primitives = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        // Should NOT throw SecurityException for primitive arrays
        assertDoesNotThrow(() -> {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(container, visit -> visited.add(visit.getNode()), null);
            assertTrue(visited.size() >= 1, "Should visit container object");
        }, "Should not limit primitive arrays");
    }
    
    @Test
    void testBackwardCompatibilityPreserved() {
        // Clear all security properties to test default behavior
        System.clearProperty("traverser.security.enabled");
        System.clearProperty("traverser.max.stack.depth");
        System.clearProperty("traverser.max.objects.visited");
        System.clearProperty("traverser.max.collection.size");
        System.clearProperty("traverser.max.array.length");
        
        // Create object graph that would trigger limits if enabled
        ComplexObject complex = createComplexObject();
        
        // Should work normally without any security restrictions
        assertDoesNotThrow(() -> {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(complex, visit -> visited.add(visit.getNode()), null);
            assertTrue(visited.size() > 10, "Should traverse complex object graph");
        }, "Should preserve backward compatibility");
    }
    
    // Helper classes for testing
    
    private static class DeepObject {
        public DeepObject child;
        public int level;
        
        public DeepObject(int level) {
            this.level = level;
        }
    }
    
    private static class WideObject {
        public List<SimpleObject> children = new ArrayList<>();
    }
    
    private static class SimpleObject {
        public String name;
        
        public SimpleObject(String name) {
            this.name = name;
        }
    }
    
    private static class CollectionContainer {
        public List<String> items = new ArrayList<>();
    }
    
    private static class ArrayContainer {
        public String[] values;
    }
    
    private static class MapContainer {
        public Map<String, String> data = new HashMap<>();
    }
    
    private static class PrimitiveArrayContainer {
        public int[] primitives;
    }
    
    private static class ComplexObject {
        public List<String> strings = new ArrayList<>();
        public Map<String, Integer> map = new HashMap<>();
        public String[] array;
        public SimpleObject nested;
        
        public ComplexObject() {
            strings.add("test1");
            strings.add("test2");
            map.put("key1", 1);
            map.put("key2", 2);
            array = new String[]{"a", "b", "c"};
            nested = new SimpleObject("nested");
        }
    }
    
    // Helper methods
    
    private DeepObject createDeeplyNestedObject(int depth) {
        DeepObject root = new DeepObject(0);
        DeepObject current = root;
        
        for (int i = 1; i < depth; i++) {
            current.child = new DeepObject(i);
            current = current.child;
        }
        
        return root;
    }
    
    private WideObject createWideObject(int childCount) {
        WideObject wide = new WideObject();
        for (int i = 0; i < childCount; i++) {
            wide.children.add(new SimpleObject("child" + i));
        }
        return wide;
    }
    
    private ComplexObject createComplexObject() {
        ComplexObject complex = new ComplexObject();
        // Add more complexity
        for (int i = 0; i < 20; i++) {
            complex.strings.add("item" + i);
            complex.map.put("key" + i, i);
        }
        return complex;
    }
}