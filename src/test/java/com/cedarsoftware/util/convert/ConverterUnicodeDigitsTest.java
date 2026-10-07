package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Number text written in the decimal digits of another script -- Arabic-Indic, Devanagari, fullwidth -- converts to
 * every number type, as the JDK's own Integer.parseInt and new BigDecimal(String) read such digits. It converted to
 * Integer, Long, Short and Year, but Double, Float, BigInteger and BigDecimal refused it, as did decimal text for the
 * integer types and number text for the time types. ASCII text parses as before, the other scripts' digits looked for
 * only when it does not.
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
class ConverterUnicodeDigitsTest {

    private static final String ARABIC_42 = "٤٢";
    private static final String DEVANAGARI_42_5 = "४२.५";
    private static final String FULLWIDTH_1500 = "１５００";

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void everyNumberTypeReadsThem() {
        assertEquals(Integer.valueOf(42), converter.convert(ARABIC_42, Integer.class));
        assertEquals(Long.valueOf(42), converter.convert(ARABIC_42, Long.class));
        assertEquals(Double.valueOf(42), converter.convert(ARABIC_42, Double.class));
        assertEquals(Float.valueOf(42), converter.convert(ARABIC_42, Float.class));
        assertEquals(BigInteger.valueOf(42), converter.convert(ARABIC_42, BigInteger.class));
        assertEquals(new BigDecimal("42"), converter.convert(ARABIC_42, BigDecimal.class));
        assertEquals(42L, converter.convert(ARABIC_42, AtomicLong.class).get());
        assertEquals(Year.of(42), converter.convert(ARABIC_42, Year.class));
    }

    @Test
    void decimalTextAndTheIntegerTypes() {
        assertEquals(new BigDecimal("42.5"), converter.convert(DEVANAGARI_42_5, BigDecimal.class));
        assertEquals(42.5, converter.convert(DEVANAGARI_42_5, double.class));
        assertEquals(Integer.valueOf(42), converter.convert(DEVANAGARI_42_5, Integer.class));    // truncated, as "42.5"
        assertEquals(BigInteger.valueOf(42), converter.convert(DEVANAGARI_42_5, BigInteger.class));
    }

    /** Number text converts to a time type as that number: integer text milliseconds, decimal text seconds. */
    @Test
    void numberTextForTimeTypes() {
        assertEquals(Instant.ofEpochMilli(1500), converter.convert(FULLWIDTH_1500, Instant.class));
        assertEquals(converter.convert("1500", Duration.class), converter.convert(FULLWIDTH_1500, Duration.class));
        assertEquals(converter.convert("42.5", Instant.class), converter.convert(DEVANAGARI_42_5, Instant.class));
    }

    /** Text that is not a number in any script is still refused. */
    @Test
    void otherTextIsStillRefused() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("٤x", Double.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("٤٫٥", BigDecimal.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("abc", BigInteger.class));
    }
}
