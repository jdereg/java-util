package com.cedarsoftware.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DateUtilities' regex time limit, which applies when {@code dateutilities.security.enabled=true}, is kept on the
 * calling thread, as RegexUtilities' is (4.119.0). It handed every match to a thread of a pool that grew without
 * limit and waited for the answer, several times for each date parsed.
 */
class DateUtilitiesRegexOnCallerThreadTest {
    private static final String[] PROPERTIES = {"dateutilities.security.enabled",
            "dateutilities.regex.timeout.enabled", "dateutilities.regex.timeout.milliseconds",
            "cedarsoftware.regex.timeout.enabled", "cedarsoftware.regex.timeout.milliseconds"};
    private static final ZoneId UTC = ZoneId.of("UTC");
    private final String[] saved = new String[PROPERTIES.length];

    @BeforeEach
    void rememberProperties() {
        for (int i = 0; i < PROPERTIES.length; i++) {
            saved[i] = System.getProperty(PROPERTIES[i]);
            System.clearProperty(PROPERTIES[i]);
        }
        DateUtilities.resetSecurityConfigCacheForTesting();
    }

    @AfterEach
    void restoreProperties() {
        for (int i = 0; i < PROPERTIES.length; i++) {
            if (saved[i] == null) {
                System.clearProperty(PROPERTIES[i]);
            } else {
                System.setProperty(PROPERTIES[i], saved[i]);
            }
        }
        DateUtilities.resetSecurityConfigCacheForTesting();
    }

    private static void set(String property, String value) {
        System.setProperty(property, value);
        DateUtilities.resetSecurityConfigCacheForTesting();
    }

    @Test
    @Timeout(120)
    void manyThreadsParseWithTheTimeLimitOnAndNoThreadIsStartedForThem() throws Exception {
        set("dateutilities.security.enabled", "true");
        assertTrue(DateUtilities.isRegexTimeoutProtectionEnabled());
        final String[] texts = {"2024-07-03T09:46:40Z", "July 3, 2024 09:46:40 UTC", "2024/07/03 09:46", "3 Jul 2024",
                "Wed Jul 03 09:46:40 UTC 2024", "1720000000000"};
        final ZonedDateTime[] expected = new ZonedDateTime[texts.length];
        System.clearProperty("dateutilities.security.enabled");
        DateUtilities.resetSecurityConfigCacheForTesting();
        for (int i = 0; i < texts.length; i++) {
            expected[i] = DateUtilities.parseDate(texts[i], UTC, true);         // without the limit: the oracle
        }
        set("dateutilities.security.enabled", "true");

        final int threads = 64;
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicLong parsed = new AtomicLong();
        final AtomicReference<Throwable> failed = new AtomicReference<>();
        Thread[] all = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            all[t] = new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 400; i++) {
                        int which = i % texts.length;
                        if (expected[which].equals(DateUtilities.parseDate(texts[which], UTC, true))) {
                            parsed.incrementAndGet();
                        }
                    }
                } catch (Throwable e) {
                    failed.compareAndSet(null, e);
                }
            });
            all[t].start();
        }
        go.countDown();
        for (Thread thread : all) {
            thread.join();
        }
        assertNull(failed.get());
        assertEquals(threads * 400L, parsed.get());
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            assertFalse(thread.getName().startsWith("DateUtilities-Regex"), "a thread of its own: " + thread.getName());
        }
    }

    /**
     * A switch asked for with stray spaces or capitals is on. Read with Boolean.parseBoolean, "true " left all of
     * DateUtilities' protection off, and "yes" switched the time limit off where anything but "false" should
     * leave it on.
     */
    @Test
    void theSwitchesReadWhatWasMeant() {
        assertFalse(DateUtilities.isRegexTimeoutProtectionEnabled(), "off unless asked for");
        for (String on : new String[]{"true", "TRUE", " true", "true ", "True"}) {
            set("dateutilities.security.enabled", on);
            assertTrue(DateUtilities.isRegexTimeoutProtectionEnabled(), "[" + on + "]");
        }
        for (String off : new String[]{"false", "", "no", "1"}) {
            set("dateutilities.security.enabled", off);
            assertFalse(DateUtilities.isRegexTimeoutProtectionEnabled(), "[" + off + "]");
        }
        set("dateutilities.security.enabled", "true");
        for (String stillOn : new String[]{"true", " true", "yes", "1", "on"}) {
            set("dateutilities.regex.timeout.enabled", stillOn);
            assertTrue(DateUtilities.isRegexTimeoutProtectionEnabled(), "[" + stillOn + "]");
            System.clearProperty("dateutilities.regex.timeout.enabled");
            set("cedarsoftware.regex.timeout.enabled", stillOn);
            assertTrue(DateUtilities.isRegexTimeoutProtectionEnabled(), "global [" + stillOn + "]");
            System.clearProperty("cedarsoftware.regex.timeout.enabled");
        }
        for (String off : new String[]{"false", " FALSE "}) {
            set("dateutilities.regex.timeout.enabled", off);
            assertFalse(DateUtilities.isRegexTimeoutProtectionEnabled(), "[" + off + "]");
            System.clearProperty("dateutilities.regex.timeout.enabled");
            set("cedarsoftware.regex.timeout.enabled", off);
            assertFalse(DateUtilities.isRegexTimeoutProtectionEnabled(), "global [" + off + "]");
            System.clearProperty("cedarsoftware.regex.timeout.enabled");
        }
        set("dateutilities.regex.timeout.milliseconds", " 250 ");
        assertEquals(250, DateUtilities.getRegexTimeoutMilliseconds());
    }
}
