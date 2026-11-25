package com.cedarsoftware.util;

import java.util.Random;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringUtilities#wildcardToRegexString(String)} matches what it always matched, in time that grows with the
 * text, not with the number of stars.
 * <p>
 * Each '*' used to become its own {@code .*}. When the text does not match, the regex engine then tries every way of
 * sharing the text among them: {@code *********x} against 40 letters took 6 to 9 seconds, and each further star
 * multiplied that; {@code *a*a*a*x} against 300 letters took a second. The pattern-length and wildcard-count limits
 * did not help: eight stars are far inside them. A wildcard typed into a search box is enough, and n-cube's cube
 * search runs one against every cube name.
 * <p>
 * Now the text between two stars is matched at its first place and kept ({@code (?>.*?text)}, an atomic group), and
 * stars in a row are one star. A wildcard is matched the way a glob matcher walks it, with nothing to go back over.
 */
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class StringUtilitiesWildcardTest {

    /** The translation before 4.119.0: each '*' its own ".*". It is the oracle for what a wildcard matches. */
    private static String starByStar(String wildcard) {
        StringBuilder s = new StringBuilder("^");
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            if (c == '*') {
                s.append(".*");
            } else if (c == '?') {
                s.append('.');
            } else if ("()[]$^.{}|\\+".indexOf(c) >= 0) {
                s.append('\\').append(c);
            } else {
                s.append(c);
            }
        }
        return s.append('$').toString();
    }

    private static String random(Random random, String alphabet, int maxLength) {
        int length = random.nextInt(maxLength + 1);
        StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            s.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return s.toString();
    }

    private static String withoutAnchors(String regex) {
        return regex.substring(1, regex.length() - 1);
    }

    /**
     * 80,000 random wildcards and texts (stars, question marks, a dot, a parenthesis, line ends, both cases): the
     * new regex and the old one agree on matches(), and on find() with the anchors taken off and case ignored, which
     * is how n-cube searches cube names.
     */
    @Test
    void aWildcardMatchesWhatItAlwaysDid() {
        Random random = new Random(20261125L);
        for (int i = 0; i < 80_000; i++) {
            String wildcard = random(random, "ab*?x.*(A*\n", 8);
            String text = random(random, "abxAB.(\n", 14);
            String before = starByStar(wildcard);
            String now = StringUtilities.wildcardToRegexString(wildcard);
            String what = "wildcard [" + wildcard + "] text [" + text + "] regex " + now;
            assertEquals(Pattern.compile(before).matcher(text).matches(),
                    Pattern.compile(now).matcher(text).matches(), what);
            assertEquals(
                    Pattern.compile(withoutAnchors(before), Pattern.CASE_INSENSITIVE).matcher(text).find(),
                    Pattern.compile(withoutAnchors(now), Pattern.CASE_INSENSITIVE).matcher(text).find(), what);
            assertEquals(Pattern.compile("(?i)" + before).matcher(text).matches(),
                    Pattern.compile("(?i)" + now).matcher(text).matches(), what);
        }
    }

    /** The class timeout fails these on the earlier translation: the first took minutes, the second hours. */
    @Test
    void starsDoNotMultiplyTheWork() {
        String forty = StringUtilities.repeat("a", 40);
        String manyStars = StringUtilities.wildcardToRegexString(StringUtilities.repeat("*", 30) + "x");
        assertFalse(forty.matches(manyStars));
        assertTrue((forty + "x").matches(manyStars));
        assertFalse(Pattern.compile(withoutAnchors(manyStars), Pattern.CASE_INSENSITIVE).matcher(forty).find());

        String long100k = StringUtilities.repeat("a", 100_000);
        String spread = StringUtilities.wildcardToRegexString("*a*a*a*a*a*a*x");
        assertFalse(long100k.matches(spread));
        assertTrue((long100k + "x").matches(spread));
        // Without the anchors a search may start at every character, so it costs the text's length squared for any
        // wildcard that ends in a star and a letter, one star or six alike (100,000 characters: 6.6 s and 6.5 s).
        // 2,000 characters keep this to milliseconds; the earlier translation did not finish them.
        String long2k = StringUtilities.repeat("a", 2_000);
        assertFalse(Pattern.compile(withoutAnchors(spread), Pattern.CASE_INSENSITIVE).matcher(long2k).find());

        String questions = StringUtilities.wildcardToRegexString("*?a*?a*?a*?a*?a*b");
        assertFalse(StringUtilities.repeat("a", 2_000).matches(questions));
    }

    @Test
    void theRegexAWildcardBecomes() {
        // no star, or one star: as it always was
        assertEquals("^$", StringUtilities.wildcardToRegexString(""));
        assertEquals("^abc$", StringUtilities.wildcardToRegexString("abc"));
        assertEquals("^a.c$", StringUtilities.wildcardToRegexString("a?c"));
        assertEquals("^.*$", StringUtilities.wildcardToRegexString("*"));
        assertEquals("^.*\\.txt$", StringUtilities.wildcardToRegexString("*.txt"));
        assertEquals("^a.*$", StringUtilities.wildcardToRegexString("a*"));
        assertEquals("^com\\..*\\.util$", StringUtilities.wildcardToRegexString("com.*.util"));
        String specials = "a.b(c)[d]{e}|f\\g+h$i^j";
        assertEquals(starByStar(specials), StringUtilities.wildcardToRegexString(specials));

        // two stars or more: the text between two stars is found at its first place and kept
        assertEquals("^a(?>.*?b).*c$", StringUtilities.wildcardToRegexString("a*b*c"));
        assertEquals("^(?>.*?a)(?>.*?b.c).*$", StringUtilities.wildcardToRegexString("*a*b?c*"));
        assertEquals("^(?>.*?\\.)(?>.*?\\.).*$", StringUtilities.wildcardToRegexString("*.*.*"));

        // stars in a row are one star
        assertEquals("^.*$", StringUtilities.wildcardToRegexString("**"));
        assertEquals("^.*x$", StringUtilities.wildcardToRegexString("*********x"));
        assertEquals("^a(?>.*?b).*$", StringUtilities.wildcardToRegexString("a**b***"));
    }
}
