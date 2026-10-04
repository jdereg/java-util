package com.cedarsoftware.util.convert;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Pattern converted to a String and back matches what it matched. Pattern to String wrote pattern() alone and lost
 * the flags (CASE_INSENSITIVE came back case-sensitive); now the flags are written inline, "(?i)abc", in Java's own
 * syntax. String to Pattern trimmed the text, and whitespace is part of a regex: " x " became /x/.
 */
class ConverterPatternTextTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Pattern roundTrip(Pattern pattern) {
        return converter.convert(converter.convert(pattern, String.class), Pattern.class);
    }

    @Test
    void flagsAreWrittenInline() {
        assertEquals("abc", converter.convert(Pattern.compile("abc"), String.class));
        assertEquals("(?i)abc", converter.convert(Pattern.compile("abc", Pattern.CASE_INSENSITIVE), String.class));
        assertEquals("(?ms)^a.b$", converter.convert(Pattern.compile("^a.b$", Pattern.MULTILINE | Pattern.DOTALL),
                String.class));
        assertEquals("(?dx)a b", converter.convert(Pattern.compile("a b", Pattern.UNIX_LINES | Pattern.COMMENTS),
                String.class));
    }

    @Test
    void aPatternRoundTripsThroughItsTextMatchingWhatItMatched() {
        Pattern caseless = roundTrip(Pattern.compile("abc", Pattern.CASE_INSENSITIVE));
        assertTrue(caseless.matcher("ABC").matches());

        Pattern multiline = roundTrip(Pattern.compile("^b$", Pattern.MULTILINE));
        assertTrue(multiline.matcher("a\nb\nc").find());

        Pattern dotall = roundTrip(Pattern.compile("a.b", Pattern.DOTALL));
        assertTrue(dotall.matcher("a\nb").matches());

        Pattern comments = roundTrip(Pattern.compile("a b # spaces and comments are ignored", Pattern.COMMENTS));
        assertTrue(comments.matcher("ab").matches());

        Pattern unicode = roundTrip(Pattern.compile("é", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        assertTrue(unicode.matcher("É").matches());

        Pattern wordChars = roundTrip(Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS));
        assertTrue(wordChars.matcher("été").matches());

        // LITERAL is written as \\Q...\\E: the dot is a dot
        Pattern literal = roundTrip(Pattern.compile("a.b", Pattern.LITERAL));
        assertTrue(literal.matcher("a.b").matches());
        assertFalse(literal.matcher("axb").matches());
        Pattern literalCaseless = roundTrip(Pattern.compile("a.b\\E", Pattern.LITERAL | Pattern.CASE_INSENSITIVE));
        assertTrue(literalCaseless.matcher("A.B\\E").matches());
    }

    /**
     * Pattern.flags() also reports the flags a regex turns on by itself: "(?i)abc" compiled without flags reports
     * CASE_INSENSITIVE. Those are not written again: "(?i)abc" stays "(?i)abc", and "a(?i)b" does not become
     * "(?i)a(?i)b", which would fold the case of the 'a'. The Map form wrote them as flags, so "a(?i)b" read back
     * from its Map matched "Ab".
     */
    @Test
    void flagsTheRegexSetsItselfAreNotAddedAgain() {
        assertEquals("(?i)abc", converter.convert(Pattern.compile("(?i)abc"), String.class));
        assertEquals("a(?i)b", converter.convert(Pattern.compile("a(?i)b"), String.class));
        assertEquals("(?m)a(?i)b", converter.convert(Pattern.compile("a(?i)b", Pattern.MULTILINE), String.class));
        // a flag the regex turns off at its top level is not reported at all, and the regex says it anyway
        assertEquals("(?-i)abc", converter.convert(Pattern.compile("(?-i)abc", Pattern.CASE_INSENSITIVE),
                String.class));
        // under COMMENTS, "#(?i)" is a comment, not a flag
        assertEquals("(?x)a #(?i)\n b", converter.convert(Pattern.compile("a #(?i)\n b", Pattern.COMMENTS),
                String.class));

        Pattern twice = roundTrip(roundTrip(Pattern.compile("(?i)abc")));
        assertEquals("(?i)abc", twice.pattern());
        assertFalse(roundTrip(Pattern.compile("a(?i)b")).matcher("Ab").matches());
        assertTrue(roundTrip(Pattern.compile("a(?i)b")).matcher("aB").matches());

        Map<?, ?> map = converter.convert(Pattern.compile("a(?i)b"), Map.class);
        assertFalse(map.containsKey("flags"), map.toString());
        assertFalse(converter.convert(map, Pattern.class).matcher("Ab").matches());
    }

    @Test
    void whitespaceIsPartOfTheRegex() {
        assertEquals(" x ", converter.convert(" x ", Pattern.class).pattern());
        assertTrue(converter.convert(" x ", Pattern.class).matcher(" x ").matches());
        assertEquals(" x ", roundTrip(Pattern.compile(" x ")).pattern());
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("value", " x ");
        assertEquals(" x ", converter.convert(map, Pattern.class).pattern());
    }

    /** The Map form keeps the flags as a number, as before. */
    @Test
    void theMapFormKeepsTheFlags() {
        Pattern caseless = Pattern.compile("abc", Pattern.CASE_INSENSITIVE);
        Map<?, ?> map = converter.convert(caseless, Map.class);
        assertEquals("abc", map.get("value"));
        Pattern back = converter.convert(map, Pattern.class);
        assertEquals(Pattern.CASE_INSENSITIVE, back.flags());
        assertEquals("abc", back.pattern());
    }
}
