package com.cedarsoftware.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * 12-31-2023, 12/31/2023, 12.31.2023     mm is 1-12 or 01-12, dd is 1-31 or 01-31, and yyyy can be 0000 to 9999.
 *                                  
 * 2023-12-31, 2023/12/31, 2023.12.31     mm is 1-12 or 01-12, dd is 1-31 or 01-31, and yyyy can be 0000 to 9999.
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
 *                                  supports up to millisecond precision, so anything after 3 decimal places is ignored.
 *
 * hh:mm:offset -or-                offset can be specified as +HH:mm, +HHmm, +HH, -HH:mm, -HHmm, -HH, or Z (GMT)
 * hh:mm:ss.sss:offset              which will match: "12:34", "12:34:56", "12:34.789", "12:34:56.789", "12:34+01:00",
 *                                  "12:34:56+1:00", "12:34-01", "12:34:56-1", "12:34Z", "12:34:56Z"
 *
 * hh:mm:zone -or-                  Zone can be specified as Z (Zulu = UTC), older short forms: GMT, EST, CST, MST,
 * hh:mm:ss.sss:zone                PST, IST, JST, BST etc. as well as the long forms: "America/New York", "Asia/Saigon",
 *                                  etc. See ZoneId.getAvailableZoneIds().
 * </pre>
 * DateUtilities will parse Epoch-based integer-based value. It is considered number of milliseconds since Jan, 1970 GMT.
 * <pre>
 * "0" to                           A string of numeric digits will be parsed and returned as the number of milliseconds
 * "999999999999999999"             the Unix Epoch, January 1st, 1970 00:00:00 UTC.
 * </pre>
 * On all patterns above (excluding the numeric epoch millis), if a day-of-week (e.g. Thu, Sunday, etc.) is included
 * (front, back, or between date and time), it will be ignored, allowing for even more formats than listed here.
 * The day-of-week is not be used to influence the Date calculation.
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
    private static final String days = "\\b(monday|mon|tuesday|tues|tue|wednesday|wed|thursday|thur|thu|friday|fri|saturday|sat|sunday|sun)\\b"; // longer before shorter matters
    private static final String mos = "\\b(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sept|Sep|October|Oct|November|Nov|December|Dec)\\b";
    private static final String yr = "(\\d{4})";
    private static final String dig1or2 = "\\d{1,2}";
    private static final String dig1or2grp = "(" + dig1or2 + ")";
    private static final String ord = dig1or2grp + "(st|nd|rd|th)?";
    private static final String dig2 = "\\d{2}";
    private static final String sep = "([./-])";
    private static final String ws = "\\s+";
    private static final String wsOp = "\\s*";
    private static final String wsOrComma = "[ ,]+";
    private static final String tzUnix = "([A-Z]{1,3})?";
    private static final String nano = "\\.\\d+";
    private static final String dayOfMon = dig1or2grp;
    private static final String tz_Hh_MM = "[+-]\\d{1,2}:\\d{2}";
    private static final String tz_HHMM = "[+-]\\d{4}";
    private static final String tz_Hh = "[+-]\\d{1,2}";
    private static final String tzNamed = wsOp + "\\[?[A-Za-z][A-Za-z0-9~\\/._+-]+]?";

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
            "(" + dig2 + "):(" + dig2 + "):?(" + dig2 + ")?(" + nano + ")?(" + tz_Hh_MM + "|" + tz_HHMM + "|" + tz_Hh + "|Z|" + tzNamed + ")?",    // 5 groups
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

    /**
     * Main API. Retrieve date-time from passed in String.  If the date-time given does not include a timezone or
     * timezone offset, then ZoneId.systemDefault() will be used.
     * @param dateStr String containing a date.  If there is excess content, it will be ignored.
     * @return Date instance that represents the passed in date.  See comments at top of class for supported
     * formats.  This API is intended to be super flexible in terms of what it can parse.  If a null or empty String is
     * passed in, null will be returned.
     */
    public static Date parseDate(String dateStr) {
        if (StringUtilities.isEmpty(dateStr)) {
            return null;
        }
        ZonedDateTime zonedDateTime = parseDate(dateStr, ZoneId.systemDefault(), false);
        return new Date(zonedDateTime.toInstant().toEpochMilli());
    }

    /**
     * Main API. Retrieve date-time from passed in String.  The boolean enSureSoloDate, if set true, ensures that
     * no other non-date content existed in the String.  That requires additional time to verify.
     * @param dateStr String containing a date.  See DateUtilities class Javadoc for all the supported formats.  Cannot
     *                be null or empty String.
     * @param defaultZoneId ZoneId to use if no timezone or timezone offset is given.  Cannot be null.
     * @param ensureDateTimeAlone If true, if there is excess non-Date content, it will throw an IllegalArgument exception.
     * @return ZonedDateTime instance converted from the passed in date String.  See comments at top of class for supported
     * formats.  This API is intended to be super flexible in terms of what it can parse. 
     */
    public static ZonedDateTime parseDate(String dateStr, ZoneId defaultZoneId, boolean ensureDateTimeAlone) {
        Convention.throwIfNullOrEmpty(dateStr, "'dateStr' must not be null or empty String.");
        Convention.throwIfNull(defaultZoneId, "ZoneId cannot be null.  Use ZoneId.of(\"America/New_York\"), ZoneId.systemDefault(), etc.");
        dateStr = dateStr.trim();

        if (allDigits.matcher(dateStr).matches()) {
            return Instant.ofEpochMilli(Long.parseLong(dateStr)).atZone(ZoneId.of("UTC"));
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
        String hour = null, min = null, sec = "00", fracSec = "0";
        remains = remains.trim();
        matcher = timePattern.matcher(remains);
        remnant = matcher.replaceFirst("");
        boolean noTime = false;
        
        if (remnant.length() < remains.length()) {
            hour = matcher.group(1);
            min = matcher.group(2);
            if (matcher.group(3) != null) {
                sec = matcher.group(3);
            }
            if (matcher.group(4) != null) {
                fracSec = "0." + matcher.group(4).substring(1);
            }
            if (matcher.group(5) != null) {
                tz = stripBrackets(matcher.group(5).trim());
            }
        }

        if (ensureDateTimeAlone) {
            verifyNoGarbageLeft(remnant);
        }

        ZoneId zoneId = StringUtilities.isEmpty(tz) ? defaultZoneId : getTimeZone(tz);
        ZonedDateTime zonedDateTime = getDate(dateStr, zoneId, year, month, day, hour, min, sec, fracSec);
        return zonedDateTime;
    }

    private static ZonedDateTime getDate(String dateStr,
                                ZoneId zoneId,
                                String year,
                                int month,
                                String day,
                                String hour,
                                String min,
                                String sec,
                                String fracSec) {
        // Build Calendar from date, time, and timezone components, and retrieve Date instance from Calendar.
        int y = Integer.parseInt(year);
        int d = Integer.parseInt(day);

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12 inclusive, date: " + dateStr);
        }
        if (d < 1 || d > 31) {
            throw new IllegalArgumentException("Day must be between 1 and 31 inclusive, date: " + dateStr);
        }

        if (hour == null) {   // no [valid] time portion
            return ZonedDateTime.of(y, month, d, 0, 0, 0, 0, zoneId);
        } else {
            // Regex prevents these from ever failing to parse.
            int h = Integer.parseInt(hour);
            int mn = Integer.parseInt(min);
            int s = Integer.parseInt(sec);
            long nanoOfSec = convertFractionToNanos(fracSec);

            if (h > 23) {
                throw new IllegalArgumentException("Hour must be between 0 and 23 inclusive, time: " + dateStr);
            }
            if (mn > 59) {
                throw new IllegalArgumentException("Minute must be between 0 and 59 inclusive, time: " + dateStr);
            }
            if (s > 59) {
                throw new IllegalArgumentException("Second must be between 0 and 59 inclusive, time: " + dateStr);
            }

            ZonedDateTime zdt = ZonedDateTime.of(y, month, d, h, mn, s, (int) nanoOfSec, zoneId);
            return zdt;
        }
    }

    private static long convertFractionToNanos(String fracSec) {
        double fractionalSecond = Double.parseDouble(fracSec);
        return (long) (fractionalSecond * 1_000_000_000);
    }

    private static ZoneId getTimeZone(String tz) {
        if (tz != null) {
            if (tz.startsWith("-") || tz.startsWith("+")) {
                ZoneOffset offset = ZoneOffset.of(tz);
                return ZoneId.ofOffset("GMT", offset);
            } else {
                try {
                    return ZoneId.of(tz);
                } catch (Exception e) {
                    TimeZone timeZone = TimeZone.getTimeZone(tz);
                    if (timeZone.getRawOffset() == 0) {
                        throw e;
                    }
                    return timeZone.toZoneId();
                }
            }
        }
        return ZoneId.systemDefault();
    }

    private static void verifyNoGarbageLeft(String remnant) {
        // Clear out day of week (mon, tue, wed, ...)
        if (StringUtilities.length(remnant) > 0) {
            Matcher dayMatcher = dayPattern.matcher(remnant);
            remnant = dayMatcher.replaceFirst("").trim();
            if (remnant.startsWith("T")) {
                remnant = remnant.substring(1);
            }
        }

        // Verify that nothing or "," or timezone name is all that remains
        if (StringUtilities.length(remnant) > 0) {
            remnant = remnant.replaceAll(",|\\[.*?\\]", "").trim();
            if (!remnant.isEmpty()) {
                try {
                    ZoneId.of(remnant);
                }
                catch (Exception e) {
                    TimeZone timeZone = TimeZone.getTimeZone(remnant);
                    if (timeZone.getRawOffset() == 0) {
                        throw new IllegalArgumentException("Issue parsing date-time, other characters present: " + remnant);
                    }
                }
            }
        }
    }

    private static String stripBrackets(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("^\\[|\\]$", "");
    }
}