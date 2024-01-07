package com.cedarsoftware.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing String dates with optional times, especially when the input String formats
 * may be inconsistent.  This will parse the following formats (constrained only by java.util.Date limitations...best
 * time resolution is milliseconds):<br/>
 * <pre>
 * 12-31-2023  -or-  12/31/2023     mm is 1-12 or 01-12, dd is 1-31 or 01-31, and yyyy can be 0000 to 9999.
 *                                  
 * 2023-12-31  -or-  2023/12/31     mm is 1-12 or 01-12, dd is 1-31 or 01-31, and yyyy can be 0000 to 9999.
 *                                  
 * January 6th, 2024                Month (3-4 digit abbreviation or full English name), white-space and optional comma,
 *                                  day of month (1-31 or 0-31) with optional suffixes 1st, 3rd, 22nd, whitespace and
 *                                  optional comma, and yyyy (0000-9999)
 *
 * 17th January 2024                day of month (1-31 or 0-31) with optional suffixes (e.g. 1st, 3rd, 22nd),
 *                                  Month (3-4 digit abbreviation or full English name), whites space and optional comma,
 *                                  and yyyy (0000-9999)
 *
 * 2024 January 31st                4 digit year, white space and optional comma, Month (3-4 digit abbreviation or full
 *                                  English name), white space and optional command, and day of month with optional
 *                                  suffixes (1st, 3rd, 22nd)
 *
 * Sat Jan 6 11:06:10 EST 2024      Unix/Linux style.  Day of week (3-letter or full name), Month (3-4 digit or full
 *                                  English name), time hh:mm:ss, TimeZone (Java supported Timezone names), Year
 * </pre>
 *  All dates can be followed by a Time, or the time can precede the Date. Whitespace or a single letter T must separate the
 *  date and the time for the non-Unix time formats.  The Time formats supported:<br/>
 * <pre>
 * hh:mm                            hours (00-23), minutes (00-59).  24 hour format.
 * 
 * hh:mm:ss                         hours (00-23), minutes (00-59), seconds (00-59).  24 hour format.
 *
 * hh:mm:ss.sssss                   hh:mm:ss and fractional seconds. Variable fractional seconds supported. Date only
 *                                  supports up to millisecond precision, so anything after 3 decimal places is
 *                                  effectively ignored.
 *
 * hh:mm:offset -or-                offset can be specified as +HH:mm, +HHmm, +HH, -HH:mm, -HHmm, -HH, or Z (GMT)
 * hh:mm:ss.sss:offset              which will match: "12:34", "12:34:56", "12:34.789", "12:34:56.789", "12:34+01:00",
 *                                  "12:34:56+1:00", "12:34-01", "12:34:56-1", "12:34Z", "12:34:56Z"
 *
 * hh:mm:zone -or-                  Zone can be specified as Z (Zulu = UTC), older short forms: GMT, EST, CST, MST,
 * hh:mm:ss.sss:zone                PST, IST, JST, BST etc. as well as the long forms: "America/New York", "Asia/Saigon",
 *                                  etc. See ZoneId.getAvailableZoneIds().
 * </pre>
 * DateUtilities will parse Epoch-based integer-based values. It supports the following 3 types:
 * <pre>
 * "0" through "999999"              A string of digits in this range will be parsed and returned as the number of days
 *                                   since the Unix Epoch, January 1st, 1970 00:00:00 UTC.
 *
 * "1000000" through "999999999999"  A string of digits in this range will be parsed and returned as the number of seconds
 *                                   since the Unix Epoch, January 1st, 1970 00:00:00 UTC.
 *
 * "1000000000000" or larger         A string of digits in this range will be parsed and returned as the number of milli-
 *                                   seconds since the Unix Epoch, January 1st, 1970 00:00:00 UTC.
 * </pre>
 * On all patterns above, if a day-of-week (e.g. Thu, Sunday, etc.) is included (front, back, or between date and time),
 * it will be ignored, allowing for even more formats than what is listed here.  The day-of-week is not be used to
 * influence the Date calculation.
 *
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
public final class DateUtilities {
    private static final Pattern allDigits = Pattern.compile("^\\d+$");
    private static final String days = "(monday|mon|tuesday|tues|tue|wednesday|wed|thursday|thur|thu|friday|fri|saturday|sat|sunday|sun)"; // longer before shorter matters
    private static final String mos = "(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sept|Sep|October|Oct|November|Nov|December|Dec)";
    private static final String yr = "(\\d{4})";
    private static final String dig1or2 = "\\d{1,2}";
    private static final String dig1or2grp = "(" + dig1or2 + ")";
    private static final String ord = dig1or2grp + "(st|nd|rd|th)?";
    private static final String dig2 = "\\d{2}";
    private static final String dig2gr = "(" + dig2 + ")";
    private static final String sep = "([./-])";
    private static final String ws = "\\s+";
    private static final String wsOp = "\\s*";
    private static final String wsOrComma = "[ ,]+";
    private static final String tzUnix = "([A-Z]{1,3})?";
    private static final String opNano = "(\\.\\d+)?";
    private static final String dayOfMon = dig1or2grp;
    private static final String opSec = "(?:" + ":" + dig2gr + ")?";
    private static final String hh = dig2gr;
    private static final String mm = dig2gr;
    private static final String tz_Hh_MM = "[+-]\\d{1,2}:\\d{2}";
    private static final String tz_HHMM = "[+-]\\d{4}";
    private static final String tz_Hh = "[+-]\\d{1,2}";
    private static final String tzNamed = ws + "[A-Za-z][A-Za-z0-9~/._+-]+";

    // Patterns defined in BNF-style using above named elements
    private static final Pattern isoDatePattern = Pattern.compile(    // Regex's using | (OR)
            yr + sep + dig1or2grp + "\\2" + dig1or2grp + "|" +        // 2024/01/21 (yyyy/mm/dd -or- yyyy-mm-dd -or- yyyy.mm.dd)   [optional time, optional day of week]  \2 references 1st separator (ensures both same)
            dig1or2grp + sep + dig1or2grp + "\\6" + yr);              // 01/21/2024 (mm/dd/yyyy -or- mm-dd-yyyy -or- mm.dd.yyyy)   [optional time, optional day of week]  \6 references 1st separator (ensures both same)

    private static final Pattern alphaMonthPattern = Pattern.compile(
            mos + wsOrComma + ord + wsOrComma + yr + "|" +      // Jan 21st, 2024  (comma optional between all, day of week optional, time optional, ordinal text optional [st, nd, rd, th])
            ord + wsOrComma + mos + wsOrComma + yr + "|" +            // 21st Jan, 2024  (ditto)
            yr + wsOrComma + mos + wsOrComma + ord,                   // 2024 Jan 21st   (ditto)
            Pattern.CASE_INSENSITIVE);

    private static final Pattern unixDateTimePattern = Pattern.compile(
            days + ws + mos + ws + dayOfMon + ws + "(" + dig2 + ":" + dig2 + ":" + dig2 + ")" + wsOp + tzUnix + wsOp + yr,
            Pattern.CASE_INSENSITIVE);

    private static final Pattern timePattern = Pattern.compile(
            hh + ":" + mm + opSec + opNano + "(" + tz_Hh_MM + "|" + tz_HHMM + "|" + tz_Hh + "|Z|" + tzNamed +")?",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern dayPattern = Pattern.compile(days, Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> months = new ConcurrentHashMap<>();
    
    static {
        // Month name to number map
        months.put("jan", 1);
        months.put("january", 1);
        months.put("feb", 2);
        months.put("february", 2);
        months.put("mar", 3);
        months.put("march", 3);
        months.put("apr", 4);
        months.put("april", 4);
        months.put("may", 5);
        months.put("jun", 6);
        months.put("june", 6);
        months.put("jul", 7);
        months.put("july", 7);
        months.put("aug", 8);
        months.put("august", 8);
        months.put("sep", 9);
        months.put("sept", 9);
        months.put("september", 9);
        months.put("oct", 10);
        months.put("october", 10);
        months.put("nov", 11);
        months.put("november", 11);
        months.put("dec", 12);
        months.put("december", 12);
    }

    private DateUtilities() {
    }

    public static Date parseDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }

        dateStr = dateStr.trim();
        if (dateStr.isEmpty()) {
            return null;
        }

        if (allDigits.matcher(dateStr).matches()) {
            return parseEpochString(dateStr);
        }

        String year, day, remains, tz = null;
        int month;

        // Determine which date pattern to use
        Matcher matcher = isoDatePattern.matcher(dateStr);
        String remnant = matcher.replaceFirst("");
        if (remnant.length() < dateStr.length()) {
            if (matcher.group(1) != null) {
                year = matcher.group(1);
                month = Integer.parseInt(matcher.group(3));
                day = matcher.group(4);
            } else {
                year = matcher.group(8);
                month = Integer.parseInt(matcher.group(5));
                day = matcher.group(7);
            }
            remains = remnant;
        } else {
            matcher = alphaMonthPattern.matcher(dateStr);
            remnant = matcher.replaceFirst("");
            if (remnant.length() < dateStr.length()) {
                String mon;
                if (matcher.group(1) != null) {
                    mon = matcher.group(1);
                    day = matcher.group(2);
                    year = matcher.group(4);
                    remains = remnant;
                } else if (matcher.group(7) != null) {
                    mon = matcher.group(7);
                    day = matcher.group(5);
                    year = matcher.group(8);
                    remains = remnant;
                } else {
                    year = matcher.group(9);
                    mon = matcher.group(10);
                    day = matcher.group(11);
                    remains = remnant;
                }
                month = months.get(mon.trim().toLowerCase());
            } else {
                matcher = unixDateTimePattern.matcher(dateStr);
                if (matcher.replaceFirst("").length() == dateStr.length()) {
                    throw new IllegalArgumentException("Unable to parse: " + dateStr + " as a date");
                }
                year = matcher.group(6);
                String mon = matcher.group(2);
                month = months.get(mon.trim().toLowerCase());
                day = matcher.group(3);
                tz = matcher.group(5);
                remains = matcher.group(4);     // leave optional time portion remaining
            }
        }

        // For the remaining String, match the time portion (which could have appeared ahead of the date portion)
        String hour = null, min = null, sec = "00", milli = "0";
        remains = remains.trim();
        matcher = timePattern.matcher(remains);
        remnant = matcher.replaceFirst("");
        if (remnant.length() < remains.length()) {
            hour = matcher.group(1);
            min = matcher.group(2);
            if (matcher.group(3) != null) {
                sec = matcher.group(3);
            }
            if (matcher.group(4) != null) {
                milli = matcher.group(4).substring(1);
            }
            if (matcher.group(5) != null) {
                tz = matcher.group(5).trim();
            }
        } else {
            matcher = null;     // indicates no "time" portion
        }

        remains = remnant;

        // Clear out day of week (mon, tue, wed, ...)
        if (StringUtilities.length(remains) > 0) {
            Matcher dayMatcher = dayPattern.matcher(remains);
            remains = dayMatcher.replaceFirst("").trim();
        }

        // Verify that nothing or , or T is all that remains
        if (StringUtilities.length(remains) > 0) {
            remains = remains.trim();
            if (!remains.equals(",") && (!remains.equals("T"))) {
                throw new IllegalArgumentException("Issue parsing data/time, other characters present: " + remains);
            }
        }

        // Set Timezone into Calendar if one is supplied
        Calendar c = Calendar.getInstance();
        if (tz != null) {
            if (tz.startsWith("-") || tz.startsWith("+")) {
                ZoneOffset offset = ZoneOffset.of(tz);
                ZoneId zoneId = ZoneId.ofOffset("GMT", offset);
                TimeZone timeZone = TimeZone.getTimeZone(zoneId);
                c.setTimeZone(timeZone);
            } else {
                try {
                    ZoneId zoneId = ZoneId.of(tz);
                    TimeZone timeZone = TimeZone.getTimeZone(zoneId);
                    c.setTimeZone(timeZone);
                } catch (Exception e) {
                    TimeZone timeZone = TimeZone.getTimeZone(tz);
                    if (timeZone.getRawOffset() != 0) {
                        c.setTimeZone(timeZone);
                    } else {
                        throw e;
                    }
                }
            }
        }
        c.clear();

        // Build Calendar from date, time, and timezone components, and retrieve Date instance from Calendar.
        int y = Integer.parseInt(year);
        int m = month - 1;    // months are 0-based
        int d = Integer.parseInt(day);

        if (m < 0 || m > 11) {
            throw new IllegalArgumentException("Month must be between 1 and 12 inclusive, date: " + dateStr);
        }
        if (d < 1 || d > 31) {
            throw new IllegalArgumentException("Day must be between 1 and 31 inclusive, date: " + dateStr);
        }

        if (matcher == null) {   // no [valid] time portion
            c.set(y, m, d);
        } else {
            // Regex prevents these from ever failing to parse.
            int h = Integer.parseInt(hour);
            int mn = Integer.parseInt(min);
            int s = Integer.parseInt(sec);
            int ms = Integer.parseInt(prepareMillis(milli));   // Must be between 0 and 999.

            if (h > 23) {
                throw new IllegalArgumentException("Hour must be between 0 and 23 inclusive, time: " + dateStr);
            }
            if (mn > 59) {
                throw new IllegalArgumentException("Minute must be between 0 and 59 inclusive, time: " + dateStr);
            }
            if (s > 59) {
                throw new IllegalArgumentException("Second must be between 0 and 59 inclusive, time: " + dateStr);
            }

            // regex enforces millis to number
            c.set(y, m, d, h, mn, s);
            c.set(Calendar.MILLISECOND, ms);
        }
        return c.getTime();
    }

    /**
     * Calendar & Date are only accurate to milliseconds.
     */
    private static String prepareMillis(String milli) {
        if (StringUtilities.isEmpty(milli)) {
            return "000";
        }
        final int len = milli.length();
        if (len == 1) {
            return milli + "00";
        } else if (len == 2) {
            return milli + "0";
        } else {
            return milli.substring(0, 3);
        }
    }

    private static Date parseEpochString(String dateStr) {
        long num = Long.parseLong(dateStr);
        if (dateStr.length() < 8) {         // days since epoch (good until 1970 +/- 27,397 years)
            return new Date(LocalDate.ofEpochDay(num).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } else if (dateStr.length() < 13) { // seconds since epoch (good until 1970 +/- 31,709 years)
            return new Date(num * 1000);
        } else {                            // millis since epoch (good until 1970 +/- 31,709,791 years)
            return new Date(num);
        }
    }
}