/*
 * @(#)BigSignificand.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * A mutable significand with a fixed number of bits.
 */
final class BigSignificand {
    private static final long LONG_MASK = 0xffffffffL;
    private final int numInts;
    private final int[] x;
    private int firstNonZeroInt;

    public BigSignificand(long numBits) {
        if (numBits <= 0 || numBits >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("numBits=" + numBits);
        }
        int numLongs = (int) ((numBits + 63) >>> 6) + 1;
        numInts = numLongs << 1;
        x = new int[numInts];
        firstNonZeroInt = numInts;
    }

    /**
     * Adds the specified value to the significand in place.
     *
     * @param value the addend, must be a non-negative value
     * @throws ArrayIndexOutOfBoundsException on overflow
     */
    public void add(int value) {
        if (value == 0) {
            return;
        }
        long carry = value & LONG_MASK;
        int i = numInts - 1;
        for (; carry != 0; i--) {
            long sum = (x(i) & LONG_MASK) + carry;
            x(i, (int) sum);
            carry = sum >>> 32;
        }
        firstNonZeroInt = Math.min(firstNonZeroInt, i + 1);
    }

    /**
     * Multiplies the significand with the specified factor in place,
     * and then adds the specified addend to it (also in place).
     *
     * @param factor the multiplication factor, must be a non-negative value
     * @param addend the addend, must be a non-negative value
     * @throws ArrayIndexOutOfBoundsException on overflow
     */
    public void fma(int factor, int addend) {
        long factorL = factor & LONG_MASK;
        long carry = addend;
        int i = numInts - 1;
        for (; i >= firstNonZeroInt; i--) {
            long product = factorL * (x(i) & LONG_MASK) + carry;
            x(i, (int) product);
            carry = product >>> 32;
        }
        if (carry != 0) {
            x(i, (int) carry);
            firstNonZeroInt = i;
        }
    }

    public BigInteger toBigInteger() {
        byte[] bytes = new byte[x.length << 2];
        IntBuffer buf = ByteBuffer.wrap(bytes).asIntBuffer();
        for (int i = 0; i < x.length; i++) {
            buf.put(i, x[i]);
        }
        return new BigInteger(bytes);
    }

    private void x(int i, int value) {
        x[i] = value;
    }

    private int x(int i) {
        return x[i];
    }
}
