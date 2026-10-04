package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Case-insensitive equality and hashing agree with the running JDK's String.equalsIgnoreCase(), including letters
 * outside the Basic Multilingual Plane. From JDK 16 on, String.equalsIgnoreCase() folds case by code point, so the
 * Deseret letters U+10400 and U+10428 are equal ignoring case; hashCodeIgnoreCase() folded one char at a time, so it
 * hashed them apart, and a case-insensitive MultiKeyMap, CaseInsensitiveMap, CompactMap or CompactSet missed the
 * key. Equality for other CharSequences (a StringBuilder) compared char by char, so it disagreed with String. The
 * hash now folds each surrogate pair as one code point (on every JDK: equal chars are equal code points), and a
 * CharSequence compares by code point exactly when the JDK's String does. The JDK is the oracle here.
 */
class StringUtilitiesSupplementaryCaseTest {

    private static final int[] POOL = {
            'a', 'A', 'z', 'Z', '0', ' ', 'k', 'K', 's', 'S', 'i', 'I',
            0x00E9, 0x00C9, 0x00DF, 0x1E9E, 0x017F, 0x212A, 0x0131, 0x0130,     // é É ß ẞ ſ Kelvin ı İ
            0x03A3, 0x03C3, 0x03C2, 0x10D0, 0x1C90,                             // Σ σ ς, Georgian ა Ა
            0x10400, 0x10428, 0x104B0, 0x104D8,                                 // Deseret, Osage,
            0x1E900, 0x1E922, 0x10C80, 0x10CC0,                                 // Adlam, Old Hungarian
            0x1F600, 0xD801, 0xDC00                                              // emoji, lone high, lone low surrogate
    };

    private static String randomString(Random random) {
        StringBuilder sb = new StringBuilder();
        int n = random.nextInt(6);
        for (int i = 0; i < n; i++) {
            sb.appendCodePoint(POOL[random.nextInt(POOL.length)]);
        }
        return sb.toString();
    }

    /** The same code points, each randomly upper-cased, lower-cased or kept. */
    private static String caseVariant(String s, Random random) {
        StringBuilder sb = new StringBuilder();
        s.codePoints().forEach(cp -> {
            int r = random.nextInt(3);
            sb.appendCodePoint(r == 0 ? Character.toUpperCase(cp) : r == 1 ? Character.toLowerCase(cp) : cp);
        });
        return sb.toString();
    }

    private static String show(String s) {
        StringBuilder sb = new StringBuilder("\"");
        s.chars().forEach(c -> sb.append(c < 128 ? String.valueOf((char) c) : String.format("\\u%04X", c)));
        return sb.append('"').toString();
    }

    @Test
    void equalityAndHashAgreeWithTheJdk() {
        Random random = new Random(42);
        List<String> wrong = new ArrayList<>();
        for (int round = 0; round < 40_000; round++) {
            String s = randomString(random);
            String t = random.nextBoolean() ? caseVariant(s, random) : randomString(random);
            boolean expected = s.equalsIgnoreCase(t);
            String pair = show(s) + " / " + show(t) + " (JDK: " + expected + ")";
            if (StringUtilities.equalsIgnoreCase(new StringBuilder(s), t) != expected
                    || StringUtilities.equalsIgnoreCase(s, new StringBuilder(t)) != expected
                    || StringUtilities.equalsIgnoreCase(new StringBuilder(s), new StringBuilder(t)) != expected) {
                wrong.add("equality " + pair);
            }
            if (expected && (StringUtilities.hashCodeIgnoreCase(s) != StringUtilities.hashCodeIgnoreCase(t)
                    || StringUtilities.hashCodeIgnoreCase(new StringBuilder(s))
                    != StringUtilities.hashCodeIgnoreCase(new StringBuilder(t)))) {
                wrong.add("hash " + pair);
            }
            if (StringUtilities.hashCodeIgnoreCase(s) != StringUtilities.hashCodeIgnoreCase(new StringBuilder(s))) {
                wrong.add("String vs StringBuilder hash " + show(s));
            }
            if (!s.isEmpty() && !t.isEmpty()) {
                int toffset = random.nextInt(s.length());
                int ooffset = random.nextInt(t.length());
                int len = random.nextInt(Math.min(s.length() - toffset, t.length() - ooffset) + 1);
                boolean region = s.regionMatches(true, toffset, t, ooffset, len);
                if (StringUtilities.regionMatches(new StringBuilder(s), true, toffset, t, ooffset, len) != region) {
                    wrong.add("regionMatches(" + toffset + ", " + ooffset + ", " + len + ") " + show(s) + " / "
                            + show(t) + " (JDK: " + region + ")");
                }
            }
        }
        if (!wrong.isEmpty()) {
            fail(wrong.size() + " disagreements with the JDK, first ones:\n"
                    + String.join("\n", wrong.subList(0, Math.min(20, wrong.size()))));
        }
    }

    /** The maps that hash with hashCodeIgnoreCase() find a key by any case the JDK calls equal. */
    @Test
    void caseInsensitiveMapsFindSupplementaryLetters() {
        String upper = new String(Character.toChars(0x10400));
        String lower = new String(Character.toChars(0x10428));
        String expected = upper.equalsIgnoreCase(lower) ? "v" : null;      // JDK 16+: equal; JDK 8-15: not

        MultiKeyMap<String> mkm = MultiKeyMap.<String>builder().caseSensitive(false).build();
        mkm.put(upper, "v");
        mkm.putMultiKey("v", "x", upper);
        assertEquals(expected, mkm.get(lower));
        assertEquals(expected, mkm.get(new StringBuilder(lower)));
        assertEquals(expected, mkm.getMultiKey("X", lower));

        Map<String, String> cim = new CaseInsensitiveMap<>();
        cim.put(upper, "v");
        assertEquals(expected, cim.get(lower));

        Map<String, String> compact = CompactMap.<String, String>builder().caseSensitive(false).build();
        compact.put(upper, "v");
        for (int i = 0; i < 40; i++) {
            compact.put("k" + i, "x");          // past the compact size, into the backing map
        }
        assertEquals(expected, compact.get(lower));
    }
}
