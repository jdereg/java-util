package com.cedarsoftware.util;

import java.util.Map;
import java.util.regex.Pattern;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RegexUtilities' pattern caches are bounded. They kept every pattern ever compiled, and patterns compiled from data --
 * the Converter's String to Pattern, json-io reading a Pattern field -- came from outside: a stream of distinct ones
 * grew memory without limit. Each cache now keeps the {@value RegexUtilities#PATTERN_CACHE_CAPACITY} most recently
 * used, and a pattern in use stays cached.
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
class RegexUtilitiesBoundedCacheTest {

    private static final int MANY = 3 * RegexUtilities.PATTERN_CACHE_CAPACITY;

    @BeforeEach
    @AfterEach
    void clear() {
        RegexUtilities.clearPatternCache();
    }

    private static int stat(String name) {
        Map<String, Object> stats = RegexUtilities.getPatternCacheStats();
        return (Integer) stats.get(name);
    }

    private static void assertBounded(String name) {
        int size = stat(name);
        assertTrue(size <= RegexUtilities.PATTERN_CACHE_CAPACITY, name + " holds " + size);
    }

    @Test
    void everyCacheIsBounded() {
        for (int i = 0; i < MANY; i++) {
            RegexUtilities.getCachedPattern("p" + i);
            RegexUtilities.getCachedPattern("ci" + i, true);
            RegexUtilities.getCachedPattern("m" + i, Pattern.MULTILINE);
            assertNull(RegexUtilities.getCachedPattern("bad(" + i));
            assertNull(RegexUtilities.getCachedPattern("bad[" + i, Pattern.DOTALL));
        }
        assertBounded("cacheSize");
        assertBounded("cacheSizeCaseInsensitive");
        assertBounded("cacheSizeWithFlags");
        assertBounded("invalidPatternCount");
        assertBounded("invalidPatternKeyCount");
    }

    /** The Converter's String to Pattern -- how a Pattern read from JSON is compiled -- stays bounded too. */
    @Test
    void patternsFromDataAreBounded() {
        Converter converter = new Converter(new DefaultConverterOptions());
        for (int i = 0; i < MANY; i++) {
            converter.convert("data-" + i + "\\d+", Pattern.class);
        }
        assertBounded("cacheSize");
    }

    /** A pattern in use is not evicted by a stream of others: the same compiled instance comes back. */
    @Test
    void aPatternInUseStaysCached() {
        Pattern hot = RegexUtilities.getCachedPattern("[a-z]+@[a-z]+\\.com");
        for (int i = 0; i < MANY; i++) {
            RegexUtilities.getCachedPattern("cold" + i);
            assertSame(hot, RegexUtilities.getCachedPattern("[a-z]+@[a-z]+\\.com"));
        }
    }
}
