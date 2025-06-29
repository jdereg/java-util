package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

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
 * <p><strong>Security Configuration:</strong></p>
 * <p>MathUtilities provides configurable security options through system properties.
 * All security features are <strong>disabled by default</strong> for backward compatibility:</p>
 * <ul>
 *   <li><code>mathutilities.security.enabled=false</code> &mdash; Master switch to enable all security features</li>
 *   <li><code>mathutilities.max.array.size=0</code> &mdash; Array size limit for min/max operations (0=disabled)</li>
 *   <li><code>mathutilities.max.string.length=0</code> &mdash; String length limit for parsing (0=disabled)</li>
 *   <li><code>mathutilities.max.permutation.size=0</code> &mdash; List size limit for permutations (0=disabled)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * // Enable security with default limits
 * System.setProperty("mathutilities.security.enabled", "true");
 *
 * // Or enable with custom limits
 * System.setProperty("mathutilities.security.enabled", "true");
 * System.setProperty("mathutilities.max.array.size", "1000");
 * System.setProperty("mathutilities.max.string.length", "100");
 * System.setProperty("mathutilities.max.permutation.size", "10");
 * }</pre>
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

    // Security Configuration - using dynamic property reading for testability
    // Default limits used when security is enabled but no custom limits specified
    private static final int DEFAULT_MAX_ARRAY_SIZE = 100000;       // 100K array elements
    private static final int DEFAULT_MAX_STRING_LENGTH = 100000;    // 100K character string
    private static final int DEFAULT_MAX_PERMUTATION_SIZE = 10;     // 10! = 3.6M permutations
    
    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("mathutilities.security.enabled", "false"));
    }
    
    private static int getMaxArraySize() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String value = System.getProperty("mathutilities.max.array.size");
        if (value == null) {
            return DEFAULT_MAX_ARRAY_SIZE;
        }
        try {
            int limit = Integer.parseInt(value);
            return limit <= 0 ? 0 : limit; // 0 or negative means disabled
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_ARRAY_SIZE;
        }
    }
    
    private static int getMaxStringLength() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String value = System.getProperty("mathutilities.max.string.length");
        if (value == null) {
            return DEFAULT_MAX_STRING_LENGTH;
        }
        try {
            int limit = Integer.parseInt(value);
            return limit <= 0 ? 0 : limit; // 0 or negative means disabled
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_STRING_LENGTH;
        }
    }
    
    private static int getMaxPermutationSize() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String value = System.getProperty("mathutilities.max.permutation.size");
        if (value == null) {
            return DEFAULT_MAX_PERMUTATION_SIZE;
        }
        try {
            int limit = Integer.parseInt(value);
            return limit <= 0 ? 0 : limit; // 0 or negative means disabled
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_PERMUTATION_SIZE;
        }
    }

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
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
        long current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.min(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the maximum value from an array of values.
     *
     * @param values Array of values.
     * @return maximum value of the provided set.
     */
    public static long maximum(long... values)
    {
        final int len = values.length;
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
        final int len = values.length;
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
        double current = values[0];

        for (int i=1; i < len; i++)
        {
            current = Math.min(values[i], current);
        }

        return current;
    }

    /**
     * Calculate the maximum value from an array of values.
     *
     * @param values Array of values.
     * @return maximum value of the provided set.
     */
    public static double maximum(double... values)
    {
        final int len = values.length;
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
     * Calculate the maximum value from an array of values.
     *
     * @param values Array of values.
     * @return maximum value of the provided set.
     */
    public static BigInteger maximum(BigInteger... values)
    {
        final int len = values.length;
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
        if (len == 0)
        {
            throw new IllegalArgumentException("values cannot be empty");
        }
        // Security check: validate array size
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && len > maxArraySize)
        {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
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
    public static Number parseToMinimalNumericType(String numStr)
    {
        Objects.requireNonNull(numStr, "numStr");
        
        // Security check: validate string length
        int maxStringLength = getMaxStringLength();
        if (maxStringLength > 0 && numStr.length() > maxStringLength)
        {
            throw new SecurityException("String length exceeds maximum allowed: " + maxStringLength);
        }

        boolean negative = false;
        boolean positive = false;
        int index = 0;
        if (numStr.startsWith("-"))
        {
            negative = true;
            index = 1;
        }
        else if (numStr.startsWith("+"))
        {
            positive = true;
            index = 1;
        }

        StringBuilder digits = new StringBuilder(numStr.length() - index);
        int len = numStr.length();
        while (index < len && numStr.charAt(index) == '0' && index + 1 < len && Character.isDigit(numStr.charAt(index + 1)))
        {
            index++;
        }
        digits.append(numStr.substring(index));
        if (digits.length() == 0)
        {
            digits.append('0');
        }
        numStr = (negative ? "-" : (positive ? "+" : "")) + digits.toString();

        boolean hasDecimalPoint = false;
        boolean hasExponent = false;
        int mantissaSize = 0;
        StringBuilder exponentValue = new StringBuilder();
        len = numStr.length();

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

        if (hasDecimalPoint || hasExponent)
        {
            if (mantissaSize < 17)
            {
                try
                {
                    if (exponentValue.length() == 0 || Math.abs(Integer.parseInt(exponentValue.toString())) < 308)
                    {
                        return Double.parseDouble(numStr);
                    }
                }
                catch (NumberFormatException ignore)
                {
                    // fall through to BigDecimal
                }
            }
            return new BigDecimal(numStr);
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
    public static <T extends Comparable<? super T>> boolean nextPermutation(List<T> list)
    {
        if (list == null)
        {
            throw new IllegalArgumentException("list cannot be null");
        }
        
        // Security check: validate list size
        int maxPermutationSize = getMaxPermutationSize();
        if (maxPermutationSize > 0 && list.size() > maxPermutationSize)
        {
            throw new SecurityException("List size exceeds maximum allowed for permutation: " + maxPermutationSize);
        }
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
