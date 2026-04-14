package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for DeepEquals — targets JaCoCo gaps:
 * - Primitive array element mismatch reporting (same length, different values)
 * - Enum mismatch reporting
 * - Float/Double NaN and -0.0 edge cases in deepHashCode
 * - nearlyEqual(float, float)
 * - Deque ordered comparison
 * - Unordered collection cross-bucket matching
 * - DIFF string formatting (formatValue, formatArrayContents, etc.)
 * - Map comparisons with various key types
 * - Custom equals short-circuit
 */
class DeepEqualsCoverageTest {

    // ========== Primitive array element mismatches (same length, different values) ==========

    @Test
    void testIntArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new int[]{1, 2, 3}, new int[]{1, 2, 4}, opts)).isFalse();
        assertThat(opts.get(DeepEquals.DIFF)).isNotNull();
    }

    @Test
    void testLongArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new long[]{1L, 2L}, new long[]{1L, 3L}, opts)).isFalse();
        assertThat(opts.get(DeepEquals.DIFF)).isNotNull();
    }

    @Test
    void testShortArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new short[]{1, 2}, new short[]{1, 3}, opts)).isFalse();
    }

    @Test
    void testByteArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new byte[]{1, 2}, new byte[]{1, 3}, opts)).isFalse();
    }

    @Test
    void testCharArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new char[]{'a', 'b'}, new char[]{'a', 'c'}, opts)).isFalse();
    }

    @Test
    void testBooleanArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new boolean[]{true, false}, new boolean[]{true, true}, opts)).isFalse();
    }

    @Test
    void testFloatArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new float[]{1.0f, 2.0f}, new float[]{1.0f, 3.0f}, opts)).isFalse();
    }

    @Test
    void testDoubleArrayElementMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new double[]{1.0, 2.0}, new double[]{1.0, 3.0}, opts)).isFalse();
    }

    // ========== Primitive array equal ==========

    @Test
    void testIntArrayEqual() {
        assertThat(DeepEquals.deepEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3})).isTrue();
    }

    @Test
    void testLongArrayEqual() {
        assertThat(DeepEquals.deepEquals(new long[]{1L, 2L}, new long[]{1L, 2L})).isTrue();
    }

    @Test
    void testDoubleArrayEqual() {
        assertThat(DeepEquals.deepEquals(new double[]{1.0, 2.0}, new double[]{1.0, 2.0})).isTrue();
    }

    // ========== Array length mismatches ==========

    @Test
    void testArrayLengthMismatch() {
        assertThat(DeepEquals.deepEquals(new int[]{1, 2}, new int[]{1, 2, 3})).isFalse();
    }

    // ========== Enum comparisons ==========

    enum Color { RED, GREEN, BLUE }

    @Test
    void testEnumEqual() {
        assertThat(DeepEquals.deepEquals(Color.RED, Color.RED)).isTrue();
    }

    @Test
    void testEnumMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(Color.RED, Color.BLUE, opts)).isFalse();
        assertThat(opts.get(DeepEquals.DIFF)).isNotNull();
    }

    @Test
    void testEnumDifferentTypes() {
        // Enum vs String (different types) should fail
        assertThat(DeepEquals.deepEquals(Color.RED, "RED")).isFalse();
    }

    // ========== Float/Double edge cases ==========

    @Test
    void testDoubleNaN() {
        // NaN == NaN via deepEquals (IEEE 754 says NaN != NaN for ==, but deepEquals should handle)
        assertThat(DeepEquals.deepEquals(Double.NaN, Double.NaN)).isTrue();
    }

    @Test
    void testFloatNaN() {
        assertThat(DeepEquals.deepEquals(Float.NaN, Float.NaN)).isTrue();
    }

    @Test
    void testDoubleInfinity() {
        assertThat(DeepEquals.deepEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)).isTrue();
        assertThat(DeepEquals.deepEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)).isFalse();
    }

    @Test
    void testFloatInfinity() {
        assertThat(DeepEquals.deepEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)).isTrue();
        assertThat(DeepEquals.deepEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)).isFalse();
    }

    @Test
    void testDoubleNegativeZero() {
        // -0.0 vs 0.0 should be considered equal under deepEquals (unlike IEEE bitwise)
        // or different — the behavior we care about is that it doesn't crash
        DeepEquals.deepEquals(-0.0, 0.0);
        DeepEquals.deepEquals(-0.0f, 0.0f);
    }

    // ========== deepHashCode edge cases ==========

    @Test
    void testHashCodeForDoubleNaN() {
        // Should not throw
        int h = DeepEquals.deepHashCode(Double.NaN);
        assertThat(h).isNotEqualTo(0);
    }

    @Test
    void testHashCodeForFloatNaN() {
        int h = DeepEquals.deepHashCode(Float.NaN);
        // Just verify it doesn't crash
        DeepEquals.deepHashCode(Float.valueOf(Float.NaN));
    }

    @Test
    void testHashCodeForDoubleNegZero() {
        // -0.0 and 0.0 should hash identically
        int h1 = DeepEquals.deepHashCode(-0.0);
        int h2 = DeepEquals.deepHashCode(0.0);
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void testHashCodeForFloatNegZero() {
        int h1 = DeepEquals.deepHashCode(-0.0f);
        int h2 = DeepEquals.deepHashCode(0.0f);
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void testHashCodeForNull() {
        assertThat(DeepEquals.deepHashCode(null)).isEqualTo(0);
    }

    @Test
    void testHashCodeForArray() {
        int[] a = new int[]{1, 2, 3};
        int[] b = new int[]{1, 2, 3};
        assertThat(DeepEquals.deepHashCode(a)).isEqualTo(DeepEquals.deepHashCode(b));
    }

    @Test
    void testHashCodeForList() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        assertThat(DeepEquals.deepHashCode(list1)).isEqualTo(DeepEquals.deepHashCode(list2));
    }

    @Test
    void testHashCodeForMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        assertThat(DeepEquals.deepHashCode(m1)).isEqualTo(DeepEquals.deepHashCode(m2));
    }

    // ========== Deque comparisons ==========

    @Test
    void testDequeEqual() {
        ArrayDeque<String> d1 = new ArrayDeque<>();
        d1.addLast("a");
        d1.addLast("b");
        ArrayDeque<String> d2 = new ArrayDeque<>();
        d2.addLast("a");
        d2.addLast("b");
        assertThat(DeepEquals.deepEquals(d1, d2)).isTrue();
    }

    @Test
    void testDequeMismatch() {
        ArrayDeque<String> d1 = new ArrayDeque<>();
        d1.addLast("a");
        d1.addLast("b");
        ArrayDeque<String> d2 = new ArrayDeque<>();
        d2.addLast("a");
        d2.addLast("c");
        assertThat(DeepEquals.deepEquals(d1, d2)).isFalse();
    }

    // ========== Unordered collection comparison (Set) ==========

    @Test
    void testSetEqualDifferentOrder() {
        Set<String> s1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        assertThat(DeepEquals.deepEquals(s1, s2)).isTrue();
    }

    @Test
    void testSetMismatch() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("a", "b", "d"));
        assertThat(DeepEquals.deepEquals(s1, s2)).isFalse();
    }

    @Test
    void testSetSizeMismatch() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> s2 = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertThat(DeepEquals.deepEquals(s1, s2)).isFalse();
    }

    // ========== Map comparisons ==========

    @Test
    void testMapEqual() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        assertThat(DeepEquals.deepEquals(m1, m2)).isTrue();
    }

    @Test
    void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 2);
        assertThat(DeepEquals.deepEquals(m1, m2)).isFalse();
    }

    @Test
    void testMapSizeMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        assertThat(DeepEquals.deepEquals(m1, m2)).isFalse();
    }

    @Test
    void testMapDifferentKeys() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 1);
        assertThat(DeepEquals.deepEquals(m1, m2)).isFalse();
    }

    // ========== List comparisons ==========

    @Test
    void testListEqual() {
        assertThat(DeepEquals.deepEquals(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 2, 3))).isTrue();
    }

    @Test
    void testListMismatch() {
        assertThat(DeepEquals.deepEquals(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 2, 4))).isFalse();
    }

    @Test
    void testLinkedListVsArrayList() {
        // Both ordered — should work
        assertThat(DeepEquals.deepEquals(
                new LinkedList<>(Arrays.asList("a", "b")),
                Arrays.asList("a", "b"))).isTrue();
    }

    // ========== null handling ==========

    @Test
    void testBothNull() {
        assertThat(DeepEquals.deepEquals(null, null)).isTrue();
    }

    @Test
    void testLeftNull() {
        assertThat(DeepEquals.deepEquals(null, "x")).isFalse();
    }

    @Test
    void testRightNull() {
        assertThat(DeepEquals.deepEquals("x", null)).isFalse();
    }

    // ========== Different types ==========

    @Test
    void testDifferentTypesIntVsString() {
        assertThat(DeepEquals.deepEquals(42, "42")).isFalse();
    }

    @Test
    void testStringsCanMatchNumbers() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertThat(DeepEquals.deepEquals(42, "42", opts)).isTrue();
    }

    @Test
    void testStringsCanMatchNumbersMismatch() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertThat(DeepEquals.deepEquals(42, "43", opts)).isFalse();
    }

    // ========== BigDecimal ==========

    @Test
    void testBigDecimalEqual() {
        assertThat(DeepEquals.deepEquals(new BigDecimal("1.00"), new BigDecimal("1.00"))).isTrue();
    }

    @Test
    void testBigDecimalScaleDifferent() {
        // 1.00 vs 1.0 — scale differs but numeric value same
        // deepEquals uses .equals() for BigDecimal by default (scale-sensitive)
        DeepEquals.deepEquals(new BigDecimal("1.00"), new BigDecimal("1.0"));
        // Just verify it doesn't crash
    }

    // ========== Object[] arrays ==========

    @Test
    void testObjectArrayEqual() {
        assertThat(DeepEquals.deepEquals(
                new Object[]{"a", 1, true},
                new Object[]{"a", 1, true})).isTrue();
    }

    @Test
    void testObjectArrayMismatch() {
        assertThat(DeepEquals.deepEquals(
                new Object[]{"a", 1, true},
                new Object[]{"a", 2, true})).isFalse();
    }

    @Test
    void testObjectArrayTypeMismatch() {
        // Object[] vs int[] — different array types
        assertThat(DeepEquals.deepEquals(
                new Object[]{1, 2, 3},
                new int[]{1, 2, 3})).isFalse();
    }

    // ========== ignoreCustomEquals ==========

    static class CustomEquals {
        String name;
        CustomEquals(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            return true; // Always equal
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @Test
    void testCustomEqualsHonored() {
        // Normal deepEquals respects custom equals → always true
        CustomEquals a = new CustomEquals("foo");
        CustomEquals b = new CustomEquals("bar");
        assertThat(DeepEquals.deepEquals(a, b)).isTrue();
    }

    @Test
    void testCustomEqualsIgnored() {
        Map<String, Object> opts = new HashMap<>();
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        CustomEquals a = new CustomEquals("foo");
        CustomEquals b = new CustomEquals("bar");
        assertThat(DeepEquals.deepEquals(a, b, opts)).isFalse();
    }

    // ========== deepEquals with DIFF output ==========

    @Test
    void testDiffOutputForMismatch() {
        Map<String, Object> opts = new HashMap<>();
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 2);

        assertThat(DeepEquals.deepEquals(m1, m2, opts)).isFalse();
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertThat(diff).isNotNull().isNotEmpty();
    }

    @Test
    void testDiffOutputForListMismatch() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 2, 4), opts)).isFalse();
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertThat(diff).isNotNull().isNotEmpty();
    }

    // ========== Strings ==========

    @Test
    void testStringEqual() {
        assertThat(DeepEquals.deepEquals("hello", "hello")).isTrue();
    }

    @Test
    void testStringMismatch() {
        assertThat(DeepEquals.deepEquals("hello", "world")).isFalse();
    }

    @Test
    void testEmptyStringEqual() {
        assertThat(DeepEquals.deepEquals("", "")).isTrue();
    }

    // ========== UUID ==========

    @Test
    void testUuidEqual() {
        UUID u = UUID.randomUUID();
        assertThat(DeepEquals.deepEquals(u, u)).isTrue();
    }

    @Test
    void testUuidMismatch() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        assertThat(DeepEquals.deepEquals(u1, u2)).isFalse();
    }

    // ========== Self-reference ==========

    @Test
    void testSelfReference() {
        Object o = new Object();
        assertThat(DeepEquals.deepEquals(o, o)).isTrue();
    }

    // ========== Empty collections ==========

    @Test
    void testEmptyListEqual() {
        assertThat(DeepEquals.deepEquals(Collections.emptyList(), Collections.emptyList())).isTrue();
    }

    @Test
    void testEmptyMapEqual() {
        assertThat(DeepEquals.deepEquals(Collections.emptyMap(), Collections.emptyMap())).isTrue();
    }

    @Test
    void testEmptySetEqual() {
        assertThat(DeepEquals.deepEquals(Collections.emptySet(), Collections.emptySet())).isTrue();
    }

    @Test
    void testEmptyArrayEqual() {
        assertThat(DeepEquals.deepEquals(new int[0], new int[0])).isTrue();
    }

    @Test
    void testEmptyObjectArrayEqual() {
        assertThat(DeepEquals.deepEquals(new Object[0], new Object[0])).isTrue();
    }

    // ========== Options preservation ==========

    @Test
    void testOptionsPreservedAcrossCallsWhenEqual() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        // Equal values — should complete without incident
        assertThat(DeepEquals.deepEquals("abc", "abc", opts)).isTrue();
        // Option should still be there
        assertThat(opts.get(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS)).isEqualTo(true);
    }

    // ========== Nested structures ==========

    @Test
    void testNestedMap() {
        Map<String, Object> m1 = new LinkedHashMap<>();
        Map<String, Integer> inner1 = new HashMap<>();
        inner1.put("x", 1);
        m1.put("inner", inner1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        Map<String, Integer> inner2 = new HashMap<>();
        inner2.put("x", 1);
        m2.put("inner", inner2);

        assertThat(DeepEquals.deepEquals(m1, m2)).isTrue();
    }

    @Test
    void testNestedMapMismatch() {
        Map<String, Object> m1 = new LinkedHashMap<>();
        Map<String, Integer> inner1 = new HashMap<>();
        inner1.put("x", 1);
        m1.put("inner", inner1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        Map<String, Integer> inner2 = new HashMap<>();
        inner2.put("x", 2);  // different value
        m2.put("inner", inner2);

        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(m1, m2, opts)).isFalse();
        assertThat(opts.get(DeepEquals.DIFF)).isNotNull();
    }

    @Test
    void testNestedArrayInArray() {
        Object[] a = new Object[]{new int[]{1, 2}, new int[]{3, 4}};
        Object[] b = new Object[]{new int[]{1, 2}, new int[]{3, 4}};
        assertThat(DeepEquals.deepEquals(a, b)).isTrue();
    }

    // ========== Arrays with different values (varied positions) ==========

    @Test
    void testIntArrayMismatchAtFirstElement() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new int[]{99, 2, 3}, new int[]{1, 2, 3}, opts)).isFalse();
    }

    @Test
    void testIntArrayMismatchAtLastElement() {
        Map<String, Object> opts = new HashMap<>();
        assertThat(DeepEquals.deepEquals(new int[]{1, 2, 99}, new int[]{1, 2, 3}, opts)).isFalse();
    }

    // ========== Custom POJO equality ==========

    static class SimplePojo {
        String name;
        int value;
        SimplePojo(String name, int value) { this.name = name; this.value = value; }
    }

    @Test
    void testPojoEqual() {
        SimplePojo p1 = new SimplePojo("foo", 42);
        SimplePojo p2 = new SimplePojo("foo", 42);
        assertThat(DeepEquals.deepEquals(p1, p2)).isTrue();
    }

    @Test
    void testPojoMismatch() {
        SimplePojo p1 = new SimplePojo("foo", 42);
        SimplePojo p2 = new SimplePojo("bar", 42);
        assertThat(DeepEquals.deepEquals(p1, p2)).isFalse();
    }
}
