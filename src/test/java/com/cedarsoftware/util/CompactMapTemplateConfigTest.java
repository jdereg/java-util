package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for template class generation correctness:
 *
 * Bug 1: generateClassName() omitted INNER_MAP_TYPE from the config hash, so two
 * case-insensitive configs differing only in inner map type shared one template class —
 * and building the second config rewrote the shared class's injected static config,
 * retroactively changing getNewMap() for all existing instances of the first config.
 *
 * Bug 2: getOrCreateTemplateClass() consulted the parent ClassLoader, which can never
 * see classes defined in the private template loader — every build() re-patched
 * bytecode and re-injected static fields instead of reusing the defined class.
 */
class CompactMapTemplateConfigTest {

    @Test
    void testCaseInsensitiveConfigsWithDifferentInnerMapsDoNotShareTemplateClass() {
        CompactMap<String, String> hashBacked = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .compactSize(3)
                .mapType(HashMap.class)
                .build();

        CompactMap<String, String> chmBacked = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .compactSize(3)
                .mapType(ConcurrentHashMap.class)
                .build();

        assertNotSame(hashBacked.getClass(), chmBacked.getClass(),
                "Different inner map types must not share a template class");

        // Each map's config must reflect its own inner map type — before the fix,
        // building the second map rewrote the first map's backing map type.
        assertEquals(HashMap.class, hashBacked.getConfig().get(CompactMap.MAP_TYPE));
        assertEquals(ConcurrentHashMap.class, chmBacked.getConfig().get(CompactMap.MAP_TYPE));

        // Push both past compactSize so they actually construct their backing maps
        for (int i = 0; i < 5; i++) {
            hashBacked.put("Key" + i, "v" + i);
            chmBacked.put("Key" + i, "v" + i);
        }
        assertEquals(5, hashBacked.size());
        assertEquals(5, chmBacked.size());
        assertEquals("v3", hashBacked.get("KEY3"));
        assertEquals("v3", chmBacked.get("KEY3"));
    }

    @Test
    void testIdenticalConfigsReuseTemplateClass() {
        CompactMap<String, String> a = CompactMap.<String, String>builder()
                .caseSensitive(true)
                .compactSize(40)
                .build();
        CompactMap<String, String> b = CompactMap.<String, String>builder()
                .caseSensitive(true)
                .compactSize(40)
                .build();

        assertSame(a.getClass(), b.getClass(), "Identical configs must reuse the template class");
    }

    @Test
    void testNonStringSingleValueKeyDoesNotCrash() {
        // Previously threw ClassCastException from generateClassName()
        CompactMap<Object, String> map = CompactMap.<Object, String>builder()
                .singleValueKey(42)
                .build();

        map.put(42, "answer");
        assertEquals(1, map.size());
        assertEquals("answer", map.get(42));
        map.put("other", "x");
        assertEquals(2, map.size());
        assertTrue(map.containsKey(42));
    }

    @Test
    void testConfigSurvivesInterleavedBuilds() {
        // Build A, force backing map creation, then build B with a different inner type,
        // and verify A's behavior did not change retroactively.
        CompactMap<String, String> a = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .compactSize(2)
                .mapType(HashMap.class)
                .build();
        for (int i = 0; i < 4; i++) {
            a.put("K" + i, "v" + i);
        }
        Map<String, Object> configBefore = a.getConfig();

        CompactMap<String, String> b = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .compactSize(2)
                .mapType(ConcurrentHashMap.class)
                .build();
        for (int i = 0; i < 4; i++) {
            b.put("K" + i, "v" + i);
        }

        assertEquals(configBefore.get(CompactMap.MAP_TYPE), a.getConfig().get(CompactMap.MAP_TYPE),
                "Building a different config must not rewrite an existing map's config");
        assertEquals("v2", a.get("k2"));
        assertEquals("v2", b.get("k2"));
    }
}
