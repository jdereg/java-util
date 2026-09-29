package com.cedarsoftware.util;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A float with a fraction is the double of the decimal it prints (see MultiKeyMapNumericValueTest), and MultiKeyMap
 * reads that decimal with its own parser rather than Double.parseDouble(), which was about 27 ns of the 65 a float
 * cost -- per element for a float[] key. The parser must give the very double parseDouble() gives. Every one of the
 * 2,499,805,184 floats with a fraction was checked once, on JDK 21 and on JDK 8, with no difference; this test
 * checks a spread of about four million of them, plus the families at the edges of the parser's fast path.
 */
class MultiKeyMapFloatDecimalParseTest {

    private static Method parser() throws Exception {
        Method m = MultiKeyMap.class.getDeclaredMethod("parseDecimal", String.class);
        m.setAccessible(true);
        return m;
    }

    private static void check(Method parse, float f) throws Exception {
        double w = f;
        if (!Double.isFinite(w) || w == Math.rint(w)) {
            return;                                 // whole and non-finite floats never reach the parser
        }
        String s = Float.toString(f);
        double expected = Double.parseDouble(s);
        double actual = (Double) parse.invoke(null, s);
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual), s);
    }

    @Test
    void givesTheDoubleParseDoubleGives() throws Exception {
        Method parse = parser();
        for (long bits = 0; bits <= 0xFFFFFFFFL; bits += 1031) {
            check(parse, Float.intBitsToFloat((int) bits));
        }
    }

    /** Tiny floats (exponents beyond 10^-22 take parseDouble()), values near 1, and many digits. */
    @Test
    void atTheEdgesOfTheFastPath() throws Exception {
        Method parse = parser();
        float[] edges = {Float.MIN_VALUE, Float.MIN_NORMAL, 1.0E-20f, 1.0E-22f, 1.0E-23f, 1.2345678E-15f, 0.1f, 0.2f,
                0.3f, 0.7f, 1.1f, 3.4028235E-5f, 0.99999994f, 1.0000001f, 8388607.5f, 4194303.8f, 1.0E-3f, 9.999999E-4f,
                123.456f, 0.001f, 1.0E-5f, 3.14159265f, 2.7182817f};
        for (float f : edges) {
            check(parse, f);
            check(parse, -f);
            check(parse, Math.nextUp(f));
            check(parse, Math.nextDown(f));
        }
    }
}
