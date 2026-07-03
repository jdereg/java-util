package com.cedarsoftware.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the 4.106.0 review fix to {@link CollectionUtilities#deepCopyContainers(Object)}.
 * <p>
 * Previously, element copies were inserted into content-sensitive parent collections
 * (LinkedHashSet, TreeSet, PriorityQueue) while still EMPTY — they are only filled later by
 * the work queue. Two distinct-but-unfilled containers compare equal, so sets silently
 * deduplicated them (data loss), comparators saw empty shells (mis-ordering, or exceptions
 * from content-inspecting comparators), and heaps were built on wrong comparisons. The fix
 * buffers elements for content-sensitive targets and installs them after the graph is fully
 * copied, children before parents.
 */
class CollectionUtilitiesReviewFixesTest {

    @Test
    void testSetOfDistinctListsKeepsAllElements() {
        Set<List<String>> source = new LinkedHashSet<>();
        source.add(new ArrayList<>(Arrays.asList("a")));
        source.add(new ArrayList<>(Arrays.asList("b")));

        Set<List<String>> copy = CollectionUtilities.deepCopyContainers(source);

        assertEquals(2, copy.size(), "Distinct lists must not be deduplicated as empty shells");
        assertEquals(source, copy, "Copy must be content-equal to the source");
        for (List<String> copied : copy) {
            for (List<String> original : source) {
                assertNotSame(original, copied, "Elements must be deep-copied, not shared");
            }
        }
    }

    @Test
    void testTreeSetWithContentComparatorKeepsAllElementsSorted() {
        TreeSet<List<Integer>> source = new TreeSet<>(Comparator.comparingInt(l -> l.isEmpty() ? -1 : l.get(0)));
        source.add(new ArrayList<>(Arrays.asList(2)));
        source.add(new ArrayList<>(Arrays.asList(1)));
        source.add(new ArrayList<>(Arrays.asList(3)));

        Set<List<Integer>> copy = CollectionUtilities.deepCopyContainers(source);

        assertEquals(3, copy.size());
        Iterator<List<Integer>> it = copy.iterator();
        assertEquals(Integer.valueOf(1), it.next().get(0), "TreeSet copy must sort by final content");
        assertEquals(Integer.valueOf(2), it.next().get(0));
        assertEquals(Integer.valueOf(3), it.next().get(0));
    }

    @Test
    void testTreeSetComparatorThatInspectsContentDoesNotSeeEmptyShells() {
        // This comparator throws on an empty list — it would have blown up (or mis-sorted)
        // when elements were inserted before being filled.
        TreeSet<List<Integer>> source = new TreeSet<>(Comparator.comparingInt(l -> l.get(0)));
        source.add(new ArrayList<>(Arrays.asList(10)));
        source.add(new ArrayList<>(Arrays.asList(5)));

        Set<List<Integer>> copy = CollectionUtilities.deepCopyContainers(source);

        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(5), copy.iterator().next().get(0));
    }

    @Test
    void testPriorityQueueHeapBuiltOnFinalContent() {
        PriorityQueue<List<Integer>> source = new PriorityQueue<>(Comparator.comparingInt(l -> l.get(0)));
        source.add(new ArrayList<>(Arrays.asList(3)));
        source.add(new ArrayList<>(Arrays.asList(1)));
        source.add(new ArrayList<>(Arrays.asList(2)));

        PriorityQueue<List<Integer>> copy = CollectionUtilities.deepCopyContainers(source);

        assertEquals(3, copy.size());
        assertEquals(Integer.valueOf(1), copy.poll().get(0));
        assertEquals(Integer.valueOf(2), copy.poll().get(0));
        assertEquals(Integer.valueOf(3), copy.poll().get(0));
    }

    @Test
    void testNestedSetsFillChildrenBeforeParents() {
        Set<Set<List<String>>> outer = new LinkedHashSet<>();
        Set<List<String>> innerA = new LinkedHashSet<>();
        innerA.add(new ArrayList<>(Arrays.asList("a")));
        Set<List<String>> innerB = new LinkedHashSet<>();
        innerB.add(new ArrayList<>(Arrays.asList("b")));
        outer.add(innerA);
        outer.add(innerB);

        Set<Set<List<String>>> copy = CollectionUtilities.deepCopyContainers(outer);

        assertEquals(2, copy.size(), "Inner sets must be complete before the outer set hashes them");
        assertEquals(outer, copy);
    }

    @Test
    void testSelfReferentialSetStillCopies() {
        Set<Object> source = new LinkedHashSet<>();
        source.add(source);

        Set<Object> copy = CollectionUtilities.deepCopyContainers(source);

        assertEquals(1, copy.size());
        assertSame(copy, copy.iterator().next(), "Cycle must point at the copy, not the source");
    }

    @Test
    void testMixedCycleThroughSetAndList() {
        Set<Object> set = new LinkedHashSet<>();
        List<Object> list = new ArrayList<>();
        set.add(list);
        list.add(set);

        Set<Object> copy = CollectionUtilities.deepCopyContainers(set);

        assertEquals(1, copy.size());
        List<?> copiedList = (List<?>) copy.iterator().next();
        assertNotSame(list, copiedList);
        assertSame(copy, copiedList.get(0), "List copy must reference the set copy (cycle preserved)");
    }

    @Test
    void testEnumSetStillFilledDirectly() {
        EnumSet<java.time.DayOfWeek> source = EnumSet.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY);

        Set<java.time.DayOfWeek> copy = CollectionUtilities.deepCopyContainers(source);

        assertInstanceOf(EnumSet.class, copy, "EnumSet type must be preserved");
        assertNotSame(source, copy);
        assertEquals(source, copy);
    }

    @Test
    void testSetOfDeduplicatesSilentlyByDesign() {
        // Deliberate divergence from JDK Set.of (which throws IllegalArgumentException on
        // duplicates): setOf treats a collision as normal set semantics — first occurrence
        // wins, first-occurrence insertion order is preserved, no exception. Confirmed as
        // the intended contract 2026-07-03; this test pins it.
        Set<String> set = CollectionUtilities.setOf("A", "B", "A", "C", "B");

        assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B", "C")), set);
        Iterator<String> it = set.iterator();
        assertEquals("A", it.next());
        assertEquals("B", it.next());
        assertEquals("C", it.next());

        // The library's other set factory follows the same lenient contract.
        assertEquals(1, ClassValueSet.of(String.class, String.class).size());
    }

    @Test
    void testPositionalContainersUnchanged() {
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("first", "middle", "last"));
        Deque<String> dequeCopy = CollectionUtilities.deepCopyContainers(deque);
        assertEquals("first", dequeCopy.peekFirst());
        assertEquals("last", dequeCopy.peekLast());
        assertEquals(3, dequeCopy.size());

        Object[] array = { new ArrayList<>(Arrays.asList("x")), "berry" };
        Object[] arrayCopy = CollectionUtilities.deepCopyContainers(array);
        assertNotSame(array, arrayCopy);
        assertNotSame(array[0], arrayCopy[0]);
        assertEquals(array[0], arrayCopy[0]);
        assertSame(array[1], arrayCopy[1], "Berries keep their reference");

        Set<String> berries = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        Set<String> berriesCopy = CollectionUtilities.deepCopyContainers(berries);
        assertEquals(berries, berriesCopy);
        assertTrue(berriesCopy.iterator().next().equals("x"), "Insertion order preserved");
    }
}
