package com.cedarsoftware.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.MatchResult;
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

    /**
     * The longest regex the caches keep. A compiled Pattern takes about ten bytes for each character of its regex,
     * and the caches are bounded by how many they hold, not by how big: a thousand 50,000-character patterns read
     * from data (the Converter's String to Pattern, json-io reading a Pattern) held half a gigabyte in one cache.
     * A longer regex is compiled each time it is asked for and not kept.
     */
    static final int MAX_CACHED_REGEX_LENGTH = 4096;

    /**
     * The most patterns each cache keeps; the least recently used are evicted. They were unbounded, and patterns
     * compiled from data (the Converter's String to Pattern, json-io reading a Pattern) stayed forever, so a stream of
     * distinct ones grew memory without limit.
     */
    static final int PATTERN_CACHE_CAPACITY = 1000;

    // Pattern caches - separate caches for different flag combinations
    private static final Map<String, Pattern> PATTERN_CACHE = new LRUCache<>(PATTERN_CACHE_CAPACITY);
    private static final Map<String, Pattern> PATTERN_CACHE_CI = new LRUCache<>(PATTERN_CACHE_CAPACITY);
    private static final Map<PatternCacheKey, Pattern> PATTERN_CACHE_FLAGS = new LRUCache<>(PATTERN_CACHE_CAPACITY);

    // Invalid pattern tracking to avoid repeated compilation errors
    private static final Set<String> INVALID_PATTERNS =
            Collections.newSetFromMap(new LRUCache<>(PATTERN_CACHE_CAPACITY));
    private static final Set<PatternCacheKey> INVALID_PATTERN_KEYS =
            Collections.newSetFromMap(new LRUCache<>(PATTERN_CACHE_CAPACITY));

    private RegexUtilities() {
        // Utility class - prevent instantiation
    }

    private static boolean isTimeoutProtectionEnabled() {
        return isSecurityEnabled() && isRegexTimeoutEnabled();
    }

    /**
     * Runs a regex operation on the calling thread, over text that stops it when its time is up.
     * <p>
     * {@code java.util.regex} has no time limit and checks no interrupt, but it reads its input one character at a
     * time through {@link CharSequence#charAt}, so the input it is given here is a {@link TimedText}: every
     * {@value TimedText#READS_PER_CHECK} characters read it looks at the clock, and throws when the time is up or
     * the thread has been interrupted. A runaway match stops within microseconds of its limit, on the thread that
     * asked for it.
     * <p>
     * Until 4.119.0 the operation was handed to a pool of at most eight threads shared by the whole JVM, and the
     * caller waited for it. That cost about 6 microseconds for a match that takes 15 nanoseconds; with more callers
     * than the pool and its queue could take (136 on a large machine, 34 on two CPUs) harmless matches were
     * refused; and eight runaway matches in flight held every other caller for the whole timeout, since time
     * spent waiting for a pool thread counted against the caller. Whatever the operation threw came back wrapped
     * in a SecurityException, a StackOverflowError included.
     */
    private static <T> T runTimed(Function<CharSequence, T> operation, String input) {
        return runTimed(operation, input, getRegexTimeoutMilliseconds());
    }

    /** The same, with the caller's own time limit: DateUtilities has one. */
    static <T> T runTimed(Function<CharSequence, T> operation, String input, long timeout) {
        final long nanos = timeout > Long.MAX_VALUE / 1_000_000L ? Long.MAX_VALUE : timeout * 1_000_000L;
        try {
            return operation.apply(new TimedText(input, nanos));
        } catch (TimedText.Stopped stopped) {
            if (stopped.interrupted) {
                throw new SecurityException("Regex operation interrupted", stopped);
            }
            throw new SecurityException("Regex operation timed out (>" + timeout + "ms) - possible ReDoS attack",
                    stopped);
        }
    }

    // ========== Configuration Methods ==========

    /**
     * Checks if security features are enabled.
     * @return true if security is enabled (default), false otherwise
     */
    public static boolean isSecurityEnabled() {
        return !isFalse(System.getProperty(SECURITY_ENABLED_PROPERTY));
    }

    /**
     * A protection is off only when its property says "false" (any case, spaces around it ignored). Anything
     * else leaves it on: read with Boolean.parseBoolean, a stray space (" true"), "1" or "yes" switched it off.
     */
    private static boolean isFalse(String value) {
        return value != null && "false".equalsIgnoreCase(value.trim());
    }

    /**
     * Checks if regex timeout protection is enabled.
     * @return true if timeout is enabled (default), false otherwise
     */
    public static boolean isRegexTimeoutEnabled() {
        return !isFalse(System.getProperty(REGEX_TIMEOUT_ENABLED_PROPERTY));
    }

    /**
     * Gets the configured regex timeout in milliseconds.
     * @return timeout in milliseconds (default: 5000)
     */
    public static long getRegexTimeoutMilliseconds() {
        String value = System.getProperty(REGEX_TIMEOUT_MS_PROPERTY);
        if (value != null) {
            try {
                long timeout = Long.parseLong(value.trim());
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

        if (regex.length() > MAX_CACHED_REGEX_LENGTH) {
            return compileUncached(regex, 0);
        }

        Pattern cached = PATTERN_CACHE.get(regex);
        if (cached != null) {
            return cached;
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

        if (regex.length() > MAX_CACHED_REGEX_LENGTH) {
            return compileUncached(regex, Pattern.CASE_INSENSITIVE);
        }

        Pattern cached = PATTERN_CACHE_CI.get(regex);
        if (cached != null) {
            return cached;
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

        if (regex.length() > MAX_CACHED_REGEX_LENGTH) {
            return compileUncached(regex, flags);
        }

        PatternCacheKey key = new PatternCacheKey(regex, flags);
        Pattern cached = PATTERN_CACHE_FLAGS.get(key);
        if (cached != null) {
            return cached;
        }

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

    /** A regex too long to keep ({@link #MAX_CACHED_REGEX_LENGTH}): compiled for this call, null if invalid. */
    private static Pattern compileUncached(String regex, int flags) {
        try {
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            return null;
        }
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

        return runTimed(text -> pattern.matcher(text).matches(), input);
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

        return runTimed(text -> {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return new SafeMatchResult(matcher, input);
            } else {
                return new SafeMatchResult(null, input);
            }
        }, input);
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

        final String finalReplacement = replacement;
        return runTimed(text -> pattern.matcher(text).replaceFirst(finalReplacement), input);
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

        final String finalReplacement = replacement;
        return runTimed(text -> pattern.matcher(text).replaceAll(finalReplacement), input);
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

        return runTimed(text -> pattern.split(text), input);
    }

    // ========== Inner Classes ==========

    /**
     * The input of one timed operation: the text itself, with a clock looked at once in every
     * {@value #READS_PER_CHECK} characters read. Reading the clock on every character would cost several times
     * what the read does; once in 1,024 is a look every few microseconds of a runaway match.
     * <p>
     * The time allowed runs from the first look, not from the call: an operation that ends within its first 1,024
     * reads, as nearly all do, never reads the clock at all. (On macOS every {@code System.nanoTime()} writes one
     * word shared by the whole JVM; read once per call, eight threads matching at once took 370 ns a match
     * instead of 40.)
     * <p>
     * It also stops for an interrupt, as the pooled version did, but only one that arrives during the operation: a
     * thread whose interrupt flag was already set when it asked can match, as it can with a plain Matcher.
     * What a Matcher reads after matching (groups, the text between matches) comes from {@link #subSequence} and
     * {@link #toString}, which are the String's own.
     */
    private static final class TimedText implements CharSequence {
        static final int READS_PER_CHECK = 1024;
        private final String text;
        private long start;                 // when the clock was first looked at; 0 until then
        private final long limitNanos;
        private final boolean interruptedBefore = Thread.currentThread().isInterrupted();
        private int readsLeft = READS_PER_CHECK;    // until the next look at the clock

        TimedText(String text, long limitNanos) {
            this.text = text;
            this.limitNanos = limitNanos;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            if (--readsLeft == 0) {
                readsLeft = READS_PER_CHECK;
                check();
            }
            return text.charAt(index);
        }

        private void check() {
            long now = System.nanoTime();
            if (start == 0) {
                start = now == 0 ? 1 : now;
            } else if (now - start > limitNanos) {
                throw new Stopped(false);
            }
            if (!interruptedBefore && Thread.currentThread().isInterrupted()) {
                throw new Stopped(true);
            }
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }

        @Override
        public String toString() {
            return text;
        }

        /** Thrown through the Matcher to end the operation; never seen outside this class. */
        static final class Stopped extends RuntimeException {
            private static final long serialVersionUID = 1L;
            final boolean interrupted;

            Stopped(boolean interrupted) {
                super(interrupted ? "interrupted" : "out of time", null, false, false);
                this.interrupted = interrupted;
            }
        }
    }

    /**
     * Immutable container for regex match results.
     * This class safely captures match data from a Matcher object,
     * allowing the data to be used after the timeout-protected execution completes.
     */
    public static class SafeMatchResult {
        // Lazy snapshot: matchResult is a MatchResult (Matcher.toMatchResult()) which copies the
        // internal int[] of group start/end indices but does NOT eagerly allocate group strings.
        // Each group(i) call lazily slices the input on demand. For patterns with many capturing
        // groups where callers read only a few, this is dramatically cheaper than the prior
        // "extract every group as a String at construction time" pattern.
        private final MatchResult matchResult;
        private final String input;
        private String replacement;         // made when first asked for: see getReplacement()
        private final boolean matched;

        /**
         * Creates a SafeMatchResult from a Matcher.
         * If matcher is null or holds no match, creates an unmatched result.
         *
         * @param matcher The Matcher to capture data from (null if no match)
         * @param originalInput The original input string
         */
        public SafeMatchResult(Matcher matcher, String originalInput) {
            MatchResult result = null;
            if (matcher != null) {
                try {
                    result = matcher.toMatchResult();
                    result.start();                 // throws when the matcher holds no match
                } catch (IllegalStateException noMatch) {
                    result = null;
                }
            }
            this.matchResult = result;
            this.input = originalInput;
            this.matched = result != null;
        }

        /**
         * Returns the captured group at the specified index.
         *
         * @param index The group index (0 = entire match, 1+ = capturing groups)
         * @return The captured string, or null if index is out of range or no match
         */
        public String group(int index) {
            if (matchResult == null || index < 0 || index > matchResult.groupCount()) {
                return null;
            }
            return matchResult.group(index);
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
            return matchResult == null ? 0 : matchResult.groupCount();
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
            return matchResult == null ? -1 : matchResult.start();
        }

        /**
         * Returns the end index of the match.
         *
         * @return End index, or -1 if no match
         */
        public int end() {
            return matchResult == null ? -1 : matchResult.end();
        }

        /**
         * Returns the input string with the first match replaced with empty string.
         * This is useful for iteratively processing a string by removing matched portions.
         *
         * @return The input string with the first match removed, or the original input if no match
         */
        public String getReplacement() {
            // Built here, not in the constructor: it is a copy of the whole input, and a caller that wants the
            // match and its groups never asks for it. Finding a date at the head of 10 MB of text took 3
            // milliseconds for the copy. (A race to build it builds the same String twice.)
            String result = replacement;
            if (result == null && input != null) {
                result = matched ? removeMatchedRange(input, matchResult.start(), matchResult.end()) : input;
                replacement = result;
            }
            return result;
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
