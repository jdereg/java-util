package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class TestDateUtilities
{
    @Test
    public void testXmlDates()
    {
        Date t12 = DateUtilities.parseDate("2013-08-30T22:00Z");
        Date t22 = DateUtilities.parseDate("2013-08-30T22:00+00:00");
        Date t32 = DateUtilities.parseDate("2013-08-30T22:00-00:00");
        Date t42 = DateUtilities.parseDate("2013-08-30T22:00+0000");
        Date t52 = DateUtilities.parseDate("2013-08-30T22:00-0000");
        assertEquals(t12, t22);
        assertEquals(t22, t32);
        assertEquals(t32, t42);
        assertEquals(t42, t52);

        Date t11 = DateUtilities.parseDate("2013-08-30T22:00:00Z");
        Date t21 = DateUtilities.parseDate("2013-08-30T22:00:00+00:00");
        Date t31 = DateUtilities.parseDate("2013-08-30T22:00:00-00:00");
        Date t41 = DateUtilities.parseDate("2013-08-30T22:00:00+0000");
        Date t51 = DateUtilities.parseDate("2013-08-30T22:00:00-0000");
        assertEquals(t11, t12);
        assertEquals(t11, t21);
        assertEquals(t21, t31);
        assertEquals(t31, t41);
        assertEquals(t41, t51);

        Date t1 = DateUtilities.parseDate("2013-08-30T22:00:00.0Z");
        Date t2 = DateUtilities.parseDate("2013-08-30T22:00:00.0+00:00");
        Date t3 = DateUtilities.parseDate("2013-08-30T22:00:00.0-00:00");
        Date t4 = DateUtilities.parseDate("2013-08-30T22:00:00.0+0000");
        Date t5 = DateUtilities.parseDate("2013-08-30T22:00:00.0-0000");
        assertEquals(t1, t11);
        assertEquals(t1, t2);
        assertEquals(t2, t3);
        assertEquals(t3, t4);
        assertEquals(t4, t5);

        Date t13 = DateUtilities.parseDate("2013-08-30T22:00:00.000000000Z");
        Date t23 = DateUtilities.parseDate("2013-08-30T22:00:00.000000000+00:00");
        Date t33 = DateUtilities.parseDate("2013-08-30T22:00:00.000000000-00:00");
        Date t43 = DateUtilities.parseDate("2013-08-30T22:00:00.000000000+0000");
        Date t53 = DateUtilities.parseDate("2013-08-30T22:00:00.000000000-0000");
        assertEquals(t13, t1);
        assertEquals(t13, t23);
        assertEquals(t23, t33);
        assertEquals(t33, t43);
        assertEquals(t43, t53);

        Date t14 = DateUtilities.parseDate("2013-08-30T22:00:00.123456789Z");
        Date t24 = DateUtilities.parseDate("2013-08-30T22:00:00.123456789+00:00");
        Date t34 = DateUtilities.parseDate("2013-08-30T22:00:00.123456789-00:00");
        Date t44 = DateUtilities.parseDate("2013-08-30T22:00:00.123456789+0000");
        Date t54 = DateUtilities.parseDate("2013-08-30T22:00:00.123456789-0000");
        assertNotEquals(t14, t13);
        assertEquals(t14, t24);
        assertEquals(t24, t34);
        assertEquals(t34, t44);
        assertEquals(t44, t54);
    }

    @Test
    public void testXmlDatesWithOffsets()
    {
        Date t1 = DateUtilities.parseDate("2013-08-30T22:00Z");
        Date t2 = DateUtilities.parseDate("2013-08-30T22:00+01:00");
        Date t3 = DateUtilities.parseDate("2013-08-30T22:00-01:00");
        Date t4 = DateUtilities.parseDate("2013-08-30T22:00+0100");
        Date t5 = DateUtilities.parseDate("2013-08-30T22:00-0100");

        assertEquals(60 * 60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t5.getTime());

        t1 = DateUtilities.parseDate("2013-08-30T22:17Z");
        t2 = DateUtilities.parseDate("2013-08-30T22:17+01:00");
        t3 = DateUtilities.parseDate("2013-08-30T22:17-01:00");
        t4 = DateUtilities.parseDate("2013-08-30T22:17+0100");
        t5 = DateUtilities.parseDate("2013-08-30T22:17-0100");

        assertEquals(60 * 60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t5.getTime());

        t1 = DateUtilities.parseDate("2013-08-30T22:17:34Z");
        t2 = DateUtilities.parseDate("2013-08-30T22:17:34+01:00");
        t3 = DateUtilities.parseDate("2013-08-30T22:17:34-01:00");
        t4 = DateUtilities.parseDate("2013-08-30T22:17:34+0100");
        t5 = DateUtilities.parseDate("2013-08-30T22:17:34-0100");

        assertEquals(60 * 60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t5.getTime());

        t1 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789Z");
        t2 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+01:00");
        t3 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-01:00");
        t4 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+0100");
        t5 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-0100");

        assertEquals(60 * 60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 60 * 1000, t1.getTime() - t5.getTime());

        t1 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789Z");
        t2 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+13:00");
        t3 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-13:00");
        t4 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+1300");
        t5 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-1300");

        assertEquals(60 * 60 * 1000 * 13, t1.getTime() - t2.getTime());
        assertEquals(-60 * 60 * 1000 * 13, t1.getTime() - t3.getTime());
        assertEquals(60 * 60 * 1000 * 13, t1.getTime() - t4.getTime());
        assertEquals(-60 * 60 * 1000 * 13, t1.getTime() - t5.getTime());
    }

    @Test
    public void testXmlDatesWithMinuteOffsets()
    {
        Date t1 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789Z");
        Date t2 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+00:01");
        Date t3 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-00:01");
        Date t4 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789+0001");
        Date t5 = DateUtilities.parseDate("2013-08-30T22:17:34.123456789-0001");

        assertEquals(60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 1000, t1.getTime() - t5.getTime());

        t1 = DateUtilities.parseDate("2013-08-30T22:17Z");
        t2 = DateUtilities.parseDate("2013-08-30T22:17+00:01");
        t3 = DateUtilities.parseDate("2013-08-30T22:17-00:01");
        t4 = DateUtilities.parseDate("2013-08-30T22:17+0001");
        t5 = DateUtilities.parseDate("2013-08-30T22:17-0001");

        assertEquals(60 * 1000, t1.getTime() - t2.getTime());
        assertEquals(-60 * 1000, t1.getTime() - t3.getTime());
        assertEquals(60 * 1000, t1.getTime() - t4.getTime());
        assertEquals(-60 * 1000, t1.getTime() - t5.getTime());
    }
    @Test
    public void testConstructorIsPrivate() throws Exception
    {
        Class<?> c = DateUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testDateAloneNumbers()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, 0, 18, 0, 0, 0);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/01/18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/1/18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("1/18/2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("01/18/2014");
        assertEquals(c.getTime(), d1);
    }

    @Test
    public void testDateAloneNames()
    {
        Date d1 = DateUtilities.parseDate("2014 Jan 18");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, 0, 18, 0, 0, 0);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014 January 18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014 January, 18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("18 Jan 2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("18 Jan, 2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("Jan 18 2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("Jan 18, 2014");
        assertEquals(c.getTime(), d1);
    }

    @Test
    public void testDate24TimeParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, 0, 18, 16, 43, 0);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/01/18 16:43");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/1/18 16:43");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("1/18/2014 16:43");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("01/18/2014 16:43");
        assertEquals(c.getTime(), d1);

        d1 = DateUtilities.parseDate("16:43 2014-01-18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43 2014/01/18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43 2014/1/18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43 1/18/2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43 01/18/2014");
        assertEquals(c.getTime(), d1);
    }

    @Test
    public void testDate24TimeSecParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43:27");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, 0, 18, 16, 43, 27);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/1/18 16:43:27");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("1/18/2014 16:43:27");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("01/18/2014 16:43:27");
        assertEquals(c.getTime(), d1);
    }

    @Test
    public void testDate24TimeSecMilliParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43:27.123");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, 0, 18, 16, 43, 27);
        c.setTimeInMillis(c.getTime().getTime() + 123);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/1/18 16:43:27.123");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("1/18/2014 16:43:27.123");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("01/18/2014 16:43:27.123");
        assertEquals(c.getTime(), d1);

        d1 = DateUtilities.parseDate("16:43:27.123 2014-01-18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43:27.123 2014/1/18");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43:27.123 1/18/2014");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("16:43:27.123 01/18/2014");
        assertEquals(c.getTime(), d1);
    }

    @Test
    public void testParseWithNull()
    {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("     "));
    }

    @Test
    public void testDayOfWeek()
    {
        DateUtilities.parseDate("thu, Dec 25, 2014");
        DateUtilities.parseDate("thur, Dec 25, 2014");
        DateUtilities.parseDate("thursday, December 25, 2014");

        DateUtilities.parseDate("Dec 25, 2014 thu");
        DateUtilities.parseDate("Dec 25, 2014 thur");
        DateUtilities.parseDate("Dec 25, 2014 thursday");

        DateUtilities.parseDate("thu Dec 25, 2014");
        DateUtilities.parseDate("thur Dec 25, 2014");
        DateUtilities.parseDate("thursday December 25, 2014");

        DateUtilities.parseDate(" thu, Dec 25, 2014 ");
        DateUtilities.parseDate(" thur, Dec 25, 2014 ");
        DateUtilities.parseDate(" thursday, Dec 25, 2014 ");

        DateUtilities.parseDate(" thu Dec 25, 2014 ");
        DateUtilities.parseDate(" thur Dec 25, 2014 ");
        DateUtilities.parseDate(" thursday Dec 25, 2014 ");

        DateUtilities.parseDate(" Dec 25, 2014, thu ");
        DateUtilities.parseDate(" Dec 25, 2014, thur ");
        DateUtilities.parseDate(" Dec 25, 2014, thursday ");

        try
        {
            DateUtilities.parseDate("text Dec 25, 2014");
            fail();
        }
        catch (Exception ignored)
        { }

        try
        {
            DateUtilities.parseDate("Dec 25, 2014 text");
            fail();
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testDaySuffixesLower()
    {
        Date x = DateUtilities.parseDate("January 21st, 1994");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(1994, Calendar.JANUARY, 21, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("January 22nd 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("Jan 23rd 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 23, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("June 24th, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("21st January, 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 21, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("22nd January 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("23rd Jan 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 23, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("24th June, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("24th, June, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());
    }

    @Test
    public void testDaySuffixesUpper()
    {
        Date x = DateUtilities.parseDate("January 21ST, 1994");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(1994, Calendar.JANUARY, 21, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("January 22ND 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("Jan 23RD 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 23, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("June 24TH, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("21ST January, 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 21, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("22ND January 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("23RD Jan 1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 23, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("24TH June, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("24TH, June, 1994");
        c.clear();
        c.set(1994, Calendar.JUNE, 24, 0, 0, 0);
        assertEquals(x, c.getTime());
    }

    @Test
    public void testWeirdSpacing()
    {
        Date x = DateUtilities.parseDate("January    21ST  ,   1994");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(1994, Calendar.JANUARY, 21, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("January    22ND    1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("January    22ND    1994   Wed");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate(" Wednesday January    22ND    1994  ");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("22ND    January    1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("22ND    January  ,  1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("22ND  ,  Jan  ,  1994");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("1994 ,  Jan    22ND");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("1994  ,  January  ,  22nd");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("1994 ,  Jan    22ND Wed");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("Wed 1994  ,  January  ,  22nd");
        c.clear();
        c.set(1994, Calendar.JANUARY, 22, 0, 0, 0);
        assertEquals(x, c.getTime());
    }

    @Test
    public void test2DigitYear()
    {
        try
        {
            DateUtilities.parseDate("07/04/19");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    @Test
    public void testDateToStringFormat()
    {
        Date x = new Date();
        Date y = DateUtilities.parseDate(x.toString());
        assertEquals(x.toString(), y.toString());
    }

    @Test
    public void testDatePrecision()
    {
        Date x = DateUtilities.parseDate("2021-01-13T13:01:54.6747552-05:00");
        Date y = DateUtilities.parseDate("2021-01-13T13:01:55.2589242-05:00");
        assertTrue(x.compareTo(y) < 0);
    }

    @Test
    public void testParseErrors()
    {
        try
        {
            DateUtilities.parseDate("2014-11-j 16:43:27.123");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 24:43:27.123");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 23:61:27.123");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 23:00:75.123");
            fail("should not make it here");
        }
        catch (Exception igored)
        {
        }

        try
        {
            DateUtilities.parseDate("27 Jume 2014");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("13/01/2014");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("00/01/2014");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("12/32/2014");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            DateUtilities.parseDate("12/00/2014");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }
    }
}