package com.cedarsoftware.util;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the Map-like APIs of MultiKeyMap.
 */
class MultiKeyMapMapApiTest {
    
    @Test
    void testIsEmpty() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertTrue(map.isEmpty(), "New map should be empty");
        assertEquals(0, map.size(), "Empty map should have size 0");
        
        map.putMultiKey("test", String.class, Integer.class, 1L);
        assertFalse(map.isEmpty(), "Map with entries should not be empty");
        assertEquals(1, map.size(), "Map should have size 1");
    }
    
    @Test
    void testContainsValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertFalse(map.containsValue("test"), "Empty map should not contain any value");
        assertFalse(map.containsValue(null), "Empty map should not contain null");
        
        map.putMultiKey("test1", String.class, Integer.class, 1L);
        map.putMultiKey("test2", String.class, Long.class, 2L);
        map.putMultiKey(null, Integer.class, String.class, 3L);
        
        assertTrue(map.containsValue("test1"), "Should contain 'test1'");
        assertTrue(map.containsValue("test2"), "Should contain 'test2'");
        assertTrue(map.containsValue(null), "Should contain null value");
        assertFalse(map.containsValue("nonexistent"), "Should not contain 'nonexistent'");
        assertFalse(map.containsValue("TEST1"), "Should not contain 'TEST1' (case sensitive)");
    }
    
    @Test
    void testContainsMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertFalse(map.containsMultiKey(String.class, Integer.class, 1L), "Empty map should not contain any key");
        
        map.putMultiKey("test1", String.class, Integer.class, 1L);
        map.putMultiKey("test2", String.class, Long.class, 2L);
        
        assertTrue(map.containsMultiKey(String.class, Integer.class, 1L), "Should contain key (String, Integer, 1)");
        assertTrue(map.containsMultiKey(String.class, Long.class, 2L), "Should contain key (String, Long, 2)");
        assertFalse(map.containsMultiKey(String.class, Integer.class, 2L), "Should not contain key (String, Integer, 2)");
        assertFalse(map.containsMultiKey(Integer.class, String.class, 1L), "Should not contain key (Integer, String, 1)");
        assertFalse(map.containsMultiKey(String.class, Integer.class, 999L), "Should not contain key with different instanceId");
    }
    
    @Test
    void testRemoveMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test remove from empty map
        assertNull(map.removeMultiKey(String.class, Integer.class, 1L), "Remove from empty map should return null");
        
        // Add some entries
        map.putMultiKey("test1", String.class, Integer.class, 1L);
        map.putMultiKey("test2", String.class, Long.class, 2L);
        map.putMultiKey("test3", Integer.class, String.class, 3L);
        
        assertEquals(3, map.size(), "Should have 3 entries");
        
        // Test successful removal
        assertEquals("test2", map.removeMultiKey(String.class, Long.class, 2L), "Should return removed value");
        assertEquals(2, map.size(), "Should have 2 entries after removal");
        assertFalse(map.containsMultiKey(String.class, Long.class, 2L), "Removed key should no longer exist");
        
        // Test removal of non-existent key
        assertNull(map.removeMultiKey(String.class, Long.class, 2L), "Remove non-existent key should return null");
        assertEquals(2, map.size(), "Size should remain unchanged");
        
        // Test removal with different instanceId
        assertNull(map.removeMultiKey(String.class, Integer.class, 999L), "Remove with wrong instanceId should return null");
        assertEquals(2, map.size(), "Size should remain unchanged");
        
        // Remove remaining entries
        assertEquals("test1", map.removeMultiKey(String.class, Integer.class, 1L));
        assertEquals("test3", map.removeMultiKey(Integer.class, String.class, 3L));
        
        assertTrue(map.isEmpty(), "Map should be empty after removing all entries");
    }
    
    @Test
    void testClear() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test clear on empty map
        map.clear();
        assertTrue(map.isEmpty(), "Clear on empty map should still be empty");
        
        // Add entries and then clear
        map.putMultiKey("test1", String.class, Integer.class, 1L);
        map.putMultiKey("test2", String.class, Long.class, 2L);
        map.putMultiKey("test3", Integer.class, String.class, 3L);
        
        assertEquals(3, map.size(), "Should have 3 entries before clear");
        assertFalse(map.isEmpty(), "Should not be empty before clear");
        
        map.clear();
        
        assertTrue(map.isEmpty(), "Should be empty after clear");
        assertEquals(0, map.size(), "Should have size 0 after clear");
        assertFalse(map.containsMultiKey(String.class, Integer.class, 1L), "Should not contain any keys after clear");
        assertFalse(map.containsValue("test1"), "Should not contain any values after clear");
        
        // Test that we can add entries after clear
        map.putMultiKey("after_clear", Double.class, Boolean.class, 10L);
        assertEquals(1, map.size(), "Should be able to add entries after clear");
        assertTrue(map.containsMultiKey(Double.class, Boolean.class, 10L), "Should contain new entry");
    }
    
    @Test
    void testValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test values on empty map
        Collection<String> values = map.values();
        assertNotNull(values, "Values collection should not be null");
        assertTrue(values.isEmpty(), "Values collection should be empty for empty map");
        
        // Add entries
        map.putMultiKey("test1", String.class, Integer.class, 1L);
        map.putMultiKey("test2", String.class, Long.class, 2L);
        map.putMultiKey("test1", Integer.class, String.class, 3L); // Duplicate value
        map.putMultiKey(null, Double.class, Boolean.class, 4L); // Null value
        
        values = map.values();
        assertEquals(4, values.size(), "Values collection should have 4 entries");
        
        assertTrue(values.contains("test1"), "Should contain 'test1'");
        assertTrue(values.contains("test2"), "Should contain 'test2'");
        assertTrue(values.contains(null), "Should contain null value");
        assertFalse(values.contains("nonexistent"), "Should not contain 'nonexistent'");
        
        // Check that duplicate values are included
        long test1Count = values.stream().filter(v -> "test1".equals(v)).count();
        assertEquals(2, test1Count, "Should contain 'test1' twice");
    }
    
    @Test
    void testRemoveMultiKeyWithCollisions() {
        MultiKeyMap<String> map = new MultiKeyMap<>(2); // Small capacity to force collisions
        
        // Add multiple entries that should cause hash collisions
        map.putMultiKey("value1", String.class, Integer.class, 1L);
        map.putMultiKey("value2", String.class, Integer.class, 2L);
        map.putMultiKey("value3", String.class, Integer.class, 3L);
        map.putMultiKey("value4", String.class, Long.class, 1L);
        
        assertEquals(4, map.size(), "Should have 4 entries");
        
        // Remove middle entry from a chain
        assertEquals("value2", map.removeMultiKey(String.class, Integer.class, 2L));
        assertEquals(3, map.size(), "Should have 3 entries after removal");
        
        // Verify other entries still exist
        assertTrue(map.containsMultiKey(String.class, Integer.class, 1L), "Should still contain first entry");
        assertTrue(map.containsMultiKey(String.class, Integer.class, 3L), "Should still contain third entry");
        assertTrue(map.containsMultiKey(String.class, Long.class, 1L), "Should still contain fourth entry");
        assertFalse(map.containsMultiKey(String.class, Integer.class, 2L), "Should not contain removed entry");
        
        // Verify values are correct
        assertEquals("value1", map.getMultiKey(String.class, Integer.class, 1L));
        assertEquals("value3", map.getMultiKey(String.class, Integer.class, 3L));
        assertEquals("value4", map.getMultiKey(String.class, Long.class, 1L));
    }
    
    @Test
    void testMapApiConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test consistency between size, isEmpty, and containsKey/containsValue
        assertTrue(map.isEmpty() == (map.size() == 0), "isEmpty should be consistent with size");
        
        map.putMultiKey("test", String.class, Integer.class, 1L);
        assertTrue(!map.isEmpty() == (map.size() > 0), "isEmpty should be consistent with size");
        assertTrue(map.containsMultiKey(String.class, Integer.class, 1L), "containsKey should return true for existing key");
        assertTrue(map.containsValue("test"), "containsValue should return true for existing value");
        
        map.removeMultiKey(String.class, Integer.class, 1L);
        assertTrue(map.isEmpty() == (map.size() == 0), "isEmpty should be consistent with size after removal");
        assertFalse(map.containsMultiKey(String.class, Integer.class, 1L), "containsKey should return false after removal");
        assertFalse(map.containsValue("test"), "containsValue should return false after removal");
        
        map.clear();
        assertTrue(map.isEmpty(), "Should be empty after clear");
        assertEquals(0, map.size(), "Size should be 0 after clear");
    }
}