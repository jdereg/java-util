package com.cedarsoftware.util.convert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * With modern.time.long.precision = nanos, every modern date-time type converts to and from long in nanoseconds.
 */
class ConverterLongPrecisionTest {

    private static final String PROPERTY = "cedarsoftware.converter." + InstantConversions.MODERN_TIME_LONG_PRECISION;
    private String saved;
    private Converter converter;

    @BeforeEach
    void nanos() {
        saved = System.getProperty(PROPERTY);
        System.clearProperty(PROPERTY);
        ConverterOptions options = new DefaultConverterOptions() {
            @Override
            public ZoneId getZoneId() {
                return ZoneId.of("America/New_York");
            }
        };
        options.getCustomOptions().put(InstantConversions.MODERN_TIME_LONG_PRECISION, Converter.PRECISION_NANOS);
        converter = new Converter(options);
    }

    @AfterEach
    void restore() {
        if (saved == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, saved);
        }
    }

    /**
     * ZonedDateTime, OffsetDateTime, LocalDateTime and LocalDate converted to long in milliseconds whatever the
     * setting, while long converted back to them in nanoseconds: ZonedDateTime -> long -> ZonedDateTime landed in 1969.
     */
    @Test
    void everyModernTypeConvertsToLongInNanoseconds() {
        Instant instant = Instant.parse("2024-01-15T10:20:30.123456789Z");
        long nanos = 1_705_314_030_123_456_789L;
        assertEquals(nanos, (long) converter.convert(instant, long.class));
        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/New_York"));
        assertEquals(nanos, (long) converter.convert(zdt, long.class));
        assertEquals(zdt, converter.convert(converter.convert(zdt, long.class), ZonedDateTime.class));
        OffsetDateTime odt = zdt.toOffsetDateTime();
        assertEquals(nanos, (long) converter.convert(odt, long.class));
        assertEquals(odt, converter.convert(converter.convert(odt, long.class), OffsetDateTime.class));
        LocalDateTime ldt = zdt.toLocalDateTime();
        assertEquals(ldt, converter.convert(converter.convert(ldt, long.class), LocalDateTime.class));
        LocalDate date = LocalDate.of(2024, 1, 15);
        assertEquals(date, converter.convert(converter.convert(date, long.class), LocalDate.class));
    }

    /** An instant past what a long holds in nanoseconds (2262-04-11) throws, instead of wrapping to 1715. */
    @Test
    void anInstantPastTheNanosecondRangeThrows() {
        Instant past = Instant.parse("2300-01-01T00:00:00Z");
        assertThrows(ArithmeticException.class, () -> converter.convert(past, long.class));
        assertThrows(ArithmeticException.class,
                () -> converter.convert(ZonedDateTime.parse("2300-01-01T00:00:00Z"), long.class));
    }
}
