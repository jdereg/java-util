package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class TestSimpleDateFormat
{
    @Test
    public void testSimpleDateFormat1() throws Exception
    {
        SafeSimpleDateFormat x = new SafeSimpleDateFormat("yyyy-MM-dd");
        String s = x.format(getDate(2013, 9, 7, 16, 15, 31));
        assertEquals("2013-09-07", s);

        Date then = x.parse(s);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(then);
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(8, cal.get(Calendar.MONTH));   // Sept
        assertEquals(7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    public void testSetLenient() throws Exception
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
    public void testSetCalendar() throws Exception
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
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
        assertEquals(31, cal.get(Calendar.SECOND));

        try
        {
            then = x.parse("March 33, 2013");
            fail("should not make it here");
        }
        catch (ParseException ignored) { }
    }

    @Test
    public void testSetDateSymbols() throws Exception {
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
    public void testTimeZone() throws Exception
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

    @Test
    public void testConcurrencyWillFail() throws Exception
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

        assertNotNull(passed[0]);
//        System.out.println("r = " + r[0]);
//        System.out.println("s = " + s[0]);
//        System.out.println("t = " + t[0]);
    }

    @Test
    public void testConcurrencyWontFail() throws Exception
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
//        System.out.println("r = " + r[0]);
//        System.out.println("s = " + s[0]);
//        System.out.println("t = " + t[0]);
    }

    @Test
    public void testParseObject() {
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
    public void test2DigitYear() throws Exception {
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
    public void testSetSymbols() throws Exception {
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

    private Date getDate(int year, int month, int day, int hour, int min, int sec)
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day, hour, min, sec);
        return cal.getTime();
    }
}
