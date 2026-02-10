package com.cedarsoftware.util.convert;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
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
class PatternConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testStringToPattern() {
        // Basic patterns
        assertPattern("\\d+", "123");
        assertPattern("\\w+", "abc123");
        assertPattern("[a-zA-Z]+", "abcXYZ");

        // Quantifiers
        assertPattern("a{1,3}", "a", "aa", "aaa");
        assertPattern("\\d*", "", "1", "123");
        assertPattern("\\w+?", "a", "ab");

        // Character classes
        assertPattern("\\s*\\w+\\s*", " abc ", "def", " ghi");
        assertPattern("[^\\s]+", "no_whitespace");

        // Groups and alternation
        assertPattern("(foo|bar)", "foo", "bar");
        assertPattern("(a(b)c)", "abc");

        // Anchors
        assertPattern("^abc$", "abc");
        assertPattern("\\Aabc\\Z", "abc");

        // Should trim input string
        Pattern p = converter.convert(" \\d+ ", Pattern.class);
        assertEquals("\\d+", p.pattern());
    }

    @Test
    void testPatternToString() {
        // Basic patterns
        assertThat(converter.convert(Pattern.compile("\\d+"), String.class)).isEqualTo("\\d+");
        assertThat(converter.convert(Pattern.compile("\\w+"), String.class)).isEqualTo("\\w+");

        // With flags
        assertThat(converter.convert(Pattern.compile("abc", Pattern.CASE_INSENSITIVE), String.class))
                .isEqualTo("abc");

        // Complex patterns
        assertThat(converter.convert(Pattern.compile("(foo|bar)[0-9]+"), String.class))
                .isEqualTo("(foo|bar)[0-9]+");

        // Special characters
        assertThat(converter.convert(Pattern.compile("\\t\\n\\r"), String.class))
                .isEqualTo("\\t\\n\\r");
    }

    @Test
    void testMapToPattern() {
        Map<String, Object> map = Collections.singletonMap(VALUE, "\\d+");
        Pattern pattern = converter.convert(map, Pattern.class);
        assertThat(pattern.pattern()).isEqualTo("\\d+");

        map = Collections.singletonMap(VALUE, "(foo|bar)");
        pattern = converter.convert(map, Pattern.class);
        assertThat(pattern.pattern()).isEqualTo("(foo|bar)");
    }

    @Test
    void testPatternToMap() {
        Pattern pattern = Pattern.compile("\\d+");
        Map<String, String> map = converter.convert(pattern, Map.class);
        assertThat(map).containsEntry(VALUE, "\\d+");

        pattern = Pattern.compile("(foo|bar)");
        map = converter.convert(pattern, Map.class);
        assertThat(map).containsEntry(VALUE, "(foo|bar)");
    }

    @Test
    void testPatternToPattern() {
        assertAll(
                () -> {
                    Pattern original = Pattern.compile("\\d+");
                    Pattern converted = converter.convert(original, Pattern.class);
                    assertThat(converted.pattern()).isEqualTo(original.pattern());
                    assertThat(converted.flags()).isEqualTo(original.flags());
                },
                () -> {
                    Pattern original = Pattern.compile("abc", Pattern.CASE_INSENSITIVE);
                    Pattern converted = converter.convert(original, Pattern.class);
                    assertThat(converted.pattern()).isEqualTo(original.pattern());
                    assertThat(converted.flags()).isEqualTo(original.flags());
                }
        );
    }

    // ---- Bug: Pattern flags lost in toMap() round-trip ----

    @Test
    void testPatternToMap_shouldPreserveFlags() {
        Pattern pattern = Pattern.compile("foo", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Map<String, Object> map = converter.convert(pattern, Map.class);

        assertThat(map).containsKey("flags");
        int flags = ((Number) map.get("flags")).intValue();
        assertThat(flags).isEqualTo(Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }

    @Test
    void testRoundTrip_patternWithFlags_shouldPreserve() {
        Pattern original = Pattern.compile("bar", Pattern.DOTALL | Pattern.UNICODE_CASE);

        Map<String, Object> map = converter.convert(original, Map.class);
        Pattern restored = converter.convert(map, Pattern.class);

        assertThat(restored.pattern()).isEqualTo(original.pattern());
        assertThat(restored.flags()).isEqualTo(original.flags());
    }

    @Test
    void testRoundTrip_flagsLost_caseInsensitiveBroken() {
        Pattern original = Pattern.compile("foo", Pattern.CASE_INSENSITIVE);
        assertThat(original.matcher("FOO").matches()).isTrue();

        Map<String, Object> map = converter.convert(original, Map.class);
        Pattern restored = converter.convert(map, Pattern.class);

        assertThat(restored.flags()).isEqualTo(original.flags());
        assertThat(restored.matcher("FOO").matches())
                .as("Restored pattern should match case-insensitively (flags must be preserved)")
                .isTrue();
    }

    private void assertPattern(String pattern, String... matchingStrings) {
        Pattern p = converter.convert(pattern, Pattern.class);
        assertThat(p.pattern()).isEqualTo(pattern);
        for (String s : matchingStrings) {
            assertThat(p.matcher(s).matches())
                    .as("Pattern '%s' should match '%s'", pattern, s)
                    .isTrue();
        }
    }
}