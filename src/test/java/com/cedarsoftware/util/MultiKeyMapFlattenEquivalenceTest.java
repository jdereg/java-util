package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * In flatten mode an array and a List with the same elements are one key, as in the default mode, also when the key
 * holds a null or holds nothing. A List is always expanded there and an array of plain elements is not, and the two
 * paths hashed a null (the expansion hashed its marker) and an empty key (the expansion hashed 1) differently: put
 * of [null, a] as an array was not found as a List, and an empty List and an empty array were two keys.
 */
class MultiKeyMapFlattenEquivalenceTest {

    private static MultiKeyMap<String> flatten() {
        return MultiKeyMap.<String>builder().flattenDimensions(true).build();
    }

    @Test
    void aNullInsideIsTheSameKeyInAnArrayAndAList() {
        MultiKeyMap<String> map = flatten();
        map.put(new Object[]{null, "a"}, "v");
        assertEquals("v", map.get(Arrays.asList(null, "a")));
        assertEquals("v", map.get(new Object[]{null, Arrays.asList("a")}));
        map.put(Arrays.asList(null, "a"), "w");
        assertEquals(1, map.size());
        assertEquals("w", map.get(new Object[]{null, "a"}));

        MultiKeyMap<String> single = flatten();
        single.put(new Object[]{null}, "v");
        single.put(Arrays.asList((Object) null), "w");
        assertEquals(1, single.size());
    }

    @Test
    void anEmptyListIsAnEmptyArray() {
        MultiKeyMap<String> map = flatten();
        map.put(new Object[0], "v");
        assertEquals("v", map.get(new ArrayList<>()));
        assertEquals("v", map.get(new Object[]{new Object[0]}), "a nested empty array flattens to nothing");
        map.put(new ArrayList<>(), "w");
        assertEquals(1, map.size());
        assertEquals("w", map.get(new Object[0]));
    }

    @Test
    void theDefaultModeAgrees() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{null, "a"}, "v");
        assertEquals("v", map.get(Arrays.asList(null, "a")));
        map.put(new Object[0], "e");
        assertEquals("e", map.get(new ArrayList<>()));
        assertEquals(2, map.size());
    }
}
