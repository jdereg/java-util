package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for TrackingMap concurrent functionality when wrapping
 * ConcurrentMap and ConcurrentNavigableMap implementations.
 */
public class TrackingMapConcurrentTest {

    private TrackingMap<String, Integer> concurrentTrackingMap;
    private TrackingMap<String, Integer> navigableTrackingMap;
    private TrackingMap<String, Integer> regularTrackingMap;

    @BeforeEach
    void setUp() {
        // ConcurrentHashMap backing
        concurrentTrackingMap = new TrackingMap<>(new ConcurrentHashMap<>());
        
        // ConcurrentSkipListMap backing  
        navigableTrackingMap = new TrackingMap<>(new ConcurrentSkipListMap<>());
        
        // Regular HashMap backing
        regularTrackingMap = new TrackingMap<>(new HashMap<>());
    }

    @Test
    public void testPutIfAbsentConcurrent() {
        // Test putIfAbsent with ConcurrentHashMap backing
        assertNull(concurrentTrackingMap.putIfAbsent("key1", 100));
        assertEquals(Integer.valueOf(100), concurrentTrackingMap.putIfAbsent("key1", 200));
        
        // Verify tracking behavior - putIfAbsent doesn't mark as accessed
        assertFalse(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Verify the value is correct (this will mark as accessed)
        assertEquals(Integer.valueOf(100), concurrentTrackingMap.get("key1"));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
    }

    @Test
    public void testPutIfAbsentNavigable() {
        // Test putIfAbsent with ConcurrentSkipListMap backing
        assertNull(navigableTrackingMap.putIfAbsent("key1", 100));
        assertEquals(Integer.valueOf(100), navigableTrackingMap.putIfAbsent("key1", 200));
        assertEquals(Integer.valueOf(100), navigableTrackingMap.get("key1"));
        
        // Verify access tracking
        assertTrue(navigableTrackingMap.keysUsed().contains("key1"));
    }

    @Test
    public void testPutIfAbsentRegular() {
        // Test putIfAbsent with regular HashMap (uses fallback implementation)
        assertNull(regularTrackingMap.putIfAbsent("key1", 100));
        assertEquals(Integer.valueOf(100), regularTrackingMap.putIfAbsent("key1", 200));
        assertEquals(Integer.valueOf(100), regularTrackingMap.get("key1"));
    }

    @Test
    public void testPutIfAbsentRegularTreatsNullMappedValueAsAbsent() {
        regularTrackingMap.put("key1", null);
        assertNull(regularTrackingMap.putIfAbsent("key1", 100));
        assertEquals(Integer.valueOf(100), regularTrackingMap.get("key1"));
    }

    @Test
    public void testRemoveByKeyValueConcurrent() {
        concurrentTrackingMap.put("key1", 100);
        concurrentTrackingMap.get("key1"); // Mark as accessed
        
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Remove with wrong value should fail
        assertFalse(concurrentTrackingMap.remove("key1", 999));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Remove with correct value should succeed and remove from tracking
        assertTrue(concurrentTrackingMap.remove("key1", 100));
        assertFalse(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Verify key is no longer in map
        assertFalse(concurrentTrackingMap.containsKey("key1"));
    }

    @Test
    public void testReplaceMethods() {
        concurrentTrackingMap.put("key1", 100);
        
        // Test replace(K, V, V)
        assertFalse(concurrentTrackingMap.replace("key1", 999, 200));
        assertEquals(Integer.valueOf(100), concurrentTrackingMap.get("key1"));
        
        assertTrue(concurrentTrackingMap.replace("key1", 100, 200));
        assertEquals(Integer.valueOf(200), concurrentTrackingMap.get("key1"));
        
        // Test replace(K, V)
        assertEquals(Integer.valueOf(200), concurrentTrackingMap.replace("key1", 300));
        assertEquals(Integer.valueOf(300), concurrentTrackingMap.get("key1"));
        
        assertNull(concurrentTrackingMap.replace("nonexistent", 400));
    }

    @Test
    public void testComputeMethods() {
        // Test computeIfAbsent
        Integer result = concurrentTrackingMap.computeIfAbsent("key1", k -> k.length() * 10);
        assertEquals(Integer.valueOf(40), result); // "key1".length() * 10 = 40
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Second call should return existing value
        Integer result2 = concurrentTrackingMap.computeIfAbsent("key1", k -> 999);
        assertEquals(Integer.valueOf(40), result2);
        
        // Test computeIfPresent
        Integer result3 = concurrentTrackingMap.computeIfPresent("key1", (k, v) -> v + 10);
        assertEquals(Integer.valueOf(50), result3);
        
        // Test compute
        Integer result4 = concurrentTrackingMap.compute("key2", (k, v) -> v == null ? 100 : v + 1);
        assertEquals(Integer.valueOf(100), result4);
        assertTrue(concurrentTrackingMap.keysUsed().contains("key2"));
    }

    @Test
    public void testComputeIfPresentTracksKeyWhenRemappingRemovesEntry() {
        concurrentTrackingMap.put("key1", 100);

        assertNull(concurrentTrackingMap.computeIfPresent("key1", (k, v) -> null));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        assertFalse(concurrentTrackingMap.getWrappedMap().containsKey("key1"));
    }

    @Test
    public void testMergeMethod() {
        concurrentTrackingMap.put("key1", 100);
        
        // Merge with existing key
        Integer result = concurrentTrackingMap.merge("key1", 50, Integer::sum);
        assertEquals(Integer.valueOf(150), result);
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Merge with new key
        Integer result2 = concurrentTrackingMap.merge("key2", 75, Integer::sum);
        assertEquals(Integer.valueOf(75), result2);
        assertTrue(concurrentTrackingMap.keysUsed().contains("key2"));
    }

    @Test
    public void testGetOrDefault() {
        concurrentTrackingMap.put("key1", 100);
        
        // Existing key
        assertEquals(Integer.valueOf(100), concurrentTrackingMap.getOrDefault("key1", 999));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        
        // Non-existing key
        assertEquals(Integer.valueOf(999), concurrentTrackingMap.getOrDefault("key2", 999));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key2")); // Should track access attempt
    }

    @Test
    public void testNavigableMapMethods() {
        // Test with ConcurrentSkipListMap backing
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        navigableTrackingMap.put("date", 4);
        
        // Test navigation methods
        assertEquals("apple", navigableTrackingMap.firstKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
        
        assertEquals("date", navigableTrackingMap.lastKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("date"));
        
        assertEquals("banana", navigableTrackingMap.ceilingKey("b"));
        assertTrue(navigableTrackingMap.keysUsed().contains("banana"));
        
        assertEquals("apple", navigableTrackingMap.floorKey("b"));
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
        
        assertEquals("cherry", navigableTrackingMap.higherKey("banana"));
        assertTrue(navigableTrackingMap.keysUsed().contains("cherry"));
        
        assertEquals("apple", navigableTrackingMap.lowerKey("banana"));
        // apple was already accessed above, so this just confirms it's still tracked
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void testNavigableMapEntryMethods() {
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        navigableTrackingMap.put("date", 4);
        
        // Test firstEntry and lastEntry
        Map.Entry<String, Integer> firstEntry = navigableTrackingMap.firstEntry();
        assertNotNull(firstEntry);
        assertEquals("apple", firstEntry.getKey());
        assertEquals(Integer.valueOf(1), firstEntry.getValue());
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
        
        Map.Entry<String, Integer> lastEntry = navigableTrackingMap.lastEntry();
        assertNotNull(lastEntry);
        assertEquals("date", lastEntry.getKey());
        assertEquals(Integer.valueOf(4), lastEntry.getValue());
        assertTrue(navigableTrackingMap.keysUsed().contains("date"));
        
        // Test ceilingEntry (>= key)
        Map.Entry<String, Integer> ceilingEntry = navigableTrackingMap.ceilingEntry("b");
        assertNotNull(ceilingEntry);
        assertEquals("banana", ceilingEntry.getKey());
        assertEquals(Integer.valueOf(2), ceilingEntry.getValue());
        assertTrue(navigableTrackingMap.keysUsed().contains("banana"));
        
        // Test floorEntry (<= key) - should return greatest key <= "cherry", which is "cherry" itself
        Map.Entry<String, Integer> floorEntry = navigableTrackingMap.floorEntry("cherry");
        assertNotNull(floorEntry);
        assertEquals("cherry", floorEntry.getKey());
        assertEquals(Integer.valueOf(3), floorEntry.getValue());
        assertTrue(navigableTrackingMap.keysUsed().contains("cherry"));
        
        // Test lowerEntry (< key)
        Map.Entry<String, Integer> lowerEntry = navigableTrackingMap.lowerEntry("cherry");
        assertNotNull(lowerEntry);
        assertEquals("banana", lowerEntry.getKey());
        assertEquals(Integer.valueOf(2), lowerEntry.getValue());
        // banana was already tracked above, so this just confirms it's still tracked
        assertTrue(navigableTrackingMap.keysUsed().contains("banana"));
        
        // Test higherEntry (> key)
        Map.Entry<String, Integer> higherEntry = navigableTrackingMap.higherEntry("banana");
        assertNotNull(higherEntry);
        assertEquals("cherry", higherEntry.getKey());
        assertEquals(Integer.valueOf(3), higherEntry.getValue());
        // cherry was already tracked above, so this just confirms it's still tracked
        assertTrue(navigableTrackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void testNavigableMapEntryMethodsWithNonExistentKeys() {
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("date", 4);
        
        // Test entry methods with keys that don't exist but should return neighboring entries
        
        // ceilingEntry with key before all entries
        Map.Entry<String, Integer> ceilingEntryFirst = navigableTrackingMap.ceilingEntry("a");
        assertNotNull(ceilingEntryFirst);
        assertEquals("banana", ceilingEntryFirst.getKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("banana"));
        
        // floorEntry with key after all entries  
        Map.Entry<String, Integer> floorEntryLast = navigableTrackingMap.floorEntry("z");
        assertNotNull(floorEntryLast);
        assertEquals("date", floorEntryLast.getKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("date"));
        
        // lowerEntry with key before all entries should return null
        Map.Entry<String, Integer> lowerEntryNull = navigableTrackingMap.lowerEntry("a");
        assertNull(lowerEntryNull);
        
        // higherEntry with key after all entries should return null
        Map.Entry<String, Integer> higherEntryNull = navigableTrackingMap.higherEntry("z");
        assertNull(higherEntryNull);
        
        // Test with key between existing keys
        Map.Entry<String, Integer> ceilingEntryBetween = navigableTrackingMap.ceilingEntry("c");
        assertNotNull(ceilingEntryBetween);
        assertEquals("date", ceilingEntryBetween.getKey());
        // date was already tracked above
        assertTrue(navigableTrackingMap.keysUsed().contains("date"));
        
        Map.Entry<String, Integer> floorEntryBetween = navigableTrackingMap.floorEntry("c");
        assertNotNull(floorEntryBetween);
        assertEquals("banana", floorEntryBetween.getKey());
        // banana was already tracked above
        assertTrue(navigableTrackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void testPollMethods() {
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        
        // Track some keys first
        navigableTrackingMap.get("apple");
        navigableTrackingMap.get("cherry");
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
        assertTrue(navigableTrackingMap.keysUsed().contains("cherry"));
        
        // Poll first entry - should remove from tracking
        Map.Entry<String, Integer> first = navigableTrackingMap.pollFirstEntry();
        assertNotNull(first);
        assertEquals("apple", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());
        assertFalse(navigableTrackingMap.keysUsed().contains("apple"));
        
        // Poll last entry - should remove from tracking
        Map.Entry<String, Integer> last = navigableTrackingMap.pollLastEntry();
        assertNotNull(last);
        assertEquals("cherry", last.getKey());
        assertEquals(Integer.valueOf(3), last.getValue());
        assertFalse(navigableTrackingMap.keysUsed().contains("cherry"));
        
        // Only banana should remain
        assertEquals(1, navigableTrackingMap.size());
        assertTrue(navigableTrackingMap.containsKey("banana"));
    }

    @Test
    public void testSubMapViews() {
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        navigableTrackingMap.put("date", 4);
        
        // Test subMap
        TrackingMap<String, Integer> subMap = navigableTrackingMap.subMap("banana", true, "date", false);
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey("banana"));
        assertTrue(subMap.containsKey("cherry"));
        assertFalse(subMap.containsKey("date"));
        
        // Test headMap
        TrackingMap<String, Integer> headMap = navigableTrackingMap.headMap("cherry", false);
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey("apple"));
        assertTrue(headMap.containsKey("banana"));
        
        // Test tailMap
        TrackingMap<String, Integer> tailMap = navigableTrackingMap.tailMap("banana", true);
        assertEquals(3, tailMap.size());
        assertTrue(tailMap.containsKey("banana"));
        assertTrue(tailMap.containsKey("cherry"));
        assertTrue(tailMap.containsKey("date"));
    }

    @Test
    public void testNavigableKeySetAndDescendingKeySet() {
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        
        NavigableSet<String> keySet = navigableTrackingMap.navigableKeySet();
        assertEquals(3, keySet.size());
        assertEquals("apple", keySet.first());
        assertEquals("cherry", keySet.last());
        
        NavigableSet<String> descendingKeySet = navigableTrackingMap.descendingKeySet();
        assertEquals(3, descendingKeySet.size());
        assertEquals("cherry", descendingKeySet.first());
        assertEquals("apple", descendingKeySet.last());
    }

    @Test
    public void testUnsupportedOperationsWithRegularMap() {
        // Test that NavigableMap operations throw exceptions with regular HashMap
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.firstKey());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.lastKey());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.lowerKey("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.higherKey("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.floorKey("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.ceilingKey("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.firstEntry());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.lastEntry());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.lowerEntry("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.higherEntry("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.floorEntry("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.ceilingEntry("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.pollFirstEntry());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.pollLastEntry());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.navigableKeySet());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.descendingKeySet());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.subMap("a", true, "z", true));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.headMap("m", true));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.tailMap("m", true));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.comparator());
    }

    @Test
    public void testConcurrentThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final AtomicInteger errorCount = new AtomicInteger(0);

        // Pre-populate the map
        for (int i = 0; i < 50; i++) {
            concurrentTrackingMap.put("key" + i, i);
        }

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "key" + (threadId * operationsPerThread + i);
                        
                        // Mix of operations
                        switch (i % 5) {
                            case 0:
                                concurrentTrackingMap.put(key, i);
                                break;
                            case 1:
                                concurrentTrackingMap.get(key);
                                break;
                            case 2:
                                concurrentTrackingMap.putIfAbsent(key, i);
                                break;
                            case 3:
                                concurrentTrackingMap.computeIfAbsent(key, k -> k.hashCode());
                                break;
                            case 4:
                                concurrentTrackingMap.containsKey(key);
                                break;
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(30, TimeUnit.SECONDS));
        assertEquals(0, errorCount.get(), "No exceptions should occur during concurrent operations");
        
        // Verify the map is in a consistent state
        assertNotNull(concurrentTrackingMap.keysUsed());
        assertTrue(concurrentTrackingMap.size() > 0);
    }

    @Test
    public void testForEachAndReplaceAll() {
        concurrentTrackingMap.put("key1", 10);
        concurrentTrackingMap.put("key2", 20);
        concurrentTrackingMap.put("key3", 30);
        
        // Test forEach
        final Map<String, Integer> collected = new HashMap<>();
        concurrentTrackingMap.forEach(collected::put);
        assertEquals(3, collected.size());
        assertEquals(Integer.valueOf(10), collected.get("key1"));
        assertEquals(Integer.valueOf(20), collected.get("key2"));
        assertEquals(Integer.valueOf(30), collected.get("key3"));
        
        // Test replaceAll
        concurrentTrackingMap.replaceAll((k, v) -> v * 2);
        assertEquals(Integer.valueOf(20), concurrentTrackingMap.get("key1"));
        assertEquals(Integer.valueOf(40), concurrentTrackingMap.get("key2"));
        assertEquals(Integer.valueOf(60), concurrentTrackingMap.get("key3"));
        
        // Verify tracking
        assertTrue(concurrentTrackingMap.keysUsed().contains("key1"));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key2"));
        assertTrue(concurrentTrackingMap.keysUsed().contains("key3"));
    }

    @Test
    public void testHeadMapAndTailMapMethods() {
        // Test with ConcurrentSkipListMap backing to test NavigableMap methods
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("cherry", 3);
        navigableTrackingMap.put("date", 4);
        navigableTrackingMap.put("elderberry", 5);
        
        // Test headMap(K toKey) - exclusive
        TrackingMap<String, Integer> headMapExclusive = navigableTrackingMap.headMap("cherry");
        assertEquals(2, headMapExclusive.size());
        assertTrue(headMapExclusive.containsKey("apple"));
        assertTrue(headMapExclusive.containsKey("banana"));
        assertFalse(headMapExclusive.containsKey("cherry"));
        
        // Test headMap(K toKey, boolean inclusive) - inclusive
        TrackingMap<String, Integer> headMapInclusive = navigableTrackingMap.headMap("cherry", true);
        assertEquals(3, headMapInclusive.size());
        assertTrue(headMapInclusive.containsKey("apple"));
        assertTrue(headMapInclusive.containsKey("banana"));
        assertTrue(headMapInclusive.containsKey("cherry"));
        
        // Test tailMap(K fromKey) - inclusive
        TrackingMap<String, Integer> tailMapInclusive = navigableTrackingMap.tailMap("cherry");
        assertEquals(3, tailMapInclusive.size());
        assertTrue(tailMapInclusive.containsKey("cherry"));
        assertTrue(tailMapInclusive.containsKey("date"));
        assertTrue(tailMapInclusive.containsKey("elderberry"));
        
        // Test tailMap(K fromKey, boolean inclusive) - exclusive
        TrackingMap<String, Integer> tailMapExclusive = navigableTrackingMap.tailMap("cherry", false);
        assertEquals(2, tailMapExclusive.size());
        assertFalse(tailMapExclusive.containsKey("cherry"));
        assertTrue(tailMapExclusive.containsKey("date"));
        assertTrue(tailMapExclusive.containsKey("elderberry"));
        
        // Verify that the sub-maps maintain tracking behavior
        headMapInclusive.get("apple");
        assertTrue(headMapInclusive.keysUsed().contains("apple"));
        
        tailMapInclusive.get("date");
        assertTrue(tailMapInclusive.keysUsed().contains("date"));
    }

    @Test
    public void testHeadMapAndTailMapWithRegularMap() {
        // Test that NavigableMap operations throw exceptions with regular HashMap
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.headMap("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.headMap("test", true));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.tailMap("test"));
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.tailMap("test", true));
    }

    @Test
    public void testFirstKeyAndLastKeyMethods() {
        // Test with ConcurrentSkipListMap backing
        navigableTrackingMap.put("banana", 2);
        navigableTrackingMap.put("apple", 1);
        navigableTrackingMap.put("cherry", 3);
        
        // Test firstKey and lastKey - these should mark keys as accessed
        assertEquals("apple", navigableTrackingMap.firstKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("apple"));
        
        assertEquals("cherry", navigableTrackingMap.lastKey());
        assertTrue(navigableTrackingMap.keysUsed().contains("cherry"));
        
        // Test with regular HashMap - should throw exception
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.firstKey());
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.lastKey());
    }

    @Test
    public void testComparatorMethod() {
        // Test with ConcurrentSkipListMap (natural ordering)
        assertNull(navigableTrackingMap.comparator());
        
        // Test with regular HashMap - should throw exception
        assertThrows(UnsupportedOperationException.class, () -> regularTrackingMap.comparator());
        
        // Test with custom comparator
        TrackingMap<String, Integer> customComparatorMap = new TrackingMap<>(
            new ConcurrentSkipListMap<String, Integer>(String.CASE_INSENSITIVE_ORDER)
        );
        assertNotNull(customComparatorMap.comparator());
        assertEquals(String.CASE_INSENSITIVE_ORDER, customComparatorMap.comparator());
    }

    @Test
    public void testTrackingBehaviorWithConcurrentOperations() {
        // Verify that read operations mark keys as accessed but write operations don't
        concurrentTrackingMap.put("write1", 1);
        assertFalse(concurrentTrackingMap.keysUsed().contains("write1"));
        
        concurrentTrackingMap.putIfAbsent("write2", 2);
        assertFalse(concurrentTrackingMap.keysUsed().contains("write2"));
        
        concurrentTrackingMap.replace("write1", 1, 10);
        assertFalse(concurrentTrackingMap.keysUsed().contains("write1"));
        
        // Read operations should mark as accessed
        concurrentTrackingMap.get("write1");
        assertTrue(concurrentTrackingMap.keysUsed().contains("write1"));
        
        concurrentTrackingMap.containsKey("write2");
        assertTrue(concurrentTrackingMap.keysUsed().contains("write2"));
        
        concurrentTrackingMap.getOrDefault("write3", 999);
        assertTrue(concurrentTrackingMap.keysUsed().contains("write3"));
        
        // Compute operations should mark as accessed (since they read current value)
        concurrentTrackingMap.computeIfAbsent("compute1", k -> 100);
        assertTrue(concurrentTrackingMap.keysUsed().contains("compute1"));
        
        concurrentTrackingMap.merge("merge1", 50, Integer::sum);
        assertTrue(concurrentTrackingMap.keysUsed().contains("merge1"));
    }
}
