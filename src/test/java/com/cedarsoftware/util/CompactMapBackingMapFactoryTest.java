package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A builder map makes its backing map, when it outgrows its array, from a factory worked out once per generated class
 * instead of its getNewMap(), which resolves the map class and constructor reflectively on every call. The factory
 * must make the same kind of map: the same class, around the same kind of map for a case-insensitive one, and with
 * an equivalent comparator for a sorted one.
 */
class CompactMapBackingMapFactoryTest {

    private static List<Supplier<CompactMap.Builder<String, Object>>> configurations() {
        return Arrays.asList(
                () -> CompactMap.<String, Object>builder(),
                () -> CompactMap.<String, Object>builder().insertionOrder(),
                () -> CompactMap.<String, Object>builder().sortedOrder(),
                () -> CompactMap.<String, Object>builder().reverseOrder(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).insertionOrder(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).reverseOrder(),
                () -> CompactMap.<String, Object>builder().mapType(ConcurrentHashMap.class),
                () -> CompactMap.<String, Object>builder().mapType(ConcurrentSkipListMap.class).sortedOrder(),
                () -> CompactMap.<String, Object>builder().mapType(TreeMap.class),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).mapType(TreeMap.class).noOrder(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).mapType(LinkedHashMap.class));
    }

    @Test
    void theBackingMapIsTheKindGetNewMapMakes() {
        for (Supplier<CompactMap.Builder<String, Object>> configuration : configurations()) {
            CompactMap<String, Object> map = configuration.get().compactSize(2).build();
            map.put("b", 1);
            map.put("a", 2);
            map.put("C", 3);
            assertSame(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
            Map<?, ?> backing = (Map<?, ?>) map.val;
            Map<String, Object> expected = map.getNewMap();
            String what = map.getConfig().toString();
            assertSame(expected.getClass(), backing.getClass(), what);
            if (expected instanceof CaseInsensitiveMap) {
                assertSame(((CaseInsensitiveMap<?, ?>) expected).getWrappedMap().getClass(),
                        ((CaseInsensitiveMap<?, ?>) backing).getWrappedMap().getClass(), what);
            }
            if (expected instanceof SortedMap) {
                Comparator<Object> want = (Comparator<Object>) ((SortedMap<?, ?>) expected).comparator();
                Comparator<Object> got = (Comparator<Object>) ((SortedMap<?, ?>) backing).comparator();
                if (want == null) {
                    assertEquals(null, got, what);
                } else {
                    for (String[] pair : new String[][]{{"a", "B"}, {"a", "A"}, {"b", "a"}, {"Z", "a"}}) {
                        assertEquals(Integer.signum(want.compare(pair[0], pair[1])),
                                Integer.signum(got.compare(pair[0], pair[1])), what + " " + Arrays.toString(pair));
                    }
                }
            }
            assertEquals(3, map.size(), what);
            assertTrue(map.containsKey("a") && map.containsKey("b") && map.containsKey("C"), what);
        }
    }
}
