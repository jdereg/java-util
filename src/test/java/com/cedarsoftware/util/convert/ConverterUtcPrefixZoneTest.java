package com.cedarsoftware.util.convert;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A zone written with the prefix "UTC" or "UT" before an offset, "UTC+05:00", is that offset as a TimeZone on every
 * JDK. JDK 8's TimeZone.getTimeZone(ZoneId) does not read those prefixes and gave GMT, so on JDK 8 a ZoneId converted
 * to GMT, and with such a zone in the Converter's options every Calendar it made was in GMT. JDK 9+ reads them.
 */
class ConverterUtcPrefixZoneTest {

    private static final int FIVE_HOURS = 5 * 3600_000;
    private static final ZoneId UTC_PLUS_5 = ZoneId.of("UTC+05:00");

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private final Converter utcPlus5 = new Converter(new DefaultConverterOptions() {
        @Override
        public ZoneId getZoneId() {
            return UTC_PLUS_5;
        }
    });

    @Test
    void zoneIdToTimeZone() {
        assertEquals("GMT+05:00", converter.convert(UTC_PLUS_5, TimeZone.class).getID());
        assertEquals("GMT-03:30", converter.convert(ZoneId.of("UT-03:30"), TimeZone.class).getID());
        assertEquals("GMT", converter.convert(ZoneId.of("UT"), TimeZone.class).getID());
        assertEquals("UTC", converter.convert(ZoneId.of("UTC"), TimeZone.class).getID());
        assertEquals("America/New_York", converter.convert(ZoneId.of("America/New_York"), TimeZone.class).getID());
    }

    @Test
    void theConvertersZoneIsThatOffset() {
        assertEquals(FIVE_HOURS, utcPlus5.getOptions().getTimeZone().getRawOffset());
        assertEquals(FIVE_HOURS, utcPlus5.convert(0L, Calendar.class).getTimeZone().getRawOffset());
        Calendar fromSqlDate = utcPlus5.convert(Date.valueOf(LocalDate.of(2024, 7, 1)), Calendar.class);
        assertEquals(FIVE_HOURS, fromSqlDate.getTimeZone().getRawOffset());
        assertEquals(0, fromSqlDate.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    void textInThatZoneMakesACalendarInIt() {
        Calendar calendar = converter.convert("2024-07-01T12:00 UTC+5", Calendar.class);
        assertEquals(FIVE_HOURS, calendar.getTimeZone().getRawOffset());
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY));
    }
}
