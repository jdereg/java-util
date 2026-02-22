package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for cachedHashCode not being invalidated by ConcurrentMap methods.
 *
 * Bug: Methods like putIfAbsent, computeIfAbsent, computeIfPresent, compute,
 * merge, remove(K,V), replace(K,V), and replace(K,V,V) call putNoLock /
 * removeNoLock directly, bypassing the cachedHashCode = null invalidation
 * that putInternal / removeInternal perform. After any of these methods
 * mutate the map, hashCode() returns a stale cached value.
 */
class MultiKeyMapCachedHashCodeAndPutIfAbsentTest {

    private static final class CoordinatedHashKey {
        private final AtomicBoolean block = new AtomicBoolean(false);
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        void enableBlocking() {
            block.set(true);
        }

        void awaitHashEntry() {
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS), "Timed out waiting for hashCode() to start");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for hashCode() coordination", e);
            }
        }

        void releaseBlocking() {
            block.set(false);
            release.countDown();
        }

        @Override
        public int hashCode() {
            if (block.get()) {
                entered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while blocking hashCode()", e);
                }
            }
            return 31;
        }
    }

    /**
     * Build a reference HashMap with the same content and return its hashCode.
     * This lets us compare against MultiKeyMap's hashCode after mutations.
     */
    private int referenceHashCode(MultiKeyMap<String> mkm) {
        Map<Object, String> ref = new HashMap<>();
        for (Map.Entry<Object, String> e : mkm.entrySet()) {
            ref.put(e.getKey(), e.getValue());
        }
        return ref.hashCode();
    }

    @Test
    void testHashCodeInvalidatedByPutIfAbsent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode(); // caches the value

        map.putIfAbsent("b", "2"); // mutation through ConcurrentMap API

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after putIfAbsent inserts a new entry");
        assertEquals(referenceHashCode(map), hashAfter, "hashCode must match a reference map with the same entries");
    }

    @Test
    void testHashCodeInvalidatedByComputeIfAbsent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        map.computeIfAbsent("b", k -> "2");

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after computeIfAbsent inserts a new entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByComputeIfPresent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        map.computeIfPresent("a", (k, v) -> "updated");

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after computeIfPresent updates an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByCompute() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        map.compute("a", (k, v) -> "computed");

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after compute updates an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByMerge() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        map.merge("a", "2", (oldV, newV) -> oldV + newV);

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after merge updates an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByConditionalRemove() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        int hashBefore = map.hashCode();

        boolean removed = map.remove("a", "1");

        assertTrue(removed);
        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after remove(key, value) removes an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByReplace() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        map.replace("a", "replaced");

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after replace updates an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByConditionalReplace() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        int hashBefore = map.hashCode();

        boolean replaced = map.replace("a", "1", "replaced");

        assertTrue(replaced);
        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after replace(key, old, new) updates an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByComputeRemoval() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        int hashBefore = map.hashCode();

        map.compute("a", (k, v) -> null); // removes the entry

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after compute removes an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByComputeIfPresentRemoval() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        int hashBefore = map.hashCode();

        map.computeIfPresent("a", (k, v) -> null); // removes the entry

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after computeIfPresent removes an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeInvalidatedByMergeRemoval() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        int hashBefore = map.hashCode();

        map.merge("a", "x", (oldV, newV) -> null); // removes the entry

        int hashAfter = map.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after merge removes an entry");
        assertEquals(referenceHashCode(map), hashAfter);
    }

    @Test
    void testHashCodeStableWhenNoMutation() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        int hash1 = map.hashCode();
        int hash2 = map.hashCode(); // should use cache

        assertEquals(hash1, hash2, "hashCode must be stable across repeated calls without mutation");
    }

    @Test
    void testHashCodeMatchesReferenceMapAfterMultipleMutations() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("a", "1");
        map.hashCode(); // prime the cache

        map.putIfAbsent("b", "2");
        map.computeIfPresent("a", (k, v) -> "updated");
        map.merge("c", "3", (o, n) -> o + n);

        assertEquals(referenceHashCode(map), map.hashCode(),
                "hashCode must be correct after a sequence of ConcurrentMap mutations");
    }

    @Test
    void testConcurrentHashComputationDoesNotPublishStaleCache() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        CoordinatedHashKey blockingKey = new CoordinatedHashKey();
        map.put(blockingKey, "value");
        map.put("stable", "entry");

        blockingKey.enableBlocking();
        Thread hashThread = new Thread(map::hashCode);
        hashThread.start();

        blockingKey.awaitHashEntry();
        map.put("mutated", "entry");
        blockingKey.releaseBlocking();
        hashThread.join(2000);

        int expected = referenceHashCode(map);
        int actual = map.hashCode();
        assertEquals(expected, actual, "hashCode cache must not retain pre-mutation value after concurrent hashing");
    }
}
