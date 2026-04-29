/*
 * @(#)AbstractBigIntegerParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

abstract class AbstractBigIntegerParser extends AbstractNumberParser {

    /**
     * The resulting value must fit into {@code 2^31 - 1} bits.
     * The decimal representation of {@code 2^31 - 1} bits has 646,456,993 digits.
     */
    private static final int MAX_DECIMAL_DIGITS = 646_456_993;

    /**
     * The resulting value must fit into {@code 2^31 - 1} bits.
     * The hexadecimal representation of {@code 2^31 - 1} bits has 536,870,912 digits.
     */
    private static final int MAX_HEX_DIGITS = 536_870_912;
    /**
     * Threshold on the number of digits for selecting the
     * recursive algorithm instead of the iterative algorithm.
     * <p>
     * Set this to {@link Integer#MAX_VALUE} if you only want to use the
     * iterative algorithm.
     * <p>
     * Set this to {@code 0} if you only want to use the recursive algorithm.
     * <p>
     * Rationale for choosing a specific threshold value:
     * The iterative algorithm has a smaller constant overhead than the
     * recursive algorithm. We speculate that we break even somewhere at twice
     * the threshold value.
     */
    static final int RECURSION_THRESHOLD = 400;

    protected static boolean hasManyDigits(int length) {
        return length > 18;
    }

    protected static void checkHexBigIntegerBounds(int numDigits) {
        if (numDigits > MAX_HEX_DIGITS) {
            throw new NumberFormatException(AbstractNumberParser.VALUE_EXCEEDS_LIMITS);
        }
    }

    protected static void checkDecBigIntegerBounds(int numDigits) {
        if (numDigits > MAX_DECIMAL_DIGITS) {
            throw new NumberFormatException(AbstractNumberParser.VALUE_EXCEEDS_LIMITS);
        }
    }
}
