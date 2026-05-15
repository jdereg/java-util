package com.cedarsoftware.util.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for {@link VectorizedArrays}.
 *
 * <p>Goals:
 * <ul>
 *   <li>Confirm parity with the documented contract of JDK 9+'s
 *       {@code Arrays.equals/mismatch/compare(arr, int, int, arr, int, int)} on both
 *       JDK 9+ (intrinsic path) and JDK 8 (loop fallback) — the test bodies don't
 *       know which path fires, but the expected results are identical either way.</li>
 *   <li>Exercise both {@code char[]} and {@code byte[]} variants.</li>
 *   <li>Cover empty ranges, partial slices, prefix mismatches, and length mismatches.</li>
 * </ul>
 */
class VectorizedArraysTest {

    // -------------------------------------------------------------------
    // equalsRange — char[]
    // -------------------------------------------------------------------

    @Test
    void equalsRange_char_emptyRanges() {
        assertTrue(VectorizedArrays.equalsRange(new char[0], 0, 0, new char[0], 0, 0));
        assertTrue(VectorizedArrays.equalsRange(new char[]{'a'}, 0, 0, new char[]{'b'}, 0, 0));
    }

    @Test
    void equalsRange_char_identicalContent() {
        char[] a = "hello world".toCharArray();
        char[] b = "hello world".toCharArray();
        assertTrue(VectorizedArrays.equalsRange(a, 0, a.length, b, 0, b.length));
    }

    @Test
    void equalsRange_char_partialSlicesMatch() {
        char[] a = "XXhelloYY".toCharArray();
        char[] b = "AAhelloBB".toCharArray();
        assertTrue(VectorizedArrays.equalsRange(a, 2, 7, b, 2, 7));
    }

    @Test
    void equalsRange_char_differingMiddleChar() {
        char[] a = "abcde".toCharArray();
        char[] b = "abXde".toCharArray();
        assertFalse(VectorizedArrays.equalsRange(a, 0, 5, b, 0, 5));
    }

    @Test
    void equalsRange_char_lengthMismatch() {
        assertFalse(VectorizedArrays.equalsRange(new char[]{'a', 'b'}, 0, 2, new char[]{'a', 'b', 'c'}, 0, 3));
    }

    // -------------------------------------------------------------------
    // equalsRange — byte[]
    // -------------------------------------------------------------------

    @Test
    void equalsRange_byte_identicalContent() {
        byte[] a = {1, 2, 3, 4};
        byte[] b = {1, 2, 3, 4};
        assertTrue(VectorizedArrays.equalsRange(a, 0, 4, b, 0, 4));
    }

    @Test
    void equalsRange_byte_partialSlicesMatch() {
        byte[] a = {0, 1, 2, 3, 4, 5, 6};
        byte[] b = {9, 9, 2, 3, 4, 9, 9};
        assertTrue(VectorizedArrays.equalsRange(a, 2, 5, b, 2, 5));
    }

    @Test
    void equalsRange_byte_differingByte() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 9, 3};
        assertFalse(VectorizedArrays.equalsRange(a, 0, 3, b, 0, 3));
    }

    // -------------------------------------------------------------------
    // mismatchRange — char[]
    // -------------------------------------------------------------------

    @Test
    void mismatchRange_char_equalRanges_returnsNegativeOne() {
        char[] a = "abc".toCharArray();
        char[] b = "abc".toCharArray();
        assertEquals(-1, VectorizedArrays.mismatchRange(a, 0, 3, b, 0, 3));
    }

    @Test
    void mismatchRange_char_firstDifferingIndex() {
        char[] a = "abcdef".toCharArray();
        char[] b = "abcXef".toCharArray();
        assertEquals(3, VectorizedArrays.mismatchRange(a, 0, 6, b, 0, 6));
    }

    @Test
    void mismatchRange_char_differentLengthsCommonPrefix() {
        char[] a = "abc".toCharArray();
        char[] b = "abcdef".toCharArray();
        assertEquals(3, VectorizedArrays.mismatchRange(a, 0, 3, b, 0, 6));
    }

    @Test
    void mismatchRange_char_partialSlices() {
        char[] a = "ZZabcdef".toCharArray();
        char[] b = "QQabcXef".toCharArray();
        assertEquals(3, VectorizedArrays.mismatchRange(a, 2, 8, b, 2, 8));
    }

    // -------------------------------------------------------------------
    // mismatchRange — byte[]
    // -------------------------------------------------------------------

    @Test
    void mismatchRange_byte_equalRanges() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};
        assertEquals(-1, VectorizedArrays.mismatchRange(a, 0, 3, b, 0, 3));
    }

    @Test
    void mismatchRange_byte_firstDifferingByte() {
        byte[] a = {1, 2, 3, 4};
        byte[] b = {1, 2, 9, 4};
        assertEquals(2, VectorizedArrays.mismatchRange(a, 0, 4, b, 0, 4));
    }

    // -------------------------------------------------------------------
    // compareRange — char[]
    // -------------------------------------------------------------------

    @Test
    void compareRange_char_equal() {
        char[] a = "abc".toCharArray();
        char[] b = "abc".toCharArray();
        assertEquals(0, VectorizedArrays.compareRange(a, 0, 3, b, 0, 3));
    }

    @Test
    void compareRange_char_lessThan() {
        char[] a = "abc".toCharArray();
        char[] b = "abd".toCharArray();
        assertTrue(VectorizedArrays.compareRange(a, 0, 3, b, 0, 3) < 0);
    }

    @Test
    void compareRange_char_greaterThan() {
        char[] a = "abd".toCharArray();
        char[] b = "abc".toCharArray();
        assertTrue(VectorizedArrays.compareRange(a, 0, 3, b, 0, 3) > 0);
    }

    @Test
    void compareRange_char_shorterPrefixLessThanLonger() {
        char[] a = "abc".toCharArray();
        char[] b = "abcd".toCharArray();
        assertTrue(VectorizedArrays.compareRange(a, 0, 3, b, 0, 4) < 0);
    }

    // -------------------------------------------------------------------
    // compareRange — byte[]
    // -------------------------------------------------------------------

    @Test
    void compareRange_byte_equal() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};
        assertEquals(0, VectorizedArrays.compareRange(a, 0, 3, b, 0, 3));
    }

    @Test
    void compareRange_byte_lessThan() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 4};
        assertTrue(VectorizedArrays.compareRange(a, 0, 3, b, 0, 3) < 0);
    }

    @Test
    void compareRange_byte_signedComparisonNotUnsigned() {
        // Sanity: byte comparison uses signed bytes (not unsigned), matching
        // Arrays.compare(byte[], ...) which is documented as signed.
        // 0xFF (signed -1) < 0x00 (signed 0).
        byte[] a = {(byte) 0xFF};
        byte[] b = {0};
        assertTrue(VectorizedArrays.compareRange(a, 0, 1, b, 0, 1) < 0);
    }
}
