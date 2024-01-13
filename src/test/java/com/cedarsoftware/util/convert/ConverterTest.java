package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.cedarsoftware.util.convert.Converter.localDateTimeToMillis;
import static com.cedarsoftware.util.convert.Converter.localDateToMillis;
import static com.cedarsoftware.util.convert.Converter.zonedDateTimeToMillis;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.bar;
import static com.cedarsoftware.util.convert.ConverterTest.fubar.foo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
class ConverterTest
{

    private Converter converter;

    enum fubar
    {
        foo, bar, baz, quz
    }

    @BeforeEach
    public void before() {
        // create converter with default options
        this.converter = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> testByte_minValue_params() {
        return Stream.of(
                Arguments.of("-128"),
                Arguments.of(Byte.MIN_VALUE),
                Arguments.of((short)Byte.MIN_VALUE),
                Arguments.of((int)Byte.MIN_VALUE),
                Arguments.of((long)Byte.MIN_VALUE),
                Arguments.of(-128.0f),
                Arguments.of(-128.0d),
                Arguments.of( new BigDecimal("-128.0")),
                Arguments.of( new BigDecimal("-128.9")),
                Arguments.of( new BigInteger("-128")),
                Arguments.of( new AtomicInteger(-128)),
                Arguments.of( new AtomicLong(-128L)));
    }

    @ParameterizedTest
    @MethodSource("testByte_minValue_params")
    void testByte_minValue(Object value)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isEqualTo(Byte.MIN_VALUE);
    }

    @ParameterizedTest
    @MethodSource("testByte_minValue_params")
    void testByte_minValue_usingPrimitive(Object value)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isEqualTo(Byte.MIN_VALUE);
    }


    private static Stream<Arguments> testByte_maxValue_params() {
        return Stream.of(
                Arguments.of("127.9"),
                Arguments.of("127"),
                Arguments.of(Byte.MAX_VALUE),
                Arguments.of((short)Byte.MAX_VALUE),
                Arguments.of((int)Byte.MAX_VALUE),
                Arguments.of((long)Byte.MAX_VALUE),
                Arguments.of(127.0f),
                Arguments.of(127.0d),
                Arguments.of( new BigDecimal("127.0")),
                Arguments.of( new BigInteger("127")),
                Arguments.of( new AtomicInteger(127)),
                Arguments.of( new AtomicLong(127L)));
    }

    @ParameterizedTest
    @MethodSource("testByte_maxValue_params")
    void testByte_maxValue(Object value)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isEqualTo(Byte.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("testByte_maxValue_params")
    void testByte_maxValue_usingPrimitive(Object value)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isEqualTo(Byte.MAX_VALUE);
    }
    
    private static Stream<Arguments> testByte_booleanParams() {
        return Stream.of(
                Arguments.of( true, CommonValues.BYTE_ONE),
                Arguments.of( false, CommonValues.BYTE_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.BYTE_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.BYTE_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.BYTE_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.BYTE_ZERO));
    }

    @ParameterizedTest
    @MethodSource("testByte_booleanParams")
    void testByte_fromBoolean(Object value, Byte expectedResult)
    {
        Byte converted = this.converter.convert(value, Byte.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("testByte_booleanParams")
    void testByte_fromBoolean_usingPrimitive(Object value, Byte expectedResult)
    {
        byte converted = this.converter.convert(value, byte.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    private static Stream<Arguments> testByteParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a byte"),
                Arguments.of("-129", "not parseable as a byte"),
                Arguments.of("128", "not parseable as a byte"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testByteParams_withIllegalArguments")
    void testByte_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, byte.class))
                .withMessageContaining(partialMessage);
    }

    private static Stream<Arguments> testShortParams() {
        return Stream.of(
                Arguments.of("-32768.9", (short)-32768),
                Arguments.of("-32768", (short)-32768),
                Arguments.of("32767", (short)32767),
                Arguments.of("32767.9", (short)32767),
                Arguments.of(Byte.MIN_VALUE, (short)-128),
                Arguments.of(Byte.MAX_VALUE, (short)127),
                Arguments.of(Short.MIN_VALUE, (short)-32768),
                Arguments.of(Short.MAX_VALUE, (short)32767),
                Arguments.of(-25, (short)-25),
                Arguments.of(24, (short)24),
                Arguments.of(-128L, (short)-128),
                Arguments.of(127L, (short)127),
                Arguments.of(-128.0f, (short)-128),
                Arguments.of(127.0f, (short)127),
                Arguments.of(-128.0d, (short)-128),
                Arguments.of(127.0d, (short)127),
                Arguments.of( new BigDecimal("100"),(short)100),
                Arguments.of( new BigInteger("120"), (short)120),
                Arguments.of( new AtomicInteger(25), (short)25),
                Arguments.of( new AtomicLong(100L), (short)100)
        );
    }


    @ParameterizedTest
    @MethodSource("testShortParams")
    void testShort(Object value, Short expectedResult)
    {
        Short converted = this.converter.convert(value, Short.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("testShortParams")
    void testShort_usingPrimitive(Object value, short expectedResult)
    {
        short converted = this.converter.convert(value, short.class);
        assertThat(converted).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> testShort_booleanParams() {
        return Stream.of(
                Arguments.of( true, CommonValues.SHORT_ONE),
                Arguments.of( false, CommonValues.SHORT_ZERO),
                Arguments.of( Boolean.TRUE, CommonValues.SHORT_ONE),
                Arguments.of( Boolean.FALSE, CommonValues.SHORT_ZERO),
                Arguments.of( new AtomicBoolean(true), CommonValues.SHORT_ONE),
                Arguments.of( new AtomicBoolean(false), CommonValues.SHORT_ZERO));
    }

    @ParameterizedTest
    @MethodSource("testShort_booleanParams")
    void testShort_fromBoolean(Object value, Short expectedResult)
    {
        Short converted = this.converter.convert(value, Short.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("testShort_booleanParams")
    void testShort_fromBoolean_usingPrimitives(Object value, Short expectedResult)
    {
        short converted = this.converter.convert(value, short.class);
        assertThat(converted).isSameAs(expectedResult);
    }

    private static Stream<Arguments> testShortParams_withIllegalArguments() {
        return Stream.of(
                Arguments.of("45badNumber", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of("-32769", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of("32768", "not parseable as a short value or outside -32768 to 32767"),
                Arguments.of( TimeZone.getDefault(), "Unsupported conversion"));
    }

    @ParameterizedTest
    @MethodSource("testShortParams_withIllegalArguments")
    void testShort_withIllegalArguments(Object value, String partialMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->  this.converter.convert(value, short.class))
                .withMessageContaining(partialMessage);
    }


    @Test
    void testInt()
    {
        Integer x = this.converter.convert("-450000", int.class);
        assertEquals((Object) (-450000), x);
        x = this.converter.convert("550000", Integer.class);
        assertEquals((Object) 550000, x);

        x = this.converter.convert(100000, int.class);
        assertEquals((Object) 100000, x);
        x = this.converter.convert(200000, Integer.class);
        assertEquals((Object) 200000, x);

        x = this.converter.convert(new BigDecimal("100000"), int.class);
        assertEquals((Object) 100000, x);
        x = this.converter.convert(new BigInteger("200000"), Integer.class);
        assertEquals((Object) 200000, x);

        assert 1 == this.converter.convert(true, Integer.class);
        assert 0 == this.converter.convert(false, int.class);

        assert 25 == this.converter.convert(new AtomicInteger(25), int.class);
        assert 100 == this.converter.convert(new AtomicLong(100L), Integer.class);
        assert 1 == this.converter.convert(new AtomicBoolean(true), Integer.class);
        assert 0 == this.converter.convert(new AtomicBoolean(false), Integer.class);
        assert 11 == converter.convert("11.5", int.class);
        assert 11 == converter.convert("11.5", Integer.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), int.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45badNumber", int.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("value: 45badnumber not parseable as an integer value or outside -214"));
        }

        try
        {
            this.converter.convert("2147483649", int.class);
            fail();
        }
        catch (IllegalArgumentException e) { }
    }

    @Test
    void testLong()
    {
        assert 0L == this.converter.convert(null, long.class);

        Long x = this.converter.convert("-450000", long.class);
        assertEquals((Object)(-450000L), x);
        x = this.converter.convert("550000", Long.class);
        assertEquals((Object)550000L, x);

        x = this.converter.convert(100000L, long.class);
        assertEquals((Object)100000L, x);
        x = this.converter.convert(200000L, Long.class);
        assertEquals((Object)200000L, x);

        x = this.converter.convert(new BigDecimal("100000"), long.class);
        assertEquals((Object)100000L, x);
        x = this.converter.convert(new BigInteger("200000"), Long.class);
        assertEquals((Object)200000L, x);

        assert (long) 1 == this.converter.convert(true, long.class);
        assert (long) 0 == this.converter.convert(false, Long.class);

        Date now = new Date();
        long now70 = now.getTime();
        assert now70 == this.converter.convert(now, long.class);

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        assert now70 == this.converter.convert(today, Long.class);

        LocalDate localDate = LocalDate.now();
        now70 = localDate.toEpochDay();
        assert now70 == this.converter.convert(localDate, long.class);

        assert 25L == this.converter.convert(new AtomicInteger(25), long.class);
        assert 100L == this.converter.convert(new AtomicLong(100L), Long.class);
        assert 1L == this.converter.convert(new AtomicBoolean(true), Long.class);
        assert 0L == this.converter.convert(new AtomicBoolean(false), Long.class);
        assert 11L == converter.convert("11.5", long.class);
        assert 11L == converter.convert("11.5", Long.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), long.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45badNumber", long.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("value: 45badnumber not parseable as a long value or outside -922"));
        }
    }

    @Test
    void testAtomicLong()
    {
        AtomicLong x = this.converter.convert("-450000", AtomicLong.class);
        assertEquals(-450000L, x.get());
        x = this.converter.convert("550000", AtomicLong.class);
        assertEquals(550000L, x.get());

        x = this.converter.convert(100000L, AtomicLong.class);
        assertEquals(100000L, x.get());
        x = this.converter.convert(200000L, AtomicLong.class);
        assertEquals(200000L, x.get());

        x = this.converter.convert(new BigDecimal("100000"), AtomicLong.class);
        assertEquals(100000L, x.get());
        x = this.converter.convert(new BigInteger("200000"), AtomicLong.class);
        assertEquals(200000L, x.get());

        x = this.converter.convert(true, AtomicLong.class);
        assertEquals((long)1, x.get());
        x = this.converter.convert(false, AtomicLong.class);
        assertEquals((long)0, x.get());

        Date now = new Date();
        long now70 = now.getTime();
        x = this.converter.convert(now, AtomicLong.class);
        assertEquals(now70, x.get());

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        x = this.converter.convert(today, AtomicLong.class);
        assertEquals(now70, x.get());

        x = this.converter.convert(new AtomicInteger(25), AtomicLong.class);
        assertEquals(25L, x.get());
        x = this.converter.convert(new AtomicLong(100L), AtomicLong.class);
        assertEquals(100L, x.get());
        x = this.converter.convert(new AtomicBoolean(true), AtomicLong.class);
        assertEquals(1L, x.get());
        x = this.converter.convert(new AtomicBoolean(false), AtomicLong.class);
        assertEquals(0L, x.get());
        assertEquals(new AtomicLong(11).get(), converter.convert("11.5", AtomicLong.class).get());

        try
        {
            this.converter.convert(TimeZone.getDefault(), AtomicLong.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45badNumber", AtomicLong.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("Value: 45badNumber not parseable as an AtomicLong value or outside -922"));
        }
    }

    @Test
    void testString()
    {
        assertEquals("Hello", this.converter.convert("Hello", String.class));
        assertEquals("25", this.converter.convert(25.0d, String.class));
        assertEquals("3141592653589793300", this.converter.convert(3.1415926535897932384626433e18, String.class));
        assertEquals("true", this.converter.convert(true, String.class));
        assertEquals("J", this.converter.convert('J', String.class));
        assertEquals("3.1415926535897932384626433", this.converter.convert(new BigDecimal("3.1415926535897932384626433"), String.class));
        assertEquals("123456789012345678901234567890", this.converter.convert(new BigInteger("123456789012345678901234567890"), String.class));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 8, 34, 49);
        assertEquals("2015-01-17T08:34:49", this.converter.convert(cal.getTime(), String.class));
        assertEquals("2015-01-17T08:34:49", this.converter.convert(cal, String.class));

        assertEquals("25", this.converter.convert(new AtomicInteger(25), String.class));
        assertEquals("100", this.converter.convert(new AtomicLong(100L), String.class));
        assertEquals("true", this.converter.convert(new AtomicBoolean(true), String.class));

        assertEquals("1.23456789", this.converter.convert(1.23456789d, String.class));

        int x = 8;
        String s = this.converter.convert(x, String.class);
        assert s.equals("8");
        assertEquals("123456789.12345", this.converter.convert(123456789.12345, String.class));

        try
        {
            this.converter.convert(TimeZone.getDefault(), String.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        assert this.converter.convert(new HashMap<>(), HashMap.class) instanceof Map;

        try
        {
            this.converter.convert(ZoneId.systemDefault(), String.class);
            fail();
        }
        catch (Exception e)
        {
            TestUtil.assertContainsIgnoreCase(e.getMessage(), "unsupported conversion, source type [zoneregion");
        }
    }

    @Test
    void testBigDecimal()
    {
        Object o = converter.convert("", BigDecimal.class);
        assertEquals(o, BigDecimal.ZERO);
        BigDecimal x = this.converter.convert("-450000", BigDecimal.class);
        assertEquals(new BigDecimal("-450000"), x);

        assertEquals(new BigDecimal("3.14"), this.converter.convert(new BigDecimal("3.14"), BigDecimal.class));
        assertEquals(new BigDecimal("8675309"), this.converter.convert(new BigInteger("8675309"), BigDecimal.class));
        assertEquals(new BigDecimal("75"), this.converter.convert((short) 75, BigDecimal.class));
        assertEquals(BigDecimal.ONE, this.converter.convert(true, BigDecimal.class));
        assertSame(BigDecimal.ONE, this.converter.convert(true, BigDecimal.class));
        assertEquals(BigDecimal.ZERO, this.converter.convert(false, BigDecimal.class));
        assertSame(BigDecimal.ZERO, this.converter.convert(false, BigDecimal.class));

        Date now = new Date();
        BigDecimal now70 = new BigDecimal(now.getTime());
        assertEquals(now70, this.converter.convert(now, BigDecimal.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigDecimal(today.getTime().getTime());
        assertEquals(now70, this.converter.convert(today, BigDecimal.class));

        assertEquals(new BigDecimal(25), this.converter.convert(new AtomicInteger(25), BigDecimal.class));
        assertEquals(new BigDecimal(100), this.converter.convert(new AtomicLong(100L), BigDecimal.class));
        assertEquals(BigDecimal.ONE, this.converter.convert(new AtomicBoolean(true), BigDecimal.class));
        assertEquals(BigDecimal.ZERO, this.converter.convert(new AtomicBoolean(false), BigDecimal.class));

        try
        {
            this.converter.convert(TimeZone.getDefault(), BigDecimal.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45badNumber", BigDecimal.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("value: 45badnumber not parseable as a bigdecimal value"));
        }
    }

    @Test
    void testBigInteger()
    {
        BigInteger x = this.converter.convert("-450000", BigInteger.class);
        assertEquals(new BigInteger("-450000"), x);

        assertEquals(new BigInteger("3"), this.converter.convert(new BigDecimal("3.14"), BigInteger.class));
        assertEquals(new BigInteger("8675309"), this.converter.convert(new BigInteger("8675309"), BigInteger.class));
        assertEquals(new BigInteger("75"), this.converter.convert((short) 75, BigInteger.class));
        assertEquals(BigInteger.ONE, this.converter.convert(true, BigInteger.class));
        assertSame(BigInteger.ONE, this.converter.convert(true, BigInteger.class));
        assertEquals(BigInteger.ZERO, this.converter.convert(false, BigInteger.class));
        assertSame(BigInteger.ZERO, this.converter.convert(false, BigInteger.class));
        assertEquals(new BigInteger("11"), converter.convert("11.5", BigInteger.class));
        
        Date now = new Date();
        BigInteger now70 = new BigInteger(Long.toString(now.getTime()));
        assertEquals(now70, this.converter.convert(now, BigInteger.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigInteger(Long.toString(today.getTime().getTime()));
        assertEquals(now70, this.converter.convert(today, BigInteger.class));

        assertEquals(new BigInteger("25"), this.converter.convert(new AtomicInteger(25), BigInteger.class));
        assertEquals(new BigInteger("100"), this.converter.convert(new AtomicLong(100L), BigInteger.class));
        assertEquals(BigInteger.ONE, this.converter.convert(new AtomicBoolean(true), BigInteger.class));
        assertEquals(BigInteger.ZERO, this.converter.convert(new AtomicBoolean(false), BigInteger.class));

        try {
            this.converter.convert(TimeZone.getDefault(), BigInteger.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try {
            this.converter.convert("45badNumber", BigInteger.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("value: 45badnumber not parseable as a biginteger value"));
        }
    }

    @Test
    void testAtomicInteger()
    {
        AtomicInteger x = this.converter.convert("-450000", AtomicInteger.class);
        assertEquals(-450000, x.get());

        assertEquals(3, (this.converter.convert(new BigDecimal("3.14"), AtomicInteger.class)).get());
        assertEquals(8675309, (this.converter.convert(new BigInteger("8675309"), AtomicInteger.class)).get());
        assertEquals(75, (this.converter.convert((short) 75, AtomicInteger.class)).get());
        assertEquals(1, (this.converter.convert(true, AtomicInteger.class)).get());
        assertEquals(0, (this.converter.convert(false, AtomicInteger.class)).get());
        assertEquals(new AtomicInteger(11).get(), converter.convert("11.5", AtomicInteger.class).get());

        assertEquals(25, (this.converter.convert(new AtomicInteger(25), AtomicInteger.class)).get());
        assertEquals(100, (this.converter.convert(new AtomicLong(100L), AtomicInteger.class)).get());
        assertEquals(1, (this.converter.convert(new AtomicBoolean(true), AtomicInteger.class)).get());
        assertEquals(0, (this.converter.convert(new AtomicBoolean(false), AtomicInteger.class)).get());

        try
        {
            this.converter.convert(TimeZone.getDefault(), AtomicInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45badNumber", AtomicInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("45badnumber"));
        }
    }

    @Test
    void testDate()
    {
        // Date to Date
        Date utilNow = new Date();
        Date coerced = this.converter.convert(utilNow, Date.class);
        assertEquals(utilNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);
        assert coerced != utilNow;

        // Date to java.sql.Date
        java.sql.Date sqlCoerced = this.converter.convert(utilNow, java.sql.Date.class);
        assertEquals(utilNow, sqlCoerced);

        // java.sql.Date to java.sql.Date
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());
        sqlCoerced = this.converter.convert(sqlNow, java.sql.Date.class);
        assertEquals(sqlNow, sqlCoerced);

        // java.sql.Date to Date
        coerced = this.converter.convert(sqlNow, Date.class);
        assertEquals(sqlNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);

        // Date to Timestamp
        Timestamp tstamp = this.converter.convert(utilNow, Timestamp.class);
        assertEquals(utilNow, tstamp);

        // Timestamp to Date
        Date someDate = this.converter.convert(tstamp, Date.class);
        assertEquals(utilNow, tstamp);
        assertFalse(someDate instanceof Timestamp);

        // java.sql.Date to Timestamp
        tstamp = this.converter.convert(sqlCoerced, Timestamp.class);
        assertEquals(sqlCoerced, tstamp);

        // Timestamp to java.sql.Date
        java.sql.Date someDate1 = this.converter.convert(tstamp, java.sql.Date.class);
        assertEquals(someDate1, utilNow);

        // String to Date
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);
        Date date = this.converter.convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // String to java.sql.Date
        java.sql.Date sqlDate = this.converter.convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals(cal.getTime(), sqlDate);
        assert sqlDate != null;

        // Calendar to Date
        date = this.converter.convert(cal, Date.class);
        assertEquals(date, cal.getTime());
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // Calendar to java.sql.Date
        sqlDate = this.converter.convert(cal, java.sql.Date.class);
        assertEquals(sqlDate, cal.getTime());
        assert sqlDate != null;

        // long to Date
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);
        Date converted = this.converter.convert(now, Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        Date sqlConverted = this.converter.convert(now, java.sql.Date.class);
        assertEquals(dateNow, sqlConverted);
        assert sqlConverted != null;

        // AtomicLong to Date
        now = System.currentTimeMillis();
        dateNow = new Date(now);
        converted = this.converter.convert(new AtomicLong(now), Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        dateNow = new java.sql.Date(now);
        sqlConverted = this.converter.convert(new AtomicLong(now), java.sql.Date.class);
        assert sqlConverted != null;
        assertEquals(dateNow, sqlConverted);

        // BigInteger to java.sql.Date
        BigInteger bigInt = new BigInteger("" + now);
        sqlDate = this.converter.convert(bigInt, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigDecimal to java.sql.Date
        BigDecimal bigDec = new BigDecimal(now);
        sqlDate = this.converter.convert(bigDec, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigInteger to Timestamp
        bigInt = new BigInteger("" + now);
        tstamp = this.converter.convert(bigInt, Timestamp.class);
        assert tstamp.getTime() == now;

        // BigDecimal to TimeStamp
        bigDec = new BigDecimal(now);
        tstamp = this.converter.convert(bigDec, Timestamp.class);
        assert tstamp.getTime() == now;

        // Invalid source type for Date
        try
        {
            this.converter.convert(TimeZone.getDefault(), Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        // Invalid source type for java.sql.Date
        try
        {
            this.converter.convert(TimeZone.getDefault(), java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        // Invalid source date for Date
        try
        {
            this.converter.convert("2015/01/33", Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("Day must be between 1 and 31 inclusive, date: 2015/01/33"));
        }

        // Invalid source date for java.sql.Date
        try
        {
            this.converter.convert("2015/01/33", java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("day must be between 1 and 31"));
        }
    }

    @Test
    void testBogusSqlDate2()
    {
        assertThatThrownBy(() -> this.converter.convert(true, java.sql.Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Boolean (true)] target type 'java.sql.Date'");
    }

    @Test
    void testCalendar()
    {
        // Date to Calendar
        Date now = new Date();
        Calendar calendar = this.converter.convert(new Date(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // SqlDate to Calendar
        java.sql.Date sqlDate = this.converter.convert(now, java.sql.Date.class);
        calendar = this.converter.convert(sqlDate, Calendar.class);
        assertEquals(calendar.getTime(), sqlDate);

        // Timestamp to Calendar
        Timestamp timestamp = this.converter.convert(now, Timestamp.class);
        calendar = this.converter.convert(timestamp, Calendar.class);
        assertEquals(calendar.getTime(), timestamp);

        // Long to Calendar
        calendar = this.converter.convert(now.getTime(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // AtomicLong to Calendar
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        calendar = this.converter.convert(atomicLong, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // String to Calendar
        String strDate = this.converter.convert(now, String.class);
        calendar = this.converter.convert(strDate, Calendar.class);
        String strDate2 = this.converter.convert(calendar, String.class);
        assertEquals(strDate, strDate2);

        // BigInteger to Calendar
        BigInteger bigInt = new BigInteger("" + now.getTime());
        calendar = this.converter.convert(bigInt, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // BigDecimal to Calendar
        BigDecimal bigDec = new BigDecimal(now.getTime());
        calendar = this.converter.convert(bigDec, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // Other direction --> Calendar to other date types

        // Calendar to Date
        calendar = this.converter.convert(now, Calendar.class);
        Date date = this.converter.convert(calendar, Date.class);
        assertEquals(calendar.getTime(), date);

        // Calendar to SqlDate
        sqlDate = this.converter.convert(calendar, java.sql.Date.class);
        assertEquals(calendar.getTime().getTime(), sqlDate.getTime());

        // Calendar to Timestamp
        timestamp = this.converter.convert(calendar, Timestamp.class);
        assertEquals(calendar.getTime().getTime(), timestamp.getTime());

        // Calendar to Long
        long tnow = this.converter.convert(calendar, long.class);
        assertEquals(calendar.getTime().getTime(), tnow);

        // Calendar to AtomicLong
        atomicLong = this.converter.convert(calendar, AtomicLong.class);
        assertEquals(calendar.getTime().getTime(), atomicLong.get());

        // Calendar to String
        strDate = this.converter.convert(calendar, String.class);
        strDate2 = this.converter.convert(now, String.class);
        assertEquals(strDate, strDate2);

        // Calendar to BigInteger
        bigInt = this.converter.convert(calendar, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // Calendar to BigDecimal
        bigDec = this.converter.convert(calendar, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());
    }

    @Test
    void testLocalDateToOthers()
    {
        // Date to LocalDate
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2020, 8, 30, 0, 0, 0);
        Date now = calendar.getTime();
        LocalDate localDate = this.converter.convert(now, LocalDate.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), now.getTime());

        // LocalDate to LocalDate - identity check
        LocalDate x = this.converter.convert(localDate, LocalDate.class);
        assert localDate == x;

        // LocalDateTime to LocalDate
        LocalDateTime ldt = LocalDateTime.of(2020, 8, 30, 0, 0, 0);
        x = this.converter.convert(ldt, LocalDate.class);
        assert localDateTimeToMillis(ldt, ZoneId.systemDefault()) == localDateToMillis(x, ZoneId.systemDefault());

        // ZonedDateTime to LocalDate
        ZonedDateTime zdt = ZonedDateTime.of(2020, 8, 30, 0, 0, 0, 0, ZoneId.systemDefault());
        x = this.converter.convert(zdt, LocalDate.class);
        assert zonedDateTimeToMillis(zdt) == localDateToMillis(x, ZoneId.systemDefault());

        // Calendar to LocalDate
        x = this.converter.convert(calendar, LocalDate.class);
        assert localDateToMillis(localDate, ZoneId.systemDefault()) == calendar.getTime().getTime();

        // SqlDate to LocalDate
        java.sql.Date sqlDate = this.converter.convert(now, java.sql.Date.class);
        localDate = this.converter.convert(sqlDate, LocalDate.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), sqlDate.getTime());

        // Timestamp to LocalDate
        Timestamp timestamp = this.converter.convert(now, Timestamp.class);
        localDate = this.converter.convert(timestamp, LocalDate.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), timestamp.getTime());

        LocalDate nowDate = LocalDate.now();
        // Long to LocalDate
        localDate = this.converter.convert(nowDate.toEpochDay(), LocalDate.class);
        assertEquals(localDate, nowDate);

        // AtomicLong to LocalDate
        AtomicLong atomicLong = new AtomicLong(nowDate.toEpochDay());
        localDate = this.converter.convert(atomicLong, LocalDate.class);
        assertEquals(localDate, nowDate);

        // String to LocalDate
        String strDate = this.converter.convert(now, String.class);
        localDate = this.converter.convert(strDate, LocalDate.class);
        String strDate2 = this.converter.convert(localDate, String.class);
        assert strDate.startsWith(strDate2);

        // BigInteger to LocalDate
        BigInteger bigInt = new BigInteger("" + nowDate.toEpochDay());
        localDate = this.converter.convert(bigInt, LocalDate.class);
        assertEquals(localDate, nowDate);

        // BigDecimal to LocalDate
        BigDecimal bigDec = new BigDecimal(nowDate.toEpochDay());
        localDate = this.converter.convert(bigDec, LocalDate.class);
        assertEquals(localDate, nowDate);

        // Other direction --> LocalDate to other date types

        // LocalDate to Date
        localDate = this.converter.convert(now, LocalDate.class);
        Date date = this.converter.convert(localDate, Date.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), date.getTime());

        // LocalDate to SqlDate
        sqlDate = this.converter.convert(localDate, java.sql.Date.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), sqlDate.getTime());

        // LocalDate to Timestamp
        timestamp = this.converter.convert(localDate, Timestamp.class);
        assertEquals(localDateToMillis(localDate, ZoneId.systemDefault()), timestamp.getTime());

        // LocalDate to Long
        long tnow = this.converter.convert(localDate, long.class);
        assertEquals(localDate.toEpochDay(), tnow);

        // LocalDate to AtomicLong
        atomicLong = this.converter.convert(localDate, AtomicLong.class);
        assertEquals(localDate.toEpochDay(), atomicLong.get());

        // LocalDate to String
        strDate = this.converter.convert(localDate, String.class);
        strDate2 = this.converter.convert(now, String.class);
        assert strDate2.startsWith(strDate);

        // LocalDate to BigInteger
        bigInt = this.converter.convert(localDate, BigInteger.class);
        LocalDate nd = LocalDate.ofEpochDay(bigInt.longValue());
        assertEquals(localDate, nd);

        // LocalDate to BigDecimal
        bigDec = this.converter.convert(localDate, BigDecimal.class);
        nd = LocalDate.ofEpochDay(bigDec.longValue());
        assertEquals(localDate, nd);

        // Error handling
//        try {
//            this.converter.convert("2020-12-40", LocalDate.class);
//            fail();
//        }
//        catch (IllegalArgumentException e) {
//            TestUtil.assertContainsIgnoreCase(e.getMessage(), "day must be between 1 and 31");
//        }

        assert this.converter.convert(null, LocalDate.class) == null;
    }

    @Test
    void testStringToLocalDate()
    {
        String dec23rd2023 = "19714";
        LocalDate ld = this.converter.convert(dec23rd2023, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
//        assert ld.getDayOfMonth() == 23;

        dec23rd2023 = "2023-12-23";
        ld = this.converter.convert(dec23rd2023, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        dec23rd2023 = "2023/12/23";
        ld = this.converter.convert(dec23rd2023, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        dec23rd2023 = "12/23/2023";
        ld = this.converter.convert(dec23rd2023, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }

    @Test
    void testStringOnMapToLocalDate()
    {
        Map<String, Object> map = new HashMap<>();
        String dec23Epoch = "19714";
        map.put("value", dec23Epoch);
        LocalDate ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
//        assert ld.getDayOfMonth() == 23;


        dec23Epoch = "2023-12-23";
        map.put("value", dec23Epoch);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        dec23Epoch = "2023/12/23";
        map.put("value", dec23Epoch);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        dec23Epoch = "12/23/2023";
        map.put("value", dec23Epoch);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }

    @Test
    void testStringKeysOnMapToLocalDate()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("day", "23");
        map.put("month", "12");
        map.put("year", "2023");
        LocalDate ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;

        map.put("day", 23);
        map.put("month", 12);
        map.put("year", 2023);
        ld = this.converter.convert(map, LocalDate.class);
        assert ld.getYear() == 2023;
        assert ld.getMonthValue() == 12;
        assert ld.getDayOfMonth() == 23;
    }

    @Test
    void testLocalDateTimeToOthers()
    {
        // Date to LocalDateTime
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2020, 8, 30, 13, 1, 11);
        Date now = calendar.getTime();
        LocalDateTime localDateTime = this.converter.convert(now, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), now.getTime());

        // LocalDateTime to LocalDateTime - identity check
        LocalDateTime x = this.converter.convert(localDateTime, LocalDateTime.class);
        assert localDateTime == x;

        // LocalDate to LocalDateTime
        LocalDate ld = LocalDate.of(2020, 8, 30);
        x = this.converter.convert(ld, LocalDateTime.class);
        assert localDateToMillis(ld, ZoneId.systemDefault()) == localDateTimeToMillis(x, ZoneId.systemDefault());

        // ZonedDateTime to LocalDateTime
        ZonedDateTime zdt = ZonedDateTime.of(2020, 8, 30, 13, 1, 11, 0, ZoneId.systemDefault());
        x = this.converter.convert(zdt, LocalDateTime.class);
        assert zonedDateTimeToMillis(zdt) == localDateTimeToMillis(x, ZoneId.systemDefault());

        // Calendar to LocalDateTime
        x = this.converter.convert(calendar, LocalDateTime.class);
        assert localDateTimeToMillis(localDateTime, ZoneId.systemDefault()) == calendar.getTime().getTime();

        // SqlDate to LocalDateTime
        java.sql.Date sqlDate = this.converter.convert(now, java.sql.Date.class);
        localDateTime = this.converter.convert(sqlDate, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), localDateToMillis(sqlDate.toLocalDate(), ZoneId.systemDefault()));

        // Timestamp to LocalDateTime
        Timestamp timestamp = this.converter.convert(now, Timestamp.class);
        localDateTime = this.converter.convert(timestamp, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), timestamp.getTime());

        // Long to LocalDateTime
        localDateTime = this.converter.convert(now.getTime(), LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), now.getTime());

        // AtomicLong to LocalDateTime
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        localDateTime = this.converter.convert(atomicLong, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), now.getTime());

        // String to LocalDateTime
        String strDate = this.converter.convert(now, String.class);
        localDateTime = this.converter.convert(strDate, LocalDateTime.class);
        String strDate2 = this.converter.convert(localDateTime, String.class);
        assert strDate.startsWith(strDate2);

        // BigInteger to LocalDateTime
        BigInteger bigInt = new BigInteger("" + now.getTime());
        localDateTime = this.converter.convert(bigInt, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), now.getTime());

        // BigDecimal to LocalDateTime
        BigDecimal bigDec = new BigDecimal(now.getTime());
        localDateTime = this.converter.convert(bigDec, LocalDateTime.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), now.getTime());

        // Other direction --> LocalDateTime to other date types

        // LocalDateTime to Date
        localDateTime = this.converter.convert(now, LocalDateTime.class);
        Date date = this.converter.convert(localDateTime, Date.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), date.getTime());

        // LocalDateTime to SqlDate
        sqlDate = this.converter.convert(localDateTime, java.sql.Date.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), sqlDate.getTime());

        // LocalDateTime to Timestamp
        timestamp = this.converter.convert(localDateTime, Timestamp.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), timestamp.getTime());

        // LocalDateTime to Long
        long tnow = this.converter.convert(localDateTime, long.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), tnow);

        // LocalDateTime to AtomicLong
        atomicLong = this.converter.convert(localDateTime, AtomicLong.class);
        assertEquals(localDateTimeToMillis(localDateTime, ZoneId.systemDefault()), atomicLong.get());

        // LocalDateTime to String
        strDate = this.converter.convert(localDateTime, String.class);
        strDate2 = this.converter.convert(now, String.class);
        assert strDate2.startsWith(strDate);

        // LocalDateTime to BigInteger
        bigInt = this.converter.convert(localDateTime, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // LocalDateTime to BigDecimal
        bigDec = this.converter.convert(localDateTime, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());

        // Error handling
        try
        {
            this.converter.convert("2020-12-40", LocalDateTime.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            TestUtil.assertContainsIgnoreCase(e.getMessage(), "day must be between 1 and 31");
        }

        assert this.converter.convert(null, LocalDateTime.class) == null;
    }

    @Test
    void testZonedDateTimeToOthers()
    {
        // Date to ZonedDateTime
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2020, 8, 30, 13, 1, 11);
        Date now = calendar.getTime();
        ZonedDateTime zonedDateTime = this.converter.convert(now, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), now.getTime());

        // ZonedDateTime to ZonedDateTime - identity check
        ZonedDateTime x = this.converter.convert(zonedDateTime, ZonedDateTime.class);
        assert zonedDateTime == x;

        // LocalDate to ZonedDateTime
        LocalDate ld = LocalDate.of(2020, 8, 30);
        x = this.converter.convert(ld, ZonedDateTime.class);
        assert localDateToMillis(ld, ZoneId.systemDefault()) == zonedDateTimeToMillis(x);

        // LocalDateTime to ZonedDateTime
        LocalDateTime ldt = LocalDateTime.of(2020, 8, 30, 13, 1, 11);
        x = this.converter.convert(ldt, ZonedDateTime.class);
        assert localDateTimeToMillis(ldt, ZoneId.systemDefault()) == zonedDateTimeToMillis(x);

        // ZonedDateTime to ZonedDateTime
        ZonedDateTime zdt = ZonedDateTime.of(2020, 8, 30, 13, 1, 11, 0, ZoneId.systemDefault());
        x = this.converter.convert(zdt, ZonedDateTime.class);
        assert zonedDateTimeToMillis(zdt) == zonedDateTimeToMillis(x);

        // Calendar to ZonedDateTime
        x = this.converter.convert(calendar, ZonedDateTime.class);
        assert zonedDateTimeToMillis(zonedDateTime) == calendar.getTime().getTime();

        // SqlDate to ZonedDateTime
        java.sql.Date sqlDate = this.converter.convert(now, java.sql.Date.class);
        zonedDateTime = this.converter.convert(sqlDate, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), localDateToMillis(sqlDate.toLocalDate(), ZoneId.systemDefault()));

        // Timestamp to ZonedDateTime
        Timestamp timestamp = this.converter.convert(now, Timestamp.class);
        zonedDateTime = this.converter.convert(timestamp, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), timestamp.getTime());

        // Long to ZonedDateTime
        zonedDateTime = this.converter.convert(now.getTime(), ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), now.getTime());

        // AtomicLong to ZonedDateTime
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        zonedDateTime = this.converter.convert(atomicLong, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), now.getTime());

        // String to ZonedDateTime
        String strDate = this.converter.convert(now, String.class);
        zonedDateTime = this.converter.convert(strDate, ZonedDateTime.class);
        String strDate2 = this.converter.convert(zonedDateTime, String.class);
        assert strDate2.startsWith(strDate);

        // BigInteger to ZonedDateTime
        BigInteger bigInt = new BigInteger("" + now.getTime());
        zonedDateTime = this.converter.convert(bigInt, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), now.getTime());

        // BigDecimal to ZonedDateTime
        BigDecimal bigDec = new BigDecimal(now.getTime());
        zonedDateTime = this.converter.convert(bigDec, ZonedDateTime.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), now.getTime());

        // Other direction --> ZonedDateTime to other date types

        // ZonedDateTime to Date
        zonedDateTime = this.converter.convert(now, ZonedDateTime.class);
        Date date = this.converter.convert(zonedDateTime, Date.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), date.getTime());

        // ZonedDateTime to SqlDate
        sqlDate = this.converter.convert(zonedDateTime, java.sql.Date.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), sqlDate.getTime());

        // ZonedDateTime to Timestamp
        timestamp = this.converter.convert(zonedDateTime, Timestamp.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), timestamp.getTime());

        // ZonedDateTime to Long
        long tnow = this.converter.convert(zonedDateTime, long.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), tnow);

        // ZonedDateTime to AtomicLong
        atomicLong = this.converter.convert(zonedDateTime, AtomicLong.class);
        assertEquals(zonedDateTimeToMillis(zonedDateTime), atomicLong.get());

        // ZonedDateTime to String
        strDate = this.converter.convert(zonedDateTime, String.class);
        strDate2 = this.converter.convert(now, String.class);
        assert strDate.startsWith(strDate2);

        // ZonedDateTime to BigInteger
        bigInt = this.converter.convert(zonedDateTime, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // ZonedDateTime to BigDecimal
        bigDec = this.converter.convert(zonedDateTime, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());

        // Error handling
        try {
            this.converter.convert("2020-12-40", ZonedDateTime.class);
            fail();
        }
        catch (IllegalArgumentException e) {
            TestUtil.assertContainsIgnoreCase(e.getMessage(), "day must be between 1 and 31");
        }

        assert this.converter.convert(null, ZonedDateTime.class) == null;
    }

    @Test
    void testDateErrorHandlingBadInput()
    {
        assertNull(this.converter.convert(" ", java.util.Date.class));
        assertNull(this.converter.convert("", java.util.Date.class));
        assertNull(this.converter.convert(null, java.util.Date.class));

        assertNull(this.converter.convert(" ", Date.class));
        assertNull(this.converter.convert("", Date.class));
        assertNull(this.converter.convert(null, Date.class));

        assertNull(this.converter.convert(" ", java.sql.Date.class));
        assertNull(this.converter.convert("", java.sql.Date.class));
        assertNull(this.converter.convert(null, java.sql.Date.class));

        assertNull(this.converter.convert(" ", java.sql.Date.class));
        assertNull(this.converter.convert("", java.sql.Date.class));
        assertNull(this.converter.convert(null, java.sql.Date.class));

        assertNull(this.converter.convert(" ", java.sql.Timestamp.class));
        assertNull(this.converter.convert("", java.sql.Timestamp.class));
        assertNull(this.converter.convert(null, java.sql.Timestamp.class));

        assertNull(this.converter.convert(" ", Timestamp.class));
        assertNull(this.converter.convert("", Timestamp.class));
        assertNull(this.converter.convert(null, Timestamp.class));
    }

    @Test
    void testTimestamp()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        assertEquals(now, this.converter.convert(now, Timestamp.class));
        assert this.converter.convert(now, Timestamp.class) instanceof Timestamp;

        Timestamp christmas = this.converter.convert("2015/12/25", Timestamp.class);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2015, 11, 25);
        assert christmas.getTime() == c.getTime().getTime();

        Timestamp christmas2 = this.converter.convert(c, Timestamp.class);

        assertEquals(christmas, christmas2);
        assertEquals(christmas2, this.converter.convert(christmas.getTime(), Timestamp.class));

        AtomicLong al = new AtomicLong(christmas.getTime());
        assertEquals(christmas2, this.converter.convert(al, Timestamp.class));

        ZonedDateTime zdt = ZonedDateTime.of(2020, 8, 30, 13, 11, 17, 0, ZoneId.systemDefault());
        Timestamp alexaBirthday = this.converter.convert(zdt, Timestamp.class);
        assert alexaBirthday.getTime() == zonedDateTimeToMillis(zdt);
        try
        {
            this.converter.convert(Boolean.TRUE, Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unsupported conversion, source type [boolean");
        }

        try
        {
            this.converter.convert("123dhksdk", Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unable to parse: 123");
        }
    }

    @Test
    void testFloat()
    {
        assert -3.14f == this.converter.convert(-3.14f, float.class);
        assert -3.14f == this.converter.convert(-3.14f, Float.class);
        assert -3.14f == this.converter.convert("-3.14", float.class);
        assert -3.14f == this.converter.convert("-3.14", Float.class);
        assert -3.14f == this.converter.convert(-3.14d, float.class);
        assert -3.14f == this.converter.convert(-3.14d, Float.class);
        assert 1.0f == this.converter.convert(true, float.class);
        assert 1.0f == this.converter.convert(true, Float.class);
        assert 0.0f == this.converter.convert(false, float.class);
        assert 0.0f == this.converter.convert(false, Float.class);

        assert 0.0f == this.converter.convert(new AtomicInteger(0), Float.class);
        assert 0.0f == this.converter.convert(new AtomicLong(0), Float.class);
        assert 0.0f == this.converter.convert(new AtomicBoolean(false), Float.class);
        assert 1.0f == this.converter.convert(new AtomicBoolean(true), Float.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45.6badNumber", Float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("45.6badnumber"));
        }
    }

    @Test
    void testDouble()
    {
        assert -3.14d == this.converter.convert(-3.14d, double.class);
        assert -3.14d == this.converter.convert(-3.14d, Double.class);
        assert -3.14d == this.converter.convert("-3.14", double.class);
        assert -3.14d == this.converter.convert("-3.14", Double.class);
        assert -3.14d == this.converter.convert(new BigDecimal("-3.14"), double.class);
        assert -3.14d == this.converter.convert(new BigDecimal("-3.14"), Double.class);
        assert 1.0d == this.converter.convert(true, double.class);
        assert 1.0d == this.converter.convert(true, Double.class);
        assert 0.0d == this.converter.convert(false, double.class);
        assert 0.0d == this.converter.convert(false, Double.class);

        assert 0.0d == this.converter.convert(new AtomicInteger(0), double.class);
        assert 0.0d == this.converter.convert(new AtomicLong(0), double.class);
        assert 0.0d == this.converter.convert(new AtomicBoolean(false), Double.class);
        assert 1.0d == this.converter.convert(new AtomicBoolean(true), Double.class);

        try
        {
            this.converter.convert(TimeZone.getDefault(), double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [zoneinfo"));
        }

        try
        {
            this.converter.convert("45.6badNumber", Double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("45.6badnumber"));
        }
    }

    @Test
    void testBoolean()
    {
        assertEquals(true, this.converter.convert(-3.14d, boolean.class));
        assertEquals(false, this.converter.convert(0.0d, boolean.class));
        assertEquals(true, this.converter.convert(-3.14f, Boolean.class));
        assertEquals(false, this.converter.convert(0.0f, Boolean.class));

        assertEquals(false, this.converter.convert(new AtomicInteger(0), boolean.class));
        assertEquals(false, this.converter.convert(new AtomicLong(0), boolean.class));
        assertEquals(false, this.converter.convert(new AtomicBoolean(false), Boolean.class));
        assertEquals(true, this.converter.convert(new AtomicBoolean(true), Boolean.class));

        assertEquals(true, this.converter.convert("TRue", Boolean.class));
        assertEquals(true, this.converter.convert("true", Boolean.class));
        assertEquals(false, this.converter.convert("fALse", Boolean.class));
        assertEquals(false, this.converter.convert("false", Boolean.class));
        assertEquals(false, this.converter.convert("john", Boolean.class));

        assertEquals(true, this.converter.convert(true, Boolean.class));
        assertEquals(true, this.converter.convert(Boolean.TRUE, Boolean.class));
        assertEquals(false, this.converter.convert(false, Boolean.class));
        assertEquals(false, this.converter.convert(Boolean.FALSE, Boolean.class));

        try
        {
            this.converter.convert(new Date(), Boolean.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [date"));
        }
    }

    @Test
    void testAtomicBoolean()
    {
        assert (this.converter.convert(-3.14d, AtomicBoolean.class)).get();
        assert !(this.converter.convert(0.0d, AtomicBoolean.class)).get();
        assert (this.converter.convert(-3.14f, AtomicBoolean.class)).get();
        assert !(this.converter.convert(0.0f, AtomicBoolean.class)).get();

        assert !(this.converter.convert(new AtomicInteger(0), AtomicBoolean.class)).get();
        assert !(this.converter.convert(new AtomicLong(0), AtomicBoolean.class)).get();
        assert !(this.converter.convert(new AtomicBoolean(false), AtomicBoolean.class)).get();
        assert (this.converter.convert(new AtomicBoolean(true), AtomicBoolean.class)).get();

        assert (this.converter.convert("TRue", AtomicBoolean.class)).get();
        assert !(this.converter.convert("fALse", AtomicBoolean.class)).get();
        assert !(this.converter.convert("john", AtomicBoolean.class)).get();

        assert (this.converter.convert(true, AtomicBoolean.class)).get();
        assert (this.converter.convert(Boolean.TRUE, AtomicBoolean.class)).get();
        assert !(this.converter.convert(false, AtomicBoolean.class)).get();
        assert !(this.converter.convert(Boolean.FALSE, AtomicBoolean.class)).get();

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = this.converter.convert(b1, AtomicBoolean.class);
        assert b1 != b2; // ensure that it returns a different but equivalent instance
        assert b1.get() == b2.get();

        try {
            this.converter.convert(new Date(), AtomicBoolean.class);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [date"));
        }
    }

    @Test
    void testMapToAtomicBoolean()
    {
        final Map map = new HashMap();
        map.put("value", 57);
        AtomicBoolean ab = this.converter.convert(map, AtomicBoolean.class);
        assert ab.get();

        map.clear();
        map.put("value", "");
        ab = this.converter.convert(map, AtomicBoolean.class);
        assertFalse(ab.get());

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicBoolean.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicBoolean.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToAtomicInteger()
    {
        final Map map = new HashMap();
        map.put("value", 58);
        AtomicInteger ai = this.converter.convert(map, AtomicInteger.class);
        assert 58 == ai.get();

        map.clear();
        map.put("value", "");
        ai = this.converter.convert(map, AtomicInteger.class);
        assertEquals(new AtomicInteger(0).get(), ai.get());

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicInteger.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicInteger.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToAtomicLong()
    {
        final Map map = new HashMap();
        map.put("value", 58);
        AtomicLong al = this.converter.convert(map, AtomicLong.class);
        assert 58 == al.get();

        map.clear();
        map.put("value", "");
        al = this.converter.convert(map, AtomicLong.class);
        assert 0L == al.longValue();

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, AtomicLong.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, AtomicLong.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToCalendar()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", new Date(now));
        Calendar cal = this.converter.convert(map, Calendar.class);
        assert now == cal.getTimeInMillis();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Calendar.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Calendar.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time, zone], or '_v' or 'value'");
    }

    @Test
    void testMapToCalendarWithTimeZone()
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(now);

        final Map map = new HashMap();
        map.put("time", cal.getTimeInMillis());
        map.put("zone", cal.getTimeZone().getID());

        Calendar newCal = this.converter.convert(map, Calendar.class);
        assert cal.equals(newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToCalendarWithTimeNoZone()
    {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTimeInMillis(now);

        final Map map = new HashMap();
        map.put("time", cal.getTimeInMillis());

        Calendar newCal = this.converter.convert(map, Calendar.class);
        assert cal.equals(newCal);
        assert DeepEquals.deepEquals(cal, newCal);
    }

    @Test
    void testMapToGregCalendar()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", new Date(now));
        GregorianCalendar cal = this.converter.convert(map, GregorianCalendar.class);
        assert now == cal.getTimeInMillis();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, GregorianCalendar.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, GregorianCalendar.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, GregorianCalendar.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To convert from Map to Calendar, the map must include keys: [time, zone], or '_v' or 'value'");
    }

    @Test
    void testMapToDate() {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        Date date = this.converter.convert(map, Date.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Date.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Date.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time], or '_v' or 'value'");
    }

    @Test
    void testMapToSqlDate()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        java.sql.Date date = this.converter.convert(map, java.sql.Date.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, java.sql.Date.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, java.sql.Date.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, java.sql.Date.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time], or '_v' or 'value'");
    }

    @Test
    void testMapToTimestamp()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        Timestamp date = this.converter.convert(map, Timestamp.class);
        assert now == date.getTime();

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, Timestamp.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, Timestamp.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, Timestamp.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the map must include keys: [time, nanos], or '_v' or 'value'");
    }

    @Test
    void testMapToLocalDate()
    {
        LocalDate today = LocalDate.now();
        long now = today.toEpochDay();
        final Map map = new HashMap();
        map.put("value", now);
        LocalDate date = this.converter.convert(map, LocalDate.class);
        assert date.equals(today);

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, LocalDate.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, LocalDate.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, LocalDate.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to LocalDate, the map must include keys: [year, month, day], or '_v' or 'value'");
    }

    @Test
    void testMapToLocalDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        LocalDateTime ld = this.converter.convert(map, LocalDateTime.class);
        assert ld.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() == now;

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, LocalDateTime.class);

        map.clear();
        map.put("value", null);
        assert null == this.converter.convert(map, LocalDateTime.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, LocalDateTime.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to LocalDateTime, the map must include keys: '_v' or 'value'");
    }

    @Test
    void testMapToZonedDateTime()
    {
        long now = System.currentTimeMillis();
        final Map map = new HashMap();
        map.put("value", now);
        ZonedDateTime zd = this.converter.convert(map, ZonedDateTime.class);
        assert zd.toInstant().toEpochMilli() == now;

        map.clear();
        map.put("value", "");
        assert null == this.converter.convert(map, ZonedDateTime.class);

        map.clear();
        assertThatThrownBy(() -> this.converter.convert(map, ZonedDateTime.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map to ZonedDateTime, the map must include keys: '_v' or 'value'");

    }

    @Test
    void testUnsupportedType()
    {
        try
        {
            this.converter.convert("Lamb", TimeZone.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported conversion, source type [string"));
        }
    }

    @Test
    void testNullInstance()
    {
        assert 0L == this.converter.convert(null, long.class);
        assert !this.converter.convert(null, boolean.class);
        assert null == this.converter.convert(null, Boolean.class);
        assert 0 == this.converter.convert(null, byte.class);
        assert null == this.converter.convert(null, Byte.class);
        assert 0 == this.converter.convert(null, short.class);
        assert null == this.converter.convert(null, Short.class);
        assert 0 == this.converter.convert(null, int.class);
        assert null == this.converter.convert(null, Integer.class);
        assert null == this.converter.convert(null, Long.class);
        assert 0.0f == this.converter.convert(null, float.class);
        assert null == this.converter.convert(null, Float.class);
        assert 0.0d == this.converter.convert(null, double.class);
        assert null == this.converter.convert(null, Double.class);
        assert (char) 0 == this.converter.convert(null, char.class);
        assert null == this.converter.convert(null, Character.class);

        assert null == this.converter.convert(null, Date.class);
        assert null == this.converter.convert(null, java.sql.Date.class);
        assert null == this.converter.convert(null, Timestamp.class);
        assert null == this.converter.convert(null, Calendar.class);
        assert null == this.converter.convert(null, String.class);
        assert null == this.converter.convert(null, BigInteger.class);
        assert null == this.converter.convert(null, BigDecimal.class);
        assert null == this.converter.convert(null, AtomicBoolean.class);
        assert null == this.converter.convert(null, AtomicInteger.class);
        assert null == this.converter.convert(null, AtomicLong.class);

        assert null == this.converter.convert(null, Byte.class);
        assert null == this.converter.convert(null, Integer.class);
        assert null == this.converter.convert(null, Short.class);
        assert null == this.converter.convert(null, Long.class);
        assert null == this.converter.convert(null, Float.class);
        assert null == this.converter.convert(null, Double.class);
        assert null == this.converter.convert(null, Character.class);
        assert null == this.converter.convert(null, Date.class);
        assert null == this.converter.convert(null, java.sql.Date.class);
        assert null == this.converter.convert(null, Timestamp.class);
        assert null == this.converter.convert(null, AtomicBoolean.class);
        assert null == this.converter.convert(null, AtomicInteger.class);
        assert null == this.converter.convert(null, AtomicLong.class);
        assert null == this.converter.convert(null, String.class);

        assert false == this.converter.convert(null, boolean.class);
        assert 0 == this.converter.convert(null, byte.class);
        assert 0 == this.converter.convert(null, int.class);
        assert 0 == this.converter.convert(null, short.class);
        assert 0 == this.converter.convert(null, long.class);
        assert 0.0f == this.converter.convert(null, float.class);
        assert 0.0d == this.converter.convert(null, double.class);
        assert (char) 0 == this.converter.convert(null, char.class);
        assert null == this.converter.convert(null, BigInteger.class);
        assert null == this.converter.convert(null, BigDecimal.class);
        assert null == this.converter.convert(null, AtomicBoolean.class);
        assert null == this.converter.convert(null, AtomicInteger.class);
        assert null == this.converter.convert(null, AtomicLong.class);
        assert null == this.converter.convert(null, String.class);
    }

    @Test
    void testConvert2()
    {
        assert !this.converter.convert(null, boolean.class);
        assert this.converter.convert("true", boolean.class);
        assert this.converter.convert("true", Boolean.class);
        assert !this.converter.convert("false", boolean.class);
        assert !this.converter.convert("false", Boolean.class);
        assert !this.converter.convert("", boolean.class);
        assert !this.converter.convert("", Boolean.class);
        assert null == this.converter.convert(null, Boolean.class);
        assert -8 == this.converter.convert("-8", byte.class);
        assert -8 == this.converter.convert("-8", int.class);
        assert -8 == this.converter.convert("-8", short.class);
        assert -8 == this.converter.convert("-8", long.class);
        assert -8.0f == this.converter.convert("-8", float.class);
        assert -8.0d == this.converter.convert("-8", double.class);
        assert 'A' == this.converter.convert(65, char.class);
        assert new BigInteger("-8").equals(this.converter.convert("-8", BigInteger.class));
        assert new BigDecimal(-8.0d).equals(this.converter.convert("-8", BigDecimal.class));
        assert this.converter.convert("true", AtomicBoolean.class).get();
        assert -8 == this.converter.convert("-8", AtomicInteger.class).get();
        assert -8L == this.converter.convert("-8", AtomicLong.class).get();
        assert "-8".equals(this.converter.convert(-8, String.class));
    }

    @Test
    void testNullType()
    {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.converter.convert("123", null))
                // No Message was coming through here and receiving NullPointerException -- changed to convention over in convert -- hopefully that's what you had in mind.
                .withMessageContaining("toType cannot be null");
    }

    @Test
    void testEmptyString()
    {
        assertEquals(false, this.converter.convert("", boolean.class));
        assertEquals(false, this.converter.convert("", boolean.class));
        assert (byte) 0 == this.converter.convert("", byte.class);
        assert (short) 0 == this.converter.convert("", short.class);
        assert 0 == this.converter.convert("", int.class);
        assert (long) 0 == this.converter.convert("", long.class);
        assert 0.0f == this.converter.convert("", float.class);
        assert 0.0d == this.converter.convert("", double.class);
        assertEquals(BigDecimal.ZERO, this.converter.convert("", BigDecimal.class));
        assertEquals(BigInteger.ZERO, this.converter.convert("", BigInteger.class));
        assertEquals(false, this.converter.convert("", AtomicBoolean.class).get());
        assertEquals(0, this.converter.convert("", AtomicInteger.class).get());
        assertEquals(0L, this.converter.convert("", AtomicLong.class).get());
    }

    @Test
    void testEnumSupport()
    {
        assertEquals("foo", this.converter.convert(foo, String.class));
        assertEquals("bar", this.converter.convert(bar, String.class));
    }

    @Test
    void testCharacterSupport()
    {
        assert 65 == this.converter.convert('A', Byte.class);
        assert 65 == this.converter.convert('A', byte.class);
        assert 65 == this.converter.convert('A', Short.class);
        assert 65 == this.converter.convert('A', short.class);
        assert 65 == this.converter.convert('A', Integer.class);
        assert 65 == this.converter.convert('A', int.class);
        assert 65 == this.converter.convert('A', Long.class);
        assert 65 == this.converter.convert('A', long.class);
        assert 65 == this.converter.convert('A', BigInteger.class).longValue();
        assert 65 == this.converter.convert('A', BigDecimal.class).longValue();

        assert '1' == this.converter.convert(true, char.class);
        assert '0' == this.converter.convert(false, char.class);
        assert '1' == this.converter.convert(new AtomicBoolean(true), char.class);
        assert '0' == this.converter.convert(new AtomicBoolean(false), char.class);
        assert 'z' == this.converter.convert('z', char.class);
        assert 0 == this.converter.convert("", char.class);
        assert 0 == this.converter.convert("", Character.class);
        assert 'A' == this.converter.convert("65", char.class);
        assert 'A' == this.converter.convert("65", Character.class);
        try
        {
            this.converter.convert("This is not a number", char.class);
            fail();
        }
        catch (IllegalArgumentException e) { }
        try
        {
            this.converter.convert(new Date(), char.class);
            fail();
        }
        catch (IllegalArgumentException e) { }

        assertThatThrownBy(() -> this.converter.convert(Long.MAX_VALUE, char.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value: 9223372036854775807 out of range to be converted to character");
    }

    @Test
    void testConvertUnknown()
    {
        try
        {
            this.converter.convert(TimeZone.getDefault(), String.class);
            fail();
        }
        catch (IllegalArgumentException e) { }
    }

    @Test
    void testLongToBigDecimal()
    {
        BigDecimal big = this.converter.convert(7L, BigDecimal.class);
        assert big instanceof BigDecimal;
        assert big.longValue() == 7L;

        big = this.converter.convert(null, BigDecimal.class);
        assert big == null;
    }

    @Test
    void testLocalDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, 8, 4);   // 0-based for month

        BigDecimal big = this.converter.convert(LocalDate.of(2020, 9, 4), BigDecimal.class);
        LocalDate out = LocalDate.ofEpochDay(big.longValue());
        assert out.getYear() == 2020;
        assert out.getMonthValue() == 9;
        assert out.getDayOfMonth() == 4;

        BigInteger bigI = this.converter.convert(LocalDate.of(2020, 9, 4), BigInteger.class);
        out = LocalDate.ofEpochDay(bigI.longValue());
        assert out.getYear() == 2020;
        assert out.getMonthValue() == 9;
        assert out.getDayOfMonth() == 4;

        java.sql.Date sqlDate = this.converter.convert(LocalDate.of(2020, 9, 4), java.sql.Date.class);
        assert sqlDate.getTime() == cal.getTime().getTime();

        Timestamp timestamp = this.converter.convert(LocalDate.of(2020, 9, 4), Timestamp.class);
        assert timestamp.getTime() == cal.getTime().getTime();

        Date date = this.converter.convert(LocalDate.of(2020, 9, 4), Date.class);
        assert date.getTime() == cal.getTime().getTime();

        LocalDate particular = LocalDate.of(2020, 9, 4);
        Long lng = this.converter.convert(LocalDate.of(2020, 9, 4), Long.class);
        LocalDate xyz = LocalDate.ofEpochDay(lng);
        assertEquals(xyz, particular);

        AtomicLong atomicLong = this.converter.convert(LocalDate.of(2020, 9, 4), AtomicLong.class);
        out = LocalDate.ofEpochDay(atomicLong.longValue());
        assert out.getYear() == 2020;
        assert out.getMonthValue() == 9;
        assert out.getDayOfMonth() == 4;
    }

    @Test
    void testLocalDateTimeToBig()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, 8, 8, 13, 11, 1);   // 0-based for month

        BigDecimal big = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), BigDecimal.class);
        assert big.longValue() == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime();

        java.sql.Date sqlDate = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), java.sql.Date.class);
        assert sqlDate.getTime() == cal.getTime().getTime();

        Timestamp timestamp = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Timestamp.class);
        assert timestamp.getTime() == cal.getTime().getTime();

        Date date = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Date.class);
        assert date.getTime() == cal.getTime().getTime();

        Long lng = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), Long.class);
        assert lng == cal.getTime().getTime();

        AtomicLong atomicLong = this.converter.convert(LocalDateTime.of(2020, 9, 8, 13, 11, 1), AtomicLong.class);
        assert atomicLong.get() == cal.getTime().getTime();
    }

    @Test
    void testLocalZonedDateTimeToBig()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, 8, 8, 13, 11, 1);   // 0-based for month

        BigDecimal big = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), BigDecimal.class);
        assert big.longValue() == cal.getTime().getTime();

        BigInteger bigI = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), BigInteger.class);
        assert bigI.longValue() == cal.getTime().getTime();

        java.sql.Date sqlDate = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), java.sql.Date.class);
        assert sqlDate.getTime() == cal.getTime().getTime();

        Date date = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), Date.class);
        assert date.getTime() == cal.getTime().getTime();

        AtomicLong atomicLong = this.converter.convert(ZonedDateTime.of(2020, 9, 8, 13, 11, 1, 0, ZoneId.systemDefault()), AtomicLong.class);
        assert atomicLong.get() == cal.getTime().getTime();
    }

    @Test
    void testStringToClass()
    {
        Class<?> clazz = this.converter.convert("java.math.BigInteger", Class.class);
        assert clazz.getName().equals("java.math.BigInteger");

        assertThatThrownBy(() -> this.converter.convert("foo.bar.baz.Qux", Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert String 'foo.bar.baz.Qux' to class.  Class not found");

        assertNull(this.converter.convert(null, Class.class));

        assertThatThrownBy(() -> this.converter.convert(16.0, Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Double (16.0)] target type 'Class'");
    }

    @Test
    void testClassToClass()
    {
        Class<?> clazz = this.converter.convert(ConverterTest.class, Class.class);
        assert clazz.getName() == ConverterTest.class.getName();
    }

    @Test
    void testStringToUUID()
    {
        UUID uuid = this.converter.convert("00000000-0000-0000-0000-000000000064", UUID.class);
        BigInteger bigInt = this.converter.convert(uuid, BigInteger.class);
        assert bigInt.intValue() == 100;

        assertThatThrownBy(() -> this.converter.convert("00000000", UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID string: 00000000");
    }

    @Test
    void testUUIDToUUID()
    {
        UUID uuid = this.converter.convert("00000007-0000-0000-0000-000000000064", UUID.class);
        UUID uuid2 = this.converter.convert(uuid, UUID.class);
        assert uuid.equals(uuid2);
    }

    @Test
    void testBogusToUUID()
    {
        assertThatThrownBy(() -> this.converter.convert((short) 77, UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Short (77)] target type 'UUID'");
    }

    @Test
    void testBigIntegerToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        BigInteger hundred = this.converter.convert(uuid, BigInteger.class);
        assert hundred.intValue() == 100;
    }

    @Test
    void testBigDecimalToUUID()
    {
        UUID uuid = this.converter.convert(new BigDecimal("100"), UUID.class);
        BigDecimal hundred = this.converter.convert(uuid, BigDecimal.class);
        assert hundred.intValue() == 100;

        uuid = this.converter.convert(new BigDecimal("100.4"), UUID.class);
        hundred = this.converter.convert(uuid, BigDecimal.class);
        assert hundred.intValue() == 100;
    }

    @Test
    void testUUIDToBigInteger()
    {
        BigInteger bigInt = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000064"), BigInteger.class);
        assert bigInt.intValue() == 100;

        bigInt = this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), BigInteger.class);
        assert bigInt.toString().equals("-18446744073709551617");

        bigInt = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), BigInteger.class);
        assert bigInt.intValue() == 0;

        assertThatThrownBy(() -> this.converter.convert(16.0, Class.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [Double (16.0)] target type 'Class'");
    }

    @Test
    void testUUIDToBigDecimal()
    {
        BigDecimal bigDec = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000064"), BigDecimal.class);
        assert bigDec.intValue() == 100;

        bigDec = this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), BigDecimal.class);
        assert bigDec.toString().equals("-18446744073709551617");

        bigDec = this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), BigDecimal.class);
        assert bigDec.intValue() == 0;
    }

    @Test
    void testMapToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", uuid.getMostSignificantBits());
        map.put("leastSigBits", uuid.getLeastSignificantBits());
        UUID hundred = this.converter.convert(map, UUID.class);
        assertEquals("00000000-0000-0000-0000-000000000064", hundred.toString());
    }

    @Test
    void testBadMapToUUID()
    {
        UUID uuid = this.converter.convert(new BigInteger("100"), UUID.class);
        Map<String, Object> map = new HashMap<>();
        map.put("leastSigBits", uuid.getLeastSignificantBits());
        assertThatThrownBy(() -> this.converter.convert(map, UUID.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("To convert Map to UUID, the Map must contain both 'mostSigBits' and 'leastSigBits' keys");
    }

    @Test
    void testClassToString()
    {
        String str = this.converter.convert(BigInteger.class, String.class);
        assert str.equals("java.math.BigInteger");

        str = this.converter.convert(null, String.class);
        assert str == null;
    }

    @Test
    void testSqlDateToString()
    {
        long now = System.currentTimeMillis();
        java.sql.Date date = new java.sql.Date(now);
        String strDate = this.converter.convert(date, String.class);
        Date x = this.converter.convert(strDate, Date.class);
        LocalDate l1 = this.converter.convert(date, LocalDate.class);
        LocalDate l2 = this.converter.convert(x, LocalDate.class);
        assertEquals(l1, l2);
    }

    @Test
    void tesTimestampToString()
    {
        long now = System.currentTimeMillis();
        Timestamp date = new Timestamp(now);
        String strDate = this.converter.convert(date, String.class);
        Date x = this.converter.convert(strDate, Date.class);
        String str2Date = this.converter.convert(x, String.class);
        assertEquals(str2Date, strDate);
    }

    @Test
    void testByteToMap()
    {
        byte b1 = (byte) 16;
        Map<?, ?> map = this.converter.convert(b1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (byte)16);
        assert map.get(Converter.VALUE).getClass().equals(Byte.class);

        Byte b2 = (byte) 16;
        map = this.converter.convert(b2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (byte)16);
        assert map.get(Converter.VALUE).getClass().equals(Byte.class);
    }

    @Test
    void testShortToMap()
    {
        short s1 = (short) 1600;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (short)1600);
        assert map.get(Converter.VALUE).getClass().equals(Short.class);

        Short s2 = (short) 1600;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), (short)1600);
        assert map.get(Converter.VALUE).getClass().equals(Short.class);
    }

    @Test
    void testIntegerToMap()
    {
        int s1 = 1234567;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 1234567);
        assert map.get(Converter.VALUE).getClass().equals(Integer.class);

        Integer s2 = 1234567;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 1234567);
        assert map.get(Converter.VALUE).getClass().equals(Integer.class);
    }

    @Test
    void testLongToMap()
    {
        long s1 = 123456789012345L;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 123456789012345L);
        assert map.get(Converter.VALUE).getClass().equals(Long.class);

        Long s2 = 123456789012345L;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 123456789012345L);
        assert map.get(Converter.VALUE).getClass().equals(Long.class);
    }

    @Test
    void testFloatToMap()
    {
        float s1 = 3.141592f;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.141592f);
        assert map.get(Converter.VALUE).getClass().equals(Float.class);

        Float s2 = 3.141592f;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.141592f);
        assert map.get(Converter.VALUE).getClass().equals(Float.class);
    }

    @Test
    void testDoubleToMap()
    {
        double s1 = 3.14159265358979d;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.14159265358979d);
        assert map.get(Converter.VALUE).getClass().equals(Double.class);

        Double s2 = 3.14159265358979d;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 3.14159265358979d);
        assert map.get(Converter.VALUE).getClass().equals(Double.class);
    }

    @Test
    void testBooleanToMap()
    {
        boolean s1 = true;
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), true);
        assert map.get(Converter.VALUE).getClass().equals(Boolean.class);

        Boolean s2 = true;
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), true);
        assert map.get(Converter.VALUE).getClass().equals(Boolean.class);
    }

    @Test
    void testCharacterToMap()
    {
        char s1 = 'e';
        Map<?, ?> map = this.converter.convert(s1, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 'e');
        assert map.get(Converter.VALUE).getClass().equals(Character.class);

        Character s2 = 'e';
        map = this.converter.convert(s2, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), 'e');
        assert map.get(Converter.VALUE).getClass().equals(Character.class);
    }

    @Test
    void testBigIntegerToMap()
    {
        BigInteger bi = BigInteger.valueOf(1234567890123456L);
        Map<?, ?> map = this.converter.convert(bi, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), bi);
        assert map.get(Converter.VALUE).getClass().equals(BigInteger.class);
    }

    @Test
    void testBigDecimalToMap()
    {
        BigDecimal bd = new BigDecimal("3.1415926535897932384626433");
        Map<?, ?> map = this.converter.convert(bd, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), bd);
        assert map.get(Converter.VALUE).getClass().equals(BigDecimal.class);
    }

    @Test
    void testAtomicBooleanToMap()
    {
        AtomicBoolean ab = new AtomicBoolean(true);
        Map<?, ?> map = this.converter.convert(ab, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), ab);
        assert map.get(Converter.VALUE).getClass().equals(AtomicBoolean.class);
    }

    @Test
    void testAtomicIntegerToMap()
    {
        AtomicInteger ai = new AtomicInteger(123456789);
        Map<?, ?> map = this.converter.convert(ai, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), ai);
        assert map.get(Converter.VALUE).getClass().equals(AtomicInteger.class);
    }

    @Test
    void testAtomicLongToMap()
    {
        AtomicLong al = new AtomicLong(12345678901234567L);
        Map<?, ?> map = this.converter.convert(al, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), al);
        assert map.get(Converter.VALUE).getClass().equals(AtomicLong.class);
    }

    @Test
    void testClassToMap()
    {
        Class<?> clazz = ConverterTest.class;
        Map<?, ?> map = this.converter.convert(clazz, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), clazz);
    }

    @Test
    void testUUIDToMap()
    {
        UUID uuid = new UUID(1L, 2L);
        Map<?, ?> map = this.converter.convert(uuid, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), uuid);
        assert map.get(Converter.VALUE).getClass().equals(UUID.class);
    }

    @Test
    void testCalendarToMap()
    {
        Calendar cal = Calendar.getInstance();
        Map<?, ?> map = this.converter.convert(cal, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), cal);
        assert map.get(Converter.VALUE) instanceof Calendar;
    }

    @Test
    void testDateToMap()
    {
        Date now = new Date();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(Date.class);
    }

    @Test
    void testSqlDateToMap()
    {
        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(java.sql.Date.class);
    }

    @Test
    void testTimestampToMap()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(Timestamp.class);
    }

    @Test
    void testLocalDateToMap()
    {
        LocalDate now = LocalDate.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(LocalDate.class);
    }

    @Test
    void testLocalDateTimeToMap()
    {
        LocalDateTime now = LocalDateTime.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(LocalDateTime.class);
    }

    @Test
    void testZonedDateTimeToMap()
    {
        ZonedDateTime now = ZonedDateTime.now();
        Map<?, ?> map = this.converter.convert(now, Map.class);
        assert map.size() == 1;
        assertEquals(map.get(Converter.VALUE), now);
        assert map.get(Converter.VALUE).getClass().equals(ZonedDateTime.class);
    }

    @Test
    void testUnknownType()
    {
        assertThatThrownBy(() -> this.converter.convert(null, Collection.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [null] target type 'Collection'");
    }

    @Test
    void testGetSupportedConversions()
    {
        Map map = this.converter.getSupportedConversions();
        assert map.size() > 10;
    }

    @Test
    void testAllSupportedConversions()
    {
        Map map = this.converter.allSupportedConversions();
        assert map.size() > 10;
    }

    @Test
    void testIsConversionSupport()
    {
        assert this.converter.isConversionSupportedFor(int.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(Integer.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(LocalDate.class, int.class);
        assert this.converter.isConversionSupportedFor(LocalDate.class, Integer.class);

        assert !this.converter.isDirectConversionSupportedFor(byte.class, LocalDate.class);
        assert this.converter.isConversionSupportedFor(byte.class, LocalDate.class);       // byte is upgraded to Byte, which is found as Number.

        assert this.converter.isConversionSupportedFor(Byte.class, LocalDate.class);       // Number is supported
        assert !this.converter.isDirectConversionSupportedFor(Byte.class, LocalDate.class);
        assert !this.converter.isConversionSupportedFor(LocalDate.class, byte.class);
        assert !this.converter.isConversionSupportedFor(LocalDate.class, Byte.class);

        assert this.converter.isConversionSupportedFor(UUID.class, String.class);
        assert this.converter.isConversionSupportedFor(UUID.class, Map.class);
        assert this.converter.isConversionSupportedFor(UUID.class, BigDecimal.class);
        assert this.converter.isConversionSupportedFor(UUID.class, BigInteger.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, long.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, Long.class);

        assert this.converter.isConversionSupportedFor(String.class, UUID.class);
        assert this.converter.isConversionSupportedFor(Map.class, UUID.class);
        assert this.converter.isConversionSupportedFor(BigDecimal.class, UUID.class);
        assert this.converter.isConversionSupportedFor(BigInteger.class, UUID.class);
    }

    static class DumbNumber extends BigInteger
    {
        DumbNumber(String val) {
            super(val);
        }

        public String toString() {
            return super.toString();
        }
    }

    @Test
    void testDumbNumberToByte()
    {
        DumbNumber dn = new DumbNumber("25");
        byte x = this.converter.convert(dn, byte.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToShort()
    {
        DumbNumber dn = new DumbNumber("25");
        short x = this.converter.convert(dn, short.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToShort2()
    {
        DumbNumber dn = new DumbNumber("25");
        Short x = this.converter.convert(dn, Short.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToInt()
    {
        DumbNumber dn = new DumbNumber("25");
        int x = this.converter.convert(dn, int.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToLong()
    {
        DumbNumber dn = new DumbNumber("25");
        long x = this.converter.convert(dn, long.class);
        assert x == 25;
    }

    @Test
    void testDumbNumberToFloat()
    {
        DumbNumber dn = new DumbNumber("3");
        float x = this.converter.convert(dn, float.class);
        assert x == 3;
    }

    @Test
    void testDumbNumberToDouble()
    {
        DumbNumber dn = new DumbNumber("3");
        double x = this.converter.convert(dn, double.class);
        assert x == 3;
    }

    @Test
    void testDumbNumberToBoolean()
    {
        DumbNumber dn = new DumbNumber("3");
        boolean x = this.converter.convert(dn, boolean.class);
        assert x;
    }

    @Test
    void testDumbNumberToCharacter()
    {
        DumbNumber dn = new DumbNumber("3");
        char x = this.converter.convert(dn, char.class);
        assert x == '\u0003';
    }

    @Test
    void testDumbNumberToBigInteger()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        BigInteger x = this.converter.convert(dn, BigInteger.class);
        assert x.toString().equals(dn.toString());
    }

    @Test
    void testDumbNumberToBigDecimal()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        BigDecimal x = this.converter.convert(dn, BigDecimal.class);
        assert x.toString().equals(dn.toString());
    }

    @Test
    void testDumbNumberToString()
    {
        DumbNumber dn = new DumbNumber("12345678901234567890");
        String x = this.converter.convert(dn, String.class);
        assert x.toString().equals("12345678901234567890");
    }

    @Test
    void testDumbNumberToUUIDProvesInheritance()
    {
        assert this.converter.isConversionSupportedFor(DumbNumber.class, UUID.class);
        assert !this.converter.isDirectConversionSupportedFor(DumbNumber.class, UUID.class);

        DumbNumber dn = new DumbNumber("1000");

        // Converts because DumbNumber inherits from Number.
        UUID uuid = this.converter.convert(dn, UUID.class);
        assert uuid.toString().equals("00000000-0000-0000-0000-0000000003e8");

        // Add in conversion
        this.converter.addConversion(DumbNumber.class, UUID.class, (fromInstance, converter, options) -> {
            DumbNumber bigDummy = (DumbNumber) fromInstance;
            BigInteger mask = BigInteger.valueOf(Long.MAX_VALUE);
            long mostSignificantBits = bigDummy.shiftRight(64).and(mask).longValue();
            long leastSignificantBits = bigDummy.and(mask).longValue();
            return new UUID(mostSignificantBits, leastSignificantBits);
        });

        // Still converts, but not using inheritance.
        uuid = this.converter.convert(dn, UUID.class);
        assert uuid.toString().equals("00000000-0000-0000-0000-0000000003e8");

        assert this.converter.isConversionSupportedFor(DumbNumber.class, UUID.class);
        assert this.converter.isDirectConversionSupportedFor(DumbNumber.class, UUID.class);
    }

    @Test
    void testUUIDtoDumbNumber()
    {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-0000000003e8");

        Object o = this.converter.convert(uuid, DumbNumber.class);
        assert o instanceof BigInteger;
        assert 1000L == ((Number) o).longValue();

        // Add in conversion
        this.converter.addConversion(UUID.class, DumbNumber.class, (fromInstance, converter, options) -> {
            UUID uuid1 = (UUID) fromInstance;
            BigInteger mostSignificant = BigInteger.valueOf(uuid1.getMostSignificantBits());
            BigInteger leastSignificant = BigInteger.valueOf(uuid1.getLeastSignificantBits());
            // Shift the most significant bits to the left and add the least significant bits
            return new DumbNumber(mostSignificant.shiftLeft(64).add(leastSignificant).toString());
        });

        // Converts!
        DumbNumber dn = this.converter.convert(uuid, DumbNumber.class);
        assert dn.toString().equals("1000");

        assert this.converter.isConversionSupportedFor(UUID.class, DumbNumber.class);
    }

    @Test
    void testUUIDtoBoolean()
    {
        assert !this.converter.isConversionSupportedFor(UUID.class, boolean.class);
        assert !this.converter.isConversionSupportedFor(UUID.class, Boolean.class);

        assert !this.converter.isConversionSupportedFor(boolean.class, UUID.class);
        assert !this.converter.isConversionSupportedFor(Boolean.class, UUID.class);

        final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThatThrownBy(() -> this.converter.convert(uuid, boolean.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported conversion, source type [UUID (00000000-0000-0000-0000-000000000000)] target type 'Boolean'");

        // Add in conversions
        this.converter.addConversion(UUID.class, boolean.class, (fromInstance, converter, options) -> {
            UUID uuid1 = (UUID) fromInstance;
            return !"00000000-0000-0000-0000-000000000000".equals(uuid1.toString());
        });

        // Add in conversions
        this.converter.addConversion(boolean.class, UUID.class, (fromInstance, converter, options) -> {
            boolean state = (Boolean)fromInstance;
            if (state) {
                return "00000000-0000-0000-0000-000000000001";
            } else {
                return "00000000-0000-0000-0000-000000000000";
            }
        });

        // Converts!
        assert !this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000000"), boolean.class);
        assert this.converter.convert(UUID.fromString("00000000-0000-0000-0000-000000000001"), boolean.class);
        assert this.converter.convert(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), boolean.class);

        assert this.converter.isConversionSupportedFor(UUID.class, boolean.class);
        assert this.converter.isConversionSupportedFor(UUID.class, Boolean.class);

        assert this.converter.isConversionSupportedFor(boolean.class, UUID.class);
        assert this.converter.isConversionSupportedFor(Boolean.class, UUID.class);
    }

    @Test
    void testBooleanToUUID()
    {

    }

    static class Normie
    {
        String name;

        Normie(String name) {
            this.name = name;
        }

        void setName(String name)
        {
            this.name = name;
        }
    }

    static class Weirdo
    {
        String name;

        Weirdo(String name)
        {
            this.name = reverseString(name);
        }

        void setName(String name)
        {
            this.name = reverseString(name);
        }
    }

    static String reverseString(String in)
    {
        StringBuilder reversed = new StringBuilder();
        for (int i = in.length() - 1; i >= 0; i--) {
            reversed.append(in.charAt(i));
        }
        return reversed.toString();
    }

    @Test
    void testNormieToWeirdoAndBack()
    {
        this.converter.addConversion(Normie.class, Weirdo.class, (fromInstance, converter, options) -> {
            Normie normie = (Normie) fromInstance;
            Weirdo weirdo = new Weirdo(normie.name);
            return weirdo;
        });

        this.converter.addConversion(Weirdo.class, Normie.class, (fromInstance, converter, options) -> {
            Weirdo weirdo = (Weirdo) fromInstance;
            Normie normie = new Normie(reverseString(weirdo.name));
            return normie;
        });

        Normie normie = new Normie("Joe");
        Weirdo weirdo = this.converter.convert(normie, Weirdo.class);
        assertEquals(weirdo.name, "eoJ");

        weirdo = new Weirdo("Jacob");
        assertEquals(weirdo.name, "bocaJ");
        normie = this.converter.convert(weirdo, Normie.class);
        assertEquals(normie.name, "Jacob");

        assert this.converter.isConversionSupportedFor(Normie.class, Weirdo.class);
        assert this.converter.isConversionSupportedFor(Weirdo.class, Normie.class);
    }
}
