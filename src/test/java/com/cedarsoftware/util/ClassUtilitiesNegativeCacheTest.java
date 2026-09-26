package com.cedarsoftware.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
 */
class ClassUtilitiesNegativeCacheTest {

    private static final String MISSING = "com.example.nope.MissingType";

    /** Counts the times it is asked for {@link #MISSING}. */
    static final class CountingLoader extends ClassLoader {
        final AtomicInteger asked = new AtomicInteger();

        CountingLoader() {
            super(ClassUtilitiesNegativeCacheTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (MISSING.equals(name)) {
                asked.incrementAndGet();
            }
            return super.loadClass(name, resolve);
        }
    }

    @BeforeEach
    @AfterEach
    void clearCaches() {
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
}
