package com.cedarsoftware.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A zone abbreviation says which offset it means: EST is UTC-05:00 and EDT UTC-04:00, whatever the date. DateUtilities
 * read the time in the region the abbreviation names (America/New_York) by that region's rule for the date, so
 * "12:00 EST" in July came out as 12:00-04:00 and "12:00 EDT" in January as 12:00-05:00, an hour off -- and likewise
 * PST/PDT, CET/CEST, BST, AKST, AEDT... SimpleDateFormat honors the abbreviation. Now the time is read at the offset
 * the abbreviation means and given in its region: the same instant SimpleDateFormat parses. A date where the
 * abbreviation and the region agree comes out exactly as before.
 */
class DateUtilitiesAbbreviationOffsetTest {

    private static final Object[][] MEANINGS = {
            {"EST", "-05:00"}, {"EDT", "-04:00"}, {"CST", "-06:00"}, {"CDT", "-05:00"},
            {"MST", "-07:00"}, {"MDT", "-06:00"},
            {"PST", "-08:00"}, {"PDT", "-07:00"}, {"AKST", "-09:00"}, {"AKDT", "-08:00"}, {"HST", "-10:00"},
            {"GMT", "Z"}, {"BST", "+01:00"}, {"WET", "Z"}, {"WEST", "+01:00"}, {"CET", "+01:00"}, {"CEST", "+02:00"},
            {"EET", "+02:00"}, {"EEST", "+03:00"}, {"AEST", "+10:00"}, {"AEDT", "+11:00"}, {"ACST", "+09:30"},
            {"ACDT", "+10:30"}, {"NZST", "+12:00"}, {"NZDT", "+13:00"}, {"JST", "+09:00"}, {"IST", "+05:30"},
            {"MSK", "+03:00"}};

    @Test
    void anAbbreviationMeansItsOwnOffsetInEverySeason() {
        List<String> wrong = new ArrayList<>();
        for (Object[] meaning : MEANINGS) {
            for (String date : new String[]{"2024-01-15", "2024-07-04"}) {
                String text = date + " 12:00:00 " + meaning[0];
                ZonedDateTime parsed = DateUtilities.parseDate(text, ZoneId.of("UTC"), true);
                LocalDateTime local = LocalDateTime.parse(date + "T12:00:00");
                if (!parsed.toInstant().equals(local.toInstant(ZoneOffset.of((String) meaning[1])))) {
                    wrong.add(text + " -> " + parsed);
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    @Test
    void theRegionIsKept() {
        ZonedDateTime estInJuly = DateUtilities.parseDate("2024-07-04 12:00:00 EST", ZoneId.of("UTC"), true);
        assertEquals(ZoneId.of("America/New_York"), estInJuly.getZone());
        assertEquals(ZonedDateTime.parse("2024-07-04T13:00-04:00[America/New_York]"), estInJuly);   // 17:00Z
        ZonedDateTime bstInJanuary = DateUtilities.parseDate("Mon Jan 15 12:00:00 BST 2024", ZoneId.of("UTC"), true);
        assertEquals(ZonedDateTime.parse("2024-01-15T11:00Z[Europe/London]"), bstInJanuary);
    }

    @Test
    void whereTheAbbreviationAndTheRegionAgreeNothingChanges() {
        assertEquals(ZonedDateTime.parse("2024-01-15T12:00-05:00[America/New_York]"),
                DateUtilities.parseDate("2024-01-15 12:00:00 EST", ZoneId.of("UTC"), true));
        assertEquals(ZonedDateTime.parse("2024-07-04T12:00-04:00[America/New_York]"),
                DateUtilities.parseDate("Thu Jul 04 12:00:00 EDT 2024", ZoneId.of("UTC"), true));
        // a region named outright follows its own rule, as always
        assertEquals(ZonedDateTime.parse("2024-07-04T12:00-04:00[America/New_York]"),
                DateUtilities.parseDate("2024-07-04 12:00:00 America/New_York", ZoneId.of("UTC"), true));
        assertEquals(ZonedDateTime.parse("2024-07-04T12:00+05:30[Asia/Kolkata]"),
                DateUtilities.parseDate("2024-07-04 12:00:00 IST", ZoneId.of("UTC"), true));
    }
}
