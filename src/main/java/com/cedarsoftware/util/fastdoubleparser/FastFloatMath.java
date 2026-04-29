/*
 * @(#)FastFloatMath.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import static com.cedarsoftware.util.fastdoubleparser.FastDoubleMath.DOUBLE_MIN_EXPONENT_POWER_OF_TEN;
import static com.cedarsoftware.util.fastdoubleparser.FastDoubleMath.MANTISSA_64;
import static com.cedarsoftware.util.fastdoubleparser.FastIntegerMath.unsignedMultiplyHigh;

/**
 * This class complements {@link FastDoubleMath} with methods for
 * converting {@code FloatingPointLiteral} productions to floats.
 * <p>
 * See {@link JavaDoubleParser} for a description of
 * {@code FloatingPointLiteral}.
 */
final class FastFloatMath {
    /**
     * Bias used in the exponent of a float.
     */
    private static final int FLOAT_EXPONENT_BIAS = 127;
    /**
     * The number of bits in the significand, including the implicit bit.
     */
    private static final int FLOAT_SIGNIFICAND_WIDTH = 24;
    private final static int FLOAT_MIN_EXPONENT_POWER_OF_TEN = -45;
    private final static int FLOAT_MAX_EXPONENT_POWER_OF_TEN = 38;
    private final static int FLOAT_MIN_EXPONENT_POWER_OF_TWO = Float.MIN_EXPONENT;
    private final static int FLOAT_MAX_EXPONENT_POWER_OF_TWO = Float.MAX_EXPONENT;
    /**
     * Precomputed powers of ten from 10^0 to 10^10. These
     * can be represented exactly using the float type.
     */
    private static final float[] FLOAT_POWER_OF_TEN = {
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f};

    /**
     * Don't let anyone instantiate this class.
     */
    private FastFloatMath() {

    }

    static float tryDecFloatToFloatTruncated(boolean isNegative, long significand, int exponent,
                                             boolean isSignificandTruncated,
                                             int exponentOfTruncatedSignificand) {
        if (significand == 0) {
            return isNegative ? -0.0f : 0.0f;
        }

        final float result;
        if (isSignificandTruncated) {

            // We have too many digits. We may have to round up.
            // To know whether rounding up is needed, we may have to examine up to 768 digits.

            // There are cases, in which rounding has no effect.
            if (FLOAT_MIN_EXPONENT_POWER_OF_TEN <= exponentOfTruncatedSignificand
                    && exponentOfTruncatedSignificand <= FLOAT_MAX_EXPONENT_POWER_OF_TEN) {
                float withoutRounding = tryDecToFloatWithFastAlgorithm(isNegative, significand, exponentOfTruncatedSignificand);
                float roundedUp = tryDecToFloatWithFastAlgorithm(isNegative, significand + 1, exponentOfTruncatedSignificand);
                if (roundedUp == withoutRounding) {//Note: A NaN value is always != NaN
                    return withoutRounding;
                }
            }

            // We have to take a slow path.
            //return Double.parseDouble(str.toString());
            result = Float.NaN;


        } else if (FLOAT_MIN_EXPONENT_POWER_OF_TEN <= exponent && exponent <= FLOAT_MAX_EXPONENT_POWER_OF_TEN) {
            result = tryDecToFloatWithFastAlgorithm(isNegative, significand, exponent);
        } else {
            result = Float.NaN;
        }
        return result;
    }

    /**
     * Tries to compute {@code significand * 2^exponent} exactly using a fast
     * algorithm; and if {@code isNegative} is true, negate the result;
     * the significand can be truncated.
     *
     * @param isNegative                     true if the sign is negative
     * @param significand                    the significand (unsigned long, uint64)
     * @param exponent                       the exponent number (the power)
     * @param isSignificandTruncated         true if significand has been truncated
     * @param exponentOfTruncatedSignificand the exponent number of the truncated significand
     * @return the double value,
     * or {@link Double#NaN} if the fast path failed.
     */
    static float tryHexFloatToFloatTruncated(boolean isNegative, long significand, int exponent,
                                             boolean isSignificandTruncated,
                                             int exponentOfTruncatedSignificand) {
        int power = isSignificandTruncated ? exponentOfTruncatedSignificand : exponent;
        if (FLOAT_MIN_EXPONENT_POWER_OF_TWO <= power && power <= FLOAT_MAX_EXPONENT_POWER_OF_TWO) {
            // Convert the significand into a float.
            // The cast will round the significand if necessary.
            // The significand is an unsigned long, however the cast treats it like a signed number.
            // So, if the significand is negative, we have to add 1<<64 to the number.
            float d = (float) significand + (significand < 0 ? 0x1p64f : 0);

            // Scale the significand by the power.
            // This only works if power is within the supported range, so that
            // we do not underflow or overflow.
            d = fastScalb(d, power);
            return isNegative ? -d : d;
        } else {
            return Float.NaN;
        }
    }

    /**
     * Attempts to compute {@literal digits * 10^(power)} exactly;
     * and if "negative" is true, negate the result.
     * <p>
     * This function will only work in some cases, when it does not work it
     * returns null. This should work *most of the time* (like 99% of the time).
     * We assume that power is in the
     * [{@value FastDoubleMath#DOUBLE_MIN_EXPONENT_POWER_OF_TEN},
     * {@value FastDoubleMath#DOUBLE_MAX_EXPONENT_POWER_OF_TEN}]
     * interval: the caller is responsible for this check.
     * <p>
     * References:
     * <dl>
     *     <dt>Noble Mushtak, Daniel Lemire. (2023) Fast Number Parsing Without Fallback.</dt>
     *     <dd><a href="https://arxiv.org/pdf/2212.06644.pdf">arxiv.org</a></dd>
     * </dl>
     *
     * @param isNegative  whether the number is negative
     * @param significand uint64 the significand
     * @param power       int32 the exponent of the number
     * @return the computed double on success, {@link Double#NaN} on failure
     */
    static float tryDecToFloatWithFastAlgorithm(boolean isNegative, long significand, int power) {

        // we start with a fast path
        if (-10 <= power && power <= 10 && Long.compareUnsigned(significand, (1L << FLOAT_SIGNIFICAND_WIDTH) - 1L) <= 0) {
            // convert the integer into a float. This is lossless since
            // 0 <= i <= 2^24 - 1.
            float d = (float) significand;
            //
            // The general idea is as follows.
            // If 0 <= s < 2^24 and if 10^0 <= p <= 10^10 then
            // 1) Both s and p can be represented exactly as 32-bit floating-point values
            // 2) Because s and p can be represented exactly as floating-point values,
            // then s * p and s / p will produce correctly rounded values.
            //
            if (power < 0) {
                d = d / FLOAT_POWER_OF_TEN[-power];
            } else {
                d = d * FLOAT_POWER_OF_TEN[power];
            }
            return (isNegative) ? -d : d;
        }


        // The fast path has now failed, so we are falling back on the slower path.

        // We are going to need to do some 64-bit arithmetic to get a more precise product.
        // We use a table lookup approach.
        // It is safe because
        // power >= DOUBLE_MIN_EXPONENT_POWER_OF_TEN
        // and power <= DOUBLE_MAX_EXPONENT_POWER_OF_TEN
        // We recover the mantissa of the power, it has a leading 1. It is always
        // rounded down.
        long factorMantissa = MANTISSA_64[power - DOUBLE_MIN_EXPONENT_POWER_OF_TEN];


        // The exponent is 127 + 64 + power
        //     + floor(log(5**power)/log(2)).
        // The 127 is the exponent bias.
        // The 64 comes from the fact that we use a 64-bit word.
        //
        // Computing floor(log(5**power)/log(2)) could be
        // slow. Instead ,we use a fast function.
        //
        // For power in (-400,350), we have that
        // (((152170 + 65536) * power ) >> 16);
        // is equal to
        //  floor(log(5**power)/log(2)) + power when power >= 0
        // and it is equal to
        //  ceil(log(5**-power)/log(2)) + power when power < 0
        //
        //
        // The 65536 is (1<<16) and corresponds to
        // (65536 * power) >> 16 ---> power
        //
        // ((152170 * power ) >> 16) is equal to
        // floor(log(5**power)/log(2))
        //
        // Note that this is not magic: 152170/(1<<16) is
        // approximately equal to log(5)/log(2).
        // The 1<<16 value is a power of two; we could use a
        // larger power of 2 if we wanted to.
        //
        long exponent = (((152170L + 65536L) * power) >> 16) + FLOAT_EXPONENT_BIAS + 64;
        // We want the most significant bit of digits to be 1. Shift if needed.
        int lz = Long.numberOfLeadingZeros(significand);
        long shiftedSignificand = significand << lz;
        // We want the most significant 64 bits of the product. We know
        // this will be non-zero because the most significant bit of shiftedSignificand is 1.
        long upper = unsignedMultiplyHigh(shiftedSignificand, factorMantissa);

        // The computed 'product' is always sufficient.
        // Mathematical proof:
        // Noble Mushtak and Daniel Lemire, Fast Number Parsing Without Fallback.

        // The final mantissa should be 24 bits with a leading 1.
        // We shift it so that it occupies 25 bits with a leading 1.
        long upperbit = upper >>> 63;
        long mantissa = upper >>> (upperbit + 38);
        lz += (int) (1 ^ upperbit);
        // Here we have mantissa < (1<<25).
        //assert mantissa < (1<<25);

        // We have to round to even. The "to even" part
        // is only a problem when we are right in between two floating-point values
        // which we guard against.
        // If we have lots of trailing zeros, we may fall right between two
        // floating-point values.
        if (((upper & 0x3_FFFFF_FFFFL) == 0x3_FFFFF_FFFFL)
                || ((upper & 0x3_FFFFF_FFFFL) == 0) && (mantissa & 3) == 1) {
            // if mantissa & 1 == 1 we might need to round up.
            //
            // Scenarios:
            // 1. We are not in the middle. Then we should round up.
            //
            // 2. We are right in the middle. Whether we round up depends
            // on the last significant bit: if it is "one" then we round
            // up (round to even) otherwise, we do not.
            //
            // So if the last significant bit is 1, we can safely round up.
            // Hence, we only need to bail out if (mantissa & 3) == 1.
            // Otherwise, we may need more accuracy or analysis to determine whether
            // we are exactly between two floating-point numbers.
            // It can be triggered with 1e23. ??
            // Note: because the factor_mantissa and factor_mantissa_low are
            // almost always rounded down (except for small positive powers),
            // almost always should round up.
            return Float.NaN;
        }

        mantissa += 1;
        mantissa >>>= 1;

        // Here we have mantissa < (1<<24), unless there was an overflow
        if (mantissa >= (1L << FLOAT_SIGNIFICAND_WIDTH)) {
            // This will happen when parsing values such as 7.2057594037927933e+16 ??
            mantissa = (1L << (FLOAT_SIGNIFICAND_WIDTH - 1));
            lz--; // undo previous addition
        }

        mantissa &= ~(1L << (FLOAT_SIGNIFICAND_WIDTH - 1));


        long real_exponent = exponent - lz;
        // we have to check that real_exponent is in range, otherwise we bail out
        if ((real_exponent < 1) || (real_exponent > FLOAT_MAX_EXPONENT_POWER_OF_TWO + FLOAT_EXPONENT_BIAS)) {
            return Float.NaN;
        }

        int bits = (int) (mantissa | real_exponent << (FLOAT_SIGNIFICAND_WIDTH - 1)
                | (isNegative ? 1L << 31 : 0));
        return Float.intBitsToFloat(bits);
    }

    /**
     * This is a faster alternative to {@link Math#scalb(float, int)}.
     * <p>
     * This method only works if scaleFactor is within the range of {@link Float#MIN_EXPONENT}
     * through {@link Float#MAX_EXPONENT} (inclusive), so that we do not underflow or overflow.
     *
     * @param number      a double number
     * @param scaleFactor the scale factor
     * @return number × 2<sup>scaleFactor</sup>
     */
    static float fastScalb(float number, int scaleFactor) {
        return number * Float.intBitsToFloat((scaleFactor + FLOAT_EXPONENT_BIAS) << (FLOAT_SIGNIFICAND_WIDTH - 1));
    }
}
