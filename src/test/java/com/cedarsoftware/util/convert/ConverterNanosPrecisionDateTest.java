package com.cedarsoftware.util.convert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * modern.time.long.precision = nanos makes ZonedDateTime, OffsetDateTime and Instant to long count nanoseconds; a
 * Date counts milliseconds whatever it is set to. ZonedDateTime and OffsetDateTime to Date took the nanoseconds as
 * milliseconds, and so did Calendar, LocalDateTime and LocalDate, which reach Date through ZonedDateTime: 2024 became
 * the year 54,499,609.
 */
class ConverterNanosPrecisionDateTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZonedDateTime MOMENT = ZonedDateTime.of(2024, 7, 1, 5, 30, 0, 123_456_789, TOKYO);

    private final Converter nanos = new Converter(new DefaultConverterOptions() {
        private final Map<String, Object> custom = new HashMap<>();

        {
            custom.put(InstantConversions.MODERN_TIME_LONG_PRECISION, Converter.PRECISION_NANOS);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCustomOption(String name) {
            return (T) custom.get(name);
        }

        @Override
        public ZoneId getZoneId() {
            return TOKYO;
        }
    });

    @Test
    void aDateIsMillisecondsWhateverThePrecision() {
        Date expected = new Date(MOMENT.toInstant().toEpochMilli());
        assertEquals(expected, nanos.convert(MOMENT, Date.class));
        assertEquals(expected, nanos.convert(MOMENT.toOffsetDateTime(), Date.class));
        assertEquals(expected, nanos.convert(MOMENT.toLocalDateTime(), Date.class));
        assertEquals(expected, nanos.convert(GregorianCalendar.from(MOMENT), Date.class));
        assertEquals(expected, nanos.convert(MOMENT.toInstant(), Date.class));
        assertEquals(new Date(MOMENT.toLocalDate().atStartOfDay(TOKYO).toInstant().toEpochMilli()),
                nanos.convert(MOMENT.toLocalDate(), Date.class));
        assertEquals(MOMENT.toInstant().toEpochMilli(), nanos.convert(MOMENT, Calendar.class).getTimeInMillis());
    }

    /** The setting still applies where it is meant to: to long. */
    @Test
    void longIsStillNanoseconds() {
        long expected = MOMENT.toEpochSecond() * 1_000_000_000L + MOMENT.getNano();
        assertEquals(expected, (long) nanos.convert(MOMENT, long.class));
        assertEquals(expected, (long) nanos.convert(MOMENT.toOffsetDateTime(), long.class));
        assertEquals(LocalDate.of(2024, 7, 1), nanos.convert(expected, LocalDate.class));
        assertEquals(LocalDateTime.of(2024, 7, 1, 5, 30, 0, 123_456_789), nanos.convert(expected, LocalDateTime.class));
    }
}
