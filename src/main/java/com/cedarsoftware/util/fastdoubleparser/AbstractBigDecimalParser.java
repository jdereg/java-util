/*
 * @(#)AbstractBigDecimalParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

abstract class AbstractBigDecimalParser extends com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser {

    /**
     * Threshold on the number of input characters for selecting the
     * algorithm optimised for few digits in the significand vs. the algorithm for many
     * digits in the significand.
     * <p>
     * Set this to {@link Integer#MAX_VALUE} if you only want to use
     * the algorithm optimised for few digits in the significand.
     * <p>
     * Set this to {@code 0} if you only want to use the algorithm for
     * long inputs.
     * <p>
     * Rationale for choosing a specific threshold value:
     * We speculate that we only need to use the algorithm for large inputs
     * if there is zero chance, that we can parse the input with the algorithm
     * for small inputs.
     * <pre>
     * optional significant sign = 1
     * 18 significant digits = 18
     * optional decimal point in significant = 1
     * optional exponent = 1
     * optional exponent sign = 1
     * 10 exponent digits = 10
     * </pre>
     */
    public static final int MANY_DIGITS_THRESHOLD = 1 + 18 + 1 + 1 + 1 + 10;
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


    protected final static long MAX_EXPONENT_NUMBER = Integer.MAX_VALUE;
    /**
     * See {@link JavaBigDecimalParser}.
     */
    protected final static int MAX_DIGITS_WITHOUT_LEADING_ZEROS = 646_456_993;

    protected static boolean hasManyDigits(int length) {
        return length >= MANY_DIGITS_THRESHOLD;
    }

    protected static void checkParsedBigDecimalBounds(boolean illegal, int index, int endIndex, int digitCount, long exponent) {
        if (illegal || index < endIndex) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }
        if (exponent <= Integer.MIN_VALUE || exponent > Integer.MAX_VALUE || digitCount > MAX_DIGITS_WITHOUT_LEADING_ZEROS) {
            throw new NumberFormatException(VALUE_EXCEEDS_LIMITS);
        }
    }
}
