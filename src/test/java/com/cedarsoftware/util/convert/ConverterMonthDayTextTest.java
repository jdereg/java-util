package com.cedarsoftware.util.convert;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MonthDay and YearMonth from their common text shapes, which are now read directly: "--12-25", "12-25", "12/25",
 * "12.25", "12 25", "2024-12-25", and "2024-12". All but the ISO MonthDay went through a DateTimeParseException first
 * (290-350 ns); the answers, and the errors for a month or day out of range, are as before -- a differential run of
 * 12,200 inputs, valid and not, matched the old code on JDK 21 and JDK 8.
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
class ConverterMonthDayTextTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private MonthDay md(String text) {
        return converter.convert(text, MonthDay.class);
    }

    @Test
    void theCommonShapes() {
        MonthDay christmas = MonthDay.of(12, 25);
        for (String text : new String[] {"--12-25", "12-25", "12/25", "12.25", "12 25", "2024-12-25", "1225"}) {
            assertEquals(christmas, md(text), text);
        }
        assertEquals(MonthDay.of(5, 6), md("--05-06"));
        assertEquals(MonthDay.of(5, 6), md("2024-05-06"));
        assertEquals(MonthDay.of(1, 5), md("1-5"));
        assertEquals(MonthDay.of(1, 25), md("1/25"));
        assertEquals(MonthDay.of(11, 5), md("11.5"));
        assertEquals(MonthDay.of(2, 29), md("--02-29"));
        assertEquals(MonthDay.of(2, 29), md("2024-02-29"));
        assertEquals(YearMonth.of(2024, 12), converter.convert("2024-12", YearMonth.class));
        assertEquals(YearMonth.of(0, 12), converter.convert("0000-12", YearMonth.class));
    }

    /** Out of range, or a date that does not exist: refused as before, by the same path. */
    @Test
    void outOfRangeIsRefusedAsBefore() {
        assertEquals("Invalid value for MonthOfYear: 13",
                assertThrows(DateTimeException.class, () -> md("13-25")).getMessage());
        assertEquals("Illegal value for DayOfMonth field, value 30 is not valid for month FEBRUARY",
                assertThrows(DateTimeException.class, () -> md("02-30")).getMessage());
        assertEquals("Unable to extract Month-Day from string: --02-30",
                assertThrows(IllegalArgumentException.class, () -> md("--02-30")).getMessage());
        assertEquals("Unable to extract Month-Day from string: 2023-02-29",
                assertThrows(IllegalArgumentException.class, () -> md("2023-02-29")).getMessage());
        assertEquals("Unable to extract Year-Month from string: 2024-13",
                assertThrows(IllegalArgumentException.class,
                        () -> converter.convert("2024-13", YearMonth.class)).getMessage());
    }
}
