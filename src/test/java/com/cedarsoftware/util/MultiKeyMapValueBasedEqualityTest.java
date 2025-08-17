package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for value-based equality in MultiKeyMap cross-container comparisons.
 * This tests the "semantic key matching" feature where numeric values are compared
 * by value rather than type (e.g., int(1) equals long(1L) equals double(1.0)).
 */
public class MultiKeyMapValueBasedEqualityTest {

    @Test
    void testValueBasedPrimitiveArrayEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Put with int array
        map.put(new int[]{1, 2, 3}, "int-value");

        // Should match with different numeric types containing same values
        assertEquals("int-value", map.get(new long[]{1L, 2L, 3L}));           // long array
        assertEquals("int-value", map.get(new double[]{1.0, 2.0, 3.0}));      // double array
        assertEquals("int-value", map.get(new float[]{1.0f, 2.0f, 3.0f}));    // float array
        assertEquals("int-value", map.get(new short[]{1, 2, 3}));             // short array
        assertEquals("int-value", map.get(new byte[]{1, 2, 3}));              // byte array

        // Should also work with Collection containing equivalent values
        assertEquals("int-value", map.get(Arrays.asList(1L, 2L, 3L)));        // List of Longs
        assertEquals("int-value", map.get(Arrays.asList(1.0, 2.0, 3.0)));     // List of Doubles
        assertEquals("int-value", map.get(Arrays.asList(1, 2, 3)));           // List of Integers
    }

    @Test
    void testValueBasedObjectArrayEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Put with Object array containing mixed numeric types
        map.put(new Object[]{1, 2.0, 3L}, "mixed-value");

        // Should match with different arrangements of equivalent values
        assertEquals("mixed-value", map.get(new Object[]{1L, 2.0f, 3}));      // Different types, same values
        assertEquals("mixed-value", map.get(Arrays.asList(1.0, 2, 3L)));      // Collection with equivalent values
    }

    @Test
    void testValueBasedBigDecimalEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Put with BigDecimal
        map.put(new Object[]{new BigDecimal("1.0"), new BigDecimal("2.0")}, "bigdecimal-value");

        // Should match with other numeric types
        assertEquals("bigdecimal-value", map.get(new Object[]{1.0, 2.0}));               // double
        assertEquals("bigdecimal-value", map.get(new Object[]{1, 2}));                   // int
        assertEquals("bigdecimal-value", map.get(new Object[]{1L, 2L}));                 // long
        assertEquals("bigdecimal-value", map.get(Arrays.asList(1.0, 2.0)));             // Collection
    }

    @Test
    void testValueBasedBigIntegerEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Put with BigInteger
        map.put(new Object[]{new BigInteger("100"), new BigInteger("200")}, "bigint-value");

        // Should match with other integral types
        assertEquals("bigint-value", map.get(new Object[]{100, 200}));                   // int
        assertEquals("bigint-value", map.get(new Object[]{100L, 200L}));                 // long
        assertEquals("bigint-value", map.get(new Object[]{100.0, 200.0}));               // double (no fractional part)
        assertEquals("bigint-value", map.get(Arrays.asList(100, 200)));                 // Collection
    }

    @Test
    void testFloatingPointSpecialValues() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Test NaN
        map.put(new Object[]{Double.NaN, 1.0}, "nan-value");
        assertEquals("nan-value", map.get(new Object[]{Float.NaN, 1.0f}));              // NaN equals NaN

        // Test Infinity
        map.put(new Object[]{Double.POSITIVE_INFINITY, 2.0}, "infinity-value");
        assertEquals("infinity-value", map.get(new Object[]{Float.POSITIVE_INFINITY, 2.0f}));

        // Test Negative Infinity
        map.put(new Object[]{Double.NEGATIVE_INFINITY, 3.0}, "neg-infinity-value");
        assertEquals("neg-infinity-value", map.get(new Object[]{Float.NEGATIVE_INFINITY, 3.0f}));
    }

    @Test
    void testNonNumericValuesStillWorkNormally() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Non-numeric values should still use regular equals
        map.put(new Object[]{"hello", 1, true}, "mixed-value");

        assertEquals("mixed-value", map.get(new Object[]{"hello", 1L, true}));           // numeric matches
        assertNull(map.get(new Object[]{"HELLO", 1, true}));                            // string case mismatch
        assertNull(map.get(new Object[]{"hello", 1, false}));                           // boolean mismatch
    }

    @Test
    void testPrecisionHandling() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Test that precision is handled correctly
        map.put(new Object[]{1.0}, "precise-value");

        assertEquals("precise-value", map.get(new Object[]{1}));                         // int 1 equals double 1.0
        assertEquals("precise-value", map.get(new Object[]{1L}));                        // long 1 equals double 1.0
        assertEquals("precise-value", map.get(new Object[]{new BigDecimal("1.0")}));     // BigDecimal 1.0

        // But 1.1 should not match 1.0
        assertNull(map.get(new Object[]{1.1}));
        assertNull(map.get(new Object[]{new BigDecimal("1.1")}));
    }

    @Test
    void testCrossContainerValueEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Put with primitive array
        map.put(new int[]{42, 100}, "cross-container-value");

        // Should match with Object array containing equivalent values
        assertEquals("cross-container-value", map.get(new Object[]{42L, 100.0}));
        assertEquals("cross-container-value", map.get(new Long[]{42L, 100L}));
        assertEquals("cross-container-value", map.get(Arrays.asList(42.0, 100)));

        // Put with Collection
        map.put(Arrays.asList(5.0, 10.0), "collection-value");

        // Should match with arrays containing equivalent values
        assertEquals("collection-value", map.get(new int[]{5, 10}));
        assertEquals("collection-value", map.get(new Object[]{5L, 10.0}));
        assertEquals("collection-value", map.get(new double[]{5.0, 10.0}));
    }

    @Test
    void testExistingNonValueBasedStillWorks() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Regular non-numeric key matching should still work exactly as before
        map.put(new Object[]{"key1", "key2"}, "string-value");
        assertEquals("string-value", map.get(new Object[]{"key1", "key2"}));
        assertEquals("string-value", map.get(Arrays.asList("key1", "key2")));

        // And should not match different strings
        assertNull(map.get(new Object[]{"key1", "KEY2"}));                              // case difference
        assertNull(map.get(new Object[]{"key1", "key3"}));                             // different string
    }

    @Test
    void testZeroValues() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();

        // Test that different representations of zero are treated as equal
        map.put(new Object[]{0}, "zero-value");

        assertEquals("zero-value", map.get(new Object[]{0L}));                          // long zero
        assertEquals("zero-value", map.get(new Object[]{0.0}));                         // double zero
        assertEquals("zero-value", map.get(new Object[]{0.0f}));                        // float zero
        assertEquals("zero-value", map.get(new Object[]{new BigDecimal("0")}));         // BigDecimal zero
        assertEquals("zero-value", map.get(new Object[]{new BigInteger("0")}));         // BigInteger zero

        // Negative zero should also equal positive zero for floating point
        assertEquals("zero-value", map.get(new Object[]{-0.0}));
        assertEquals("zero-value", map.get(new Object[]{-0.0f}));
    }
}