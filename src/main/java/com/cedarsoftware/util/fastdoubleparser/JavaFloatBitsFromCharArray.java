/*
 * @(#)JavaFloatBitsFromCharArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

/**
 * Parses a {@code float} from a {@code char} array.
 */
final class JavaFloatBitsFromCharArray extends AbstractJavaFloatingPointBitsFromCharArray {


    /**
     * Creates a new instance.
     */
    public JavaFloatBitsFromCharArray() {

    }

    @Override
    long nan() {
        return Float.floatToRawIntBits(Float.NaN);
    }

    @Override
    long negativeInfinity() {
        return Float.floatToRawIntBits(Float.NEGATIVE_INFINITY);
    }

    @Override
    long positiveInfinity() {
        return Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
    }

    @Override
    long valueOfFloatLiteral(char[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        float result = FastFloatMath.tryDecFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.isNaN(result) ? (long) Float.floatToRawIntBits(Float.parseFloat(new String(str, startIndex, endIndex - startIndex))) : Float.floatToRawIntBits(result);
    }

    @Override
    long valueOfHexLiteral(
            char[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        float d = FastFloatMath.tryHexFloatToFloatTruncated(isNegative, significand, exponent, isSignificandTruncated, exponentOfTruncatedSignificand);
        return Float.floatToRawIntBits(Float.isNaN(d) ? Float.parseFloat(new String(str, startIndex, endIndex - startIndex)) : d);
    }
}