package com.cedarsoftware.util;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Date.toString()'s format -- "Mon Jan 15 10:30:00 EST 2024" -- now takes a fast path that reads it by position instead
 * of through four regular expressions. It must change nothing: for every string built from valid and invalid day
 * names, months, days, times, zones and years, in two default zones, strict and lenient, parseDate gives what the
 * flexible path gives -- the same ZonedDateTime, or the same exception with the same message.
 */
class DateUtilitiesDateToStringFastPathTest {

    private static final String[] WEEKDAYS = {"Mon", "sun", "FRI", "Xyz"};
    private static final String[] MONTHS = {"Jan", "feb", "JUL", "Sep", "Foo"};
    private static final String[] DAYS = {"01", "06", "29", "30", "31", "32", "00", " 6"};
    private static final String[] TIMES = {"10:30:00", "00:00:00", "23:59:59", "24:00:00", "10:60:00", "10:30:60",
            "02:30:00"};
    private static final String[] ZONES = {"EST", "EDT", "PST", "AEDT", "CEST", "BRT", "SAST", "UTC", "GMT", "Z", "XYZ",
            "est", "ABCDEF", "E5T"};
    private static final String[] YEARS = {"2024", "2023", "0000", "202a"};
    private static final ZoneId[] DEFAULT_ZONES = {ZoneId.of("UTC"), ZoneId.of("America/New_York")};

    private static String outcome(Supplier<Object> parse) {
        try {
            return String.valueOf(parse.get());
        } catch (RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    @Test
    void theFastPathAnswersAsTheFlexiblePathDoes() {
        List<String> differences = new ArrayList<>();
        int compared = 0;
        for (String weekday : WEEKDAYS) {
            for (String month : MONTHS) {
                for (String day : DAYS) {
                    for (String time : TIMES) {
                        for (String zone : ZONES) {
                            for (String year : YEARS) {
                                String text = weekday + " " + month + " " + day + " " + time + " " + zone + " " + year;
                                for (ZoneId defaultZone : DEFAULT_ZONES) {
                                    for (boolean strict : new boolean[]{true, false}) {
                                        String expected =
                                                outcome(() -> DateUtilities.parseFlexible(text, defaultZone, strict));
                                        String actual =
                                                outcome(() -> DateUtilities.parseDate(text, defaultZone, strict));
                                        compared++;
                                        if (!Objects.equals(expected, actual) && differences.size() < 20) {
                                            differences.add(text + " " + defaultZone + " strict=" + strict
                                                    + ": flexible " + expected + " / parseDate " + actual);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(compared > 100_000, "compared " + compared);
        assertTrue(differences.isEmpty(), differences.toString());
    }

    @Test
    void dateToStringParses() {
        assertEquals("2024-01-15T10:30-05:00[America/New_York]",
                DateUtilities.parseDate("Mon Jan 15 10:30:00 EST 2024", ZoneId.of("UTC"), true).toString());
        assertEquals("2024-07-04T13:00-04:00[America/New_York]",        // EST means -05:00, even in July
                DateUtilities.parseDate("Thu Jul 04 12:00:00 EST 2024", ZoneId.of("UTC"), true).toString());
    }
}
