package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * build() remembers the generated class for each configuration as written, so a repeated build() skips the
 * validation and the SHA-1 class name that took most of its time. What it hands back must not change: every
 * option still decides the map it builds, with the cache warm, from a builder reused after build(), and with a
 * source map, whose ordering is still checked.
 */
class CompactMapBuildCacheTest {

    private static void assertConfig(CompactMap<String, Object> map, int compactSize, boolean caseSensitive,
                                     String ordering, String singleKey) {
        Map<String, Object> config = map.getConfig();
        assertEquals(compactSize, config.get(CompactMap.COMPACT_SIZE));
        assertEquals(caseSensitive, config.get(CompactMap.CASE_SENSITIVE));
        assertEquals(ordering, config.get(CompactMap.ORDERING));
        assertEquals(singleKey, config.get(CompactMap.SINGLE_KEY));
    }

    @Test
    void everyOptionStillDecidesTheMapWithTheCacheWarm() {
        for (int pass = 0; pass < 3; pass++) {
            assertConfig(CompactMap.<String, Object>builder().build(), 50, true, CompactMap.UNORDERED, "id");
            assertConfig(CompactMap.<String, Object>builder().compactSize(7).build(),
                    7, true, CompactMap.UNORDERED, "id");
            assertConfig(CompactMap.<String, Object>builder().caseSensitive(false).build(),
                    50, false, CompactMap.UNORDERED, "id");
            assertConfig(CompactMap.<String, Object>builder().insertionOrder().build(),
                    50, true, CompactMap.INSERTION, "id");
            assertConfig(CompactMap.<String, Object>builder().sortedOrder().build(), 50, true, CompactMap.SORTED, "id");
            assertConfig(CompactMap.<String, Object>builder().reverseOrder().build(),
                    50, true, CompactMap.REVERSE, "id");
            assertConfig(CompactMap.<String, Object>builder().singleValueKey("uuid").build(),
                    50, true, CompactMap.UNORDERED, "uuid");
            assertSame(TreeMap.class, CompactMap.<String, Object>builder().mapType(TreeMap.class).build()
                    .getConfig().get(CompactMap.MAP_TYPE));
            assertSame(LinkedHashMap.class, CompactMap.<String, Object>builder().mapType(LinkedHashMap.class).build()
                    .getConfig().get(CompactMap.MAP_TYPE));
        }
        // and the maps behave as configured
        CompactMap<String, Object> ci = CompactMap.<String, Object>builder().caseSensitive(false).build();
        ci.put("Key", 1);
        assertEquals(1, ci.get("KEY"));
        CompactMap<String, Object> reverse = CompactMap.<String, Object>builder().reverseOrder().build();
        reverse.put("a", 1);
        reverse.put("c", 3);
        reverse.put("b", 2);
        assertEquals(java.util.Arrays.asList("c", "b", "a"), new ArrayList<>(reverse.keySet()));
    }

    @Test
    void aBuilderReusedAfterBuildBuildsItsConfigurationAgain() {
        CompactMap.Builder<String, Object> builder = CompactMap.<String, Object>builder().caseSensitive(false)
                .insertionOrder().compactSize(9);
        for (int i = 0; i < 3; i++) {
            assertConfig(builder.build(), 9, false, CompactMap.INSERTION, "id");
        }
    }

    @Test
    void aSourceMapIsStillCheckedAndCopied() {
        Supplier<CompactMap.Builder<String, Object>> sorted = () -> CompactMap.<String, Object>builder().sortedOrder();
        sorted.get().build();                                   // the configuration is cached
        Map<String, Object> insertionOrdered = new LinkedHashMap<>();
        insertionOrdered.put("b", 2);
        insertionOrdered.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> CompactMap.<String, Object>builder().reverseOrder()
                .sourceMap(new TreeMap<>(insertionOrdered)).build());
        CompactMap<String, Object> copy = sorted.get().sourceMap(new TreeMap<>(insertionOrdered)).build();
        assertEquals(java.util.Arrays.asList("a", "b"), new ArrayList<>(copy.keySet()));
    }
}
