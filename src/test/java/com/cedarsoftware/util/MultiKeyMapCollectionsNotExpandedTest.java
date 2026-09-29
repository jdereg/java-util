package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * In COLLECTIONS_NOT_EXPANDED mode a Collection the caller passes as the key is one key, compared by its own equals()
 * and hashCode(); arrays are still expanded, so an array and a List with the same elements are two keys. The mode
 * tested only the stored side: a Collection lookup matched a stored array whose hash it shared (an empty Set found
 * the empty array's value and put() overwrote it) while the reverse missed, and the keys the map expands itself --
 * an array holding an array or a List -- were compared with ArrayList.equals(), so a nested key lost the map's case
 * insensitivity and value-based equality.
 */
class MultiKeyMapCollectionsNotExpandedTest {

    private static MultiKeyMap<String> notExpanded() {
        return MultiKeyMap.<String>builder().collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();
    }

    private static int hashOf(MultiKeyMap<?> map, Object key) throws Exception {
        Method flatten = MultiKeyMap.class.getDeclaredMethod("flattenKey", Object.class);
        flatten.setAccessible(true);
        Object multiKey = flatten.invoke(map, key);
        Field hash = multiKey.getClass().getDeclaredField("hash");
        hash.setAccessible(true);
        return hash.getInt(multiKey);
    }

    private static void assertTwoKeys(Object array, Object collection) throws Exception {
        MultiKeyMap<String> probe = notExpanded();
        assertEquals(hashOf(probe, array), hashOf(probe, collection), "the test needs a hash collision");
        for (boolean arrayFirst : new boolean[]{true, false}) {
            MultiKeyMap<String> map = notExpanded();
            Object first = arrayFirst ? array : collection;
            Object second = arrayFirst ? collection : array;
            map.put(first, "first");
            assertNull(map.get(second), "arrayFirst=" + arrayFirst);
            assertNull(map.put(second, "second"), "arrayFirst=" + arrayFirst);
            assertEquals(2, map.size(), "arrayFirst=" + arrayFirst);
            assertEquals("first", map.get(first), "arrayFirst=" + arrayFirst);
            assertEquals("second", map.get(second), "arrayFirst=" + arrayFirst);
        }
    }

    /** Hashes as the element's hashCode() and equals by it, so a List of them and an array of them can collide. */
    private static final class Hashed {
        final int h;

        Hashed(int h) {
            this.h = h;
        }

        @Override
        public int hashCode() {
            return h;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Hashed && ((Hashed) o).h == h;
        }
    }

    /** An empty array and an empty Set both hash as 0. */
    @Test
    void anEmptySetIsNotTheEmptyArray() throws Exception {
        assertTwoKeys(new Object[0], new HashSet<>());
    }

    /**
     * [x, y] as an array hashes as (G + x) * G + y (G the map's golden-ratio multiplier) and as a List as
     * 961 + 31 * x + y, so they collide when (G - 31) * x == 961 - G * G. G - 31 is twice an odd number.
     */
    @Test
    void aListIsNotTheArrayOfItsElementsEvenWhenTheirHashesCollide() throws Exception {
        final int g = 0x9E3779B9;
        int odd = (g - 31) >>> 1;
        int inverse = odd;
        for (int i = 0; i < 5; i++) {
            inverse *= 2 - odd * inverse;
        }
        int x = ((961 - g * g) >>> 1) * inverse;
        assertEquals(961 - g * g, (g - 31) * x, "x solves the collision");
        Hashed[] elements = {new Hashed(x), new Hashed(12345)};
        assertTwoKeys(elements, new ArrayList<>(Arrays.asList(elements)));
        assertTwoKeys(elements.clone(), new LinkedList<>(Arrays.asList(elements)));
    }

    /** Nested containers inside an array key are expanded, and match by the map's rules as in the default mode. */
    @Test
    void aNestedKeyKeepsTheMapsRules() {
        MultiKeyMap<String> caseInsensitive = MultiKeyMap.<String>builder().caseSensitive(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build();
        caseInsensitive.put(new Object[]{"A", new String[]{"B"}}, "array in array");
        caseInsensitive.put(new Object[]{"C", Arrays.asList("D", "E")}, "list in array");
        assertEquals("array in array", caseInsensitive.get(new Object[]{"a", new String[]{"b"}}));
        assertEquals("array in array", caseInsensitive.get(new Object[]{"a", Arrays.asList("b")}));
        assertEquals("list in array", caseInsensitive.get(new Object[]{"c", new Object[]{"d", "e"}}));
        assertEquals("list in array", caseInsensitive.get(new Object[]{"c", Arrays.asList("d", "E")}));

        MultiKeyMap<String> valueBased = notExpanded();
        valueBased.put(new Object[]{1, new int[]{2}}, "ints");
        valueBased.put(new Object[]{3, Arrays.asList(4, 5.0)}, "list");
        assertEquals("ints", valueBased.get(new Object[]{1L, new long[]{2L}}));
        assertEquals("ints", valueBased.get(new Object[]{1.0, Arrays.asList(2L)}));
        assertEquals("list", valueBased.get(new Object[]{3L, new double[]{4, 5}}));
        assertEquals(2, valueBased.size());
    }

    /** The caller's Collection is compared by its own equals(): any List type, any Set type, nothing more. */
    @Test
    void aCollectionKeyIsComparedByItsOwnEquals() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().caseSensitive(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build();
        map.put(Arrays.asList(1, "A"), "list");
        map.put(new HashSet<>(Arrays.asList(1, 2, 3)), "set");
        assertEquals("list", map.get(new LinkedList<>(Arrays.asList(1, "A"))));
        assertEquals("set", map.get(new TreeSet<>(Arrays.asList(3, 2, 1))));
        assertNull(map.get(Arrays.asList(1L, "A")), "1L is not 1 to List.equals()");
        assertNull(map.get(Arrays.asList(1, "a")), "nor is a to A");
        assertNull(map.get(new Object[]{1, "A"}), "an array is a key of parts");
        assertNull(map.getMultiKey(1, "A"));
        assertNull(map.get(Arrays.asList(1, 2, 3)), "a List is not a Set");
        assertNull(map.get(new int[]{1, 2, 3}));

        List<Object> reported = new ArrayList<>(map.keySet());
        assertEquals(2, reported.size());
        for (Object key : reported) {
            assertEquals(key instanceof List ? "list" : "set", map.get(key));
        }
    }
}
