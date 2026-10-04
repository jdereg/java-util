package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A source that carries a zone keeps its own wall clock. The Converter's zone applies only where the source has none
 * (Date, Instant, Long, Timestamp, numbers), and when a local value is turned into an instant (LocalDate to Instant).
 * ZonedDateTime and OffsetDateTime were first moved to the Converter's zone, so a Tokyo ZonedDateTime at 05:30 on
 * July 1 gave LocalDate June 30 with the Converter in Los Angeles, while the same moment as a Tokyo Calendar, or as
 * text, gave July 1.
 */
class ConverterWallClockTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    private static final ZonedDateTime MOMENT = ZonedDateTime.of(2024, 7, 1, 5, 30, 0, 0, TOKYO);

    private final Converter converter = new Converter(new DefaultConverterOptions() {
        @Override
        public ZoneId getZoneId() {
            return LOS_ANGELES;
        }
    });

    private static Calendar tokyoCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TOKYO));
        calendar.setTimeInMillis(MOMENT.toInstant().toEpochMilli());
        return calendar;
    }

    /** Each local type, as the wall clock in {@code zone} shows the moment. */
    private void assertWallClock(Object source, ZoneId zone, List<String> wrong) {
        ZonedDateTime expected = MOMENT.withZoneSameInstant(zone);
        check(source, LocalDateTime.class, expected.toLocalDateTime(), wrong);
        check(source, LocalDate.class, expected.toLocalDate(), wrong);
        check(source, LocalTime.class, expected.toLocalTime(), wrong);
        check(source, java.sql.Date.class, java.sql.Date.valueOf(expected.toLocalDate()), wrong);
        check(source, Year.class, Year.from(expected), wrong);
        check(source, YearMonth.class, YearMonth.from(expected), wrong);
        check(source, MonthDay.class, MonthDay.from(expected), wrong);
    }

    private void check(Object source, Class<?> target, Object expected, List<String> wrong) {
        Object actual = converter.convert(source, target);
        if (!expected.equals(actual)) {
            wrong.add(source.getClass().getSimpleName() + " -> " + target.getSimpleName() + ": " + actual
                    + ", expected " + expected);
        }
    }

    @Test
    void aSourceWithAZoneKeepsItsOwnWallClock() {
        List<String> wrong = new ArrayList<>();
        assertWallClock(MOMENT, TOKYO, wrong);
        assertWallClock(MOMENT.toOffsetDateTime(), TOKYO, wrong);
        assertWallClock(tokyoCalendar(), TOKYO, wrong);
        assertWallClock("2024-07-01T05:30:00+09:00[Asia/Tokyo]", TOKYO, wrong);
        assertWallClock("2024-07-01T05:30:00+09:00", TOKYO, wrong);
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong: " + wrong);
    }

    /** Between zoned types too: the target is in the source's zone, at the same instant. */
    @Test
    void aZonedTargetKeepsTheSourcesZone() {
        Calendar fromZoned = converter.convert(MOMENT, Calendar.class);
        assertEquals("Asia/Tokyo", fromZoned.getTimeZone().getID());
        assertEquals(5, fromZoned.get(Calendar.HOUR_OF_DAY));
        assertEquals(MOMENT.toInstant().toEpochMilli(), fromZoned.getTimeInMillis());

        OffsetDateTime offset = MOMENT.toOffsetDateTime();
        assertEquals(offset.toZonedDateTime(), converter.convert(offset, ZonedDateTime.class));
        Calendar fromOffset = converter.convert(offset, Calendar.class);
        assertEquals(9 * 3600_000, fromOffset.getTimeZone().getRawOffset());
        assertEquals(5, fromOffset.get(Calendar.HOUR_OF_DAY));
        assertEquals(offset.toInstant().toEpochMilli(), fromOffset.getTimeInMillis());

        assertEquals(offset, converter.convert(MOMENT, OffsetDateTime.class));
        assertEquals(MOMENT, converter.convert(tokyoCalendar(), ZonedDateTime.class));
    }

    /** A source without a zone is read in the Converter's zone, as before. */
    @Test
    void aSourceWithoutAZoneUsesTheConvertersZone() {
        List<String> wrong = new ArrayList<>();
        Instant instant = MOMENT.toInstant();
        assertWallClock(instant, LOS_ANGELES, wrong);
        assertWallClock(Date.from(instant), LOS_ANGELES, wrong);
        assertWallClock(Timestamp.from(instant), LOS_ANGELES, wrong);
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong: " + wrong);
        assertEquals(MOMENT.withZoneSameInstant(LOS_ANGELES).toLocalDate(),
                converter.convert(instant.toEpochMilli(), LocalDate.class));
    }

    /** A local value becomes an instant in the Converter's zone, as before. */
    @Test
    void aLocalValueBecomesAnInstantInTheConvertersZone() {
        assertEquals(LocalDate.of(2024, 7, 1).atStartOfDay(LOS_ANGELES).toInstant(),
                converter.convert(LocalDate.of(2024, 7, 1), Instant.class));
        assertEquals(LocalDateTime.of(2024, 7, 1, 5, 30).atZone(LOS_ANGELES).toInstant(),
                converter.convert(LocalDateTime.of(2024, 7, 1, 5, 30), Instant.class));
        assertEquals(ZonedDateTime.of(2024, 7, 1, 5, 30, 0, 0, LOS_ANGELES),
                converter.convert(LocalDateTime.of(2024, 7, 1, 5, 30), ZonedDateTime.class));
        assertEquals(ZoneOffset.ofHours(-7),
                converter.convert(LocalDateTime.of(2024, 7, 1, 5, 30), OffsetDateTime.class).getOffset());
    }
}
