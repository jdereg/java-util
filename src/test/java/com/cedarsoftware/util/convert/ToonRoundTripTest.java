package com.cedarsoftware.util.convert;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.cedarsoftware.util.CollectionUtilities;

import com.cedarsoftware.io.JsonIo;
import com.cedarsoftware.io.ReadOptionsBuilder;
import com.cedarsoftware.io.WriteOptionsBuilder;
import com.cedarsoftware.util.DeepEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that all Converter-supported types can round-trip through TOON format.
 * <p>
 * TOON writes CONVERTER_SUPPORTED types as bare strings (no @type metadata).
 * This test confirms that for every type the Converter supports, writing a value
 * to TOON and reading it back with .asClass(sameType) produces an equal result.
 * <p>
 * This is intentionally separate from ConverterEverythingTest, which tests
 * cross-type conversion (e.g., ZonedDateTime → Long). Cross-type conversion
 * through TOON would require type metadata that TOON doesn't emit by default.
 * For cross-type needs, users should first read back as the original type, then
 * use the Converter to convert to the desired target type.
 */
class ToonRoundTripTest {
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    // Representative test values for each Converter-supported type.
    // Each entry maps a source type to a non-null test instance of that type.
    private static final Map<Class<?>, Object> TEST_VALUES = new LinkedHashMap<>();
    static {
        // Temporal types
        TEST_VALUES.put(ZonedDateTime.class, ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, TOKYO));
        TEST_VALUES.put(OffsetDateTime.class, OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(9)));
        TEST_VALUES.put(LocalDateTime.class, LocalDateTime.of(2024, 6, 15, 10, 30, 0));
        TEST_VALUES.put(LocalDate.class, LocalDate.of(2024, 6, 15));
        TEST_VALUES.put(LocalTime.class, LocalTime.of(10, 30, 45));
        TEST_VALUES.put(OffsetTime.class, OffsetTime.of(10, 30, 45, 0, ZoneOffset.ofHours(9)));
        TEST_VALUES.put(Instant.class, Instant.parse("2024-06-15T01:30:00Z"));
        TEST_VALUES.put(Duration.class, Duration.ofHours(1).plusMinutes(30).plusSeconds(45));
        TEST_VALUES.put(Period.class, Period.of(2, 6, 15));
        TEST_VALUES.put(Year.class, Year.of(2024));
        TEST_VALUES.put(YearMonth.class, YearMonth.of(2024, 6));
        TEST_VALUES.put(MonthDay.class, MonthDay.of(6, 15));
        TEST_VALUES.put(DayOfWeek.class, DayOfWeek.SATURDAY);
        TEST_VALUES.put(Month.class, Month.JUNE);

        // Zone/timezone types
        TEST_VALUES.put(ZoneId.class, ZoneId.of("America/New_York"));
        TEST_VALUES.put(ZoneOffset.class, ZoneOffset.ofHours(-5));
        TEST_VALUES.put(TimeZone.class, TimeZone.getTimeZone("America/New_York"));

        // Legacy date types
        TEST_VALUES.put(Date.class, new Date(1718409000000L));
        TEST_VALUES.put(java.sql.Date.class, java.sql.Date.valueOf("2024-06-15"));
        TEST_VALUES.put(Timestamp.class, new Timestamp(1718409000000L));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(1718409000000L);
        TEST_VALUES.put(Calendar.class, cal);

        // Identifiers
        TEST_VALUES.put(UUID.class, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        // Network/path types
        TEST_VALUES.put(URI.class, URI.create("https://example.com/path?q=1"));
        try {
            TEST_VALUES.put(URL.class, URI.create("https://example.com/path").toURL());
        } catch (Exception ignored) { }
        TEST_VALUES.put(File.class, new File("/tmp/test.txt"));
        TEST_VALUES.put(Path.class, Paths.get("/tmp/test.txt"));

        // Numeric types (these go through NUMBER path in ToonWriter, but should still round-trip)
        TEST_VALUES.put(BigDecimal.class, new BigDecimal("123456.789012345"));
        TEST_VALUES.put(BigInteger.class, new BigInteger("9876543210123456789"));

        // Atomic types
        TEST_VALUES.put(AtomicInteger.class, new AtomicInteger(42));
        TEST_VALUES.put(AtomicLong.class, new AtomicLong(123456789L));
        TEST_VALUES.put(AtomicBoolean.class, new AtomicBoolean(true));

        // Text types
        TEST_VALUES.put(String.class, "Hello, TOON!");
        TEST_VALUES.put(StringBuilder.class, new StringBuilder("mutable text"));
        TEST_VALUES.put(StringBuffer.class, new StringBuffer("synchronized text"));

        // Other types
        TEST_VALUES.put(Locale.class, Locale.JAPAN);
        TEST_VALUES.put(Currency.class, Currency.getInstance("USD"));
        TEST_VALUES.put(Pattern.class, Pattern.compile("^[a-z]+\\d{3}$"));
        TEST_VALUES.put(Class.class, String.class);
        BitSet bs = new BitSet();
        bs.set(0);
        bs.set(3);
        bs.set(7);
        TEST_VALUES.put(BitSet.class, bs);
        TEST_VALUES.put(Boolean.class, Boolean.TRUE);
        TEST_VALUES.put(Character.class, 'A');
        TEST_VALUES.put(Byte.class, (byte) 42);
        TEST_VALUES.put(Short.class, (short) 1000);
        TEST_VALUES.put(Integer.class, 42);
        TEST_VALUES.put(Long.class, 123456789L);
        TEST_VALUES.put(Float.class, 3.14f);
        TEST_VALUES.put(Double.class, 2.718281828);
    }

    // Types that cannot round-trip through TOON for known structural reasons
    private static final Set<Class<?>> SKIP_TYPES = CollectionUtilities.setOf(
            Void.class,
            void.class
    );

    static Stream<Arguments> converterSupportedTypes() {
        Map<Class<?>, Set<Class<?>>> allConversions = Converter.allSupportedConversions();
        return allConversions.keySet().stream()
                .filter(type -> !SKIP_TYPES.contains(type))
                .filter(type -> !type.isArray())
                .filter(type -> !java.util.Collection.class.isAssignableFrom(type))
                .filter(type -> !java.util.Map.class.isAssignableFrom(type))
                .filter(type -> !java.util.stream.BaseStream.class.isAssignableFrom(type))
                .filter(type -> !java.nio.Buffer.class.isAssignableFrom(type))
                .filter(TEST_VALUES::containsKey)
                .map(type -> Arguments.of(Converter.getShortName(type), type, TEST_VALUES.get(type)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterSupportedTypes")
    void testSameTypeRoundTrip(String typeName, Class<?> type, Object original) {
        String toon = JsonIo.toToon(original, new WriteOptionsBuilder().build());
        assertNotNull(toon, "TOON output should not be null for " + typeName);

        Object restored = JsonIo.fromToon(toon, new ReadOptionsBuilder().build()).asClass(type);
        assertNotNull(restored, "Restored value should not be null for " + typeName);

        assertValuesEqual(original, restored, typeName);
    }

    @Test
    void testPojoWithConverterSupportedFields() {
        ConverterFieldsPojo original = new ConverterFieldsPojo();
        original.zonedDateTime = ZonedDateTime.of(2024, 3, 15, 14, 30, 0, 0, TOKYO);
        original.uuid = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        original.duration = Duration.ofHours(2).plusMinutes(30);
        original.uri = URI.create("https://example.com/api");
        original.locale = Locale.FRANCE;
        original.localDate = LocalDate.of(2024, 3, 15);
        original.instant = Instant.parse("2024-03-15T05:30:00Z");
        original.currency = Currency.getInstance("EUR");
        original.bigDecimal = new BigDecimal("99999.12345");
        original.period = Period.of(1, 6, 0);
        original.year = Year.of(2024);
        original.yearMonth = YearMonth.of(2024, 3);
        original.monthDay = MonthDay.of(3, 15);
        original.zoneId = ZoneId.of("Europe/Paris");
        original.timeZone = TimeZone.getTimeZone("Europe/Paris");

        String toon = JsonIo.toToon(original, new WriteOptionsBuilder().build());
        ConverterFieldsPojo restored = JsonIo.fromToon(toon, new ReadOptionsBuilder().build())
                .asClass(ConverterFieldsPojo.class);

        assertNotNull(restored);
        assertEquals(original.zonedDateTime, restored.zonedDateTime, "zonedDateTime");
        assertEquals(original.uuid, restored.uuid, "uuid");
        assertEquals(original.duration, restored.duration, "duration");
        assertEquals(original.uri, restored.uri, "uri");
        assertEquals(original.locale, restored.locale, "locale");
        assertEquals(original.localDate, restored.localDate, "localDate");
        assertEquals(original.instant, restored.instant, "instant");
        assertEquals(original.currency, restored.currency, "currency");
        assertEquals(0, original.bigDecimal.compareTo(restored.bigDecimal), "bigDecimal");
        assertEquals(original.period, restored.period, "period");
        assertEquals(original.year, restored.year, "year");
        assertEquals(original.yearMonth, restored.yearMonth, "yearMonth");
        assertEquals(original.monthDay, restored.monthDay, "monthDay");
        assertEquals(original.zoneId, restored.zoneId, "zoneId");
        assertEquals(original.timeZone, restored.timeZone, "timeZone");
    }

    private void assertValuesEqual(Object original, Object restored, String typeName) {
        // Pattern doesn't implement equals — compare pattern strings
        if (original instanceof Pattern) {
            assertEquals(((Pattern) original).pattern(), ((Pattern) restored).pattern(), typeName);
            return;
        }
        // Atomic types don't implement equals — compare via get()
        if (original instanceof AtomicBoolean) {
            assertEquals(((AtomicBoolean) original).get(), ((AtomicBoolean) restored).get(), typeName);
            return;
        }
        if (original instanceof AtomicInteger) {
            assertEquals(((AtomicInteger) original).get(), ((AtomicInteger) restored).get(), typeName);
            return;
        }
        if (original instanceof AtomicLong) {
            assertEquals(((AtomicLong) original).get(), ((AtomicLong) restored).get(), typeName);
            return;
        }
        // Calendar — compare millis and timezone
        if (original instanceof Calendar) {
            Calendar origCal = (Calendar) original;
            Calendar resCal = (Calendar) restored;
            assertEquals(origCal.getTimeInMillis(), resCal.getTimeInMillis(), typeName + " millis");
            assertEquals(origCal.getTimeZone().getID(), resCal.getTimeZone().getID(), typeName + " timezone");
            return;
        }
        // CharSequence types — compare string value
        if (original instanceof CharSequence && !(original instanceof String)) {
            assertEquals(original.toString(), restored.toString(), typeName);
            return;
        }
        // Float — allow small epsilon
        if (original instanceof Float) {
            assertEquals((Float) original, (Float) restored, 0.001f, typeName);
            return;
        }
        // BigDecimal — use compareTo (ignores scale)
        if (original instanceof BigDecimal) {
            assertEquals(0, ((BigDecimal) original).compareTo((BigDecimal) restored), typeName);
            return;
        }
        // Default — DeepEquals
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(original, restored, options),
                typeName + " round-trip failed: " + options.get("diff"));
    }

    static class ConverterFieldsPojo {
        ZonedDateTime zonedDateTime;
        UUID uuid;
        Duration duration;
        URI uri;
        Locale locale;
        LocalDate localDate;
        Instant instant;
        Currency currency;
        BigDecimal bigDecimal;
        Period period;
        Year year;
        YearMonth yearMonth;
        MonthDay monthDay;
        ZoneId zoneId;
        TimeZone timeZone;
    }
}
