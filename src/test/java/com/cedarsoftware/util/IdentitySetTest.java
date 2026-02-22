package com.cedarsoftware.util;

import java.time.Duration;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for IdentitySet - a high-performance Set using identity comparison.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Copyright (c) Cedar Software LLC
 *         Licensed under the Apache License, Version 2.0
 */
class IdentitySetTest {
    private static final Logger LOG = Logger.getLogger(IdentitySetTest.class.getName());
    private static final Predicate<Field> INCLUDE_ALL_FIELDS = field -> true;
    private static final Map<String, Field> IDENTITY_SET_FIELDS =
            ReflectionUtils.getAllDeclaredFieldsMap(IdentitySet.class, INCLUDE_ALL_FIELDS);
    private static final Field ELEMENTS_FIELD = requireField("elements");
    private static final Field MASK_FIELD = requireField("mask");
    private static final Field DELETED_FIELD = requireField("DELETED");


    @Test
    void testBasicAddAndContains() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();

        assertTrue(set.add(obj1));
        assertTrue(set.contains(obj1));
        assertFalse(set.contains(obj2));
        assertEquals(1, set.size());

        assertTrue(set.add(obj2));
        assertTrue(set.contains(obj1));
        assertTrue(set.contains(obj2));
        assertEquals(2, set.size());
    }

    @Test
    void testAddDuplicate() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj = new Object();

        assertTrue(set.add(obj));
        assertEquals(1, set.size());

        // Adding same object again should return false
        assertFalse(set.add(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testIdentityVsEquals() {
        IdentitySet<String> set = new IdentitySet<>();

        // Two strings with same content but different identity
        String s1 = new String("test");
        String s2 = new String("test");

        // Verify they are equal but not identical
        assertEquals(s1, s2);
        assertNotSame(s1, s2);

        // Both should be added because identity is different
        assertTrue(set.add(s1));
        assertTrue(set.add(s2));
        assertEquals(2, set.size());

        // Only the identical object should be found
        assertTrue(set.contains(s1));
        assertTrue(set.contains(s2));

        // A third string with same content should not be found
        String s3 = new String("test");
        assertFalse(set.contains(s3));
    }

    @Test
    void testRemove() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        set.add(obj1);
        set.add(obj2);
        set.add(obj3);
        assertEquals(3, set.size());

        assertTrue(set.remove(obj2));
        assertEquals(2, set.size());
        assertFalse(set.contains(obj2));
        assertTrue(set.contains(obj1));
        assertTrue(set.contains(obj3));

        // Removing non-existent object
        assertFalse(set.remove(obj2));
        assertFalse(set.remove(new Object()));
    }

    @Test
    void testRemoveAndReAdd() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));

        set.remove(obj);
        assertFalse(set.contains(obj));

        // Should be able to add again after removal
        assertTrue(set.add(obj));
        assertTrue(set.contains(obj));
    }

    @Test
    void testClear() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();

        set.add(obj1);
        set.add(obj2);
        assertEquals(2, set.size());

        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains(obj1));
        assertFalse(set.contains(obj2));
    }

    @Test
    void testIsEmpty() {
        IdentitySet<Object> set = new IdentitySet<>();
        assertTrue(set.isEmpty());

        Object obj = new Object();
        set.add(obj);
        assertFalse(set.isEmpty());

        set.remove(obj);
        assertTrue(set.isEmpty());
    }

    @Test
    void testNullElement() {
        IdentitySet<Object> set = new IdentitySet<>();

        assertThrows(NullPointerException.class, () -> set.add(null));
        assertFalse(set.contains(null));
        assertFalse(set.remove(null));
    }

    @Test
    void testResize() {
        // Start with small capacity and add many elements to trigger resize
        IdentitySet<Object> set = new IdentitySet<>(4);
        Object[] objects = new Object[100];

        for (int i = 0; i < 100; i++) {
            objects[i] = new Object();
            assertTrue(set.add(objects[i]));
        }

        assertEquals(100, set.size());

        // Verify all objects are still accessible after multiple resizes
        for (int i = 0; i < 100; i++) {
            assertTrue(set.contains(objects[i]));
        }
    }

    @Test
    void testCustomInitialCapacity() {
        IdentitySet<Object> set = new IdentitySet<>(1000);
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testMixedOperations() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object[] objects = new Object[50];

        // Add all
        for (int i = 0; i < 50; i++) {
            objects[i] = new Object();
            set.add(objects[i]);
        }
        assertEquals(50, set.size());

        // Remove even indices
        for (int i = 0; i < 50; i += 2) {
            assertTrue(set.remove(objects[i]));
        }
        assertEquals(25, set.size());

        // Verify odd indices still present, even indices gone
        for (int i = 0; i < 50; i++) {
            if (i % 2 == 0) {
                assertFalse(set.contains(objects[i]));
            } else {
                assertTrue(set.contains(objects[i]));
            }
        }

        // Re-add some removed elements
        for (int i = 0; i < 10; i += 2) {
            assertTrue(set.add(objects[i]));
        }
        assertEquals(30, set.size());
    }

    @Test
    void testProbeChainAfterRemoval() {
        // This tests that removal with DELETED sentinel doesn't break probe chains
        IdentitySet<Object> set = new IdentitySet<>(4);

        // Add objects that might hash to same bucket
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        set.add(obj1);
        set.add(obj2);
        set.add(obj3);

        // Remove middle element
        set.remove(obj2);

        // obj3 should still be findable (probe chain not broken)
        assertTrue(set.contains(obj1));
        assertFalse(set.contains(obj2));
        assertTrue(set.contains(obj3));
    }

    @Test
    void testWithInternedStrings() {
        IdentitySet<String> set = new IdentitySet<>();

        String s1 = "interned";  // Interned string
        String s2 = "interned";  // Same interned string

        // These should be the same object (interned)
        assertSame(s1, s2);

        assertTrue(set.add(s1));
        assertFalse(set.add(s2));  // Same object, already present
        assertEquals(1, set.size());
    }

    @Test
    void testWithIntegerCaching() {
        IdentitySet<Integer> set = new IdentitySet<>();

        // Small integers are cached by JVM
        Integer i1 = 100;
        Integer i2 = 100;
        assertSame(i1, i2);  // Same cached object

        assertTrue(set.add(i1));
        assertFalse(set.add(i2));  // Same object
        assertEquals(1, set.size());

        // Large integers are not cached
        Integer i3 = 1000;
        Integer i4 = 1000;
        assertNotSame(i3, i4);  // Different objects

        assertTrue(set.add(i3));
        assertTrue(set.add(i4));  // Different object
        assertEquals(3, set.size());
    }

    // ============== Tests for Set interface compliance ==============

    @Test
    void testIterator() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }

        assertEquals(3, iterated.size());
        assertTrue(iterated.contains(s1));
        assertTrue(iterated.contains(s2));
        assertTrue(iterated.contains(s3));
    }

    @Test
    void testIteratorRemove() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s == s2) {
                it.remove();
            }
        }

        assertEquals(2, set.size());
        assertTrue(set.contains(s1));
        assertFalse(set.contains(s2));
        assertTrue(set.contains(s3));
    }

    @Test
    void testIteratorEmptySet() {
        IdentitySet<Object> set = new IdentitySet<>();
        Iterator<Object> it = set.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testIteratorRemoveWithoutNext() {
        IdentitySet<Object> set = new IdentitySet<>();
        set.add(new Object());

        Iterator<Object> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void testIteratorDoubleRemove() {
        IdentitySet<Object> set = new IdentitySet<>();
        set.add(new Object());

        Iterator<Object> it = set.iterator();
        it.next();
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void testConstructorFromCollection() {
        List<String> list = Arrays.asList(
                new String("a"),
                new String("b"),
                new String("c")
        );

        IdentitySet<String> set = new IdentitySet<>(list);

        assertEquals(3, set.size());
        for (String s : list) {
            assertTrue(set.contains(s));
        }
    }

    @Test
    void testAddAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.addAll(Arrays.asList(s1, s2));

        assertEquals(2, set.size());
        assertTrue(set.contains(s1));
        assertTrue(set.contains(s2));
    }

    @Test
    void testRemoveAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        set.removeAll(Arrays.asList(s1, s3));

        assertEquals(1, set.size());
        assertFalse(set.contains(s1));
        assertTrue(set.contains(s2));
        assertFalse(set.contains(s3));
    }

    @Test
    void testRetainAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        set.retainAll(Arrays.asList(s2));

        assertEquals(1, set.size());
        assertFalse(set.contains(s1));
        assertTrue(set.contains(s2));
        assertFalse(set.contains(s3));
    }

    @Test
    void testContainsAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        assertTrue(set.containsAll(Arrays.asList(s1, s2)));
        assertTrue(set.containsAll(Arrays.asList(s1, s2, s3)));
        assertFalse(set.containsAll(Arrays.asList(s1, new String("d"))));
    }

    @Test
    void testToArray() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.add(s1);
        set.add(s2);

        Object[] arr = set.toArray();
        assertEquals(2, arr.length);

        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains(s1));
        assertTrue(list.contains(s2));
    }

    @Test
    void testToArrayTyped() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.add(s1);
        set.add(s2);

        String[] arr = set.toArray(new String[0]);
        assertEquals(2, arr.length);

        List<String> list = Arrays.asList(arr);
        assertTrue(list.contains(s1));
        assertTrue(list.contains(s2));
    }

    @Test
    void testAsSetInterface() {
        // Verify it can be used as Set<T>
        Set<Object> set = new IdentitySet<>();
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testWithClassType() {
        // Common use case: tracking visited classes
        Set<Class<?>> visited = new IdentitySet<>();

        visited.add(String.class);
        visited.add(Integer.class);
        visited.add(Object.class);

        assertTrue(visited.contains(String.class));
        assertTrue(visited.contains(Integer.class));
        assertFalse(visited.contains(Long.class));
        assertEquals(3, visited.size());
    }

    // ============== Bug regression tests ==============

    @Test
    void testAddAfterRemoveDoesNotDuplicate() {
        // Bug: addInternal inserted into a DELETED slot without probing further,
        // creating a duplicate entry when the element existed later in the chain.
        IdentitySet<Object> set = new IdentitySet<>(16);
        Object[] objs = new Object[100];
        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Object();
            set.add(objs[i]);
        }

        // Remove half the elements to create DELETED tombstones
        for (int i = 0; i < objs.length; i += 2) {
            assertTrue(set.remove(objs[i]));
        }
        assertEquals(50, set.size());

        // Re-add the surviving elements — should all return false (already present)
        for (int i = 1; i < objs.length; i += 2) {
            assertFalse(set.add(objs[i]), "add() returned true for element already in set");
        }
        assertEquals(50, set.size());

        // Verify remove followed by contains is consistent
        for (int i = 1; i < objs.length; i += 2) {
            assertTrue(set.remove(objs[i]));
            assertFalse(set.contains(objs[i]), "contains() returned true after remove()");
        }
        assertEquals(0, set.size());
    }

    @Test
    void testAddReturnsCorrectBooleanAfterRemoval() {
        // Verifies the specific scenario: add A, add B (same bucket), remove A, add B → false
        IdentitySet<Object> set = new IdentitySet<>(4);
        Object a = new Object();
        Object b = new Object();

        set.add(a);
        set.add(b);
        assertEquals(2, set.size());

        set.remove(a);
        assertEquals(1, set.size());

        // b is still in the set — add must return false and size must stay 1
        assertFalse(set.add(b));
        assertEquals(1, set.size());

        // Now remove b and verify it's fully gone
        assertTrue(set.remove(b));
        assertEquals(0, set.size());
        assertFalse(set.contains(b));
    }

    @Test
    void testEdgeCaseInitialCapacities() {
        // Zero and negative values should not cause issues (clamped to minimum)
        IdentitySet<Object> set0 = new IdentitySet<>(0);
        assertTrue(set0.isEmpty());
        set0.add(new Object());
        assertEquals(1, set0.size());

        IdentitySet<Object> setNeg = new IdentitySet<>(-5);
        assertTrue(setNeg.isEmpty());
        setNeg.add(new Object());
        assertEquals(1, setNeg.size());
    }

    @Test
    @Timeout(5)
    void testLargeInitialCapacityDoesNotInfiniteLoop() {
        // Bug: initialCapacity > 2^30 caused int overflow in the power-of-2 rounding loop,
        // leading to an infinite loop. After fix, capacity is capped at 2^30.
        // The allocation may OOM on constrained JVMs — that's acceptable.
        // The critical thing is the constructor does not hang.
        try {
            IdentitySet<Object> set = new IdentitySet<>(Integer.MAX_VALUE);
            assertTrue(set.isEmpty());
        } catch (OutOfMemoryError e) {
            // Expected on most JVMs — proving the loop terminates is what matters
        }
    }

    @Test
    @Timeout(5)
    void testAllTombstonesMissOperationsDoNotLoop() {
        IdentitySet<Object> set = new IdentitySet<>(2);
        saturateTwoSlotTableWithTombstones(set);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(set.contains(new Object())));
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(set.remove(new Object())));
    }

    @Test
    @Timeout(5)
    void testAllTombstonesAddDoesNotLoop() {
        IdentitySet<Object> set = new IdentitySet<>(2);
        saturateTwoSlotTableWithTombstones(set);

        Object added = new Object();
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertTrue(set.add(added)));
        assertTrue(set.contains(added));
    }

    @Test
    void testConstructorWithCustomLoadFactor() {
        IdentitySet<Object> set = new IdentitySet<>(16, 0.75f);
        Object value = new Object();
        assertTrue(set.add(value));
        assertTrue(set.contains(value));
    }

    @Test
    void testConstructorWithInvalidLoadFactor() {
        assertThrows(IllegalArgumentException.class, () -> new IdentitySet<>(16, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new IdentitySet<>(16, -0.1f));
        assertThrows(IllegalArgumentException.class, () -> new IdentitySet<>(16, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new IdentitySet<>(16, Float.NaN));
    }

    @Test
    @Timeout(120)
    void testProbeStudyBoundaryMatrixAcrossLoadFactors() {
        final int[] boundaries = {1_024, 16_384, 65_536, 131_072}; // max at 128K
        final int[] sizes = buildBoundarySizes(boundaries, 1);
        final float[] loadFactors = {0.5f, 2.0f / 3.0f, 0.75f};
        final StringBuilder report = new StringBuilder("IdentitySet boundary probe study\n");
        report.append("| size | loadFactor | tableSize | occupancy | successAvg | successMax | missAvg | missMax |\n");
        report.append("|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        for (int size : sizes) {
            List<Object> sample = buildMixedRandomSample(size, 919_101L + size);
            for (float loadFactor : loadFactors) {
                IdentitySet<Object> set = new IdentitySet<>(16, loadFactor);
                for (Object value : sample) {
                    assertTrue(set.add(value));
                }
                assertEquals(size, set.size());

                ProbeStats success = measureSuccessfulProbeStats(set);
                ProbeStats miss = measureMissProbeStats(set, 2_000, new Random(771_001L + size));
                int tableSize = getElementsArray(set).length;
                double occupancy = ((double) set.size()) / tableSize;

                report.append(String.format(
                        "| %d | %.4f | %d | %.4f | %.3f | %d | %.3f | %d |%n",
                        size, loadFactor, tableSize, occupancy, success.averageProbes, success.maxProbes,
                        miss.averageProbes, miss.maxProbes));
            }
        }

        logReport(report.toString());
    }

    private static void saturateTwoSlotTableWithTombstones(IdentitySet<Object> set) {
        Object bucket0 = findObjectForBucketLowBit(0);
        Object bucket1 = findObjectForBucketLowBit(1);

        assertTrue(set.add(bucket0));
        assertTrue(set.remove(bucket0));
        assertTrue(set.add(bucket1));
        assertTrue(set.remove(bucket1));
    }

    private static Object findObjectForBucketLowBit(int bucket) {
        for (int i = 0; i < 100_000; i++) {
            Object candidate = new Object();
            if ((System.identityHashCode(candidate) & 1) == bucket) {
                return candidate;
            }
        }
        throw new AssertionError("Unable to find candidate for bucket " + bucket);
    }

    public static String getRandomString(Random random, int minLen, int maxLen) {
        int length = minLen + random.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int selector = random.nextInt(62);
            if (selector < 10) {
                sb.append((char) ('0' + selector));
            } else if (selector < 36) {
                sb.append((char) ('A' + selector - 10));
            } else {
                sb.append((char) ('a' + selector - 36));
            }
        }
        return sb.toString();
    }

    private static List<Object> buildMixedRandomSample(int size, long seed) {
        Random random = new Random(seed);
        List<Object> sample = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if ((i & 1) == 0) {
                sample.add(random.nextInt());
            } else {
                sample.add(getRandomString(random, 8, 24));
            }
        }
        return sample;
    }

    private static int[] buildBoundarySizes(int[] boundaries, int delta) {
        List<Integer> sizes = new ArrayList<>();
        for (int boundary : boundaries) {
            for (int i = -delta; i <= delta; i++) {
                int size = boundary + i;
                if (size > 0) {
                    sizes.add(size);
                }
            }
        }
        int[] matrix = new int[sizes.size()];
        for (int i = 0; i < sizes.size(); i++) {
            matrix[i] = sizes.get(i);
        }
        return matrix;
    }

    private static void logReport(String report) {
        String[] lines = report.split("\\R");
        for (String line : lines) {
            if (!line.isEmpty()) {
                LOG.info(line);
            }
        }
    }

    private static ProbeStats measureSuccessfulProbeStats(IdentitySet<Object> set) {
        Object[] elements = getElementsArray(set);
        int mask = getMask(set);
        Object deleted = getDeletedSentinel();
        long total = 0L;
        int max = 0;
        int samples = 0;
        for (Object element : elements) {
            if (element == null || element == deleted) {
                continue;
            }
            int probes = countLookupProbes(elements, mask, element);
            total += probes;
            max = Math.max(max, probes);
            samples++;
        }
        return new ProbeStats(samples == 0 ? 0.0 : (double) total / samples, max);
    }

    private static ProbeStats measureMissProbeStats(IdentitySet<Object> set, int sampleCount, Random random) {
        Object[] elements = getElementsArray(set);
        int mask = getMask(set);
        long total = 0L;
        int max = 0;
        for (int i = 0; i < sampleCount; i++) {
            Object probe = (i & 1) == 0 ? random.nextLong() : getRandomString(random, 10, 30);
            int probes = countLookupProbes(elements, mask, probe);
            total += probes;
            max = Math.max(max, probes);
        }
        return new ProbeStats((double) total / sampleCount, max);
    }

    private static int countLookupProbes(Object[] elements, int mask, Object key) {
        int index = System.identityHashCode(key) & mask;
        for (int probes = 1; probes <= elements.length; probes++) {
            Object existing = elements[index];
            if (existing == null || existing == key) {
                return probes;
            }
            index = (index + 1) & mask;
        }
        return elements.length;
    }

    private static Field requireField(String name) {
        Field field = IDENTITY_SET_FIELDS.get(name);
        if (field == null) {
            throw new AssertionError("Expected field not found: " + name);
        }
        return field;
    }

    private static Object[] getElementsArray(IdentitySet<Object> set) {
        try {
            return (Object[]) ELEMENTS_FIELD.get(set);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static int getMask(IdentitySet<Object> set) {
        try {
            return MASK_FIELD.getInt(set);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Object getDeletedSentinel() {
        try {
            return DELETED_FIELD.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static final class ProbeStats {
        private final double averageProbes;
        private final int maxProbes;

        private ProbeStats(double averageProbes, int maxProbes) {
            this.averageProbes = averageProbes;
            this.maxProbes = maxProbes;
        }
    }
}
