/*
 * @(#)FastIntegerMath.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class FastIntegerMath {
    public static final BigInteger FIVE = BigInteger.valueOf(5);
    final static BigInteger TEN_POW_16 = BigInteger.valueOf(10_000_000_000_000_000L);
    final static BigInteger FIVE_POW_16 = BigInteger.valueOf(152_587_890_625L);
    private final static BigInteger[] SMALL_POWERS_OF_TEN = new BigInteger[]{
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(100L),
            BigInteger.valueOf(1_000L),
            BigInteger.valueOf(10_000L),
            BigInteger.valueOf(100_000L),
            BigInteger.valueOf(1_000_000L),
            BigInteger.valueOf(10_000_000L),
            BigInteger.valueOf(100_000_000L),
            BigInteger.valueOf(1_000_000_000L),
            BigInteger.valueOf(10_000_000_000L),
            BigInteger.valueOf(100_000_000_000L),
            BigInteger.valueOf(1_000_000_000_000L),
            BigInteger.valueOf(10_000_000_000_000L),
            BigInteger.valueOf(100_000_000_000_000L),
            BigInteger.valueOf(1_000_000_000_000_000L)
    };

    /**
     * Don't let anyone instantiate this class.
     */
    private FastIntegerMath() {

    }

    /**
     * Computes the n-th power of ten.
     *
     * @param powersOfTen A map with pre-computed powers of ten
     * @param n           the power
     * @return the computed power of ten
     */
    static BigInteger computePowerOfTen(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        if (n < SMALL_POWERS_OF_TEN.length) {
            return SMALL_POWERS_OF_TEN[n];
        }
        if (powersOfTen != null) {
            Map.Entry<Integer, BigInteger> floorEntry = powersOfTen.floorEntry(n);
            Integer floorN = floorEntry.getKey();
            if (floorN == n) {
                return floorEntry.getValue();
            } else {
                return FftMultiplier.multiply(floorEntry.getValue(), computePowerOfTen(powersOfTen, n - floorN));
            }
        }
        return FIVE.pow(n).shiftLeft(n);
    }

    /**
     * Computes 10<sup>n&~15</sup>.
     */
    static BigInteger computeTenRaisedByNFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int n) {
        n = n & ~15;
        Map.Entry<Integer, BigInteger> floorEntry = powersOfTen.floorEntry(n);
        int floorPower = floorEntry.getKey();
        BigInteger floorValue = floorEntry.getValue();
        if (floorPower == n) {
            return floorValue;
        }
        int diff = n - floorPower;
        BigInteger diffValue = powersOfTen.get(diff);
        if (diffValue == null) {
            diffValue = computeTenRaisedByNFloor16Recursive(powersOfTen, diff);
            powersOfTen.put(diff, diffValue);
        }
        return FftMultiplier.multiply(floorValue, diffValue);
    }

    static NavigableMap<Integer, BigInteger> createPowersOfTenFloor16Map() {
        NavigableMap<Integer, BigInteger> powersOfTen;
        powersOfTen = new TreeMap<>();
        powersOfTen.put(0, BigInteger.ONE);
        powersOfTen.put(16, TEN_POW_16);
        return powersOfTen;
    }

    public static long estimateNumBits(long numDecimalDigits) {
        // For the decimal number 10 we need log_2(10) = 3.3219 bits.
        // The following formula uses 3.322 * 1024 = 3401.8 rounded up
        // and adds 1, so that we overestimate but never underestimate
        // the number of bits.
        return (((numDecimalDigits * 3402L) >>> 10) + 1);
    }

    /**
     * Fills a map with powers of 10 floor 16.
     *
     * @param from the start index of the character sequence that contains the digits
     * @param to   the end index of the character sequence that contains the digits
     * @return the filled map
     */
    static NavigableMap<Integer, BigInteger> fillPowersOf10Floor16(int from, int to) {
        // Fill the map with powers of 5
        NavigableMap<Integer, BigInteger> powers = new TreeMap<>();
        powers.put(0, BigInteger.valueOf(5));
        powers.put(16, FIVE_POW_16);
        fillPowersOfNFloor16Recursive(powers, from, to);

        // Shift map entries to the left to obtain powers of ten
        for (Iterator<Map.Entry<Integer, BigInteger>> iterator = powers.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, BigInteger> e = iterator.next();
            e.setValue(e.getValue().shiftLeft(e.getKey()));
        }

        return powers;
    }

    static void fillPowersOfNFloor16Recursive(NavigableMap<Integer, BigInteger> powersOfTen, int from, int to) {
        int numDigits = to - from;
        // base case:
        if (numDigits <= 18) {
            return;
        }
        // recursion case:
        int mid = splitFloor16(from, to);
        int n = to - mid;
        if (!powersOfTen.containsKey(n)) {
            fillPowersOfNFloor16Recursive(powersOfTen, from, mid);
            fillPowersOfNFloor16Recursive(powersOfTen, mid, to);
            powersOfTen.put(n, computeTenRaisedByNFloor16Recursive(powersOfTen, n));
        }
    }

    static long unsignedMultiplyHigh(long x, long y) {//before Java 18
        long x0 = x & 0xffffffffL, x1 = x >>> 32;
        long y0 = y & 0xffffffffL, y1 = y >>> 32;
        long p11 = x1 * y1, p01 = x0 * y1;
        long p10 = x1 * y0, p00 = x0 * y0;

        // 64-bit product + two 32-bit values
        long middle = p10 + (p00 >>> 32) + (p01 & 0xffffffffL);

        // 64-bit product + two 32-bit values
        return p11 + (middle >>> 32) + (p01 >>> 32);
    }

    /**
     * Finds middle of range with upper range half rounded up to multiple of 16.
     *
     * @param from start of range (inclusive)
     * @param to   end of range (exclusive)
     * @return middle of range with upper range half rounded up to multiple of 16
     */
    static int splitFloor16(int from, int to) {
        // divide length by 2 as we want the middle, round up range half to multiples of 16
        int range = (((to - from + 31) >>> 5) << 4);
        return to - range;
    }

}
