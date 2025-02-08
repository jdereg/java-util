package com.cedarsoftware.util.convert;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;

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
class LocalDateConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testLocalDateToYearMonth() {
        assertEquals(YearMonth.of(1888, 1),
                converter.convert(LocalDate.of(1888, 1, 2), YearMonth.class));
        assertEquals(YearMonth.of(1969, 12),
                converter.convert(LocalDate.of(1969, 12, 31), YearMonth.class));
        assertEquals(YearMonth.of(1970, 1),
                converter.convert(LocalDate.of(1970, 1, 1), YearMonth.class));
        assertEquals(YearMonth.of(2023, 6),
                converter.convert(LocalDate.of(2023, 6, 15), YearMonth.class));
    }

    @Test
    void testLocalDateToYear() {
        assertEquals(Year.of(1888),
                converter.convert(LocalDate.of(1888, 1, 2), Year.class));
        assertEquals(Year.of(1969),
                converter.convert(LocalDate.of(1969, 12, 31), Year.class));
        assertEquals(Year.of(1970),
                converter.convert(LocalDate.of(1970, 1, 1), Year.class));
        assertEquals(Year.of(2023),
                converter.convert(LocalDate.of(2023, 6, 15), Year.class));
    }

    @Test
    void testLocalDateToMonthDay() {
        assertEquals(MonthDay.of(1, 2),
                converter.convert(LocalDate.of(1888, 1, 2), MonthDay.class));
        assertEquals(MonthDay.of(12, 31),
                converter.convert(LocalDate.of(1969, 12, 31), MonthDay.class));
        assertEquals(MonthDay.of(1, 1),
                converter.convert(LocalDate.of(1970, 1, 1), MonthDay.class));
        assertEquals(MonthDay.of(6, 15),
                converter.convert(LocalDate.of(2023, 6, 15), MonthDay.class));
    }
}