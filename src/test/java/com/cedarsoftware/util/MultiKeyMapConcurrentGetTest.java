package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify get() never returns null for existing keys during resizing.
 */
class MultiKeyMapConcurrentGetTest {

    @Test
    void testConcurrentGetsDuringResize() throws Exception {
        final MultiKeyMap<String> map = new MultiKeyMap<>(4, 0.60f);
        final int total = 2000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(2);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicInteger written = new AtomicInteger(-1);
        final AtomicBoolean writerDone = new AtomicBoolean(false);

        Thread writer = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < total; i++) {
                    map.put("val" + i, String.class, Integer.class, (long) i);
                    written.set(i);
                    if (i % 20 == 0) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                writerDone.set(true);
                doneLatch.countDown();
            }
        });

        Thread reader = new Thread(() -> {
            try {
                startLatch.await();
                while (!writerDone.get()) {
                    int upTo = written.get();
                    for (int i = 0; i <= upTo; i++) {
                        String v = map.get(String.class, Integer.class, (long) i);
                        if (v == null) {
                            failed.set(true);
                            return;
                        }
                    }
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                failed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });

        writer.start();
        reader.start();
        startLatch.countDown();
        doneLatch.await();

        assertFalse(failed.get(), "get() returned null for an existing key");
        for (int i = 0; i < total; i++) {
            assertEquals("val" + i, map.get(String.class, Integer.class, (long) i));
        }
    }
}

