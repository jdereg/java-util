package com.cedarsoftware.util;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DateUtilitiesNegativeTests {
    
    /**
     * 2) Garbled content or random text. This is 'unparseable' because
     *    it doesn’t match any recognized date or time pattern.
     */
    @Test
    void testRandomText() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("sdklfjskldjf", ZoneId.of("UTC"), true));
    }

    /**
     * 3) "Month" out of range. The parser expects 1..12.
     *    E.g. 13 for month => fail.
     */
    @Test
    void testMonthOutOfRange() {
        // ISO style
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-13-10", ZoneId.of("UTC"), true));

        // alpha style
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("Foo 10, 2024", ZoneId.of("UTC"), true));
    }

    /**
     * 4) "Day" out of range. E.g. 32 for day => fail.
     */
    @Test
    void testDayOutOfRange() {
        // ISO style
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-01-32", ZoneId.of("UTC"), true));
    }

    /**
     * 5) "Hour" out of range. E.g. 24 for hour => fail.
     */
    @Test
    void testHourOutOfRange() {
        // Basic time after date
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-01-10 24:30:00", ZoneId.of("UTC"), true));
    }

    /**
     * 6) "Minute" out of range. E.g. 60 => fail.
     */
    @Test
    void testMinuteOutOfRange() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-01-10 23:60:00", ZoneId.of("UTC"), true));
    }

    /**
     * 7) "Second" out of range. E.g. 60 => fail.
     */
    @Test
    void testSecondOutOfRange() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-01-10 23:59:60", ZoneId.of("UTC"), true));
    }

    /**
     * 8) Time with offset beyond valid range, e.g. +30:00
     *    (the parser should fail with ZoneOffset.of(...) if it’s outside +/-18)
     */
    @Test
    void testInvalidZoneOffset() {
        assertThrows(DateTimeException.class, () ->
                DateUtilities.parseDate("2024-01-10T10:30+30:00", ZoneId.systemDefault(), true));
    }

    /**
     * 9) A bracketed zone that is unparseable
     *    (like "[not/valid/???]" or "[some junk]").
     */
    @Test
    void testInvalidBracketZone() {
        // If your code tries to parse "[some junk]" and fails =>
        // you expect exception
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-01-10T10:30:00[some junk]", ZoneId.systemDefault(), true));
    }

    /**
     * 10) Time zone with no time => fail if we enforce that rule
     *     (like "2024-02-05Z" or "2024-02-05+09:00").
     */
    @Test
    void testZoneButNoTime() {
        // If your code is set to throw on zone-without-time:
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05Z", ZoneId.of("UTC"), true));
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05+09:00", ZoneId.of("UTC"), true));
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05[Asia/Tokyo]", ZoneId.of("UTC"), true));
    }

    /**
     * 11) Found a 'T' but no actual time after it => fail
     *     (like "2024-02-05T[Asia/Tokyo]").
     */
    @Test
    void testTButNoTime() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05T[Asia/Tokyo]", ZoneId.of("UTC"), true));
    }

    /**
     * 12) Ambiguous leftover text in strict mode => fail.
     *     e.g. "2024-02-05 10:30:00 some leftover" with ensureDateTimeAlone=true
     */
    @Test
    void testTrailingGarbageStrictMode() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05 10:30:00 some leftover", ZoneId.of("UTC"), true));
    }

    /**
     * 13) For strings that appear to be 'epoch millis' but actually overflow
     *     (like "999999999999999999999").
     *     This might cause a NumberFormatException or an invalid epoch parse
     *     if your code tries to parse them as a long.
     *     If you want to confirm that it fails...
     */
    @Test
    void testOverflowEpochMillis() {
        // Input validation now catches epoch overflow before NumberFormatException
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("999999999999999999999", ZoneId.of("UTC"), true));
    }

    /**
     * 15) A partial fraction "2024-02-05T10:30:45." => fail,
     *     if your code doesn't allow fraction with no digits after the dot.
     */
    @Test
    void testIncompleteFraction() {
        assertThrows(IllegalArgumentException.class, () ->
                DateUtilities.parseDate("2024-02-05T10:30:45.", ZoneId.of("UTC"), true));
    }
}
