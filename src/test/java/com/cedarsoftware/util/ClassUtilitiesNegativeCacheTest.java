package com.cedarsoftware.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * forName() caches a name it could not load, so the class loader is asked only once. But a hit on that
 * cached miss then built and threw a ClassNotFoundException -- message and stack trace -- which forName()
 * immediately caught and turned into null: ~0.3-0.4 us and ~700 bytes per lookup from a shallow stack, 2,400
 * bytes under this test runner -- the stack trace grows with depth. json-io resolves a type name per object,
 * so a document naming a missing type paid it per object. A cached miss now returns null directly.
 * <p>
 * The not-found cache is bounded (per loader), and at the bound it refused NEW entries: any 4,096 unique
 * missing names -- junk read from untrusted input, say -- filled it for good, after which every genuinely
 * missing class cost a full loader search on every lookup. Now the OLDEST entry is forgotten instead.
 */
class ClassUtilitiesNegativeCacheTest {

    private static final String MISSING = "com.example.nope.MissingType";

    /** Counts the times it is asked for each name; {@link #asked} is the count for {@link #MISSING}. */
    static final class CountingLoader extends ClassLoader {
        final AtomicInteger asked = new AtomicInteger();
        final Map<String, AtomicInteger> askedFor = new ConcurrentHashMap<>();

        CountingLoader() {
            super(ClassUtilitiesNegativeCacheTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (MISSING.equals(name)) {
                asked.incrementAndGet();
            }
            askedFor.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
            return super.loadClass(name, resolve);
        }

        int askedFor(String name) {
            AtomicInteger n = askedFor.get(name);
            return n == null ? 0 : n.get();
        }
    }

    private static int maxNotFoundEntries() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("MAX_NEGATIVE_CLASS_CACHE_ENTRIES");
        f.setAccessible(true);
        return f.getInt(null);
    }

    /** The number of names this loader's cache holds as not found. */
    @SuppressWarnings("unchecked")
    private static int notFoundEntries(ClassLoader loader) throws Exception {
        Method find = ClassUtilities.class.getDeclaredMethod("findLoaderCache", ClassLoader.class);
        find.setAccessible(true);
        Object holder = find.invoke(null, loader);
        Field cacheField = holder.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        Map<String, WeakReference<Class<?>>> cache = (Map<String, WeakReference<Class<?>>>) cacheField.get(holder);
        int n = 0;
        for (WeakReference<Class<?>> ref : cache.values()) {
            Class<?> c = ref.get();
            if (c != null && c.getName().endsWith("$ClassNotFoundSentinel")) {
                n++;
            }
        }
        return n;
    }

    private static void missUniqueNames(ClassLoader loader, String prefix, int count) {
        for (int i = 0; i < count; i++) {
            assertNull(ClassUtilities.forName(prefix + i, loader));
        }
    }

    // These loaders are neither the JDK's nor Groovy's, so their misses expire (1 second by default): kept long here,
    // so that what is measured is the cache, however slow the machine.
    @BeforeEach
    void keepMissesAndClearCaches() {
        System.setProperty("classutilities.class.not.found.cache.millis", "600000");
        ClassUtilities.clearCaches();
    }

    @AfterEach
    void restoreAndClearCaches() {
        System.clearProperty("classutilities.class.not.found.cache.millis");
        ClassUtilities.clearCaches();
    }

    @Test
    void aKnownMissingNameIsAnsweredWithoutBuildingAnException() {
        ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean, "needs per-thread allocation counters");
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) base;
        assumeTrue(mx.isThreadAllocatedMemorySupported() && mx.isThreadAllocatedMemoryEnabled(),
                "needs per-thread allocation counters");

        ClassLoader loader = new CountingLoader();
        assertNull(ClassUtilities.forName(MISSING, loader));   // the one real miss, now cached

        long threadId = Thread.currentThread().getId();
        int lookups = 10_000;
        long before = mx.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < lookups; i++) {
            assertNull(ClassUtilities.forName(MISSING, loader));
        }
        double bytesPerLookup = (mx.getThreadAllocatedBytes(threadId) - before) / (double) lookups;

        // Throwing allocated 700-2,400 bytes per lookup (the exception, its message, its stack trace).
        assertTrue(bytesPerLookup < 100, "a cached miss allocated " + bytesPerLookup + " bytes per lookup");
    }

    @Test
    void theLoaderIsStillAskedOnlyOnce() {
        CountingLoader loader = new CountingLoader();
        for (int i = 0; i < 5; i++) {
            assertNull(ClassUtilities.forName(MISSING, loader));
        }
        assertEquals(1, loader.asked.get());
    }

    @Test
    void clearCachesForgetsTheMiss() {
        CountingLoader loader = new CountingLoader();
        assertNull(ClassUtilities.forName(MISSING, loader));
        ClassUtilities.clearCaches();
        assertNull(ClassUtilities.forName(MISSING, loader));
        assertEquals(2, loader.asked.get());
    }

    @Test
    void aMissingNameIsCachedPerLoader() {
        CountingLoader first = new CountingLoader();
        CountingLoader second = new CountingLoader();
        assertNull(ClassUtilities.forName(MISSING, first));
        assertNull(ClassUtilities.forName(MISSING, second));
        assertNull(ClassUtilities.forName(MISSING, first));
        assertEquals(1, first.asked.get());
        assertEquals(1, second.asked.get(), "one loader's miss says nothing about another's");
    }

    @Test
    void aNewMissIsStillCachedOnceTheLimitIsReached() throws Exception {
        CountingLoader loader = new CountingLoader();
        missUniqueNames(loader, "com.example.junk.J", maxNotFoundEntries());

        for (int i = 0; i < 5; i++) {
            assertNull(ClassUtilities.forName(MISSING, loader));
        }
        assertEquals(1, loader.asked.get(), "a full cache must make room, not refuse the new miss");
    }

    @Test
    void theOldestMissIsTheOneForgotten() throws Exception {
        CountingLoader loader = new CountingLoader();
        assertNull(ClassUtilities.forName(MISSING, loader));
        missUniqueNames(loader, "com.example.junk.J", maxNotFoundEntries());
        String newest = "com.example.junk.J" + (maxNotFoundEntries() - 1);

        assertNull(ClassUtilities.forName(newest, loader));
        assertEquals(1, loader.askedFor(newest), "the newest miss is still cached");
        assertNull(ClassUtilities.forName(MISSING, loader));
        assertEquals(2, loader.asked.get(), "the oldest miss was forgotten, so the loader is asked again");
        assertNull(ClassUtilities.forName(MISSING, loader));
        assertEquals(2, loader.asked.get(), "and then it is cached again");
    }

    @Test
    void theNotFoundEntriesStayBounded() throws Exception {
        int max = maxNotFoundEntries();
        CountingLoader loader = new CountingLoader();
        missUniqueNames(loader, "com.example.junk.J", 3 * max);
        assertEquals(max, notFoundEntries(loader));
    }

    @Test
    void aFoundClassIsNotEvictedByMisses() throws Exception {
        CountingLoader loader = new CountingLoader();
        String found = "java.util.UUID";
        assertEquals(java.util.UUID.class, ClassUtilities.forName(found, loader));
        missUniqueNames(loader, "com.example.junk.J", 2 * maxNotFoundEntries());

        assertEquals(java.util.UUID.class, ClassUtilities.forName(found, loader));
        assertEquals(1, loader.askedFor(found), "found classes are cached apart from the not-found bound");
    }

    @Test
    void theBoundHoldsUnderConcurrentMisses() throws Exception {
        int max = maxNotFoundEntries();
        CountingLoader loader = new CountingLoader();
        int threads = 8;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        try {
            java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
            for (int t = 0; t < threads; t++) {
                String prefix = "com.example.junk.T" + t + "_";
                futures.add(pool.submit(() -> missUniqueNames(loader, prefix, max)));
            }
            for (java.util.concurrent.Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }
        assertEquals(max, notFoundEntries(loader));

        // And it still makes room
        assertNull(ClassUtilities.forName(MISSING, loader));
        assertNull(ClassUtilities.forName(MISSING, loader));
        assertEquals(1, loader.asked.get());
    }
}
