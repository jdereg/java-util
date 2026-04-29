/*
 * @(#)JavaFloatParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import static com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser.SYNTAX_ERROR;
import static com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser.SYNTAX_ERROR_BITS;

/**
 * Parses a {@code float} value; the supported syntax is compatible with
 * {@link Float#valueOf(String)}.
 * <p>
 * See {@link JavaDoubleParser} for a description of the supported grammar.
 * <p>
 * Expected character lengths for values produced by {@link Float#toString}:
 * <ul>
 *     <li>{@code DecSignificand} ({@code IntegerPart} + {@code FractionPart}):
 *     1 to 8 digits</li>
 *     <li>{@code IntegerPart}: 1 to 7 digits</li>
 *     <li>{@code FractionPart}: 1 to 7 digits</li>
 *     <li>{@code SignedInteger} in exponent: 1 to 2 digits</li>
 *     <li>{@code FloatingPointLiteral}: 1 to 14 characters, e.g. "-1.2345678E-38"</li>
 * </ul>
 * Maximal input length supported by this parser:
 * <ul>
 *     <li>{@code FloatingPointLiteral} with or without white space around it:
 *     {@link Integer#MAX_VALUE} - 4 = 2,147,483,643 characters.</li>
 * </ul>
 */
public final class JavaFloatParser {

    private static final JavaFloatBitsFromByteArray BYTE_ARRAY_PARSER = new JavaFloatBitsFromByteArray();

    private static final JavaFloatBitsFromCharArray CHAR_ARRAY_PARSER = new JavaFloatBitsFromCharArray();

    private static final JavaFloatBitsFromCharSequence CHAR_SEQUENCE_PARSER = new JavaFloatBitsFromCharSequence();


    /**
     * Don't let anyone instantiate this class.
     */
    private JavaFloatParser() {

    }

    /**
     * Convenience method for calling {@link #parseFloat(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(CharSequence str) throws NumberFormatException {
        return parseFloat(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(CharSequence str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_SEQUENCE_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Float.intBitsToFloat((int) bitPattern);
    }


    /**
     * Convenience method for calling {@link #parseFloat(byte[], int, int)}.
     *
     * @param str the string to be parsed, a byte array with characters
     *            in ISO-8859-1, ASCII or UTF-8 encoding
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(byte[] str) throws NumberFormatException {
        return parseFloat(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first byte to parse
     * @param length The number of bytes to parse
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(byte[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = BYTE_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Float.intBitsToFloat((int) bitPattern);
    }


    /**
     * Convenience method for calling {@link #parseFloat(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static float parseFloat(char[] str) throws NumberFormatException {
        return parseFloat(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code float} value.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static float parseFloat(char[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Float.intBitsToFloat((int) bitPattern);
    }
}