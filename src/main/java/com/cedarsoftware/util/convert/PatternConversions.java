package com.cedarsoftware.util.convert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.cedarsoftware.util.convert.MapConversions.VALUE;

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
final class PatternConversions {

    // the flags that have an inline form, and their letters: "(?i)abc" is "abc" compiled CASE_INSENSITIVE
    private static final int[] INLINE_FLAGS = {Pattern.CASE_INSENSITIVE, Pattern.UNIX_LINES, Pattern.MULTILINE,
            Pattern.DOTALL, Pattern.UNICODE_CASE, Pattern.COMMENTS, Pattern.UNICODE_CHARACTER_CLASS};
    private static final String INLINE_LETTERS = "idmsuxU";

    /**
     * The regex with its flags written inline, in Java's own syntax, so the text compiles back to a Pattern that
     * matches what this one matches: "(?i)abc". LITERAL is written as the quoted text, \Q...\E. CANON_EQ has no
     * inline form and is left out; the Map form keeps it.
     */
    static String toString(Object from, Converter converter) {
        Pattern pattern = (Pattern) from;
        int flags = compiledFlags(pattern);
        String regex = pattern.pattern();
        if (flags == 0) {
            return regex;
        }
        if ((flags & Pattern.LITERAL) != 0) {
            regex = Pattern.quote(regex);
        }
        StringBuilder inline = new StringBuilder(regex.length() + 10).append("(?");
        for (int i = 0; i < INLINE_FLAGS.length; i++) {
            if ((flags & INLINE_FLAGS[i]) != 0) {
                inline.append(INLINE_LETTERS.charAt(i));
            }
        }
        if (inline.length() == 2) {
            return regex;
        }
        return inline.append(')').append(regex).toString();
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Pattern pattern = (Pattern) from;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(VALUE, pattern.pattern());
        int flags = compiledFlags(pattern);
        if (flags != 0) {
            map.put(MapConversions.FLAGS, flags);
        }
        return map;
    }

    /**
     * The flags the Pattern was compiled with. Pattern.flags() also reports the flags the regex turns on by itself at
     * its top level: "(?i)abc" compiled without flags reports CASE_INSENSITIVE. Those are left out, so "(?i)abc" is
     * not written as "(?i)(?i)abc", and "a(?i)b" is not made "(?i)a(?i)b", which would fold the case of the 'a' too.
     * When the two cannot be told apart, nothing is added that the regex does not already say.
     */
    private static int compiledFlags(Pattern pattern) {
        int flags = pattern.flags();
        String regex = pattern.pattern();
        if (flags == 0 || (flags & Pattern.LITERAL) != 0 || regex.indexOf('?') < 0) {
            return flags;   // no inline flag in the regex, or none read
        }
        try {
            int own = Pattern.compile(regex).flags();
            if (own == flags) {
                return 0;
            }
            own &= flags;
            if (own == 0) {
                return flags;
            }
            int compiled = flags & ~own;
            // the regex reads differently under COMMENTS ("#(?i)" is a comment there): keep what Pattern reports
            return Pattern.compile(regex, compiled).flags() == flags ? compiled : flags;
        } catch (PatternSyntaxException e) {
            return flags;
        }
    }
}
