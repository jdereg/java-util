package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that CaseInsensitiveMap properly implements ConcurrentMap interface
 */
class CaseInsensitiveMapConcurrentInterfaceTest {

    @Test
    void testConcurrentMapInterface() {
        // Test that CaseInsensitiveMap can be assigned to ConcurrentMap
        ConcurrentMap<String, String> concurrentMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        
        // Test basic concurrent operations through the interface
        assertNull(concurrentMap.putIfAbsent("Key1", "value1"));
        assertEquals("value1", concurrentMap.putIfAbsent("KEY1", "value2")); // Should return existing value
        
        assertEquals("value1", concurrentMap.get("key1")); // Case insensitive
        
        // Test replace operations
        assertTrue(concurrentMap.replace("KEY1", "value1", "newValue1"));
        assertEquals("newValue1", concurrentMap.get("Key1"));
        
        // Test remove with value
        assertTrue(concurrentMap.remove("key1", "newValue1"));
        assertNull(concurrentMap.get("Key1"));
        
        // Test that it's empty after removal
        assertTrue(concurrentMap.isEmpty());
    }
    
    @Test
    void testConcurrentMapInterfaceWithNonConcurrentBacking() {
        // Test that CaseInsensitiveMap can be assigned to ConcurrentMap even with non-concurrent backing
        ConcurrentMap<String, String> concurrentMap = new CaseInsensitiveMap<>(); // LinkedHashMap backing
        
        // Test basic operations still work
        assertNull(concurrentMap.putIfAbsent("Key1", "value1"));
        assertEquals("value1", concurrentMap.putIfAbsent("KEY1", "value2")); // Should return existing value
        
        assertEquals("value1", concurrentMap.get("key1")); // Case insensitive
        
        // Test replace operations
        assertTrue(concurrentMap.replace("KEY1", "value1", "newValue1"));
        assertEquals("newValue1", concurrentMap.get("Key1"));
        
        // Test remove with value
        assertTrue(concurrentMap.remove("key1", "newValue1"));
        assertNull(concurrentMap.get("Key1"));
    }
    
    @Test
    void testConcurrentMapPolymorphism() {
        // Test that we can use CaseInsensitiveMap in methods expecting ConcurrentMap
        CaseInsensitiveMap<String, String> caseInsensitiveMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        
        // Pass to method expecting ConcurrentMap
        testConcurrentMapOperations(caseInsensitiveMap);
        
        // Verify the operations worked with case insensitivity
        assertEquals("value1", caseInsensitiveMap.get("KEY1"));
        assertEquals("value2", caseInsensitiveMap.get("key2"));
    }
    
    private void testConcurrentMapOperations(ConcurrentMap<String, String> map) {
        // Method that expects ConcurrentMap interface
        map.putIfAbsent("Key1", "value1");
        map.putIfAbsent("KEY2", "value2");
        
        // These should work through the ConcurrentMap interface
        assertNotNull(map.get("Key1"));
        assertNotNull(map.get("KEY2"));
    }
    
    @Test
    void testFactoryMethods() {
        // Test concurrent() factory method
        ConcurrentMap<String, String> concurrent = CaseInsensitiveMap.concurrent();
        assertNotNull(concurrent);
        concurrent.putIfAbsent("Key", "value");
        assertEquals("value", concurrent.get("KEY")); // Case insensitive
        
        // Test concurrent(int) factory method
        CaseInsensitiveMap<String, String> concurrentWithCapacity = CaseInsensitiveMap.concurrent(100);
        assertNotNull(concurrentWithCapacity);
        concurrentWithCapacity.putIfAbsent("Key", "value");
        assertEquals("value", concurrentWithCapacity.get("key")); // Case insensitive
        
        // Test concurrentSorted() factory method
        CaseInsensitiveMap<String, String> concurrentSorted = CaseInsensitiveMap.concurrentSorted();
        assertNotNull(concurrentSorted);
        concurrentSorted.putIfAbsent("Key", "value");
        assertEquals("value", concurrentSorted.get("KEY")); // Case insensitive
        
        // Verify they can be used as ConcurrentMap interface
        ConcurrentMap<String, String> interfaceRef = CaseInsensitiveMap.concurrent();
        interfaceRef.putIfAbsent("Test", "value");
        assertEquals("value", interfaceRef.get("test"));
    }
}