package com.cedarsoftware.util;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.Converter.*;
import static com.cedarsoftware.util.TestConverter.fubar.bar;
import static com.cedarsoftware.util.TestConverter.fubar.foo;
import static org.junit.Assert.*;

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
public class TestConverter
{
    enum fubar
    {
        foo, bar, baz, quz
    }

    @Test
    public void testConstructorIsPrivateAndClassIsFinal() throws Exception
    {
        Class c = Converter.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<StringUtilities> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }
    
    @Test
    public void testByte()
    {
        Byte x = convert("-25", byte.class);
        assert -25 == x;
        x = convert("24", Byte.class);
        assert 24 == x;

        x = convert((byte) 100, byte.class);
        assert 100 == x;
        x = convert((byte) 120, Byte.class);
        assert 120 == x;

        x = convert(new BigDecimal("100"), byte.class);
        assert 100 == x;
        x = convert(new BigInteger("120"), Byte.class);
        assert 120 == x;

        Byte value = convert(true, Byte.class);
        assert value == 1;
        assert (byte)1 == convert(true, Byte.class);
        assert (byte)0 == convert(false, byte.class);

        assert (byte)25 == convert(new AtomicInteger(25), byte.class);
        assert (byte)100 == convert(new AtomicLong(100L), byte.class);
        assert (byte)1 == convert(new AtomicBoolean(true), byte.class);
        assert (byte)0 == convert(new AtomicBoolean(false), byte.class);

        byte z = convert2byte("11.5");
        assert z == 11;

        try
        {
            convert(TimeZone.getDefault(), byte.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", byte.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        try
        {
            convert2byte("257");
            fail();
        }
        catch (IllegalArgumentException e) { }
    }
    
    @Test
    public void testShort()
    {
        Short x = convert("-25000", short.class);
        assert -25000 == x;
        x = convert("24000", Short.class);
        assert 24000 == x;

        x = convert((short) 10000, short.class);
        assert 10000 == x;
        x = convert((short) 20000, Short.class);
        assert 20000 == x;

        x = convert(new BigDecimal("10000"), short.class);
        assert 10000 == x;
        x = convert(new BigInteger("20000"), Short.class);
        assert 20000 == x;

        assert (short)1 == convert(true, short.class);
        assert (short)0 == convert(false, Short.class);

        assert (short)25 == convert(new AtomicInteger(25), short.class);
        assert (short)100 == convert(new AtomicLong(100L), Short.class);
        assert (short)1 == convert(new AtomicBoolean(true), Short.class);
        assert (short)0 == convert(new AtomicBoolean(false), Short.class);

        int z = convert2short("11.5");
        assert z == 11;

        try
        {
            convert(TimeZone.getDefault(), short.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", short.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        try
        {
            convert2short("33000");
            fail();
        }
        catch (IllegalArgumentException e) { }

    }

    @Test
    public void testInt()
    {
        Integer x = convert("-450000", int.class);
        assertEquals((Object) (-450000), x);
        x = convert("550000", Integer.class);
        assertEquals((Object) 550000, x);

        x = convert(100000, int.class);
        assertEquals((Object) 100000, x);
        x = convert(200000, Integer.class);
        assertEquals((Object) 200000, x);

        x = convert(new BigDecimal("100000"), int.class);
        assertEquals((Object) 100000, x);
        x = convert(new BigInteger("200000"), Integer.class);
        assertEquals((Object) 200000, x);

        assert 1 == convert(true, Integer.class);
        assert 0 == convert(false, int.class);

        assert 25 == convert(new AtomicInteger(25), int.class);
        assert 100 == convert(new AtomicLong(100L), Integer.class);
        assert 1 == convert(new AtomicBoolean(true), Integer.class);
        assert 0 == convert(new AtomicBoolean(false), Integer.class);

        int z = convert2int("11.5");
        assert z == 11;
        
        try
        {
            convert(TimeZone.getDefault(), int.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", int.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        try
        {
            convert2int("2147483649");
            fail();
        }
        catch (IllegalArgumentException e) { }

    }

    @Test
    public void testLong()
    {
        Long x = convert("-450000", long.class);
        assertEquals((Object)(-450000L), x);
        x = convert("550000", Long.class);
        assertEquals((Object)550000L, x);

        x = convert(100000L, long.class);
        assertEquals((Object)100000L, x);
        x = convert(200000L, Long.class);
        assertEquals((Object)200000L, x);

        x = convert(new BigDecimal("100000"), long.class);
        assertEquals((Object)100000L, x);
        x = convert(new BigInteger("200000"), Long.class);
        assertEquals((Object)200000L, x);

        assert (long)1 == convert(true, long.class);
        assert (long)0 == convert(false, Long.class);

        Date now = new Date();
        long now70 = now.getTime();
        assert now70 == convert(now, long.class);

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        assert now70 == convert(today, Long.class);

        assert 25L == convert(new AtomicInteger(25), long.class);
        assert 100L == convert(new AtomicLong(100L), Long.class);
        assert 1L == convert(new AtomicBoolean(true), Long.class);
        assert 0L == convert(new AtomicBoolean(false), Long.class);

        long z = convert2int("11.5");
        assert z == 11;

        try
        {
            convert(TimeZone.getDefault(), long.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", long.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testAtomicLong()
    {
        AtomicLong x = convert("-450000", AtomicLong.class);
        assertEquals(-450000L, x.get());
        x = convert("550000", AtomicLong.class);
        assertEquals(550000L, x.get());

        x = convert(100000L, AtomicLong.class);
        assertEquals(100000L, x.get());
        x = convert(200000L, AtomicLong.class);
        assertEquals(200000L, x.get());

        x = convert(new BigDecimal("100000"), AtomicLong.class);
        assertEquals(100000L, x.get());
        x = convert(new BigInteger("200000"), AtomicLong.class);
        assertEquals(200000L, x.get());

        x = convert(true, AtomicLong.class);
        assertEquals((long)1, x.get());
        x = convert(false, AtomicLong.class);
        assertEquals((long)0, x.get());

        Date now = new Date();
        long now70 = now.getTime();
        x =  convert(now, AtomicLong.class);
        assertEquals(now70, x.get());

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        x =  convert(today, AtomicLong.class);
        assertEquals(now70, x.get());

        x = convert(new AtomicInteger(25), AtomicLong.class);
        assertEquals(25L, x.get());
        x = convert(new AtomicLong(100L), AtomicLong.class);
        assertEquals(100L, x.get());
        x = convert(new AtomicBoolean(true), AtomicLong.class);
        assertEquals(1L, x.get());
        x = convert(new AtomicBoolean(false), AtomicLong.class);
        assertEquals(0L, x.get());

        try
        {
            convert(TimeZone.getDefault(), AtomicLong.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", AtomicLong.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testString()
    {
        assertEquals("Hello", convert("Hello", String.class));
        assertEquals("25.0", convert(25.0, String.class));
        assertEquals("true", convert(true, String.class));
        assertEquals("J", convert('J', String.class));
        assertEquals("3.1415926535897932384626433", convert(new BigDecimal("3.1415926535897932384626433"), String.class));
        assertEquals("123456789012345678901234567890", convert(new BigInteger("123456789012345678901234567890"), String.class));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 8, 34, 49);
        assertEquals("2015-01-17T08:34:49", convert(cal.getTime(), String.class));
        assertEquals("2015-01-17T08:34:49", convert(cal, String.class));

        assertEquals("25", convert(new AtomicInteger(25), String.class));
        assertEquals("100", convert(new AtomicLong(100L), String.class));
        assertEquals("true", convert(new AtomicBoolean(true), String.class));

        assertEquals("1.23456789", convert(1.23456789d, String.class));

        int x = 8;
        String s = convertToString(x);
        assert s.equals("8");
        // TODO: Add following test once we have preferred method of removing exponential notation, yet retain decimal separator
//        assertEquals("123456789.12345", convert(123456789.12345, String.class));

        try
        {
            convert(TimeZone.getDefault(), String.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert(new HashMap<>(), HashMap.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported type"));
        }
    }

    @Test
    public void testBigDecimal()
    {
        BigDecimal x = convert("-450000", BigDecimal.class);
        assertEquals(new BigDecimal("-450000"), x);

        assertEquals(new BigDecimal("3.14"), convert(new BigDecimal("3.14"), BigDecimal.class));
        assertEquals(new BigDecimal("8675309"), convert(new BigInteger("8675309"), BigDecimal.class));
        assertEquals(new BigDecimal("75"), convert((short) 75, BigDecimal.class));
        assertEquals(BigDecimal.ONE, convert(true, BigDecimal.class));
        assertSame(BigDecimal.ONE, convert(true, BigDecimal.class));
        assertEquals(BigDecimal.ZERO, convert(false, BigDecimal.class));
        assertSame(BigDecimal.ZERO, convert(false, BigDecimal.class));

        Date now = new Date();
        BigDecimal now70 = new BigDecimal(now.getTime());
        assertEquals(now70, convert(now, BigDecimal.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigDecimal(today.getTime().getTime());
        assertEquals(now70, convert(today, BigDecimal.class));

        assertEquals(new BigDecimal(25), convert(new AtomicInteger(25), BigDecimal.class));
        assertEquals(new BigDecimal(100), convert(new AtomicLong(100L), BigDecimal.class));
        assertEquals(BigDecimal.ONE, convert(new AtomicBoolean(true), BigDecimal.class));
        assertEquals(BigDecimal.ZERO, convert(new AtomicBoolean(false), BigDecimal.class));

        try
        {
            convert(TimeZone.getDefault(), BigDecimal.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", BigDecimal.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testBigInteger()
    {
        BigInteger x = convert("-450000", BigInteger.class);
        assertEquals(new BigInteger("-450000"), x);

        assertEquals(new BigInteger("3"), convert(new BigDecimal("3.14"), BigInteger.class));
        assertEquals(new BigInteger("8675309"), convert(new BigInteger("8675309"), BigInteger.class));
        assertEquals(new BigInteger("75"), convert((short) 75, BigInteger.class));
        assertEquals(BigInteger.ONE, convert(true, BigInteger.class));
        assertSame(BigInteger.ONE, convert(true, BigInteger.class));
        assertEquals(BigInteger.ZERO, convert(false, BigInteger.class));
        assertSame(BigInteger.ZERO, convert(false, BigInteger.class));

        Date now = new Date();
        BigInteger now70 = new BigInteger(Long.toString(now.getTime()));
        assertEquals(now70, convert(now, BigInteger.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigInteger(Long.toString(today.getTime().getTime()));
        assertEquals(now70, convert(today, BigInteger.class));

        assertEquals(new BigInteger("25"), convert(new AtomicInteger(25), BigInteger.class));
        assertEquals(new BigInteger("100"), convert(new AtomicLong(100L), BigInteger.class));
        assertEquals(BigInteger.ONE, convert(new AtomicBoolean(true), BigInteger.class));
        assertEquals(BigInteger.ZERO, convert(new AtomicBoolean(false), BigInteger.class));

        try
        {
            convert(TimeZone.getDefault(), BigInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", BigInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testAtomicInteger()
    {
        AtomicInteger x =  convert("-450000", AtomicInteger.class);
        assertEquals(-450000, x.get());

        assertEquals(3, ( convert(new BigDecimal("3.14"), AtomicInteger.class)).get());
        assertEquals(8675309, (convert(new BigInteger("8675309"), AtomicInteger.class)).get());
        assertEquals(75, (convert((short) 75, AtomicInteger.class)).get());
        assertEquals(1, (convert(true, AtomicInteger.class)).get());
        assertEquals(0, (convert(false, AtomicInteger.class)).get());

        assertEquals(25, (convert(new AtomicInteger(25), AtomicInteger.class)).get());
        assertEquals(100, (convert(new AtomicLong(100L), AtomicInteger.class)).get());
        assertEquals(1, (convert(new AtomicBoolean(true), AtomicInteger.class)).get());
        assertEquals(0, (convert(new AtomicBoolean(false), AtomicInteger.class)).get());

        try
        {
            convert(TimeZone.getDefault(), AtomicInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45badNumber", AtomicInteger.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testDate()
    {
        // Date to Date
        Date utilNow = new Date();
        Date coerced = convert(utilNow, Date.class);
        assertEquals(utilNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);
        assert coerced != utilNow;

        // Date to java.sql.Date
        java.sql.Date sqlCoerced = convert(utilNow, java.sql.Date.class);
        assertEquals(utilNow, sqlCoerced);

        // java.sql.Date to java.sql.Date
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());
        sqlCoerced = convert(sqlNow, java.sql.Date.class);
        assertEquals(sqlNow, sqlCoerced);

        // java.sql.Date to Date
        coerced = convert(sqlNow, Date.class);
        assertEquals(sqlNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);

        // Date to Timestamp
        Timestamp tstamp = convert(utilNow, Timestamp.class);
        assertEquals(utilNow, tstamp);

        // Timestamp to Date
        Date someDate = convert(tstamp, Date.class);
        assertEquals(utilNow, tstamp);
        assertFalse(someDate instanceof Timestamp);

        // java.sql.Date to Timestamp
        tstamp = convert(sqlCoerced, Timestamp.class);
        assertEquals(sqlCoerced, tstamp);

        // Timestamp to java.sql.Date
        java.sql.Date someDate1 = convert(tstamp, java.sql.Date.class);
        assertEquals(someDate1, utilNow);

        // String to Date
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);
        Date date = convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // String to java.sql.Date
        java.sql.Date sqlDate = convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals(cal.getTime(), sqlDate);
        assert sqlDate != null;

        // Calendar to Date
        date = convert(cal, Date.class);
        assertEquals(date, cal.getTime());
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // Calendar to java.sql.Date
        sqlDate = convert(cal, java.sql.Date.class);
        assertEquals(sqlDate, cal.getTime());
        assert sqlDate != null;

        // long to Date
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);
        Date converted = convert(now, Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        Date sqlConverted = convert(now, java.sql.Date.class);
        assertEquals(dateNow, sqlConverted);
        assert sqlConverted != null;

        // AtomicLong to Date
        now = System.currentTimeMillis();
        dateNow = new Date(now);
        converted = convert(new AtomicLong(now), Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        dateNow = new java.sql.Date(now);
        sqlConverted = convert(new AtomicLong(now), java.sql.Date.class);
        assert sqlConverted != null;
        assertEquals(dateNow, sqlConverted);

        // BigInteger to java.sql.Date
        BigInteger bigInt = new BigInteger("" + now);
        sqlDate = convert(bigInt, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigDecimal to java.sql.Date
        BigDecimal bigDec = new BigDecimal(now);
        sqlDate = convert(bigDec, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigInteger to Timestamp
        bigInt = new BigInteger("" + now);
        tstamp = convert(bigInt, Timestamp.class);
        assert tstamp.getTime() == now;

        // BigDecimal to TimeStamp
        bigDec = new BigDecimal(now);
        tstamp = convert(bigDec, Timestamp.class);
        assert tstamp.getTime() == now;

        // Invalid source type for Date
        try
        {
            convert(TimeZone.getDefault(), Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value type"));
        }

        // Invalid source type for java.sql.Date
        try
        {
            convert(TimeZone.getDefault(), java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value type"));
        }

        // Invalid source date for Date
        try
        {
            convert("2015/01/33", Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        // Invalid source date for java.sql.Date
        try
        {
            convert("2015/01/33", java.sql.Date.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testCalendar()
    {
        // Date to Calendar
        Date now = new Date();
        Calendar calendar = convert(new Date(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // SqlDate to Calendar
        java.sql.Date sqlDate = convert(now, java.sql.Date.class);
        calendar = convert(sqlDate, Calendar.class);
        assertEquals(calendar.getTime(), sqlDate);

        // Timestamp to Calendar
        Timestamp timestamp = convert(now, Timestamp.class);
        calendar = convert(timestamp, Calendar.class);
        assertEquals(calendar.getTime(), timestamp);

        // Long to Calendar
        calendar = convert(now.getTime(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // AtomicLong to Calendar
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        calendar = convert(atomicLong, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // String to Calendar
        String strDate = convert(now, String.class);
        calendar = convert(strDate, Calendar.class);
        String strDate2 = convert(calendar, String.class);
        assertEquals(strDate, strDate2);

        // BigInteger to Calendar
        BigInteger bigInt = new BigInteger("" + now.getTime());
        calendar = convert(bigInt, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // BigDecimal to Calendar
        BigDecimal bigDec = new BigDecimal(now.getTime());
        calendar = convert(bigDec, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // Other direction --> Calendar to other date types

        // Calendar to Date
        calendar = convert(now, Calendar.class);
        Date date = convert(calendar, Date.class);
        assertEquals(calendar.getTime(), date);

        // Calendar to SqlDate
        sqlDate = convert(calendar, java.sql.Date.class);
        assertEquals(calendar.getTime().getTime(), sqlDate.getTime());

        // Calendar to Timestamp
        timestamp = convert(calendar, Timestamp.class);
        assertEquals(calendar.getTime().getTime(), timestamp.getTime());

        // Calendar to Long
        long tnow = convert(calendar, long.class);
        assertEquals(calendar.getTime().getTime(), tnow);

        // Calendar to AtomicLong
        atomicLong = convert(calendar, AtomicLong.class);
        assertEquals(calendar.getTime().getTime(), atomicLong.get());

        // Calendar to String
        strDate = convert(calendar, String.class);
        strDate2 = convert(now, String.class);
        assertEquals(strDate, strDate2);

        // Calendar to BigInteger
        bigInt = convert(calendar, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // Calendar to BigDecimal
        bigDec = convert(calendar, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());
    }

    @Test
    public void testDateErrorHandlingBadInput()
    {
        assertNull(convert(" ", java.util.Date.class));
        assertNull(convert("", java.util.Date.class));
        assertNull(convert(null, java.util.Date.class));

        assertNull(convertToDate(" "));
        assertNull(convertToDate(""));
        assertNull(convertToDate(null));

        assertNull(convert(" ", java.sql.Date.class));
        assertNull(convert("", java.sql.Date.class));
        assertNull(convert(null, java.sql.Date.class));

        assertNull(convertToSqlDate(" "));
        assertNull(convertToSqlDate(""));
        assertNull(convertToSqlDate(null));

        assertNull(convert(" ", java.sql.Timestamp.class));
        assertNull(convert("", java.sql.Timestamp.class));
        assertNull(convert(null, java.sql.Timestamp.class));

        assertNull(convertToTimestamp(" "));
        assertNull(convertToTimestamp(""));
        assertNull(convertToTimestamp(null));
    }

    @Test
    public void testTimestamp()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        assertEquals(now, convert(now, Timestamp.class));
        assert convert(now, Timestamp.class) instanceof Timestamp;

        Timestamp christmas = convert("2015/12/25", Timestamp.class);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2015, 11, 25);
        assert christmas.getTime() == c.getTime().getTime();

        Timestamp christmas2 = convert(c, Timestamp.class);

        assertEquals(christmas, christmas2);
        assertEquals(christmas2, convert(christmas.getTime(), Timestamp.class));

        AtomicLong al = new AtomicLong(christmas.getTime());
        assertEquals(christmas2, convert(al, Timestamp.class));

        try
        {
            convert(Boolean.TRUE, Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unsupported value type");
        }

        try
        {
            convert("123dhksdk", Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("could not be converted");
        }
    }

    @Test
    public void testFloat()
    {
        assert -3.14f == convert(-3.14f, float.class);
        assert -3.14f == convert(-3.14f, Float.class);
        assert -3.14f == convert("-3.14", float.class);
        assert -3.14f == convert("-3.14", Float.class);
        assert -3.14f == convert(-3.14d, float.class);
        assert -3.14f == convert(-3.14d, Float.class);
        assert 1.0f == convert(true, float.class);
        assert 1.0f == convert(true, Float.class);
        assert 0.0f == convert(false, float.class);
        assert 0.0f == convert(false, Float.class);

        assert 0.0f == convert(new AtomicInteger(0), Float.class);
        assert 0.0f == convert(new AtomicLong(0), Float.class);
        assert 0.0f == convert(new AtomicBoolean(false), Float.class);
        assert 1.0f == convert(new AtomicBoolean(true), Float.class);

        try
        {
            convert(TimeZone.getDefault(), float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45.6badNumber", Float.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testDouble()
    {
        assert -3.14d == convert(-3.14d, double.class);
        assert -3.14d == convert(-3.14d, Double.class);
        assert -3.14d == convert("-3.14", double.class);
        assert -3.14d == convert("-3.14", Double.class);
        assert -3.14d == convert(new BigDecimal("-3.14"), double.class);
        assert -3.14d == convert(new BigDecimal("-3.14"), Double.class);
        assert 1.0d == convert(true, double.class);
        assert 1.0d == convert(true, Double.class);
        assert 0.0d == convert(false, double.class);
        assert 0.0d == convert(false, Double.class);

        assert 0.0d == convert(new AtomicInteger(0), double.class);
        assert 0.0d == convert(new AtomicLong(0), double.class);
        assert 0.0d == convert(new AtomicBoolean(false), Double.class);
        assert 1.0d == convert(new AtomicBoolean(true), Double.class);

        try
        {
            convert(TimeZone.getDefault(), double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            convert("45.6badNumber", Double.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testBoolean()
    {
        assertEquals(true, convert(-3.14d, boolean.class));
        assertEquals(false, convert(0.0d, boolean.class));
        assertEquals(true, convert(-3.14f, Boolean.class));
        assertEquals(false, convert(0.0f, Boolean.class));

        assertEquals(false, convert(new AtomicInteger(0), boolean.class));
        assertEquals(false, convert(new AtomicLong(0), boolean.class));
        assertEquals(false, convert(new AtomicBoolean(false), Boolean.class));
        assertEquals(true, convert(new AtomicBoolean(true), Boolean.class));

        assertEquals(true, convert("TRue", Boolean.class));
        assertEquals(true, convert("true", Boolean.class));
        assertEquals(false, convert("fALse", Boolean.class));
        assertEquals(false, convert("false", Boolean.class));
        assertEquals(false, convert("john", Boolean.class));

        assertEquals(true, convert(true, Boolean.class));
        assertEquals(true, convert(Boolean.TRUE, Boolean.class));
        assertEquals(false, convert(false, Boolean.class));
        assertEquals(false, convert(Boolean.FALSE, Boolean.class));

        try
        {
            convert(new Date(), Boolean.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }
    }

    @Test
    public void testAtomicBoolean()
    {
        assert (convert(-3.14d, AtomicBoolean.class)).get();
        assert !(convert(0.0d, AtomicBoolean.class)).get();
        assert (convert(-3.14f, AtomicBoolean.class)).get();
        assert !(convert(0.0f, AtomicBoolean.class)).get();

        assert !(convert(new AtomicInteger(0), AtomicBoolean.class)).get();
        assert !(convert(new AtomicLong(0), AtomicBoolean.class)).get();
        assert !(convert(new AtomicBoolean(false), AtomicBoolean.class)).get();
        assert (convert(new AtomicBoolean(true), AtomicBoolean.class)).get();

        assert (convert("TRue", AtomicBoolean.class)).get();
        assert !(convert("fALse", AtomicBoolean.class)).get();
        assert !(convert("john", AtomicBoolean.class)).get();

        assert (convert(true, AtomicBoolean.class)).get();
        assert (convert(Boolean.TRUE, AtomicBoolean.class)).get();
        assert !(convert(false, AtomicBoolean.class)).get();
        assert !(convert(Boolean.FALSE, AtomicBoolean.class)).get();

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = convert(b1, AtomicBoolean.class);
        assert b1 != b2; // ensure that it returns a different but equivalent instance
        assert b1.get() == b2.get();

        try
        {
            convert(new Date(), AtomicBoolean.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }
    }

    @Test
    public void testUnsupportedType()
    {
        try
        {
            convert("Lamb", TimeZone.class);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported type"));
        }
    }

    @Test
    public void testNullInstance()
    {
        assert false == convert(null, boolean.class);
        assert null == convert(null, Boolean.class);
        assert 0 == convert(null, byte.class);
        assert null == convert(null, Byte.class);
        assert 0 == convert(null, short.class);
        assert null == convert(null, Short.class);
        assert 0 == convert(null, int.class);
        assert null == convert(null, Integer.class);
        assert 0L == convert(null, long.class);
        assert null == convert(null, Long.class);
        assert 0.0f == convert(null, float.class);
        assert null == convert(null, Float.class);
        assert 0.0d == convert(null, double.class);
        assert null == convert(null, Double.class);
        assert (char)0 == convert(null, char.class);
        assert null == convert(null, Character.class);

        assert null == convert(null, Date.class);
        assert null == convert(null, java.sql.Date.class);
        assert null == convert(null, Timestamp.class);
        assert null == convert(null, Calendar.class);
        assert null == convert(null, String.class);
        assert null == convert(null, BigInteger.class);
        assert null == convert(null, BigDecimal.class);
        assert null == convert(null, AtomicBoolean.class);
        assert null == convert(null, AtomicInteger.class);
        assert null == convert(null, AtomicLong.class);

        assert null == convertToByte(null);
        assert null == convertToInteger(null);
        assert null == convertToShort(null);
        assert null == convertToLong(null);
        assert null == convertToFloat(null);
        assert null == convertToDouble(null);
        assert null == convertToCharacter(null);
        assert null == convertToDate(null);
        assert null == convertToSqlDate(null);
        assert null == convertToTimestamp(null);
        assert null == convertToAtomicBoolean(null);
        assert null == convertToAtomicInteger(null);
        assert null == convertToAtomicLong(null);
        assert null == convertToString(null);

        assert false == convert2boolean(null);
        assert 0 == convert2byte(null);
        assert 0 == convert2int(null);
        assert 0 == convert2short(null);
        assert 0 == convert2long(null);
        assert 0.0f == convert2float(null);
        assert 0.0d == convert2double(null);
        assert (char)0 == convert2char(null);
        assert BIG_INTEGER_ZERO == convert2BigInteger(null);
        assert BIG_DECIMAL_ZERO == convert2BigDecimal(null);
        assert false == convert2AtomicBoolean(null).get();
        assert 0 == convert2AtomicInteger(null).get();
        assert 0L == convert2AtomicLong(null).get();
        assert "" == convert2String(null);
    }

    @Test
    public void testConvert2()
    {
        assert convert2boolean("true");
        assert -8 == convert2byte("-8");
        assert -8 == convert2int("-8");
        assert -8 == convert2short("-8");
        assert -8 == convert2long("-8");
        assert -8.0f == convert2float("-8");
        assert -8.0d == convert2double("-8");
        assert 'A' == convert2char(65);
        assert new BigInteger("-8").equals(convert2BigInteger("-8"));
        assert new BigDecimal(-8.0d).equals(convert2BigDecimal("-8"));
        assert convert2AtomicBoolean("true").get();
        assert -8 == convert2AtomicInteger("-8").get();
        assert -8L == convert2AtomicLong("-8").get();
        assert "-8".equals(convert2String(-8));
    }

    @Test
    public void testNullType()
    {
        try
        {
            convert("123", null);
            fail();
        }
        catch (Exception e)
        {
            e.getMessage().toLowerCase().contains("type cannot be null");
        }
    }

    @Test
    public void testEmptyString()
    {
        assertEquals(false, convert("", boolean.class));
        assertEquals(false, convert("", boolean.class));
        assert (byte) 0 == convert("", byte.class);
        assert (short) 0 == convert("", short.class);
        assert 0 == convert("", int.class);
        assert (long) 0 == convert("", long.class);
        assert 0.0f == convert("", float.class);
        assert 0.0d == convert("", double.class);
        assertEquals(BigDecimal.ZERO, convert("", BigDecimal.class));
        assertEquals(BigInteger.ZERO, convert("", BigInteger.class));
        assertEquals(new AtomicBoolean(false).get(), convert("", AtomicBoolean.class).get());
        assertEquals(new AtomicInteger(0).get(), convert("", AtomicInteger.class).get());
        assertEquals(new AtomicLong(0L).get(), convert("", AtomicLong.class).get());
    }

    @Test
    public void testEnumSupport()
    {
        assertEquals("foo", convert(foo, String.class));
        assertEquals("bar", convert(bar, String.class));
    }

    @Test
    public void testCharacterSupport()
    {
        assert 65 == convert('A', Short.class);
        assert 65 == convert('A', short.class);
        assert 65 == convert('A', Integer.class);
        assert 65 == convert('A', int.class);
        assert 65 == convert('A', Long.class);
        assert 65 == convert('A', long.class);
        assert 65 == convert('A', BigInteger.class).longValue();
        assert 65 == convert('A', BigDecimal.class).longValue();

        assert '1' == convert2char(true);
        assert '0' == convert2char(false);
        assert '1' == convert2char(new AtomicBoolean(true));
        assert '0' == convert2char(new AtomicBoolean(false));
        assert 'z' == convert2char('z');
        assert 0 == convert2char("");
        assert 0 == convertToCharacter("");
        assert 'A' == convert2char("65");
        assert 'A' == convertToCharacter("65");
        try
        {
            convert2char("This is not a number");
            fail();
        }
        catch (IllegalArgumentException e) { }
        try
        {
            convert2char(new Date());
            fail();
        }
        catch (IllegalArgumentException e) { }
    }

    @Test
    public void testConvertUnknown()
    {
        try
        {
            convertToString(TimeZone.getDefault());
            fail();
        }
        catch (IllegalArgumentException e) { }
    }
}
