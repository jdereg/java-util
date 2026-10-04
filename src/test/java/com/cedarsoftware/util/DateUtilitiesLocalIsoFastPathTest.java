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
 * Zone-less ISO text -- "2024-01-15", "2024-01-15T10:30", "2024-01-15 10:30:00.123" -- now takes a fast path that reads
 * it by position instead of through three regular expressions. It must change nothing: for every string built from
 * valid and invalid years, months, days, separators, times and fractions, in three zones (one across a DST change),
 * strict and lenient, parseDate gives what the flexible path gives -- the same ZonedDateTime, or the same exception
 * with the same message.
 */
class DateUtilitiesLocalIsoFastPathTest {

    private static final String[] YEARS = {"0000", "0001", "1969", "2000", "2024", "9999"};
    private static final String[] MONTHS = {"00", "01", "02", "03", "11", "12", "13", "1a"};
    private static final String[] DAYS = {"00", "01", "10", "28", "29", "30", "31", "32", "0x"};
    private static final String[] SEPARATORS = {"T", " ", "t", "_"};
    private static final String[] TIMES = {"10:30", "00:00", "02:30", "01:30", "23:59", "24:00", "10:60", "1a:30",
            "10:3", "10:30:00", "10:30:59", "10:30:60", "10:30:0", "10:30:", "10:30:00.", "10:30:00.1", "10:30:00.05",
            "10:30:00.123456789", "10:30:00.1234567891", "10:30:00.12a", "10:30:00Z", "10:30:00 EST", "10:30+05:30"};
    private static final ZoneId[] ZONES = {ZoneId.of("UTC"), ZoneId.of("America/New_York"), ZoneId.of("Asia/Kolkata")};

    private static String outcome(Supplier<Object> parse) {
        try {
            return String.valueOf(parse.get());
        } catch (RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    @Test
    void theFastPathAnswersAsTheFlexiblePathDoes() {
        List<String> texts = new ArrayList<>();
        for (String year : YEARS) {
            for (String month : MONTHS) {
                for (String day : DAYS) {
                    String date = year + "-" + month + "-" + day;
                    texts.add(date);
                    for (String separator : SEPARATORS) {
                        for (String time : TIMES) {
                            texts.add(date + separator + time);
                        }
                    }
                }
            }
        }
        List<String> differences = new ArrayList<>();
        int compared = 0;
        for (String text : texts) {
            for (ZoneId zone : ZONES) {
                for (boolean strict : new boolean[]{true, false}) {
                    String expected = outcome(() -> DateUtilities.parseFlexible(text, zone, strict));
                    String actual = outcome(() -> DateUtilities.parseDate(text, zone, strict));
                    compared++;
                    if (!Objects.equals(expected, actual) && differences.size() < 20) {
                        differences.add(text + " " + zone + " strict=" + strict + ": flexible " + expected
                                + " / parseDate " + actual);
                    }
                }
            }
        }
        assertTrue(compared > 50_000, "compared " + compared);
        assertTrue(differences.isEmpty(), differences.toString());
    }

    @Test
    void theCommonShapesParse() {
        ZoneId newYork = ZoneId.of("America/New_York");
        assertEquals("2024-01-15T00:00-05:00[America/New_York]",
                DateUtilities.parseDate("2024-01-15", newYork, true).toString());
        assertEquals("2024-07-04T10:30:15.123-04:00[America/New_York]",
                DateUtilities.parseDate("2024-07-04 10:30:15.123", newYork, true).toString());
        assertEquals("2024-03-10T03:30-04:00[America/New_York]",        // inside the spring-forward gap, moved on
                DateUtilities.parseDate("2024-03-10T02:30", newYork, true).toString());
    }
}
