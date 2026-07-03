package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the 4.106.0 review fixes to CompactMap:
 * <ol>
 *   <li>Builder/newMap no longer require javax.tools.JavaCompiler — the bytecode-template
 *       mechanism works on JRE-only runtimes; the guards were vestigial from the old
 *       javac-based implementation and contradicted the class documentation.</li>
 *   <li>removeFromMap judged key presence via containsKey AFTER remove(), so removing a
 *       null-valued key skipped the MAP→array/EMPTY downsize transitions, leaving the map
 *       permanently stuck in MAP state (never compacting again).</li>
 *   <li>keySet().retainAll() put null sentinel values into getNewMap(), which throws NPE
 *       for null-value-rejecting backings such as ConcurrentHashMap.</li>
 *   <li>hashCode()/equals() were state-dependent for case-insensitive sorted maps (raw
 *       TreeMap + CI comparator backing hashes/compares keys case-sensitively): two equal
 *       maps could disagree on hashCode (contract violation), and equals against a
 *       case-variant map flipped between states. MAP-state hashCode also recursed
 *       (StackOverflowError) on self-referential values that other states guard with
 *       sentinel constants.</li>
 * </ol>
 */
class CompactMapReviewFixesTest {

    /** Legacy subclass with a small compact size to make state transitions easy to reach. */
    static class SmallMap<K, V> extends CompactMap<K, V> {
        protected int compactSize() { return 3; }
    }

    // --- Fix 1: JRE support ---

    @Test
    void testBuilderWorksWithoutJavaCompiler() {
        System.setProperty("java.util.force.jre", "true");
        try {
            assertFalse(ReflectionUtils.isJavaCompilerAvailable(), "Simulated JRE must be in effect");
            CompactMap<String, Object> map = assertDoesNotThrow(() ->
                    CompactMap.<String, Object>builder().compactSize(10).insertionOrder().build(),
                    "Bytecode-template specialization requires no compiler and must work on a JRE");
            map.put("a", 1);
            map.put("b", 2);
            assertEquals(2, map.size());
            assertEquals(1, map.get("a"));
        } finally {
            System.clearProperty("java.util.force.jre");
        }
    }

    // --- Fix 2: null-valued key removal must not skip downsize transitions ---

    @Test
    void testRemoveNullValuedKeyAtBoundaryTransitionsToArray() {
        SmallMap<String, Object> map = new SmallMap<>();
        map.put("a", 1);
        map.put("b", null);
        map.put("c", 3);
        map.put("d", 4);   // 4 entries -> MAP state (compactSize 3)
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        assertNull(map.remove("b"), "Old value of a null-valued mapping is null");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(),
                "Reaching compactSize must transition back to ARRAY even when the removed value was null");
        assertEquals(1, map.get("a"));
        assertEquals(3, map.get("c"));
        assertEquals(4, map.get("d"));
    }

    @Test
    void testDrainEndingOnNullValuedKeyReachesEmptyState() {
        SmallMap<String, Object> map = new SmallMap<>();
        map.put("a", 1);
        map.put("b", null);
        map.put("c", 3);
        map.put("d", 4);

        // Remove the null-valued key FIRST (while in MAP state), then drain.
        map.remove("b");
        map.remove("a");
        map.remove("c");
        map.remove("d");
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    void testRemoveAbsentKeyInMapStateIsNoOp() {
        SmallMap<String, Object> map = new SmallMap<>();
        for (int i = 0; i < 4; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertNull(map.remove("missing"));
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    // --- Fix 3: retainAll with null-value-rejecting backing map ---

    @Test
    void testKeySetRetainAllWithConcurrentHashMapBacking() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder()
                .mapType(ConcurrentHashMap.class)
                .compactSize(10)
                .build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        boolean changed = map.keySet().retainAll(Arrays.asList("a", "c"));

        assertTrue(changed);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    // --- Fix 4: state-independent equals/hashCode for CI-sorted maps; self-ref safety ---

    @Test
    void testCiSortedEqualsAndHashCodeAreStateIndependent() {
        CompactMap<String, Object> mapState = CompactMap.<String, Object>builder()
                .caseSensitive(false).sortedOrder().compactSize(3).build();    // 4 entries -> MAP
        CompactMap<String, Object> arrayState = CompactMap.<String, Object>builder()
                .caseSensitive(false).sortedOrder().compactSize(50).build();   // 4 entries -> ARRAY
        for (CompactMap<String, Object> m : Arrays.asList(mapState, arrayState)) {
            m.put("Alpha", 1);
            m.put("Beta", 2);
            m.put("Gamma", 3);
            m.put("Delta", 4);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, mapState.getLogicalValueType());
        assertEquals(CompactMap.LogicalValueType.ARRAY, arrayState.getLogicalValueType());

        assertEquals(mapState, arrayState);
        assertEquals(arrayState, mapState);
        assertEquals(mapState.hashCode(), arrayState.hashCode(),
                "Equal maps must have equal hashCodes regardless of internal state");

        Map<String, Object> caseVariant = new HashMap<>();
        caseVariant.put("ALPHA", 1);
        caseVariant.put("BETA", 2);
        caseVariant.put("GAMMA", 3);
        caseVariant.put("DELTA", 4);
        assertEquals(mapState, caseVariant, "CI equals must ignore case in MAP state");
        assertEquals(arrayState, caseVariant, "CI equals must ignore case in ARRAY state");

        Map<String, Object> different = new HashMap<>(caseVariant);
        different.put("BETA", 99);
        assertFalse(mapState.equals(different));
        assertFalse(arrayState.equals(different));
    }

    @Test
    void testCaseSensitiveMapStateHashCodeUnchangedFromBackingMap() {
        // Pins the no-behavior-change guarantee for the common case: a case-sensitive map's
        // hashCode in MAP state must equal the JDK definition (sum of entry hashes).
        CompactMap<String, Object> map = new CompactMap<>();
        for (int i = 0; i < 60; i++) {
            map.put("key" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(new HashMap<>(map).hashCode(), map.hashCode());
    }

    @Test
    void testSelfReferentialValueHashCodeInMapState() {
        CompactMap<String, Object> map = new CompactMap<>();   // default compactSize 50
        for (int i = 0; i < 51; i++) {
            map.put("k" + i, i);
        }
        map.put("self", map);   // 52 entries -> MAP state
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        int hash = assertDoesNotThrow(map::hashCode,
                "Self-referential value must use the sentinel (17), not recurse");

        // An array-state twin with identical contents (self-ref resolving to itself) hashes equal.
        CompactMap<String, Object> twin = CompactMap.<String, Object>builder().compactSize(100).build();
        for (int i = 0; i < 51; i++) {
            twin.put("k" + i, i);
        }
        twin.put("self", twin);
        assertEquals(CompactMap.LogicalValueType.ARRAY, twin.getLogicalValueType());
        assertEquals(twin.hashCode(), hash, "Self-ref hashing must be state-independent");
    }
}
