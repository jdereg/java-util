package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.MonthDay;
import java.time.Period;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static com.cedarsoftware.util.convert.Converter.getShortName;
import static com.cedarsoftware.util.convert.Converter.pair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class ConverterEverythingTest
{
    private Converter converter;
    private static final Map<Map.Entry<Class<?>, Class<?>>, Object[][]> TEST_FACTORY = new ConcurrentHashMap<>(500, .8f);


    static {
        //   {source1, answer1},
        //   ...
        //   {source-n, answer-n}

        // Byte/byte
        TEST_FACTORY.put(pair(Void.class, byte.class), new Object[][] {
                { null, (byte)0 }
        });
        TEST_FACTORY.put(pair(Void.class, Byte.class), new Object[][] {
                { null, null }
        });
        TEST_FACTORY.put(pair(Byte.class, Byte.class), new Object[][] {
                { (byte)-1, (byte)-1 },
                { (byte)0, (byte)0 },
                { (byte)1, (byte)1 },
                { Byte.MIN_VALUE, Byte.MIN_VALUE },
                { Byte.MAX_VALUE, Byte.MAX_VALUE }
        });
        TEST_FACTORY.put(pair(Short.class, Byte.class), new Object[][] {
                { (short)-1, (byte)-1 },
                { (short)0, (byte) 0 },
                { (short)1, (byte)1 },
                { (short)-128, Byte.MIN_VALUE },
                { (short)127, Byte.MAX_VALUE },
                { (short)-129, (byte) 127 },  // verify wrap around
                { (short)128, (byte)-128 }    // verify wrap around
        });
        TEST_FACTORY.put(pair(Integer.class, Byte.class), new Object[][] {
                { -1, (byte)-1 },
                { 0, (byte) 0 },
                { 1, (byte) 1 },
                { -128, Byte.MIN_VALUE },
                { 127, Byte.MAX_VALUE },
                { -129, (byte) 127 }, // verify wrap around
                { 128, (byte)-128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Long.class, Byte.class), new Object[][] {
                { -1L, (byte)-1 },
                { 0L, (byte) 0 },
                { 1L, (byte) 1 },
                { -128L, Byte.MIN_VALUE },
                { 127L, Byte.MAX_VALUE },
                { -129L, (byte) 127 }, // verify wrap around
                { 128L, (byte)-128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Float.class, Byte.class), new Object[][] {
                { -1f, (byte)-1 },
                { -1.99f, (byte)-1 },
                { -1.1f, (byte)-1 },
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
                { -1.99d, (byte)-1 },
                { -1.1d, (byte)-1 },
                { 0d, (byte) 0 },
                { 1d, (byte) 1 },
                { 1.1d, (byte) 1 },
                { 1.999d, (byte) 1 },
                { -128d, Byte.MIN_VALUE },
                { 127d, Byte.MAX_VALUE },
                {-129d, (byte) 127 }, // verify wrap around
                { 128d, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Boolean.class, Byte.class), new Object[][] {
                { true, (byte) 1 },
                { false, (byte) 0 },
        });
        TEST_FACTORY.put(pair(Character.class, Byte.class), new Object[][] {
                { '1', (byte) 49 },
                { '0', (byte) 48 },
                { (char)1, (byte) 1 },
                { (char)0, (byte) 0 },
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
                { new AtomicInteger(-129), (byte)127 },
                { new AtomicInteger(128), (byte)-128 },
        });
        TEST_FACTORY.put(pair(AtomicLong.class, Byte.class), new Object[][] {
                { new AtomicLong(-1), (byte) -1 },
                { new AtomicLong(0), (byte) 0 },
                { new AtomicLong(1), (byte) 1 },
                { new AtomicLong(-128), Byte.MIN_VALUE },
                { new AtomicLong(127), Byte.MAX_VALUE },
                { new AtomicLong(-129), (byte)127 },
                { new AtomicLong(128), (byte)-128 },
        });
        TEST_FACTORY.put(pair(BigInteger.class, Byte.class), new Object[][] {
                { new BigInteger("-1"), (byte) -1 },
                { new BigInteger("0"), (byte) 0 },
                { new BigInteger("1"), (byte) 1 },
                { new BigInteger("-128"), Byte.MIN_VALUE },
                { new BigInteger("127"), Byte.MAX_VALUE },
                { new BigInteger("-129"), (byte)127 },
                { new BigInteger("128"), (byte)-128 },
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
                { new BigDecimal("-129"), (byte)127 },
                { new BigDecimal("128"), (byte)-128 },
        });
        TEST_FACTORY.put(pair(Number.class, Byte.class), new Object[][] {

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

                { mapOf("_v","-128"), Byte.MIN_VALUE },
                { mapOf("_v",-128), Byte.MIN_VALUE },

                { mapOf("_v", "127"), Byte.MAX_VALUE },
                { mapOf("_v", 127), Byte.MAX_VALUE },

                { mapOf("_v", "-129"), new IllegalArgumentException("'-129' not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", -129), (byte)127 },

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
                { "-128", (byte)-128 },
                { "127", (byte)127 },
                { "", (byte)0 },
                { "crapola", new IllegalArgumentException("Value 'crapola' not parseable as a byte value or outside -128 to 127")},
                { "54 crapola", new IllegalArgumentException("Value '54 crapola' not parseable as a byte value or outside -128 to 127")},
                { "54crapola", new IllegalArgumentException("Value '54crapola' not parseable as a byte value or outside -128 to 127")},
                { "crapola 54", new IllegalArgumentException("Value 'crapola 54' not parseable as a byte value or outside -128 to 127")},
                { "crapola54", new IllegalArgumentException("Value 'crapola54' not parseable as a byte value or outside -128 to 127")},
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
                { mapOf("_v","--01-01"), MonthDay.of(1, 1) },
                { mapOf("_v","--1-1"), new IllegalArgumentException("Unable to extract Month-Day from string: --1-1") },
                { mapOf("_v","12-31"), MonthDay.of(12, 31) },
                { mapOf("_v","--12-31"), MonthDay.of(12, 31) },
                { mapOf("_v","-12-31"), new IllegalArgumentException("Unable to extract Month-Day from string: -12-31") },
                { mapOf("_v","6-30"), MonthDay.of(6, 30) },
                { mapOf("_v","06-30"), MonthDay.of(6, 30) },
                { mapOf("_v","--06-30"), MonthDay.of(6, 30) },
                { mapOf("_v","--6-30"), new IllegalArgumentException("Unable to extract Month-Day from string: --6-30") },
                { mapOf("month","6", "day", 30), MonthDay.of(6, 30) },
                { mapOf("month",6L, "day", "30"), MonthDay.of(6, 30)},
                { mapOf("month", mapOf("_v", 6L), "day", "30"), MonthDay.of(6, 30)},    // recursive on "month"
                { mapOf("month", 6L, "day", mapOf("_v", "30")), MonthDay.of(6, 30)},    // recursive on "day"
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
                { Period.of(0, 0, 0), Period.of(0,0, 0) },
                { Period.of(1, 1, 1), Period.of(1,1, 1) },
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
                { mapOf("_v", "P1Y1M1D"), Period.of(1, 1, 1) },
                { mapOf("years", "2", "months", 2, "days", 2.0d), Period.of(2, 2, 2) },
                { mapOf("years", mapOf("_v", (byte)2), "months", mapOf("_v", 2.0f), "days", mapOf("_v", new AtomicInteger(2))), Period.of(2, 2, 2) },   // recursion
        });
    }
    
    @BeforeEach
    public void before() {
        // create converter with default options
        converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testEverything() {
        boolean failed = false;
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        int neededTests = 0;
        int count = 0;
        
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
                    continue;
                }
                
                for (int i=0; i < testData.length; i++) {
                    Object[] testPair = testData[i];
                    try {
                        verifyTestPair(sourceClass, targetClass, testPair);
                        count++;
                    } catch (Throwable e) {
                        System.err.println();
                        System.err.println("{ " + getShortName(sourceClass) + ".class ==> " + getShortName(targetClass) + ".class }");
                        System.err.print("testPair[" + i + "] = ");
                        if (testPair.length == 2) {
                            System.err.println("{ " + testPair[0].toString() + ", " + testPair[1].toString() + " }");
                        }
                        System.err.println();
                        e.printStackTrace();
                        System.err.println();
                        System.err.flush();
                        failed = true;
                    }
                }
            }
        }

        if (neededTests > 0) {
            System.err.println("Conversions needing tests: " + neededTests);
            System.err.flush();
        }
        if (failed) {
            throw new RuntimeException("One or more tests failed.");
        }
        if (neededTests > 0 || failed) {
            System.out.println("Tests passed: " + count);
            System.out.flush();
        }
    }

    private void verifyTestPair(Class<?> sourceClass, Class<?> targetClass, Object[] testPair) {
        if (testPair.length != 2) {
            throw new IllegalArgumentException("Test cases must have two values : { source instance, target instance }");
        }

        if (testPair[0] != null) {
            assertThat(testPair[0]).isInstanceOf(sourceClass);
        }
        
        if (testPair[1] instanceof Throwable) {
            Throwable t = (Throwable) testPair[1];
            assertThatExceptionOfType(t.getClass())
                    .isThrownBy(() ->  converter.convert(testPair[0], targetClass))
                    .withMessageContaining(((Throwable) testPair[1]).getMessage());
        } else {
            assertEquals(testPair[1], converter.convert(testPair[0], targetClass));
        }
    }
}
