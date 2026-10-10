package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A zone ID converts to TimeZone and ZoneId whatever its case: "america/new_york" is America/New_York, with its
 * daylight saving time. No two IDs the JDK knows differ only in case. An ID that names no zone is an
 * IllegalArgumentException for both. String to TimeZone gave GMT for both, as TimeZone.getTimeZone does, so a
 * misspelled zone moved every time computed with it by hours; String to ZoneId refused both.
 */
class ConverterZoneIdCaseTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final long JANUARY = new GregorianCalendar(2024, 0, 15).getTimeInMillis();
    private static final long JULY = new GregorianCalendar(2024, 6, 15).getTimeInMillis();

    @Test
    void anyCaseNamesTheZone() {
        for (String id : new String[] {"america/new_york", "AMERICA/NEW_YORK", "America/new_York"}) {
            TimeZone zone = converter.convert(id, TimeZone.class);
            assertEquals("America/New_York", zone.getID(), id);
            assertEquals(-5 * 3600_000, zone.getOffset(JANUARY), id);
            assertEquals(-4 * 3600_000, zone.getOffset(JULY), id);
            assertEquals(ZoneId.of("America/New_York"), converter.convert(id, ZoneId.class), id);
        }
        assertEquals("Europe/Berlin", converter.convert("europe/berlin", TimeZone.class).getID());
        assertEquals("Etc/GMT+5", converter.convert("etc/gmt+5", TimeZone.class).getID());
        assertEquals(ZoneId.of("Asia/Tokyo"), converter.convert("ASIA/TOKYO", ZoneId.class));
    }

    /** The short IDs, prefixes and offsets too: "utc", "gmt", "z", "est", "gmt+5", "utc+05:00". */
    @Test
    void shortIdsPrefixesAndOffsetsInAnyCase() {
        assertEquals("UTC", converter.convert("utc", TimeZone.class).getID());
        assertEquals("GMT", converter.convert("gmt", TimeZone.class).getID());
        assertEquals("UTC", converter.convert("z", TimeZone.class).getID());
        assertEquals("EST", converter.convert("est", TimeZone.class).getID());
        assertEquals("GMT+05:00", converter.convert("gmt+5", TimeZone.class).getID());
        assertEquals("GMT+05:00", converter.convert("utc+05:00", TimeZone.class).getID());

        assertEquals(ZoneId.of("UTC"), converter.convert("utc", ZoneId.class));
        assertEquals(ZoneId.of("Z"), converter.convert("z", ZoneId.class));
        assertEquals(ZoneId.of("GMT+5"), converter.convert("gmt+5", ZoneId.class));
        assertEquals(ZoneId.of("UTC+05:00"), converter.convert("utc+05:00", ZoneId.class));
    }

    /** What read before reads the same. */
    @Test
    void exactIdsAreUnchanged() {
        String[] ids = {"America/New_York", "UTC", "GMT", "Z", "EST", "PST", "GMT+5", "GMT+05:00", "+05:00",
                "UTC+5", "Etc/GMT+5", "CET", "UT", "GMT0"};
        String[] timeZones = {"America/New_York", "UTC", "GMT", "UTC", "EST", "PST", "GMT+05:00", "GMT+05:00",
                "GMT+05:00", "GMT+05:00", "Etc/GMT+5", "CET", "GMT", "GMT0"};
        // EST is the JDK's own: -05:00 until JDK 23, America/Panama from JDK 24 (tzdata 2024b made it a link)
        String est = TimeZone.getTimeZone("EST").toZoneId().toString();
        String[] zoneIds = {"America/New_York", "UTC", "GMT", "Z", est, "America/Los_Angeles", "GMT+05:00",
                "GMT+05:00", "+05:00", "UTC+05:00", "Etc/GMT+5", "CET", "UT", "GMT0"};
        for (int i = 0; i < ids.length; i++) {
            assertEquals(timeZones[i], converter.convert(ids[i], TimeZone.class).getID(), ids[i]);
            assertEquals(zoneIds[i], converter.convert(ids[i], ZoneId.class).toString(), ids[i]);
        }
    }

    /** An ID that names no zone is refused, by TimeZone as by ZoneId, and through the Map form. */
    @Test
    void anIdThatNamesNoZoneIsRefused() {
        List<String> wrong = new ArrayList<>();
        for (String id : new String[] {"Amercia/New_York", "America/NewYork", "bogus", "American/FunkyTown",
                "Asia/Kolkata", "gmt+99"}) {
            for (Class<?> target : new Class<?>[] {TimeZone.class, ZoneId.class}) {
                try {
                    wrong.add(id + " -> " + converter.convert(id, target));
                } catch (IllegalArgumentException e) {
                    if (!e.getMessage().contains("Unknown time-zone ID: '" + id + "'")) {
                        wrong.add(id + " -> " + target.getSimpleName() + ": " + e.getMessage());
                    }
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());

        Map<String, Object> map = Collections.singletonMap("zone", "Amercia/New_York");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(map, TimeZone.class));
        assertEquals("America/New_York",
                converter.convert(Collections.singletonMap("zone", "america/new_york"), TimeZone.class).getID());
    }
}
