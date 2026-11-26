/*
 * @(#)AbstractFloatValueParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

/**
 * Abstract base class for parsers that parse a {@code FloatingPointLiteral} from a
 * character sequence ({@code str}).
 * <p>
 * This is a C++ to Java port of Daniel Lemire's fast_double_parser.
 * <p>
 * References:
 * <dl>
 *     <dt>Daniel Lemire, fast_float number parsing library: 4x faster than strtod.
 *     <a href="https://github.com/fastfloat/fast_float/blob/dc88f6f882ac7eb8ec3765f633835cb76afa0ac2/LICENSE-MIT">MIT License</a>.</dt>
 *     <dd><a href="https://github.com/fastfloat/fast_float">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, Number Parsing at a Gigabyte per Second,
 *     Software: Practice and Experience 51 (8), 2021.
 *     arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</dt>
 *     <dd><a href="https://arxiv.org/pdf/2101.11408.pdf">arxiv.org</a></dd>
 * </dl>
 */
abstract class AbstractFloatValueParser extends AbstractNumberParser {
    /**
     * This is the maximal input length that a Java array can have.
     */
    public final static int MAX_INPUT_LENGTH = Integer.MAX_VALUE - 4;

    /**
     * This is the smallest non-negative number that has 19 decimal digits.
     */
    final static long MINIMAL_NINETEEN_DIGIT_INTEGER = 1000_00000_00000_00000L;

    /**
     * The decimal exponent of a double has a range of -324 to +308.
     * The hexadecimal exponent of a double has a range of -1022 to +1023.
     */
    final static int MAX_EXPONENT_NUMBER = 1024;

    /**
     * Where the digits of an exponent stop being added up: far below what a long holds, and far above anything
     * the number of digits in a significand could offset (an input is at most 2^31 characters).
     * <p>
     * java-util change (4.119.0). As vendored, the exponent stopped being read at {@link #MAX_EXPONENT_NUMBER},
     * 1024, on the reasoning that a double's exponent goes no further: {@code e10240} was taken as {@code e1024}.
     * But the exponent is then added to the position of the decimal point, and with about a thousand digits in
     * the significand that sum came back inside the double range: {@code 0.}, 1,005 zeros and {@code 12345e10240},
     * which is 1.2345E+9234 and so Infinity to {@code Double.parseDouble}, was returned as 1.2345E18.
     * The exponent is now added up in full, and the sum is clamped: outside the double range by any amount it
     * goes to the caller's fallback, {@code Double.parseDouble}.
     */
    final static long MAX_EXPONENT_DIGITS_VALUE = 1_000_000_000_000_000L;

    /** An exponent as an int: its own value within a billion either way, which is far outside any double. */
    static int clampExponent(long exponent) {
        return (int) Math.max(-1_000_000_000L, Math.min(1_000_000_000L, exponent));
    }


}
