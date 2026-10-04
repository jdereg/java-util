package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A digit-only String converts to MonthDay and YearMonth as the same number does, so a value survives a round trip
 * through int and String.
 */
class ConverterDigitStringTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * "1225" read as month 12, day 5: the MM.DD pattern's separator was "." (any character), so it matched 12, 2, 5.
     * The Integer 1225 gives --12-25, so MonthDay -> int -> String -> MonthDay failed.
     */
    @Test
    void digitsConvertToMonthDayAsTheNumberDoes() {
        assertEquals(MonthDay.of(12, 25), converter.convert("1225", MonthDay.class));
        assertEquals(MonthDay.of(12, 31), converter.convert("1231", MonthDay.class));
        assertEquals(MonthDay.of(1, 1), converter.convert("101", MonthDay.class));
        for (MonthDay md : new MonthDay[]{MonthDay.of(12, 25), MonthDay.of(1, 9), MonthDay.of(10, 10)}) {
            int asInt = converter.convert(md, int.class);
            assertEquals(md, converter.convert(converter.convert(asInt, String.class), MonthDay.class));
        }
        // separated forms read as before
        assertEquals(MonthDay.of(12, 25), converter.convert("12/25", MonthDay.class));
        assertEquals(MonthDay.of(12, 25), converter.convert("12-25", MonthDay.class));
        assertEquals(MonthDay.of(12, 25), converter.convert("12.25", MonthDay.class));
        assertEquals(MonthDay.of(1, 5), converter.convert("1 5", MonthDay.class));
        assertEquals(MonthDay.of(12, 25), converter.convert("--12-25", MonthDay.class));
    }

    /** "202401" was read by the date parser as epoch milliseconds (1969-12); the Integer 202401 gives 2024-01. */
    @Test
    void digitsConvertToYearMonthAsTheNumberDoes() {
        assertEquals(YearMonth.of(2024, 1), converter.convert("202401", YearMonth.class));
        assertEquals(YearMonth.of(1999, 12), converter.convert("199912", YearMonth.class));
        for (YearMonth ym : new YearMonth[]{YearMonth.of(2024, 1), YearMonth.of(1970, 12)}) {
            int asInt = converter.convert(ym, int.class);
            assertEquals(ym, converter.convert(converter.convert(asInt, String.class), YearMonth.class));
        }
        assertEquals(YearMonth.of(2024, 1), converter.convert("2024-01", YearMonth.class));
    }
}
