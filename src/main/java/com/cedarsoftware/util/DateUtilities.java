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
 * Utility for parsing String dates with optional times, supporting a wide variety of formats and patterns.
 * Handles inconsistent input formats, optional time components, and various timezone specifications.
 *
 * <h2>Supported Date Formats</h2>
 * <table border="1" summary="Supported date formats">
 *   <tr><th>Format</th><th>Example</th><th>Description</th></tr>
 *   <tr>
 *     <td>Numeric with separators</td>
 *     <td>12-31-2023, 12/31/2023, 12.31.2023</td>
 *     <td>mm is 1-12 or 01-12, dd is 1-31 or 01-31, yyyy is 0000-9999</td>
 *   </tr>
 *   <tr>
 *     <td>ISO-style</td>
 *     <td>2023-12-31, 2023/12/31, 2023.12.31</td>
 *     <td>yyyy-mm-dd format with flexible separators (-, /, .)</td>
 *   </tr>
 *   <tr>
 *     <td>Month first</td>
 *     <td>January 6th, 2024</td>
 *     <td>Month name (full or 3-4 letter), day with optional suffix, year</td>
 *   </tr>
 *   <tr>
 *     <td>Day first</td>
 *     <td>17th January 2024</td>
 *     <td>Day with optional suffix, month name, year</td>
 *   </tr>
 *   <tr>
 *     <td>Year first</td>
 *     <td>2024 January 31st</td>
 *     <td>Year, month name, day with optional suffix</td>
 *   </tr>
 *   <tr>
 *     <td>Unix style</td>
 *     <td>Sat Jan 6 11:06:10 EST 2024</td>
 *     <td>Day of week, month, day, time, timezone, year</td>
 *   </tr>
 * </table>
 *
 * <h2>Supported Time Formats</h2>
 * <table border="1" summary="Supported time formats">
 *   <tr><th>Format</th><th>Example</th><th>Description</th></tr>
 *   <tr>
 *     <td>Basic time</td>
 *     <td>13:30</td>
 *     <td>24-hour format (00-23:00-59)</td>
 *   </tr>
 *   <tr>
 *     <td>With seconds</td>
 *     <td>13:30:45</td>
 *     <td>Includes seconds (00-59)</td>
 *   </tr>
 *   <tr>
 *     <td>With fractional seconds</td>
 *     <td>13:30:45.123456</td>
 *     <td>Variable precision fractional seconds</td>
 *   </tr>
 *   <tr>
 *     <td>With offset</td>
 *     <td>13:30+01:00, 13:30:45-0500</td>
 *     <td>Supports +HH:mm, +HHmm, +HH, -HH:mm, -HHmm, -HH, Z</td>
 *   </tr>
 *   <tr>
 *     <td>With timezone</td>
 *     <td>13:30 EST, 13:30:45 America/New_York</td>
 *     <td>Supports abbreviations and full zone IDs</td>
 *   </tr>
 * </table>
 *
 * <h2>Special Features</h2>
 * <ul>
 *   <li>Supports Unix epoch milliseconds (e.g., "1640995200000")</li>
 *   <li>Optional day-of-week in any position (ignored in date calculation)</li>
 *   <li>Flexible date/time separator (space or 'T')</li>
 *   <li>Time can appear before or after date</li>
 *   <li>Extensive timezone support including abbreviations and full zone IDs</li>
 *   <li>Handles ambiguous timezone abbreviations with population-based resolution</li>
 *   <li>Thread-safe implementation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Basic parsing with system default timezone
 * Date date1 = DateUtilities.parseDate("2024-01-15 14:30:00");
 *
 * // Parsing with specific timezone
 * ZonedDateTime date2 = DateUtilities.parseDate("2024-01-15 14:30:00",
 *     ZoneId.of("America/New_York"), true);
 *
 * // Parsing Unix style date
 * Date date3 = DateUtilities.parseDate("Tue Jan 15 14:30:00 EST 2024");
 * }</pre>
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
    private static final String days = "monday|mon|tuesday|tues|tue|wednesday|wed|thursday|thur|thu|friday|fri|saturday|sat|sunday|sun"; // longer before shorter matters
    private static final String mos = "January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sept|Sep|October|Oct|November|Nov|December|Dec";
    private static final String yr = "[+-]?\\d{4,5}\\b";
    private static final String d1or2 = "\\d{1,2}";
    private static final String d2 = "\\d{2}";
    private static final String ord = "st|nd|rd|th";
    private static final String sep = "[./-]";
    private static final String ws = "\\s+";
    private static final String wsOp = "\\s*";
    private static final String wsOrComma = "[ ,]+";
    private static final String tzUnix = "[A-Z]{1,3}";
    private static final String tz_Hh_MM = "[+-]\\d{1,2}:\\d{2}";
    private static final String tz_Hh_MM_SS = "[+-]\\d{1,2}:\\d{2}:\\d{2}";
    private static final String tz_HHMM = "[+-]\\d{4}";
    private static final String tz_Hh = "[+-]\\d{1,2}";
    private static final String tzNamed = wsOp + "\\[?[A-Za-z][A-Za-z0-9~\\/._+-]+]?";
    private static final String nano = "\\.\\d+";

    // Patterns defined in BNF influenced style using above named elements
    private static final Pattern isoDatePattern = Pattern.compile(    // Regex's using | (OR)
            "(" + yr + ")(" + sep + ")(" + d1or2 + ")" + "\\2" + "(" + d1or2 + ")|" +        // 2024/01/21 (yyyy/mm/dd -or- yyyy-mm-dd -or- yyyy.mm.dd)   [optional time, optional day of week]  \2 references 1st separator (ensures both same)
            "(" + d1or2 + ")(" + sep + ")(" + d1or2 + ")" + "\\6(" + yr + ")");              // 01/21/2024 (mm/dd/yyyy -or- mm-dd-yyyy -or- mm.dd.yyyy)   [optional time, optional day of week]  \6 references 2nd 1st separator (ensures both same)

    private static final Pattern alphaMonthPattern = Pattern.compile(
            "\\b(" + mos + ")\\b" + wsOrComma + "(" + d1or2 + ")(" + ord + ")?" + wsOrComma + "(" + yr + ")|" +   // Jan 21st, 2024  (comma optional between all, day of week optional, time optional, ordinal text optional [st, nd, rd, th])
            "(" + d1or2 + ")(" + ord + ")?" + wsOrComma + "\\b(" + mos + ")\\b" + wsOrComma + "(" + yr + ")|" +         // 21st Jan, 2024  (ditto)
            "(" + yr + ")" + wsOrComma + "\\b(" + mos + "\\b)" + wsOrComma + "(" + d1or2 + ")(" + ord + ")?",           // 2024 Jan 21st   (ditto)
            Pattern.CASE_INSENSITIVE);

    private static final Pattern unixDateTimePattern = Pattern.compile(
            "\\b(" + days + ")\\b" + ws + "\\b(" + mos + ")\\b" + ws + "(" + d1or2 + ")" + ws + "(" + d2 + ":" + d2 + ":" + d2 + ")" + wsOp + "(" + tzUnix + ")?" + wsOp + "(" + yr + ")",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern timePattern = Pattern.compile(
            "(" + d2 + "):(" + d2 + ")(?::(" + d2 + ")(" + nano + ")?)?(" + tz_Hh_MM_SS + "|" + tz_Hh_MM + "|" + tz_HHMM + "|" + tz_Hh + "|Z)?(" + tzNamed + ")?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern dayPattern = Pattern.compile("\\b(" + days + ")\\b", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> months = new ConcurrentHashMap<>();
    private static final Map<String, String> ABBREVIATION_TO_TIMEZONE = new ConcurrentHashMap<>();

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

        // North American Time Zones
        ABBREVIATION_TO_TIMEZONE.put("EST", "America/New_York");    // Eastern Standard Time
        ABBREVIATION_TO_TIMEZONE.put("EDT", "America/New_York");    // Eastern Daylight Time

        // CST is ambiguous: could be Central Standard Time (North America) or China Standard Time
        ABBREVIATION_TO_TIMEZONE.put("CST", "America/Chicago");     // Central Standard Time

        ABBREVIATION_TO_TIMEZONE.put("CDT", "America/Chicago");     // Central Daylight Time
        // Note: CDT can also be Cuba Daylight Time (America/Havana)

        // MST is ambiguous: could be Mountain Standard Time (North America) or Myanmar Standard Time
        // Chose Myanmar Standard Time due to larger population
        // Conflicts: America/Denver (Mountain Standard Time)
        ABBREVIATION_TO_TIMEZONE.put("MST", "America/Denver");      // Mountain Standard Time

        ABBREVIATION_TO_TIMEZONE.put("MDT", "America/Denver");      // Mountain Daylight Time

        // PST is ambiguous: could be Pacific Standard Time (North America) or Philippine Standard Time
        ABBREVIATION_TO_TIMEZONE.put("PST", "America/Los_Angeles"); // Pacific Standard Time
        ABBREVIATION_TO_TIMEZONE.put("PDT", "America/Los_Angeles"); // Pacific Daylight Time

        ABBREVIATION_TO_TIMEZONE.put("AKST", "America/Anchorage");  // Alaska Standard Time
        ABBREVIATION_TO_TIMEZONE.put("AKDT", "America/Anchorage");  // Alaska Daylight Time

        ABBREVIATION_TO_TIMEZONE.put("HST", "Pacific/Honolulu");    // Hawaii Standard Time
        // Hawaii does not observe Daylight Saving Time

        // European Time Zones
        ABBREVIATION_TO_TIMEZONE.put("GMT", "Europe/London");       // Greenwich Mean Time

        // BST is ambiguous: could be British Summer Time or Bangladesh Standard Time
        // Chose British Summer Time as it's more commonly used in international contexts
        ABBREVIATION_TO_TIMEZONE.put("BST", "Europe/London");       // British Summer Time
        ABBREVIATION_TO_TIMEZONE.put("WET", "Europe/Lisbon");       // Western European Time
        ABBREVIATION_TO_TIMEZONE.put("WEST", "Europe/Lisbon");      // Western European Summer Time

        ABBREVIATION_TO_TIMEZONE.put("CET", "Europe/Berlin");       // Central European Time
        ABBREVIATION_TO_TIMEZONE.put("CEST", "Europe/Berlin");      // Central European Summer Time

        ABBREVIATION_TO_TIMEZONE.put("EET", "Europe/Kiev");         // Eastern European Time
        ABBREVIATION_TO_TIMEZONE.put("EEST", "Europe/Kiev");        // Eastern European Summer Time

        // Australia and New Zealand Time Zones
        ABBREVIATION_TO_TIMEZONE.put("AEST", "Australia/Brisbane"); // Australian Eastern Standard Time
        // Brisbane does not observe Daylight Saving Time

        ABBREVIATION_TO_TIMEZONE.put("AEDT", "Australia/Sydney");   // Australian Eastern Daylight Time

        ABBREVIATION_TO_TIMEZONE.put("ACST", "Australia/Darwin");   // Australian Central Standard Time
        // Darwin does not observe Daylight Saving Time

        ABBREVIATION_TO_TIMEZONE.put("ACDT", "Australia/Adelaide"); // Australian Central Daylight Time

        ABBREVIATION_TO_TIMEZONE.put("AWST", "Australia/Perth");    // Australian Western Standard Time
        // Perth does not observe Daylight Saving Time

        ABBREVIATION_TO_TIMEZONE.put("NZST", "Pacific/Auckland");   // New Zealand Standard Time
        ABBREVIATION_TO_TIMEZONE.put("NZDT", "Pacific/Auckland");   // New Zealand Daylight Time

        // South American Time Zones
        ABBREVIATION_TO_TIMEZONE.put("CLT", "America/Santiago");    // Chile Standard Time
        ABBREVIATION_TO_TIMEZONE.put("CLST", "America/Santiago");   // Chile Summer Time

        ABBREVIATION_TO_TIMEZONE.put("PYT", "America/Asuncion");    // Paraguay Standard Time
        ABBREVIATION_TO_TIMEZONE.put("PYST", "America/Asuncion");   // Paraguay Summer Time

        // ART is ambiguous: could be Argentina Time or Eastern European Time (Egypt)
        // Chose Argentina Time due to larger population
        // Conflicts: Africa/Cairo (Egypt)
        ABBREVIATION_TO_TIMEZONE.put("ART", "America/Argentina/Buenos_Aires"); // Argentina Time

        // Middle East Time Zones
        // IST is ambiguous: could be India Standard Time, Israel Standard Time, or Irish Standard Time
        // Chose India Standard Time due to larger population
        // Conflicts: Asia/Jerusalem (Israel), Europe/Dublin (Ireland)
        ABBREVIATION_TO_TIMEZONE.put("IST", "Asia/Kolkata");        // India Standard Time

        ABBREVIATION_TO_TIMEZONE.put("IDT", "Asia/Jerusalem");      // Israel Daylight Time

        ABBREVIATION_TO_TIMEZONE.put("IRST", "Asia/Tehran");        // Iran Standard Time
        ABBREVIATION_TO_TIMEZONE.put("IRDT", "Asia/Tehran");        // Iran Daylight Time

        // Africa Time Zones
        ABBREVIATION_TO_TIMEZONE.put("WAT", "Africa/Lagos");        // West Africa Time
        ABBREVIATION_TO_TIMEZONE.put("CAT", "Africa/Harare");       // Central Africa Time

        // Asia Time Zones
        ABBREVIATION_TO_TIMEZONE.put("JST", "Asia/Tokyo");          // Japan Standard Time

        // KST is ambiguous: could be Korea Standard Time or Kazakhstan Standard Time
        // Chose Korea Standard Time due to larger population
        // Conflicts: Asia/Almaty (Kazakhstan)
        ABBREVIATION_TO_TIMEZONE.put("KST", "Asia/Seoul");          // Korea Standard Time

        ABBREVIATION_TO_TIMEZONE.put("HKT", "Asia/Hong_Kong");      // Hong Kong Time

        // SGT is ambiguous: could be Singapore Time or Sierra Leone Time (defunct)
        // Chose Singapore Time due to larger population
        ABBREVIATION_TO_TIMEZONE.put("SGT", "Asia/Singapore");      // Singapore Time

        // MST is mapped to America/Denver (Mountain Standard Time) above
        // MYT is Malaysia Time
        ABBREVIATION_TO_TIMEZONE.put("MYT", "Asia/Kuala_Lumpur");   // Malaysia Time

        // Additional Time Zones
        ABBREVIATION_TO_TIMEZONE.put("MSK", "Europe/Moscow");       // Moscow Standard Time
        ABBREVIATION_TO_TIMEZONE.put("MSD", "Europe/Moscow");       // Moscow Daylight Time (historical)

        ABBREVIATION_TO_TIMEZONE.put("EAT", "Africa/Nairobi");      // East Africa Time

        // HKT is unique to Hong Kong Time
        // No conflicts

        // ICT is unique to Indochina Time
        // Covers Cambodia, Laos, Thailand, Vietnam
        ABBREVIATION_TO_TIMEZONE.put("ICT", "Asia/Bangkok");        // Indochina Time

        // Chose "COT" for Colombia Time
        ABBREVIATION_TO_TIMEZONE.put("COT", "America/Bogota");      // Colombia Time

        // Chose "PET" for Peru Time
        ABBREVIATION_TO_TIMEZONE.put("PET", "America/Lima");        // Peru Time

        // Chose "PKT" for Pakistan Standard Time
        ABBREVIATION_TO_TIMEZONE.put("PKT", "Asia/Karachi");        // Pakistan Standard Time

        // Chose "WIB" for Western Indonesian Time
        ABBREVIATION_TO_TIMEZONE.put("WIB", "Asia/Jakarta");        // Western Indonesian Time

        // Chose "KST" for Korea Standard Time (already mapped)
        // Chose "PST" for Philippine Standard Time (already mapped)
        // Chose "CCT" for China Coast Time (historical, now China Standard Time)
        // Chose "SGT" for Singapore Time (already mapped)

        // Add more mappings as needed, following the same pattern
    }

    private DateUtilities() {
    }

    /**
     * Original API. If the date-time given does not include a timezone offset or name, then ZoneId.systemDefault()
     * will be used. We recommend using parseDate(String, ZoneId, boolean) version, so you can control the default
     * timezone used when one is not specified.
     * @param dateStr String containing a date.  If there is excess content, it will throw an IllegalArgumentException.
     * @return Date instance that represents the passed in date.  See comments at top of class for supported
     * formats.  This API is intended to be super flexible in terms of what it can parse. If a null or empty String is
     * passed in, null will be returned.
     */
    public static Date parseDate(String dateStr) {
        if (StringUtilities.isEmpty(dateStr)) {
            return null;
        }
        Instant instant;
        ZonedDateTime dateTime = parseDate(dateStr, ZoneId.systemDefault(), true);
        instant = Instant.from(dateTime);
        return Date.from(instant);
    }

    /**
     * Main API. Retrieve date-time from passed in String.  The boolean ensureDateTimeAlone, if set true, ensures that
     * no other non-date content existed in the String.
     * @param dateStr String containing a date.  See DateUtilities class Javadoc for all the supported formats.
     * @param defaultZoneId ZoneId to use if no timezone offset or name is given.  Cannot be null.
     * @param ensureDateTimeAlone If true, if there is excess non-Date content, it will throw an IllegalArgument exception.
     * @return ZonedDateTime instance converted from the passed in date String.  See comments at top of class for supported
     * formats.  This API is intended to be super flexible in terms of what it can parse. If a null or empty String is
     * passed in, null will be returned.
     */
    public static ZonedDateTime parseDate(String dateStr, ZoneId defaultZoneId, boolean ensureDateTimeAlone) {
        dateStr = StringUtilities.trimToNull(dateStr);
        if (dateStr == null) {
            return null;
        }
        Convention.throwIfNull(defaultZoneId, "ZoneId cannot be null.  Use ZoneId.of(\"America/New_York\"), ZoneId.systemDefault(), etc.");

        // If purely digits => epoch millis
        if (allDigits.matcher(dateStr).matches()) {
            return Instant.ofEpochMilli(Long.parseLong(dateStr)).atZone(defaultZoneId);
        }

        String year, day, remains, tz = null;
        int month;

        // 1) Try matching ISO or numeric style date
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
            // 2) Try alphaMonthPattern
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
                // 3) Try unixDateTimePattern
                matcher = unixDateTimePattern.matcher(dateStr);
                if (matcher.replaceFirst("").length() == dateStr.length()) {
                    throw new IllegalArgumentException("Unable to parse: " + dateStr + " as a date-time");
                }
                year = matcher.group(6);
                String mon = matcher.group(2);
                month = months.get(mon.trim().toLowerCase());
                day = matcher.group(3);

                // e.g. "EST"
                tz = matcher.group(5);

                // time portion remains to parse
                remains = matcher.group(4);
            }
        }

        // 4) Parse time portion (could appear before or after date)
        String hour = null, min = null, sec = "00", fracSec = "0";
        remains = remains.trim();
        matcher = timePattern.matcher(remains);
        remnant = matcher.replaceFirst("");

        if (remnant.length() < remains.length()) {
            hour = matcher.group(1);
            min  = matcher.group(2);
            if (matcher.group(3) != null) {
                sec = matcher.group(3);
            }
            if (matcher.group(4) != null) {
                fracSec = "0" + matcher.group(4);
            }
            if (matcher.group(5) != null) {
                tz = matcher.group(5).trim();
            }
            if (matcher.group(6) != null) {
                tz = stripBrackets(matcher.group(6).trim());
            }
        }

        // 5) If strict, verify no leftover text
        if (ensureDateTimeAlone) {
            verifyNoGarbageLeft(remnant);
        }

        ZoneId zoneId;
        try {
            zoneId = StringUtilities.isEmpty(tz) ? defaultZoneId : getTimeZone(tz);
        } catch (Exception e) {
            if (ensureDateTimeAlone) {
                // In strict mode, rethrow
                throw e;
            }
            // else in non-strict mode, ignore the invalid zone and default
            zoneId = defaultZoneId;
        }

        // 6) Build the ZonedDateTime
        return getDate(dateStr, zoneId, year, month, day, hour, min, sec, fracSec);
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

        if (hour == null) { // no [valid] time portion
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

            return ZonedDateTime.of(y, month, d, h, mn, s, (int) nanoOfSec, zoneId);
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
                        String zoneName = ABBREVIATION_TO_TIMEZONE.get(tz);
                        if (zoneName != null) {
                            return ZoneId.of(zoneName);
                        }
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
        }

        // Verify that nothing, "T" or "," is all that remains
        if (StringUtilities.length(remnant) > 0) {
            remnant = remnant.replaceAll("[T,]", "").trim();
            if (!remnant.isEmpty()) {
                throw new IllegalArgumentException("Issue parsing date-time, other characters present: " + remnant);
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
