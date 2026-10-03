package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConcurrentSet behavior beyond the basic Set contract tests.
 */
class ConcurrentSetBehaviorTest {

    /**
     * toArray(T[]) checked that null was absent, then copied the backing set: a null added in between went out as
     * the set's internal null marker. Every element handed out must be one of the set's elements or null.
     */
    @Test
    void toArrayNeverHandsOutTheInternalNullMarker() throws Exception {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        for (int i = 0; i < 8; i++) {
            set.add("e" + i);
        }
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = new Thread(() -> {
            while (!stop.get()) {
                set.add(null);
                set.remove(null);
            }
        });
        writer.start();
        long strays = 0;
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(300);
        try {
            while (System.nanoTime() < end) {
                for (Object o : set.toArray(new Object[0])) {
                    if (o != null && !(o instanceof String)) {
                        strays++;
                    }
                }
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        assertEquals(0, strays, "elements that are neither null nor one of the set's strings");
    }

    /**
     * retainAll keeps what the argument contains by its own rule, as the Collection contract says: a set with a
     * comparator or identity semantics is asked. ConcurrentSet copied the argument into a HashSet, which compares
     * by equals: a case-insensitive TreeSet holding "A" removed "a", and an identity set kept an equal stranger.
     */
    @Test
    void retainAllAsksTheArgumentWhatItContains() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b"));
        TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A");
        assertTrue(set.retainAll(caseInsensitive));
        assertEquals(new HashSet<>(Collections.singletonList("a")), new HashSet<>(set));

        String k = "k";
        ConcurrentSet<String> identityCase = new ConcurrentSet<>(Collections.singletonList(k));
        Set<String> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(new String(k));
        assertTrue(identityCase.retainAll(identity));
        assertTrue(identityCase.isEmpty());

        ConcurrentSet<String> withNull = new ConcurrentSet<>(Arrays.asList("a", null, "c"));
        Set<String> plain = new HashSet<>(Arrays.asList(null, "c"));
        assertTrue(withNull.retainAll(plain));
        assertEquals(plain, new HashSet<>(withNull));

        ConcurrentSet<String> fromList = new ConcurrentSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(fromList.retainAll(Arrays.asList("c", "a", "a")));
        assertEquals(new HashSet<>(Arrays.asList("a", "c")), new HashSet<>(fromList));
    }
}
