package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Timestamp to String: ISO-8601 in UTC, with 3 decimals for whole milliseconds and 9 otherwise, the same under any
 * default locale.
 */
class ConverterTimestampToStringTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /** The text as the conversion has always built it: a formatter made for each call. */
    private static String reference(Timestamp ts) {
        String pattern = ts.getNanos() % 1_000_000 == 0
                ? "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" : "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";
        return ts.toInstant().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(pattern));
    }

    @Test
    void textIsAsBeforeForMillisecondAndNanosecondTimestamps() {
        Locale saved = Locale.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.US, Locale.forLanguageTag("th-TH"),
                    Locale.forLanguageTag("ar-SA"), Locale.GERMANY}) {
                Locale.setDefault(locale);
                Random random = new Random(7);
                for (int i = 0; i < 5_000; i++) {
                    long millis = random.nextLong() % 32_503_680_000_000L;      // years 0970 to 2999
                    Timestamp ts = new Timestamp(millis);
                    if (i % 2 == 0) {
                        ts.setNanos(random.nextInt(1_000_000_000));
                    }
                    assertEquals(reference(ts), converter.convert(ts, String.class), locale + " " + ts);
                }
            }
        } finally {
            Locale.setDefault(saved);
        }
    }
}
