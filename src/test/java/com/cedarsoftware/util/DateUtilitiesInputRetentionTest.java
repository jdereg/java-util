package com.cedarsoftware.util;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DateUtilities reuses one Matcher per pattern per thread, and a Matcher keeps the text it last matched. So the last
 * string each thread parsed stayed reachable after the call -- however large: with no length limit by default, a
 * 4 MB input that failed to parse stayed in memory until that thread parsed another date. The reused Matchers now
 * let go of their input once a parse has read what it needs.
 */
class DateUtilitiesInputRetentionTest {

    private static boolean collected(WeakReference<String> ref) throws InterruptedException {
        for (int i = 0; i < 20 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(25);
        }
        return ref.get() == null;
    }

    @Test
    void aLargeInputThatFailsToParseIsNotKept() throws InterruptedException {
        char[] filler = new char[2_000_000];
        Arrays.fill(filler, 'x');
        String input = "2024-01-15 " + new String(filler);
        WeakReference<String> ref = new WeakReference<>(input);
        try {
            DateUtilities.parseDate(input, ZoneId.of("UTC"), true);
            fail("trailing text should not parse");
        } catch (IllegalArgumentException expected) {
            // what is under test is what stays reachable afterwards
        }
        input = null;
        assertNull(collected(ref) ? null : "still reachable", "the input outlived the parse");
    }

    @Test
    void anInputThatParsesIsNotKeptEither() throws InterruptedException {
        String input = new String("Jan 15, 2024 10:30:00");        // its own instance, not an interned literal
        WeakReference<String> ref = new WeakReference<>(input);
        DateUtilities.parseDate(input, ZoneId.of("UTC"), true);
        input = null;
        assertNull(collected(ref) ? null : "still reachable", "the input outlived the parse");
    }
}
