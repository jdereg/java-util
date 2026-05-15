package com.cedarsoftware.util.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import com.cedarsoftware.util.SystemUtilities;

/**
 * <h2>Internal API — not for external use.</h2>
 *
 * Range-based array operations that dispatch to JDK 9+ {@link Arrays} intrinsics
 * (SIMD-vectorized on supported HW) when the runtime JVM supports them, and fall
 * back to hand-rolled loops on JDK 8. {@code java-util}'s source/target is JDK 1.8,
 * so the JDK 9+ {@code Arrays.equals(arr, int, int, arr, int, int)} / {@code mismatch}
 * / {@code compare} signatures can't be referenced at compile time — but the same
 * library is overwhelmingly run on modern JVMs in production. This helper bridges
 * the gap with one-time reflective resolution at class load.
 *
 * <h3>Dispatch mechanics</h3>
 * <ol>
 *   <li>At class load, {@link SystemUtilities#isJavaVersionAtLeast(int, int) SystemUtilities.isJavaVersionAtLeast(9, 0)}
 *       is queried once. On JDK 8 we short-circuit to {@code null} handles and
 *       skip the reflection cost entirely.</li>
 *   <li>On JDK 9+, each operation is resolved via {@link MethodHandles#publicLookup()}
 *       and cached in a {@code static final} {@link MethodHandle}. {@code invokeExact}
 *       on a {@code static final MH} is recognised by HotSpot and inlined to direct
 *       intrinsic dispatch in steady state.</li>
 *   <li>Per-call cost is one static-field read + null-check + (on JDK 9+)
 *       {@code MH.invokeExact}. No per-call version checks.</li>
 * </ol>
 *
 * <h3>Exposed operations</h3>
 * <ul>
 *   <li>{@link #equalsRange(char[], int, int, char[], int, int) equalsRange} — char and byte variants</li>
 *   <li>{@link #mismatchRange(char[], int, int, char[], int, int) mismatchRange} — char and byte variants</li>
 *   <li>{@link #compareRange(char[], int, int, char[], int, int) compareRange} — char and byte variants</li>
 * </ul>
 * (Note: {@code System.arraycopy} is already a HotSpot intrinsic on JDK 8+;
 * there's no slower portable fallback to dispatch to, so it's not exposed here.
 * Use {@code System.arraycopy} directly.)
 *
 * <h3>Visibility</h3>
 * Exposed to {@code com.cedarsoftware:json-io} via a qualified JPMS export
 * ({@code exports com.cedarsoftware.util.internal to com.cedarsoftware.io}).
 * Signatures and semantics may change without notice across minor releases.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 */
public final class VectorizedArrays {

    private static final MethodHandle MH_EQUALS_CHAR;
    private static final MethodHandle MH_EQUALS_BYTE;
    private static final MethodHandle MH_MISMATCH_CHAR;
    private static final MethodHandle MH_MISMATCH_BYTE;
    private static final MethodHandle MH_COMPARE_CHAR;
    private static final MethodHandle MH_COMPARE_BYTE;

    static {
        boolean jdk9Plus = SystemUtilities.isJavaVersionAtLeast(9, 0);
        MH_EQUALS_CHAR   = jdk9Plus ? findRange("equals",   boolean.class, char[].class) : null;
        MH_EQUALS_BYTE   = jdk9Plus ? findRange("equals",   boolean.class, byte[].class) : null;
        MH_MISMATCH_CHAR = jdk9Plus ? findRange("mismatch", int.class,     char[].class) : null;
        MH_MISMATCH_BYTE = jdk9Plus ? findRange("mismatch", int.class,     byte[].class) : null;
        MH_COMPARE_CHAR  = jdk9Plus ? findRange("compare",  int.class,     char[].class) : null;
        MH_COMPARE_BYTE  = jdk9Plus ? findRange("compare",  int.class,     byte[].class) : null;
    }

    private static MethodHandle findRange(String name, Class<?> ret, Class<?> arr) {
        try {
            return MethodHandles.publicLookup().findStatic(Arrays.class, name,
                    MethodType.methodType(ret, arr, int.class, int.class, arr, int.class, int.class));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private VectorizedArrays() {
        // no instances
    }

    // -------------------------------------------------------------------
    // equalsRange — element-wise equality over [fromIndex, toIndex) ranges
    // -------------------------------------------------------------------

    /**
     * Equivalent to JDK 9+'s {@code Arrays.equals(a, aFrom, aTo, b, bFrom, bTo)} —
     * returns {@code true} iff the two ranges have the same length and contain
     * element-wise equal chars. SIMD-vectorized on JDK 9+; manual loop on JDK 8.
     */
    public static boolean equalsRange(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_EQUALS_CHAR;
        if (mh != null) {
            try {
                return (boolean) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through to loop
            }
        }
        return equalsRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    /** Byte variant of {@link #equalsRange(char[], int, int, char[], int, int)}. */
    public static boolean equalsRange(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_EQUALS_BYTE;
        if (mh != null) {
            try {
                return (boolean) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through to loop
            }
        }
        return equalsRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    private static boolean equalsRangeLoop(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom;
        if (aLen != bTo - bFrom) return false;
        for (int i = 0; i < aLen; i++) {
            if (a[aFrom + i] != b[bFrom + i]) return false;
        }
        return true;
    }

    private static boolean equalsRangeLoop(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom;
        if (aLen != bTo - bFrom) return false;
        for (int i = 0; i < aLen; i++) {
            if (a[aFrom + i] != b[bFrom + i]) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------
    // mismatchRange — index of first differing element, or -1 if equal
    // -------------------------------------------------------------------

    /**
     * Equivalent to JDK 9+'s {@code Arrays.mismatch(a, aFrom, aTo, b, bFrom, bTo)}.
     * Returns the relative index of the first mismatching element (i.e. {@code 0}
     * for the first element in each range), or {@code -1} if the ranges are equal
     * over their common prefix and have the same length. If the ranges have
     * different lengths and are equal over the common prefix, returns the length
     * of the shorter range.
     */
    public static int mismatchRange(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_MISMATCH_CHAR;
        if (mh != null) {
            try {
                return (int) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return mismatchRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    /** Byte variant of {@link #mismatchRange(char[], int, int, char[], int, int)}. */
    public static int mismatchRange(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_MISMATCH_BYTE;
        if (mh != null) {
            try {
                return (int) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return mismatchRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    private static int mismatchRangeLoop(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom, bLen = bTo - bFrom;
        int common = Math.min(aLen, bLen);
        for (int i = 0; i < common; i++) {
            if (a[aFrom + i] != b[bFrom + i]) return i;
        }
        return aLen == bLen ? -1 : common;
    }

    private static int mismatchRangeLoop(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom, bLen = bTo - bFrom;
        int common = Math.min(aLen, bLen);
        for (int i = 0; i < common; i++) {
            if (a[aFrom + i] != b[bFrom + i]) return i;
        }
        return aLen == bLen ? -1 : common;
    }

    // -------------------------------------------------------------------
    // compareRange — lexicographic comparison of two ranges
    // -------------------------------------------------------------------

    /**
     * Equivalent to JDK 9+'s {@code Arrays.compare(a, aFrom, aTo, b, bFrom, bTo)}.
     * Returns a negative integer, zero, or a positive integer as the first range
     * is lexicographically less than, equal to, or greater than the second. If the
     * ranges are equal over the common prefix, the shorter range compares less.
     */
    public static int compareRange(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_COMPARE_CHAR;
        if (mh != null) {
            try {
                return (int) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return compareRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    /** Byte variant of {@link #compareRange(char[], int, int, char[], int, int)}. */
    public static int compareRange(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        MethodHandle mh = MH_COMPARE_BYTE;
        if (mh != null) {
            try {
                return (int) mh.invokeExact(a, aFrom, aTo, b, bFrom, bTo);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return compareRangeLoop(a, aFrom, aTo, b, bFrom, bTo);
    }

    private static int compareRangeLoop(char[] a, int aFrom, int aTo, char[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom, bLen = bTo - bFrom;
        int common = Math.min(aLen, bLen);
        for (int i = 0; i < common; i++) {
            char ac = a[aFrom + i], bc = b[bFrom + i];
            if (ac != bc) return Character.compare(ac, bc);
        }
        return Integer.compare(aLen, bLen);
    }

    private static int compareRangeLoop(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        int aLen = aTo - aFrom, bLen = bTo - bFrom;
        int common = Math.min(aLen, bLen);
        for (int i = 0; i < common; i++) {
            byte ab = a[aFrom + i], bb = b[bFrom + i];
            if (ab != bb) return Byte.compare(ab, bb);
        }
        return Integer.compare(aLen, bLen);
    }
}
