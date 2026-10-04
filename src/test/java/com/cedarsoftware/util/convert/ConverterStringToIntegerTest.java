package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import com.cedarsoftware.util.MathUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * String to Byte, Short, Integer and Long: an integer literal as Long.parseLong reads it, anything else through
 * BigDecimal (truncated toward zero), within the type's range; otherwise IllegalArgumentException.
 */
class ConverterStringToIntegerTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final Class<?>[] TYPES = {Byte.class, Short.class, Integer.class, Long.class};
    private static final long[] MIN = {Byte.MIN_VALUE, Short.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE};
    private static final long[] MAX = {Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE};

    /** The previous algorithm: valueOf first, and on its exception the BigDecimal path (truncated, range-checked). */
    private static Object reference(String text, int type) {
        String str = text.trim();
        if (str.isEmpty()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(str);
            if (value >= MIN[type] && value <= MAX[type]) {
                return value;
            }
        } catch (NumberFormatException e) {
            // the decimal path decides
        }
        try {
            BigDecimal big = MathUtilities.parseBigDecimal(str);
            long wholeDigits = (long) big.precision() - big.scale();     // as the converter: no huge setScale
            if (wholeDigits > 19) {
                return "fails";
            }
            if (wholeDigits <= 0) {
                return 0L;
            }
            big = big.setScale(0, RoundingMode.DOWN);
            if (big.compareTo(BigDecimal.valueOf(MIN[type])) < 0 || big.compareTo(BigDecimal.valueOf(MAX[type])) > 0) {
                return "fails";
            }
            return big.longValue();
        } catch (Exception e) {
            return "fails";
        }
    }

    private Object actual(String text, int type) {
        try {
            return ((Number) converter.convert(text, TYPES[type])).longValue();
        } catch (IllegalArgumentException e) {
            return "fails";
        }
    }

    @Test
    void answersAsBeforeForIntegersDecimalsExponentsAndJunk() {
        String[] fixed = {"0", "-0", "+7", "12345", "12345.0", "12345.9", "-12345.9", "1e3", "1E3", "1.5e2", "2.5E-1",
                "2147483647", "2147483648", "-2147483649", "9223372036854775807", "9223372036854775808", "1e19",
                "-9223372036854775808", "-9223372036854775809", "127", "128", "-128", "-129", "32767", "32768",
                "+", "-", "99999999999999999999", "-99999999999999999999", "123456789012345678901234567890",
                "18446744073709551621",
                " 42 ", "4 2", "abc", "12a", ".", "e", "1.2.3", "٣", "١٢٣", "0x10", "--5", "5-", "1_000", ""};
        for (String text : fixed) {
            for (int type = 0; type < TYPES.length; type++) {
                String where = TYPES[type].getSimpleName() + " '" + text + "'";
                assertEquals(reference(text, type), actual(text, type), where);
            }
        }
        Random random = new Random(11);
        String alphabet = "0123456789.eE+- x";
        for (int i = 0; i < 100_000; i++) {
            StringBuilder sb = new StringBuilder();
            int length = 1 + random.nextInt(12);
            for (int k = 0; k < length; k++) {
                sb.append(random.nextInt(4) == 0 ? alphabet.charAt(random.nextInt(alphabet.length()))
                        : (char) ('0' + random.nextInt(10)));
            }
            String text = sb.toString();
            for (int type = 0; type < TYPES.length; type++) {
                String where = TYPES[type].getSimpleName() + " '" + text + "'";
                assertEquals(reference(text, type), actual(text, type), where);
            }
        }
    }
}
