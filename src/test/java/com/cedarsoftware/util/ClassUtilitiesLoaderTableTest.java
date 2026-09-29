package com.cedarsoftware.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * forName()'s per-loader caches are found for any loader without a lock or an allocation. They were kept in a
 * synchronized WeakHashMap behind a one-entry memo of the loader last used: lookups alternating between loaders
 * took the global lock and allocated a new memo on every switch (two alternating loaders: 31 million lookups a
 * second on one thread against about 170 for one loader, and 10-27 in total on eight threads). The table holds each
 * loader weakly, keeps every loader's cache through rebuilds, and drops the entries of loaders since collected.
 * Timed on a separate thread: a table that filled up would leave a probe spinning, which an interrupt cannot stop.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ClassUtilitiesLoaderTableTest {

    private static ClassLoader newLoader() {
        return new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
    }

    private static Object findLoaderCache(ClassLoader loader) throws Exception {
        Method find = ClassUtilities.class.getDeclaredMethod("findLoaderCache", ClassLoader.class);
        find.setAccessible(true);
        return find.invoke(null, loader);
    }

    private static Object[] loaderTable() throws Exception {
        Field table = ClassUtilities.class.getDeclaredField("loaderTable");
        table.setAccessible(true);
        return (Object[]) table.get(null);
    }

    /** The measure of the fix: cache hits that alternate between loaders allocate nothing (a memo each switch). */
    @Test
    void alternatingLoadersAllocateNothing() {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        if (!(mx instanceof com.sun.management.ThreadMXBean)) {
            return;                                     // allocation counts need HotSpot's bean
        }
        com.sun.management.ThreadMXBean hotspot = (com.sun.management.ThreadMXBean) mx;
        ClassLoader[] loaders = {ClassLoader.getSystemClassLoader(), newLoader(), newLoader()};
        String[] names = {"java.lang.String", "java.util.ArrayList", "java.lang.Integer"};
        Class<?>[] classes = {String.class, ArrayList.class, Integer.class};
        for (int warm = 0; warm < 200_000; warm++) {
            assertSame(classes[warm % 3], ClassUtilities.forName(names[warm % 3], loaders[warm % loaders.length]));
        }
        long thread = Thread.currentThread().getId();
        long before = hotspot.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 30_000; i++) {
            if (ClassUtilities.forName(names[i % 3], loaders[(i / 3) % loaders.length]) != classes[i % 3]) {
                throw new AssertionError("wrong class at " + i);
            }
        }
        long allocated = hotspot.getThreadAllocatedBytes(thread) - before;
        assertTrue(allocated < 16_384, "30,000 alternating hits allocated " + allocated + " bytes");
    }

    /** A hundred loaders, each with its own cache, all still found after the rebuilds that added the others. */
    @Test
    void everyLoaderKeepsItsOwnCacheThroughRebuilds() throws Exception {
        List<ClassLoader> loaders = new ArrayList<>();
        List<Object> caches = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ClassLoader loader = newLoader();
            assertSame(String.class, ClassUtilities.forName("java.lang.String", loader));
            loaders.add(loader);
            caches.add(findLoaderCache(loader));
        }
        for (int i = 0; i < loaders.size(); i++) {
            assertSame(caches.get(i), findLoaderCache(loaders.get(i)), "loader " + i);
            for (int j = 0; j < i; j++) {
                assertNotSame(caches.get(i), caches.get(j));
            }
        }
        Object[] table = loaderTable();
        int live = 0;
        for (Object e : table) {
            if (e != null && ((WeakReference<?>) e).get() != null) {
                live++;
            }
        }
        assertTrue(table.length >= 2 * live, "at most half full: " + live + " live in " + table.length);
    }

    /** The table holds a loader weakly: once collected, it is gone, and the next rebuild drops its entry. */
    @Test
    void aCollectedLoaderIsNotPinned() throws Exception {
        WeakReference<ClassLoader> gone = useThrowawayLoader();
        for (int i = 0; i < 50 && gone.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(gone.get(), "the loader was kept reachable");
        ClassLoader fresh = newLoader();
        assertSame(String.class, ClassUtilities.forName("java.lang.String", fresh));   // rebuilds the table
        for (Object e : loaderTable()) {
            if (e != null) {
                assertNotNull(((WeakReference<?>) e).get(), "a collected loader's entry survived a rebuild");
            }
        }
    }

    private static WeakReference<ClassLoader> useThrowawayLoader() throws Exception {
        ClassLoader loader = newLoader();
        assertSame(String.class, ClassUtilities.forName("java.lang.String", loader));
        assertNull(ClassUtilities.forName("com.cedarsoftware.nowhere.Missing", loader));
        assertNotNull(findLoaderCache(loader));
        return new WeakReference<>(loader);
    }

    /**
     * Lookups through many loaders on many threads agree, while another thread keeps adding loaders.
     */
    @Test
    void concurrentLookupsAgreeWhileLoadersAreAdded() throws Exception {
        List<ClassLoader> loaders = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            loaders.add(newLoader());
        }
        String[] names = {"java.lang.String", "java.util.HashMap", "java.lang.Long"};
        Class<?>[] classes = {String.class, java.util.HashMap.class, Long.class};
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final int id = t;
            threads.add(new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 200_000; i++) {
                        int k = (i + id) % 3;
                        Class<?> c = ClassUtilities.forName(names[k], loaders.get((i * 7 + id) % loaders.size()));
                        if (c != classes[k]) {
                            throw new AssertionError(names[k] + " gave " + c);
                        }
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }));
        }
        Thread adder = new Thread(() -> {
            while (!stop.get()) {
                ClassUtilities.forName("java.lang.String", newLoader());
            }
        });
        threads.forEach(t -> t.setDaemon(true));    // so a hung probe cannot keep the test JVM alive
        adder.setDaemon(true);
        threads.forEach(Thread::start);
        adder.start();
        go.countDown();
        for (Thread t : threads) {
            t.join();
        }
        stop.set(true);
        adder.join();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        assertEquals(16, loaders.size());
    }
}
