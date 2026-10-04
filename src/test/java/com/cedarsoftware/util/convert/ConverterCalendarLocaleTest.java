package com.cedarsoftware.util.convert;

import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every Calendar the Converter makes uses the calendar system of the default FORMAT locale, as Calendar.getInstance
 * does: a BuddhistCalendar where that locale is th_TH. String to Calendar alone took the general default locale, so
 * when the two were set apart it made a different kind of Calendar than Long to Calendar did.
 */
class ConverterCalendarLocaleTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void stringToCalendarUsesTheFormatLocaleAsTheOthersDo() {
        Locale saved = Locale.getDefault();
        Locale savedFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.US);
            Locale.setDefault(Locale.Category.FORMAT, new Locale("th", "TH"));
            Instant instant = Instant.parse("2024-07-01T12:00:00Z");

            Calendar fromLong = converter.convert(instant.toEpochMilli(), Calendar.class);
            Calendar fromString = converter.convert("2024-07-01T12:00:00Z", Calendar.class);
            assertEquals(Calendar.getInstance().getClass(), fromLong.getClass());
            assertEquals(fromLong.getClass(), fromString.getClass());
            assertEquals(2567, fromString.get(Calendar.YEAR));
            assertEquals(instant.toEpochMilli(), fromString.getTimeInMillis());
        } finally {
            Locale.setDefault(saved);
            Locale.setDefault(Locale.Category.FORMAT, savedFormat);
        }
    }
}
