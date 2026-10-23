package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

import com.cedarsoftware.util.fastdoubleparser.JavaBigDecimalParser;
import com.cedarsoftware.util.fastdoubleparser.JavaBigIntegerParser;
import com.cedarsoftware.util.fastdoubleparser.JavaDoubleParser;
import com.cedarsoftware.util.fastdoubleparser.JavaFloatParser;

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
     *   <li>Decimal values a double holds: returns {@link Double}. A double holds a value when it reads back
     *       ({@link Double#toString(double)}) as the same number: always for 15 significant digits or fewer in the
     *       double range, sometimes for 16 or 17, never for more.</li>
     *   <li>Other decimal values: returns {@link BigDecimal}, with the value unchanged: more digits than a double
     *       keeps, a value beyond the double range (never Infinity), or one so small it would lose digits or become
     *       0.0. On JDK 8-18, whose {@code Double.toString} does not always write the shortest form (about 3 doubles
     *       in 1,000 get an 18th digit), the type can differ from JDK 19+ for such numbers; the value is the same.</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * parseToMinimalNumericType("123")      → Long(123)
     * parseToMinimalNumericType("1.23")     → Double(1.23)
     * parseToMinimalNumericType("0.30000000000000004") → Double (what Double.toString writes for 0.1 + 0.2)
     * parseToMinimalNumericType("8.000000000000001")   → BigDecimal (the double reads back as 8.000000000000002)
     * parseToMinimalNumericType("1e308")    → Double(1.0E308)
     * parseToMinimalNumericType("1e309")    → BigDecimal
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
        int mantissaEnd = len;
        int pointIndex = -1;
        long exponentAbs = 0;
        boolean exponentNegative = false;
        boolean exponentOverflow = false;

        for (int i = start; i < len; i++) {
            char c = text.charAt(i);
            if (c == '.') {
                if (!hasDecimalPoint) {
                    pointIndex = i;
                }
                hasDecimalPoint = true;
                inExponent = false;
                exponentSignAllowed = false;
            } else if (c == 'e' || c == 'E') {
                if (!hasExponent) {
                    mantissaEnd = i;
                }
                hasExponent = true;
                inExponent = true;
                exponentSignAllowed = true;
                exponentHasDigits = false;
                exponentAbs = 0;
                exponentNegative = false;
                exponentOverflow = false;
            } else if (inExponent) {
                if (exponentSignAllowed && (c == '+' || c == '-')) {
                    exponentNegative = c == '-';
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
            if (!hasExponent || (exponentHasDigits && !exponentOverflow))
            {
                try
                {
                    long exponent = exponentNegative ? -exponentAbs : exponentAbs;
                    Double d = doubleThatReadsBack(text, start, pointIndex, mantissaEnd, exponent, mantissaSize);
                    if (d != null)
                    {
                        return d;
                    }
                }
                catch (NumberFormatException ignore)
                {
                    // fall through to BigDecimal
                }
            }
            return JavaBigDecimalParser.parseBigDecimal(text);
        } else {
            int digitCount = len - start;
            if (digitCount < 19) {
                return Long.parseLong(text);
            }
            BigInteger bigInt = JavaBigIntegerParser.parseBigInteger(text);
            if (bigInt.compareTo(BIG_INT_LONG_MIN) >= 0 && bigInt.compareTo(BIG_INT_LONG_MAX) <= 0) {
                return bigInt.longValue(); // Correctly convert BigInteger back to Long if within range
            } else {
                return bigInt;
            }
        }
    }

    /**
     * The double {@code text} reads as, when it reads back as the same number ({@link Double#toString(double)} writes
     * that number); else null, and the caller makes a BigDecimal. A double in its normal range always reads back as
     * text of 15 significant digits or fewer (DBL_DIG), and never as text of more than 17, since no double is written
     * with more. For 16 or 17 digits, or a subnormal, compare the text with what Double.toString writes for the double.
     *
     * @param pointIndex     where the decimal point is, or -1
     * @param exponent       the value of the exponent, 0 without one
     * @param mantissaDigits the digits in {@code text[start, mantissaEnd)}, the digits before any exponent
     */
    private static Double doubleThatReadsBack(String text, int start, int pointIndex, int mantissaEnd, long exponent,
                                              int mantissaDigits)
    {
        int digits = mantissaDigits <= 15 ? mantissaDigits
                : significantDigits(text, start, mantissaEnd, mantissaDigits);
        if (digits > 17)
        {
            return null;
        }
        double d = JavaDoubleParser.parseDouble(text);
        double abs = Math.abs(d);
        if (abs > Double.MAX_VALUE)
        {
            return null;        // finite text past the double range
        }
        if (abs < Double.MIN_NORMAL)
        {
            digits = significantDigits(text, start, mantissaEnd, mantissaDigits);
            if (digits == 0)
            {
                return d;       // zero, whatever its exponent
            }
            if (abs == 0.0)
            {
                return null;    // nonzero text too small for any double
            }
        }
        else if (digits <= 15)
        {
            return d;
        }
        else if (digits == 16 && leadingExponent(text, start, pointIndex, mantissaEnd, exponent) - 15
                > (Math.getExponent(d) - 52) * LOG10_2)
        {
            // 16-digit decimals here lie farther apart than this double's ulp: no other reads as it, nor a shorter one
            return d;
        }
        String printed = Double.toString(d);
        if (printed.equals(text))
        {
            return d;           // what Double.toString writes, as json-io and TOON write doubles
        }
        Decimal written = Decimal.parse(text);
        if (written == null)
        {
            return d;           // text only the double parser reads (a d suffix, spaces, hex): it has no other reading
        }
        return written.sameNumber(Decimal.parse(printed)) ? d : null;
    }

    /** log10(2): the decimal digits one binary digit is worth. */
    private static final double LOG10_2 = 0.30102999566398119521;

    /**
     * The power of ten of the first nonzero digit in {@code text[start, end)}, which has one, with its decimal point at
     * {@code pointIndex} (-1 for none) and {@code exponent} after it: "0.0012" is -3, "123.4" is 2.
     */
    private static long leadingExponent(String text, int start, int pointIndex, int end, long exponent)
    {
        int i = start;
        while (i < end && !isNonzeroDigit(text.charAt(i)))
        {
            i++;
        }
        int point = pointIndex < 0 ? end : pointIndex;
        return (i < point ? point - i - 1 : point - i) + exponent;
    }

    /**
     * The significant digits in {@code text[start, end)}: its {@code digits} digits less the zeros before the first
     * nonzero digit and after the last ("0.0120" has 2). 0 when every digit is zero.
     */
    private static int significantDigits(String text, int start, int end, int digits)
    {
        int i = start;
        for (; i < end && !isNonzeroDigit(text.charAt(i)); i++)
        {
            if (text.charAt(i) == '0')
            {
                digits--;
            }
        }
        if (i == end)
        {
            return 0;
        }
        for (int j = end - 1; j > i && !isNonzeroDigit(text.charAt(j)); j--)
        {
            if (text.charAt(j) == '0')
            {
                digits--;
            }
        }
        return digits;
    }

    private static boolean isNonzeroDigit(char c)
    {
        return c >= '1' && c <= '9';
    }

    /** Plain decimal text: sign, significant digits (no leading or trailing zeros), the last one's power of ten. */
    private static final class Decimal
    {
        private final boolean negative;
        private final long significand;     // exact for 18 digits or fewer
        private final int digits;           // significant digits, counted past 18 too
        private final long exponent;        // the value is significand x 10^exponent

        private Decimal(boolean negative, long significand, int digits, long exponent)
        {
            this.negative = negative;
            this.significand = significand;
            this.digits = digits;
            this.exponent = exponent;
        }

        /** {@code [sign] digits [. digits] [e|E [sign] digits]}, with a digit before the exponent; else null. */
        static Decimal parse(String s)
        {
            final int len = s.length();
            int i = 0;
            boolean negative = false;
            if (i < len && (s.charAt(i) == '-' || s.charAt(i) == '+'))
            {
                negative = s.charAt(i) == '-';
                i++;
            }
            long significand = 0;
            int digits = 0;
            int zeroRun = 0;        // zeros since the last nonzero digit: significant only if another nonzero follows
            long exponent = 0;
            boolean point = false;
            boolean anyDigit = false;
            for (; i < len; i++)
            {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9')
                {
                    anyDigit = true;
                    if (point)
                    {
                        exponent--;
                    }
                    if (c == '0')
                    {
                        if (digits > 0)
                        {
                            zeroRun++;
                        }
                        continue;
                    }
                    for (; zeroRun > 0; zeroRun--)
                    {
                        if (digits++ < 18)
                        {
                            significand *= 10;
                        }
                    }
                    if (digits++ < 18)
                    {
                        significand = significand * 10 + (c - '0');
                    }
                }
                else if (c == '.' && !point)
                {
                    point = true;
                }
                else
                {
                    break;
                }
            }
            if (!anyDigit)
            {
                return null;
            }
            exponent += zeroRun;    // trailing zeros
            if (i < len)
            {
                char c = s.charAt(i++);
                if (c != 'e' && c != 'E')
                {
                    return null;
                }
                boolean negativeExponent = false;
                if (i < len && (s.charAt(i) == '-' || s.charAt(i) == '+'))
                {
                    negativeExponent = s.charAt(i) == '-';
                    i++;
                }
                if (i == len)
                {
                    return null;
                }
                long e = 0;
                for (; i < len; i++)
                {
                    c = s.charAt(i);
                    if (c < '0' || c > '9')
                    {
                        return null;
                    }
                    if (e < 1_000_000_000L)
                    {
                        e = e * 10 + (c - '0');
                    }
                }
                exponent += negativeExponent ? -e : e;
            }
            return new Decimal(negative, significand, digits, exponent);
        }

        boolean sameNumber(Decimal other)
        {
            return other != null && digits == other.digits && significand == other.significand
                    && exponent == other.exponent && negative == other.negative;
        }
    }

    // ========== Fast Number Parsing ==========
    // Backed by the vendored Randelshofer FastDoubleParser (Eisel-Lemire algorithm).
    // 2-4x faster than the JDK parsers on typical inputs and dramatically faster
    // on large BigInteger / BigDecimal values. Output is bit-identical to the
    // JDK reference parsers (Double.parseDouble, Float.parseFloat, new BigDecimal/BigInteger(String))
    // — covered by FastDoubleParserParityTest.

    /**
     * Parses a {@code double} from a {@link CharSequence} (String, StringBuilder, etc.).
     * Equivalent to {@link Double#parseDouble(String)} but ~2-4x faster.
     *
     * @throws NumberFormatException if the input is not a valid double representation
     * @throws NullPointerException if {@code s} is null
     */
    public static double parseDouble(CharSequence s) {
        return JavaDoubleParser.parseDouble(s);
    }

    /**
     * Parses a {@code double} from a {@code char[]} slice without materializing
     * an intermediate {@link String}. Faster than {@code Double.parseDouble(new String(chars, off, len))}.
     */
    public static double parseDouble(char[] chars, int offset, int length) {
        return JavaDoubleParser.parseDouble(chars, offset, length);
    }

    /** @see #parseDouble(char[], int, int) */
    public static double parseDouble(char[] chars) {
        return JavaDoubleParser.parseDouble(chars, 0, chars.length);
    }

    /**
     * Parses a {@code double} from an ASCII/UTF-8 {@code byte[]} slice. Useful when
     * reading from an {@link java.io.InputStream}-backed buffer.
     */
    public static double parseDouble(byte[] bytes, int offset, int length) {
        return JavaDoubleParser.parseDouble(bytes, offset, length);
    }

    /** @see #parseDouble(byte[], int, int) */
    public static double parseDouble(byte[] bytes) {
        return JavaDoubleParser.parseDouble(bytes, 0, bytes.length);
    }

    /**
     * Parses a {@code float} from a {@link CharSequence}. Equivalent to
     * {@link Float#parseFloat(String)} but ~2-4x faster.
     */
    public static float parseFloat(CharSequence s) {
        return JavaFloatParser.parseFloat(s);
    }

    /** @see #parseFloat(CharSequence) */
    public static float parseFloat(char[] chars, int offset, int length) {
        return JavaFloatParser.parseFloat(chars, offset, length);
    }

    /** @see #parseFloat(CharSequence) */
    public static float parseFloat(char[] chars) {
        return JavaFloatParser.parseFloat(chars, 0, chars.length);
    }

    /** @see #parseFloat(CharSequence) */
    public static float parseFloat(byte[] bytes, int offset, int length) {
        return JavaFloatParser.parseFloat(bytes, offset, length);
    }

    /** @see #parseFloat(CharSequence) */
    public static float parseFloat(byte[] bytes) {
        return JavaFloatParser.parseFloat(bytes, 0, bytes.length);
    }

    /**
     * Parses a {@link BigDecimal}. Materially faster than {@code new BigDecimal(String)}
     * for long mantissas; uses recursive multiplication for the digit accumulation.
     */
    /**
     * How many digits {@code value} takes written out in full, as {@link BigDecimal#toPlainString()} writes it,
     * ignoring the sign and the decimal point: {@code 1E+3} is 4 ("1000"), {@code 0.05} is 3 ("0.05"), {@code 12.5} is
     * 3. Computed from the precision and scale, without writing anything.
     *
     * @param value the number
     * @return its digit count written out in full
     */
    public static long plainDigits(BigDecimal value) {
        long precision = value.precision();
        long scale = value.scale();
        return scale <= 0 ? precision - scale : Math.max(precision, scale + 1);
    }

    /**
     * Whether {@code value} may be written out in full: its plain form takes at most {@code maxDigits} digits, or no
     * more than its own significant digits (so an exponent would save nothing). 999 trillion to the cent is 17 digits;
     * {@code 1E+999999999}, 12 characters, is a billion digits written out.
     *
     * @param value     the number
     * @param maxDigits the most digits an exponent may expand it to
     * @return true if its plain form is within the limit
     */
    public static boolean fitsPlainDigits(BigDecimal value, int maxDigits) {
        int scale = value.scale();
        if (scale >= 0 && scale < maxDigits) {
            return true;    // its plain form is its own digits, or a fraction of fewer than maxDigits: no count needed
        }
        return plainDigits(value) <= Math.max(maxDigits, value.precision());
    }

    /**
     * {@code value} as text: its plain digits, as {@link BigDecimal#toPlainString()} writes them, while
     * {@link #fitsPlainDigits(BigDecimal, int)}; past that, {@link BigDecimal#toString()}'s exponent form
     * ({@code "1E+999999999"} rather than a billion digits). Either reads back as the same BigDecimal.
     *
     * @param value     the number
     * @param maxDigits the most digits an exponent may expand it to (the Converter's default is 1,000)
     * @return its plain form, or its exponent form when the plain one would be longer than that
     */
    public static String toPlainString(BigDecimal value, int maxDigits) {
        return fitsPlainDigits(value, maxDigits) ? value.toPlainString() : value.toString();
    }

    public static BigDecimal parseBigDecimal(CharSequence s) {
        return JavaBigDecimalParser.parseBigDecimal(s);
    }

    /** @see #parseBigDecimal(CharSequence) */
    public static BigDecimal parseBigDecimal(char[] chars, int offset, int length) {
        return JavaBigDecimalParser.parseBigDecimal(chars, offset, length);
    }

    /** @see #parseBigDecimal(CharSequence) */
    public static BigDecimal parseBigDecimal(char[] chars) {
        return JavaBigDecimalParser.parseBigDecimal(chars, 0, chars.length);
    }

    /** @see #parseBigDecimal(CharSequence) */
    public static BigDecimal parseBigDecimal(byte[] bytes, int offset, int length) {
        return JavaBigDecimalParser.parseBigDecimal(bytes, offset, length);
    }

    /** @see #parseBigDecimal(CharSequence) */
    public static BigDecimal parseBigDecimal(byte[] bytes) {
        return JavaBigDecimalParser.parseBigDecimal(bytes, 0, bytes.length);
    }

    /**
     * Parses a {@link BigInteger}. Dramatically faster than {@code new BigInteger(String)}
     * for large numbers (5-50x on values with 100+ digits) — JDK uses an O(n²) algorithm
     * where this uses recursive multiplication.
     */
    public static BigInteger parseBigInteger(CharSequence s) {
        return JavaBigIntegerParser.parseBigInteger(s);
    }

    /** @see #parseBigInteger(CharSequence) */
    public static BigInteger parseBigInteger(char[] chars, int offset, int length) {
        return JavaBigIntegerParser.parseBigInteger(chars, offset, length);
    }

    /** @see #parseBigInteger(CharSequence) */
    public static BigInteger parseBigInteger(char[] chars) {
        return JavaBigIntegerParser.parseBigInteger(chars, 0, chars.length);
    }

    /** @see #parseBigInteger(CharSequence) */
    public static BigInteger parseBigInteger(byte[] bytes, int offset, int length) {
        return JavaBigIntegerParser.parseBigInteger(bytes, offset, length);
    }

    /** @see #parseBigInteger(CharSequence) */
    public static BigInteger parseBigInteger(byte[] bytes) {
        return JavaBigIntegerParser.parseBigInteger(bytes, 0, bytes.length);
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
