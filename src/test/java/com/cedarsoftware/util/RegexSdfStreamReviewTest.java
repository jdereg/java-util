package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the RegexUtilities / SafeSimpleDateFormat / StreamGobbler / SystemUtilities review fixes:
 *
 * 1. RegexUtilities timeout protection now actually stops runaway matches: java.util.regex
 *    never checks interrupts, so Future.cancel(true) left each timed-out match burning a
 *    pool thread until backtracking completed. With the bounded executor (max 8 threads),
 *    a handful of ReDoS inputs permanently saturated the pool, and every subsequent regex
 *    operation — including trivial ones — failed. Matching now runs over an
 *    interrupt-checking CharSequence (same mechanism as DateUtilities).
 *
 * 2. SafeSimpleDateFormat.clone() was inherited from DateFormat and shared the
 *    AtomicReference configuration state — mutating a clone reconfigured the original.
 *
 * 3. SafeSimpleDateFormat declared serialVersionUID but its State/NFSig were not
 *    Serializable, so writeObject threw NotSerializableException.
 */
class RegexSdfStreamReviewTest {

    // ---------------------------------------------------------------
    // 1. RegexUtilities — ReDoS timeout frees pool threads
    // ---------------------------------------------------------------

    @Test
    void testRedosTimeoutFreesExecutorThreads() {
        String originalTimeout = System.getProperty("cedarsoftware.regex.timeout.milliseconds");
        try {
            System.setProperty("cedarsoftware.regex.timeout.milliseconds", "75");

            // Sequential unbounded quantifiers: still catastrophic on modern JDKs (JDK 9+'s
            // loop memoization defuses the classic "(a+)+$" form, but not sequential stars —
            // this backtracks O(n^14) positions before concluding '!' never matches)
            Pattern evil = RegexUtilities.getCachedPattern("a*a*a*a*a*a*a*a*a*a*a*a*a*a*!");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 80; i++) {
                sb.append('a');
            }
            String attack = sb.toString();

            // More timed-out operations than the executor has threads (max 8). Before the fix,
            // each one left a pool thread burning CPU indefinitely.
            for (int i = 0; i < 10; i++) {
                assertThrows(SecurityException.class, () -> RegexUtilities.safeMatches(evil, attack));
            }

            // With interrupt-aborting matches, the pool threads are free again — a trivial
            // protected match must succeed. Before the fix this timed out behind the stuck
            // threads and threw SecurityException.
            Pattern simple = RegexUtilities.getCachedPattern("\\d+");
            assertTrue(RegexUtilities.safeMatches(simple, "12345"));
            assertEquals("x-y", RegexUtilities.safeReplaceAll(RegexUtilities.getCachedPattern("\\s+"), "x y", "-"));
        } finally {
            if (originalTimeout == null) {
                System.clearProperty("cedarsoftware.regex.timeout.milliseconds");
            } else {
                System.setProperty("cedarsoftware.regex.timeout.milliseconds", originalTimeout);
            }
        }
    }

    @Test
    void testSafeOperationsStillCorrectWithProtectionEnabled() {
        Pattern p = RegexUtilities.getCachedPattern("(\\d+)-(\\d+)");
        RegexUtilities.SafeMatchResult result = RegexUtilities.safeFind(p, "order 123-456 shipped");
        assertTrue(result.matched());
        assertEquals("123-456", result.group());
        assertEquals("123", result.group(1));
        assertEquals("456", result.group(2));

        String[] parts = RegexUtilities.safeSplit(RegexUtilities.getCachedPattern(","), "a,b,c");
        assertEquals(3, parts.length);
        assertEquals("b", parts[1]);
    }

    // ---------------------------------------------------------------
    // 2. SafeSimpleDateFormat.clone() detaches configuration state
    // ---------------------------------------------------------------

    @Test
    void testCloneIsDetachedFromOriginal() {
        SafeSimpleDateFormat original = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        original.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date moment = new Date(86400000L * 10000);   // fixed instant

        String before = original.format(moment);

        SafeSimpleDateFormat copy = (SafeSimpleDateFormat) original.clone();
        copy.setTimeZone(TimeZone.getTimeZone("GMT+05:00"));

        assertEquals(before, original.format(moment),
                "mutating the clone must not reconfigure the original");
        assertNotEquals(original.format(moment), copy.format(moment),
                "clone must carry its own timezone");
    }

    // ---------------------------------------------------------------
    // 3. SafeSimpleDateFormat serialization round-trip
    // ---------------------------------------------------------------

    @Test
    void testSerializationRoundTrip() throws Exception {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy/MM/dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date moment = new Date(86400000L * 12345);
        String expected = sdf.format(moment);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(sdf);   // previously: NotSerializableException (State)
        }
        SafeSimpleDateFormat revived;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            revived = (SafeSimpleDateFormat) ois.readObject();
        }

        assertEquals(expected, revived.format(moment));
        assertEquals(sdf.toString(), revived.toString());   // pattern preserved
    }
}
