package com.cedarsoftware.util;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * getConfig() reported the library default compact size (50) for every legacy subclass, whatever its compactSize()
 * said, so json-io wrote 50 for a subclass of 80 and read it back as a map that leaves its array at 51 entries. It
 * now reports the subclass's own compact size, the one the map moves to its backing map at.
 */
class CompactMapLegacyCompactSizeTest {

    static class Compact80<K, V> extends CompactMap<K, V> {
        @Override
        protected int compactSize() {
            return 80;
        }
    }

    @Test
    void aLegacySubclassReportsItsOwnCompactSize() {
        Compact80<String, Object> map = new Compact80<>();
        assertEquals(80, map.getConfig().get(CompactMap.COMPACT_SIZE));
        for (int i = 0; i < 80; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());     // what it reports, it does
        map.put("k80", 80);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    @Test
    void aCopyMadeFromItsConfigurationHasThatCompactSize() {
        Compact80<String, Object> map = new Compact80<>();
        map.put("a", 1);
        Map<String, Object> config = map.getConfig();
        CompactMap<String, Object> copy = CompactMap.<String, Object>builder()
                .compactSize((Integer) config.get(CompactMap.COMPACT_SIZE)).build();
        assertEquals(80, copy.getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(80, map.withConfig(Collections.<String, Object>emptyMap()).getConfig().get(CompactMap.COMPACT_SIZE));
    }

    @Test
    void theLibrarysOwnSubclassesStillReportTheDefault() {
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, new CompactMap<String, Object>().getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, new CompactLinkedMap<String, Object>().getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, new CompactCILinkedMap<String, Object>().getConfig().get(CompactMap.COMPACT_SIZE));
    }
}
