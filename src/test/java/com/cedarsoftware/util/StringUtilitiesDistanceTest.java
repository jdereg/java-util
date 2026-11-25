package com.cedarsoftware.util;

import java.lang.management.ManagementFactory;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link StringUtilities#levenshteinDistance} and {@link StringUtilities#damerauLevenshteinDistance} give the
 * distances the textbook tables give, from rows instead of a table.
 * <p>
 * damerauLevenshteinDistance built the whole (n+1) x (m+1) table: 36 MB for two 3,000-character strings, 100 MB for
 * two of 5,000 (the length its own security limit suggests), and no heap is enough for two of 30,000. A cell looks
 * back two rows at most, so three rows of the target's length hold everything it reads. n-cube calls it for every
 * column of a NEAREST axis of Strings on every lookup.
 */
class StringUtilitiesDistanceTest {

    /** Levenshtein by the full table. */
    private static int levenshteinTable(CharSequence s, CharSequence t) {
        int[][] d = new int[s.length() + 1][t.length() + 1];
        for (int i = 0; i <= s.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= t.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= s.length(); i++) {
            for (int j = 1; j <= t.length(); j++) {
                int cost = s.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[s.length()][t.length()];
    }

    /** The same table with the swap of two neighbours as one edit: what damerauLevenshteinDistance computes. */
    private static int swapTable(CharSequence s, CharSequence t) {
        int[][] d = new int[s.length() + 1][t.length() + 1];
        for (int i = 0; i <= s.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= t.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= s.length(); i++) {
            for (int j = 1; j <= t.length(); j++) {
                int cost = s.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && s.charAt(i - 1) == t.charAt(j - 2) && s.charAt(i - 2) == t.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost);
                }
            }
        }
        return d[s.length()][t.length()];
    }

    private static String random(Random random, int maxLength) {
        int length = random.nextInt(maxLength + 1);
        StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            s.append((char) ('a' + random.nextInt(4)));        // four letters: plenty of repeats and swaps
        }
        return s.toString();
    }

    @Test
    void bothDistancesAreTheTablesOn150000RandomPairs() {
        Random random = new Random(20261125L);
        for (int i = 0; i < 150_000; i++) {
            int max = i % 50 == 0 ? 70 : 12;
            String s = random(random, max);
            String t = random(random, max);
            assertEquals(levenshteinTable(s, t), StringUtilities.levenshteinDistance(s, t), s + " / " + t);
            assertEquals(swapTable(s, t), StringUtilities.damerauLevenshteinDistance(s, t), s + " / " + t);
            if (i % 10 == 0) {              // any CharSequence, the same answer
                assertEquals(levenshteinTable(s, t),
                        StringUtilities.levenshteinDistance(new StringBuilder(s), new StringBuilder(t)));
                assertEquals(swapTable(s, t),
                        StringUtilities.damerauLevenshteinDistance(new StringBuilder(s), new StringBuilder(t)));
            }
        }
    }

    @Test
    void knownDistances() {
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(2, StringUtilities.levenshteinDistance("teh", "the"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("teh", "the"));          // one swap
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("CA", "ABC"));           // a swapped pair is not edited again
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", null));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance(null, "abc"));
        assertEquals(3, StringUtilities.levenshteinDistance("abc", ""));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("a", "b"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ab", "ba"));
        assertEquals(2, StringUtilities.levenshteinDistance("ab", "ba"));
    }

    /** Two 2,000-character strings: the table was 16 MB; three rows of 2,001 ints are 24 KB. */
    @Test
    void memoryGrowsWithOneStringNotWithBoth() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean, "needs per-thread allocation counts");
        com.sun.management.ThreadMXBean counts = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(counts.isThreadAllocatedMemorySupported() && counts.isThreadAllocatedMemoryEnabled());
        String s = StringUtilities.repeat("ab", 1000);
        String t = StringUtilities.repeat("ba", 1000);
        StringUtilities.damerauLevenshteinDistance("warm", "up");
        StringUtilities.levenshteinDistance("warm", "up");
        long id = Thread.currentThread().getId();

        long before = counts.getThreadAllocatedBytes(id);
        int swapDistance = StringUtilities.damerauLevenshteinDistance(s, t);
        long forSwaps = counts.getThreadAllocatedBytes(id) - before;
        before = counts.getThreadAllocatedBytes(id);
        int distance = StringUtilities.levenshteinDistance(s, t);
        long forEdits = counts.getThreadAllocatedBytes(id) - before;

        assertEquals(swapTable(s, t), swapDistance);
        assertEquals(levenshteinTable(s, t), distance);
        assertTrue(forSwaps < 200_000, "damerauLevenshteinDistance allocated " + forSwaps + " bytes");
        assertTrue(forEdits < 200_000, "levenshteinDistance allocated " + forEdits + " bytes");
    }

    /** Two 20,000-character strings: a 1.6 GB table before; now 400 million cell steps in three short rows. */
    @Test
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void longStrings() {
        String a = StringUtilities.repeat("a", 20_000);
        String b = StringUtilities.repeat("b", 20_000);
        assertEquals(20_000, StringUtilities.damerauLevenshteinDistance(a, b));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance(a, a.substring(1)));
        assertEquals(20_000, StringUtilities.levenshteinDistance(a, b));
    }
}
