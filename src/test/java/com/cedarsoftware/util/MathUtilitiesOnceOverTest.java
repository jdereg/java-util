package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 4.119.0 once-over of MathUtilities. Its fast number parsers promise what the JDK's return, which is the
 * exact value rounded once. Both are the oracle here: the exact decimal ({@code BigDecimal}) rounded once, and
 * {@code Double.parseDouble} on the same text. They differ on JDK 8 only, whose parser answers Infinity for some
 * numbers written with hundreds of zeros after the point and an exponent past 330 ({@code 0.} + 463 zeros +
 * {@code 515586E+490}, which is 5.15586E27). The fast parser gets those right itself, except where it hands the
 * text to the JDK (a result below the normal range), and there it answers as that JDK does.
 * <p>
 * The one that mattered: the vendored parser stopped reading an exponent at 1024 ({@code e10240} was taken as
 * {@code e1024}), then added it to the position of the decimal point. With about a thousand digits in the
 * significand the sum came back inside the double range, and a number far outside it was returned as an ordinary
 * one: {@code 0.} + 1,005 zeros + {@code 12345e10240}, which is 1.2345E+9234, came back as 1.2345E18 from
 * {@code parseDouble}, from the Converter, and from json-io with its default options. The shortest such text is 728
 * characters.
 */
class MathUtilitiesOnceOverTest {

    private static String zeros(int count) {
        return String.join("", Collections.nCopies(count, "0"));
    }

    /** Every way in: the bits of the exact value rounded once, or of what this JDK's own parser answers. */
    private static void assertParsesAsTheJdk(String text) {
        final long jdk = Double.doubleToRawLongBits(Double.parseDouble(text));
        final int jdkFloat = Float.floatToRawIntBits(Float.parseFloat(text));
        long exact = jdk;
        int exactFloat = jdkFloat;
        String plain = text.trim();
        if (plain.endsWith("d") || plain.endsWith("D") || plain.endsWith("f") || plain.endsWith("F")) {
            plain = plain.substring(0, plain.length() - 1);
        }
        try {
            String canonical = new BigDecimal(plain).toString();    // no leading zeros in it
            exact = Double.doubleToRawLongBits(Double.parseDouble(canonical));
            exactFloat = Float.floatToRawIntBits(Float.parseFloat(canonical));
        } catch (NumberFormatException notADecimalABigDecimalHolds) {
            // hex, or an exponent past the int range: the JDK's answer is the only one
        }
        char[] chars = text.toCharArray();
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        String shown = text.length() > 60 ? text.substring(0, 24) + "...(" + text.length() + " chars)..."
                + text.substring(text.length() - 14) : text;
        long[] doubles = {Double.doubleToRawLongBits(MathUtilities.parseDouble(text)),
                Double.doubleToRawLongBits(MathUtilities.parseDouble(chars)),
                Double.doubleToRawLongBits(MathUtilities.parseDouble(bytes)),
                Double.doubleToRawLongBits(
                        MathUtilities.parseDouble(("x" + text + "y").toCharArray(), 1, chars.length))};
        for (long parsed : doubles) {
            assertTrue(parsed == exact || parsed == jdk, shown + " parsed as " + Double.longBitsToDouble(parsed)
                    + ", exact " + Double.longBitsToDouble(exact) + ", this JDK " + Double.longBitsToDouble(jdk));
        }
        int[] floats = {Float.floatToRawIntBits(MathUtilities.parseFloat(text)),
                Float.floatToRawIntBits(MathUtilities.parseFloat(chars)),
                Float.floatToRawIntBits(MathUtilities.parseFloat(bytes))};
        for (int parsed : floats) {
            assertTrue(parsed == exactFloat || parsed == jdkFloat, shown + " parsed as "
                    + Float.intBitsToFloat(parsed) + ", exact " + Float.intBitsToFloat(exactFloat));
        }
    }

    @Test
    void anExponentOffsetByALongSignificandIsNotReadAsAnOrdinaryNumber() {
        String hugeWrittenSmall = "0." + zeros(1005) + "12345e10240";          // 1.2345E+9234
        String tinyWrittenLarge = "12345" + zeros(1019) + "e-10240";            // 1.2345E-9217
        assertEquals(Double.POSITIVE_INFINITY, new BigDecimal(hugeWrittenSmall).doubleValue());
        assertEquals(0.0, new BigDecimal(tinyWrittenLarge).doubleValue());
        for (String text : new String[]{hugeWrittenSmall, "-" + hugeWrittenSmall, tinyWrittenLarge,
                "-" + tinyWrittenLarge, "0." + zeros(2017) + "123456789012e20000", "0." + zeros(1012) + "5e10240",
                "+0." + zeros(700) + "9e1024", "1" + zeros(700) + "e-1024", "0." + zeros(1005) + "12345e10240d",
                "  0." + zeros(1005) + "12345E+10240  ",
                "0." + zeros(1005) + "12345e99999999999999999999", "12345" + zeros(1019) + "e-99999999999999999999",
                "0." + zeros(300) + "1e308", "0." + zeros(300) + "1e309", "1" + zeros(300) + "e-300", "1e1024",
                "1e-1024",
                "0x0." + zeros(1000) + "1p40960", "0x1" + zeros(1000) + "p-40960", "0x0." + zeros(1000) + "1p4000",
                "0x1.8p1", "1e400", "1e-400", "4.9e-324", "1.7976931348623157e308"}) {
            assertParsesAsTheJdk(text);
        }

        // and through the method that picks a type: never the Double that the fast path made up
        assertEquals(new BigDecimal(hugeWrittenSmall), MathUtilities.parseToMinimalNumericType(hugeWrittenSmall));
        assertEquals(new BigDecimal(tinyWrittenLarge), MathUtilities.parseToMinimalNumericType(tinyWrittenLarge));
    }

    /** Where the vendored parser went wrong, at random: long significands, exponents of every size. */
    @Test
    void longSignificandsWithLargeExponentsParseAsTheJdkParsesThem() {
        Random random = new Random(7);
        for (int i = 0; i < 4_000; i++) {
            StringBuilder text = new StringBuilder();
            if (random.nextInt(4) == 0) {
                text.append('-');
            }
            int before = random.nextInt(3) == 0 ? 0 : random.nextInt(1_400);
            int after = random.nextInt(1_400);
            boolean leadingZeros = random.nextBoolean();
            for (int d = 0; d < Math.max(1, before); d++) {
                text.append(before == 0 || (leadingZeros && d < before - 3) ? '0' : (char) ('0' + random.nextInt(10)));
            }
            if (after > 0) {
                text.append('.');
                for (int d = 0; d < after; d++) {
                    text.append(leadingZeros && d < after - 6 ? '0' : (char) ('0' + random.nextInt(10)));
                }
            }
            int magnitude = random.nextInt(6);
            long exponent = (long) (random.nextDouble() * Math.pow(10, magnitude + 1));
            text.append(random.nextBoolean() ? 'e' : 'E')
                    .append(random.nextBoolean() ? "-" : random.nextBoolean() ? "+" : "")
                    .append(exponent);
            assertParsesAsTheJdk(text.toString());
        }
    }

    /** No digits is not a number: empty text was an index exception, and an empty slice was 0. */
    @Test
    void parseBigIntegerRefusesEmptyInputAsTheJdkDoes() {
        assertThrows(NumberFormatException.class, () -> new BigInteger(""));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger(""));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger(new char[0]));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger(new byte[0]));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger(new char[]{'7'}, 0, 0));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger(new byte[]{'7'}, 1, 0));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseBigInteger("-"));
        assertEquals(BigInteger.valueOf(7), MathUtilities.parseBigInteger(new char[]{'x', '7'}, 1, 1));
        assertEquals(new BigInteger("-12345678901234567890"), MathUtilities.parseBigInteger("-12345678901234567890"));
    }

    /** Zero is zero whatever exponent follows it; "0e400" already was. Past the int range it was an exception. */
    @Test
    void zeroWithAnExponentPastAnyRangeIsStillZero() {
        for (String text : new String[]{"0e99999999999", "0E+99999999999", "0.000e99999999999", "0e-99999999999",
                "0e400", "0.0e-400"}) {
            Number zero = MathUtilities.parseToMinimalNumericType(text);
            assertEquals(Double.class, zero.getClass(), text);
            assertEquals(0L, Double.doubleToRawLongBits((Double) zero), text);
        }
        Number negative = MathUtilities.parseToMinimalNumericType("-0e99999999999");
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits((Double) negative));

        // what is not zero, and what is not a number, is refused as before
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType("1e99999999999"));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType("0.001e-99999999999"));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType("0e99999999999x"));
        assertThrows(NumberFormatException.class, () -> MathUtilities.parseToMinimalNumericType("0ee99999999999"));
        assertTrue(MathUtilities.parseToMinimalNumericType("1e2147483647") instanceof BigDecimal);
    }
}
