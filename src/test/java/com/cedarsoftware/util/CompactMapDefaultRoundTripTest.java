package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cedarsoftware.io.JsonIo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A builder map in the default configuration -- {@code CompactMap.builder().build()} -- said it was a default
 * CompactMap, so json-io wrote it as an ordinary object of its class, under the generated class's name
 * ({@code CompactMap$d1b04c6f91811170}), which no reader can load: the document could not be read back. A builder map is
 * no longer a "default CompactMap", so json-io writes it with its configuration, as it writes every other builder map.
 * A CompactSet still asks only its map's configuration: the set is written under its own class's name.
 */
class CompactMapDefaultRoundTripTest {

    private static Object roundTrip(Object o) {
        return JsonIo.toJava(JsonIo.toJson(o, null), null).asClass(Object.class);
    }

    @Test
    void onlyAPlainCompactMapIsADefaultCompactMap() {
        assertTrue(new CompactMap<String, Object>().isDefaultCompactMap());
        assertFalse(CompactMap.<String, Object>builder().build().isDefaultCompactMap());
        assertTrue(new CompactSet<String>().isDefaultCompactSet());
        assertTrue(CompactSet.<String>builder().build().isDefaultCompactSet());
    }

    @Test
    void aDefaultBuilderMapReadsBack() {
        for (int entries : new int[]{0, 1, 3, 60}) {
            Map<String, Object> map = CompactMap.<String, Object>builder().build();
            for (int i = 0; i < entries; i++) {
                map.put("k" + i, i);
            }
            Object back = roundTrip(map);
            assertTrue(back instanceof CompactMap, entries + " entries: " + back.getClass());
            assertEquals(map, back, entries + " entries");
            assertEquals(((CompactMap<?, ?>) map).getConfig(), ((CompactMap<?, ?>) back).getConfig());
        }
        Map<String, Object> single = CompactMap.<String, Object>builder().build();
        single.put("id", 42L);                              // stored alone, under the single-value key
        assertEquals(single, roundTrip(single));
    }

    @Test
    void plainMapsAndSetsStillReadBack() {
        Map<String, Object> plain = new CompactMap<>();
        plain.put("a", 1L);
        assertEquals(plain, roundTrip(plain));

        for (Set<String> set : Arrays.<Set<String>>asList(new CompactSet<>(), CompactSet.<String>builder().build())) {
            set.addAll(Arrays.asList("x", "y"));
            Object back = roundTrip(set);
            assertEquals(new HashSet<>(set), back);
        }
    }
}
