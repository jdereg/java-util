package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * getConfig(), isDefaultCompactMap() and withConfig() learn a map's backing map type from a throwaway backing map. A
 * builder map's getNewMap() resolves the map class and constructor reflectively on every call (0.2-0.9 us), and json-io
 * asks isDefaultCompactMap() and getConfig() of every CompactMap it writes; they now make that map with the generated
 * class's cached factory. They must answer exactly as getNewMap() implies, for every configuration.
 */
class CompactMapConfigReportTest {

    private static List<Supplier<CompactMap<String, Object>>> maps() {
        return Arrays.asList(
                () -> CompactMap.<String, Object>builder().build(),
                () -> CompactMap.<String, Object>builder().insertionOrder().build(),
                () -> CompactMap.<String, Object>builder().sortedOrder().build(),
                () -> CompactMap.<String, Object>builder().reverseOrder().build(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).build(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).insertionOrder().build(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().build(),
                () -> CompactMap.<String, Object>builder().mapType(ConcurrentHashMap.class).build(),
                () -> CompactMap.<String, Object>builder().mapType(ConcurrentSkipListMap.class).sortedOrder().build(),
                () -> CompactMap.<String, Object>builder().mapType(TreeMap.class).build(),
                () -> CompactMap.<String, Object>builder().caseSensitive(false).mapType(LinkedHashMap.class).build(),
                () -> CompactMap.<String, Object>builder().compactSize(7).singleValueKey("key").build(),
                CompactMap::new,
                CompactLinkedMap::new,
                CompactCILinkedMap::new);
    }

    private static Class<?> backingType(CompactMap<String, Object> map) {
        Map<String, Object> made = map.getNewMap();
        return made instanceof CaseInsensitiveMap ? ((CaseInsensitiveMap<?, ?>) made).getWrappedMap().getClass()
                : made.getClass();
    }

    @Test
    void theConfigurationIsReportedAsGetNewMapImplies() {
        for (Supplier<CompactMap<String, Object>> supplier : maps()) {
            CompactMap<String, Object> map = supplier.get();
            Map<String, Object> config = map.getConfig();
            String what = map.getClass().getName() + " " + config;
            assertSame(backingType(map), config.get(CompactMap.MAP_TYPE), what);

            boolean isDefault = map.compactSize() == CompactMap.DEFAULT_COMPACT_SIZE && !map.isCaseInsensitive()
                    && CompactMap.UNORDERED.equals(map.getOrdering())
                    && CompactMap.DEFAULT_SINGLE_KEY.equals(map.getSingleValueKey())
                    && map.getNewMap().getClass() == HashMap.class;
            assertEquals(isDefault, map.isDefaultCompactMap(), what);

            map.put("b", 1);
            map.put("a", 2);
            CompactMap<String, Object> copy = map.withConfig(Collections.<String, Object>emptyMap());
            assertEquals(config, copy.getConfig(), what);
            assertEquals(map, copy, what);
        }
    }
}
