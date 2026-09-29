package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keys MultiKeyMap reports (keySet(), entrySet(), and so forEach(), replaceAll(), copies) are keys that find
 * their own entries again, one per entry. They unwrapped any 1-element key ([a] was reported as a, [null] as null --
 * different keys), put every key holding only Sets into one flat Set ({{a},{b}} became {a,b}; in flatten mode
 * [x,{a,b}] and [{a,b},x] both became {a,b,x}), and in COLLECTIONS_NOT_EXPANDED mode returned internal lists with
 * their markers. So entries went missing from keySet() and entrySet(), a map was not equal to its own copy, putAll()
 * and new HashMap<>(map) lost keys, forEach() skipped entries, and a key read from the map did not find its value.
 */
class MultiKeyMapReportedKeysTest {

    private static Set<Object> set(Object... o) {
        return new LinkedHashSet<>(Arrays.asList(o));
    }

    private static List<Object> list(Object... o) {
        return new ArrayList<>(Arrays.asList(o));
    }

    /** Key shapes: single, flat, nested, Sets, empties, nulls inside. */
    private static Object[] shapes() {
        List<Object> shapes = new ArrayList<>(Arrays.asList(
                "a", new Object[]{"a"}, list("a"), null, new Object[]{"a", "b"}, list("a", "b"), new int[]{1, 2},
                new Object[]{"a", list("b")}, new Object[]{list("a"), "b"}, set("a", "b"), set(set("a"), set("b")),
                new Object[]{"x", set("a", "b")}, new Object[]{set("a", "b"), "x"}, list(list("a", "b"), list("c")),
                list(list("a"), "b", set("c")), new Object[]{}, list(), set(), new Object[]{list()},
                new Object[]{set()}, list(set("a")), new Object[]{new Object[]{"a"}}, list(1, 2L, 3.0)));
        shapes.addAll(Arrays.asList(new Object[]{null}, list((Object) null), new Object[]{"a", null}));
        return shapes.toArray();
    }

    private static MultiKeyMap<String> filled(MultiKeyMap<String> map, Object[] shapes) {
        for (int i = 0; i < shapes.length; i++) {
            map.put(shapes[i], "v" + i);
        }
        return map;
    }

    private static void assertKeysFindTheirEntries(MultiKeyMap<String> map, String mode) {
        assertEquals(map.size(), map.keySet().size(), mode + ": keySet");
        assertEquals(map.size(), map.entrySet().size(), mode + ": entrySet");
        for (Map.Entry<Object, String> e : map.entrySet()) {
            assertEquals(e.getValue(), map.get(e.getKey()), mode + ": " + e.getKey());
            assertTrue(map.containsKey(e.getKey()), mode + ": " + e.getKey());
        }
        MultiKeyMap<String> copy = MultiKeyMap.<String>builder().flattenDimensions(map.getFlattenDimensions())
                .collectionKeyMode(map.getCollectionKeyMode()).build();
        copy.putAll(map);
        assertEquals(map.size(), copy.size(), mode + ": putAll copy");
        assertEquals(map, copy, mode + ": a copy is equal");
        assertEquals(copy, map, mode + ": a copy is equal");
        assertEquals(map.hashCode(), copy.hashCode(), mode + ": and hashes alike");
    }

    @Test
    void everyReportedKeyFindsItsOwnEntry() {
        assertKeysFindTheirEntries(filled(new MultiKeyMap<>(), shapes()), "default");
        assertKeysFindTheirEntries(filled(MultiKeyMap.<String>builder().flattenDimensions(true).build(),
                shapes()), "flatten");
        assertKeysFindTheirEntries(filled(MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build(), shapes()),
                "collections not expanded");
        // In flatten mode an expanded array's first token is often a plain element or a Set's marker, which is how
        // an internal list was told from a caller's Collection: it was reported markers and all ([x, {, a, b, }]),
        // and 4 of these 22 entries were missing from keySet() and from a copy
        assertKeysFindTheirEntries(filled(MultiKeyMap.<String>builder().flattenDimensions(true)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build(), shapes()),
                "collections not expanded, flatten");
    }

    @Test
    void aOneElementKeyIsReportedAsAOneElementList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "bare");
        map.put(new Object[]{"a"}, "array");
        map.put(null, "null");
        map.put(new Object[]{null}, "array of null");
        assertEquals(4, map.keySet().size());
        assertTrue(map.keySet().contains(Collections.singletonList("a")));
        assertTrue(map.keySet().contains(Collections.singletonList(null)));
        assertEquals("array", map.get(Collections.singletonList("a")));
        assertEquals(4, new HashMap<>(map).size());

        AtomicInteger visits = new AtomicInteger();
        map.forEach((k, v) -> visits.incrementAndGet());
        assertEquals(4, visits.get());
        map.replaceAll((k, v) -> v + "!");
        assertEquals("array!", map.get(new Object[]{"a"}));
        assertEquals("array of null!", map.get(new Object[]{null}));
        assertEquals("bare!", map.get("a"));
    }

    @Test
    void nestingIsReportedAsItWasStored() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(set(set("a"), set("b")), "set of sets");
        map.put(set("a", "b"), "set");
        assertTrue(map.keySet().contains(set(set("a"), set("b"))));
        assertTrue(map.keySet().contains(set("a", "b")));

        MultiKeyMap<String> flat = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        flat.put(new Object[]{"x", set("a", "b")}, "x first");
        flat.put(new Object[]{set("a", "b"), "x"}, "set first");
        assertTrue(flat.keySet().contains(Arrays.asList("x", set("a", "b"))));
        assertTrue(flat.keySet().contains(Arrays.asList(set("a", "b"), "x")));
    }

    @Test
    void inCollectionsNotExpandedModeAnArrayKeyIsReportedAsAnArray() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build();
        map.put(new Object[]{"x", set("a", "b")}, "nested");
        Object key = map.keySet().iterator().next();
        assertTrue(key instanceof Object[], "not an internal list with markers: " + key);
        assertArrayEquals(new Object[]{"x", set("a", "b")}, (Object[]) key);
        assertEquals("nested", map.get(key));
    }

    /** The key part of each toString() line: "  🆔 key → 🟣 value". */
    private static List<String> renderedKeys(MultiKeyMap<String> map) {
        List<String> keys = new ArrayList<>();
        for (String line : map.toString().split("\n")) {
            int start = line.indexOf("🆔 ");
            int arrow = line.lastIndexOf(" → ");
            if (start >= 0 && arrow > start) {
                keys.add(line.substring(start + "🆔 ".length(), arrow));
            }
        }
        return keys;
    }

    /**
     * toString() shows each entry's own key, as keySet() reports it. From the raw keys, a and [a] both showed as a,
     * null and [null] as the null sign, a Set of Sets lost its commas ({{a}{b}}), a primitive array showed as its
     * identity ([I@1b6d3586), and in COLLECTIONS_NOT_EXPANDED mode a Set showed as [..] and {} as [].
     */
    @Test
    void toStringShowsEachEntrysOwnKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put(new Object[]{"a"}, "2");
        map.put(null, "3");
        map.put(new Object[]{null}, "4");
        map.put(set(set("a"), set("b")), "5");
        map.put(new int[]{1, 2}, "6");
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "[a]", "∅", "[∅]", "{{a}, {b}}", "[1, 2]")),
                new LinkedHashSet<>(renderedKeys(map)));

        MultiKeyMap<String> flat = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        flat.put(new Object[]{"x", set("a", "b")}, "1");
        assertEquals(Collections.singletonList("[x, {a, b}]"), renderedKeys(flat));

        MultiKeyMap<String> whole = MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build();
        whole.put(set(), "1");
        whole.put(list(), "2");
        whole.put(set("a", "b"), "3");
        assertEquals(new LinkedHashSet<>(Arrays.asList("{}", "[]", "{a, b}")), new LinkedHashSet<>(renderedKeys(whole)));

        for (MultiKeyMap<String> m : Arrays.asList(filled(new MultiKeyMap<>(), shapes()),
                filled(MultiKeyMap.<String>builder().flattenDimensions(true).build(), shapes()))) {
            List<String> rendered = renderedKeys(m);
            assertEquals(m.size(), rendered.size());
            assertEquals(m.size(), new LinkedHashSet<>(rendered).size(), "each entry its own key: " + rendered);
        }
    }
}
