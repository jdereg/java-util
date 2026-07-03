package com.cedarsoftware.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the DateUtilities / DeepEquals / DataGeneratorInputStream review fixes:
 *
 * 1. DateUtilities used default-locale toLowerCase()/toUpperCase(); on a Turkish-locale
 *    JVM, "APRIL" lowercases to "aprıl" (dotless ı) which misses the month map (NPE),
 *    and "ist" uppercases to "İST" which misses the timezone abbreviation map.
 *
 * 2. DeepEquals.hashDouble/hashFloat quantization (value * scale) saturated Math.round
 *    for large magnitudes, collapsing every float above ~21,474 (double above ~9.2e8)
 *    into a single hash bucket — degrading unordered comparison to O(n^2).
 *
 * 3. DataGeneratorInputStream.withRandomStrings(size, r, 0, 0, sep) generated an empty
 *    word and threw ArrayIndexOutOfBoundsException on the first read.
 */
class ReviewFixes2026Test {

    // --- DateUtilities locale-independence ---

    @Test
    void testMonthParsingUnderTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            Date date = DateUtilities.parseDate("APRIL 21st, 2024");
            assertNotNull(date, "Uppercase month name must parse regardless of default locale");
            Date lower = DateUtilities.parseDate("april 21st, 2024");
            assertEquals(lower, date);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void testTimezoneAbbreviationUnderTurkishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            // "ist" contains 'i' — Turkish uppercasing produces dotted İ and misses the map
            Date date = DateUtilities.parseDate("2024-01-15 14:30:00 ist");
            assertNotNull(date, "Timezone abbreviation must resolve regardless of default locale");
        } finally {
            Locale.setDefault(original);
        }
    }

    // --- DeepEquals hash distribution for large magnitudes ---

    @Test
    void testLargeFloatsHashDistinctly() {
        Set<Integer> hashes = new HashSet<>();
        hashes.add(DeepEquals.deepHashCode(30000.0f));
        hashes.add(DeepEquals.deepHashCode(99999.0f));
        hashes.add(DeepEquals.deepHashCode(1.0e30f));
        hashes.add(DeepEquals.deepHashCode(5.5e10f));
        assertEquals(4, hashes.size(), "Distinct large floats must not collapse into one hash bucket");
    }

    @Test
    void testLargeDoublesHashDistinctly() {
        Set<Integer> hashes = new HashSet<>();
        hashes.add(DeepEquals.deepHashCode(2.0e9));
        hashes.add(DeepEquals.deepHashCode(1.0e15));
        hashes.add(DeepEquals.deepHashCode(7.7e20));
        hashes.add(DeepEquals.deepHashCode(3.14e12));
        assertEquals(4, hashes.size(), "Distinct large doubles must not collapse into one hash bucket");
    }

    @Test
    void testLargeValueSetsStillCompareCorrectly() {
        Set<Double> a = new HashSet<>();
        Set<Double> b = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            a.add(1.0e12 + i * 1.0e6);
            b.add(1.0e12 + i * 1.0e6);
        }
        assertTrue(DeepEquals.deepEquals(a, b));
        b.remove(1.0e12);
        b.add(9.9e12);
        assertNotEquals(true, DeepEquals.deepEquals(a, b));
    }

    @Test
    void testIdenticalLargeValuesStillHashEqually() {
        assertEquals(DeepEquals.deepHashCode(1.0e15), DeepEquals.deepHashCode(1.0e15));
        assertEquals(DeepEquals.deepHashCode(99999.0f), DeepEquals.deepHashCode(99999.0f));
    }

    // --- DataGeneratorInputStream empty-word guard ---

    @Test
    void testWithRandomStringsRejectsZeroMinWordLen() {
        assertThrows(IllegalArgumentException.class,
                () -> DataGeneratorInputStream.withRandomStrings(100, new Random(1), 0, 0, ' '));
        assertThrows(IllegalArgumentException.class,
                () -> DataGeneratorInputStream.withRandomStrings(100, new Random(1), 5, 3, ' '));
    }

    @Test
    void testWithRandomStringsValidBoundsStillWork() throws Exception {
        try (DataGeneratorInputStream in = DataGeneratorInputStream.withRandomStrings(50, new Random(1), 1, 5, ' ')) {
            byte[] buf = new byte[50];
            int total = in.read(buf, 0, buf.length);
            assertEquals(50, total);
        }
    }

    // --- ExceptionUtilities: null callable must throw per doc, not be swallowed ---

    @Test
    void testSafelyIgnoreExceptionNullCallableThrows() {
        // Previously the NPE from invoking a null callable was caught by the
        // catch-all and defaultValue was silently returned
        assertThrows(IllegalArgumentException.class,
                () -> ExceptionUtilities.safelyIgnoreException(null, "default"));
    }

    // --- FastByteArrayOutputStream: invalid offset must throw even when len == 0 ---

    @Test
    void testWriteInvalidOffsetWithZeroLenThrows() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = new byte[4];
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, 5, 0));
        out.write(data, 4, 0);   // off == b.length with len 0 is legal per the contract
        assertEquals(0, out.size());
    }
}
