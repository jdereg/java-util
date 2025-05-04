package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.cedarsoftware.io.JsonIo;
import com.cedarsoftware.io.JsonIoException;
import com.cedarsoftware.io.ReadOptions;
import com.cedarsoftware.io.ReadOptionsBuilder;
import com.cedarsoftware.io.WriteOptions;
import com.cedarsoftware.io.WriteOptionsBuilder;
import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.SystemUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static com.cedarsoftware.util.convert.MapConversions.CALENDAR;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.DURATION;
import static com.cedarsoftware.util.convert.MapConversions.EPOCH_MILLIS;
import static com.cedarsoftware.util.convert.MapConversions.ID;
import static com.cedarsoftware.util.convert.MapConversions.INSTANT;
import static com.cedarsoftware.util.convert.MapConversions.LEAST_SIG_BITS;
import static com.cedarsoftware.util.convert.MapConversions.LOCALE;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_DATE;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.LOCAL_TIME;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;
import static com.cedarsoftware.util.convert.MapConversions.MOST_SIG_BITS;
import static com.cedarsoftware.util.convert.MapConversions.OFFSET_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.OFFSET_TIME;
import static com.cedarsoftware.util.convert.MapConversions.PERIOD;
import static com.cedarsoftware.util.convert.MapConversions.SQL_DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIMESTAMP;
import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static com.cedarsoftware.util.convert.MapConversions.V;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class ConverterEverythingTest {
    private static final String TOKYO = "Asia/Tokyo";
    private static final ZoneId TOKYO_Z = ZoneId.of(TOKYO);
    private static final ZoneOffset TOKYO_ZO = ZoneOffset.of("+09:00");
    private static final TimeZone TOKYO_TZ = TimeZone.getTimeZone(TOKYO_Z);
    private static final Set<Class<?>> immutable = new HashSet<>();
    private static final long now = System.currentTimeMillis();
    private Converter converter;
    private final ConverterOptions options = new ConverterOptions() {
        public ZoneId getZoneId() {
            return TOKYO_Z;
        }
    };
    private static final Map<Map.Entry<Class<?>, Class<?>>, Object[][]> TEST_DB = new ConcurrentHashMap<>(500, .8f);
    private static final Map<Map.Entry<Class<?>, Class<?>>, Boolean> STAT_DB = new ConcurrentHashMap<>(500, .8f);

    static {
        // List classes that should be checked for immutability
        immutable.add(Number.class);
        immutable.add(byte.class);
        immutable.add(Byte.class);
        immutable.add(short.class);
        immutable.add(Short.class);
        immutable.add(int.class);
        immutable.add(Integer.class);
        immutable.add(long.class);
        immutable.add(Long.class);
        immutable.add(float.class);
        immutable.add(Float.class);
        immutable.add(double.class);
        immutable.add(Double.class);
        immutable.add(boolean.class);
        immutable.add(Boolean.class);
        immutable.add(char.class);
        immutable.add(Character.class);
        immutable.add(BigInteger.class);
        immutable.add(BigDecimal.class);
        immutable.add(LocalTime.class);
        immutable.add(LocalDate.class);
        immutable.add(LocalDateTime.class);
        immutable.add(ZonedDateTime.class);
        immutable.add(OffsetTime.class);
        immutable.add(OffsetDateTime.class);
        immutable.add(Instant.class);
        immutable.add(Duration.class);
        immutable.add(Period.class);
        immutable.add(Month.class);
        immutable.add(Year.class);
        immutable.add(MonthDay.class);
        immutable.add(YearMonth.class);
        immutable.add(Locale.class);
        immutable.add(TimeZone.class);

        loadCollectionTest();
        loadNumberTest();
        loadByteTest();
        loadByteArrayTest();
        loadByteBufferTest();
        loadCharBufferTest();
        loadCharacterArrayTest();
        loadCharArrayTest();
        loadStringBufferTest();
        loadStringBuilderTest();
        loadShortTests();
        loadIntegerTests();
        loadLongTests();
        loadFloatTests();
        loadDoubleTests();
        loadBooleanTests();
        loadCharacterTests();
        loadBigIntegerTests();
        loadBigDecimalTests();
        loadInstantTests();
        loadDateTests();
        loadSqlDateTests();
        loadCalendarTests();
        loadDurationTests();
        loadOffsetDateTimeTests();
        loadMonthDayTests();
        loadYearMonthTests();
        loadPeriodTests();
        loadYearTests();
        loadZoneIdTests();
        loadTimestampTests();
        loadLocalDateTests();
        loadLocalTimeTests();
        loadLocalDateTimeTests();
        loadZoneDateTimeTests();
        loadZoneOffsetTests();
        loadStringTests();
        loadAtomicLongTests();
        loadAtomicIntegerTests();
        loadAtomicBooleanTests();
        loadMapTests();
        loadClassTests();
        loadLocaleTests();
        loadOffsetTimeTests();
        loadTimeZoneTests();
        loadUriTests();
        loadUrlTests();
        loadUuidTests();
        loadEnumTests();
        loadThrowableTests();
        loadCurrencyTests();
        loadPatternTests();
    }

    /**
     * Creates a key pair consisting of source and target classes for conversion mapping.
     *
     * @param source The source class to convert from.
     * @param target The target class to convert to.
     * @return A {@code Map.Entry} representing the source-target class pair.
     */
    static Map.Entry<Class<?>, Class<?>> pair(Class<?> source, Class<?> target) {
        return new AbstractMap.SimpleImmutableEntry<>(source, target);
    }

    /**
     * Currency
     */
    private static void loadPatternTests() {
        TEST_DB.put(pair(Void.class, Pattern.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Pattern.class, Pattern.class), new Object[][]{
                {Pattern.compile("abc"), Pattern.compile("abc")},
        });
        TEST_DB.put(pair(String.class, Pattern.class), new Object[][]{
                {"x.*y", Pattern.compile("x.*y")},
        });
        TEST_DB.put(pair(Map.class, Pattern.class), new Object[][]{
                {mapOf("value", Pattern.compile(".*")), Pattern.compile(".*")},
        });
    }

    /**
     * Currency
     */
    private static void loadCurrencyTests() {
        TEST_DB.put(pair(Void.class, Currency.class), new Object[][]{
                { null, null},
        });
        TEST_DB.put(pair(Currency.class, Currency.class), new Object[][]{
                { Currency.getInstance("USD"), Currency.getInstance("USD") },
                { Currency.getInstance("JPY"), Currency.getInstance("JPY") },
        });
        TEST_DB.put(pair(Map.class, Currency.class), new Object[][] {
                // Bidirectional tests (true) - major currencies
                {mapOf(VALUE, "USD"), Currency.getInstance("USD"), true},
                {mapOf(VALUE, "EUR"), Currency.getInstance("EUR"), true},
                {mapOf(VALUE, "JPY"), Currency.getInstance("JPY"), true},
                {mapOf(VALUE, "GBP"), Currency.getInstance("GBP"), true},

                // One-way tests (false) - with whitespace that should be trimmed
                {mapOf(V, " USD "), Currency.getInstance("USD"), false},
                {mapOf(VALUE, " EUR "), Currency.getInstance("EUR"), false},
                {mapOf(VALUE, "\tJPY\n"), Currency.getInstance("JPY"), false}
        });    }

    /**
     * Enum
     */
    private static void loadEnumTests() {
        TEST_DB.put(pair(Enum.class, Map.class), new Object[][]{
                { DayOfWeek.FRIDAY, mapOf("name", DayOfWeek.FRIDAY.name())},
        });
        TEST_DB.put(pair(Map.class, Enum.class), new Object[][]{
                { mapOf("name", "funky bunch"), new IllegalArgumentException("Unsupported conversion, source type [UnmodifiableMap ({name=funky bunch})] target type 'Enum'")},
        });
    }

    /**
     * Throwable
     */
    private static void loadThrowableTests() {
        TEST_DB.put(pair(Void.class, Throwable.class), new Object[][]{
                {null, null},
        });
        // Would like to add this test, but it triggers
        TEST_DB.put(pair(Map.class, Throwable.class), new Object[][]{
                {mapOf(MESSAGE, "Test error", CAUSE, null), new Throwable("Test error")}
        });
    }

    /**
     * UUID
     */
    private static void loadUuidTests() {
        TEST_DB.put(pair(Void.class, UUID.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(UUID.class, UUID.class), new Object[][]{
                {UUID.fromString("f0000000-0000-0000-0000-000000000001"), UUID.fromString("f0000000-0000-0000-0000-000000000001")},
        });
        TEST_DB.put(pair(Map.class, UUID.class), new Object[][]{
                {mapOf("UUID", "f0000000-0000-0000-0000-000000000001"), UUID.fromString("f0000000-0000-0000-0000-000000000001"), true},
                {mapOf("UUID", "f0000000-0000-0000-0000-00000000000x"), new IllegalArgumentException("Unable to convert 'f0000000-0000-0000-0000-00000000000x' to UUID")},
                {mapOf("xyz", "f0000000-0000-0000-0000-000000000000"), new IllegalArgumentException("Map to 'UUID' the map must include: [UUID], [value], [_v], or [mostSigBits, leastSigBits] as key with associated value")},
                {mapOf(MOST_SIG_BITS, "1", LEAST_SIG_BITS, "2"), UUID.fromString("00000000-0000-0001-0000-000000000002")},
        });
        TEST_DB.put(pair(String.class, UUID.class), new Object[][]{
                {"f0000000-0000-0000-0000-000000000001", UUID.fromString("f0000000-0000-0000-0000-000000000001"), true},
                {"f0000000-0000-0000-0000-00000000000x", new IllegalArgumentException("Unable to convert 'f0000000-0000-0000-0000-00000000000x' to UUID")},
                {"00000000-0000-0000-0000-000000000000", new UUID(0L, 0L), true},
                {"00000000-0000-0001-0000-000000000001", new UUID(1L, 1L), true},
                {"7fffffff-ffff-ffff-7fff-ffffffffffff", new UUID(Long.MAX_VALUE, Long.MAX_VALUE), true},
                {"80000000-0000-0000-8000-000000000000", new UUID(Long.MIN_VALUE, Long.MIN_VALUE), true},
        });
        TEST_DB.put(pair(BigDecimal.class, UUID.class), new Object[][]{
                {BigDecimal.ZERO, new UUID(0L, 0L), true},
                {new BigDecimal("18446744073709551617"), new UUID(1L, 1L), true},
                {new BigDecimal("170141183460469231722463931679029329919"), new UUID(Long.MAX_VALUE, Long.MAX_VALUE), true},
                {BigDecimal.ZERO, UUID.fromString("00000000-0000-0000-0000-000000000000"), true},
                {BigDecimal.valueOf(1), UUID.fromString("00000000-0000-0000-0000-000000000001"), true},
                {new BigDecimal("18446744073709551617"), UUID.fromString("00000000-0000-0001-0000-000000000001"), true},
                {new BigDecimal("340282366920938463463374607431768211455"), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), true},
                {new BigDecimal("340282366920938463463374607431768211454"), UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffffe"), true},
                {new BigDecimal("319014718988379809496913694467282698240"), UUID.fromString("f0000000-0000-0000-0000-000000000000"), true},
                {new BigDecimal("319014718988379809496913694467282698241"), UUID.fromString("f0000000-0000-0000-0000-000000000001"), true},
                {new BigDecimal("170141183460469231731687303715884105726"), UUID.fromString("7fffffff-ffff-ffff-ffff-fffffffffffe"), true},
                {new BigDecimal("170141183460469231731687303715884105727"), UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), true},
                {new BigDecimal("170141183460469231731687303715884105728"), UUID.fromString("80000000-0000-0000-0000-000000000000"), true},
        });
        TEST_DB.put(pair(BigInteger.class, UUID.class), new Object[][]{
                {BigInteger.ZERO, new UUID(0L, 0L), true},
                {new BigInteger("18446744073709551617"), new UUID(1L, 1L), true},
                {new BigInteger("170141183460469231722463931679029329919"), new UUID(Long.MAX_VALUE, Long.MAX_VALUE), true},
                {BigInteger.ZERO, UUID.fromString("00000000-0000-0000-0000-000000000000"), true},
                {BigInteger.valueOf(-1), new IllegalArgumentException("Cannot convert a negative number [-1] to a UUID")},
                {BigInteger.valueOf(1), UUID.fromString("00000000-0000-0000-0000-000000000001"), true},
                {new BigInteger("18446744073709551617"), UUID.fromString("00000000-0000-0001-0000-000000000001"), true},
                {new BigInteger("340282366920938463463374607431768211455"), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), true},
                {new BigInteger("340282366920938463463374607431768211454"), UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffffe"), true},
                {new BigInteger("319014718988379809496913694467282698240"), UUID.fromString("f0000000-0000-0000-0000-000000000000"), true},
                {new BigInteger("319014718988379809496913694467282698241"), UUID.fromString("f0000000-0000-0000-0000-000000000001"), true},
                {new BigInteger("170141183460469231731687303715884105726"), UUID.fromString("7fffffff-ffff-ffff-ffff-fffffffffffe"), true},
                {new BigInteger("170141183460469231731687303715884105727"), UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), true},
                {new BigInteger("170141183460469231731687303715884105728"), UUID.fromString("80000000-0000-0000-0000-000000000000"), true},
        });
    }

    /**
     * URL
     */
    private static void loadUrlTests() {
        TEST_DB.put(pair(Void.class, URL.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(URL.class, URL.class), new Object[][]{
                {toURL("https://chat.openai.com"), toURL("https://chat.openai.com")},
        });
        TEST_DB.put(pair(URI.class, URL.class), new Object[][]{
                {toURI("urn:isbn:0451450523"), new IllegalArgumentException("Unable to convert URI to URL")},
                {toURI("https://cedarsoftware.com"), toURL("https://cedarsoftware.com"), true},
                {toURI("https://cedarsoftware.com:8001"), toURL("https://cedarsoftware.com:8001"), true},
                {toURI("https://cedarsoftware.com:8001#ref1"), toURL("https://cedarsoftware.com:8001#ref1"), true},
        });
        TEST_DB.put(pair(String.class, URL.class), new Object[][]{
                {"", null},
                {"https://domain.com", toURL("https://domain.com"), true},
                {"http://localhost", toURL("http://localhost"), true},
                {"http://localhost:8080", toURL("http://localhost:8080"), true},
                {"http://localhost:8080/file/path", toURL("http://localhost:8080/file/path"), true},
                {"http://localhost:8080/path/file.html", toURL("http://localhost:8080/path/file.html"), true},
                {"http://localhost:8080/path/file.html?foo=1&bar=2", toURL("http://localhost:8080/path/file.html?foo=1&bar=2"), true},
                {"http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation", toURL("http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation"), true},
                {"https://foo.bar.com/", toURL("https://foo.bar.com/"), true},
                {"https://foo.bar.com/path/foo%20bar.html", toURL("https://foo.bar.com/path/foo%20bar.html"), true},
                {"https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter", toURL("https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter"), true},
                {"ftp://user@example.com/foo/bar.txt", toURL("ftp://user@example.com/foo/bar.txt"), true},
                {"ftp://user:password@example.com/foo/bar.txt", toURL("ftp://user:password@example.com/foo/bar.txt"), true},
                {"ftp://user:password@example.com:8192/foo/bar.txt", toURL("ftp://user:password@example.com:8192/foo/bar.txt"), true},
                // These below slow down tests - they work, you can uncomment and verify
//                {"file:/path/to/file", toURL("file:/path/to/file"), true},
//                {"file://localhost/path/to/file.json", toURL("file://localhost/path/to/file.json"), true},
//                {"file://servername/path/to/file.json", toURL("file://servername/path/to/file.json"), true},
                {"jar:file:/c://my.jar!/", toURL("jar:file:/c://my.jar!/"), true},
                {"jar:file:/c://my.jar!/com/mycompany/MyClass.class", toURL("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), true}
        });
        TEST_DB.put(pair(Map.class, URL.class), new Object[][]{
                { mapOf(URL_KEY, "https://domain.com"), toURL("https://domain.com"), true},
                { mapOf(URL_KEY, "bad earl"), new IllegalArgumentException("Cannot convert String 'bad earl' to URL")},
                { mapOf(MapConversions.VALUE, "https://domain.com"), toURL("https://domain.com")},
                { mapOf(V, "https://domain.com"), toURL("https://domain.com")},
        });
        TEST_DB.put(pair(URI.class, URL.class), new Object[][]{
                {toURI("urn:isbn:0451450523"), new IllegalArgumentException("Unable to convert URI to URL")},
        });
    }

    /**
     * URI
     */
    private static void loadUriTests() {
        TEST_DB.put(pair(Void.class, URI.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(URI.class, URI.class), new Object[][]{
                {toURI("https://chat.openai.com"), toURI("https://chat.openai.com"), true},
        });
        TEST_DB.put(pair(URL.class, URI.class), new Object[][]{
                { (Supplier<URL>) () -> {
                    try {return new URL("https://domain.com");} catch(Exception e){return null;}
                }, toURI("https://domain.com"), true},
                { (Supplier<URL>) () -> {
                    try {return new URL("http://example.com/query?param=value with spaces");} catch(Exception e){return null;}
                }, new IllegalArgumentException("with spaces")},
        });
        TEST_DB.put(pair(String.class, URI.class), new Object[][]{
                {"", null},
                {"https://domain.com", toURI("https://domain.com"), true},
                {"http://localhost", toURI("http://localhost"), true},
                {"http://localhost:8080", toURI("http://localhost:8080"), true},
                {"http://localhost:8080/file/path", toURI("http://localhost:8080/file/path"), true},
                {"http://localhost:8080/path/file.html", toURI("http://localhost:8080/path/file.html"), true},
                {"http://localhost:8080/path/file.html?foo=1&bar=2", toURI("http://localhost:8080/path/file.html?foo=1&bar=2"), true},
                {"http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation", toURI("http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation"), true},
                {"https://foo.bar.com/", toURI("https://foo.bar.com/"), true},
                {"https://foo.bar.com/path/foo%20bar.html", toURI("https://foo.bar.com/path/foo%20bar.html"), true},
                {"https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter", toURI("https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter"), true},
                {"ftp://user@example.com/foo/bar.txt", toURI("ftp://user@example.com/foo/bar.txt"), true},
                {"ftp://user:password@example.com/foo/bar.txt", toURI("ftp://user:password@example.com/foo/bar.txt"), true},
                {"ftp://user:password@example.com:8192/foo/bar.txt", toURI("ftp://user:password@example.com:8192/foo/bar.txt"), true},
                {"file:/path/to/file", toURI("file:/path/to/file"), true},
                {"file://localhost/path/to/file.json", toURI("file://localhost/path/to/file.json"), true},
                {"file://servername/path/to/file.json", toURI("file://servername/path/to/file.json"), true},
                {"jar:file:/c://my.jar!/", toURI("jar:file:/c://my.jar!/"), true},
                {"jar:file:/c://my.jar!/com/mycompany/MyClass.class", toURI("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), true}
        });
        TEST_DB.put(pair(Map.class, URI.class), new Object[][]{
                { mapOf(URI_KEY, "https://domain.com"), toURI("https://domain.com"), true},
                { mapOf(URI_KEY, "bad uri"), new IllegalArgumentException("Illegal character in path at index 3: bad uri")},
                { mapOf(MapConversions.VALUE, "https://domain.com"), toURI("https://domain.com")},
        });
    }

    /**
     * TimeZone
     */
    private static void loadTimeZoneTests() {
        TEST_DB.put(pair(Void.class, TimeZone.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(TimeZone.class, TimeZone.class), new Object[][]{
                {TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("GMT")},
        });
        TEST_DB.put(pair(ZoneOffset.class, TimeZone.class), new Object[][]{
                {ZoneOffset.of("Z"), TimeZone.getTimeZone("Z"), true},
                {ZoneOffset.of("+09:00"), TimeZone.getTimeZone(ZoneId.of("+09:00")), true},
        });
        TEST_DB.put(pair(String.class, TimeZone.class), new Object[][]{
                {"", null},
                {"America/New_York", TimeZone.getTimeZone("America/New_York"), true},
                {"EST", TimeZone.getTimeZone("EST"), true},
                {"GMT+05:00", TimeZone.getTimeZone(ZoneId.of("+05:00")), true},
                {"America/Denver", TimeZone.getTimeZone(ZoneId.of("America/Denver")), true},
                {"American/FunkyTown", TimeZone.getTimeZone("GMT")},    // Per javadoc's
                {"GMT", TimeZone.getTimeZone("GMT"), true},            // Added
        });
        TEST_DB.put(pair(Map.class, TimeZone.class), new Object[][]{
                {mapOf(ZONE, "GMT"), TimeZone.getTimeZone("GMT"), true},
                {mapOf(ZONE, "America/New_York"), TimeZone.getTimeZone("America/New_York"), true},
                {mapOf(ZONE, "Asia/Tokyo"), TimeZone.getTimeZone("Asia/Tokyo"), true},
        });
    }

    /**
     * OffsetTime
     */
    private static void loadOffsetTimeTests() {
        TEST_DB.put(pair(Void.class, OffsetTime.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Integer.class, OffsetTime.class), new Object[][]{  // millis
                {-1, OffsetTime.parse("08:59:59.999+09:00"), true},
                {0, OffsetTime.parse("09:00:00.000+09:00"), true},
                {1, OffsetTime.parse("09:00:00.001+09:00"), true},
        });
        TEST_DB.put(pair(Long.class, OffsetTime.class), new Object[][]{ // millis
                {-1L, OffsetTime.parse("08:59:59.999+09:00"), true},
                {0L, OffsetTime.parse("09:00:00.000+09:00"), true},
                {1L, OffsetTime.parse("09:00:00.001+09:00"), true},
        });
        TEST_DB.put(pair(Double.class, OffsetTime.class), new Object[][]{   // seconds & fractional seconds
                {-1d, OffsetTime.parse("08:59:59.000+09:00"), true},
                {-1.1, OffsetTime.parse("08:59:58.9+09:00"), true},
                {0d, OffsetTime.parse("09:00:00.000+09:00"), true},
                {1d, OffsetTime.parse("09:00:01.000+09:00"), true},
                {1.1d, OffsetTime.parse("09:00:01.1+09:00"), true},
                {1.01d, OffsetTime.parse("09:00:01.01+09:00"), true},
                {1.002d, OffsetTime.parse("09:00:01.002+09:00"), true},   // skipped 1.001 because of double's imprecision
        });
        TEST_DB.put(pair(BigInteger.class, OffsetTime.class), new Object[][]{  // nanos
                {BigInteger.valueOf(-1), OffsetTime.parse("08:59:59.999999999+09:00"), true},
                {BigInteger.valueOf(0), OffsetTime.parse("09:00:00+09:00"), true},
                {BigInteger.valueOf(1), OffsetTime.parse("09:00:00.000000001+09:00"), true},
                {BigInteger.valueOf(1000000000), OffsetTime.parse("09:00:01+09:00"), true},
                {BigInteger.valueOf(1000000001), OffsetTime.parse("09:00:01.000000001+09:00"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, OffsetTime.class), new Object[][]{  // seconds & fractional seconds
                {BigDecimal.valueOf(-1), OffsetTime.parse("08:59:59+09:00"), true},
                {BigDecimal.valueOf(-1.1), OffsetTime.parse("08:59:58.9+09:00"), true},
                {BigDecimal.valueOf(0), OffsetTime.parse("09:00:00+09:00"), true},
                {BigDecimal.valueOf(1), OffsetTime.parse("09:00:01+09:00"), true},
                {BigDecimal.valueOf(1.1), OffsetTime.parse("09:00:01.1+09:00"), true},
                {BigDecimal.valueOf(1.01), OffsetTime.parse("09:00:01.01+09:00"), true},
                {BigDecimal.valueOf(1.001), OffsetTime.parse("09:00:01.001+09:00"), true},    // no imprecision with BigDecimal
        });
        TEST_DB.put(pair(AtomicInteger.class, OffsetTime.class), new Object[][]{        // millis
                {new AtomicInteger(-1), OffsetTime.parse("08:59:59.999+09:00"), true},
                {new AtomicInteger(0), OffsetTime.parse("09:00:00.000+09:00"), true},
                {new AtomicInteger(1), OffsetTime.parse("09:00:00.001+09:00"), true},
        });
        TEST_DB.put(pair(AtomicLong.class, OffsetTime.class), new Object[][]{           // millis
                {new AtomicLong(-1), OffsetTime.parse("08:59:59.999+09:00"), true},
                {new AtomicLong(0), OffsetTime.parse("09:00:00.000+09:00"), true},
                {new AtomicLong(1), OffsetTime.parse("09:00:00.001+09:00"), true},
        });
        TEST_DB.put(pair(OffsetTime.class, OffsetTime.class), new Object[][]{
                {OffsetTime.parse("00:00+09:00"), OffsetTime.parse("00:00:00+09:00"), true},
        });
        TEST_DB.put(pair(String.class, OffsetTime.class), new Object[][]{
                {"", null},
                {"2024-03-23T03:51", OffsetTime.parse("03:51+09:00")},
                {"10:15:30+01:00", OffsetTime.parse("10:15:30+01:00"), true},
                {"10:15:30+01:00:59", OffsetTime.parse("10:15:30+01:00:59"), true},
                {"10:15:30+01:00.001", new IllegalArgumentException("Unable to parse '10:15:30+01:00.001' as an OffsetTime")},
        });
        TEST_DB.put(pair(Map.class, OffsetTime.class), new Object[][]{
                {mapOf(OFFSET_TIME, "00:00+09:00"), OffsetTime.parse("00:00+09:00"), true},
                {mapOf(OFFSET_TIME, "00:00+09:01:23"), OffsetTime.parse("00:00+09:01:23"), true},
                {mapOf(OFFSET_TIME, "00:00+09:01:23.1"), new IllegalArgumentException("Unable to parse '00:00+09:01:23.1' as an OffsetTime")},
                {mapOf(OFFSET_TIME, "00:00-09:00"), OffsetTime.parse("00:00-09:00"), true},
                {mapOf(OFFSET_TIME, "00:00:00+09:00"), OffsetTime.parse("00:00+09:00")},       // no reverse
                {mapOf(OFFSET_TIME, "00:00:00+09:00:00"), OffsetTime.parse("00:00+09:00")},    // no reverse
                {mapOf(OFFSET_TIME, "garbage"), new IllegalArgumentException("Unable to parse 'garbage' as an OffsetTime")},    // no reverse
                {mapOf(OFFSET_TIME, "01:30"), new IllegalArgumentException("Unable to parse '01:30' as an OffsetTime")},
                {mapOf(OFFSET_TIME, "01:30:59"), new IllegalArgumentException("Unable to parse '01:30:59' as an OffsetTime")},
                {mapOf(OFFSET_TIME, "01:30:59.123456789"), new IllegalArgumentException("Unable to parse '01:30:59.123456789' as an OffsetTime")},
                {mapOf(OFFSET_TIME, "01:30:59.123456789-05:30"), OffsetTime.parse("01:30:59.123456789-05:30")},
                {mapOf(OFFSET_TIME, "01:30:59.123456789-05:3x"), new IllegalArgumentException("Unable to parse '01:30:59.123456789-05:3x' as an OffsetTime")},
                {mapOf(VALUE, "16:20:00-05:00"), OffsetTime.parse("16:20:00-05:00") },
        });
        TEST_DB.put(pair(OffsetDateTime.class, OffsetTime.class), new Object[][]{
                {odt("1969-12-31T23:59:59.999999999Z"), OffsetTime.parse("08:59:59.999999999+09:00")},
                {odt("1970-01-01T00:00Z"), OffsetTime.parse("09:00+09:00")},
                {odt("1970-01-01T00:00:00.000000001Z"), OffsetTime.parse("09:00:00.000000001+09:00")},
        });
    }

    /**
     * Locale
     */
    private static void loadLocaleTests() {
        TEST_DB.put(pair(Void.class, Locale.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Locale.class, Locale.class), new Object[][]{
                {Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-US")},
        });
        TEST_DB.put(pair(String.class, Locale.class), new Object[][]{
                { "", null},
                { "en-Latn-US-POSIX", Locale.forLanguageTag("en-Latn-US-POSIX"), true},
                { "en-Latn-US", Locale.forLanguageTag("en-Latn-US"), true},
                { "en-US", Locale.forLanguageTag("en-US"), true},
                { "en", Locale.forLanguageTag("en"), true},
        });
        TEST_DB.put(pair(Map.class, Locale.class), new Object[][]{
                {mapOf(LOCALE, "joker 75-Latn-US-POSIX"), Locale.forLanguageTag("joker 75-Latn-US-POSIX")},
                {mapOf(LOCALE, "en-Amerika-Latn-POSIX"), Locale.forLanguageTag("en-Amerika-Latn-POSIX")},
                {mapOf(LOCALE, "en-US-Jello-POSIX"), Locale.forLanguageTag("en-US-Jello-POSIX")},
                {mapOf(LOCALE, "en-Latn-US-Monkey @!#!# "), Locale.forLanguageTag("en-Latn-US-Monkey @!#!# ")},
                {mapOf(LOCALE, "en-Latn-US-POSIX"), Locale.forLanguageTag("en-Latn-US-POSIX"), true},
                {mapOf(LOCALE, "en-Latn-US"), Locale.forLanguageTag("en-Latn-US"), true},
                {mapOf(LOCALE, "en-US"), Locale.forLanguageTag("en-US"), true},
                {mapOf(LOCALE, "en"), Locale.forLanguageTag("en"), true},
                {mapOf(V, "en-Latn-US-POSIX"), Locale.forLanguageTag("en-Latn-US-POSIX")},   // no reverse
                {mapOf(VALUE, "en-Latn-US-POSIX"), Locale.forLanguageTag("en-Latn-US-POSIX")},   // no reverse
        });
    }

    /**
     * Map
     */
    private static void loadClassTests() {
        TEST_DB.put(pair(Void.class, Class.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Class.class, Class.class), new Object[][]{
                {int.class, int.class}
        });
        TEST_DB.put(pair(String.class, Class.class), new Object[][]{
                {"java.util.Date", Date.class, true},
                {"NoWayJose", new IllegalArgumentException("not found")},
        });
        TEST_DB.put(pair(Map.class, Class.class), new Object[][]{
                { mapOf(V, Long.class), Long.class, true},
                { mapOf(VALUE, "not a class"), new IllegalArgumentException("Cannot convert String 'not a class' to class.  Class not found")},
        });
    }

    /**
     * Map
     */
    private static void loadMapTests() {
        TEST_DB.put(pair(Void.class, Map.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Pattern.class, Map.class), new Object[][]{
                {Pattern.compile("(foo|bar)"), mapOf(VALUE, "(foo|bar)")},
        });
        TEST_DB.put(pair(Map.class, Map.class), new Object[][]{
                { new HashMap<>(), new IllegalArgumentException("Unsupported conversion") }
        });
        TEST_DB.put(pair(ByteBuffer.class, Map.class), new Object[][]{
                {ByteBuffer.wrap("ABCD\0\0zyxw".getBytes(StandardCharsets.UTF_8)), mapOf(VALUE, "QUJDRAAAenl4dw==")},
                {ByteBuffer.wrap("\0\0foo\0\0".getBytes(StandardCharsets.UTF_8)), mapOf(VALUE, "AABmb28AAA==")},
        });
        TEST_DB.put(pair(CharBuffer.class, Map.class), new Object[][]{
                {CharBuffer.wrap("ABCD\0\0zyxw"), mapOf(VALUE, "ABCD\0\0zyxw")},
                {CharBuffer.wrap("\0\0foo\0\0"), mapOf(VALUE, "\0\0foo\0\0")},
        });
        TEST_DB.put(pair(Throwable.class, Map.class), new Object[][]{
                { new Throwable("divide by 0", new IllegalArgumentException("root issue")), mapOf(MESSAGE, "divide by 0", CLASS, Throwable.class.getName(), CAUSE, IllegalArgumentException.class.getName(), CAUSE_MESSAGE, "root issue")},
                { new IllegalArgumentException("null not allowed"), mapOf(MESSAGE, "null not allowed", CLASS, IllegalArgumentException.class.getName())},
        });
    }

    /**
     * AtomicBoolean
     */
    private static void loadAtomicBooleanTests() {
        TEST_DB.put(pair(Void.class, AtomicBoolean.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Short.class, AtomicBoolean.class), new Object[][]{
                {(short)-1, new AtomicBoolean(true)},
                {(short)0, new AtomicBoolean(false), true},
                {(short)1, new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(Integer.class, AtomicBoolean.class), new Object[][]{
                {-1, new AtomicBoolean(true)},
                {0, new AtomicBoolean(false), true},
                {1, new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(Long.class, AtomicBoolean.class), new Object[][]{
                {-1L, new AtomicBoolean(true)},
                {0L, new AtomicBoolean(false), true},
                {1L, new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(Float.class, AtomicBoolean.class), new Object[][]{
                {1.9f, new AtomicBoolean(true)},
                {1.0f, new AtomicBoolean(true), true},
                {-1.0f, new AtomicBoolean(true)},
                {0.0f, new AtomicBoolean(false), true},
        });
        TEST_DB.put(pair(Double.class, AtomicBoolean.class), new Object[][]{
                {1.1, new AtomicBoolean(true)},
                {1.0, new AtomicBoolean(true), true},
                {-1.0, new AtomicBoolean(true)},
                {0.0, new AtomicBoolean(false), true},
        });
        TEST_DB.put(pair(AtomicBoolean.class, AtomicBoolean.class), new Object[][] {
                { new AtomicBoolean(false), new AtomicBoolean(false)},
                { new AtomicBoolean(true), new AtomicBoolean(true)},
        });
        TEST_DB.put(pair(AtomicInteger.class, AtomicBoolean.class), new Object[][] {
                { new AtomicInteger(-1), new AtomicBoolean(true)},
                { new AtomicInteger(0), new AtomicBoolean(false), true},
                { new AtomicInteger(1), new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(AtomicLong.class, AtomicBoolean.class), new Object[][] {
                { new AtomicLong((byte)-1), new AtomicBoolean(true)},
                { new AtomicLong((byte)0), new AtomicBoolean(false), true},
                { new AtomicLong((byte)1), new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(BigInteger.class, AtomicBoolean.class), new Object[][] {
                { BigInteger.valueOf(-1), new AtomicBoolean(true)},
                { BigInteger.ZERO, new AtomicBoolean(false), true},
                { BigInteger.valueOf(1), new AtomicBoolean(true), true},
        });
        TEST_DB.put(pair(BigDecimal.class, AtomicBoolean.class), new Object[][] {
                { new BigDecimal("-1.1"), new AtomicBoolean(true)},
                { BigDecimal.valueOf(-1), new AtomicBoolean(true)},
                { BigDecimal.ZERO, new AtomicBoolean(false), true},
                { BigDecimal.valueOf(1), new AtomicBoolean(true), true},
                { new BigDecimal("1.1"), new AtomicBoolean(true)},
        });
        TEST_DB.put(pair(Character.class, AtomicBoolean.class), new Object[][]{
                {(char) 0, new AtomicBoolean(false), true},
                {(char) 1, new AtomicBoolean(true), true},
                {'0', new AtomicBoolean(false)},
                {'1', new AtomicBoolean(true)},
                {'f', new AtomicBoolean(false)},
                {'t', new AtomicBoolean(true)},
                {'F', new AtomicBoolean(false)},
                {'T', new AtomicBoolean(true)},
        });
        TEST_DB.put(pair(Year.class, AtomicBoolean.class), new Object[][]{
                {Year.of(2024), new AtomicBoolean(true)},
                {Year.of(0), new AtomicBoolean(false)},
                {Year.of(1), new AtomicBoolean(true)},
        });
        TEST_DB.put(pair(String.class, AtomicBoolean.class), new Object[][]{
                {"false", new AtomicBoolean(false), true},
                {"true", new AtomicBoolean(true), true},
                {"t", new AtomicBoolean(true)},
                {"f", new AtomicBoolean(false)},
                {"x", new AtomicBoolean(false)},
                {"z", new AtomicBoolean(false)},
        });
        TEST_DB.put(pair(Map.class, AtomicBoolean.class), new Object[][] {
                { mapOf("_v", "true"), new AtomicBoolean(true)},
                { mapOf("_v", true), new AtomicBoolean(true)},
                { mapOf("_v", "false"), new AtomicBoolean(false)},
                { mapOf("_v", false), new AtomicBoolean(false)},
                { mapOf("_v", BigInteger.valueOf(1)), new AtomicBoolean(true)},
                { mapOf("_v", BigDecimal.ZERO), new AtomicBoolean(false)},
        });
    }

    /**
     * AtomicInteger
     */
    private static void loadAtomicIntegerTests() {
        TEST_DB.put(pair(Void.class, AtomicInteger.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Integer.class, AtomicInteger.class), new Object[][]{
                {-1, new AtomicInteger(-1)},
                {0, new AtomicInteger(0), true},
                {1, new AtomicInteger(1), true},
                {Integer.MIN_VALUE, new AtomicInteger(-2147483648)},
                {Integer.MAX_VALUE, new AtomicInteger(2147483647)},
        });
        TEST_DB.put(pair(Long.class, AtomicInteger.class), new Object[][]{
                {-1L, new AtomicInteger(-1)},
                {0L, new AtomicInteger(0), true},
                {1L, new AtomicInteger(1), true},
                {(long)Integer.MIN_VALUE, new AtomicInteger(-2147483648)},
                {(long)Integer.MAX_VALUE, new AtomicInteger(2147483647)},
        });
        TEST_DB.put(pair(AtomicInteger.class, AtomicInteger.class), new Object[][] {
                { new AtomicInteger(1), new AtomicInteger((byte)1), true}
        });
        TEST_DB.put(pair(AtomicLong.class, AtomicInteger.class), new Object[][] {
                { new AtomicLong(Integer.MIN_VALUE), new AtomicInteger(Integer.MIN_VALUE), true},
                { new AtomicLong(-1), new AtomicInteger((byte)-1), true},
                { new AtomicLong(0), new AtomicInteger(0), true},
                { new AtomicLong(1), new AtomicInteger((byte)1), true},
                { new AtomicLong(Integer.MAX_VALUE), new AtomicInteger(Integer.MAX_VALUE), true},
        });
        TEST_DB.put(pair(Float.class, AtomicInteger.class), new Object[][]{
                {0.0f, new AtomicInteger(0), true},
                {-1.0f, new AtomicInteger(-1)},
                {1.0f, new AtomicInteger(1), true},
                {-16777216.0f, new AtomicInteger(-16777216)},
                {16777216.0f, new AtomicInteger(16777216)},
        });
        TEST_DB.put(pair(Double.class, AtomicInteger.class), new Object[][]{
                {(double) Integer.MIN_VALUE, new AtomicInteger(-2147483648), true},
                {-1.99, new AtomicInteger(-1)},
                {-1.0, new AtomicInteger(-1), true},
                {0.0, new AtomicInteger(0), true},
                {1.0, new AtomicInteger(1), true},
                {1.99, new AtomicInteger(1)},
                {(double) Integer.MAX_VALUE, new AtomicInteger(2147483647), true},
        });
        TEST_DB.put(pair(BigInteger.class, AtomicInteger.class), new Object[][] {
                { BigInteger.valueOf(Integer.MIN_VALUE), new AtomicInteger(Integer.MIN_VALUE), true},
                { BigInteger.valueOf(-1), new AtomicInteger((byte)-1), true},
                { BigInteger.valueOf(0), new AtomicInteger(0), true},
                { BigInteger.valueOf(1), new AtomicInteger((byte)1), true},
                { BigInteger.valueOf(Integer.MAX_VALUE), new AtomicInteger(Integer.MAX_VALUE), true},
        });
        TEST_DB.put(pair(String.class, AtomicInteger.class), new Object[][]{
                {"-1", new AtomicInteger(-1), true},
                {"0", new AtomicInteger(0), true},
                {"1", new AtomicInteger(1), true},
                {"-2147483648", new AtomicInteger(Integer.MIN_VALUE), true},
                {"2147483647", new AtomicInteger(Integer.MAX_VALUE), true},
                {"bad man", new IllegalArgumentException("'bad man' not parseable")},
        });
    }

    /**
     * AtomicLong
     */
    private static void loadAtomicLongTests() {
        TEST_DB.put(pair(Void.class, AtomicLong.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(AtomicLong.class, AtomicLong.class), new Object[][]{
                {new AtomicLong(16), new AtomicLong(16)}
        });
        TEST_DB.put(pair(Long.class, AtomicLong.class), new Object[][]{
                {-1L, new AtomicLong(-1), true},
                {0L, new AtomicLong(0), true},
                {1L, new AtomicLong(1), true},
                {Long.MAX_VALUE, new AtomicLong(Long.MAX_VALUE), true},
                {Long.MIN_VALUE, new AtomicLong(Long.MIN_VALUE), true},
        });
        TEST_DB.put(pair(Float.class, AtomicLong.class), new Object[][]{
                {-1f, new AtomicLong(-1), true},
                {0f, new AtomicLong(0), true},
                {1f, new AtomicLong(1), true},
                {-16777216f, new AtomicLong(-16777216), true},
                {16777216f, new AtomicLong(16777216), true},
        });
        TEST_DB.put(pair(Double.class, AtomicLong.class), new Object[][]{
                {-9007199254740991.0, new AtomicLong(-9007199254740991L), true},
                {-1.99, new AtomicLong(-1)},
                {-1.0, new AtomicLong(-1), true},
                {0.0, new AtomicLong(0), true},
                {1.0, new AtomicLong(1), true},
                {1.99, new AtomicLong(1)},
                {9007199254740991.0, new AtomicLong(9007199254740991L), true},
        });
        TEST_DB.put(pair(BigInteger.class, AtomicLong.class), new Object[][] {
                { BigInteger.valueOf(Long.MIN_VALUE), new AtomicLong(Long.MIN_VALUE), true},
                { BigInteger.valueOf(-1), new AtomicLong((byte)-1), true},
                { BigInteger.valueOf(0), new AtomicLong(0), true},
                { BigInteger.valueOf(1), new AtomicLong((byte)1), true},
                { BigInteger.valueOf(Long.MAX_VALUE), new AtomicLong(Long.MAX_VALUE), true},
        });
        TEST_DB.put(pair(Instant.class, AtomicLong.class), new Object[][]{
                {Instant.parse("0000-01-01T00:00:00Z"), new AtomicLong(-62167219200000L), true},
                {Instant.parse("0000-01-01T00:00:00.001Z"), new AtomicLong(-62167219199999L), true},
                {Instant.parse("1969-12-31T23:59:59Z"), new AtomicLong(-1000L), true},
                {Instant.parse("1969-12-31T23:59:59.999Z"), new AtomicLong(-1L), true},
                {Instant.parse("1970-01-01T00:00:00Z"), new AtomicLong(0L), true},
                {Instant.parse("1970-01-01T00:00:00.001Z"), new AtomicLong(1L), true},
                {Instant.parse("1970-01-01T00:00:00.999Z"), new AtomicLong(999L), true},
        });
        TEST_DB.put(pair(Duration.class, AtomicLong.class), new Object[][]{
                {Duration.ofMillis(Long.MIN_VALUE / 2), new AtomicLong(Long.MIN_VALUE / 2), true},
                {Duration.ofMillis(Integer.MIN_VALUE), new AtomicLong(Integer.MIN_VALUE), true},
                {Duration.ofMillis(-1), new AtomicLong(-1), true},
                {Duration.ofMillis(0), new AtomicLong(0), true},
                {Duration.ofMillis(1), new AtomicLong(1), true},
                {Duration.ofMillis(Integer.MAX_VALUE), new AtomicLong(Integer.MAX_VALUE), true},
                {Duration.ofMillis(Long.MAX_VALUE / 2), new AtomicLong(Long.MAX_VALUE / 2), true},
        });
        TEST_DB.put(pair(String.class, AtomicLong.class), new Object[][]{
                {"-1", new AtomicLong(-1), true},
                {"0", new AtomicLong(0), true},
                {"1", new AtomicLong(1), true},
                {"-9223372036854775808", new AtomicLong(Long.MIN_VALUE), true},
                {"9223372036854775807", new AtomicLong(Long.MAX_VALUE), true},
        });
        TEST_DB.put(pair(Map.class, AtomicLong.class), new Object[][]{
                {mapOf(VALUE, new AtomicLong(0)), new AtomicLong(0)},
                {mapOf(VALUE, new AtomicLong(1)), new AtomicLong(1)},
                {mapOf(VALUE, 1), new AtomicLong(1)},
        });
    }

    /**
     * String
     */
    private static void loadStringTests() {
        TEST_DB.put(pair(Void.class, String.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(BigInteger.class, String.class), new Object[][]{
                {new BigInteger("-1"), "-1"},
                {BigInteger.ZERO, "0"},
                {new BigInteger("1"), "1"},
        });
        TEST_DB.put(pair(byte[].class, String.class), new Object[][]{
                {new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x8d, (byte) 0xba}, "\uD83C\uDF7A", true}, // beer mug, byte[] treated as UTF-8.
                {new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68}, "ABCD", true}
        });
        TEST_DB.put(pair(Character[].class, String.class), new Object[][]{
                {new Character[]{'A', 'B', 'C', 'D'}, "ABCD", true}
        });
        TEST_DB.put(pair(ByteBuffer.class, String.class), new Object[][]{
                {ByteBuffer.wrap(new byte[]{(byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33}), "0123", true}
        });
        TEST_DB.put(pair(java.sql.Date.class, String.class), new Object[][]{
                // Basic cases around epoch
                {java.sql.Date.valueOf("1969-12-31"), "1969-12-31", true},
                {java.sql.Date.valueOf("1970-01-01"), "1970-01-01", true},

                // Modern dates
                {java.sql.Date.valueOf("2025-01-29"), "2025-01-29", true},
                {java.sql.Date.valueOf("2025-12-31"), "2025-12-31", true},

                // Edge cases
                {java.sql.Date.valueOf("0001-01-01"), "0001-01-01", true},
                {java.sql.Date.valueOf("9999-12-31"), "9999-12-31", true},

                // Leap year cases
                {java.sql.Date.valueOf("2024-02-29"), "2024-02-29", true},
                {java.sql.Date.valueOf("2000-02-29"), "2000-02-29", true},

                // Month boundaries
                {java.sql.Date.valueOf("2025-01-01"), "2025-01-01", true},
                {java.sql.Date.valueOf("2025-12-31"), "2025-12-31", true}
        });
        TEST_DB.put(pair(Timestamp.class, String.class), new Object[][]{
                {new Timestamp(-1), "1969-12-31T23:59:59.999Z", true},
                {new Timestamp(0), "1970-01-01T00:00:00.000Z", true},
                {new Timestamp(1), "1970-01-01T00:00:00.001Z", true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, String.class), new Object[][]{
                // UTC/Zero offset cases
                {ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z[UTC]"), "1969-12-31T23:59:59.999999999Z[UTC]", true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z[UTC]"), "1970-01-01T00:00:00Z[UTC]", true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z[UTC]"), "1970-01-01T00:00:00.000000001Z[UTC]", true},

                // Different time zones and offsets
                {ZonedDateTime.parse("2024-02-02T15:30:00+05:30[Asia/Kolkata]"), "2024-02-02T15:30:00+05:30[Asia/Kolkata]", true},
                {ZonedDateTime.parse("2024-02-02T10:00:00-05:00[America/New_York]"), "2024-02-02T10:00:00-05:00[America/New_York]", true},
                {ZonedDateTime.parse("2024-02-02T19:00:00+09:00[Asia/Tokyo]"), "2024-02-02T19:00:00+09:00[Asia/Tokyo]", true},

                // DST transition times (non-ambiguous)
                {ZonedDateTime.parse("2024-03-10T01:59:59-05:00[America/New_York]"), "2024-03-10T01:59:59-05:00[America/New_York]", true},  // Just before spring forward
                {ZonedDateTime.parse("2024-03-10T03:00:00-04:00[America/New_York]"), "2024-03-10T03:00:00-04:00[America/New_York]", true},  // Just after spring forward
                {ZonedDateTime.parse("2024-11-03T00:59:59-04:00[America/New_York]"), "2024-11-03T00:59:59-04:00[America/New_York]", true},  // Before fall back
                {ZonedDateTime.parse("2024-11-03T02:00:00-05:00[America/New_York]"), "2024-11-03T02:00:00-05:00[America/New_York]", true},  // After fall back

                // Different precisions
                {ZonedDateTime.parse("2024-02-02T12:00:00+01:00[Europe/Paris]"), "2024-02-02T12:00:00+01:00[Europe/Paris]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00.123+01:00[Europe/Paris]"), "2024-02-02T12:00:00.123+01:00[Europe/Paris]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00.123456789+01:00[Europe/Paris]"), "2024-02-02T12:00:00.123456789+01:00[Europe/Paris]", true},

                // Extreme dates
                {ZonedDateTime.parse("+999999999-12-31T23:59:59.999999999Z[UTC]"), "+999999999-12-31T23:59:59.999999999Z[UTC]", true},
                {ZonedDateTime.parse("-999999999-01-01T00:00:00Z[UTC]"), "-999999999-01-01T00:00:00Z[UTC]", true},

                // Special zones
                {ZonedDateTime.parse("2024-02-02T12:00:00+00:00[Etc/GMT]"), "2024-02-02T12:00:00Z[Etc/GMT]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00+00:00[Etc/UTC]"), "2024-02-02T12:00:00Z[Etc/UTC]", true},

                // Zones with unusual offsets
                {ZonedDateTime.parse("2024-02-02T12:00:00+05:45[Asia/Kathmandu]"), "2024-02-02T12:00:00+05:45[Asia/Kathmandu]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00+13:00[Pacific/Apia]"), "2024-02-02T12:00:00+13:00[Pacific/Apia]", true},

                {ZonedDateTime.parse("2024-11-03T01:00:00-04:00[America/New_York]"), "2024-11-03T01:00:00-04:00[America/New_York]", true},  // Before transition
                {ZonedDateTime.parse("2024-11-03T02:00:00-05:00[America/New_York]"), "2024-11-03T02:00:00-05:00[America/New_York]", true},  // After transition

                // International Date Line cases
                {ZonedDateTime.parse("2024-02-02T23:59:59+14:00[Pacific/Kiritimati]"), "2024-02-02T23:59:59+14:00[Pacific/Kiritimati]", true},
                {ZonedDateTime.parse("2024-02-02T00:00:00-11:00[Pacific/Niue]"), "2024-02-02T00:00:00-11:00[Pacific/Niue]", true},

                // Historical timezone changes (after standardization)
                {ZonedDateTime.parse("1920-01-01T12:00:00-05:00[America/New_York]"), "1920-01-01T12:00:00-05:00[America/New_York]", true},

                // Leap second potential dates (even though Java doesn't handle leap seconds)
                {ZonedDateTime.parse("2016-12-31T23:59:59Z[UTC]"), "2016-12-31T23:59:59Z[UTC]", true},
                {ZonedDateTime.parse("2017-01-01T00:00:00Z[UTC]"), "2017-01-01T00:00:00Z[UTC]", true},

                // Military time zones
                {ZonedDateTime.parse("2024-02-02T12:00:00Z[Etc/GMT-0]"), "2024-02-02T12:00:00Z[Etc/GMT-0]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00+01:00[Etc/GMT-1]"), "2024-02-02T12:00:00+01:00[Etc/GMT-1]", true},

                // More precision variations
                {ZonedDateTime.parse("2024-02-02T12:00:00.1+01:00[Europe/Paris]"), "2024-02-02T12:00:00.1+01:00[Europe/Paris]", true},
                {ZonedDateTime.parse("2024-02-02T12:00:00.12+01:00[Europe/Paris]"), "2024-02-02T12:00:00.12+01:00[Europe/Paris]", true},
                
                // Year boundary cases
                {ZonedDateTime.parse("2024-12-31T23:59:59.999999999-05:00[America/New_York]"), "2024-12-31T23:59:59.999999999-05:00[America/New_York]", true},
                {ZonedDateTime.parse("2025-01-01T00:00:00-05:00[America/New_York]"), "2025-01-01T00:00:00-05:00[America/New_York]", true},
        });
        TEST_DB.put(pair(Map.class, String.class), new Object[][]{
                {mapOf("_v", "alpha"), "alpha"},
                {mapOf("value", "alpha"), "alpha"},
        });
        TEST_DB.put(pair(Enum.class, String.class), new Object[][]{
                {DayOfWeek.MONDAY, "MONDAY"},
                {Month.JANUARY, "JANUARY"},
        });
        TEST_DB.put(pair(String.class, String.class), new Object[][]{
                {"same", "same"},
        });
        TEST_DB.put(pair(StringBuffer.class, String.class), new Object[][]{
                {new StringBuffer("buffy"), "buffy"},
        });
        TEST_DB.put(pair(StringBuilder.class, String.class), new Object[][]{
                {new StringBuilder("buildy"), "buildy"},
        });
        TEST_DB.put(pair(Pattern.class, String.class), new Object[][] {
                {Pattern.compile("\\d+"), "\\d+", false},
                {Pattern.compile("\\w+"), "\\w+", false},
                {Pattern.compile("[a-zA-Z]+"), "[a-zA-Z]+", false},
                {Pattern.compile("\\s*"), "\\s*", false},
                {Pattern.compile("^abc$"), "^abc$", false},
                {Pattern.compile("(foo|bar)"), "(foo|bar)", false},
                {Pattern.compile("a{1,3}"), "a{1,3}", false},
                {Pattern.compile("[^\\s]+"), "[^\\s]+", false}
        });
        TEST_DB.put(pair(String.class, Currency.class), new Object[][] {
                {"USD", Currency.getInstance("USD"), true},
                {"EUR", Currency.getInstance("EUR"), true},
                {"JPY", Currency.getInstance("JPY"), true},
                {" USD ", Currency.getInstance("USD"), false}  // one-way due to trimming
        });
    }

    /**
     * ZoneOffset
     */
    private static void loadZoneOffsetTests() {
        TEST_DB.put(pair(Void.class, ZoneOffset.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(ZoneOffset.class, ZoneOffset.class), new Object[][]{
                {ZoneOffset.of("-05:00"), ZoneOffset.of("-05:00")},
                {ZoneOffset.of("+5"), ZoneOffset.of("+05:00")},
        });
        TEST_DB.put(pair(ZoneId.class, ZoneOffset.class), new Object[][]{
                {ZoneId.of("Asia/Tokyo"), ZoneOffset.of("+09:00")},
        });
        TEST_DB.put(pair(String.class, ZoneOffset.class), new Object[][]{
                {"", null},
                {"-00:00", ZoneOffset.of("+00:00")},
                {"-05:00", ZoneOffset.of("-05:00"), true},
                {"+5", ZoneOffset.of("+05:00")},
                {"+05:00:01", ZoneOffset.of("+05:00:01"), true},
                {"05:00:01", new IllegalArgumentException("Unknown time-zone offset: '05:00:01'")},
                {"America/New_York", new IllegalArgumentException("Unknown time-zone offset: 'America/New_York'")},
        });
        TEST_DB.put(pair(Map.class, ZoneOffset.class), new Object[][]{
                {mapOf(ZONE_OFFSET, "+05:30:16"), ZoneOffset.of("+05:30:16"), true},
                {mapOf(ZONE_OFFSET, "+05:30:16"), ZoneOffset.of("+05:30:16"), true},
                {mapOf(VALUE, "-10:00"), ZoneOffset.of("-10:00")},
                {mapOf(V, "-10:00"), ZoneOffset.of("-10:00")},
                {mapOf(ZONE_OFFSET, "-10:00"), ZoneOffset.of("-10:00"), true},
                {mapOf("invalid", "-10:00"), new IllegalArgumentException("'ZoneOffset' the map must include: [zoneOffset], [value], or [_v]")},
                {mapOf(ZONE_OFFSET, "-10:00"), ZoneOffset.of("-10:00")},
                {mapOf(ZONE_OFFSET, "-10:15:01"), ZoneOffset.of("-10:15:01")},
                {mapOf(ZONE_OFFSET, "+10:15:01"), ZoneOffset.of("+10:15:01")},
        });
    }

    /**
     * ZonedDateTime
     */
    private static void loadZoneDateTimeTests() {
        TEST_DB.put(pair(Void.class, ZonedDateTime.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(ZonedDateTime.class, ZonedDateTime.class), new Object[][]{
                {zdt("1970-01-01T00:00:00.000000000Z"), zdt("1970-01-01T00:00:00.000000000Z")},
        });
        TEST_DB.put(pair(Double.class, ZonedDateTime.class), new Object[][]{
                {-62167219200.0, zdt("0000-01-01T00:00:00Z"), true},
                {-0.000000001, zdt("1969-12-31T23:59:59.999999999Z"), true},
                {0.0, zdt("1970-01-01T00:00:00Z"), true},
                {0.000000001, zdt("1970-01-01T00:00:00.000000001Z"), true},
                {86400d, zdt("1970-01-02T00:00:00Z"), true},
                {86400.000000001, zdt("1970-01-02T00:00:00.000000001Z"), true},
        });
        TEST_DB.put(pair(AtomicLong.class, ZonedDateTime.class), new Object[][]{
                {new AtomicLong(-62167219200000L), zdt("0000-01-01T00:00:00Z"), true},
                {new AtomicLong(-62167219199999L), zdt("0000-01-01T00:00:00.001Z"), true},
                {new AtomicLong(-1), zdt("1969-12-31T23:59:59.999Z"), true},
                {new AtomicLong(0), zdt("1970-01-01T00:00:00Z"), true},
                {new AtomicLong(1), zdt("1970-01-01T00:00:00.001Z"), true},
        });
        TEST_DB.put(pair(BigInteger.class, ZonedDateTime.class), new Object[][]{
                {new BigInteger("-62167219200000000000"), zdt("0000-01-01T00:00:00Z"), true},
                {new BigInteger("-62167219199999999999"), zdt("0000-01-01T00:00:00.000000001Z"), true},
                {new BigInteger("-1"), zdt("1969-12-31T23:59:59.999999999Z"), true},
                {BigInteger.ZERO, zdt("1970-01-01T00:00:00Z"), true},
                {new BigInteger("1"), zdt("1970-01-01T00:00:00.000000001Z"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, ZonedDateTime.class), new Object[][]{
                {new BigDecimal("-62167219200"), zdt("0000-01-01T00:00:00Z"), true},
                {new BigDecimal("-0.000000001"), zdt("1969-12-31T23:59:59.999999999Z"), true},
                {BigDecimal.ZERO, zdt("1970-01-01T00:00:00Z"), true},
                {new BigDecimal("0.000000001"), zdt("1970-01-01T00:00:00.000000001Z"), true},
                {BigDecimal.valueOf(86400), zdt("1970-01-02T00:00:00Z"), true},
                {new BigDecimal("86400.000000001"), zdt("1970-01-02T00:00:00.000000001Z"), true},
        });
        TEST_DB.put(pair(Timestamp.class, ZonedDateTime.class), new Object[][]{
                {new Timestamp(-1), zdt("1969-12-31T23:59:59.999+00:00"), true},
                {new Timestamp(0), zdt("1970-01-01T00:00:00+00:00"), true},
                {new Timestamp(1), zdt("1970-01-01T00:00:00.001+00:00"), true},
        });
        TEST_DB.put(pair(Instant.class, ZonedDateTime.class), new Object[][]{
                {Instant.ofEpochSecond(-62167219200L), zdt("0000-01-01T00:00:00Z"), true},
                {Instant.ofEpochSecond(-62167219200L, 1), zdt("0000-01-01T00:00:00.000000001Z"), true},
                {Instant.ofEpochSecond(0, -1), zdt("1969-12-31T23:59:59.999999999Z"), true},
                {Instant.ofEpochSecond(0, 0), zdt("1970-01-01T00:00:00Z"), true},
                {Instant.ofEpochSecond(0, 1), zdt("1970-01-01T00:00:00.000000001Z"), true},
                {Instant.parse("2024-03-10T11:43:00Z"), zdt("2024-03-10T11:43:00Z"), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, ZonedDateTime.class), new Object[][]{
                {ldt("1970-01-01T08:59:59.999999999"), zdt("1969-12-31T23:59:59.999999999Z"), true},
                {ldt("1970-01-01T09:00:00"), zdt("1970-01-01T00:00:00Z"), true},
                {ldt("1970-01-01T09:00:00.000000001"), zdt("1970-01-01T00:00:00.000000001Z"), true},
                {ldt("1969-12-31T23:59:59.999999999"), zdt("1969-12-31T23:59:59.999999999+09:00"), true},
                {ldt("1970-01-01T00:00:00"), zdt("1970-01-01T00:00:00+09:00"), true},
                {ldt("1970-01-01T00:00:00.000000001"), zdt("1970-01-01T00:00:00.000000001+09:00"), true},

                // DST transitions (adjusted for Asia/Tokyo being +09:00)
                {ldt("2024-03-10T15:59:59"), zdt("2024-03-10T01:59:59-05:00"), true},  // DST transition
                {ldt("2024-11-03T14:00:00"), zdt("2024-11-03T01:00:00-04:00"), true},  // Fall back

                // Extreme dates (adjusted for Asia/Tokyo)
                {ldt("1888-01-01T09:00:00"), zdt("1888-01-01T00:00:00Z"), true},  // Earliest reliable date for Asia/Tokyo
                {ldt("9999-01-01T08:59:59.999999999"), zdt("9998-12-31T23:59:59.999999999Z"), true}  // Far future
        });
        TEST_DB.put(pair(Map.class, ZonedDateTime.class), new Object[][]{
                {mapOf(VALUE, new AtomicLong(now)), Instant.ofEpochMilli(now).atZone(TOKYO_Z)},
                {mapOf(EPOCH_MILLIS, now), Instant.ofEpochMilli(now).atZone(TOKYO_Z)},
                {mapOf(ZONED_DATE_TIME, "1969-12-31T23:59:59.999999999+09:00[Asia/Tokyo]"), zdt("1969-12-31T23:59:59.999999999+09:00"), true},
                {mapOf(ZONED_DATE_TIME, "1970-01-01T00:00:00+09:00[Asia/Tokyo]"), zdt("1970-01-01T00:00:00+09:00"), true},
                {mapOf(ZONED_DATE_TIME, "1970-01-01T00:00:00.000000001+09:00[Asia/Tokyo]"), zdt("1970-01-01T00:00:00.000000001+09:00"), true},
                {mapOf(ZONED_DATE_TIME, "2024-03-10T15:59:59+09:00[Asia/Tokyo]"), zdt("2024-03-10T01:59:59-05:00"), true},
                {mapOf(ZONED_DATE_TIME, "2024-11-03T14:00:00+09:00[Asia/Tokyo]"), zdt("2024-11-03T01:00:00-04:00"), true},
                {mapOf(ZONED_DATE_TIME, "1970-01-01T09:00:00+09:00[Asia/Tokyo]"), zdt("1970-01-01T00:00:00Z"), true},
                {mapOf(VALUE, "1970-01-01T09:00:00+09:00[Asia/Tokyo]"), zdt("1970-01-01T00:00:00Z")},
                {mapOf(V, "1970-01-01T09:00:00+09:00[Asia/Tokyo]"), zdt("1970-01-01T00:00:00Z")}
        });
    }

    /**
     * LocalDateTime
     */
    private static void loadLocalDateTimeTests() {
        TEST_DB.put(pair(Void.class, LocalDateTime.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(LocalDateTime.class, LocalDateTime.class), new Object[][]{
                {LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(1970, 1, 1, 0, 0), true}
        });
        TEST_DB.put(pair(AtomicLong.class, LocalDateTime.class), new Object[][]{
                {new AtomicLong(-1), zdt("1969-12-31T23:59:59.999Z").toLocalDateTime(), true},
                {new AtomicLong(0), zdt("1970-01-01T00:00:00Z").toLocalDateTime(), true},
                {new AtomicLong(1), zdt("1970-01-01T00:00:00.001Z").toLocalDateTime(), true},
        });
        TEST_DB.put(pair(Calendar.class, LocalDateTime.class), new Object[][] {
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.MARCH, 2, 22, 54, 17);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }, ldt("2024-03-02T22:54:17"), true},
        });
        TEST_DB.put(pair(java.sql.Date.class, LocalDateTime.class), new Object[][]{
                {java.sql.Date.valueOf("1970-01-01"),
                        LocalDateTime.of(1970, 1, 1, 0, 0), true},  // Simple case
                {java.sql.Date.valueOf("2024-02-06"),
                        LocalDateTime.of(2024, 2, 6, 0, 0), true},  // Current date
                {java.sql.Date.valueOf("0001-01-01"),
                        LocalDateTime.of(1, 1, 1, 0, 0), true},     // Very old date
        });
        TEST_DB.put(pair(Instant.class, LocalDateTime.class), new Object[][] {
                {Instant.parse("0000-01-01T00:00:00Z"), zdt("0000-01-01T00:00:00Z").toLocalDateTime(), true},
                {Instant.parse("0000-01-01T00:00:00.000000001Z"), zdt("0000-01-01T00:00:00.000000001Z").toLocalDateTime(), true},
                {Instant.parse("1969-12-31T23:59:59.999999999Z"), zdt("1969-12-31T23:59:59.999999999Z").toLocalDateTime(), true},
                {Instant.parse("1970-01-01T00:00:00Z"), zdt("1970-01-01T00:00:00Z").toLocalDateTime(), true},
                {Instant.parse("1970-01-01T00:00:00.000000001Z"), zdt("1970-01-01T00:00:00.000000001Z").toLocalDateTime(), true},
        });
        TEST_DB.put(pair(LocalDate.class, LocalDateTime.class), new Object[][] {
                {LocalDate.parse("0000-01-01"), ldt("0000-01-01T00:00:00"), true},
                {LocalDate.parse("1969-12-31"), ldt("1969-12-31T00:00:00"), true},
                {LocalDate.parse("1970-01-01"), ldt("1970-01-01T00:00:00"), true},
                {LocalDate.parse("1970-01-02"), ldt("1970-01-02T00:00:00"), true},
        });
        TEST_DB.put(pair(String.class, LocalDateTime.class), new Object[][]{
                {"", null},
                {"1965-12-31T16:20:00", ldt("1965-12-31T16:20:00"), true},
        });
        TEST_DB.put(pair(Map.class, LocalDateTime.class), new Object[][] {
                { mapOf(LOCAL_DATE_TIME, "1969-12-31T23:59:59.999999999"), ldt("1969-12-31T23:59:59.999999999"), true},
                { mapOf(LOCAL_DATE_TIME, "1970-01-01T00:00"), ldt("1970-01-01T00:00"), true},
                { mapOf(LOCAL_DATE_TIME, "1970-01-01"), ldt("1970-01-01T00:00")},
                { mapOf(LOCAL_DATE_TIME, "1970-01-01T00:00:00.000000001"), ldt("1970-01-01T00:00:00.000000001"), true},
                { mapOf(LOCAL_DATE_TIME, "2024-03-10T11:07:00.123456789"), ldt("2024-03-10T11:07:00.123456789"), true},
                { mapOf(VALUE, "2024-03-10T11:07:00.123456789"), ldt("2024-03-10T11:07:00.123456789")},
        });
    }

    /**
     * LocalTime
     */
    private static void loadLocalTimeTests() {
        TEST_DB.put(pair(Void.class, LocalTime.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(LocalTime.class, LocalTime.class), new Object[][]{
                { LocalTime.parse("12:34:56"), LocalTime.parse("12:34:56"), true}
        });
        TEST_DB.put(pair(Integer.class, LocalTime.class), new Object[][]{
                { -1, new IllegalArgumentException("value [-1]")},
                { 0, LocalTime.parse("00:00:00"), true},
                { 1, LocalTime.parse("00:00:00.001"), true},
                { 86399999, LocalTime.parse("23:59:59.999"), true},
                { 86400000, new IllegalArgumentException("value [86400000]")},
        });
        TEST_DB.put(pair(Long.class, LocalTime.class), new Object[][]{
                { -1L, new IllegalArgumentException("value [-1]")},
                { 0L, LocalTime.parse("00:00:00"), true},
                { 1L, LocalTime.parse("00:00:00.001"), true},
                { 86399999L, LocalTime.parse("23:59:59.999"), true},
                { 86400000L, new IllegalArgumentException("value [86400000]")},
        });
        TEST_DB.put(pair(Double.class, LocalTime.class), new Object[][]{
                { -0.000000001, new IllegalArgumentException("value [-1.0E-9]")},
                { 0.0, LocalTime.parse("00:00:00"), true},
                { 0.000000001, LocalTime.parse("00:00:00.000000001"), true},
                { 1.0, LocalTime.parse("00:00:01"), true},
                { 86399.999999999, LocalTime.parse("23:59:59.999999999"), true},
                { 86400.0, new IllegalArgumentException("value [86400.0]")},
        });
        TEST_DB.put(pair(AtomicInteger.class, LocalTime.class), new Object[][]{
                { new AtomicInteger(-1), new IllegalArgumentException("value [-1]")},
                { new AtomicInteger(0), LocalTime.parse("00:00:00"), true},
                { new AtomicInteger(1), LocalTime.parse("00:00:00.001"), true},
                { new AtomicInteger(86399999), LocalTime.parse("23:59:59.999"), true},
                { new AtomicInteger(86400000), new IllegalArgumentException("value [86400000]")},
        });
        TEST_DB.put(pair(AtomicLong.class, LocalTime.class), new Object[][]{
                { new AtomicLong(-1), new IllegalArgumentException("Input value [-1] for conversion to LocalTime must be >= 0 && <= 86399999")},
                { new AtomicLong(0), LocalTime.parse("00:00:00"), true},
                { new AtomicLong(1), LocalTime.parse("00:00:00.001"), true},
                { new AtomicLong(86399999), LocalTime.parse("23:59:59.999"), true},
                { new AtomicLong(86400000), new IllegalArgumentException("value [86400000]")},
        });
        TEST_DB.put(pair(BigInteger.class, LocalTime.class), new Object[][]{
                { BigInteger.valueOf(-1), new IllegalArgumentException("value [-1]")},
                { BigInteger.valueOf(0), LocalTime.parse("00:00:00"), true},
                { BigInteger.valueOf(1), LocalTime.parse("00:00:00.000000001"), true},
                { BigInteger.valueOf(86399999999999L), LocalTime.parse("23:59:59.999999999"), true},
                { BigInteger.valueOf(86400000000000L), new IllegalArgumentException("value [86400000000000]")},
        });
        TEST_DB.put(pair(BigDecimal.class, LocalTime.class), new Object[][]{
                { BigDecimal.valueOf(-0.000000001), new IllegalArgumentException("value [-0.0000000010]")},
                { BigDecimal.valueOf(0), LocalTime.parse("00:00:00"), true},
                { BigDecimal.valueOf(0.000000001), LocalTime.parse("00:00:00.000000001"), true},
                { BigDecimal.valueOf(1), LocalTime.parse("00:00:01"), true},
                { BigDecimal.valueOf(86399.999999999), LocalTime.parse("23:59:59.999999999"), true},
                { BigDecimal.valueOf(86400.0), new IllegalArgumentException("value [86400.0]")},
        });
        TEST_DB.put(pair(Calendar.class, LocalTime.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);

                    // Set the calendar instance to have the same time as the LocalTime passed in
                    cal.set(Calendar.HOUR_OF_DAY, 22);
                    cal.set(Calendar.MINUTE, 47);
                    cal.set(Calendar.SECOND, 55);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }, LocalTime.of(22, 47, 55), true }
        });
        TEST_DB.put(pair(Date.class, LocalTime.class), new Object[][]{
                { new Date(-1L), LocalTime.parse("08:59:59.999")},
                { new Date(0L), LocalTime.parse("09:00:00")},
                { new Date(1L), LocalTime.parse("09:00:00.001")},
                { new Date(1001L), LocalTime.parse("09:00:01.001")},
                { new Date(86399999L), LocalTime.parse("08:59:59.999")},
                { new Date(86400000L), LocalTime.parse("09:00:00")},
        });
        TEST_DB.put(pair(Timestamp.class, LocalTime.class), new Object[][]{
                { new Timestamp(-1), LocalTime.parse("08:59:59.999")},
        });
        TEST_DB.put(pair(LocalDateTime.class, LocalTime.class), new Object[][]{   // no reverse option (Time local to Tokyo)
                { ldt("0000-01-01T00:00:00"), LocalTime.parse("00:00:00")},
                { ldt("0000-01-02T00:00:00"), LocalTime.parse("00:00:00")},
                { ldt("1969-12-31T23:59:59.999999999"), LocalTime.parse("23:59:59.999999999")},
                { ldt("1970-01-01T00:00:00"), LocalTime.parse("00:00:00")},
                { ldt("1970-01-01T00:00:00.000000001"), LocalTime.parse("00:00:00.000000001")},
        });
        TEST_DB.put(pair(Instant.class, LocalTime.class), new Object[][]{   // no reverse option (Time local to Tokyo)
                { Instant.parse("1969-12-31T23:59:59.999999999Z"), LocalTime.parse("08:59:59.999999999")},
                { Instant.parse("1970-01-01T00:00:00Z"), LocalTime.parse("09:00:00")},
                { Instant.parse("1970-01-01T00:00:00.000000001Z"), LocalTime.parse("09:00:00.000000001")},
        });
        TEST_DB.put(pair(OffsetDateTime.class, LocalTime.class), new Object[][]{
                {odt("1969-12-31T23:59:59.999999999Z"), LocalTime.parse("08:59:59.999999999")},
                {odt("1970-01-01T00:00Z"), LocalTime.parse("09:00")},
                {odt("1970-01-01T00:00:00.000000001Z"), LocalTime.parse("09:00:00.000000001")},
        });
        TEST_DB.put(pair(ZonedDateTime.class, LocalTime.class), new Object[][]{
                {zdt("1969-12-31T23:59:59.999999999Z"), LocalTime.parse("08:59:59.999999999")},
                {zdt("1970-01-01T00:00Z"), LocalTime.parse("09:00")},
                {zdt("1970-01-01T00:00:00.000000001Z"), LocalTime.parse("09:00:00.000000001")},
        });
        TEST_DB.put(pair(String.class, LocalTime.class), new Object[][]{
                {"", null},
                {"2024-03-23T03:35", LocalTime.parse("03:35")},
                {"16:20:00", LocalTime.parse("16:20:00"), true},
                {"09:26:00", LocalTime.of(9, 26), true},
                {"09:26:17", LocalTime.of(9, 26, 17), true},
                {"09:26:17.000000001", LocalTime.of(9, 26, 17, 1), true},
        });
        TEST_DB.put(pair(Map.class, LocalTime.class), new Object[][] {
                {mapOf(LOCAL_TIME, "00:00"), LocalTime.parse("00:00:00.000000000"), true},
                {mapOf(LOCAL_TIME, "00:00:00.000000001"), LocalTime.parse("00:00:00.000000001"), true},
                {mapOf(LOCAL_TIME, "00:00"), LocalTime.parse("00:00:00"), true},
                {mapOf(LOCAL_TIME, "23:59:59.999999999"), LocalTime.parse("23:59:59.999999999"), true},
                {mapOf(LOCAL_TIME, "23:59"), LocalTime.parse("23:59") , true},
                {mapOf(LOCAL_TIME, "23:59:59"), LocalTime.parse("23:59:59"), true },
                {mapOf(VALUE, "23:59:59.999999999"), LocalTime.parse("23:59:59.999999999") },
        });
    }

    /**
     * LocalDate
     */
    private static void loadLocalDateTests() {
        TEST_DB.put(pair(Void.class, LocalDate.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(LocalDate.class, LocalDate.class), new Object[][]{
                {LocalDate.parse("1970-01-01"), LocalDate.parse("1970-01-01"), true}
        });
        TEST_DB.put(pair(Double.class, LocalDate.class), new Object[][]{   // options timezone is factored in (86,400 seconds per day)
                {-62167252739.0, LocalDate.parse("0000-01-01"), true},
                {-118800d, LocalDate.parse("1969-12-31"), true},
                {-32400d, LocalDate.parse("1970-01-01"), true},
                {0.0, LocalDate.parse("1970-01-01")},         // Showing that there is a wide range of numbers that will convert to this date
                {53999.999, LocalDate.parse("1970-01-01")}, // Showing that there is a wide range of numbers that will convert to this date
                {54000d, LocalDate.parse("1970-01-02"), true},
        });
        TEST_DB.put(pair(AtomicLong.class, LocalDate.class), new Object[][]{   // options timezone is factored in (86,400 seconds per day)
                {new AtomicLong(-118800000), LocalDate.parse("1969-12-31"), true},
                {new AtomicLong(-32400000), LocalDate.parse("1970-01-01"), true},
                {new AtomicLong(0), LocalDate.parse("1970-01-01")},         // Showing that there is a wide range of numbers that will convert to this date
                {new AtomicLong(53999999), LocalDate.parse("1970-01-01")}, // Showing that there is a wide range of numbers that will convert to this date
                {new AtomicLong(54000000), LocalDate.parse("1970-01-02"), true},
        });
        TEST_DB.put(pair(BigInteger.class, LocalDate.class), new Object[][]{   // options timezone is factored in (86,400 seconds per day)
                {new BigInteger("-62167252739000000000"), LocalDate.parse("0000-01-01")},
                {new BigInteger("-62167219200000000000"), LocalDate.parse("0000-01-01")},
                {new BigInteger("-62167219200000000000"), zdt("0000-01-01T00:00:00Z").toLocalDate()},
                {new BigInteger("-118800000000000"), LocalDate.parse("1969-12-31"), true},
                {new BigInteger("-32400000000000"), LocalDate.parse("1970-01-01"), true},
                {BigInteger.ZERO, zdt("1970-01-01T00:00:00Z").toLocalDate()},
                {new BigInteger("53999999000000"), LocalDate.parse("1970-01-01")},
                {new BigInteger("54000000000000"), LocalDate.parse("1970-01-02"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, LocalDate.class), new Object[][]{   // options timezone is factored in (86,400 seconds per day)
                {new BigDecimal("-62167252739"), LocalDate.parse("0000-01-01")},
                {new BigDecimal("-62167219200"), LocalDate.parse("0000-01-01")},
                {new BigDecimal("-62167219200"), zdt("0000-01-01T00:00:00Z").toLocalDate()},
                {new BigDecimal("-118800"), LocalDate.parse("1969-12-31"), true},
                // These 4 are all in the same date range
                {new BigDecimal("-32400"), LocalDate.parse("1970-01-01"), true},
                {BigDecimal.ZERO, zdt("1970-01-01T00:00:00Z").toLocalDate()},
                {new BigDecimal("53999.999"), LocalDate.parse("1970-01-01")},
                {new BigDecimal("54000"), LocalDate.parse("1970-01-02"), true},
        });
        TEST_DB.put(pair(Calendar.class, LocalDate.class), new Object[][] {
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.clear();
                    cal.set(2024, Calendar.MARCH, 2);
                    return cal;
                }, LocalDate.parse("2024-03-02"), true }
        });
        TEST_DB.put(pair(ZonedDateTime.class, LocalDate.class), new Object[][] {
                {ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameLocal(TOKYO_Z), LocalDate.parse("0000-01-01"), true },
                {ZonedDateTime.parse("0000-01-02T00:00:00Z").withZoneSameLocal(TOKYO_Z), LocalDate.parse("0000-01-02"), true },
                {ZonedDateTime.parse("1969-12-31T00:00:00Z").withZoneSameLocal(TOKYO_Z), LocalDate.parse("1969-12-31"), true },
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameLocal(TOKYO_Z), LocalDate.parse("1970-01-01"), true },
                {ZonedDateTime.parse("1970-01-02T00:00:00Z").withZoneSameLocal(TOKYO_Z), LocalDate.parse("1970-01-02"), true },
        });
        TEST_DB.put(pair(OffsetDateTime.class, LocalDate.class), new Object[][] {
                {OffsetDateTime.parse("0000-01-01T00:00:00+09:00"), LocalDate.parse("0000-01-01"), true },
                {OffsetDateTime.parse("0000-01-02T00:00:00+09:00"), LocalDate.parse("0000-01-02"), true },
                {OffsetDateTime.parse("1969-12-31T00:00:00+09:00"), LocalDate.parse("1969-12-31"), true },
                {OffsetDateTime.parse("1970-01-01T00:00:00+09:00"), LocalDate.parse("1970-01-01"), true },
                {OffsetDateTime.parse("1970-01-02T00:00:00+09:00"), LocalDate.parse("1970-01-02"), true },
        });
        TEST_DB.put(pair(String.class, LocalDate.class), new Object[][]{
                { "", null}, 
                {"1969-12-31", LocalDate.parse("1969-12-31"), true},
                {"1970-01-01", LocalDate.parse("1970-01-01"), true},
                {"2024-03-20", LocalDate.parse("2024-03-20"), true},
        });
        TEST_DB.put(pair(Map.class, LocalDate.class), new Object[][] {
                {mapOf(LOCAL_DATE, "1969-12-31"), LocalDate.parse("1969-12-31"), true},
                {mapOf(LOCAL_DATE, "1970-01-01"), LocalDate.parse("1970-01-01"), true},
                {mapOf(LOCAL_DATE, "1970-01-02"), LocalDate.parse("1970-01-02"), true},
                {mapOf(VALUE, "2024-03-18"), LocalDate.parse("2024-03-18")},
                {mapOf(V, "2024/03/18"), LocalDate.parse("2024-03-18")},
        });
    }

    /**
     * Timestamp
     */
    private static void loadTimestampTests() {
        TEST_DB.put(pair(Void.class, Timestamp.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Timestamp.class, Timestamp.class), new Object[][]{
                {timestamp("1970-01-01T00:00:00Z"), timestamp("1970-01-01T00:00:00Z")},
        });
        TEST_DB.put(pair(String.class, Timestamp.class), new Object[][]{
                {"0000-01-01T00:00:00Z", new IllegalArgumentException("Cannot convert to Timestamp")},
        });
        TEST_DB.put(pair(AtomicLong.class, Timestamp.class), new Object[][]{
                {new AtomicLong(-62135596800000L), timestamp("0001-01-01T00:00:00.000Z"), true},
                {new AtomicLong(-62131377719000L), timestamp("0001-02-18T19:58:01.000Z"), true},
                {new AtomicLong(-1000), timestamp("1969-12-31T23:59:59.000000000Z"), true},
                {new AtomicLong(-999), timestamp("1969-12-31T23:59:59.001Z"), true},
                {new AtomicLong(-900), timestamp("1969-12-31T23:59:59.100000000Z"), true},
                {new AtomicLong(-100), timestamp("1969-12-31T23:59:59.900000000Z"), true},
                {new AtomicLong(-1), timestamp("1969-12-31T23:59:59.999Z"), true},
                {new AtomicLong(0), timestamp("1970-01-01T00:00:00.000000000Z"), true},
                {new AtomicLong(1), timestamp("1970-01-01T00:00:00.001Z"), true},
                {new AtomicLong(100), timestamp("1970-01-01T00:00:00.100Z"), true},
                {new AtomicLong(900), timestamp("1970-01-01T00:00:00.900Z"), true},
                {new AtomicLong(999), timestamp("1970-01-01T00:00:00.999Z"), true},
                {new AtomicLong(1000), timestamp("1970-01-01T00:00:01.000Z"), true},
                {new AtomicLong(253374983881000L), timestamp("9999-02-18T19:58:01.000Z"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Timestamp.class), new Object[][]{
                {new BigDecimal("-62135596800"), timestamp("0001-01-01T00:00:00Z"), true},
                {new BigDecimal("-62135596799.999999999"), timestamp("0001-01-01T00:00:00.000000001Z"), true},
                {new BigDecimal("-1.000000001"), timestamp("1969-12-31T23:59:58.999999999Z"), true},
                {new BigDecimal("-1"), timestamp("1969-12-31T23:59:59Z"), true},
                {new BigDecimal("-0.00000001"), timestamp("1969-12-31T23:59:59.99999999Z"), true},
                {new BigDecimal("-0.000000001"), timestamp("1969-12-31T23:59:59.999999999Z"), true},
                {BigDecimal.ZERO, timestamp("1970-01-01T00:00:00.000000000Z"), true},
                {new BigDecimal("0.000000001"), timestamp("1970-01-01T00:00:00.000000001Z"), true},
                {new BigDecimal(".999999999"), timestamp("1970-01-01T00:00:00.999999999Z"), true},
                {new BigDecimal("1"), timestamp("1970-01-01T00:00:01Z"), true},
        });
        TEST_DB.put(pair(Calendar.class, Timestamp.class), new Object[][] {
                {cal(now), new Timestamp(now), true},
        });
        TEST_DB.put(pair(LocalDate.class, Timestamp.class), new Object[][] {
                {LocalDate.parse("0001-01-01"), timestamp("0001-01-01T00:00:00Z"), true },
                {LocalDate.parse("0001-01-02"), timestamp("0001-01-02T00:00:00Z"), true },
                {LocalDate.parse("1969-12-31"), timestamp("1969-12-31T00:00:00Z"), true },
                {LocalDate.parse("1970-01-01"), timestamp("1970-01-01T00:00:00Z"), true },
                {LocalDate.parse("1970-01-02"), timestamp("1970-01-02T00:00:00Z"), true },
        });
        TEST_DB.put(pair(LocalDateTime.class, Timestamp.class), new Object[][]{
                {zdt("0001-01-01T00:00:00Z").toLocalDateTime(), new Timestamp(-62135596800000L), true},
                {zdt("0001-01-01T00:00:00.001Z").toLocalDateTime(), new Timestamp(-62135596799999L), true},
                {zdt("0001-01-01T00:00:00.000000001Z").toLocalDateTime(), (Supplier<Timestamp>) () -> {
                    Timestamp ts = new Timestamp(-62135596800000L);
                    ts.setNanos(1);
                    return ts;
                }, true},
                {zdt("1969-12-31T23:59:59Z").toLocalDateTime(), new Timestamp(-1000L), true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDateTime(), new Timestamp(-1L), true},
                {zdt("1969-12-31T23:59:59.999999999Z").toLocalDateTime(), (Supplier<Timestamp>) () -> {
                    Timestamp ts = new Timestamp(-1L);
                    ts.setNanos(999999999);
                    return ts;
                }, true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), new Timestamp(0L), true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDateTime(), new Timestamp(1L), true},
                {zdt("1970-01-01T00:00:00.000000001Z").toLocalDateTime(), (Supplier<Timestamp>) () -> {
                    Timestamp ts = new Timestamp(0L);
                    ts.setNanos(1);
                    return ts;
                }, true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDateTime(), new Timestamp(999L), true},
        });
        TEST_DB.put(pair(Duration.class, Timestamp.class), new Object[][]{
                {Duration.ofSeconds(-62135596800L), timestamp("0001-01-01T00:00:00Z"), true},
                {Duration.ofSeconds(-62135596800L, 1), timestamp("0001-01-01T00:00:00.000000001Z"), true},
                {Duration.ofNanos(-1000000001), timestamp("1969-12-31T23:59:58.999999999Z"), true},
                {Duration.ofNanos(-1000000000), timestamp("1969-12-31T23:59:59.000000000Z"), true},
                {Duration.ofNanos(-999999999), timestamp("1969-12-31T23:59:59.000000001Z"), true},
                {Duration.ofNanos(-1), timestamp("1969-12-31T23:59:59.999999999Z"), true},
                {Duration.ofNanos(0), timestamp("1970-01-01T00:00:00.000000000Z"), true},
                {Duration.ofNanos(1), timestamp("1970-01-01T00:00:00.000000001Z"), true},
                {Duration.ofNanos(999999999), timestamp("1970-01-01T00:00:00.999999999Z"), true},
                {Duration.ofNanos(1000000000), timestamp("1970-01-01T00:00:01.000000000Z"), true},
                {Duration.ofNanos(1000000001), timestamp("1970-01-01T00:00:01.000000001Z"), true},
                {Duration.ofNanos(686629800000000001L), timestamp("1991-10-05T02:30:00.000000001Z"), true},
                {Duration.ofNanos(1199145600000000001L), timestamp("2008-01-01T00:00:00.000000001Z"), true},
                {Duration.ofNanos(1708255140987654321L), timestamp("2024-02-18T11:19:00.987654321Z"), true},
                {Duration.ofNanos(2682374400000000001L), timestamp("2055-01-01T00:00:00.000000001Z"), true},
        });
        TEST_DB.put(pair(Instant.class, Timestamp.class), new Object[][]{
                {Instant.ofEpochSecond(-62135596800L), timestamp("0001-01-01T00:00:00Z"), true},
                {Instant.ofEpochSecond(-62135596800L, 1), timestamp("0001-01-01T00:00:00.000000001Z"), true},
                {Instant.ofEpochSecond(0, -1), timestamp("1969-12-31T23:59:59.999999999Z"), true},
                {Instant.ofEpochSecond(0, 0), timestamp("1970-01-01T00:00:00.000000000Z"), true},
                {Instant.ofEpochSecond(0, 1), timestamp("1970-01-01T00:00:00.000000001Z"), true},
                {Instant.parse("2024-03-10T11:36:00Z"), timestamp("2024-03-10T11:36:00Z"), true},
                {Instant.parse("2024-03-10T11:36:00.123456789Z"), timestamp("2024-03-10T11:36:00.123456789Z"), true},
        });
        // No symmetry checks - because an OffsetDateTime of "2024-02-18T06:31:55.987654321+00:00" and "2024-02-18T15:31:55.987654321+09:00" are equivalent but not equals. They both describe the same Instant.
        TEST_DB.put(pair(Map.class, Timestamp.class), new Object[][] {
                { mapOf(EPOCH_MILLIS, -1L), timestamp("1969-12-31T23:59:59.999Z") },
                { mapOf(EPOCH_MILLIS, 0L), timestamp("1970-01-01T00:00:00Z") },
                { mapOf(EPOCH_MILLIS, 1L), timestamp("1970-01-01T00:00:00.001Z") },
                { mapOf(EPOCH_MILLIS, -1L), new Timestamp(-1L)},
                { mapOf(EPOCH_MILLIS, 0L), new Timestamp(0L)},
                { mapOf(EPOCH_MILLIS, 1L), new Timestamp(1L)},
                { mapOf(EPOCH_MILLIS, 1710714535152L), new Timestamp(1710714535152L)},
                { mapOf(TIMESTAMP, "1969-12-31T23:59:59.987654321Z"), timestamp("1969-12-31T23:59:59.987654321Z"), true },
                { mapOf(TIMESTAMP, "1970-01-01T00:00:00.000000001Z"), timestamp("1970-01-01T00:00:00.000000001Z"), true},
                { mapOf(TIMESTAMP, "2024-03-17T22:28:55.152000001Z"), (Supplier<Timestamp>) () -> {
                    Timestamp ts = new Timestamp(1710714535152L);
                    ts.setNanos(152000001);
                    return ts;
                }, true},
                { mapOf("bad key", "2024-03-18T07:28:55.152", ZONE, TOKYO_Z.toString()), new IllegalArgumentException("Map to 'Timestamp' the map must include: [timestamp], [value], [_v], or [epochMillis] as key with associated value")},
        });
    }

    /**
     * ZoneId
     */
    private static void loadZoneIdTests() {
        ZoneId NY_Z = ZoneId.of("America/New_York");
        ZoneId TOKYO_Z = ZoneId.of("Asia/Tokyo");

        TEST_DB.put(pair(Void.class, ZoneId.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(ZoneId.class, ZoneId.class), new Object[][]{
                {NY_Z, NY_Z},
                {TOKYO_Z, TOKYO_Z},
        });
        TEST_DB.put(pair(ZoneOffset.class, ZoneId.class), new Object[][]{
                {ZoneOffset.of("+09:00"), ZoneId.of("+09:00")},
                {ZoneOffset.of("-05:00"), ZoneId.of("-05:00")},
        });
        TEST_DB.put(pair(TimeZone.class, ZoneId.class), new Object[][]{
                {TimeZone.getTimeZone("America/New_York"), ZoneId.of("America/New_York"),true},
                {TimeZone.getTimeZone("Asia/Tokyo"), ZoneId.of("Asia/Tokyo"),true},
                {TimeZone.getTimeZone("GMT"), ZoneId.of("GMT"), true},
                {TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"), true},
        });
        TEST_DB.put(pair(String.class, ZoneId.class), new Object[][]{
                {"", null},
                {"America/New_York", NY_Z, true},
                {"Asia/Tokyo", TOKYO_Z, true},
                {"America/Cincinnati", new IllegalArgumentException("Unknown time-zone ID: 'America/Cincinnati'")},
                {"Z", ZoneId.of("Z"), true},
                {"UTC", ZoneId.of("UTC"), true},
                {"GMT", ZoneId.of("GMT"), true},
                {"EST", SystemUtilities.currentMajor() >= 24 ? ZoneId.of("America/Panama") : ZoneOffset.of("-05:00")},
        });
        TEST_DB.put(pair(Map.class, ZoneId.class), new Object[][]{
                {mapOf("_v", "America/New_York"), NY_Z},
                {mapOf("_v", NY_Z), NY_Z},
                {mapOf(ZONE, "America/New_York"), NY_Z, true},
                {mapOf(ZONE, NY_Z), NY_Z},
                {mapOf(ID, NY_Z), NY_Z},
                {mapOf("_v", "Asia/Tokyo"), TOKYO_Z},
                {mapOf("_v", TOKYO_Z), TOKYO_Z},
                {mapOf(ZONE, mapOf("_v", TOKYO_Z)), TOKYO_Z},
        });
    }

    /**
     * Year
     */
    private static void loadYearTests() {
        TEST_DB.put(pair(Void.class, Year.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Year.class, Year.class), new Object[][]{
                {Year.of(1970), Year.of(1970), true},
        });
        TEST_DB.put(pair(Calendar.class, Year.class), new Object[][] {
                {createCalendar(1888, 1, 2, 0, 0, 0), Year.of(1888), false},
                {createCalendar(1969, 12, 31, 0, 0, 0), Year.of(1969), false},
                {createCalendar(1970, 1, 1, 0, 0, 0), Year.of(1970), false},
                {createCalendar(2023, 6, 15, 0, 0, 0), Year.of(2023), false},
                {createCalendar(2023, 6, 15, 12, 30, 45), Year.of(2023), false},
                {createCalendar(2023, 12, 31, 23, 59, 59), Year.of(2023), false},
                {createCalendar(2023, 1, 1, 1, 0, 1), Year.of(2023), false}
        });
        TEST_DB.put(pair(Date.class, Year.class), new Object[][] {
                {date("1888-01-01T15:00:00Z"), Year.of(1888), false},    // 1888-01-02 00:00 Tokyo
                {date("1969-12-30T15:00:00Z"), Year.of(1969), false},    // 1969-12-31 00:00 Tokyo
                {date("1969-12-31T15:00:00Z"), Year.of(1970), false},    // 1970-01-01 00:00 Tokyo
                {date("2023-06-14T15:00:00Z"), Year.of(2023), false},    // 2023-06-15 00:00 Tokyo
                {date("2023-06-15T12:30:45Z"), Year.of(2023), false},   // 2023-06-15 21:30:45 Tokyo
                {date("2023-06-15T14:59:59Z"), Year.of(2023), false},   // 2023-06-15 23:59:59 Tokyo
                {date("2023-06-15T00:00:01Z"), Year.of(2023), false}    // 2023-06-15 09:00:01 Tokyo
        });
        TEST_DB.put(pair(java.sql.Date.class, Year.class), new Object[][] {
                {java.sql.Date.valueOf("1888-01-02"), Year.of(1888), false},
                {java.sql.Date.valueOf("1969-12-31"), Year.of(1969), false},
                {java.sql.Date.valueOf("1970-01-01"), Year.of(1970), false},
                {java.sql.Date.valueOf("2023-06-15"), Year.of(2023), false},
                {java.sql.Date.valueOf("2023-01-01"), Year.of(2023), false},
                {java.sql.Date.valueOf("2023-12-31"), Year.of(2023), false}
        });
        TEST_DB.put(pair(LocalDate.class, Year.class), new Object[][] {
                {LocalDate.of(1888, 1, 2), Year.of(1888), false},
                {LocalDate.of(1969, 12, 31), Year.of(1969), false},
                {LocalDate.of(1970, 1, 1), Year.of(1970), false},
                {LocalDate.of(2023, 6, 15), Year.of(2023), false},
                {LocalDate.of(2023, 1, 1), Year.of(2023), false},
                {LocalDate.of(2023, 12, 31), Year.of(2023), false}
        });
        TEST_DB.put(pair(LocalDateTime.class, Year.class), new Object[][] {
                {LocalDateTime.of(1888, 1, 2, 0, 0), Year.of(1888), false},
                {LocalDateTime.of(1969, 12, 31, 0, 0), Year.of(1969), false},
                {LocalDateTime.of(1970, 1, 1, 0, 0), Year.of(1970), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0), Year.of(2023), false},

                // One-way tests (false) - various times on same date
                {LocalDateTime.of(2023, 6, 15, 12, 30, 45), Year.of(2023), false},
                {LocalDateTime.of(2023, 6, 15, 23, 59, 59, 999_999_999), Year.of(2023), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0, 0, 1), Year.of(2023), false},

                // One-way tests (false) - different dates in same year
                {LocalDateTime.of(2023, 1, 1, 12, 0), Year.of(2023), false},
                {LocalDateTime.of(2023, 12, 31, 12, 0), Year.of(2023), false}
        });
        TEST_DB.put(pair(OffsetDateTime.class, Year.class), new Object[][] {
                {odt("1888-01-01T15:00:00Z"), Year.of(1888), false},    // 1888-01-02 00:00 Tokyo
                {odt("1969-12-30T15:00:00Z"), Year.of(1969), false},    // 1969-12-31 00:00 Tokyo
                {odt("1969-12-31T15:00:00Z"), Year.of(1970), false},    // 1970-01-01 00:00 Tokyo
                {odt("2023-06-14T15:00:00Z"), Year.of(2023), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {odt("2023-06-15T12:30:45Z"), Year.of(2023), false},       // 21:30:45 Tokyo
                {odt("2023-06-15T14:59:59.999Z"), Year.of(2023), false},   // 23:59:59.999 Tokyo
                {odt("2023-06-15T00:00:01Z"), Year.of(2023), false},       // 09:00:01 Tokyo

                // One-way tests (false) - same date in different offset
                {odt("2023-06-15T00:00:00+09:00"), Year.of(2023), false},  // Tokyo local time
                {odt("2023-06-15T00:00:00-05:00"), Year.of(2023), false}   // US Eastern time
        });
        TEST_DB.put(pair(ZonedDateTime.class, Year.class), new Object[][] {
                {zdt("1888-01-01T15:00:00Z"), Year.of(1888), false},    // 1888-01-02 00:00 Tokyo
                {zdt("1969-12-30T15:00:00Z"), Year.of(1969), false},    // 1969-12-31 00:00 Tokyo
                {zdt("1969-12-31T15:00:00Z"), Year.of(1970), false},    // 1970-01-01 00:00 Tokyo
                {zdt("2023-06-14T15:00:00Z"), Year.of(2023), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {zdt("2023-06-15T12:30:45Z"), Year.of(2023), false},       // 21:30:45 Tokyo
                {zdt("2023-06-15T14:59:59.999Z"), Year.of(2023), false},   // 23:59:59.999 Tokyo
                {zdt("2023-06-15T00:00:01Z"), Year.of(2023), false},       // 09:00:01 Tokyo

                // One-way tests (false) - same time in different zones
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo")), Year.of(2023), false},
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("America/New_York")), Year.of(2023), false}
        });
        TEST_DB.put(pair(Timestamp.class, Year.class), new Object[][] {
                // Bidirectional tests (true) - all at midnight Tokyo (+09:00)
                {timestamp("1888-01-01T15:00:00Z"), Year.of(1888), false},    // 1888-01-02 00:00 Tokyo
                {timestamp("1969-12-30T15:00:00Z"), Year.of(1969), false},    // 1969-12-31 00:00 Tokyo
                {timestamp("1969-12-31T15:00:00Z"), Year.of(1970), false},    // 1970-01-01 00:00 Tokyo
                {timestamp("2023-06-14T15:00:00Z"), Year.of(2023), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {timestamp("2023-06-15T12:30:45.123Z"), Year.of(2023), false},     // 21:30:45 Tokyo
                {timestamp("2023-06-15T14:59:59.999Z"), Year.of(2023), false},     // 23:59:59.999 Tokyo
                {timestamp("2023-06-15T00:00:00.001Z"), Year.of(2023), false},     // 09:00:00.001 Tokyo

                // One-way tests (false) - with nanosecond precision
                {timestamp("2023-06-15T12:00:00.123456789Z"), Year.of(2023), false}  // 21:00:00.123456789 Tokyo
        });
        TEST_DB.put(pair(String.class, Year.class), new Object[][]{
                {"", null},
                {"2024-03-23T04:10", Year.of(2024)},
                {"1970", Year.of(1970), true},
                {"1999", Year.of(1999), true},
                {"2000", Year.of(2000), true},
                {"2024", Year.of(2024), true},
                {"1670", Year.of(1670), true},
                {"1582", Year.of(1582), true},
                {"500", Year.of(500), true},
                {"1", Year.of(1), true},
                {"0", Year.of(0), true},
                {"-1", Year.of(-1), true},
                {"PONY", new IllegalArgumentException("Unable to parse 4-digit year from 'PONY'")},
        });
        TEST_DB.put(pair(Map.class, Year.class), new Object[][]{
                {mapOf("_v", "1984"), Year.of(1984)},
                {mapOf("value", 1984L), Year.of(1984)},
                {mapOf("year", 1492), Year.of(1492), true},
                {mapOf("year", mapOf("_v", (short) 2024)), Year.of(2024)}, // recursion
        });
        TEST_DB.put(pair(Byte.class, Year.class), new Object[][]{
                {(byte) 101, new IllegalArgumentException("Unsupported conversion, source type [Byte (101)] target type 'Year'")},
        });
        TEST_DB.put(pair(Short.class, Year.class), new Object[][]{
                {(short) 2024, Year.of(2024)},
        });
        TEST_DB.put(pair(Float.class, Year.class), new Object[][]{
                {2024f, Year.of(2024)},
        });
        TEST_DB.put(pair(Double.class, Year.class), new Object[][]{
                {2024.0, Year.of(2024)},
        });
        TEST_DB.put(pair(BigInteger.class, Year.class), new Object[][]{
                {BigInteger.valueOf(2024), Year.of(2024), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Year.class), new Object[][]{
                {BigDecimal.valueOf(2024), Year.of(2024), true},
        });
        TEST_DB.put(pair(AtomicInteger.class, Year.class), new Object[][]{
                {new AtomicInteger(2024), Year.of(2024), true},
        });
        TEST_DB.put(pair(AtomicLong.class, Year.class), new Object[][]{
                {new AtomicLong(2024), Year.of(2024), true},
                {new AtomicLong(-1), Year.of(-1), true},
        });
    }

    /**
     * Period
     */
    private static void loadPeriodTests() {
        TEST_DB.put(pair(Void.class, Period.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Period.class, Period.class), new Object[][]{
                {Period.of(0, 0, 0), Period.of(0, 0, 0)},
                {Period.of(1, 1, 1), Period.of(1, 1, 1)},
        });
        TEST_DB.put(pair(String.class, Period.class), new Object[][]{
                {"P0D", Period.of(0, 0, 0), true},
                {"P1D", Period.of(0, 0, 1), true},
                {"P1M", Period.of(0, 1, 0), true},
                {"P1Y", Period.of(1, 0, 0), true},
                {"P1Y1M", Period.of(1, 1, 0), true},
                {"P1Y1D", Period.of(1, 0, 1), true},
                {"P1Y1M1D", Period.of(1, 1, 1), true},
                {"P10Y10M10D", Period.of(10, 10, 10), true},
                {"P6Y3M21D", Period.of(6, 3, 21), true},
                {"P1120D", Period.ofWeeks(160), true},
                {"PONY", new IllegalArgumentException("Unable to parse 'PONY' as a Period.")},

        });
        TEST_DB.put(pair(Map.class, Period.class), new Object[][]{
                {mapOf(V, "P0D"), Period.of(0, 0, 0)},
                {mapOf(VALUE, "P1Y1M1D"), Period.of(1, 1, 1)},
                {mapOf(PERIOD, "P2Y2M2D"), Period.of(2, 2, 2), true},
                {mapOf(PERIOD, "P2Y5M16D"), Period.of(2, 5, 16), true},
                {mapOf("x", ""), new IllegalArgumentException("map must include: [period], [value], or [_v]")},
        });
    }

    /**
     * YearMonth
     */
    private static void loadYearMonthTests() {
        TEST_DB.put(pair(Void.class, YearMonth.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(YearMonth.class, YearMonth.class), new Object[][]{
                {YearMonth.of(2023, 12), YearMonth.of(2023, 12), true},
                {YearMonth.of(1970, 1), YearMonth.of(1970, 1), true},
                {YearMonth.of(1999, 6), YearMonth.of(1999, 6), true},
        });
        TEST_DB.put(pair(Date.class, YearMonth.class), new Object[][] {
                {date("1888-01-01T15:00:00Z"), YearMonth.of(1888, 1), false},    // 1888-01-02 00:00 Tokyo
                {date("1969-12-30T15:00:00Z"), YearMonth.of(1969, 12), false},   // 1969-12-31 00:00 Tokyo
                {date("1969-12-31T15:00:00Z"), YearMonth.of(1970, 1), false},    // 1970-01-01 00:00 Tokyo
                {date("2023-06-14T15:00:00Z"), YearMonth.of(2023, 6), false},    // 2023-06-15 00:00 Tokyo
                {date("2023-06-15T12:30:45Z"), YearMonth.of(2023, 6), false},   // 2023-06-15 21:30:45 Tokyo
                {date("2023-06-15T14:59:59Z"), YearMonth.of(2023, 6), false},   // 2023-06-15 23:59:59 Tokyo
                {date("2023-06-15T00:00:01Z"), YearMonth.of(2023, 6), false}    // 2023-06-15 09:00:01 Tokyo
        });
        TEST_DB.put(pair(java.sql.Date.class, YearMonth.class), new Object[][] {
                {java.sql.Date.valueOf("1888-01-02"), YearMonth.of(1888, 1), false},
                {java.sql.Date.valueOf("1969-12-31"), YearMonth.of(1969, 12), false},
                {java.sql.Date.valueOf("1970-01-01"), YearMonth.of(1970, 1), false},
                {java.sql.Date.valueOf("2023-06-15"), YearMonth.of(2023, 6), false},
                {java.sql.Date.valueOf("2023-06-01"), YearMonth.of(2023, 6), false},
                {java.sql.Date.valueOf("2023-06-30"), YearMonth.of(2023, 6), false}
        });
        TEST_DB.put(pair(LocalDate.class, YearMonth.class), new Object[][] {
                {LocalDate.of(1888, 1, 2), YearMonth.of(1888, 1), false},
                {LocalDate.of(1969, 12, 31), YearMonth.of(1969, 12), false},
                {LocalDate.of(1970, 1, 1), YearMonth.of(1970, 1), false},
                {LocalDate.of(2023, 6, 15), YearMonth.of(2023, 6), false},
                {LocalDate.of(2023, 6, 1), YearMonth.of(2023, 6), false},
                {LocalDate.of(2023, 6, 30), YearMonth.of(2023, 6), false}
        });
        TEST_DB.put(pair(LocalDateTime.class, YearMonth.class), new Object[][] {
                {LocalDateTime.of(1888, 1, 2, 0, 0), YearMonth.of(1888, 1), false},
                {LocalDateTime.of(1969, 12, 31, 0, 0), YearMonth.of(1969, 12), false},
                {LocalDateTime.of(1970, 1, 1, 0, 0), YearMonth.of(1970, 1), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0), YearMonth.of(2023, 6), false},

                // One-way tests (false) - various times on same date
                {LocalDateTime.of(2023, 6, 15, 12, 30, 45), YearMonth.of(2023, 6), false},
                {LocalDateTime.of(2023, 6, 15, 23, 59, 59, 999_999_999), YearMonth.of(2023, 6), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0, 0, 1), YearMonth.of(2023, 6), false},

                // One-way tests (false) - different days in same month
                {LocalDateTime.of(2023, 6, 1, 12, 0), YearMonth.of(2023, 6), false},
                {LocalDateTime.of(2023, 6, 30, 12, 0), YearMonth.of(2023, 6), false}
        });
        TEST_DB.put(pair(OffsetDateTime.class, YearMonth.class), new Object[][] {
                // Bidirectional tests (true) - all at midnight Tokyo (+09:00)
                {odt("1888-01-01T15:00:00Z"), YearMonth.of(1888, 1), false},    // 1888-01-02 00:00 Tokyo
                {odt("1969-12-30T15:00:00Z"), YearMonth.of(1969, 12), false},   // 1969-12-31 00:00 Tokyo
                {odt("1969-12-31T15:00:00Z"), YearMonth.of(1970, 1), false},    // 1970-01-01 00:00 Tokyo
                {odt("2023-06-14T15:00:00Z"), YearMonth.of(2023, 6), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {odt("2023-06-15T12:30:45Z"), YearMonth.of(2023, 6), false},       // 21:30:45 Tokyo
                {odt("2023-06-15T14:59:59.999Z"), YearMonth.of(2023, 6), false},   // 23:59:59.999 Tokyo
                {odt("2023-06-15T00:00:01Z"), YearMonth.of(2023, 6), false},       // 09:00:01 Tokyo

                // One-way tests (false) - same date in different offset
                {odt("2023-06-15T00:00:00+09:00"), YearMonth.of(2023, 6), false},  // Tokyo local time
                {odt("2023-06-15T00:00:00-05:00"), YearMonth.of(2023, 6), false}   // US Eastern time
        });
        TEST_DB.put(pair(ZonedDateTime.class, YearMonth.class), new Object[][] {
                {zdt("1888-01-01T15:00:00Z"), YearMonth.of(1888, 1), false},    // 1888-01-02 00:00 Tokyo
                {zdt("1969-12-30T15:00:00Z"), YearMonth.of(1969, 12), false},   // 1969-12-31 00:00 Tokyo
                {zdt("1969-12-31T15:00:00Z"), YearMonth.of(1970, 1), false},    // 1970-01-01 00:00 Tokyo
                {zdt("2023-06-14T15:00:00Z"), YearMonth.of(2023, 6), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {zdt("2023-06-15T12:30:45Z"), YearMonth.of(2023, 6), false},       // 21:30:45 Tokyo
                {zdt("2023-06-15T14:59:59.999Z"), YearMonth.of(2023, 6), false},   // 23:59:59.999 Tokyo
                {zdt("2023-06-15T00:00:01Z"), YearMonth.of(2023, 6), false},       // 09:00:01 Tokyo

                // One-way tests (false) - same time in different zones
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo")), YearMonth.of(2023, 6), false},
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("America/New_York")), YearMonth.of(2023, 6), false}
        });
        TEST_DB.put(pair(Timestamp.class, YearMonth.class), new Object[][] {
                // Bidirectional tests (true) - all at midnight Tokyo (+09:00)
                {timestamp("1888-01-01T15:00:00Z"), YearMonth.of(1888, 1), false},    // 1888-01-02 00:00 Tokyo
                {timestamp("1969-12-30T15:00:00Z"), YearMonth.of(1969, 12), false},   // 1969-12-31 00:00 Tokyo
                {timestamp("1969-12-31T15:00:00Z"), YearMonth.of(1970, 1), false},    // 1970-01-01 00:00 Tokyo
                {timestamp("2023-06-14T15:00:00Z"), YearMonth.of(2023, 6), false},    // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {timestamp("2023-06-15T12:30:45.123Z"), YearMonth.of(2023, 6), false},     // 21:30:45 Tokyo
                {timestamp("2023-06-15T14:59:59.999Z"), YearMonth.of(2023, 6), false},     // 23:59:59.999 Tokyo
                {timestamp("2023-06-15T00:00:00.001Z"), YearMonth.of(2023, 6), false},     // 09:00:00.001 Tokyo

                // One-way tests (false) - with nanosecond precision
                {timestamp("2023-06-15T12:00:00.123456789Z"), YearMonth.of(2023, 6), false}  // 21:00:00.123456789 Tokyo
        });
        TEST_DB.put(pair(Calendar.class, YearMonth.class), new Object[][] {
                {createCalendar(1888, 1, 2, 0, 0, 0), YearMonth.of(1888, 1), false},
                {createCalendar(1969, 12, 31, 0, 0, 0), YearMonth.of(1969, 12), false},
                {createCalendar(1970, 1, 1, 0, 0, 0), YearMonth.of(1970, 1), false},
                {createCalendar(2023, 6, 15, 0, 0, 0), YearMonth.of(2023, 6), false},
                {createCalendar(2023, 6, 15, 12, 30, 45), YearMonth.of(2023, 6), false},
                {createCalendar(2023, 12, 31, 23, 59, 59), YearMonth.of(2023, 12), false},
                {createCalendar(2023, 1, 1, 1, 0, 1), YearMonth.of(2023, 1), false}
        });
        TEST_DB.put(pair(String.class, YearMonth.class), new Object[][]{
                {"", null},
                {"2024-01", YearMonth.of(2024, 1), true},
                {"2024-1", new IllegalArgumentException("Unable to extract Year-Month from string: 2024-1")},
                {"2024-1-1", YearMonth.of(2024, 1)},
                {"2024-06-01", YearMonth.of(2024, 6)},
                {"2024-06", YearMonth.of(2024, 6), true},
                {"2024-12-31", YearMonth.of(2024, 12)},
                {"2024-12", YearMonth.of(2024, 12), true},
                {"05:45 2024-12-31", YearMonth.of(2024, 12)},
        });
        TEST_DB.put(pair(Map.class, YearMonth.class), new Object[][]{
                {mapOf(V, "2024-01"), YearMonth.of(2024, 1)},
                {mapOf(VALUE, "2024-01"), YearMonth.of(2024, 1)},
                {mapOf(YEAR_MONTH, "2024-12"), YearMonth.of(2024, 12), true},
        });
    }

    /**
     * MonthDay
     */
    private static void loadMonthDayTests() {
        TEST_DB.put(pair(Void.class, MonthDay.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(MonthDay.class, MonthDay.class), new Object[][]{
                {MonthDay.of(1, 1), MonthDay.of(1, 1)},
                {MonthDay.of(12, 31), MonthDay.of(12, 31)},
                {MonthDay.of(6, 30), MonthDay.of(6, 30)},
        });
        TEST_DB.put(pair(Calendar.class, MonthDay.class), new Object[][] {
                {createCalendar(1888, 1, 2, 0, 0, 0), MonthDay.of(1, 2), false},
                {createCalendar(1969, 12, 31, 0, 0, 0), MonthDay.of(12, 31), false},
                {createCalendar(1970, 1, 1, 0, 0, 0), MonthDay.of(1, 1), false},
                {createCalendar(2023, 6, 15, 0, 0, 0), MonthDay.of(6, 15), false},
                {createCalendar(2023, 6, 15, 12, 30, 45), MonthDay.of(6, 15), false},
                {createCalendar(2023, 6, 15, 23, 59, 59), MonthDay.of(6, 15), false},
                {createCalendar(2023, 6, 15, 1, 0, 1), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(Date.class, MonthDay.class), new Object[][] {
                {date("1888-01-02T00:00:00Z"), MonthDay.of(1, 2), false},
                {date("1969-12-31T00:00:00Z"), MonthDay.of(12, 31), false},
                {date("1970-01-01T00:00:00Z"), MonthDay.of(1, 1), false},
                {date("2023-06-15T00:00:00Z"), MonthDay.of(6, 15), false},
                {date("2023-06-15T12:30:45Z"), MonthDay.of(6, 15), false},
                {date("2023-06-14T23:59:59Z"), MonthDay.of(6, 15), false},
                {date("2023-06-15T00:00:01Z"), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(java.sql.Date.class, MonthDay.class), new Object[][] {
                // Bidirectional tests (true) - dates represent same month/day regardless of timezone
                {java.sql.Date.valueOf("1888-01-02"), MonthDay.of(1, 2), false},
                {java.sql.Date.valueOf("1969-12-31"), MonthDay.of(12, 31), false},
                {java.sql.Date.valueOf("1970-01-01"), MonthDay.of(1, 1), false},
                {java.sql.Date.valueOf("2023-06-15"), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(LocalDate.class, MonthDay.class), new Object[][] {
                {LocalDate.of(1888, 1, 2), MonthDay.of(1, 2), false},
                {LocalDate.of(1969, 12, 31), MonthDay.of(12, 31), false},
                {LocalDate.of(1970, 1, 1), MonthDay.of(1, 1), false},
                {LocalDate.of(2023, 6, 15), MonthDay.of(6, 15), false},
                {LocalDate.of(2022, 6, 15), MonthDay.of(6, 15), false},
                {LocalDate.of(2024, 6, 15), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(LocalDateTime.class, MonthDay.class), new Object[][] {
                // One-way
                {LocalDateTime.of(1888, 1, 2, 0, 0), MonthDay.of(1, 2), false},
                {LocalDateTime.of(1969, 12, 31, 0, 0), MonthDay.of(12, 31), false},
                {LocalDateTime.of(1970, 1, 1, 0, 0), MonthDay.of(1, 1), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0), MonthDay.of(6, 15), false},

                // One-way tests (false) - various times on same date
                {LocalDateTime.of(2023, 6, 15, 12, 30, 45), MonthDay.of(6, 15), false},
                {LocalDateTime.of(2023, 6, 15, 23, 59, 59, 999_999_999), MonthDay.of(6, 15), false},
                {LocalDateTime.of(2023, 6, 15, 0, 0, 0, 1), MonthDay.of(6, 15), false},

                // One-way tests (false) - same month-day in different years
                {LocalDateTime.of(2022, 6, 15, 12, 0), MonthDay.of(6, 15), false},
                {LocalDateTime.of(2024, 6, 15, 12, 0), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(OffsetDateTime.class, MonthDay.class), new Object[][] {
                {odt("1888-01-01T15:00:00Z"), MonthDay.of(1, 2), false},    // 1888-01-02 00:00 Tokyo
                {odt("1969-12-30T15:00:00Z"), MonthDay.of(12, 31), false},  // 1969-12-31 00:00 Tokyo
                {odt("1969-12-31T15:00:00Z"), MonthDay.of(1, 1), false},    // 1970-01-01 00:00 Tokyo
                {odt("2023-06-14T15:00:00Z"), MonthDay.of(6, 15), false},   // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {odt("2023-06-15T12:30:45Z"), MonthDay.of(6, 15), false},  // 21:30:45 Tokyo
                {odt("2023-06-15T14:59:59.999Z"), MonthDay.of(6, 15), false},  // 23:59:59.999 Tokyo
                {odt("2023-06-15T00:00:01Z"), MonthDay.of(6, 15), false},  // 09:00:01 Tokyo

                // One-way tests (false) - same date in different offset
                {odt("2023-06-15T00:00:00+09:00"), MonthDay.of(6, 15), false},  // Tokyo local time
                {odt("2023-06-15T00:00:00-05:00"), MonthDay.of(6, 15), false}   // US Eastern time
        });
        TEST_DB.put(pair(ZonedDateTime.class, MonthDay.class), new Object[][] {
                {zdt("1888-01-01T15:00:00Z"), MonthDay.of(1, 2), false},    // 1888-01-02 00:00 Tokyo
                {zdt("1969-12-30T15:00:00Z"), MonthDay.of(12, 31), false},  // 1969-12-31 00:00 Tokyo
                {zdt("1969-12-31T15:00:00Z"), MonthDay.of(1, 1), false},    // 1970-01-01 00:00 Tokyo
                {zdt("2023-06-14T15:00:00Z"), MonthDay.of(6, 15), false},   // 2023-06-15 00:00 Tokyo
                {zdt("2023-06-15T12:30:45Z"), MonthDay.of(6, 15), false},       // 21:30:45 Tokyo
                {zdt("2023-06-15T14:59:59.999Z"), MonthDay.of(6, 15), false},   // 23:59:59.999 Tokyo
                {zdt("2023-06-15T00:00:01Z"), MonthDay.of(6, 15), false},       // 09:00:01 Tokyo

                // One-way tests (false) - same time in different zones
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo")), MonthDay.of(6, 15), false},
                {ZonedDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneId.of("America/New_York")), MonthDay.of(6, 15), false}
        });
        TEST_DB.put(pair(Timestamp.class, MonthDay.class), new Object[][] {
                {timestamp("1888-01-01T15:00:00Z"), MonthDay.of(1, 2), false},    // 1888-01-02 00:00 Tokyo
                {timestamp("1969-12-30T15:00:00Z"), MonthDay.of(12, 31), false},  // 1969-12-31 00:00 Tokyo
                {timestamp("1969-12-31T15:00:00Z"), MonthDay.of(1, 1), false},    // 1970-01-01 00:00 Tokyo
                {timestamp("2023-06-14T15:00:00Z"), MonthDay.of(6, 15), false},   // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - various times before Tokyo midnight
                {timestamp("2023-06-15T12:30:45.123Z"), MonthDay.of(6, 15), false},     // 21:30:45 Tokyo
                {timestamp("2023-06-15T14:59:59.999Z"), MonthDay.of(6, 15), false},     // 23:59:59.999 Tokyo
                {timestamp("2023-06-15T00:00:00.001Z"), MonthDay.of(6, 15), false},     // 09:00:00.001 Tokyo

                // One-way tests (false) - with nanosecond precision
                {timestamp("2023-06-15T12:00:00.123456789Z"), MonthDay.of(6, 15), false}  // 21:00:00.123456789 Tokyo
        });
        TEST_DB.put(pair(String.class, MonthDay.class), new Object[][]{
                {"", null},
                {"1-1", MonthDay.of(1, 1)},
                {"01-01", MonthDay.of(1, 1)},
                {"--01-01", MonthDay.of(1, 1), true},
                {"--1-1", new IllegalArgumentException("Unable to extract Month-Day from string: --1-1")},
                {"12-31", MonthDay.of(12, 31)},
                {"--12-31", MonthDay.of(12, 31), true},
                {"-12-31", new IllegalArgumentException("Unable to extract Month-Day from string: -12-31")},
                {"6-30", MonthDay.of(6, 30)},
                {"06-30", MonthDay.of(6, 30)},
                {"2024-06-30", MonthDay.of(6, 30)},
                {"--06-30", MonthDay.of(6, 30), true},
                {"--6-30", new IllegalArgumentException("Unable to extract Month-Day from string: --6-30")},
        });
        TEST_DB.put(pair(Map.class, MonthDay.class), new Object[][]{
                {mapOf(MONTH_DAY, "1-1"), MonthDay.of(1, 1)},
                {mapOf(VALUE, "1-1"), MonthDay.of(1, 1)},
                {mapOf(V, "01-01"), MonthDay.of(1, 1)},
                {mapOf(MONTH_DAY, "--01-01"), MonthDay.of(1, 1)},
                {mapOf(MONTH_DAY, "--1-1"), new IllegalArgumentException("Unable to extract Month-Day from string: --1-1")},
                {mapOf(MONTH_DAY, "12-31"), MonthDay.of(12, 31)},
                {mapOf(MONTH_DAY, "--12-31"), MonthDay.of(12, 31)},
                {mapOf(MONTH_DAY, "-12-31"), new IllegalArgumentException("Unable to extract Month-Day from string: -12-31")},
                {mapOf(MONTH_DAY, "6-30"), MonthDay.of(6, 30)},
                {mapOf(MONTH_DAY, "06-30"), MonthDay.of(6, 30)},
                {mapOf(MONTH_DAY, "--06-30"), MonthDay.of(6, 30)},
                {mapOf(MONTH_DAY, "--6-30"), new IllegalArgumentException("Unable to extract Month-Day from string: --6-30")},
                {mapOf(MONTH_DAY, "--06-30"), MonthDay.of(6, 30), true},
                {mapOf(MONTH_DAY, "--06-30"), MonthDay.of(6, 30)},
                {mapOf(MONTH_DAY, mapOf("_v", "--06-30")), MonthDay.of(6, 30)},    // recursive on monthDay
                {mapOf(VALUE, "--06-30"), MonthDay.of(6, 30)},                      // using VALUE key
        });
    }

    /**
     * OffsetDateTime
     */
    private static void loadOffsetDateTimeTests() {
        TEST_DB.put(pair(Void.class, OffsetDateTime.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(OffsetDateTime.class, OffsetDateTime.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), true},
        });
        TEST_DB.put(pair(Double.class, OffsetDateTime.class), new Object[][]{
                {-1.0, odt("1969-12-31T23:59:59Z"), true},
                {-0.000000002, odt("1969-12-31T23:59:59.999999998Z"), true},
                {-0.000000001, odt("1969-12-31T23:59:59.999999999Z"), true},
                {0.0, odt("1970-01-01T00:00:00Z"), true},
                {0.000000001, odt("1970-01-01T00:00:00.000000001Z"), true},
                {0.000000002, odt("1970-01-01T00:00:00.000000002Z"), true},
                {1.0, odt("1970-01-01T00:00:01Z"), true},
        });
        TEST_DB.put(pair(AtomicLong.class, OffsetDateTime.class), new Object[][]{
                {new AtomicLong(-1), odt("1969-12-31T23:59:59.999Z"), true},
                {new AtomicLong(0), odt("1970-01-01T00:00:00Z"), true},
                {new AtomicLong(1), odt("1970-01-01T00:00:00.001Z"), true},
        });
        TEST_DB.put(pair(Timestamp.class, OffsetDateTime.class), new Object[][]{
                {new Timestamp(-1), odt("1969-12-31T23:59:59.999+00:00"), true},
                {new Timestamp(-1), odt("1969-12-31T23:59:59.999-00:00"), true},
                {new Timestamp(0), odt("1970-01-01T00:00:00+00:00"), true},
                {new Timestamp(0), odt("1970-01-01T00:00:00-00:00"), true},
                {new Timestamp(1), odt("1970-01-01T00:00:00.001+00:00"), true},
                {new Timestamp(1), odt("1970-01-01T00:00:00.001-00:00"), true},
                {timestamp("1969-12-31T23:59:59.999999999Z"), OffsetDateTime.parse("1970-01-01T08:59:59.999999999+09:00"), true},
                {timestamp("1970-01-01T00:00:00Z"), OffsetDateTime.parse("1970-01-01T09:00:00+09:00"), true},
                {timestamp("1970-01-01T00:00:00.000000001Z"), OffsetDateTime.parse("1970-01-01T09:00:00.000000001+09:00"), true},
                {timestamp("2024-02-18T06:31:55.987654321Z"), OffsetDateTime.parse("2024-02-18T15:31:55.987654321+09:00"), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, OffsetDateTime.class), new Object[][]{
                {ldt("1970-01-01T08:59:59.999999999"), odt("1969-12-31T23:59:59.999999999Z"), true},
                {ldt("1970-01-01T09:00:00"), odt("1970-01-01T00:00:00Z"), true},
                {ldt("1970-01-01T09:00:00.000000001"), odt("1970-01-01T00:00:00.000000001Z"), true},
                {ldt("1969-12-31T23:59:59.999999999"), odt("1969-12-31T23:59:59.999999999+09:00"), true},
                {ldt("1970-01-01T00:00:00"), odt("1970-01-01T00:00:00+09:00"), true},
                {ldt("1970-01-01T00:00:00.000000001"), odt("1970-01-01T00:00:00.000000001+09:00"), true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, OffsetDateTime.class), new Object[][]{
                {zdt("1890-01-01T00:00:00Z"), odt("1890-01-01T00:00:00Z"), true},
                {zdt("1969-12-31T23:59:59.999999999Z"), odt("1969-12-31T23:59:59.999999999Z"), true},
                {zdt("1970-01-01T00:00:00Z"), odt("1970-01-01T00:00:00Z"), true},
                {zdt("1970-01-01T00:00:00.000000001Z"), odt("1970-01-01T00:00:00.000000001Z"), true},
                {zdt("2024-03-20T21:18:05.123456Z"), odt("2024-03-20T21:18:05.123456Z"), true},
        });
        TEST_DB.put(pair(String.class, OffsetDateTime.class), new Object[][]{
                {"", null},
                {"2024-02-10T10:15:07+01:00", OffsetDateTime.parse("2024-02-10T10:15:07+01:00"), true},
        });
        TEST_DB.put(pair(Map.class, OffsetDateTime.class), new Object[][] {
                { mapOf(OFFSET_DATE_TIME, "1969-12-31T23:59:59.999999999+09:00"), OffsetDateTime.parse("1969-12-31T23:59:59.999999999+09:00"), true},
                { mapOf(OFFSET_DATE_TIME, "1970-01-01T00:00:00+09:00"), OffsetDateTime.parse("1970-01-01T00:00+09:00"), true},
                { mapOf(OFFSET_DATE_TIME, "1970-01-01T00:00:00.000000001+09:00"), OffsetDateTime.parse("1970-01-01T00:00:00.000000001+09:00"), true},
                { mapOf(OFFSET_DATE_TIME, "2024-03-10T11:07:00.123456789+09:00"), OffsetDateTime.parse("2024-03-10T11:07:00.123456789+09:00"), true},
                { mapOf("foo", "2024-03-10T11:07:00.123456789+00:00"), new IllegalArgumentException("Map to 'OffsetDateTime' the map must include: [offsetDateTime], [value], [_v], or [epochMillis] as key with associated value")},
                { mapOf(OFFSET_DATE_TIME, "2024-03-10T11:07:00.123456789+09:00"), OffsetDateTime.parse("2024-03-10T11:07:00.123456789+09:00")},
                { mapOf(VALUE, "2024-03-10T11:07:00.123456789+09:00"), OffsetDateTime.parse("2024-03-10T11:07:00.123456789+09:00")},
                { mapOf(V, "2024-03-10T11:07:00.123456789+09:00"), OffsetDateTime.parse("2024-03-10T11:07:00.123456789+09:00")},
        });
    }

    /**
     * Duration
     */
    private static void loadDurationTests() {
        TEST_DB.put(pair(Void.class, Duration.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Duration.class, Duration.class), new Object[][]{
                {Duration.ofMillis(1), Duration.ofMillis(1)}
        });
        TEST_DB.put(pair(String.class, Duration.class), new Object[][]{
                {"PT1S", Duration.ofSeconds(1), true},
                {"PT10S", Duration.ofSeconds(10), true},
                {"PT1M", Duration.ofSeconds(60), true},
                {"PT1M40S", Duration.ofSeconds(100), true},
                {"PT16M40S", Duration.ofSeconds(1000), true},
                {"PT20.345S", Duration.parse("PT20.345S") , true},
                {"PT2H46M40S", Duration.ofSeconds(10000), true},
                {"Bitcoin", new IllegalArgumentException("Unable to parse 'Bitcoin' as a Duration")},
                {"", new IllegalArgumentException("Unable to parse '' as a Duration")},
        });
        TEST_DB.put(pair(BigInteger.class, Duration.class), new Object[][]{
                {BigInteger.valueOf(-1000000), Duration.ofNanos(-1000000), true},
                {BigInteger.valueOf(-1000), Duration.ofNanos(-1000), true},
                {BigInteger.valueOf(-1), Duration.ofNanos(-1), true},
                {BigInteger.ZERO, Duration.ofNanos(0), true},
                {BigInteger.valueOf(1), Duration.ofNanos(1), true},
                {BigInteger.valueOf(1000), Duration.ofNanos(1000), true},
                {BigInteger.valueOf(1000000), Duration.ofNanos(1000000), true},
                {BigInteger.valueOf(Integer.MAX_VALUE), Duration.ofNanos(Integer.MAX_VALUE), true},
                {BigInteger.valueOf(Integer.MIN_VALUE), Duration.ofNanos(Integer.MIN_VALUE), true},
                {BigInteger.valueOf(Long.MAX_VALUE), Duration.ofNanos(Long.MAX_VALUE), true},
                {BigInteger.valueOf(Long.MIN_VALUE), Duration.ofNanos(Long.MIN_VALUE), true},
        });
        TEST_DB.put(pair(Map.class, Duration.class), new Object[][] {
                // Standard seconds/nanos format (the default key is "seconds", expecting a BigDecimal or numeric value)
                { mapOf(DURATION, "-0.001"), Duration.ofMillis(-1) },   // not reversible
                { mapOf(DURATION, "PT-0.001S"), Duration.ofSeconds(-1, 999_000_000), true },
                { mapOf(DURATION, "PT0S"),       Duration.ofMillis(0), true },
                { mapOf(DURATION, "PT0.001S"),   Duration.ofMillis(1), true },

                // Numeric strings for seconds/nanos (key "seconds" gets a BigDecimal representing seconds.nanos)
                { mapOf(DURATION, new BigDecimal("123.456000000")), Duration.ofSeconds(123, 456000000) },
                { mapOf(DURATION, new BigDecimal("-123.456000000")), Duration.ofSeconds(-124, 544_000_000) },

                // ISO 8601 format (the key "value" is expected to hold a String in ISO 8601 format)
                { mapOf(VALUE, "PT15M"), Duration.ofMinutes(15) },
                { mapOf(VALUE, "PT1H30M"), Duration.ofMinutes(90) },
                { mapOf(VALUE, "-PT1H30M"), Duration.ofMinutes(-90) },
                { mapOf(VALUE, "PT1.5S"), Duration.ofMillis(1500) },

                // Different value field keys (if the key is "value" or its alias then the value must be ISO 8601)
                { mapOf(VALUE, "PT16S"), Duration.ofSeconds(16) },
                { mapOf(V, "PT16S"), Duration.ofSeconds(16) },
                { mapOf(VALUE, "PT16S"), Duration.ofSeconds(16) },

                // Edge cases (using the "seconds" key with a BigDecimal value)
                { mapOf(DURATION, new BigDecimal(Long.toString(Long.MAX_VALUE) + ".999999999")), Duration.ofSeconds(Long.MAX_VALUE, 999999999) },
                { mapOf(DURATION, new BigDecimal(Long.toString(Long.MIN_VALUE))), Duration.ofSeconds(Long.MIN_VALUE, 0) },

                // Mixed formats:
                { mapOf(DURATION, "PT1H"), Duration.ofHours(1) },   // ISO string in seconds field (converter should detect the ISO 8601 pattern)
                { mapOf(DURATION, new BigDecimal("1.5")), Duration.ofMillis(1500) }, // Decimal value in seconds field

                // Optional nanos (when only seconds are provided using the "seconds" key)
                { mapOf(DURATION, new BigDecimal("123")), Duration.ofSeconds(123) },
                { mapOf(DURATION, new BigDecimal("123")), Duration.ofSeconds(123) }
        });
    }

    /**
     * java.sql.Date
     */
    private static void loadSqlDateTests() {
        TEST_DB.put(pair(Void.class, java.sql.Date.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(java.sql.Date.class, java.sql.Date.class), new Object[][] {
                { new java.sql.Date(0), new java.sql.Date(0) },
        });
        TEST_DB.put(pair(Double.class, java.sql.Date.class), new Object[][]{
                // --------------------------------------------------------------------
                // Bidirectional tests:
                // The input double value is exactly the seconds corresponding to Tokyo midnight.
                // Thus, converting to java.sql.Date (by truncating any fractional part) yields the
                // date whose "start of day" in Tokyo corresponds to that exact second value.
                // --------------------------------------------------------------------
                { -32400.0,      java.sql.Date.valueOf("1970-01-01"), true },
                { 54000.0,       java.sql.Date.valueOf("1970-01-02"), true },
                { 140400.0,      java.sql.Date.valueOf("1970-01-03"), true },
                { 31503600.0,    java.sql.Date.valueOf("1971-01-01"), true },
                { 946652400.0,   java.sql.Date.valueOf("2000-01-01"), true },
                { 1577804400.0,  java.sql.Date.valueOf("2020-01-01"), true },
                { -1988182800.0, java.sql.Date.valueOf("1907-01-01"), true },

                // --------------------------------------------------------------------
                // Unidirectional tests:
                // The input double value is not exactly the midnight seconds value.
                // Although converting to Date yields the correct local day, the reverse conversion
                // (which always yields the Tokyo midnight value) will differ.
                // --------------------------------------------------------------------
                { 0.0,             java.sql.Date.valueOf("1970-01-01"), false },
                { -0.001,          java.sql.Date.valueOf("1970-01-01"), false },
                { 0.001,           java.sql.Date.valueOf("1970-01-01"), false },
                { -32399.5,        java.sql.Date.valueOf("1970-01-01"), false },
                { -1988182800.987, java.sql.Date.valueOf("1907-01-01"), false },
                { 1577804400.123,  java.sql.Date.valueOf("2020-01-01"), false }
        });
        TEST_DB.put(pair(AtomicLong.class, java.sql.Date.class), new Object[][]{
                // --------------------------------------------------------------------
                // BIDIRECTIONAL tests: the input millisecond value equals the epoch
                // value for the local midnight of the given date in Asia/Tokyo.
                // (i.e. x == date.atStartOfDay(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli())
                // --------------------------------------------------------------------
                // For 1970-01-01: midnight in Tokyo is 1970-01-01T00:00 JST, which in UTC is 1969-12-31T15:00Z,
                // i.e. -9 hours in ms = -32400000.
                { new AtomicLong(-32400000L),      java.sql.Date.valueOf("1970-01-01"), true },

                // For 1970-01-02: midnight in Tokyo is 1970-01-02T00:00 JST = 1970-01-01T15:00Z,
                // which is -32400000 + 86400000 = 54000000.
                { new AtomicLong(54000000L),       java.sql.Date.valueOf("1970-01-02"), true },

                // For 1970-01-03: midnight in Tokyo is 1970-01-03T00:00 JST = 1970-01-02T15:00Z,
                // which is 54000000 + 86400000 = 140400000.
                { new AtomicLong(140400000L),      java.sql.Date.valueOf("1970-01-03"), true },

                // For 1971-01-01: 1970-01-01 midnight in Tokyo is -32400000; add 365 days:
                // 365*86400000 = 31536000000, so -32400000 + 31536000000 = 31503600000.
                { new AtomicLong(31503600000L),    java.sql.Date.valueOf("1971-01-01"), true },

                // For 2000-01-01: 2000-01-01T00:00 JST equals 1999-12-31T15:00Z.
                // Since 2000-01-01T00:00Z is 946684800000, subtract 9 hours (32400000) to get:
                // 946684800000 - 32400000 = 946652400000.
                { new AtomicLong(946652400000L),   java.sql.Date.valueOf("2000-01-01"), true },

                // For 2020-01-01: 2020-01-01T00:00 JST equals 2019-12-31T15:00Z.
                // (Epoch for 2020-01-01T00:00Z is 1577836800000, minus 32400000 equals 1577804400000.)
                { new AtomicLong(1577804400000L),  java.sql.Date.valueOf("2020-01-01"), true },

                // A far‐past date – for example, 1907-01-01.
                // (Compute: 1907-01-01T00:00 JST equals 1906-12-31T15:00Z.
                //  From 1907-01-01 to 1970-01-01 is 23011 days; 23011*86400000 = 1,988,150,400,000.
                //  Then: -32400000 - 1,988,150,400,000 = -1,988,182,800,000.)
                { new AtomicLong(-1988182800000L), java.sql.Date.valueOf("1907-01-01"), true },

                // --------------------------------------------------------------------
                // UNIDIRECTIONAL tests: the input millisecond value is not at local midnight.
                // Although converting to Date yields the correct local day, if you convert back
                // you will get the epoch value for midnight (i.e. the “rounded‐down” value).
                // --------------------------------------------------------------------
                // -1L:  1969-12-31T23:59:59.999Z → in Tokyo becomes 1970-01-01T08:59:59.999, so date is 1970-01-01.
                { new AtomicLong(-1L),            java.sql.Date.valueOf("1970-01-01"), false },

                // 1L: 1970-01-01T00:00:00.001Z → in Tokyo 1970-01-01T09:00:00.001 → still 1970-01-01.
                { new AtomicLong(1L),             java.sql.Date.valueOf("1970-01-01"), false },

                // 43,200,000L: 12 hours after epoch: 1970-01-01T12:00:00Z → in Tokyo 1970-01-01T21:00:00 → date: 1970-01-01.
                { new AtomicLong(43200000L),      java.sql.Date.valueOf("1970-01-01"), false },

                // 86,399,999L: 1 ms before 86400000; 1970-01-01T23:59:59.999Z → in Tokyo 1970-01-02T08:59:59.999 → date: 1970-01-02.
                { new AtomicLong(86399999L),      java.sql.Date.valueOf("1970-01-02"), false },

                // 86,401,000L: (86400000 + 1000) ms → 1970-01-02T00:00:01Z → in Tokyo 1970-01-02T09:00:01 → date: 1970-01-02.
                { new AtomicLong(86400000L + 1000),java.sql.Date.valueOf("1970-01-02"), false },
                { new AtomicLong(10000000000L),   java.sql.Date.valueOf("1970-04-27"), false },
                { new AtomicLong(1577836800001L), java.sql.Date.valueOf("2020-01-01"), false },
        });
        TEST_DB.put(pair(Date.class, java.sql.Date.class), new Object[][] {
                // Bidirectional tests (true) - using dates that represent midnight in Tokyo
                {date("1888-01-01T15:00:00Z"), java.sql.Date.valueOf("1888-01-02"), true},  // 1888-01-02 00:00 Tokyo
                {date("1969-12-30T15:00:00Z"), java.sql.Date.valueOf("1969-12-31"), true},  // 1969-12-31 00:00 Tokyo
                {date("1969-12-31T15:00:00Z"), java.sql.Date.valueOf("1970-01-01"), true},  // 1970-01-01 00:00 Tokyo
                {date("1970-01-01T15:00:00Z"), java.sql.Date.valueOf("1970-01-02"), true},  // 1970-01-02 00:00 Tokyo
                {date("2023-06-14T15:00:00Z"), java.sql.Date.valueOf("2023-06-15"), true},  // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - proving time portion is dropped
                {date("2023-06-15T08:30:45.123Z"), java.sql.Date.valueOf("2023-06-15"), false},  // 17:30 Tokyo
                {date("2023-06-15T14:59:59.999Z"), java.sql.Date.valueOf("2023-06-15"), false},  // 23:59:59.999 Tokyo
                {date("2023-06-15T00:00:00.001Z"), java.sql.Date.valueOf("2023-06-15"), false}   // 09:00:00.001 Tokyo
        });
        TEST_DB.put(pair(OffsetDateTime.class, java.sql.Date.class), new Object[][]{
                // Bidirectional tests (true) - all at midnight Tokyo time (UTC+9)
                {odt("1969-12-31T15:00:00Z"), java.sql.Date.valueOf("1970-01-01"), true},    // Jan 1, 00:00 Tokyo time
                {odt("1970-01-01T15:00:00Z"), java.sql.Date.valueOf("1970-01-02"), true},    // Jan 2, 00:00 Tokyo time
                {odt("2023-06-14T15:00:00Z"), java.sql.Date.valueOf("2023-06-15"), true},    // Jun 15, 00:00 Tokyo time

                // One-way tests (false) - various times that should truncate to midnight Tokyo time
                {odt("1970-01-01T03:30:00Z"), java.sql.Date.valueOf("1970-01-01"), false},           // Jan 1, 12:30 Tokyo
                {odt("1970-01-01T14:59:59.999Z"), java.sql.Date.valueOf("1970-01-01"), false},       // Jan 1, 23:59:59.999 Tokyo
                {odt("1970-01-01T15:00:00.001Z"), java.sql.Date.valueOf("1970-01-02"), false},       // Jan 2, 00:00:00.001 Tokyo
                {odt("2023-06-14T18:45:30Z"), java.sql.Date.valueOf("2023-06-15"), false},           // Jun 15, 03:45:30 Tokyo
                {odt("2023-06-14T23:30:00+09:00"), java.sql.Date.valueOf("2023-06-14"), false},      // Jun 15, 23:30 Tokyo
        });
        TEST_DB.put(pair(Timestamp.class, java.sql.Date.class), new Object[][]{
                // Bidirectional tests (true) - all at midnight Tokyo time
                {timestamp("1888-01-01T15:00:00Z"), java.sql.Date.valueOf("1888-01-02"), true},  // 1888-01-02 00:00 Tokyo
                {timestamp("1969-12-30T15:00:00Z"), java.sql.Date.valueOf("1969-12-31"), true},  // 1969-12-31 00:00 Tokyo
                {timestamp("1969-12-31T15:00:00Z"), java.sql.Date.valueOf("1970-01-01"), true},  // 1970-01-01 00:00 Tokyo
                {timestamp("1970-01-01T15:00:00Z"), java.sql.Date.valueOf("1970-01-02"), true},  // 1970-01-02 00:00 Tokyo
                {timestamp("2023-06-14T15:00:00Z"), java.sql.Date.valueOf("2023-06-15"), true},  // 2023-06-15 00:00 Tokyo

                // One-way tests (false) - proving time portion is dropped
                {timestamp("1970-01-01T12:30:45.123Z"), java.sql.Date.valueOf("1970-01-01"), false},
                {timestamp("2023-06-15T00:00:00.000Z"), java.sql.Date.valueOf("2023-06-15"), false},
                {timestamp("2023-06-15T14:59:59.999Z"), java.sql.Date.valueOf("2023-06-15"), false},
                {timestamp("2023-06-15T15:00:00.000Z"), java.sql.Date.valueOf("2023-06-16"), false}
        });
        TEST_DB.put(pair(LocalDate.class, java.sql.Date.class), new Object[][] {
                // Bidirectional tests (true)
                {LocalDate.of(1888, 1, 2), java.sql.Date.valueOf("1888-01-02"), true},
                {LocalDate.of(1969, 12, 31), java.sql.Date.valueOf("1969-12-31"), true},
                {LocalDate.of(1970, 1, 1), java.sql.Date.valueOf("1970-01-01"), true},
                {LocalDate.of(1970, 1, 2), java.sql.Date.valueOf("1970-01-02"), true},
                {LocalDate.of(2023, 6, 15), java.sql.Date.valueOf("2023-06-15"), true},

                // One-way tests (false) - though for LocalDate, all conversions should be bidirectional
                // since both types represent dates without time components
                {LocalDate.of(1970, 1, 1), java.sql.Date.valueOf("1970-01-01"), false},
                {LocalDate.of(2023, 12, 31), java.sql.Date.valueOf("2023-12-31"), false}
        });
        TEST_DB.put(pair(Calendar.class, java.sql.Date.class), new Object[][] {
                // Bidirectional tests (true) - all at midnight Tokyo time
                {createCalendar(1888, 1, 2, 0, 0, 0), java.sql.Date.valueOf("1888-01-02"), true},
                {createCalendar(1969, 12, 31, 0, 0, 0), java.sql.Date.valueOf("1969-12-31"), true},
                {createCalendar(1970, 1, 1, 0, 0, 0), java.sql.Date.valueOf("1970-01-01"), true},
                {createCalendar(1970, 1, 2, 0, 0, 0), java.sql.Date.valueOf("1970-01-02"), true},
                {createCalendar(2023, 6, 15, 0, 0, 0), java.sql.Date.valueOf("2023-06-15"), true},

                // One-way tests (false) - proving time portion is dropped
                {createCalendar(1970, 1, 1, 12, 30, 45), java.sql.Date.valueOf("1970-01-01"), false},
                {createCalendar(2023, 6, 15, 23, 59, 59), java.sql.Date.valueOf("2023-06-15"), false},
                {createCalendar(2023, 6, 15, 1, 0, 1), java.sql.Date.valueOf("2023-06-15"), false}
        });
        TEST_DB.put(pair(Instant.class, java.sql.Date.class), new Object[][]{
                // These instants, when viewed in Asia/Tokyo, yield the local date "0000-01-01"
                { Instant.parse("0000-01-01T00:00:00Z"),       java.sql.Date.valueOf("0000-01-01"), false },
                { Instant.parse("0000-01-01T00:00:00.001Z"),     java.sql.Date.valueOf("0000-01-01"), false },

                // These instants, when viewed in Asia/Tokyo, yield the local date "1970-01-01"
                { Instant.parse("1969-12-31T23:59:59Z"),         java.sql.Date.valueOf("1970-01-01"), false },
                { Instant.parse("1969-12-31T23:59:59.999Z"),      java.sql.Date.valueOf("1970-01-01"), false },
                { Instant.parse("1970-01-01T00:00:00Z"),         java.sql.Date.valueOf("1970-01-01"), false },
                { Instant.parse("1970-01-01T00:00:00.001Z"),      java.sql.Date.valueOf("1970-01-01"), false },
                { Instant.parse("1970-01-01T00:00:00.999Z"),      java.sql.Date.valueOf("1970-01-01"), false },
        });
        TEST_DB.put(pair(java.sql.Date.class, Instant.class), new Object[][] {
                // Bidirectional tests (true) - all at midnight Tokyo
                {java.sql.Date.valueOf("1888-01-02"), Instant.parse("1888-01-01T15:00:00Z"), true},    // 1888-01-02 00:00 Tokyo
                {java.sql.Date.valueOf("1969-12-31"), Instant.parse("1969-12-30T15:00:00Z"), true},    // 1969-12-31 00:00 Tokyo
                {java.sql.Date.valueOf("1970-01-01"), Instant.parse("1969-12-31T15:00:00Z"), true},    // 1970-01-01 00:00 Tokyo
                {java.sql.Date.valueOf("2023-06-15"), Instant.parse("2023-06-14T15:00:00Z"), true},    // 2023-06-15 00:00 Tokyo
        });
        TEST_DB.put(pair(ZonedDateTime.class, java.sql.Date.class), new Object[][]{
                // When it's midnight in Tokyo (UTC+9), it's 15:00 the previous day in UTC
                {zdt("1888-01-01T15:00:00+00:00"), java.sql.Date.valueOf("1888-01-02"), true},
                {zdt("1969-12-31T15:00:00+00:00"), java.sql.Date.valueOf("1970-01-01"), true},
                {zdt("1970-01-01T15:00:00+00:00"), java.sql.Date.valueOf("1970-01-02"), true},

                // One-way tests (false) - various times that should truncate to Tokyo midnight
                {zdt("1969-12-31T14:59:59+00:00"), java.sql.Date.valueOf("1969-12-31"), false},  // Just before Tokyo midnight
                {zdt("1969-12-31T15:00:01+00:00"), java.sql.Date.valueOf("1970-01-01"), false},  // Just after Tokyo midnight
                {zdt("1970-01-01T03:30:00+00:00"), java.sql.Date.valueOf("1970-01-01"), false},  // Middle of Tokyo day
                {zdt("1970-01-01T14:59:59+00:00"), java.sql.Date.valueOf("1970-01-01"), false},  // End of Tokyo day
        });
        TEST_DB.put(pair(Map.class, java.sql.Date.class), new Object[][] {
                { mapOf(SQL_DATE, 1703043551033L), java.sql.Date.valueOf("2023-12-20")},
                { mapOf(EPOCH_MILLIS, -1L), java.sql.Date.valueOf("1970-01-01")},
                { mapOf(EPOCH_MILLIS, 0L), java.sql.Date.valueOf("1970-01-01")},
                { mapOf(EPOCH_MILLIS, 1L), java.sql.Date.valueOf("1970-01-01")},
                { mapOf(EPOCH_MILLIS, 1710714535152L), java.sql.Date.valueOf("2024-03-18") },
                { mapOf(SQL_DATE, "1969-12-31"), java.sql.Date.valueOf("1969-12-31"), true},  // One day before epoch
                { mapOf(SQL_DATE, "1970-01-01"), java.sql.Date.valueOf("1970-01-01"), true},  // Epoch
                { mapOf(SQL_DATE, "1970-01-02"), java.sql.Date.valueOf("1970-01-02"), true},  // One day after epoch
                { mapOf(SQL_DATE, "X1970-01-01T00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(SQL_DATE, "1970-01-01X00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(SQL_DATE, "1970-01-01T00:00bad zone"), new IllegalArgumentException("Issue parsing date-time, other characters present: zone")},
                { mapOf(SQL_DATE, "1970-01-01 00:00:00Z"), java.sql.Date.valueOf("1970-01-01")},
                { mapOf("foo", "bar"), new IllegalArgumentException("Map to 'java.sql.Date' the map must include: [sqlDate], [value], [_v], or [epochMillis] as key with associated value")},
                { mapOf("foo", "bar"), new IllegalArgumentException("Map to 'java.sql.Date' the map must include: [sqlDate], [value], [_v], or [epochMillis] as key with associated value")},
        });
    }

    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);  // month is 0-based in Calendar
        return cal;
    }

    /**
     * Date
     */
    private static void loadDateTests() {
        TEST_DB.put(pair(Void.class, Date.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Date.class, Date.class), new Object[][] {
                { new Date(0), new Date(0)}
        });
        TEST_DB.put(pair(AtomicLong.class, Date.class), new Object[][]{
                {new AtomicLong(Long.MIN_VALUE), new Date(Long.MIN_VALUE), true},
                {new AtomicLong(-1), new Date(-1), true},
                {new AtomicLong(0), new Date(0), true},
                {new AtomicLong(1), new Date(1), true},
                {new AtomicLong(Long.MAX_VALUE), new Date(Long.MAX_VALUE), true},
        });
        TEST_DB.put(pair(Calendar.class, Date.class), new Object[][] {
                {cal(now), new Date(now), true }
        });
        TEST_DB.put(pair(Timestamp.class, Date.class), new Object[][]{
//                {new Timestamp(Long.MIN_VALUE), new Date(Long.MIN_VALUE), true},
                {new Timestamp(Integer.MIN_VALUE), new Date(Integer.MIN_VALUE), true},
                {new Timestamp(now), new Date(now), true},
                {new Timestamp(-1), new Date(-1), true},
                {new Timestamp(0), new Date(0), true},
                {new Timestamp(1), new Date(1), true},
                {new Timestamp(Integer.MAX_VALUE), new Date(Integer.MAX_VALUE), true},
//                {new Timestamp(Long.MAX_VALUE), new Date(Long.MAX_VALUE), true},
                {timestamp("1969-12-31T23:59:59.999Z"), new Date(-1), true},
                {timestamp("1970-01-01T00:00:00.000Z"), new Date(0), true},
                {timestamp("1970-01-01T00:00:00.001Z"), new Date(1), true},
        });
        TEST_DB.put(pair(LocalDate.class, Date.class), new Object[][] {
                {zdt("0000-01-01T00:00:00Z").toLocalDate(), new Date(-62167252739000L), true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDate(), new Date(-62167252739000L), true},
                {zdt("1969-12-31T14:59:59.999Z").toLocalDate(), new Date(-118800000L), true},
                {zdt("1969-12-31T15:00:00Z").toLocalDate(), new Date(-32400000L), true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDate(), new Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00Z").toLocalDate(), new Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDate(), new Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDate(), new Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDate(), new Date(-32400000L), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Date.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), new Date(-62167219200000L), true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDateTime(), new Date(-62167219199999L), true},
                {zdt("1969-12-31T23:59:59Z").toLocalDateTime(), new Date(-1000L), true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDateTime(), new Date(-1L), true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), new Date(0L), true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDateTime(), new Date(1L), true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDateTime(), new Date(999L), true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, Date.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z"), new Date(-62167219200000L), true},
                {zdt("0000-01-01T00:00:00.001Z"), new Date(-62167219199999L), true},
                {zdt("1969-12-31T23:59:59Z"), new Date(-1000), true},
                {zdt("1969-12-31T23:59:59.999Z"), new Date(-1), true},
                {zdt("1970-01-01T00:00:00Z"), new Date(0), true},
                {zdt("1970-01-01T00:00:00.001Z"), new Date(1), true},
                {zdt("1970-01-01T00:00:00.999Z"), new Date(999), true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Date.class), new Object[][]{
                {odt("1969-12-31T23:59:59Z"), new Date(-1000), true},
                {odt("1969-12-31T23:59:59.999Z"), new Date(-1), true},
                {odt("1970-01-01T00:00:00Z"), new Date(0), true},
                {odt("1970-01-01T00:00:00.001Z"), new Date(1), true},
                {odt("1970-01-01T00:00:00.999Z"), new Date(999), true},
        });
        TEST_DB.put(pair(String.class, Date.class), new Object[][]{
                {"", null},
                {"1969-12-31T23:59:59.999Z", new Date(-1), true},
                {"1970-01-01T00:00:00.000Z", new Date(0), true},
                {"1970-01-01T00:00:00.001Z", new Date(1), true},
        });
        TEST_DB.put(pair(Map.class, Date.class), new Object[][] {
                { mapOf(EPOCH_MILLIS, -1L), new Date(-1L)},
                { mapOf(EPOCH_MILLIS, 0L), new Date(0L)},
                { mapOf(EPOCH_MILLIS, 1L), new Date(1L)},
                { mapOf(EPOCH_MILLIS, 1710714535152L), new Date(1710714535152L)},
                { mapOf(DATE, "1970-01-01T00:00:00.000Z"), new Date(0L), true},
                { mapOf(DATE, "X1970-01-01T00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(DATE, "1970-01-01X00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(DATE, "1970-01-01T00:00bad zone"), new IllegalArgumentException("Issue parsing date-time, other characters present: zone")},
                { mapOf(DATE, "1970-01-01 00:00:00Z"), new Date(0L)},
                { mapOf(DATE, "X1970-01-01 00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(DATE, "X1970-01-01T00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf(DATE, "1970-01-01X00:00:00Z"), new IllegalArgumentException("Issue parsing date-time, other characters present: X")},
                { mapOf("foo", "bar"), new IllegalArgumentException("Map to 'Date' the map must include: [date], [value], [_v], or [epochMillis] as key with associated value")},
        });
    }

    /**
     * Calendar
     */
    private static void loadCalendarTests() {
        TEST_DB.put(pair(Void.class, Calendar.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Calendar.class, Calendar.class), new Object[][] {
                {cal(now), cal(now)}
        });
        TEST_DB.put(pair(Long.class, Calendar.class), new Object[][]{
                {-1L, cal(-1), true},
                {0L, cal(0), true},
                {1L, cal(1), true},
                {1707705480000L, (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }, true},
                {now, cal(now), true},
        });
        TEST_DB.put(pair(AtomicLong.class, Calendar.class), new Object[][]{
                {new AtomicLong(-1), cal(-1), true},
                {new AtomicLong(0), cal(0), true},
                {new AtomicLong(1), cal(1), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Calendar.class), new Object[][]{
                {new BigDecimal(-1), cal(-1000), true},
                {new BigDecimal("-0.001"), cal(-1), true},
                {BigDecimal.ZERO, cal(0), true},
                {new BigDecimal("0.001"), cal(1), true},
                {new BigDecimal(1), cal(1000), true},
        });
        TEST_DB.put(pair(Map.class, Calendar.class), new Object[][]{
                // Test with timezone name format
                {mapOf(CALENDAR, "2024-02-05T22:31:17.409+09:00[Asia/Tokyo]"), (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }, true},

                // Test with offset format
                {mapOf(CALENDAR, "2024-02-05T22:31:17.409+09:00"), (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+09:00"));
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }, false},  // re-writing it out, will go from offset back to zone name, hence not bi-directional
                
                // Test with no milliseconds
                {mapOf(CALENDAR, "2024-02-05T22:31:17+09:00[Asia/Tokyo]"), (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }, true},

                // Test New York timezone
                {mapOf(CALENDAR, "1970-01-01T00:00:00-05:00[America/New_York]"), (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
                    cal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }, true},

                // Test flexible parsing (space instead of T) - bidirectional false since it will normalize to T
                {mapOf(CALENDAR, "2024-02-05 22:31:17.409+09:00[Asia/Tokyo]"), (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }, false},

                // Test date with no time (will use start of day)
                {mapOf(CALENDAR, "2024-02-05[Asia/Tokyo]"), new IllegalArgumentException("time"), false}
        });
        TEST_DB.put(pair(ZonedDateTime.class, Calendar.class), new Object[][] {
                {zdt("1969-12-31T23:59:59.999Z"), cal(-1), true},
                {zdt("1970-01-01T00:00Z"), cal(0), true},
                {zdt("1970-01-01T00:00:00.001Z"), cal(1), true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Calendar.class), new Object[][] {
                {odt("1969-12-31T23:59:59.999Z"), cal(-1), true},
                {odt("1970-01-01T00:00Z"), cal(0), true},
                {odt("1970-01-01T00:00:00.001Z"), cal(1), true},
        });
        TEST_DB.put(pair(String.class, Calendar.class), new Object[][]{
                { "", null},
                {"0000-01-01T00:00:00Z", new IllegalArgumentException("Cannot convert to Calendar"), false},
                {"1970-01-01T08:59:59.999+09:00[Asia/Tokyo]", cal(-1), true},
                {"1970-01-01T09:00:00+09:00[Asia/Tokyo]", cal(0), true},
                {"1970-01-01T09:00:00.001+09:00[Asia/Tokyo]", cal(1), true},
                {"1970-01-01T08:59:59.999+09:00", (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT+09:00")));
                    cal.setTimeInMillis(-1);
                    return cal;
                }, false},  // zone offset vs zone name
                {"1970-01-01T09:00:00.000+09:00", (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT+09:00")));
                    cal.setTimeInMillis(0);
                    return cal;
                }, false},
                {"1970-01-01T09:00:00.001+09:00", (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT+09:00")));
                    cal.setTimeInMillis(1);
                    return cal;
                }, false},
        });
    }

    /**
     * Instant
     */
    private static void loadInstantTests() {
        TEST_DB.put(pair(Void.class, Instant.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Instant.class, Instant.class), new Object[][]{
                {Instant.parse("1996-12-24T00:00:00Z"), Instant.parse("1996-12-24T00:00:00Z")}
        });
        TEST_DB.put(pair(String.class, Instant.class), new Object[][]{
                {"0000-01-01T00:00:00Z", Instant.ofEpochMilli(-62167219200000L), true},
                {"0000-01-01T00:00:00.001Z", Instant.ofEpochMilli(-62167219199999L), true},
                {"1969-12-31T23:59:59.999Z", Instant.ofEpochMilli(-1), true},
                {"1970-01-01T00:00:00Z", Instant.ofEpochMilli(0), true},
                {"1970-01-01T00:00:00.001Z", Instant.ofEpochMilli(1), true},
                {"1970-01-01T00:00:01Z", Instant.ofEpochMilli(1000), true},
                {"1970-01-01T00:00:01.001Z", Instant.ofEpochMilli(1001), true},
                {"1970-01-01T00:01:00Z", Instant.ofEpochSecond(60), true},
                {"1970-01-01T00:01:01Z", Instant.ofEpochSecond(61), true},
                {"1970-01-01T00:00:00Z", Instant.ofEpochSecond(0, 0), true},
                {"1970-01-01T00:00:00.000000001Z", Instant.ofEpochSecond(0, 1), true},
                {"1970-01-01T00:00:00.999999999Z", Instant.ofEpochSecond(0, 999999999), true},
                {"1970-01-01T00:00:09.999999999Z", Instant.ofEpochSecond(0, 9999999999L), true},
                {"", null},
                {" ", null},
                {"1980-01-01T00:00:00Z", Instant.parse("1980-01-01T00:00:00Z"), true},
                {"2024-12-31T23:59:59.999999999Z", Instant.parse("2024-12-31T23:59:59.999999999Z"), true},
                {"Not even close", new IllegalArgumentException("Unable to parse")},
        });
        TEST_DB.put(pair(Calendar.class, Instant.class), new Object[][] {
                {cal(now), Instant.ofEpochMilli(now), true }
        });
        TEST_DB.put(pair(Date.class, Instant.class), new Object[][] {
                {new Date(-62135596800000L), Instant.ofEpochMilli(-62135596800000L), true },  // 0001-01-01
                {new Date(-1), Instant.ofEpochMilli(-1), true },
                {new Date(0), Instant.ofEpochMilli(0), true },                                 // 1970-01-01
                {new Date(1), Instant.ofEpochMilli(1), true },
                {new Date(253402300799999L), Instant.ofEpochMilli(253402300799999L), true },  // 9999-12-31 23:59:59.999
        });
        TEST_DB.put(pair(LocalDate.class, Instant.class), new Object[][] {  // Tokyo time zone is 9 hours offset (9 + 15 = 24)
                {LocalDate.parse("1969-12-31"), Instant.parse("1969-12-30T15:00:00Z"), true},
                {LocalDate.parse("1970-01-01"), Instant.parse("1969-12-31T15:00:00Z"), true},
                {LocalDate.parse("1970-01-02"), Instant.parse("1970-01-01T15:00:00Z"), true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Instant.class), new Object[][]{
                {odt("0000-01-01T00:00:00Z"), Instant.ofEpochMilli(-62167219200000L), true},
                {odt("0000-01-01T00:00:00.001Z"), Instant.ofEpochMilli(-62167219199999L), true},
                {odt("1969-12-31T23:59:59.999Z"), Instant.ofEpochMilli(-1), true},
                {odt("1970-01-01T00:00:00Z"), Instant.ofEpochMilli(0), true},
                {odt("1970-01-01T00:00:00.001Z"), Instant.ofEpochMilli(1), true},
                {odt("1970-01-01T00:00:01Z"), Instant.ofEpochMilli(1000), true},
                {odt("1970-01-01T00:00:01.001Z"), Instant.ofEpochMilli(1001), true},
                {odt("1970-01-01T00:01:00Z"), Instant.ofEpochSecond(60), true},
                {odt("1970-01-01T00:01:01Z"), Instant.ofEpochSecond(61), true},
                {odt("1970-01-01T00:00:00Z"), Instant.ofEpochSecond(0, 0), true},
                {odt("1970-01-01T00:00:00.000000001Z"), Instant.ofEpochSecond(0, 1), true},
                {odt("1970-01-01T00:00:00.999999999Z"), Instant.ofEpochSecond(0, 999999999), true},
                {odt("1970-01-01T00:00:09.999999999Z"), Instant.ofEpochSecond(0, 9999999999L), true},
                {odt("1980-01-01T00:00:00Z"), Instant.parse("1980-01-01T00:00:00Z"), true},
                {odt("2024-12-31T23:59:59.999999999Z"), Instant.parse("2024-12-31T23:59:59.999999999Z"), true},
        });
        TEST_DB.put(pair(Map.class, Instant.class), new Object[][] {
                { mapOf(INSTANT, "2024-03-10T11:07:00.123456789Z"), Instant.parse("2024-03-10T11:07:00.123456789Z"), true},
                { mapOf(INSTANT, "1969-12-31T23:59:59.999999999Z"), Instant.parse("1969-12-31T23:59:59.999999999Z"), true},
                { mapOf(INSTANT, "1970-01-01T00:00:00Z"), Instant.parse("1970-01-01T00:00:00Z"), true},
                { mapOf(INSTANT, "1970-01-01T00:00:00.000000001Z"), Instant.parse("1970-01-01T00:00:00.000000001Z"), true},
                { mapOf(VALUE, "1969-12-31T23:59:59.999Z"), Instant.parse("1969-12-31T23:59:59.999Z")},
                { mapOf(VALUE, "1970-01-01T00:00:00Z"), Instant.parse("1970-01-01T00:00:00Z")},
                { mapOf(V, "1970-01-01T00:00:00.001Z"), Instant.parse("1970-01-01T00:00:00.001Z")},
        });
    }

    /**
     * BigDecimal
     */
    private static void loadBigDecimalTests() {
        TEST_DB.put(pair(Void.class, BigDecimal.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(BigDecimal.class, BigDecimal.class), new Object[][]{
                {new BigDecimal("3.1415926535897932384626433"), new BigDecimal("3.1415926535897932384626433"), true}
        });
        TEST_DB.put(pair(AtomicInteger.class, BigDecimal.class), new Object[][] {
                { new AtomicInteger(Integer.MIN_VALUE), BigDecimal.valueOf(Integer.MIN_VALUE), true},
                { new AtomicInteger(-1), BigDecimal.valueOf(-1), true},
                { new AtomicInteger(0), BigDecimal.ZERO, true},
                { new AtomicInteger(1), BigDecimal.valueOf(1), true},
                { new AtomicInteger(Integer.MAX_VALUE), BigDecimal.valueOf(Integer.MAX_VALUE), true},
        });
        TEST_DB.put(pair(AtomicLong.class, BigDecimal.class), new Object[][] {
                { new AtomicLong(Long.MIN_VALUE), BigDecimal.valueOf(Long.MIN_VALUE), true},
                { new AtomicLong(-1), BigDecimal.valueOf(-1), true},
                { new AtomicLong(0), BigDecimal.ZERO, true},
                { new AtomicLong(1), BigDecimal.valueOf(1), true},
                { new AtomicLong(Long.MAX_VALUE), BigDecimal.valueOf(Long.MAX_VALUE), true},
        });
        TEST_DB.put(pair(Date.class, BigDecimal.class), new Object[][]{
                {date("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200"), true},
                {date("0000-01-01T00:00:00.001Z"), new BigDecimal("-62167219199.999"), true},
                {date("1969-12-31T23:59:59.999Z"), new BigDecimal("-0.001"), true},
                {date("1970-01-01T00:00:00Z"), BigDecimal.ZERO, true},
                {date("1970-01-01T00:00:00.001Z"), new BigDecimal("0.001"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, java.sql.Date.class), new Object[][] {
                // Bidirectional tests (true) - all representing midnight Tokyo time
                {new BigDecimal("1686754800"), java.sql.Date.valueOf("2023-06-15"), true},  // 2023-06-15 00:00 Tokyo
                {new BigDecimal("-32400"), java.sql.Date.valueOf("1970-01-01"), true},      // 1970-01-01 00:00 Tokyo
                {new BigDecimal("-118800"), java.sql.Date.valueOf("1969-12-31"), true},     // 1969-12-31 00:00 Tokyo

                // Pre-epoch dates
                {new BigDecimal("-86400"), java.sql.Date.valueOf("1969-12-31"), false},    // 1 day before epoch
                {new BigDecimal("-172800"), java.sql.Date.valueOf("1969-12-30"), false},   // 2 days before epoch

                // Epoch
                {new BigDecimal("0"), java.sql.Date.valueOf("1970-01-01"), false},         // epoch
                {new BigDecimal("86400"), java.sql.Date.valueOf("1970-01-02"), false},     // 1 day after epoch

                // Recent dates
                {new BigDecimal("1686787200"), java.sql.Date.valueOf("2023-06-15"), false},

                // Fractional seconds (should truncate to same date)
                {new BigDecimal("86400.123"), java.sql.Date.valueOf("1970-01-02"), false},
                {new BigDecimal("86400.999"), java.sql.Date.valueOf("1970-01-02"), false},

                // Scientific notation
                {new BigDecimal("8.64E4"), java.sql.Date.valueOf("1970-01-02"), false},    // 1 day after epoch
                {new BigDecimal("1.686787200E9"), java.sql.Date.valueOf("2023-06-15"), false}
        });
        TEST_DB.put(pair(LocalDateTime.class, BigDecimal.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), new BigDecimal("-62167219200.0"), true},
                {zdt("0000-01-01T00:00:00.000000001Z").toLocalDateTime(), new BigDecimal("-62167219199.999999999"), true},
                {zdt("1969-12-31T00:00:00Z").toLocalDateTime(), new BigDecimal("-86400"), true},
                {zdt("1969-12-31T00:00:00.000000001Z").toLocalDateTime(), new BigDecimal("-86399.999999999"), true},
                {zdt("1969-12-31T23:59:59.999999999Z").toLocalDateTime(), new BigDecimal("-0.000000001"), true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), BigDecimal.ZERO, true},
                {zdt("1970-01-01T00:00:00.000000001Z").toLocalDateTime(), new BigDecimal("0.000000001"), true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, BigDecimal.class), new Object[][]{   // no reverse due to .toString adding zone offset
                {odt("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200")},
                {odt("0000-01-01T00:00:00.000000001Z"), new BigDecimal("-62167219199.999999999")},
                {odt("1969-12-31T23:59:59.999999999Z"), new BigDecimal("-0.000000001"), true},
                {odt("1970-01-01T00:00:00Z"), BigDecimal.ZERO, true},
                {odt("1970-01-01T00:00:00.000000001Z"), new BigDecimal(".000000001"), true},

        });
        TEST_DB.put(pair(Duration.class, BigDecimal.class), new Object[][]{
                {Duration.ofSeconds(-1, -1), new BigDecimal("-1.000000001"), true},
                {Duration.ofSeconds(-1), new BigDecimal("-1"), true},
                {Duration.ofSeconds(0), BigDecimal.ZERO, true},
                {Duration.ofNanos(0), BigDecimal.ZERO, true},
                {Duration.ofSeconds(1), new BigDecimal("1"), true},
                {Duration.ofNanos(1), new BigDecimal("0.000000001"), true},
                {Duration.ofNanos(1_000_000_000), new BigDecimal("1"), true},
                {Duration.ofNanos(2_000_000_001), new BigDecimal("2.000000001"), true},
                {Duration.ofSeconds(3, 6), new BigDecimal("3.000000006"), true},
                {Duration.ofSeconds(10, 9), new BigDecimal("10.000000009"), true},
                {Duration.ofSeconds(100), new BigDecimal("100"), true},
                {Duration.ofDays(1), new BigDecimal("86400"), true},
        });
        TEST_DB.put(pair(Instant.class, BigDecimal.class), new Object[][]{      // JDK 1.8 cannot handle the format +01:00 in Instant.parse().  JDK11+ handles it fine.
                {Instant.parse("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200.0"), true},
                {Instant.parse("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200.0"), true},
                {Instant.parse("0000-01-01T00:00:00.000000001Z"), new BigDecimal("-62167219199.999999999"), true},
                {Instant.parse("1969-12-31T00:00:00Z"), new BigDecimal("-86400"), true},
                {Instant.parse("1969-12-31T00:00:00.999999999Z"), new BigDecimal("-86399.000000001"), true},
                {Instant.parse("1969-12-31T23:59:59.999999999Z"), new BigDecimal("-0.000000001"), true},
                {Instant.parse("1970-01-01T00:00:00Z"), BigDecimal.ZERO, true},
                {Instant.parse("1970-01-01T00:00:00.000000001Z"), new BigDecimal("0.000000001"), true},
                {Instant.parse("1970-01-02T00:00:00Z"), new BigDecimal("86400"), true},
                {Instant.parse("1970-01-02T00:00:00.000000001Z"), new BigDecimal("86400.000000001"), true},
        });
        TEST_DB.put(pair(String.class, BigDecimal.class), new Object[][]{
                {"", BigDecimal.ZERO},
                {"-1", new BigDecimal("-1"), true},
                {"-1", new BigDecimal("-1.0"), true},
                {"0", BigDecimal.ZERO, true},
                {"0", new BigDecimal("0.0"), true},
                {"1", new BigDecimal("1.0"), true},
                {"3.141519265358979323846264338", new BigDecimal("3.141519265358979323846264338"), true},
                {"1.gf.781", new IllegalArgumentException("not parseable")},
        });
        TEST_DB.put(pair(Map.class, BigDecimal.class), new Object[][]{
                {mapOf("_v", "0"), BigDecimal.ZERO},
                {mapOf("_v", BigDecimal.valueOf(0)), BigDecimal.ZERO, true},
                {mapOf("_v", BigDecimal.valueOf(1.1)), BigDecimal.valueOf(1.1), true},
        });
    }

    /**
     * BigInteger
     */
    private static void loadBigIntegerTests() {
        TEST_DB.put(pair(Void.class, BigInteger.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Float.class, BigInteger.class), new Object[][]{
                {-1.99f, BigInteger.valueOf(-1)},
                {-1f, BigInteger.valueOf(-1), true},
                {0f, BigInteger.ZERO, true},
                {1f, BigInteger.valueOf(1), true},
                {1.1f, BigInteger.valueOf(1)},
                {1.99f, BigInteger.valueOf(1)},
                {1.0e6f, new BigInteger("1000000"), true},
                {-16777216f, BigInteger.valueOf(-16777216), true},
                {16777216f, BigInteger.valueOf(16777216), true},
        });
        TEST_DB.put(pair(Double.class, BigInteger.class), new Object[][]{
                {-1.0, BigInteger.valueOf(-1), true},
                {0.0, BigInteger.ZERO, true},
                {1.0, new BigInteger("1"), true},
                {1.0e9, new BigInteger("1000000000"), true},
                {-9007199254740991.0, BigInteger.valueOf(-9007199254740991L), true},
                {9007199254740991.0, BigInteger.valueOf(9007199254740991L), true},
        });
        TEST_DB.put(pair(BigInteger.class, BigInteger.class), new Object[][]{
                {new BigInteger("16"), BigInteger.valueOf(16), true},
        });
        TEST_DB.put(pair(BigDecimal.class, BigInteger.class), new Object[][]{
                {BigDecimal.ZERO, BigInteger.ZERO, true},
                {BigDecimal.valueOf(-1), BigInteger.valueOf(-1), true},
                {BigDecimal.valueOf(-1.1), BigInteger.valueOf(-1)},
                {BigDecimal.valueOf(-1.9), BigInteger.valueOf(-1)},
                {BigDecimal.valueOf(1.9), BigInteger.valueOf(1)},
                {BigDecimal.valueOf(1.1), BigInteger.valueOf(1)},
                {BigDecimal.valueOf(1.0e6), new BigInteger("1000000")},
                {BigDecimal.valueOf(-16777216), BigInteger.valueOf(-16777216), true},
        });
        TEST_DB.put(pair(Date.class, BigInteger.class), new Object[][]{
                {date("0000-01-01T00:00:00Z"), new BigInteger("-62167219200000000000"), true},
                {date("0001-02-18T19:58:01Z"), new BigInteger("-62131377719000000000"), true},
                {date("1969-12-31T23:59:59Z"), BigInteger.valueOf(-1_000_000_000), true},
                {date("1969-12-31T23:59:59.1Z"), BigInteger.valueOf(-900000000), true},
                {date("1969-12-31T23:59:59.9Z"), BigInteger.valueOf(-100000000), true},
                {date("1970-01-01T00:00:00Z"), BigInteger.ZERO, true},
                {date("1970-01-01T00:00:00.1Z"), BigInteger.valueOf(100000000), true},
                {date("1970-01-01T00:00:00.9Z"), BigInteger.valueOf(900000000), true},
                {date("1970-01-01T00:00:01Z"), BigInteger.valueOf(1000000000), true},
                {date("9999-02-18T19:58:01Z"), new BigInteger("253374983881000000000"), true},
        });
        TEST_DB.put(pair(java.sql.Date.class, BigInteger.class), new Object[][]{
                // Bidirectional tests (true) - all at midnight Tokyo time
                {java.sql.Date.valueOf("1888-01-02"),
                        BigInteger.valueOf(Instant.parse("1888-01-01T15:00:00Z").toEpochMilli()).multiply(BigInteger.valueOf(1_000_000)), true},  // 1888-01-02 00:00 Tokyo

                {java.sql.Date.valueOf("1969-12-31"),
                        BigInteger.valueOf(Instant.parse("1969-12-30T15:00:00Z").toEpochMilli()).multiply(BigInteger.valueOf(1_000_000)), true},  // 1969-12-31 00:00 Tokyo

                {java.sql.Date.valueOf("1970-01-01"),
                        BigInteger.valueOf(Instant.parse("1969-12-31T15:00:00Z").toEpochMilli()).multiply(BigInteger.valueOf(1_000_000)), true},  // 1970-01-01 00:00 Tokyo

                {java.sql.Date.valueOf("1970-01-02"),
                        BigInteger.valueOf(Instant.parse("1970-01-01T15:00:00Z").toEpochMilli()).multiply(BigInteger.valueOf(1_000_000)), true},  // 1970-01-02 00:00 Tokyo

                {java.sql.Date.valueOf("2023-06-15"),
                        BigInteger.valueOf(Instant.parse("2023-06-14T15:00:00Z").toEpochMilli()).multiply(BigInteger.valueOf(1_000_000)), true}   // 2023-06-15 00:00 Tokyo
        });
        TEST_DB.put(pair(Timestamp.class, BigInteger.class), new Object[][]{
                // Timestamp uses a proleptic Gregorian calendar starting at year 1, hence no 0000 tests.
                {timestamp("0001-01-01T00:00:00.000000000Z"), new BigInteger("-62135596800000000000"), true},
                {timestamp("0001-02-18T19:58:01.000000000Z"), new BigInteger("-62131377719000000000"), true},
                {timestamp("1969-12-31T23:59:59.000000000Z"), BigInteger.valueOf(-1000000000), true},
                {timestamp("1969-12-31T23:59:59.000000001Z"), BigInteger.valueOf(-999999999), true},
                {timestamp("1969-12-31T23:59:59.100000000Z"), BigInteger.valueOf(-900000000), true},
                {timestamp("1969-12-31T23:59:59.900000000Z"), BigInteger.valueOf(-100000000), true},
                {timestamp("1969-12-31T23:59:59.999999999Z"), BigInteger.valueOf(-1), true},
                {timestamp("1970-01-01T00:00:00.000000000Z"), BigInteger.ZERO, true},
                {timestamp("1970-01-01T00:00:00.000000001Z"), BigInteger.valueOf(1), true},
                {timestamp("1970-01-01T00:00:00.100000000Z"), BigInteger.valueOf(100000000), true},
                {timestamp("1970-01-01T00:00:00.900000000Z"), BigInteger.valueOf(900000000), true},
                {timestamp("1970-01-01T00:00:00.999999999Z"), BigInteger.valueOf(999999999), true},
                {timestamp("1970-01-01T00:00:01.000000000Z"), BigInteger.valueOf(1000000000), true},
                {timestamp("9999-02-18T19:58:01.000000000Z"), new BigInteger("253374983881000000000"), true},
        });
        TEST_DB.put(pair(Instant.class, BigInteger.class), new Object[][]{
                {Instant.parse("0000-01-01T00:00:00.000000000Z"), new BigInteger("-62167219200000000000"), true},
                {Instant.parse("0000-01-01T00:00:00.000000001Z"), new BigInteger("-62167219199999999999"), true},
                {Instant.parse("1969-12-31T23:59:59.000000000Z"), BigInteger.valueOf(-1000000000), true},
                {Instant.parse("1969-12-31T23:59:59.000000001Z"), BigInteger.valueOf(-999999999), true},
                {Instant.parse("1969-12-31T23:59:59.100000000Z"), BigInteger.valueOf(-900000000), true},
                {Instant.parse("1969-12-31T23:59:59.900000000Z"), BigInteger.valueOf(-100000000), true},
                {Instant.parse("1969-12-31T23:59:59.999999999Z"), BigInteger.valueOf(-1), true},
                {Instant.parse("1970-01-01T00:00:00.000000000Z"), BigInteger.ZERO, true},
                {Instant.parse("1970-01-01T00:00:00.000000001Z"), BigInteger.valueOf(1), true},
                {Instant.parse("1970-01-01T00:00:00.100000000Z"), BigInteger.valueOf(100000000), true},
                {Instant.parse("1970-01-01T00:00:00.900000000Z"), BigInteger.valueOf(900000000), true},
                {Instant.parse("1970-01-01T00:00:00.999999999Z"), BigInteger.valueOf(999999999), true},
                {Instant.parse("1970-01-01T00:00:01.000000000Z"), BigInteger.valueOf(1000000000), true},
                {Instant.parse("9999-02-18T19:58:01.000000000Z"), new BigInteger("253374983881000000000"), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, BigInteger.class), new Object[][]{
                {ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-62167252739000000000"), true},
                {ZonedDateTime.parse("0000-01-01T00:00:00.000000001Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-62167252738999999999"), true},
                {ZonedDateTime.parse("1969-12-31T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-118800000000000"), true},
                {ZonedDateTime.parse("1969-12-31T00:00:00.000000001Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-118799999999999"), true},
                {ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-32400000000001"), true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigInteger("-32400000000000"), true},
                {zdt("1969-12-31T23:59:59.999999999Z").toLocalDateTime(), new BigInteger("-1"), true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), BigInteger.ZERO, true},
                {zdt("1970-01-01T00:00:00.000000001Z").toLocalDateTime(), new BigInteger("1"), true},
        });
        TEST_DB.put(pair(Calendar.class, BigInteger.class), new Object[][]{
                {cal(-1), BigInteger.valueOf(-1000000), true},
                {cal(0), BigInteger.ZERO, true},
                {cal(1), BigInteger.valueOf(1000000), true},
        });
        TEST_DB.put(pair(Map.class, BigInteger.class), new Object[][]{
                {mapOf("_v", 0), BigInteger.ZERO},
                {mapOf("_v", BigInteger.valueOf(0)), BigInteger.ZERO, true},
                {mapOf("_v", BigInteger.valueOf(1)), BigInteger.valueOf(1), true},
        });
        TEST_DB.put(pair(String.class, BigInteger.class), new Object[][]{
                {"0", BigInteger.ZERO},
                {"0.0", BigInteger.ZERO},
                {"rock", new IllegalArgumentException("Value 'rock' not parseable as a BigInteger value")},
                {"", BigInteger.ZERO},
                {" ", BigInteger.ZERO},
        });
        TEST_DB.put(pair(Map.class, AtomicInteger.class), new Object[][]{
                {mapOf("_v", 0), new AtomicInteger(0)},
                {mapOf("_v", new AtomicInteger(0)), new AtomicInteger(0)},
                {mapOf("_v", new AtomicInteger(1)), new AtomicInteger(1)},
        });
        TEST_DB.put(pair(OffsetDateTime.class, BigInteger.class), new Object[][]{
                {odt("0000-01-01T00:00:00Z"), new BigInteger("-62167219200000000000")},
                {odt("0000-01-01T00:00:00.000000001Z"), new BigInteger("-62167219199999999999")},
                {odt("1969-12-31T23:59:59.999999999Z"), new BigInteger("-1"), true},
                {odt("1970-01-01T00:00:00Z"), BigInteger.ZERO, true},
                {odt("1970-01-01T00:00:00.000000001Z"), new BigInteger("1"), true},
        });
    }

    /**
     * Character/char
     */
    private static void loadCharacterTests() {
        TEST_DB.put(pair(Void.class, char.class), new Object[][]{
                {null, (char) 0},
        });
        TEST_DB.put(pair(Void.class, Character.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Short.class, Character.class), new Object[][]{
                {(short) -1, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {(short) 0, (char) 0, true},
                {(short) 1, (char) 1, true},
                {(short) 49, '1', true},
                {(short) 48, '0', true},
                {Short.MAX_VALUE, (char) Short.MAX_VALUE, true},
        });
        TEST_DB.put(pair(Integer.class, Character.class), new Object[][]{
                {-1, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0, (char) 0, true},
                {1, (char) 1, true},
                {49, '1', true},
                {48, '0', true},
                {65535, (char) 65535, true},
                {65536, new IllegalArgumentException("Value '65536' out of range to be converted to character")},

        });
        TEST_DB.put(pair(Long.class, Character.class), new Object[][]{
                {-1L, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0L, (char) 0L, true},
                {1L, (char) 1L, true},
                {48L, '0', true},
                {49L, '1', true},
                {65535L, (char) 65535L, true},
                {65536L, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Float.class, Character.class), new Object[][]{
                {-1f, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0f, (char) 0, true},
                {1f, (char) 1, true},
                {49f, '1', true},
                {48f, '0', true},
                {65535f, (char) 65535f, true},
                {65536f, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Double.class, Character.class), new Object[][]{
                {-1.0, new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {0.0, (char) 0, true},
                {1.0, (char) 1, true},
                {48.0, '0', true},
                {49.0, '1', true},
                {65535.0, (char) 65535.0, true},
                {65536.0, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Character.class, Character.class), new Object[][]{
                {(char) 0, (char) 0, true},
                {(char) 1, (char) 1, true},
                {(char) 65535, (char) 65535, true},
        });
        TEST_DB.put(pair(AtomicInteger.class, Character.class), new Object[][]{
                {new AtomicInteger(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {new AtomicInteger(0), (char) 0, true},
                {new AtomicInteger(1), (char) 1, true},
                {new AtomicInteger(65535), (char) 65535},
                {new AtomicInteger(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(AtomicLong.class, Character.class), new Object[][]{
                {new AtomicLong(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {new AtomicLong(0), (char) 0, true},
                {new AtomicLong(1), (char) 1, true},
                {new AtomicLong(65535), (char) 65535},
                {new AtomicLong(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(BigInteger.class, Character.class), new Object[][]{
                {BigInteger.valueOf(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {BigInteger.ZERO, (char) 0, true},
                {BigInteger.valueOf(1), (char) 1, true},
                {BigInteger.valueOf(65535), (char) 65535, true},
                {BigInteger.valueOf(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(BigDecimal.class, Character.class), new Object[][]{
                {BigDecimal.valueOf(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {BigDecimal.ZERO, (char) 0, true},
                {BigDecimal.valueOf(1), (char) 1, true},
                {BigDecimal.valueOf(65535), (char) 65535, true},
                {BigDecimal.valueOf(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Map.class, Character.class), new Object[][]{
                {mapOf("_v", -1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {mapOf("value", 0), (char) 0},
                {mapOf("_v", 1), (char) 1},
                {mapOf("_v", 65535), (char) 65535},
                {mapOf("_v", mapOf("_v", 65535)), (char) 65535},
                {mapOf("_v", "0"), (char) 48},
                {mapOf("_v", 65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
                {mapOf(V, (char)0), (char) 0, true},
                {mapOf(V, (char)1), (char) 1, true},
                {mapOf(V, (char)65535), (char) 65535, true},
                {mapOf(V, '0'), (char) 48, true},
                {mapOf(V, '1'), (char) 49, true},
        });
        TEST_DB.put(pair(String.class, Character.class), new Object[][]{
                {"", (char) 0},
                {" ", (char) 32, true},
                {"0", '0', true},
                {"1", '1', true},
                {"A", 'A', true},
                {"{", '{', true},
                {"\uD7FF", '\uD7FF', true},
                {"\uFFFF", '\uFFFF', true},
                {"FFFZ", new IllegalArgumentException("Unable to parse 'FFFZ' as a char/Character. Invalid Unicode escape sequence.FFFZ")},
        });
    }

    /**
     * Boolean/boolean
     */
    private static void loadBooleanTests() {
        TEST_DB.put(pair(Void.class, boolean.class), new Object[][]{
                {null, false},
        });
        TEST_DB.put(pair(Void.class, Boolean.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Boolean.class), new Object[][]{
                {(byte) -2, true},
                {(byte) -1, true},
                {(byte) 0, false, true},
                {(byte) 1, true, true},
                {(byte) 2, true},
        });
        TEST_DB.put(pair(Short.class, Boolean.class), new Object[][]{
                {(short) -2, true},
                {(short) -1, true },
                {(short) 0, false, true},
                {(short) 1, true, true},
                {(short) 2, true},
        });
        TEST_DB.put(pair(Integer.class, Boolean.class), new Object[][]{
                {-2, true},
                {-1, true},
                {0, false, true},
                {1, true, true},
                {2, true},
        });
        TEST_DB.put(pair(Long.class, Boolean.class), new Object[][]{
                {-2L, true},
                {-1L, true},
                {0L, false, true},
                {1L, true, true},
                {2L, true},
        });
        TEST_DB.put(pair(Float.class, Boolean.class), new Object[][]{
                {-2f, true},
                {-1.5f, true},
                {-1f, true},
                {0f, false, true},
                {1f, true, true},
                {1.5f, true},
                {2f, true},
        });
        TEST_DB.put(pair(Double.class, Boolean.class), new Object[][]{
                {-2.0, true},
                {-1.5, true},
                {-1.0, true},
                {0.0, false, true},
                {1.0, true, true},
                {1.5, true},
                {2.0, true},
        });
        TEST_DB.put(pair(Boolean.class, Boolean.class), new Object[][]{
                {true, true},
                {false, false},
        });
        TEST_DB.put(pair(Character.class, Boolean.class), new Object[][]{
                {(char) 0, false, true},
                {(char) 1, true, true},
                {'0', false},
                {'1', true},
                {'2', false},
                {'a', false},
                {'z', false},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Boolean.class), new Object[][]{
                {new AtomicBoolean(true), true, true},
                {new AtomicBoolean(false), false, true},
        });
        TEST_DB.put(pair(AtomicInteger.class, Boolean.class), new Object[][]{
                {new AtomicInteger(-2), true},
                {new AtomicInteger(-1), true},
                {new AtomicInteger(0), false, true},
                {new AtomicInteger(1), true, true},
                {new AtomicInteger(2), true},
        });
        TEST_DB.put(pair(AtomicLong.class, Boolean.class), new Object[][]{
                {new AtomicLong(-2), true},
                {new AtomicLong(-1), true},
                {new AtomicLong(0), false, true},
                {new AtomicLong(1), true, true},
                {new AtomicLong(2), true},
        });
        TEST_DB.put(pair(BigInteger.class, Boolean.class), new Object[][]{
                {BigInteger.valueOf(-2), true},
                {BigInteger.valueOf(-1), true},
                {BigInteger.ZERO, false, true, true},
                {BigInteger.valueOf(1), true, true},
                {BigInteger.valueOf(2), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Boolean.class), new Object[][]{
                {BigDecimal.valueOf(-2L), true},
                {BigDecimal.valueOf(-1L), true},
                {BigDecimal.valueOf(0L), false, true},
                {BigDecimal.valueOf(1L), true, true},
                {BigDecimal.valueOf(2L), true},
        });
        TEST_DB.put(pair(Map.class, Boolean.class), new Object[][]{
                {mapOf(V, 16), true},
                {mapOf(V, 0), false},
                {mapOf(V, "0"), false},
                {mapOf(V, "1"), true},
                {mapOf(V, mapOf(V, 5.0)), true},
                {mapOf(V, true), true, true},
                {mapOf(V, false), false, true},
        });
        TEST_DB.put(pair(String.class, Boolean.class), new Object[][]{
                {"0", false},
                {"false", false, true},
                {"FaLse", false},
                {"FALSE", false},
                {"F", false},
                {"f", false},
                {"1", true},
                {"true", true, true},
                {"TrUe", true},
                {"TRUE", true},
                {"T", true},
                {"t", true},
                {"Bengals", false},
        });
    }

    /**
     * Double/double
     */
    private static void loadDoubleTests() {
        TEST_DB.put(pair(Void.class, double.class), new Object[][]{
                {null, 0.0}
        });
        TEST_DB.put(pair(Void.class, Double.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Integer.class, Double.class), new Object[][]{
                {-1, -1.0},
                {0, 0.0},
                {1, 1.0},
                {2147483647, 2147483647.0},
                {-2147483648, -2147483648.0},
        });
        TEST_DB.put(pair(Long.class, Double.class), new Object[][]{
                {-1L, -1.0},
                {0L, 0.0},
                {1L, 1.0},
                {9007199254740991L, 9007199254740991.0},
                {-9007199254740991L, -9007199254740991.0},
        });
        TEST_DB.put(pair(Float.class, Double.class), new Object[][]{
                {-1f, -1.0},
                {0f, 0.0},
                {1f, 1.0},
                {Float.MIN_VALUE, (double) Float.MIN_VALUE},
                {Float.MAX_VALUE, (double) Float.MAX_VALUE},
                {-Float.MAX_VALUE, (double) -Float.MAX_VALUE},
        });
        TEST_DB.put(pair(Double.class, Double.class), new Object[][]{
                {-1.0, -1.0},
                {-1.99, -1.99},
                {-1.1, -1.1},
                {0.0, 0.0},
                {1.0, 1.0},
                {1.1, 1.1},
                {1.999, 1.999},
                {Double.MIN_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MAX_VALUE},
                {-Double.MAX_VALUE, -Double.MAX_VALUE},
        });
        TEST_DB.put(pair(Duration.class, Double.class), new Object[][]{
                {Duration.ofSeconds(-1, -1), -1.000000001, true},
                {Duration.ofSeconds(-1), -1.0, true},
                {Duration.ofSeconds(0), 0.0, true},
                {Duration.ofSeconds(1), 1.0, true},
                {Duration.ofSeconds(3, 6), 3.000000006, true},
                {Duration.ofNanos(-1), -0.000000001, true},
                {Duration.ofNanos(1), 0.000000001, true},
                {Duration.ofNanos(1_000_000_000), 1.0, true},
                {Duration.ofNanos(2_000_000_001), 2.000000001, true},
                {Duration.ofSeconds(10, 9), 10.000000009, true},
                {Duration.ofDays(1), 86400d, true},
        });
        TEST_DB.put(pair(Instant.class, Double.class), new Object[][]{      // JDK 1.8 cannot handle the format +01:00 in Instant.parse().  JDK11+ handles it fine.
                {Instant.parse("0000-01-01T00:00:00Z"), -62167219200.0, true},
                {Instant.parse("1969-12-31T00:00:00Z"), -86400d, true},
                {Instant.parse("1969-12-31T00:00:00.999999999Z"), -86399.000000001, true},
                {Instant.parse("1969-12-31T23:59:59.999999999Z"), -0.000000001, true },
                {Instant.parse("1970-01-01T00:00:00Z"), 0.0, true},
                {Instant.parse("1970-01-01T00:00:00.000000001Z"), 0.000000001, true},
                {Instant.parse("1970-01-02T00:00:00Z"), 86400d, true},
                {Instant.parse("1970-01-02T00:00:00.000000001Z"), 86400.000000001, true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Double.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), -62167219200.0, true},
                {zdt("1969-12-31T23:59:59.999999998Z").toLocalDateTime(), -0.000000002, true},
                {zdt("1969-12-31T23:59:59.999999999Z").toLocalDateTime(), -0.000000001, true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), 0.0, true},
                {zdt("1970-01-01T00:00:00.000000001Z").toLocalDateTime(), 0.000000001, true},
                {zdt("1970-01-01T00:00:00.000000002Z").toLocalDateTime(), 0.000000002, true},
        });
        TEST_DB.put(pair(Date.class, Double.class), new Object[][]{
                {new Date(Long.MIN_VALUE), (double) Long.MIN_VALUE / 1000d, true},
                {new Date(Integer.MIN_VALUE), (double) Integer.MIN_VALUE / 1000d, true},
                {new Date(0), 0.0, true},
                {new Date(now), (double) now / 1000d, true},
                {date("2024-02-18T06:31:55.987654321Z"), 1708237915.987, true},    // Date only has millisecond resolution
                {date("2024-02-18T06:31:55.123456789Z"), 1708237915.123, true},      // Date only has millisecond resolution
                {new Date(Integer.MAX_VALUE), (double) Integer.MAX_VALUE / 1000d, true},
                {new Date(Long.MAX_VALUE), (double) Long.MAX_VALUE / 1000d, true},
        });
        TEST_DB.put(pair(Timestamp.class, Double.class), new Object[][]{
                {new Timestamp(0), 0.0, true},
                {new Timestamp((long) (now * 1000d)), (double) now, true},
                {timestamp("1969-12-31T00:00:00Z"), -86400d, true},
                {timestamp("1969-12-31T00:00:00.000000001Z"), -86399.999999999, true},
                {timestamp("1969-12-31T23:59:59.999999999Z"), -0.000000001, true},
                {timestamp("1970-01-01T00:00:00Z"), 0.0, true},
                {timestamp("1970-01-01T00:00:00.000000001Z"), 0.000000001, true},
                {timestamp("1970-01-01T00:00:00.9Z"), 0.9, true},
                {timestamp("1970-01-01T00:00:00.999999999Z"), 0.999999999, true},
        });
        TEST_DB.put(pair(Calendar.class, Double.class), new Object[][]{
                {cal(-1000), -1.0, true},
                {cal(-1), -0.001, true},
                {cal(0), 0.0, true},
                {cal(1), 0.001, true},
                {cal(1000), 1.0, true},
        });
        TEST_DB.put(pair(BigDecimal.class, Double.class), new Object[][]{
                {new BigDecimal("-1"), -1.0, true},
                {new BigDecimal("-1.1"), -1.1, true},
                {new BigDecimal("-1.9"), -1.9, true},
                {BigDecimal.ZERO, 0.0, true},
                {new BigDecimal("1"), 1.0, true},
                {new BigDecimal("1.1"), 1.1, true},
                {new BigDecimal("1.9"), 1.9, true},
                {new BigDecimal("-9007199254740991"), -9007199254740991.0, true},
                {new BigDecimal("9007199254740991"), 9007199254740991.0, true},
        });
        TEST_DB.put(pair(Map.class, Double.class), new Object[][]{
                {mapOf("_v", "-1"), -1.0},
                {mapOf("_v", -1.0), -1.0, true},
                {mapOf("value", "-1"), -1.0},
                {mapOf("value", -1L), -1.0},

                {mapOf("_v", "0"), 0.0},
                {mapOf("_v", 0.0), 0.0, true},

                {mapOf("_v", "1"), 1.0},
                {mapOf("_v", 1.0), 1.0, true},

                {mapOf("_v", "-9007199254740991"), -9007199254740991.0},
                {mapOf("_v", -9007199254740991L), -9007199254740991.0},

                {mapOf("_v", "9007199254740991"), 9007199254740991.0},
                {mapOf("_v", 9007199254740991L), 9007199254740991.0},

                {mapOf("_v", mapOf("_v", -9007199254740991L)), -9007199254740991.0},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Double.class), new Object[][]{
                {"-1.0", -1.0, true},
                {"-1.1", -1.1},
                {"-1.9", -1.9},
                {"0", 0.0, true},
                {"1.0", 1.0, true},
                {"1.1", 1.1, true},
                {"1.9", 1.9, true},
                {"-2147483648", -2147483648.0},
                {"2147483647", 2147483647.0},
                {"", 0.0},
                {" ", 0.0},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a double")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a double")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a double")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a double")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a double")},
                {"4.9E-324", Double.MIN_VALUE, true},
                {"-1.7976931348623157E308", -Double.MAX_VALUE, true},
                {"1.7976931348623157E308", Double.MAX_VALUE},
                {"1.23456789E8", 123456789.0, true},
                {"1.23456789E-7", 0.000000123456789, true},
                {"12345.0", 12345.0, true},
                {"1.2345E-4", 0.00012345, true},

        });
        TEST_DB.put(pair(Year.class, Double.class), new Object[][]{
                {Year.of(2024), 2024.0}
        });
    }

    /**
     * Float/float
     */
    private static void loadFloatTests() {
        TEST_DB.put(pair(Void.class, float.class), new Object[][]{
                {null, 0.0f}
        });
        TEST_DB.put(pair(Void.class, Float.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Short.class, Float.class), new Object[][]{
                {(short) -1, -1f},
                {(short) 0, 0f},
                {(short) 1, 1f},
                {Short.MIN_VALUE, (float) Short.MIN_VALUE},
                {Short.MAX_VALUE, (float) Short.MAX_VALUE},
        });
        TEST_DB.put(pair(Integer.class, Float.class), new Object[][]{
                {-1, -1f},
                {0, 0f},
                {1, 1f},
                {16777216, 16_777_216f},
                {-16777216, -16_777_216f},
        });
        TEST_DB.put(pair(Long.class, Float.class), new Object[][]{
                {-1L, -1f},
                {0L, 0f},
                {1L, 1f},
                {16777216L, 16_777_216f},
                {-16777216L, -16_777_216f},
        });
        TEST_DB.put(pair(Float.class, Float.class), new Object[][]{
                {-1f, -1f},
                {0f, 0f},
                {1f, 1f},
                {Float.MIN_VALUE, Float.MIN_VALUE},
                {Float.MAX_VALUE, Float.MAX_VALUE},
                {-Float.MAX_VALUE, -Float.MAX_VALUE},
        });
        TEST_DB.put(pair(Double.class, Float.class), new Object[][]{
                {-1.0, -1f},
                {-1.99, -1.99f},
                {-1.1, -1.1f},
                {0.0, 0f},
                {1.0, 1f},
                {1.1, 1.1f},
                {1.999, 1.999f},
                {(double) Float.MIN_VALUE, Float.MIN_VALUE},
                {(double) Float.MAX_VALUE, Float.MAX_VALUE},
                {(double) -Float.MAX_VALUE, -Float.MAX_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Float.class), new Object[][]{
                {new BigDecimal("-1"), -1f, true},
                {new BigDecimal("-1.1"), -1.1f},        // no reverse - IEEE 754 rounding errors
                {new BigDecimal("-1.9"), -1.9f},        // no reverse - IEEE 754 rounding errors
                {BigDecimal.ZERO, 0f, true},
                {new BigDecimal("1"), 1f, true},
                {new BigDecimal("1.1"), 1.1f},        // no reverse - IEEE 754 rounding errors
                {new BigDecimal("1.9"), 1.9f},        // no reverse - IEEE 754 rounding errors
                {new BigDecimal("-16777216"), -16777216f, true},
                {new BigDecimal("16777216"), 16777216f, true},
        });
        TEST_DB.put(pair(Map.class, Float.class), new Object[][]{
                {mapOf("_v", "-1"), -1f},
                {mapOf("_v", -1f), -1f, true},
                {mapOf("value", "-1"), -1f},
                {mapOf("value", -1f), -1f},

                {mapOf("_v", "0"), 0f},
                {mapOf("_v", 0f), 0f, true},

                {mapOf("_v", "1"), 1f},
                {mapOf("_v", 1f), 1f, true},

                {mapOf("_v", "-16777216"), -16777216f},
                {mapOf("_v", -16777216), -16777216f},

                {mapOf("_v", "16777216"), 16777216f},
                {mapOf("_v", 16777216), 16777216f},

                {mapOf("_v", mapOf("_v", 16777216)), 16777216f},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Float.class), new Object[][]{
                {"-1.0", -1f, true},
                {"-1.1", -1.1f, true},
                {"-1.9", -1.9f, true},
                {"0", 0f, true},
                {"1.0", 1f, true},
                {"1.1", 1.1f, true},
                {"1.9", 1.9f, true},
                {"-16777216", -16777216f},
                {"16777216", 16777216f},
                {"1.4E-45", Float.MIN_VALUE, true},
                {"-3.4028235E38", -Float.MAX_VALUE, true},
                {"3.4028235E38", Float.MAX_VALUE, true},
                {"1.2345679E7", 12345679f, true},
                {"1.2345679E-7", 0.000000123456789f, true},
                {"12345.0", 12345f, true},
                {"1.2345E-4", 0.00012345f, true},
                {"", 0f},
                {" ", 0f},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a float")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a float")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a float")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a float")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a float")},
        });
        TEST_DB.put(pair(Year.class, Float.class), new Object[][]{
                {Year.of(2024), 2024f}
        });
    }

    /**
     * Long/long
     */
    private static void loadLongTests() {
        TEST_DB.put(pair(Void.class, long.class), new Object[][]{
                {null, 0L},
        });
        TEST_DB.put(pair(Void.class, Long.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Short.class, Long.class), new Object[][]{
                {(short) -1, -1L},
                {(short) 0, 0L},
                {(short) 1, 1L},
                {Short.MIN_VALUE, (long) Short.MIN_VALUE},
                {Short.MAX_VALUE, (long) Short.MAX_VALUE},
        });
        TEST_DB.put(pair(Integer.class, Long.class), new Object[][]{
                {-1, -1L},
                {0, 0L},
                {1, 1L},
                {Integer.MAX_VALUE, (long) Integer.MAX_VALUE},
                {Integer.MIN_VALUE, (long) Integer.MIN_VALUE},
        });
        TEST_DB.put(pair(Long.class, Long.class), new Object[][]{
                {-1L, -1L},
                {0L, 0L},
                {1L, 1L},
                {9223372036854775807L, Long.MAX_VALUE},
                {-9223372036854775808L, Long.MIN_VALUE},
        });
        TEST_DB.put(pair(Float.class, Long.class), new Object[][]{
                {-1f, -1L},
                {-1.99f, -1L},
                {-1.1f, -1L},
                {0f, 0L},
                {1f, 1L},
                {1.1f, 1L},
                {1.999f, 1L},
                {-214748368f, -214748368L},    // large representable -float
                {214748368f, 214748368L},      // large representable +float
        });
        TEST_DB.put(pair(Double.class, Long.class), new Object[][]{
                {-1.0, -1L},
                {-1.99, -1L},
                {-1.1, -1L},
                {0.0, 0L},
                {1.0, 1L},
                {1.1, 1L},
                {1.999, 1L},
                {-9223372036854775808.0, Long.MIN_VALUE},
                {9223372036854775807.0, Long.MAX_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Long.class), new Object[][]{
                {new BigInteger("-1"), -1L, true},
                {BigInteger.ZERO, 0L, true},
                {new BigInteger("1"), 1L, true},
                {new BigInteger("-9223372036854775808"), Long.MIN_VALUE, true},
                {new BigInteger("9223372036854775807"), Long.MAX_VALUE, true},
                {new BigInteger("-9223372036854775809"), Long.MAX_VALUE},       // Test wrap around
                {new BigInteger("9223372036854775808"), Long.MIN_VALUE},        // Test wrap around
        });
        TEST_DB.put(pair(BigDecimal.class, Long.class), new Object[][]{
                {new BigDecimal("-1"), -1L, true},
                {new BigDecimal("-1.1"), -1L},
                {new BigDecimal("-1.9"), -1L},
                {BigDecimal.ZERO, 0L, true},
                {new BigDecimal("1"), 1L, true},
                {new BigDecimal("1.1"), 1L},
                {new BigDecimal("1.9"), 1L},
                {new BigDecimal("-9223372036854775808"), Long.MIN_VALUE, true},
                {new BigDecimal("9223372036854775807"), Long.MAX_VALUE, true},
                {new BigDecimal("-9223372036854775809"), Long.MAX_VALUE},       // wrap around
                {new BigDecimal("9223372036854775808"), Long.MIN_VALUE},        // wrap around
        });
        TEST_DB.put(pair(Map.class, Long.class), new Object[][]{
                {mapOf(V, "-1"), -1L},
                {mapOf(V, -1L), -1L, true},
                {mapOf(V, "-1"), -1L},
                {mapOf("value", -1L), -1L},

                {mapOf("_v", "0"), 0L},
                {mapOf("_v", 0), 0L},

                {mapOf("_v", "1"), 1L},
                {mapOf("_v", 1), 1L},

                {mapOf("_v", "-9223372036854775808"), Long.MIN_VALUE},
                {mapOf("_v", -9223372036854775808L), Long.MIN_VALUE, true},

                {mapOf("_v", "9223372036854775807"), Long.MAX_VALUE},
                {mapOf("_v", 9223372036854775807L), Long.MAX_VALUE, true},

                {mapOf("_v", "-9223372036854775809"), new IllegalArgumentException("'-9223372036854775809' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},

                {mapOf("_v", "9223372036854775808"), new IllegalArgumentException("'9223372036854775808' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {mapOf("_v", mapOf("_v", -9223372036854775808L)), Long.MIN_VALUE},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Long.class), new Object[][]{
                {"-1", -1L, true},
                {"-1.1", -1L},
                {"-1.9", -1L},
                {"0", 0L, true},
                {"1", 1L, true},
                {"1.1", 1L},
                {"1.9", 1L},
                {"-2147483648", -2147483648L, true},
                {"2147483647", 2147483647L, true},
                {"", 0L},
                {" ", 0L},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"-9223372036854775809", new IllegalArgumentException("'-9223372036854775809' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {"9223372036854775808", new IllegalArgumentException("'9223372036854775808' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
        });
        TEST_DB.put(pair(Year.class, Long.class), new Object[][]{
                {Year.of(-1), -1L},
                {Year.of(0), 0L},
                {Year.of(1), 1L},
                {Year.of(1582), 1582L},
                {Year.of(1970), 1970L},
                {Year.of(2000), 2000L},
                {Year.of(2024), 2024L},
                {Year.of(9999), 9999L},
        });
        TEST_DB.put(pair(Date.class, Long.class), new Object[][]{
                {new Date(Long.MIN_VALUE), Long.MIN_VALUE, true},
                {new Date(now), now, true},
                {new Date(Integer.MIN_VALUE), (long) Integer.MIN_VALUE, true},
                {new Date(0), 0L, true},
                {new Date(Integer.MAX_VALUE), (long) Integer.MAX_VALUE, true},
                {new Date(Long.MAX_VALUE), Long.MAX_VALUE, true},
        });
        TEST_DB.put(pair(java.sql.Date.class, Long.class), new Object[][]{
                // --------------------------------------------------------------------
                // BIDIRECTIONAL tests: the date was created from the exact Tokyo midnight value.
                // Converting the date back will yield the same epoch millis.
                // --------------------------------------------------------------------
                { java.sql.Date.valueOf("1970-01-01"), -32400000L,      true },
                { java.sql.Date.valueOf("1970-01-02"),  54000000L,      true },
                { java.sql.Date.valueOf("1970-01-03"), 140400000L,      true },
                { java.sql.Date.valueOf("1971-01-01"), 31503600000L,    true },
                { java.sql.Date.valueOf("2000-01-01"), 946652400000L,   true },
                { java.sql.Date.valueOf("2020-01-01"), 1577804400000L,  true },
                { java.sql.Date.valueOf("1907-01-01"), -1988182800000L, true },

                // --------------------------------------------------------------------
                // UNIDIRECTIONAL tests: the date was produced from a non–midnight long value.
                // Although converting to Date yields the correct local day, converting back will
                // always produce the Tokyo midnight epoch value (i.e. “rounded down”).
                // --------------------------------------------------------------------
                // These tests correspond to original forward tests that used non-midnight values.
                { java.sql.Date.valueOf("1970-01-01"), -32400000L,      false }, // from original long -1L
                { java.sql.Date.valueOf("1970-01-02"),  54000000L,      false }, // from original long (86400000 + 1000)
                { java.sql.Date.valueOf("1970-04-27"),  9990000000L,    false }, // from original long 10000000000L
                { java.sql.Date.valueOf("2020-01-01"), 1577804400000L,  false }  // from original long 1577836800001L
        });
        TEST_DB.put(pair(Timestamp.class, Long.class), new Object[][]{
//                {new Timestamp(Long.MIN_VALUE), Long.MIN_VALUE, true},
                {new Timestamp(Integer.MIN_VALUE), (long) Integer.MIN_VALUE, true},
                {new Timestamp(now), now, true},
                {new Timestamp(0), 0L, true},
                {new Timestamp(Integer.MAX_VALUE), (long) Integer.MAX_VALUE, true},
                {new Timestamp(Long.MAX_VALUE), Long.MAX_VALUE, true},
        });
        TEST_DB.put(pair(Duration.class, Long.class), new Object[][]{
                {Duration.ofMillis(Long.MIN_VALUE / 2), Long.MIN_VALUE / 2, true},
                {Duration.ofMillis(Integer.MIN_VALUE), (long) Integer.MIN_VALUE, true},
                {Duration.ofMillis(-1), -1L, true},
                {Duration.ofMillis(0), 0L, true},
                {Duration.ofMillis(1), 1L, true},
                {Duration.ofMillis(Integer.MAX_VALUE), (long) Integer.MAX_VALUE, true},
                {Duration.ofMillis(Long.MAX_VALUE / 2), Long.MAX_VALUE / 2, true},
        });
        TEST_DB.put(pair(Instant.class, Long.class), new Object[][]{
                {Instant.parse("0000-01-01T00:00:00Z"), -62167219200000L, true},
                {Instant.parse("0000-01-01T00:00:00.001Z"), -62167219199999L, true},
                {Instant.parse("1969-12-31T23:59:59Z"), -1000L, true},
                {Instant.parse("1969-12-31T23:59:59.999Z"), -1L, true},
                {Instant.parse("1970-01-01T00:00:00Z"), 0L, true},
                {Instant.parse("1970-01-01T00:00:00.001Z"), 1L, true},
                {Instant.parse("1970-01-01T00:00:00.999Z"), 999L, true},
        });
        TEST_DB.put(pair(LocalDate.class, Long.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDate(), -62167252739000L, true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDate(), -62167252739000L, true},
                {zdt("1969-12-31T14:59:59.999Z").toLocalDate(), -118800000L, true},
                {zdt("1969-12-31T15:00:00Z").toLocalDate(), -32400000L, true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDate(), -32400000L, true},
                {zdt("1970-01-01T00:00:00Z").toLocalDate(), -32400000L, true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDate(), -32400000L, true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDate(), -32400000L, true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Long.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), -62167219200000L, true},
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), -62167219200000L, true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDateTime(), -62167219199999L, true},
                {zdt("1969-12-31T23:59:59Z").toLocalDateTime(), -1000L, true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDateTime(), -1L, true},
                {zdt("1970-01-01T00:00:00Z").toLocalDateTime(), 0L, true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDateTime(), 1L, true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDateTime(), 999L, true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, Long.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z"), -62167219200000L, true},
                {zdt("0000-01-01T00:00:00.001Z"), -62167219199999L, true},
                {zdt("1969-12-31T23:59:59Z"), -1000L, true},
                {zdt("1969-12-31T23:59:59.999Z"), -1L, true},
                {zdt("1970-01-01T00:00:00Z"), 0L, true},
                {zdt("1970-01-01T00:00:00.001Z"), 1L, true},
                {zdt("1970-01-01T00:00:00.999Z"), 999L, true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Long.class), new Object[][]{
                {odt("0000-01-01T00:00:00Z"), -62167219200000L},
                {odt("0000-01-01T00:00:00.001Z"), -62167219199999L},
                {odt("1969-12-31T23:59:59.999Z"), -1L, true},
                {odt("1970-01-01T00:00Z"), 0L, true},
                {odt("1970-01-01T00:00:00.001Z"), 1L, true},
        });
        TEST_DB.put(pair(Year.class, Long.class), new Object[][]{
                {Year.of(2024), 2024L, true},
        });
    }

    /**
     * Integer/int
     */
    private static void loadIntegerTests() {
        TEST_DB.put(pair(Void.class, int.class), new Object[][]{
                {null, 0},
        });
        TEST_DB.put(pair(Void.class, Integer.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Short.class, Integer.class), new Object[][]{
                {(short) -1, -1},
                {(short) 0, 0},
                {(short) 1, 1},
                {Short.MIN_VALUE, (int) Short.MIN_VALUE},
                {Short.MAX_VALUE, (int) Short.MAX_VALUE},
        });
        TEST_DB.put(pair(Integer.class, Integer.class), new Object[][]{
                {-1, -1},
                {0, 0},
                {1, 1},
                {Integer.MAX_VALUE, Integer.MAX_VALUE},
                {Integer.MIN_VALUE, Integer.MIN_VALUE},
        });
        TEST_DB.put(pair(Long.class, Integer.class), new Object[][]{
                {-1L, -1},
                {0L, 0},
                {1L, 1},
                {-2147483649L, Integer.MAX_VALUE},   // wrap around check
                {2147483648L, Integer.MIN_VALUE},   // wrap around check
        });
        TEST_DB.put(pair(Float.class, Integer.class), new Object[][]{
                {-1f, -1},
                {-1.99f, -1},
                {-1.1f, -1},
                {0f, 0},
                {1f, 1},
                {1.1f, 1},
                {1.999f, 1},
                {-214748368f, -214748368},    // large representable -float
                {214748368f, 214748368},      // large representable +float
        });
        TEST_DB.put(pair(Double.class, Integer.class), new Object[][]{
                {-1.0, -1},
                {-1.99, -1},
                {-1.1, -1},
                {0.0, 0},
                {1.0, 1},
                {1.1, 1},
                {1.999, 1},
                {-2147483648.0, Integer.MIN_VALUE},
                {2147483647.0, Integer.MAX_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Integer.class), new Object[][]{
                {new AtomicLong(-1), -1, true},
                {new AtomicLong(0), 0, true},
                {new AtomicLong(1), 1, true},
                {new AtomicLong(-2147483648), Integer.MIN_VALUE, true},
                {new AtomicLong(2147483647), Integer.MAX_VALUE, true},
        });
        TEST_DB.put(pair(BigInteger.class, Integer.class), new Object[][]{
                {new BigInteger("-1"), -1, true},
                {BigInteger.ZERO, 0, true},
                {new BigInteger("1"), 1, true},
                {new BigInteger("-2147483648"), Integer.MIN_VALUE, true},
                {new BigInteger("2147483647"), Integer.MAX_VALUE, true},
                {new BigInteger("-2147483649"), Integer.MAX_VALUE},
                {new BigInteger("2147483648"), Integer.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Integer.class), new Object[][]{
                {new BigDecimal("-1"), -1, true},
                {new BigDecimal("-1.1"), -1},
                {new BigDecimal("-1.9"), -1},
                {BigDecimal.ZERO, 0, true},
                {new BigDecimal("1"), 1, true},
                {new BigDecimal("1.1"), 1},
                {new BigDecimal("1.9"), 1},
                {new BigDecimal("-2147483648"), Integer.MIN_VALUE, true},
                {new BigDecimal("2147483647"), Integer.MAX_VALUE, true},
                {new BigDecimal("-2147483649"), Integer.MAX_VALUE},       // wrap around test
                {new BigDecimal("2147483648"), Integer.MIN_VALUE},        // wrap around test
        });
        TEST_DB.put(pair(Map.class, Integer.class), new Object[][]{
                {mapOf("_v", "-1"), -1},
                {mapOf("_v", -1), -1, true},
                {mapOf("value", "-1"), -1},
                {mapOf("value", -1L), -1},

                {mapOf("_v", "0"), 0},
                {mapOf("_v", 0), 0, true},

                {mapOf("_v", "1"), 1},
                {mapOf("_v", 1), 1, true},

                {mapOf("_v", "-2147483648"), Integer.MIN_VALUE},
                {mapOf("_v", -2147483648), Integer.MIN_VALUE},

                {mapOf("_v", "2147483647"), Integer.MAX_VALUE},
                {mapOf("_v", 2147483647), Integer.MAX_VALUE},

                {mapOf("_v", "-2147483649"), new IllegalArgumentException("'-2147483649' not parseable as an int value or outside -2147483648 to 2147483647")},
                {mapOf("_v", -2147483649L), Integer.MAX_VALUE},

                {mapOf("_v", "2147483648"), new IllegalArgumentException("'2147483648' not parseable as an int value or outside -2147483648 to 2147483647")},
                {mapOf("_v", 2147483648L), Integer.MIN_VALUE},
                {mapOf("_v", mapOf("_v", 2147483648L)), Integer.MIN_VALUE},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Integer.class), new Object[][]{
                {"-1", -1, true},
                {"-1.1", -1},
                {"-1.9", -1},
                {"0", 0, true},
                {"1", 1, true},
                {"1.1", 1},
                {"1.9", 1},
                {"-2147483648", -2147483648, true},
                {"2147483647", 2147483647, true},
                {"", 0},
                {" ", 0},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"-2147483649", new IllegalArgumentException("'-2147483649' not parseable as an int value or outside -2147483648 to 2147483647")},
                {"2147483648", new IllegalArgumentException("'2147483648' not parseable as an int value or outside -2147483648 to 2147483647")},
        });
        TEST_DB.put(pair(Year.class, Integer.class), new Object[][]{
                {Year.of(-1), -1, true},
                {Year.of(0), 0, true},
                {Year.of(1), 1, true},
                {Year.of(1582), 1582, true},
                {Year.of(1970), 1970, true},
                {Year.of(2000), 2000, true},
                {Year.of(2024), 2024, true},
                {Year.of(9999), 9999, true},
        });
    }

    /**
     * Short/short
     */
    private static void loadShortTests() {
        TEST_DB.put(pair(Void.class, short.class), new Object[][]{
                {null, (short) 0},
        });
        TEST_DB.put(pair(Void.class, Short.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Short.class, Short.class), new Object[][]{
                {(short) -1, (short) -1},
                {(short) 0, (short) 0},
                {(short) 1, (short) 1},
                {Short.MIN_VALUE, Short.MIN_VALUE},
                {Short.MAX_VALUE, Short.MAX_VALUE},
        });
        TEST_DB.put(pair(Integer.class, Short.class), new Object[][]{
                {-1, (short) -1},
                {0, (short) 0},
                {1, (short) 1},
                {-32769, Short.MAX_VALUE},   // wrap around check
                {32768, Short.MIN_VALUE},   // wrap around check
        });
        TEST_DB.put(pair(Long.class, Short.class), new Object[][]{
                {-1L, (short) -1},
                {0L, (short) 0},
                {1L, (short) 1},
                {-32769L, Short.MAX_VALUE},   // wrap around check
                {32768L, Short.MIN_VALUE},   // wrap around check
        });
        TEST_DB.put(pair(Float.class, Short.class), new Object[][]{
                {-1f, (short) -1},
                {-1.99f, (short) -1},
                {-1.1f, (short) -1},
                {0f, (short) 0},
                {1f, (short) 1},
                {1.1f, (short) 1},
                {1.999f, (short) 1},
                {-32768f, Short.MIN_VALUE},
                {32767f, Short.MAX_VALUE},
                {-32769f, Short.MAX_VALUE}, // verify wrap around
                {32768f, Short.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Double.class, Short.class), new Object[][]{
                {-1.0, (short) -1, true},
                {-1.99, (short) -1},
                {-1.1, (short) -1},
                {0.0, (short) 0, true},
                {1.0, (short) 1, true},
                {1.1, (short) 1},
                {1.999, (short) 1},
                {-32768.0, Short.MIN_VALUE, true},
                {32767.0, Short.MAX_VALUE, true},
                {-32769.0, Short.MAX_VALUE}, // verify wrap around
                {32768.0, Short.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(AtomicInteger.class, Short.class), new Object[][]{
                {new AtomicInteger(-1), (short) -1, true},
                {new AtomicInteger(0), (short) 0, true},
                {new AtomicInteger(1), (short) 1, true},
                {new AtomicInteger(-32768), Short.MIN_VALUE, true},
                {new AtomicInteger(32767), Short.MAX_VALUE, true},
                {new AtomicInteger(-32769), Short.MAX_VALUE},
                {new AtomicInteger(32768), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Short.class), new Object[][]{
                {new AtomicLong(-1), (short) -1, true},
                {new AtomicLong(0), (short) 0, true},
                {new AtomicLong(1), (short) 1, true},
                {new AtomicLong(-32768), Short.MIN_VALUE, true},
                {new AtomicLong(32767), Short.MAX_VALUE, true},
                {new AtomicLong(-32769), Short.MAX_VALUE},
                {new AtomicLong(32768), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Short.class), new Object[][]{
                {new BigInteger("-1"), (short) -1, true},
                {BigInteger.ZERO, (short) 0, true},
                {new BigInteger("1"), (short) 1, true},
                {new BigInteger("-32768"), Short.MIN_VALUE, true},
                {new BigInteger("32767"), Short.MAX_VALUE, true},
                {new BigInteger("-32769"), Short.MAX_VALUE},
                {new BigInteger("32768"), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Short.class), new Object[][]{
                {new BigDecimal("-1"), (short) -1, true},
                {new BigDecimal("-1.1"), (short) -1},
                {new BigDecimal("-1.9"), (short) -1},
                {BigDecimal.ZERO, (short) 0, true},
                {new BigDecimal("1"), (short) 1, true},
                {new BigDecimal("1.1"), (short) 1},
                {new BigDecimal("1.9"), (short) 1},
                {new BigDecimal("-32768"), Short.MIN_VALUE, true},
                {new BigDecimal("32767"), Short.MAX_VALUE, true},
                {new BigDecimal("-32769"), Short.MAX_VALUE},
                {new BigDecimal("32768"), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(Map.class, Short.class), new Object[][]{
                {mapOf("_v", "-1"), (short) -1},
                {mapOf("_v", -1), (short) -1},
                {mapOf("value", "-1"), (short) -1},
                {mapOf("value", -1L), (short) -1},

                {mapOf("_v", "0"), (short) 0},
                {mapOf("_v", 0), (short) 0},

                {mapOf("_v", "1"), (short) 1},
                {mapOf("_v", 1), (short) 1},

                {mapOf("_v", "-32768"), Short.MIN_VALUE},
                {mapOf("_v", (short)-32768), Short.MIN_VALUE, true},

                {mapOf("_v", "32767"), Short.MAX_VALUE},
                {mapOf("_v", (short)32767), Short.MAX_VALUE, true},

                {mapOf("_v", "-32769"), new IllegalArgumentException("'-32769' not parseable as a short value or outside -32768 to 32767")},
                {mapOf("_v", -32769), Short.MAX_VALUE},

                {mapOf("_v", "32768"), new IllegalArgumentException("'32768' not parseable as a short value or outside -32768 to 32767")},
                {mapOf("_v", 32768), Short.MIN_VALUE},
                {mapOf("_v", mapOf("_v", 32768L)), Short.MIN_VALUE},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Short.class), new Object[][]{
                {"-1", (short) -1, true},
                {"-1.1", (short) -1},
                {"-1.9", (short) -1},
                {"0", (short) 0, true},
                {"1", (short) 1, true},
                {"1.1", (short) 1},
                {"1.9", (short) 1},
                {"-32768", (short) -32768, true},
                {"32767", (short) 32767, true},
                {"", (short) 0},
                {" ", (short) 0},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a short value or outside -32768 to 32767")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a short value or outside -32768 to 32767")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a short value or outside -32768 to 32767")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a short value or outside -32768 to 32767")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a short value or outside -32768 to 32767")},
                {"-32769", new IllegalArgumentException("'-32769' not parseable as a short value or outside -32768 to 32767")},
                {"32768", new IllegalArgumentException("'32768' not parseable as a short value or outside -32768 to 32767")},
        });
        TEST_DB.put(pair(Year.class, Short.class), new Object[][]{
                {Year.of(-1), (short) -1},
                {Year.of(0), (short) 0},
                {Year.of(1), (short) 1},
                {Year.of(1582), (short) 1582},
                {Year.of(1970), (short) 1970},
                {Year.of(2000), (short) 2000},
                {Year.of(2024), (short) 2024},
                {Year.of(9999), (short) 9999},
        });
    }

    /**
     * Collection
     */
    private static void loadCollectionTest() {
        TEST_DB.put(pair(Collection.class, Collection.class), new Object[][]{
                {Arrays.asList(1, null, "three"), new Vector<>(Arrays.asList(1, null, "three")), true},
        });
    }

    /**
     * Number
     */
    private static void loadNumberTest() {
        TEST_DB.put(pair(byte.class, Number.class), new Object[][]{
                {(byte) 1, (byte) 1, true},
        });
        TEST_DB.put(pair(Byte.class, Number.class), new Object[][]{
                {Byte.MAX_VALUE, Byte.MAX_VALUE, true},
        });
        TEST_DB.put(pair(short.class, Number.class), new Object[][]{
                {(short) -1, (short) -1, true},
        });
        TEST_DB.put(pair(Short.class, Number.class), new Object[][]{
                {Short.MIN_VALUE, Short.MIN_VALUE, true},
        });
        TEST_DB.put(pair(int.class, Number.class), new Object[][]{
                {-1, -1, true},
        });
        TEST_DB.put(pair(Integer.class, Number.class), new Object[][]{
                {Integer.MAX_VALUE, Integer.MAX_VALUE, true},
        });
        TEST_DB.put(pair(long.class, Number.class), new Object[][]{
                {(long) -1, (long) -1, true},
        });
        TEST_DB.put(pair(Long.class, Number.class), new Object[][]{
                {Long.MIN_VALUE, Long.MIN_VALUE, true},
        });
        TEST_DB.put(pair(float.class, Number.class), new Object[][]{
                {-1.1f, -1.1f, true},
        });
        TEST_DB.put(pair(Float.class, Number.class), new Object[][]{
                {Float.MAX_VALUE, Float.MAX_VALUE, true},
        });
        TEST_DB.put(pair(double.class, Number.class), new Object[][]{
                {-1.1d, -1.1d, true},
        });
        TEST_DB.put(pair(Double.class, Number.class), new Object[][]{
                {Double.MAX_VALUE, Double.MAX_VALUE, true},
        });
        TEST_DB.put(pair(AtomicInteger.class, Number.class), new Object[][]{
                {new AtomicInteger(16), new AtomicInteger(16), true},
        });
        TEST_DB.put(pair(AtomicLong.class, Number.class), new Object[][]{
                {new AtomicLong(-16), new AtomicLong(-16), true},
        });
        TEST_DB.put(pair(BigInteger.class, Number.class), new Object[][]{
                {new BigInteger("7"), new BigInteger("7"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Number.class), new Object[][]{
                {new BigDecimal("3.14159"), new BigDecimal("3.14159"), true},
        });
    }

    /**
     * Byte/byte
     */
    private static void loadByteTest() {
        TEST_DB.put(pair(Void.class, byte.class), new Object[][]{
                {null, (byte) 0},
        });
        TEST_DB.put(pair(Void.class, Byte.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Byte.class), new Object[][]{
                {(byte) -1, (byte) -1},
                {(byte) 0, (byte) 0},
                {(byte) 1, (byte) 1},
                {Byte.MIN_VALUE, Byte.MIN_VALUE},
                {Byte.MAX_VALUE, Byte.MAX_VALUE},
        });
        TEST_DB.put(pair(Short.class, Byte.class), new Object[][]{
                {(short) -1, (byte) -1, true},
                {(short) 0, (byte) 0, true},
                {(short) 1, (byte) 1, true},
                {(short) -128, Byte.MIN_VALUE, true},
                {(short) 127, Byte.MAX_VALUE, true},
                {(short) -129, Byte.MAX_VALUE},    // verify wrap around
                {(short) 128, Byte.MIN_VALUE},    // verify wrap around
        });
        TEST_DB.put(pair(Integer.class, Byte.class), new Object[][]{
                {-1, (byte) -1, true},
                {0, (byte) 0, true},
                {1, (byte) 1, true},
                {-128, Byte.MIN_VALUE, true},
                {127, Byte.MAX_VALUE, true},
                {-129, Byte.MAX_VALUE},   // verify wrap around
                {128, Byte.MIN_VALUE},   // verify wrap around
        });
        TEST_DB.put(pair(Long.class, Byte.class), new Object[][]{
                {-1L, (byte) -1, true},
                {0L, (byte) 0, true},
                {1L, (byte) 1, true},
                {-128L, Byte.MIN_VALUE, true},
                {127L, Byte.MAX_VALUE, true},
                {-129L, Byte.MAX_VALUE}, // verify wrap around
                {128L, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Float.class, Byte.class), new Object[][]{
                {-1f, (byte) -1, true},
                {-1.99f, (byte) -1},
                {-1.1f, (byte) -1},
                {0f, (byte) 0, true},
                {1f, (byte) 1, true},
                {1.1f, (byte) 1},
                {1.999f, (byte) 1},
                {-128f, Byte.MIN_VALUE, true},
                {127f, Byte.MAX_VALUE, true},
                {-129f, Byte.MAX_VALUE}, // verify wrap around
                {128f, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Double.class, Byte.class), new Object[][]{
                {-1.0, (byte) -1, true},
                {-1.99, (byte) -1},
                {-1.1, (byte) -1},
                {0.0, (byte) 0, true},
                {1.0, (byte) 1, true},
                {1.1, (byte) 1},
                {1.999, (byte) 1},
                {-128.0, Byte.MIN_VALUE, true},
                {127.0, Byte.MAX_VALUE, true},
                {-129.0, Byte.MAX_VALUE}, // verify wrap around
                {128.0, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Character.class, Byte.class), new Object[][]{
                {'1', (byte) 49, true},
                {'0', (byte) 48, true},
                {(char) 1, (byte) 1, true},
                {(char) 0, (byte) 0, true},
                {(char) -1, (byte) 65535, true},
                {(char) Byte.MAX_VALUE, Byte.MAX_VALUE, true},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Byte.class), new Object[][]{
                {new AtomicBoolean(true), (byte) 1, true},
                {new AtomicBoolean(false), (byte) 0, true},
        });
        TEST_DB.put(pair(AtomicInteger.class, Byte.class), new Object[][]{
                {new AtomicInteger(-1), (byte) -1, true},
                {new AtomicInteger(0), (byte) 0, true},
                {new AtomicInteger(1), (byte) 1, true},
                {new AtomicInteger(-128), Byte.MIN_VALUE, true},
                {new AtomicInteger(127), Byte.MAX_VALUE, true},
        });
        TEST_DB.put(pair(AtomicLong.class, Byte.class), new Object[][]{
                {new AtomicLong(-1), (byte) -1, true},
                {new AtomicLong(0), (byte) 0, true},
                {new AtomicLong(1), (byte) 1, true},
                {new AtomicLong(-128), Byte.MIN_VALUE, true},
                {new AtomicLong(127), Byte.MAX_VALUE, true},
        });
        TEST_DB.put(pair(BigInteger.class, Byte.class), new Object[][]{
                {new BigInteger("-1"), (byte) -1, true},
                {BigInteger.ZERO, (byte) 0, true},
                {new BigInteger("1"), (byte) 1, true},
                {new BigInteger("-128"), Byte.MIN_VALUE, true},
                {new BigInteger("127"), Byte.MAX_VALUE, true},
                {new BigInteger("-129"), Byte.MAX_VALUE},
                {new BigInteger("128"), Byte.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Byte.class), new Object[][]{
                {new BigDecimal("-1"), (byte) -1, true},
                {new BigDecimal("-1.1"), (byte) -1},
                {new BigDecimal("-1.9"), (byte) -1},
                {BigDecimal.ZERO, (byte) 0, true},
                {new BigDecimal("1"), (byte) 1, true},
                {new BigDecimal("1.1"), (byte) 1},
                {new BigDecimal("1.9"), (byte) 1},
                {new BigDecimal("-128"), Byte.MIN_VALUE, true},
                {new BigDecimal("127"), Byte.MAX_VALUE, true},
                {new BigDecimal("-129"), Byte.MAX_VALUE},
                {new BigDecimal("128"), Byte.MIN_VALUE},
        });
        TEST_DB.put(pair(Map.class, Byte.class), new Object[][]{
                {mapOf(V, "-1"), (byte) -1},
                {mapOf(V, -1), (byte) -1},
                {mapOf(VALUE, "-1"), (byte) -1},
                {mapOf(VALUE, -1L), (byte) -1},

                {mapOf(V, "0"), (byte) 0},
                {mapOf(V, 0), (byte) 0},

                {mapOf(V, "1"), (byte) 1},
                {mapOf(V, 1), (byte) 1},

                {mapOf(V, "-128"), Byte.MIN_VALUE},
                {mapOf(V, -128), Byte.MIN_VALUE},

                {mapOf(V, "127"), Byte.MAX_VALUE},
                {mapOf(V, 127), Byte.MAX_VALUE},

                {mapOf(V, "-129"), new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127")},
                {mapOf(V, -129), Byte.MAX_VALUE},

                {mapOf(V, "128"), new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127")},
                {mapOf(V, 128), Byte.MIN_VALUE},
                {mapOf(V, mapOf(V, 128L)), Byte.MIN_VALUE},    // Prove use of recursive call to .convert()
                {mapOf(V, (byte)1), (byte)1, true},
                {mapOf(V, (byte)2), (byte)2, true},
                {mapOf(VALUE, "nope"), new IllegalArgumentException("Value 'nope' not parseable as a byte value or outside -128 to 127")},

        });
        TEST_DB.put(pair(Year.class, Byte.class), new Object[][]{
                {Year.of(2024), new IllegalArgumentException("Unsupported conversion, source type [Year (2024)] target type 'Byte'") },
        });
        TEST_DB.put(pair(String.class, Byte.class), new Object[][]{
                {"-1", (byte) -1, true},
                {"-1.1", (byte) -1},
                {"-1.9", (byte) -1},
                {"0", (byte) 0, true},
                {"1", (byte) 1, true},
                {"1.1", (byte) 1},
                {"1.9", (byte) 1},
                {"-128", (byte) -128, true},
                {"127", (byte) 127, true},
                {"", (byte) 0},
                {" ", (byte) 0},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a byte value or outside -128 to 127")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a byte value or outside -128 to 127")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a byte value or outside -128 to 127")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a byte value or outside -128 to 127")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a byte value or outside -128 to 127")},
                {"-129", new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127")},
                {"128", new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127")},
        });
    }

    /**
     * byte[]
     */
    private static void loadByteArrayTest()
    {
        TEST_DB.put(pair(Void.class, byte[].class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(byte[].class, byte[].class), new Object[][]{
                {new byte[] {}, new byte[] {}},
                {new byte[] {1, 2}, new byte[] {1, 2}},
        });
        TEST_DB.put(pair(ByteBuffer.class, byte[].class), new Object[][]{
                {ByteBuffer.wrap(new byte[]{}), new byte[] {}, true},
                {ByteBuffer.wrap(new byte[]{-1}), new byte[] {-1}, true},
                {ByteBuffer.wrap(new byte[]{1, 2}), new byte[] {1, 2}, true},
                {ByteBuffer.wrap(new byte[]{1, 2, -3}), new byte[] {1, 2, -3}, true},
                {ByteBuffer.wrap(new byte[]{-128, 0, 127, 16}), new byte[] {-128, 0, 127, 16}, true},
        });
        TEST_DB.put(pair(char[].class, byte[].class), new Object[][] {
                {new char[] {}, new byte[] {}, true},
                {new char[] {'a', 'b'}, new byte[] {97, 98}, true},
        });
        TEST_DB.put(pair(CharBuffer.class, byte[].class), new Object[][]{
                {CharBuffer.wrap(new char[]{}), new byte[] {}, true},
                {CharBuffer.wrap(new char[]{'a', 'b'}), new byte[] {'a', 'b'}, true},
        });
        TEST_DB.put(pair(StringBuffer.class, byte[].class), new Object[][]{
                {new StringBuffer(), new byte[] {}, true},
                {new StringBuffer("ab"), new byte[] {'a', 'b'}, true},
        });
        TEST_DB.put(pair(StringBuilder.class, byte[].class), new Object[][]{
                {new StringBuilder(), new byte[] {}, true},
                {new StringBuilder("ab"), new byte[] {'a', 'b'}, true},
        });
    }

    /**
     * ByteBuffer
     */
    private static void loadByteBufferTest() {
        TEST_DB.put(pair(Void.class, ByteBuffer.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(ByteBuffer.class, ByteBuffer.class), new Object[][]{
                {ByteBuffer.wrap(new byte[] {'h'}), ByteBuffer.wrap(new byte[]{'h'})},
        });
        TEST_DB.put(pair(CharBuffer.class, ByteBuffer.class), new Object[][]{
                {CharBuffer.wrap(new char[] {'h', 'i'}), ByteBuffer.wrap(new byte[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(StringBuffer.class, ByteBuffer.class), new Object[][]{
                {new StringBuffer("hi"), ByteBuffer.wrap(new byte[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(StringBuilder.class, ByteBuffer.class), new Object[][]{
                {new StringBuilder("hi"), ByteBuffer.wrap(new byte[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(Map.class, ByteBuffer.class), new Object[][]{
                {mapOf(VALUE, "QUJDRAAAenl4dw=="), ByteBuffer.wrap(new byte[]{'A', 'B', 'C', 'D', 0, 0, 'z', 'y', 'x', 'w'})},
                {mapOf(V, "AABmb28AAA=="), ByteBuffer.wrap(new byte[]{0, 0, 'f', 'o', 'o', 0, 0})},
        });
    }

    /**
     * CharBuffer
     */
    private static void loadCharBufferTest() {
        TEST_DB.put(pair(Void.class, CharBuffer.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(CharBuffer.class, CharBuffer.class), new Object[][]{
                {CharBuffer.wrap(new char[] {'h'}), CharBuffer.wrap(new char[]{'h'})},
        });
        TEST_DB.put(pair(String.class, CharBuffer.class), new Object[][]{
                {"hi", CharBuffer.wrap(new char[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(StringBuffer.class, CharBuffer.class), new Object[][]{
                {new StringBuffer("hi"), CharBuffer.wrap(new char[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(StringBuilder.class, CharBuffer.class), new Object[][]{
                {new StringBuilder("hi"), CharBuffer.wrap(new char[]{'h', 'i'}), true},
        });
        TEST_DB.put(pair(Map.class, CharBuffer.class), new Object[][]{
                {mapOf(VALUE, "Claude"), CharBuffer.wrap("Claude")},
                {mapOf(V, "Anthropic"), CharBuffer.wrap("Anthropic")},
        });
    }

    /**
     * Character[]
     */
    private static void loadCharacterArrayTest() {
        TEST_DB.put(pair(Void.class, Character[].class), new Object[][]{
                {null, null},
        });
    }

    /**
     * char[]
     */
    private static void loadCharArrayTest() {
        TEST_DB.put(pair(Void.class, char[].class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(char[].class, char[].class), new Object[][]{
                {new char[] {'h'}, new char[] {'h'}},
        });
        TEST_DB.put(pair(ByteBuffer.class, char[].class), new Object[][]{
                {ByteBuffer.wrap(new byte[] {'h', 'i'}), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(CharBuffer.class, char[].class), new Object[][]{
                {CharBuffer.wrap(new char[] {}), new char[] {}, true},
                {CharBuffer.wrap(new char[] {'h', 'i'}), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(StringBuffer.class, char[].class), new Object[][]{
                {new StringBuffer("hi"), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(StringBuilder.class, char[].class), new Object[][]{
                {new StringBuilder("hi"), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(String.class, char[].class), new Object[][]{
                {"", new char[]{}, true},
                {"ABCD", new char[]{'A', 'B', 'C', 'D'}, true},
        });
    }

    /**
     * StringBuffer
     */
    private static void loadStringBufferTest() {
        TEST_DB.put(pair(Void.class, StringBuffer.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(StringBuffer.class, StringBuffer.class), new Object[][]{
                {new StringBuffer("Hi"), new StringBuffer("Hi")},
        });
        TEST_DB.put(pair(Character[].class, StringBuffer.class), new Object[][]{
                {new Character[] { 'H', 'i' }, new StringBuffer("Hi"), true},
        });
        TEST_DB.put(pair(String.class, StringBuffer.class), new Object[][]{
                {"same", new StringBuffer("same")},
        });
        TEST_DB.put(pair(Map.class, StringBuffer.class), new Object[][]{
                {mapOf("_v", "alpha"), new StringBuffer("alpha")},
                {mapOf("value", "beta"), new StringBuffer("beta")},
        });
    }

    /**
     * StringBuilder
     */
    private static void loadStringBuilderTest() {
        TEST_DB.put(pair(Void.class, StringBuilder.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(StringBuilder.class, StringBuilder.class), new Object[][]{
                {new StringBuilder("Hi"), new StringBuilder("Hi")},
        });
        TEST_DB.put(pair(Character[].class, StringBuilder.class), new Object[][]{
                {new Character[] { 'H', 'i' }, new StringBuilder("Hi"), true},
        });
        TEST_DB.put(pair(String.class, StringBuilder.class), new Object[][]{
                {"Poker", new StringBuilder("Poker")},
        });
        TEST_DB.put(pair(StringBuffer.class, StringBuilder.class), new Object[][]{
                {new StringBuffer("Poker"), new StringBuilder("Poker"), true},
        });
        TEST_DB.put(pair(Map.class, StringBuilder.class), new Object[][]{
                {mapOf("_v", "alpha"), new StringBuilder("alpha")},
                {mapOf("value", "beta"), new StringBuilder("beta")},
        });
    }

    private static URL toURL(String url) {
        try {
            return toURI(url).toURL();
        }
        catch (Exception e) {
            return null;
        }
    }

    private static URI toURI(String url) {
        return URI.create(url);
    }

    @BeforeEach
    void before() {
        // create converter with default options
        converter = new Converter(options);
    }
    
    private static Object possiblyConvertSupplier(Object possibleSupplier) {
        if (possibleSupplier instanceof Supplier) {
            return ((Supplier<?>) possibleSupplier).get();
        }

        return possibleSupplier;
    }


    private static Stream<Arguments> generateTestEverythingParams() {
        List<Arguments> list = new ArrayList<>(400);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Object[][]> entry : TEST_DB.entrySet()) {
            Class<?> sourceClass = entry.getKey().getKey();
            Class<?> targetClass = entry.getKey().getValue();

            // Skip Atomic's to Map - assertEquals() does not know to call .get() on the value side of the Map.
            if (isHardCase(sourceClass, targetClass)) {
                continue;
            }
            String sourceName = Converter.getShortName(sourceClass);
            String targetName = Converter.getShortName(targetClass);
            Object[][] testData = entry.getValue();
            int index = 0;
            for (Object[] testPair : testData) {
                Object source = possiblyConvertSupplier(testPair[0]);
                Object target = possiblyConvertSupplier(testPair[1]);

                list.add(Arguments.of(sourceName, targetName, source, target, sourceClass, targetClass, index++));
            }
        }

        return Stream.of(list.toArray(new Arguments[]{}));
    }

    private static Stream<Arguments> generateTestEverythingParamsInReverse() {
        List<Arguments> list = new ArrayList<>(400);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Object[][]> entry : TEST_DB.entrySet()) {
            Class<?> sourceClass = entry.getKey().getKey();
            Class<?> targetClass = entry.getKey().getValue();
            
            if (isHardCase(sourceClass, targetClass)) {
                continue;
            }

            String sourceName = Converter.getShortName(sourceClass);
            String targetName = Converter.getShortName(targetClass);
            Object[][] testData = entry.getValue();
            int index = 0;
            for (Object[] testPair : testData) {
                boolean reverse = false;
                Object source = possiblyConvertSupplier(testPair[0]);
                Object target = possiblyConvertSupplier(testPair[1]);

                if (testPair.length > 2) {
                    reverse = (boolean) testPair[2];
                }

                if (!reverse) {
                    continue;
                }

                list.add(Arguments.of(targetName, sourceName, target, source, targetClass, sourceClass, index++));
            }
        }

        return Stream.of(list.toArray(new Arguments[]{}));
    }

    /**
     * Run all conversion tests this way ==> Source to JSON, JSON to target (root class).  This will ensure that our
     * root class converts from what was passed to what was "asked for" by the rootType (Class) parameter.
     *
     * Need to wait for json-io 4.34.0 to enable.
     */
    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParams")
    void testConvertJsonIo(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass, int index) {
        if (shortNameSource.equals("Void")) {
            return;
        }

        // Special case for java.sql.Date comparisons
        if ((sourceClass.equals(java.sql.Date.class) && targetClass.equals(Date.class)) ||
                (sourceClass.equals(Date.class) && targetClass.equals(java.sql.Date.class)) ||
                (sourceClass.equals(java.sql.Date.class) && targetClass.equals(java.sql.Date.class))) {
            WriteOptions writeOptions = new WriteOptionsBuilder().build();
            ReadOptions readOptions = new ReadOptionsBuilder().setZoneId(TOKYO_Z).build();
            String json = JsonIo.toJson(source, writeOptions);
            Object restored = JsonIo.toObjects(json, readOptions, targetClass);

            // Compare dates by LocalDate
            LocalDate restoredDate = (restored instanceof java.sql.Date) ?
                    ((java.sql.Date) restored).toLocalDate() :
                    Instant.ofEpochMilli(((Date) restored).getTime())
                            .atZone(TOKYO_Z)
                            .toLocalDate();

            LocalDate targetDate = (target instanceof java.sql.Date) ?
                    ((java.sql.Date) target).toLocalDate() :
                    Instant.ofEpochMilli(((Date) target).getTime())
                            .atZone(TOKYO_Z)
                            .toLocalDate();

            if (!restoredDate.equals(targetDate)) {
                System.out.println("Conversion failed for: " + shortNameSource + " ==> " + shortNameTarget);
                System.out.println("restored = " + restored);
                System.out.println("target   = " + target);
                System.out.println("diff     = [value mismatch] ▶ Date: " + restoredDate + " vs " + targetDate);
                fail();
            }
            updateStat(pair(sourceClass, targetClass), true);
            return;
        }

        // Conversions that don't fail as anticipated
        boolean skip1 = sourceClass.equals(Byte.class) && targetClass.equals(Year.class) || sourceClass.equals(Year.class) && targetClass.equals(Byte.class);
        if (skip1) {
            return;
        }
        boolean skip2 = sourceClass.equals(Map.class) && targetClass.equals(Map.class);
        if (skip2) {
            return;
        }
        boolean skip3 = sourceClass.equals(Map.class) && targetClass.equals(Enum.class);
        if (skip3) {
            return;
        }
        boolean skip4 = sourceClass.equals(Map.class) && targetClass.equals(Throwable.class);
        if (skip4) {
            return;
        }
        WriteOptions writeOptions = new WriteOptionsBuilder().build();
        ReadOptions readOptions = new ReadOptionsBuilder().setZoneId(TOKYO_Z).build();
        String json = JsonIo.toJson(source, writeOptions);
        if (target instanceof Throwable) {
            Throwable t = (Throwable) target;
            try {
                Object x = JsonIo.toObjects(json, readOptions, targetClass);
//                System.out.println("x = " + x);
                fail("This test: " + shortNameSource + " ==> " + shortNameTarget + " should have thrown: " + target.getClass().getName());
            } catch (Throwable e) {
                if (e instanceof JsonIoException) {
                    e = e.getCause();
                }
                assertEquals(e.getClass(), t.getClass());
                updateStat(pair(sourceClass, targetClass), true);
            }
        } else {
            Object restored = null;
            try {
                restored = JsonIo.toObjects(json, readOptions, targetClass);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
//            System.out.println("source = " + source);
//            System.out.println("target = " + target);
//            System.out.println("restored = " + restored);
//            System.out.println("*****");
            Map<String, Object> options = new HashMap<>();
            if (restored instanceof Pattern) {
                assertEquals(restored.toString(), target.toString());
            } else if (!DeepEquals.deepEquals(restored, target, options)) {
                System.out.println("Conversion failed for: " + shortNameSource + " ==> " + shortNameTarget);
                System.out.println("restored = " + restored);
                System.out.println("target   = " + target);
                System.out.println("diff     = " + options.get("diff"));
                fail();
            }
            updateStat(pair(sourceClass, targetClass), true);
        }
    }

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParamsInReverse")
    void testConvertReverseJsonIo(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass, int index) {
        testConvertJsonIo(shortNameSource, shortNameTarget, source, target, sourceClass, targetClass, index);
    }

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParams")
    void testConvert(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass, int index) {
        if (index == 0) {
            Map.Entry<Class<?>, Class<?>> entry = pair(sourceClass, targetClass);
            Boolean alreadyCompleted = STAT_DB.get(entry);
            if (Boolean.TRUE.equals(alreadyCompleted) && !sourceClass.equals(targetClass)) {
//                System.err.println("Duplicate test pair: " + shortNameSource + " ==> " + shortNameTarget);
            }
        }

        if (source == null) {
            assertEquals(Void.class, sourceClass, "On the source-side of test input, null can only appear in the Void.class data");
        } else {
            assert ClassUtilities.toPrimitiveWrapperClass(sourceClass).isInstance(source) : "source type mismatch ==> Expected: " + shortNameSource + ", Actual: " + Converter.getShortName(source.getClass());
        }
        assert target == null || target instanceof Throwable || ClassUtilities.toPrimitiveWrapperClass(targetClass).isInstance(target) : "target type mismatch ==> Expected: " + shortNameTarget + ", Actual: " + Converter.getShortName(target.getClass());

        // if the source/target are the same Class, and the class is listed in the immutable Set, then ensure identity lambda is used.
        if (sourceClass.equals(targetClass) && immutable.contains(sourceClass)) {
            assertSame(source, converter.convert(source, targetClass));
        }

        if (target instanceof Throwable) {
            Throwable t = (Throwable) target;
            Object actual;
            try {
                // A test that returns a Throwable, as opposed to throwing it.
                actual = converter.convert(source, targetClass);
                Throwable actualExceptionReturnValue = (Throwable) actual;
                assert actualExceptionReturnValue.getMessage().equals(((Throwable) target).getMessage());
                assert actualExceptionReturnValue.getClass().equals(target.getClass());
                updateStat(pair(sourceClass, targetClass), true);
            } catch (Throwable e) {
                if (!e.getMessage().contains(t.getMessage())) {
                    System.out.println(e.getMessage());
                    System.out.println(t.getMessage());
                    System.out.println();
                }
                assert e.getMessage().contains(t.getMessage());
                assert e.getClass().equals(t.getClass());
            }
        } else {
            // Assert values are equals
            Object actual = converter.convert(source, targetClass);
            try {
                if (target instanceof CharSequence) {
                    assertEquals(target.toString(), actual.toString());
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(Pattern.class)) {
                    if (target == null) {
                        assert actual == null;
                    } else {
                        assertEquals(target.toString(), actual.toString());
                    }
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(byte[].class)) {
                    assertArrayEquals((byte[]) target, (byte[]) actual);
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(char[].class)) {
                    assertArrayEquals((char[]) target, (char[]) actual);
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(Character[].class)) {
                    assertArrayEquals((Character[]) target, (Character[]) actual);
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (target instanceof AtomicBoolean) {
                    assertEquals(((AtomicBoolean) target).get(), ((AtomicBoolean) actual).get());
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (target instanceof AtomicInteger) {
                    assertEquals(((AtomicInteger) target).get(), ((AtomicInteger) actual).get());
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (target instanceof AtomicLong) {
                    assertEquals(((AtomicLong) target).get(), ((AtomicLong) actual).get());
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (target instanceof BigDecimal) {
                    if (((BigDecimal) target).compareTo((BigDecimal) actual) != 0) {
                        assertEquals(target, actual);
                    }
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(java.sql.Date.class)) {
                    if (actual != null) {
                        java.sql.Date actualDate = java.sql.Date.valueOf(((java.sql.Date) actual).toLocalDate());
                        java.sql.Date targetDate = java.sql.Date.valueOf(((java.sql.Date) target).toLocalDate());
                        assertEquals(targetDate, actualDate);
                    }
                    updateStat(pair(sourceClass, targetClass), true);
                } else {
                    assertEquals(target, actual);
                    updateStat(pair(sourceClass, targetClass), true);
                }
            }
            catch (Throwable e) {
                String actualClass;
                if (actual == null) {
                    actualClass = "Class:null";
                } else {
                    actualClass = Converter.getShortName(actual.getClass());
                }

                System.err.println(shortNameSource + "[" + toStr(source) + "] ==> " + shortNameTarget + "[" + toStr(target) + "] Failed with: " + actualClass + "[" + toStr(actual) + "]");
                throw e;
            }
        }
    }
    
    private static void updateStat(Map.Entry<Class<?>, Class<?>> pair, boolean state) {
        STAT_DB.put(pair, state);
    }

    private String toStr(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof Calendar) {
            Calendar cal = (Calendar) o;
            return CalendarConversions.toString(cal, converter);
        } else {
            return o.toString();
        }
    }

    private static Date date(String s) {
        return Date.from(Instant.parse(s));
    }

    private static Timestamp timestamp(String s) {
        return Timestamp.from(Instant.parse(s));
    }

    private static ZonedDateTime zdt(String s) {
        return ZonedDateTime.parse(s).withZoneSameInstant(TOKYO_Z);
    }

    private static OffsetDateTime odt(String s) {
        return OffsetDateTime.parse(s).withOffsetSameInstant(TOKYO_ZO);
    }

    private static LocalDateTime ldt(String s) {
        return LocalDateTime.parse(s);
    }

    private static Calendar cal(long epochMillis) {
        Calendar cal = Calendar.getInstance(TOKYO_TZ);
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    // Rare pairings that cannot be tested without drilling into the class - Atomic's require .get() to be called,
    // so an Atomic inside a Map is a hard-case.
    private static boolean isHardCase(Class<?> sourceClass, Class<?> targetClass) {
        return targetClass.equals(Map.class) && (sourceClass.equals(AtomicBoolean.class) || sourceClass.equals(AtomicInteger.class) || sourceClass.equals(AtomicLong.class));
    }

    @BeforeAll
    static void statPrep() {
        Map<Class<?>, Set<Class<?>>> map = com.cedarsoftware.util.Converter.allSupportedConversions();

        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Set<Class<?>> targetClasses = entry.getValue();
            for (Class<?> targetClass : targetClasses) {
                updateStat(pair(sourceClass, targetClass), false);
            }
        }
    }

    @AfterAll
    static void printStats() {
        Set<String> testPairNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int missing = 0;

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Boolean> entry : STAT_DB.entrySet()) {
            Map.Entry<Class<?>, Class<?>> pair = entry.getKey();
            boolean value = entry.getValue();
            if (!value) {
                Class<?> sourceClass = pair.getKey();
                Class<?> targetClass = pair.getValue();
                if (isHardCase(sourceClass, targetClass)) {
                    continue;
                }
                missing++;
                testPairNames.add("\n  " + Converter.getShortName(pair.getKey()) + " ==> " + Converter.getShortName(pair.getValue()));
            }
        }
        
        System.out.println("Total conversion pairs      = " + STAT_DB.size());
        System.out.println("Conversion pairs tested     = " + (STAT_DB.size() - missing));
        System.out.println("Conversion pairs not tested = " + missing);
        System.out.print("Tests needed ");
        System.out.println(testPairNames);
    }

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParamsInReverse")
    void testConvertReverse(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass, int index) {
        testConvert(shortNameSource, shortNameTarget, source, target, sourceClass, targetClass, index);
    }
}
