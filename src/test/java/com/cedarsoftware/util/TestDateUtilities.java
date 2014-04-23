package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
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
    public void testConstructorIsPrivate() throws Exception {
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
    public void testParseErrors()
    {
        try
        {
            DateUtilities.parseDate("2014-11-j 16:43:27.123");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 24:43:27.123");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 23:61:27.123");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("2014-6-10 23:00:75.123");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("27 Jume 2014");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("13/01/2014");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            DateUtilities.parseDate("12/32/2014");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }
    }
}