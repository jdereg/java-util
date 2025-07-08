package com.cedarsoftware.util;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
public class SimpleDateFormatTest  {
    private static final Logger LOG = Logger.getLogger(SimpleDateFormatTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @ParameterizedTest
    @MethodSource("testDates")
    void testSimpleDateFormat1(int year, int month, int day, int hour, int min, int sec, String expectedDateFormat) throws Exception
    {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd");
        String s = x.format(getDate(year, month, day, hour, min, sec));
        assertEquals(expectedDateFormat, s);

        Date then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(year, cal.get(Calendar.YEAR));
        assertEquals(month - 1, cal.get(Calendar.MONTH));   // Sept
        assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    private static Stream<Arguments> testDates() {
        return Stream.of(
                Arguments.of(2013, 9, 7, 16, 15, 31, "2013-09-07"),
                Arguments.of(169, 5, 1, 11, 45, 15, "0169-05-01"),
                Arguments.of(42, 1, 28, 7, 4, 23, "0042-01-28"),
                Arguments.of(8, 11, 2, 12, 43, 56, "0008-11-02")
        );
    }

    @Test
    void testSetLenient() throws Exception
    {
        //February 942, 1996
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("MMM dd, yyyy");
        Date then = x.parse("March 33, 2013");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(3, cal.get(Calendar.MONTH));  // April
        assertEquals(2, cal.get(Calendar.DAY_OF_MONTH));   // 2nd
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));

        x.setLenient(false);
        try
        {
            then = x.parse("March 33, 2013");
            fail("should not make it here");
        }
        catch (ParseException ignore)
        { }
    }

    @Test
    void testSetCalendar() throws Exception
    {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        x.setCalendar(Calendar.getInstance());

        String s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        assertEquals("2013-09-07 04:15:31", s);

        Date then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
        assertEquals(31, cal.get(Calendar.SECOND));

        SafeSimpleDateFormat x2 = new SafeSimpleDateFormat("MMM dd, yyyy");
        then = x2.parse("March 31, 2013");

        cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(2, cal.get(Calendar.MONTH));   // March
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));

        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("PST"));
        cal.setLenient(false);
        x.setCalendar(cal);
        x2.setCalendar(cal);

        then = x.parse(s);

        cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
//        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY));       // Depends on what TimeZone test is run within
//        assertEquals(15, cal.get(Calendar.MINUTE));
//        assertEquals(31, cal.get(Calendar.SECOND));

        try
        {
            then = x.parse("March 33, 2013");
            fail("should not make it here");
        }
        catch (ParseException ignored) { }
    }

    @Test
    void testSetDateSymbols() throws Exception {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        x.setCalendar(Calendar.getInstance());

        String s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        assertEquals("2013-09-07 04:15:31", s);

        Date then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
        assertEquals(31, cal.get(Calendar.SECOND));

        x = new SafeSimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        x.setNumberFormat(new NumberFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo;
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo;
            }

            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                return 0;
            }
        });
        s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        assertEquals("2013-09-07 04:15:31", s);

        //NumberFormat.getPercentInstance();
    }

    @Test
    void testTimeZone() throws Exception
    {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        assertEquals("2013-09-07 04:15:31", s);

        Date then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
        assertEquals(31, cal.get(Calendar.SECOND));

        TimeZone localTz = TimeZone.getDefault();

        TimeZone tzLA = TimeZone.getTimeZone("America/Los_Angeles");

		long txDiff = localTz.getRawOffset() + localTz.getDSTSavings()
				- tzLA.getRawOffset() - tzLA.getDSTSavings();

        x.setTimeZone(tzLA);

        Calendar expectedDate = Calendar.getInstance();
        expectedDate.setTimeInMillis(then.getTime() + txDiff);

        then = x.parse(s);
        cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(expectedDate.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(expectedDate.get(Calendar.MONTH), cal.get(Calendar.MONTH));   // Sept
        assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(expectedDate.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(expectedDate.get(Calendar.MINUTE), cal.get(Calendar.MINUTE));
        assertEquals(expectedDate.get(Calendar.SECOND), cal.get(Calendar.SECOND));
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testConcurrencyWillFail() throws Exception
    {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Random random = new Random();
        Thread[] threads = new Thread[16];
        final long[] iter = new long[16];

        final Date date1 = getDate(1965, 12, 17, 17, 40, 05);
        final Date date2 = getDate(1996, 12, 24, 16, 18, 43);
        final Date date3 = getDate(1998, 9, 30, 0, 25, 17);

        final int[] r = new int[1];
        final int[] s = new int[1];
        final int[] t = new int[1];

        final String[] passed = new String[1];
        passed[0] = null;

        for (int i=0; i < 16; i++)
        {
            final int index = i;
            threads[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        long start = System.currentTimeMillis();

                        while (System.currentTimeMillis() - start < 1000)
                        {
                            for (int j=0; j < 100; j++)
                            {
                                int op = random.nextInt(100);
                                if (op < 5)
                                {	// 5% calls to format date1
                                    String a = format.format(date1);
                                    Date x = format.parse(a);
                                    r[0]++;
                                }
                                else if (op < 20)
                                {	// 15% puts
                                    String b = format.format(date2);
                                    Date y = format.parse(b);
                                    s[0]++;
                                }
                                else
                                {	// 80% gets
                                    String c = format.format(date3);
                                    Date z = format.parse(c);
                                    t[0]++;
                                }
                            }
                            iter[index]++;
                        }
                    }
                    catch (Exception e)
                    {
                        passed[0] = e.getMessage();
                    }
                }
            });
            threads[i].setName("SafeSimpleDateFormat_T" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        for (int i=0; i < 16; i++)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        assertNotNull(passed[0]);
        LOG.info("r = " + r[0]);
        LOG.info("s = " + s[0]);
        LOG.info("t = " + t[0]);
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testConcurrencyWontFail() throws Exception
    {
        final SafeSimpleDateFormat format = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Random random = new Random();
        Thread[] threads = new Thread[16];
        final long[] iter = new long[16];

        final Date date1 = getDate(1965, 12, 17, 17, 40, 05);
        final Date date2 = getDate(1996, 12, 24, 16, 18, 43);
        final Date date3 = getDate(1998, 9, 30, 0, 25, 17);

        final int[] r = new int[1];
        final int[] s = new int[1];
        final int[] t = new int[1];

        final String[] passed = new String[1];
        passed[0] = null;

        for (int i=0; i < 16; i++)
        {
            final int index = i;
            threads[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        long start = System.currentTimeMillis();

                        while (System.currentTimeMillis() - start < 2000)
                        {
                            for (int j=0; j < 100; j++)
                            {
                                int op = random.nextInt(100);
                                if (op < 5)
                                {	// 5% calls to format date1
                                    String a = format.format(date1);
                                    Date x = format.parse(a);
                                    r[0]++;
                                }
                                else if (op < 20)
                                {	// 15% puts
                                    String b = format.format(date2);
                                    Date y = format.parse(b);
                                    s[0]++;
                                }
                                else
                                {	// 80% gets
                                    String c = format.format(date3);
                                    Date z = format.parse(c);
                                    t[0]++;
                                }
                            }
                            iter[index]++;
                        }
                    }
                    catch (Exception e)
                    {
                        passed[0] = e.getMessage();
                    }
                }
            });
            threads[i].setName("SafeSimpleDateFormat_T" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        for (int i=0; i < 16; i++)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        assertNull(passed[0]);
//        LOG.info("r = " + r[0]);
//        LOG.info("s = " + s[0]);
//        LOG.info("t = " + t[0]);
    }

    @Test
    void testParseObject() {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd");
        String s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        String d = "date: " + s;
        assertEquals("2013-09-07", s);

        Object then = (Date)x.parseObject(d, new ParsePosition(5));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime((Date)then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));

    }

    @Test
    void test2DigitYear() throws Exception {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yy-MM-dd");
        String s = x.format(getDate(13, 9, 7, 16, 15, 31));
        assertEquals("13-09-07", s);

        Object then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime((Date)then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));

        cal.add(Calendar.YEAR, 87);
        x.set2DigitYearStart(cal.getTime());

        Object fut = (Date)x.parse(s);

        cal.clear();
        cal.setTime((Date)fut);
        assertEquals(2113, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void testSetSymbols() throws Exception {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yy.MM.dd hh:mm aaa");
        String s = x.format(getDate(13, 9, 7, 16, 15, 31));
        assertEquals("13.09.07 04:15 PM", s);

        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd");
        DateFormatSymbols symbols = format.getDateFormatSymbols();

        String[] ampm = new String[] { "foo", "bar"};
        symbols.setAmPmStrings(ampm);
        x.setDateFormatSymbols(symbols);

        s = x.format(getDate(13, 9, 7, 16, 15, 31));
        assertEquals("13.09.07 04:15 bar", s);
    }

    @Test
    void testToString()
    {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy/MM/dd");
        assertEquals(safe.toString(), "yyyy/MM/dd");
    }

    private Date getDate(int year, int month, int day, int hour, int min, int sec)
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day, hour, min, sec);
        return cal.getTime();
    }
}
