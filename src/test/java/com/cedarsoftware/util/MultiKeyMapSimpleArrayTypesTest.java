package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to verify the expanded SIMPLE_ARRAY_TYPES optimization works correctly
 * for the many JDK DTO array types we added.
 */
public class MultiKeyMapSimpleArrayTypesTest {

    @Test
    void testExpandedSimpleArrayTypes() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Test basic wrapper types
        String[] strings = {"hello", "world"};
        Integer[] integers = {1, 2, 3};
        Double[] doubles = {1.1, 2.2};
        
        map.put(strings, "strings");
        map.put(integers, "integers");
        map.put(doubles, "doubles");
        
        assertEquals("strings", map.get(strings));
        assertEquals("integers", map.get(integers));
        assertEquals("doubles", map.get(doubles));

        // Test Date/Time types
        Date[] dates = {new Date(), new Date(System.currentTimeMillis() + 1000)};
        LocalDate[] localDates = {LocalDate.now(), LocalDate.now().plusDays(1)};
        LocalDateTime[] localDateTimes = {LocalDateTime.now(), LocalDateTime.now().plusHours(1)};
        
        map.put(dates, "dates");
        map.put(localDates, "localDates");
        map.put(localDateTimes, "localDateTimes");
        
        assertEquals("dates", map.get(dates));
        assertEquals("localDates", map.get(localDates));
        assertEquals("localDateTimes", map.get(localDateTimes));

        // Test Math/Precision types
        BigInteger[] bigInts = {BigInteger.valueOf(123), BigInteger.valueOf(456)};
        BigDecimal[] bigDecimals = {new BigDecimal("123.45"), new BigDecimal("678.90")};
        
        map.put(bigInts, "bigInts");
        map.put(bigDecimals, "bigDecimals");
        
        assertEquals("bigInts", map.get(bigInts));
        assertEquals("bigDecimals", map.get(bigDecimals));

        // Test Network/IO types
        URL[] urls = {new URL("http://example.com"), new URL("https://test.com")};
        URI[] uris = {new URI("http://example.com"), new URI("https://test.com")};
        
        map.put(urls, "urls");
        map.put(uris, "uris");
        
        assertEquals("urls", map.get(urls));
        assertEquals("uris", map.get(uris));

        // Test Utility types
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID()};
        
        map.put(uuids, "uuids");
        assertEquals("uuids", map.get(uuids));

        // Verify all entries are present
        assertEquals(11, map.size());
    }

    @Test
    void testSimpleArrayTypesPerformance() {
        // Create a performance test to ensure the Set lookup is actually being used
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            String[] key = {"test" + i};
            map.put(key, "value" + i);
        }

        // Time many operations with different simple array types
        String[] strings = {"perf", "test"};
        Integer[] integers = {100, 200};
        Double[] doubles = {1.5, 2.5};
        Date[] dates = {new Date()};

        long start = System.nanoTime();
        
        // Perform many operations to test performance
        for (int i = 0; i < 10000; i++) {
            map.put(strings, "strings" + i);
            map.get(strings);
            
            map.put(integers, "integers" + i);
            map.get(integers);
            
            map.put(doubles, "doubles" + i);
            map.get(doubles);
            
            map.put(dates, "dates" + i);
            map.get(dates);
        }
        
        long end = System.nanoTime();
        long duration = end - start;
        
        System.out.printf("Simple array types operations: %,d ns (%.2f ms) for 40,000 operations%n", 
            duration, duration / 1_000_000.0);
        System.out.printf("Average per operation: %.2f ns%n", duration / 40_000.0);

        // Verify final state
        assertEquals("strings9999", map.get(strings));
        assertEquals("integers9999", map.get(integers));
        assertEquals("doubles9999", map.get(doubles));
        assertEquals("dates9999", map.get(dates));
    }

    @Test
    void testMixedArrayTypes() {
        // Test that mixing simple and complex array types works correctly
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Simple array types (should use fast path)
        String[] simpleStrings = {"simple", "array"};
        Integer[] simpleInts = {1, 2, 3};

        // Complex array types (should use slow path)
        Object[] complexArray = {"outer", new String[]{"nested"}};
        String[][] nestedArray = {{"deep", "nested"}, {"more", "nested"}};

        map.put(simpleStrings, "simple_strings");
        map.put(simpleInts, "simple_ints");
        map.put(complexArray, "complex_array");
        map.put(nestedArray, "nested_array");

        assertEquals("simple_strings", map.get(simpleStrings));
        assertEquals("simple_ints", map.get(simpleInts));
        assertEquals("complex_array", map.get(complexArray));
        assertEquals("nested_array", map.get(nestedArray));

        assertEquals(4, map.size());
    }

    @Test
    void testEmptySimpleArrays() {
        // Test empty arrays of simple types
        // Note: In MultiKeyMap, empty arrays are equivalent regardless of type
        MultiKeyMap<String> map = new MultiKeyMap<>();

        String[] emptyStrings = {};
        Integer[] emptyInts = {};
        Date[] emptyDates = {};

        map.put(emptyStrings, "empty_strings");
        map.put(emptyInts, "empty_ints");      // This overwrites the previous due to equivalence
        map.put(emptyDates, "empty_dates");    // This overwrites the previous due to equivalence

        // All empty arrays are equivalent, so they all return the last value set
        assertEquals("empty_dates", map.get(emptyStrings));
        assertEquals("empty_dates", map.get(emptyInts));
        assertEquals("empty_dates", map.get(emptyDates));
        
        // Only one entry since all empty arrays are equivalent
        assertEquals(1, map.size());
    }

    @Test
    void testSimpleArraysWithNulls() {
        // Test arrays with null elements
        MultiKeyMap<String> map = new MultiKeyMap<>();

        String[] stringsWithNull = {"hello", null, "world"};
        Integer[] intsWithNull = {1, null, 3};
        Date[] datesWithNull = {new Date(), null};

        map.put(stringsWithNull, "strings_with_null");
        map.put(intsWithNull, "ints_with_null");
        map.put(datesWithNull, "dates_with_null");

        assertEquals("strings_with_null", map.get(stringsWithNull));
        assertEquals("ints_with_null", map.get(intsWithNull));
        assertEquals("dates_with_null", map.get(datesWithNull));
    }
}