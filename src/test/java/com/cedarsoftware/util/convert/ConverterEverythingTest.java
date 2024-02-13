package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
import java.util.List;
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

import com.cedarsoftware.util.ClassUtilities;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static com.cedarsoftware.util.convert.Converter.getShortName;
import static com.cedarsoftware.util.convert.Converter.pair;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private static final TimeZone TOKYO_TZ = TimeZone.getTimeZone(TOKYO_Z);
    private Converter converter;
    private final ConverterOptions options = new ConverterOptions() {
        public TimeZone getTimeZone() {
            return TOKYO_TZ;
        }
    };
    private static final Map<Map.Entry<Class<?>, Class<?>>, Object[][]> TEST_DB = new ConcurrentHashMap<>(500, .8f);

    static {
        //   {source1, answer1},
        //   ...
        //   {source-n, answer-n}

        /////////////////////////////////////////////////////////////
        // Byte/byte
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, byte.class), new Object[][] {
                { null, (byte) 0 },
        });
        TEST_DB.put(pair(Void.class, Byte.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Byte.class, Byte.class), new Object[][] {
                { (byte) -1, (byte) -1 },
                { (byte) 0, (byte) 0 },
                { (byte) 1, (byte) 1 },
                { Byte.MIN_VALUE, Byte.MIN_VALUE },
                { Byte.MAX_VALUE, Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Byte.class), new Object[][] {
                { (short) -1, (byte) -1 },
                { (short) 0, (byte) 0 },
                { (short) 1, (byte) 1 },
                { (short) -128, Byte.MIN_VALUE },
                { (short) 127, Byte.MAX_VALUE },
                { (short) -129, Byte.MAX_VALUE },    // verify wrap around
                { (short) 128, Byte.MIN_VALUE },    // verify wrap around
        });
        TEST_DB.put(pair(Integer.class, Byte.class), new Object[][] {
                { -1, (byte) -1 },
                { 0, (byte) 0 },
                { 1, (byte) 1 },
                { -128, Byte.MIN_VALUE },
                { 127, Byte.MAX_VALUE },
                { -129, Byte.MAX_VALUE },   // verify wrap around
                { 128, Byte.MIN_VALUE },   // verify wrap around
        });
        TEST_DB.put(pair(Long.class, Byte.class), new Object[][] {
                { -1L, (byte) -1 },
                { 0L, (byte) 0 },
                { 1L, (byte) 1 },
                { -128L, Byte.MIN_VALUE },
                { 127L, Byte.MAX_VALUE },
                { -129L, Byte.MAX_VALUE }, // verify wrap around
                { 128L, Byte.MIN_VALUE }   // verify wrap around
        });
        TEST_DB.put(pair(Float.class, Byte.class), new Object[][] {
                { -1f, (byte) -1 },
                { -1.99f, (byte) -1 },
                { -1.1f, (byte) -1 },
                { 0f, (byte) 0 },
                { 1f, (byte) 1 },
                { 1.1f, (byte) 1 },
                { 1.999f, (byte) 1 },
                { -128f, Byte.MIN_VALUE },
                { 127f, Byte.MAX_VALUE },
                { -129f, Byte.MAX_VALUE }, // verify wrap around
                { 128f, Byte.MIN_VALUE }   // verify wrap around
        });
        TEST_DB.put(pair(Double.class, Byte.class), new Object[][] {
                { -1d, (byte) -1 },
                { -1.99d, (byte) -1 },
                { -1.1d, (byte) -1 },
                { 0d, (byte) 0 },
                { 1d, (byte) 1 },
                { 1.1d, (byte) 1 },
                { 1.999d, (byte) 1 },
                { -128d, Byte.MIN_VALUE },
                { 127d, Byte.MAX_VALUE },
                { -129d, Byte.MAX_VALUE }, // verify wrap around
                { 128d, Byte.MIN_VALUE }   // verify wrap around
        });
        TEST_DB.put(pair(Boolean.class, Byte.class), new Object[][] {
                { true, (byte) 1 },
                { false, (byte) 0 },
        });
        TEST_DB.put(pair(Character.class, Byte.class), new Object[][] {
                { '1', (byte) 49 },
                { '0', (byte) 48 },
                { (char) 1, (byte) 1 },
                { (char) 0, (byte) 0 },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Byte.class), new Object[][] {
                { new AtomicBoolean(true), (byte) 1 },
                { new AtomicBoolean(false), (byte) 0 },
        });
        TEST_DB.put(pair(AtomicInteger.class, Byte.class), new Object[][] {
                { new AtomicInteger(-1), (byte) -1 },
                { new AtomicInteger(0), (byte) 0 },
                { new AtomicInteger(1), (byte) 1 },
                { new AtomicInteger(-128), Byte.MIN_VALUE },
                { new AtomicInteger(127), Byte.MAX_VALUE },
                { new AtomicInteger(-129), Byte.MAX_VALUE },
                { new AtomicInteger(128), Byte.MIN_VALUE },
        });
        TEST_DB.put(pair(AtomicLong.class, Byte.class), new Object[][] {
                { new AtomicLong(-1), (byte) -1 },
                { new AtomicLong(0), (byte) 0 },
                { new AtomicLong(1), (byte) 1 },
                { new AtomicLong(-128), Byte.MIN_VALUE },
                { new AtomicLong(127), Byte.MAX_VALUE },
                { new AtomicLong(-129), Byte.MAX_VALUE },
                { new AtomicLong(128), Byte.MIN_VALUE },
        });
        TEST_DB.put(pair(BigInteger.class, Byte.class), new Object[][] {
                { new BigInteger("-1"), (byte) -1 },
                { new BigInteger("0"), (byte) 0 },
                { new BigInteger("1"), (byte) 1 },
                { new BigInteger("-128"), Byte.MIN_VALUE },
                { new BigInteger("127"), Byte.MAX_VALUE },
                { new BigInteger("-129"), Byte.MAX_VALUE },
                { new BigInteger("128"), Byte.MIN_VALUE },
        });
        TEST_DB.put(pair(BigDecimal.class, Byte.class), new Object[][] {
                { new BigDecimal("-1"), (byte) -1 },
                { new BigDecimal("-1.1"), (byte) -1 },
                { new BigDecimal("-1.9"), (byte) -1 },
                { new BigDecimal("0"), (byte) 0 },
                { new BigDecimal("1"), (byte) 1 },
                { new BigDecimal("1.1"), (byte) 1 },
                { new BigDecimal("1.9"), (byte) 1 },
                { new BigDecimal("-128"), Byte.MIN_VALUE },
                { new BigDecimal("127"), Byte.MAX_VALUE },
                { new BigDecimal("-129"), Byte.MAX_VALUE },
                { new BigDecimal("128"), Byte.MIN_VALUE },
        });
        TEST_DB.put(pair(Number.class, Byte.class), new Object[][] {
                { -2L, (byte) -2 },
        });
        TEST_DB.put(pair(Map.class, Byte.class), new Object[][] {
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
                { mapOf("_v", -129), Byte.MAX_VALUE },

                { mapOf("_v", "128"), new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", 128), Byte.MIN_VALUE },
                { mapOf("_v", mapOf("_v", 128L)), Byte.MIN_VALUE },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(Year.class, Byte.class), new Object[][] {
                {Year.of(2024), new IllegalArgumentException("Unsupported conversion, source type [Year (2024)] target type 'Byte'")},
        });
        TEST_DB.put(pair(String.class, Byte.class), new Object[][] {
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
                { " ", (byte) 0 },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a byte value or outside -128 to 127") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a byte value or outside -128 to 127") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a byte value or outside -128 to 127") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a byte value or outside -128 to 127") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a byte value or outside -128 to 127") },
                { "-129", new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127") },
                { "128", new IllegalArgumentException("'128' not parseable as a byte value or outside -128 to 127") },
        });

        /////////////////////////////////////////////////////////////
        // Short/short
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, short.class), new Object[][] {
                { null, (short) 0 },
        });
        TEST_DB.put(pair(Void.class, Short.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Byte.class, Short.class), new Object[][] {
                { (byte) -1, (short) -1 },
                { (byte) 0, (short) 0 },
                { (byte) 1, (short) 1 },
                { Byte.MIN_VALUE, (short)Byte.MIN_VALUE },
                { Byte.MAX_VALUE, (short)Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Short.class), new Object[][] {
                { (short) -1, (short) -1 },
                { (short) 0, (short) 0 },
                { (short) 1, (short) 1 },
                { Short.MIN_VALUE, Short.MIN_VALUE },
                { Short.MAX_VALUE, Short.MAX_VALUE },
        });
        TEST_DB.put(pair(Integer.class, Short.class), new Object[][] {
                { -1, (short) -1 },
                { 0, (short) 0 },
                { 1, (short) 1 },
                { -32769, Short.MAX_VALUE },   // wrap around check
                { 32768, Short.MIN_VALUE },   // wrap around check
        });
        TEST_DB.put(pair(Long.class, Short.class), new Object[][] {
                { -1L, (short) -1 },
                { 0L, (short) 0 },
                { 1L, (short) 1 },
                { -32769L, Short.MAX_VALUE },   // wrap around check
                { 32768L, Short.MIN_VALUE },   // wrap around check
        });
        TEST_DB.put(pair(Float.class, Short.class), new Object[][] {
                { -1f, (short) -1 },
                { -1.99f, (short) -1 },
                { -1.1f, (short) -1 },
                { 0f, (short) 0 },
                { 1f, (short) 1 },
                { 1.1f, (short) 1 },
                { 1.999f, (short) 1 },
                { -32768f, Short.MIN_VALUE },
                { 32767f, Short.MAX_VALUE },
                { -32769f, Short.MAX_VALUE  }, // verify wrap around
                { 32768f, Short.MIN_VALUE }   // verify wrap around
        });
        TEST_DB.put(pair(Double.class, Short.class), new Object[][] {
                { -1d, (short) -1 },
                { -1.99d, (short) -1 },
                { -1.1d, (short) -1 },
                { 0d, (short) 0 },
                { 1d, (short) 1 },
                { 1.1d, (short) 1 },
                { 1.999d, (short) 1 },
                { -32768d, Short.MIN_VALUE },
                { 32767d, Short.MAX_VALUE },
                { -32769d, Short.MAX_VALUE }, // verify wrap around
                { 32768d, Short.MIN_VALUE }   // verify wrap around
        });
        TEST_DB.put(pair(Boolean.class, Short.class), new Object[][] {
                { true, (short) 1 },
                { false, (short) 0 },
        });
        TEST_DB.put(pair(Character.class, Short.class), new Object[][] {
                { '1', (short) 49 },
                { '0', (short) 48 },
                { (char) 1, (short) 1 },
                { (char) 0, (short) 0 },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Short.class), new Object[][] {
                { new AtomicBoolean(true), (short) 1 },
                { new AtomicBoolean(false), (short) 0 },
        });
        TEST_DB.put(pair(AtomicInteger.class, Short.class), new Object[][] {
                { new AtomicInteger(-1), (short) -1 },
                { new AtomicInteger(0), (short) 0 },
                { new AtomicInteger(1), (short) 1 },
                { new AtomicInteger(-32768), Short.MIN_VALUE },
                { new AtomicInteger(32767), Short.MAX_VALUE },
                { new AtomicInteger(-32769), Short.MAX_VALUE },
                { new AtomicInteger(32768), Short.MIN_VALUE },
        });
        TEST_DB.put(pair(AtomicLong.class, Short.class), new Object[][] {
                { new AtomicLong(-1), (short) -1 },
                { new AtomicLong(0), (short) 0 },
                { new AtomicLong(1), (short) 1 },
                { new AtomicLong(-32768), Short.MIN_VALUE },
                { new AtomicLong(32767), Short.MAX_VALUE },
                { new AtomicLong(-32769), Short.MAX_VALUE },
                { new AtomicLong(32768), Short.MIN_VALUE },
        });
        TEST_DB.put(pair(BigInteger.class, Short.class), new Object[][] {
                { new BigInteger("-1"), (short) -1 },
                { new BigInteger("0"), (short) 0 },
                { new BigInteger("1"), (short) 1 },
                { new BigInteger("-32768"), Short.MIN_VALUE },
                { new BigInteger("32767"), Short.MAX_VALUE },
                { new BigInteger("-32769"), Short.MAX_VALUE },
                { new BigInteger("32768"), Short.MIN_VALUE },
        });
        TEST_DB.put(pair(BigDecimal.class, Short.class), new Object[][] {
                { new BigDecimal("-1"), (short) -1 },
                { new BigDecimal("-1.1"), (short) -1 },
                { new BigDecimal("-1.9"), (short) -1 },
                { new BigDecimal("0"), (short) 0 },
                { new BigDecimal("1"), (short) 1 },
                { new BigDecimal("1.1"), (short) 1 },
                { new BigDecimal("1.9"), (short) 1 },
                { new BigDecimal("-32768"), Short.MIN_VALUE },
                { new BigDecimal("32767"), Short.MAX_VALUE },
                { new BigDecimal("-32769"), Short.MAX_VALUE },
                { new BigDecimal("32768"), Short.MIN_VALUE },
        });
        TEST_DB.put(pair(Number.class, Short.class), new Object[][] {
                { -2L, (short) -2 },
        });
        TEST_DB.put(pair(Map.class, Short.class), new Object[][] {
                { mapOf("_v", "-1"), (short) -1 },
                { mapOf("_v", -1), (short) -1 },
                { mapOf("value", "-1"), (short) -1 },
                { mapOf("value", -1L), (short) -1 },

                { mapOf("_v", "0"), (short) 0 },
                { mapOf("_v", 0), (short) 0 },

                { mapOf("_v", "1"), (short) 1 },
                { mapOf("_v", 1), (short) 1 },

                { mapOf("_v", "-32768"), Short.MIN_VALUE },
                { mapOf("_v", -32768), Short.MIN_VALUE },

                { mapOf("_v", "32767"), Short.MAX_VALUE },
                { mapOf("_v", 32767), Short.MAX_VALUE },

                { mapOf("_v", "-32769"), new IllegalArgumentException("'-32769' not parseable as a short value or outside -32768 to 32767") },
                { mapOf("_v", -32769), Short.MAX_VALUE },

                { mapOf("_v", "32768"), new IllegalArgumentException("'32768' not parseable as a short value or outside -32768 to 32767") },
                { mapOf("_v", 32768), Short.MIN_VALUE },
                { mapOf("_v", mapOf("_v", 32768L)), Short.MIN_VALUE },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Short.class), new Object[][] {
                { "-1", (short) -1 },
                { "-1.1", (short) -1 },
                { "-1.9", (short) -1 },
                { "0", (short) 0 },
                { "1", (short) 1 },
                { "1.1", (short) 1 },
                { "1.9", (short) 1 },
                { "-32768", (short) -32768 },
                { "32767", (short) 32767 },
                { "", (short) 0 },
                { " ", (short) 0 },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a short value or outside -32768 to 32767") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a short value or outside -32768 to 32767") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a short value or outside -32768 to 32767") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a short value or outside -32768 to 32767") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a short value or outside -32768 to 32767") },
                { "-32769", new IllegalArgumentException("'-32769' not parseable as a short value or outside -32768 to 32767") },
                { "32768", new IllegalArgumentException("'32768' not parseable as a short value or outside -32768 to 32767") },
        });
        TEST_DB.put(pair(Year.class, Short.class), new Object[][] {
                { Year.of(-1), (short)-1 },
                { Year.of(0), (short) 0 },
                { Year.of(1), (short) 1 },
                { Year.of(1582), (short) 1582 },
                { Year.of(1970), (short) 1970 },
                { Year.of(2000), (short) 2000 },
                { Year.of(2024), (short) 2024 },
                { Year.of(9999), (short) 9999 },
        });

        /////////////////////////////////////////////////////////////
        // Integer/int
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, int.class), new Object[][] {
                { null, 0 },
        });
        TEST_DB.put(pair(Void.class, Integer.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Byte.class, Integer.class), new Object[][] {
                { (byte) -1, -1 },
                { (byte) 0, 0 },
                { (byte) 1, 1 },
                { Byte.MIN_VALUE, (int)Byte.MIN_VALUE },
                { Byte.MAX_VALUE, (int)Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Integer.class), new Object[][] {
                { (short)-1, -1 },
                { (short)0, 0 },
                { (short)1, 1 },
                { Short.MIN_VALUE, (int)Short.MIN_VALUE },
                { Short.MAX_VALUE, (int)Short.MAX_VALUE },
        });
        TEST_DB.put(pair(Integer.class, Integer.class), new Object[][] {
                { -1, -1 },
                { 0, 0 },
                { 1, 1 },
                { Integer.MAX_VALUE, Integer.MAX_VALUE },
                { Integer.MIN_VALUE, Integer.MIN_VALUE },
        });
        TEST_DB.put(pair(Long.class, Integer.class), new Object[][] {
                { -1L, -1 },
                { 0L, 0 },
                { 1L, 1 },
                { -2147483649L, Integer.MAX_VALUE },   // wrap around check
                { 2147483648L, Integer.MIN_VALUE },   // wrap around check
        });
        TEST_DB.put(pair(Float.class, Integer.class), new Object[][] {
                { -1f, -1 },
                { -1.99f, -1 },
                { -1.1f, -1 },
                { 0f, 0 },
                { 1f, 1 },
                { 1.1f, 1 },
                { 1.999f, 1 },
                { -214748368f, -214748368 },    // large representable -float
                { 214748368f, 214748368 },      // large representable +float
        });
        TEST_DB.put(pair(Double.class, Integer.class), new Object[][] {
                { -1d, -1 },
                { -1.99d, -1 },
                { -1.1d, -1 },
                { 0d, 0 },
                { 1d, 1 },
                { 1.1d, 1 },
                { 1.999d, 1 },
                { -2147483648d, Integer.MIN_VALUE },
                { 2147483647d, Integer.MAX_VALUE },
        });
        TEST_DB.put(pair(Boolean.class, Integer.class), new Object[][] {
                { true, 1 },
                { false, 0 },
        });
        TEST_DB.put(pair(Character.class, Integer.class), new Object[][] {
                { '1', 49 },
                { '0', 48 },
                { (char) 1, 1 },
                { (char) 0, 0 },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Integer.class), new Object[][] {
                { new AtomicBoolean(true), 1 },
                { new AtomicBoolean(false), 0 },
        });
        TEST_DB.put(pair(AtomicInteger.class, Integer.class), new Object[][] {
                { new AtomicInteger(-1), -1 },
                { new AtomicInteger(0), 0 },
                { new AtomicInteger(1), 1 },
                { new AtomicInteger(-2147483648), Integer.MIN_VALUE },
                { new AtomicInteger(2147483647), Integer.MAX_VALUE },
        });
        TEST_DB.put(pair(AtomicLong.class, Integer.class), new Object[][] {
                { new AtomicLong(-1), -1 },
                { new AtomicLong(0), 0 },
                { new AtomicLong(1), 1 },
                { new AtomicLong(-2147483648), Integer.MIN_VALUE },
                { new AtomicLong(2147483647), Integer.MAX_VALUE },
                { new AtomicLong(-2147483649L), Integer.MAX_VALUE },
                { new AtomicLong(2147483648L), Integer.MIN_VALUE },
        });
        TEST_DB.put(pair(BigInteger.class, Integer.class), new Object[][] {
                { new BigInteger("-1"), -1 },
                { new BigInteger("0"), 0 },
                { new BigInteger("1"), 1 },
                { new BigInteger("-2147483648"), Integer.MIN_VALUE },
                { new BigInteger("2147483647"), Integer.MAX_VALUE },
                { new BigInteger("-2147483649"), Integer.MAX_VALUE },
                { new BigInteger("2147483648"), Integer.MIN_VALUE },
        });
        TEST_DB.put(pair(BigDecimal.class, Integer.class), new Object[][] {
                { new BigDecimal("-1"), -1 },
                { new BigDecimal("-1.1"), -1 },
                { new BigDecimal("-1.9"), -1 },
                { new BigDecimal("0"), 0 },
                { new BigDecimal("1"), 1 },
                { new BigDecimal("1.1"), 1 },
                { new BigDecimal("1.9"), 1 },
                { new BigDecimal("-2147483648"), Integer.MIN_VALUE },
                { new BigDecimal("2147483647"), Integer.MAX_VALUE },
                { new BigDecimal("-2147483649"), Integer.MAX_VALUE },
                { new BigDecimal("2147483648"), Integer.MIN_VALUE },
        });
        TEST_DB.put(pair(Number.class, Integer.class), new Object[][] {
                { -2L, -2 },
        });
        TEST_DB.put(pair(Map.class, Integer.class), new Object[][] {
                { mapOf("_v", "-1"), -1 },
                { mapOf("_v", -1), -1 },
                { mapOf("value", "-1"), -1 },
                { mapOf("value", -1L), -1 },

                { mapOf("_v", "0"), 0 },
                { mapOf("_v", 0), 0 },

                { mapOf("_v", "1"), 1 },
                { mapOf("_v", 1), 1 },

                { mapOf("_v", "-2147483648"), Integer.MIN_VALUE },
                { mapOf("_v", -2147483648), Integer.MIN_VALUE },

                { mapOf("_v", "2147483647"), Integer.MAX_VALUE },
                { mapOf("_v", 2147483647), Integer.MAX_VALUE },

                { mapOf("_v", "-2147483649"), new IllegalArgumentException("'-2147483649' not parseable as an int value or outside -2147483648 to 2147483647") },
                { mapOf("_v", -2147483649L), Integer.MAX_VALUE },

                { mapOf("_v", "2147483648"), new IllegalArgumentException("'2147483648' not parseable as an int value or outside -2147483648 to 2147483647") },
                { mapOf("_v", 2147483648L), Integer.MIN_VALUE },
                { mapOf("_v", mapOf("_v", 2147483648L)), Integer.MIN_VALUE },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Integer.class), new Object[][] {
                { "-1", -1 },
                { "-1.1", -1 },
                { "-1.9", -1 },
                { "0", 0 },
                { "1", 1 },
                { "1.1", 1 },
                { "1.9", 1 },
                { "-2147483648", -2147483648 },
                { "2147483647", 2147483647 },
                { "", 0 },
                { " ", 0 },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "-2147483649", new IllegalArgumentException("'-2147483649' not parseable as an int value or outside -2147483648 to 2147483647") },
                { "2147483648", new IllegalArgumentException("'2147483648' not parseable as an int value or outside -2147483648 to 2147483647") },
        });
        TEST_DB.put(pair(Year.class, Integer.class), new Object[][] {
                { Year.of(-1), -1 },
                { Year.of(0), 0 },
                { Year.of(1), 1 },
                { Year.of(1582), 1582 },
                { Year.of(1970), 1970 },
                { Year.of(2000), 2000 },
                { Year.of(2024), 2024 },
                { Year.of(9999), 9999 },
        });

        /////////////////////////////////////////////////////////////
        // Long/long
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, long.class), new Object[][] {
                { null, 0L },
        });
        TEST_DB.put(pair(Void.class, Long.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Byte.class, Long.class), new Object[][] {
                { (byte) -1, -1L },
                { (byte) 0, 0L },
                { (byte) 1, 1L },
                { Byte.MIN_VALUE, (long)Byte.MIN_VALUE },
                { Byte.MAX_VALUE, (long)Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Long.class), new Object[][] {
                { (short)-1, -1L },
                { (short)0, 0L },
                { (short)1, 1L },
                { Short.MIN_VALUE, (long)Short.MIN_VALUE },
                { Short.MAX_VALUE, (long)Short.MAX_VALUE },
        });
        TEST_DB.put(pair(Integer.class, Long.class), new Object[][] {
                { -1, -1L },
                { 0, 0L },
                { 1, 1L },
                { Integer.MAX_VALUE, (long)Integer.MAX_VALUE },
                { Integer.MIN_VALUE, (long)Integer.MIN_VALUE },
        });
        TEST_DB.put(pair(Long.class, Long.class), new Object[][] {
                { -1L, -1L },
                { 0L, 0L },
                { 1L, 1L },
                { 9223372036854775807L, Long.MAX_VALUE },
                { -9223372036854775808L, Long.MIN_VALUE },
        });
        TEST_DB.put(pair(Float.class, Long.class), new Object[][] {
                { -1f, -1L },
                { -1.99f, -1L },
                { -1.1f, -1L },
                { 0f, 0L },
                { 1f, 1L },
                { 1.1f, 1L },
                { 1.999f, 1L },
                { -214748368f, -214748368L },    // large representable -float
                { 214748368f, 214748368L },      // large representable +float
        });
        TEST_DB.put(pair(Double.class, Long.class), new Object[][] {
                { -1d, -1L },
                { -1.99d, -1L },
                { -1.1d, -1L },
                { 0d, 0L },
                { 1d, 1L },
                { 1.1d, 1L },
                { 1.999d, 1L },
                { -9223372036854775808d, Long.MIN_VALUE },
                { 9223372036854775807d, Long.MAX_VALUE },
        });
        TEST_DB.put(pair(Boolean.class, Long.class), new Object[][] {
                { true, 1L },
                { false, 0L },
        });
        TEST_DB.put(pair(Character.class, Long.class), new Object[][] {
                { '1', 49L },
                { '0', 48L },
                { (char) 1, 1L },
                { (char) 0, 0L },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Long.class), new Object[][] {
                { new AtomicBoolean(true), 1L },
                { new AtomicBoolean(false), 0L },
        });
        TEST_DB.put(pair(AtomicInteger.class, Long.class), new Object[][] {
                { new AtomicInteger(-1), -1L },
                { new AtomicInteger(0), 0L },
                { new AtomicInteger(1), 1L },
                { new AtomicInteger(-2147483648), (long)Integer.MIN_VALUE },
                { new AtomicInteger(2147483647), (long)Integer.MAX_VALUE },
        });
        TEST_DB.put(pair(AtomicLong.class, Long.class), new Object[][] {
                { new AtomicLong(-1), -1L },
                { new AtomicLong(0), 0L },
                { new AtomicLong(1), 1L },
                { new AtomicLong(-9223372036854775808L), Long.MIN_VALUE },
                { new AtomicLong(9223372036854775807L), Long.MAX_VALUE },
        });
        TEST_DB.put(pair(BigInteger.class, Long.class), new Object[][] {
                { new BigInteger("-1"), -1L },
                { new BigInteger("0"), 0L },
                { new BigInteger("1"), 1L },
                { new BigInteger("-9223372036854775808"), Long.MIN_VALUE },
                { new BigInteger("9223372036854775807"), Long.MAX_VALUE },
                { new BigInteger("-9223372036854775809"), Long.MAX_VALUE },
                { new BigInteger("9223372036854775808"), Long.MIN_VALUE },
        });
        TEST_DB.put(pair(BigDecimal.class, Long.class), new Object[][] {
                { new BigDecimal("-1"), -1L },
                { new BigDecimal("-1.1"), -1L },
                { new BigDecimal("-1.9"), -1L },
                { new BigDecimal("0"), 0L },
                { new BigDecimal("1"), 1L },
                { new BigDecimal("1.1"), 1L },
                { new BigDecimal("1.9"), 1L },
                { new BigDecimal("-9223372036854775808"), Long.MIN_VALUE },
                { new BigDecimal("9223372036854775807"), Long.MAX_VALUE },
                { new BigDecimal("-9223372036854775809"), Long.MAX_VALUE },       // wrap around
                { new BigDecimal("9223372036854775808"), Long.MIN_VALUE },        // wrap around
        });
        TEST_DB.put(pair(Number.class, Long.class), new Object[][] {
                { -2, -2L },
        });
        TEST_DB.put(pair(Map.class, Long.class), new Object[][] {
                { mapOf("_v", "-1"), -1L },
                { mapOf("_v", -1), -1L },
                { mapOf("value", "-1"), -1L },
                { mapOf("value", -1L), -1L },

                { mapOf("_v", "0"), 0L },
                { mapOf("_v", 0), 0L },

                { mapOf("_v", "1"), 1L },
                { mapOf("_v", 1), 1L },

                { mapOf("_v", "-9223372036854775808"), Long.MIN_VALUE },
                { mapOf("_v", -9223372036854775808L), Long.MIN_VALUE },

                { mapOf("_v", "9223372036854775807"), Long.MAX_VALUE },
                { mapOf("_v", 9223372036854775807L), Long.MAX_VALUE },

                { mapOf("_v", "-9223372036854775809"), new IllegalArgumentException("'-9223372036854775809' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },

                { mapOf("_v", "9223372036854775808"), new IllegalArgumentException("'9223372036854775808' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { mapOf("_v", mapOf("_v", -9223372036854775808L)), Long.MIN_VALUE },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Long.class), new Object[][] {
                { "-1", -1L },
                { "-1.1", -1L },
                { "-1.9", -1L },
                { "0", 0L },
                { "1", 1L },
                { "1.1", 1L },
                { "1.9", 1L },
                { "-2147483648", -2147483648L },
                { "2147483647", 2147483647L },
                { "", 0L },
                { " ", 0L },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "-9223372036854775809", new IllegalArgumentException("'-9223372036854775809' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
                { "9223372036854775808", new IllegalArgumentException("'9223372036854775808' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807") },
        });
        TEST_DB.put(pair(Year.class, Long.class), new Object[][] {
                { Year.of(-1), -1L },
                { Year.of(0), 0L },
                { Year.of(1), 1L },
                { Year.of(1582), 1582L },
                { Year.of(1970), 1970L },
                { Year.of(2000), 2000L },
                { Year.of(2024), 2024L },
                { Year.of(9999), 9999L },
        });
        TEST_DB.put(pair(Date.class, Long.class), new Object[][] {
                { new Date(Long.MIN_VALUE), Long.MIN_VALUE },
                { new Date(Integer.MIN_VALUE), (long)Integer.MIN_VALUE },
                { new Date(0), 0L },
                { new Date(Integer.MAX_VALUE), (long)Integer.MAX_VALUE },
                { new Date(Long.MAX_VALUE), Long.MAX_VALUE },
        });
        TEST_DB.put(pair(java.sql.Date.class, Long.class), new Object[][] {
                { new java.sql.Date(Long.MIN_VALUE), Long.MIN_VALUE },
                { new java.sql.Date(Integer.MIN_VALUE), (long)Integer.MIN_VALUE },
                { new java.sql.Date(0), 0L },
                { new java.sql.Date(Integer.MAX_VALUE), (long)Integer.MAX_VALUE },
                { new java.sql.Date(Long.MAX_VALUE), Long.MAX_VALUE },
        });
        TEST_DB.put(pair(Timestamp.class, Long.class), new Object[][] {
                { new Timestamp(Long.MIN_VALUE), Long.MIN_VALUE },
                { new Timestamp(Integer.MIN_VALUE), (long)Integer.MIN_VALUE },
                { new Timestamp(0), 0L },
                { new Timestamp(Integer.MAX_VALUE), (long)Integer.MAX_VALUE },
                { new Timestamp(Long.MAX_VALUE), Long.MAX_VALUE },
        });
        TEST_DB.put(pair(Instant.class, Long.class), new Object[][] {
                { ZonedDateTime.parse("2024-02-12T11:38:00+01:00").toInstant(), 1707734280000L },
        });
        TEST_DB.put(pair(LocalDate.class, Long.class), new Object[][] {
                { (Supplier<LocalDate>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDate();           
                }, 1707714000000L },                    // Epoch millis in Tokyo timezone (at start of day - no time)
        });
        TEST_DB.put(pair(LocalDateTime.class, Long.class), new Object[][] {
                { (Supplier<LocalDateTime>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDateTime();
                }, 1707784680000L },                    // Epoch millis in Tokyo timezone
        });
        TEST_DB.put(pair(ZonedDateTime.class, Long.class), new Object[][] {
                { ZonedDateTime.parse("2024-02-12T11:38:00+01:00"), 1707734280000L },
        });
        TEST_DB.put(pair(Calendar.class, Long.class), new Object[][] {
                { (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    return cal;
                }, 1707705480000L }
        });
        TEST_DB.put(pair(OffsetDateTime.class, Long.class), new Object[][] {
                { OffsetDateTime.parse("2024-02-12T11:38:00+01:00"), 1707734280000L },
        });
        TEST_DB.put(pair(Year.class, Long.class), new Object[][] {
                { Year.of(2024), 2024L},
        });

        /////////////////////////////////////////////////////////////
        // Float/float
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, float.class), new Object[][] {
                { null, 0.0f }
        });
        TEST_DB.put(pair(Void.class, Float.class), new Object[][] {
                { null, null }
        });
        TEST_DB.put(pair(Byte.class, Float.class), new Object[][] {
                { (byte) -1, -1f },
                { (byte) 0, 0f },
                { (byte) 1, 1f },
                { Byte.MIN_VALUE, (float)Byte.MIN_VALUE },
                { Byte.MAX_VALUE, (float)Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Float.class), new Object[][] {
                { (short)-1, -1f },
                { (short)0, 0f },
                { (short)1, 1f },
                { Short.MIN_VALUE, (float)Short.MIN_VALUE },
                { Short.MAX_VALUE, (float)Short.MAX_VALUE },
        });
        TEST_DB.put(pair(Integer.class, Float.class), new Object[][] {
                { -1, -1f },
                { 0, 0f },
                { 1, 1f },
                { 16777216, 16_777_216f },
                { -16777216, -16_777_216f },
        });
        TEST_DB.put(pair(Long.class, Float.class), new Object[][] {
                { -1L, -1f },
                { 0L, 0f },
                { 1L, 1f },
                { 16777216L, 16_777_216f },
                { -16777216L, -16_777_216f },
        });
        TEST_DB.put(pair(Float.class, Float.class), new Object[][] {
                { -1f, -1f },
                { 0f, 0f },
                { 1f, 1f },
                { Float.MIN_VALUE, Float.MIN_VALUE },
                { Float.MAX_VALUE, Float.MAX_VALUE },
                { -Float.MAX_VALUE, -Float.MAX_VALUE },
        });
        TEST_DB.put(pair(Double.class, Float.class), new Object[][] {
                { -1d, -1f },
                { -1.99d, -1.99f },
                { -1.1d, -1.1f },
                { 0d, 0f },
                { 1d, 1f },
                { 1.1d, 1.1f },
                { 1.999d, 1.999f },
                { (double)Float.MIN_VALUE, Float.MIN_VALUE },
                { (double)Float.MAX_VALUE, Float.MAX_VALUE },
                { (double)-Float.MAX_VALUE, -Float.MAX_VALUE },
        });
        TEST_DB.put(pair(Boolean.class, Float.class), new Object[][] {
                { true, 1f },
                { false, 0f }
        });
        TEST_DB.put(pair(Character.class, Float.class), new Object[][] {
                { '1', 49f },
                { '0', 48f },
                { (char) 1, 1f },
                { (char) 0, 0f },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Float.class), new Object[][] {
                { new AtomicBoolean(true), 1f },
                { new AtomicBoolean(false), 0f }
        });
        TEST_DB.put(pair(AtomicInteger.class, Float.class), new Object[][] {
                { new AtomicInteger(-1), -1f },
                { new AtomicInteger(0), 0f },
                { new AtomicInteger(1), 1f },
                { new AtomicInteger(-16777216), -16777216f },
                { new AtomicInteger(16777216), 16777216f },
        });
        TEST_DB.put(pair(AtomicLong.class, Float.class), new Object[][] {
                { new AtomicLong(-1), -1f },
                { new AtomicLong(0), 0f },
                { new AtomicLong(1), 1f },
                { new AtomicLong(-16777216), -16777216f },
                { new AtomicLong(16777216), 16777216f },
        });
        TEST_DB.put(pair(BigInteger.class, Float.class), new Object[][] {
                { new BigInteger("-1"), -1f },
                { new BigInteger("0"), 0f },
                { new BigInteger("1"), 1f },
                { new BigInteger("-16777216"), -16777216f  },
                { new BigInteger("16777216"), 16777216f  },
        });
        TEST_DB.put(pair(BigDecimal.class, Float.class), new Object[][] {
                { new BigDecimal("-1"), -1f },
                { new BigDecimal("-1.1"), -1.1f },
                { new BigDecimal("-1.9"), -1.9f },
                { new BigDecimal("0"), 0f },
                { new BigDecimal("1"), 1f },
                { new BigDecimal("1.1"), 1.1f },
                { new BigDecimal("1.9"), 1.9f },
                { new BigDecimal("-16777216"), -16777216f  },
                { new BigDecimal("16777216"), 16777216f  },
        });
        TEST_DB.put(pair(Number.class, Float.class), new Object[][] {
                { -2.2d, -2.2f}
        });
        TEST_DB.put(pair(Map.class, Float.class), new Object[][] {
                { mapOf("_v", "-1"), -1f },
                { mapOf("_v", -1), -1f },
                { mapOf("value", "-1"), -1f },
                { mapOf("value", -1L), -1f },

                { mapOf("_v", "0"), 0f },
                { mapOf("_v", 0), 0f },

                { mapOf("_v", "1"), 1f },
                { mapOf("_v", 1), 1f },

                { mapOf("_v", "-16777216"), -16777216f },
                { mapOf("_v", -16777216), -16777216f },

                { mapOf("_v", "16777216"), 16777216f },
                { mapOf("_v", 16777216), 16777216f },

                { mapOf("_v", mapOf("_v", 16777216)), 16777216f },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Float.class), new Object[][] {
                { "-1", -1f },
                { "-1.1", -1.1f },
                { "-1.9", -1.9f },
                { "0", 0f },
                { "1", 1f },
                { "1.1", 1.1f },
                { "1.9", 1.9f },
                { "-16777216", -16777216f },
                { "16777216", 16777216f },
                { "", 0f },
                { " ", 0f },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a float") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a float") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a float") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a float") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a float") },
        });
        TEST_DB.put(pair(Year.class, Float.class), new Object[][] {
                { Year.of(2024), 2024f }
        });

        /////////////////////////////////////////////////////////////
        // Double/double
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, double.class), new Object[][] {
                { null, 0d }
        });
        TEST_DB.put(pair(Void.class, Double.class), new Object[][] {
                { null, null }
        });
        TEST_DB.put(pair(Byte.class, Double.class), new Object[][] {
                { (byte) -1, -1d },
                { (byte) 0, 0d },
                { (byte) 1, 1d },
                { Byte.MIN_VALUE, (double)Byte.MIN_VALUE },
                { Byte.MAX_VALUE, (double)Byte.MAX_VALUE },
        });
        TEST_DB.put(pair(Short.class, Double.class), new Object[][] {
                { (short)-1, -1d },
                { (short)0, 0d },
                { (short)1, 1d },
                { Short.MIN_VALUE, (double)Short.MIN_VALUE },
                { Short.MAX_VALUE, (double)Short.MAX_VALUE },
        });
        TEST_DB.put(pair(Integer.class, Double.class), new Object[][] {
                { -1, -1d },
                { 0, 0d },
                { 1, 1d },
                { 2147483647, 2147483647d },
                { -2147483648, -2147483648d },
        });
        TEST_DB.put(pair(Long.class, Double.class), new Object[][] {
                { -1L, -1d },
                { 0L, 0d },
                { 1L, 1d },
                { 9007199254740991L, 9007199254740991d },
                { -9007199254740991L, -9007199254740991d },
        });
        TEST_DB.put(pair(Float.class, Double.class), new Object[][] {
                { -1f, -1d },
                { 0f, 0d },
                { 1f, 1d },
                { Float.MIN_VALUE, (double)Float.MIN_VALUE },
                { Float.MAX_VALUE, (double)Float.MAX_VALUE },
                { -Float.MAX_VALUE, (double)-Float.MAX_VALUE },
        });
        TEST_DB.put(pair(Double.class, Double.class), new Object[][] {
                { -1d, -1d },
                { -1.99d, -1.99d },
                { -1.1d, -1.1d },
                { 0d, 0d },
                { 1d, 1d },
                { 1.1d, 1.1d },
                { 1.999d, 1.999d },
                { Double.MIN_VALUE, Double.MIN_VALUE },
                { Double.MAX_VALUE, Double.MAX_VALUE },
                { -Double.MAX_VALUE, -Double.MAX_VALUE },
        });
        TEST_DB.put(pair(Boolean.class, Double.class), new Object[][] {
                { true, 1d },
                { false, 0d },
        });
        TEST_DB.put(pair(Character.class, Double.class), new Object[][] {
                { '1', 49d },
                { '0', 48d },
                { (char) 1, 1d },
                { (char) 0, 0d },
        });
        TEST_DB.put(pair(Instant.class, Double.class), new Object[][] {
                { ZonedDateTime.parse("2024-02-12T11:38:00+01:00").toInstant(), 1707734280000d },
        });
        TEST_DB.put(pair(LocalDate.class, Double.class), new Object[][] {
                { (Supplier<LocalDate>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDate();
                }, 1.707714E12d },                    // Epoch millis in Tokyo timezone (at start of day - no time)
        });
        TEST_DB.put(pair(LocalDateTime.class, Double.class), new Object[][] {
                { (Supplier<LocalDateTime>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDateTime();
                }, 1.70778468E12d },                    // Epoch millis in Tokyo timezone
        });
        TEST_DB.put(pair(ZonedDateTime.class, Double.class), new Object[][] {
                { ZonedDateTime.parse("2024-02-12T11:38:00+01:00"), 1707734280000d },
        });
        TEST_DB.put(pair(Date.class, Double.class), new Object[][] {
                { new Date(Long.MIN_VALUE), (double) Long.MIN_VALUE },
                { new Date(Integer.MIN_VALUE), (double) Integer.MIN_VALUE },
                { new Date(0), 0d },
                { new Date(Integer.MAX_VALUE), (double) Integer.MAX_VALUE },
                { new Date(Long.MAX_VALUE), (double) Long.MAX_VALUE },
        });
        TEST_DB.put(pair(java.sql.Date.class, Double.class), new Object[][] {
                { new java.sql.Date(Long.MIN_VALUE), (double) Long.MIN_VALUE },
                { new java.sql.Date(Integer.MIN_VALUE), (double) Integer.MIN_VALUE },
                { new java.sql.Date(0), 0d },
                { new java.sql.Date(Integer.MAX_VALUE), (double) Integer.MAX_VALUE },
                { new java.sql.Date(Long.MAX_VALUE), (double) Long.MAX_VALUE },
        });
        TEST_DB.put(pair(Timestamp.class, Double.class), new Object[][] {
                { new Timestamp(Long.MIN_VALUE), (double) Long.MIN_VALUE },
                { new Timestamp(Integer.MIN_VALUE), (double) Integer.MIN_VALUE },
                { new Timestamp(0), 0d },
                { new Timestamp(Integer.MAX_VALUE), (double) Integer.MAX_VALUE },
                { new Timestamp(Long.MAX_VALUE), (double) Long.MAX_VALUE },
        });
        TEST_DB.put(pair(AtomicBoolean.class, Double.class), new Object[][] {
                { new AtomicBoolean(true), 1d },
                { new AtomicBoolean(false), 0d },
        });
        TEST_DB.put(pair(AtomicInteger.class, Double.class), new Object[][] {
                { new AtomicInteger(-1), -1d },
                { new AtomicInteger(0), 0d },
                { new AtomicInteger(1), 1d },
                { new AtomicInteger(-2147483648), (double)Integer.MIN_VALUE },
                { new AtomicInteger(2147483647), (double)Integer.MAX_VALUE },
        });
        TEST_DB.put(pair(AtomicLong.class, Double.class), new Object[][] {
                { new AtomicLong(-1), -1d },
                { new AtomicLong(0), 0d },
                { new AtomicLong(1), 1d },
                { new AtomicLong(-9007199254740991L), -9007199254740991d },
                { new AtomicLong(9007199254740991L), 9007199254740991d },
        });
        TEST_DB.put(pair(BigInteger.class, Double.class), new Object[][] {
                { new BigInteger("-1"), -1d },
                { new BigInteger("0"), 0d },
                { new BigInteger("1"), 1d },
                { new BigInteger("-9007199254740991"), -9007199254740991d },
                { new BigInteger("9007199254740991"), 9007199254740991d },
        });
        TEST_DB.put(pair(BigDecimal.class, Double.class), new Object[][] {
                { new BigDecimal("-1"), -1d },
                { new BigDecimal("-1.1"), -1.1d },
                { new BigDecimal("-1.9"), -1.9d },
                { new BigDecimal("0"), 0d },
                { new BigDecimal("1"), 1d },
                { new BigDecimal("1.1"), 1.1d },
                { new BigDecimal("1.9"), 1.9d },
                { new BigDecimal("-9007199254740991"), -9007199254740991d },
                { new BigDecimal("9007199254740991"), 9007199254740991d },
        });
        TEST_DB.put(pair(Calendar.class, Double.class), new Object[][] {
                { (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    return cal;
                }, 1707705480000d }
        });
        TEST_DB.put(pair(Number.class, Double.class), new Object[][] {
                { 2.5f, 2.5d }
        });
        TEST_DB.put(pair(Map.class, Double.class), new Object[][] {
                { mapOf("_v", "-1"), -1d },
                { mapOf("_v", -1), -1d },
                { mapOf("value", "-1"), -1d },
                { mapOf("value", -1L), -1d },

                { mapOf("_v", "0"), 0d },
                { mapOf("_v", 0), 0d },

                { mapOf("_v", "1"), 1d },
                { mapOf("_v", 1), 1d },

                { mapOf("_v", "-9007199254740991"), -9007199254740991d },
                { mapOf("_v", -9007199254740991L), -9007199254740991d },

                { mapOf("_v", "9007199254740991"), 9007199254740991d },
                { mapOf("_v", 9007199254740991L), 9007199254740991d },
                
                { mapOf("_v", mapOf("_v", -9007199254740991L)), -9007199254740991d },    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Double.class), new Object[][] {
                { "-1", -1d },
                { "-1.1", -1.1d },
                { "-1.9", -1.9d },
                { "0", 0d },
                { "1", 1d },
                { "1.1", 1.1d },
                { "1.9", 1.9d },
                { "-2147483648", -2147483648d },
                { "2147483647", 2147483647d },
                { "", 0d },
                { " ", 0d },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a double") },
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a double") },
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a double") },
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a double") },
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a double") },
        });
        TEST_DB.put(pair(Year.class, Double.class), new Object[][] {
                { Year.of(2024), 2024d }
        });
        
        /////////////////////////////////////////////////////////////
        // Instant
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(String.class, Instant.class), new Object[][] {
                { "", null },
                { " ", null },
                { "1980-01-01T00:00Z", Instant.parse("1980-01-01T00:00:00Z") },
                { "2024-12-31T23:59:59.999999999Z", Instant.parse("2024-12-31T23:59:59.999999999Z") },
        });

        // MonthDay
        TEST_DB.put(pair(Void.class, MonthDay.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(MonthDay.class, MonthDay.class), new Object[][] {
                { MonthDay.of(1, 1), MonthDay.of(1, 1) },
                { MonthDay.of(12, 31), MonthDay.of(12, 31) },
                { MonthDay.of(6, 30), MonthDay.of(6, 30) },
        });
        TEST_DB.put(pair(String.class, MonthDay.class), new Object[][] {
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
        TEST_DB.put(pair(Map.class, MonthDay.class), new Object[][] {
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

        /////////////////////////////////////////////////////////////
        // YearMonth
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, YearMonth.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(YearMonth.class, YearMonth.class), new Object[][] {
                { YearMonth.of(2023, 12), YearMonth.of(2023, 12) },
                { YearMonth.of(1970, 1), YearMonth.of(1970, 1) },
                { YearMonth.of(1999, 6), YearMonth.of(1999, 6) },
        });
        TEST_DB.put(pair(String.class, YearMonth.class), new Object[][] {
                { "2024-01", YearMonth.of(2024, 1) },
                { "2024-1", new IllegalArgumentException("Unable to extract Year-Month from string: 2024-1") },
                { "2024-1-1", YearMonth.of(2024, 1) },
                { "2024-06-01", YearMonth.of(2024, 6) },
                { "2024-12-31", YearMonth.of(2024, 12) },
                { "05:45 2024-12-31", YearMonth.of(2024, 12) },
        });
        TEST_DB.put(pair(Map.class, YearMonth.class), new Object[][] {
                { mapOf("_v", "2024-01"), YearMonth.of(2024, 1) },
                { mapOf("value", "2024-01"), YearMonth.of(2024, 1) },
                { mapOf("year", "2024", "month", 12), YearMonth.of(2024, 12) },
                { mapOf("year", new BigInteger("2024"), "month", "12"), YearMonth.of(2024, 12) },
                { mapOf("year", mapOf("_v", 2024), "month", "12"), YearMonth.of(2024, 12) },    // prove recursion on year
                { mapOf("year", 2024, "month", mapOf("_v", "12")), YearMonth.of(2024, 12) },    // prove recursion on month
                { mapOf("year", 2024, "month", mapOf("_v", mapOf("_v", "12"))), YearMonth.of(2024, 12) },    // prove multiple recursive calls
        });

        /////////////////////////////////////////////////////////////
        // Period
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Period.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Period.class, Period.class), new Object[][] {
                { Period.of(0, 0, 0), Period.of(0, 0, 0) },
                { Period.of(1, 1, 1), Period.of(1, 1, 1) },
        });
        TEST_DB.put(pair(String.class, Period.class), new Object[][] {
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
        TEST_DB.put(pair(Map.class, Period.class), new Object[][] {
                { mapOf("_v", "P0D"), Period.of(0, 0, 0) },
                { mapOf("value", "P1Y1M1D"), Period.of(1, 1, 1) },
                { mapOf("years", "2", "months", 2, "days", 2.0d), Period.of(2, 2, 2) },
                { mapOf("years", mapOf("_v", (byte) 2), "months", mapOf("_v", 2.0f), "days", mapOf("_v", new AtomicInteger(2))), Period.of(2, 2, 2) },   // recursion
        });

        /////////////////////////////////////////////////////////////
        // Year
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Year.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(Year.class, Year.class), new Object[][] {
                { Year.of(1970), Year.of(1970) },
        });
        TEST_DB.put(pair(String.class, Year.class), new Object[][] {
                { "1970", Year.of(1970) },
                { "1999", Year.of(1999) },
                { "2000", Year.of(2000) },
                { "2024", Year.of(2024) },
                { "1670", Year.of(1670) },
                { "PONY", new IllegalArgumentException("Unable to parse 4-digit year from 'PONY'") },
        });
        TEST_DB.put(pair(Map.class, Year.class), new Object[][] {
                { mapOf("_v", "1984"), Year.of(1984) },
                { mapOf("value", 1984L), Year.of(1984) },
                { mapOf("year", 1492), Year.of(1492) },
                { mapOf("year", mapOf("_v", (short) 2024)), Year.of(2024) }, // recursion
        });
        TEST_DB.put(pair(Number.class, Year.class), new Object[][] {
                { (byte) 101, new IllegalArgumentException("Unsupported conversion, source type [Byte (101)] target type 'Year'") },
                { (short) 2024, Year.of(2024) },
        });

        /////////////////////////////////////////////////////////////
        // ZoneId
        /////////////////////////////////////////////////////////////
        ZoneId NY_Z = ZoneId.of("America/New_York");
        ZoneId TOKYO_Z = ZoneId.of("Asia/Tokyo");
        TEST_DB.put(pair(Void.class, ZoneId.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(ZoneId.class, ZoneId.class), new Object[][] {
                { NY_Z, NY_Z },
                { TOKYO_Z, TOKYO_Z },
        });
        TEST_DB.put(pair(String.class, ZoneId.class), new Object[][] {
                { "America/New_York", NY_Z },
                { "Asia/Tokyo", TOKYO_Z },
                { "America/Cincinnati", new IllegalArgumentException("Unknown time-zone ID: 'America/Cincinnati'") },
        });
        TEST_DB.put(pair(Map.class, ZoneId.class), new Object[][] {
                { mapOf("_v", "America/New_York"), NY_Z },
                { mapOf("_v", NY_Z), NY_Z },
                { mapOf("zone", NY_Z), NY_Z },
                { mapOf("_v", "Asia/Tokyo"), TOKYO_Z },
                { mapOf("_v", TOKYO_Z), TOKYO_Z },
                { mapOf("zone", mapOf("_v", TOKYO_Z)), TOKYO_Z },
        });

        /////////////////////////////////////////////////////////////
        // ZoneOffset
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, ZoneOffset.class), new Object[][] {
                { null, null },
        });
        TEST_DB.put(pair(ZoneOffset.class, ZoneOffset.class), new Object[][] {
                { ZoneOffset.of("-05:00"), ZoneOffset.of("-05:00") },
                { ZoneOffset.of("+5"), ZoneOffset.of("+05:00") },
        });
        TEST_DB.put(pair(String.class, ZoneOffset.class), new Object[][] {
                { "-00:00", ZoneOffset.of("+00:00") },
                { "-05:00", ZoneOffset.of("-05:00") },
                { "+5", ZoneOffset.of("+05:00") },
                { "+05:00:01", ZoneOffset.of("+05:00:01") },
                { "America/New_York", new IllegalArgumentException("Unknown time-zone offset: 'America/New_York'") },
        });
        TEST_DB.put(pair(Map.class, ZoneOffset.class), new Object[][] {
                { mapOf("_v", "-10"), ZoneOffset.of("-10:00") },
                { mapOf("hours", -10L), ZoneOffset.of("-10:00") },
                { mapOf("hours", -10L, "minutes", "0"), ZoneOffset.of("-10:00") },
                { mapOf("hrs", -10L, "mins", "0"), new IllegalArgumentException("Map to ZoneOffset the map must include one of the following: [hours, minutes, seconds], [_v], or [value]") },
                { mapOf("hours", -10L, "minutes", "0", "seconds", 0), ZoneOffset.of("-10:00") },
                { mapOf("hours", "-10", "minutes", (byte) -15, "seconds", "-1"), ZoneOffset.of("-10:15:01") },
                { mapOf("hours", "10", "minutes", (byte) 15, "seconds", true), ZoneOffset.of("+10:15:01") },
                { mapOf("hours", mapOf("_v", "10"), "minutes", mapOf("_v", (byte) 15), "seconds", mapOf("_v", true)), ZoneOffset.of("+10:15:01") }, // full recursion
        });

        /////////////////////////////////////////////////////////////
        // String
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, String.class), new Object[][] {
                { null, null }
        });
        TEST_DB.put(pair(Byte.class, String.class), new Object[][] {
                { (byte) 0, "0" },
                { Byte.MIN_VALUE, "-128" },
                { Byte.MAX_VALUE, "127" },
        });
        TEST_DB.put(pair(Short.class, String.class), new Object[][] {
                { (short) 0, "0" },
                { Short.MIN_VALUE, "-32768" },
                { Short.MAX_VALUE, "32767" },
        });
        TEST_DB.put(pair(Integer.class, String.class), new Object[][] {
                { 0, "0" },
                { Integer.MIN_VALUE, "-2147483648" },
                { Integer.MAX_VALUE, "2147483647" },
        });
        TEST_DB.put(pair(Long.class, String.class), new Object[][] {
                { 0L, "0" },
                { Long.MIN_VALUE, "-9223372036854775808" },
                { Long.MAX_VALUE, "9223372036854775807" },
        });
        TEST_DB.put(pair(Float.class, String.class), new Object[][] {
                { 0f, "0" },
                { 0.0f, "0" },
                { Float.MIN_VALUE, "1.4E-45" },
                { -Float.MAX_VALUE, "-3.4028235E38" },
                { Float.MAX_VALUE, "3.4028235E38" },
                { 12345679f, "1.2345679E7" },
                { 0.000000123456789f, "1.2345679E-7" },
                { 12345f, "12345.0" },
                { 0.00012345f, "1.2345E-4" },
        });
        TEST_DB.put(pair(Double.class, String.class), new Object[][] {
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
        TEST_DB.put(pair(Boolean.class, String.class), new Object[][] {
                { false, "false" },
                { true, "true" }
        });
        TEST_DB.put(pair(Character.class, String.class), new Object[][] {
                { '1', "1" },
                { (char) 32, " " },
        });
        TEST_DB.put(pair(BigInteger.class, String.class), new Object[][] {
                { new BigInteger("-1"), "-1" },
                { new BigInteger("0"), "0" },
                { new BigInteger("1"), "1" },
        });
        TEST_DB.put(pair(BigDecimal.class, String.class), new Object[][] {
                { new BigDecimal("-1"), "-1" },
                { new BigDecimal("-1.0"), "-1" },
                { new BigDecimal("0"), "0" },
                { new BigDecimal("0.0"), "0" },
                { new BigDecimal("1.0"), "1" },
                { new BigDecimal("3.141519265358979323846264338"), "3.141519265358979323846264338" },
        });
        TEST_DB.put(pair(AtomicBoolean.class, String.class), new Object[][] {
                { new AtomicBoolean(false), "false" },
                { new AtomicBoolean(true), "true" },
        });
        TEST_DB.put(pair(AtomicInteger.class, String.class), new Object[][] {
                { new AtomicInteger(-1), "-1" },
                { new AtomicInteger(0), "0" },
                { new AtomicInteger(1), "1" },
                { new AtomicInteger(Integer.MIN_VALUE), "-2147483648" },
                { new AtomicInteger(Integer.MAX_VALUE), "2147483647" },
        });
        TEST_DB.put(pair(AtomicLong.class, String.class), new Object[][] {
                { new AtomicLong(-1), "-1" },
                { new AtomicLong(0), "0" },
                { new AtomicLong(1), "1" },
                { new AtomicLong(Long.MIN_VALUE), "-9223372036854775808" },
                { new AtomicLong(Long.MAX_VALUE), "9223372036854775807" },
        });
        TEST_DB.put(pair(byte[].class, String.class), new Object[][] {
                { new byte[] { (byte) 0xf0, (byte) 0x9f, (byte) 0x8d, (byte) 0xba }, "\uD83C\uDF7A" }, // beer mug, byte[] treated as UTF-8.
                { new byte[] { (byte) 65, (byte) 66, (byte) 67, (byte) 68 }, "ABCD" }
        });
        TEST_DB.put(pair(char[].class, String.class), new Object[][] {
                { new char[] { 'A', 'B', 'C', 'D' }, "ABCD" }
        });
        TEST_DB.put(pair(Character[].class, String.class), new Object[][] {
                { new Character[] { 'A', 'B', 'C', 'D' }, "ABCD" }
        });
        TEST_DB.put(pair(ByteBuffer.class, String.class), new Object[][] {
                { ByteBuffer.wrap(new byte[] { (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33 }), "0123" }
        });
        TEST_DB.put(pair(CharBuffer.class, String.class), new Object[][] {
                { CharBuffer.wrap(new char[] { 'A', 'B', 'C', 'D' }), "ABCD" },
        });
        TEST_DB.put(pair(Class.class, String.class), new Object[][] {
                { Date.class, "java.util.Date" }
        });
        TEST_DB.put(pair(Date.class, String.class), new Object[][] {
                { new Date(1), toGmtString(new Date(1)) },
                { new Date(Integer.MAX_VALUE), toGmtString(new Date(Integer.MAX_VALUE)) },
                { new Date(Long.MAX_VALUE), toGmtString(new Date(Long.MAX_VALUE)) }
        });
        TEST_DB.put(pair(java.sql.Date.class, String.class), new Object[][] {
                { new java.sql.Date(1), toGmtString(new java.sql.Date(1)) },
                { new java.sql.Date(Integer.MAX_VALUE), toGmtString(new java.sql.Date(Integer.MAX_VALUE)) },
                { new java.sql.Date(Long.MAX_VALUE), toGmtString(new java.sql.Date(Long.MAX_VALUE)) }
        });
        TEST_DB.put(pair(Timestamp.class, String.class), new Object[][] {
                { new Timestamp(1), toGmtString(new Timestamp(1)) },
                { new Timestamp(Integer.MAX_VALUE), toGmtString(new Timestamp(Integer.MAX_VALUE)) },
                { new Timestamp(Long.MAX_VALUE), toGmtString(new Timestamp(Long.MAX_VALUE)) },
        });
        TEST_DB.put(pair(LocalDate.class, String.class), new Object[][] {
                { LocalDate.parse("1965-12-31"), "1965-12-31" },
        });
        TEST_DB.put(pair(LocalTime.class, String.class), new Object[][] {
                { LocalTime.parse("16:20:00"), "16:20:00" },
        });
        TEST_DB.put(pair(LocalDateTime.class, String.class), new Object[][] {
                { LocalDateTime.parse("1965-12-31T16:20:00"), "1965-12-31T16:20:00" },
        });
        TEST_DB.put(pair(ZonedDateTime.class, String.class), new Object[][] {
                { ZonedDateTime.parse("1965-12-31T16:20:00+00:00"), "1965-12-31T16:20:00Z" },
                { ZonedDateTime.parse("2024-02-14T19:20:00-05:00"), "2024-02-14T19:20:00-05:00" },
                { ZonedDateTime.parse("2024-02-14T19:20:00+05:00"), "2024-02-14T19:20:00+05:00" }
        });
        TEST_DB.put(pair(UUID.class, String.class), new Object[][] {
                { new UUID(0L, 0L), "00000000-0000-0000-0000-000000000000" },
                { new UUID(1L, 1L), "00000000-0000-0001-0000-000000000001" },
                { new UUID(Long.MAX_VALUE, Long.MAX_VALUE), "7fffffff-ffff-ffff-7fff-ffffffffffff" },
                { new UUID(Long.MIN_VALUE, Long.MIN_VALUE), "80000000-0000-0000-8000-000000000000" },
        });
        TEST_DB.put(pair(Calendar.class, String.class), new Object[][] {
                { (Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 0);
                    return cal;
                }, "2024-02-05T22:31:00" }
        });
        TEST_DB.put(pair(Number.class, String.class), new Object[][] {
                { (byte) 1, "1" },
                { (short) 2, "2" },
                { 3, "3" },
                { 4L, "4" },
                { 5f, "5.0" },
                { 6d, "6.0" },
                { new AtomicInteger(7), "7" },
                { new AtomicLong(8L), "8" },
                { new BigInteger("9"), "9" },
                { new BigDecimal("10"), "10" },
        });
        TEST_DB.put(pair(Map.class, String.class), new Object[][] {
                { mapOf("_v", "alpha"), "alpha" },
                { mapOf("value", "alpha"), "alpha" },
        });
        TEST_DB.put(pair(Enum.class, String.class), new Object[][] {
                { DayOfWeek.MONDAY, "MONDAY" },
                { Month.JANUARY, "JANUARY" },
        });
        TEST_DB.put(pair(String.class, String.class), new Object[][] {
                { "same", "same" },
        });
        TEST_DB.put(pair(Duration.class, String.class), new Object[][] {
                { Duration.parse("PT20.345S"), "PT20.345S"},
                { Duration.ofSeconds(60), "PT1M"},
        });
        TEST_DB.put(pair(Instant.class, String.class), new Object[][] {
                { Instant.ofEpochMilli(0), "1970-01-01T00:00:00Z"},
                { Instant.ofEpochMilli(1), "1970-01-01T00:00:00.001Z"},
                { Instant.ofEpochMilli(1000), "1970-01-01T00:00:01Z"},
                { Instant.ofEpochMilli(1001), "1970-01-01T00:00:01.001Z"},
                { Instant.ofEpochSecond(0), "1970-01-01T00:00:00Z"},
                { Instant.ofEpochSecond(1), "1970-01-01T00:00:01Z"},
                { Instant.ofEpochSecond(60), "1970-01-01T00:01:00Z"},
                { Instant.ofEpochSecond(61), "1970-01-01T00:01:01Z"},
                { Instant.ofEpochSecond(0, 0), "1970-01-01T00:00:00Z"},
                { Instant.ofEpochSecond(0, 1), "1970-01-01T00:00:00.000000001Z"},
                { Instant.ofEpochSecond(0, 999999999), "1970-01-01T00:00:00.999999999Z"},
                { Instant.ofEpochSecond(0, 9999999999L), "1970-01-01T00:00:09.999999999Z"},
        });
        TEST_DB.put(pair(LocalTime.class, String.class), new Object[][] {
                { LocalTime.of(9, 26), "09:26" },
                { LocalTime.of(9, 26, 17), "09:26:17" },
                { LocalTime.of(9, 26, 17, 1), "09:26:17.000000001" },
        });
        TEST_DB.put(pair(MonthDay.class, String.class), new Object[][] {
                { MonthDay.of(1, 1), "--01-01"},
                { MonthDay.of(12, 31), "--12-31"},
        });
        TEST_DB.put(pair(YearMonth.class, String.class), new Object[][] {
                { YearMonth.of(2024, 1), "2024-01" },
                { YearMonth.of(2024, 12), "2024-12" },
        });
        TEST_DB.put(pair(Period.class, String.class), new Object[][] {
                { Period.of(6, 3, 21), "P6Y3M21D" },
                { Period.ofWeeks(160), "P1120D" },
        });
        TEST_DB.put(pair(ZoneId.class, String.class), new Object[][] {
                { ZoneId.of("America/New_York"), "America/New_York"},
                { ZoneId.of("Z"), "Z"},
                { ZoneId.of("UTC"), "UTC"},
                { ZoneId.of("GMT"), "GMT"},
        });
        TEST_DB.put(pair(ZoneOffset.class, String.class), new Object[][] {
                { ZoneOffset.of("+1"), "+01:00" },
                { ZoneOffset.of("+0109"), "+01:09" },
        });
        TEST_DB.put(pair(OffsetTime.class, String.class), new Object[][] {
                { OffsetTime.parse("10:15:30+01:00"), "10:15:30+01:00" },
        });
        TEST_DB.put(pair(OffsetDateTime.class, String.class), new Object[][] {
                { OffsetDateTime.parse("2024-02-10T10:15:07+01:00"), "2024-02-10T10:15:07+01:00" },
        });
        TEST_DB.put(pair(Year.class, String.class), new Object[][] {
                { Year.of(2024), "2024" },
                { Year.of(1582), "1582" },
                { Year.of(500), "500" },
                { Year.of(1), "1" },
                { Year.of(0), "0" },
                { Year.of(-1), "-1" },
        });
    }
    
    private static String toGmtString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(TOKYO_TZ);
        return simpleDateFormat.format(date);
    }

    @BeforeEach
    void before() {
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
                Object[][] testData = TEST_DB.get(pair(sourceClass, targetClass));

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
        List<Arguments> list = new ArrayList<>(400);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Object[][]> entry : TEST_DB.entrySet()) {
            Class<?> sourceClass = entry.getKey().getKey();
            Class<?> targetClass = entry.getKey().getValue();

            String sourceName = Converter.getShortName(sourceClass);
            String targetName = Converter.getShortName(targetClass);
            Object[][] testData = entry.getValue();

            for (Object[] testPair : testData) {
                Object source = possiblyConvertSupplier(testPair[0]);
                Object target = possiblyConvertSupplier(testPair[1]);

                list.add(Arguments.of(sourceName, targetName, source, target, sourceClass, targetClass));
            }
        }

        return Stream.of(list.toArray(new Arguments[] {}));
    }
    
    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParams")
    void testConvert(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass) {
        // Make sure source instance is of the sourceClass
        if (source == null) {
            assertEquals(sourceClass, Void.class, "On the source-side of test input, null can only appear in the Void.class data");
        } else {
            assertTrue(ClassUtilities.toPrimitiveWrapperClass(sourceClass).isInstance(source), "source type mismatch");
        }
        assertTrue(target == null || target instanceof Throwable || ClassUtilities.toPrimitiveWrapperClass(targetClass).isInstance(target), "target type mismatch");

        // if the source/target are the same Class, then ensure identity lambda is used.
        if (sourceClass.equals(targetClass)) {
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
                assertEquals(target, actual);
            } catch (Throwable e) {
                System.err.println(shortNameSource + "[" + source + "] ==> " + shortNameTarget + "[" + target + "] Failed with: " + actual);
                throw e;
            }
        }
    }
}
