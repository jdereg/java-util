package com.cedarsoftware.util.convert;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * java.sql.Time conversions (4.113.0). A Time is JDBC's time of day, written "HH:mm:ss" (as Time.toString()), with
 * ".SSS" when it has milliseconds.
 * <p>
 * Before, Time had no conversions of its own: converting anything to a Time returned a java.util.Date (the Date
 * conversion, found through Time's superclass), "13:14:15" and a LocalTime could not become one, and a Time could not
 * become a String, an Instant or a Map (Time.toInstant() throws UnsupportedOperationException).
 * <p>
 * The pairs are in ConverterEverythingTest too (loadSqlTimeTests), round-tripped through the test-scope json-io;
 * this class keeps the checks a pair cannot state: the result is a Time (not a Date) from every source,
 * and a copy is a new object.
 * <p>
 * 4.115.0: a Time is also a Date, and {@code new Time(millis)} takes any instant. 4.113.0 and 4.114.0 read text
 * that names a date ("2020-06-28T01:00:00") as its time of day on 1970-01-01, where every release before had kept
 * the instant, and wrote a Time on another day as "HH:mm:ss". Both lost the date without a word, and the two Times
 * print the same. Now the date is kept in both directions (the last three tests).
 */
class SqlTimeConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final Time T = Time.valueOf("13:14:15");
    private static final Time T123 = new Time(T.getTime() + 123);

    @Test
    void everySourceThatReachesADateReachesATimeAsATime() {
        // One value of each source the Converter turns into a Date; each must now become a Time, of the same instant
        Map<Object, String> sources = new HashMap<>();
        sources.put(T123.getTime(), "Long");
        sources.put(T123.getTime() / 1000.0, "Double");
        sources.put(java.math.BigInteger.valueOf(T123.getTime()), "BigInteger");
        sources.put(java.math.BigDecimal.valueOf(T123.getTime(), 3), "BigDecimal");
        sources.put(new java.util.concurrent.atomic.AtomicLong(T123.getTime()), "AtomicLong");
        sources.put(new Date(T123.getTime()), "Date");
        sources.put(new Timestamp(T123.getTime()), "Timestamp");
        sources.put(java.sql.Date.valueOf("2026-09-27"), "java.sql.Date");
        sources.put(Instant.ofEpochMilli(T123.getTime()), "Instant");
        sources.put(ZonedDateTime.of(2026, 9, 27, 13, 14, 15, 0, ZoneId.of("Asia/Tokyo")), "ZonedDateTime");
        sources.put(ZonedDateTime.of(2026, 9, 27, 13, 14, 15, 0, ZoneId.of("Asia/Tokyo")).toOffsetDateTime(),
                "OffsetDateTime");
        sources.put(java.time.LocalDateTime.of(2026, 9, 27, 13, 14, 15), "LocalDateTime");
        sources.put(java.time.LocalDate.of(2026, 9, 27), "LocalDate");
        sources.put(java.time.Duration.ofMillis(T123.getTime()), "Duration");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(T123.getTime());
        sources.put(calendar, "Calendar");

        for (Map.Entry<Object, String> source : sources.entrySet()) {
            Object time = converter.convert(source.getKey(), Time.class);
            assertSame(Time.class, time.getClass(), source.getValue() + " -> Time");
            Date date = converter.convert(source.getKey(), Date.class);
            assertEquals(date.getTime(), ((Time) time).getTime(), source.getValue() + ": the same instant as its Date");
        }
        assertNull(converter.convert(null, Time.class));
    }

    @Test
    void epochMillisRoundTripExactly() {
        assertEquals(T123, converter.convert(T123.getTime(), Time.class));
        assertEquals(T123.getTime(), (long) converter.convert(T123, Long.class));
        Time copy = converter.convert(T123, Time.class);
        assertEquals(T123, copy);
        assertTrue(copy != T123, "a copy, as for every mutable Date");
    }

    @Test
    void stringsAreTimesOfDay() {
        assertEquals("13:14:15", converter.convert(T, String.class));
        assertEquals("13:14:15.123", converter.convert(T123, String.class));
        assertEquals("13:14:15.005", converter.convert(new Time(T.getTime() + 5), String.class));
        assertEquals("13:14:15.050", converter.convert(new Time(T.getTime() + 50), String.class));

        assertEquals(T, converter.convert("13:14:15", Time.class));
        assertEquals(T123, converter.convert("13:14:15.123", Time.class));
        assertEquals(Time.valueOf("13:14:00"), converter.convert("13:14", Time.class));
        assertEquals(T, converter.convert("  13:14:15  ", Time.class));
        assertNull(converter.convert("", Time.class));

        // What a CharSequence, StringBuilder or StringBuffer holds, too
        assertEquals(T123, converter.convert(new StringBuilder("13:14:15.123"), Time.class));
        assertEquals("13:14:15.123", converter.convert(T123, StringBuilder.class).toString());
    }

    @Test
    void localTimesKeepTheirMilliseconds() {
        assertEquals(LocalTime.of(13, 14, 15, 123_000_000), converter.convert(T123, LocalTime.class));
        assertEquals(T123, converter.convert(LocalTime.of(13, 14, 15, 123_456_789), Time.class));
        assertEquals(T, converter.convert(LocalTime.of(13, 14, 15), Time.class));
    }

    @Test
    void mapsHoldTheTimeOfDayAndReadTheOldJsonIoForm() {
        Map<?, ?> map = converter.convert(T123, Map.class);
        assertEquals(1, map.size());
        assertEquals("13:14:15.123", map.get("sqlTime"));
        assertEquals(T123, converter.convert(map, Time.class));

        assertEquals(T, converter.convert(mapOf("value", "13:14:15"), Time.class));
        assertEquals(T123, converter.convert(mapOf("epochMillis", T123.getTime()), Time.class));
        // json-io wrote a Time as {"date": epochMillis} before Time had conversions
        assertEquals(T123, converter.convert(mapOf("date", T123.getTime()), Time.class));
    }

    @Test
    void aTimeReachesEveryTargetADateDoes() {
        // Time.toInstant() throws UnsupportedOperationException, and the Date conversions that used it failed
        Set<Class<?>> dateTargets = Converter.allSupportedConversions().get(Date.class);
        for (Class<?> target : dateTargets) {
            if (!target.isPrimitive()) {
                converter.convert(T123, target);   // must not throw
            }
        }
        assertEquals(Instant.ofEpochMilli(T123.getTime()), converter.convert(T123, Instant.class));
    }

    @Test
    void textThatNamesADateKeepsItsInstant() {
        // As reported: written as 2020-06-28T01:00:00, read back as 1970-01-01T01:00:00. Both print "01:00:00".
        long instant = LocalDateTime.parse("2020-06-28T01:00:00").atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli();
        Time read = converter.convert("2020-06-28T01:00:00", Time.class);
        assertSame(Time.class, read.getClass());
        assertEquals(instant, read.getTime());
        assertEquals("01:00:00", read.toString());
        assertEquals(instant, converter.convert(mapOf("value", "2020-06-28T01:00:00"), Time.class).getTime());

        // The oracle is the Date conversion: text a Date reads, a Time reads to the same instant, alone or in a Map
        String[] keys = {"sqlTime", "value", "_v", "epochMillis", "date"};
        Random random = new Random(20261013L);
        int checked = 0;
        for (int i = 0; i < 300; i++) {
            LocalDateTime dateTime = LocalDateTime.of(1900 + random.nextInt(200), 1 + random.nextInt(12),
                    1 + random.nextInt(28), random.nextInt(24), random.nextInt(60), random.nextInt(60),
                    random.nextInt(1000) * 1_000_000);
            String iso = dateTime.toString();
            String[] spellings = {iso, iso + "Z", iso + "+02:00", iso + "-09:30", iso + "[Asia/Tokyo]",
                    iso.replace('T', ' '), dateTime.toLocalDate().toString(),
                    dateTime.getMonthValue() + "/" + dateTime.getDayOfMonth() + "/" + dateTime.getYear()};
            for (String text : spellings) {
                long expected = converter.convert(text, Date.class).getTime();
                Time time = converter.convert(text, Time.class);
                assertSame(Time.class, time.getClass(), text);
                assertEquals(expected, time.getTime(), text);
                String key = keys[random.nextInt(keys.length)];
                assertEquals(expected, converter.convert(mapOf(key, text), Time.class).getTime(), key + "=" + text);
                checked++;
            }
        }
        assertEquals(2400, checked);
    }

    @Test
    void everyTimeComesBackFromItsTextAndItsMap() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate epochDay = LocalDate.of(1970, 1, 1);
        long startOfEpochDay = Time.valueOf("00:00:00").getTime();
        Random random = new Random(4115L);
        int timesOfDay = 0;
        int onAnotherDay = 0;
        for (int i = 0; i < 3000; i++) {
            long millis = i % 3 == 0
                    ? startOfEpochDay + random.nextInt(86_400_000)                              // a time of day
                    : (long) ((random.nextDouble() - 0.5) * 2 * 100 * 365.25 * 86_400_000L);    // 1870 to 2070
            Time time = new Time(millis);
            String text = converter.convert(time, String.class);
            Time fromText = converter.convert(text, Time.class);
            assertSame(Time.class, fromText.getClass());
            assertEquals(millis, fromText.getTime(), text);
            Map<?, ?> map = converter.convert(time, Map.class);
            assertEquals(text, map.get("sqlTime"));
            assertEquals(millis, converter.convert(map, Time.class).getTime(), text);

            // The form, with java.time as the oracle for the day: a time of day is written as Time.toString()
            // writes it (and its milliseconds); a Time on another day as the Date it is
            if (Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().equals(epochDay)) {
                String millisPart = millis % 1000 == 0 ? "" : String.format(".%03d", Math.floorMod(millis, 1000L));
                assertEquals(time + millisPart, text);
                timesOfDay++;
            } else {
                assertEquals(converter.convert(new Date(millis), String.class), text);
                onAnotherDay++;
            }
        }
        assertEquals(1000, timesOfDay);
        assertEquals(2000, onAnotherDay);
    }

    @Test
    void theDayOfATimeOfDayIsTheJvmZonesFirstOfJanuary1970() {
        TimeZone saved = TimeZone.getDefault();
        try {
            // Hobart had daylight time on that day: its offset then is not its standard one
            for (String id : new String[]{"UTC", "America/New_York", "Asia/Tokyo", "Asia/Kolkata", "Pacific/Pago_Pago",
                    "Pacific/Kiritimati", "Europe/London", "Australia/Hobart"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(id));
                Converter inZone = new Converter(new DefaultConverterOptions());
                long start = Time.valueOf("00:00:00").getTime();

                // The first and the last millisecond of the day are times of day
                assertEquals("00:00:00", inZone.convert(new Time(start), String.class), id);
                assertEquals("23:59:59.999", inZone.convert(new Time(start + 86_399_999L), String.class), id);
                // One millisecond before and after are not: the date is written, and comes back
                // new Time(0) is one of them wherever the zone is west of UTC
                for (long millis : new long[]{start - 1, start + 86_400_000L, 0L, start, start + 86_399_999L}) {
                    String text = inZone.convert(new Time(millis), String.class);
                    if (millis < start || millis >= start + 86_400_000L) {
                        assertEquals(inZone.convert(new Date(millis), String.class), text, id);
                    }
                    assertEquals(millis, inZone.convert(text, Time.class).getTime(), id + " " + text);
                }
            }
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
