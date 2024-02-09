package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static com.cedarsoftware.util.convert.Converter.getShortName;
import static com.cedarsoftware.util.convert.Converter.pair;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class ConverterEverythingTest {
    private static final TimeZone TZ_TOKYO = TimeZone.getTimeZone("Asia/Tokyo");
    private Converter converter;
    private ConverterOptions options = new ConverterOptions() {
        public TimeZone getTimeZone() {
            return TZ_TOKYO;
        }
    };
    private static final Map<Map.Entry<Class<?>, Class<?>>, Object[][]> TEST_FACTORY = new ConcurrentHashMap<>(500, .8f);

    static {
        //   {source1, answer1},
        //   ...
        //   {source-n, answer-n}

        // Byte/byte
        TEST_FACTORY.put(pair(Void.class, byte.class), new Object[][] {
                { null, (byte) 0 }
        });
        TEST_FACTORY.put(pair(Void.class, Byte.class), new Object[][] {
                { null, null }
        });
        TEST_FACTORY.put(pair(Byte.class, Byte.class), new Object[][] {
                { (byte) -1, (byte) -1 },
                { (byte) 0, (byte) 0 },
                { (byte) 1, (byte) 1 },
                { Byte.MIN_VALUE, Byte.MIN_VALUE },
                { Byte.MAX_VALUE, Byte.MAX_VALUE }
        });
        TEST_FACTORY.put(pair(Short.class, Byte.class), new Object[][] {
                { (short) -1, (byte) -1 },
                { (short) 0, (byte) 0 },
                { (short) 1, (byte) 1 },
                { (short) -128, Byte.MIN_VALUE },
                { (short) 127, Byte.MAX_VALUE },
                { (short) -129, (byte) 127 },  // verify wrap around
                { (short) 128, (byte) -128 }    // verify wrap around
        });
        TEST_FACTORY.put(pair(Integer.class, Byte.class), new Object[][] {
                { -1, (byte) -1 },
                { 0, (byte) 0 },
                { 1, (byte) 1 },
                { -128, Byte.MIN_VALUE },
                { 127, Byte.MAX_VALUE },
                { -129, (byte) 127 }, // verify wrap around
                { 128, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Long.class, Byte.class), new Object[][] {
                { -1L, (byte) -1 },
                { 0L, (byte) 0 },
                { 1L, (byte) 1 },
                { -128L, Byte.MIN_VALUE },
                { 127L, Byte.MAX_VALUE },
                { -129L, (byte) 127 }, // verify wrap around
                { 128L, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Float.class, Byte.class), new Object[][] {
                { -1f, (byte) -1 },
                { -1.99f, (byte) -1 },
                { -1.1f, (byte) -1 },
                { 0f, (byte) 0 },
                { 1f, (byte) 1 },
                { 1.1f, (byte) 1 },
                { 1.999f, (byte) 1 },
                { -128f, Byte.MIN_VALUE },
                { 127f, Byte.MAX_VALUE },
                { -129f, (byte) 127 }, // verify wrap around
                { 128f, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Double.class, Byte.class), new Object[][] {
                { -1d, (byte) -1 },
                { -1.99d, (byte) -1 },
                { -1.1d, (byte) -1 },
                { 0d, (byte) 0 },
                { 1d, (byte) 1 },
                { 1.1d, (byte) 1 },
                { 1.999d, (byte) 1 },
                { -128d, Byte.MIN_VALUE },
                { 127d, Byte.MAX_VALUE },
                { -129d, (byte) 127 }, // verify wrap around
                { 128d, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Boolean.class, Byte.class), new Object[][] {
                { true, (byte) 1 },
                { false, (byte) 0 },
        });
        TEST_FACTORY.put(pair(Character.class, Byte.class), new Object[][] {
                { '1', (byte) 49 },
                { '0', (byte) 48 },
                { (char) 1, (byte) 1 },
                { (char) 0, (byte) 0 },
        });
        TEST_FACTORY.put(pair(AtomicBoolean.class, Byte.class), new Object[][] {
                { new AtomicBoolean(true), (byte) 1 },
                { new AtomicBoolean(false), (byte) 0 },
        });
        TEST_FACTORY.put(pair(AtomicInteger.class, Byte.class), new Object[][] {
                { new AtomicInteger(-1), (byte) -1 },
                { new AtomicInteger(0), (byte) 0 },
                { new AtomicInteger(1), (byte) 1 },
                { new AtomicInteger(-128), Byte.MIN_VALUE },
                { new AtomicInteger(127), Byte.MAX_VALUE },
                { new AtomicInteger(-129), (byte) 127 },
                { new AtomicInteger(128), (byte) -128 },
        });
        TEST_FACTORY.put(pair(AtomicLong.class, Byte.class), new Object[][] {
                { new AtomicLong(-1), (byte) -1 },
                { new AtomicLong(0), (byte) 0 },
                { new AtomicLong(1), (byte) 1 },
                { new AtomicLong(-128), Byte.MIN_VALUE },
                { new AtomicLong(127), Byte.MAX_VALUE },
                { new AtomicLong(-129), (byte) 127 },
                { new AtomicLong(128), (byte) -128 },
        });
        TEST_FACTORY.put(pair(BigInteger.class, Byte.class), new Object[][] {
                { new BigInteger("-1"), (byte) -1 },
                { new BigInteger("0"), (byte) 0 },
                { new BigInteger("1"), (byte) 1 },
                { new BigInteger("-128"), Byte.MIN_VALUE },
                { new BigInteger("127"), Byte.MAX_VALUE },
                { new BigInteger("-129"), (byte) 127 },
                { new BigInteger("128"), (byte) -128 },
        });
        TEST_FACTORY.put(pair(BigDecimal.class, Byte.class), new Object[][] {
                { new BigDecimal("-1"), (byte) -1 },
                { new BigDecimal("-1.1"), (byte) -1 },
                { new BigDecimal("-1.9"), (byte) -1 },
                { new BigDecimal("0"), (byte) 0 },
                { new BigDecimal("1"), (byte) 1 },
                { new BigDecimal("1.1"), (byte) 1 },
                { new BigDecimal("1.9"), (byte) 1 },
                { new BigDecimal("-128"), Byte.MIN_VALUE },
                { new BigDecimal("127"), Byte.MAX_VALUE },
                { new BigDecimal("-129"), (byte) 127 },
                { new BigDecimal("128"), (byte) -128 },
        });
        TEST_FACTORY.put(pair(Number.class, Byte.class), new Object[][] {
                { -2L, (byte) -2 },
        });
        TEST_FACTORY.put(pair(Map.class, Byte.class), new Object[][] {
                { mapOf("_v", "-1"), (byte) -1 },
                { mapOf("_v", -1), (byte) -1 },
                { mapOf("value", "-1"), (byte) -1 },
                { mapOf("value", -1L), (byte) -1 },

                { mapOf("_v", "0"), (byte) 0 },
                { mapOf("_v", 0), (byte) 0 },

                { mapOf("_v", "1"), (byte) 1 },
                { mapOf("_v", 1), (byte) 1 },

                { mapOf("_v", "-128"), Byte.MIN_VALUE },
                { mapOf("_v", -128), Byte.MIN_VALUE },

                { mapOf("_v", "127"), Byte.MAX_VALUE },
                { mapOf("_v", 127), Byte.MAX_VALUE },

                { mapOf("_v", "-129"), new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", -129), (byte) 127 },

                { mapOf("_v", "128"), new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", 128), (byte) -128 },
                { mapOf("_v", mapOf("_v", 128L)), (byte) -128 },    // Prove use of recursive call to .convert()
        });
        TEST_FACTORY.put(pair(String.class, Byte.class), new Object[][] {
                { "-1", (byte) -1 },
                { "-1.1", (byte) -1 },
                { "-1.9", (byte) -1 },
                { "0", (byte) 0 },
                { "1", (byte) 1 },
                { "1.1", (byte) 1 },
                { "1.9", (byte) 1 },
                { "-128", (byte) -128 },
                { "127", (byte) 127 },
                { "", (byte) 0 },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a byte value or outside -128 to 127") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a byte value or outside -128 to 127") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a byte value or outside -128 to 127") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a byte value or outside -128 to 127") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a byte value or outside -128 to 127") },
                { "-129", new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127") },
                { "128", new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127") },
        });

        // MonthDay
        TEST_FACTORY.put(pair(Void.class, MonthDay.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(MonthDay.class, MonthDay.class), new Object[][] {
                { MonthDay.of(1, 1), MonthDay.of(1, 1) },
                { MonthDay.of(12, 31), MonthDay.of(12, 31) },
                { MonthDay.of(6, 30), MonthDay.of(6, 30) },
        });
        TEST_FACTORY.put(pair(String.class, MonthDay.class), new Object[][] {
                { "1-1", MonthDay.of(1, 1) },
                { "01-01", MonthDay.of(1, 1) },
                { "--01-01", MonthDay.of(1, 1) },
                { "--1-1", new IllegalArgumentException("Unable to extract Month-Day from string: --1-1") },
                { "12-31", MonthDay.of(12, 31) },
                { "--12-31", MonthDay.of(12, 31) },
                { "-12-31", new IllegalArgumentException("Unable to extract Month-Day from string: -12-31") },
                { "6-30", MonthDay.of(6, 30) },
                { "06-30", MonthDay.of(6, 30) },
                { "--06-30", MonthDay.of(6, 30) },
                { "--6-30", new IllegalArgumentException("Unable to extract Month-Day from string: --6-30") },
        });
        TEST_FACTORY.put(pair(Map.class, MonthDay.class), new Object[][] {
                { mapOf("_v", "1-1"), MonthDay.of(1, 1) },
                { mapOf("value", "1-1"), MonthDay.of(1, 1) },
                { mapOf("_v", "01-01"), MonthDay.of(1, 1) },
                { mapOf("_v", "--01-01"), MonthDay.of(1, 1) },
                { mapOf("_v", "--1-1"), new IllegalArgumentException("Unable to extract Month-Day from string: --1-1") },
                { mapOf("_v", "12-31"), MonthDay.of(12, 31) },
                { mapOf("_v", "--12-31"), MonthDay.of(12, 31) },
                { mapOf("_v", "-12-31"), new IllegalArgumentException("Unable to extract Month-Day from string: -12-31") },
                { mapOf("_v", "6-30"), MonthDay.of(6, 30) },
                { mapOf("_v", "06-30"), MonthDay.of(6, 30) },
                { mapOf("_v", "--06-30"), MonthDay.of(6, 30) },
                { mapOf("_v", "--6-30"), new IllegalArgumentException("Unable to extract Month-Day from string: --6-30") },
                { mapOf("month", "6", "day", 30), MonthDay.of(6, 30) },
                { mapOf("month", 6L, "day", "30"), MonthDay.of(6, 30) },
                { mapOf("month", mapOf("_v", 6L), "day", "30"), MonthDay.of(6, 30) },    // recursive on "month"
                { mapOf("month", 6L, "day", mapOf("_v", "30")), MonthDay.of(6, 30) },    // recursive on "day"
        });

        // YearMonth
        TEST_FACTORY.put(pair(Void.class, YearMonth.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(YearMonth.class, YearMonth.class), new Object[][] {
                { YearMonth.of(2023, 12), YearMonth.of(2023, 12) },
                { YearMonth.of(1970, 1), YearMonth.of(1970, 1) },
                { YearMonth.of(1999, 6), YearMonth.of(1999, 6) },
        });
        TEST_FACTORY.put(pair(String.class, YearMonth.class), new Object[][] {
                { "2024-01", YearMonth.of(2024, 1) },
                { "2024-1", new IllegalArgumentException("Unable to extract Year-Month from string: 2024-1") },
                { "2024-1-1", YearMonth.of(2024, 1) },
                { "2024-06-01", YearMonth.of(2024, 6) },
                { "2024-12-31", YearMonth.of(2024, 12) },
                { "05:45 2024-12-31", YearMonth.of(2024, 12) },
        });
        TEST_FACTORY.put(pair(Map.class, YearMonth.class), new Object[][] {
                { mapOf("_v", "2024-01"), YearMonth.of(2024, 1) },
                { mapOf("value", "2024-01"), YearMonth.of(2024, 1) },
                { mapOf("year", "2024", "month", 12), YearMonth.of(2024, 12) },
                { mapOf("year", new BigInteger("2024"), "month", "12"), YearMonth.of(2024, 12) },
                { mapOf("year", mapOf("_v", 2024), "month", "12"), YearMonth.of(2024, 12) },    // prove recursion on year
                { mapOf("year", 2024, "month", mapOf("_v", "12")), YearMonth.of(2024, 12) },    // prove recursion on month
                { mapOf("year", 2024, "month", mapOf("_v", mapOf("_v", "12"))), YearMonth.of(2024, 12) },    // prove multiple recursive calls
        });

        // Period
        TEST_FACTORY.put(pair(Void.class, Period.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(Period.class, Period.class), new Object[][] {
                { Period.of(0, 0, 0), Period.of(0, 0, 0) },
                { Period.of(1, 1, 1), Period.of(1, 1, 1) },
        });
        TEST_FACTORY.put(pair(String.class, Period.class), new Object[][] {
                { "P0D", Period.of(0, 0, 0) },
                { "P1D", Period.of(0, 0, 1) },
                { "P1M", Period.of(0, 1, 0) },
                { "P1Y", Period.of(1, 0, 0) },
                { "P1Y1M", Period.of(1, 1, 0) },
                { "P1Y1D", Period.of(1, 0, 1) },
                { "P1Y1M1D", Period.of(1, 1, 1) },
                { "P10Y10M10D", Period.of(10, 10, 10) },
                { "PONY", new IllegalArgumentException("Unable to parse 'PONY' as a Period.") },
        });
        TEST_FACTORY.put(pair(Map.class, Period.class), new Object[][] {
                { mapOf("_v", "P0D"), Period.of(0, 0, 0) },
                { mapOf("value", "P1Y1M1D"), Period.of(1, 1, 1) },
                { mapOf("years", "2", "months", 2, "days", 2.0d), Period.of(2, 2, 2) },
                { mapOf("years", mapOf("_v", (byte) 2), "months", mapOf("_v", 2.0f), "days", mapOf("_v", new AtomicInteger(2))), Period.of(2, 2, 2) },   // recursion
        });

        // Year
        TEST_FACTORY.put(pair(Void.class, Year.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(Year.class, Year.class), new Object[][] {
                { Year.of(1970), Year.of(1970) },
        });
        TEST_FACTORY.put(pair(String.class, Year.class), new Object[][] {
                { "1970", Year.of(1970) },
                { "1999", Year.of(1999) },
                { "2000", Year.of(2000) },
                { "2024", Year.of(2024) },
                { "1670", Year.of(1670) },
                { "PONY", new IllegalArgumentException("Unable to parse 4-digit year from 'PONY'") },
        });
        TEST_FACTORY.put(pair(Map.class, Year.class), new Object[][] {
                { mapOf("_v", "1984"), Year.of(1984) },
                { mapOf("value", 1984L), Year.of(1984) },
                { mapOf("year", 1492), Year.of(1492) },
                { mapOf("year", mapOf("_v", (short) 2024)), Year.of(2024) }, // recursion
        });
        TEST_FACTORY.put(pair(Number.class, Year.class), new Object[][] {
                { (byte) 101, new IllegalArgumentException("Unsupported conversion, source type [Byte (101)] target type 'Year'") },
                { (short) 2024, Year.of(2024) },
        });

        // ZoneId
        ZoneId NY_Z = ZoneId.of("America/New_York");
        ZoneId TOKYO_Z = ZoneId.of("Asia/Tokyo");
        TEST_FACTORY.put(pair(Void.class, ZoneId.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(ZoneId.class, ZoneId.class), new Object[][] {
                { NY_Z, NY_Z },
                { TOKYO_Z, TOKYO_Z },
        });
        TEST_FACTORY.put(pair(String.class, ZoneId.class), new Object[][] {
                { "America/New_York", NY_Z },
                { "Asia/Tokyo", TOKYO_Z },
                { "America/Cincinnati", new IllegalArgumentException("Unknown time-zone ID: 'America/Cincinnati'") },
        });
        TEST_FACTORY.put(pair(Map.class, ZoneId.class), new Object[][] {
                { mapOf("_v", "America/New_York"), NY_Z },
                { mapOf("_v", NY_Z), NY_Z },
                { mapOf("zone", NY_Z), NY_Z },
                { mapOf("_v", "Asia/Tokyo"), TOKYO_Z },
                { mapOf("_v", TOKYO_Z), TOKYO_Z },
                { mapOf("zone", mapOf("_v", TOKYO_Z)), TOKYO_Z },
        });

        // ZoneOffset
        TEST_FACTORY.put(pair(Void.class, ZoneOffset.class), new Object[][] {
                { null, null },
        });
        TEST_FACTORY.put(pair(ZoneOffset.class, ZoneOffset.class), new Object[][] {
                { ZoneOffset.of("-05:00"), ZoneOffset.of("-05:00") },
                { ZoneOffset.of("+5"), ZoneOffset.of("+05:00") },
        });
        TEST_FACTORY.put(pair(String.class, ZoneOffset.class), new Object[][] {
                { "-00:00", ZoneOffset.of("+00:00") },
                { "-05:00", ZoneOffset.of("-05:00") },
                { "+5", ZoneOffset.of("+05:00") },
                { "+05:00:01", ZoneOffset.of("+05:00:01") },
                { "America/New_York", new IllegalArgumentException("Unknown time-zone offset: 'America/New_York'") },
        });
        TEST_FACTORY.put(pair(Map.class, ZoneOffset.class), new Object[][] {
                { mapOf("_v", "-10"), ZoneOffset.of("-10:00") },
                { mapOf("hours", -10L), ZoneOffset.of("-10:00") },
                { mapOf("hours", -10L, "minutes", "0"), ZoneOffset.of("-10:00") },
                { mapOf("hrs", -10L, "mins", "0"), new IllegalArgumentException("Map to ZoneOffset the map must include one of the following: [hours, minutes, seconds], [_v], or [value]") },
                { mapOf("hours", -10L, "minutes", "0", "seconds", 0), ZoneOffset.of("-10:00") },
                { mapOf("hours", "-10", "minutes", (byte) -15, "seconds", "-1"), ZoneOffset.of("-10:15:01") },
                { mapOf("hours", "10", "minutes", (byte) 15, "seconds", true), ZoneOffset.of("+10:15:01") },
                { mapOf("hours", mapOf("_v", "10"), "minutes", mapOf("_v", (byte) 15), "seconds", mapOf("_v", true)), ZoneOffset.of("+10:15:01") }, // full recursion
        });

        // String
        TEST_FACTORY.put(pair(Void.class, String.class), new Object[][] {
                { null, null }
        });
        TEST_FACTORY.put(pair(Byte.class, String.class), new Object[][] {
                { (byte) 0, "0" },
                { Byte.MIN_VALUE, "-128" },
                { Byte.MAX_VALUE, "127" },
        });
        TEST_FACTORY.put(pair(Short.class, String.class), new Object[][] {
                { (short) 0, "0" },
                { Short.MIN_VALUE, "-32768" },
                { Short.MAX_VALUE, "32767" },
        });
        TEST_FACTORY.put(pair(Integer.class, String.class), new Object[][] {
                { 0, "0" },
                { Integer.MIN_VALUE, "-2147483648" },
                { Integer.MAX_VALUE, "2147483647" },
        });
        TEST_FACTORY.put(pair(Long.class, String.class), new Object[][] {
                { 0L, "0" },
                { Long.MIN_VALUE, "-9223372036854775808" },
                { Long.MAX_VALUE, "9223372036854775807" },
        });
        TEST_FACTORY.put(pair(Float.class, String.class), new Object[][] {
                { 0f, "0" },
                { 0.0f, "0" },
                { Float.MIN_VALUE, "1.4E-45" },
                { -Float.MAX_VALUE, "-3.4028235E38" },
                { Float.MAX_VALUE, "3.4028235E38" },
                { 123456789f, "1.23456792E8" },
                { 0.000000123456789f, "1.2345679E-7" },
                { 12345f, "12345.0" },
                { 0.00012345f, "1.2345E-4" },
        });
        TEST_FACTORY.put(pair(Double.class, String.class), new Object[][] {
                { 0d, "0" },
                { 0.0d, "0" },
                { Double.MIN_VALUE, "4.9E-324" },
                { -Double.MAX_VALUE, "-1.7976931348623157E308" },
                { Double.MAX_VALUE, "1.7976931348623157E308" },
                { 123456789d, "1.23456789E8" },
                { 0.000000123456789d, "1.23456789E-7" },
                { 12345d, "12345.0" },
                { 0.00012345d, "1.2345E-4" },
        });
        TEST_FACTORY.put(pair(Boolean.class, String.class), new Object[][] {
                { false, "false" },
                { true, "true" }
        });
        TEST_FACTORY.put(pair(Character.class, String.class), new Object[][] {
                { '1', "1" },
                { (char) 32, " " },
        });
        TEST_FACTORY.put(pair(BigInteger.class, String.class), new Object[][] {
                { new BigInteger("-1"), "-1" },
                { new BigInteger("0"), "0" },
                { new BigInteger("1"), "1" },
        });
        TEST_FACTORY.put(pair(BigDecimal.class, String.class), new Object[][] {
                { new BigDecimal("-1"), "-1" },
                { new BigDecimal("-1.0"), "-1" },
                { new BigDecimal("0"), "0" },
                { new BigDecimal("0.0"), "0" },
                { new BigDecimal("1.0"), "1" },
                { new BigDecimal("3.141519265358979323846264338"), "3.141519265358979323846264338" },
        });
        TEST_FACTORY.put(pair(AtomicBoolean.class, String.class), new Object[][] {
                { new AtomicBoolean(false), "false" },
                { new AtomicBoolean(true), "true" },
        });
        TEST_FACTORY.put(pair(AtomicInteger.class, String.class), new Object[][] {
                { new AtomicInteger(-1), "-1" },
                { new AtomicInteger(0), "0" },
                { new AtomicInteger(1), "1" },
                { new AtomicInteger(Integer.MIN_VALUE), "-2147483648" },
                { new AtomicInteger(Integer.MAX_VALUE), "2147483647" },
        });
        TEST_FACTORY.put(pair(AtomicLong.class, String.class), new Object[][] {
                { new AtomicLong(-1), "-1" },
                { new AtomicLong(0), "0" },
                { new AtomicLong(1), "1" },
                { new AtomicLong(Long.MIN_VALUE), "-9223372036854775808" },
                { new AtomicLong(Long.MAX_VALUE), "9223372036854775807" },
        });
        TEST_FACTORY.put(pair(byte[].class, String.class), new Object[][] {
                { new byte[] { (byte) 0xf0, (byte) 0x9f, (byte) 0x8d, (byte) 0xba }, "\uD83C\uDF7A" }, // beer mug, byte[] treated as UTF-8.
                { new byte[] { (byte) 65, (byte) 66, (byte) 67, (byte) 68 }, "ABCD" }
        });
        TEST_FACTORY.put(pair(char[].class, String.class), new Object[][] {
                { new char[] { 'A', 'B', 'C', 'D' }, "ABCD" }
        });
        TEST_FACTORY.put(pair(Character[].class, String.class), new Object[][] {
                { new Character[] { 'A', 'B', 'C', 'D' }, "ABCD" }
        });
        TEST_FACTORY.put(pair(ByteBuffer.class, String.class), new Object[][] {
                { ByteBuffer.wrap(new byte[] { (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33 }), "0123" }
        });
        TEST_FACTORY.put(pair(CharBuffer.class, String.class), new Object[][] {
                { CharBuffer.wrap(new char[] { 'A', 'B', 'C', 'D' }), "ABCD" },
        });
        TEST_FACTORY.put(pair(Class.class, String.class), new Object[][] {
                { Date.class, "java.util.Date" }
        });
        TEST_FACTORY.put(pair(Date.class, String.class), new Object[][] {
                { new Date(1), toGmtString(new Date(1)) },
                { new Date(Integer.MAX_VALUE), toGmtString(new Date(Integer.MAX_VALUE)) },
                { new Date(Long.MAX_VALUE), toGmtString(new Date(Long.MAX_VALUE)) }
        });
        TEST_FACTORY.put(pair(java.sql.Date.class, String.class), new Object[][] {
                { new java.sql.Date(1), toGmtString(new java.sql.Date(1)) },
                { new java.sql.Date(Integer.MAX_VALUE), toGmtString(new java.sql.Date(Integer.MAX_VALUE)) },
                { new java.sql.Date(Long.MAX_VALUE), toGmtString(new java.sql.Date(Long.MAX_VALUE)) }
        });
        TEST_FACTORY.put(pair(Timestamp.class, String.class), new Object[][] {
                { new Timestamp(1), toGmtString(new Timestamp(1)) },
                { new Timestamp(Integer.MAX_VALUE), toGmtString(new Timestamp(Integer.MAX_VALUE)) },
                { new Timestamp(Long.MAX_VALUE), toGmtString(new Timestamp(Long.MAX_VALUE)) },
        });
        TEST_FACTORY.put(pair(LocalDate.class, String.class), new Object[][] {
                { LocalDate.parse("1965-12-31"), "1965-12-31" },
        });
        TEST_FACTORY.put(pair(LocalTime.class, String.class), new Object[][] {
                { LocalTime.parse("16:20:00"), "16:20:00" },
        });
        TEST_FACTORY.put(pair(LocalDateTime.class, String.class), new Object[][] {
                { LocalDateTime.parse("1965-12-31T16:20:00"), "1965-12-31T16:20:00" },
        });
        TEST_FACTORY.put(pair(ZonedDateTime.class, String.class), new Object[][] {
                { ZonedDateTime.parse("1965-12-31T16:20:00+00:00"), "1965-12-31T16:20:00Z" },
                { ZonedDateTime.parse("2024-02-14T19:20:00-05:00"), "2024-02-14T19:20:00-05:00" },
                { ZonedDateTime.parse("2024-02-14T19:20:00+05:00"), "2024-02-14T19:20:00+05:00" }
        });
        TEST_FACTORY.put(pair(UUID.class, String.class), new Object[][] {
                { new UUID(0L, 0L), "00000000-0000-0000-0000-000000000000" },
                { new UUID(1L, 1L), "00000000-0000-0001-0000-000000000001" },
                { new UUID(Long.MAX_VALUE, Long.MAX_VALUE), "7fffffff-ffff-ffff-7fff-ffffffffffff" },
                { new UUID(Long.MIN_VALUE, Long.MIN_VALUE), "80000000-0000-0000-8000-000000000000" },
        });
        TEST_FACTORY.put(pair(Calendar.class, String.class), new Object[][] {
                { (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TZ_TOKYO);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 0);
                    return cal;
                }, "2024-02-05T22:31:00" }
        });
        TEST_FACTORY.put(pair(Number.class, String.class), new Object[][] {
                { (byte) 1, "1" },
                { (short) 2, "2" },
                { 3, "3" },
                { 4L, "4" },
                { 5f, "5.0" },
                { 6d, "6.0" },
        });
        TEST_FACTORY.put(pair(Map.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(Enum.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(String.class, String.class), new Object[][] {
                { "same", "same" },
        });
        TEST_FACTORY.put(pair(Duration.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(Instant.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(LocalTime.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(MonthDay.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(YearMonth.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(Period.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(ZoneId.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(ZoneOffset.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(OffsetTime.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(OffsetDateTime.class, String.class), new Object[][] {

        });
        TEST_FACTORY.put(pair(Year.class, String.class), new Object[][] {

        });
    }

    public static Map<String, Class<?>> shortNamesToClass = new ConcurrentHashMap<>();

    private static String toGmtString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(TZ_TOKYO);
        return simpleDateFormat.format(date);
    }

    @BeforeEach
    public void before() {
        // create converter with default options
        converter = new Converter(options);
    }


    @Test
    void testForMissingTests() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        int neededTests = 0;

        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Set<Class<?>> targetClasses = entry.getValue();



            for (Class<?> targetClass : targetClasses) {

                Object[][] testData = TEST_FACTORY.get(pair(sourceClass, targetClass));

                if (testData == null) { // data set needs added
                    // Change to throw exception, so that when new conversions are added, the tests will fail until
                    // an "everything" test entry is added.
                    System.err.println("No test data for: " + getShortName(sourceClass) + " ==> " + getShortName(targetClass));
                    neededTests++;
                }
            }
        }

        if (neededTests > 0) {
            System.err.println(neededTests + " tests need to be added.");
            System.err.flush();
            // fail(neededTests + " tests need to be added.");
        }
    }

    private static Object possiblyConvertSupplier(Object possibleSupplier) {
        if (possibleSupplier instanceof Supplier) {
            return ((Supplier<?>) possibleSupplier).get();
        }

        return possibleSupplier;
    }

    private static Stream<Arguments> generateTestEverythingParams() {

        ArrayList<Arguments> list = new ArrayList<>(400);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Object[][]> entry : TEST_FACTORY.entrySet()) {
            Class<?> sourceClass = entry.getKey().getKey();
            Class<?> targetClass = entry.getKey().getValue();
            Object[][] testData = entry.getValue();

            for (int i = 0; i < testData.length; i++) {
                Object[] testPair = testData[i];

                // don't worry about putting back into the pair for tests when suppliers.  We can get each time for now.
                // protecting Test integrity.
                Object source = possiblyConvertSupplier(testPair[0]);
                Object target = possiblyConvertSupplier(testPair[1]);

                String shortNameSource = addShortName(sourceClass);
                String shortNameTarget = addShortName(targetClass);

                list.add(Arguments.of(shortNameSource, shortNameTarget, source, target));
            }

        }

        return Stream.of(list.toArray(new Arguments[] {}));
    }

    @ParameterizedTest(name = "<{0}, {1}> ==> {2}")
    @MethodSource("generateTestEverythingParams")
    void testSourceMatchesExpectedType(String shortNameSource, String shortNameTarget, Object source, Object actual) {
        Class<?> sourceClass = getFromShortName(shortNameSource);
        assertTrue(source == null || sourceClass.isAssignableFrom(source.getClass()));
    }

    @ParameterizedTest(name = "<{0}, {1}> ==> {2}")
    @MethodSource("generateTestEverythingParams")
    void testConvert(String shortNameSource, String shortNameTarget, Object source, Object target) {
        Class<?> targetClass = getFromShortName(shortNameTarget);
        //TODO:  does the exception actually get thrown on the convert or should we just check if they are equal?
        if (target instanceof Throwable) {
            Throwable t = (Throwable) target;
            assertThatExceptionOfType(t.getClass())
                    .isThrownBy(() -> converter.convert(source, targetClass, options))
                    .withMessageContaining(((Throwable) target).getMessage());
        } else {
            // Assert values are equals
            Object actual = converter.convert(source, targetClass, options);
            assertEquals(target, actual);
        }
    }

    @ParameterizedTest(name = "<{0}, {1}> ==> {2}")
    @MethodSource("generateTestEverythingParams")
    void testIdentity(String shortNameSource, String shortNameTarget, Object source, Object actual) {
        Class<?> sourceClass = getFromShortName(shortNameSource);
        Class<?> targetClass = getFromShortName(shortNameTarget);
        // do not test identity on Throwables.
        // if source and target classes match then we expect the objects to be the same object.
        assertTrue(actual instanceof Throwable ||
                !sourceClass.equals(targetClass) ||
                (source == converter.convert(source, targetClass, options)));
    }

    public static String addShortName(Class<?> c) {
        String name = c.getSimpleName();
        if (java.sql.Date.class.isAssignableFrom(c)) {
            name = "java.sql.Date";
        }
        shortNamesToClass.put(name, c);
        return name;
    }

    public static Class<?> getFromShortName(String name) {
        return shortNamesToClass.get(name);
    }

}
