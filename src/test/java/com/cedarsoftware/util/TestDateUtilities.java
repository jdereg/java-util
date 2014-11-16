package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class TestDateUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception
    {
        Class c = DateUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<DateUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
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
    public void testDateToStringFormat()
    {
        Date x = new Date();
        Date y = DateUtilities.parseDate(x.toString());
        assertEquals(x.toString(), y.toString());
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