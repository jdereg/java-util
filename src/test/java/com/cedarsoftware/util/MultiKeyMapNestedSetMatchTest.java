package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A Set inside a MultiKeyMap key is compared element by element, each element whole: a List inside it keeps its order,
 * and a Set inside it its own members. The Set's tokens were matched as one multiset, so once hashes collided
 * {[0, 2^31]} matched {[2^31, 0]}, and {{AaAa, AaBB}, {BBAa, BBBB}} matched {{AaAa, BBAa}, {AaBB, BBBB}} -- get()
 * returned the other key's value and put() overwrote it.
 */
class MultiKeyMapNestedSetMatchTest {

    private static final long BIG = 2147483648L;        // hashes as 0x80000000: [0, BIG] and [BIG, 0] collide

    private static Set<Object> set(Object... o) {
        return new LinkedHashSet<>(Arrays.asList(o));
    }

    private static int hashOf(MultiKeyMap<?> map, Object key) throws Exception {
        Method flatten = MultiKeyMap.class.getDeclaredMethod("flattenKey", Object.class);
        flatten.setAccessible(true);
        Object multiKey = flatten.invoke(map, key);
        Field hash = multiKey.getClass().getDeclaredField("hash");
        hash.setAccessible(true);
        return hash.getInt(multiKey);
    }

    private static void assertDistinct(Object first, Object second) throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertEquals(hashOf(map, first), hashOf(map, second), "the test needs a hash collision");
        map.put(first, "first");
        assertNull(map.get(second));
        assertNull(map.put(second, "second"));
        assertEquals(2, map.size());
        assertEquals("first", map.get(first));
        assertEquals("second", map.get(second));
    }

    @Test
    void aListInsideASetKeepsItsOrder() throws Exception {
        assertDistinct(set(Arrays.asList(0, BIG)), set(Arrays.asList(BIG, 0)));
    }

    @Test
    void aSetInsideASetKeepsItsMembers() throws Exception {
        assertDistinct(set(set("AaAa", "AaBB"), set("BBAa", "BBBB")), set(set("AaAa", "BBAa"), set("AaBB", "BBBB")));
    }

    /** More than six elements: the path that buckets elements by hash. */
    @Test
    void inALargerSetToo() throws Exception {
        List<Object> common = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            common.add(Arrays.asList(i, i + 100));
        }
        Set<Object> first = new LinkedHashSet<>(common);
        first.add(Arrays.asList(0, BIG));
        Set<Object> second = new LinkedHashSet<>(common);
        second.add(Arrays.asList(BIG, 0));
        assertDistinct(first, second);

        Set<Object> pairing1 = new LinkedHashSet<>(common);
        pairing1.add(set("AaAa", "AaBB"));
        pairing1.add(set("BBAa", "BBBB"));
        Set<Object> pairing2 = new LinkedHashSet<>(common);
        pairing2.add(set("AaAa", "BBAa"));
        pairing2.add(set("AaBB", "BBBB"));
        assertDistinct(pairing1, pairing2);
    }

    /** Equal keys still match: members in another order, nested Lists and Sets, whole numbers of other types. */
    @Test
    void equalNestedSetsStillMatch() {
        for (int size : new int[]{3, 9}) {
            List<Object> members = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                members.add(i % 2 == 0 ? Arrays.asList(i, "x" + i) : set(i, set("y" + i)));
            }
            MultiKeyMap<String> map = new MultiKeyMap<>();
            map.put(new LinkedHashSet<>(members), "v");

            List<Object> reordered = new ArrayList<>();
            for (int i = size - 1; i >= 0; i--) {
                reordered.add(i % 2 == 0 ? Arrays.asList((long) i, "x" + i) : set(set("y" + i), (double) i));
            }
            assertEquals("v", map.get(new LinkedHashSet<>(reordered)), "size " + size);
            Collections.shuffle(reordered, new java.util.Random(size));
            assertEquals("v", map.get(new LinkedHashSet<>(reordered)), "size " + size);
        }
    }
}
