package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text that is a number converts to a date or time type as that number would: integer text ("1500") as a Long --
 * milliseconds, or nanoseconds under the precision setting -- and decimal text ("1.5") as a BigDecimal, seconds.
 * "1500" was 25 minutes as a Duration (seconds) but 1.5 s as an Instant (milliseconds); as a LocalTime it was the
 * time of day of the instant 1.5 s after 1970 in the Converter's zone (19:00:01.500 in New York); and decimal text
 * failed to convert to an Instant, a Date or a LocalTime.
 */
class ConverterNumberTextTest {

    private static final Class<?>[] DATE_TYPES = {Instant.class, Date.class, Timestamp.class, Calendar.class,
            ZonedDateTime.class, OffsetDateTime.class, LocalDateTime.class, LocalDate.class, java.sql.Date.class};
    private static final Class<?>[] TIME_TYPES = {Duration.class, LocalTime.class, OffsetTime.class,
            java.sql.Time.class};

    private static Converter converter(String zone) {
        DefaultConverterOptions options = new DefaultConverterOptions() {
            @Override
            public ZoneId getZoneId() {
                return ZoneId.of(zone);
            }
        };
        return new Converter(options);
    }

    private final Converter converter = converter("America/New_York");

    @Test
    void integerTextConvertsAsALong() {
        assertEquals(Duration.ofMillis(1500), converter.convert("1500", Duration.class));
        assertEquals(LocalTime.of(0, 0, 1, 500_000_000), converter.convert("1500", LocalTime.class));
        assertEquals(LocalTime.of(0, 0, 1, 500_000_000), converter("Asia/Tokyo").convert("1500", LocalTime.class));
        List<String> wrong = new ArrayList<>();
        for (String text : new String[] {"1500", "-1500", "1700000000000", "+42"}) {
            long number = Long.parseLong(text);
            for (Class<?> type : allTypes()) {
                same(number, text, type, wrong);
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    @Test
    void decimalTextConvertsAsABigDecimal() {
        assertEquals(Duration.ofMillis(1500), converter.convert("1.5", Duration.class));
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), converter.convert("45296.789", LocalTime.class));
        assertEquals(Instant.parse("1970-01-01T00:00:01.500Z"), converter.convert("1.5", Instant.class));
        List<String> wrong = new ArrayList<>();
        for (String text : new String[] {"1.5", "1500.0", "-2.25", "1700000000.123"}) {
            BigDecimal number = new BigDecimal(text);
            for (Class<?> type : allTypes()) {
                same(number, text, type, wrong);
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    /** Under the nanosecond precision setting, integer text follows the Long into nanoseconds. */
    @Test
    void integerTextFollowsThePrecisionSetting() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getCustomOptions().put("modern.time.long.precision", "nanos");
        options.getCustomOptions().put("duration.long.precision", "nanos");
        Converter nanos = new Converter(options);
        assertEquals(Instant.ofEpochSecond(0, 1500), nanos.convert("1500", Instant.class));
        assertEquals(Duration.ofNanos(1500), nanos.convert("1500", Duration.class));
    }

    /** Eight digits that make a date are still that date, for the date types; other text is parsed as before. */
    @Test
    void datesAndOtherTextReadAsBefore() {
        assertEquals(LocalDate.of(2024, 1, 15), converter.convert("20240115", LocalDate.class));
        assertEquals(LocalDate.of(2024, 1, 15), converter.convert("20240115", java.sql.Date.class).toLocalDate());
        assertEquals(Duration.ofMinutes(1), converter.convert("PT1M", Duration.class));
        assertEquals(LocalTime.of(12, 30), converter.convert("12:30", LocalTime.class));
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), converter.convert("2024-01-15T10:00:00Z", Instant.class));
        assertEquals(LocalDate.of(2024, 1, 15), converter.convert("2024-01-15", LocalDate.class));
    }

    /** In the Map form the value converts by its own type: BigDecimal seconds, Long and integer text milliseconds. */
    @Test
    void theMapFormConvertsItsValueByItsType() {
        assertEquals(Duration.ofSeconds(123), converter.convert(mapOf("duration", new BigDecimal("123")), Duration.class));
        assertEquals(Duration.ofSeconds(Long.MIN_VALUE),
                converter.convert(mapOf("duration", new BigDecimal(Long.toString(Long.MIN_VALUE))), Duration.class));
        assertEquals(Duration.ofMillis(123), converter.convert(mapOf("duration", 123L), Duration.class));
        assertEquals(Duration.ofMillis(123), converter.convert(mapOf("duration", "123"), Duration.class));
        assertEquals(Duration.ofMillis(1500), converter.convert(mapOf("duration", "1.5"), Duration.class));
    }

    private static java.util.Map<String, Object> mapOf(String key, Object value) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    /** Integer text that does not fit a long is refused, as the number would be. */
    @Test
    void integerTextBeyondALongIsRefused() {
        for (Class<?> type : new Class<?>[] {Duration.class, Instant.class, LocalTime.class}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert("99999999999999999999", type),
                    type.getSimpleName());
        }
    }

    private static Class<?>[] allTypes() {
        Class<?>[] all = new Class<?>[DATE_TYPES.length + TIME_TYPES.length];
        System.arraycopy(DATE_TYPES, 0, all, 0, DATE_TYPES.length);
        System.arraycopy(TIME_TYPES, 0, all, DATE_TYPES.length, TIME_TYPES.length);
        return all;
    }

    /** The text converts to what the number converts to, or both are refused. */
    private void same(Object number, String text, Class<?> type, List<String> wrong) {
        Object expected = outcome(number, type);
        Object actual = outcome(text, type);
        if (!expected.equals(actual)) {
            wrong.add("\"" + text + "\" -> " + type.getSimpleName() + ": " + actual + ", the number gives " + expected);
        }
    }

    private Object outcome(Object source, Class<?> type) {
        try {
            Object result = converter.convert(source, type);
            return result instanceof Calendar ? ((Calendar) result).getTimeInMillis() : result;
        } catch (IllegalArgumentException e) {
            return "refused";
        }
    }
}
