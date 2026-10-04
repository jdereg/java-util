package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

import com.cedarsoftware.util.DeepEquals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A number is written out in plain digits, or built as a whole number, while that takes at most 1,000 digits
 * (ConverterOptions.getMaxPlainDigits()), or no more than the digits it already has. Past that it is written with an
 * exponent, and refused as a BigInteger. "1e999999999" is 11 characters; written out it was a billion digits, and as
 * a BigInteger about 415 MB: either one exhausted the heap.
 */
class ConverterPlainDigitsTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void moneyAndOrdinaryNumbersAreAlwaysPlain() {
        assertEquals("999999999999999.99", converter.convert(new BigDecimal("999999999999999.99"), String.class));
        assertEquals("999000000000000", converter.convert(new BigDecimal("9.99E14"), String.class));
        assertEquals("1000000000000000000", converter.convert(new BigDecimal("1E18"), String.class));
        assertEquals("0.00000001", converter.convert(new BigDecimal("1E-8"), String.class));
        assertEquals("1" + zeros(999), converter.convert(new BigDecimal("1E999"), String.class));   // 1,000 digits
        assertEquals("0." + zeros(997) + "1", converter.convert(new BigDecimal("1E-998"), String.class));
        // a number spelled out in more than 1,000 digits of its own stays plain: an exponent would save nothing
        String digits1234 = "7" + zeros(1232) + "3";
        assertEquals(digits1234, converter.convert(new BigDecimal(digits1234), String.class));
    }

    @Test
    void anExponentThatWouldAddMoreThan1000DigitsIsWrittenAsAnExponent() {
        assertEquals("1E+1000", converter.convert(new BigDecimal("1E1000"), String.class));
        assertEquals("1E-1000", converter.convert(new BigDecimal("1E-1000"), String.class));
        assertEquals(new BigDecimal("1E1000"), converter.convert("1E+1000", BigDecimal.class));
        Map<?, ?> map = converter.convert(new BigDecimal("1E1000"), Map.class);
        assertTrue(map.values().contains("1E+1000"), map.toString());
        assertTimeout(Duration.ofMillis(500), () -> {
            assertEquals("1E+999999999", converter.convert(new BigDecimal("1E999999999"), String.class));
            assertEquals("1E-999999999", converter.convert(new BigDecimal("1E-999999999"), String.class));
        });
    }

    @Test
    void aWholeNumberThatWouldTakeMoreThan1000DigitsIsRefused() {
        assertEquals(BigInteger.TEN.pow(999), converter.convert(new BigDecimal("1E999"), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(999), converter.convert("1e999", BigInteger.class));
        String digits1234 = "7" + zeros(1232) + "3";
        assertEquals(new BigInteger(digits1234), converter.convert(digits1234, BigInteger.class));
        assertEquals(new BigInteger(digits1234), converter.convert(new BigDecimal(digits1234), BigInteger.class));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(new BigDecimal("1E1000"), BigInteger.class));
        assertTrue(e.getMessage().contains("1E+1000") && e.getMessage().contains("1000"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> converter.convert("1e1000", BigInteger.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(new BigDecimal("1E1000"), UUID.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(new BigDecimal("1E1000"), BitSet.class));
        assertTimeout(Duration.ofMillis(500), () -> {
            assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(new BigDecimal("1E999999999"), BigInteger.class));
            assertThrows(IllegalArgumentException.class, () -> converter.convert("1e999999999", BigInteger.class));
        });
    }

    @Test
    void theLimitIsAnOption() {
        Converter fifty = new Converter(new DefaultConverterOptions() {
            @Override
            public int getMaxPlainDigits() {
                return 50;
            }
        });
        assertEquals("1E+60", fifty.convert(new BigDecimal("1E60"), String.class));
        assertEquals("1" + zeros(40), fifty.convert(new BigDecimal("1E40"), String.class));
        assertThrows(IllegalArgumentException.class, () -> fifty.convert(new BigDecimal("1E60"), BigInteger.class));
        assertEquals(BigInteger.TEN.pow(40), fifty.convert(new BigDecimal("1E40"), BigInteger.class));
    }

    /** Messages and DeepEquals' difference report write such a number with its exponent, at once. */
    @Test
    void messagesAndDifferencesStayShort() {
        assertTimeout(Duration.ofMillis(500), () -> {
            String message = assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(new BigDecimal("1E999999999"), LocalTime.class)).getMessage();
            assertTrue(message.length() < 300, message.length() + " characters");
            Map<String, Object> options = new java.util.HashMap<>();
            DeepEquals.deepEquals(new BigDecimal("1E999999999"), new BigDecimal("2E999999999"), options);
            String diff = String.valueOf(options.get("diff"));
            assertTrue(diff.length() < 2000, diff.length() + " characters");
        });
    }

    private static String zeros(int count) {
        StringBuilder text = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            text.append('0');
        }
        return text.toString();
    }
}
