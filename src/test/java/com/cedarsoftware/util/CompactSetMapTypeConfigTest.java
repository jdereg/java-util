package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CompactSet.getConfig() reports the backing map type, as CompactMap.getConfig() does. It left it out, so a reader
 * of the config (json-io writing a CompactSet) could not keep it: a set built on ConcurrentHashMap was read back on
 * HashMap, no longer thread-safe.
 */
class CompactSetMapTypeConfigTest {

    @Test
    void theConfigNamesTheBackingMap() {
        assertEquals(ConcurrentHashMap.class, CompactSet.<String>builder().noOrder().mapType(ConcurrentHashMap.class)
                .build().getConfig().get(CompactMap.MAP_TYPE));
        assertEquals(ConcurrentSkipListMap.class, CompactSet.<String>builder().sortedOrder()
                .mapType(ConcurrentSkipListMap.class).build().getConfig().get(CompactMap.MAP_TYPE));
        assertEquals(HashMap.class, new CompactSet<String>().getConfig().get(CompactMap.MAP_TYPE));
        assertEquals(TreeMap.class, CompactSet.<String>builder().sortedOrder().build().getConfig()
                .get(CompactMap.MAP_TYPE));
        assertEquals(LinkedHashMap.class, CompactSet.<String>builder().insertionOrder().build().getConfig()
                .get(CompactMap.MAP_TYPE));
    }

    /** withConfig keeps the backing map, as before; the config it is given now names it too. */
    @Test
    void withConfigKeepsTheBackingMap() {
        CompactSet<String> concurrent = CompactSet.<String>builder().noOrder().mapType(ConcurrentHashMap.class)
                .build();
        concurrent.add("a");
        CompactSet<String> copy = concurrent.withConfig(concurrent.getConfig());
        assertEquals(ConcurrentHashMap.class, copy.getConfig().get(CompactMap.MAP_TYPE));
        assertEquals(concurrent, copy);
    }
}
