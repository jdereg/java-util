package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IntervalSet edge behavior found by review.
 */
class IntervalSetBehaviorTest {

    /**
     * The key sets are read-only views. They were the skip list's own writable views, so pollFirst() or remove()
     * on them deleted intervals without the set's lock, beside its merges and splits.
     */
    @Test
    void keySetsAreReadOnlyViews() {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(10, 20);
        set.add(30, 40);
        for (NavigableSet<Integer> keys : Arrays.asList(set.keySet(), set.descendingKeySet())) {
            assertThrows(UnsupportedOperationException.class, keys::pollFirst);
            assertThrows(UnsupportedOperationException.class, () -> keys.remove(10));
            assertThrows(UnsupportedOperationException.class, keys::clear);
            assertThrows(UnsupportedOperationException.class, () -> keys.headSet(30).clear());
        }
        assertEquals(2, set.size());
        set.add(50, 60);
        assertEquals(3, set.keySet().size(), "still a view: it shows later writes");
        assertEquals(Integer.valueOf(50), set.descendingKeySet().first());
    }

    /**
     * removeExact matches the end as the set orders values (compareTo), as it already matched the start: 2.00 is
     * the end 2.0. It compared the end with equals(), so the scale had to match too.
     */
    @Test
    void removeExactMatchesTheEndAsTheSetOrdersValues() {
        IntervalSet<BigDecimal> set = new IntervalSet<>();
        set.add(new BigDecimal("1.0"), new BigDecimal("2.0"));
        assertFalse(set.removeExact(new BigDecimal("1.0"), new BigDecimal("2.01")));
        assertTrue(set.removeExact(new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertTrue(set.isEmpty());
    }

    /**
     * The default totalDuration() throws ArithmeticException when a numeric difference does not fit a long, as
     * documented. longValue() wrapped a BigInteger ([0, 2^64 + 5) was 5 ns) and saturated a double ([0, 1e30)
     * was Long.MAX_VALUE ns).
     */
    @Test
    void defaultTotalDurationThrowsWhenADifferenceDoesNotFitALong() {
        BigInteger twoTo64 = BigInteger.ONE.shiftLeft(64);
        IntervalSet<BigInteger> wide = new IntervalSet<>();
        wide.add(BigInteger.ZERO, twoTo64.add(BigInteger.valueOf(5)));
        assertThrows(ArithmeticException.class, wide::totalDuration);
        IntervalSet<Double> huge = new IntervalSet<>();
        huge.add(0.0, 1e30);
        assertThrows(ArithmeticException.class, huge::totalDuration);
        IntervalSet<Double> infinite = new IntervalSet<>();
        infinite.add(0.0, Double.POSITIVE_INFINITY);
        assertThrows(ArithmeticException.class, infinite::totalDuration);

        IntervalSet<BigInteger> far = new IntervalSet<>();         // the bounds do not fit a long; the difference does
        far.add(twoTo64, twoTo64.add(BigInteger.valueOf(5)));
        assertEquals(Duration.ofNanos(5), far.totalDuration());
        IntervalSet<Double> fractional = new IntervalSet<>();      // whole parts, as before: 3 - 0
        fractional.add(0.5, 3.7);
        assertEquals(Duration.ofNanos(3), fractional.totalDuration());
        IntervalSet<BigDecimal> decimal = new IntervalSet<>();
        decimal.add(new BigDecimal("0.5"), new BigDecimal("3.7"));
        assertEquals(Duration.ofNanos(3), decimal.totalDuration());
        IntervalSet<Long> longs = new IntervalSet<>();
        longs.add(Long.MIN_VALUE, 0L);
        longs.add(1L, Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, longs::totalDuration);
    }

    /**
     * The default totalDuration() takes a BigDecimal bound's whole part without building the number an exponent
     * names. toBigInteger() did: [1, 1E+30000000) held a thread for 3.6 seconds and 1E+600000000 for five minutes
     * and 237 MB (JDK 25), on the way to an ArithmeticException; and [1E-30000000, 5), whose answer is 5 ns, cost
     * the same 3.6 seconds, since finding that the bound's whole part is 0 divided by 10 to the power of the scale.
     * A bound below 1 is 0 at once,
     * and a whole part that an exponent expands past 1,000 digits is refused from its digit count, as the Converter
     * refuses it (ConverterOptions.getMaxPlainDigits()).
     */
    @Test
    @Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void defaultTotalDurationDoesNotBuildTheNumberAnExponentNames() {
        for (String huge : new String[]{"1E+600000000", "1.5E+600000000", "1E+30000000", "1E+1001"}) {
            IntervalSet<BigDecimal> up = new IntervalSet<>();
            up.add(BigDecimal.ONE, new BigDecimal(huge));
            assertThrows(ArithmeticException.class, up::totalDuration, huge);
            IntervalSet<BigDecimal> down = new IntervalSet<>();
            down.add(new BigDecimal(huge).negate(), BigDecimal.ONE);
            assertThrows(ArithmeticException.class, down::totalDuration, "-" + huge);
        }
        for (String tiny : new String[]{"1E-600000000", "123456789E-600000000", "1E-30000000"}) {
            IntervalSet<BigDecimal> fraction = new IntervalSet<>();         // whole parts: 5 - 0
            fraction.add(new BigDecimal(tiny), new BigDecimal("5"));
            assertEquals(Duration.ofNanos(5), fraction.totalDuration(), tiny);
            IntervalSet<BigDecimal> negative = new IntervalSet<>();         // -0 is 0: 0 - -5
            negative.add(new BigDecimal("-5"), new BigDecimal(tiny).negate());
            assertEquals(Duration.ofNanos(5), negative.totalDuration(), "-" + tiny);
        }

        for (String zero : new String[]{"0E+2000", "0E+600000000", "0E+30", "0E-2000"}) {     // zero, however written
            IntervalSet<BigDecimal> fromZero = new IntervalSet<>();
            fromZero.add(new BigDecimal(zero), new BigDecimal("5"));
            assertEquals(Duration.ofNanos(5), fromZero.totalDuration(), zero);
        }

        // Unchanged: an exponent that names up to 1,000 digits is built, and so is any bound from its own digits
        IntervalSet<BigDecimal> thousand = new IntervalSet<>();             // 1E+999 is 1,000 digits
        thousand.add(new BigDecimal("1E+999"), new BigDecimal("1E+999").add(BigDecimal.valueOf(7)));
        assertEquals(Duration.ofNanos(7), thousand.totalDuration());
        BigDecimal written = new BigDecimal(BigInteger.TEN.pow(1500));      // 1,501 digits of its own, scale 0
        IntervalSet<BigDecimal> own = new IntervalSet<>();
        own.add(written, written.add(new BigDecimal("7.25")));
        assertEquals(Duration.ofNanos(7), own.totalDuration());
        IntervalSet<BigDecimal> exponent = new IntervalSet<>();             // 2.5E+3 is 2500
        exponent.add(new BigDecimal("2.5E+3"), new BigDecimal("2.6E+3"));
        assertEquals(Duration.ofNanos(100), exponent.totalDuration());
        IntervalSet<BigDecimal> scaled = new IntervalSet<>();               // a long fraction below a whole part
        scaled.add(new BigDecimal("1." + StringUtilities.repeat("3", 2000)), new BigDecimal("9.99"));
        assertEquals(Duration.ofNanos(8), scaled.totalDuration());
    }
}
