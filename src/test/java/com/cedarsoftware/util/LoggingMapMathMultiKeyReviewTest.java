package com.cedarsoftware.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the LoggingConfig / MapUtilities / MultiKeyMap review fixes:
 *
 * 1. MultiKeyMap.resizeInternal zeroed atomicSize and re-incremented it per entry while
 *    rehashing. size()/isEmpty()/longSize() are lock-free, so concurrent readers observed
 *    the size transiently collapse toward 0 mid-resize (isEmpty() == true on a populated
 *    map). The entry count is invariant during resize; the counter is no longer touched.
 *
 * 2. MapUtilities.mapOf/mapOfEntries javadoc claimed NullPointerException for null
 *    keys/values, but LinkedHashMap semantics permit them — behavior pinned, docs fixed.
 *
 * 3. LoggingConfig.UniformFormatter replaced per-record String.format("%-5s", level)
 *    with manual padding — output format pinned here.
 */
class LoggingMapMathMultiKeyReviewTest {

    // ---------------------------------------------------------------
    // 1. MultiKeyMap — size must never regress during resize
    // ---------------------------------------------------------------

    @Test
    void testSizeNeverRegressesDuringResize() throws Exception {
        MultiKeyMap<Integer> map = new MultiKeyMap<>(16);
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger violations = new AtomicInteger(0);

        // Insert-only workload: size is monotonically non-decreasing by definition.
        // Before the fix, each resize reset atomicSize to 0 and counted back up,
        // so a lock-free reader saw the size collapse toward 0 mid-resize.
        Thread reader = new Thread(() -> {
            long lastSeen = 0;
            while (!stop.get()) {
                long s = map.longSize();
                if (s < lastSeen) {
                    violations.incrementAndGet();
                }
                if (s > lastSeen) {
                    lastSeen = s;
                }
            }
        });
        reader.setDaemon(true);
        reader.start();

        for (int i = 0; i < 200_000; i++) {
            map.put("key" + i, i);
        }

        stop.set(true);
        reader.join(5000);

        assertEquals(0, violations.get(),
                "size()/longSize() must never regress during resize under an insert-only workload");
        assertEquals(200_000, map.size());
        assertTrue(map.containsKey("key0"));
        assertTrue(map.containsKey("key199999"));
    }

    // ---------------------------------------------------------------
    // 2. MapUtilities — null tolerance and immutability pinned
    // ---------------------------------------------------------------

    @Test
    void testMapOfToleratesNullKeysAndValues() {
        Map<String, String> m = MapUtilities.mapOf("a", null, null, "b");
        assertEquals(2, m.size());
        assertNull(m.get("a"));
        assertEquals("b", m.get(null));
        assertThrows(UnsupportedOperationException.class, () -> m.put("x", "y"));
    }

    @Test
    void testMapOfEntriesToleratesNullKeyAndValueButNotNullEntry() {
        Map<String, String> m = MapUtilities.mapOfEntries(
                new java.util.AbstractMap.SimpleEntry<>("a", (String) null),
                new java.util.AbstractMap.SimpleEntry<>((String) null, "b"));
        assertEquals(2, m.size());
        assertNull(m.get("a"));
        assertEquals("b", m.get(null));

        assertThrows(NullPointerException.class,
                () -> MapUtilities.mapOfEntries(new java.util.AbstractMap.SimpleEntry<>("a", "1"), null));
    }

    // ---------------------------------------------------------------
    // 3. LoggingConfig — UniformFormatter level padding preserved
    // ---------------------------------------------------------------

    @Test
    void testUniformFormatterLevelPaddingUnchanged() {
        LoggingConfig.UniformFormatter formatter = new LoggingConfig.UniformFormatter("yyyy-MM-dd");

        LogRecord info = new LogRecord(Level.INFO, "hello");
        info.setLoggerName("my.Logger");
        String formatted = formatter.format(info);
        // "INFO" (4 chars) pads to 5, then the single separator space: "INFO  my.Logger - hello"
        assertTrue(formatted.contains("INFO  my.Logger - hello"), () -> "was: " + formatted);

        LogRecord warning = new LogRecord(Level.WARNING, "careful");
        warning.setLoggerName("my.Logger");
        String formattedWarn = formatter.format(warning);
        // "WARNING" (7 chars) exceeds the pad width — no extra padding, single separator space
        assertTrue(formattedWarn.contains("WARNING my.Logger - careful"), () -> "was: " + formattedWarn);
    }
}
