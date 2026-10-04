package com.cedarsoftware.util.convert;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * A String with a huge or tiny exponent converts to Byte, Short, Integer or Long at once.
 */
class ConverterHugeExponentTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private void outOfRangeOrZero(String huge, String tiny) {
        for (Class<?> type : new Class<?>[]{Byte.class, Short.class, Integer.class, Long.class}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert(huge, type), huge + " " + type);
            assertEquals(0L, ((Number) converter.convert(tiny, type)).longValue(), tiny + " " + type);
        }
    }

    /**
     * Text such as "1e9999999" was parsed to a BigDecimal and truncated to a whole number before the range check,
     * which built all of its 10 million digits: 1.5 s for this one, ten times longer for each digit added to the
     * exponent, so an 11-character string held a thread for minutes. Out of range, or below 1, is now settled from
     * the digit count first.
     */
    @Test
    void hugeAndTinyExponentsAnswerAtOnce() {
        assertTimeout(Duration.ofMillis(500), () -> outOfRangeOrZero("1e9999999", "1e-9999999"));
        assertTimeout(Duration.ofMillis(500), () -> outOfRangeOrZero("-1e999999999", "-1e-999999999"));
        // ordinary values are unchanged
        assertEquals(100_000, (int) converter.convert("1e5", Integer.class));
        assertEquals(12, (int) converter.convert("12.9", Integer.class));
        assertEquals(-12L, (long) converter.convert("-12.9", Long.class));
        assertEquals(0, (int) converter.convert("0.000", Integer.class));
        assertEquals(Long.MAX_VALUE, (long) converter.convert("9223372036854775807.5", Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("9223372036854775808", Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("1e10", Integer.class));
    }
}
