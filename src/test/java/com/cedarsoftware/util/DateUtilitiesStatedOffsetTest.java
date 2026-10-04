package com.cedarsoftware.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text that states an offset and then names its zone -- "2025-11-02T01:30:00-05:00[America/New_York]", the form
 * ZonedDateTime.toString() and the Converter write -- is read at the stated offset whenever the zone has that offset
 * at that local time. DateUtilities dropped the offset and read the time by the zone's rule alone, which answers the
 * earlier of the two times a fall-back repeats, and since the abbreviation rule it read "[CET]" as the abbreviation
 * CET (+01:00) in summer: an hour off either way, for a string the Converter had just written.
 */
class DateUtilitiesStatedOffsetTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    /** New York repeats 01:00-01:59 on 2025-11-02: first at -04:00, then at -05:00. */
    @Test
    void theOffsetSaysWhichOfTheRepeatedTimesIsMeant() {
        assertEquals(Instant.parse("2025-11-02T05:30:00Z"),
                DateUtilities.parseDate("2025-11-02T01:30:00-04:00[America/New_York]", UTC, true).toInstant());
        assertEquals(Instant.parse("2025-11-02T06:30:00Z"),
                DateUtilities.parseDate("2025-11-02T01:30:00-05:00[America/New_York]", UTC, true).toInstant());
        assertEquals(Instant.parse("2025-10-26T01:30:00Z"),
                DateUtilities.parseDate("2025-10-26T02:30:00+01:00 Europe/Berlin", UTC, true).toInstant());
    }

    /**
     * CET, EET, MET and WET are zone IDs that keep summer time, as well as abbreviations. Written by
     * ZonedDateTime.toString() in July they carry their summer offset, which says what is meant.
     */
    @Test
    void aZoneIdThatIsAlsoAnAbbreviationIsReadAtItsStatedOffset() {
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"),
                DateUtilities.parseDate("2026-07-01T12:00:00+02:00[CET]", UTC, true).toInstant());
        assertEquals(Instant.parse("2026-07-01T09:00:00Z"),
                DateUtilities.parseDate("2026-07-01T12:00:00+03:00[EET]", UTC, true).toInstant());
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"),
                DateUtilities.parseDate("2026-07-01T12:00:00+02:00[MET]", UTC, true).toInstant());
        assertEquals(Instant.parse("2026-07-01T11:00:00Z"),
                DateUtilities.parseDate("2026-07-01T12:00:00+01:00[WET]", UTC, true).toInstant());
    }

    /**
     * An offset the zone does not have at that time leaves the time as the zone reads it, as before: an abbreviation
     * without one still means its own offset, and an offset truncated to the minute still yields to the zone's local
     * mean time (-04:56:02 in 1850 New York).
     */
    @Test
    void anOffsetTheZoneDoesNotHaveThereChangesNothing() {
        assertEquals(Instant.parse("2024-07-04T17:00:00Z"),
                DateUtilities.parseDate("2024-07-04 12:00:00 EST", UTC, true).toInstant());
        assertEquals(Instant.parse("2024-07-04T17:00:00Z"),
                DateUtilities.parseDate("2024-07-04T12:00:00-05:00 EST", UTC, true).toInstant());
        ZonedDateTime lmt = ZonedDateTime.of(1850, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        assertEquals(lmt.toInstant(),
                DateUtilities.parseDate("1850-01-01T12:00:00-04:56[America/New_York]", UTC, true).toInstant());
    }

    /**
     * Every zone, around every transition of 2025 and 2026 and in mid-January and mid-July: a ZonedDateTime and a
     * Calendar converted to String and back keep their instant. 1,644 of 8,642 instants failed of each, before.
     */
    @Test
    void everyZoneRoundTripsThroughTheConvertersText() {
        Converter converter = new Converter(new DefaultConverterOptions());
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2027-01-01T00:00:00Z");
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (String id : new TreeSet<>(ZoneId.getAvailableZoneIds())) {
            ZoneId zone = ZoneId.of(id);
            ZoneRules rules = zone.getRules();
            List<Instant> instants = new ArrayList<>();
            instants.add(Instant.parse("2026-01-15T12:00:00Z"));
            instants.add(Instant.parse("2026-07-15T12:00:00Z"));
            for (ZoneOffsetTransition t = rules.nextTransition(from); t != null && t.getInstant().isBefore(to);
                 t = rules.nextTransition(t.getInstant())) {
                for (int minutes : new int[] {-90, -30, -1, 0, 1, 30, 59, 61, 90}) {
                    instants.add(t.getInstant().plusSeconds(minutes * 60L));
                }
            }
            for (Instant instant : instants) {
                checked++;
                String text = converter.convert(instant.atZone(zone), String.class);
                Instant back = converter.convert(text, ZonedDateTime.class).toInstant();
                if (!back.equals(instant)) {
                    wrong.add(text + " -> " + back);
                }
                Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(zone));
                calendar.setTimeInMillis(instant.toEpochMilli());
                text = converter.convert(calendar, String.class);
                back = converter.convert(text, Calendar.class).toInstant();
                if (!back.equals(instant)) {
                    wrong.add("Calendar " + text + " -> " + back);
                }
            }
        }
        assertTrue(checked > 1000, "checked " + checked);
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong, e.g. " + wrong.subList(0, Math.min(5, wrong.size())));
    }
}
