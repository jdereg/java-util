package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MultiKeyMap's key hashes spread, and every path that hashes a key agrees. Elements were combined as h * 31 + e, so
 * small numbers and short strings collided linearly ((0, 31) hashed as (1, 0)): a 1,000 x 1,000 grid of int pairs put
 * 31 keys on each hash, and lookups walked the chains (get 900-1,100 ns, now about 250). A Set's elements were XORed,
 * which cancels: the 41,664 Sets {i, j, k} of 0..63 had 127 distinct hashes (get about 16,000 ns, now about 200).
 * The combiner is used at every site -- arrays of each primitive type, Object[], Collections, nested expansion -- so
 * the forms of one key must stay one key.
 */
class MultiKeyMapHashSpreadTest {

    private static int hashOf(MultiKeyMap<?> map, Object key) throws Exception {
        Method flatten = MultiKeyMap.class.getDeclaredMethod("flattenKey", Object.class);
        flatten.setAccessible(true);
        Object multiKey = flatten.invoke(map, key);
        Field hash = multiKey.getClass().getDeclaredField("hash");
        hash.setAccessible(true);
        return hash.getInt(multiKey);
    }

    @Test
    void aGridOfIntPairsHasNearlyOneKeyPerHash() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<Integer> hashes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                hashes.add(hashOf(map, new Object[]{i, j}));
            }
        }
        assertTrue(hashes.size() > 990_000, "distinct hashes for 1,000,000 int pairs: " + hashes.size());
    }

    @Test
    void setsOfSmallIntsHaveNearlyOneKeyPerHash() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<Integer> hashes = new HashSet<>();
        int sets = 0;
        for (int i = 0; i < 64; i++) {
            for (int j = i + 1; j < 64; j++) {
                for (int k = j + 1; k < 64; k++) {
                    hashes.add(hashOf(map, new HashSet<>(Arrays.asList(i, j, k))));
                    sets++;
                }
            }
        }
        assertEquals(41_664, sets);
        assertTrue(hashes.size() > 41_000, "distinct hashes for 41,664 Sets: " + hashes.size());
    }

    /** Every form of the same tuple of whole numbers is one key: arrays of each numeric type, Object[], Lists. */
    @Test
    void everyFormOfAKeyIsOneKey() {
        Random random = new Random(11);
        for (int round = 0; round < 500; round++) {
            int n = 1 + random.nextInt(4);
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = random.nextInt(100);
            }
            List<Object> forms = forms(values);
            MultiKeyMap<String> map = new MultiKeyMap<>();
            map.put(forms.get(0), "v");
            for (Object form : forms) {
                assertEquals("v", map.get(form), Arrays.toString(values) + " as " + form.getClass().getSimpleName());
                map.put(form, "v");
                assertEquals(1, map.size(), Arrays.toString(values) + " as " + form.getClass().getSimpleName());
            }
        }
    }

    private static List<Object> forms(int[] values) {
        int n = values.length;
        Object[] integers = new Object[n];
        Object[] longs = new Object[n];
        Object[] doubles = new Object[n];
        Integer[] boxed = new Integer[n];
        long[] longArray = new long[n];
        short[] shortArray = new short[n];
        byte[] byteArray = new byte[n];
        double[] doubleArray = new double[n];
        float[] floatArray = new float[n];
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            integers[i] = values[i];
            longs[i] = (long) values[i];
            doubles[i] = (double) values[i];
            boxed[i] = values[i];
            longArray[i] = values[i];
            shortArray[i] = (short) values[i];
            byteArray[i] = (byte) values[i];
            doubleArray[i] = values[i];
            floatArray[i] = values[i];
            list.add(values[i]);
        }
        return Arrays.asList(integers, longs, doubles, boxed, values.clone(), longArray, shortArray, byteArray,
                doubleArray, floatArray, list, Collections.unmodifiableList(new ArrayList<>(list)));
    }

    /** A Set is one key whatever the order of its elements and the type of its whole numbers. */
    @Test
    void aSetIsOneKeyInAnyOrder() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), "v");
        assertEquals("v", map.get(new LinkedHashSet<>(Arrays.asList(3, 1, 2))));
        assertEquals("v", map.get(new LinkedHashSet<>(Arrays.asList(2L, 3.0, 1))));
        assertEquals("v", map.get(new HashSet<>(Arrays.asList(3, 2, 1))));
    }

    /** In flatten mode the nested and flat forms of a key are one key. */
    @Test
    void inFlattenModeNestedAndFlatFormsAreOneKey() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        map.put(new Object[]{1, Arrays.asList(2, 3)}, "v");
        assertEquals("v", map.get(new int[]{1, 2, 3}));
        assertEquals("v", map.get(Arrays.asList(1, 2, 3)));
        assertEquals("v", map.get(new Object[]{new long[]{1, 2}, 3}));
        assertEquals(1, map.size());
    }
}
