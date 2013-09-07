package com.cedarsoftware.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author John DeRegnaucourt
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
        assertTrue(cal.get(Calendar.YEAR) == 2013);
        assertTrue(cal.get(Calendar.MONTH) == 8);   // Sept
        assertTrue(cal.get(Calendar.DAY_OF_MONTH) == 7);
    }

    @Test
    public void testConcurrencyWillFail() throws Exception
    {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Random random = new Random();
        Thread[] threads = new Thread[16];
        final long[]iter = new long[16];

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

        assertFalse(passed[0], passed[0] == null);
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
        final long[]iter = new long[16];

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

        assertTrue(passed[0], passed[0] == null);
//        System.out.println("r = " + r[0]);
//        System.out.println("s = " + s[0]);
//        System.out.println("t = " + t[0]);
    }


    private Date getDate(int year, int month, int day, int hour, int min, int sec)
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day, hour, min, sec);
        return cal.getTime();
    }
}
