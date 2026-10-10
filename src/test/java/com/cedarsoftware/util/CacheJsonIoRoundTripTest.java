package com.cedarsoftware.util;

import com.cedarsoftware.io.JsonIo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * An {@link LRUCache} and a {@link TTLCache}, populated, written by json-io and read back, still working as caches:
 * the LRUCache with its entries and room for each (at least 16); the TTLCache empty, configured as it was -- json-io
 * writes a TTLCache as its settings and no entries, as Guava and Caffeine serialize their caches. No json-io release
 * before 4.113.0 could read either back.
 * <p>
 * java-util's tests use the previous json-io release, so these run once that is json-io 4.113.0 or later
 * ({@code version.json-io} in the pom); until then they are skipped. json-io's own CacheRoundTripTest covers the
 * same ground against the java-util it is built with.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class CacheJsonIoRoundTripTest {

    private static void assumeJsonIoReadsCachesBack() {
        assumeTrue(JsonIoOnTestClassPath.isAtLeast(4, 113),
                "json-io " + JsonIoOnTestClassPath.version() + " on the test class path predates 4.113.0");
    }

    @Test
    void anLruCacheReadsBackWithItsEntriesAndWorks() {
        assumeJsonIoReadsCachesBack();
        LRUCache<String, Integer> lru = new LRUCache<>(10);
        lru.put("a", 1);
        lru.put("b", 2);
        lru.put("c", 3);

        LRUCache<?, ?> back = JsonIo.toJava(JsonIo.toJson(lru, null), null).asClass(null);
        assertInstanceOf(LRUCache.class, back);
        assertEquals(lru, back);
        assertEquals(16, back.getCapacity());

        @SuppressWarnings("unchecked")
        LRUCache<String, Integer> cache = (LRUCache<String, Integer>) back;
        for (int i = 0; i < 40; i++) {
            cache.put("more" + i, i);
        }
        assertEquals(16, cache.size());                     // full, and evicting
        assertEquals(39, cache.get("more39"));
    }

    @Test
    void aTtlCacheReadsBackEmptyConfiguredAsItWas() {
        assumeJsonIoReadsCachesBack();
        TTLCache<String, String> ttl = new TTLCache<>(5000, 10, 250);
        ttl.put("a", "1");
        TTLCache<?, ?> back = null;
        try {
            String json = JsonIo.toJson(ttl, null);
            assertTrue(json.contains("\"config\":\"5000/10/250\""), json);
            back = JsonIo.toJava(json, null).asClass(null);
            assertInstanceOf(TTLCache.class, back);
            assertTrue(back.isEmpty());
            assertEquals(5000, back.getTtlMillis());
            assertEquals(10, back.getMaxSize());
            assertEquals(250, back.getCleanupIntervalMillis());
        } finally {
            ttl.close();
            if (back != null) {
                back.close();
            }
        }
    }

    @Test
    void theVersionGateReadsVersions() {
        assertTrue(JsonIoOnTestClassPath.isAtLeast("4.113.0", 4, 113));
        assertTrue(JsonIoOnTestClassPath.isAtLeast("4.120.1-SNAPSHOT", 4, 113));
        assertTrue(JsonIoOnTestClassPath.isAtLeast("5.0.0", 4, 113));
        assertTrue(!JsonIoOnTestClassPath.isAtLeast("4.112.0", 4, 113));
        assertTrue(!JsonIoOnTestClassPath.isAtLeast(null, 4, 113));
    }
}
