package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

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
class OffsetDateTimeConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    // Some interesting offsets to test with
    private static final ZoneOffset TOKYO = ZoneOffset.ofHours(9);    // UTC+9
    private static final ZoneOffset PARIS = ZoneOffset.ofHours(1);    // UTC+1
    private static final ZoneOffset NY = ZoneOffset.ofHours(-5);      // UTC-5
    private static final ZoneOffset NEPAL = ZoneOffset.ofHoursMinutes(5, 45); // UTC+5:45

    @Test
    void testOffsetDateTimeToYearMonth() {
        assertEquals(YearMonth.of(1888, 1),
                converter.convert(OffsetDateTime.of(1888, 1, 2, 12, 30, 45, 123_456_789, TOKYO), YearMonth.class));
        assertEquals(YearMonth.of(1969, 12),
                converter.convert(OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999, PARIS), YearMonth.class));
        assertEquals(YearMonth.of(1970, 1),
                converter.convert(OffsetDateTime.of(1970, 1, 1, 0, 0, 1, 1, NY), YearMonth.class));
        assertEquals(YearMonth.of(2023, 6),
                converter.convert(OffsetDateTime.of(2023, 6, 15, 15, 30, 0, 500_000_000, NEPAL), YearMonth.class));
    }

    @Test
    void testOffsetDateTimeToYear() {
        assertEquals(Year.of(1888),
                converter.convert(OffsetDateTime.of(1888, 1, 2, 9, 15, 30, 333_333_333, PARIS), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(OffsetDateTime.of(1969, 12, 31, 18, 45, 15, 777_777_777, NY), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(OffsetDateTime.of(1970, 1, 1, 6, 20, 10, 111_111_111, TOKYO), Year.class));
        assertEquals(Year.of(2023),
                converter.convert(OffsetDateTime.of(2023, 6, 15, 21, 5, 55, 888_888_888, NEPAL), Year.class));
    }

    @Test
    void testOffsetDateTimeToMonthDay() {
        assertEquals(MonthDay.of(1, 2),
                converter.convert(OffsetDateTime.of(1888, 1, 2, 3, 45, 20, 222_222_222, NY), MonthDay.class));
        assertEquals(MonthDay.of(12, 31),
                converter.convert(OffsetDateTime.of(1969, 12, 31, 14, 25, 35, 444_444_444, TOKYO), MonthDay.class));
        assertEquals(MonthDay.of(1, 1),
                converter.convert(OffsetDateTime.of(1970, 1, 1, 8, 50, 40, 666_666_666, PARIS), MonthDay.class));
        assertEquals(MonthDay.of(6, 15),
                converter.convert(OffsetDateTime.of(2023, 6, 15, 17, 10, 5, 999_999_999, NEPAL), MonthDay.class));
    }
}