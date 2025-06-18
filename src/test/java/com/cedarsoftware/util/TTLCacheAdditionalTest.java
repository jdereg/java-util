package com.cedarsoftware.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TTLCacheAdditionalTest {

    @AfterAll
    static void shutdown() {
        TTLCache.shutdown();
    }

    @Test
    void testDefaultConstructorAndPurgeRun() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(50);
        cache.put(1, "A");

        // wait for entry to expire
        Thread.sleep(70);

        Field taskField = TTLCache.class.getDeclaredField("purgeTask");
        taskField.setAccessible(true);
        Object task = taskField.get(cache);
        Method run = task.getClass().getDeclaredMethod("run");
        run.setAccessible(true);
        run.invoke(task); // triggers purgeExpiredEntries()

        assertEquals(0, cache.size());
        assertNull(cache.get(1));
    }

    @Test
    void testPurgeRunCancelsFutureWhenCacheGone() throws Exception {
        Class<?> taskClass = Class.forName("com.cedarsoftware.util.TTLCache$PurgeTask");
        Constructor<?> ctor = taskClass.getDeclaredConstructor(WeakReference.class);
        ctor.setAccessible(true);
        Object task = ctor.newInstance(new WeakReference<>(null));

        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        try {
            ScheduledFuture<?> future = exec.schedule(() -> { }, 1, TimeUnit.SECONDS);
            Method setFuture = taskClass.getDeclaredMethod("setFuture", ScheduledFuture.class);
            setFuture.setAccessible(true);
            setFuture.invoke(task, future);

            Method run = taskClass.getDeclaredMethod("run");
            run.setAccessible(true);
            run.invoke(task); // should cancel future

            assertTrue(future.isCancelled());
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    void testEntrySetClear() {
        TTLCache<Integer, String> cache = new TTLCache<>(100, -1);
        cache.put(1, "A");
        cache.put(2, "B");

        cache.entrySet().clear();

        assertTrue(cache.isEmpty());
    }
}
