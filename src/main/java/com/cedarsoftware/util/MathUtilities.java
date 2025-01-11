package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static java.util.Collections.swap;

/**
 * Mathematical utility class providing enhanced numeric operations and algorithms.
 * <p>
 * This class provides:
 * </p>
 * <ul>
 *   <li>Minimum/Maximum calculations for various numeric types</li>
 *   <li>Smart numeric parsing with minimal type selection</li>
 *   <li>Permutation generation</li>
 *   <li>Common mathematical constants</li>
 * </ul>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Support for primitive types (long, double)</li>
 *   <li>Support for BigInteger and BigDecimal</li>
 *   <li>Null-safe operations</li>
 *   <li>Efficient implementations</li>
 *   <li>Thread-safe operations</li>
 * </ul>
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
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class MathUtilities
{
    public static final BigInteger BIG_INT_LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger BIG_INT_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigDecimal BIG_DEC_DOUBLE_MIN = BigDecimal.valueOf(-Double.MAX_VALUE);
    public static final BigDecimal BIG_DEC_DOUBLE_MAX = BigDecimal.valueOf(Double.MAX_VALUE);

    private MathUtilities()
    {
        super();
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static long minimum(long... values)
    {
        final int len = values.length;
        long current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.min(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static long maximum(long... values)
    {
        final int len = values.length;
        long current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.max(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static double minimum(double... values)
    {
        final int len =values.length;
        double current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.min(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static double maximum(double... values)
    {
        final int len = values.length;
        double current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.max(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static BigInteger minimum(BigInteger... values)
    {
        final int len = values.length;
        if (len == 1)
        {
            if (values[0] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigInteger entry to minimum()");
            }
            return values[0];
        }
        BigInteger current = values[0];

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigInteger entry to minimum()");
            }
            current = values[i].min(current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static BigInteger maximum(BigInteger... values)
    {
        final int len = values.length;
        if (len == 1)
        {
            if (values[0] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigInteger entry to maximum()");
            }
            return values[0];
        }
        BigInteger current = values[0];

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigInteger entry to maximum()");
            }
            current = values[i].max(current);
        }

        return current;
    }

    /**
     * Calculate the minimum value from an array of values.
     *
     * @param values Array of values.
     * @return minimum value of the provided set.
     */
    public static BigDecimal minimum(BigDecimal... values)
    {
        final int len = values.length;
        if (len == 1)
        {
            if (values[0] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigDecimal entry to minimum()");
            }
            return values[0];
        }
        BigDecimal current = values[0];

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot passed null BigDecimal entry to minimum()");
            }
            current = values[i].min(current);
        }

        return current;
    }

    /**
     * Calculate the maximum value from an array of values.
     *
     * @param values Array of values.
     * @return maximum value of the provided set.
     */
    public static BigDecimal maximum(BigDecimal... values)
    {
        final int len = values.length;
        if (len == 1)
        {
            if (values[0] == null)
            {
                throw new IllegalArgumentException("Cannot pass null BigDecimal entry to maximum()");
            }
            return values[0];
        }
        BigDecimal current = values[0];

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot pass null BigDecimal entry to maximum()");
            }
            current = values[i].max(current);
        }

        return current;
    }

    /**
     * Parses a string representation of a number into the most appropriate numeric type.
     * <p>
     * This method intelligently selects the smallest possible numeric type that can accurately
     * represent the value, following these rules:
     * </p>
     * <ul>
     *   <li>Integer values within Long range: returns {@link Long}</li>
     *   <li>Integer values outside Long range: returns {@link BigInteger}</li>
     *   <li>Decimal values within Double precision: returns {@link Double}</li>
     *   <li>Decimal values requiring more precision: returns {@link BigDecimal}</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * parseToMinimalNumericType("123")      → Long(123)
     * parseToMinimalNumericType("1.23")     → Double(1.23)
     * parseToMinimalNumericType("1e308")    → BigDecimal
     * parseToMinimalNumericType("999999999999999999999") → BigInteger
     * }</pre>
     *
     * @param numStr the string to parse, must not be null
     * @return the parsed number in its most appropriate type
     * @throws NumberFormatException if the string cannot be parsed as a number
     * @throws IllegalArgumentException if numStr is null
     */
    public static Number parseToMinimalNumericType(String numStr) {
        // Handle and preserve negative signs correctly while removing leading zeros
        boolean isNegative = numStr.startsWith("-");
        if (isNegative || numStr.startsWith("+")) {
            char sign = numStr.charAt(0);
            numStr = sign + numStr.substring(1).replaceFirst("^0+", "");
        } else {
            numStr = numStr.replaceFirst("^0+", "");
        }

        boolean hasDecimalPoint = false;
        boolean hasExponent = false;
        int mantissaSize = 0;
        StringBuilder exponentValue = new StringBuilder();
        int len = numStr.length();

        for (int i = 0; i < len; i++) {
            char c = numStr.charAt(i);
            if (c == '.') {
                hasDecimalPoint = true;
            } else if (c == 'e' || c == 'E') {
                hasExponent = true;
            } else if (c >= '0' && c <= '9') {
                if (!hasExponent) {
                    mantissaSize++; // Count digits in the mantissa only
                } else {
                    exponentValue.append(c);
                }
            }
        }

        if (hasDecimalPoint || hasExponent) {
            if (mantissaSize < 17 && (exponentValue.length() == 0 || Math.abs(Integer.parseInt(exponentValue.toString())) < 308)) {
                return Double.parseDouble(numStr);
            } else {
                return new BigDecimal(numStr);
            }
        } else {
            if (numStr.length() < 19) {
                return Long.parseLong(numStr);
            }
            BigInteger bigInt = new BigInteger(numStr);
            if (bigInt.compareTo(BIG_INT_LONG_MIN) >= 0 && bigInt.compareTo(BIG_INT_LONG_MAX) <= 0) {
                return bigInt.longValue(); // Correctly convert BigInteger back to Long if within range
            } else {
                return bigInt;
            }
        }
    }

    /**
     * Generates the next lexicographically ordered permutation of the given list.
     * <p>
     * This method modifies the input list in-place to produce the next permutation.
     * If there are no more permutations possible, it returns false.
     * </p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
     * do {
     *     System.out.println(list);  // Prints each permutation
     * } while (nextPermutation(list));
     * // Output:
     * // [1, 2, 3]
     * // [1, 3, 2]
     * // [2, 1, 3]
     * // [2, 3, 1]
     * // [3, 1, 2]
     * // [3, 2, 1]
     * }</pre>
     *
     * @param <T> type of elements in the list, must implement Comparable
     * @param list the list to permute, will be modified in-place
     * @return true if a next permutation exists and was generated, false if no more permutations exist
     * @throws IllegalArgumentException if list is null
     */
    public static <T extends Comparable<? super T>> boolean nextPermutation(List<T> list) {
        int k = list.size() - 2;
        while (k >= 0 && list.get(k).compareTo(list.get(k + 1)) >= 0) {
            k--;
        }
        if (k < 0) {
            return false;  // No more permutations
        }
        int l = list.size() - 1;
        while (list.get(k).compareTo(list.get(l)) >= 0) {
            l--;
        }
        swap(list, k, l);
        for (int i = k + 1, j = list.size() - 1; i < j; i++, j--) {
            swap(list, i, j);
        }
        return true;
    }
}