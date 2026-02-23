package com.cedarsoftware.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class for safe and efficient regular expression operations.
 * Provides ReDoS (Regular Expression Denial of Service) protection through
 * timeout enforcement and performance optimization through pattern caching.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>ReDoS Protection:</b> Enforces configurable timeouts on regex operations to prevent catastrophic backtracking</li>
 *   <li><b>Pattern Caching:</b> Caches compiled Pattern objects to avoid repeated compilation overhead</li>
 *   <li><b>Thread Safety:</b> All operations are thread-safe with concurrent caching</li>
 *   <li><b>Invalid Pattern Tracking:</b> Remembers invalid patterns to avoid repeated compilation attempts</li>
 * </ul>
 *
 * <h2>Security Configuration</h2>
 * Security features can be controlled via system properties:
 * <ul>
 *   <li><code>cedarsoftware.security.enabled</code> - Enable/disable all security features (default: true)</li>
 *   <li><code>cedarsoftware.regex.timeout.enabled</code> - Enable/disable regex timeout (default: true)</li>
 *   <li><code>cedarsoftware.regex.timeout.milliseconds</code> - Timeout in milliseconds (default: 5000)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>
 * // Safe matching with timeout protection
 * Pattern pattern = RegexUtilities.getCachedPattern("\\d+");
 * boolean matches = RegexUtilities.safeMatches(pattern, "12345");
 *
 * // Safe find operation with result capture
 * SafeMatchResult result = RegexUtilities.safeFind(pattern, "abc123def");
 * if (result.matched()) {
 *     String found = result.group(0);  // "123"
 * }
 *
 * // Case-insensitive pattern caching
 * Pattern ciPattern = RegexUtilities.getCachedPattern("hello", true);
 * </pre>
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
public final class RegexUtilities {

    // System property keys for configuration
    private static final String SECURITY_ENABLED_PROPERTY = "cedarsoftware.security.enabled";
    private static final String REGEX_TIMEOUT_ENABLED_PROPERTY = "cedarsoftware.regex.timeout.enabled";
    private static final String REGEX_TIMEOUT_MS_PROPERTY = "cedarsoftware.regex.timeout.milliseconds";

    // Default configuration values
    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final int MAX_TIMEOUT_THREADS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
    private static final int MAX_TIMEOUT_QUEUE_SIZE = MAX_TIMEOUT_THREADS * 16;

    // Pattern caches - separate caches for different flag combinations
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMapNullSafe<>();
    private static final Map<String, Pattern> PATTERN_CACHE_CI = new ConcurrentHashMapNullSafe<>();
    private static final Map<PatternCacheKey, Pattern> PATTERN_CACHE_FLAGS = new ConcurrentHashMapNullSafe<>();

    // Invalid pattern tracking to avoid repeated compilation errors
    private static final Set<String> INVALID_PATTERNS = ConcurrentHashMap.newKeySet();
    private static final Set<PatternCacheKey> INVALID_PATTERN_KEYS = ConcurrentHashMap.newKeySet();

    // Shared bounded executor for regex timeout operations.
    private static final ThreadPoolExecutor REGEX_EXECUTOR = createRegexExecutor();

    private RegexUtilities() {
        // Utility class - prevent instantiation
    }

    private static ThreadPoolExecutor createRegexExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadCounter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "RegexUtilities-Timeout-Thread-" + threadCounter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                MAX_TIMEOUT_THREADS,
                MAX_TIMEOUT_THREADS,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_TIMEOUT_QUEUE_SIZE),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static boolean isTimeoutProtectionEnabled() {
        return isSecurityEnabled() && isRegexTimeoutEnabled();
    }

    private static <T> T executeWithTimeout(Callable<T> operation, long timeout) {
        Future<T> future;
        try {
            future = REGEX_EXECUTOR.submit(operation);
        } catch (RejectedExecutionException e) {
            throw new SecurityException("Regex operation rejected due timeout executor saturation", e);
        }

        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new SecurityException("Regex operation interrupted", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SecurityException("Regex operation timed out (>" + timeout + "ms) - possible ReDoS attack", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new SecurityException("Regex operation failed: " + cause.getMessage(), cause);
        }
    }

    // ========== Configuration Methods ==========

    /**
     * Checks if security features are enabled.
     * @return true if security is enabled (default), false otherwise
     */
    public static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty(SECURITY_ENABLED_PROPERTY, "true"));
    }

    /**
     * Checks if regex timeout protection is enabled.
     * @return true if timeout is enabled (default), false otherwise
     */
    public static boolean isRegexTimeoutEnabled() {
        return Boolean.parseBoolean(System.getProperty(REGEX_TIMEOUT_ENABLED_PROPERTY, "true"));
    }

    /**
     * Gets the configured regex timeout in milliseconds.
     * @return timeout in milliseconds (default: 5000)
     */
    public static long getRegexTimeoutMilliseconds() {
        String value = System.getProperty(REGEX_TIMEOUT_MS_PROPERTY);
        if (value != null) {
            try {
                long timeout = Long.parseLong(value);
                if (timeout > 0L) {
                    return timeout;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to default
            }
        }
        return DEFAULT_TIMEOUT_MS;
    }

    // ========== Pattern Caching Methods ==========

    /**
     * Gets a cached Pattern for the given regex string.
     * Patterns are compiled once and cached for reuse.
     *
     * @param regex The regular expression string
     * @return Cached Pattern object, or null if the pattern is invalid
     */
    public static Pattern getCachedPattern(String regex) {
        if (regex == null) {
            return null;
        }

        // Check if this pattern is known to be invalid
        if (INVALID_PATTERNS.contains(regex)) {
            return null;
        }

        return PATTERN_CACHE.computeIfAbsent(regex, r -> {
            try {
                return Pattern.compile(r);
            } catch (PatternSyntaxException e) {
                INVALID_PATTERNS.add(r);
                return null;
            }
        });
    }

    /**
     * Gets a cached Pattern for the given regex string with case sensitivity option.
     *
     * @param regex The regular expression string
     * @param caseInsensitive If true, pattern matching will be case-insensitive
     * @return Cached Pattern object, or null if the pattern is invalid
     */
    public static Pattern getCachedPattern(String regex, boolean caseInsensitive) {
        if (!caseInsensitive) {
            return getCachedPattern(regex);
        }

        if (regex == null) {
            return null;
        }

        // Check if this pattern is known to be invalid
        if (INVALID_PATTERNS.contains(regex)) {
            return null;
        }

        return PATTERN_CACHE_CI.computeIfAbsent(regex, r -> {
            try {
                return Pattern.compile(r, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                INVALID_PATTERNS.add(r);
                return null;
            }
        });
    }

    /**
     * Gets a cached Pattern for the given regex string with specific flags.
     *
     * @param regex The regular expression string
     * @param flags Match flags, a bit mask from Pattern constants (CASE_INSENSITIVE, MULTILINE, etc.)
     * @return Cached Pattern object, or null if the pattern is invalid
     */
    public static Pattern getCachedPattern(String regex, int flags) {
        if (regex == null) {
            return null;
        }

        // For common cases, use specialized caches
        if (flags == 0) {
            return getCachedPattern(regex);
        }
        if (flags == Pattern.CASE_INSENSITIVE) {
            return getCachedPattern(regex, true);
        }

        PatternCacheKey key = new PatternCacheKey(regex, flags);

        // Check if this pattern is known to be invalid
        if (INVALID_PATTERN_KEYS.contains(key)) {
            return null;
        }

        return PATTERN_CACHE_FLAGS.computeIfAbsent(key, k -> {
            try {
                return Pattern.compile(k.regex, k.flags);
            } catch (PatternSyntaxException e) {
                INVALID_PATTERN_KEYS.add(k);
                return null;
            }
        });
    }

    /**
     * Clears all cached patterns. Useful for freeing memory in long-running applications.
     */
    public static void clearPatternCache() {
        PATTERN_CACHE.clear();
        PATTERN_CACHE_CI.clear();
        PATTERN_CACHE_FLAGS.clear();
        INVALID_PATTERNS.clear();
        INVALID_PATTERN_KEYS.clear();
    }

    /**
     * Gets statistics about the pattern cache.
     * @return Map containing cache statistics (size, invalidCount)
     */
    public static Map<String, Object> getPatternCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cacheSize", PATTERN_CACHE.size());
        stats.put("cacheSizeCaseInsensitive", PATTERN_CACHE_CI.size());
        stats.put("cacheSizeWithFlags", PATTERN_CACHE_FLAGS.size());
        stats.put("invalidPatternCount", INVALID_PATTERNS.size());
        stats.put("invalidPatternKeyCount", INVALID_PATTERN_KEYS.size());
        stats.put("totalCachedPatterns",
            PATTERN_CACHE.size() + PATTERN_CACHE_CI.size() + PATTERN_CACHE_FLAGS.size());
        return stats;
    }

    // ========== Safe Regex Operations ==========

    /**
     * Safely executes a pattern match operation with timeout protection.
     * This protects against ReDoS (Regular Expression Denial of Service) attacks.
     *
     * @param pattern The Pattern to match against
     * @param input The input string to match
     * @return true if the entire input matches the pattern, false otherwise
     * @throws SecurityException if the operation times out (possible ReDoS attack)
     */
    public static boolean safeMatches(Pattern pattern, String input) {
        if (pattern == null || input == null) {
            return false;
        }

        if (!isTimeoutProtectionEnabled()) {
            // Fast path when security disabled
            return pattern.matcher(input).matches();
        }

        long timeout = getRegexTimeoutMilliseconds();
        return executeWithTimeout(() -> pattern.matcher(input).matches(), timeout);
    }

    /**
     * Safely executes a pattern find operation with timeout protection.
     * Returns a SafeMatchResult containing the match data if found.
     *
     * @param pattern The Pattern to search for
     * @param input The input string to search
     * @return SafeMatchResult containing match data, or an unmatched result if not found
     * @throws SecurityException if the operation times out (possible ReDoS attack)
     */
    public static SafeMatchResult safeFind(Pattern pattern, String input) {
        if (pattern == null || input == null) {
            return new SafeMatchResult(null, input);
        }

        if (!isTimeoutProtectionEnabled()) {
            // Fast path when security disabled
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return new SafeMatchResult(matcher, input);
            } else {
                return new SafeMatchResult(null, input);
            }
        }

        long timeout = getRegexTimeoutMilliseconds();
        return executeWithTimeout(() -> {
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return new SafeMatchResult(matcher, input);
            } else {
                return new SafeMatchResult(null, input);
            }
        }, timeout);
    }

    /**
     * Safely replaces the first occurrence of the pattern with the replacement string.
     *
     * @param pattern The Pattern to search for
     * @param input The input string
     * @param replacement The replacement string
     * @return The input string with the first match replaced
     * @throws SecurityException if the operation times out (possible ReDoS attack)
     */
    public static String safeReplaceFirst(Pattern pattern, String input, String replacement) {
        if (pattern == null || input == null) {
            return input;
        }
        if (replacement == null) {
            replacement = "";
        }

        if (!isTimeoutProtectionEnabled()) {
            // Fast path when security disabled
            return pattern.matcher(input).replaceFirst(replacement);
        }

        long timeout = getRegexTimeoutMilliseconds();
        final String finalReplacement = replacement;
        return executeWithTimeout(() -> pattern.matcher(input).replaceFirst(finalReplacement), timeout);
    }

    /**
     * Safely replaces all occurrences of the pattern with the replacement string.
     *
     * @param pattern The Pattern to search for
     * @param input The input string
     * @param replacement The replacement string
     * @return The input string with all matches replaced
     * @throws SecurityException if the operation times out (possible ReDoS attack)
     */
    public static String safeReplaceAll(Pattern pattern, String input, String replacement) {
        if (pattern == null || input == null) {
            return input;
        }
        if (replacement == null) {
            replacement = "";
        }

        if (!isTimeoutProtectionEnabled()) {
            // Fast path when security disabled
            return pattern.matcher(input).replaceAll(replacement);
        }

        long timeout = getRegexTimeoutMilliseconds();
        final String finalReplacement = replacement;
        return executeWithTimeout(() -> pattern.matcher(input).replaceAll(finalReplacement), timeout);
    }

    /**
     * Safely splits the input string around matches of the pattern.
     *
     * @param pattern The Pattern to split on
     * @param input The input string to split
     * @return Array of strings split around pattern matches
     * @throws SecurityException if the operation times out (possible ReDoS attack)
     */
    public static String[] safeSplit(Pattern pattern, String input) {
        if (pattern == null || input == null) {
            return new String[] { input };
        }

        if (!isTimeoutProtectionEnabled()) {
            // Fast path when security disabled
            return pattern.split(input);
        }

        long timeout = getRegexTimeoutMilliseconds();
        return executeWithTimeout(() -> pattern.split(input), timeout);
    }

    // ========== Inner Classes ==========

    /**
     * Immutable container for regex match results.
     * This class safely captures match data from a Matcher object,
     * allowing the data to be used after the timeout-protected execution completes.
     */
    public static class SafeMatchResult {
        private final String[] groups;
        private final String replacement;
        private final boolean matched;
        private final int start;
        private final int end;

        /**
         * Creates a SafeMatchResult from a Matcher.
         * If matcher is null or didn't match, creates an unmatched result.
         *
         * @param matcher The Matcher to capture data from (null if no match)
         * @param originalInput The original input string
         */
        public SafeMatchResult(Matcher matcher, String originalInput) {
            if (matcher != null) {
                int groupCount = matcher.groupCount();
                this.groups = new String[groupCount + 1];
                for (int i = 0; i <= groupCount; i++) {
                    this.groups[i] = matcher.group(i);
                }
                this.matched = true;
                this.start = matcher.start();
                this.end = matcher.end();
                this.replacement = removeMatchedRange(originalInput, this.start, this.end);
            } else {
                this.groups = new String[0];
                this.replacement = originalInput;
                this.matched = false;
                this.start = -1;
                this.end = -1;
            }
        }

        /**
         * Returns the captured group at the specified index.
         *
         * @param index The group index (0 = entire match, 1+ = capturing groups)
         * @return The captured string, or null if index is out of range or no match
         */
        public String group(int index) {
            if (index < 0 || index >= groups.length) {
                return null;
            }
            return groups[index];
        }

        /**
         * Returns the entire matched string (equivalent to group(0)).
         *
         * @return The matched string, or null if no match
         */
        public String group() {
            return group(0);
        }

        /**
         * Returns the number of capturing groups in the pattern.
         *
         * @return Number of capturing groups (0 if no match)
         */
        public int groupCount() {
            return matched ? groups.length - 1 : 0;
        }

        /**
         * Checks if a match was found.
         *
         * @return true if the pattern matched, false otherwise
         */
        public boolean matched() {
            return matched;
        }

        /**
         * Returns the start index of the match.
         *
         * @return Start index, or -1 if no match
         */
        public int start() {
            return start;
        }

        /**
         * Returns the end index of the match.
         *
         * @return End index, or -1 if no match
         */
        public int end() {
            return end;
        }

        /**
         * Returns the input string with the first match replaced with empty string.
         * This is useful for iteratively processing a string by removing matched portions.
         *
         * @return The input string with the first match removed, or the original input if no match
         */
        public String getReplacement() {
            return replacement;
        }

        private static String removeMatchedRange(String input, int start, int end) {
            if (input == null) {
                return null;
            }
            if (start <= 0 && end >= input.length()) {
                return "";
            }

            StringBuilder builder = new StringBuilder(input.length() - (end - start));
            builder.append(input, 0, start);
            builder.append(input, end, input.length());
            return builder.toString();
        }
    }

    /**
     * Cache key for patterns with specific flags.
     * Uses both regex string and flags for equality/hashing.
     */
    private static class PatternCacheKey {
        private final String regex;
        private final int flags;
        private final int hashCode;

        PatternCacheKey(String regex, int flags) {
            this.regex = regex;
            this.flags = flags;
            this.hashCode = 31 * regex.hashCode() + flags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternCacheKey that = (PatternCacheKey) o;
            return flags == that.flags && regex.equals(that.regex);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
