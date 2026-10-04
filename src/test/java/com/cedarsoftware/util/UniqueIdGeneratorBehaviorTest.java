package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UniqueIdGenerator behavior found by review. Each test runs a fresh copy of the generator in its own class loader,
 * so it can set the generator's state without disturbing the ids the rest of the JVM gets.
 */
class UniqueIdGeneratorBehaviorTest {

    /**
     * After the wall clock steps back (an NTP correction, a VM snapshot restored), ids keep coming once a millisecond's
     * capacity is used. They are never issued for a time before ids already issued, so the generator waited for the
     * wall clock to catch up: every caller blocked for as long as the clock had stepped back, an hour for an hour. It
     * now moves on by one millisecond of its own per real millisecond.
     */
    @Test
    void aClockThatSteppedBackDoesNotStallIdGeneration() throws Exception {
        URL[] javaUtil = {new File("target/classes").toURI().toURL()};
        try (URLClassLoader fresh = new URLClassLoader(javaUtil, ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> generator = Class.forName(UniqueIdGenerator.class.getName(), true, fresh);
            Field lastIdField = generator.getDeclaredField("LAST_ID_16");
            lastIdField.setAccessible(true);
            AtomicLong lastId = (AtomicLong) lastIdField.get(null);
            Method getUniqueId = generator.getMethod("getUniqueId");
            int serverId = (Integer) generator.getMethod("getServerIdConfigured").invoke(null);

            long issuedUpTo = System.currentTimeMillis() + 3000;      // ids issued before the clock stepped back 3 s
            lastId.set(issuedUpTo * 100_000L + serverId);
            long start = System.nanoTime();
            long previous = lastId.get();
            for (int i = 0; i < 1_100; i++) {                         // a millisecond holds 1,000
                long id = (Long) getUniqueId.invoke(null);
                assertTrue(id > previous, "ids keep increasing");
                previous = id;
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(elapsedMs < 1000, "1,100 ids after the clock stepped back 3 s took " + elapsedMs + " ms");
        }
    }
}
