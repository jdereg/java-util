package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the vendored Randelshofer FastDoubleParser produces output that
 * matches the JDK's reference parsers ({@code Double.parseDouble},
 * {@code Float.parseFloat}, {@code new BigDecimal(String)},
 * {@code new BigInteger(String)}) across a comprehensive value corpus.
 *
 * <p>Parity covers: ordinary values, scientific notation, hex floats,
 * subnormals/denormals, signed zeros, infinities, NaN, boundary doubles,
 * long-digit BigIntegers, and parity-of-exception on malformed input.</p>
 */
class FastDoubleParserParityTest {

    // ========== Double parity ==========

    private static Stream<String> doubleCorpus() {
        return Stream.of(
                "0", "1", "-1", "0.0", "-0.0", "+0.0",
                "3.14", "-3.14", "0.5", "-0.5",
                "1e10", "1e-10", "1E10", "1.5e308", "5e-324", "2.2250738585072014E-308",
                "1.7976931348623157E308", // Double.MAX_VALUE
                "4.9E-324", // Double.MIN_VALUE
                "2.2250738585072014E-308", // Double.MIN_NORMAL
                "Infinity", "-Infinity", "NaN", "+Infinity",
                "0x1.8p10", "-0x1.0p-1022", // hex floats
                "1234567890.0987654321",
                "9999999999999999999999999999.9999999999999999999",
                "0.1", "0.2", "0.3",
                "1e-100", "1e+100",
                "  3.14  ", // surrounding whitespace
                "1234567890123456789", // long integer-shaped
                "1.23456789012345678e-50"
        );
    }

    @ParameterizedTest
    @MethodSource("doubleCorpus")
    void parseDouble_charSequence_matchesJdk(String input) {
        double expected = Double.parseDouble(input.trim());
        double actual = JavaDoubleParser.parseDouble(input.trim());
        assertBitwiseEqual(expected, actual, input);
    }

    @ParameterizedTest
    @MethodSource("doubleCorpus")
    void parseDouble_charArray_matchesJdk(String input) {
        String trimmed = input.trim();
        double expected = Double.parseDouble(trimmed);
        char[] chars = trimmed.toCharArray();
        double actual = JavaDoubleParser.parseDouble(chars, 0, chars.length);
        assertBitwiseEqual(expected, actual, input);
    }

    @ParameterizedTest
    @MethodSource("doubleCorpus")
    void parseDouble_byteArray_matchesJdk(String input) {
        String trimmed = input.trim();
        double expected = Double.parseDouble(trimmed);
        byte[] bytes = trimmed.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        double actual = JavaDoubleParser.parseDouble(bytes, 0, bytes.length);
        assertBitwiseEqual(expected, actual, input);
    }

    private static void assertBitwiseEqual(double expected, double actual, String input) {
        // For NaN, Double.compare is the right comparison (== returns false even for two NaNs).
        long expBits = Double.doubleToRawLongBits(expected);
        long actBits = Double.doubleToRawLongBits(actual);
        assertEquals(expBits, actBits,
                () -> "double parity mismatch for input='" + input + "': expected="
                        + Long.toHexString(expBits) + " actual=" + Long.toHexString(actBits));
    }

    @Test
    void parseDouble_nullThrows() {
        assertThrows(NullPointerException.class, () -> JavaDoubleParser.parseDouble((CharSequence) null));
    }

    @Test
    void parseDouble_emptyThrows() {
        assertThrows(NumberFormatException.class, () -> JavaDoubleParser.parseDouble(""));
    }

    @Test
    void parseDouble_garbageThrows() {
        assertThrows(NumberFormatException.class, () -> JavaDoubleParser.parseDouble("not-a-number"));
    }

    // ========== Float parity ==========

    private static Stream<String> floatCorpus() {
        return Stream.of(
                "0", "1", "-1", "0.0", "-0.0",
                "3.14", "-3.14",
                "1e10", "1e-10",
                "3.4028235E38", // Float.MAX_VALUE
                "1.4E-45", // Float.MIN_VALUE (smallest positive denormal)
                "1.17549435E-38", // Float.MIN_NORMAL
                "Infinity", "-Infinity", "NaN",
                "0.1", "1234.5678"
        );
    }

    @ParameterizedTest
    @MethodSource("floatCorpus")
    void parseFloat_matchesJdk(String input) {
        float expected = Float.parseFloat(input);
        float actualSeq = JavaFloatParser.parseFloat(input);
        float actualArr = JavaFloatParser.parseFloat(input.toCharArray(), 0, input.length());
        int expBits = Float.floatToRawIntBits(expected);
        assertEquals(expBits, Float.floatToRawIntBits(actualSeq),
                () -> "float parity mismatch (seq) for '" + input + "'");
        assertEquals(expBits, Float.floatToRawIntBits(actualArr),
                () -> "float parity mismatch (arr) for '" + input + "'");
    }

    // ========== BigDecimal parity ==========

    private static Stream<String> bigDecimalCorpus() {
        return Stream.of(
                "0", "1", "-1", "1.0", "0.0",
                "3.14", "-3.14",
                "100000000000000000000000000000.000000000000001",
                "9999999999999999999999999999999999999999999.99999999999",
                "1E100", "1.5E-200",
                "0.0000000000000000000000000000000000000000001",
                "-12345.6789",
                "2147483647", "9223372036854775807"
        );
    }

    @ParameterizedTest
    @MethodSource("bigDecimalCorpus")
    void parseBigDecimal_matchesJdk(String input) {
        BigDecimal expected = new BigDecimal(input);
        BigDecimal actualSeq = JavaBigDecimalParser.parseBigDecimal(input);
        BigDecimal actualArr = JavaBigDecimalParser.parseBigDecimal(input.toCharArray(), 0, input.length());
        // BigDecimal equals compares value AND scale; compareTo compares only value.
        // The fast parser preserves the same scale as the JDK parser.
        assertEquals(0, expected.compareTo(actualSeq),
                () -> "BigDecimal value mismatch (seq) for '" + input + "'");
        assertEquals(0, expected.compareTo(actualArr),
                () -> "BigDecimal value mismatch (arr) for '" + input + "'");
        assertEquals(expected.scale(), actualSeq.scale(),
                () -> "BigDecimal scale mismatch (seq) for '" + input + "'");
    }

    // ========== BigInteger parity ==========

    private static Stream<String> bigIntegerCorpus() {
        return Stream.of(
                "0", "1", "-1", "100",
                "9223372036854775807", // Long.MAX_VALUE
                "-9223372036854775808", // Long.MIN_VALUE
                "12345678901234567890", // beyond long range
                // ~100-digit number — where fast parser should diverge from JDK perf
                "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345",
                // ~500-digit number
                buildLongDigitString(500)
        );
    }

    @ParameterizedTest
    @MethodSource("bigIntegerCorpus")
    void parseBigInteger_matchesJdk(String input) {
        BigInteger expected = new BigInteger(input);
        BigInteger actualSeq = JavaBigIntegerParser.parseBigInteger(input);
        BigInteger actualArr = JavaBigIntegerParser.parseBigInteger(input.toCharArray(), 0, input.length());
        assertEquals(expected, actualSeq, () -> "BigInteger seq mismatch for '" + input.substring(0, Math.min(40, input.length())) + "...'");
        assertEquals(expected, actualArr, () -> "BigInteger arr mismatch for '" + input.substring(0, Math.min(40, input.length())) + "...'");
    }

    private static String buildLongDigitString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + (i % 10)));
        }
        // Avoid leading zero — make first digit non-zero.
        if (sb.charAt(0) == '0') sb.setCharAt(0, '1');
        return sb.toString();
    }

    // ========== Sanity: round-trip via JDK toString ==========

    @Test
    void roundTrip_jdkToString_then_fastParse_preservesBits() {
        double[] sweep = {
                0.0, -0.0, 1.0, -1.0, Math.PI, Math.E,
                Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                1e-300, 1e300, 0.1, 0.2, 1.0 / 3.0
        };
        for (double d : sweep) {
            String s = Double.toString(d);
            double parsed = JavaDoubleParser.parseDouble(s);
            assertEquals(Double.doubleToRawLongBits(d), Double.doubleToRawLongBits(parsed),
                    () -> "round-trip failed for " + d + " -> '" + Double.toString(d) + "' -> " + parsed);
        }
    }
}
