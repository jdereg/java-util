/*
 * @(#)JavaDoubleBitsFromByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.nio.charset.StandardCharsets;

/**
 * Parses a {@code double} from a {@code byte} array.
 */
final class JavaDoubleBitsFromByteArray extends AbstractJavaFloatingPointBitsFromByteArray {

    /**
     * Creates a new instance.
     */
    public JavaDoubleBitsFromByteArray() {

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
    long valueOfFloatLiteral(byte[] str, int startIndex, int endIndex, boolean isNegative,
                             long significand, int exponent, boolean isSignificandTruncated,
                             int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryDecFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d)
                // via Double.parseDouble
                ? Double.parseDouble(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1))

                // via BigDecimal
                // This only makes sense from JDK 21 onwards.
                // See fix for https://bugs.openjdk.org/browse/JDK-8205592
                // FIXME Only pass up to 764 significand digits to the BigDecimalParser
                // new JavaBigDecimalFromByteArray().valueOfBigDecimalString(str,integerPartIndex,decimalPointIndex,nonZeroFractionalPartIndex,exponentIndicatorIndex,isNegative,exponent).doubleValue()
                //? new JavaBigDecimalFromByteArray().parseBigDecimalString(str, startIndex, endIndex - startIndex).doubleValue()

                : d);
    }

    @Override
    long valueOfHexLiteral(
            byte[] str, int startIndex, int endIndex, boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand) {
        double d = FastDoubleMath.tryHexFloatToDoubleTruncated(isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
        return Double.doubleToRawLongBits(Double.isNaN(d) ? Double.parseDouble(new String(str, startIndex, endIndex - startIndex, StandardCharsets.ISO_8859_1)) : d);
    }
}