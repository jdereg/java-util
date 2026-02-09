package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for CalendarConversions bugs.
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
class CalendarConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    // Some interesting timezones to test with
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");         // UTC+9
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");       // UTC+1/+2
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York"); // UTC-5/-4

    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return zoneId; }
        };
        return new Converter(options);
    }

    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);  // month is 0-based in Calendar
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testCalendarToYearMonth() {
        assertEquals(YearMonth.of(1888, 1),
                converter.convert(createCalendar(1888, 1, 2, 12, 30, 45, 123, TOKYO), YearMonth.class));
        assertEquals(YearMonth.of(1969, 12),
                converter.convert(createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS), YearMonth.class));
        assertEquals(YearMonth.of(1970, 1),
                converter.convert(createCalendar(1970, 1, 1, 0, 0, 1, 1, NEW_YORK), YearMonth.class));
        assertEquals(YearMonth.of(2023, 6),
                converter.convert(createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO), YearMonth.class));
    }

    @Test
    void testCalendarToYear() {
        assertEquals(Year.of(1888),
                converter.convert(createCalendar(1888, 1, 2, 9, 15, 30, 333, PARIS), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(createCalendar(1969, 12, 31, 18, 45, 15, 777, NEW_YORK), Year.class));
        // Calendar at 1970-01-01 06:20 Tokyo — Calendar's own zone says year 1970
        assertEquals(Year.of(1970),
                converter.convert(createCalendar(1970, 1, 1, 6, 20, 10, 111, TOKYO), Year.class));
        assertEquals(Year.of(2023),
                converter.convert(createCalendar(2023, 6, 15, 21, 5, 55, 888, PARIS), Year.class));
    }

    @Test
    void testCalendarToMonthDay() {
        assertEquals(MonthDay.of(1, 2),
                converter.convert(createCalendar(1888, 1, 2, 3, 45, 20, 222, NEW_YORK), MonthDay.class));
        assertEquals(MonthDay.of(12, 31),
                converter.convert(createCalendar(1969, 12, 31, 14, 25, 35, 444, TOKYO), MonthDay.class));
        assertEquals(MonthDay.of(1, 1),
                converter.convert(createCalendar(1970, 1, 1, 8, 50, 40, 666, PARIS), MonthDay.class));
        assertEquals(MonthDay.of(6, 15),
                converter.convert(createCalendar(2023, 6, 15, 17, 10, 5, 999, NEW_YORK), MonthDay.class));
    }

    // ---- Bug: toYear/toYearMonth/toMonthDay/toSqlDate use converter's zone instead of Calendar's zone ----
    // toZonedDateTime correctly uses Calendar's zone. These methods should be consistent.

    @Test
    void toYear_usesCalendarZone_notConverterZone() {
        // Calendar: 2024-01-01 00:30 Tokyo (= 2023-12-31 15:30 UTC)
        Calendar cal = Calendar.getInstance(TOKYO);
        cal.clear();
        cal.set(2024, Calendar.JANUARY, 1, 0, 30, 0);

        // Converter configured for UTC — different from Calendar's zone
        Converter utcConverter = converterWithZone(ZoneId.of("UTC"));

        Year result = CalendarConversions.toYear(cal, utcConverter);
        // Should be 2024 (Calendar's Tokyo zone), NOT 2023 (converter's UTC zone)
        assertEquals(Year.of(2024), result);
    }

    @Test
    void toYearMonth_usesCalendarZone_notConverterZone() {
        Calendar cal = Calendar.getInstance(TOKYO);
        cal.clear();
        cal.set(2024, Calendar.JANUARY, 1, 0, 30, 0);

        Converter utcConverter = converterWithZone(ZoneId.of("UTC"));

        YearMonth result = CalendarConversions.toYearMonth(cal, utcConverter);
        // Should be 2024-01 (Tokyo), NOT 2023-12 (UTC)
        assertEquals(YearMonth.of(2024, 1), result);
    }

    @Test
    void toMonthDay_usesCalendarZone_notConverterZone() {
        // Calendar: 2024-03-01 00:30 Tokyo (= 2024-02-29 15:30 UTC — leap day!)
        Calendar cal = Calendar.getInstance(TOKYO);
        cal.clear();
        cal.set(2024, Calendar.MARCH, 1, 0, 30, 0);

        Converter utcConverter = converterWithZone(ZoneId.of("UTC"));

        MonthDay result = CalendarConversions.toMonthDay(cal, utcConverter);
        // Should be March 1 (Tokyo), NOT February 29 (UTC)
        assertEquals(MonthDay.of(3, 1), result);
    }

    @Test
    void toSqlDate_usesCalendarZone_notConverterZone() {
        Calendar cal = Calendar.getInstance(TOKYO);
        cal.clear();
        cal.set(2024, Calendar.JANUARY, 1, 0, 30, 0);

        Converter utcConverter = converterWithZone(ZoneId.of("UTC"));

        java.sql.Date result = CalendarConversions.toSqlDate(cal, utcConverter);
        // Should be 2024-01-01 (Calendar's zone), not 2023-12-31 (UTC)
        assertEquals("2024-01-01", result.toString());
    }
}