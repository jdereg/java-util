package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class TestDateUtilities
{
    @Test
    void testXmlDates()
    {
        Date t12 = DateUtilities.parseDate("2013-08-30T22:00Z");
        Date t22 = DateUtilities.parseDate("2013-08-30T22:00+00:00");
        Date t32 = DateUtilities.parseDate("2013-08-30T22:00-00:00");
        Date t42 = DateUtilities.parseDate("2013-08-30T22:00+0000");
        Date t52 = DateUtilities.parseDate("2013-08-30T22:00-0000");
        Date t62 = DateUtilities.parseDate("2013-08-30T22:00+00:00:01");
        assertEquals(t12, t22);
        assertEquals(t22, t32);
        assertEquals(t32, t42);
        assertEquals(t42, t52);
        assertNotEquals(t52, t62);

        Date t11 = DateUtilities.parseDate("2013-08-30T22:00:00Z");
        Date t21 = DateUtilities.parseDate("2013-08-30T22:00:00+00:00");
        Date t31 = DateUtilities.parseDate("2013-08-30T22:00:00-00:00");
        Date t41 = DateUtilities.parseDate("2013-08-30T22:00:00+0000");
        Date t51 = DateUtilities.parseDate("2013-08-30T22:00:00-0000");
        Date t61 = DateUtilities.parseDate("2013-08-30T22:00:00-00:00:00");
        assertEquals(t11, t12);
        assertEquals(t11, t21);
        assertEquals(t21, t31);
        assertEquals(t31, t41);
        assertEquals(t41, t51);
        assertEquals(t51, t61);

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
    void testXmlDatesWithOffsets()
    {
        Date t1 = DateUtilities.parseDate("2013-08-30T22:00Z");
        Date t2 = DateUtilities.parseDate("2013-08-30T22:00+01:00");
        assertEquals(60 * 60 * 1000, t1.getTime() - t2.getTime());

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
    void testXmlDatesWithMinuteOffsets()
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
    void testConstructorIsPrivate() throws Exception
    {
        Class<?> c = DateUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<?> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    void testDateAloneNumbers()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, Calendar.JANUARY, 18, 0, 0, 0);
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
    void testDateAloneNames()
    {
        Date d1 = DateUtilities.parseDate("2014 Jan 18");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, Calendar.JANUARY, 18, 0, 0, 0);
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
    void testDate24TimeParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, Calendar.JANUARY, 18, 16, 43, 0);
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
    void testDate24TimeSecParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43:27");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, Calendar.JANUARY, 18, 16, 43, 27);
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("2014/1/18 16:43:27");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("1/18/2014 16:43:27");
        assertEquals(c.getTime(), d1);
        d1 = DateUtilities.parseDate("01/18/2014 16:43:27");
        assertEquals(c.getTime(), d1);
    }

    @Test
    void testDate24TimeSecMilliParse()
    {
        Date d1 = DateUtilities.parseDate("2014-01-18 16:43:27.123");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2014, Calendar.JANUARY, 18, 16, 43, 27);
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
    void testParseWithNull()
    {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("     "));
    }

    @Test
    void testDayOfWeek()
    {
        for (int i=0; i < 1; i++) {
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
        }
        try {
            TemporalAccessor dateTime = DateUtilities.parseDate("text Dec 25, 2014", ZoneId.systemDefault(), true);
            fail();
        } catch (Exception ignored) { }

        try {
            DateUtilities.parseDate("Dec 25, 2014 text", ZoneId.systemDefault(), true);
            fail();
        } catch (Exception ignored) { }
    }

    @Test
    void testDaySuffixesLower()
    {
        for (int i=0; i < 1; i++) {
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
    }

    @Test
    void testDaySuffixesUpper()
    {
        for (int i=0; i < 1; i++) {
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
    }

    @Test
    void testWeirdSpacing()
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
    void test2DigitYear()
    {
        try {
            DateUtilities.parseDate("07/04/19");
            fail("should not make it here");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    void testDatePrecision()
    {
        Date x = DateUtilities.parseDate("2021-01-13T13:01:54.6747552-05:00");
        Date y = DateUtilities.parseDate("2021-01-13T13:01:55.2589242-05:00");
        assertTrue(x.compareTo(y) < 0);
    }

    @Test
    void testDateToStringFormat()
    {
        List<String> timeZoneOldSchoolNames = Arrays.asList("JST", "IST", "CET", "BST", "EST", "CST", "MST", "PST", "CAT", "EAT", "ART", "ECT", "NST", "AST", "HST");
        Date x = new Date();
        String dateToString = x.toString();
        boolean isOldSchoolTimezone = false;
        for (String zoneName : timeZoneOldSchoolNames) {
            if (!dateToString.contains(zoneName)) {
                isOldSchoolTimezone = true;
            }
        }

        if (isOldSchoolTimezone) {
            assertThatThrownBy(() -> DateUtilities.parseDate(x.toString()))
                    .isInstanceOf(DateTimeException.class)
                    .hasMessageContaining("Unknown time-zone ID");
        } else {
            Date y = DateUtilities.parseDate(x.toString());
            assertEquals(x.toString(), y.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"JST", "IST", "CET", "BST", "EST", "CST", "MST", "PST", "CAT", "EAT", "ART", "ECT", "NST", "AST", "HST"})
    void testTimeZoneValidShortNames(String timeZoneId) {
        // Support for some of the oldie but goodies (when the TimeZone returned does not have a 0 offset)
        Date date = DateUtilities.parseDate("2021-01-13T13:01:54.6747552 " + timeZoneId);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));
        calendar.clear();
        calendar.set(2021, Calendar.JANUARY, 13, 13, 1, 54);
        assert date.getTime() - calendar.getTime().getTime() == 674;    // less than 1000 millis
    }

    @Test
    void testTimeZoneLongName()
    {
        DateUtilities.parseDate("2021-01-13T13:01:54.6747552 Asia/Saigon");
        DateUtilities.parseDate("2021-01-13T13:01:54.6747552 America/New_York");

        assertThatThrownBy(() -> DateUtilities.parseDate("2021-01-13T13:01:54 Mumbo/Jumbo"))
                .isInstanceOf(java.time.zone.ZoneRulesException.class)
                .hasMessageContaining("Unknown time-zone ID: Mumbo/Jumbo");
    }

    @Test
    void testOffsetTimezone()
    {
        Date london = DateUtilities.parseDate("2024-01-06T00:00:01 GMT");
        Date london_pos_short_offset = DateUtilities.parseDate("2024-01-6T00:00:01+00");
        Date london_pos_med_offset = DateUtilities.parseDate("2024-01-6T00:00:01+0000");
        Date london_pos_offset = DateUtilities.parseDate("2024-01-6T00:00:01+00:00");
        Date london_neg_short_offset = DateUtilities.parseDate("2024-01-6T00:00:01-00");
        Date london_neg_med_offset = DateUtilities.parseDate("2024-01-6T00:00:01-0000");
        Date london_neg_offset = DateUtilities.parseDate("2024-01-6T00:00:01-00:00");
        Date london_z = DateUtilities.parseDate("2024-01-6T00:00:01Z");
        Date london_utc = DateUtilities.parseDate("2024-01-06T00:00:01 UTC");
        
        assertEquals(london, london_pos_short_offset);
        assertEquals(london_pos_short_offset, london_pos_med_offset);
        assertEquals(london_pos_med_offset, london_pos_short_offset);
        assertEquals(london_pos_short_offset, london_pos_offset);
        assertEquals(london_pos_offset, london_neg_short_offset);
        assertEquals(london_neg_short_offset, london_neg_med_offset);
        assertEquals(london_neg_med_offset, london_neg_offset);
        assertEquals(london_neg_offset, london_z);
        assertEquals(london_z, london_utc);

        Date ny = DateUtilities.parseDate("2024-01-06T00:00:01 America/New_York");
        assert ny.getTime() - london.getTime() == 5*60*60*1000;

        Date ny_offset = DateUtilities.parseDate("2024-01-6T00:00:01-5");
        assert ny_offset.getTime() - london.getTime() == 5*60*60*1000;

        Date la_offset = DateUtilities.parseDate("2024-01-6T00:00:01-08:00");
        assert la_offset.getTime() - london.getTime() == 8*60*60*1000;
    }

    @Test
    void testTimeBeforeDate()
    {
        Date x = DateUtilities.parseDate("13:01:54 2021-01-14");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2021, Calendar.JANUARY, 14, 13, 1, 54);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("13:01:54T2021-01-14");
        c.clear();
        c.set(2021, Calendar.JANUARY, 14, 13, 1, 54);
        assertEquals(x, c.getTime());

        x = DateUtilities.parseDate("13:01:54.1234567T2021-01-14");
        c.clear();
        c.set(2021, Calendar.JANUARY, 14, 13, 1, 54);
        c.set(Calendar.MILLISECOND, 123);
        assertEquals(x, c.getTime());

        DateUtilities.parseDate("13:01:54.1234567ZT2021-01-14");
        DateUtilities.parseDate("13:01:54.1234567-10T2021-01-14");
        DateUtilities.parseDate("13:01:54.1234567-10:00T2021-01-14");
        x = DateUtilities.parseDate("13:01:54.1234567 America/New_York T2021-01-14");
        Date y = DateUtilities.parseDate("13:01:54.1234567-0500T2021-01-14");
        assertEquals(x, y);
    }

    @Test
    void testParseErrors()
    {
        try {
            DateUtilities.parseDate("2014-11-j 16:43:27.123");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("2014-6-10 24:43:27.123");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("2014-6-10 23:61:27.123");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("2014-6-10 23:00:75.123");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("27 Jume 2014");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("13/01/2014");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("00/01/2014");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("12/32/2014");
            fail("should not make it here");
        } catch (Exception ignored) {}

        try {
            DateUtilities.parseDate("12/00/2014");
            fail("should not make it here");
        } catch (Exception ignored) {}
    }

    @ParameterizedTest
    @ValueSource(strings = {"JST", "IST", "CET", "BST", "EST", "CST", "MST", "PST", "CAT", "EAT", "ART", "ECT", "NST", "AST", "HST"})
    void testMacUnixDateFormat(String timeZoneId)
    {
        Date date = DateUtilities.parseDate("Sat Jan  6 20:06:58 " + timeZoneId + " 2024");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));
        calendar.clear();
        calendar.set(2024, Calendar.JANUARY, 6, 20, 6, 58);
        assertEquals(calendar.getTime(), date);
    }

    @Test
    void testUnixDateFormat()
    {
        Date date = DateUtilities.parseDate("Sat Jan  6 20:06:58 2024");
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2024, Calendar.JANUARY, 6, 20, 6, 58);
        assertEquals(calendar.getTime(), date);
    }

    @Test
    void testInconsistentDateSeparators()
    {
        assertThatThrownBy(() -> DateUtilities.parseDate("12/24-1996"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to parse: 12/24-1996 as a date");
        
        assertThatThrownBy(() -> DateUtilities.parseDate("1996-12/24"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to parse: 1996-12/24 as a date");
    }

    @Test
    void testBadTimeSeparators()
    {
        assertThatThrownBy(() -> DateUtilities.parseDate("12/24/1996 12.49.58", ZoneId.systemDefault(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Issue parsing date-time, other characters present: 12.49.58");

        assertThatThrownBy(() -> DateUtilities.parseDate("12.49.58 12/24/1996", ZoneId.systemDefault(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Issue parsing date-time, other characters present: 12.49.58");

        Date date = DateUtilities.parseDate("12:49:58 12/24/1996"); // time with valid separators before date
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(1996, Calendar.DECEMBER, 24, 12, 49, 58);
        assertEquals(calendar.getTime(), date);

        assertThatThrownBy(() -> DateUtilities.parseDate("12/24/1996 12-49-58", ZoneId.systemDefault(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Issue parsing date-time, other characters present: 12-49-58");
    }
    
    @Test
    void testEpochMillis()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        // 12 digits - 0 case
        Date date = DateUtilities.parseDate("000000000000");
        String gmtDateString = sdf.format(date);
        assertEquals("1970-01-01 00:00:00.000", gmtDateString);

        // 12 digits - 1 case
        date = DateUtilities.parseDate("000000000001");
        gmtDateString = sdf.format(date);
        assertEquals("1970-01-01 00:00:00.001", gmtDateString);

        // 18 digits - 1 case
        date = DateUtilities.parseDate("000000000000000001");
        gmtDateString = sdf.format(date);
        assertEquals("1970-01-01 00:00:00.001", gmtDateString);

        // 18 digits - max case
        date = DateUtilities.parseDate("999999999999999999");
        gmtDateString = sdf.format(date);
        assertEquals("31690708-07-05 01:46:39.999", gmtDateString);
    }

    private static Stream provideTimeZones()
    {
        return Stream.of(
                Arguments.of("2024-01-19T15:30:45[Europe/London]", 1705678245000L),
                Arguments.of("2024-01-19T10:15:30[Asia/Tokyo]", 1705626930000L),
                Arguments.of("2024-01-19T20:45:00[America/New_York]", 1705715100000L),
                Arguments.of("2024-01-19T15:30:45 Europe/London", 1705678245000L),
                Arguments.of("2024-01-19T10:15:30 Asia/Tokyo", 1705626930000L),
                Arguments.of("2024-01-19T20:45:00 America/New_York", 1705715100000L),
                Arguments.of("2024-01-19T07:30GMT", 1705649400000L),
                Arguments.of("2024-01-19T07:30[GMT]", 1705649400000L),
                Arguments.of("2024-01-19T07:30 GMT", 1705649400000L),
                Arguments.of("2024-01-19T07:30 [GMT]", 1705649400000L),
                Arguments.of("2024-01-19T07:30  GMT", 1705649400000L),
                Arguments.of("2024-01-19T07:30  [GMT] ", 1705649400000L),

                Arguments.of("2024-01-19T07:30  GMT ", 1705649400000L),
                Arguments.of("2024-01-19T07:30:01 GMT", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01 [GMT]", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01GMT", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01[GMT]", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01.1 GMT", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.1 [GMT]", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.1GMT", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.1[GMT]", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12GMT", 1705649401120L),

                Arguments.of("2024-01-19T07:30:01Z", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01.1Z", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12Z", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01UTC", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01.1UTC", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12UTC", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01[UTC]", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01.1[UTC]", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12[UTC]", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01 UTC", 1705649401000L),

                Arguments.of("2024-01-19T07:30:01.1 UTC", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12 UTC", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01 [UTC]", 1705649401000L),
                Arguments.of("2024-01-19T07:30:01.1 [UTC]", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12 [UTC]", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01.1 UTC", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12 UTC", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01.1 [UTC]", 1705649401100L),
                Arguments.of("2024-01-19T07:30:01.12 [UTC]", 1705649401120L),

                Arguments.of("2024-01-19T07:30:01.12[GMT]", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01.12 GMT", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01.12 [GMT]", 1705649401120L),
                Arguments.of("2024-01-19T07:30:01.123GMT", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.123[GMT]", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.123 GMT", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.123 [GMT]", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.1234GMT", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.1234[GMT]", 1705649401123L),
                Arguments.of("2024-01-19T07:30:01.1234 GMT", 1705649401123L),

                Arguments.of("2024-01-19T07:30:01.1234 [GMT]", 1705649401123L),

                Arguments.of("07:30EST 2024-01-19", 1705667400000L),
                Arguments.of("07:30[EST] 2024-01-19", 1705667400000L),
                Arguments.of("07:30 EST 2024-01-19", 1705667400000L),

                Arguments.of("07:30 [EST] 2024-01-19", 1705667400000L),
                Arguments.of("07:30:01EST 2024-01-19", 1705667401000L),
                Arguments.of("07:30:01[EST] 2024-01-19", 1705667401000L),
                Arguments.of("07:30:01 EST 2024-01-19", 1705667401000L),
                Arguments.of("07:30:01 [EST] 2024-01-19", 1705667401000L),
                Arguments.of("07:30:01.123 EST 2024-01-19", 1705667401123L),
                Arguments.of("07:30:01.123 [EST] 2024-01-19", 1705667401123L)
                );
    }

    @ParameterizedTest
    @MethodSource("provideTimeZones")
    void testTimeZoneParsing(String exampleZone, Long epochMilli)
    {
        Date date = DateUtilities.parseDate(exampleZone);
        assertEquals(date.getTime(), epochMilli);


        TemporalAccessor dateTime = DateUtilities.parseDate(exampleZone, ZoneId.systemDefault(), true);
        ZonedDateTime zdt = (ZonedDateTime) dateTime;

        assertEquals(zdt.toInstant().toEpochMilli(), epochMilli);
    }

    @Test
    void testTimeBetterThanMilliResolution()
    {
        ZonedDateTime zonedDateTime = (ZonedDateTime) DateUtilities.parseDate("Jan 22nd, 2024 21:52:05.123456789-05:00", ZoneId.systemDefault(), true);
        assertEquals(123456789, zonedDateTime.getNano());
        assertEquals(2024, zonedDateTime.getYear());
        assertEquals(1, zonedDateTime.getMonthValue());
        assertEquals(22, zonedDateTime.getDayOfMonth());
        assertEquals(21, zonedDateTime.getHour());
        assertEquals(52, zonedDateTime.getMinute());
        assertEquals(5, zonedDateTime.getSecond());
        assertEquals(123456789, zonedDateTime.getNano());
        assertEquals(ZoneId.of("GMT-0500"), zonedDateTime.getZone());
        assertEquals(-60*60*5, zonedDateTime.getOffset().getTotalSeconds());
    }

    private static Stream provideRedundantFormats() {
        return Stream.of(
                Arguments.of("2024-01-19T12:00:00-08:00[America/Los_Angeles]"),
                Arguments.of("2024-01-19T22:30:00+01:00[Europe/Paris]"),
                Arguments.of("2024-01-19T18:15:45+10:00[Australia/Sydney]"),
                Arguments.of("2024-01-19T05:00:00-03:00[America/Sao_Paulo]"),
                Arguments.of("2024-01-19T14:30:00+05:30[Asia/Kolkata]"),
                Arguments.of("2024-01-19T21:45:00-05:00[America/Toronto]"),
                Arguments.of("2024-01-19T16:00:00+02:00[Africa/Cairo]"),
                Arguments.of("2024-01-19T07:30:00-07:00[America/Denver]"),
                Arguments.of("2024-01-19T18:15:45+10:00 Australia/Sydney"),
                Arguments.of("2024-01-19T05:00:00-03:00 America/Sao_Paulo"),
                Arguments.of("2024-01-19T14:30:00+05:30 Asia/Kolkata"),
                Arguments.of("2024-01-19T21:45:00-05:00 America/Toronto"),
                Arguments.of("2024-01-19T16:00:00+02:00 Africa/Cairo"),
                Arguments.of("2024-01-19T07:30:00-07:00 America/Denver"),
                Arguments.of("2024-01-19T12:00:00-08:00 America/Los_Angeles"),
                Arguments.of("2024-01-19T22:30:00+01:00 Europe/Paris"),
                Arguments.of("2024-01-19T23:59:59Z UTC"),
                Arguments.of("2024-01-19T23:59:59Z[UTC]"),
                Arguments.of("2024-01-19T07:30:01.123+0100GMT"),
                Arguments.of("2024-01-19T07:30:01.123+0100[GMT]"),
                Arguments.of("2024-01-19T07:30:01.123+0100 GMT"),
                Arguments.of("2024-01-19T07:30:01.123+0100 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123-1000GMT"),
                Arguments.of("2024-01-19T07:30:01.123-1000 GMT"),
                Arguments.of("2024-01-19T07:30:01.123-1000 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123+2 GMT"),
                Arguments.of("2024-01-19T07:30:01.123+2 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123-2 GMT"),
                Arguments.of("2024-01-19T07:30:01.123-2 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123+2GMT"),
                Arguments.of("2024-01-19T07:30:01.123+2[GMT]"),
                Arguments.of("2024-01-19T07:30:01.123-2GMT"),
                Arguments.of("2024-01-19T07:30:01.123-2[GMT]"),
                Arguments.of("2024-01-19T07:30:01.123+18 GMT"),
                Arguments.of("2024-01-19T07:30:01.123+18 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123-18 GMT"),
                Arguments.of("2024-01-19T07:30:01.123-18 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123+18:00 GMT"),
                Arguments.of("2024-01-19T07:30:01.123+18:00 [GMT]"),
                Arguments.of("2024-01-19T07:30:01.123-18:00 GMT"),
                Arguments.of("2024-01-19T07:30:01.123-18:00 [GMT]"),
                Arguments.of("2024-01-19T07:30:00+10 EST"),
                Arguments.of("07:30:01.123+1100 EST 2024-01-19"),
                Arguments.of("07:30:01.123-1100 [EST] 2024-01-19"),
                Arguments.of("07:30:01.123+11:00 [EST] 2024-01-19"),
                Arguments.of("07:30:01.123-11:00 [EST] 2024-01-19"),
                Arguments.of("Wed 07:30:01.123-11:00 [EST] 2024-01-19"),
                Arguments.of("07:30:01.123-11:00 [EST] 2024-01-19 Wed"),
                Arguments.of("07:30:01.123-11:00 [EST] Sunday, January 21, 2024"),
                Arguments.of("07:30:01.123-11:00 [EST] Sunday January 21, 2024"),
                Arguments.of("07:30:01.123-11:00 [EST] January 21, 2024 Sunday"),
                Arguments.of("07:30:01.123-11:00 [EST] January 21, 2024, Sunday"),
                Arguments.of("07:30:01.123-11:00 [America/New_York] January 21, 2024, Sunday"),
                Arguments.of("07:30:01.123-11:00 [Africa/Cairo] 21 Jan 2024 Sun"),
                Arguments.of("07:30:01.123-11:00 [Africa/Cairo] 2024 Jan 21st Sat")
                );
    }

    @ParameterizedTest
    @MethodSource("provideRedundantFormats")
    void testFormatsThatShouldNotWork(String badFormat)
    {
        DateUtilities.parseDate(badFormat, ZoneId.systemDefault(), true);
    }
}
