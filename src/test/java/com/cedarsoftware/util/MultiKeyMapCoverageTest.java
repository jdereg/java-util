package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for MultiKeyMap — targets JaCoCo gaps:
 * - Set-as-key comparison (small set brute-force and large set hash-bucket)
 * - AtomicIntegerArray / AtomicLongArray / AtomicReferenceArray as keys
 * - Primitive array vs non-RandomAccess Collection comparison
 * - Builder getter methods (longSize, capacity, loadFactor, etc.)
 * - toString formatting for array values
 * - isArrayOrCollection paths
 */
class MultiKeyMapCoverageTest {

    // ========== Builder getter methods ==========

    @Test
    void testGetCollectionKeyMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_EXPANDED)
                .build();
        assertThat(map.getCollectionKeyMode()).isEqualTo(MultiKeyMap.CollectionKeyMode.COLLECTIONS_EXPANDED);
    }

    @Test
    void testGetSimpleKeysMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .simpleKeysMode(true)
                .build();
        assertThat(map.getSimpleKeysMode()).isTrue();
    }

    @Test
    void testGetCapacity() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(32)
                .build();
        // Capacity may round up to power of 2
        assertThat(map.getCapacity()).isGreaterThanOrEqualTo(32);
    }

    @Test
    void testGetLoadFactor() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .loadFactor(0.5f)
                .build();
        assertThat(map.getLoadFactor()).isEqualTo(0.5f);
    }

    @Test
    void testGetValueBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .valueBasedEquality(false)
                .build();
        assertThat(map.getValueBasedEquality()).isFalse();
    }

    @Test
    void testGetFlattenDimensions() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();
        assertThat(map.getFlattenDimensions()).isTrue();
    }

    @Test
    void testGetCaseSensitive() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        assertThat(map.getCaseSensitive()).isFalse();
    }

    @Test
    void testLongSize() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.longSize()).isEqualTo(0L);
        map.putMultiKey("value", "a", "b");
        assertThat(map.longSize()).isEqualTo(1L);
    }

    // ========== Basic put/get ==========

    @Test
    void testPutGetSingleKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "key1");
        assertThat(map.getMultiKey("key1")).isEqualTo("value");
    }

    @Test
    void testPutGetTwoKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2");
        assertThat(map.getMultiKey("k1", "k2")).isEqualTo("value");
    }

    @Test
    void testPutGetThreeKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2", "k3");
        assertThat(map.getMultiKey("k1", "k2", "k3")).isEqualTo("value");
    }

    @Test
    void testPutGetFourKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2", "k3", "k4");
        assertThat(map.getMultiKey("k1", "k2", "k3", "k4")).isEqualTo("value");
    }

    @Test
    void testPutGetFiveKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2", "k3", "k4", "k5");
        assertThat(map.getMultiKey("k1", "k2", "k3", "k4", "k5")).isEqualTo("value");
    }

    // ========== Size / isEmpty / remove ==========

    @Test
    void testSize() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.size()).isEqualTo(0);
        map.putMultiKey("a", "k1");
        map.putMultiKey("b", "k2");
        assertThat(map.size()).isEqualTo(2);
    }

    @Test
    void testIsEmpty() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.isEmpty()).isTrue();
        map.putMultiKey("a", "k1");
        assertThat(map.isEmpty()).isFalse();
    }

    @Test
    void testRemoveMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2");
        assertThat(map.removeMultiKey("k1", "k2")).isEqualTo("value");
        assertThat(map.size()).isEqualTo(0);
    }

    @Test
    void testRemoveNonExistent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.removeMultiKey("k1", "k2")).isNull();
    }

    @Test
    void testClear() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("a", "k1");
        map.putMultiKey("b", "k2");
        map.clear();
        assertThat(map.size()).isEqualTo(0);
    }

    // ========== containsMultiKey ==========

    @Test
    void testContainsMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2");
        assertThat(map.containsMultiKey("k1", "k2")).isTrue();
        assertThat(map.containsMultiKey("k1", "k3")).isFalse();
    }

    // ========== Set as key (small set — brute force path) ==========

    @Test
    void testSmallSetAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<String> key = new HashSet<>(Arrays.asList("a", "b", "c"));
        map.put(key, "value");

        // Look up with equivalent set (different instance, different order)
        Set<String> lookup = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        assertThat(map.get(lookup)).isEqualTo("value");
    }

    @Test
    void testSmallSetAsKeyDifferentElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<String> key1 = new HashSet<>(Arrays.asList("a", "b"));
        map.put(key1, "value1");

        Set<String> key2 = new HashSet<>(Arrays.asList("a", "c"));
        assertThat(map.get(key2)).isNull();
    }

    @Test
    void testLargeSetAsKey() {
        // Large set — hits hash-bucket path (>6 elements)
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<String> key = new LinkedHashSet<>();
        for (int i = 0; i < 20; i++) {
            key.add("item" + i);
        }
        map.put(key, "value");

        // Look up with equivalent set in different order
        Set<String> lookup = new LinkedHashSet<>();
        for (int i = 19; i >= 0; i--) {
            lookup.add("item" + i);
        }
        assertThat(map.get(lookup)).isEqualTo("value");
    }

    // ========== Primitive array as key with non-RandomAccess collection lookup ==========

    @Test
    void testPrimitiveArrayKeyVsLinkedList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int[] arrayKey = {1, 2, 3};
        map.put(arrayKey, "value");

        // LinkedList is not RandomAccess
        LinkedList<Integer> linkedListKey = new LinkedList<>(Arrays.asList(1, 2, 3));
        assertThat(map.get(linkedListKey)).isEqualTo("value");
    }

    @Test
    void testLongArrayKeyVsLinkedList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        long[] arrayKey = {10L, 20L, 30L};
        map.put(arrayKey, "value");

        LinkedList<Long> linkedListKey = new LinkedList<>(Arrays.asList(10L, 20L, 30L));
        assertThat(map.get(linkedListKey)).isEqualTo("value");
    }

    @Test
    void testDoubleArrayKeyVsLinkedList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        double[] arrayKey = {1.5, 2.5, 3.5};
        map.put(arrayKey, "value");

        LinkedList<Double> linkedListKey = new LinkedList<>(Arrays.asList(1.5, 2.5, 3.5));
        assertThat(map.get(linkedListKey)).isEqualTo("value");
    }

    // ========== AtomicIntegerArray / AtomicLongArray / AtomicReferenceArray as keys ==========

    @Test
    void testAtomicIntegerArrayAsKey() {
        // Exercises flattenKey path for AtomicIntegerArray
        MultiKeyMap<String> map = new MultiKeyMap<>();
        AtomicIntegerArray key = new AtomicIntegerArray(new int[]{1, 2, 3});
        map.put(key, "value");
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    void testAtomicLongArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        AtomicLongArray key = new AtomicLongArray(new long[]{100L, 200L, 300L});
        map.put(key, "value");
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    void testAtomicReferenceArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        AtomicReferenceArray<String> key = new AtomicReferenceArray<>(new String[]{"a", "b", "c"});
        map.put(key, "value");
        assertThat(map.size()).isEqualTo(1);
    }

    // ========== toString ==========

    @Test
    void testToStringEmpty() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        String result = map.toString();
        assertThat(result).isNotNull();
    }

    @Test
    void testToStringWithEntries() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "k1", "k2");
        String result = map.toString();
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void testToStringWithArrayValue() {
        // Values containing arrays — exercise formatArrayValueForToString
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        map.putMultiKey(new int[]{1, 2, 3}, "k1");
        map.putMultiKey(new String[]{"a", "b"}, "k2");
        String result = map.toString();
        assertThat(result).isNotNull();
    }

    // ========== Value-based equality ==========

    @Test
    void testValueBasedEqualityNumbers() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .valueBasedEquality(true)
                .build();
        map.put(Integer.valueOf(42), "value");
        // Look up with Long — should match due to value-based equality
        assertThat(map.get(Long.valueOf(42L))).isEqualTo("value");
    }

    @Test
    void testIdentityBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .valueBasedEquality(false)
                .build();
        map.put(Integer.valueOf(42), "value");
        // Without value-based equality, Integer(42) != Long(42L)
        assertThat(map.get(Long.valueOf(42L))).isNull();
    }

    // ========== Case sensitivity ==========

    @Test
    void testCaseInsensitive() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        map.put("Hello", "value");
        assertThat(map.get("HELLO")).isEqualTo("value");
        assertThat(map.get("hello")).isEqualTo("value");
    }

    @Test
    void testCaseSensitive() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .caseSensitive(true)
                .build();
        map.put("Hello", "value");
        assertThat(map.get("HELLO")).isNull();
        assertThat(map.get("Hello")).isEqualTo("value");
    }

    // ========== putAll / putIfAbsent / compute ==========

    @Test
    void testPutAll() {
        MultiKeyMap<String> source = new MultiKeyMap<>();
        source.put("k1", "v1");
        source.put("k2", "v2");

        MultiKeyMap<String> dest = new MultiKeyMap<>();
        dest.putAll(source);
        assertThat(dest.size()).isEqualTo(2);
        assertThat(dest.get("k1")).isEqualTo("v1");
    }

    @Test
    void testPutIfAbsent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.putIfAbsent("k1", "v1")).isNull();
        assertThat(map.putIfAbsent("k1", "v2")).isEqualTo("v1"); // should not overwrite
        assertThat(map.get("k1")).isEqualTo("v1");
    }

    @Test
    void testComputeIfAbsent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        String result = map.computeIfAbsent("k1", k -> "computed");
        assertThat(result).isEqualTo("computed");
        assertThat(map.get("k1")).isEqualTo("computed");

        // Second call should not recompute
        String result2 = map.computeIfAbsent("k1", k -> "new value");
        assertThat(result2).isEqualTo("computed");
    }

    @Test
    void testComputeIfPresent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");

        String result = map.computeIfPresent("k1", (k, v) -> v + "-updated");
        assertThat(result).isEqualTo("v1-updated");
        assertThat(map.get("k1")).isEqualTo("v1-updated");

        // Key not present
        String result2 = map.computeIfPresent("k2", (k, v) -> "should not execute");
        assertThat(result2).isNull();
    }

    @Test
    void testCompute() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");

        String result = map.compute("k1", (k, v) -> (v == null) ? "new" : v + "-modified");
        assertThat(result).isEqualTo("v1-modified");

        String result2 = map.compute("k2", (k, v) -> (v == null) ? "new" : v + "-modified");
        assertThat(result2).isEqualTo("new");
    }

    @Test
    void testRemoveWithValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        assertThat(map.remove("k1", "v1")).isTrue();
        assertThat(map.size()).isEqualTo(0);
    }

    @Test
    void testRemoveWithValueMismatch() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        assertThat(map.remove("k1", "v2")).isFalse();
        assertThat(map.size()).isEqualTo(1);
    }

    // ========== Constructors ==========

    @Test
    void testConstructorDefault() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertThat(map.size()).isEqualTo(0);
    }

    @Test
    void testConstructorWithCapacity() {
        MultiKeyMap<String> map = new MultiKeyMap<>(64);
        assertThat(map.size()).isEqualTo(0);
        assertThat(map.getCapacity()).isGreaterThanOrEqualTo(64);
    }

    @Test
    void testConstructorWithCapacityAndLoadFactor() {
        MultiKeyMap<String> map = new MultiKeyMap<>(64, 0.75f);
        assertThat(map.size()).isEqualTo(0);
        assertThat(map.getLoadFactor()).isEqualTo(0.75f);
    }

    @Test
    void testCopyConstructor() {
        MultiKeyMap<String> source = new MultiKeyMap<>();
        source.put("k1", "v1");
        source.put("k2", "v2");

        MultiKeyMap<String> copy = new MultiKeyMap<>(source);
        assertThat(copy.size()).isEqualTo(2);
        assertThat(copy.get("k1")).isEqualTo("v1");
    }

    // ========== Entries / keys / values ==========

    @Test
    void testEntrySet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        assertThat(map.entrySet()).hasSize(2);
    }

    @Test
    void testValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        assertThat(map.values()).hasSize(2);
        assertThat(map.values()).contains("v1", "v2");
    }

    @Test
    void testKeySet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        assertThat(map.keySet()).hasSize(2);
    }

    // ========== Null values ==========

    @Test
    void testPutNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", null);
        assertThat(map.containsKey("k1")).isTrue();
        assertThat(map.get("k1")).isNull();
    }

    // ========== Overwrite existing key ==========

    @Test
    void testOverwriteExistingKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        String prev = map.put("k1", "v2");
        assertThat(prev).isEqualTo("v1");
        assertThat(map.get("k1")).isEqualTo("v2");
    }

    // ========== containsValue ==========

    @Test
    void testContainsValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("k1", "v1");
        assertThat(map.containsValue("v1")).isTrue();
        assertThat(map.containsValue("v2")).isFalse();
    }
}
