package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for NumberConversions bugs.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class NumberConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ---- Bug #1: toYear uses shortValue() — truncates years outside -32768..32767 ----

    @Test
    void toYear_normalYear_works() {
        Year year = NumberConversions.toYear(2024, converter);
        assertEquals(Year.of(2024), year);
    }

    @Test
    void toYear_yearAboveShortMax_shouldNotTruncate() {
        // Year 40000 exceeds Short.MAX_VALUE (32767)
        // shortValue() silently truncates: (short)40000 = -25536
        Year year = NumberConversions.toYear(40000, converter);
        assertEquals(Year.of(40000), year);
    }

    @Test
    void toYear_fromLong_largeValue() {
        Year year = NumberConversions.toYear(100000L, converter);
        assertEquals(Year.of(100000), year);
    }

    @Test
    void toYear_fromBigInteger_largeValue() {
        Year year = NumberConversions.toYear(BigInteger.valueOf(50000), converter);
        assertEquals(Year.of(50000), year);
    }

    @Test
    void toYear_fromBigDecimal() {
        Year year = NumberConversions.toYear(new BigDecimal("2025"), converter);
        assertEquals(Year.of(2025), year);
    }

    @Test
    void toYear_negativeYear() {
        Year year = NumberConversions.toYear(-500, converter);
        assertEquals(Year.of(-500), year);
    }

    // ---- Bug #2: floatingPointToBigInteger crashes on NaN/Infinity ----

    @Test
    void floatingPointToBigInteger_nan_shouldThrowDescriptiveError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NumberConversions.floatingPointToBigInteger(Double.NaN, converter));
        assertTrue(e.getMessage().contains("NaN"));
    }

    @Test
    void floatingPointToBigInteger_positiveInfinity_shouldThrowDescriptiveError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NumberConversions.floatingPointToBigInteger(Double.POSITIVE_INFINITY, converter));
        assertTrue(e.getMessage().contains("Infinity"));
    }

    @Test
    void floatingPointToBigInteger_negativeInfinity_shouldThrowDescriptiveError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NumberConversions.floatingPointToBigInteger(Double.NEGATIVE_INFINITY, converter));
        assertTrue(e.getMessage().contains("Infinity"));
    }

    @Test
    void floatingPointToBigInteger_floatNan_shouldThrowDescriptiveError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NumberConversions.floatingPointToBigInteger(Float.NaN, converter));
        assertTrue(e.getMessage().contains("NaN"));
    }

    @Test
    void floatingPointToBigInteger_normalValue_works() {
        BigInteger result = NumberConversions.floatingPointToBigInteger(42.9, converter);
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void floatingPointToBigInteger_negativeValue_works() {
        BigInteger result = NumberConversions.floatingPointToBigInteger(-99.1, converter);
        assertEquals(BigInteger.valueOf(-99), result);
    }

    // ---- Bug #3: floatToString/doubleToString collapse negative zero to "0" ----

    @Test
    void floatToString_negativeZero_shouldPreserveSign() {
        // -0.0f == 0f is true in IEEE 754, so the special case catches it
        // Float.toString(-0.0f) returns "-0.0", but the method returns "0"
        String result = NumberConversions.floatToString(-0.0f, converter);
        assertEquals("-0.0", result);
    }

    @Test
    void floatToString_positiveZero_returnsZero() {
        String result = NumberConversions.floatToString(0.0f, converter);
        assertEquals("0", result);
    }

    @Test
    void floatToString_normalValue_works() {
        String result = NumberConversions.floatToString(3.14f, converter);
        assertEquals("3.14", result);
    }

    @Test
    void doubleToString_negativeZero_shouldPreserveSign() {
        // -0.0d == 0d is true in IEEE 754, so the special case catches it
        // Double.toString(-0.0) returns "-0.0", but the method returns "0"
        String result = NumberConversions.doubleToString(-0.0, converter);
        assertEquals("-0.0", result);
    }

    @Test
    void doubleToString_positiveZero_returnsZero() {
        String result = NumberConversions.doubleToString(0.0, converter);
        assertEquals("0", result);
    }

    @Test
    void doubleToString_normalValue_works() {
        String result = NumberConversions.doubleToString(2.718, converter);
        assertEquals("2.718", result);
    }
}
