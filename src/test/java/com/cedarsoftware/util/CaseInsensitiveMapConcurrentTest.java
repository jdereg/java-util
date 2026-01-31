package com.cedarsoftware.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CaseInsensitiveMap's concurrent-specific methods that delegate to ConcurrentHashMap.
 */
class CaseInsensitiveMapConcurrentTest {

    private CaseInsensitiveMap<String, String> concurrentMap;
    private CaseInsensitiveMap<String, String> linkedMap;

    @BeforeEach
    void setUp() {
        // Create CaseInsensitiveMap backed by ConcurrentHashMap
        concurrentMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        concurrentMap.put("Key1", "value1");
        concurrentMap.put("KEY2", "value2");
        concurrentMap.put("key3", "value3");

        // Create CaseInsensitiveMap backed by LinkedHashMap for fallback testing
        linkedMap = new CaseInsensitiveMap<>();
        linkedMap.put("Key1", "value1");
        linkedMap.put("KEY2", "value2");
        linkedMap.put("key3", "value3");
    }

    @Test
    void testMappingCount_ConcurrentHashMap() {
        // Test with ConcurrentHashMap backing
        long count = concurrentMap.mappingCount();
        assertEquals(3L, count);

        // Add more entries to verify dynamic count
        concurrentMap.put("Key4", "value4");
        assertEquals(4L, concurrentMap.mappingCount());
    }

    @Test
    void testMappingCount_LinkedHashMap() {
        // Test fallback with LinkedHashMap backing
        long count = linkedMap.mappingCount();
        assertEquals(3L, count);

        // Should fall back to size() for non-concurrent maps
        linkedMap.put("Key4", "value4");
        assertEquals(4L, linkedMap.mappingCount());
    }

    @Test
    void testForEachWithParallelismThreshold_ConcurrentHashMap() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<String> lastKey = new AtomicReference<>();

        concurrentMap.forEach(1L, (key, value) -> {
            counter.incrementAndGet();
            lastKey.set(key);
            // Verify we get original String keys, not CaseInsensitiveString
            assertTrue(key instanceof String);
            assertFalse(key.getClass().getSimpleName().contains("CaseInsensitive"));
        });

        assertEquals(3, counter.get());
        assertNotNull(lastKey.get());
    }

    @Test
    void testForEachWithParallelismThreshold_LinkedHashMap() {
        AtomicInteger counter = new AtomicInteger(0);

        linkedMap.forEach(1L, (key, value) -> {
            counter.incrementAndGet();
            // Should still unwrap keys properly
            assertTrue(key instanceof String);
            assertFalse(key.getClass().getSimpleName().contains("CaseInsensitive"));
        });

        assertEquals(3, counter.get());
    }

    @Test
    void testForEachKeyWithParallelismThreshold_ConcurrentHashMap() {
        AtomicInteger counter = new AtomicInteger(0);

        concurrentMap.forEachKey(1L, key -> {
            counter.incrementAndGet();
            // Verify original String keys
            assertTrue(key instanceof String);
            assertFalse(key.getClass().getSimpleName().contains("CaseInsensitive"));
            assertTrue(key.equals("Key1") || key.equals("KEY2") || key.equals("key3"));
        });

        assertEquals(3, counter.get());
    }

    @Test
    void testForEachKeyWithParallelismThreshold_LinkedHashMap() {
        AtomicInteger counter = new AtomicInteger(0);

        linkedMap.forEachKey(1L, key -> {
            counter.incrementAndGet();
            assertTrue(key instanceof String);
            assertFalse(key.getClass().getSimpleName().contains("CaseInsensitive"));
        });

        assertEquals(3, counter.get());
    }

    @Test
    void testForEachValueWithParallelismThreshold_ConcurrentHashMap() {
        AtomicInteger counter = new AtomicInteger(0);

        concurrentMap.forEachValue(1L, value -> {
            counter.incrementAndGet();
            assertTrue(value.startsWith("value"));
        });

        assertEquals(3, counter.get());
    }

    @Test
    void testForEachValueWithParallelismThreshold_LinkedHashMap() {
        AtomicInteger counter = new AtomicInteger(0);

        linkedMap.forEachValue(1L, value -> {
            counter.incrementAndGet();
            assertTrue(value.startsWith("value"));
        });

        assertEquals(3, counter.get());
    }

    @Test
    void testSearchKeys_ConcurrentHashMap() {
        // Search for a key that matches pattern
        String result = concurrentMap.searchKeys(1L, key -> {
            if (key.toLowerCase().equals("key1")) {
                return "found:" + key;
            }
            return null;
        });

        assertEquals("found:Key1", result);

        // Search for non-existent pattern
        String notFound = concurrentMap.searchKeys(1L, key -> {
            if (key.equals("nonexistent")) {
                return "found";
            }
            return null;
        });

        assertNull(notFound);
    }

    @Test
    void testSearchKeys_LinkedHashMap() {
        String result = linkedMap.searchKeys(1L, key -> {
            if (key.toLowerCase().equals("key2")) {
                return "found:" + key;
            }
            return null;
        });

        assertEquals("found:KEY2", result);
    }

    @Test
    void testSearchValues_ConcurrentHashMap() {
        String result = concurrentMap.searchValues(1L, value -> {
            if (value.equals("value2")) {
                return "found:" + value;
            }
            return null;
        });

        assertEquals("found:value2", result);

        // Search for non-existent value
        String notFound = concurrentMap.searchValues(1L, value -> {
            if (value.equals("nonexistent")) {
                return "found";
            }
            return null;
        });

        assertNull(notFound);
    }

    @Test
    void testSearchValues_LinkedHashMap() {
        String result = linkedMap.searchValues(1L, value -> {
            if (value.equals("value3")) {
                return "found:" + value;
            }
            return null;
        });

        assertEquals("found:value3", result);
    }

    @Test
    void testReduceKeys_ConcurrentHashMap() {
        // Concatenate all keys
        String result = concurrentMap.reduceKeys(1L, 
            key -> key.toLowerCase(), 
            (a, b) -> a + "," + b);

        assertNotNull(result);
        // Should contain all three keys in some order
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains("key3"));
    }

    @Test
    void testReduceKeys_LinkedHashMap() {
        String result = linkedMap.reduceKeys(1L,
            key -> key.toLowerCase(),
            (a, b) -> a + "," + b);

        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains("key3"));
    }

    @Test
    void testReduceValues_ConcurrentHashMap() {
        // Sum the numbers in values (assuming they're "value1", "value2", etc.)
        Integer result = concurrentMap.reduceValues(1L,
            value -> Integer.parseInt(value.substring(5)), // Extract number from "valueN"
            Integer::sum);

        assertEquals(Integer.valueOf(6), result); // 1 + 2 + 3 = 6
    }

    @Test
    void testReduceValues_LinkedHashMap() {
        Integer result = linkedMap.reduceValues(1L,
            value -> Integer.parseInt(value.substring(5)),
            Integer::sum);

        assertEquals(Integer.valueOf(6), result); // 1 + 2 + 3 = 6
    }

    @Test
    void testReduceKeysWithNullTransformer() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.reduceKeys(1L, (Function<String, String>) null, String::concat);
        });
    }

    @Test
    void testReduceKeysWithNullReducer() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.reduceKeys(1L, key -> key, (BiFunction<String, String, String>) null);
        });
    }

    @Test
    void testReduceValuesWithNullTransformer() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.reduceValues(1L, (Function<String, String>) null, String::concat);
        });
    }

    @Test
    void testReduceValuesWithNullReducer() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.reduceValues(1L, value -> value, (BiFunction<String, String, String>) null);
        });
    }

    @Test
    void testSearchKeysWithNullFunction() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.searchKeys(1L, null);
        });
    }

    @Test
    void testSearchValuesWithNullFunction() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.searchValues(1L, null);
        });
    }

    @Test
    void testForEachWithNullAction() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.forEach(1L, null);
        });
    }

    @Test
    void testForEachKeyWithNullAction() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.forEachKey(1L, null);
        });
    }

    @Test
    void testForEachValueWithNullAction() {
        assertThrows(NullPointerException.class, () -> {
            concurrentMap.forEachValue(1L, null);
        });
    }

    @Test
    void testKeyUnwrappingInConcurrentOperations() {
        // Verify that keys are properly unwrapped in all concurrent operations
        concurrentMap.forEach(1L, (key, value) -> {
            // Key should be the original String, not CaseInsensitiveString
            String keyClassName = key.getClass().getSimpleName();
            assertEquals("String", keyClassName);
        });

        concurrentMap.forEachKey(1L, key -> {
            String keyClassName = key.getClass().getSimpleName();
            assertEquals("String", keyClassName);
        });

        concurrentMap.searchKeys(1L, key -> {
            String keyClassName = key.getClass().getSimpleName();
            assertEquals("String", keyClassName);
            return null;
        });

        concurrentMap.reduceKeys(1L, key -> {
            String keyClassName = key.getClass().getSimpleName();
            assertEquals("String", keyClassName);
            return key;
        }, (a, b) -> a + "," + b);
    }

    @Test
    void testEmptyMapOperations() {
        CaseInsensitiveMap<String, String> emptyMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());

        assertEquals(0L, emptyMap.mappingCount());

        AtomicInteger counter = new AtomicInteger(0);
        emptyMap.forEach(1L, (k, v) -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        emptyMap.forEachKey(1L, k -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        emptyMap.forEachValue(1L, v -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        assertNull(emptyMap.searchKeys(1L, k -> "found"));
        assertNull(emptyMap.searchValues(1L, v -> "found"));
        assertNull(emptyMap.reduceKeys(1L, k -> k, (a, b) -> a + "," + b));
        assertNull(emptyMap.reduceValues(1L, v -> v, (a, b) -> a + "," + b));
    }
}