package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestConverter
{
    @Test
    public void testByte()
    {
        Byte x = (Byte) Converter.convert("-25", byte.class);
        assertTrue(-25 == x);
        x = (Byte) Converter.convert("24", Byte.class);
        assertTrue(24 == x);

        x = (Byte) Converter.convert((byte) 100, byte.class);
        assertTrue(100 == x);
        x = (Byte) Converter.convert((byte) 120, Byte.class);
        assertTrue(120 == x);

        x = (Byte) Converter.convert(new BigDecimal("100"), byte.class);
        assertTrue(100 == x);
        x = (Byte) Converter.convert(new BigInteger("120"), Byte.class);
        assertTrue(120 == x);

        assertEquals((byte)1, Converter.convert(true, Byte.class));
        assertEquals((byte)0, Converter.convert(false, byte.class));

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
        Short x = (Short) Converter.convert("-25000", short.class);
        assertTrue(-25000 == x);
        x = (Short) Converter.convert("24000", Short.class);
        assertTrue(24000 == x);

        x = (Short) Converter.convert((short) 10000, short.class);
        assertTrue(10000 == x);
        x = (Short) Converter.convert((short) 20000, Short.class);
        assertTrue(20000 == x);

        x = (Short) Converter.convert(new BigDecimal("10000"), short.class);
        assertTrue(10000 == x);
        x = (Short) Converter.convert(new BigInteger("20000"), Short.class);
        assertTrue(20000 == x);

        assertEquals((short)1, Converter.convert(true, short.class));
        assertEquals((short)0, Converter.convert(false, Short.class));

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
        Integer x = (Integer) Converter.convert("-450000", int.class);
        assertEquals((Object)(-450000), x);
        x = (Integer) Converter.convert("550000", Integer.class);
        assertEquals((Object)550000, x);

        x = (Integer) Converter.convert(100000, int.class);
        assertEquals((Object)100000, x);
        x = (Integer) Converter.convert(200000, Integer.class);
        assertEquals((Object)200000, x);

        x = (Integer) Converter.convert(new BigDecimal("100000"), int.class);
        assertEquals((Object)100000, x);
        x = (Integer) Converter.convert(new BigInteger("200000"), Integer.class);
        assertEquals((Object)200000, x);

        assertEquals(1, Converter.convert(true, Integer.class));
        assertEquals(0, Converter.convert(false, int.class));

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
        Long x = (Long) Converter.convert("-450000", long.class);
        assertEquals((Object)(-450000L), x);
        x = (Long) Converter.convert("550000", Long.class);
        assertEquals((Object)550000L, x);

        x = (Long) Converter.convert(100000L, long.class);
        assertEquals((Object)100000L, x);
        x = (Long) Converter.convert(200000L, Long.class);
        assertEquals((Object)200000L, x);

        x = (Long) Converter.convert(new BigDecimal("100000"), long.class);
        assertEquals((Object)100000L, x);
        x = (Long) Converter.convert(new BigInteger("200000"), Long.class);
        assertEquals((Object)200000L, x);

        assertEquals((long)1, Converter.convert(true, long.class));
        assertEquals((long)0, Converter.convert(false, Long.class));

        Date now = new Date();
        long now70 = now.getTime();
        assertEquals(now70, Converter.convert(now, long.class));

        Calendar today = Calendar.getInstance();
        now70 = today.getTime().getTime();
        assertEquals(now70, Converter.convert(today, Long.class));

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
        BigDecimal x = (BigDecimal) Converter.convert("-450000", BigDecimal.class);
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
        BigInteger x = (BigInteger) Converter.convert("-450000", BigInteger.class);
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
    public void testDate()
    {
        // Date to Date
        Date utilNow = new Date();
        Date coerced = (Date) Converter.convert(utilNow, Date.class);
        assertEquals(utilNow, coerced);
        assertTrue(coerced instanceof Date);
        assertFalse(coerced instanceof java.sql.Date);

        // Date to java.sql.Date
        java.sql.Date sqlCoerced = (java.sql.Date) Converter.convert(utilNow, java.sql.Date.class);
        assertEquals(utilNow, sqlCoerced);
        assertTrue(sqlCoerced instanceof Date);
        assertTrue(sqlCoerced instanceof java.sql.Date);

        // java.sql.Date to java.sql.Date
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());
        sqlCoerced = (java.sql.Date) Converter.convert(sqlNow, java.sql.Date.class);
        assertEquals(sqlNow, sqlCoerced);
        assertTrue(sqlCoerced instanceof Date);
        assertTrue(sqlCoerced instanceof java.sql.Date);

        // java.sql.Date to Date
        coerced = (Date) Converter.convert(sqlNow, Date.class);
        assertEquals(sqlNow, coerced);
        assertTrue(coerced instanceof Date);
        assertFalse(coerced instanceof java.sql.Date);

        // String to Date
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);
        Date date = (Date) Converter.convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assertTrue(date instanceof Date);
        assertFalse(date instanceof java.sql.Date);

        // String to java.sql.Date
        java.sql.Date sqlDate = (java.sql.Date) Converter.convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals(cal.getTime(), sqlDate);
        assertTrue(sqlDate instanceof Date);
        assertTrue(sqlDate instanceof java.sql.Date);

        // Calendar to Date
        date = (Date) Converter.convert(cal, Date.class);
        assertEquals(date, cal.getTime());
        assertTrue(date instanceof Date);
        assertFalse(date instanceof java.sql.Date);

        // Calendar to java.sql.Date
        sqlDate = (java.sql.Date) Converter.convert(cal, java.sql.Date.class);
        assertEquals(sqlDate, cal.getTime());
        assertTrue(sqlDate instanceof Date);
        assertTrue(sqlDate instanceof java.sql.Date);

        // long to Date
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);
        Date converted = (Date) Converter.convert(now, Date.class);
        assertEquals(dateNow, converted);
        assertTrue(converted instanceof Date);
        assertFalse(converted instanceof java.sql.Date);

        // long to java.sql.Date
        Date sqlConverted = (java.sql.Date) Converter.convert(now, java.sql.Date.class);
        assertEquals(dateNow, sqlConverted);
        assertTrue(sqlConverted instanceof Date);
        assertTrue(sqlConverted instanceof java.sql.Date);

        // Invalid source type for Date
        try
        {
            Converter.convert(TimeZone.getDefault(), Date.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
        }

        // Invalid source type for java.sql.Date
        try
        {
            Converter.convert(TimeZone.getDefault(), java.sql.Date.class);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("could not be converted"));
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
    public void testFloat()
    {
        assertEquals(-3.14f, Converter.convert(-3.14f, float.class));
        assertEquals(-3.14f, Converter.convert(-3.14f, Float.class));
        assertEquals(-3.14f, Converter.convert("-3.14", float.class));
        assertEquals(-3.14f, Converter.convert("-3.14", Float.class));
        assertEquals(-3.14f, Converter.convert(-3.14d, float.class));
        assertEquals(-3.14f, Converter.convert(-3.14d, Float.class));
        assertEquals(1.0f, Converter.convert(true, float.class));
        assertEquals(1.0f, Converter.convert(true, Float.class));
        assertEquals(0.0f, Converter.convert(false, float.class));
        assertEquals(0.0f, Converter.convert(false, Float.class));

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
        assertEquals(-3.14d, Converter.convert(-3.14d, double.class));
        assertEquals(-3.14d, Converter.convert(-3.14d, Double.class));
        assertEquals(-3.14d, Converter.convert("-3.14", double.class));
        assertEquals(-3.14d, Converter.convert("-3.14", Double.class));
        assertEquals(-3.14d, Converter.convert(new BigDecimal("-3.14"), double.class));
        assertEquals(-3.14d, Converter.convert(new BigDecimal("-3.14"), Double.class));
        assertEquals(1.0d, Converter.convert(true, double.class));
        assertEquals(1.0d, Converter.convert(true, Double.class));
        assertEquals(0.0d, Converter.convert(false, double.class));
        assertEquals(0.0d, Converter.convert(false, Double.class));

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
}
