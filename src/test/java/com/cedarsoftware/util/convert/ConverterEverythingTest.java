package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import java.util.ArrayList;
import java.util.Calendar;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static com.cedarsoftware.util.convert.Converter.VALUE;
import static com.cedarsoftware.util.convert.Converter.pair;
import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.DATE_TIME;
import static com.cedarsoftware.util.convert.MapConversions.EPOCH_MILLIS;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.NANOS;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static com.cedarsoftware.util.convert.MapConversions.V;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
// TODO: More exception tests (make sure IllegalArgumentException is thrown, for example, not DateTimeException)
// TODO: Throwable conversions need to be added for all the popular exception types
// TODO: Enum and EnumSet conversions need to be added
// TODO: MapConversions --> Var args of Object[]'s - show as 'OR' in message: [DATE, TIME], [epochMillis], [dateTime], [_V], or [VALUE]
// TODO: MapConversions --> Performance - containsKey() + get() ==> get() and null checks
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
        TEST_DB.put(pair(String.class, URL.class), new Object[][]{
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
                {"ftp://user@bar.com/foo/bar.txt", toURL("ftp://user@bar.com/foo/bar.txt"), true},
                {"ftp://user:password@host/foo/bar.txt", toURL("ftp://user:password@host/foo/bar.txt"), true},
                {"ftp://user:password@host:8192/foo/bar.txt", toURL("ftp://user:password@host:8192/foo/bar.txt"), true},
                {"file:/path/to/file", toURL("file:/path/to/file"), true},
                {"file://localhost/path/to/file.json", toURL("file://localhost/path/to/file.json"), true},
                {"file://servername/path/to/file.json", toURL("file://servername/path/to/file.json"), true},
                {"jar:file:/c://my.jar!/", toURL("jar:file:/c://my.jar!/"), true},
                {"jar:file:/c://my.jar!/com/mycompany/MyClass.class", toURL("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), true}
        });
        TEST_DB.put(pair(Map.class, URL.class), new Object[][]{
                { mapOf(URL_KEY, "https://domain.com"), toURL("https://domain.com"), true},
                { mapOf(URL_KEY, "bad earl"), new IllegalArgumentException("Cannot convert Map to URL. Malformed URL: 'bad earl'")},
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
                {toURI("https://chat.openai.com"), toURI("https://chat.openai.com")},
        });
        TEST_DB.put(pair(String.class, URI.class), new Object[][]{
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
                {"ftp://user@bar.com/foo/bar.txt", toURI("ftp://user@bar.com/foo/bar.txt"), true},
                {"ftp://user:password@host/foo/bar.txt", toURI("ftp://user:password@host/foo/bar.txt"), true},
                {"ftp://user:password@host:8192/foo/bar.txt", toURI("ftp://user:password@host:8192/foo/bar.txt"), true},
                {"file:/path/to/file", toURI("file:/path/to/file"), true},
                {"file://localhost/path/to/file.json", toURI("file://localhost/path/to/file.json"), true},
                {"file://servername/path/to/file.json", toURI("file://servername/path/to/file.json"), true},
                {"jar:file:/c://my.jar!/", toURI("jar:file:/c://my.jar!/"), true},
                {"jar:file:/c://my.jar!/com/mycompany/MyClass.class", toURI("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), true}
        });
        TEST_DB.put(pair(Map.class, URI.class), new Object[][]{
                { mapOf(URI_KEY, "https://domain.com"), toURI("https://domain.com"), true},
                { mapOf(URI_KEY, "bad uri"), new IllegalArgumentException("Cannot convert Map to URI. Malformed URI: 'bad uri'")},
        });
        TEST_DB.put(pair(URL.class, URI.class), new Object[][]{
                { (Supplier<URL>) () -> {
                       try {return new URL("https://domain.com");} catch(Exception e){return null;}
                }, toURI("https://domain.com"), true},
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
        TEST_DB.put(pair(String.class, TimeZone.class), new Object[][]{
                {"America/New_York", TimeZone.getTimeZone("America/New_York"), true},
                {"EST", TimeZone.getTimeZone("EST"), true},
                {"GMT+05:00", TimeZone.getTimeZone(ZoneId.of("+05:00")), true},
                {"America/Denver", TimeZone.getTimeZone(ZoneId.of("America/Denver")), true},
                {"American/FunkyTown", TimeZone.getTimeZone("GMT")},    // Per javadoc's
        });
        TEST_DB.put(pair(Map.class, TimeZone.class), new Object[][]{
                { mapOf(ZONE, "GMT"), TimeZone.getTimeZone("GMT"), true},
                { mapOf(ZONE, "America/New_York"), TimeZone.getTimeZone("America/New_York"), true},
                { mapOf(ZONE, "Asia/Tokyo"), TimeZone.getTimeZone("Asia/Tokyo"), true},
        });
    }

    /**
     * OffsetTime
     */
    private static void loadOffsetTimeTests() {
        TEST_DB.put(pair(Void.class, OffsetTime.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(OffsetTime.class, OffsetTime.class), new Object[][]{
                {OffsetTime.parse("00:00+09:00"), OffsetTime.parse("00:00:00+09:00")},
        });
        TEST_DB.put(pair(String.class, OffsetTime.class), new Object[][]{
                {"10:15:30+01:00", OffsetTime.parse("10:15:30+01:00"), true},
                {"10:15:30+01:00:59", OffsetTime.parse("10:15:30+01:00:59"), true},
                {"10:15:30+01:00.001", new IllegalArgumentException("Unable to parse '10:15:30+01:00.001' as an OffsetTime")},
        });
        TEST_DB.put(pair(Map.class, OffsetTime.class), new Object[][]{
                {mapOf(TIME, "00:00+09:00"), OffsetTime.parse("00:00+09:00"), true},
                {mapOf(TIME, "00:00+09:01:23"), OffsetTime.parse("00:00+09:01:23"), true},
                {mapOf(TIME, "00:00+09:01:23.1"), new IllegalArgumentException("Unable to parse OffsetTime")},
                {mapOf(TIME, "00:00-09:00"), OffsetTime.parse("00:00-09:00"), true},
                {mapOf(TIME, "00:00:00+09:00"), OffsetTime.parse("00:00+09:00")},       // no reverse
                {mapOf(TIME, "00:00:00+09:00:00"), OffsetTime.parse("00:00+09:00")},    // no reverse
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
                {new Locale.Builder().setLanguage("en").setRegion("US").build(), new Locale.Builder().setLanguage("en").setRegion("US").build()},
        });
        TEST_DB.put(pair(Map.class, Locale.class), new Object[][]{
                {mapOf(LANGUAGE, "en", COUNTRY, "US", SCRIPT, "Latn", VARIANT, "POSIX"), new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").setVariant("POSIX").build(), true},
                {mapOf(LANGUAGE, "en", COUNTRY, "US", SCRIPT, "Latn"), new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").build(), true},
                {mapOf(LANGUAGE, "en", COUNTRY, "US"), new Locale.Builder().setLanguage("en").setRegion("US").build(), true},
                {mapOf(LANGUAGE, "en"), new Locale.Builder().setLanguage("en").build(), true},
                {mapOf(V, "en-Latn-US-POSIX"), new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").setVariant("POSIX").build()},   // no reverse
                {mapOf(VALUE, "en-Latn-US-POSIX"), new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").setVariant("POSIX").build()},   // no reverse
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
    }

    /**
     * Map
     */
    private static void loadMapTests() {
        TEST_DB.put(pair(Void.class, Map.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Map.class, Map.class), new Object[][]{
                { new HashMap<>(), new IllegalArgumentException("Unsupported conversion") }
        });
        TEST_DB.put(pair(Boolean.class, Map.class), new Object[][]{
                {true, mapOf(VALUE, true)},
                {false, mapOf(VALUE, false)}
        });
        TEST_DB.put(pair(Byte.class, Map.class), new Object[][]{
                {(byte)1, mapOf(VALUE, (byte)1)},
                {(byte)2, mapOf(VALUE, (byte)2)}
        });
        TEST_DB.put(pair(Integer.class, Map.class), new Object[][]{
                {-1, mapOf(VALUE, -1)},
                {0, mapOf(VALUE, 0)},
                {1, mapOf(VALUE, 1)}
        });
        TEST_DB.put(pair(Float.class, Map.class), new Object[][]{
                {1.0f, mapOf(VALUE, 1.0f)},
                {2.0f, mapOf(VALUE, 2.0f)}
        });
        TEST_DB.put(pair(Double.class, Map.class), new Object[][]{
                {1.0, mapOf(VALUE, 1.0)},
                {2.0, mapOf(VALUE, 2.0)}
        });
        TEST_DB.put(pair(Calendar.class, Map.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }, (Supplier<Map<String, Object>>) () -> {
                    Map<String, Object> map = new CompactLinkedMap<>();
                    map.put(DATE, "2024-02-05");
                    map.put(TIME, "22:31:17.409");
                    map.put(ZONE, TOKYO);
                    map.put(EPOCH_MILLIS, 1707139877409L);
                    return map;
                }, true},
        });
        TEST_DB.put(pair(Date.class, Map.class), new Object[][] {
                { new Date(-1L), mapOf(EPOCH_MILLIS, -1L, DATE, "1970-01-01", TIME, "08:59:59.999", ZONE, TOKYO_Z.toString()), true},
                { new Date(0L), mapOf(EPOCH_MILLIS, 0L, DATE, "1970-01-01", TIME, "09:00", ZONE, TOKYO_Z.toString()), true},
                { new Date(1L), mapOf(EPOCH_MILLIS, 1L, DATE, "1970-01-01", TIME, "09:00:00.001", ZONE, TOKYO_Z.toString()), true},
                { new Date(1710714535152L), mapOf(EPOCH_MILLIS, 1710714535152L, DATE, "2024-03-18", TIME, "07:28:55.152", ZONE, TOKYO_Z.toString()), true},
        });
        TEST_DB.put(pair(java.sql.Date.class, Map.class), new Object[][] {
                { new java.sql.Date(-1L), mapOf(EPOCH_MILLIS, -1L, DATE, "1970-01-01", TIME, "08:59:59.999", ZONE, TOKYO_Z.toString()), true},
                { new java.sql.Date(0L), mapOf(EPOCH_MILLIS, 0L, DATE, "1970-01-01", TIME, "09:00", ZONE, TOKYO_Z.toString()), true},
                { new java.sql.Date(1L), mapOf(EPOCH_MILLIS, 1L, DATE, "1970-01-01", TIME, "09:00:00.001", ZONE, TOKYO_Z.toString()), true},
                { new java.sql.Date(1710714535152L), mapOf(EPOCH_MILLIS, 1710714535152L, DATE, "2024-03-18", TIME, "07:28:55.152", ZONE, TOKYO_Z.toString()), true},
        });
        TEST_DB.put(pair(Timestamp.class, Map.class), new Object[][] {
                { timestamp("1969-12-31T23:59:59.999999999Z"), mapOf(EPOCH_MILLIS, -1L, NANOS, 999999999, DATE, "1970-01-01", TIME, "08:59:59.999999999", ZONE, TOKYO_Z.toString()), true},
                { new Timestamp(-1L), mapOf(EPOCH_MILLIS, -1L, NANOS, 999000000, DATE, "1970-01-01", TIME, "08:59:59.999", ZONE, TOKYO_Z.toString()), true},
                { timestamp("1970-01-01T00:00:00Z"), mapOf(EPOCH_MILLIS, 0L, NANOS, 0, DATE, "1970-01-01", TIME, "09:00", ZONE, TOKYO_Z.toString()), true},
                { new Timestamp(0L), mapOf(EPOCH_MILLIS, 0L, NANOS, 0, DATE, "1970-01-01", TIME, "09:00", ZONE, TOKYO_Z.toString()), true},
                { timestamp("1970-01-01T00:00:00.000000001Z"), mapOf(EPOCH_MILLIS, 0L, NANOS, 1, DATE, "1970-01-01", TIME, "09:00:00.000000001", ZONE, TOKYO_Z.toString()), true},
                { new Timestamp(1L), mapOf(EPOCH_MILLIS, 1L, NANOS, 1000000, DATE, "1970-01-01", TIME, "09:00:00.001", ZONE, TOKYO_Z.toString()), true},
                { new Timestamp(1710714535152L), mapOf(EPOCH_MILLIS, 1710714535152L, NANOS, 152000000, DATE, "2024-03-18", TIME, "07:28:55.152", ZONE, TOKYO_Z.toString()), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Map.class), new Object[][] {
                { ldt("1969-12-31T23:59:59.999999999"), mapOf(DATE, "1969-12-31", TIME, "23:59:59.999999999"), true},
                { ldt("1970-01-01T00:00"), mapOf(DATE, "1970-01-01", TIME, "00:00"), true},
                { ldt("1970-01-01T00:00:00.000000001"), mapOf(DATE, "1970-01-01", TIME, "00:00:00.000000001"), true},
                { ldt("2024-03-10T11:07:00.123456789"), mapOf(DATE, "2024-03-10", TIME, "11:07:00.123456789"), true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Map.class), new Object[][] {
                { OffsetDateTime.parse("1969-12-31T23:59:59.999999999+09:00"), mapOf(DATE, "1969-12-31", TIME, "23:59:59.999999999", "offset", "+09:00"), true},
                { OffsetDateTime.parse("1970-01-01T00:00+09:00"), mapOf(DATE, "1970-01-01", TIME, "00:00", "offset", "+09:00"), true},
                { OffsetDateTime.parse("1970-01-01T00:00:00.000000001+09:00"), mapOf(DATE, "1970-01-01", TIME, "00:00:00.000000001", "offset", "+09:00"), true},
                { OffsetDateTime.parse("2024-03-10T11:07:00.123456789+09:00"), mapOf(DATE, "2024-03-10", TIME, "11:07:00.123456789", "offset", "+09:00"), true},
        });
        TEST_DB.put(pair(Duration.class, Map.class), new Object[][] {
                { Duration.ofMillis(-1), mapOf("seconds", -1L, "nanos", 999000000), true},
        });
        TEST_DB.put(pair(Instant.class, Map.class), new Object[][] {
                { Instant.parse("2024-03-10T11:07:00.123456789Z"), mapOf("seconds", 1710068820L, "nanos", 123456789), true},
        });
        TEST_DB.put(pair(Character.class, Map.class), new Object[][]{
                {(char) 0, mapOf(VALUE, (char)0)},
                {(char) 1, mapOf(VALUE, (char)1)},
                {(char) 65535, mapOf(VALUE, (char)65535)},
                {(char) 48, mapOf(VALUE, '0')},
                {(char) 49, mapOf(VALUE, '1')},
        });
        TEST_DB.put(pair(Class.class, Map.class), new Object[][]{
                { Long.class, mapOf(VALUE, Long.class), true}
        });
        TEST_DB.put(pair(Enum.class, Map.class), new Object[][]{
                { DayOfWeek.FRIDAY, mapOf("name", DayOfWeek.FRIDAY.name())}
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
        TEST_DB.put(pair(Double.class, String.class), new Object[][]{
                {0.0, "0"},
                {0.0, "0"},
                {Double.MIN_VALUE, "4.9E-324"},
                {-Double.MAX_VALUE, "-1.7976931348623157E308"},
                {Double.MAX_VALUE, "1.7976931348623157E308"},
                {123456789.0, "1.23456789E8"},
                {0.000000123456789, "1.23456789E-7"},
                {12345.0, "12345.0"},
                {0.00012345, "1.2345E-4"},
        });
        TEST_DB.put(pair(BigInteger.class, String.class), new Object[][]{
                {new BigInteger("-1"), "-1"},
                {BigInteger.ZERO, "0"},
                {new BigInteger("1"), "1"},
        });
        TEST_DB.put(pair(BigDecimal.class, String.class), new Object[][]{
                {new BigDecimal("-1"), "-1", true},
                {new BigDecimal("-1.0"), "-1", true},
                {BigDecimal.ZERO, "0", true},
                {new BigDecimal("0.0"), "0", true},
                {new BigDecimal("1.0"), "1", true},
                {new BigDecimal("3.141519265358979323846264338"), "3.141519265358979323846264338", true},
        });
        TEST_DB.put(pair(byte[].class, String.class), new Object[][]{
                {new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x8d, (byte) 0xba}, "\uD83C\uDF7A", true}, // beer mug, byte[] treated as UTF-8.
                {new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68}, "ABCD", true}
        });
        TEST_DB.put(pair(char[].class, String.class), new Object[][]{
                {new char[]{'A', 'B', 'C', 'D'}, "ABCD", true}
        });
        TEST_DB.put(pair(Character[].class, String.class), new Object[][]{
                {new Character[]{'A', 'B', 'C', 'D'}, "ABCD", true}
        });
        TEST_DB.put(pair(ByteBuffer.class, String.class), new Object[][]{
                {ByteBuffer.wrap(new byte[]{(byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33}), "0123", true}
        });
        TEST_DB.put(pair(Class.class, String.class), new Object[][]{
                {Date.class, "java.util.Date", true}
        });
        TEST_DB.put(pair(Date.class, String.class), new Object[][]{
                {new Date(-1), "1970-01-01T08:59:59.999+09:00", true},  // Tokyo (set in options - defaults to system when not set explicitly)
                {new Date(0), "1970-01-01T09:00:00.000+09:00", true},
                {new Date(1), "1970-01-01T09:00:00.001+09:00", true},
        });
        TEST_DB.put(pair(java.sql.Date.class, String.class), new Object[][]{
                {new java.sql.Date(-1), "1970-01-01T08:59:59.999+09:00", true}, // Tokyo (set in options - defaults to system when not set explicitly)
                {new java.sql.Date(0), "1970-01-01T09:00:00.000+09:00", true},
                {new java.sql.Date(1), "1970-01-01T09:00:00.001+09:00", true},
        });
        TEST_DB.put(pair(Timestamp.class, String.class), new Object[][]{
                {new Timestamp(-1), "1970-01-01T08:59:59.999+09:00", true},     // Tokyo (set in options - defaults to system when not set explicitly)
                {new Timestamp(0), "1970-01-01T09:00:00.000+09:00", true},
                {new Timestamp(1), "1970-01-01T09:00:00.001+09:00", true},
        });
        TEST_DB.put(pair(LocalDate.class, String.class), new Object[][]{
                {LocalDate.parse("1969-12-31"), "1969-12-31", true},
                {LocalDate.parse("1970-01-01"), "1970-01-01", true},
                {LocalDate.parse("2024-03-20"), "2024-03-20", true},
        });
        TEST_DB.put(pair(LocalTime.class, String.class), new Object[][]{
                {LocalTime.parse("16:20:00"), "16:20:00", true},
                {LocalTime.of(9, 26), "09:26:00", true},
                {LocalTime.of(9, 26, 17), "09:26:17", true},
                {LocalTime.of(9, 26, 17, 1), "09:26:17.000000001", true},
        });
        TEST_DB.put(pair(LocalDateTime.class, String.class), new Object[][]{
                {ldt("1965-12-31T16:20:00"), "1965-12-31T16:20:00", true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, String.class), new Object[][]{
                {ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z"), "1969-12-31T23:59:59.999999999Z", true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z"), "1970-01-01T00:00:00Z", true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z"), "1970-01-01T00:00:00.000000001Z", true},
        });
        TEST_DB.put(pair(Calendar.class, String.class), new Object[][]{
                {cal(-1), "1970-01-01T08:59:59.999+09:00", true},
                {cal(0), "1970-01-01T09:00:00.000+09:00", true},
                {cal(1), "1970-01-01T09:00:00.001+09:00", true},
        });
        TEST_DB.put(pair(Number.class, String.class), new Object[][]{
                {(byte) 1, "1"},
                {(short) 2, "2"},
                {3, "3"},
                {4L, "4"},
                {5f, "5.0"},
                {6.0, "6.0"},
                {new AtomicInteger(7), "7"},
                {new AtomicLong(8L), "8"},
                {new BigInteger("9"), "9"},
                {new BigDecimal("10"), "10"},
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
        TEST_DB.put(pair(OffsetDateTime.class, String.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-10T10:15:07+01:00"), "2024-02-10T10:15:07+01:00", true},
        });
        TEST_DB.put(pair(String.class, StringBuffer.class), new Object[][]{
                {"same", new StringBuffer("same")},
        });
        TEST_DB.put(pair(Locale.class, String.class), new Object[][]{
                { new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").setVariant("POSIX").build(), "en-Latn-US-POSIX", true},
                { new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").build(), "en-Latn-US", true},
                { new Locale.Builder().setLanguage("en").setRegion("US").build(), "en-US", true},
                { new Locale.Builder().setLanguage("en").build(), "en", true},
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
        TEST_DB.put(pair(String.class, ZoneOffset.class), new Object[][]{
                {"-00:00", ZoneOffset.of("+00:00")},
                {"-05:00", ZoneOffset.of("-05:00"), true},
                {"+5", ZoneOffset.of("+05:00")},
                {"+05:00:01", ZoneOffset.of("+05:00:01"), true},
                {"America/New_York", new IllegalArgumentException("Unknown time-zone offset: 'America/New_York'")},
        });
        TEST_DB.put(pair(Map.class, ZoneOffset.class), new Object[][]{
                {mapOf("_v", "-10"), ZoneOffset.of("-10:00")},
                {mapOf("hours", -10L), ZoneOffset.of("-10:00")},
                {mapOf("hours", -10, "minutes", 0), ZoneOffset.of("-10:00"), true},
                {mapOf("hrs", -10L, "mins", "0"), new IllegalArgumentException("Map to ZoneOffset the map must include one of the following: [hours, minutes, seconds], [_v], or [value]")},
                {mapOf("hours", -10L, "minutes", "0", "seconds", 0), ZoneOffset.of("-10:00")},
                {mapOf("hours", "-10", "minutes", (byte) -15, "seconds", "-1"), ZoneOffset.of("-10:15:01")},
                {mapOf("hours", "10", "minutes", (byte) 15, "seconds", true), ZoneOffset.of("+10:15:01")},
                {mapOf("hours", mapOf("_v", "10"), "minutes", mapOf("_v", (byte) 15), "seconds", mapOf("_v", true)), ZoneOffset.of("+10:15:01")}, // full recursion
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
        });
        TEST_DB.put(pair(Map.class, ZonedDateTime.class), new Object[][]{
                {mapOf(VALUE, new AtomicLong(now)), Instant.ofEpochMilli(now).atZone(TOKYO_Z)},
                {mapOf(EPOCH_MILLIS, now), Instant.ofEpochMilli(now).atZone(TOKYO_Z)},
                {mapOf(DATE_TIME, "1970-01-01T00:00:00", ZONE, TOKYO), zdt("1970-01-01T00:00:00+09:00")},
                {mapOf(DATE, "1969-12-31", TIME, "23:59:59.999999999", ZONE, TOKYO), zdt("1969-12-31T23:59:59.999999999+09:00"), true},
                {mapOf(DATE, "1970-01-01", TIME, "00:00", ZONE, TOKYO), zdt("1970-01-01T00:00:00+09:00"), true},
                {mapOf(DATE, "1970-01-01", TIME, "00:00:00.000000001", ZONE, TOKYO), zdt("1970-01-01T00:00:00.000000001+09:00"), true},
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
                {new java.sql.Date(-62167219200000L), zdt("0000-01-01T00:00:00Z").toLocalDateTime(), true},
                {new java.sql.Date(-62167219199999L), zdt("0000-01-01T00:00:00.001Z").toLocalDateTime(), true},
                {new java.sql.Date(-1000L), zdt("1969-12-31T23:59:59Z").toLocalDateTime(), true},
                {new java.sql.Date(-1L), zdt("1969-12-31T23:59:59.999Z").toLocalDateTime(), true},
                {new java.sql.Date(0L), zdt("1970-01-01T00:00:00Z").toLocalDateTime(), true},
                {new java.sql.Date(1L), zdt("1970-01-01T00:00:00.001Z").toLocalDateTime(), true},
                {new java.sql.Date(999L), zdt("1970-01-01T00:00:00.999Z").toLocalDateTime(), true},
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
                { new AtomicLong(-1), new IllegalArgumentException("value [-1]")},
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
        TEST_DB.put(pair(java.sql.Date.class, LocalTime.class), new Object[][]{
                { new java.sql.Date(-1L), LocalTime.parse("08:59:59.999")},
                { new java.sql.Date(0L), LocalTime.parse("09:00:00")},
                { new java.sql.Date(1L), LocalTime.parse("09:00:00.001")},
                { new java.sql.Date(1001L), LocalTime.parse("09:00:01.001")},
                { new java.sql.Date(86399999L), LocalTime.parse("08:59:59.999")},
                { new java.sql.Date(86400000L), LocalTime.parse("09:00:00")},
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
        TEST_DB.put(pair(Map.class, LocalTime.class), new Object[][] {
                {mapOf(TIME, "00:00"), LocalTime.parse("00:00:00.000000000"), true},
                {mapOf(TIME, "00:00:00.000000001"), LocalTime.parse("00:00:00.000000001"), true},
                {mapOf(TIME, "00:00"), LocalTime.parse("00:00:00"), true},
                {mapOf(TIME, "23:59:59.999999999"), LocalTime.parse("23:59:59.999999999"), true},
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
        TEST_DB.put(pair(Map.class, LocalDate.class), new Object[][] {
                {mapOf(DATE, "1969-12-31"), LocalDate.parse("1969-12-31"), true},
                {mapOf(DATE, "1970-01-01"), LocalDate.parse("1970-01-01"), true},
                {mapOf(DATE, "1970-01-02"), LocalDate.parse("1970-01-02"), true},
                {mapOf(VALUE, "2024-03-18"), LocalDate.parse("2024-03-18")},
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
        TEST_DB.put(pair(AtomicLong.class, Timestamp.class), new Object[][]{
                {new AtomicLong(-62167219200000L), timestamp("0000-01-01T00:00:00.000Z"), true},
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
                {new BigDecimal("-62167219200"), timestamp("0000-01-01T00:00:00Z"), true},
                {new BigDecimal("-62167219199.999999999"), timestamp("0000-01-01T00:00:00.000000001Z"), true},
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
                {LocalDate.parse("0000-01-01"), timestamp("0000-01-01T00:00:00Z"), true },
                {LocalDate.parse("0000-01-02"), timestamp("0000-01-02T00:00:00Z"), true },
                {LocalDate.parse("1969-12-31"), timestamp("1969-12-31T00:00:00Z"), true },
                {LocalDate.parse("1970-01-01"), timestamp("1970-01-01T00:00:00Z"), true },
                {LocalDate.parse("1970-01-02"), timestamp("1970-01-02T00:00:00Z"), true },
        });
        TEST_DB.put(pair(LocalDateTime.class, Timestamp.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z").toLocalDateTime(), new Timestamp(-62167219200000L), true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDateTime(), new Timestamp(-62167219199999L), true},
                {zdt("0000-01-01T00:00:00.000000001Z").toLocalDateTime(), (Supplier<Timestamp>) () -> {
                    Timestamp ts = new Timestamp(-62167219200000L);
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
                {Duration.ofSeconds(-62167219200L), timestamp("0000-01-01T00:00:00Z"), true},
                {Duration.ofSeconds(-62167219200L, 1), timestamp("0000-01-01T00:00:00.000000001Z"), true},
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
                {Instant.ofEpochSecond(-62167219200L), timestamp("0000-01-01T00:00:00Z"), true},
                {Instant.ofEpochSecond(-62167219200L, 1), timestamp("0000-01-01T00:00:00.000000001Z"), true},
                {Instant.ofEpochSecond(0, -1), timestamp("1969-12-31T23:59:59.999999999Z"), true},
                {Instant.ofEpochSecond(0, 0), timestamp("1970-01-01T00:00:00.000000000Z"), true},
                {Instant.ofEpochSecond(0, 1), timestamp("1970-01-01T00:00:00.000000001Z"), true},
                {Instant.parse("2024-03-10T11:36:00Z"), timestamp("2024-03-10T11:36:00Z"), true},
                {Instant.parse("2024-03-10T11:36:00.123456789Z"), timestamp("2024-03-10T11:36:00.123456789Z"), true},
        });
        // No symmetry checks - because an OffsetDateTime of "2024-02-18T06:31:55.987654321+00:00" and "2024-02-18T15:31:55.987654321+09:00" are equivalent but not equals. They both describe the same Instant.
        TEST_DB.put(pair(OffsetDateTime.class, Timestamp.class), new Object[][]{
                {OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z"), timestamp("1969-12-31T23:59:59.999999999Z")},
                {OffsetDateTime.parse("1970-01-01T00:00:00.000000000Z"), timestamp("1970-01-01T00:00:00.000000000Z")},
                {OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z"), timestamp("1970-01-01T00:00:00.000000001Z")},
                {OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), timestamp("2024-02-18T06:31:55.987654321Z")},
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
        TEST_DB.put(pair(String.class, ZoneId.class), new Object[][]{
                {"America/New_York", NY_Z, true},
                {"Asia/Tokyo", TOKYO_Z, true},
                {"America/Cincinnati", new IllegalArgumentException("Unknown time-zone ID: 'America/Cincinnati'")},
                {"Z", ZoneId.of("Z"), true},
                {"UTC", ZoneId.of("UTC"), true},
                {"GMT", ZoneId.of("GMT"), true},
        });
        TEST_DB.put(pair(TimeZone.class, ZoneId.class), new Object[][]{
                {TimeZone.getTimeZone("America/New_York"), ZoneId.of("America/New_York"),true},
                {TimeZone.getTimeZone("Asia/Tokyo"), ZoneId.of("Asia/Tokyo"),true},
                {TimeZone.getTimeZone("GMT"), ZoneId.of("GMT"), true},
                {TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"), true},
        });
        TEST_DB.put(pair(Map.class, ZoneId.class), new Object[][]{
                {mapOf("_v", "America/New_York"), NY_Z},
                {mapOf("_v", NY_Z), NY_Z},
                {mapOf("zone", "America/New_York"), NY_Z, true},
                {mapOf("zone", NY_Z), NY_Z},
                {mapOf("_v", "Asia/Tokyo"), TOKYO_Z},
                {mapOf("_v", TOKYO_Z), TOKYO_Z},
                {mapOf("zone", mapOf("_v", TOKYO_Z)), TOKYO_Z},
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
        TEST_DB.put(pair(String.class, Year.class), new Object[][]{
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
                {mapOf("_v", "P0D"), Period.of(0, 0, 0)},
                {mapOf("value", "P1Y1M1D"), Period.of(1, 1, 1)},
                {mapOf("years", "2", "months", 2, "days", 2.0), Period.of(2, 2, 2)},
                {mapOf("years", mapOf("_v", (byte) 2), "months", mapOf("_v", 2.0f), "days", mapOf("_v", new AtomicInteger(2))), Period.of(2, 2, 2)},   // recursion
                {mapOf("years", 2, "months", 5, "days", 16), Period.of(2, 5, 16), true},
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
        TEST_DB.put(pair(String.class, YearMonth.class), new Object[][]{
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
                {mapOf("_v", "2024-01"), YearMonth.of(2024, 1)},
                {mapOf("value", "2024-01"), YearMonth.of(2024, 1)},
                {mapOf("year", 2024, "month", 12), YearMonth.of(2024, 12), true},
                {mapOf("year", "2024", "month", 12), YearMonth.of(2024, 12)},
                {mapOf("year", new BigInteger("2024"), "month", "12"), YearMonth.of(2024, 12)},
                {mapOf("year", mapOf("_v", 2024), "month", "12"), YearMonth.of(2024, 12)},    // prove recursion on year
                {mapOf("year", 2024, "month", mapOf("_v", "12")), YearMonth.of(2024, 12)},    // prove recursion on month
                {mapOf("year", 2024, "month", mapOf("_v", mapOf("_v", "12"))), YearMonth.of(2024, 12)},    // prove multiple recursive calls
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
        TEST_DB.put(pair(String.class, MonthDay.class), new Object[][]{
                {"1-1", MonthDay.of(1, 1)},
                {"01-01", MonthDay.of(1, 1)},
                {"--01-01", MonthDay.of(1, 1), true},
                {"--1-1", new IllegalArgumentException("Unable to extract Month-Day from string: --1-1")},
                {"12-31", MonthDay.of(12, 31)},
                {"--12-31", MonthDay.of(12, 31), true},
                {"-12-31", new IllegalArgumentException("Unable to extract Month-Day from string: -12-31")},
                {"6-30", MonthDay.of(6, 30)},
                {"06-30", MonthDay.of(6, 30)},
                {"--06-30", MonthDay.of(6, 30), true},
                {"--6-30", new IllegalArgumentException("Unable to extract Month-Day from string: --6-30")},
        });
        TEST_DB.put(pair(Map.class, MonthDay.class), new Object[][]{
                {mapOf("_v", "1-1"), MonthDay.of(1, 1)},
                {mapOf("value", "1-1"), MonthDay.of(1, 1)},
                {mapOf("_v", "01-01"), MonthDay.of(1, 1)},
                {mapOf("_v", "--01-01"), MonthDay.of(1, 1)},
                {mapOf("_v", "--1-1"), new IllegalArgumentException("Unable to extract Month-Day from string: --1-1")},
                {mapOf("_v", "12-31"), MonthDay.of(12, 31)},
                {mapOf("_v", "--12-31"), MonthDay.of(12, 31)},
                {mapOf("_v", "-12-31"), new IllegalArgumentException("Unable to extract Month-Day from string: -12-31")},
                {mapOf("_v", "6-30"), MonthDay.of(6, 30)},
                {mapOf("_v", "06-30"), MonthDay.of(6, 30)},
                {mapOf("_v", "--06-30"), MonthDay.of(6, 30)},
                {mapOf("_v", "--6-30"), new IllegalArgumentException("Unable to extract Month-Day from string: --6-30")},
                {mapOf("month", 6, "day", 30), MonthDay.of(6, 30), true},
                {mapOf("month", 6L, "day", "30"), MonthDay.of(6, 30)},
                {mapOf("month", mapOf("_v", 6L), "day", "30"), MonthDay.of(6, 30)},    // recursive on "month"
                {mapOf("month", 6L, "day", mapOf("_v", "30")), MonthDay.of(6, 30)},    // recursive on "day"
        });
    }

    /**
     * OffsetDateTime
     */
    private static void loadOffsetDateTimeTests() {
        ZoneOffset tokyoOffset = ZonedDateTime.now(TOKYO_Z).getOffset();

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
                {new Timestamp(0), odt("1970-01-01T00:00:00+00:00"), true},
                {new Timestamp(1), odt("1970-01-01T00:00:00.001+00:00"), true},
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
                {-62167219200.0, sqlDate("0000-01-01T00:00:00Z"), true},
                {-62167219199.999, sqlDate("0000-01-01T00:00:00.001Z"), true},
                {-1.002, sqlDate("1969-12-31T23:59:58.998Z"), true},
                {-1.0, sqlDate("1969-12-31T23:59:59Z"), true},
                {-0.002, sqlDate("1969-12-31T23:59:59.998Z"), true},
                {-0.001, sqlDate("1969-12-31T23:59:59.999Z"), true},
                {0.0, sqlDate("1970-01-01T00:00:00.000000000Z"), true},
                {0.001, sqlDate("1970-01-01T00:00:00.001Z"), true},
                {0.999, sqlDate("1970-01-01T00:00:00.999Z"), true},
                {1.0, sqlDate("1970-01-01T00:00:01Z"), true},
        });
        TEST_DB.put(pair(AtomicLong.class, java.sql.Date.class), new Object[][]{
                {new AtomicLong(-62167219200000L), sqlDate("0000-01-01T00:00:00Z"), true},
                {new AtomicLong(-62167219199999L), sqlDate("0000-01-01T00:00:00.001Z"), true},
                {new AtomicLong(-1001), sqlDate("1969-12-31T23:59:58.999Z"), true},
                {new AtomicLong(-1000), sqlDate("1969-12-31T23:59:59Z"), true},
                {new AtomicLong(-1), sqlDate("1969-12-31T23:59:59.999Z"), true},
                {new AtomicLong(0), sqlDate("1970-01-01T00:00:00Z"), true},
                {new AtomicLong(1), sqlDate("1970-01-01T00:00:00.001Z"), true},
                {new AtomicLong(999), sqlDate("1970-01-01T00:00:00.999Z"), true},
                {new AtomicLong(1000), sqlDate("1970-01-01T00:00:01Z"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, java.sql.Date.class), new Object[][]{
                {new BigDecimal("-62167219200"), sqlDate("0000-01-01T00:00:00Z"), true},
                {new BigDecimal("-62167219199.999"), sqlDate("0000-01-01T00:00:00.001Z"), true},
                {new BigDecimal("-1.001"), sqlDate("1969-12-31T23:59:58.999Z"), true},
                {new BigDecimal("-1"), sqlDate("1969-12-31T23:59:59Z"), true},
                {new BigDecimal("-0.001"), sqlDate("1969-12-31T23:59:59.999Z"), true},
                {BigDecimal.ZERO, sqlDate("1970-01-01T00:00:00.000000000Z"), true},
                {new BigDecimal("0.001"), sqlDate("1970-01-01T00:00:00.001Z"), true},
                {new BigDecimal(".999"), sqlDate("1970-01-01T00:00:00.999Z"), true},
                {new BigDecimal("1"), sqlDate("1970-01-01T00:00:01Z"), true},
        });
        TEST_DB.put(pair(Date.class, java.sql.Date.class), new Object[][] {
                {new Date(Long.MIN_VALUE), new java.sql.Date(Long.MIN_VALUE), true },
                {new Date(-1), new java.sql.Date(-1), true },
                {new Date(0), new java.sql.Date(0), true },
                {new Date(1), new java.sql.Date(1), true },
                {new Date(Long.MAX_VALUE), new java.sql.Date(Long.MAX_VALUE), true },
        });
        TEST_DB.put(pair(OffsetDateTime.class, java.sql.Date.class), new Object[][]{
                {odt("1969-12-31T23:59:59Z"), new java.sql.Date(-1000), true},
                {odt("1969-12-31T23:59:59.999Z"), new java.sql.Date(-1), true},
                {odt("1970-01-01T00:00:00Z"), new java.sql.Date(0), true},
                {odt("1970-01-01T00:00:00.001Z"), new java.sql.Date(1), true},
                {odt("1970-01-01T00:00:00.999Z"), new java.sql.Date(999), true},
        });
        TEST_DB.put(pair(Timestamp.class, java.sql.Date.class), new Object[][]{
                {new Timestamp(Long.MIN_VALUE), new java.sql.Date(Long.MIN_VALUE), true},
                {new Timestamp(Integer.MIN_VALUE), new java.sql.Date(Integer.MIN_VALUE), true},
                {new Timestamp(now), new java.sql.Date(now), true},
                {new Timestamp(-1), new java.sql.Date(-1), true},
                {new Timestamp(0), new java.sql.Date(0), true},
                {new Timestamp(1), new java.sql.Date(1), true},
                {new Timestamp(Integer.MAX_VALUE), new java.sql.Date(Integer.MAX_VALUE), true},
                {new Timestamp(Long.MAX_VALUE), new java.sql.Date(Long.MAX_VALUE), true},
                {timestamp("1969-12-31T23:59:59.999Z"), new java.sql.Date(-1), true},
                {timestamp("1970-01-01T00:00:00.000Z"), new java.sql.Date(0), true},
                {timestamp("1970-01-01T00:00:00.001Z"), new java.sql.Date(1), true},
        });
        TEST_DB.put(pair(LocalDate.class, java.sql.Date.class), new Object[][] {
                {zdt("0000-01-01T00:00:00Z").toLocalDate(), new java.sql.Date(-62167252739000L), true},
                {zdt("0000-01-01T00:00:00.001Z").toLocalDate(), new java.sql.Date(-62167252739000L), true},
                {zdt("1969-12-31T14:59:59.999Z").toLocalDate(), new java.sql.Date(-118800000L), true},
                {zdt("1969-12-31T15:00:00Z").toLocalDate(), new java.sql.Date(-32400000L), true},
                {zdt("1969-12-31T23:59:59.999Z").toLocalDate(), new java.sql.Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00Z").toLocalDate(), new java.sql.Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.001Z").toLocalDate(), new java.sql.Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDate(), new java.sql.Date(-32400000L), true},
                {zdt("1970-01-01T00:00:00.999Z").toLocalDate(), new java.sql.Date(-32400000L), true},
        });
        TEST_DB.put(pair(Calendar.class, java.sql.Date.class), new Object[][] {
                {cal(now), new java.sql.Date(now), true},
                {cal(0), new java.sql.Date(0), true}
        });
        TEST_DB.put(pair(Instant.class, java.sql.Date.class), new Object[][]{
                {Instant.parse("0000-01-01T00:00:00Z"), new java.sql.Date(-62167219200000L), true},
                {Instant.parse("0000-01-01T00:00:00.001Z"), new java.sql.Date(-62167219199999L), true},
                {Instant.parse("1969-12-31T23:59:59Z"), new java.sql.Date(-1000L), true},
                {Instant.parse("1969-12-31T23:59:59.999Z"), new java.sql.Date(-1L), true},
                {Instant.parse("1970-01-01T00:00:00Z"), new java.sql.Date(0L), true},
                {Instant.parse("1970-01-01T00:00:00.001Z"), new java.sql.Date(1L), true},
                {Instant.parse("1970-01-01T00:00:00.999Z"), new java.sql.Date(999L), true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, java.sql.Date.class), new Object[][]{
                {zdt("0000-01-01T00:00:00Z"), new java.sql.Date(-62167219200000L), true},
                {zdt("0000-01-01T00:00:00.001Z"), new java.sql.Date(-62167219199999L), true},
                {zdt("1969-12-31T23:59:59Z"), new java.sql.Date(-1000), true},
                {zdt("1969-12-31T23:59:59.999Z"), new java.sql.Date(-1), true},
                {zdt("1970-01-01T00:00:00Z"), new java.sql.Date(0), true},
                {zdt("1970-01-01T00:00:00.001Z"), new java.sql.Date(1), true},
                {zdt("1970-01-01T00:00:00.999Z"), new java.sql.Date(999), true},
        });
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
                {new Timestamp(Long.MIN_VALUE), new Date(Long.MIN_VALUE), true},
                {new Timestamp(Integer.MIN_VALUE), new Date(Integer.MIN_VALUE), true},
                {new Timestamp(now), new Date(now), true},
                {new Timestamp(-1), new Date(-1), true},
                {new Timestamp(0), new Date(0), true},
                {new Timestamp(1), new Date(1), true},
                {new Timestamp(Integer.MAX_VALUE), new Date(Integer.MAX_VALUE), true},
                {new Timestamp(Long.MAX_VALUE), new Date(Long.MAX_VALUE), true},
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
                {(Supplier<Map<String, Object>>) () -> {
                    Map<String, Object> map = new CompactLinkedMap<>();
                    map.put(VALUE, "2024-02-05T22:31:17.409[" + TOKYO + "]");
                    return map;
                }, (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }},
                {(Supplier<Map<String, Object>>) () -> {
                    Map<String, Object> map = new CompactLinkedMap<>();
                    map.put(VALUE, "2024-02-05T22:31:17.409" + TOKYO_ZO.toString());
                    return map;
                }, (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }},
                {(Supplier<Map<String, Object>>) () -> {
                    Map<String, Object> map = new CompactLinkedMap<>();
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    map.put(VALUE, cal);
                    return map;
                }, (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 17);
                    cal.set(Calendar.MILLISECOND, 409);
                    return cal;
                }},
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
                {new Date(Long.MIN_VALUE), Instant.ofEpochMilli(Long.MIN_VALUE), true },
                {new Date(-1), Instant.ofEpochMilli(-1), true },
                {new Date(0), Instant.ofEpochMilli(0), true },
                {new Date(1), Instant.ofEpochMilli(1), true },
                {new Date(Long.MAX_VALUE), Instant.ofEpochMilli(Long.MAX_VALUE), true },
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
                {sqlDate("0000-01-01T00:00:00Z"), new BigInteger("-62167219200000000000"), true},
                {sqlDate("0001-02-18T19:58:01Z"), new BigInteger("-62131377719000000000"), true},
                {sqlDate("1969-12-31T23:59:59Z"), BigInteger.valueOf(-1_000_000_000), true},
                {sqlDate("1969-12-31T23:59:59.1Z"), BigInteger.valueOf(-900000000), true},
                {sqlDate("1969-12-31T23:59:59.9Z"), BigInteger.valueOf(-100000000), true},
                {sqlDate("1970-01-01T00:00:00Z"), BigInteger.ZERO, true},
                {sqlDate("1970-01-01T00:00:00.1Z"), BigInteger.valueOf(100000000), true},
                {sqlDate("1970-01-01T00:00:00.9Z"), BigInteger.valueOf(900000000), true},
                {sqlDate("1970-01-01T00:00:01Z"), BigInteger.valueOf(1000000000), true},
                {sqlDate("9999-02-18T19:58:01Z"), new BigInteger("253374983881000000000"), true},
        });
        TEST_DB.put(pair(Timestamp.class, BigInteger.class), new Object[][]{
                {timestamp("0000-01-01T00:00:00.000000000Z"), new BigInteger("-62167219200000000000"), true},
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
        TEST_DB.put(pair(Number.class, BigInteger.class), new Object[][]{
                {0, BigInteger.ZERO},
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
        TEST_DB.put(pair(Number.class, Character.class), new Object[][]{
                {BigDecimal.valueOf(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {BigDecimal.ZERO, (char) 0},
                {BigInteger.valueOf(1), (char) 1},
                {BigInteger.valueOf(65535), (char) 65535},
                {BigInteger.valueOf(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Map.class, Character.class), new Object[][]{
                {mapOf("_v", -1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {mapOf("value", 0), (char) 0},
                {mapOf("_v", 1), (char) 1},
                {mapOf("_v", 65535), (char) 65535},
                {mapOf("_v", mapOf("_v", 65535)), (char) 65535},
                {mapOf("_v", "0"), (char) 48},
                {mapOf("_v", 65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(String.class, Character.class), new Object[][]{
                {" ", (char) 32, true},
                {"0", '0', true},
                {"1", '1', true},
                {"A", 'A', true},
                {"{", '{', true},
                {"\uD83C", '\uD83C', true},
                {"\uFFFF", '\uFFFF', true},
                {"FFFZ", new IllegalArgumentException("Unable to parse 'FFFZ' as a Character")},
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
        TEST_DB.put(pair(Number.class, Boolean.class), new Object[][]{
                {-2, true},
                {-1L, true},
                {0.0, false},
                {1.0f, true},
                {BigInteger.valueOf(2), true},
        });
        TEST_DB.put(pair(Map.class, Boolean.class), new Object[][]{
                {mapOf("_v", 16), true},
                {mapOf("_v", 0), false},
                {mapOf("_v", "0"), false},
                {mapOf("_v", "1"), true},
                {mapOf("_v", mapOf("_v", 5.0)), true},
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
        TEST_DB.put(pair(Number.class, Double.class), new Object[][]{
                {2.5f, 2.5}
        });
        TEST_DB.put(pair(Map.class, Double.class), new Object[][]{
                {mapOf("_v", "-1"), -1.0},
                {mapOf("_v", -1), -1.0},
                {mapOf("value", "-1"), -1.0},
                {mapOf("value", -1L), -1.0},

                {mapOf("_v", "0"), 0.0},
                {mapOf("_v", 0), 0.0},

                {mapOf("_v", "1"), 1.0},
                {mapOf("_v", 1), 1.0},

                {mapOf("_v", "-9007199254740991"), -9007199254740991.0},
                {mapOf("_v", -9007199254740991L), -9007199254740991.0},

                {mapOf("_v", "9007199254740991"), 9007199254740991.0},
                {mapOf("_v", 9007199254740991L), 9007199254740991.0},

                {mapOf("_v", mapOf("_v", -9007199254740991L)), -9007199254740991.0},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Double.class), new Object[][]{
                {"-1", -1.0},
                {"-1.1", -1.1},
                {"-1.9", -1.9},
                {"0", 0.0},
                {"1", 1.0},
                {"1.1", 1.1},
                {"1.9", 1.9},
                {"-2147483648", -2147483648.0},
                {"2147483647", 2147483647.0},
                {"", 0.0},
                {" ", 0.0},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a double")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a double")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a double")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a double")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a double")},
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
        TEST_DB.put(pair(Number.class, Float.class), new Object[][]{
                {-2.2, -2.2f}
        });
        TEST_DB.put(pair(Map.class, Float.class), new Object[][]{
                {mapOf("_v", "-1"), -1f},
                {mapOf("_v", -1), -1f},
                {mapOf("value", "-1"), -1f},
                {mapOf("value", -1L), -1f},

                {mapOf("_v", "0"), 0f},
                {mapOf("_v", 0), 0f},

                {mapOf("_v", "1"), 1f},
                {mapOf("_v", 1), 1f},

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
        TEST_DB.put(pair(Number.class, Long.class), new Object[][]{
                {-2, -2L},
        });
        TEST_DB.put(pair(Map.class, Long.class), new Object[][]{
                {mapOf("_v", "-1"), -1L},
                {mapOf("_v", -1L), -1L, true},
                {mapOf("value", "-1"), -1L},
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
                {new java.sql.Date(Long.MIN_VALUE), Long.MIN_VALUE, true},
                {new java.sql.Date(Integer.MIN_VALUE), (long) Integer.MIN_VALUE, true},
                {new java.sql.Date(now), now, true},
                {new java.sql.Date(0), 0L, true},
                {new java.sql.Date(Integer.MAX_VALUE), (long) Integer.MAX_VALUE, true},
                {new java.sql.Date(Long.MAX_VALUE), Long.MAX_VALUE, true},
        });
        TEST_DB.put(pair(Timestamp.class, Long.class), new Object[][]{
                {new Timestamp(Long.MIN_VALUE), Long.MIN_VALUE, true},
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
        TEST_DB.put(pair(Number.class, Integer.class), new Object[][]{
                {-2L, -2},
        });
        TEST_DB.put(pair(Map.class, Integer.class), new Object[][]{
                {mapOf("_v", "-1"), -1},
                {mapOf("_v", -1), -1},
                {mapOf("value", "-1"), -1},
                {mapOf("value", -1L), -1},

                {mapOf("_v", "0"), 0},
                {mapOf("_v", 0), 0},

                {mapOf("_v", "1"), 1},
                {mapOf("_v", 1), 1},

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
        TEST_DB.put(pair(Number.class, Short.class), new Object[][]{
                {-2L, (short) -2},
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
        TEST_DB.put(pair(Number.class, Byte.class), new Object[][]{
                {-2L, (byte) -2},
        });
        TEST_DB.put(pair(Map.class, Byte.class), new Object[][]{
                {mapOf("_v", "-1"), (byte) -1},
                {mapOf("_v", -1), (byte) -1},
                {mapOf("value", "-1"), (byte) -1},
                {mapOf("value", -1L), (byte) -1},

                {mapOf("_v", "0"), (byte) 0},
                {mapOf("_v", 0), (byte) 0},

                {mapOf("_v", "1"), (byte) 1},
                {mapOf("_v", 1), (byte) 1},

                {mapOf("_v", "-128"), Byte.MIN_VALUE},
                {mapOf("_v", -128), Byte.MIN_VALUE},

                {mapOf("_v", "127"), Byte.MAX_VALUE},
                {mapOf("_v", 127), Byte.MAX_VALUE},

                {mapOf("_v", "-129"), new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127")},
                {mapOf("_v", -129), Byte.MAX_VALUE},

                {mapOf("_v", "128"), new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127")},
                {mapOf("_v", 128), Byte.MIN_VALUE},
                {mapOf("_v", mapOf("_v", 128L)), Byte.MIN_VALUE},    // Prove use of recursive call to .convert()
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
                {ByteBuffer.wrap(new byte[]{1, 2}), new byte[] {1, 2}, true},
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
                {CharBuffer.wrap(new char[] {'h', 'i'}), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(StringBuffer.class, char[].class), new Object[][]{
                {new StringBuffer("hi"), new char[] {'h', 'i'}, true},
        });
        TEST_DB.put(pair(StringBuilder.class, char[].class), new Object[][]{
                {new StringBuilder("hi"), new char[] {'h', 'i'}, true},
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

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParams")
    void testConvert(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass, int index) {
        if (index == 0) {
            Map.Entry<Class<?>, Class<?>> entry = pair(sourceClass, targetClass);
            Boolean alreadyCompleted = STAT_DB.get(entry);
            if (Boolean.TRUE.equals(alreadyCompleted) && !sourceClass.equals(targetClass)) {
                System.err.println("Duplicate test pair: " + shortNameSource + " ==> " + shortNameTarget);
            }
        }

        if (source == null) {
            assertEquals(sourceClass, Void.class, "On the source-side of test input, null can only appear in the Void.class data");
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
            assertThatExceptionOfType(t.getClass())
                    .isThrownBy(() -> converter.convert(source, targetClass))
                    .withMessageContaining(((Throwable) target).getMessage());
        } else {
            // Assert values are equals
            Object actual = converter.convert(source, targetClass);
            try {
                if (target instanceof CharSequence) {
                    assertEquals(actual.toString(), target.toString());
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(byte[].class)) {
                    assertArrayEquals((byte[]) actual, (byte[]) target);
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(char[].class)) {
                    assertArrayEquals((char[]) actual, (char[]) target);
                    updateStat(pair(sourceClass, targetClass), true);
                } else if (targetClass.equals(Character[].class)) {
                    assertArrayEquals((Character[]) actual, (Character[]) target);
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
                } else {
                    assertEquals(actual, target);
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

    private static java.sql.Date sqlDate(String s) {
        return new java.sql.Date(Instant.parse(s).toEpochMilli());
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
