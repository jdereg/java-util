package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.cedarsoftware.io.JsonObject;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * When a Map's names cannot be matched to constructor parameters (a class compiled without
 * {@code -parameters}), newInstance() passes the map's values positionally, in the alphabetical order of
 * their keys -- unless the map is a LinkedHashMap or SortedMap, whose own order is used. Any other map,
 * json-io's JsonObject included, was sorted on every call, and each value then fetched again with get(),
 * which for JsonObject is a linear scan: ~290 of ~370 ns per object with 20 keys. The sorted order is now
 * remembered per class and reused while the keys arrive in the same order, and values are read in one pass.
 */
class ClassUtilitiesPositionalKeyOrderTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @BeforeEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    /** Parameter names that look synthetic: named matching skips it, as for a class built without -parameters. */
    static class Two {
        final String first;
        final String second;

        Two(String arg0, String arg1) {
            this.first = arg0;
            this.second = arg1;
        }
    }

    /** A map in insertion order that is not a LinkedHashMap, like json-io's JsonObject; counts get() calls. */
    static final class InsertionOrderMap extends AbstractMap<String, Object> {
        private final Map<String, Object> inner = new LinkedHashMap<>();
        final AtomicInteger gets = new AtomicInteger();

        InsertionOrderMap with(String key, Object value) {
            inner.put(key, value);
            return this;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return inner.entrySet();
        }

        @Override
        public Object get(Object key) {
            gets.incrementAndGet();
            return inner.get(key);
        }
    }

    /** Reports a size other than what it iterates, as a map being changed while it is read might. */
    static final class WrongSizeMap extends AbstractMap<String, Object> {
        private final Map<String, Object> inner = new LinkedHashMap<>();
        private final int reportedSize;

        WrongSizeMap(int reportedSize) {
            this.reportedSize = reportedSize;
        }

        WrongSizeMap with(String key, Object value) {
            inner.put(key, value);
            return this;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return inner.entrySet();
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }

    private Two make(Map<String, Object> map) {
        return (Two) ClassUtilities.newInstance(converter, Two.class, map);
    }

    @Test
    void valuesFollowTheAlphabeticalOrderOfTheirKeys() {
        for (int i = 0; i < 3; i++) {
            Two t = make(new InsertionOrderMap().with("b", "B" + i).with("a", "A" + i));
            assertEquals("A" + i, t.first, "call " + i);
            assertEquals("B" + i, t.second, "call " + i);
        }
    }

    @Test
    void aDifferentKeyOrderIsSortedAgain() {
        Two t1 = make(new InsertionOrderMap().with("b", "B").with("a", "A"));
        Two t2 = make(new InsertionOrderMap().with("a", "A").with("b", "B"));
        Two t3 = make(new InsertionOrderMap().with("b", "B").with("a", "A"));
        for (Two t : new Two[]{t1, t2, t3}) {
            assertEquals("A", t.first);
            assertEquals("B", t.second);
        }
    }

    @Test
    void differentKeysOfTheSameCountAreNotConfused() {
        Two t1 = make(new InsertionOrderMap().with("b", "B").with("a", "A"));
        assertEquals("A", t1.first);
        // "c" sorts after "b": with the remembered order of [b, a] this would come out reversed
        Two t2 = make(new InsertionOrderMap().with("c", "C").with("b", "B"));
        assertEquals("B", t2.first);
        assertEquals("C", t2.second);
    }

    @Test
    void valuesAreReadInOnePassNotLookedUpByKey() {
        InsertionOrderMap map = new InsertionOrderMap().with("b", "B").with("a", "A");
        make(map);
        make(map);
        assertEquals(0, map.gets.get(), "get() per key made every object cost a scan per key in JsonObject");
    }

    @Test
    void aMapWhoseSizeDisagreesWithItsContentsStillWorks() {
        Two tooSmall = make(new WrongSizeMap(1).with("b", "B").with("a", "A"));
        assertEquals("A", tooSmall.first);
        assertEquals("B", tooSmall.second);

        Two tooLarge = make(new WrongSizeMap(3).with("b", "B").with("a", "A"));
        assertEquals("A", tooLarge.first);
        assertEquals("B", tooLarge.second);
    }

    @Test
    void aLinkedHashMapKeepsItsOwnOrder() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", "B");
        map.put("a", "A");
        Two t = make(map);
        assertEquals("B", t.first, "a LinkedHashMap's insertion order is used as is");
        assertEquals("A", t.second);
    }

    /** Up to 64 keys and beyond take different sorts: both must agree with Collections.sort. */
    @Test
    void manyKeysInAnyOrderAreSortedAsCollectionsSortWould() {
        for (int count : new int[]{5, 30, 64, 65, 200}) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                keys.add("k" + i);          // "k10" sorts before "k2": text order, not numeric
            }
            java.util.Collections.shuffle(keys, new java.util.Random(count));
            InsertionOrderMap map = new InsertionOrderMap();
            for (String key : keys) {
                map.with(key, "v_" + key);
            }
            java.util.List<String> sorted = new java.util.ArrayList<>(keys);
            java.util.Collections.sort(sorted);

            for (int call = 0; call < 2; call++) {
                Two t = make(map);
                assertEquals("v_" + sorted.get(0), t.first, count + " keys, call " + call);
                assertEquals("v_" + sorted.get(1), t.second, count + " keys, call " + call);
            }
        }
    }

    /**
     * json-io's parser appends fields without looking for duplicates, so a document that repeats a key gives a
     * JsonObject holding it twice. The values passed have always been get(key) for each key in sorted order --
     * so the first value, twice. Reading values in one pass must not change what such a map produces.
     */
    @Test
    @SuppressWarnings("unchecked")
    void aJsonObjectWithARepeatedKeyGivesWhatGetGave() {
        for (int call = 0; call < 2; call++) {
            JsonObject obj = new JsonObject();
            obj.appendFieldForParser("a", "A1");
            obj.appendFieldForParser("a", "A2");
            obj.appendFieldForParser("b", "B");
            Two t = make((Map<String, Object>) (Map<?, ?>) obj);
            assertEquals("A1", t.first, "call " + call);
            assertEquals("A1", t.second, "call " + call);
        }
    }

    /** The same past 64 keys, where a different sort is used; and a repeat that is not the first key. */
    @Test
    @SuppressWarnings("unchecked")
    void aRepeatedKeyAmongManyGivesWhatGetGave() {
        for (int count : new int[]{10, 64, 100}) {
            JsonObject obj = new JsonObject();
            for (int i = count - 1; i >= 0; i--) {
                obj.appendFieldForParser(String.format("k%03d", i), "v" + i);
            }
            obj.appendFieldForParser("k000", "repeat");
            Two t = make((Map<String, Object>) (Map<?, ?>) obj);
            // Which of the two get() returns is JsonObject's business (past 4 entries its index keeps the last)
            Object viaGet = obj.get("k000");
            assertEquals(viaGet, t.first, count + " keys");
            assertEquals(viaGet, t.second, count + " keys: both k000 slots take what get() gives");
        }
    }
}
