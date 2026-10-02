package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * json-io reads a CompactMap's configuration from the document it parses, and each configuration was a generated
 * class that is never unloaded, with no bound on the compact size: a document could fill the JVM with classes, or
 * make every insert copy and scan millions of entries. The compact size is now clipped to a maximum (1,000 by
 * default), and past a maximum number of configurations (1,000) a new one keeps what decides its behavior -- case
 * sensitivity, ordering, map type -- and takes the default storage. Neither throws: each is logged once.
 */
class CompactMapLimitsTest {

    private final List<LogRecord> records = new ArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private final Logger logger = Logger.getLogger(CompactMap.class.getName());

    @BeforeEach
    void listen() {
        CompactMap.resetLimitLogs();
        logger.addHandler(handler);
    }

    @AfterEach
    void stopListening() {
        logger.removeHandler(handler);
        System.clearProperty(CompactMap.MAX_COMPACT_SIZE_PROPERTY);
        System.clearProperty(CompactMap.MAX_CONFIGURATIONS_PROPERTY);
    }

    private int infoRecordsMentioning(String text) {
        int n = 0;
        for (LogRecord record : records) {
            if (record.getLevel() == Level.INFO && record.getMessage().contains(text)) {
                n++;
            }
        }
        return n;
    }

    @Test
    void aCompactSizeAboveTheMaximumIsClippedAndLoggedOnce() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().compactSize(5000).build();
        assertEquals(1000, map.getConfig().get(CompactMap.COMPACT_SIZE));
        for (int i = 0; i <= 1000; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());     // 1,001 entries
        assertEquals(1001, map.size());

        CompactMap<String, Object> another = CompactMap.<String, Object>builder().compactSize(Integer.MAX_VALUE)
                .caseSensitive(false).build();
        assertEquals(1000, another.getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(1, infoRecordsMentioning(CompactMap.MAX_COMPACT_SIZE_PROPERTY));
    }

    @Test
    void theMaximumCompactSizeIsASystemProperty() {
        System.setProperty(CompactMap.MAX_COMPACT_SIZE_PROPERTY, "300");
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().compactSize(400)
                .singleValueKey("limitsTestA").build();          // a configuration of its own: validated afresh
        assertEquals(300, map.getConfig().get(CompactMap.COMPACT_SIZE));
        CompactMap<String, Object> within = CompactMap.<String, Object>builder().compactSize(200)
                .singleValueKey("limitsTestA").build();
        assertEquals(200, within.getConfig().get(CompactMap.COMPACT_SIZE));
    }

    @Test
    void pastTheMaximumOfConfigurationsANewOneKeepsItsBehaviorAndTakesTheDefaultStorage() {
        int existing = CompactMap.generatedConfigurationCount();
        System.setProperty(CompactMap.MAX_CONFIGURATIONS_PROPERTY, String.valueOf(existing + 2));
        List<CompactMap<String, Object>> maps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            maps.add(CompactMap.<String, Object>builder().caseSensitive(false).reverseOrder().compactSize(7 + i)
                    .singleValueKey("limitsTestB" + i).build());
        }
        for (int i = 0; i < 5; i++) {
            Map<String, Object> config = maps.get(i).getConfig();
            assertEquals(false, config.get(CompactMap.CASE_SENSITIVE));
            assertEquals(CompactMap.REVERSE, config.get(CompactMap.ORDERING));
            if (i < 2) {
                assertEquals(7 + i, config.get(CompactMap.COMPACT_SIZE));
                assertEquals("limitsTestB" + i, config.get(CompactMap.SINGLE_KEY));
            } else {
                assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
                assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
            }
            CompactMap<String, Object> map = maps.get(i);
            map.put("Beta", 2);
            map.put("alpha", 1);
            map.put("GAMMA", 3);
            assertEquals(1, map.get("ALPHA"));
            assertEquals("GAMMA", map.keySet().iterator().next());
        }
        assertTrue(CompactMap.generatedConfigurationCount() <= existing + 3);   // the cap, and the shared default
        assertEquals(1, infoRecordsMentioning(CompactMap.MAX_CONFIGURATIONS_PROPERTY));
        assertFalse(records.isEmpty());
    }
}
