package com.cedarsoftware.util.convert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Calendar converts in its own zone: to String, so String back to Calendar gives the same local time, and to every
 * local date and time type.
 */
class ConverterCalendarZoneTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * A Calendar in GMT printed in Europe/London, which keeps summer time: noon GMT on July 1 became
     * 13:00+01:00[Europe/London], and read back as a London Calendar its hour of day was 13. Reading a string, the
     * Converter takes "GMT" as GMT, not London.
     */
    @Test
    void aGmtCalendarKeepsGmt() {
        Calendar gmt = calendar("GMT", 2026, Calendar.JULY, 1, 12);
        String text = converter.convert(gmt, String.class);
        assertEquals("2026-07-01T12:00:00Z[GMT]", text);
        Calendar back = converter.convert(text, Calendar.class);
        assertEquals(gmt.getTimeInMillis(), back.getTimeInMillis());
        assertEquals(12, back.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, back.getTimeZone().getOffset(back.getTimeInMillis()));
    }

    /**
     * The zones the Converter reads as a region (CET as Europe/Berlin, EET as Europe/Kyiv, WET as Europe/Lisbon) are
     * printed as that region, so a string read back gives the same instant even where the two disagreed about the
     * offset (EET and Kyiv in 1960).
     */
    @Test
    void aZoneReadAsARegionRoundTripsTheInstant() {
        for (String id : new String[] {"CET", "EET", "WET", "GMT", "UTC", "America/New_York", "EST", "Asia/Kolkata"}) {
            for (int year : new int[] {1960, 2026}) {
                for (int month : new int[] {Calendar.JANUARY, Calendar.JULY}) {
                    Calendar cal = calendar(id, year, month, 1, 12);
                    String text = converter.convert(cal, String.class);
                    Calendar back = converter.convert(text, Calendar.class);
                    assertEquals(cal.getTimeInMillis(), back.getTimeInMillis(), id + " " + text);
                }
            }
        }
    }

    /**
     * 05:30 on July 1 in Tokyo is 16:30 on June 30 in New York. With the Converter's zone New York, a Tokyo Calendar
     * gave LocalTime 16:30 while its LocalDateTime was 2024-07-01T05:30: LocalTime alone went through ZonedDateTime,
     * which converts to the Converter's zone.
     */
    @Test
    void everyLocalTypeIsTheCalendarsOwnLocalTime() {
        Converter newYork = new Converter(new DefaultConverterOptions() {
            @Override
            public ZoneId getZoneId() {
                return ZoneId.of("America/New_York");
            }
        });
        Calendar tokyo = calendar("Asia/Tokyo", 2024, Calendar.JULY, 1, 5);
        tokyo.set(Calendar.MINUTE, 30);
        assertEquals(LocalTime.of(5, 30), newYork.convert(tokyo, LocalTime.class));
        assertEquals(LocalDateTime.of(2024, 7, 1, 5, 30), newYork.convert(tokyo, LocalDateTime.class));
        assertEquals(LocalDate.of(2024, 7, 1), newYork.convert(tokyo, LocalDate.class));
        assertEquals(java.sql.Date.valueOf("2024-07-01"), newYork.convert(tokyo, java.sql.Date.class));
        assertEquals(YearMonth.of(2024, 7), newYork.convert(tokyo, YearMonth.class));
        assertEquals(MonthDay.of(7, 1), newYork.convert(tokyo, MonthDay.class));
        assertEquals(OffsetDateTime.of(2024, 7, 1, 5, 30, 0, 0, ZoneOffset.ofHours(9)),
                newYork.convert(tokyo, OffsetDateTime.class));
    }

    private static Calendar calendar(String zone, int year, int month, int day, int hour) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.clear();
        cal.set(year, month, day, hour, 0, 0);
        return cal;
    }
}
