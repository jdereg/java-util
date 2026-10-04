package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A CompactMap whose backing map refuses nulls -- ConcurrentHashMap, ConcurrentSkipListMap -- accepted a null key or
 * value while it held its entries in an array, then failed when it outgrew the array: the backing map refused the
 * null, that put threw NullPointerException, and the map could grow no further. A null is now refused at once, in
 * every state, as the backing map refuses it: put, get, containsKey, remove for a key, put and containsValue for a
 * value. Maps whose backing map takes nulls are unchanged.
 */
class CompactMapNullPolicyTest {

    private static CompactMap<Object, Object> backedBy(Class<? extends Map> type) {
        CompactMap.Builder<Object, Object> builder = CompactMap.builder().compactSize(3).mapType(type);
        return type == ConcurrentSkipListMap.class ? builder.sortedOrder().build() : builder.build();
    }

    @Test
    void aBackingMapThatRefusesNullsMakesTheCompactMapRefuseThemInEveryState() {
        for (Class<? extends Map> type : new Class[]{ConcurrentHashMap.class, ConcurrentSkipListMap.class}) {
            for (int entries : new int[]{0, 1, 2, 5}) {
                CompactMap<Object, Object> map = backedBy(type);
                for (int i = 0; i < entries; i++) {
                    map.put("k" + i, i);
                }
                String what = type.getSimpleName() + " with " + entries + " entries";
                assertThrows(NullPointerException.class, () -> map.put(null, 1), what);
                assertThrows(NullPointerException.class, () -> map.put("x", null), what);
                assertThrows(NullPointerException.class, () -> map.get(null), what);
                assertThrows(NullPointerException.class, () -> map.containsKey(null), what);
                assertThrows(NullPointerException.class, () -> map.remove(null), what);
                assertThrows(NullPointerException.class, () -> map.containsValue(null), what);
                assertEquals(entries, map.size(), what);
            }
        }
    }

    /** Every way in for a null is refused, and leaves a map that can still grow past its array. */
    @Test
    void everyRouteRefusesTheNullAndLeavesTheMapAbleToGrow() {
        List<Consumer<CompactMap<Object, Object>>> routes = Arrays.asList(
                map -> map.entrySet().iterator().next().setValue(null),
                map -> map.replaceAll((k, v) -> null),
                map -> map.putAll(Collections.singletonMap("n", null)),
                map -> map.putAll(Collections.singletonMap(null, 1)),
                map -> map.putIfAbsent("n", null),
                map -> map.put("k0", null));
        for (Class<? extends Map> type : new Class[]{ConcurrentHashMap.class, ConcurrentSkipListMap.class}) {
            for (int entries = 1; entries <= 3; entries++) {
                for (int route = 0; route < routes.size(); route++) {
                    CompactMap<Object, Object> map = backedBy(type);
                    for (int i = 0; i < entries; i++) {
                        map.put("k" + i, i);
                    }
                    String what = type.getSimpleName() + " with " + entries + " entries, route " + route;
                    Consumer<CompactMap<Object, Object>> nullRoute = routes.get(route);
                    assertThrows(NullPointerException.class, () -> nullRoute.accept(map), what);
                    assertEquals(entries, map.size(), what);
                    assertEquals(0, map.get("k0"), what);
                    for (int i = entries; i < 10; i++) {
                        map.put("k" + i, i);
                    }
                    assertEquals(10, map.size(), what);
                    assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), what);
                }
            }
        }
        Map<Object, Object> source = Collections.singletonMap("n", null);
        assertThrows(NullPointerException.class, () -> CompactMap.builder().mapType(ConcurrentHashMap.class)
                .sourceMap(source).build());
    }

    @Test
    void theMapCanAlwaysGrowPastItsArray() {
        CompactMap<Object, Object> map = backedBy(ConcurrentHashMap.class);
        for (int i = 0; i < 10; i++) {
            map.put("k" + i, i);
        }
        assertEquals(10, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    @Test
    void mapsWhoseBackingMapTakesNullsStillTakeThem() {
        for (CompactMap<Object, Object> map : new CompactMap[]{new CompactMap<>(),
                CompactMap.builder().sortedOrder().build(),
                CompactMap.builder().caseSensitive(false).insertionOrder().build()}) {
            map.put(null, 1);
            map.put("v", null);
            assertEquals(1, map.get(null));
            assertTrue(map.containsKey(null) && map.containsValue(null));
            for (int i = 0; i < 60; i++) {
                map.put("k" + i, i);
            }
            assertEquals(1, map.get(null));
            assertNull(map.get("v"));
            assertTrue(map.containsKey("v"));
        }
    }
}
