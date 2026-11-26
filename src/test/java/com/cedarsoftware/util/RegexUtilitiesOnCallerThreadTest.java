package com.cedarsoftware.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RegexUtilities' time limit is kept on the thread that asked.
 * <p>
 * Until 4.119.0 every protected operation was handed to one pool of at most eight threads and the caller waited:
 * with more callers than the pool and its queue of 128 could take, harmless matches were refused ("rejected due to
 * timeout executor saturation"); eight runaway matches in flight held every other caller in the JVM for the whole
 * timeout; and whatever the operation threw came back as a SecurityException. The operation now runs where it was
 * called, over text that looks at the clock as it is read.
 */
class RegexUtilitiesOnCallerThreadTest {
    private static final String[] PROPERTIES = {"cedarsoftware.security.enabled",
            "cedarsoftware.regex.timeout.enabled", "cedarsoftware.regex.timeout.milliseconds"};
    /** Days on 80 characters: fourteen stars that can share the a's between them in 80^13 ways. */
    private static final Pattern RUNAWAY = Pattern.compile("a*a*a*a*a*a*a*a*a*a*a*a*a*a*!");
    private static final String RUNAWAY_INPUT = String.join("", Collections.nCopies(80, "a"));
    private final String[] saved = new String[PROPERTIES.length];

    @BeforeEach
    void rememberProperties() {
        for (int i = 0; i < PROPERTIES.length; i++) {
            saved[i] = System.getProperty(PROPERTIES[i]);
            System.clearProperty(PROPERTIES[i]);
        }
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
        Thread.interrupted();
    }

    /** 200 threads matching at once: the pool took 8 and queued 128, and refused the rest. */
    @Test
    @Timeout(60)
    void manyThreadsMatchingAtOnceAreAllServed() throws Exception {
        final Pattern digits = Pattern.compile("\\d+");
        final int threads = 200;
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final AtomicLong matched = new AtomicLong();
        final AtomicReference<Throwable> refused = new AtomicReference<>();
        Thread[] all = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            all[t] = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                    for (int i = 0; i < 300; i++) {
                        if (RegexUtilities.safeMatches(digits, "12345")
                                && RegexUtilities.safeFind(digits, "ab 77 cd").matched()
                                && "x-x".equals(RegexUtilities.safeReplaceAll(digits, "1-2", "x"))) {
                            matched.incrementAndGet();
                        }
                    }
                } catch (Throwable e) {
                    refused.compareAndSet(null, e);
                }
            });
            all[t].start();
        }
        ready.await();
        go.countDown();
        for (Thread thread : all) {
            thread.join();
        }
        assertNull(refused.get(), "a harmless match was refused");
        assertEquals(threads * 300L, matched.get());
    }

    /** Sixteen runaway matches in flight do not make anyone else wait: each costs only its own thread. */
    @Test
    @Timeout(60)
    void runawayMatchesDoNotHoldUpOtherCallers() throws Exception {
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "1500");
        final int attackers = 16;
        final CountDownLatch started = new CountDownLatch(attackers);
        final AtomicInteger timedOut = new AtomicInteger();
        Thread[] all = new Thread[attackers];
        for (int t = 0; t < attackers; t++) {
            all[t] = new Thread(() -> {
                started.countDown();
                try {
                    RegexUtilities.safeMatches(RUNAWAY, RUNAWAY_INPUT);
                } catch (SecurityException e) {
                    if (e.getMessage().contains("timed out")) {
                        timedOut.incrementAndGet();
                    }
                }
            });
            all[t].start();
        }
        started.await();
        Thread.sleep(100);                              // every one of them is inside its match

        Pattern digits = Pattern.compile("\\d+");
        long slowest = 0;
        for (int i = 0; i < 50; i++) {
            long start = System.nanoTime();
            assertTrue(RegexUtilities.safeMatches(digits, "12345"));
            slowest = Math.max(slowest, System.nanoTime() - start);
        }
        for (Thread thread : all) {
            thread.join();
        }
        assertEquals(attackers, timedOut.get(), "each runaway match was stopped at its limit");
        assertTrue(slowest < 500_000_000L, "a harmless match waited " + slowest / 1_000_000 + " ms behind them");
    }

    @Test
    @Timeout(30)
    void aRunawayMatchIsStoppedAtItsLimitAndNoThreadIsStartedForIt() {
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "150");
        long start = System.nanoTime();
        SecurityException stopped = assertThrows(SecurityException.class,
                () -> RegexUtilities.safeMatches(RUNAWAY, RUNAWAY_INPUT));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(stopped.getMessage().contains("timed out (>150ms)"), stopped.getMessage());
        assertTrue(millis >= 140 && millis < 5_000, "stopped after " + millis + " ms");
        assertThrows(SecurityException.class, () -> RegexUtilities.safeFind(RUNAWAY, RUNAWAY_INPUT));
        assertThrows(SecurityException.class, () -> RegexUtilities.safeReplaceAll(RUNAWAY, RUNAWAY_INPUT, "x"));
        assertThrows(SecurityException.class, () -> RegexUtilities.safeReplaceFirst(RUNAWAY, RUNAWAY_INPUT, "x"));
        assertThrows(SecurityException.class, () -> RegexUtilities.safeSplit(RUNAWAY, RUNAWAY_INPUT));

        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            assertFalse(thread.getName().startsWith("RegexUtilities"), "a thread of its own: " + thread.getName());
        }
    }

    /** What the operation throws is what the caller gets: these three came back as SecurityException. */
    @Test
    void whatTheJdkThrowsIsWhatTheCallerGets() {
        Pattern word = Pattern.compile("(\\w+)");
        String jdk = assertThrows(IndexOutOfBoundsException.class,
                () -> word.matcher("abc").replaceAll("$2")).getMessage();
        assertEquals(jdk, assertThrows(IndexOutOfBoundsException.class,
                () -> RegexUtilities.safeReplaceAll(word, "abc", "$2")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> RegexUtilities.safeReplaceFirst(word, "abc", "x\\"));
        assertThrows(IllegalArgumentException.class, () -> RegexUtilities.safeReplaceAll(word, "abc", "${nope}"));
    }

    /** A thread whose interrupt flag is already set can match, as it can with a Matcher; the flag stays set. */
    @Test
    void aThreadThatWasAlreadyInterruptedCanMatch() {
        Pattern digits = Pattern.compile("(\\d+)");
        Thread.currentThread().interrupt();
        try {
            assertTrue(RegexUtilities.safeMatches(digits, "12345"));
            assertEquals("77", RegexUtilities.safeFind(digits, "ab 77 cd").group(1));
            assertEquals("a-b", RegexUtilities.safeReplaceAll(digits, "a1b", "-"));
            assertEquals(2, RegexUtilities.safeSplit(digits, "a1b").length);
            assertTrue(Thread.currentThread().isInterrupted(), "the flag was cleared");
        } finally {
            Thread.interrupted();
        }
    }

    /** An interrupt that arrives while a match runs still ends it, and stays on the thread. */
    @Test
    @Timeout(30)
    void anInterruptDuringAMatchEndsIt() throws Exception {
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "20000");
        final AtomicReference<String> outcome = new AtomicReference<>();
        final AtomicBoolean flagAfter = new AtomicBoolean();
        Thread matcher = new Thread(() -> {
            try {
                RegexUtilities.safeMatches(RUNAWAY, RUNAWAY_INPUT);
                outcome.set("returned");
            } catch (SecurityException e) {
                outcome.set(e.getMessage());
            }
            flagAfter.set(Thread.currentThread().isInterrupted());
        });
        matcher.start();
        Thread.sleep(150);
        matcher.interrupt();
        matcher.join(10_000);
        assertEquals("Regex operation interrupted", outcome.get());
        assertTrue(flagAfter.get());
    }

    /**
     * Only "false" switches a protection off. Read with Boolean.parseBoolean, anything that was not exactly "true"
     * did: a space after it in a properties file, "1", "yes".
     */
    @Test
    @Timeout(60)
    void onlyFalseSwitchesTheProtectionOff() {
        for (String property : new String[]{"cedarsoftware.security.enabled", "cedarsoftware.regex.timeout.enabled"}) {
            for (String on : new String[]{"true", "TRUE", " true", "true ", "1", "yes", "on", "enabled", ""}) {
                System.setProperty(property, on);
                assertTrue(RegexUtilities.isSecurityEnabled() && RegexUtilities.isRegexTimeoutEnabled(),
                        property + "=[" + on + "]");
            }
            for (String off : new String[]{"false", "FALSE", " false ", "False"}) {
                System.setProperty(property, off);
                assertFalse(RegexUtilities.isSecurityEnabled() && RegexUtilities.isRegexTimeoutEnabled(),
                        property + "=[" + off + "]");
            }
            System.clearProperty(property);
        }
        System.setProperty("cedarsoftware.regex.timeout.enabled", "true ");
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", " 120 ");
        assertEquals(120, RegexUtilities.getRegexTimeoutMilliseconds());
        assertThrows(SecurityException.class, () -> RegexUtilities.safeMatches(RUNAWAY, RUNAWAY_INPUT));
    }

    /** A regex longer than 4,096 characters is compiled for the call and not kept: the caches hold by count. */
    @Test
    void aVeryLongRegexIsCompiledButNotKept() {
        RegexUtilities.clearPatternCache();
        StringBuilder regex = new StringBuilder("x");
        while (regex.length() <= RegexUtilities.MAX_CACHED_REGEX_LENGTH) {
            regex.append("|word").append(regex.length());
        }
        String longRegex = regex.toString();
        Pattern first = RegexUtilities.getCachedPattern(longRegex);
        Pattern second = RegexUtilities.getCachedPattern(longRegex);
        assertNotNull(first);
        assertNotSame(first, second);
        assertTrue(RegexUtilities.safeMatches(first, "x"));
        assertEquals(Pattern.CASE_INSENSITIVE, RegexUtilities.getCachedPattern(longRegex, true).flags());
        assertEquals(Pattern.MULTILINE | Pattern.DOTALL,
                RegexUtilities.getCachedPattern(longRegex, Pattern.MULTILINE | Pattern.DOTALL).flags());
        assertNull(RegexUtilities.getCachedPattern(longRegex + "(", Pattern.MULTILINE));
        assertNull(RegexUtilities.getCachedPattern(longRegex + "("));

        Map<String, Object> stats = RegexUtilities.getPatternCacheStats();
        assertEquals(0, stats.get("totalCachedPatterns"));
        assertEquals(0, stats.get("invalidPatternCount"));
        assertEquals(0, stats.get("invalidPatternKeyCount"));

        // one of ordinary length is kept, as before
        assertSame(RegexUtilities.getCachedPattern("\\d+"), RegexUtilities.getCachedPattern("\\d+"));
        assertEquals(1, RegexUtilities.getPatternCacheStats().get("cacheSize"));
    }

    /** A match result made from a Matcher that holds no match is an unmatched result, as its Javadoc says. */
    @Test
    void aMatcherWithoutAMatchMakesAnUnmatchedResult() {
        Matcher matcher = Pattern.compile("\\d+").matcher("abc");
        assertFalse(matcher.find());
        RegexUtilities.SafeMatchResult none = new RegexUtilities.SafeMatchResult(matcher, "abc");
        assertFalse(none.matched());
        assertNull(none.group());
        assertEquals(-1, none.start());
        assertEquals("abc", none.getReplacement());

        RegexUtilities.SafeMatchResult found =
                RegexUtilities.safeFind(Pattern.compile("(\\d+)-(\\d+)"), "on 12-34 at noon");
        assertTrue(found.matched());
        assertEquals("12", found.group(1));
        assertEquals("on  at noon", found.getReplacement());
        assertSame(found.getReplacement(), found.getReplacement());
        assertEquals("", RegexUtilities.safeFind(Pattern.compile(".*"), "all of it").getReplacement());
        assertNull(new RegexUtilities.SafeMatchResult(null, null).getReplacement());
    }
}
