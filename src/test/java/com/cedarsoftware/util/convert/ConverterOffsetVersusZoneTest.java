package com.cedarsoftware.util.convert;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text whose offset the zone does not have at that time ("2024-07-01T12:00+05:00[Europe/Berlin]") is read at the
 * offset, as java.time reads it: the instant the text states, shown in the zone. The zone won, so text written
 * before a zone's rules changed (Brazil dropped daylight saving time in 2019) read back an hour off. The exception
 * is the zone's own offset cut to whole minutes, as "XXX" patterns write the local mean time of old dates ("+00:53"
 * for Berlin's +00:53:28 until 1893): the zone's exact offset is meant.
 */
class ConverterOffsetVersusZoneTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Instant read(String text) {
        return converter.convert(text, ZonedDateTime.class).toInstant();
    }

    /**
     * Wherever the offset is not the zone's own cut to minutes, the instant is the one the offset states, as java.time
     * reads it on JDK 9 and later. JDK 8's ZonedDateTime.parse let the zone win (even in the repeated hour); the
     * Converter reads these the same on every JDK.
     */
    @Test
    void theOffsetWinsAsJavaTimeReadsIt() {
        String[][] cases = {
                {"2024-07-01T12:00:00+05:00[Europe/Berlin]", "2024-07-01T07:00:00Z"},     // never Berlin's offset
                {"2024-07-01T12:00:00+01:00[Europe/Berlin]", "2024-07-01T11:00:00Z"},     // winter offset, in July
                {"2019-11-15T12:00:00-02:00[America/Sao_Paulo]", "2019-11-15T14:00:00Z"}, // daylight time, abolished
                {"2024-03-31T02:30:00+01:00[Europe/Berlin]", "2024-03-31T01:30:00Z"},     // the hour clocks skip
                {"2024-03-31T02:30:00+02:00[Europe/Berlin]", "2024-03-31T00:30:00Z"},
                {"2024-10-27T02:30:00+02:00[Europe/Berlin]", "2024-10-27T00:30:00Z"},     // the repeated hour
                {"2024-10-27T02:30:00+01:00[Europe/Berlin]", "2024-10-27T01:30:00Z"},
                {"2024-07-01T12:00:00+02:00[Europe/Berlin]", "2024-07-01T10:00:00Z"},     // consistent
                {"2024-07-01T12:00:00+05:00[UTC]", "2024-07-01T07:00:00Z"},
                {"1880-01-01T12:00:00+00:53:28[Europe/Berlin]", "1880-01-01T11:06:32Z"},  // local mean time in full
        };
        boolean java9 = !System.getProperty("java.version").startsWith("1.");
        List<String> wrong = new ArrayList<>();
        for (String[] each : cases) {
            Instant expected = Instant.parse(each[1]);
            Instant actual = read(each[0]);
            if (!expected.equals(actual)) {
                wrong.add(each[0] + " -> " + actual + ", expected " + expected);
            }
            if (java9 && !expected.equals(ZonedDateTime.parse(each[0]).toInstant())) {
                wrong.add(each[0] + ": java.time reads " + ZonedDateTime.parse(each[0]).toInstant());
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());

        ZonedDateTime berlin = converter.convert("2024-07-01T12:00+05:00[Europe/Berlin]", ZonedDateTime.class);
        assertEquals(ZonedDateTime.parse("2024-07-01T09:00+02:00[Europe/Berlin]"), berlin);
    }

    /** The zone's own offset without its seconds means the zone's exact offset. */
    @Test
    void theZonesOffsetCutToMinutesIsTheZones() {
        assertEquals(Instant.parse("1880-01-01T11:06:32Z"), read("1880-01-01T12:00:00+00:53[Europe/Berlin]"));
        assertEquals(Instant.parse("1960-01-01T12:44:30Z"), read("1960-01-01T12:00:00-00:44[Africa/Monrovia]"));
        assertEquals(Instant.parse("1880-01-01T16:56:02Z"), read("1880-01-01T12:00:00-04:56[America/New_York]"));
    }

    /**
     * Calendar to String writes the offset's seconds when it has any, so the text is exact: "+00:53:28" for Berlin in
     * 1880, where it wrote "+00:53". Every date since 1972, when Liberia gave up -00:44:30, is written as before.
     */
    @Test
    void aCalendarIsWrittenWithTheSecondsOfItsOffset() {
        ZonedDateTime old = ZonedDateTime.of(1880, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTimeInMillis(old.toInstant().toEpochMilli());
        String text = converter.convert(calendar, String.class);
        assertEquals("1880-01-01T12:00:00+00:53:28[Europe/Berlin]", text);
        assertEquals(calendar.getTimeInMillis(), converter.convert(text, Calendar.class).getTimeInMillis());

        Calendar summer = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        summer.setTimeInMillis(Instant.parse("2024-07-01T10:00:00Z").toEpochMilli());
        assertEquals("2024-07-01T12:00:00+02:00[Europe/Berlin]", converter.convert(summer, String.class));
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(Instant.parse("2024-07-01T10:00:00Z").toEpochMilli());
        assertEquals("2024-07-01T10:00:00Z[UTC]", converter.convert(utc, String.class));
    }
}
