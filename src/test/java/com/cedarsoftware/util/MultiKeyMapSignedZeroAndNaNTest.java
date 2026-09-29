package com.cedarsoftware.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * -0.0 and +0.0 are one key and NaN is one key, in both equality modes, as the class Javadoc says. Type-strict mode
 * (valueBasedEquality = false) compared with Double.equals() and Arrays.equals(), which tell -0.0 from +0.0 by their
 * bits, while the hash already treated them as one: get(-0.0) missed a key stored as 0.0, and put() made a second
 * entry. Type-strict mode keeps only its type distinction (0 is not 0.0, 0.0f is not 0.0d).
 */
class MultiKeyMapSignedZeroAndNaNTest {

    private static final double OTHER_NAN = Double.longBitsToDouble(0x7ff8000000000001L);
    private static final float OTHER_FLOAT_NAN = Float.intBitsToFloat(0x7fc00001);

    private static void assertOneKey(Object stored, Object lookup) {
        for (boolean valueBased : new boolean[]{true, false}) {
            String what = (valueBased ? "value-based: " : "type-strict: ") + show(stored) + " and " + show(lookup);
            MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(valueBased).build();
            map.put(stored, "v");
            assertEquals("v", map.get(lookup), what);
            map.put(lookup, "w");
            assertEquals(1, map.size(), what + ": one entry");
            assertEquals("w", map.get(stored), what);
        }
    }

    private static String show(Object key) {
        return key instanceof Object[] ? Arrays.deepToString((Object[]) key) : key instanceof double[]
                ? Arrays.toString((double[]) key) : key instanceof float[] ? Arrays.toString((float[]) key)
                : String.valueOf(key);
    }

    @Test
    void signedZerosAreOneKeyInBothModes() {
        assertOneKey(0.0d, -0.0d);
        assertOneKey(0.0f, -0.0f);
        assertOneKey(new double[]{0.0, 1.0}, new double[]{-0.0, 1.0});
        assertOneKey(new float[]{1.0f, 0.0f}, new float[]{1.0f, -0.0f});
        assertOneKey(new Object[]{0.0d, 2.0d}, new Object[]{-0.0d, 2.0d});
        assertOneKey(new Object[]{0.0d, 2.0d}, new Double[]{-0.0d, 2.0d});
        assertOneKey(new double[]{0.0}, Arrays.asList(-0.0d));
        assertOneKey(new Object[]{"x", new double[]{0.0}}, new Object[]{"x", new double[]{-0.0}});
        assertOneKey(new Object[]{"x", Arrays.asList(0.0f)}, new Object[]{"x", new float[]{-0.0f}});
    }

    /** Every NaN is one key: Double.equals() and Arrays.equals() already agreed, so this pins it. */
    @Test
    void nanIsOneKeyInBothModes() {
        assertOneKey(Double.NaN, OTHER_NAN);
        assertOneKey(Float.NaN, OTHER_FLOAT_NAN);
        assertOneKey(new double[]{Double.NaN}, new double[]{OTHER_NAN});
        assertOneKey(new float[]{Float.NaN}, new float[]{OTHER_FLOAT_NAN});
        assertOneKey(new Object[]{Double.NaN, 1.0d}, new Object[]{OTHER_NAN, 1.0d});
    }

    @Test
    void typeStrictModeStillTellsTypesApart() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(false).build();
        map.put(0.0d, "double");
        map.put(new double[]{0.0}, "double[]");
        assertNull(map.get(0));
        assertNull(map.get(0L));
        assertNull(map.get(-0.0f));
        assertNull(map.get(new float[]{-0.0f}));
        assertNull(map.get(new long[]{0}));
        assertEquals("double", map.get(-0.0d));
        assertEquals("double[]", map.get(new double[]{-0.0}));
    }
}
