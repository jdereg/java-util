package com.cedarsoftware.util.convert;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JDK 8's Duration.parse() drops the sign of a seconds component between -1 and 0 (JDK-8054978, fixed
 * in JDK 9): "PT-0.001S" parses as +1 ms -- JDK 8 cannot read back its own Duration.ofMillis(-1).toString().
 * String -> Duration inherited that, so on JDK 8 it silently flipped the sign. The expected values here
 * are built arithmetically, never with Duration.parse(), so the test is meaningful on every JDK.
 */
class StringToDurationNegativeFractionTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Duration convert(String s) {
        return converter.convert(s, Duration.class);
    }

    @ParameterizedTest
    @CsvSource({
            // text,                     seconds,  nanos adjustment (added to seconds)
            "PT-0.001S,                  0,        -1000000",
            "PT-0.000000001S,            0,        -1",
            "PT-0.5S,                    0,        -500000000",
            "PT-0.999999999S,            0,        -999999999",
            "pt-0.25s,                   0,        -250000000",
            "'PT-0,25S',                 0,        -250000000",
            "PT1H-0.5S,                  3600,     -500000000",
            "PT2M-0.1S,                  120,      -100000000",
            "P1DT-0.5S,                  86400,    -500000000",
            "-PT-0.5S,                   0,        500000000",
            "+PT-0.5S,                   0,        -500000000",
            "-PT1H-0.5S,                 -3600,    500000000",
            // not the bug's shape: unchanged
            "PT-1.5S,                    -1,       -500000000",
            "PT0.5S,                     0,        500000000",
            "-PT0.001S,                  0,        -1000000",
            "PT-0S,                      0,        0",
            "PT-0.0S,                    0,        0",
            "PT1H,                       3600,     0"})
    void parsesWithTheSignIntact(String text, long seconds, long nanos) {
        assertEquals(Duration.ofSeconds(seconds).plusNanos(nanos), convert(text));
    }

    @Test
    void readsBackDurationToString() {
        long[] millis = {-1, -999, -1000, -1001, -123456, 1, 999};
        for (long ms : millis) {
            Duration d = Duration.ofMillis(ms);
            assertEquals(d, convert(d.toString()), "round trip of " + d);
        }
        Duration nano = Duration.ofNanos(-1);
        assertEquals(nano, convert(nano.toString()));
    }
}
