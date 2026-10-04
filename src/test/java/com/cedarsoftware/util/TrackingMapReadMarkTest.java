package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which keys TrackingMap records as read: random reads, misses, re-reads, writes and removes against a model (the
 * keys read since each was last removed), on five kinds of wrapped map; and, on a concurrent map, reads from
 * several threads at once.
 */
class TrackingMapReadMarkTest {

    private static final String[] KEYS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    @Test
    void readsAreRecordedAsTheModelSays() {
        List<Supplier<Map<String, Integer>>> kinds = Arrays.asList(HashMap::new, TreeMap::new,
                CaseInsensitiveMap::new, ConcurrentHashMap::new, ConcurrentHashMapNullSafe::new);
        for (Supplier<Map<String, Integer>> kind : kinds) {
            for (int seed = 1; seed <= 20; seed++) {
                Random random = new Random(seed);
                TrackingMap<String, Integer> tracking = new TrackingMap<>(kind.get());
                Set<Object> read = new HashSet<>();
                for (int step = 0; step < 300; step++) {
                    String key = KEYS[random.nextInt(KEYS.length)];
                    switch (random.nextInt(6)) {
                        case 0:
                            tracking.put(key, step);            // a write is not a read
                            break;
                        case 1:
                            tracking.remove(key);               // forgets the read
                            read.remove(key);
                            break;
                        case 2:
                            tracking.containsKey(key);          // reads, hit or miss
                            read.add(key);
                            break;
                        case 3:
                            tracking.getOrDefault(key, -1);
                            read.add(key);
                            break;
                        default:
                            tracking.get(key);
                            read.add(key);
                            break;
                    }
                    assertEquals(read, new HashSet<>(tracking.keysUsed()),
                            tracking.getWrappedMap().getClass().getSimpleName() + " seed " + seed + " step " + step);
                }
            }
        }
    }

    @Test
    void readsFromManyThreadsAreAllRecorded() throws Exception {
        for (int round = 0; round < 20; round++) {
            Map<String, Integer> map = new ConcurrentHashMap<>();
            for (int i = 0; i < 1000; i += 2) {
                map.put("k" + i, i);                            // half the keys read will be misses
            }
            TrackingMap<String, Integer> tracking = new TrackingMap<>(map);
            List<Thread> threads = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                int from = t * 200;
                threads.add(new Thread(() -> {
                    for (int pass = 0; pass < 3; pass++) {
                        for (int i = from; i < from + 400; i++) {   // ranges overlap: keys read by two threads
                            tracking.get("k" + i);
                        }
                    }
                }));
            }
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            Set<Object> expected = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                expected.add("k" + i);
            }
            assertEquals(expected, new HashSet<>(tracking.keysUsed()), "round " + round);
        }
    }
}
