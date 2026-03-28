package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

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
 *   <li><code>mathutilities.max.array.size=100000</code> &mdash; Array size limit for min/max operations when security is enabled (0 or negative disables)</li>
 *   <li><code>mathutilities.max.string.length=100000</code> &mdash; String length limit for parsing when security is enabled (0 or negative disables)</li>
 *   <li><code>mathutilities.max.permutation.size=10</code> &mdash; List size limit for permutations when security is enabled (0 or negative disables)</li>
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
    private static final Object SECURITY_CONFIG_LOCK = new Object();
    private static volatile SecurityConfig cachedSecurityConfig;

    private static int getMaxArraySize() {
        return getSecurityConfig().maxArraySize;
    }

    private static int getMaxStringLength() {
        return getSecurityConfig().maxStringLength;
    }

    private static int getMaxPermutationSize() {
        return getSecurityConfig().maxPermutationSize;
    }

    private static SecurityConfig getSecurityConfig() {
        String securityEnabledSource = System.getProperty("mathutilities.security.enabled", "false");
        boolean securityEnabled = Boolean.parseBoolean(securityEnabledSource);
        String maxArraySizeSource = securityEnabled ? System.getProperty("mathutilities.max.array.size") : null;
        String maxStringLengthSource = securityEnabled ? System.getProperty("mathutilities.max.string.length") : null;
        String maxPermutationSizeSource = securityEnabled ? System.getProperty("mathutilities.max.permutation.size") : null;

        SecurityConfig config = cachedSecurityConfig;
        if (config != null && config.hasSameSources(securityEnabledSource, maxArraySizeSource, maxStringLengthSource, maxPermutationSizeSource)) {
            return config;
        }

        synchronized (SECURITY_CONFIG_LOCK) {
            config = cachedSecurityConfig;
            if (config != null && config.hasSameSources(securityEnabledSource, maxArraySizeSource, maxStringLengthSource, maxPermutationSizeSource)) {
                return config;
            }

            int maxArraySize = securityEnabled ? parseSecurityLimit(maxArraySizeSource, DEFAULT_MAX_ARRAY_SIZE) : 0;
            int maxStringLength = securityEnabled ? parseSecurityLimit(maxStringLengthSource, DEFAULT_MAX_STRING_LENGTH) : 0;
            int maxPermutationSize = securityEnabled ? parseSecurityLimit(maxPermutationSizeSource, DEFAULT_MAX_PERMUTATION_SIZE) : 0;

            config = new SecurityConfig(securityEnabledSource, maxArraySizeSource, maxStringLengthSource,
                    maxPermutationSizeSource, maxArraySize, maxStringLength, maxPermutationSize);
            cachedSecurityConfig = config;
            return config;
        }
    }

    private static int parseSecurityLimit(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            int limit = Integer.parseInt(value);
            return limit <= 0 ? 0 : limit; // 0 or negative means disabled
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void validateValuesArray(Object values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
    }

    private static final class SecurityConfig {
        private final String securityEnabledSource;
        private final String maxArraySizeSource;
        private final String maxStringLengthSource;
        private final String maxPermutationSizeSource;
        private final int maxArraySize;
        private final int maxStringLength;
        private final int maxPermutationSize;

        private SecurityConfig(String securityEnabledSource, String maxArraySizeSource, String maxStringLengthSource,
                               String maxPermutationSizeSource, int maxArraySize, int maxStringLength,
                               int maxPermutationSize) {
            this.securityEnabledSource = securityEnabledSource;
            this.maxArraySizeSource = maxArraySizeSource;
            this.maxStringLengthSource = maxStringLengthSource;
            this.maxPermutationSizeSource = maxPermutationSizeSource;
            this.maxArraySize = maxArraySize;
            this.maxStringLength = maxStringLength;
            this.maxPermutationSize = maxPermutationSize;
        }

        private boolean hasSameSources(String securityEnabledSource, String maxArraySizeSource,
                                       String maxStringLengthSource, String maxPermutationSizeSource) {
            return Objects.equals(this.securityEnabledSource, securityEnabledSource)
                    && Objects.equals(this.maxArraySizeSource, maxArraySizeSource)
                    && Objects.equals(this.maxStringLengthSource, maxStringLengthSource)
                    && Objects.equals(this.maxPermutationSizeSource, maxPermutationSizeSource);
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
        validateValuesArray(values);
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
        validateValuesArray(values);
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
        validateValuesArray(values);
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
        validateValuesArray(values);
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
        validateValuesArray(values);
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
        BigInteger current = values[0];
        if (current == null)
        {
            throw new IllegalArgumentException("Cannot pass null BigInteger entry to minimum()");
        }

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot pass null BigInteger entry to minimum()");
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
        validateValuesArray(values);
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
        BigInteger current = values[0];
        if (current == null)
        {
            throw new IllegalArgumentException("Cannot pass null BigInteger entry to maximum()");
        }

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot pass null BigInteger entry to maximum()");
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
        validateValuesArray(values);
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
        BigDecimal current = values[0];
        if (current == null)
        {
            throw new IllegalArgumentException("Cannot pass null BigDecimal entry to minimum()");
        }

        for (int i=1; i < len; i++)
        {
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot pass null BigDecimal entry to minimum()");
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
        validateValuesArray(values);
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
        BigDecimal current = values[0];
        if (current == null)
        {
            throw new IllegalArgumentException("Cannot pass null BigDecimal entry to maximum()");
        }

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
        return parseToMinimalNumericType((CharSequence) Objects.requireNonNull(numStr, "numStr"));
    }

    /**
     * Parses a character sequence representation of a number into the most appropriate numeric type.
     * This overload avoids intermediate String construction for callers that already hold a StringBuilder.
     *
     * @param numStr the sequence to parse, must not be null
     * @return the parsed number in its most appropriate type
     */
    public static Number parseToMinimalNumericType(CharSequence numStr)
    {
        Objects.requireNonNull(numStr, "numStr");
        final String text = numStr instanceof String ? (String) numStr : numStr.toString();

        // Security check: validate string length
        int maxStringLength = getMaxStringLength();
        if (maxStringLength > 0 && text.length() > maxStringLength)
        {
            throw new SecurityException("String length exceeds maximum allowed: " + maxStringLength);
        }

        final int len = text.length();
        int start = 0;
        if (len > 0) {
            char first = text.charAt(0);
            if (first == '-' || first == '+') {
                start = 1;
            }
        }

        // Trim integer leading zeros (keeping one zero before non-digit, e.g., "000.1" -> "0.1")
        while (start + 1 < len
                && text.charAt(start) == '0'
                && Character.isDigit(text.charAt(start + 1)))
        {
            start++;
        }

        boolean hasDecimalPoint = false;
        boolean hasExponent = false;
        boolean inExponent = false;
        boolean exponentSignAllowed = false;
        boolean exponentHasDigits = false;
        int mantissaSize = 0;
        int exponentSign = 1;
        long exponentAbs = 0;
        boolean exponentOverflow = false;

        for (int i = start; i < len; i++) {
            char c = text.charAt(i);
            if (c == '.') {
                hasDecimalPoint = true;
                inExponent = false;
                exponentSignAllowed = false;
            } else if (c == 'e' || c == 'E') {
                hasExponent = true;
                inExponent = true;
                exponentSignAllowed = true;
                exponentHasDigits = false;
                exponentSign = 1;
                exponentAbs = 0;
                exponentOverflow = false;
            } else if (inExponent) {
                if (exponentSignAllowed && (c == '+' || c == '-')) {
                    exponentSign = c == '-' ? -1 : 1;
                    exponentSignAllowed = false;
                } else if (c >= '0' && c <= '9') {
                    exponentSignAllowed = false;
                    exponentHasDigits = true;
                    if (!exponentOverflow) {
                        exponentAbs = exponentAbs * 10L + (c - '0');
                        if (exponentAbs > Integer.MAX_VALUE) {
                            exponentOverflow = true;
                        }
                    }
                }
            } else if (c >= '0' && c <= '9') {
                mantissaSize++;
            }
        }

        if (hasDecimalPoint || hasExponent)
        {
            if (mantissaSize < 17)
            {
                try
                {
                    if (!hasExponent || (exponentHasDigits && !exponentOverflow && Math.abs(exponentSign * (int) exponentAbs) < 308))
                    {
                        return Double.parseDouble(text);
                    }
                }
                catch (NumberFormatException ignore)
                {
                    // fall through to BigDecimal
                }
            }
            return new BigDecimal(text);
        } else {
            int digitCount = len - start;
            if (digitCount < 19) {
                return Long.parseLong(text);
            }
            BigInteger bigInt = new BigInteger(text);
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
     *     LOG.info(list);  // Prints each permutation
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
        if (list.size() < 2) {
            return false;
        }
        if (list instanceof RandomAccess) {
            return nextPermutationRandomAccess(list);
        }

        ArrayList<T> orderedCopy = new ArrayList<>(list);
        boolean hasNext = nextPermutationRandomAccess(orderedCopy);
        if (!hasNext) {
            return false;
        }

        ListIterator<T> iterator = list.listIterator();
        for (T value : orderedCopy) {
            iterator.next();
            iterator.set(value);
        }
        return true;
    }

    private static <T extends Comparable<? super T>> boolean nextPermutationRandomAccess(List<T> list) {
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
