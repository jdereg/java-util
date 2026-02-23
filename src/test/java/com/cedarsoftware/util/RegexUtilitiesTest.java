package com.cedarsoftware.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RegexUtilities.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class RegexUtilitiesTest {

    private String originalSecurityEnabled;
    private String originalTimeoutEnabled;
    private String originalTimeoutMs;

    @BeforeEach
    void setUp() {
        // Save original system properties
        originalSecurityEnabled = System.getProperty("cedarsoftware.security.enabled");
        originalTimeoutEnabled = System.getProperty("cedarsoftware.regex.timeout.enabled");
        originalTimeoutMs = System.getProperty("cedarsoftware.regex.timeout.milliseconds");

        // Clear pattern cache before each test
        RegexUtilities.clearPatternCache();
    }

    @AfterEach
    void tearDown() {
        // Restore original system properties
        restoreProperty("cedarsoftware.security.enabled", originalSecurityEnabled);
        restoreProperty("cedarsoftware.regex.timeout.enabled", originalTimeoutEnabled);
        restoreProperty("cedarsoftware.regex.timeout.milliseconds", originalTimeoutMs);

        // Clear cache after each test
        RegexUtilities.clearPatternCache();
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    // ========== Configuration Tests ==========

    @Test
    void testSecurityEnabledByDefault() {
        System.clearProperty("cedarsoftware.security.enabled");
        assertTrue(RegexUtilities.isSecurityEnabled());
    }

    @Test
    void testSecurityCanBeDisabled() {
        System.setProperty("cedarsoftware.security.enabled", "false");
        assertFalse(RegexUtilities.isSecurityEnabled());
    }

    @Test
    void testRegexTimeoutEnabledByDefault() {
        System.clearProperty("cedarsoftware.regex.timeout.enabled");
        assertTrue(RegexUtilities.isRegexTimeoutEnabled());
    }

    @Test
    void testRegexTimeoutCanBeDisabled() {
        System.setProperty("cedarsoftware.regex.timeout.enabled", "false");
        assertFalse(RegexUtilities.isRegexTimeoutEnabled());
    }

    @Test
    void testDefaultTimeoutIs5000ms() {
        System.clearProperty("cedarsoftware.regex.timeout.milliseconds");
        assertEquals(5000L, RegexUtilities.getRegexTimeoutMilliseconds());
    }

    @Test
    void testTimeoutCanBeConfigured() {
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "10000");
        assertEquals(10000L, RegexUtilities.getRegexTimeoutMilliseconds());
    }

    @Test
    void testInvalidTimeoutFallsBackToDefault() {
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "-1");
        assertEquals(5000L, RegexUtilities.getRegexTimeoutMilliseconds());

        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "not-a-number");
        assertEquals(5000L, RegexUtilities.getRegexTimeoutMilliseconds());
    }

    // ========== Pattern Caching Tests ==========

    @Test
    void testGetCachedPattern() {
        Pattern pattern1 = RegexUtilities.getCachedPattern("\\d+");
        Pattern pattern2 = RegexUtilities.getCachedPattern("\\d+");

        assertNotNull(pattern1);
        assertSame(pattern1, pattern2, "Same pattern instance should be returned from cache");
    }

    @Test
    void testGetCachedPatternCaseInsensitive() {
        Pattern pattern1 = RegexUtilities.getCachedPattern("hello", true);
        Pattern pattern2 = RegexUtilities.getCachedPattern("hello", true);

        assertNotNull(pattern1);
        assertSame(pattern1, pattern2, "Same case-insensitive pattern should be cached");
        assertTrue(pattern1.matcher("HELLO").matches());
        assertTrue(pattern1.matcher("hello").matches());
    }

    @Test
    void testGetCachedPatternWithFlags() {
        Pattern pattern1 = RegexUtilities.getCachedPattern("test", Pattern.MULTILINE);
        Pattern pattern2 = RegexUtilities.getCachedPattern("test", Pattern.MULTILINE);

        assertNotNull(pattern1);
        assertSame(pattern1, pattern2, "Same pattern with flags should be cached");
    }

    @Test
    void testGetCachedPatternDifferentFlagsAreSeparate() {
        Pattern pattern1 = RegexUtilities.getCachedPattern("test");
        Pattern pattern2 = RegexUtilities.getCachedPattern("test", true);
        Pattern pattern3 = RegexUtilities.getCachedPattern("test", Pattern.MULTILINE);

        assertNotNull(pattern1);
        assertNotNull(pattern2);
        assertNotNull(pattern3);
        assertNotSame(pattern1, pattern2);
        assertNotSame(pattern1, pattern3);
        assertNotSame(pattern2, pattern3);
    }

    @Test
    void testGetCachedPatternInvalidRegex() {
        Pattern pattern1 = RegexUtilities.getCachedPattern("(unclosed");
        Pattern pattern2 = RegexUtilities.getCachedPattern("(unclosed");

        assertNull(pattern1);
        assertNull(pattern2, "Invalid pattern should be cached as null");
    }

    @Test
    void testGetCachedPatternNullRegex() {
        Pattern pattern = RegexUtilities.getCachedPattern(null);
        assertNull(pattern);
    }

    @Test
    void testClearPatternCache() {
        RegexUtilities.getCachedPattern("test1");
        RegexUtilities.getCachedPattern("test2", true);
        RegexUtilities.getCachedPattern("test3", Pattern.MULTILINE);

        Map<String, Object> stats = RegexUtilities.getPatternCacheStats();
        assertTrue((Integer) stats.get("totalCachedPatterns") > 0);

        RegexUtilities.clearPatternCache();

        stats = RegexUtilities.getPatternCacheStats();
        assertEquals(0, stats.get("totalCachedPatterns"));
    }

    @Test
    void testGetPatternCacheStats() {
        RegexUtilities.getCachedPattern("test1");
        RegexUtilities.getCachedPattern("test2", true);
        RegexUtilities.getCachedPattern("test3", Pattern.MULTILINE);
        RegexUtilities.getCachedPattern("(invalid");

        Map<String, Object> stats = RegexUtilities.getPatternCacheStats();

        assertNotNull(stats);
        assertEquals(1, stats.get("cacheSize"));
        assertEquals(1, stats.get("cacheSizeCaseInsensitive"));
        assertEquals(1, stats.get("cacheSizeWithFlags"));
        assertEquals(1, stats.get("invalidPatternCount"));
        assertEquals(3, stats.get("totalCachedPatterns"));
    }

    // ========== Safe Matches Tests ==========

    @Test
    void testSafeMatchesBasic() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        assertTrue(RegexUtilities.safeMatches(pattern, "12345"));
        assertFalse(RegexUtilities.safeMatches(pattern, "abc"));
    }

    @Test
    void testSafeMatchesNullPattern() {
        assertFalse(RegexUtilities.safeMatches(null, "test"));
    }

    @Test
    void testSafeMatchesNullInput() {
        Pattern pattern = RegexUtilities.getCachedPattern("test");
        assertFalse(RegexUtilities.safeMatches(pattern, null));
    }

    @Test
    void testSafeMatchesWithSecurityDisabled() {
        System.setProperty("cedarsoftware.security.enabled", "false");
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        assertTrue(RegexUtilities.safeMatches(pattern, "12345"));
    }

    // Note: Timeout testing is intentionally not included as it's highly environment-dependent.
    // Modern Java regex engines have optimizations that make it difficult to reliably test
    // timeout behavior across different JVM versions and platforms. The timeout mechanism
    // exists and is proven to work by the implementation, but creating a reliably slow
    // pattern for testing is not practical.

    // ========== Safe Find Tests ==========

    @Test
    void testSafeFindBasic() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abc123def");

        assertTrue(result.matched());
        assertEquals("123", result.group());
        assertEquals("123", result.group(0));
        assertEquals(3, result.start());
        assertEquals(6, result.end());
    }

    @Test
    void testSafeFindNoMatch() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abcdef");

        assertFalse(result.matched());
        assertNull(result.group());
        assertEquals(-1, result.start());
        assertEquals(-1, result.end());
    }

    @Test
    void testSafeFindWithGroups() {
        Pattern pattern = RegexUtilities.getCachedPattern("(\\d+)-([a-z]+)");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abc123-test456");

        assertTrue(result.matched());
        assertEquals("123-test", result.group(0));
        assertEquals("123", result.group(1));
        assertEquals("test", result.group(2));
        assertEquals(2, result.groupCount());
    }

    @Test
    void testSafeFindNullPattern() {
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(null, "test");
        assertFalse(result.matched());
    }

    @Test
    void testSafeFindNullInput() {
        Pattern pattern = RegexUtilities.getCachedPattern("test");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, null);
        assertFalse(result.matched());
    }

    // ========== Safe Replace Tests ==========

    @Test
    void testSafeReplaceFirst() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        String result = RegexUtilities.safeReplaceFirst(pattern, "abc123def456", "X");
        assertEquals("abcXdef456", result);
    }

    @Test
    void testSafeReplaceAll() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        String result = RegexUtilities.safeReplaceAll(pattern, "abc123def456", "X");
        assertEquals("abcXdefX", result);
    }

    @Test
    void testSafeReplaceFirstNullReplacement() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        String result = RegexUtilities.safeReplaceFirst(pattern, "abc123def", null);
        assertEquals("abcdef", result);
    }

    @Test
    void testSafeReplaceAllNullPattern() {
        String result = RegexUtilities.safeReplaceAll(null, "abc123", "X");
        assertEquals("abc123", result);
    }

    @Test
    void testSafeReplaceAllNullInput() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        String result = RegexUtilities.safeReplaceAll(pattern, null, "X");
        assertNull(result);
    }

    // ========== Safe Split Tests ==========

    @Test
    void testSafeSplit() {
        Pattern pattern = RegexUtilities.getCachedPattern(",");
        String[] result = RegexUtilities.safeSplit(pattern, "a,b,c,d");

        assertEquals(4, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("d", result[3]);
    }

    @Test
    void testSafeSplitNullPattern() {
        String[] result = RegexUtilities.safeSplit(null, "a,b,c");
        assertEquals(1, result.length);
        assertEquals("a,b,c", result[0]);
    }

    @Test
    void testSafeSplitNullInput() {
        Pattern pattern = RegexUtilities.getCachedPattern(",");
        String[] result = RegexUtilities.safeSplit(pattern, null);
        assertEquals(1, result.length);
        assertNull(result[0]);
    }

    // ========== SafeMatchResult Tests ==========

    @Test
    void testSafeMatchResultGroupOutOfRange() {
        Pattern pattern = RegexUtilities.getCachedPattern("(\\d+)");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "123");

        assertNotNull(result.group(0));
        assertNotNull(result.group(1));
        assertNull(result.group(2));  // Out of range
        assertNull(result.group(-1));  // Negative index
    }

    @Test
    void testSafeMatchResultUnmatchedGroupCount() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abc");

        assertFalse(result.matched());
        assertEquals(0, result.groupCount());
    }

    @Test
    void testSafeMatchResultGetReplacement() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abc123def");

        assertTrue(result.matched());
        assertEquals("123", result.group());
        assertEquals("abcdef", result.getReplacement());
    }

    @Test
    void testSafeMatchResultGetReplacementNoMatch() {
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(pattern, "abcdef");

        assertFalse(result.matched());
        assertEquals("abcdef", result.getReplacement());
    }

    // ========== Integration Tests ==========

    @Test
    void testCachedPatternWithSafeOperations() {
        // Verify that cached patterns work correctly with all safe operations
        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");

        // Use the same cached pattern for multiple operations
        assertTrue(RegexUtilities.safeMatches(pattern, "12345"));
        assertTrue(RegexUtilities.safeFind(pattern, "abc123").matched());
        assertEquals("abcX", RegexUtilities.safeReplaceFirst(pattern, "abc123", "X"));
        assertEquals("abcXdefX", RegexUtilities.safeReplaceAll(pattern, "abc123def456", "X"));
        assertEquals(2, RegexUtilities.safeSplit(pattern, "abc123def").length);
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // Test that concurrent access to pattern cache is thread-safe
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    Pattern pattern = RegexUtilities.getCachedPattern("test" + (index % 3));
                    assertNotNull(pattern);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Should have cached 3 different patterns
        Map<String, Object> stats = RegexUtilities.getPatternCacheStats();
        assertEquals(3, stats.get("cacheSize"));
    }

    @Test
    void testFastPathWhenTimeoutDisabled() {
        System.setProperty("cedarsoftware.regex.timeout.enabled", "false");

        Pattern pattern = RegexUtilities.getCachedPattern("\\d+");

        // These should use fast path (no ExecutorService overhead)
        assertTrue(RegexUtilities.safeMatches(pattern, "12345"));
        assertTrue(RegexUtilities.safeFind(pattern, "abc123").matched());
        assertEquals("abcX", RegexUtilities.safeReplaceFirst(pattern, "abc123", "X"));
    }

    @Test
    void testSafeMatchesPreservesInterruptStatus() throws InterruptedException {
        System.setProperty("cedarsoftware.security.enabled", "true");
        System.setProperty("cedarsoftware.regex.timeout.enabled", "true");
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "10000");

        Pattern pattern = RegexUtilities.getCachedPattern("(a+)+$");
        String input = repeat('a', 20000) + "X";

        AtomicReference<String> outcome = new AtomicReference<>("none");
        AtomicBoolean interruptedAfter = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            try {
                RegexUtilities.safeMatches(pattern, input);
                outcome.set("completed");
            } catch (SecurityException e) {
                outcome.set("security");
            }
            interruptedAfter.set(Thread.currentThread().isInterrupted());
        });

        worker.start();
        Thread.sleep(50);
        worker.interrupt();
        worker.join(2000);

        assertFalse(worker.isAlive(), "Worker thread should finish promptly after interruption");
        assertEquals("security", outcome.get());
        assertTrue(interruptedAfter.get(), "Interrupted status should be preserved");
    }

    @Test
    void testTimeoutThreadGrowthIsBounded() throws InterruptedException {
        System.setProperty("cedarsoftware.security.enabled", "true");
        System.setProperty("cedarsoftware.regex.timeout.enabled", "true");
        System.setProperty("cedarsoftware.regex.timeout.milliseconds", "1");

        Pattern pattern = RegexUtilities.getCachedPattern("(a+)+$");
        String input = repeat('a', 5000) + "X";

        int before = countRegexTimeoutThreads();
        for (int i = 0; i < 30; i++) {
            try {
                RegexUtilities.safeMatches(pattern, input);
            } catch (SecurityException ignored) {
            }
        }

        Thread.sleep(200);
        int after = countRegexTimeoutThreads();

        assertTrue(after - before <= 8,
                "Timeout worker thread growth should be bounded (before=" + before + ", after=" + after + ")");
    }

    private static int countRegexTimeoutThreads() {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith("RegexUtilities-Timeout-Thread")) {
                count++;
            }
        }
        return count;
    }

    private static String repeat(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}
