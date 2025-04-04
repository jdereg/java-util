package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;

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
class TimestampConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Timestamp createTimestamp(String dateStr, int hour, int minute, int second, int nanos) {
        return Timestamp.from(LocalDate.parse(dateStr)
                .atTime(hour, minute, second, nanos)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    @Test
    void testTimestampToYearMonth() {
        assertEquals(YearMonth.of(1888, 1),
                converter.convert(createTimestamp("1888-01-02", 12, 30, 45, 123_456_789), YearMonth.class));
        assertEquals(YearMonth.of(1969, 12),
                converter.convert(createTimestamp("1969-12-31", 23, 59, 59, 999_999_999), YearMonth.class));
        assertEquals(YearMonth.of(1970, 1),
                converter.convert(createTimestamp("1970-01-01", 0, 0, 1, 1), YearMonth.class));
        assertEquals(YearMonth.of(2023, 6),
                converter.convert(createTimestamp("2023-06-15", 15, 30, 0, 500_000_000), YearMonth.class));
    }

    @Test
    void testTimestampToYear() {
        assertEquals(Year.of(1888),
                converter.convert(createTimestamp("1888-01-02", 9, 15, 30, 333_333_333), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(createTimestamp("1969-12-31", 18, 45, 15, 777_777_777), Year.class));
        assertEquals(Year.of(1970),
                converter.convert(createTimestamp("1970-01-01", 6, 20, 10, 111_111_111), Year.class));
        assertEquals(Year.of(2023),
                converter.convert(createTimestamp("2023-06-15", 21, 5, 55, 888_888_888), Year.class));
    }

    @Test
    void testTimestampToMonthDay() {
        assertEquals(MonthDay.of(1, 2),
                converter.convert(createTimestamp("1888-01-02", 3, 45, 20, 222_222_222), MonthDay.class));
        assertEquals(MonthDay.of(12, 31),
                converter.convert(createTimestamp("1969-12-31", 14, 25, 35, 444_444_444), MonthDay.class));
        assertEquals(MonthDay.of(1, 1),
                converter.convert(createTimestamp("1970-01-01", 8, 50, 40, 666_666_666), MonthDay.class));
        assertEquals(MonthDay.of(6, 15),
                converter.convert(createTimestamp("2023-06-15", 17, 10, 5, 999_999_999), MonthDay.class));
    }
}