package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

/**
 * Test the Map-like APIs of MultiKeyMap.
 */
class MultiKeyMapMapApiTest {
    
    @Test
    void testIsEmpty() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertTrue(map.isEmpty(), "New map should be empty");
        assertEquals(0, map.size(), "Empty map should have size 0");
        
        map.put(String.class, Integer.class, 1L, "test");
        assertFalse(map.isEmpty(), "Map with entries should not be empty");
        assertEquals(1, map.size(), "Map should have size 1");
    }
    
    @Test
    void testContainsValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertFalse(map.containsValue("test"), "Empty map should not contain any value");
        assertFalse(map.containsValue(null), "Empty map should not contain null");
        
        map.put(String.class, Integer.class, 1L, "test1");
        map.put(String.class, Long.class, 2L, "test2");
        map.put(Integer.class, String.class, 3L, null);
        
        assertTrue(map.containsValue("test1"), "Should contain 'test1'");
        assertTrue(map.containsValue("test2"), "Should contain 'test2'");
        assertTrue(map.containsValue(null), "Should contain null value");
        assertFalse(map.containsValue("nonexistent"), "Should not contain 'nonexistent'");
        assertFalse(map.containsValue("TEST1"), "Should not contain 'TEST1' (case sensitive)");
    }
    
    @Test
    void testContainsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        assertFalse(map.containsKey(String.class, Integer.class, 1L), "Empty map should not contain any key");
        
        map.put(String.class, Integer.class, 1L, "test1");
        map.put(String.class, Long.class, 2L, "test2");
        
        assertTrue(map.containsKey(String.class, Integer.class, 1L), "Should contain key (String, Integer, 1)");
        assertTrue(map.containsKey(String.class, Long.class, 2L), "Should contain key (String, Long, 2)");
        assertFalse(map.containsKey(String.class, Integer.class, 2L), "Should not contain key (String, Integer, 2)");
        assertFalse(map.containsKey(Integer.class, String.class, 1L), "Should not contain key (Integer, String, 1)");
        assertFalse(map.containsKey(String.class, Integer.class, 999L), "Should not contain key with different instanceId");
    }
    
    @Test
    void testRemove() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test remove from empty map
        assertNull(map.remove(String.class, Integer.class, 1L), "Remove from empty map should return null");
        
        // Add some entries
        map.put(String.class, Integer.class, 1L, "test1");
        map.put(String.class, Long.class, 2L, "test2");
        map.put(Integer.class, String.class, 3L, "test3");
        
        assertEquals(3, map.size(), "Should have 3 entries");
        
        // Test successful removal
        assertEquals("test2", map.remove(String.class, Long.class, 2L), "Should return removed value");
        assertEquals(2, map.size(), "Should have 2 entries after removal");
        assertFalse(map.containsKey(String.class, Long.class, 2L), "Removed key should no longer exist");
        
        // Test removal of non-existent key
        assertNull(map.remove(String.class, Long.class, 2L), "Remove non-existent key should return null");
        assertEquals(2, map.size(), "Size should remain unchanged");
        
        // Test removal with different instanceId
        assertNull(map.remove(String.class, Integer.class, 999L), "Remove with wrong instanceId should return null");
        assertEquals(2, map.size(), "Size should remain unchanged");
        
        // Remove remaining entries
        assertEquals("test1", map.remove(String.class, Integer.class, 1L));
        assertEquals("test3", map.remove(Integer.class, String.class, 3L));
        
        assertTrue(map.isEmpty(), "Map should be empty after removing all entries");
    }
    
    @Test
    void testClear() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test clear on empty map
        map.clear();
        assertTrue(map.isEmpty(), "Clear on empty map should still be empty");
        
        // Add entries and then clear
        map.put(String.class, Integer.class, 1L, "test1");
        map.put(String.class, Long.class, 2L, "test2");
        map.put(Integer.class, String.class, 3L, "test3");
        
        assertEquals(3, map.size(), "Should have 3 entries before clear");
        assertFalse(map.isEmpty(), "Should not be empty before clear");
        
        map.clear();
        
        assertTrue(map.isEmpty(), "Should be empty after clear");
        assertEquals(0, map.size(), "Should have size 0 after clear");
        assertFalse(map.containsKey(String.class, Integer.class, 1L), "Should not contain any keys after clear");
        assertFalse(map.containsValue("test1"), "Should not contain any values after clear");
        
        // Test that we can add entries after clear
        map.put(Double.class, Boolean.class, 10L, "after_clear");
        assertEquals(1, map.size(), "Should be able to add entries after clear");
        assertTrue(map.containsKey(Double.class, Boolean.class, 10L), "Should contain new entry");
    }
    
    @Test
    void testValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test values on empty map
        Collection<String> values = map.values();
        assertNotNull(values, "Values collection should not be null");
        assertTrue(values.isEmpty(), "Values collection should be empty for empty map");
        
        // Add entries
        map.put(String.class, Integer.class, 1L, "test1");
        map.put(String.class, Long.class, 2L, "test2");
        map.put(Integer.class, String.class, 3L, "test1"); // Duplicate value
        map.put(Double.class, Boolean.class, 4L, null); // Null value
        
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
    void testRemoveWithCollisions() {
        MultiKeyMap<String> map = new MultiKeyMap<>(2); // Small capacity to force collisions
        
        // Add multiple entries that should cause hash collisions
        map.put(String.class, Integer.class, 1L, "value1");
        map.put(String.class, Integer.class, 2L, "value2");
        map.put(String.class, Integer.class, 3L, "value3");
        map.put(String.class, Long.class, 1L, "value4");
        
        assertEquals(4, map.size(), "Should have 4 entries");
        
        // Remove middle entry from a chain
        assertEquals("value2", map.remove(String.class, Integer.class, 2L));
        assertEquals(3, map.size(), "Should have 3 entries after removal");
        
        // Verify other entries still exist
        assertTrue(map.containsKey(String.class, Integer.class, 1L), "Should still contain first entry");
        assertTrue(map.containsKey(String.class, Integer.class, 3L), "Should still contain third entry");
        assertTrue(map.containsKey(String.class, Long.class, 1L), "Should still contain fourth entry");
        assertFalse(map.containsKey(String.class, Integer.class, 2L), "Should not contain removed entry");
        
        // Verify values are correct
        assertEquals("value1", map.get(String.class, Integer.class, 1L));
        assertEquals("value3", map.get(String.class, Integer.class, 3L));
        assertEquals("value4", map.get(String.class, Long.class, 1L));
    }
    
    @Test
    void testMapApiConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test consistency between size, isEmpty, and containsKey/containsValue
        assertTrue(map.isEmpty() == (map.size() == 0), "isEmpty should be consistent with size");
        
        map.put(String.class, Integer.class, 1L, "test");
        assertTrue(!map.isEmpty() == (map.size() > 0), "isEmpty should be consistent with size");
        assertTrue(map.containsKey(String.class, Integer.class, 1L), "containsKey should return true for existing key");
        assertTrue(map.containsValue("test"), "containsValue should return true for existing value");
        
        map.remove(String.class, Integer.class, 1L);
        assertTrue(map.isEmpty() == (map.size() == 0), "isEmpty should be consistent with size after removal");
        assertFalse(map.containsKey(String.class, Integer.class, 1L), "containsKey should return false after removal");
        assertFalse(map.containsValue("test"), "containsValue should return false after removal");
        
        map.clear();
        assertTrue(map.isEmpty(), "Should be empty after clear");
        assertEquals(0, map.size(), "Size should be 0 after clear");
    }
}