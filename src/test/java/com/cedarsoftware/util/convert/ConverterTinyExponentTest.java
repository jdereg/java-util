package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.BitSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A value with a tiny exponent (below 1 in magnitude) converts to BigInteger, and through it to UUID, at once: its
 * whole part is 0. To BitSet, which takes whole numbers only, it is refused at once.
 */
class ConverterTinyExponentTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private void wholePartIsZero(String tiny) {
        assertEquals(BigInteger.ZERO, converter.convert(tiny, BigInteger.class));
        BigDecimal value = new BigDecimal(tiny);
        assertEquals(BigInteger.ZERO, converter.convert(value, BigInteger.class));
        assertEquals(new UUID(0, 0), converter.convert(value, UUID.class));
        String message = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(value, BitSet.class)).getMessage();
        assertTrue(message.length() < 100, message.length() + " characters");
    }

    /**
     * Truncating to the whole part divided by 10 to the power of the scale: 1.5 to 2 s each for "1e-9999999", ten
     * times longer for each digit added to the exponent, so a short string or a parsed BigDecimal held a thread for
     * minutes to produce 0.
     */
    @Test
    void tinyExponentsGiveZeroAtOnce() {
        assertTimeout(Duration.ofMillis(500), () -> wholePartIsZero("1e-9999999"));
        assertTimeout(Duration.ofMillis(500), () -> wholePartIsZero("-1e-999999999"));
        assertEquals(BigInteger.valueOf(12), converter.convert(new BigDecimal("12.9"), BigInteger.class));
        assertEquals(BigInteger.valueOf(-12), converter.convert("-12.9", BigInteger.class));
        assertEquals(new BigInteger("100000"), converter.convert("1e5", BigInteger.class));
    }
}
