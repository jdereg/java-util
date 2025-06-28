package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

import static com.cedarsoftware.util.DateUtilities.ABBREVIATION_TO_TIMEZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class DateUtilitiesTest
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
        boolean okToTest = false;

        for (String zoneName : timeZoneOldSchoolNames) {
            if (dateToString.contains(" " + zoneName)) {
                okToTest = true;
                break;
            }
        }

        if (okToTest) {
            Date y = DateUtilities.parseDate(x.toString());
            assertEquals(x.toString(), y.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"JST", "IST", "CET", "BST", "EST", "CST", "MST", "PST", "CAT", "EAT", "ART", "ECT", "NST", "AST", "HST"})
    void testTimeZoneValidShortNames(String timeZoneId) {
        String resolvedId = ABBREVIATION_TO_TIMEZONE.get(timeZoneId);
        if (resolvedId == null) {
            // fallback
            resolvedId = timeZoneId;
        }

        // Support for some of the oldie but goodies (when the TimeZone returned does not have a 0 offset)
        Date date = DateUtilities.parseDate("2021-01-13T13:01:54.6747552 " + timeZoneId);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(resolvedId));
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
        String resolvedId = ABBREVIATION_TO_TIMEZONE.get(timeZoneId);
        if (resolvedId == null) {
            // fallback
            resolvedId = timeZoneId;
        }

        Date date = DateUtilities.parseDate("Sat Jan  6 20:06:58 " + timeZoneId + " 2024");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(resolvedId));
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
    void testEpochMillis2()
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

    @Test
    void testParseInvalidTimeZoneFormats() {
        // Test with named timezone without time - should fail
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05[Asia/Tokyo]", ZoneId.of("Z"), false),
                "Should fail with timezone but no time");

        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05[Asia/Tokyo]", ZoneId.of("Z"), true),
                "Should fail with timezone but no time");

        // Test with offset without time - should fail
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05+09:00", ZoneId.of("Z"), false),
                "Should fail with offset but no time");

        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05+09:00", ZoneId.of("Z"), true),
                "Should fail with offset but no time");

        // Test with Z without time - should fail
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05Z", ZoneId.of("Z"), false),
                "Should fail with Z but no time");

        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05Z", ZoneId.of("Z"), true),
                "Should fail with Z but no time");

        // Test with T but no time - should fail
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05T[Asia/Tokyo]", ZoneId.of("Z"), false),
                "Should fail with T but no time");

        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05T[Asia/Tokyo]", ZoneId.of("Z"), true),
                "Should fail with T but no time");
    }

    @Test
    void testParseWithTrailingText() {
        // Test with trailing text - should pass with strict=false
        ZonedDateTime zdt = DateUtilities.parseDate("2024-02-05 is a great day", ZoneId.of("Z"), false);
        assertEquals(2024, zdt.getYear());
        assertEquals(2, zdt.getMonthValue());
        assertEquals(5, zdt.getDayOfMonth());
        assertEquals(ZoneId.of("Z"), zdt.getZone());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());

        // Test with trailing text - should fail with strict=true
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05 is a great day", ZoneId.of("Z"), true),
                "Should fail with trailing text in strict mode");

        // Test with trailing text after full datetime - should pass with strict=false
        zdt = DateUtilities.parseDate("2024-02-05T10:30:45Z and then some text", ZoneId.of("Z"), false);
        assertEquals(2024, zdt.getYear());
        assertEquals(2, zdt.getMonthValue());
        assertEquals(5, zdt.getDayOfMonth());
        assertEquals(10, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        assertEquals(ZoneId.of("Z"), zdt.getZone());

        // Test with trailing text after full datetime - should fail with strict=true
        assertThrows(IllegalArgumentException.class, () ->
                        DateUtilities.parseDate("2024-02-05T10:30:45Z and then some text", ZoneId.of("Z"), true),
                "Should fail with trailing text in strict mode");
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
        ZonedDateTime zonedDateTime = DateUtilities.parseDate("Jan 22nd, 2024 21:52:05.123456789-05:00", ZoneId.systemDefault(), true);
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

    /**
     * Basic ISO 8601 date-times (strictly valid), with or without time,
     * fractional seconds, and 'T' separators.
     */
    @Test
    void testBasicIso8601() {
        // 1) Simple date + time with 'T'
        ZonedDateTime zdt1 = DateUtilities.parseDate("2025-02-15T10:30:00", ZoneId.of("UTC"), true);
        assertNotNull(zdt1);
        assertEquals(2025, zdt1.getYear());
        assertEquals(2, zdt1.getMonthValue());
        assertEquals(15, zdt1.getDayOfMonth());
        assertEquals(10, zdt1.getHour());
        assertEquals(30, zdt1.getMinute());
        assertEquals(0, zdt1.getSecond());
        assertEquals(ZoneId.of("UTC"), zdt1.getZone());

        // 2) Date + time with fractional seconds
        ZonedDateTime zdt2 = DateUtilities.parseDate("2025-02-15T10:30:45.123", ZoneId.of("UTC"), true);
        assertNotNull(zdt2);
        assertEquals(45, zdt2.getSecond());
        // We can't do an exact nanos compare easily, but let's do:
        assertEquals(123_000_000, zdt2.getNano());

        // 3) Using '/' separators
        ZonedDateTime zdt3 = DateUtilities.parseDate("2025/02/15 10:30:00", ZoneId.of("UTC"), true);
        assertNotNull(zdt3);
        assertEquals(10, zdt3.getHour());

        // 4) Only date (no time). Should default to 00:00:00 in UTC
        ZonedDateTime zdt4 = DateUtilities.parseDate("2025-02-15", ZoneId.of("UTC"), true);
        assertNotNull(zdt4);
        assertEquals(0, zdt4.getHour());
        assertEquals(0, zdt4.getMinute());
        assertEquals(0, zdt4.getSecond());
        assertEquals(ZoneId.of("UTC"), zdt4.getZone());
    }

    /**
     * Test Java's ZonedDateTime.toString() style, e.g. "YYYY-MM-DDTHH:mm:ss-05:00[America/New_York]".
     */
    @Test
    void testZonedDateTimeToString() {
        // Example from Java's ZonedDateTime
        // Typically: "2025-05-10T13:15:30-04:00[America/New_York]"
        String javaString = "2025-05-10T13:15:30-04:00[America/New_York]";
        ZonedDateTime zdt = DateUtilities.parseDate(javaString, ZoneId.systemDefault(), true);
        assertNotNull(zdt);
        assertEquals(2025, zdt.getYear());
        assertEquals(5, zdt.getMonthValue());
        assertEquals(10, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals("America/New_York", zdt.getZone().getId());
        // -04:00 offset is inside the bracketed zone.
        // The final zone is "America/New_York" with whatever offset it has on that date.
    }

    /**
     * Unix / Linux style strings, like: "Thu Jan 6 11:06:10 EST 2024".
     */
    @Test
    void testUnixStyle() {
        // 1) Basic Unix date
        ZonedDateTime zdt1 = DateUtilities.parseDate("Thu Jan 6 11:06:10 EST 2024", ZoneId.of("UTC"), true);
        assertNotNull(zdt1);
        assertEquals(2024, zdt1.getYear());
        assertEquals(1, zdt1.getMonthValue());    // January
        assertEquals(6, zdt1.getDayOfMonth());
        assertEquals(11, zdt1.getHour());
        assertEquals(6, zdt1.getMinute());
        assertEquals(10, zdt1.getSecond());
        // "EST" should become "America/New_York"
        assertEquals("America/New_York", zdt1.getZone().getId());

        // 2) Variation in day-of-week
        ZonedDateTime zdt2 = DateUtilities.parseDate("Friday Apr 1 07:10:00 CST 2022", ZoneId.of("UTC"), true);
        assertNotNull(zdt2);
        assertEquals(4, zdt2.getMonthValue());  // April
        assertEquals("America/Chicago", zdt2.getZone().getId());
    }

    /**
     * Test zone offsets in various legal formats, e.g. +HH, +HH:mm, -HHmm, etc.
     * Also test Z for UTC.
     */
    @Test
    void testZoneOffsets() {
        // 1) +HH:mm
        ZonedDateTime zdt1 = DateUtilities.parseDate("2025-06-15T08:30+02:00", ZoneId.of("UTC"), true);
        assertNotNull(zdt1);
        // The final zone is "GMT+02:00" internally
        assertEquals(8, zdt1.getHour());
        assertEquals(30, zdt1.getMinute());
        // Because we used +02:00, the local time is 08:30 in that offset
        assertEquals(ZoneOffset.ofHours(2), zdt1.getOffset());

        // 2) -HH
        ZonedDateTime zdt2 = DateUtilities.parseDate("2025-06-15 08:30-5", ZoneId.of("UTC"), true);
        assertNotNull(zdt2);
        assertEquals(ZoneOffset.ofHours(-5), zdt2.getOffset());

        // 3) +HHmm (4-digit)
        ZonedDateTime zdt3 = DateUtilities.parseDate("2025-06-15T08:30+0230", ZoneId.of("UTC"), true);
        assertNotNull(zdt3);
        assertEquals(ZoneOffset.ofHoursMinutes(2, 30), zdt3.getOffset());

        // 4) Z for UTC
        ZonedDateTime zdt4 = DateUtilities.parseDate("2025-06-15T08:30Z", ZoneId.systemDefault(), true);
        assertNotNull(zdt4);
        // Should parse as UTC
        assertEquals(ZoneOffset.UTC, zdt4.getOffset());
    }

    /**
     * Test old-fashioned full month name, day, year, with or without ordinal suffix
     * (like "January 21st, 2024").
     */
    @Test
    void testFullMonthName() {
        // 1) "January 21, 2024"
        ZonedDateTime zdt1 = DateUtilities.parseDate("January 21, 2024", ZoneId.of("UTC"), true);
        assertNotNull(zdt1);
        assertEquals(2024, zdt1.getYear());
        assertEquals(1, zdt1.getMonthValue());
        assertEquals(21, zdt1.getDayOfMonth());

        // 2) With an ordinal suffix
        ZonedDateTime zdt2 = DateUtilities.parseDate("January 21st, 2024", ZoneId.of("UTC"), true);
        assertNotNull(zdt2);
        assertEquals(21, zdt2.getDayOfMonth());

        // 3) Mixed upper/lower on suffix
        ZonedDateTime zdt3 = DateUtilities.parseDate("January 21ST, 2024", ZoneId.of("UTC"), true);
        assertNotNull(zdt3);
        assertEquals(21, zdt3.getDayOfMonth());
    }

    /**
     * Test random but valid combos: day-of-week + alpha month + leftover spacing,
     * with time possibly preceding the date, or date first, etc.
     */
    @Test
    void testMiscFlexibleCombos() {
        // 1) Day-of-week up front, alpha month, year
        ZonedDateTime zdt1 = DateUtilities.parseDate("thu, Dec 25, 2014", ZoneId.systemDefault(), true);
        assertNotNull(zdt1);
        assertEquals(2014, zdt1.getYear());
        assertEquals(12, zdt1.getMonthValue());
        assertEquals(25, zdt1.getDayOfMonth());

        // 2) Time first, then date
        ZonedDateTime zdt2 = DateUtilities.parseDate("07:45:33 2024-11-23", ZoneId.of("UTC"), true);
        assertNotNull(zdt2);
        assertEquals(2024, zdt2.getYear());
        assertEquals(11, zdt2.getMonthValue());
        assertEquals(23, zdt2.getDayOfMonth());
        assertEquals(7, zdt2.getHour());
        assertEquals(45, zdt2.getMinute());
        assertEquals(33, zdt2.getSecond());
    }

    /**
     * Test Unix epoch-millis (all digits).
     */
    @Test
    void testEpochMillis() {
        // Let's pick an arbitrary timestamp: 1700000000000 =>
        // Wed Nov 15 2023 06:13:20 UTC (for example)
        long epochMillis = 1700000000000L;
        ZonedDateTime zdt = DateUtilities.parseDate(String.valueOf(epochMillis), ZoneId.of("UTC"), true);
        assertNotNull(zdt);
        // Re-verify the instant
        Instant inst = Instant.ofEpochMilli(epochMillis);
        assertEquals(inst, zdt.toInstant());
    }

    /**
     * Confirm that a parseDate(String) -> Date (old Java date) also works
     * for some old-style or common formats.
     */
    @Test
    void testLegacyDateApi() {
        // parseDate(String) returns a Date (overloaded method).
        // e.g. "Mar 15 1997 13:55:44 PDT"
        Date d1 = DateUtilities.parseDate("Mar 15 13:55:44 PDT 1997");
        assertNotNull(d1);

        // Check the time
        ZonedDateTime zdt1 = d1.toInstant().atZone(ZoneId.of("UTC"));
        // 1997-03-15T20:55:44Z = 13:55:44 PDT is UTC-7
        assertEquals(1997, zdt1.getYear());
        assertEquals(3, zdt1.getMonthValue());
        assertEquals(15, zdt1.getDayOfMonth());
    }

    @Test
    void testTokyoOffset() {
        // Input string has explicit Asia/Tokyo zone
        String input = "2024-02-05T22:31:17.409[Asia/Tokyo]";

        // When parseDate sees an explicit zone, it should keep it,
        // ignoring the "default" zone (ZoneId.of("UTC")) because the string
        // already contains a zone or offset.
        ZonedDateTime zdt = DateUtilities.parseDate(input, ZoneId.of("UTC"), true);

        // Also convert the same string to a Calendar
        Calendar cal = Converter.convert(input, Calendar.class);

        // Check that the utility did NOT "force" UTC,
        // because the string has an explicit zone: Asia/Tokyo
        assertThat(zdt).isNotNull();
        assertThat(zdt.getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        // The local date-time portion should remain 2024-02-05T22:31:17.409
        assertThat(zdt.getHour()).isEqualTo(22);
        assertThat(zdt.getMinute()).isEqualTo(31);
        assertThat(zdt.getSecond()).isEqualTo(17);
        // And the offset from UTC should be +09:00
        assertThat(zdt.getOffset()).isEqualTo(ZoneOffset.ofHours(9));

        // The actual instant in UTC is 9 hours earlier: 2024-02-05T13:31:17.409Z
        Instant expectedInstant = Instant.parse("2024-02-05T13:31:17.409Z");
        assertThat(zdt.toInstant()).isEqualTo(expectedInstant);

        // Now check the Calendar result
        assertThat(cal).isNotNull();
        // The Calendar might have a different TimeZone internally,
        // but it should still represent the same Instant.
        Instant calInstant = cal.toInstant();
        assertThat(calInstant).isEqualTo(expectedInstant);

        // Round-trip check: convert the Calendar back to String, parse again,
        // and verify we land on the same Instant.
        String roundTripped = Converter.convert(cal, String.class);
        ZonedDateTime roundTrippedZdt = DateUtilities.parseDate(roundTripped, ZoneId.of("UTC"), true);
        assertThat(roundTrippedZdt.toInstant()).isEqualTo(expectedInstant);
    }

    @Test
    void testReDoSProtection_timePattern() {
        // Test that ReDoS vulnerability fix prevents catastrophic backtracking
        // Previous pattern with nested quantifiers could cause exponential time complexity
        
        // Test normal cases still work (date + time format)
        ZonedDateTime normal = DateUtilities.parseDate("2024-01-01 12:34:56.123", ZoneId.of("UTC"), true);
        assertNotNull(normal);
        assertEquals(12, normal.getHour());
        assertEquals(34, normal.getMinute());
        assertEquals(56, normal.getSecond());
        
        // Test potentially malicious inputs complete quickly (should not hang)
        long startTime = System.currentTimeMillis();
        
        // Test case 1: Multiple digits in nano could cause backtracking (with date)
        StringBuilder sb1 = new StringBuilder("2024-01-01 12:34:56.");
        for (int i = 0; i < 100; i++) sb1.append('1');
        try {
            DateUtilities.parseDate(sb1.toString(), ZoneId.of("UTC"), true);
        } catch (Exception e) {
            // Expected to fail parsing, but should fail quickly
        }
        
        // Test case 2: Long timezone names that could cause backtracking (with date)
        StringBuilder sb2 = new StringBuilder("2024-01-01 12:34:56 ");
        for (int i = 0; i < 200; i++) sb2.append('A');
        try {
            DateUtilities.parseDate(sb2.toString(), ZoneId.of("UTC"), true);
        } catch (Exception e) {
            // Expected to fail parsing, but should fail quickly
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete within reasonable time (not exponential backtracking)
        assertTrue(duration < 1000, "ReDoS protection failed - parsing took too long: " + duration + "ms");
    }

    @Test
    void testReDoSProtection_timezonePatternLimits() {
        // Test that timezone pattern limits prevent excessive repetition
        
        // Valid timezone should work
        ZonedDateTime valid = DateUtilities.parseDate("2024-01-01 12:34:56 EST", ZoneId.of("America/New_York"), true);
        assertNotNull(valid);
        
        // Extremely long timezone should be rejected or handled safely
        StringBuilder longTimezone = new StringBuilder("2024-01-01 12:34:56 ");
        for (int i = 0; i < 100; i++) longTimezone.append('A'); // Exceeds 50 char limit
        long startTime = System.currentTimeMillis();
        
        try {
            DateUtilities.parseDate(longTimezone.toString(), ZoneId.of("UTC"), true);
        } catch (Exception e) {
            // Expected to fail, but should fail quickly
        }
        
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 500, "Timezone pattern processing took too long: " + duration + "ms");
    }

    @Test
    void testReDoSProtection_nanoSecondsLimit() {
        // Test that nanoseconds pattern limits precision appropriately
        
        // Valid nanoseconds (1-9 digits) should work
        ZonedDateTime valid = DateUtilities.parseDate("2024-01-01 12:34:56.123456789", ZoneId.of("UTC"), true);
        assertNotNull(valid);
        assertEquals(123456789, valid.getNano());
        
        // Test exactly 9 digits (maximum)
        ZonedDateTime max = DateUtilities.parseDate("2024-01-01 12:34:56.999999999", ZoneId.of("UTC"), true);
        assertNotNull(max);
        assertEquals(999999999, max.getNano());
        
        // More than 9 digits should either be truncated or cause quick failure
        long startTime = System.currentTimeMillis();
        StringBuilder longNanos = new StringBuilder("2024-01-01 12:34:56.");
        for (int i = 0; i < 50; i++) longNanos.append('1');
        try {
            DateUtilities.parseDate(longNanos.toString(), ZoneId.of("UTC"), true);
        } catch (Exception e) {
            // Expected to fail or truncate, but should be quick
        }
        
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 500, "Nanoseconds pattern processing took too long: " + duration + "ms");
    }

    @Test
    void testTimezoneMapThreadSafety() {
        // Test that ABBREVIATION_TO_TIMEZONE map is immutable
        assertThatThrownBy(() -> DateUtilities.ABBREVIATION_TO_TIMEZONE.put("TEST", "Test/Zone"))
            .isInstanceOf(UnsupportedOperationException.class);
        
        // Test that map contains expected timezone mappings
        assertEquals("America/New_York", DateUtilities.ABBREVIATION_TO_TIMEZONE.get("EST"));
        assertEquals("America/Chicago", DateUtilities.ABBREVIATION_TO_TIMEZONE.get("CST"));
        assertEquals("America/Denver", DateUtilities.ABBREVIATION_TO_TIMEZONE.get("MST"));
        assertEquals("America/Los_Angeles", DateUtilities.ABBREVIATION_TO_TIMEZONE.get("PST"));
        
        // Test concurrent access safety - no exceptions should occur
        assertDoesNotThrow(() -> {
            Runnable task = () -> {
                for (int i = 0; i < 1000; i++) {
                    String timezone = DateUtilities.ABBREVIATION_TO_TIMEZONE.get("EST");
                    assertEquals("America/New_York", timezone);
                }
            };
            
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(task);
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
        });
    }

    @Test
    void testInputValidation_MaxLength() {
        // Test date string length validation (max 256 characters)
        StringBuilder longString = new StringBuilder("2024-01-01");
        for (int i = 0; i < 250; i++) {
            longString.append("X"); // Use non-whitespace characters to avoid trimming
        }
        // This should be > 256 characters total (10 + 250 = 260)
        
        assertThatThrownBy(() -> DateUtilities.parseDate(longString.toString(), ZoneId.of("UTC"), true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Date string too long");
    }

    @Test
    void testInputValidation_EpochMilliseconds() {
        // Test epoch milliseconds bounds (max 19 digits)
        String tooLong = "12345678901234567890"; // 20 digits
        assertThatThrownBy(() -> DateUtilities.parseDate(tooLong, ZoneId.of("UTC"), true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Epoch milliseconds value too large");
        
        // Test valid epoch milliseconds still works
        String valid = "1640995200000"; // 13 digits - valid
        ZonedDateTime result = DateUtilities.parseDate(valid, ZoneId.of("UTC"), true);
        assertNotNull(result);
        assertEquals(2022, result.getYear());
    }

    @Test
    void testInputValidation_YearBounds() {
        // Test boundary values are accepted (the validation is primarily for extreme edge cases)
        ZonedDateTime result1 = DateUtilities.parseDate("999999999-01-01", ZoneId.of("UTC"), true);
        assertNotNull(result1);
        assertEquals(999999999, result1.getYear());
        
        ZonedDateTime result2 = DateUtilities.parseDate("-999999999-01-01", ZoneId.of("UTC"), true);
        assertNotNull(result2);
        assertEquals(-999999999, result2.getYear());
        
        // Test that reasonable years work normally
        ZonedDateTime result3 = DateUtilities.parseDate("2024-01-01", ZoneId.of("UTC"), true);
        assertNotNull(result3);
        assertEquals(2024, result3.getYear());
    }

    @Test
    void testRegexPerformance_SampleBenchmark() {
        // Basic performance test to verify regex optimizations don't hurt performance
        // Tests common date parsing patterns for performance regression detection
        String[] testDates = {
            "2024-01-15 14:30:00",
            "2024/01/15 14:30:00.123+05:00",
            "January 15th, 2024 2:30 PM EST",
            "15th Jan 2024 14:30:00.123456",
            "Mon Jan 15 14:30:00 EST 2024",
            "1705339800000" // epoch milliseconds
        };
        
        ZoneId utc = ZoneId.of("UTC");
        long startTime = System.nanoTime();
        
        // Parse each test date multiple times to measure performance
        for (int i = 0; i < 100; i++) {
            for (String testDate : testDates) {
                try {
                    ZonedDateTime result = DateUtilities.parseDate(testDate, utc, false);
                    assertNotNull(result, "Failed to parse: " + testDate);
                } catch (Exception e) {
                    // Some test dates may not parse perfectly - that's ok for performance test
                }
            }
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Performance should complete within reasonable time (regression detection)
        // This is not a strict benchmark, just ensuring no major performance degradation
        assertTrue(durationMs < 5000, "Date parsing took too long: " + durationMs + "ms - possible performance regression");
    }
}
