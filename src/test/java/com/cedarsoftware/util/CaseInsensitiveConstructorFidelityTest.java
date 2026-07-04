package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the constructor "backing fidelity" dispatch of {@link CaseInsensitiveSet#CaseInsensitiveSet(java.util.Collection)}
 * and {@link CaseInsensitiveMap#CaseInsensitiveMap(Map)}:
 *
 * <ul>
 *   <li>Copying a CaseInsensitiveSet/CaseInsensitiveMap replicates the source's inner map type
 *       (concurrent stays concurrent, sorted stays sorted) instead of collapsing to LinkedHashMap.</li>
 *   <li>{@code ConcurrentHashMap.newKeySet()} and {@code CopyOnWriteArraySet} sources produce
 *       concurrent inner maps (previously silently lost thread-safety).</li>
 *   <li>{@code HashSet} sources produce a HashMap inner map (mirrors the Map side's
 *       HashMap-to-HashMap registry entry); {@code LinkedHashSet} and other collections keep
 *       LinkedHashMap with encounter order.</li>
 *   <li>{@code IdentitySet} sources are rejected, mirroring the Map side's IdentityHashMap rejection.</li>
 *   <li>A SortedSet source's custom comparator is not carried over (comparator equality would
 *       break case-insensitive uniqueness) — natural case-insensitive order is used.</li>
 * </ul>
 */
class CaseInsensitiveConstructorFidelityTest {

    private static Class<?> innerMapClass(CaseInsensitiveSet<?> set) {
        return ((CaseInsensitiveMap<?, ?>) set.getBackingMap()).getWrappedMap().getClass();
    }

    // ---------------------------------------------------------------
    // CaseInsensitiveSet(Collection) dispatch
    // ---------------------------------------------------------------

    @Test
    void testHashSetSourceGetsHashMapInner() {
        Set<String> source = new HashSet<>(Arrays.asList("Alpha", "Beta"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(HashMap.class, innerMapClass(set));
        assertTrue(set.contains("ALPHA"));
        assertTrue(set.contains("beta"));
        assertEquals(2, set.size());
    }

    @Test
    void testHashSetSourceWithNullElementStillWorks() {
        Set<String> source = new HashSet<>(Arrays.asList("Alpha", null));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(HashMap.class, innerMapClass(set));
        assertTrue(set.contains(null));
        assertEquals(2, set.size());
    }

    @Test
    void testLinkedHashSetSourceKeepsLinkedHashMapAndOrder() {
        Set<String> source = new LinkedHashSet<>(Arrays.asList("Charlie", "Alpha", "Beta"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(LinkedHashMap.class, innerMapClass(set));
        Iterator<String> i = set.iterator();
        assertEquals("Charlie", i.next());
        assertEquals("Alpha", i.next());
        assertEquals("Beta", i.next());
    }

    @Test
    void testListSourceGetsLinkedHashMapAndKeepsEncounterOrder() {
        List<String> source = new ArrayList<>(Arrays.asList("Zulu", "Kilo", "Echo"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(LinkedHashMap.class, innerMapClass(set));
        Iterator<String> i = set.iterator();
        assertEquals("Zulu", i.next());
        assertEquals("Kilo", i.next());
        assertEquals("Echo", i.next());
    }

    @Test
    void testTreeSetSourceGetsTreeMap() {
        TreeSet<String> source = new TreeSet<>(Arrays.asList("beta", "Alpha"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(TreeMap.class, innerMapClass(set));
    }

    @Test
    void testSortedSetComparatorNotCarriedButStillCaseInsensitive() {
        // Reverse-order source: iteration would be [beta, Alpha]. The copy uses natural,
        // case-insensitive order — the comparator must not be carried over.
        TreeSet<String> source = new TreeSet<>(Comparator.reverseOrder());
        source.addAll(Arrays.asList("Alpha", "beta"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        Iterator<String> i = set.iterator();
        assertEquals("Alpha", i.next());
        assertEquals("beta", i.next());
        assertFalse(set.add("ALPHA"), "case-insensitive uniqueness must hold in the sorted copy");
    }

    @Test
    void testConcurrentKeySetViewSourceGetsConcurrentHashMap() {
        Set<String> source = ConcurrentHashMap.newKeySet();
        source.addAll(Arrays.asList("Alpha", "Beta"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentHashMap.class, innerMapClass(set));
        assertTrue(set.contains("alpha"));
    }

    @Test
    void testCopyOnWriteArraySetSourceGetsNullSafeConcurrentInner() {
        CopyOnWriteArraySet<String> source = new CopyOnWriteArraySet<>(Arrays.asList("Alpha", null));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentHashMapNullSafe.class, innerMapClass(set));
        assertTrue(set.contains("ALPHA"));
        assertTrue(set.contains(null), "CopyOnWriteArraySet permits null — the copy must too");
    }

    @Test
    void testConcurrentSetSourceGetsNullSafeConcurrentInner() {
        ConcurrentSet<String> source = new ConcurrentSet<>();
        source.add("Alpha");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentHashMapNullSafe.class, innerMapClass(set));
    }

    @Test
    void testConcurrentSkipListSetSourceGetsSkipListMapInner() {
        ConcurrentSkipListSet<String> source = new ConcurrentSkipListSet<>(Arrays.asList("beta", "Alpha"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentSkipListMap.class, innerMapClass(set));
    }

    @Test
    void testConcurrentNavigableSetNullSafeSourceGetsNavigableNullSafeInner() {
        ConcurrentNavigableSetNullSafe<String> source = new ConcurrentNavigableSetNullSafe<>();
        source.add("Alpha");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentNavigableMapNullSafe.class, innerMapClass(set));
    }

    @Test
    void testCaseInsensitiveSetCopyReplicatesConcurrentInner() {
        CaseInsensitiveSet<String> source =
                new CaseInsensitiveSet<>(Collections.<String>emptySet(), new ConcurrentHashMap<>());
        source.add("Alpha");
        source.add("Beta");
        CaseInsensitiveSet<String> copy = new CaseInsensitiveSet<>(source);
        assertEquals(ConcurrentHashMap.class, innerMapClass(copy),
                "copying a concurrent-backed CaseInsensitiveSet must stay concurrent");
        assertTrue(copy.contains("ALPHA"));
        assertEquals(2, copy.size());
    }

    @Test
    void testCaseInsensitiveSetCopyReplicatesSortedInner() {
        CaseInsensitiveSet<String> source =
                new CaseInsensitiveSet<>(Collections.<String>emptySet(), new TreeMap<>());
        source.add("beta");
        source.add("Alpha");
        CaseInsensitiveSet<String> copy = new CaseInsensitiveSet<>(source);
        assertEquals(TreeMap.class, innerMapClass(copy),
                "copying a tree-backed CaseInsensitiveSet must stay sorted");
        Iterator<String> i = copy.iterator();
        assertEquals("Alpha", i.next());
        assertEquals("beta", i.next());
    }

    @Test
    void testCaseInsensitiveSetDefaultCopyKeepsLinkedHashMapAndOrder() {
        CaseInsensitiveSet<String> source = new CaseInsensitiveSet<>();
        source.add("Charlie");
        source.add("Alpha");
        CaseInsensitiveSet<String> copy = new CaseInsensitiveSet<>(source);
        assertEquals(LinkedHashMap.class, innerMapClass(copy));
        Iterator<String> i = copy.iterator();
        assertEquals("Charlie", i.next());
        assertEquals("Alpha", i.next());
    }

    @Test
    void testIdentitySetSourceRejected() {
        IdentitySet<String> source = new IdentitySet<>();
        source.add("Alpha");
        assertThrows(IllegalArgumentException.class, () -> new CaseInsensitiveSet<>(source));
    }

    // ---------------------------------------------------------------
    // CaseInsensitiveMap(Map) copy fidelity
    // ---------------------------------------------------------------

    @Test
    void testCaseInsensitiveMapCopyReplicatesConcurrentInner() {
        CaseInsensitiveMap<String, String> source =
                new CaseInsensitiveMap<>(Collections.<String, String>emptyMap(), new ConcurrentHashMap<>());
        source.put("Alpha", "1");
        source.put("Beta", "2");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(source);
        assertEquals(ConcurrentHashMap.class, copy.getWrappedMap().getClass(),
                "copying a concurrent-backed CaseInsensitiveMap must stay concurrent");
        assertEquals("1", copy.get("ALPHA"));
        assertEquals("2", copy.get("beta"));
    }

    @Test
    void testCaseInsensitiveMapCopyReplicatesSortedInner() {
        CaseInsensitiveMap<String, String> source =
                new CaseInsensitiveMap<>(Collections.<String, String>emptyMap(), new TreeMap<>());
        source.put("beta", "2");
        source.put("Alpha", "1");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(source);
        assertEquals(TreeMap.class, copy.getWrappedMap().getClass(),
                "copying a tree-backed CaseInsensitiveMap must stay sorted");
        Iterator<String> keys = copy.keySet().iterator();
        assertEquals("Alpha", keys.next());
        assertEquals("beta", keys.next());
    }

    @Test
    void testCaseInsensitiveMapDefaultCopyKeepsLinkedHashMap() {
        CaseInsensitiveMap<String, String> source = new CaseInsensitiveMap<>();
        source.put("Charlie", "3");
        CaseInsensitiveMap<String, String> copy = new CaseInsensitiveMap<>(source);
        assertEquals(LinkedHashMap.class, copy.getWrappedMap().getClass());
        assertEquals("3", copy.get("CHARLIE"));
    }

    @Test
    void testPlainHashMapSourceStillGetsHashMapInner() {
        Map<String, String> source = new HashMap<>();
        source.put("Alpha", "1");
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
        assertEquals(HashMap.class, map.getWrappedMap().getClass());
        assertEquals("1", map.get("alpha"));
    }
}
