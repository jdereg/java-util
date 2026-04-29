/*
 * @(#)AbstractJavaFloatingPointBitsFromCharArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

/**
 * Parses a Java {@code FloatingPointLiteral} from a {@code char} array.
 * <p>
 * This class should have a type parameter for the return value of its parse
 * methods. Unfortunately Java does not support type parameters for primitive
 * types. As a workaround we use {@code long}. A {@code long} has enough bits to
 * fit a {@code double} value or a {@code float} value.
 * <p>
 * See {@link JavaDoubleParser} for the grammar of {@code FloatingPointLiteral}.
 */
abstract class AbstractJavaFloatingPointBitsFromCharArray extends AbstractFloatValueParser {

    private static final boolean CONDITIONAL_COMPILATION_PARSE_EIGHT_HEX_DIGITS = true;


    /**
     * Skips optional white space in the provided string
     *
     * @param str      a string
     * @param index    start index (inclusive) of the optional white space
     * @param endIndex end index (exclusive) of the optional white space
     * @return index after the optional white space
     */
    private static int skipWhitespace(char[] str, int index, int endIndex) {
        while (index < endIndex && str[index] <= ' ') {
            index++;
        }
        return index;
    }

    /**
     * @return a NaN constant in the specialized type wrapped in a {@code long}
     */
    abstract long nan();

    /**
     * @return a negative infinity constant in the specialized type wrapped in a
     * {@code long}
     */
    abstract long negativeInfinity();

    /**
     * Parses a {@code DecimalFloatingPointLiteral} production with optional
     * trailing white space until the end of the text.
     * Given that we have already consumed the optional leading zero of
     * the {@code DecSignificand}.
     * <blockquote>
     * <dl>
     * <dt><i>DecimalFloatingPointLiteralWithWhiteSpace:</i></dt>
     * <dd><i>DecimalFloatingPointLiteral [WhiteSpace] EOT</i></dd>
     * </dl>
     * </blockquote>
     * See {@link JavaDoubleParser} for the grammar of
     * {@code DecimalFloatingPointLiteral} and {@code DecSignificand}.
     *
     * @param str        a string
     * @param index      the current index
     * @param startIndex start index inclusive of the {@code DecimalFloatingPointLiteralWithWhiteSpace}
     * @param endIndex   end index (exclusive)
     * @param isNegative true if the float value is negative
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    private long parseDecFloatLiteral(char[] str, int index, int startIndex, int endIndex, boolean isNegative) {
        // Parse significand
        // -----------------
        // Note: a multiplication by a constant is cheaper than an
        //       arbitrary integer multiplication.
        long significand = 0;// significand is treated as an unsigned long
        final int significandStartIndex = index;
        int integerDigitCount = -1;
        boolean illegal = false;
        char ch = 0;
        int swarLimit = Math.min(endIndex - 4, 1 << 30);
        for (; index < endIndex; index++) {
            ch = str[index];
            int digit = (char) (ch - '0');
            if (digit < 10) {
                // This might overflow, we deal with it later.
                significand = 10 * significand + digit;
            } else if (ch == '.') {
                illegal |= integerDigitCount >= 0;
                integerDigitCount = index - significandStartIndex;
                for (; index < swarLimit; index += 4) {
                    int digits = FastDoubleSwar.tryToParseFourDigits(str, index + 1);
                    if (digits < 0) {
                        break;
                    }
                    // This might overflow, we deal with it later.
                    significand = 10_000L * significand + digits;
                }
            } else {
                break;
            }
        }
        final int digitCount;
        final int significandEndIndex = index;
        int exponent;
        if (integerDigitCount < 0) {
            digitCount = index - significandStartIndex;
            integerDigitCount = digitCount;
            exponent = 0;
        } else {
            digitCount = index - significandStartIndex - 1;
            exponent = integerDigitCount - digitCount;
        }
        illegal |= digitCount == 0 && significandEndIndex > significandStartIndex;

        // Parse exponent number
        // ---------------------
        int expNumber = 0;
        if ((ch | 0x20) == 'e') {// equals ignore case
            ch = charAt(str, ++index, endIndex);
            boolean isExponentNegative = ch == '-';
            if (isExponentNegative || ch == '+') {
                ch = charAt(str, ++index, endIndex);
            }
            int digit = (char) (ch - '0');
            illegal |= digit >= 10;
            do {
                // Guard against overflow
                if (expNumber < AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * expNumber + digit;
                }
                ch = charAt(str, ++index, endIndex);
                digit = (char) (ch - '0');
            } while (digit < 10);
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Parse NaN or Infinity (this occurs rarely)
        // ---------------------
        if (!illegal && digitCount == 0) {
            return parseNaNOrInfinity(str, index, endIndex, isNegative);
        }

        // Skip optional FloatTypeSuffix
        // long-circuit-or is faster than short-circuit-or
        // ------------------------
        if ((ch | 0x22) == 'f') { // ~ "fFdD"
            index++;
        }

        // Skip trailing whitespace and check if FloatingPointLiteral is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex) {
            return SYNTAX_ERROR_BITS;
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int exponentOfTruncatedSignificand;
        if (digitCount > 19) {
            int truncatedDigitCount = 0;
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str[index];
                int digit = (char) (ch - '0');
                if (digit < 10) {
                    if (Long.compareUnsigned(significand, AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = 10 * significand + digit;
                        truncatedDigitCount++;
                    } else {
                        break;
                    }
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
            exponentOfTruncatedSignificand = integerDigitCount - truncatedDigitCount + expNumber;
        } else {
            isSignificandTruncated = false;
            exponentOfTruncatedSignificand = 0;
        }
        return valueOfFloatLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                exponentOfTruncatedSignificand);
    }

    /**
     * Parses a {@code FloatingPointLiteral} production with optional leading and trailing
     * white space.
     * <blockquote>
     * <dl>
     * <dt><i>FloatingPointLiteralWithWhiteSpace:</i></dt>
     * <dd><i>[WhiteSpace] FloatingPointLiteral [WhiteSpace]</i></dd>
     * </dl>
     * </blockquote>
     * See {@link JavaDoubleParser} for the grammar of
     * {@code FloatingPointLiteral}.
     *
     * @param str    a string containing a {@code FloatingPointLiteralWithWhiteSpace}
     * @param offset start offset of {@code FloatingPointLiteralWithWhiteSpace} in {@code str}
     * @param length length of {@code FloatingPointLiteralWithWhiteSpace} in {@code str}
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    public long parseFloatingPointLiteral(char[] str, int offset, int length) {
        final int endIndex = checkBounds(str.length, offset, length);

        // Skip leading whitespace
        // -------------------
        int index = skipWhitespace(str, offset, endIndex);
        if (index == endIndex) {
            return SYNTAX_ERROR_BITS;
        }
        char ch = str[index];

        // Parse optional sign
        // -------------------
        final boolean isNegative = ch == '-';
        if (isNegative || ch == '+') {
            ch = charAt(str, ++index, endIndex);
            if (ch == 0) {
                return SYNTAX_ERROR_BITS;
            }
        }

        // Parse optional leading zero
        // ---------------------------
        final boolean hasLeadingZero = ch == '0';
        if (hasLeadingZero) {
            ch = charAt(str, ++index, endIndex);
            if ((ch | 0x20) == 'x') {// equals ignore case
                return parseHexFloatLiteral(str, index + 1, offset, endIndex, isNegative);
            }
            index--;//push the leading zero back
        }

        return parseDecFloatLiteral(str, index, offset, endIndex, isNegative);
    }

    /**
     * Parses the following rules
     * (more rules are defined in {@link AbstractFloatValueParser}):
     * <dl>
     * <dt><i>RestOfHexFloatingPointLiteral</i>:
     * <dd><i>RestOfHexSignificand BinaryExponent</i>
     * </dl>
     *
     * <dl>
     * <dt><i>RestOfHexSignificand:</i>
     * <dd><i>HexDigits</i>
     * <dd><i>HexDigits</i> {@code .}
     * <dd><i>[HexDigits]</i> {@code .} <i>HexDigits</i>
     * </dl>
     *
     * @param str        the input string
     * @param index      index to the first character of RestOfHexFloatingPointLiteral
     * @param startIndex the start index of the string
     * @param endIndex   the end index of the string
     * @param isNegative if the resulting number is negative
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    private long parseHexFloatLiteral(
            char[] str, int index, int startIndex, int endIndex, boolean isNegative) {

        // Parse HexSignificand
        // ------------
        long significand = 0;// significand is treated as an unsigned long
        int exponent = 0;
        final int significandStartIndex = index;
        int virtualIndexOfPoint = -1;
        final int digitCount;
        boolean illegal = false;
        char ch = 0;
        for (; index < endIndex; index++) {
            ch = str[index];
            // Table look up is faster than a sequence of if-else-branches.
            int hexValue = lookupHex(ch);
            if (hexValue >= 0) {
                significand = (significand << 4) | hexValue;// This might overflow, we deal with it later.
            } else if (hexValue == DECIMAL_POINT_CLASS) {
                illegal |= virtualIndexOfPoint >= 0;
                virtualIndexOfPoint = index;
                if (CONDITIONAL_COMPILATION_PARSE_EIGHT_HEX_DIGITS) {
                    for (; index < endIndex - 8; index += 8) {
                        long parsed = tryToParseEightHexDigits(str, index + 1);
                        if (parsed >= 0) {
                            // This might overflow, we deal with it later.
                            significand = (significand << 32) + parsed;
                        } else {
                            break;
                        }
                    }
                }
            } else {
                break;
            }
        }
        final int significandEndIndex = index;
        if (virtualIndexOfPoint < 0) {
            digitCount = significandEndIndex - significandStartIndex;
            virtualIndexOfPoint = significandEndIndex;
        } else {
            digitCount = significandEndIndex - significandStartIndex - 1;
            exponent = Math.min(virtualIndexOfPoint - index + 1, AbstractFloatValueParser.MAX_EXPONENT_NUMBER) * 4;
        }

        // Parse exponent
        // --------------
        int expNumber = 0;
        final boolean hasExponent = (ch | 0x20) == 'p';// equals ignore case;
        if (hasExponent) {
            ch = charAt(str, ++index, endIndex);
            boolean isExponentNegative = ch == '-';
            if (isExponentNegative || ch == '+') {
                ch = charAt(str, ++index, endIndex);
            }
            int digit = (char) (ch - '0');
            illegal |= digit >= 10;
            do {
                // Guard against overflow
                if (expNumber < AbstractFloatValueParser.MAX_EXPONENT_NUMBER) {
                    expNumber = 10 * (expNumber) + digit;
                }
                ch = charAt(str, ++index, endIndex);
                digit = (char) (ch - '0');
            } while (digit < 10);
            if (isExponentNegative) {
                expNumber = -expNumber;
            }
            exponent += expNumber;
        }

        // Skip optional FloatTypeSuffix
        // long-circuit-or is faster than short-circuit-or
        // ------------------------
        if ((ch | 0x22) == 'f') { // ~ "fFdD"
            index++;
        }

        // Skip trailing whitespace and check if FloatingPointLiteral is complete
        // ------------------------
        index = skipWhitespace(str, index, endIndex);
        if (illegal || index < endIndex
                || digitCount == 0
                || !hasExponent) {
            return SYNTAX_ERROR_BITS;
        }

        // Re-parse significand in case of a potential overflow
        // -----------------------------------------------
        final boolean isSignificandTruncated;
        int skipCountInTruncatedDigits = 0;//counts +1 if we skipped over the decimal point
        if (digitCount > 16) {
            significand = 0;
            for (index = significandStartIndex; index < significandEndIndex; index++) {
                ch = str[index];
                // Table look up is faster than a sequence of if-else-branches.
                int hexValue = lookupHex(ch);
                if (hexValue >= 0) {
                    if (Long.compareUnsigned(significand, AbstractFloatValueParser.MINIMAL_NINETEEN_DIGIT_INTEGER) < 0) {
                        significand = (significand << 4) | hexValue;
                    } else {
                        break;
                    }
                } else {
                    skipCountInTruncatedDigits++;
                }
            }
            isSignificandTruncated = (index < significandEndIndex);
        } else {
            isSignificandTruncated = false;
        }

        return valueOfHexLiteral(str, startIndex, endIndex, isNegative, significand, exponent, isSignificandTruncated,
                (virtualIndexOfPoint - index + skipCountInTruncatedDigits) * 4 + expNumber);
    }

    private long parseNaNOrInfinity(char[] str, int index, int endIndex, boolean isNegative) {
        if (index < endIndex) {
            if (str[index] == 'N') {
                if (index + 2 < endIndex
                        // && str[index] == 'N'
                        && str[index + 1] == 'a'
                        && str[index + 2] == 'N') {

                    index = skipWhitespace(str, index + 3, endIndex);
                    if (index == endIndex) {
                        return nan();
                    }
                }
            } else {
                if (index + 7 < endIndex
                        && str[index] == 'I'
                        && str[index + 1] == 'n'
                        && str[index + 2] == 'f'
                        && str[index + 3] == 'i'
                        && str[index + 4] == 'n'
                        && str[index + 5] == 'i'
                        && str[index + 6] == 't'
                        && str[index + 7] == 'y'
                ) {
                    index = skipWhitespace(str, index + 8, endIndex);
                    if (index == endIndex) {
                        return isNegative ? negativeInfinity() : positiveInfinity();
                    }
                }
            }
        }
        return SYNTAX_ERROR_BITS;
    }

    /**
     * @return a positive infinity constant in the specialized type wrapped in a
     * {@code long}
     */
    abstract long positiveInfinity();

    private long tryToParseEightHexDigits(char[] str, int offset) {
        return FastDoubleSwar.tryToParseEightHexDigits(str, offset);
    }


    /**
     * Computes a float value from the given components of a decimal float
     * literal.
     *
     * @param str                            the string that contains the float literal (and maybe more)
     * @param startIndex                     the start index (inclusive) of the float literal
     *                                       inside the string
     * @param endIndex                       the end index (exclusive) of the float literal inside
     *                                       the string
     * @param isNegative                     whether the float value is negative
     * @param significand                    the significand of the float value (can be truncated)
     * @param exponent                       the exponent of the float value
     * @param isSignificandTruncated         whether the significand is truncated
     * @param exponentOfTruncatedSignificand the exponent value of the truncated
     *                                       significand
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    abstract long valueOfFloatLiteral(
            char[] str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);


    /**
     * Computes a float value from the given components of a hexadecimal float
     * literal.
     *
     * @param str                            the string that contains the float literal (and maybe more)
     * @param startIndex                     the start index (inclusive) of the float literal
     *                                       inside the string
     * @param endIndex                       the end index (exclusive) of the float literal inside
     *                                       the string
     * @param isNegative                     whether the float value is negative
     * @param significand                    the significand of the float value (can be truncated)
     * @param exponent                       the exponent of the float value
     * @param isSignificandTruncated         whether the significand is truncated
     * @param exponentOfTruncatedSignificand the exponent value of the truncated
     *                                       significand
     * @return the bit pattern of the parsed value, if the input is legal;
     * otherwise, {@code -1L}.
     */
    abstract long valueOfHexLiteral(
            char[] str, int startIndex, int endIndex,
            boolean isNegative, long significand, int exponent,
            boolean isSignificandTruncated, int exponentOfTruncatedSignificand);

}