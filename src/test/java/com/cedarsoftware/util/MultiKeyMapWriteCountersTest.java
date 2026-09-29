package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MultiKeyMap's writes no longer update counters every writer shares, and the cached hashCode() and size() stay right.
 * Every insert and remove incremented one AtomicLong for the size, and every write incremented a version counter and
 * made two volatile stores for the cached hash: writes on more threads got slower (replace-put, 8 threads: 6.5 Mops/s
 * against 60 on one; now 72). The size is a LongAdder, and the version is bumped only once hashCode() has been asked
 * for. The compute paths also cleared the cached hash without bumping the version; they rely on the write they make.
 */
class MultiKeyMapWriteCountersTest {

    private static int freshHash(MultiKeyMap<String> map) {
        MultiKeyMap<String> copy = new MultiKeyMap<>();
        copy.putAll(map);
        return copy.hashCode();
    }

    private static void assertChangesTheHash(String what, Consumer<MultiKeyMap<String>> write) {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put(new Object[]{"b", "c"}, "2");
        int before = map.hashCode();                            // cached
        write.accept(map);
        assertNotEquals(before, map.hashCode(), what + " left the cached hash in place");
        assertEquals(freshHash(map), map.hashCode(), what);
    }

    @Test
    void everyKindOfWriteChangesACachedHash() {
        assertChangesTheHash("put of a new key", m -> m.put("z", "9"));
        assertChangesTheHash("put replacing a value", m -> m.put("a", "changed"));
        assertChangesTheHash("remove", m -> m.remove("a"));
        assertChangesTheHash("computeIfAbsent", m -> m.computeIfAbsent("z", k -> "9"));
        assertChangesTheHash("computeIfPresent", m -> m.computeIfPresent("a", (k, v) -> v + "!"));
        assertChangesTheHash("computeIfPresent removing", m -> m.computeIfPresent("a", (k, v) -> null));
        assertChangesTheHash("compute", m -> m.compute("a", (k, v) -> "c"));
        assertChangesTheHash("compute removing", m -> m.compute("a", (k, v) -> null));
        assertChangesTheHash("merge", m -> m.merge("a", "x", (o, n) -> o + n));
        assertChangesTheHash("merge removing", m -> m.merge("a", "x", (o, n) -> null));
        assertChangesTheHash("putIfAbsent", m -> m.putIfAbsent("z", "9"));
        assertChangesTheHash("replace", m -> m.replace("a", "r"));
        assertChangesTheHash("replaceAll", m -> m.replaceAll((k, v) -> v + "?"));
        assertChangesTheHash("clear", MultiKeyMap::clear);
    }

    /** Writers race a thread that keeps asking for hashCode(); once they are done, the cached hash is the right one. */
    @Test
    void aHashCachedDuringConcurrentWritesIsNotStale() throws Exception {
        for (int round = 0; round < 20; round++) {
            MultiKeyMap<String> map = new MultiKeyMap<>();
            int writers = 4;
            CountDownLatch go = new CountDownLatch(1);
            AtomicBoolean done = new AtomicBoolean();
            List<Thread> threads = new ArrayList<>();
            for (int t = 0; t < writers; t++) {
                final int id = t;
                threads.add(new Thread(() -> {
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    for (int i = 0; i < 2000; i++) {
                        map.put(new Object[]{id, i}, "v" + i);
                        if (i % 3 == 0) {
                            map.remove(new Object[]{id, i - 1});
                        }
                    }
                }));
            }
            Thread hasher = new Thread(() -> {
                while (!done.get()) {
                    map.hashCode();
                }
            });
            threads.forEach(Thread::start);
            hasher.start();
            go.countDown();
            for (Thread t : threads) {
                t.join();
            }
            done.set(true);
            hasher.join();
            assertEquals(freshHash(map), map.hashCode(), "round " + round);
        }
    }

    @Test
    void theSizeIsExactOnceWritersAreDone() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int writers = 8;
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < writers; t++) {
            final int id = t;
            threads.add(new Thread(() -> {
                for (int i = 0; i < 5000; i++) {
                    map.put(new Object[]{id, i}, "v");
                }
                for (int i = 0; i < 5000; i += 2) {
                    map.remove(new Object[]{id, i});
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(writers * 2500, map.size());
        assertEquals(writers * 2500L, map.longSize());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }
}
