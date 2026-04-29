/*
 * @(#)FftMultiplier.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;

import static com.cedarsoftware.util.fastdoubleparser.FastDoubleMath.fastScalb;
import static com.cedarsoftware.util.fastdoubleparser.FastDoubleSwar.fma;

/**
 * Provides methods for multiplying two {@link BigInteger}s using the
 * {@code FFT algorithm}.
 * <p>
 * This code is based on {@code bigint} by Timothy Buktu.
 * <p>
 * References:
 * <dl>
 * <dt>bigint, Copyright 2013 Timothy Buktu, 2-clause BSD License.<br>
 * Note: We only use portions from this project, that have been marked with 2-clause BSD License
 * in this file <a href="https://github.com/tbuktu/bigint/blob/617c8cd8a7c5e4fb4d919c6a4d11e2586107f029/LICENSE">LICENSE</a>.
 * </dt>
 * <dd><a href="https://github.com/tbuktu/bigint/tree/floatfft">github.com</a></dd>
 * </dl>
 */
final class FftMultiplier {

    public static final double COS_0_25 = Math.cos(0.25 * Math.PI);
    public static final double SIN_0_25 = Math.sin(0.25 * Math.PI);
    /**
     * The threshold value for using floating point FFT multiplication.
     * If the number of bits in each mag array is greater than the
     * Toom-Cook threshold, and the number of bits in at least one of
     * the mag arrays is greater than this threshold, then FFT
     * multiplication will be used.
     */
    private static final int FFT_THRESHOLD = 33220;
    /**
     * This constant limits {@code mag.length} of BigIntegers to the supported
     * range.
     */
    private static final int MAX_MAG_LENGTH = Integer.MAX_VALUE / Integer.SIZE + 1; // (1 << 26)
    /**
     * for FFTs of length up to 3*2^19
     */
    private static final int ROOTS3_CACHE_SIZE = 20;
    /**
     * for FFTs of length up to 2^19
     */
    private static final int ROOTS2_CACHE_SIZE = 20;
    /**
     * The threshold value for using 3-way Toom-Cook multiplication.
     */
    private static final int TOOM_COOK_THRESHOLD = 240 * 8;
    /**
     * Sets of complex roots of unity. The set at index k contains 2^k
     * elements representing all (2^(k+2))-th roots between 0 and pi/2.
     * Used for FFT multiplication.
     */
    private volatile static ComplexVector[] ROOTS2_CACHE = new ComplexVector[ROOTS2_CACHE_SIZE];
    /**
     * Sets of complex roots of unity. The set at index k contains 3*2^k
     * elements representing all (3*2^(k+2))-th roots between 0 and pi/2.
     * Used for FFT multiplication.
     */
    private volatile static ComplexVector[] ROOTS3_CACHE = new ComplexVector[ROOTS3_CACHE_SIZE];

    private static final ComplexVector ONE;
    static {
        ONE = new ComplexVector(1);
        ONE.set(0, 1.0, 0.0);
    }
    /**
     * Returns the maximum number of bits that one double precision number can fit without
     * causing the multiplication to be incorrect.
     *
     * @param bitLen length of this number in bits
     * @return the maximum number of bits
     */
    static int bitsPerFftPoint(int bitLen) {
        if (bitLen <= 19 * (1 << 9)) {
            return 19;
        }
        if (bitLen <= 18 * (1 << 10)) {
            return 18;
        }
        if (bitLen <= 17 * (1 << 12)) {
            return 17;
        }
        if (bitLen <= 16 * (1 << 14)) {
            return 16;
        }
        if (bitLen <= 15 * (1 << 16)) {
            return 15;
        }
        if (bitLen <= 14 * (1 << 18)) {
            return 14;
        }
        if (bitLen <= 13 * (1 << 20)) {
            return 13;
        }
        if (bitLen <= 12 * (1 << 21)) {
            return 12;
        }
        if (bitLen <= 11 * (1 << 23)) {
            return 11;
        }
        if (bitLen <= 10 * (1 << 25)) {
            return 10;
        }
        if (bitLen <= 9 * (1 << 27)) {
            return 9;
        }
        return 8;
    }

    /**
     * Returns n-th complex roots of unity for the angles 0..pi/2, suitable
     * for a transform of length n.
     * They are used as twiddle factors and as weights for the right-angle transform.
     * n must be 1 or an even number.
     */
    private static ComplexVector calculateRootsOfUnity(int n) {
        if (n == 1) {
            return ONE;
        }
        ComplexVector roots = new ComplexVector(n);
        roots.set(0, 1.0, 0.0);
        double cos = COS_0_25;
        double sin = SIN_0_25;
        roots.set(n / 2, cos, sin);
        double angleTerm = 0.5 * Math.PI / n;
        for (int i = 1; i < n / 2; i++) {
            double angle = angleTerm * i;
            cos = Math.cos(angle);
            sin = Math.sin(angle);
            roots.set(i, cos, sin);
            roots.set(n - i, sin, cos);
        }
        return roots;
    }

    private static ComplexVector calculateRootsOfUnity(int n, ComplexVector prev) {
        if (n == 1) {
            return ONE;
        }
        ComplexVector roots = new ComplexVector(n);
        roots.set(0, 1.0, 0.0);
        double cos = COS_0_25;
        double sin = SIN_0_25;
        roots.set(n / 2, cos, sin);

        double angleTerm = 0.5 * Math.PI / n;
        int ratio = n / prev.length;
        for (int i = 1, j = 1; j < n / 2; i++, j += ratio) {
            for (int k = 0; k < ratio - 1; k++) {
                int outIdx = j + k;
                double angle = angleTerm * outIdx;
                cos = Math.cos(angle);
                sin = Math.sin(angle);
                roots.set(outIdx, cos, sin);
                roots.set(n - outIdx, sin, cos);
            }
            cos = prev.real(i);
            sin = prev.imag(i);
            int outIdx = j + ratio - 1;
            roots.set(outIdx, cos, sin);
            roots.set(n - outIdx, sin, cos);
        }
        return roots;
    }

    /**
     * Performs an FFT of length 2^n on the vector {@code a}.
     * This is a decimation-in-frequency implementation.
     *
     * @param a     input and output, must be a power of two in size
     * @param roots an array that contains one set of roots at indices
     *              log2(a.length), log2(a.length)-2, log2(a.length)-4, ...
     *              Each roots[s] must contain 2^s roots of unity such that
     *              {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *              i.e., they must cover the first quadrant.
     */
    private static void fft(ComplexVector a, ComplexVector[] roots) {
        int n = a.length;
        int logN = 31 - Integer.numberOfLeadingZeros(n);
        MutableComplex a0 = new MutableComplex();
        MutableComplex a1 = new MutableComplex();
        MutableComplex a2 = new MutableComplex();
        MutableComplex a3 = new MutableComplex();

        // do two FFT stages at a time (radix-4)
        MutableComplex omega1 = new MutableComplex();
        MutableComplex omega2 = new MutableComplex();
        int s = logN;
        for (; s >= 2; s -= 2) {
            ComplexVector rootsS = roots[s - 2];
            int m = 1 << s;
            for (int i = 0; i < n; i += m) {
                for (int j = 0; j < m / 4; j++) {
                    omega1.set(rootsS, j);
                    // computing omega2 from omega1 is less accurate than Math.cos() and Math.sin(),
                    // but it is the same error we'd incur with radix-2, so we're not breaking the
                    // assumptions of the Percival paper.
                    omega1.squareInto(omega2);

                    int idx0 = i + j;
                    int idx1 = i + j + m / 4;
                    int idx2 = i + j + m / 2;
                    int idx3 = i + j + m * 3 / 4;

                    // radix-4 butterfly:
                    //   a[idx0] = (a[idx0] + a[idx1]      + a[idx2]      + a[idx3])      * w^0
                    //   a[idx1] = (a[idx0] + a[idx1]*(-i) + a[idx2]*(-1) + a[idx3]*i)    * w^1
                    //   a[idx2] = (a[idx0] + a[idx1]*(-1) + a[idx2]      + a[idx3]*(-1)) * w^2
                    //   a[idx3] = (a[idx0] + a[idx1]*i    + a[idx2]*(-1) + a[idx3]*(-i)) * w^3
                    // where w = omega1^(-1) = conjugate(omega1)
                    a.addInto(idx0, a, idx1, a0);
                    a0.add(a, idx2);
                    a0.add(a, idx3);

                    a.subtractTimesIInto(idx0, a, idx1, a1);
                    a1.subtract(a, idx2);
                    a1.addTimesI(a, idx3);
                    a1.multiplyConjugate(omega1);

                    a.subtractInto(idx0, a, idx1, a2);
                    a2.add(a, idx2);
                    a2.subtract(a, idx3);
                    a2.multiplyConjugate(omega2);

                    a.addTimesIInto(idx0, a, idx1, a3);
                    a3.subtract(a, idx2);
                    a3.subtractTimesI(a, idx3);
                    a3.multiply(omega1);   // Bernstein's trick: multiply by omega^(-1) instead of omega^3

                    a0.copyInto(a, idx0);
                    a1.copyInto(a, idx1);
                    a2.copyInto(a, idx2);
                    a3.copyInto(a, idx3);
                }
            }
        }

        // do one final radix-2 step if there is an odd number of stages
        if (s > 0) {
            for (int i = 0; i < n; i += 2) {
                // omega = 1

                //    a0 = a[i];
                //    a1 = a[i + IMAG];
                //    a[i] += a1;
                //    a[i + IMAG] = a0 - a1;
                a.copyInto(i, a0);
                a.copyInto(i + ComplexVector.IMAG, a1);
                a.add(i, a1);
                a0.subtractInto(a1, a, i + 1);
            }
        }
    }

    /**
     * Performs FFTs or IFFTs of size 3 on the vector {@code (a0[i], a1[i], a2[i])}
     * for each {@code i}. The output is placed back into {@code a0, a1, and a2}.
     *
     * @param a0    inputs / outputs for the first FFT coefficient
     * @param a1    inputs / outputs for the second FFT coefficient
     * @param a2    inputs / outputs for the third FFT coefficient
     * @param sign  1 for a forward FFT, -1 for an inverse FFT
     * @param scale 1 for a forward FFT, 1/3 for an inverse FFT
     */
    private static void fft3(ComplexVector a0, ComplexVector a1, ComplexVector a2, int sign, double scale) {
        double omegaImag = sign * -0.5 * Math.sqrt(3);   // imaginary part of omega for n=3: sin(sign*(-2)*pi*1/3)
        for (int i = 0; i < a0.length; i++) {
            double a0Real = a0.real(i) + a1.real(i) + a2.real(i);
            double a0Imag = a0.imag(i) + a1.imag(i) + a2.imag(i);
            double c = omegaImag * (a2.imag(i) - a1.imag(i));
            double d = omegaImag * (a1.real(i) - a2.real(i));
            double e = 0.5 * (a1.real(i) + a2.real(i));
            double f = 0.5 * (a1.imag(i) + a2.imag(i));
            double a1Real = a0.real(i) - e + c;
            double a1Imag = a0.imag(i) + d - f;
            double a2Real = a0.real(i) - e - c;
            double a2Imag = a0.imag(i) - d - f;
            a0.real(i, a0Real * scale);
            a0.imag(i, a0Imag * scale);
            a1.real(i, a1Real * scale);
            a1.imag(i, a1Imag * scale);
            a2.real(i, a2Real * scale);
            a2.imag(i, a2Imag * scale);
        }
    }

    /**
     * Performs an FFT of length 3*2^n on the vector {@code a}.
     * Uses the 4-step algorithm to decompose the 3*2^n FFT into 2^n FFTs of
     * length 3 and 3 FFTs of length 2^n.
     * See https://www.nas.nasa.gov/assets/pdf/techreports/1989/rnr-89-004.pdf
     *
     * @param a      input and output, must be 3*2^n in size for some n>=2
     * @param roots2 an array that contains one set of roots at indices
     *               log2(a.length/3), log2(a.length/3)-2, log2(a.length/3)-4, ...
     *               Each roots[s] must contain 2^s roots of unity such that
     *               {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *               i.e., they must cover the first quadrant.
     * @param roots3 must be the same length as {@code a} and contain roots of
     *               unity such that {@code roots[k] = e^(pi*k*i/(2*roots3.length))},
     *               i.e., they need to cover the first quadrant.
     */
    private static void fftMixedRadix(ComplexVector a, ComplexVector[] roots2, ComplexVector roots3) {
        int oneThird = a.length / 3;
        ComplexVector a0 = new ComplexVector(a, 0, oneThird);
        ComplexVector a1 = new ComplexVector(a, oneThird, oneThird * 2);
        ComplexVector a2 = new ComplexVector(a, oneThird * 2, a.length);

        // step 1: perform a.length/3 transforms of length 3
        fft3(a0, a1, a2, 1, 1);

        // step 2: multiply by roots of unity
        MutableComplex omega = new MutableComplex();
        for (int i = 0; i < a.length / 4; i++) {
            omega.set(roots3, i);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyConjugate(i, omega);
            a2.multiplyConjugate(i, omega);
            a2.multiplyConjugate(i, omega);
        }
        for (int i = a.length / 4; i < oneThird; i++) {
            omega.set(roots3, i - a.length / 4);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyConjugateTimesI(i, omega);
            a2.multiplyConjugateTimesI(i, omega);
            a2.multiplyConjugateTimesI(i, omega);
        }

        // step 3 is not needed

        // step 4: perform 3 transforms of length a.length/3
        fft(a0, roots2);
        fft(a1, roots2);
        fft(a2, roots2);
    }

    static BigInteger fromFftVector(ComplexVector fftVec, int signum, int bitsPerFftPoint) {
        assert bitsPerFftPoint <= 25 : bitsPerFftPoint + " does not fit into an int with slack";

        int fftLen = (int) Math.min(fftVec.length, ((long) MAX_MAG_LENGTH * 32) / bitsPerFftPoint + 1);
        int magLen = (int) (8 * ((long) fftLen * bitsPerFftPoint + 31) / 32);
        byte[] mag = new byte[magLen];
        int base = 1 << bitsPerFftPoint;
        int bitMask = base - 1;
        int bitPadding = 32 - bitsPerFftPoint;
        long carry = 0;
        int bitLength = mag.length * 8;
        int bitIdx = bitLength - bitsPerFftPoint;
        int magComponent = 0;
        int prevIdx = Math.min(Math.max(0, bitIdx >> 3), mag.length - 4);
        for (int part = 0; part <= 1; part++) {   // 0=real, 1=imaginary
            for (int fftIdx = 0; fftIdx < fftLen; fftIdx++) {
                long fftElem = Math.round(fftVec.part(fftIdx, part)) + carry;
                carry = fftElem >> bitsPerFftPoint;

                int idx = Math.min(Math.max(0, bitIdx >> 3), mag.length - 4);
                magComponent >>>= (prevIdx - idx) << 3;
                int shift = bitPadding - bitIdx + (idx << 3);
                magComponent |= (int) ((fftElem & bitMask) << shift);
                FastDoubleSwar.writeIntBE(mag, idx, magComponent);

                prevIdx = idx;
                bitIdx -= bitsPerFftPoint;
            }
        }
        return new BigInteger(signum, mag);
    }

    /**
     * Returns sets of complex roots of unity. For k=logN, logN-2, logN-4, ...,
     * the return value contains all k-th roots between 0 and pi/2.
     *
     * @param logN for a transform of length 2^logN
     */
    static ComplexVector[] getRootsOfUnity2(int logN) {
        ComplexVector[] roots = new ComplexVector[logN + 1];
        for (int i = logN % 2; i <= logN; i += 2) {
            if (i < ROOTS2_CACHE_SIZE) {
                if (ROOTS2_CACHE[i] == null) {
                    ROOTS2_CACHE[i] = getRootOfUnity(1, i, ROOTS2_CACHE);
                }
                roots[i] = ROOTS2_CACHE[i];
            } else {
                roots[i] = getRootOfUnity(1, i, ROOTS2_CACHE);
            }
        }
        return roots;
    }

    private static ComplexVector getRootOfUnity(int b, int e, ComplexVector[] roots) {
        int nearest = floorEntry(e, roots);
        return nearest >= 2
                ? calculateRootsOfUnity(b << e, roots[nearest])
                : calculateRootsOfUnity(b << e);
    }

    private static int floorEntry(int i, ComplexVector[] roots) {
        while (i >= 2 && roots[i] == null) { i--; }
        return i;
    }

    /**
     * Returns sets of complex roots of unity. For k=logN, logN-2, logN-4, ...,
     * the return value contains all k-th roots between 0 and pi/2.
     *
     * @param logN for a transform of length 3*2^logN
     */
    private static ComplexVector getRootsOfUnity3(int logN) {
        if (logN < ROOTS3_CACHE_SIZE) {
            if (ROOTS3_CACHE[logN] == null) {
                ROOTS3_CACHE[logN] = getRootOfUnity(3, logN, ROOTS3_CACHE);
            }
            return ROOTS3_CACHE[logN];
        } else {
            return getRootOfUnity(3, logN, ROOTS3_CACHE);
        }
    }

    /**
     * Performs an inverse FFT of length 2^n on the vector {@code a}.
     * This is a decimation-in-time implementation.
     *
     * @param a     input and output, must be a power of two in size
     * @param roots an array that contains one set of roots at indices
     *              log2(a.length), log2(a.length)-2, log2(a.length)-4, ...
     *              Each roots[s] must contain 2^s roots of unity such that
     *              {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *              i.e., they must cover the first quadrant.
     */
    private static void ifft(ComplexVector a, ComplexVector[] roots) {
        int n = a.length;
        int logN = 31 - Integer.numberOfLeadingZeros(n);
        MutableComplex a0 = new MutableComplex();
        MutableComplex a1 = new MutableComplex();
        MutableComplex a2 = new MutableComplex();
        MutableComplex a3 = new MutableComplex();
        MutableComplex b0 = new MutableComplex();
        MutableComplex b1 = new MutableComplex();
        MutableComplex b2 = new MutableComplex();
        MutableComplex b3 = new MutableComplex();

        int s = 1;
        // do one radix-2 step if there is an odd number of stages
        if (logN % 2 != 0) {
            for (int i = 0; i < n; i += 2) {
                // omega = 1
                a.copyInto(i + 1, a2);
                a.copyInto(i, a0);
                a.add(i, a2);
                a0.subtractInto(a2, a, i + 1);
            }
            s++;
        }

        // do the remaining stages two at a time (radix-4)
        MutableComplex omega1 = new MutableComplex();
        MutableComplex omega2 = new MutableComplex();
        for (; s <= logN; s += 2) {
            ComplexVector rootsS = roots[s - 1];
            int m = 1 << (s + 1);
            for (int i = 0; i < n; i += m) {
                for (int j = 0; j < m / 4; j++) {
                    omega1.set(rootsS, j);
                    // computing omega2 from omega1 is less accurate than Math.cos() and Math.sin(),
                    // but it is the same error we'd incur with radix-2, so we're not breaking the
                    // assumptions of the Percival paper.
                    omega1.squareInto(omega2);

                    int idx0 = i + j;
                    int idx1 = i + j + m / 4;
                    int idx2 = i + j + m / 2;
                    int idx3 = i + j + m * 3 / 4;

                    // radix-4 butterfly:
                    //   a[idx0] = a[idx0]*w^0 + a[idx1]*w^1      + a[idx2]*w^2      + a[idx3]*w^3
                    //   a[idx1] = a[idx0]*w^0 + a[idx1]*i*w^1    + a[idx2]*(-1)*w^2 + a[idx3]*(-i)*w^3
                    //   a[idx2] = a[idx0]*w^0 + a[idx1]*(-1)*w^1 + a[idx2]*w^2      + a[idx3]*(-1)*w^3
                    //   a[idx3] = a[idx0]*w^0 + a[idx1]*(-i)*w^1 + a[idx2]*(-1)*w^2 + a[idx3]*i*w^3
                    // where w = omega1
                    a.copyInto(idx0, a0);
                    a.multiplyInto(idx1, omega1, a1);
                    a.multiplyInto(idx2, omega2, a2);
                    a.multiplyConjugateInto(idx3, omega1, a3);   // Bernstein's trick: multiply by omega^(-1) instead of omega^3

                    a0.addInto(a1, b0);
                    b0.add(a2);
                    b0.add(a3);

                    a0.addTimesIInto(a1, b1);
                    b1.subtract(a2);
                    b1.subtractTimesI(a3);

                    a0.subtractInto(a1, b2);
                    b2.add(a2);
                    b2.subtract(a3);

                    a0.subtractTimesIInto(a1, b3);
                    b3.subtract(a2);
                    b3.addTimesI(a3);

                    b0.copyInto(a, idx0);
                    b1.copyInto(a, idx1);
                    b2.copyInto(a, idx2);
                    b3.copyInto(a, idx3);
                }
            }
        }

        // divide all vector elements by n
        for (int i = 0; i < n; i++) {
            a.timesTwoToThe(i, -logN);
        }
    }

    /**
     * Performs an inverse FFT of length 3*2^n on the vector {@code a}.
     * Uses the 4-step algorithm to decompose the 3*2^n FFT into 2^n FFTs of
     * length 3 and 3 FFTs of length 2^n.
     * See https://www.nas.nasa.gov/assets/pdf/techreports/1989/rnr-89-004.pdf
     *
     * @param a      input and output, must be 3*2^n in size for some n>=2
     * @param roots2 an array that contains one set of roots at indices
     *               log2(a.length/3), log2(a.length/3)-2, log2(a.length/3)-4, ...
     *               Each roots[s] must contain 2^s roots of unity such that
     *               {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *               i.e., they must cover the first quadrant.
     * @param roots3 must be the same length as {@code a} and contain roots of
     *               unity such that {@code roots[k] = e^(pi*k*i/(2*roots3.length))},
     *               i.e., they need to cover the first quadrant.
     */
    private static void ifftMixedRadix(ComplexVector a, ComplexVector[] roots2, ComplexVector roots3) {
        int oneThird = a.length / 3;
        ComplexVector a0 = new ComplexVector(a, 0, oneThird);
        ComplexVector a1 = new ComplexVector(a, oneThird, oneThird * 2);
        ComplexVector a2 = new ComplexVector(a, oneThird * 2, a.length);

        // step 1: perform 3 transforms of length a.length/3
        ifft(a0, roots2);
        ifft(a1, roots2);
        ifft(a2, roots2);

        // step 2: multiply by roots of unity
        MutableComplex omega = new MutableComplex();
        for (int i = 0; i < a.length / 4; i++) {
            omega.set(roots3, i);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiply(i, omega);
            a2.multiply(i, omega);
            a2.multiply(i, omega);
        }
        for (int i = a.length / 4; i < oneThird; i++) {
            omega.set(roots3, i - a.length / 4);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyByIAnd(i, omega);
            a2.multiplyByIAnd(i, omega);
            a2.multiplyByIAnd(i, omega);
        }

        // step 3 is not needed

        // step 4: perform a.length/3 transforms of length 3
        fft3(a0, a1, a2, -1, 1.0 / 3);
    }

    /**
     * Returns a BigInteger whose value is {@code (a * b)}.
     *
     * @param a value a
     * @param b value b
     * @return {@code this * val}
     * @implNote An implementation may offer better algorithmic
     * performance when {@code a == b}.
     */
    static BigInteger multiply(BigInteger a, BigInteger b) {
        assert a != null : "a==null";
        assert b != null : "b==null";

        if (b.signum() == 0 || a.signum() == 0) {
            return BigInteger.ZERO;
        }
        // Squaring is slightly faster than multiplication.
        // We check for identity here and not for equality, because an equality check of big integers is very expensive.
        if (b == a) {
            return square(b);
        }

        int xlen = a.bitLength();
        int ylen = b.bitLength();
        if ((long) xlen + ylen > 32L * MAX_MAG_LENGTH) {
            throw new ArithmeticException("BigInteger would overflow supported range");
        }

        if (xlen > TOOM_COOK_THRESHOLD
                && ylen > TOOM_COOK_THRESHOLD
                && (xlen > FFT_THRESHOLD || ylen > FFT_THRESHOLD)) {
            return multiplyFft(a, b);
        }
        return a.multiply(b);
    }

    /**
     * Multiplies two BigIntegers using a floating-point FFT.
     * <p>
     * Floating-point math is inaccurate; to ensure the output of the FFT and
     * IFFT rounds to the correct result for every input, the provably safe
     * FFT error bounds from "Rapid Multiplication Modulo The Sum And
     * Difference of Highly Composite Numbers" by Colin Percival, pg. 392
     * (<a href="https://www.daemonology.net/papers/fft.pdf">fft.pdf</a>) are used, the vector is
     * "balanced" before the FFT, and accurate twiddle factors are used.
     * <p>
     * This implementation incorporates several features compared to the
     * standard FFT algorithm
     * (<a href="https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm">Cooley Tukey FFT algorithm</a>):
     * <ul>
     * <li>It uses a variant called right-angle convolution which weights the
     *     vector before the transform. The benefit of the right-angle
     *     convolution is that when multiplying two numbers of length n, an
     *     FFT of length n suffices whereas a regular FFT needs length 2n.
     *     This is because the right-angle convolution places half of the
     *     result in the real part and the other half in the imaginary part.
     *     See: Discrete Weighted Transforms And Large-Integer Arithmetic by
     *     Richard Crandall and Barry Fagin.
     * <li>FFTs of length 3*2^n are supported in addition to 2^n.
     * <li>Radix-4 butterflies; see
     *     https://www.nxp.com/docs/en/application-note/AN3666.pdf
     * <li>Bernstein's conjugate twiddle trick for a small speed gain at the
     *     expense of (further) reordering the output of the FFT which is not
     *     a problem because it is reordered back in the IFFT.
     * <li>Roots of unity are cached
     * </ul>
     * FFT vectors are stored as arrays of primitive doubles (two array
     * elements are needed for representing one complex number). Storing them
     * as arrays of primitive doubles instead of as MutableComplex objects is
     * memory efficient,
     * but in some cases below ~10^6 decimal digits, it hurts speed because
     * it requires additional copying. Ideally this would be implemented using
     * value types when they become available.
     *
     * @param a value a
     * @param b value b
     * @return a*b
     */
    static BigInteger multiplyFft(BigInteger a, BigInteger b) {
        int signum = a.signum() * b.signum();
        byte[] aMag = (a.signum() < 0 ? a.negate() : a).toByteArray();
        byte[] bMag = (b.signum() < 0 ? b.negate() : b).toByteArray();
        int bitLen = Math.max(aMag.length, bMag.length) * 8;
        int bitsPerPoint = bitsPerFftPoint(bitLen);
        int fftLen = (bitLen + bitsPerPoint - 1) / bitsPerPoint + 1;   // +1 for a possible carry, see toFFTVector()
        int logFFTLen = 32 - Integer.numberOfLeadingZeros(fftLen - 1);

        // Use a 2^n or 3*2^n transform, whichever is shortest
        int fftLen2 = 1 << (logFFTLen);   // rounded to 2^n
        int fftLen3 = fftLen2 * 3 / 4;   // rounded to 3*2^n
        if (fftLen < fftLen3 && logFFTLen > 3) {
            ComplexVector[] roots2 = getRootsOfUnity2(logFFTLen - 2);   // roots for length fftLen/3 which is a power of two
            ComplexVector weights = getRootsOfUnity3(logFFTLen - 2);
            ComplexVector twiddles = getRootsOfUnity3(logFFTLen - 4);
            ComplexVector aVec = toFftVector(aMag, fftLen3, bitsPerPoint);
            aVec.applyWeights(weights);
            fftMixedRadix(aVec, roots2, twiddles);
            ComplexVector bVec = toFftVector(bMag, fftLen3, bitsPerPoint);
            bVec.applyWeights(weights);
            fftMixedRadix(bVec, roots2, twiddles);
            aVec.multiplyPointwise(bVec);
            ifftMixedRadix(aVec, roots2, twiddles);
            aVec.applyInverseWeights(weights);
            return fromFftVector(aVec, signum, bitsPerPoint);
        } else {
            ComplexVector[] roots = getRootsOfUnity2(logFFTLen);
            ComplexVector aVec = toFftVector(aMag, fftLen2, bitsPerPoint);
            aVec.applyWeights(roots[logFFTLen]);
            fft(aVec, roots);
            ComplexVector bVec = toFftVector(bMag, fftLen2, bitsPerPoint);
            bVec.applyWeights(roots[logFFTLen]);
            fft(bVec, roots);
            aVec.multiplyPointwise(bVec);
            ifft(aVec, roots);
            aVec.applyInverseWeights(roots[logFFTLen]);
            return fromFftVector(aVec, signum, bitsPerPoint);
        }
    }

    /**
     * Returns a BigInteger whose value is {@code (this<sup>2</sup>)}.
     *
     * @return {@code this<sup>2</sup>}
     */
    static BigInteger square(BigInteger a) {
        if (a.signum() == 0) {
            return BigInteger.ZERO;
        }
        return a.bitLength() < FFT_THRESHOLD ? a.multiply(a) : squareFft(a);
    }

    static BigInteger squareFft(BigInteger a) {
        byte[] mag = a.toByteArray();
        int bitLen = mag.length * 8;
        int bitsPerPoint = bitsPerFftPoint(bitLen);
        int fftLen = (bitLen + bitsPerPoint - 1) / bitsPerPoint + 1;   // +1 for a possible carry, see toFFTVector()
        int logFFTLen = 32 - Integer.numberOfLeadingZeros(fftLen - 1);

        // Use a 2^n or 3*2^n transform, whichever is shorter
        int fftLen2 = 1 << (logFFTLen);   // rounded to 2^n
        int fftLen3 = fftLen2 * 3 / 4;   // rounded to 3*2^n
        if (fftLen < fftLen3) {
            fftLen = fftLen3;
            ComplexVector vec = toFftVector(mag, fftLen, bitsPerPoint);
            ComplexVector[] roots2 = getRootsOfUnity2(logFFTLen - 2);   // roots for length fftLen/3 which is a power of two
            ComplexVector weights = getRootsOfUnity3(logFFTLen - 2);
            ComplexVector twiddles = getRootsOfUnity3(logFFTLen - 4);
            vec.applyWeights(weights);
            fftMixedRadix(vec, roots2, twiddles);
            vec.squarePointwise();
            ifftMixedRadix(vec, roots2, twiddles);
            vec.applyInverseWeights(weights);
            return fromFftVector(vec, 1, bitsPerPoint);
        } else {
            fftLen = fftLen2;
            ComplexVector vec = toFftVector(mag, fftLen, bitsPerPoint);
            ComplexVector[] roots = getRootsOfUnity2(logFFTLen);
            vec.applyWeights(roots[logFFTLen]);
            fft(vec, roots);
            vec.squarePointwise();
            ifft(vec, roots);
            vec.applyInverseWeights(roots[logFFTLen]);
            return fromFftVector(vec, 1, bitsPerPoint);
        }
    }

    /**
     * Converts this BigInteger into an array of complex numbers suitable for an FFT.
     * Populates the real parts and sets the imaginary parts to zero.
     */
    static ComplexVector toFftVector(byte[] mag, int fftLen, int bitsPerFftPoint) {
        assert bitsPerFftPoint <= 25 : bitsPerFftPoint + " does not fit into an int with slack";

        ComplexVector fftVec = new ComplexVector(fftLen);
        if (mag.length < 4) {
            byte[] paddedMag = new byte[4];
            System.arraycopy(mag, 0, paddedMag, 4 - mag.length, mag.length);
            mag = paddedMag;
        }

        // Read fftPoint bits from right (least significant) to left (most significant)
        int base = 1 << bitsPerFftPoint;
        int halfBase = base / 2;
        int bitMask = base - 1;
        int bitPadding = 32 - bitsPerFftPoint;
        int bitLength = mag.length * 8;
        int carry = 0;// when we subtract base from a digit, we need to carry one
        int fftIdx = 0;
        for (int bitIdx = bitLength - bitsPerFftPoint; bitIdx > -bitsPerFftPoint; bitIdx -= bitsPerFftPoint) {
            int idx = Math.min(Math.max(0, bitIdx >> 3), mag.length - 4);
            int shift = bitPadding - bitIdx + (idx << 3);
            int fftPoint = (FastDoubleSwar.readIntBE(mag, idx) >>> shift) & bitMask;

            // "balance" the output digits so -base/2 < digit < base/2
            fftPoint += carry;
            carry = (halfBase - fftPoint) >>> 31;// if fftPoint>halfBase then carry:=1, else carry:=0
            fftPoint -= base & (-carry);//if (carry != 0) then  fftPoint -= base;

            fftVec.real(fftIdx, fftPoint);
            fftIdx++;
        }
        // final carry
        if (carry > 0) {
            fftVec.real(fftIdx, carry);
        }

        return fftVec;
    }


    final static class ComplexVector {
        /**
         * A complex number in an FFT double[] vector occupies 2^1 array elements.
         */
        private final static int COMPLEX_SIZE_SHIFT = 1;
        final static int IMAG = 1;
        final static int REAL = 0;
        /**
         * This arrays contains complex numbers.
         * <p>
         * A complex number occupies 2 consecutive array elements:
         * the real part and then the imaginary part.
         */
        private final double[] a;
        /**
         * The number of complex numbers stored in this vector.
         */
        private final int length;
        /**
         * Offset to the real part of a complex number.
         */
        private final int offset;

        ComplexVector(int length) {
            this.a = new double[length << COMPLEX_SIZE_SHIFT];
            this.length = length;
            this.offset = 0;
        }

        /**
         * Creates a view on another vector.
         *
         * @param c    the other vector
         * @param from start index of the view
         * @param to   end index of the view
         */
        ComplexVector(ComplexVector c, int from, int to) {
            this.length = to - from;
            this.a = c.a;
            this.offset = from << 1;
        }

        void add(int idxa, MutableComplex c) {
            a[realIdx(idxa)] += c.real;
            a[imagIdx(idxa)] += c.imag;
        }

        void addInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] + c.real(idxc);
            destination.imag = a[imagIdx(idxa)] + c.imag(idxc);
        }

        void addTimesIInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] - c.imag(idxc);
            destination.imag = a[imagIdx(idxa)] + c.real(idxc);
        }

        /**
         * Multiplies the elements of an FFT vector by 1/weight.
         * Used for the right-angle convolution.
         */
        void applyInverseWeights(ComplexVector weights) {
            int offw = weights.offset;
            double[] w = weights.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += 2) {
                // the following code is the same as: this.multiplyConjugate(i, weights[i]);

                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                a[offa] = fma(real, w[offw + REAL], imag * w[offw + IMAG]);
                a[offa + IMAG] = fma(-real, w[offw + IMAG], imag * w[offw + REAL]);
                offw += 2;
            }
        }

        /**
         * Multiplies the elements of an FFT vector by weights.
         * Doing this makes a regular FFT convolution a right-angle convolution.
         */
        void applyWeights(ComplexVector weights) {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.multiply(i,weights,i);
            // We use the fact that all a.imag(i) = 0.0
            int offw = weights.offset;
            double[] w = weights.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += 2) {
                double real = a[offa + REAL];
                a[offa + REAL] = real * w[offw + REAL];
                a[offa + IMAG] = real * w[offw + IMAG];
                offw += 2;
            }
        }

        void copyInto(int idxa, MutableComplex destination) {
            destination.real = a[realIdx(idxa)];
            destination.imag = a[imagIdx(idxa)];
        }

        double imag(int idxa) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG];
        }

        void imag(int idxa, double value) {
            a[(idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG] = value;
        }

        private int imagIdx(int idxa) {
            return (idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG;
        }

        /**
         * Multiplies {@code a} by {@code c}.
         * Stores the result in a[idx] and a[idx+1].
         */
        void multiply(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(real, c.real, -imag * c.imag);
            a[ii] = fma(real, c.imag, +imag * c.real);
        }

        /**
         * Multiplies {@code a} by {@code c} and by {@code i}.
         * Stores the result in a[idx] and a[idx+1].
         */
        void multiplyByIAnd(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(-real, c.imag, -imag * c.real);
            a[ii] = fma(real, c.real, -imag * c.imag);
        }

        /**
         * Multiplies {@code a} by the conjugate of {@code c}.
         * Stores the result in a[i] and a[i+1].
         */
        void multiplyConjugate(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(real, c.real, +imag * c.imag);
            a[ii] = fma(-real, c.imag, +imag * c.real);
        }

        void multiplyConjugateInto(int idxa, MutableComplex c, MutableComplex destination) {
            double real = a[realIdx(idxa)];
            double imag = a[imagIdx(idxa)];
            destination.real = fma(real, c.real, +imag * c.imag);
            destination.imag = fma(-real, c.imag, +imag * c.real);
        }

        /**
         * Multiplies {@code (a[i].real,a[i+1].imaginary)} by the conjugate of {@code c}
         * and by {@code i}.
         * Stores the result in a[i] and a[i+1].
         */
        void multiplyConjugateTimesI(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(-real, c.imag, +imag * c.real);
            a[ii] = fma(-real, c.real, -imag * c.imag);
        }

        void multiplyInto(int idxa, MutableComplex c, MutableComplex destination) {
            double real = a[realIdx(idxa)];
            double imag = a[imagIdx(idxa)];
            destination.real = fma(real, c.real, -imag * c.imag);
            destination.imag = fma(real, c.imag, +imag * c.real);
        }

        void multiplyPointwise(ComplexVector cvec) {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.multiply(i,cvec,i);
            int offc = cvec.offset;
            double[] c = cvec.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += 2) {
                // The following code is the same as: this.multiply(i,cvec[i]);
                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                double creal = c[offc + REAL];
                double cimag = c[offc + IMAG];
                a[offa + REAL] = fma(real, creal, -imag * cimag);
                a[offa + IMAG] = fma(real, cimag, +imag * creal);
                offc += 2;
            }
        }

        double part(int idxa, int part) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + part];
        }

        double real(int idxa) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + offset];
        }

        void real(int idxa, double value) {
            a[(idxa << COMPLEX_SIZE_SHIFT) + offset] = value;
        }

        private int realIdx(int idxa) {
            return (idxa << COMPLEX_SIZE_SHIFT) + offset;
        }

        void set(int idxa, double real, double imag) {
            int idx = realIdx(idxa);
            a[idx] = real;
            a[idx + IMAG] = imag;
        }

        /**
         * The result is placed in the argument
         */
        void squarePointwise() {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.square(i);
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += 2) {
                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                a[offa + REAL] = fma(real, real, -imag * imag);
                a[offa + IMAG] = 2 * real * imag;
            }
        }

        void subtractInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] - c.real(idxc);
            destination.imag = a[imagIdx(idxa)] - c.imag(idxc);
        }

        void subtractTimesIInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] + c.imag(idxc);
            destination.imag = a[imagIdx(idxa)] - c.real(idxc);
        }

        void timesTwoToThe(int idxa, int n) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fastScalb(real, n);
            a[ii] = fastScalb(imag, n);
        }
    }

    final static class MutableComplex {
        double real, imag;

        MutableComplex() {
        }

        void add(MutableComplex c) {
            real += c.real;
            imag += c.imag;
        }

        void add(ComplexVector c, int idxc) {
            real += c.real(idxc);
            imag += c.imag(idxc);
        }

        /**
         * Adds c to this number and stores the result in destination.
         * Leaves this number unmodified.
         */
        void addInto(MutableComplex c, MutableComplex destination) {
            destination.real = real + c.real;
            destination.imag = imag + c.imag;
        }

        /**
         * Adds c*i to this number.
         */
        void addTimesI(MutableComplex c) {
            real -= c.imag;
            imag += c.real;
        }

        void addTimesI(ComplexVector c, int idxc) {
            real -= c.imag(idxc);
            imag += c.real(idxc);
        }

        /**
         * Adds c*i to this number and stores the result in destination.
         * Leaves this number unmodified.
         */
        void addTimesIInto(MutableComplex c, MutableComplex destination) {
            destination.real = real - c.imag;
            destination.imag = imag + c.real;
        }

        void copyInto(ComplexVector c, int idxc) {
            c.real(idxc, real);
            c.imag(idxc, imag);
        }

        void multiply(MutableComplex c) {
            double temp = real;
            real = fma(temp, c.real, -imag * c.imag);
            imag = fma(temp, c.imag, imag * c.real);
        }

        /**
         * multiplies this number by the conjugate of c.
         */
        void multiplyConjugate(MutableComplex c) {
            double temp = real;
            real = fma(temp, c.real, imag * c.imag);
            imag = fma(-temp, c.imag, imag * c.real);
        }

        void set(ComplexVector c, int idxc) {
            real = c.real(idxc);
            imag = c.imag(idxc);
        }

        void squareInto(MutableComplex destination) {
            destination.real = fma(real, real, -imag * imag);
            destination.imag = 2 * real * imag;
        }

        void subtract(MutableComplex c) {
            real -= c.real;
            imag -= c.imag;
        }

        void subtract(ComplexVector c, int idxc) {
            real -= c.real(idxc);
            imag -= c.imag(idxc);
        }

        void subtractInto(MutableComplex c, MutableComplex destination) {
            destination.real = real - c.real;
            destination.imag = imag - c.imag;
        }

        void subtractInto(MutableComplex c, ComplexVector destination, int idxd) {
            destination.real(idxd, real - c.real);
            destination.imag(idxd, imag - c.imag);
        }

        void subtractTimesI(MutableComplex c) {
            real += c.imag;
            imag -= c.real;
        }

        void subtractTimesI(ComplexVector c, int idxc) {
            real += c.imag(idxc);
            imag -= c.real(idxc);
        }

        void subtractTimesIInto(MutableComplex c, MutableComplex destination) {
            destination.real = real + c.imag;
            destination.imag = imag - c.real;
        }
    }
}
