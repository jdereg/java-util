package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.MapUtilities.mapOf;
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
        // [ [source1, answer1],
        //   [source2, answer2],
        //   ...
        //   [source-n, answer-n]
        // ]
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
                { 0f, (byte) 0 },
                { 1f, (byte) 1 },
                { -128f, Byte.MIN_VALUE },
                { 127f, Byte.MAX_VALUE },
                { -129f, (byte) 127 }, // verify wrap around
                { 128f, (byte) -128 }   // verify wrap around
        });
        TEST_FACTORY.put(pair(Double.class, Byte.class), new Object[][] {
                { -1d, (byte) -1 },
                { 0d, (byte) 0 },
                { 1d, (byte) 1 },
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
                { new BigDecimal("0"), (byte) 0 },
                { new BigDecimal("1"), (byte) 1 },
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
                { mapOf("value", "0"), (byte) 0 },
                { mapOf("value", 0L), (byte) 0 },

                { mapOf("_v", "1"), (byte) 1 },
                { mapOf("_v", 1), (byte) 1 },
                { mapOf("value", "1"), (byte) 1 },
                { mapOf("value", 1L), (byte) 1 },

                { mapOf("_v","-128"), Byte.MIN_VALUE },
                { mapOf("_v",-128), Byte.MIN_VALUE },
                { mapOf("value","-128"), Byte.MIN_VALUE },
                { mapOf("value",-128L), Byte.MIN_VALUE },

                { mapOf("_v", "127"), Byte.MAX_VALUE },
                { mapOf("_v", 127), Byte.MAX_VALUE },
                { mapOf("value", "127"), Byte.MAX_VALUE },
                { mapOf("value", 127L), Byte.MAX_VALUE },

                { mapOf("_v", "-129"), new IllegalArgumentException("-129 not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", -129), (byte)127 },
                { mapOf("value", "-129"), new IllegalArgumentException("-129 not parseable as a byte value or outside -128 to 127") },
                { mapOf("value", -129L), (byte) 127 },

                { mapOf("_v", "128"), new IllegalArgumentException("128 not parseable as a byte value or outside -128 to 127") },
                { mapOf("_v", 128), (byte) -128 },
                { mapOf("value", "128"), new IllegalArgumentException("128 not parseable as a byte value or outside -128 to 127") },
                { mapOf("value", 128L), (byte) -128 },
        });
        TEST_FACTORY.put(pair(String.class, Byte.class), new Object[][] {
                { "-1", (byte)-1 },
                { "0", (byte)0 },
                { "1", (byte)1 },
                { "-128", (byte)-128 },
                { "127", (byte)127 },
                { "-129", new IllegalArgumentException("-129 not parseable as a byte value or outside -128 to 127") },
                { "128", new IllegalArgumentException("128 not parseable as a byte value or outside -128 to 127") },
        });
    }
    @BeforeEach
    public void before() {
        // create converter with default options
        converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testEverything() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();
        
        for (Map.Entry<Class<?>, Set<Class<?>>> entry : map.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Set<Class<?>> targetClasses = entry.getValue();
            
            for (Class<?> targetClass : targetClasses) {
                Object[][] testData = TEST_FACTORY.get(pair(sourceClass, targetClass));

                if (testData == null) { // data set needs added
                    // Change to throw exception, so that when new conversions are added, the tests will fail until
                    // an "everything" test entry is added.
                    System.out.println("No test data for: " + Converter.getShortName(sourceClass));
                    continue;
                }
                
                for (int i=0; i < testData.length; i++) {
                    Object[] testPair = testData[i];
                    try {
                        if (testPair.length != 2) {
                            throw new IllegalArgumentException("Test cases must have two values : [ source instance, target instance]");
                        }
                        if (testPair[1] instanceof Throwable) {
                            Throwable t = (Throwable) testPair[1];
                            assertThatExceptionOfType(t.getClass())
                                    .isThrownBy(() ->  converter.convert(testPair[0], targetClass))
                                    .withMessageContaining(((Throwable) testPair[1]).getMessage());

                        } else {
                            if (testPair[0] != null) {
                                assertThat(testPair[0]).isInstanceOf(sourceClass);
                            }
                            assertEquals(testPair[1], converter.convert(testPair[0], targetClass));
                        }
                    } catch (Throwable e) {
                        // Useful for debugging.  Stop here, look at
                        // source: testPair[0] and target: testPair[1] (and try conveter.convert(testPair[0], targetClass) to see what you are getting back
                        System.err.println(Converter.getShortName(sourceClass) + ".class ==> " + Converter.getShortName(targetClass) + ".class");
                        System.err.print("testPair[" + i + "]=");
                        if (testPair.length == 2) {
                            System.err.println("{ " + testPair[0].toString() + ", " + testPair[1].toString() + " }");
                        }
                        throw e;
                    }
                }
            }
        }
    }
}
