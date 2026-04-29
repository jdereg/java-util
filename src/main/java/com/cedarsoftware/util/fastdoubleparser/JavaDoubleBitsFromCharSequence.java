/*
 * @(#)JavaDoubleBitsFromCharSequence.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

/**
 * Parses a {@code double} from a {@link CharSequence}.
 */
final class JavaDoubleBitsFromCharSequence extends AbstractJavaFloatingPointBitsFromCharSequence {

    /**
     * Creates a new instance.
     */
    public JavaDoubleBitsFromCharSequence() {

    }

    @Override
    long nan() {
        return Double.doubleToRawLongBits(Double.NaN);
    }

    @Override
    long negativeInfinity() {
        return Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY);
    }

    @Override
    long positiveInfinity() {
        return Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
    }

    @Override
    long valueOfFloatLiteral(CharSequence str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d)
                ? Double.parseDouble(str.subSequence(startIndex, endIndex).toString())
                : d);
    }

    @Override
    long valueOfHexLiteral(
            CharSequence str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryHexFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d)
                ? Double.parseDouble(str.subSequence(startIndex, endIndex).toString())
                : d);
    }
}