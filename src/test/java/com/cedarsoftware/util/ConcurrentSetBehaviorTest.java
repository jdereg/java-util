package com.cedarsoftware.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
