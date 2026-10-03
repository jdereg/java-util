package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ConcurrentMap methods the null-safe maps do not inherit from their backing map: putIfAbsent, forEach and
 * replaceAll, on ConcurrentHashMapNullSafe and ConcurrentNavigableMapNullSafe.
 */
class ConcurrentNullSafeMapDefaultsTest {

    private static final String[] KEYS = {null, "a", "b", "c", "d"};
    private static final String[] VALUES = {null, "1", "2", "3"};

    /**
     * Another write sets the key to null while replaceAll is computing its new value. The key is still mapped
     * (to null), so replaceAll must apply the function to null, as it would had the write come first. The
     * inherited ConcurrentMap.replaceAll read the null as "key removed" and skipped it.
     */
    @Test
    void replaceAllAppliesTheFunctionToAValueThatBecameNull() {
        List<ConcurrentMap<String, String>> maps = new ArrayList<>();
        maps.add(new ConcurrentHashMapNullSafe<>());
        maps.add(new ConcurrentNavigableMapNullSafe<>());
        for (ConcurrentMap<String, String> map : maps) {
            map.put("k", "x");
            AtomicInteger calls = new AtomicInteger();
            map.replaceAll((k, v) -> {
                if (calls.getAndIncrement() == 0) {
                    map.put(k, null);           // a concurrent writer, between the read and the replace
                }
                return v + "!";
            });
            String name = map.getClass().getSimpleName();
            assertTrue(map.containsKey("k"), name);
            assertEquals("null!", map.get("k"), name);
        }
    }

    /**
     * Random puts, putIfAbsent, removes, replaceAll and forEach, null keys and values included, against a
     * HashMap (and, for the navigable map, a TreeMap ordering null last): the same return values, contents and,
     * where ordered, iteration order after every step.
     */
    @Test
    void putIfAbsentForEachAndReplaceAllAnswerAsTheReferenceMapDoes() {
        Comparator<String> nullsLast = Comparator.nullsLast(Comparator.<String>naturalOrder());
        for (int seed = 1; seed <= 20; seed++) {
            run(new ConcurrentHashMapNullSafe<>(), new HashMap<>(), false, new Random(seed));
            run(new ConcurrentNavigableMapNullSafe<>(), new TreeMap<>(nullsLast), true, new Random(seed));
        }
    }

    private static void run(Map<String, String> map, Map<String, String> reference, boolean ordered,
                            Random random) {
        for (int step = 0; step < 500; step++) {
            String key = KEYS[random.nextInt(KEYS.length)];
            String value = VALUES[random.nextInt(VALUES.length)];
            String where = map.getClass().getSimpleName() + " step " + step;
            switch (random.nextInt(5)) {
                case 0:
                    assertEquals(reference.put(key, value), map.put(key, value), where + " put");
                    break;
                case 1:
                case 2:
                    assertEquals(reference.putIfAbsent(key, value), map.putIfAbsent(key, value),
                            where + " putIfAbsent(" + key + ", " + value + ")");
                    break;
                case 3:
                    assertEquals(reference.remove(key), map.remove(key), where + " remove");
                    break;
                default:
                    String suffix = value;
                    reference.replaceAll((k, v) -> v == null ? suffix : v + suffix);
                    map.replaceAll((k, v) -> v == null ? suffix : v + suffix);
                    break;
            }
            List<Object> expected = new ArrayList<>();
            reference.forEach((k, v) -> {
                expected.add(k);
                expected.add(v);
            });
            List<Object> actual = new ArrayList<>();
            map.forEach((k, v) -> {
                actual.add(k);
                actual.add(v);
            });
            if (ordered) {
                assertEquals(expected, actual, where + " forEach");
            } else {
                assertEquals(pairs(expected), pairs(actual), where + " forEach");
            }
            assertEquals(reference, map, where);
        }
    }

    private static Map<Object, Object> pairs(List<Object> flat) {
        Map<Object, Object> pairs = new HashMap<>();
        for (int i = 0; i < flat.size(); i += 2) {
            assertTrue(!pairs.containsKey(flat.get(i)), "forEach visited " + flat.get(i) + " twice");
            pairs.put(flat.get(i), flat.get(i + 1));
        }
        return pairs;
    }
}
