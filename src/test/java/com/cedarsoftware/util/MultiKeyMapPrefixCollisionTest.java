package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Two Collection keys, one a prefix of the other, are different keys even when their hashes collide. The size check
 * was skipped for two Collections in the default (expanded) mode, and the comparison walked only the stored key's
 * length: get() of the longer key returned the shorter key's value, put() of it overwrote that entry, and get() or
 * put() of the shorter key, with the longer one stored, threw NoSuchElementException.
 * <p>
 * The collision is built at run time from the map's own hashes (read through flattenKey), so it stays a collision if
 * the hash combiner changes: for a combiner h * K + e, a third element whose hash is h2 - h2 * K leaves the hash of
 * [1, 2] unchanged. A Long below 2^32 hashes to its own value.
 */
class MultiKeyMapPrefixCollisionTest {

    private static int hashOf(MultiKeyMap<?> map, Object key) throws Exception {
        Method flatten = MultiKeyMap.class.getDeclaredMethod("flattenKey", Object.class);
        flatten.setAccessible(true);
        Object multiKey = flatten.invoke(map, key);
        Field hash = multiKey.getClass().getDeclaredField("hash");
        hash.setAccessible(true);
        return hash.getInt(multiKey);
    }

    /** A Long that, appended to [1, 2], leaves its hash unchanged. */
    private static long collidingTail(MultiKeyMap<?> map) throws Exception {
        int k = hashOf(map, Arrays.asList(0L));                 // 1 * K + hash(0) = K
        int h2 = hashOf(map, Arrays.asList(1L, 2L));
        long tail = (h2 - h2 * k) & 0xFFFFFFFFL;
        assertEquals(h2, hashOf(map, Arrays.asList(1L, 2L, tail)), "the test must build a real collision");
        return tail;
    }

    @Test
    void aLongerKeyDoesNotMatchItsPrefix() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        long tail = collidingTail(map);
        List<Object> two = Arrays.asList(1L, 2L);
        List<Object> three = Arrays.asList(1L, 2L, tail);

        map.put(two, "two");
        assertNull(map.get(three));
        assertFalse(map.containsKey(three));
        assertNull(map.put(three, "three"), "a new key, not the prefix's entry");
        assertEquals(2, map.size());
        assertEquals("two", map.get(two));
        assertEquals("three", map.get(three));
    }

    @Test
    void aShorterKeyDoesNotThrowAgainstALongerOne() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        long tail = collidingTail(map);
        List<Object> two = Arrays.asList(1L, 2L);
        List<Object> three = Arrays.asList(1L, 2L, tail);

        map.put(three, "three");
        assertNull(map.get(two));
        assertNull(map.put(two, "two"));
        assertEquals(2, map.size());
        assertEquals("two", map.remove(two));
        assertEquals("three", map.get(three));
    }

    @Test
    void inFlattenModeTooNestedListsOfDifferentLengths() throws Exception {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        long tail = collidingTail(map);
        map.put(new Object[]{1L, Arrays.asList(2L)}, "two");
        assertNull(map.get(new Object[]{1L, Arrays.asList(2L, tail)}));
        assertEquals("two", map.get(new Object[]{1L, Arrays.asList(2L)}));
    }
}
