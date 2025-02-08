package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class ZonedDateTimeConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    // Some interesting timezones to test with
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");         // UTC+9
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");       // UTC+1/+2
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York"); // UTC-5/-4

    @Test
    void testZonedDateTimeToYearMonth() {
        assertEquals(YearMonth.of(1888, 1),
                converter.convert(ZonedDateTime.of(1888, 1, 2, 12, 30, 45, 123_456_789, TOKYO), YearMonth.class));
        assertEquals(YearMonth.of(1969, 12),
                converter.convert(ZonedDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999, PARIS), YearMonth.class));
        assertEquals(YearMonth.of(1970, 1),
                converter.convert(ZonedDateTime.of(1970, 1, 1, 0, 0, 1, 1, NEW_YORK), YearMonth.class));
        assertEquals(YearMonth.of(2023, 6),
                converter.convert(ZonedDateTime.of(2023, 6, 15, 15, 30, 0, 500_000_000, TOKYO), YearMonth.class));
    }

    @Test
    void testZonedDateTimeToYear() {
        assertEquals(Year.of(1888),
                converter.convert(ZonedDateTime.of(1888, 1, 2, 9, 15, 30, 333_333_333, PARIS), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(ZonedDateTime.of(1969, 12, 31, 18, 45, 15, 777_777_777, NEW_YORK), Year.class));
        assertEquals(Year.of(1969),  // was 1970
                converter.convert(ZonedDateTime.of(1970, 1, 1, 6, 20, 10, 111_111_111, TOKYO), Year.class));
        assertEquals(Year.of(2023),
                converter.convert(ZonedDateTime.of(2023, 6, 15, 21, 5, 55, 888_888_888, PARIS), Year.class));
    }

    @Test
    void testZonedDateTimeToMonthDay() {
        assertEquals(MonthDay.of(1, 2),
                converter.convert(ZonedDateTime.of(1888, 1, 2, 3, 45, 20, 222_222_222, NEW_YORK), MonthDay.class));
        assertEquals(MonthDay.of(12, 31),
                converter.convert(ZonedDateTime.of(1969, 12, 31, 14, 25, 35, 444_444_444, TOKYO), MonthDay.class));
        assertEquals(MonthDay.of(1, 1),
                converter.convert(ZonedDateTime.of(1970, 1, 1, 8, 50, 40, 666_666_666, PARIS), MonthDay.class));
        assertEquals(MonthDay.of(6, 15),
                converter.convert(ZonedDateTime.of(2023, 6, 15, 17, 10, 5, 999_999_999, NEW_YORK), MonthDay.class));
    }
}