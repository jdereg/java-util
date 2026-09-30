package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * hashCodeIgnoreCase() folds an ASCII char by clearing its case bit ({@code c & 0xDF}) instead of branching on its
 * case: on keys of mixed case that branch was mispredicted about half the time. What must hold is consistency with
 * String.equalsIgnoreCase() -- two strings it calls equal hash alike -- through the String overload and the
 * CharSequence overload alike, since MultiKeyMap hashes a String key with one and a StringBuilder with the other.
 * The chars outside ASCII that fold INTO ASCII (KELVIN SIGN to 'k', dotless i to 'i', LONG S to 's') are the ones
 * a fold by bit gets wrong.
 */
class HashCodeIgnoreCaseAsciiFoldTest {

    private static int hashOf(String s) {
        return StringUtilities.hashCodeIgnoreCase(s);
    }

    private static int hashOfSequence(String s) {
        return StringUtilities.hashCodeIgnoreCase(new StringBuilder(s));
    }

    /** Every pair of chars String.equalsIgnoreCase() calls equal: same upper case, or same lower case of that. */
    @Test
    void everyPairOfCharsEqualIgnoringCaseHashesAlike() {
        Map<Integer, List<Character>> byUpper = new HashMap<>();
        Map<Integer, List<Character>> byFold = new HashMap<>();
        for (int c = 0; c <= 0xFFFF; c++) {
            char ch = (char) c;
            byUpper.computeIfAbsent((int) Character.toUpperCase(ch), k -> new ArrayList<>()).add(ch);
            byFold.computeIfAbsent((int) Character.toLowerCase(Character.toUpperCase(ch)), k -> new ArrayList<>())
                    .add(ch);
        }
        int pairs = 0;
        List<String> apart = new ArrayList<>();
        for (Map<Integer, List<Character>> groups : Arrays.asList(byUpper, byFold)) {
            for (List<Character> group : groups.values()) {
                for (char x : group) {
                    for (char y : group) {
                        String sx = String.valueOf(x);
                        String sy = String.valueOf(y);
                        if (!sx.equalsIgnoreCase(sy)) {
                            continue;
                        }
                        pairs++;
                        if (hashOf(sx) != hashOf(sy) || hashOfSequence(sx) != hashOfSequence(sy)) {
                            apart.add(String.format("U+%04X/U+%04X", (int) x, (int) y));
                        }
                    }
                }
            }
        }
        assertTrue(pairs > 2 * 65536, "pairs checked: " + pairs);
        assertTrue(apart.isEmpty(),
                apart.size() + " pairs hash apart, first " + apart.subList(0, Math.min(8, apart.size())));
    }

    @Test
    void charsThatFoldIntoAsciiHashAsTheAsciiLetterDoes() {
        String[][] spellings = {
                {"kelvin", "KELVIN", "Kelvin", "KELVIN"},     // KELVIN SIGN
                {"ice", "ICE", "ıce", "İCE"},                  // dotless i, I with dot above
                {"sun", "SUN", "ſun", "ſUN"},                  // LONG S
        };
        for (String[] group : spellings) {
            for (String a : group) {
                for (String b : group) {
                    if (a.equalsIgnoreCase(b)) {
                        assertEquals(hashOf(a), hashOf(b), a + " / " + b);
                        assertEquals(hashOfSequence(a), hashOfSequence(b), a + " / " + b);
                        assertEquals(hashOf(a), hashOfSequence(b), a + " / " + b);
                    }
                }
            }
        }

        CaseInsensitiveMap<String, Integer> cim = new CaseInsensitiveMap<>();
        cim.put("kelvin", 1);
        cim.put("ıce", 2);
        cim.put("SUN", 3);
        assertEquals(1, cim.get("Kelvin"));
        assertEquals(1, cim.get("KELVIN"));
        assertEquals(2, cim.get("ICE"));
        assertEquals(3, cim.get("ſun"));

        MultiKeyMap<Integer> mkm = MultiKeyMap.<Integer>builder().caseSensitive(false).build();
        mkm.put("kelvin", 1);
        mkm.put("ıce", 2);
        mkm.put("SUN", 3);
        assertEquals(1, mkm.get("Kelvin"));
        assertEquals(1, mkm.get(new StringBuilder("KELVIN")));
        assertEquals(2, mkm.get(new StringBuilder("ice")));
        assertEquals(3, mkm.get("ſun"));
    }

    /** MultiKeyMap hashes a String with one overload and any other CharSequence with the other: they must agree. */
    @Test
    void theStringAndCharSequenceOverloadsAgree() {
        for (int c = 0; c <= 0xFFFF; c++) {
            String s = String.valueOf((char) c);
            assertEquals(hashOf(s), hashOfSequence(s), String.format("U+%04X", c));
        }
        Random random = new Random(11);
        for (int k = 0; k < 200_000; k++) {
            String s = randomText(random);
            assertEquals(hashOf(s), hashOfSequence(s), s);
        }
    }

    /** Random text against random re-casings of itself, per code point, including surrogate pairs. */
    @Test
    void textReCasedHashesAlike() {
        Random random = new Random(3);
        int compared = 0;
        for (int k = 0; k < 200_000; k++) {
            StringBuilder text = new StringBuilder();
            StringBuilder reCased = new StringBuilder();
            int length = random.nextInt(24);
            for (int j = 0; j < length; j++) {
                int cp = randomCodePoint(random);
                text.appendCodePoint(cp);
                int pick = random.nextInt(3);
                reCased.appendCodePoint(
                        pick == 0 ? Character.toUpperCase(cp) : pick == 1 ? Character.toLowerCase(cp) : cp);
            }
            String a = text.toString();
            String b = reCased.toString();
            if (a.equalsIgnoreCase(b)) {
                compared++;
                assertEquals(hashOf(a), hashOf(b), a + " / " + b);
                assertEquals(hashOfSequence(a), hashOfSequence(b), a + " / " + b);
            }
        }
        assertTrue(compared > 100_000, "compared: " + compared);
    }

    private static String randomText(Random random) {
        StringBuilder sb = new StringBuilder();
        int length = random.nextInt(24);
        for (int j = 0; j < length; j++) {
            if (random.nextInt(20) == 0) {
                sb.append((char) (0xD800 + random.nextInt(0x800)));     // a lone surrogate, high or low
            } else {
                sb.appendCodePoint(randomCodePoint(random));
            }
        }
        return sb.toString();
    }

    /** Mostly ASCII, some Latin-1 and the rest of the BMP's cased scripts, some Deseret (cased, outside the BMP). */
    private static int randomCodePoint(Random random) {
        int pick = random.nextInt(10);
        if (pick < 6) {
            return random.nextInt(128);
        }
        if (pick < 8) {
            return 128 + random.nextInt(0x2F00);
        }
        if (pick < 9) {
            return random.nextBoolean() ? 0x212A : 0x0130 + random.nextInt(0x50);     // KELVIN SIGN, Latin Ext-A
        }
        return 0x10400 + random.nextInt(0x50);
    }
}
