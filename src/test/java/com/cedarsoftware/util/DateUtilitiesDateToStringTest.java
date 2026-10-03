package com.cedarsoftware.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Date.toString() -- "Thu Jul 04 22:30:00 AEST 2024" -- is the "Unix style" DateUtilities documents, but half of what
 * it prints did not parse: the zone was matched as 1-3 letters, so every 4-letter abbreviation (AEST, AEDT, ACDT,
 * NZST, CEST, AKST, SAST) failed, and an abbreviation outside DateUtilities' own table (BRT) was rejected. The zone
 * now takes up to 5 letters, and an abbreviation outside the table is found among the names the JDK gives its zones --
 * the names Date.toString() prints and SimpleDateFormat parses.
 */
class DateUtilitiesDateToStringTest {

    private static final String[] ZONES = {"America/New_York", "America/Los_Angeles", "America/Anchorage", "Europe/Berlin",
            "Europe/London", "Australia/Sydney", "Australia/Adelaide", "Pacific/Auckland", "Asia/Tokyo", "Asia/Kolkata",
            "America/Sao_Paulo", "Africa/Johannesburg", "UTC"};
    private static final long[] INSTANTS = {1705314600000L, 1720096200000L};       // 2024-01-15T10:30Z, 2024-07-04T12:30Z

    /** Date.toString()'s format, in a zone of our choosing, without touching the JVM's default zone. */
    private static String dateToString(long millis, String zone) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(zone));
        return format.format(new Date(millis));
    }

    @Test
    void whatDateToStringPrintsParsesBackToTheSameInstant() {
        List<String> failures = new ArrayList<>();
        for (String zone : ZONES) {
            for (long millis : INSTANTS) {
                String text = dateToString(millis, zone);
                try {
                    Instant back = DateUtilities.parseDate(text, ZoneId.of("UTC"), true).toInstant();
                    if (back.toEpochMilli() != millis) {
                        failures.add(text + " -> " + back);
                    }
                } catch (RuntimeException e) {
                    failures.add(text + " -> " + e);
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test
    void fourLetterAbbreviationsNameTheirRegions() {
        assertEquals(ZoneId.of("Australia/Sydney"),
                DateUtilities.parseDate("Mon Jan 15 21:30:00 AEDT 2024", ZoneId.of("UTC"), true).getZone());
        assertEquals(ZoneId.of("Europe/Berlin"),
                DateUtilities.parseDate("Thu Jul 04 14:30:00 CEST 2024", ZoneId.of("UTC"), true).getZone());
    }

    @Test
    void anAbbreviationOutsideTheTableIsFoundAmongTheJdksNames() {
        ZonedDateTime brazil = DateUtilities.parseDate("2024-01-15 07:30:00 BRT", ZoneId.of("UTC"), true);
        assertEquals(ZoneOffset.ofHours(-3), brazil.getOffset());
        assertTrue(brazil.getZone().getId().startsWith("America/"), brazil.getZone().getId());
        ZonedDateTime southAfrica = DateUtilities.parseDate("2024-01-15 12:30:00 SAST", ZoneId.of("UTC"), true);
        assertEquals(ZoneOffset.ofHours(2), southAfrica.getOffset());
    }

    @Test
    void theTablesChoicesStand() {
        // CST names zones on three continents; DateUtilities' table says Chicago, whatever the JVM's default zone
        assertEquals(ZoneId.of("America/Chicago"),
                DateUtilities.parseDate("Mon Jan 15 04:30:00 CST 2024", ZoneId.of("UTC"), true).getZone());
        assertEquals(ZoneId.of("Asia/Kolkata"),
                DateUtilities.parseDate("2024-01-15 16:00:00 IST", ZoneId.of("UTC"), true).getZone());
    }
}
