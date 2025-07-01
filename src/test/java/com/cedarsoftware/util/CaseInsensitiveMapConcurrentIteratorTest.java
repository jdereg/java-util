package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying concurrent-aware iterator behavior in CaseInsensitiveMap.
 * Tests that iterators properly inherit concurrent properties when backed by ConcurrentHashMap.
 */
class CaseInsensitiveMapConcurrentIteratorTest {

    private CaseInsensitiveMap<String, String> concurrentMap;
    private CaseInsensitiveMap<String, String> hashMap;

    @BeforeEach
    void setUp() {
        // Map backed by ConcurrentHashMap
        concurrentMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        concurrentMap.put("Key1", "value1");
        concurrentMap.put("KEY2", "value2"); 
        concurrentMap.put("key3", "value3");

        // Map backed by regular HashMap
        hashMap = new CaseInsensitiveMap<>(new HashMap<>());
        hashMap.put("Key1", "value1");
        hashMap.put("KEY2", "value2");
        hashMap.put("key3", "value3");
    }

    @AfterEach
    void tearDown() {
        concurrentMap = null;
        hashMap = null;
    }

    @Test
    void testKeyIteratorConcurrentBacking_ConcurrentHashMap() {
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        // Verify we get the custom concurrent-aware iterator
        assertTrue(iterator.getClass().getName().contains("ConcurrentAwareKeyIterator"));
        
        // Test basic iteration functionality
        Set<String> keys = new HashSet<>();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        
        assertEquals(3, keys.size());
        assertTrue(keys.contains("Key1"));
        assertTrue(keys.contains("KEY2")); 
        assertTrue(keys.contains("key3"));
    }

    @Test
    void testKeyIteratorConcurrentBacking_HashMap() {
        Iterator<String> iterator = hashMap.keySet().iterator();
        
        // Verify we get the custom concurrent-aware iterator (even for HashMap)
        assertTrue(iterator.getClass().getName().contains("ConcurrentAwareKeyIterator"));
        
        // Test basic iteration functionality
        Set<String> keys = new HashSet<>();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        
        assertEquals(3, keys.size());
        assertTrue(keys.contains("Key1"));
        assertTrue(keys.contains("KEY2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    void testEntryIteratorConcurrentBacking_ConcurrentHashMap() {
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        // Verify we get the custom concurrent-aware iterator
        assertTrue(iterator.getClass().getName().contains("ConcurrentAwareEntryIterator"));
        
        // Test basic iteration functionality
        Map<String, String> entries = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            entries.put(entry.getKey(), entry.getValue());
        }
        
        assertEquals(3, entries.size());
        assertEquals("value1", entries.get("Key1"));
        assertEquals("value2", entries.get("KEY2"));
        assertEquals("value3", entries.get("key3"));
    }

    @Test
    void testEntryIteratorConcurrentBacking_HashMap() {
        Iterator<Map.Entry<String, String>> iterator = hashMap.entrySet().iterator();
        
        // Verify we get the custom concurrent-aware iterator
        assertTrue(iterator.getClass().getName().contains("ConcurrentAwareEntryIterator"));
        
        // Test basic iteration functionality
        Map<String, String> entries = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            entries.put(entry.getKey(), entry.getValue());
        }
        
        assertEquals(3, entries.size());
        assertEquals("value1", entries.get("Key1"));
        assertEquals("value2", entries.get("KEY2"));
        assertEquals("value3", entries.get("key3"));
    }

    @Test
    void testKeyIteratorForEachRemaining_ConcurrentHashMap() {
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        // Consume first element
        assertTrue(iterator.hasNext());
        iterator.next();
        
        // Test forEachRemaining on remaining elements
        Set<String> remainingKeys = new HashSet<>();
        iterator.forEachRemaining(remainingKeys::add);
        
        assertEquals(2, remainingKeys.size());
    }

    @Test
    void testEntryIteratorForEachRemaining_ConcurrentHashMap() {
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        // Consume first element
        assertTrue(iterator.hasNext());
        iterator.next();
        
        // Test forEachRemaining on remaining elements
        Map<String, String> remainingEntries = new HashMap<>();
        iterator.forEachRemaining(entry -> remainingEntries.put(entry.getKey(), entry.getValue()));
        
        assertEquals(2, remainingEntries.size());
    }

    @Test
    void testKeyIteratorRemove_ConcurrentHashMap() {
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        assertTrue(iterator.hasNext());
        String firstKey = iterator.next();
        assertNotNull(firstKey);
        
        // Remove the first element
        iterator.remove();
        
        // Verify removal
        assertEquals(2, concurrentMap.size());
        assertFalse(concurrentMap.containsKey(firstKey));
    }

    @Test
    void testEntryIteratorRemove_ConcurrentHashMap() {
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        assertTrue(iterator.hasNext());
        Map.Entry<String, String> firstEntry = iterator.next();
        assertNotNull(firstEntry);
        String firstKey = firstEntry.getKey();
        
        // Remove the first element
        iterator.remove();
        
        // Verify removal
        assertEquals(2, concurrentMap.size());
        assertFalse(concurrentMap.containsKey(firstKey));
    }

    @Test
    void testConcurrentModificationTolerance_KeyIterator() {
        // This test verifies that ConcurrentHashMap-backed iterators don't throw
        // ConcurrentModificationException during concurrent modifications
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        // Start iteration
        assertTrue(iterator.hasNext());
        String firstKey = iterator.next();
        assertNotNull(firstKey);
        
        // Modify the map during iteration (this would cause CME with HashMap)
        concurrentMap.put("NewKey", "newValue");
        
        // Iterator should continue to work without throwing CME
        assertDoesNotThrow(() -> {
            while (iterator.hasNext()) {
                iterator.next();
            }
        });
    }

    @Test
    void testConcurrentModificationTolerance_EntryIterator() {
        // This test verifies that ConcurrentHashMap-backed iterators don't throw
        // ConcurrentModificationException during concurrent modifications
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        // Start iteration
        assertTrue(iterator.hasNext());
        Map.Entry<String, String> firstEntry = iterator.next();
        assertNotNull(firstEntry);
        
        // Modify the map during iteration (this would cause CME with HashMap)
        concurrentMap.put("NewKey", "newValue");
        
        // Iterator should continue to work without throwing CME
        assertDoesNotThrow(() -> {
            while (iterator.hasNext()) {
                iterator.next();
            }
        });
    }

    @Test
    void testWeakConsistency_KeyIterator() {
        // Test weak consistency: iterator may or may not see concurrent additions
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        Set<String> iteratedKeys = new HashSet<>();
        
        // Collect first key
        if (iterator.hasNext()) {
            iteratedKeys.add(iterator.next());
        }
        
        // Add new key during iteration
        concurrentMap.put("WeakConsistencyTest", "value");
        
        // Continue iteration - may or may not see the new key (weak consistency)
        while (iterator.hasNext()) {
            iteratedKeys.add(iterator.next());
        }
        
        // The iterator will see at least the original keys
        assertTrue(iteratedKeys.size() >= 2); // At least 2 of the original 3, since we consumed 1
        assertTrue(iteratedKeys.size() <= 4); // At most all original + the new one
    }

    @Test
    void testWeakConsistency_EntryIterator() {
        // Test weak consistency: iterator may or may not see concurrent additions
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        Set<String> iteratedKeys = new HashSet<>();
        
        // Collect first entry
        if (iterator.hasNext()) {
            iteratedKeys.add(iterator.next().getKey());
        }
        
        // Add new entry during iteration
        concurrentMap.put("WeakConsistencyTest", "value");
        
        // Continue iteration - may or may not see the new entry (weak consistency)
        while (iterator.hasNext()) {
            iteratedKeys.add(iterator.next().getKey());
        }
        
        // The iterator will see at least the original entries
        assertTrue(iteratedKeys.size() >= 2); // At least 2 of the original 3, since we consumed 1
        assertTrue(iteratedKeys.size() <= 4); // At most all original + the new one
    }

    @Test
    void testKeyUnwrapping_KeyIterator() {
        // Verify that keys are properly unwrapped from CaseInsensitiveString to String
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        while (iterator.hasNext()) {
            String key = iterator.next();
            assertNotNull(key);
            assertTrue(key instanceof String);
            // Key should be unwrapped String, not the internal representation
            assertFalse(key.getClass().getName().contains("CaseInsensitiveString"));
        }
    }

    @Test
    void testKeyUnwrapping_EntryIterator() {
        // Verify that entry keys are properly unwrapped from CaseInsensitiveString to String
        Iterator<Map.Entry<String, String>> iterator = concurrentMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            assertNotNull(key);
            assertTrue(key instanceof String);
            // Key should be unwrapped String, not the internal representation
            assertFalse(key.getClass().getName().contains("CaseInsensitiveString"));
        }
    }

    @Test
    void testForEachRemainingWithConsumerException() {
        Iterator<String> iterator = concurrentMap.keySet().iterator();
        
        // Consume first element
        iterator.next();
        
        // Test that exceptions in the consumer are properly propagated
        AtomicInteger count = new AtomicInteger(0);
        
        assertThrows(RuntimeException.class, () -> {
            iterator.forEachRemaining(key -> {
                count.incrementAndGet();
                if (count.get() == 1) {
                    throw new RuntimeException("Test exception");
                }
            });
        });
        
        assertEquals(1, count.get());
    }

    @Test
    void testIteratorConsistencyAcrossDifferentBackingMaps() {
        // Test that iterator behavior is consistent regardless of backing map type
        
        // Both iterators should have the same basic behavior
        Iterator<String> concurrentIterator = concurrentMap.keySet().iterator();
        Iterator<String> hashIterator = hashMap.keySet().iterator();
        
        // Both should have elements
        assertTrue(concurrentIterator.hasNext());
        assertTrue(hashIterator.hasNext());
        
        // Both should return String keys (not CaseInsensitiveString)
        String concurrentKey = concurrentIterator.next();
        String hashKey = hashIterator.next();
        
        assertTrue(concurrentKey instanceof String);
        assertTrue(hashKey instanceof String);
        // Keys should be unwrapped Strings, not the internal representation
        assertFalse(concurrentKey.getClass().getName().contains("CaseInsensitiveString"));
        assertFalse(hashKey.getClass().getName().contains("CaseInsensitiveString"));
    }

    @Test 
    void testMultipleIteratorsFromSameMap() {
        // Test that multiple iterators can be created from the same map
        Iterator<String> iterator1 = concurrentMap.keySet().iterator();
        Iterator<String> iterator2 = concurrentMap.keySet().iterator();
        
        // Both iterators should be independent
        assertTrue(iterator1.hasNext());
        assertTrue(iterator2.hasNext());
        
        String key1 = iterator1.next();
        String key2 = iterator2.next();
        
        // They might return the same key (different instances) or different keys
        assertNotNull(key1);
        assertNotNull(key2);
        
        // Both should still have remaining elements (since we only consumed one from each)
        assertTrue(iterator1.hasNext());
        assertTrue(iterator2.hasNext());
    }

    @Test
    void testEmptyMapIterator() {
        CaseInsensitiveMap<String, String> emptyMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        
        Iterator<String> keyIterator = emptyMap.keySet().iterator();
        Iterator<Map.Entry<String, String>> entryIterator = emptyMap.entrySet().iterator();
        
        assertFalse(keyIterator.hasNext());
        assertFalse(entryIterator.hasNext());
        
        // forEachRemaining should work on empty iterators
        AtomicInteger count = new AtomicInteger(0);
        
        assertDoesNotThrow(() -> {
            keyIterator.forEachRemaining(key -> count.incrementAndGet());
            entryIterator.forEachRemaining(entry -> count.incrementAndGet());
        });
        
        assertEquals(0, count.get());
    }
}