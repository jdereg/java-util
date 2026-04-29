/*
 * @(#)JavaBigIntegerFromByteArray.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.cedarsoftware.util.fastdoubleparser.FastIntegerMath.fillPowersOf10Floor16;

final class JavaBigIntegerFromByteArray extends AbstractBigIntegerParser {

    /**
     * Parses a {@code BigIntegerLiteral} as specified in {@link JavaBigIntegerParser}.
     *
     * @param str    the input string
     * @param offset the start of the string
     * @param length the length of the string
     * @param radix  the radix of the number
     * @return the parsed value (always non-null)
     * @throws NumberFormatException if parsing fails
     */
    public BigInteger parseBigIntegerString(byte[] str, int offset, int length, int radix)
            throws NumberFormatException {
        try {
            final int endIndex = AbstractNumberParser.checkBounds(str.length, offset, length);

            // Parse optional sign
            // -------------------
            int index = offset;
            byte ch = str[index];
            final boolean isNegative = ch == '-';
            if (isNegative || ch == '+') {
                ch = charAt(str, ++index, endIndex);
                if (ch == 0) {
                    throw new NumberFormatException(SYNTAX_ERROR);
                }
            }
            switch (radix) {
                case 10:
                    return parseDecDigits(str, index, endIndex, isNegative);
                case 16:
                    return parseHexDigits(str, index, endIndex, isNegative);
                default:
                    return new BigInteger(new String(str, offset, length, StandardCharsets.ISO_8859_1), radix);
            }
        } catch (ArithmeticException e) {
            NumberFormatException nfe = new NumberFormatException(VALUE_EXCEEDS_LIMITS);
            nfe.initCause(e);
            throw nfe;
        }
    }

    private BigInteger parseDecDigits(byte[] str, int from, int to, boolean isNegative) {
        int numDigits = to - from;
        if (hasManyDigits(numDigits)) {
            return parseManyDecDigits(str, from, to, isNegative);
        }
        int preroll = from + (numDigits & 7);
        long significand = FastDoubleSwar.tryToParseUpTo7Digits(str, from, preroll);
        boolean success = significand >= 0;
        for (from = preroll; from < to; from += 8) {
            int addend = FastDoubleSwar.tryToParseEightDigitsUtf8(str, from);
            success &= addend >= 0;
            significand = significand * 100_000_000L + addend;
        }
        if (!success) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }
        return BigInteger.valueOf(isNegative ? -significand : significand);
    }

    private BigInteger parseHexDigits(byte[] str, int from, int to, boolean isNegative) {
        from = skipZeroes(str, from, to);
        int numDigits = to - from;
        if (numDigits <= 0) {
            return BigInteger.ZERO;
        }
        checkHexBigIntegerBounds(numDigits);
        byte[] bytes = new byte[((numDigits + 1) >> 1) + 1];
        int index = 1;
        boolean illegalDigits = false;

        if ((numDigits & 1) != 0) {
            byte chLow = str[from++];
            int valueLow = lookupHex(chLow);
            bytes[index++] = (byte) valueLow;
            illegalDigits = valueLow < 0;
        }
        int prerollLimit = from + ((to - from) & 7);
        for (; from < prerollLimit; from += 2) {
            byte chHigh = str[from];
            byte chLow = str[from + 1];
            int valueHigh = lookupHex(chHigh);
            int valueLow = lookupHex(chLow);
            bytes[index++] = (byte) (valueHigh << 4 | valueLow);
            illegalDigits |= valueHigh < 0 || valueLow < 0;
        }
        for (; from < to; from += 8, index += 4) {
            long value = FastDoubleSwar.tryToParseEightHexDigits(str, from);
            FastDoubleSwar.writeIntBE(bytes, index, (int) value);
            illegalDigits |= value < 0;
        }
        if (illegalDigits) {
            throw new NumberFormatException(SYNTAX_ERROR);
        }
        BigInteger result = new BigInteger(bytes);
        return isNegative ? result.negate() : result;
    }

    private BigInteger parseManyDecDigits(byte[] str, int from, int to, boolean isNegative) {
        from = skipZeroes(str, from, to);
        int numDigits = to - from;
        checkDecBigIntegerBounds(numDigits);
        Map<Integer, BigInteger> powersOfTen = fillPowersOf10Floor16(from, to);
        BigInteger result = ParseDigitsTaskByteArray.parseDigitsRecursive(str, from, to, powersOfTen, RECURSION_THRESHOLD);
        return isNegative ? result.negate() : result;
    }

    private int skipZeroes(byte[] str, int from, int to) {
        while (from < to - 8 && FastDoubleSwar.isEightZeroes(str, from)) {
            from += 8;
        }
        while (from < to && str[from] == '0') {
            from++;
        }
        return from;
    }
}
