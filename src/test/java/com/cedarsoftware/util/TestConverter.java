package com.cedarsoftware.util;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.TestConverter.fubar.bar;
import static com.cedarsoftware.util.TestConverter.fubar.foo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John DeRegnaucourt (john@cedarsoftware.com) & Ken Partlow
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
    public void testConstructorIsPrivateAndClassIsFinal() throws Exception {
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
        Byte x = Converter.convert("-25", byte.class);
        assert -25 == x;
        x = Converter.convert("24", Byte.class);
        assert 24 == x;

        x = Converter.convert((byte) 100, byte.class);
        assert 100 == x;
        x = Converter.convert((byte) 120, Byte.class);
        assert 120 == x;

        x = Converter.convert(new BigDecimal("100"), byte.class);
        assert 100 == x;
        x = Converter.convert(new BigInteger("120"), Byte.class);
        assert 120 == x;

        Byte value = Converter.convert(true, Byte.class);
        assert value == 1;
        assert (byte)1 == Converter.convert(true, Byte.class);
        assert (byte)0 == Converter.convert(false, byte.class);

        assert (byte)25 == Converter.convert(new AtomicInteger(25), byte.class);
        assert (byte)100 == Converter.convert(new AtomicLong(100L), byte.class);
        assert (byte)1 == Converter.convert(new AtomicBoolean(true), byte.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), byte.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", byte.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }
    
    @Test
    public void testShort()
    {
        Short x = Converter.convert("-25000", short.class);
        assert -25000 == x;
        x = Converter.convert("24000", Short.class);
        assert 24000 == x;

        x = Converter.convert((short) 10000, short.class);
        assert 10000 == x;
        x = Converter.convert((short) 20000, Short.class);
        assert 20000 == x;

        x = Converter.convert(new BigDecimal("10000"), short.class);
        assert 10000 == x;
        x = Converter.convert(new BigInteger("20000"), Short.class);
        assert 20000 == x;

        assert (short)1 == Converter.convert(true, short.class);
        assert (short)0 == Converter.convert(false, Short.class);

        assert (short)25 == Converter.convert(new AtomicInteger(25), short.class);
        assert (short)100 == Converter.convert(new AtomicLong(100L), Short.class);
        assert (short)1 == Converter.convert(new AtomicBoolean(true), Short.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), short.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", short.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testInt()
    {
        Integer x = Converter.convert("-450000", int.class);
        assertEquals((Object) (-450000), x);
        x = Converter.convert("550000", Integer.class);
        assertEquals((Object) 550000, x);

        x = Converter.convert(100000, int.class);
        assertEquals((Object) 100000, x);
        x = Converter.convert(200000, Integer.class);
        assertEquals((Object) 200000, x);

        x = Converter.convert(new BigDecimal("100000"), int.class);
        assertEquals((Object) 100000, x);
        x = Converter.convert(new BigInteger("200000"), Integer.class);
        assertEquals((Object) 200000, x);

        assert 1 == Converter.convert(true, Integer.class);
        assert 0 == Converter.convert(false, int.class);

        assert 25 == Converter.convert(new AtomicInteger(25), int.class);
        assert 100 == Converter.convert(new AtomicLong(100L), Integer.class);
        assert 1 == Converter.convert(new AtomicBoolean(true), Integer.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), int.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", int.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testLong()
    {
        Long x = Converter.convert("-450000", long.class);
        assertEquals((Object)(-450000L), x);
        x = Converter.convert("550000", Long.class);
        assertEquals((Object)550000L, x);

        x = Converter.convert(100000L, long.class);
        assertEquals((Object)100000L, x);
        x = Converter.convert(200000L, Long.class);
        assertEquals((Object)200000L, x);

        x = Converter.convert(new BigDecimal("100000"), long.class);
        assertEquals((Object)100000L, x);
        x = Converter.convert(new BigInteger("200000"), Long.class);
        assertEquals((Object)200000L, x);

        assert (long)1 == Converter.convert(true, long.class);
        assert (long)0 == Converter.convert(false, Long.class);

        Date now = new Date();
        long now70 = now.getTime();
        assert now70 == Converter.convert(now, long.class);

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        assert now70 == Converter.convert(today, Long.class);

        assert 25L == Converter.convert(new AtomicInteger(25), long.class);
        assert 100L == Converter.convert(new AtomicLong(100L), Long.class);
        assert 1L == Converter.convert(new AtomicBoolean(true), Long.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), long.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", long.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testAtomicLong()
    {
        AtomicLong x = Converter.convert("-450000", AtomicLong.class);
        assertEquals(-450000L, x.get());
        x = Converter.convert("550000", AtomicLong.class);
        assertEquals(550000L, x.get());

        x = Converter.convert(100000L, AtomicLong.class);
        assertEquals(100000L, x.get());
        x = Converter.convert(200000L, AtomicLong.class);
        assertEquals(200000L, x.get());

        x = Converter.convert(new BigDecimal("100000"), AtomicLong.class);
        assertEquals(100000L, x.get());
        x = Converter.convert(new BigInteger("200000"), AtomicLong.class);
        assertEquals(200000L, x.get());

        x = Converter.convert(true, AtomicLong.class);
        assertEquals((long)1, x.get());
        x = Converter.convert(false, AtomicLong.class);
        assertEquals((long)0, x.get());

        Date now = new Date();
        long now70 = now.getTime();
        x =  Converter.convert(now, AtomicLong.class);
        assertEquals(now70, x.get());

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        x =  Converter.convert(today, AtomicLong.class);
        assertEquals(now70, x.get());

        x = Converter.convert(new AtomicInteger(25), AtomicLong.class);
        assertEquals(25L, x.get());
        x = Converter.convert(new AtomicLong(100L), AtomicLong.class);
        assertEquals(100L, x.get());
        x = Converter.convert(new AtomicBoolean(true), AtomicLong.class);
        assertEquals(1L, x.get());

        try
        {
            Converter.convert(TimeZone.getDefault(), AtomicLong.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", AtomicLong.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testString()
    {
        assertEquals("Hello", Converter.convert("Hello", String.class));
        assertEquals("25.0", Converter.convert(25.0, String.class));
        assertEquals("true", Converter.convert(true, String.class));
        assertEquals("J", Converter.convert('J', String.class));
        assertEquals("3.1415926535897932384626433", Converter.convert(new BigDecimal("3.1415926535897932384626433"), String.class));
        assertEquals("123456789012345678901234567890", Converter.convert(new BigInteger("123456789012345678901234567890"), String.class));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 8, 34, 49);
        assertEquals("2015-01-17T08:34:49", Converter.convert(cal.getTime(), String.class));
        assertEquals("2015-01-17T08:34:49", Converter.convert(cal, String.class));

        assertEquals("25", Converter.convert(new AtomicInteger(25), String.class));
        assertEquals("100", Converter.convert(new AtomicLong(100L), String.class));
        assertEquals("true", Converter.convert(new AtomicBoolean(true), String.class));

        assertEquals("1.23456789", Converter.convert(1.23456789d, String.class));
        // TODO: Add following test once we have preferred method of removing exponential notation, yet retain decimal separator
//        assertEquals("123456789.12345", Converter.convert(123456789.12345, String.class));

        try
        {
            Converter.convert(TimeZone.getDefault(), String.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }
    }

    @Test
    public void testBigDecimal()
    {
        BigDecimal x = Converter.convert("-450000", BigDecimal.class);
        assertEquals(new BigDecimal("-450000"), x);

        assertEquals(new BigDecimal("3.14"), Converter.convert(new BigDecimal("3.14"), BigDecimal.class));
        assertEquals(new BigDecimal("8675309"), Converter.convert(new BigInteger("8675309"), BigDecimal.class));
        assertEquals(new BigDecimal("75"), Converter.convert((short) 75, BigDecimal.class));
        assertEquals(BigDecimal.ONE, Converter.convert(true, BigDecimal.class));
        assertSame(BigDecimal.ONE, Converter.convert(true, BigDecimal.class));
        assertEquals(BigDecimal.ZERO, Converter.convert(false, BigDecimal.class));
        assertSame(BigDecimal.ZERO, Converter.convert(false, BigDecimal.class));

        Date now = new Date();
        BigDecimal now70 = new BigDecimal(now.getTime());
        assertEquals(now70, Converter.convert(now, BigDecimal.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigDecimal(today.getTime().getTime());
        assertEquals(now70, Converter.convert(today, BigDecimal.class));

        assertEquals(new BigDecimal(25), Converter.convert(new AtomicInteger(25), BigDecimal.class));
        assertEquals(new BigDecimal(100), Converter.convert(new AtomicLong(100L), BigDecimal.class));
        assertEquals(BigDecimal.ONE, Converter.convert(new AtomicBoolean(true), BigDecimal.class));

        try
        {
            Converter.convert(TimeZone.getDefault(), BigDecimal.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", BigDecimal.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testBigInteger()
    {
        BigInteger x = Converter.convert("-450000", BigInteger.class);
        assertEquals(new BigInteger("-450000"), x);

        assertEquals(new BigInteger("3"), Converter.convert(new BigDecimal("3.14"), BigInteger.class));
        assertEquals(new BigInteger("8675309"), Converter.convert(new BigInteger("8675309"), BigInteger.class));
        assertEquals(new BigInteger("75"), Converter.convert((short) 75, BigInteger.class));
        assertEquals(BigInteger.ONE, Converter.convert(true, BigInteger.class));
        assertSame(BigInteger.ONE, Converter.convert(true, BigInteger.class));
        assertEquals(BigInteger.ZERO, Converter.convert(false, BigInteger.class));
        assertSame(BigInteger.ZERO, Converter.convert(false, BigInteger.class));

        Date now = new Date();
        BigInteger now70 = new BigInteger(Long.toString(now.getTime()));
        assertEquals(now70, Converter.convert(now, BigInteger.class));

        Calendar today = Calendar.getInstance();
        now70 = new BigInteger(Long.toString(today.getTime().getTime()));
        assertEquals(now70, Converter.convert(today, BigInteger.class));

        assertEquals(new BigInteger("25"), Converter.convert(new AtomicInteger(25), BigInteger.class));
        assertEquals(new BigInteger("100"), Converter.convert(new AtomicLong(100L), BigInteger.class));
        assertEquals(BigInteger.ONE, Converter.convert(new AtomicBoolean(true), BigInteger.class));

        try
        {
            Converter.convert(TimeZone.getDefault(), BigInteger.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", BigInteger.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testAtomicInteger()
    {
        AtomicInteger x =  Converter.convert("-450000", AtomicInteger.class);
        assertEquals(-450000, x.get());

        assertEquals(3, ( Converter.convert(new BigDecimal("3.14"), AtomicInteger.class)).get());
        assertEquals(8675309, (Converter.convert(new BigInteger("8675309"), AtomicInteger.class)).get());
        assertEquals(75, (Converter.convert((short) 75, AtomicInteger.class)).get());
        assertEquals(1, (Converter.convert(true, AtomicInteger.class)).get());
        assertEquals(0, (Converter.convert(false, AtomicInteger.class)).get());

        assertEquals(25, (Converter.convert(new AtomicInteger(25), AtomicInteger.class)).get());
        assertEquals(100, (Converter.convert(new AtomicLong(100L), AtomicInteger.class)).get());
        assertEquals(1, (Converter.convert(new AtomicBoolean(true), AtomicInteger.class)).get());

        try
        {
            Converter.convert(TimeZone.getDefault(), AtomicInteger.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45badNumber", AtomicInteger.class);
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
        Date coerced = Converter.convert(utilNow, Date.class);
        assertEquals(utilNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);
        assert coerced != utilNow;

        // Date to java.sql.Date
        java.sql.Date sqlCoerced = Converter.convert(utilNow, java.sql.Date.class);
        assertEquals(utilNow, sqlCoerced);

        // java.sql.Date to java.sql.Date
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());
        sqlCoerced = Converter.convert(sqlNow, java.sql.Date.class);
        assertEquals(sqlNow, sqlCoerced);

        // java.sql.Date to Date
        coerced = Converter.convert(sqlNow, Date.class);
        assertEquals(sqlNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);

        // Date to Timestamp
        Timestamp tstamp = Converter.convert(utilNow, Timestamp.class);
        assertEquals(utilNow, tstamp);

        // Timestamp to Date
        Date someDate = Converter.convert(tstamp, Date.class);
        assertEquals(utilNow, tstamp);
        assertFalse(someDate instanceof Timestamp);

        // java.sql.Date to Timestamp
        tstamp = Converter.convert(sqlCoerced, Timestamp.class);
        assertEquals(sqlCoerced, tstamp);

        // Timestamp to java.sql.Date
        java.sql.Date someDate1 = Converter.convert(tstamp, java.sql.Date.class);
        assertEquals(someDate1, utilNow);

        // String to Date
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);
        Date date = Converter.convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // String to java.sql.Date
        java.sql.Date sqlDate = Converter.convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals(cal.getTime(), sqlDate);
        assert sqlDate != null;

        // Calendar to Date
        date = Converter.convert(cal, Date.class);
        assertEquals(date, cal.getTime());
        assert date != null;
        assertFalse(date instanceof java.sql.Date);

        // Calendar to java.sql.Date
        sqlDate = Converter.convert(cal, java.sql.Date.class);
        assertEquals(sqlDate, cal.getTime());
        assert sqlDate != null;

        // long to Date
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);
        Date converted = Converter.convert(now, Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        Date sqlConverted = Converter.convert(now, java.sql.Date.class);
        assertEquals(dateNow, sqlConverted);
        assert sqlConverted != null;

        // AtomicLong to Date
        now = System.currentTimeMillis();
        dateNow = new Date(now);
        converted = Converter.convert(new AtomicLong(now), Date.class);
        assert converted != null;
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        dateNow = new java.sql.Date(now);
        sqlConverted = Converter.convert(new AtomicLong(now), java.sql.Date.class);
        assert sqlConverted != null;
        assertEquals(dateNow, sqlConverted);

        // BigInteger to java.sql.Date
        BigInteger bigInt = new BigInteger("" + now);
        sqlDate = Converter.convert(bigInt, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigDecimal to java.sql.Date
        BigDecimal bigDec = new BigDecimal(now);
        sqlDate = Converter.convert(bigDec, java.sql.Date.class);
        assert sqlDate.getTime() == now;

        // BigInteger to Timestamp
        bigInt = new BigInteger("" + now);
        tstamp = Converter.convert(bigInt, Timestamp.class);
        assert tstamp.getTime() == now;

        // BigDecimal to TimeStamp
        bigDec = new BigDecimal(now);
        tstamp = Converter.convert(bigDec, Timestamp.class);
        assert tstamp.getTime() == now;

        // Invalid source type for Date
        try
        {
            Converter.convert(TimeZone.getDefault(), Date.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value type"));
        }

        // Invalid source type for java.sql.Date
        try
        {
            Converter.convert(TimeZone.getDefault(), java.sql.Date.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value type"));
        }

        // Invalid source date for Date
        try
        {
            Converter.convert("2015/01/33", Date.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        // Invalid source date for java.sql.Date
        try
        {
            Converter.convert("2015/01/33", java.sql.Date.class);
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
        Calendar calendar = Converter.convert(new Date(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // SqlDate to Calendar
        java.sql.Date sqlDate = Converter.convert(now, java.sql.Date.class);
        calendar = Converter.convert(sqlDate, Calendar.class);
        assertEquals(calendar.getTime(), sqlDate);

        // Timestamp to Calendar
        Timestamp timestamp = Converter.convert(now, Timestamp.class);
        calendar = Converter.convert(timestamp, Calendar.class);
        assertEquals(calendar.getTime(), timestamp);

        // Long to Calendar
        calendar = Converter.convert(now.getTime(), Calendar.class);
        assertEquals(calendar.getTime(), now);

        // AtomicLong to Calendar
        AtomicLong atomicLong = new AtomicLong(now.getTime());
        calendar = Converter.convert(atomicLong, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // String to Calendar
        String strDate = Converter.convert(now, String.class);
        calendar = Converter.convert(strDate, Calendar.class);
        String strDate2 = Converter.convert(calendar, String.class);
        assertEquals(strDate, strDate2);

        // BigInteger to Calendar
        BigInteger bigInt = new BigInteger("" + now.getTime());
        calendar = Converter.convert(bigInt, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // BigDecimal to Calendar
        BigDecimal bigDec = new BigDecimal(now.getTime());
        calendar = Converter.convert(bigDec, Calendar.class);
        assertEquals(calendar.getTime(), now);

        // Other direction --> Calendar to other date types

        // Calendar to Date
        calendar = Converter.convert(now, Calendar.class);
        Date date = Converter.convert(calendar, Date.class);
        assertEquals(calendar.getTime(), date);

        // Calendar to SqlDate
        sqlDate = Converter.convert(calendar, java.sql.Date.class);
        assertEquals(calendar.getTime().getTime(), sqlDate.getTime());

        // Calendar to Timestamp
        timestamp = Converter.convert(calendar, Timestamp.class);
        assertEquals(calendar.getTime().getTime(), timestamp.getTime());

        // Calendar to Long
        long tnow = Converter.convert(calendar, long.class);
        assertEquals(calendar.getTime().getTime(), tnow);

        // Calendar to AtomicLong
        atomicLong = Converter.convert(calendar, AtomicLong.class);
        assertEquals(calendar.getTime().getTime(), atomicLong.get());

        // Calendar to String
        strDate = Converter.convert(calendar, String.class);
        strDate2 = Converter.convert(now, String.class);
        assertEquals(strDate, strDate2);

        // Calendar to BigInteger
        bigInt = Converter.convert(calendar, BigInteger.class);
        assertEquals(now.getTime(), bigInt.longValue());

        // Calendar to BigDecimal
        bigDec = Converter.convert(calendar, BigDecimal.class);
        assertEquals(now.getTime(), bigDec.longValue());
    }

    @Test
    public void testDateErrorHandlingBadInput()
    {
        assertNull(Converter.convert(" ", java.util.Date.class));
        assertNull(Converter.convert("", java.util.Date.class));
        assertNull(Converter.convert(null, java.util.Date.class));

        assertNull(Converter.convertToDate(" "));
        assertNull(Converter.convertToDate(""));
        assertNull(Converter.convertToDate(null));

        assertNull(Converter.convert(" ", java.sql.Date.class));
        assertNull(Converter.convert("", java.sql.Date.class));
        assertNull(Converter.convert(null, java.sql.Date.class));

        assertNull(Converter.convertToSqlDate(" "));
        assertNull(Converter.convertToSqlDate(""));
        assertNull(Converter.convertToSqlDate(null));

        assertNull(Converter.convert(" ", java.sql.Timestamp.class));
        assertNull(Converter.convert("", java.sql.Timestamp.class));
        assertNull(Converter.convert(null, java.sql.Timestamp.class));

        assertNull(Converter.convertToTimestamp(" "));
        assertNull(Converter.convertToTimestamp(""));
        assertNull(Converter.convertToTimestamp(null));
    }

    @Test
    public void testTimestamp()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        assertEquals(now, Converter.convert(now, Timestamp.class));
        assert Converter.convert(now, Timestamp.class) instanceof Timestamp;

        Timestamp christmas = Converter.convert("2015/12/25", Timestamp.class);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2015, 11, 25);
        assert christmas.getTime() == c.getTime().getTime();

        Timestamp christmas2 = Converter.convert(c, Timestamp.class);

        assertEquals(christmas, christmas2);
        assertEquals(christmas2, Converter.convert(christmas.getTime(), Timestamp.class));

        AtomicLong al = new AtomicLong(christmas.getTime());
        assertEquals(christmas2, Converter.convert(al, Timestamp.class));

        try
        {
            Converter.convert(Boolean.TRUE, Timestamp.class);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assert e.getMessage().toLowerCase().contains("unsupported value type");
        }

        try
        {
            Converter.convert("123dhksdk", Timestamp.class);
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
        assert -3.14f == Converter.convert(-3.14f, float.class);
        assert -3.14f == Converter.convert(-3.14f, Float.class);
        assert -3.14f == Converter.convert("-3.14", float.class);
        assert -3.14f == Converter.convert("-3.14", Float.class);
        assert -3.14f == Converter.convert(-3.14d, float.class);
        assert -3.14f == Converter.convert(-3.14d, Float.class);
        assert 1.0f == Converter.convert(true, float.class);
        assert 1.0f == Converter.convert(true, Float.class);
        assert 0.0f == Converter.convert(false, float.class);
        assert 0.0f == Converter.convert(false, Float.class);

        assert 0.0f == Converter.convert(new AtomicInteger(0), Float.class);
        assert 0.0f == Converter.convert(new AtomicLong(0), Float.class);
        assert 0.0f == Converter.convert(new AtomicBoolean(false), Float.class);
        assert 1.0f == Converter.convert(new AtomicBoolean(true), Float.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), float.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45.6badNumber", Float.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testDouble()
    {
        assert -3.14d == Converter.convert(-3.14d, double.class);
        assert -3.14d == Converter.convert(-3.14d, Double.class);
        assert -3.14d == Converter.convert("-3.14", double.class);
        assert -3.14d == Converter.convert("-3.14", Double.class);
        assert -3.14d == Converter.convert(new BigDecimal("-3.14"), double.class);
        assert -3.14d == Converter.convert(new BigDecimal("-3.14"), Double.class);
        assert 1.0d == Converter.convert(true, double.class);
        assert 1.0d == Converter.convert(true, Double.class);
        assert 0.0d == Converter.convert(false, double.class);
        assert 0.0d == Converter.convert(false, Double.class);

        assert 0.0d == Converter.convert(new AtomicInteger(0), double.class);
        assert 0.0d == Converter.convert(new AtomicLong(0), double.class);
        assert 0.0d == Converter.convert(new AtomicBoolean(false), Double.class);
        assert 1.0d == Converter.convert(new AtomicBoolean(true), Double.class);

        try
        {
            Converter.convert(TimeZone.getDefault(), double.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }

        try
        {
            Converter.convert("45.6badNumber", Double.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }
    }

    @Test
    public void testBoolean()
    {
        assertEquals(true, Converter.convert(-3.14d, boolean.class));
        assertEquals(false, Converter.convert(0.0d, boolean.class));
        assertEquals(true, Converter.convert(-3.14f, Boolean.class));
        assertEquals(false, Converter.convert(0.0f, Boolean.class));

        assertEquals(false, Converter.convert(new AtomicInteger(0), boolean.class));
        assertEquals(false, Converter.convert(new AtomicLong(0), boolean.class));
        assertEquals(false, Converter.convert(new AtomicBoolean(false), Boolean.class));
        assertEquals(true, Converter.convert(new AtomicBoolean(true), Boolean.class));

        assertEquals(true, Converter.convert("TRue", Boolean.class));
        assertEquals(true, Converter.convert("true", Boolean.class));
        assertEquals(false, Converter.convert("fALse", Boolean.class));
        assertEquals(false, Converter.convert("false", Boolean.class));
        assertEquals(false, Converter.convert("john", Boolean.class));

        assertEquals(true, Converter.convert(true, Boolean.class));
        assertEquals(true, Converter.convert(Boolean.TRUE, Boolean.class));
        assertEquals(false, Converter.convert(false, Boolean.class));
        assertEquals(false, Converter.convert(Boolean.FALSE, Boolean.class));

        try
        {
            Converter.convert(new Date(), Boolean.class);
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("unsupported value"));
        }
    }

    @Test
    public void testAtomicBoolean()
    {
        assert (Converter.convert(-3.14d, AtomicBoolean.class)).get();
        assert !(Converter.convert(0.0d, AtomicBoolean.class)).get();
        assert (Converter.convert(-3.14f, AtomicBoolean.class)).get();
        assert !(Converter.convert(0.0f, AtomicBoolean.class)).get();

        assert !(Converter.convert(new AtomicInteger(0), AtomicBoolean.class)).get();
        assert !(Converter.convert(new AtomicLong(0), AtomicBoolean.class)).get();
        assert !(Converter.convert(new AtomicBoolean(false), AtomicBoolean.class)).get();
        assert (Converter.convert(new AtomicBoolean(true), AtomicBoolean.class)).get();

        assert (Converter.convert("TRue", AtomicBoolean.class)).get();
        assert !(Converter.convert("fALse", AtomicBoolean.class)).get();
        assert !(Converter.convert("john", AtomicBoolean.class)).get();

        assert (Converter.convert(true, AtomicBoolean.class)).get();
        assert (Converter.convert(Boolean.TRUE, AtomicBoolean.class)).get();
        assert !(Converter.convert(false, AtomicBoolean.class)).get();
        assert !(Converter.convert(Boolean.FALSE, AtomicBoolean.class)).get();

        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = Converter.convert(b1, AtomicBoolean.class);
        assert b1 != b2; // ensure that it returns a different but equivalent instance
        assert b1.get() == b2.get();

        try
        {
            Converter.convert(new Date(), AtomicBoolean.class);
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
            Converter.convert("Lamb", TimeZone.class);
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
        assertEquals(false, Converter.convert(null, boolean.class));
        assertFalse((Boolean) Converter.convert(null, Boolean.class));
        assert (byte) 0 == Converter.convert(null, byte.class);
        assert (byte) 0 == Converter.convert(null, Byte.class);
        assert (short) 0 == Converter.convert(null, short.class);
        assert (short) 0 == Converter.convert(null, Short.class);
        assert 0 == Converter.convert(null, int.class);
        assert 0 == Converter.convert(null, Integer.class);
        assert 0L == Converter.convert(null, long.class);
        assert 0L == Converter.convert(null, Long.class);
        assert 0.0f == Converter.convert(null, float.class);
        assert 0.0f == Converter.convert(null, Float.class);
        assert 0.0d == Converter.convert(null, double.class);
        assert 0.0d == Converter.convert(null, Double.class);
        assertNull(Converter.convert(null, Date.class));
        assertNull(Converter.convert(null, java.sql.Date.class));
        assertNull(Converter.convert(null, Timestamp.class));
        assertNull(Converter.convert(null, String.class));
        assertEquals(BigInteger.ZERO, Converter.convert(null, BigInteger.class));
        assertEquals(BigDecimal.ZERO, Converter.convert(null, BigDecimal.class));
        assertEquals(false, Converter.convert(null, AtomicBoolean.class).get());
        assertEquals(0, Converter.convert(null, AtomicInteger.class).get());
        assertEquals(0, Converter.convert(null, AtomicLong.class).get());

        assert 0 == Converter.convertToByte(null);
        assert 0 == Converter.convertToInteger(null);
        assert 0 == Converter.convertToShort(null);
        assert 0L == Converter.convertToLong(null);
        assert 0.0f == Converter.convertToFloat(null);
        assert 0.0d == Converter.convertToDouble(null);
        assert null == Converter.convertToDate(null);
        assert null == Converter.convertToSqlDate(null);
        assert null == Converter.convertToTimestamp(null);
        assert false == Converter.convertToAtomicBoolean(null).get();
        assert 0 == Converter.convertToAtomicInteger(null).get();
        assert 0L == Converter.convertToAtomicLong(null).get();
        assert null == Converter.convertToString(null);
    }

    @Test
    public void testNullType()
    {
        try
        {
            Converter.convert("123", null);
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
        assertEquals(false, Converter.convert("", boolean.class));
        assertEquals(false, Converter.convert("", boolean.class));
        assert (byte) 0 == Converter.convert("", byte.class);
        assert (short) 0 == Converter.convert("", short.class);
        assert (int) 0 == Converter.convert("", int.class);
        assert (long) 0 == Converter.convert("", long.class);
        assert 0.0f == Converter.convert("", float.class);
        assert 0.0d == Converter.convert("", double.class);
        assertEquals(BigDecimal.ZERO, Converter.convert("", BigDecimal.class));
        assertEquals(BigInteger.ZERO, Converter.convert("", BigInteger.class));
        assertEquals(new AtomicBoolean(false).get(), Converter.convert("", AtomicBoolean.class).get());
        assertEquals(new AtomicInteger(0).get(), Converter.convert("", AtomicInteger.class).get());
        assertEquals(new AtomicLong(0L).get(), Converter.convert("", AtomicLong.class).get());
    }

    @Test
    public void testEnumSupport()
    {
        assertEquals("foo", Converter.convert(foo, String.class));
        assertEquals("bar", Converter.convert(bar, String.class));
    }
}
