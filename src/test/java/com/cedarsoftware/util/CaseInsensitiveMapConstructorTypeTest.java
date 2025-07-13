package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that CaseInsensitiveMap copy constructor creates the same map type as the source.
 */
class CaseInsensitiveMapConstructorTypeTest {

    private static final Logger LOG = Logger.getLogger(CaseInsensitiveMapConstructorTypeTest.class.getName());
    static {
        LoggingConfig.init();
    }

    @Test
    void testCopyConstructorWithHashMap() {
        // Create source HashMap
        HashMap<String, Object> source = new HashMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Verify the backing map type is HashMap
        assertTrue(ciMap.getWrappedMap() instanceof HashMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }

    @Test
    void testCopyConstructorWithLinkedHashMap() {
        // Create source LinkedHashMap
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Verify the backing map type is LinkedHashMap
        assertTrue(ciMap.getWrappedMap() instanceof LinkedHashMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }

    @Test
    void testCopyConstructorWithTreeMap() {
        // Create source TreeMap
        TreeMap<String, Object> source = new TreeMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Verify the backing map type is TreeMap
        assertTrue(ciMap.getWrappedMap() instanceof TreeMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }

    @Test
    void testCopyConstructorWithConcurrentHashMap() {
        // Create source ConcurrentHashMap
        ConcurrentHashMap<String, Object> source = new ConcurrentHashMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Verify the backing map type is ConcurrentHashMap
        assertTrue(ciMap.getWrappedMap() instanceof ConcurrentHashMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }

    @Test
    void testCopyConstructorWithConcurrentSkipListMap() {
        // Create source ConcurrentSkipListMap
        ConcurrentSkipListMap<String, Object> source = new ConcurrentSkipListMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Verify the backing map type is ConcurrentSkipListMap
        assertTrue(ciMap.getWrappedMap() instanceof ConcurrentSkipListMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }

    @Test
    void testCopyConstructorWithUnsupportedMapType() {
        // Create a custom map type that ClassUtilities.newInstance() can't handle
        CustomMap<String, Object> source = new CustomMap<>();
        source.put("Key1", "Value1");
        source.put("Key2", "Value2");
        
        // Create CaseInsensitiveMap from source - should fall back to determineBackingMap
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(source);
        
        // Debug: log the actual backing map type
        LOG.info("Actual backing map type: " + ciMap.getWrappedMap().getClass().getName());
        
        // Should fall back to HashMap (since CustomMap extends HashMap, it should match in the registry)
        assertTrue(ciMap.getWrappedMap() instanceof HashMap);
        assertEquals(2, ciMap.size());
        assertEquals("Value1", ciMap.get("key1")); // Case insensitive
        assertEquals("Value2", ciMap.get("KEY2")); // Case insensitive
    }
    
    // Custom map class for testing fallback behavior
    private static class CustomMap<K, V> extends HashMap<K, V> {
        // No default constructor - will cause ClassUtilities.newInstance() to fail
        public CustomMap(String dummy) {
            super();
        }
        
        private CustomMap() {
            // Private constructor to prevent instantiation
            super();
        }
    }
}