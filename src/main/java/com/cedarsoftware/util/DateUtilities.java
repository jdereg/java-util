package com.cedarsoftware.util;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handy utilities for working with Java Dates.
 *
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
public class DateUtilities
{
    private static String mos = "(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|Sept|September|Oct|October|Nov|November|Dec|December)";
    private static final Pattern datePattern1 = Pattern.compile("(\\d{4})[\\./-](\\d{1,2})[\\./-](\\d{1,2})");
    private static final Pattern datePattern2 = Pattern.compile("(\\d{1,2})[\\./-](\\d{1,2})[\\./-](\\d{4})");
    private static final Pattern datePattern3 = Pattern.compile(mos + "[ ,]+(\\d{1,2})[ ,]+(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern datePattern4 = Pattern.compile("(\\d{1,2})[ ,]" + mos + "[ ,]+(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern datePattern5 = Pattern.compile("(\\d{4})[ ,]" + mos + "[ ,]+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern timePattern1 = Pattern.compile("(\\d{2})[:\\.](\\d{2})[:\\.](\\d{2})[\\.](\\d{1,3})");
    private static final Pattern timePattern2 = Pattern.compile("(\\d{2})[:\\.](\\d{2})[:\\.](\\d{2})");
    private static final Pattern timePattern3 = Pattern.compile("(\\d{2})[:\\.](\\d{2})");
    private static final Map<String, String> months = new LinkedHashMap<String, String>();

    static
    {
        // Month name to number map
        months.put("jan", "1");
        months.put("january", "1");
        months.put("feb", "2");
        months.put("february", "2");
        months.put("mar", "3");
        months.put("march", "3");
        months.put("apr", "4");
        months.put("april", "4");
        months.put("may", "5");
        months.put("jun", "6");
        months.put("june", "6");
        months.put("jul", "7");
        months.put("july", "7");
        months.put("aug", "8");
        months.put("august", "8");
        months.put("sep", "9");
        months.put("sept", "9");
        months.put("september", "9");
        months.put("oct", "10");
        months.put("october", "10");
        months.put("nov", "11");
        months.put("november", "11");
        months.put("dec", "12");
        months.put("december", "12");
    }

    public static Date parseDate(String dateStr)
    {
        dateStr = dateStr.trim();

        // Determine which date pattern (Matcher) to use
        Matcher matcher = datePattern1.matcher(dateStr);

        String year, month = null, day, mon = null;

        if (matcher.find())
        {
            year = matcher.group(1);
            month = matcher.group(2);
            day = matcher.group(3);
        }
        else
        {
            matcher = datePattern2.matcher(dateStr);
            if (matcher.find())
            {
                month = matcher.group(1);
                day = matcher.group(2);
                year = matcher.group(3);
            }
            else
            {
                matcher = datePattern3.matcher(dateStr);
                if (matcher.find())
                {
                    mon = matcher.group(1);
                    day = matcher.group(2);
                    year = matcher.group(3);
                }
                else
                {
                    matcher = datePattern4.matcher(dateStr);
                    if (matcher.find())
                    {
                        day = matcher.group(1);
                        mon = matcher.group(2);
                        year = matcher.group(3);
                    }
                    else
                    {
                        matcher = datePattern5.matcher(dateStr);
                        if (!matcher.find())
                        {
                            error("Unable to parse: " + dateStr);
                        }
                        year = matcher.group(1);
                        mon = matcher.group(2);
                        day = matcher.group(3);
                    }
                }
            }
        }

        if (mon != null)
        {   // Month will always be in Map, because regex forces this.
            month = months.get(mon.trim().toLowerCase());
        }

        // Determine which date pattern (Matcher) to use
        matcher = timePattern1.matcher(dateStr);
        if (!matcher.find())
        {
            matcher = timePattern2.matcher(dateStr);
            if (!matcher.find())
            {
                matcher = timePattern3.matcher(dateStr);
                if (!matcher.find())
                {
                    matcher = null;
                }
            }
        }

        Calendar c = Calendar.getInstance();
        c.clear();

        // Regex prevents these from ever failing to parse
        int y = Integer.parseInt(year);
        int m = Integer.parseInt(month) - 1;    // months are 0-based
        int d = Integer.parseInt(day);

        if (m < 0 || m > 11)
        {
            error("Month must be between 1 and 12, date: " + dateStr);
        }
        if (d < 0 || d > 31)
        {
            error("Day cannot be > 31, date: " + dateStr);
        }

        if (matcher == null)
        {   // no [valid] time portion
            c.set(y, m, d);
        }
        else
        {
            String hour = matcher.group(1);
            String min = matcher.group(2);
            String sec = "00";
            String milli = "000";
            if (matcher.groupCount() > 2)
            {
                sec = matcher.group(3);
            }
            if (matcher.groupCount() > 3)
            {
                milli = matcher.group(4);
            }

            // Regex prevents these from ever failing to parse.
            int h = Integer.parseInt(hour);
            int mn = Integer.parseInt(min);
            int s = Integer.parseInt(sec);
            int ms = Integer.parseInt(milli);

            if (h < 0 || h > 23)
            {
                error("Hour must be between 0 and 23, time: " + dateStr);
            }
            if (mn < 0 || mn > 59)
            {
                error("Minute must be between 0 and 59, time: " + dateStr);
            }
            if (s < 0 || s > 59)
            {
                error("Second must be between 0 and 59, time: " + dateStr);
            }

            // regex enforces millis to 000 to 999 or none
            c.set(y, m, d, h, mn, s);
            c.set(Calendar.MILLISECOND, ms);
        }
        return c.getTime();
    }

    private static void error(String msg)
    {
        throw new IllegalArgumentException(msg);
    }
}