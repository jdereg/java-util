package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * In value-based mode two numbers are one key when their values are equal, whatever their types. The value of a
 * number, restated here as the oracle:
 * <ul>
 *     <li>a whole number is its exact integer, at any size (2^60 as a double is 2^60, as it is against a long);</li>
 *     <li>a number with a fraction is the decimal it prints: 0.1d and 0.1f are both 0.1, as BigDecimal("0.1") is;</li>
 *     <li>NaN is NaN and an infinity is itself, among floats and doubles only.</li>
 * </ul>
 * The map met a BigDecimal with a double's printed decimal even when the double was whole (2^60 became
 * ...980, not 2^60, while a long compared exactly), with a float's own printed decimal while comparing that float
 * with doubles as its binary value (0.1f matched BigDecimal("0.1") but not 0.1d, and hashed apart from the
 * BigDecimal), and let a saturating cast make 2^63 as a double equal Long.MAX_VALUE. Equal keys hashed apart, so
 * get() missed them, and unequal keys matched.
 */
class MultiKeyMapNumericValueTest {

    private static final BigInteger TWO_60 = BigInteger.ONE.shiftLeft(60);
    private static final BigInteger TWO_63 = BigInteger.ONE.shiftLeft(63);
    private static final BigInteger TWO_70 = BigInteger.ONE.shiftLeft(70);

    private static List<Number> pool() {
        return new ArrayList<>(Arrays.asList(
                0, 0L, 0.0d, -0.0d, 0.0f, -0.0f, new BigDecimal("0.00"), BigInteger.ZERO,
                1, 1L, (short) 1, (byte) 1, new AtomicInteger(1), new AtomicLong(1), 1.0d, 1.0f, new BigDecimal("1"),
                new BigDecimal("1.00"), BigInteger.ONE,
                0.1d, 0.1f, new BigDecimal("0.1"), new BigDecimal("0.10"), new BigDecimal(0.1),
                new BigDecimal("0.10000000149011612"), (double) 0.1f,
                1.1f, 1.1d, new BigDecimal("1.1"), (double) 1.1f,
                0.5f, 0.5d, new BigDecimal("0.5"),
                3.14f, 3.14d, new BigDecimal("3.14"),
                1.0E-5f, 1.0E-5d, new BigDecimal("0.00001"),
                (1L << 53) + 1, (double) (1L << 53), 1L << 53, new BigDecimal((1L << 53) + 1),
                1L << 60, 0x1p60, 0x1p60f, new BigDecimal(TWO_60), new BigDecimal("1.15292150460684698E18"), TWO_60,
                Long.MAX_VALUE, 0x1p63, TWO_63, new BigDecimal(TWO_63), BigInteger.valueOf(Long.MAX_VALUE),
                Long.MIN_VALUE, -0x1p63, BigInteger.valueOf(Long.MIN_VALUE),
                0x1p70, TWO_70, new BigDecimal("1.1805916207174113E21"),
                1e23, new BigDecimal("1E+23"), new BigDecimal(1e23).toBigInteger(),
                Double.NaN, Float.NaN, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    /** The oracle's value of a finite number. */
    private static BigDecimal value(Number n) {
        if (n instanceof Float) {
            float f = n.floatValue();
            return f == Math.rint(f) ? new BigDecimal((double) f) : new BigDecimal(Float.toString(f));
        }
        if (n instanceof Double) {
            double d = n.doubleValue();
            return d == Math.rint(d) ? new BigDecimal(d) : new BigDecimal(Double.toString(d));
        }
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        if (n instanceof BigInteger) {
            return new BigDecimal((BigInteger) n);
        }
        return BigDecimal.valueOf(n.longValue());
    }

    private static boolean nonFinite(Number n) {
        return (n instanceof Double || n instanceof Float) && !Double.isFinite(n.doubleValue());
    }

    private static boolean oracle(Number a, Number b) {
        if (nonFinite(a) || nonFinite(b)) {
            if (!nonFinite(a) || !nonFinite(b)) {
                return false;
            }
            double x = a.doubleValue(), y = b.doubleValue();
            return x == y || (Double.isNaN(x) && Double.isNaN(y));
        }
        return value(a).compareTo(value(b)) == 0;
    }

    private static String show(Number n) {
        return n + " (" + n.getClass().getSimpleName() + ")";
    }

    @Test
    void numbersAreOneKeyExactlyWhenTheirValuesAreEqual() {
        List<Number> pool = pool();
        List<String> wrong = new ArrayList<>();
        for (Number a : pool) {
            for (Number b : pool) {
                boolean expected = oracle(a, b);
                MultiKeyMap<String> map = new MultiKeyMap<>();
                map.put(a, "single");
                map.put(new Object[]{a, "k"}, "array");
                map.put(Arrays.asList("k", a, "k"), "list");
                check(wrong, expected, map.get(b), "single", a, b);
                check(wrong, expected, map.get(new Object[]{b, "k"}), "array", a, b);
                check(wrong, expected, map.get(Arrays.asList("k", b, "k")), "list", a, b);
            }
        }
        if (!wrong.isEmpty()) {
            fail(wrong.size() + " wrong lookups, first ones:\n" + String.join("\n", wrong.subList(0,
                    Math.min(25, wrong.size()))));
        }
    }

    private static void check(List<String> wrong, boolean expected, String got, String form, Number a, Number b) {
        if (expected != form.equals(got)) {
            wrong.add(form + ": put " + show(a) + ", get " + show(b) + " -> " + got + " (expected "
                    + (expected ? "found" : "missing") + ")");
        }
    }

    /** Keys the map calls equal hash alike: otherwise a lookup misses an equal key, and equal maps hash apart. */
    @Test
    void numbersTheMapCallsEqualHashAlike() throws Exception {
        Method equals = MultiKeyMap.class.getDeclaredMethod("elementEquals", Object.class, Object.class,
                boolean.class, boolean.class);
        Method hash = MultiKeyMap.class.getDeclaredMethod("computeElementHash", Object.class, boolean.class);
        equals.setAccessible(true);
        hash.setAccessible(true);
        List<Number> pool = pool();
        for (Number a : pool) {
            for (Number b : pool) {
                if ((Boolean) equals.invoke(null, a, b, true, true)) {
                    assertEquals(hash.invoke(null, a, true), hash.invoke(null, b, true), show(a) + " and " + show(b));
                }
            }
        }
    }

    @Test
    void primitiveArraysFollowTheSameValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new float[]{0.1f, 1.1f}, "floats");
        assertEquals("floats", map.get(new double[]{0.1, 1.1}));
        assertEquals("floats", map.get(Arrays.asList(new BigDecimal("0.1"), new BigDecimal("1.10"))));
        assertNull(map.get(new double[]{(double) 0.1f, (double) 1.1f}));

        map.put(new double[]{0x1p60, 0x1p63}, "whole");
        assertEquals("whole", map.get(new Object[]{TWO_60, TWO_63}));
        assertEquals("whole", map.get(Arrays.asList(1L << 60, new BigDecimal(TWO_63))));
        assertNull(map.get(new long[]{1L << 60, Long.MAX_VALUE}));
        assertNull(map.get(new Object[]{new BigDecimal("1.15292150460684698E18"), TWO_63}));
    }

    /** Type-strict mode is untouched: a float and a double are never one key there. */
    @Test
    void typeStrictModeStillTellsFloatsFromDoubles() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(false).build();
        map.put(0.1f, "float");
        assertNull(map.get(0.1d));
        assertEquals("float", map.get(0.1f));
    }
}
