/*
 * @(#)ParseDigitsTaskByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;
import java.util.Map;

import static com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser.SYNTAX_ERROR;
import static com.cedarsoftware.util.fastdoubleparser.FastIntegerMath.splitFloor16;

/**
 * Parses digits.
 */
final class ParseDigitsTaskByteArray {
    /**
     * Don't let anyone instantiate this class.
     */
    private ParseDigitsTaskByteArray() {
    }


    /**
     * Parses digits in quadratic time O(N<sup>2</sup>).
     */
    static BigInteger parseDigitsIterative(byte[] str, int from, int to) {
        assert str != null : "str==null";

        int numDigits = to - from;

        BigSignificand bigSignificand = new BigSignificand(FastIntegerMath.estimateNumBits(numDigits));
        int preroll = from + (numDigits & 7);
        int value = FastDoubleSwar.tryToParseUpTo7Digits(str, from, preroll);
        boolean success = value >= 0;
        bigSignificand.add(value);
        for (from = preroll; from < to; from += 8) {
            int addend = FastDoubleSwar.tryToParseEightDigits(str, from);
            success &= addend >= 0;
            bigSignificand.fma(100_000_000, addend);
        }
        if (!success) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }
        return bigSignificand.toBigInteger();
    }

    /**
     * Parses digits in O(N log N (log log N)) time.
     * <p>
     * A conventional recursive algorithm would require O(N<sup>1.5</sup>).
     * We achieve better performance by performing multiplications of long bit sequences
     * in the frequency domain using {@link FftMultiplier}.
     */
    static BigInteger parseDigitsRecursive(byte[] str, int from, int to, Map<Integer, BigInteger> powersOfTen, int recursionThreshold) {
        assert str != null : "str==null";
        assert powersOfTen != null : "powersOfTen==null";

        int numDigits = to - from;

        // Base case: Short sequences can be parsed iteratively.
        if (numDigits <= recursionThreshold) {
            return parseDigitsIterative(str, from, to);
        }

        // Recursion case: Split large sequences up into two parts. The lower part is a multiple of 16 digits.
        int mid = splitFloor16(from, to);
        BigInteger high = parseDigitsRecursive(str, from, mid, powersOfTen, recursionThreshold);
        BigInteger low = parseDigitsRecursive(str, mid, to, powersOfTen, recursionThreshold);

        //high = high.multiply(powersOfTen.get(to - mid));
        high = FftMultiplier.multiply(high, powersOfTen.get(to - mid));
        return low.add(high);
    }
}
