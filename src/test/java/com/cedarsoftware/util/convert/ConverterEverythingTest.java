package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
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

import com.cedarsoftware.util.ClassUtilities;
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
    private static final TimeZone TOKYO_TZ = TimeZone.getTimeZone(TOKYO_Z);
    private Converter converter;
    private final ConverterOptions options = new ConverterOptions() {
        public ZoneId getZoneId() {
            return TOKYO_Z;
        }
    };
    private static final Map<Map.Entry<Class<?>, Class<?>>, Object[][]> TEST_DB = new ConcurrentHashMap<>(500, .8f);

    static {
        //   {source1, answer1},
        //   ...
        //   {source-n, answer-n}

        // Useful values for input
        long now = System.currentTimeMillis();

        /////////////////////////////////////////////////////////////
        // Byte/byte
        /////////////////////////////////////////////////////////////
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
                {(short) -1, (byte) -1},
                {(short) 0, (byte) 0},
                {(short) 1, (byte) 1},
                {(short) -128, Byte.MIN_VALUE},
                {(short) 127, Byte.MAX_VALUE},
                {(short) -129, Byte.MAX_VALUE},    // verify wrap around
                {(short) 128, Byte.MIN_VALUE},    // verify wrap around
        });
        TEST_DB.put(pair(Integer.class, Byte.class), new Object[][]{
                {-1, (byte) -1},
                {0, (byte) 0},
                {1, (byte) 1},
                {-128, Byte.MIN_VALUE},
                {127, Byte.MAX_VALUE},
                {-129, Byte.MAX_VALUE},   // verify wrap around
                {128, Byte.MIN_VALUE},   // verify wrap around
        });
        TEST_DB.put(pair(Long.class, Byte.class), new Object[][]{
                {-1L, (byte) -1},
                {0L, (byte) 0},
                {1L, (byte) 1},
                {-128L, Byte.MIN_VALUE},
                {127L, Byte.MAX_VALUE},
                {-129L, Byte.MAX_VALUE}, // verify wrap around
                {128L, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Float.class, Byte.class), new Object[][]{
                {-1f, (byte) -1},
                {-1.99f, (byte) -1},
                {-1.1f, (byte) -1},
                {0f, (byte) 0},
                {1f, (byte) 1},
                {1.1f, (byte) 1},
                {1.999f, (byte) 1},
                {-128f, Byte.MIN_VALUE},
                {127f, Byte.MAX_VALUE},
                {-129f, Byte.MAX_VALUE}, // verify wrap around
                {128f, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Double.class, Byte.class), new Object[][]{
                {-1d, (byte) -1},
                {-1.99, (byte) -1},
                {-1.1, (byte) -1},
                {0d, (byte) 0},
                {1d, (byte) 1},
                {1.1, (byte) 1},
                {1.999, (byte) 1},
                {-128d, Byte.MIN_VALUE},
                {127d, Byte.MAX_VALUE},
                {-129d, Byte.MAX_VALUE}, // verify wrap around
                {128d, Byte.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Boolean.class, Byte.class), new Object[][]{
                {true, (byte) 1},
                {false, (byte) 0},
        });
        TEST_DB.put(pair(Character.class, Byte.class), new Object[][]{
                {'1', (byte) 49},
                {'0', (byte) 48},
                {(char) 1, (byte) 1},
                {(char) 0, (byte) 0},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Byte.class), new Object[][]{
                {new AtomicBoolean(true), (byte) 1},
                {new AtomicBoolean(false), (byte) 0},
        });
        TEST_DB.put(pair(AtomicInteger.class, Byte.class), new Object[][]{
                {new AtomicInteger(-1), (byte) -1},
                {new AtomicInteger(0), (byte) 0},
                {new AtomicInteger(1), (byte) 1},
                {new AtomicInteger(-128), Byte.MIN_VALUE},
                {new AtomicInteger(127), Byte.MAX_VALUE},
                {new AtomicInteger(-129), Byte.MAX_VALUE},
                {new AtomicInteger(128), Byte.MIN_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Byte.class), new Object[][]{
                {new AtomicLong(-1), (byte) -1},
                {new AtomicLong(0), (byte) 0},
                {new AtomicLong(1), (byte) 1},
                {new AtomicLong(-128), Byte.MIN_VALUE},
                {new AtomicLong(127), Byte.MAX_VALUE},
                {new AtomicLong(-129), Byte.MAX_VALUE},
                {new AtomicLong(128), Byte.MIN_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Byte.class), new Object[][]{
                {new BigInteger("-1"), (byte) -1},
                {new BigInteger("0"), (byte) 0},
                {new BigInteger("1"), (byte) 1},
                {new BigInteger("-128"), Byte.MIN_VALUE},
                {new BigInteger("127"), Byte.MAX_VALUE},
                {new BigInteger("-129"), Byte.MAX_VALUE},
                {new BigInteger("128"), Byte.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Byte.class), new Object[][]{
                {new BigDecimal("-1"), (byte) -1},
                {new BigDecimal("-1.1"), (byte) -1},
                {new BigDecimal("-1.9"), (byte) -1},
                {new BigDecimal("0"), (byte) 0},
                {new BigDecimal("1"), (byte) 1},
                {new BigDecimal("1.1"), (byte) 1},
                {new BigDecimal("1.9"), (byte) 1},
                {new BigDecimal("-128"), Byte.MIN_VALUE},
                {new BigDecimal("127"), Byte.MAX_VALUE},
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
                {Year.of(2024), new IllegalArgumentException("Unsupported conversion, source type [Year (2024)] target type 'Byte'")},
        });
        TEST_DB.put(pair(String.class, Byte.class), new Object[][]{
                {"-1", (byte) -1},
                {"-1.1", (byte) -1},
                {"-1.9", (byte) -1},
                {"0", (byte) 0},
                {"1", (byte) 1},
                {"1.1", (byte) 1},
                {"1.9", (byte) 1},
                {"-128", (byte) -128},
                {"127", (byte) 127},
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

        /////////////////////////////////////////////////////////////
        // Short/short
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, short.class), new Object[][]{
                {null, (short) 0},
        });
        TEST_DB.put(pair(Void.class, Short.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Short.class), new Object[][]{
                {(byte) -1, (short) -1},
                {(byte) 0, (short) 0},
                {(byte) 1, (short) 1},
                {Byte.MIN_VALUE, (short) Byte.MIN_VALUE},
                {Byte.MAX_VALUE, (short) Byte.MAX_VALUE},
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
                {-1d, (short) -1},
                {-1.99, (short) -1},
                {-1.1, (short) -1},
                {0d, (short) 0},
                {1d, (short) 1},
                {1.1, (short) 1},
                {1.999, (short) 1},
                {-32768d, Short.MIN_VALUE},
                {32767d, Short.MAX_VALUE},
                {-32769d, Short.MAX_VALUE}, // verify wrap around
                {32768d, Short.MIN_VALUE}   // verify wrap around
        });
        TEST_DB.put(pair(Boolean.class, Short.class), new Object[][]{
                {true, (short) 1},
                {false, (short) 0},
        });
        TEST_DB.put(pair(Character.class, Short.class), new Object[][]{
                {'1', (short) 49},
                {'0', (short) 48},
                {(char) 1, (short) 1},
                {(char) 0, (short) 0},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Short.class), new Object[][]{
                {new AtomicBoolean(true), (short) 1},
                {new AtomicBoolean(false), (short) 0},
        });
        TEST_DB.put(pair(AtomicInteger.class, Short.class), new Object[][]{
                {new AtomicInteger(-1), (short) -1},
                {new AtomicInteger(0), (short) 0},
                {new AtomicInteger(1), (short) 1},
                {new AtomicInteger(-32768), Short.MIN_VALUE},
                {new AtomicInteger(32767), Short.MAX_VALUE},
                {new AtomicInteger(-32769), Short.MAX_VALUE},
                {new AtomicInteger(32768), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Short.class), new Object[][]{
                {new AtomicLong(-1), (short) -1},
                {new AtomicLong(0), (short) 0},
                {new AtomicLong(1), (short) 1},
                {new AtomicLong(-32768), Short.MIN_VALUE},
                {new AtomicLong(32767), Short.MAX_VALUE},
                {new AtomicLong(-32769), Short.MAX_VALUE},
                {new AtomicLong(32768), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Short.class), new Object[][]{
                {new BigInteger("-1"), (short) -1},
                {new BigInteger("0"), (short) 0},
                {new BigInteger("1"), (short) 1},
                {new BigInteger("-32768"), Short.MIN_VALUE},
                {new BigInteger("32767"), Short.MAX_VALUE},
                {new BigInteger("-32769"), Short.MAX_VALUE},
                {new BigInteger("32768"), Short.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Short.class), new Object[][]{
                {new BigDecimal("-1"), (short) -1},
                {new BigDecimal("-1.1"), (short) -1},
                {new BigDecimal("-1.9"), (short) -1},
                {new BigDecimal("0"), (short) 0},
                {new BigDecimal("1"), (short) 1},
                {new BigDecimal("1.1"), (short) 1},
                {new BigDecimal("1.9"), (short) 1},
                {new BigDecimal("-32768"), Short.MIN_VALUE},
                {new BigDecimal("32767"), Short.MAX_VALUE},
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
                {mapOf("_v", -32768), Short.MIN_VALUE},

                {mapOf("_v", "32767"), Short.MAX_VALUE},
                {mapOf("_v", 32767), Short.MAX_VALUE},

                {mapOf("_v", "-32769"), new IllegalArgumentException("'-32769' not parseable as a short value or outside -32768 to 32767")},
                {mapOf("_v", -32769), Short.MAX_VALUE},

                {mapOf("_v", "32768"), new IllegalArgumentException("'32768' not parseable as a short value or outside -32768 to 32767")},
                {mapOf("_v", 32768), Short.MIN_VALUE},
                {mapOf("_v", mapOf("_v", 32768L)), Short.MIN_VALUE},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Short.class), new Object[][]{
                {"-1", (short) -1},
                {"-1.1", (short) -1},
                {"-1.9", (short) -1},
                {"0", (short) 0},
                {"1", (short) 1},
                {"1.1", (short) 1},
                {"1.9", (short) 1},
                {"-32768", (short) -32768},
                {"32767", (short) 32767},
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

        /////////////////////////////////////////////////////////////
        // Integer/int
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, int.class), new Object[][]{
                {null, 0},
        });
        TEST_DB.put(pair(Void.class, Integer.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Integer.class), new Object[][]{
                {(byte) -1, -1},
                {(byte) 0, 0},
                {(byte) 1, 1},
                {Byte.MIN_VALUE, (int) Byte.MIN_VALUE},
                {Byte.MAX_VALUE, (int) Byte.MAX_VALUE},
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
                {-1d, -1},
                {-1.99, -1},
                {-1.1, -1},
                {0d, 0},
                {1d, 1},
                {1.1, 1},
                {1.999, 1},
                {-2147483648d, Integer.MIN_VALUE},
                {2147483647d, Integer.MAX_VALUE},
        });
        TEST_DB.put(pair(Boolean.class, Integer.class), new Object[][]{
                {true, 1},
                {false, 0},
        });
        TEST_DB.put(pair(Character.class, Integer.class), new Object[][]{
                {'1', 49},
                {'0', 48},
                {(char) 1, 1},
                {(char) 0, 0},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Integer.class), new Object[][]{
                {new AtomicBoolean(true), 1},
                {new AtomicBoolean(false), 0},
        });
        TEST_DB.put(pair(AtomicInteger.class, Integer.class), new Object[][]{
                {new AtomicInteger(-1), -1},
                {new AtomicInteger(0), 0},
                {new AtomicInteger(1), 1},
                {new AtomicInteger(-2147483648), Integer.MIN_VALUE},
                {new AtomicInteger(2147483647), Integer.MAX_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Integer.class), new Object[][]{
                {new AtomicLong(-1), -1},
                {new AtomicLong(0), 0},
                {new AtomicLong(1), 1},
                {new AtomicLong(-2147483648), Integer.MIN_VALUE},
                {new AtomicLong(2147483647), Integer.MAX_VALUE},
                {new AtomicLong(-2147483649L), Integer.MAX_VALUE},
                {new AtomicLong(2147483648L), Integer.MIN_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Integer.class), new Object[][]{
                {new BigInteger("-1"), -1},
                {new BigInteger("0"), 0},
                {new BigInteger("1"), 1},
                {new BigInteger("-2147483648"), Integer.MIN_VALUE},
                {new BigInteger("2147483647"), Integer.MAX_VALUE},
                {new BigInteger("-2147483649"), Integer.MAX_VALUE},
                {new BigInteger("2147483648"), Integer.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Integer.class), new Object[][]{
                {new BigDecimal("-1"), -1},
                {new BigDecimal("-1.1"), -1},
                {new BigDecimal("-1.9"), -1},
                {new BigDecimal("0"), 0},
                {new BigDecimal("1"), 1},
                {new BigDecimal("1.1"), 1},
                {new BigDecimal("1.9"), 1},
                {new BigDecimal("-2147483648"), Integer.MIN_VALUE},
                {new BigDecimal("2147483647"), Integer.MAX_VALUE},
                {new BigDecimal("-2147483649"), Integer.MAX_VALUE},
                {new BigDecimal("2147483648"), Integer.MIN_VALUE},
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
                {"-1", -1},
                {"-1.1", -1},
                {"-1.9", -1},
                {"0", 0},
                {"1", 1},
                {"1.1", 1},
                {"1.9", 1},
                {"-2147483648", -2147483648},
                {"2147483647", 2147483647},
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
                {Year.of(-1), -1},
                {Year.of(0), 0},
                {Year.of(1), 1},
                {Year.of(1582), 1582},
                {Year.of(1970), 1970},
                {Year.of(2000), 2000},
                {Year.of(2024), 2024},
                {Year.of(9999), 9999},
        });

        /////////////////////////////////////////////////////////////
        // Long/long
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, long.class), new Object[][]{
                {null, 0L},
        });
        TEST_DB.put(pair(Void.class, Long.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Long.class), new Object[][]{
                {(byte) -1, -1L},
                {(byte) 0, 0L},
                {(byte) 1, 1L},
                {Byte.MIN_VALUE, (long) Byte.MIN_VALUE},
                {Byte.MAX_VALUE, (long) Byte.MAX_VALUE},
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
                {-1d, -1L},
                {-1.99, -1L},
                {-1.1, -1L},
                {0d, 0L},
                {1d, 1L},
                {1.1, 1L},
                {1.999, 1L},
                {-9223372036854775808d, Long.MIN_VALUE},
                {9223372036854775807d, Long.MAX_VALUE},
        });
        TEST_DB.put(pair(Boolean.class, Long.class), new Object[][]{
                {true, 1L},
                {false, 0L},
        });
        TEST_DB.put(pair(Character.class, Long.class), new Object[][]{
                {'1', 49L},
                {'0', 48L},
                {(char) 1, 1L},
                {(char) 0, 0L},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Long.class), new Object[][]{
                {new AtomicBoolean(true), 1L},
                {new AtomicBoolean(false), 0L},
        });
        TEST_DB.put(pair(AtomicInteger.class, Long.class), new Object[][]{
                {new AtomicInteger(-1), -1L},
                {new AtomicInteger(0), 0L},
                {new AtomicInteger(1), 1L},
                {new AtomicInteger(-2147483648), (long) Integer.MIN_VALUE},
                {new AtomicInteger(2147483647), (long) Integer.MAX_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Long.class), new Object[][]{
                {new AtomicLong(-1), -1L},
                {new AtomicLong(0), 0L},
                {new AtomicLong(1), 1L},
                {new AtomicLong(-9223372036854775808L), Long.MIN_VALUE},
                {new AtomicLong(9223372036854775807L), Long.MAX_VALUE},
        });
        TEST_DB.put(pair(BigInteger.class, Long.class), new Object[][]{
                {new BigInteger("-1"), -1L},
                {new BigInteger("0"), 0L},
                {new BigInteger("1"), 1L},
                {new BigInteger("-9223372036854775808"), Long.MIN_VALUE},
                {new BigInteger("9223372036854775807"), Long.MAX_VALUE},
                {new BigInteger("-9223372036854775809"), Long.MAX_VALUE},
                {new BigInteger("9223372036854775808"), Long.MIN_VALUE},
        });
        TEST_DB.put(pair(BigDecimal.class, Long.class), new Object[][]{
                {new BigDecimal("-1"), -1L},
                {new BigDecimal("-1.1"), -1L},
                {new BigDecimal("-1.9"), -1L},
                {new BigDecimal("0"), 0L},
                {new BigDecimal("1"), 1L},
                {new BigDecimal("1.1"), 1L},
                {new BigDecimal("1.9"), 1L},
                {new BigDecimal("-9223372036854775808"), Long.MIN_VALUE},
                {new BigDecimal("9223372036854775807"), Long.MAX_VALUE},
                {new BigDecimal("-9223372036854775809"), Long.MAX_VALUE},       // wrap around
                {new BigDecimal("9223372036854775808"), Long.MIN_VALUE},        // wrap around
        });
        TEST_DB.put(pair(Number.class, Long.class), new Object[][]{
                {-2, -2L},
        });
        TEST_DB.put(pair(Map.class, Long.class), new Object[][]{
                {mapOf("_v", "-1"), -1L},
                {mapOf("_v", -1), -1L},
                {mapOf("value", "-1"), -1L},
                {mapOf("value", -1L), -1L},

                {mapOf("_v", "0"), 0L},
                {mapOf("_v", 0), 0L},

                {mapOf("_v", "1"), 1L},
                {mapOf("_v", 1), 1L},

                {mapOf("_v", "-9223372036854775808"), Long.MIN_VALUE},
                {mapOf("_v", -9223372036854775808L), Long.MIN_VALUE},

                {mapOf("_v", "9223372036854775807"), Long.MAX_VALUE},
                {mapOf("_v", 9223372036854775807L), Long.MAX_VALUE},

                {mapOf("_v", "-9223372036854775809"), new IllegalArgumentException("'-9223372036854775809' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},

                {mapOf("_v", "9223372036854775808"), new IllegalArgumentException("'9223372036854775808' not parseable as a long value or outside -9223372036854775808 to 9223372036854775807")},
                {mapOf("_v", mapOf("_v", -9223372036854775808L)), Long.MIN_VALUE},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Long.class), new Object[][]{
                {"-1", -1L},
                {"-1.1", -1L},
                {"-1.9", -1L},
                {"0", 0L},
                {"1", 1L},
                {"1.1", 1L},
                {"1.9", 1L},
                {"-2147483648", -2147483648L},
                {"2147483647", 2147483647L},
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
                {new Date(Long.MIN_VALUE), Long.MIN_VALUE},
                {new Date(now), now},
                {new Date(Integer.MIN_VALUE), (long) Integer.MIN_VALUE},
                {new Date(0), 0L},
                {new Date(Integer.MAX_VALUE), (long) Integer.MAX_VALUE},
                {new Date(Long.MAX_VALUE), Long.MAX_VALUE},
        });
        TEST_DB.put(pair(java.sql.Date.class, Long.class), new Object[][]{
                {new java.sql.Date(Long.MIN_VALUE), Long.MIN_VALUE},
                {new java.sql.Date(Integer.MIN_VALUE), (long) Integer.MIN_VALUE},
                {new java.sql.Date(now), now},
                {new java.sql.Date(0), 0L},
                {new java.sql.Date(Integer.MAX_VALUE), (long) Integer.MAX_VALUE},
                {new java.sql.Date(Long.MAX_VALUE), Long.MAX_VALUE},
        });
        TEST_DB.put(pair(Timestamp.class, Long.class), new Object[][]{
                {new Timestamp(Long.MIN_VALUE), Long.MIN_VALUE},
                {new Timestamp(Integer.MIN_VALUE), (long) Integer.MIN_VALUE},
                {new Timestamp(now), now},
                {new Timestamp(0), 0L},
                {new Timestamp(Integer.MAX_VALUE), (long) Integer.MAX_VALUE},
                {new Timestamp(Long.MAX_VALUE), Long.MAX_VALUE},
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
                {ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -62167252739000L, true},
                {ZonedDateTime.parse("0000-01-01T00:00:00.001Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -62167252739000L, true},
                {ZonedDateTime.parse("1969-12-31T14:59:59.999Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -118800000L, true},
                {ZonedDateTime.parse("1969-12-31T15:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400000L, true},
                {ZonedDateTime.parse("1969-12-31T23:59:59.999Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400000L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400000L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.001Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400000L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.999Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400000L, true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Long.class), new Object[][]{
                {ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -62167219200000L, true},
                {ZonedDateTime.parse("0000-01-01T00:00:00.001Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -62167219199999L, true},
                {ZonedDateTime.parse("1969-12-31T23:59:59Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -1000L, true},
                {ZonedDateTime.parse("1969-12-31T23:59:59.999Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -1L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), 0L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.001Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), 1L, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.999Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), 999L, true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, Long.class), new Object[][]{      // no reverse check - timezone display issue
                {ZonedDateTime.parse("0000-01-01T00:00:00Z"), -62167219200000L},
                {ZonedDateTime.parse("0000-01-01T00:00:00.001Z"), -62167219199999L},
                {ZonedDateTime.parse("1969-12-31T23:59:59Z"), -1000L},
                {ZonedDateTime.parse("1969-12-31T23:59:59.999Z"), -1L},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z"), 0L},
                {ZonedDateTime.parse("1970-01-01T00:00:00.001Z"), 1L},
                {ZonedDateTime.parse("1970-01-01T00:00:00.999Z"), 999L},
        });
        TEST_DB.put(pair(Calendar.class, Long.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    return cal;
                }, 1707705480000L},
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.setTimeInMillis(now);   // Calendar maintains time to millisecond resolution
                    return cal;
                }, now}
        });
        TEST_DB.put(pair(OffsetDateTime.class, Long.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-12T11:38:00+01:00"), 1707734280000L},
                {OffsetDateTime.parse("2024-02-12T11:38:00.123+01:00"), 1707734280123L},        // maintains millis (best long can do)
                {OffsetDateTime.parse("2024-02-12T11:38:00.12399+01:00"), 1707734280123L},      // maintains millis (best long can do)
        });
        TEST_DB.put(pair(Year.class, Long.class), new Object[][]{
                {Year.of(2024), 2024L},
        });

        /////////////////////////////////////////////////////////////
        // Float/float
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, float.class), new Object[][]{
                {null, 0.0f}
        });
        TEST_DB.put(pair(Void.class, Float.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Byte.class, Float.class), new Object[][]{
                {(byte) -1, -1f},
                {(byte) 0, 0f},
                {(byte) 1, 1f},
                {Byte.MIN_VALUE, (float) Byte.MIN_VALUE},
                {Byte.MAX_VALUE, (float) Byte.MAX_VALUE},
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
                {-1d, -1f},
                {-1.99, -1.99f},
                {-1.1, -1.1f},
                {0d, 0f},
                {1d, 1f},
                {1.1, 1.1f},
                {1.999, 1.999f},
                {(double) Float.MIN_VALUE, Float.MIN_VALUE},
                {(double) Float.MAX_VALUE, Float.MAX_VALUE},
                {(double) -Float.MAX_VALUE, -Float.MAX_VALUE},
        });
        TEST_DB.put(pair(Boolean.class, Float.class), new Object[][]{
                {true, 1f},
                {false, 0f}
        });
        TEST_DB.put(pair(Character.class, Float.class), new Object[][]{
                {'1', 49f},
                {'0', 48f},
                {(char) 1, 1f},
                {(char) 0, 0f},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Float.class), new Object[][]{
                {new AtomicBoolean(true), 1f},
                {new AtomicBoolean(false), 0f}
        });
        TEST_DB.put(pair(AtomicInteger.class, Float.class), new Object[][]{
                {new AtomicInteger(-1), -1f},
                {new AtomicInteger(0), 0f},
                {new AtomicInteger(1), 1f},
                {new AtomicInteger(-16777216), -16777216f},
                {new AtomicInteger(16777216), 16777216f},
        });
        TEST_DB.put(pair(AtomicLong.class, Float.class), new Object[][]{
                {new AtomicLong(-1), -1f},
                {new AtomicLong(0), 0f},
                {new AtomicLong(1), 1f},
                {new AtomicLong(-16777216), -16777216f},
                {new AtomicLong(16777216), 16777216f},
        });
        TEST_DB.put(pair(BigInteger.class, Float.class), new Object[][]{
                {new BigInteger("-1"), -1f},
                {new BigInteger("0"), 0f},
                {new BigInteger("1"), 1f},
                {new BigInteger("-16777216"), -16777216f},
                {new BigInteger("16777216"), 16777216f},
        });
        TEST_DB.put(pair(BigDecimal.class, Float.class), new Object[][]{
                {new BigDecimal("-1"), -1f},
                {new BigDecimal("-1.1"), -1.1f},
                {new BigDecimal("-1.9"), -1.9f},
                {new BigDecimal("0"), 0f},
                {new BigDecimal("1"), 1f},
                {new BigDecimal("1.1"), 1.1f},
                {new BigDecimal("1.9"), 1.9f},
                {new BigDecimal("-16777216"), -16777216f},
                {new BigDecimal("16777216"), 16777216f},
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
                {"-1", -1f},
                {"-1.1", -1.1f},
                {"-1.9", -1.9f},
                {"0", 0f},
                {"1", 1f},
                {"1.1", 1.1f},
                {"1.9", 1.9f},
                {"-16777216", -16777216f},
                {"16777216", 16777216f},
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

        /////////////////////////////////////////////////////////////
        // Double/double
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, double.class), new Object[][]{
                {null, 0d}
        });
        TEST_DB.put(pair(Void.class, Double.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Byte.class, Double.class), new Object[][]{
                {(byte) -1, -1d},
                {(byte) 0, 0d},
                {(byte) 1, 1d},
                {Byte.MIN_VALUE, (double) Byte.MIN_VALUE},
                {Byte.MAX_VALUE, (double) Byte.MAX_VALUE},
        });
        TEST_DB.put(pair(Short.class, Double.class), new Object[][]{
                {(short) -1, -1d},
                {(short) 0, 0d},
                {(short) 1, 1d},
                {Short.MIN_VALUE, (double) Short.MIN_VALUE},
                {Short.MAX_VALUE, (double) Short.MAX_VALUE},
        });
        TEST_DB.put(pair(Integer.class, Double.class), new Object[][]{
                {-1, -1d},
                {0, 0d},
                {1, 1d},
                {2147483647, 2147483647d},
                {-2147483648, -2147483648d},
        });
        TEST_DB.put(pair(Long.class, Double.class), new Object[][]{
                {-1L, -1d},
                {0L, 0d},
                {1L, 1d},
                {9007199254740991L, 9007199254740991d},
                {-9007199254740991L, -9007199254740991d},
        });
        TEST_DB.put(pair(Float.class, Double.class), new Object[][]{
                {-1f, -1d},
                {0f, 0d},
                {1f, 1d},
                {Float.MIN_VALUE, (double) Float.MIN_VALUE},
                {Float.MAX_VALUE, (double) Float.MAX_VALUE},
                {-Float.MAX_VALUE, (double) -Float.MAX_VALUE},
        });
        TEST_DB.put(pair(Double.class, Double.class), new Object[][]{
                {-1d, -1d},
                {-1.99, -1.99},
                {-1.1, -1.1},
                {0d, 0d},
                {1d, 1d},
                {1.1, 1.1},
                {1.999, 1.999},
                {Double.MIN_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MAX_VALUE},
                {-Double.MAX_VALUE, -Double.MAX_VALUE},
        });
        TEST_DB.put(pair(Boolean.class, Double.class), new Object[][]{
                {true, 1d},
                {false, 0d},
        });
        TEST_DB.put(pair(Character.class, Double.class), new Object[][]{
                {'1', 49d},
                {'0', 48d},
                {(char) 1, 1d},
                {(char) 0, 0d},
        });
        TEST_DB.put(pair(Duration.class, Double.class), new Object[][] {
                { Duration.ofSeconds(-1, -1), -1.000000001, true },
                { Duration.ofSeconds(-1), -1d, true },
                { Duration.ofSeconds(0), 0d, true },
                { Duration.ofSeconds(1), 1d, true },
                { Duration.ofNanos(1), 0.000000001, true },
                { Duration.ofNanos(1_000_000_000), 1d, true },
                { Duration.ofNanos(2_000_000_001), 2.000000001, true },
                { Duration.ofSeconds(10, 9), 10.000000009, true },
                { Duration.ofDays(1), 86400d, true},
        });
        TEST_DB.put(pair(Instant.class, Double.class), new Object[][]{      // JDK 1.8 cannot handle the format +01:00 in Instant.parse().  JDK11+ handles it fine.
                {Instant.parse("0000-01-01T00:00:00Z"), -62167219200.0, true},
                {Instant.parse("1969-12-31T00:00:00Z"), -86400d, true},
                {Instant.parse("1969-12-31T00:00:00Z"), -86400d, true},
                {Instant.parse("1969-12-31T00:00:00.999999999Z"), -86399.000000001, true },
//                {Instant.parse("1969-12-31T23:59:59.999999999Z"), -0.000000001 },    // IEEE-754 double cannot represent this number precisely
                {Instant.parse("1970-01-01T00:00:00Z"), 0d, true},
                {Instant.parse("1970-01-01T00:00:00.000000001Z"), 0.000000001, true},
                {Instant.parse("1970-01-02T00:00:00Z"), 86400d, true},
                {Instant.parse("1970-01-02T00:00:00.000000001Z"), 86400.000000001, true},
        });
        TEST_DB.put(pair(LocalDate.class, Double.class), new Object[][]{
                {LocalDate.parse("0000-01-01"), -62167252739d, true},  // Proves it always works from "startOfDay", using the zoneId from options
                {LocalDate.parse("1969-12-31"), -118800d, true},
                {LocalDate.parse("1970-01-01"), -32400d, true},
                {LocalDate.parse("1970-01-02"), 54000d, true},    
                {ZonedDateTime.parse("1969-12-31T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), -118800d, true},    // Proves it always works from "startOfDay", using the zoneId from options
                {ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), -118800d, true},    // Proves it always works from "startOfDay", using the zoneId from options
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), -32400d, true},    // Proves it always works from "startOfDay", using the zoneId from options
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400d, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), -32400d, true},
        });
        TEST_DB.put(pair(LocalDateTime.class, Double.class), new Object[][]{
                {ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -62167219200.0, true},    
                {ZonedDateTime.parse("1969-12-31T00:00:00.999999999Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), -86399.000000001, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), -32400d, true},    // Time portion affects the answer unlike LocalDate
                {ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), 0d, true},
                {ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), 0.000000001, true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, Double.class), new Object[][]{    // no reverse due to .toString adding zone name
                {ZonedDateTime.parse("0000-01-01T00:00:00Z"), -62167219200.0 },
                {ZonedDateTime.parse("1969-12-31T23:59:58.9Z"), -1.1  },
                {ZonedDateTime.parse("1969-12-31T23:59:59Z"), -1d },
//                {ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z"), -0.000000001},  // IEEE-754 double resolution not quite good enough to represent, but very close.
                {ZonedDateTime.parse("1970-01-01T00:00:00Z"), 0d},
                {ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z"), 0.000000001},
        });
        TEST_DB.put(pair(OffsetDateTime.class, Double.class), new Object[][]{   // OffsetDateTime .toString() method prevents reverse
                {OffsetDateTime.parse("0000-01-01T00:00:00Z"), -62167219200.0 },
                {OffsetDateTime.parse("1969-12-31T23:59:58.9Z"), -1.1 },
                {OffsetDateTime.parse("1969-12-31T23:59:59.000000001Z"), -0.999999999 },
                {OffsetDateTime.parse("1969-12-31T23:59:59Z"), -1d },
//                {OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z"), -0.000000001},  // IEEE-754 double resolution not quite good enough to represent, but very close.
                {OffsetDateTime.parse("1970-01-01T00:00:00Z"), 0d },
                {OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z"), 0.000000001 },
        });
        TEST_DB.put(pair(Date.class, Double.class), new Object[][]{
                {new Date(Long.MIN_VALUE), (double) Long.MIN_VALUE / 1000d, true},
                {new Date(Integer.MIN_VALUE), (double) Integer.MIN_VALUE / 1000d, true},
                {new Date(0), 0d, true},
                {new Date(now), (double) now / 1000d, true},
                {Date.from(Instant.parse("2024-02-18T06:31:55.987654321Z")), 1708237915.987, true },    // Date only has millisecond resolution
                {Date.from(Instant.parse("2024-02-18T06:31:55.123456789Z")), 1708237915.123, true },      // Date only has millisecond resolution
                {new Date(Integer.MAX_VALUE), (double) Integer.MAX_VALUE / 1000d, true},
                {new Date(Long.MAX_VALUE), (double) Long.MAX_VALUE / 1000d, true},
        });
        TEST_DB.put(pair(java.sql.Date.class, Double.class), new Object[][]{
                {new java.sql.Date(Long.MIN_VALUE), (double) Long.MIN_VALUE / 1000d, true},
                {new java.sql.Date(Integer.MIN_VALUE), (double) Integer.MIN_VALUE / 1000d, true},
                {new java.sql.Date(0), 0d, true},
                {new java.sql.Date(now), (double) now / 1000d, true},
                {new java.sql.Date(Instant.parse("2024-02-18T06:31:55.987654321Z").toEpochMilli()), 1708237915.987, true },    // java.sql.Date only has millisecond resolution
                {new java.sql.Date(Instant.parse("2024-02-18T06:31:55.123456789Z").toEpochMilli()), 1708237915.123, true },      // java.sql.Date only has millisecond resolution
                {new java.sql.Date(Integer.MAX_VALUE), (double) Integer.MAX_VALUE / 1000d, true},
                {new java.sql.Date(Long.MAX_VALUE), (double) Long.MAX_VALUE / 1000d, true},
        });
        TEST_DB.put(pair(Timestamp.class, Double.class), new Object[][]{
                {new Timestamp(0), 0d, true},
                { Timestamp.from(Instant.parse("1969-12-31T00:00:00Z")), -86400d, true},
                { Timestamp.from(Instant.parse("1969-12-31T00:00:00.000000001Z")), -86399.999999999},    // IEEE-754 resolution issue (almost symmetrical)
                { Timestamp.from(Instant.parse("1969-12-31T00:00:01Z")), -86399d, true },    
                { Timestamp.from(Instant.parse("1969-12-31T23:59:58.9Z")), -1.1, true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59Z")), -1.0, true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00Z")), 0d, true},
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), 0.000000001, true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.9Z")), 0.9, true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.999999999Z")), 0.999999999, true },
        });
        TEST_DB.put(pair(Calendar.class, Double.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(-1);
                    return cal;
                }, -1d},
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(0);
                    return cal;
                }, 0d},
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
                    cal.setTimeInMillis(1);
                    return cal;
                }, 1d},
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    return cal;
                }, 1707705480000d},
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.setTimeInMillis(now);   // Calendar maintains time to millisecond resolution
                    return cal;
                }, (double)now}
        });
        TEST_DB.put(pair(AtomicBoolean.class, Double.class), new Object[][]{
                {new AtomicBoolean(true), 1d},
                {new AtomicBoolean(false), 0d},
        });
        TEST_DB.put(pair(AtomicInteger.class, Double.class), new Object[][]{
                {new AtomicInteger(-1), -1d},
                {new AtomicInteger(0), 0d},
                {new AtomicInteger(1), 1d},
                {new AtomicInteger(-2147483648), (double) Integer.MIN_VALUE},
                {new AtomicInteger(2147483647), (double) Integer.MAX_VALUE},
        });
        TEST_DB.put(pair(AtomicLong.class, Double.class), new Object[][]{
                {new AtomicLong(-1), -1d},
                {new AtomicLong(0), 0d},
                {new AtomicLong(1), 1d},
                {new AtomicLong(-9007199254740991L), -9007199254740991d},
                {new AtomicLong(9007199254740991L), 9007199254740991d},
        });
        TEST_DB.put(pair(BigInteger.class, Double.class), new Object[][]{
                {new BigInteger("-1"), -1d, true},
                {new BigInteger("0"), 0d, true},
                {new BigInteger("1"), 1d, true},
                {new BigInteger("-9007199254740991"), -9007199254740991d, true},
                {new BigInteger("9007199254740991"), 9007199254740991d, true},
        });
        TEST_DB.put(pair(BigDecimal.class, Double.class), new Object[][]{
                {new BigDecimal("-1"), -1d},
                {new BigDecimal("-1.1"), -1.1},
                {new BigDecimal("-1.9"), -1.9},
                {new BigDecimal("0"), 0d},
                {new BigDecimal("1"), 1d},
                {new BigDecimal("1.1"), 1.1},
                {new BigDecimal("1.9"), 1.9},
                {new BigDecimal("-9007199254740991"), -9007199254740991d},
                {new BigDecimal("9007199254740991"), 9007199254740991d},
        });
        TEST_DB.put(pair(Number.class, Double.class), new Object[][]{
                {2.5f, 2.5}
        });
        TEST_DB.put(pair(Map.class, Double.class), new Object[][]{
                {mapOf("_v", "-1"), -1d},
                {mapOf("_v", -1), -1d},
                {mapOf("value", "-1"), -1d},
                {mapOf("value", -1L), -1d},

                {mapOf("_v", "0"), 0d},
                {mapOf("_v", 0), 0d},

                {mapOf("_v", "1"), 1d},
                {mapOf("_v", 1), 1d},

                {mapOf("_v", "-9007199254740991"), -9007199254740991d},
                {mapOf("_v", -9007199254740991L), -9007199254740991d},

                {mapOf("_v", "9007199254740991"), 9007199254740991d},
                {mapOf("_v", 9007199254740991L), 9007199254740991d},

                {mapOf("_v", mapOf("_v", -9007199254740991L)), -9007199254740991d},    // Prove use of recursive call to .convert()
        });
        TEST_DB.put(pair(String.class, Double.class), new Object[][]{
                {"-1", -1d},
                {"-1.1", -1.1},
                {"-1.9", -1.9},
                {"0", 0d},
                {"1", 1d},
                {"1.1", 1.1},
                {"1.9", 1.9},
                {"-2147483648", -2147483648d},
                {"2147483647", 2147483647d},
                {"", 0d},
                {" ", 0d},
                {"crapola", new IllegalArgumentException("Value 'crapola' not parseable as a double")},
                {"54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a double")},
                {"54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a double")},
                {"crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a double")},
                {"crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a double")},
        });
        TEST_DB.put(pair(Year.class, Double.class), new Object[][]{
                {Year.of(2024), 2024d}
        });

        /////////////////////////////////////////////////////////////
        // Boolean/boolean
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, boolean.class), new Object[][]{
                {null, false},
        });
        TEST_DB.put(pair(Void.class, Boolean.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Boolean.class), new Object[][]{
                {(byte) -2, true},
                {(byte) -1, true},
                {(byte) 0, false},
                {(byte) 1, true},
                {(byte) 2, true},
        });
        TEST_DB.put(pair(Short.class, Boolean.class), new Object[][]{
                {(short) -2, true},
                {(short) -1, true},
                {(short) 0, false},
                {(short) 1, true},
                {(short) 2, true},
        });
        TEST_DB.put(pair(Integer.class, Boolean.class), new Object[][]{
                {-2, true},
                {-1, true},
                {0, false},
                {1, true},
                {2, true},
        });
        TEST_DB.put(pair(Long.class, Boolean.class), new Object[][]{
                {-2L, true},
                {-1L, true},
                {0L, false},
                {1L, true},
                {2L, true},
        });
        TEST_DB.put(pair(Float.class, Boolean.class), new Object[][]{
                {-2f, true},
                {-1.5f, true},
                {-1f, true},
                {0f, false},
                {1f, true},
                {1.5f, true},
                {2f, true},
        });
        TEST_DB.put(pair(Double.class, Boolean.class), new Object[][]{
                {-2d, true},
                {-1.5, true},
                {-1d, true},
                {0d, false},
                {1d, true},
                {1.5, true},
                {2d, true},
        });
        TEST_DB.put(pair(Boolean.class, Boolean.class), new Object[][]{
                {true, true},
                {false, false},
        });
        TEST_DB.put(pair(Character.class, Boolean.class), new Object[][]{
                {(char) 1, true},
                {'1', true},
                {'2', false},
                {'a', false},
                {'z', false},
                {(char) 0, false},
                {'0', false},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Boolean.class), new Object[][]{
                {new AtomicBoolean(true), true},
                {new AtomicBoolean(false), false},
        });
        TEST_DB.put(pair(AtomicInteger.class, Boolean.class), new Object[][]{
                {new AtomicInteger(-2), true},
                {new AtomicInteger(-1), true},
                {new AtomicInteger(0), false},
                {new AtomicInteger(1), true},
                {new AtomicInteger(2), true},
        });
        TEST_DB.put(pair(AtomicLong.class, Boolean.class), new Object[][]{
                {new AtomicLong(-2), true},
                {new AtomicLong(-1), true},
                {new AtomicLong(0), false},
                {new AtomicLong(1), true},
                {new AtomicLong(2), true},
        });
        TEST_DB.put(pair(BigInteger.class, Boolean.class), new Object[][]{
                {BigInteger.valueOf(-2), true},
                {BigInteger.valueOf(-1), true},
                {BigInteger.ZERO, false},
                {BigInteger.valueOf(1), true},
                {BigInteger.valueOf(2), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Boolean.class), new Object[][]{
                {BigDecimal.valueOf(-2L), true},
                {BigDecimal.valueOf(-1L), true},
                {BigDecimal.valueOf(0L), false},
                {BigDecimal.valueOf(1L), true},
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
                {"false", false},
                {"FaLse", false},
                {"FALSE", false},
                {"F", false},
                {"f", false},
                {"1", true},
                {"true", true},
                {"TrUe", true},
                {"TRUE", true},
                {"T", true},
                {"t", true},
        });

        /////////////////////////////////////////////////////////////
        // Character/char
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, char.class), new Object[][]{
                {null, (char) 0},
        });
        TEST_DB.put(pair(Void.class, Character.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(Byte.class, Character.class), new Object[][]{
                {(byte) -1, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {(byte) 0, (char) 0, true},
                {(byte) 1, (char) 1, true},
                {Byte.MAX_VALUE, (char) Byte.MAX_VALUE, true},
        });
        TEST_DB.put(pair(Short.class, Character.class), new Object[][]{
                {(short) -1, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {(short) 0, (char) 0, true},
                {(short) 1, (char) 1, true},
                {Short.MAX_VALUE, (char) Short.MAX_VALUE, true},
        });
        TEST_DB.put(pair(Integer.class, Character.class), new Object[][]{
                {-1, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0, (char) 0, true},
                {1, (char) 1, true},
                {65535, (char) 65535, true},
                {65536, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Long.class, Character.class), new Object[][]{
                {-1L, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0L, (char) 0L, true},
                {1L, (char) 1L, true},
                {65535L, (char) 65535L, true},
                {65536L, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Float.class, Character.class), new Object[][]{
                {-1f, new IllegalArgumentException("Value '-1' out of range to be converted to character"),},
                {0f, (char) 0, true},
                {1f, (char) 1, true},
                {65535f, (char) 65535f, true},
                {65536f, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Double.class, Character.class), new Object[][]{
                {-1d, new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {0d, (char) 0, true},
                {1d, (char) 1, true},
                {65535d, (char) 65535d, true},
                {65536d, new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Boolean.class, Character.class), new Object[][]{
                {false, (char) 0, true},
                {true, (char) 1, true},
        });
        TEST_DB.put(pair(Character.class, Character.class), new Object[][]{
                {(char) 0, (char) 0, true},
                {(char) 1, (char) 1, true},
                {(char) 65535, (char) 65535, true},
        });
        TEST_DB.put(pair(AtomicBoolean.class, Character.class), new Object[][]{
                {new AtomicBoolean(true), (char) 1},  // can't run reverse because equals() on AtomicBoolean is not implemented, it needs .get() called first.
                {new AtomicBoolean(false), (char) 0},
        });
        TEST_DB.put(pair(AtomicInteger.class, Character.class), new Object[][]{
                {new AtomicInteger(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {new AtomicInteger(0), (char) 0},
                {new AtomicInteger(1), (char) 1},
                {new AtomicInteger(65535), (char) 65535},
                {new AtomicInteger(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(AtomicLong.class, Character.class), new Object[][]{
                {new AtomicLong(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {new AtomicLong(0), (char) 0},
                {new AtomicLong(1), (char) 1},
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
                {BigDecimal.valueOf(0), (char) 0, true},
                {BigDecimal.valueOf(1), (char) 1, true},
                {BigDecimal.valueOf(65535), (char) 65535, true},
                {BigDecimal.valueOf(65536), new IllegalArgumentException("Value '65536' out of range to be converted to character")},
        });
        TEST_DB.put(pair(Number.class, Character.class), new Object[][]{
                {BigDecimal.valueOf(-1), new IllegalArgumentException("Value '-1' out of range to be converted to character")},
                {BigDecimal.valueOf(0), (char) 0},
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
                {"0", '0', true},
                {"A", 'A', true},
                {"{", '{', true},
                {"\uD83C", '\uD83C', true},
                {"\uFFFF", '\uFFFF', true},
        });

        /////////////////////////////////////////////////////////////
        // BigInteger
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, BigInteger.class), new Object[][]{
                { null, null },
        });
        TEST_DB.put(pair(Byte.class, BigInteger.class), new Object[][]{
                { (byte) -1, BigInteger.valueOf(-1), true },
                { (byte) 0, BigInteger.ZERO, true },
                { Byte.MIN_VALUE, BigInteger.valueOf(Byte.MIN_VALUE), true },
                { Byte.MAX_VALUE, BigInteger.valueOf(Byte.MAX_VALUE), true },
        });
        TEST_DB.put(pair(Short.class, BigInteger.class), new Object[][]{
                { (short) -1, BigInteger.valueOf(-1), true },
                { (short) 0, BigInteger.ZERO, true },
                { Short.MIN_VALUE, BigInteger.valueOf(Short.MIN_VALUE), true },
                { Short.MAX_VALUE, BigInteger.valueOf(Short.MAX_VALUE), true },
        });
        TEST_DB.put(pair(Integer.class, BigInteger.class), new Object[][]{
                { -1, BigInteger.valueOf(-1), true },
                { 0, BigInteger.ZERO, true },
                { Integer.MIN_VALUE, BigInteger.valueOf(Integer.MIN_VALUE), true },
                { Integer.MAX_VALUE, BigInteger.valueOf(Integer.MAX_VALUE), true },
        });
        TEST_DB.put(pair(Long.class, BigInteger.class), new Object[][]{
                { -1L, BigInteger.valueOf(-1), true },
                { 0L, BigInteger.ZERO, true },
                { Long.MIN_VALUE, BigInteger.valueOf(Long.MIN_VALUE), true },
                { Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE), true },
        });
        TEST_DB.put(pair(Float.class, BigInteger.class), new Object[][]{
                { -1f, BigInteger.valueOf(-1), true },
                { 0f, BigInteger.ZERO, true },
                { 1.0e6f, new BigInteger("1000000"), true },
                { -16777216f, BigInteger.valueOf(-16777216), true },
                { 16777216f, BigInteger.valueOf(16777216), true },
        });
        TEST_DB.put(pair(Double.class, BigInteger.class), new Object[][]{
                { -1d, BigInteger.valueOf(-1), true },
                { 0d, BigInteger.ZERO, true },
                { 1.0e9d, new BigInteger("1000000000"), true },
                { -9007199254740991d, BigInteger.valueOf(-9007199254740991L), true },
                { 9007199254740991d, BigInteger.valueOf(9007199254740991L), true },
        });
        TEST_DB.put(pair(Boolean.class, BigInteger.class), new Object[][]{
                { false, BigInteger.ZERO, true },
                { true, BigInteger.valueOf(1), true },
        });
        TEST_DB.put(pair(Character.class, BigInteger.class), new Object[][]{
                { (char) 0, BigInteger.ZERO, true },
                { (char) 1, BigInteger.valueOf(1), true },
                { (char) 65535, BigInteger.valueOf(65535), true },
        });
        TEST_DB.put(pair(BigInteger.class, BigInteger.class), new Object[][]{
                { new BigInteger("16"), BigInteger.valueOf(16), true },
        });
        TEST_DB.put(pair(BigDecimal.class, BigInteger.class), new Object[][]{
                { BigDecimal.valueOf(0), BigInteger.ZERO, true },
                { BigDecimal.valueOf(-1), BigInteger.valueOf(-1), true },
                { BigDecimal.valueOf(-1.1), BigInteger.valueOf(-1) },
                { BigDecimal.valueOf(-1.9), BigInteger.valueOf(-1) },
                { BigDecimal.valueOf(1.9), BigInteger.valueOf(1) },
                { BigDecimal.valueOf(1.1), BigInteger.valueOf(1) },
                { BigDecimal.valueOf(1.0e6d), new BigInteger("1000000") },
                { BigDecimal.valueOf(-16777216), BigInteger.valueOf(-16777216), true },
        });
        TEST_DB.put(pair(AtomicBoolean.class, BigInteger.class), new Object[][]{
                { new AtomicBoolean(false), BigInteger.ZERO },
                { new AtomicBoolean(true), BigInteger.valueOf(1) },
        });
        TEST_DB.put(pair(AtomicInteger.class, BigInteger.class), new Object[][]{
                { new AtomicInteger(-1), BigInteger.valueOf(-1) },
                { new AtomicInteger(0), BigInteger.ZERO },
                { new AtomicInteger(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MIN_VALUE) },
                { new AtomicInteger(Integer.MAX_VALUE), BigInteger.valueOf(Integer.MAX_VALUE) },
        });
        TEST_DB.put(pair(AtomicLong.class, BigInteger.class), new Object[][]{
                { new AtomicLong(-1), BigInteger.valueOf(-1) },
                { new AtomicLong(0), BigInteger.ZERO },
                { new AtomicLong(Long.MIN_VALUE), BigInteger.valueOf(Long.MIN_VALUE) },
                { new AtomicLong(Long.MAX_VALUE), BigInteger.valueOf(Long.MAX_VALUE) },
        });
        TEST_DB.put(pair(Date.class, BigInteger.class), new Object[][]{
                { Date.from(Instant.parse("0000-01-01T00:00:00Z")), new BigInteger("-62167219200000000000"), true },
                { Date.from(Instant.parse("0001-02-18T19:58:01Z")), new BigInteger("-62131377719000000000"), true },
                { Date.from(Instant.parse("1969-12-31T23:59:59Z")), BigInteger.valueOf(-1_000_000_000), true },
                { Date.from(Instant.parse("1969-12-31T23:59:59.1Z")), BigInteger.valueOf(-900000000), true },
                { Date.from(Instant.parse("1969-12-31T23:59:59.9Z")), BigInteger.valueOf(-100000000), true },
                { Date.from(Instant.parse("1970-01-01T00:00:00Z")), BigInteger.valueOf(0), true },
                { Date.from(Instant.parse("1970-01-01T00:00:00.1Z")), BigInteger.valueOf(100000000), true },
                { Date.from(Instant.parse("1970-01-01T00:00:00.9Z")), BigInteger.valueOf(900000000), true },
                { Date.from(Instant.parse("1970-01-01T00:00:01Z")), BigInteger.valueOf(1000000000), true },
                { Date.from(Instant.parse("9999-02-18T19:58:01Z")), new BigInteger("253374983881000000000"), true },
        });
        TEST_DB.put(pair(java.sql.Date.class, BigInteger.class), new Object[][]{
                { new java.sql.Date(Instant.parse("0000-01-01T00:00:00Z").toEpochMilli()), new BigInteger("-62167219200000000000"), true },
                { new java.sql.Date(Instant.parse("0001-02-18T19:58:01Z").toEpochMilli()), new BigInteger("-62131377719000000000"), true },
                { new java.sql.Date(Instant.parse("1969-12-31T23:59:59Z").toEpochMilli()), BigInteger.valueOf(-1_000_000_000), true },
                { new java.sql.Date(Instant.parse("1969-12-31T23:59:59.1Z").toEpochMilli()), BigInteger.valueOf(-900000000), true },
                { new java.sql.Date(Instant.parse("1969-12-31T23:59:59.9Z").toEpochMilli()), BigInteger.valueOf(-100000000), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:00Z").toEpochMilli()), BigInteger.valueOf(0), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:00.1Z").toEpochMilli()), BigInteger.valueOf(100000000), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:00.9Z").toEpochMilli()), BigInteger.valueOf(900000000), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:01Z").toEpochMilli()), BigInteger.valueOf(1000000000), true },
                { new java.sql.Date(Instant.parse("9999-02-18T19:58:01Z").toEpochMilli()), new BigInteger("253374983881000000000"), true },
        });
        TEST_DB.put(pair(Timestamp.class, BigInteger.class), new Object[][]{
                { Timestamp.from(Instant.parse("0000-01-01T00:00:00.000000000Z")), new BigInteger("-62167219200000000000"), true },
                { Timestamp.from(Instant.parse("0001-02-18T19:58:01.000000000Z")), new BigInteger("-62131377719000000000"), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.000000000Z")), BigInteger.valueOf(-1000000000), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.000000001Z")), BigInteger.valueOf(-999999999), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.100000000Z")), BigInteger.valueOf(-900000000), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.900000000Z")), BigInteger.valueOf(-100000000), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z")), BigInteger.valueOf(-1), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000000Z")), BigInteger.valueOf(0), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), BigInteger.valueOf(1), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.100000000Z")), BigInteger.valueOf(100000000), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.900000000Z")), BigInteger.valueOf(900000000), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.999999999Z")), BigInteger.valueOf(999999999), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:01.000000000Z")), BigInteger.valueOf(1000000000), true },
                { Timestamp.from(Instant.parse("9999-02-18T19:58:01.000000000Z")), new BigInteger("253374983881000000000"), true },
        });
        TEST_DB.put(pair(Duration.class, BigInteger.class), new Object[][] {
                { Duration.ofNanos(-1000000), BigInteger.valueOf(-1000000), true},
                { Duration.ofNanos(-1000), BigInteger.valueOf(-1000), true},
                { Duration.ofNanos(-1), BigInteger.valueOf(-1), true},
                { Duration.ofNanos(0), BigInteger.valueOf(0), true},
                { Duration.ofNanos(1), BigInteger.valueOf(1), true},
                { Duration.ofNanos(1000), BigInteger.valueOf(1000), true},
                { Duration.ofNanos(1000000), BigInteger.valueOf(1000000), true},
                { Duration.ofNanos(Integer.MAX_VALUE), BigInteger.valueOf(Integer.MAX_VALUE), true},
                { Duration.ofNanos(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MIN_VALUE), true},
                { Duration.ofNanos(Long.MAX_VALUE), BigInteger.valueOf(Long.MAX_VALUE), true},
                { Duration.ofNanos(Long.MIN_VALUE), BigInteger.valueOf(Long.MIN_VALUE), true},
        });
        TEST_DB.put(pair(Instant.class, BigInteger.class), new Object[][]{
                { Instant.parse("0000-01-01T00:00:00.000000000Z"), new BigInteger("-62167219200000000000"), true },
                { Instant.parse("0001-02-18T19:58:01.000000000Z"), new BigInteger("-62131377719000000000"), true },
                { Instant.parse("1969-12-31T23:59:59.000000000Z"), BigInteger.valueOf(-1000000000), true },
                { Instant.parse("1969-12-31T23:59:59.000000001Z"), BigInteger.valueOf(-999999999), true },
                { Instant.parse("1969-12-31T23:59:59.100000000Z"), BigInteger.valueOf(-900000000), true },
                { Instant.parse("1969-12-31T23:59:59.900000000Z"), BigInteger.valueOf(-100000000), true },
                { Instant.parse("1969-12-31T23:59:59.999999999Z"), BigInteger.valueOf(-1), true },
                { Instant.parse("1970-01-01T00:00:00.000000000Z"), BigInteger.valueOf(0), true },
                { Instant.parse("1970-01-01T00:00:00.000000001Z"), BigInteger.valueOf(1), true },
                { Instant.parse("1970-01-01T00:00:00.100000000Z"), BigInteger.valueOf(100000000), true },
                { Instant.parse("1970-01-01T00:00:00.900000000Z"), BigInteger.valueOf(900000000), true },
                { Instant.parse("1970-01-01T00:00:00.999999999Z"), BigInteger.valueOf(999999999), true },
                { Instant.parse("1970-01-01T00:00:01.000000000Z"), BigInteger.valueOf(1000000000), true },
                { Instant.parse("9999-02-18T19:58:01.000000000Z"), new BigInteger("253374983881000000000"), true },
        });
        TEST_DB.put(pair(LocalDate.class, BigInteger.class), new Object[][]{
                {(Supplier<LocalDate>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDate();
                }, BigInteger.valueOf(1707663600000L)},                    // Epoch millis in Tokyo timezone (at start of day - no time)
        });
        TEST_DB.put(pair(LocalDateTime.class, BigInteger.class), new Object[][]{
                {(Supplier<LocalDateTime>) () -> {
                    ZonedDateTime zdt = ZonedDateTime.parse("2024-02-12T11:38:00+01:00");
                    zdt = zdt.withZoneSameInstant(TOKYO_Z);
                    return zdt.toLocalDateTime();
                }, BigInteger.valueOf(1707734280000L)},                    // Epoch millis in Tokyo timezone
        });
        TEST_DB.put(pair(ZonedDateTime.class, BigInteger.class), new Object[][]{
                {ZonedDateTime.parse("2024-02-12T11:38:00+01:00"), BigInteger.valueOf(1707734280000L)},
        });
        TEST_DB.put(pair(UUID.class, BigInteger.class), new Object[][]{
                { new UUID(0L, 0L), BigInteger.ZERO, true },
                { new UUID(1L, 1L), new BigInteger("18446744073709551617"), true },
                { new UUID(Long.MAX_VALUE, Long.MAX_VALUE), new BigInteger("170141183460469231722463931679029329919"), true },
                { UUID.fromString("00000000-0000-0000-0000-000000000000"), BigInteger.ZERO, true },
                { UUID.fromString("00000000-0000-0000-0000-000000000001"), BigInteger.valueOf(1), true },
                { UUID.fromString("00000000-0000-0001-0000-000000000001"), new BigInteger("18446744073709551617"), true },
                { UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), new BigInteger("340282366920938463463374607431768211455"), true },
                { UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffffe"), new BigInteger("340282366920938463463374607431768211454"), true },
                { UUID.fromString("f0000000-0000-0000-0000-000000000000"), new BigInteger("319014718988379809496913694467282698240"), true },
                { UUID.fromString("f0000000-0000-0000-0000-000000000001"), new BigInteger("319014718988379809496913694467282698241"), true },
                { UUID.fromString("7fffffff-ffff-ffff-ffff-fffffffffffe"), new BigInteger("170141183460469231731687303715884105726"), true },
                { UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), new BigInteger("170141183460469231731687303715884105727"), true },
                { UUID.fromString("80000000-0000-0000-0000-000000000000"), new BigInteger("170141183460469231731687303715884105728"), true },
        });
        TEST_DB.put(pair(Calendar.class, BigInteger.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 12, 11, 38, 0);
                    return cal;
                }, BigInteger.valueOf(1707705480000L)}
        });
        TEST_DB.put(pair(Number.class, BigInteger.class), new Object[][]{
                {0, BigInteger.ZERO},
        });
        TEST_DB.put(pair(Map.class, BigInteger.class), new Object[][]{
                {mapOf("_v", 0), BigInteger.ZERO},
        });
        TEST_DB.put(pair(String.class, BigInteger.class), new Object[][]{
                {"0", BigInteger.ZERO},
                {"0.0", BigInteger.ZERO},
        });
        TEST_DB.put(pair(OffsetDateTime.class, BigInteger.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-12T11:38:00+01:00"), BigInteger.valueOf(1707734280000L)},
        });
        TEST_DB.put(pair(Year.class, BigInteger.class), new Object[][]{
                {Year.of(2024), BigInteger.valueOf(2024)},
        });

        /////////////////////////////////////////////////////////////
        // BigDecimal
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, BigDecimal.class), new Object[][]{
                { null, null }
        });
        TEST_DB.put(pair(String.class, BigDecimal.class), new Object[][]{
                { "3.1415926535897932384626433", new BigDecimal("3.1415926535897932384626433"), true}
        });
        TEST_DB.put(pair(Date.class, BigDecimal.class), new Object[][] {
                { Date.from(Instant.parse("0000-01-01T00:00:00Z")), new BigDecimal("-62167219200"), true },
                { Date.from(Instant.parse("0000-01-01T00:00:00.001Z")), new BigDecimal("-62167219199.999"), true },
                { Date.from(Instant.parse("1969-12-31T23:59:59.999Z")), new BigDecimal("-0.001"), true },
                { Date.from(Instant.parse("1970-01-01T00:00:00Z")), new BigDecimal("0"), true },
                { Date.from(Instant.parse("1970-01-01T00:00:00.001Z")), new BigDecimal("0.001"), true },
        });
        TEST_DB.put(pair(java.sql.Date.class, BigDecimal.class), new Object[][] {
                { new java.sql.Date(Instant.parse("0000-01-01T00:00:00Z").toEpochMilli()), new BigDecimal("-62167219200"), true },
                { new java.sql.Date(Instant.parse("0000-01-01T00:00:00.001Z").toEpochMilli()), new BigDecimal("-62167219199.999"), true },
                { new java.sql.Date(Instant.parse("1969-12-31T23:59:59.999Z").toEpochMilli()), new BigDecimal("-0.001"), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:00Z").toEpochMilli()), new BigDecimal("0"), true },
                { new java.sql.Date(Instant.parse("1970-01-01T00:00:00.001Z").toEpochMilli()), new BigDecimal("0.001"), true },
        });
        TEST_DB.put(pair(Timestamp.class, BigDecimal.class), new Object[][] {
                { Timestamp.from(Instant.parse("0000-01-01T00:00:00Z")), new BigDecimal("-62167219200"), true },
                { Timestamp.from(Instant.parse("0000-01-01T00:00:00.000000001Z")), new BigDecimal("-62167219199.999999999"), true },
                { Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z")), new BigDecimal("-0.000000001"), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00Z")), new BigDecimal("0"), true },
                { Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), new BigDecimal("0.000000001"), true },
        });
        TEST_DB.put(pair(LocalDate.class, BigDecimal.class), new Object[][]{
                { LocalDate.parse("0000-01-01"), new BigDecimal("-62167252739"), true},  // Proves it always works from "startOfDay", using the zoneId from options
                { LocalDate.parse("1969-12-31"), new BigDecimal("-118800"), true},
                { LocalDate.parse("1970-01-01"), new BigDecimal("-32400"), true},
                { LocalDate.parse("1970-01-02"), new BigDecimal("54000"), true},
                { ZonedDateTime.parse("1969-12-31T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), new BigDecimal("-118800"), true},    // Proves it always works from "startOfDay", using the zoneId from options
                { ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), new BigDecimal("-118800"), true},    // Proves it always works from "startOfDay", using the zoneId from options
                { ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDate(), new BigDecimal("-32400"), true},    // Proves it always works from "startOfDay", using the zoneId from options
                { ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), new BigDecimal("-32400"), true},
                { ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z).toLocalDate(), new BigDecimal("-32400"), true},
        });
        TEST_DB.put(pair(LocalDateTime.class, BigDecimal.class), new Object[][]{
                { ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), new BigDecimal("-62167219200.0"), true},
                { ZonedDateTime.parse("0000-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), new BigDecimal("-62167219199.999999999"), true},
                { ZonedDateTime.parse("1969-12-31T00:00:00.999999999Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), new BigDecimal("-86399.000000001"), true},
                { ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime(), new BigDecimal("-32400"), true},    // Time portion affects the answer unlike LocalDate
                { ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), BigDecimal.ZERO, true},
                { ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z).toLocalDateTime(), new BigDecimal("0.000000001"), true},
        });
        TEST_DB.put(pair(ZonedDateTime.class, BigDecimal.class), new Object[][] {   // no reverse due to .toString adding zone offset
                { ZonedDateTime.parse("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200") },
                { ZonedDateTime.parse("0000-01-01T00:00:00.000000001Z"), new BigDecimal("-62167219199.999999999") },
                { ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z"), new BigDecimal("-0.000000001") },
                { ZonedDateTime.parse("1970-01-01T00:00:00Z"), new BigDecimal("0") },
                { ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z"), new BigDecimal("0.000000001") },
        });
        TEST_DB.put(pair(OffsetDateTime.class, BigDecimal.class), new Object[][] {   // no reverse due to .toString adding zone offset
                { OffsetDateTime.parse("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200")  },
                { OffsetDateTime.parse("0000-01-01T00:00:00.000000001Z"), new BigDecimal("-62167219199.999999999") },
                { OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z"), new BigDecimal("-0.000000001") },
                { OffsetDateTime.parse("1970-01-01T00:00:00Z"), new BigDecimal("0") },
                { OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z"), new BigDecimal("0.000000001") },
        });
        TEST_DB.put(pair(Duration.class, BigDecimal.class), new Object[][] {
                { Duration.ofSeconds(-1, -1), new BigDecimal("-1.000000001"), true },
                { Duration.ofSeconds(-1), new BigDecimal("-1"), true },
                { Duration.ofSeconds(0), new BigDecimal("0"), true },
                { Duration.ofSeconds(1), new BigDecimal("1"), true },
                { Duration.ofNanos(1), new BigDecimal("0.000000001"), true },
                { Duration.ofNanos(1_000_000_000), new BigDecimal("1"), true },
                { Duration.ofNanos(2_000_000_001), new BigDecimal("2.000000001"), true },
                { Duration.ofSeconds(10, 9), new BigDecimal("10.000000009"), true },
                { Duration.ofDays(1), new BigDecimal("86400"), true},
        });
        TEST_DB.put(pair(Instant.class, BigDecimal.class), new Object[][]{      // JDK 1.8 cannot handle the format +01:00 in Instant.parse().  JDK11+ handles it fine.
                { Instant.parse("0000-01-01T00:00:00Z"), new BigDecimal("-62167219200.0"), true},
                { Instant.parse("0000-01-01T00:00:00.000000001Z"), new BigDecimal("-62167219199.999999999"), true},
                { Instant.parse("1969-12-31T00:00:00Z"), new BigDecimal("-86400"), true},
                { Instant.parse("1969-12-31T00:00:00.999999999Z"), new BigDecimal("-86399.000000001"), true },
                { Instant.parse("1969-12-31T23:59:59.999999999Z"), new BigDecimal("-0.000000001"), true },
                { Instant.parse("1970-01-01T00:00:00Z"), BigDecimal.ZERO, true},
                { Instant.parse("1970-01-01T00:00:00.000000001Z"), new BigDecimal("0.000000001"), true},
                { Instant.parse("1970-01-02T00:00:00Z"), new BigDecimal("86400"), true},
                { Instant.parse("1970-01-02T00:00:00.000000001Z"), new BigDecimal("86400.000000001"), true},
        });

        /////////////////////////////////////////////////////////////
        // Instant
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Instant.class), new Object[][]{
                { null, null }
        });
        TEST_DB.put(pair(String.class, Instant.class), new Object[][]{
                { "", null},
                { " ", null},
                { "1980-01-01T00:00:00Z", Instant.parse("1980-01-01T00:00:00Z"), true},
                { "2024-12-31T23:59:59.999999999Z", Instant.parse("2024-12-31T23:59:59.999999999Z")},
        });
        TEST_DB.put(pair(Double.class, Instant.class), new Object[][]{
                { -62167219200d, Instant.parse("0000-01-01T00:00:00Z"), true},
                { -0.000000001, Instant.parse("1969-12-31T23:59:59.999999999Z")}, // IEEE-754 precision not good enough for reverse
                { 0d, Instant.parse("1970-01-01T00:00:00Z"), true},
                { 0.000000001, Instant.parse("1970-01-01T00:00:00.000000001Z"), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Instant.class), new Object[][]{
                { new BigDecimal("-62167219200"), Instant.parse("0000-01-01T00:00:00Z"), true},
                { new BigDecimal("-62167219199.999999999"), Instant.parse("0000-01-01T00:00:00.000000001Z"), true},
                { new BigDecimal("-0.000000001"), Instant.parse("1969-12-31T23:59:59.999999999Z"), true},
                { BigDecimal.ZERO, Instant.parse("1970-01-01T00:00:00Z"), true},
                { new BigDecimal("0.000000001"), Instant.parse("1970-01-01T00:00:00.000000001Z"), true},
        });

        /////////////////////////////////////////////////////////////
        // Date
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Date.class), new Object[][]{
                { null, null }
        });
        // No identity test for Date, as it is mutable
        TEST_DB.put(pair(BigDecimal.class, Date.class), new Object[][] {
                { new BigDecimal("-62167219200"), Date.from(Instant.parse("0000-01-01T00:00:00Z")), true },
                { new BigDecimal("-62167219199.999"), Date.from(Instant.parse("0000-01-01T00:00:00.001Z")), true },
                { new BigDecimal("-1.001"), Date.from(Instant.parse("1969-12-31T23:59:58.999Z")), true },
                { new BigDecimal("-1"), Date.from(Instant.parse("1969-12-31T23:59:59Z")), true },
                { new BigDecimal("-0.001"), Date.from(Instant.parse("1969-12-31T23:59:59.999Z")), true },
                { new BigDecimal("0"), Date.from(Instant.parse("1970-01-01T00:00:00.000000000Z")), true },
                { new BigDecimal("0.001"), Date.from(Instant.parse("1970-01-01T00:00:00.001Z")), true },
                { new BigDecimal(".999"), Date.from(Instant.parse("1970-01-01T00:00:00.999Z")), true },
                { new BigDecimal("1"), Date.from(Instant.parse("1970-01-01T00:00:01Z")), true },
        });
        
        /////////////////////////////////////////////////////////////
        // java.sql.Date
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, java.sql.Date.class), new Object[][]{
                { null, null }
        });
        // No identity test for Date, as it is mutable
        TEST_DB.put(pair(BigDecimal.class, java.sql.Date.class), new Object[][] {
                { new BigDecimal("-62167219200"), new java.sql.Date(Date.from(Instant.parse("0000-01-01T00:00:00Z")).getTime()), true },
                { new BigDecimal("-62167219199.999"), new java.sql.Date(Date.from(Instant.parse("0000-01-01T00:00:00.001Z")).getTime()), true },
                { new BigDecimal("-1.001"), new java.sql.Date(Date.from(Instant.parse("1969-12-31T23:59:58.999Z")).getTime()), true },
                { new BigDecimal("-1"), new java.sql.Date(Date.from(Instant.parse("1969-12-31T23:59:59Z")).getTime()), true },
                { new BigDecimal("-0.001"), new java.sql.Date(Date.from(Instant.parse("1969-12-31T23:59:59.999Z")).getTime()), true },
                { new BigDecimal("0"), new java.sql.Date(Date.from(Instant.parse("1970-01-01T00:00:00.000000000Z")).getTime()), true },
                { new BigDecimal("0.001"), new java.sql.Date(Date.from(Instant.parse("1970-01-01T00:00:00.001Z")).getTime()), true },
                { new BigDecimal(".999"), new java.sql.Date(Date.from(Instant.parse("1970-01-01T00:00:00.999Z")).getTime()), true },
                { new BigDecimal("1"), new java.sql.Date(Date.from(Instant.parse("1970-01-01T00:00:01Z")).getTime()), true },
        });

        /////////////////////////////////////////////////////////////
        // Duration
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Duration.class), new Object[][]{
                { null, null }
        });
        TEST_DB.put(pair(String.class, Duration.class), new Object[][]{
                {"PT1S", Duration.ofSeconds(1), true},
                {"PT10S", Duration.ofSeconds(10), true},
                {"PT1M40S", Duration.ofSeconds(100), true},
                {"PT16M40S", Duration.ofSeconds(1000), true},
                {"PT2H46M40S", Duration.ofSeconds(10000), true},
        });
        TEST_DB.put(pair(Double.class, Duration.class), new Object[][]{
                {-0.000000001, Duration.ofNanos(-1) },   // IEEE 754 prevents reverse
                {0d, Duration.ofNanos(0), true},
                {0.000000001, Duration.ofNanos(1), true },
                {1d, Duration.ofSeconds(1), true},
                {10d, Duration.ofSeconds(10), true},
                {100d, Duration.ofSeconds(100), true},
                {3.000000006d, Duration.ofSeconds(3, 6) },  // IEEE 754 prevents reverse
        });
        TEST_DB.put(pair(BigDecimal.class, Duration.class), new Object[][]{
                {new BigDecimal("-0.000000001"), Duration.ofNanos(-1), true },
                {BigDecimal.ZERO, Duration.ofNanos(0), true},
                {new BigDecimal("0.000000001"), Duration.ofNanos(1), true },
                {new BigDecimal("100"), Duration.ofSeconds(100), true},
                {new BigDecimal("1"), Duration.ofSeconds(1), true},
                {new BigDecimal("100"), Duration.ofSeconds(100), true},
                {new BigDecimal("100"), Duration.ofSeconds(100), true},
                {new BigDecimal("3.000000006"), Duration.ofSeconds(3, 6), true },
        });

        /////////////////////////////////////////////////////////////
        // OffsetDateTime
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, OffsetDateTime.class), new Object[][]{
                { null, null }
        });
        TEST_DB.put(pair(OffsetDateTime.class, OffsetDateTime.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), true },
        });
        TEST_DB.put(pair(Double.class, OffsetDateTime.class), new Object[][]{
                {-0.000000001, OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()) },  // IEEE-754 resolution prevents perfect symmetry (close)
                {0d, OffsetDateTime.parse("1970-01-01T00:00:00Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()), true },
                {0.000000001, OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()), true },
        });
        TEST_DB.put(pair(BigDecimal.class, OffsetDateTime.class), new Object[][]{
                {new BigDecimal("-0.000000001"), OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()) },  // IEEE-754 resolution prevents perfect symmetry (close)
                {new BigDecimal("0"), OffsetDateTime.parse("1970-01-01T00:00:00Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()), true },
                {new BigDecimal(".000000001"), OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z").withOffsetSameInstant(ZonedDateTime.now(TOKYO_Z).getOffset()), true },
        });

        /////////////////////////////////////////////////////////////
        // MonthDay
        /////////////////////////////////////////////////////////////
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
                {mapOf("month", "6", "day", 30), MonthDay.of(6, 30)},
                {mapOf("month", 6L, "day", "30"), MonthDay.of(6, 30)},
                {mapOf("month", mapOf("_v", 6L), "day", "30"), MonthDay.of(6, 30)},    // recursive on "month"
                {mapOf("month", 6L, "day", mapOf("_v", "30")), MonthDay.of(6, 30)},    // recursive on "day"
        });

        /////////////////////////////////////////////////////////////
        // YearMonth
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, YearMonth.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(YearMonth.class, YearMonth.class), new Object[][]{
                {YearMonth.of(2023, 12), YearMonth.of(2023, 12), true},
                {YearMonth.of(1970, 1), YearMonth.of(1970, 1), true},
                {YearMonth.of(1999, 6), YearMonth.of(1999, 6), true},
        });
        TEST_DB.put(pair(String.class, YearMonth.class), new Object[][]{
                {"2024-01", YearMonth.of(2024, 1)},
                {"2024-1", new IllegalArgumentException("Unable to extract Year-Month from string: 2024-1")},
                {"2024-1-1", YearMonth.of(2024, 1)},
                {"2024-06-01", YearMonth.of(2024, 6)},
                {"2024-12-31", YearMonth.of(2024, 12)},
                {"05:45 2024-12-31", YearMonth.of(2024, 12)},
        });
        TEST_DB.put(pair(Map.class, YearMonth.class), new Object[][]{
                {mapOf("_v", "2024-01"), YearMonth.of(2024, 1)},
                {mapOf("value", "2024-01"), YearMonth.of(2024, 1)},
                {mapOf("year", "2024", "month", 12), YearMonth.of(2024, 12)},
                {mapOf("year", new BigInteger("2024"), "month", "12"), YearMonth.of(2024, 12)},
                {mapOf("year", mapOf("_v", 2024), "month", "12"), YearMonth.of(2024, 12)},    // prove recursion on year
                {mapOf("year", 2024, "month", mapOf("_v", "12")), YearMonth.of(2024, 12)},    // prove recursion on month
                {mapOf("year", 2024, "month", mapOf("_v", mapOf("_v", "12"))), YearMonth.of(2024, 12)},    // prove multiple recursive calls
        });

        /////////////////////////////////////////////////////////////
        // Period
        /////////////////////////////////////////////////////////////
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
                {"PONY", new IllegalArgumentException("Unable to parse 'PONY' as a Period.")},
        });
        TEST_DB.put(pair(Map.class, Period.class), new Object[][]{
                {mapOf("_v", "P0D"), Period.of(0, 0, 0)},
                {mapOf("value", "P1Y1M1D"), Period.of(1, 1, 1)},
                {mapOf("years", "2", "months", 2, "days", 2.0), Period.of(2, 2, 2)},
                {mapOf("years", mapOf("_v", (byte) 2), "months", mapOf("_v", 2.0f), "days", mapOf("_v", new AtomicInteger(2))), Period.of(2, 2, 2)},   // recursion
        });

        /////////////////////////////////////////////////////////////
        // Year
        /////////////////////////////////////////////////////////////
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
                {"PONY", new IllegalArgumentException("Unable to parse 4-digit year from 'PONY'")},
        });
        TEST_DB.put(pair(Map.class, Year.class), new Object[][]{
                {mapOf("_v", "1984"), Year.of(1984)},
                {mapOf("value", 1984L), Year.of(1984)},
                {mapOf("year", 1492), Year.of(1492), true},
                {mapOf("year", mapOf("_v", (short) 2024)), Year.of(2024)}, // recursion
        });
        TEST_DB.put(pair(Number.class, Year.class), new Object[][]{
                {(byte) 101, new IllegalArgumentException("Unsupported conversion, source type [Byte (101)] target type 'Year'")},
                {(short) 2024, Year.of(2024)},
        });

        /////////////////////////////////////////////////////////////
        // ZoneId
        /////////////////////////////////////////////////////////////
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
                {"America/New_York", NY_Z},
                {"Asia/Tokyo", TOKYO_Z},
                {"America/Cincinnati", new IllegalArgumentException("Unknown time-zone ID: 'America/Cincinnati'")},
        });
        TEST_DB.put(pair(Map.class, ZoneId.class), new Object[][]{
                {mapOf("_v", "America/New_York"), NY_Z},
                {mapOf("_v", NY_Z), NY_Z},
                {mapOf("zone", NY_Z), NY_Z},
                {mapOf("_v", "Asia/Tokyo"), TOKYO_Z},
                {mapOf("_v", TOKYO_Z), TOKYO_Z},
                {mapOf("zone", mapOf("_v", TOKYO_Z)), TOKYO_Z},
        });

        /////////////////////////////////////////////////////////////
        // Timestamp
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, Timestamp.class), new Object[][]{
                { null, null },
        });
        // No identity test - Timestamp is mutable
        TEST_DB.put(pair(Double.class, Timestamp.class), new Object[][]{
                { -0.000000001, Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z"))},    // IEEE-754 limit prevents reverse test
                { 0d, Timestamp.from(Instant.parse("1970-01-01T00:00:00Z")), true},
                { 0.000000001, Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), true},
                { (double)now, new Timestamp((long)(now * 1000d)), true},
        });
        TEST_DB.put(pair(BigDecimal.class, Timestamp.class), new Object[][] {
                { new BigDecimal("-62167219200"), Timestamp.from(Instant.parse("0000-01-01T00:00:00Z")), true },
                { new BigDecimal("-62167219199.999999999"), Timestamp.from(Instant.parse("0000-01-01T00:00:00.000000001Z")), true },
                { new BigDecimal("-1.000000001"), Timestamp.from(Instant.parse("1969-12-31T23:59:58.999999999Z")), true },
                { new BigDecimal("-1"), Timestamp.from(Instant.parse("1969-12-31T23:59:59Z")), true },
                { new BigDecimal("-0.00000001"), Timestamp.from(Instant.parse("1969-12-31T23:59:59.99999999Z")), true },
                { new BigDecimal("-0.000000001"), Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z")), true },
                { new BigDecimal("0"), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000000Z")), true },
                { new BigDecimal("0.000000001"), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), true },
                { new BigDecimal(".999999999"), Timestamp.from(Instant.parse("1970-01-01T00:00:00.999999999Z")), true },
                { new BigDecimal("1"), Timestamp.from(Instant.parse("1970-01-01T00:00:01Z")), true },
        });
        TEST_DB.put(pair(Duration.class, Timestamp.class), new Object[][] {
                { Duration.ofSeconds(-62167219200L), Timestamp.from(Instant.parse("0000-01-01T00:00:00Z")), true},
                { Duration.ofSeconds(-62167219200L, 1), Timestamp.from(Instant.parse("0000-01-01T00:00:00.000000001Z")), true},
                { Duration.ofNanos(-1000000001), Timestamp.from(Instant.parse("1969-12-31T23:59:58.999999999Z")), true},
                { Duration.ofNanos(-1000000000), Timestamp.from(Instant.parse("1969-12-31T23:59:59.000000000Z")), true},
                { Duration.ofNanos(-999999999), Timestamp.from(Instant.parse("1969-12-31T23:59:59.000000001Z")), true},
                { Duration.ofNanos(-1), Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z")), true},
                { Duration.ofNanos(0), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000000Z")), true},
                { Duration.ofNanos(1), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")), true},
                { Duration.ofNanos(999999999), Timestamp.from(Instant.parse("1970-01-01T00:00:00.999999999Z")), true},
                { Duration.ofNanos(1000000000), Timestamp.from(Instant.parse("1970-01-01T00:00:01.000000000Z")), true},
                { Duration.ofNanos(1000000001), Timestamp.from(Instant.parse("1970-01-01T00:00:01.000000001Z")), true},
                { Duration.ofNanos(686629800000000001L), Timestamp.from(Instant.parse("1991-10-05T02:30:00.000000001Z")), true },
                { Duration.ofNanos(1199145600000000001L), Timestamp.from(Instant.parse("2008-01-01T00:00:00.000000001Z")), true },
                { Duration.ofNanos(1708255140987654321L), Timestamp.from(Instant.parse("2024-02-18T11:19:00.987654321Z")), true },
                { Duration.ofNanos(2682374400000000001L), Timestamp.from(Instant.parse("2055-01-01T00:00:00.000000001Z")), true },
        });

        // No symmetry checks - because an OffsetDateTime of "2024-02-18T06:31:55.987654321+00:00" and "2024-02-18T15:31:55.987654321+09:00" are equivalent but not equals. They both describe the same Instant.
        TEST_DB.put(pair(OffsetDateTime.class, Timestamp.class), new Object[][]{
                {OffsetDateTime.parse("1969-12-31T23:59:59.999999999Z"), Timestamp.from(Instant.parse("1969-12-31T23:59:59.999999999Z")) },
                {OffsetDateTime.parse("1970-01-01T00:00:00.000000000Z"), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000000Z")) },
                {OffsetDateTime.parse("1970-01-01T00:00:00.000000001Z"), Timestamp.from(Instant.parse("1970-01-01T00:00:00.000000001Z")) },
                {OffsetDateTime.parse("2024-02-18T06:31:55.987654321Z"), Timestamp.from(Instant.parse("2024-02-18T06:31:55.987654321Z")) },
        });

        /////////////////////////////////////////////////////////////
        // LocalDate
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, LocalDate.class), new Object[][] {
                { null, null }
        });
        TEST_DB.put(pair(LocalDate.class, LocalDate.class), new Object[][] {
                { LocalDate.parse("1970-01-01"), LocalDate.parse("1970-01-01"), true }
        });
        TEST_DB.put(pair(Double.class, LocalDate.class), new Object[][] {   // options timezone is factored in (86,400 seconds per day)
                { -118800d, LocalDate.parse("1969-12-31"), true },
                { -32400d, LocalDate.parse("1970-01-01"), true },
                { 0d, LocalDate.parse("1970-01-01")  },         // Showing that there is a wide range of numbers that will convert to this date
                { 53999.999, LocalDate.parse("1970-01-01")  }, // Showing that there is a wide range of numbers that will convert to this date
                { 54000d, LocalDate.parse("1970-01-02"), true },
        });
        TEST_DB.put(pair(BigDecimal.class, LocalDate.class), new Object[][] {   // options timezone is factored in (86,400 seconds per day)
                { new BigDecimal("-62167219200"), LocalDate.parse("0000-01-01") },
                { new BigDecimal("-62167219200"), ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate() },
                { new BigDecimal("-118800"), LocalDate.parse("1969-12-31"), true },
                // These 4 are all in the same date range
                { new BigDecimal("-32400"), LocalDate.parse("1970-01-01"), true },
                { BigDecimal.ZERO, ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z).toLocalDate()  },
                { new BigDecimal("53999.999"), LocalDate.parse("1970-01-01")  },
                { new BigDecimal("54000"), LocalDate.parse("1970-01-02"), true },
        });

        /////////////////////////////////////////////////////////////
        // LocalDateTime
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, LocalDateTime.class), new Object[][] {
                { null, null }
        });
        TEST_DB.put(pair(LocalDateTime.class, LocalDateTime.class), new Object[][] {
                { LocalDateTime.of(1970, 1, 1, 0,0), LocalDateTime.of(1970, 1, 1, 0,0), true }
        });
        TEST_DB.put(pair(Double.class, LocalDateTime.class), new Object[][] {
                { -0.000000001, LocalDateTime.parse("1969-12-31T23:59:59.999999999").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime() },   // IEEE-754 prevents perfect symmetry
                { 0d, LocalDateTime.parse("1970-01-01T00:00:00").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
                { 0.000000001, LocalDateTime.parse("1970-01-01T00:00:00.000000001").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
        });
        TEST_DB.put(pair(BigDecimal.class, LocalDateTime.class), new Object[][] {
                { new BigDecimal("-62167219200"), LocalDateTime.parse("0000-01-01T00:00:00").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
                { new BigDecimal("-62167219199.999999999"), LocalDateTime.parse("0000-01-01T00:00:00.000000001").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
                { new BigDecimal("-0.000000001"), LocalDateTime.parse("1969-12-31T23:59:59.999999999").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
                { BigDecimal.valueOf(0), LocalDateTime.parse("1970-01-01T00:00:00").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
                { new BigDecimal("0.000000001"), LocalDateTime.parse("1970-01-01T00:00:00.000000001").atZone(ZoneId.of("UTC")).withZoneSameInstant(TOKYO_Z).toLocalDateTime(), true },
        });

        /////////////////////////////////////////////////////////////
        // ZonedDateTime
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, ZonedDateTime.class), new Object[][]{
                { null, null },
        });
        TEST_DB.put(pair(ZonedDateTime.class, ZonedDateTime.class), new Object[][]{
                { ZonedDateTime.parse("1970-01-01T00:00:00.000000000Z").withZoneSameInstant(TOKYO_Z), ZonedDateTime.parse("1970-01-01T00:00:00.000000000Z").withZoneSameInstant(TOKYO_Z) },
        });
        TEST_DB.put(pair(Double.class, ZonedDateTime.class), new Object[][]{
                { -62167219200.0, ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z), true},
                { -0.000000001, ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z").withZoneSameInstant(TOKYO_Z)},    // IEEE-754 limit prevents reverse test
                { 0d, ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z), true},
                { 0.000000001, ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z), true},
                { 86400d, ZonedDateTime.parse("1970-01-02T00:00:00Z").withZoneSameInstant(TOKYO_Z), true},
                { 86400.000000001, ZonedDateTime.parse("1970-01-02T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z), true},
        });
        TEST_DB.put(pair(BigDecimal.class, ZonedDateTime.class), new Object[][]{
                { new BigDecimal("-62167219200"), ZonedDateTime.parse("0000-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z), true },
                { new BigDecimal("-0.000000001"), ZonedDateTime.parse("1969-12-31T23:59:59.999999999Z").withZoneSameInstant(TOKYO_Z), true},
                { BigDecimal.valueOf(0), ZonedDateTime.parse("1970-01-01T00:00:00Z").withZoneSameInstant(TOKYO_Z), true},
                { new BigDecimal("0.000000001"), ZonedDateTime.parse("1970-01-01T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z), true},
                { BigDecimal.valueOf(86400), ZonedDateTime.parse("1970-01-02T00:00:00Z").withZoneSameInstant(TOKYO_Z), true},
                { new BigDecimal("86400.000000001"),  ZonedDateTime.parse("1970-01-02T00:00:00.000000001Z").withZoneSameInstant(TOKYO_Z), true},
        });

        /////////////////////////////////////////////////////////////
        // ZoneOffset
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, ZoneOffset.class), new Object[][]{
                {null, null},
        });
        TEST_DB.put(pair(ZoneOffset.class, ZoneOffset.class), new Object[][]{
                {ZoneOffset.of("-05:00"), ZoneOffset.of("-05:00")},
                {ZoneOffset.of("+5"), ZoneOffset.of("+05:00")},
        });
        TEST_DB.put(pair(String.class, ZoneOffset.class), new Object[][]{
                {"-00:00", ZoneOffset.of("+00:00")},
                {"-05:00", ZoneOffset.of("-05:00")},
                {"+5", ZoneOffset.of("+05:00")},
                {"+05:00:01", ZoneOffset.of("+05:00:01")},
                {"America/New_York", new IllegalArgumentException("Unknown time-zone offset: 'America/New_York'")},
        });
        TEST_DB.put(pair(Map.class, ZoneOffset.class), new Object[][]{
                {mapOf("_v", "-10"), ZoneOffset.of("-10:00")},
                {mapOf("hours", -10L), ZoneOffset.of("-10:00")},
                {mapOf("hours", -10L, "minutes", "0"), ZoneOffset.of("-10:00")},
                {mapOf("hrs", -10L, "mins", "0"), new IllegalArgumentException("Map to ZoneOffset the map must include one of the following: [hours, minutes, seconds], [_v], or [value]")},
                {mapOf("hours", -10L, "minutes", "0", "seconds", 0), ZoneOffset.of("-10:00")},
                {mapOf("hours", "-10", "minutes", (byte) -15, "seconds", "-1"), ZoneOffset.of("-10:15:01")},
                {mapOf("hours", "10", "minutes", (byte) 15, "seconds", true), ZoneOffset.of("+10:15:01")},
                {mapOf("hours", mapOf("_v", "10"), "minutes", mapOf("_v", (byte) 15), "seconds", mapOf("_v", true)), ZoneOffset.of("+10:15:01")}, // full recursion
        });

        /////////////////////////////////////////////////////////////
        // String
        /////////////////////////////////////////////////////////////
        TEST_DB.put(pair(Void.class, String.class), new Object[][]{
                {null, null}
        });
        TEST_DB.put(pair(Byte.class, String.class), new Object[][]{
                {(byte) 0, "0"},
                {Byte.MIN_VALUE, "-128"},
                {Byte.MAX_VALUE, "127"},
        });
        TEST_DB.put(pair(Short.class, String.class), new Object[][]{
                {(short) 0, "0", true},
                {Short.MIN_VALUE, "-32768", true},
                {Short.MAX_VALUE, "32767", true},
        });
        TEST_DB.put(pair(Integer.class, String.class), new Object[][]{
                {0, "0", true},
                {Integer.MIN_VALUE, "-2147483648", true},
                {Integer.MAX_VALUE, "2147483647", true},
        });
        TEST_DB.put(pair(Long.class, String.class), new Object[][]{
                {0L, "0", true},
                {Long.MIN_VALUE, "-9223372036854775808", true},
                {Long.MAX_VALUE, "9223372036854775807", true},
        });
        TEST_DB.put(pair(Float.class, String.class), new Object[][]{
                {0f, "0", true},
                {0.0f, "0", true},
                {Float.MIN_VALUE, "1.4E-45", true},
                {-Float.MAX_VALUE, "-3.4028235E38", true},
                {Float.MAX_VALUE, "3.4028235E38", true},
                {12345679f, "1.2345679E7", true},
                {0.000000123456789f, "1.2345679E-7", true},
                {12345f, "12345.0", true},
                {0.00012345f, "1.2345E-4", true},
        });
        TEST_DB.put(pair(Double.class, String.class), new Object[][]{
                {0d, "0"},
                {0.0, "0"},
                {Double.MIN_VALUE, "4.9E-324"},
                {-Double.MAX_VALUE, "-1.7976931348623157E308"},
                {Double.MAX_VALUE, "1.7976931348623157E308"},
                {123456789d, "1.23456789E8"},
                {0.000000123456789d, "1.23456789E-7"},
                {12345d, "12345.0"},
                {0.00012345d, "1.2345E-4"},
        });
        TEST_DB.put(pair(Boolean.class, String.class), new Object[][]{
                {false, "false"},
                {true, "true"}
        });
        TEST_DB.put(pair(Character.class, String.class), new Object[][]{
                {'1', "1"},
                {(char) 32, " "},
        });
        TEST_DB.put(pair(BigInteger.class, String.class), new Object[][]{
                {new BigInteger("-1"), "-1"},
                {new BigInteger("0"), "0"},
                {new BigInteger("1"), "1"},
        });
        TEST_DB.put(pair(BigDecimal.class, String.class), new Object[][]{
                {new BigDecimal("-1"), "-1"},
                {new BigDecimal("-1.0"), "-1"},
                {new BigDecimal("0"), "0", true},
                {new BigDecimal("0.0"), "0"},
                {new BigDecimal("1.0"), "1"},
                {new BigDecimal("3.141519265358979323846264338"), "3.141519265358979323846264338", true},
        });
        TEST_DB.put(pair(AtomicBoolean.class, String.class), new Object[][]{
                {new AtomicBoolean(false), "false"},
                {new AtomicBoolean(true), "true"},
        });
        TEST_DB.put(pair(AtomicInteger.class, String.class), new Object[][]{
                {new AtomicInteger(-1), "-1"},
                {new AtomicInteger(0), "0"},
                {new AtomicInteger(1), "1"},
                {new AtomicInteger(Integer.MIN_VALUE), "-2147483648"},
                {new AtomicInteger(Integer.MAX_VALUE), "2147483647"},
        });
        TEST_DB.put(pair(AtomicLong.class, String.class), new Object[][]{
                {new AtomicLong(-1), "-1"},
                {new AtomicLong(0), "0"},
                {new AtomicLong(1), "1"},
                {new AtomicLong(Long.MIN_VALUE), "-9223372036854775808"},
                {new AtomicLong(Long.MAX_VALUE), "9223372036854775807"},
        });
        TEST_DB.put(pair(byte[].class, String.class), new Object[][]{
                {new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0x8d, (byte) 0xba}, "\uD83C\uDF7A"}, // beer mug, byte[] treated as UTF-8.
                {new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68}, "ABCD"}
        });
        TEST_DB.put(pair(char[].class, String.class), new Object[][]{
                {new char[]{'A', 'B', 'C', 'D'}, "ABCD"}
        });
        TEST_DB.put(pair(Character[].class, String.class), new Object[][]{
                {new Character[]{'A', 'B', 'C', 'D'}, "ABCD"}
        });
        TEST_DB.put(pair(ByteBuffer.class, String.class), new Object[][]{
                {ByteBuffer.wrap(new byte[]{(byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33}), "0123"}
        });
        TEST_DB.put(pair(CharBuffer.class, String.class), new Object[][]{
                {CharBuffer.wrap(new char[]{'A', 'B', 'C', 'D'}), "ABCD"},
        });
        TEST_DB.put(pair(Class.class, String.class), new Object[][]{
                {Date.class, "java.util.Date", true}
        });
        TEST_DB.put(pair(Date.class, String.class), new Object[][]{
                {new Date(1), toGmtString(new Date(1))},
                {new Date(Integer.MAX_VALUE), toGmtString(new Date(Integer.MAX_VALUE))},
                {new Date(Long.MAX_VALUE), toGmtString(new Date(Long.MAX_VALUE))}
        });
        TEST_DB.put(pair(java.sql.Date.class, String.class), new Object[][]{
                {new java.sql.Date(1), toGmtString(new java.sql.Date(1))},
                {new java.sql.Date(Integer.MAX_VALUE), toGmtString(new java.sql.Date(Integer.MAX_VALUE))},
                {new java.sql.Date(Long.MAX_VALUE), toGmtString(new java.sql.Date(Long.MAX_VALUE))}
        });
        TEST_DB.put(pair(Timestamp.class, String.class), new Object[][]{
                {new Timestamp(1), toGmtString(new Timestamp(1))},
                {new Timestamp(Integer.MAX_VALUE), toGmtString(new Timestamp(Integer.MAX_VALUE))},
                {new Timestamp(Long.MAX_VALUE), toGmtString(new Timestamp(Long.MAX_VALUE))},
        });
        TEST_DB.put(pair(LocalDate.class, String.class), new Object[][]{
                {LocalDate.parse("1965-12-31"), "1965-12-31"},
        });
        TEST_DB.put(pair(LocalTime.class, String.class), new Object[][]{
                {LocalTime.parse("16:20:00"), "16:20:00"},
        });
        TEST_DB.put(pair(LocalDateTime.class, String.class), new Object[][]{
                {LocalDateTime.parse("1965-12-31T16:20:00"), "1965-12-31T16:20:00"},
        });
        TEST_DB.put(pair(ZonedDateTime.class, String.class), new Object[][]{
                {ZonedDateTime.parse("1965-12-31T16:20:00+00:00"), "1965-12-31T16:20:00Z"},
                {ZonedDateTime.parse("2024-02-14T19:20:00-05:00"), "2024-02-14T19:20:00-05:00"},
                {ZonedDateTime.parse("2024-02-14T19:20:00+05:00"), "2024-02-14T19:20:00+05:00"},
        });
        TEST_DB.put(pair(UUID.class, String.class), new Object[][]{
                {new UUID(0L, 0L), "00000000-0000-0000-0000-000000000000", true},
                {new UUID(1L, 1L), "00000000-0000-0001-0000-000000000001", true},
                {new UUID(Long.MAX_VALUE, Long.MAX_VALUE), "7fffffff-ffff-ffff-7fff-ffffffffffff", true},
                {new UUID(Long.MIN_VALUE, Long.MIN_VALUE), "80000000-0000-0000-8000-000000000000", true},
        });
        TEST_DB.put(pair(Calendar.class, String.class), new Object[][]{
                {(Supplier<Calendar>) () -> {
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTimeZone(TOKYO_TZ);
                    cal.set(2024, Calendar.FEBRUARY, 5, 22, 31, 0);
                    return cal;
                }, "2024-02-05T22:31:00"}
        });
        TEST_DB.put(pair(Number.class, String.class), new Object[][]{
                {(byte) 1, "1"},
                {(short) 2, "2"},
                {3, "3"},
                {4L, "4"},
                {5f, "5.0"},
                {6d, "6.0"},
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
        TEST_DB.put(pair(Duration.class, String.class), new Object[][]{
                {Duration.parse("PT20.345S"), "PT20.345S", true},
                {Duration.ofSeconds(60), "PT1M", true},
        });
        TEST_DB.put(pair(Instant.class, String.class), new Object[][]{
                {Instant.ofEpochMilli(0), "1970-01-01T00:00:00Z", true},
                {Instant.ofEpochMilli(1), "1970-01-01T00:00:00.001Z", true},
                {Instant.ofEpochMilli(1000), "1970-01-01T00:00:01Z", true},
                {Instant.ofEpochMilli(1001), "1970-01-01T00:00:01.001Z", true},
                {Instant.ofEpochSecond(0), "1970-01-01T00:00:00Z", true},
                {Instant.ofEpochSecond(1), "1970-01-01T00:00:01Z", true},
                {Instant.ofEpochSecond(60), "1970-01-01T00:01:00Z", true},
                {Instant.ofEpochSecond(61), "1970-01-01T00:01:01Z", true},
                {Instant.ofEpochSecond(0, 0), "1970-01-01T00:00:00Z", true},
                {Instant.ofEpochSecond(0, 1), "1970-01-01T00:00:00.000000001Z", true},
                {Instant.ofEpochSecond(0, 999999999), "1970-01-01T00:00:00.999999999Z", true},
                {Instant.ofEpochSecond(0, 9999999999L), "1970-01-01T00:00:09.999999999Z", true},
        });
        TEST_DB.put(pair(LocalTime.class, String.class), new Object[][]{
                {LocalTime.of(9, 26), "09:26"},
                {LocalTime.of(9, 26, 17), "09:26:17"},
                {LocalTime.of(9, 26, 17, 1), "09:26:17.000000001"},
        });
        TEST_DB.put(pair(MonthDay.class, String.class), new Object[][]{
                {MonthDay.of(1, 1), "--01-01", true},
                {MonthDay.of(12, 31), "--12-31", true},
        });
        TEST_DB.put(pair(YearMonth.class, String.class), new Object[][]{
                {YearMonth.of(2024, 1), "2024-01", true},
                {YearMonth.of(2024, 12), "2024-12", true},
        });
        TEST_DB.put(pair(Period.class, String.class), new Object[][]{
                {Period.of(6, 3, 21), "P6Y3M21D", true},
                {Period.ofWeeks(160), "P1120D", true},
        });
        TEST_DB.put(pair(ZoneId.class, String.class), new Object[][]{
                {ZoneId.of("America/New_York"), "America/New_York", true},
                {ZoneId.of("Z"), "Z", true},
                {ZoneId.of("UTC"), "UTC", true},
                {ZoneId.of("GMT"), "GMT", true},
        });
        TEST_DB.put(pair(ZoneOffset.class, String.class), new Object[][]{
                {ZoneOffset.of("+1"), "+01:00", true},
                {ZoneOffset.of("+0109"), "+01:09", true},
        });
        TEST_DB.put(pair(OffsetTime.class, String.class), new Object[][]{
                {OffsetTime.parse("10:15:30+01:00"), "10:15:30+01:00", true},
        });
        TEST_DB.put(pair(OffsetDateTime.class, String.class), new Object[][]{
                {OffsetDateTime.parse("2024-02-10T10:15:07+01:00"), "2024-02-10T10:15:07+01:00", true},
        });
        TEST_DB.put(pair(Year.class, String.class), new Object[][]{
                {Year.of(2024), "2024", true},
                {Year.of(1582), "1582", true},
                {Year.of(500), "500", true},
                {Year.of(1), "1", true},
                {Year.of(0), "0", true},
                {Year.of(-1), "-1", true},
        });

        TEST_DB.put(pair(URL.class, String.class), new Object[][]{
                {toURL("https://domain.com"), "https://domain.com", true},
                {toURL("http://localhost"), "http://localhost", true},
                {toURL("http://localhost:8080"), "http://localhost:8080", true},
                {toURL("http://localhost:8080/file/path"), "http://localhost:8080/file/path", true},
                {toURL("http://localhost:8080/path/file.html"), "http://localhost:8080/path/file.html", true},
                {toURL("http://localhost:8080/path/file.html?foo=1&bar=2"), "http://localhost:8080/path/file.html?foo=1&bar=2", true},
                {toURL("http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation"), "http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation", true},
                {toURL("https://foo.bar.com/"), "https://foo.bar.com/", true},
                {toURL("https://foo.bar.com/path/foo%20bar.html"), "https://foo.bar.com/path/foo%20bar.html", true},
                {toURL("https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter"), "https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter", true},
                {toURL("ftp://user@bar.com/foo/bar.txt"), "ftp://user@bar.com/foo/bar.txt", true},
                {toURL("ftp://user:password@host/foo/bar.txt"), "ftp://user:password@host/foo/bar.txt", true},
                {toURL("ftp://user:password@host:8192/foo/bar.txt"), "ftp://user:password@host:8192/foo/bar.txt", true},
                {toURL("file:/path/to/file"), "file:/path/to/file", true},
                {toURL("file://localhost/path/to/file.json"), "file://localhost/path/to/file.json", true},
                {toURL("file://servername/path/to/file.json"), "file://servername/path/to/file.json", true},
                {toURL("jar:file:/c://my.jar!/"), "jar:file:/c://my.jar!/", true},
                {toURL("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), "jar:file:/c://my.jar!/com/mycompany/MyClass.class", true}
        });

        TEST_DB.put(pair(URI.class, String.class), new Object[][]{
                {toURI("https://domain.com"), "https://domain.com", true},
                {toURI("http://localhost"), "http://localhost", true},
                {toURI("http://localhost:8080"), "http://localhost:8080", true},
                {toURI("http://localhost:8080/file/path"), "http://localhost:8080/file/path", true},
                {toURI("http://localhost:8080/path/file.html"), "http://localhost:8080/path/file.html", true},
                {toURI("http://localhost:8080/path/file.html?foo=1&bar=2"), "http://localhost:8080/path/file.html?foo=1&bar=2", true},
                {toURI("http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation"), "http://localhost:8080/path/file.html?foo=bar&qux=quy#AnchorLocation", true},
                {toURI("https://foo.bar.com/"), "https://foo.bar.com/", true},
                {toURI("https://foo.bar.com/path/foo%20bar.html"), "https://foo.bar.com/path/foo%20bar.html", true},
                {toURI("https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter"), "https://foo.bar.com/path/file.html?text=Hello+G%C3%BCnter", true},
                {toURI("ftp://user@bar.com/foo/bar.txt"), "ftp://user@bar.com/foo/bar.txt", true},
                {toURI("ftp://user:password@host/foo/bar.txt"), "ftp://user:password@host/foo/bar.txt", true},
                {toURI("ftp://user:password@host:8192/foo/bar.txt"), "ftp://user:password@host:8192/foo/bar.txt", true},
                {toURI("file:/path/to/file"), "file:/path/to/file", true},
                {toURI("file://localhost/path/to/file.json"), "file://localhost/path/to/file.json", true},
                {toURI("file://servername/path/to/file.json"), "file://servername/path/to/file.json", true},
                {toURI("jar:file:/c://my.jar!/"), "jar:file:/c://my.jar!/", true},
                {toURI("jar:file:/c://my.jar!/com/mycompany/MyClass.class"), "jar:file:/c://my.jar!/com/mycompany/MyClass.class", true}
        });

        TEST_DB.put(pair(TimeZone.class, String.class), new Object[][]{
                {TimeZone.getTimeZone("America/New_York"), "America/New_York", true},
                {TimeZone.getTimeZone("EST"), "EST", true},
                {TimeZone.getTimeZone(ZoneId.of("+05:00")), "GMT+05:00", true},
                {TimeZone.getTimeZone(ZoneId.of("America/Denver")), "America/Denver", true},
        });
    }

    private static String toGmtString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(TOKYO_TZ);
        return simpleDateFormat.format(date);
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

    @Test
    void testForMissingTests() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        int neededTests = 0;
        int conversionPairCount = 0;
        int testCount = 0;

        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Set<Class<?>> targetClasses = entry.getValue();

            for (Class<?> targetClass : targetClasses) {
                Object[][] testData = TEST_DB.get(pair(sourceClass, targetClass));
                conversionPairCount++;

                if (testData == null) { // data set needs added
                    // Change to throw exception, so that when new conversions are added, the tests will fail until
                    // an "everything" test entry is added.
                    System.err.println("No test data for: " + getShortName(sourceClass) + " ==> " + getShortName(targetClass));
                    neededTests++;
                } else {
                    if (testData.length == 0) {
                        throw new IllegalStateException("No test instances for given pairing: " + Converter.getShortName(sourceClass) + " ==> " + Converter.getShortName(targetClass));
                    }
                    testCount += testData.length;
                }
            }
        }

        System.out.println("Total conversion pairs = " + conversionPairCount);
        System.out.println("Total tests            = " + testCount);
        if (neededTests > 0) {
            System.err.println("Conversion pairs not tested = " + neededTests);
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

        return Stream.of(list.toArray(new Arguments[]{}));
    }

    private static Stream<Arguments> generateTestEverythingParamsInReverse() {
        List<Arguments> list = new ArrayList<>(400);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Object[][]> entry : TEST_DB.entrySet()) {
            Class<?> sourceClass = entry.getKey().getKey();
            Class<?> targetClass = entry.getKey().getValue();

            String sourceName = Converter.getShortName(sourceClass);
            String targetName = Converter.getShortName(targetClass);
            Object[][] testData = entry.getValue();

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

                list.add(Arguments.of(targetName, sourceName, target, source, targetClass, sourceClass));
            }
        }

        return Stream.of(list.toArray(new Arguments[]{}));
    }

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParams")
    void testConvert(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass) {
        if (source == null) {
            assertEquals(sourceClass, Void.class, "On the source-side of test input, null can only appear in the Void.class data");
        } else {
            assert ClassUtilities.toPrimitiveWrapperClass(sourceClass).isInstance(source) : "source type mismatch ==> Expected: " + shortNameSource + ", Actual: " + Converter.getShortName(source.getClass());
        }
        assert target == null || target instanceof Throwable || ClassUtilities.toPrimitiveWrapperClass(targetClass).isInstance(target) : "target type mismatch ==> " + shortNameTarget + ", actual: " + Converter.getShortName(target.getClass());
        
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
                if (target instanceof BigDecimal) {
                    if (((BigDecimal) target).compareTo((BigDecimal) actual) != 0) {
                        assertEquals(target, actual);
                    }
                } else {
                    assertEquals(target, actual);
                }
            }
            catch (Throwable e) {
                System.err.println(shortNameSource + "[" + source + "] ==> " + shortNameTarget + "[" + target + "] Failed with: " + actual);
                throw e;
            }
        }
    }

    @ParameterizedTest(name = "{0}[{2}] ==> {1}[{3}]")
    @MethodSource("generateTestEverythingParamsInReverse")
    void testConvertReverse(String shortNameSource, String shortNameTarget, Object source, Object target, Class<?> sourceClass, Class<?> targetClass) {
        testConvert(shortNameSource, shortNameTarget, source, target, sourceClass, targetClass);
    }
}
