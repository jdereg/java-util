/*
 * @(#)JavaDoubleParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import static com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser.SYNTAX_ERROR;
import static com.cedarsoftware.util.fastdoubleparser.AbstractNumberParser.SYNTAX_ERROR_BITS;

/**
 * Parses a {@code double} value; the supported syntax is compatible with
 * {@link Double#valueOf(String)}.
 * <p>
 * <b>Syntax</b>
 * <p>
 * Leading and trailing whitespace characters in the string are ignored.
 * Whitespace is removed as if by the {@link java.lang.String#trim()} method;
 * that is, characters in the range [U+0000,U+0020].
 * <p>
 * The rest of string should constitute a Java {@code FloatingPointLiteral}
 * as described by the lexical syntax rules shown below:
 * <blockquote>
 * <dl>
 * <dt><i>FloatingPointLiteral:</i></dt>
 * <dd><i>[Sign]</i> {@code NaN}</dd>
 * <dd><i>[Sign]</i> {@code Infinity}</dd>
 * <dd><i>[Sign] DecimalFloatingPointLiteral</i></dd>
 * <dd><i>[Sign] HexFloatingPointLiteral</i></dd>
 * </dl>
 *
 * <dl>
 * <dt><i>HexFloatingPointLiteral</i>:
 * <dd><i>HexSignificand BinaryExponent [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexSignificand:</i>
 * <dd><i>HexNumeral</i>
 * <dd><i>HexNumeral</i> {@code .}
 * <dd>{@code 0x} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * <dd>{@code 0X} <i>[HexDigits]</i> {@code .} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponent:</i>
 * <dd><i>BinaryExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>BinaryExponentIndicator:</i>
 * <dd>{@code p}
 * <dd>{@code P}
 * </dl>
 *
 * <dl>
 * <dt><i>DecimalFloatingPointLiteral:</i>
 * <dd><i>DecSignificand [DecExponent] [FloatTypeSuffix]</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecSignificand:</i>
 * <dd><i>IntegerPart {@code .} [FractionPart]</i>
 * <dd><i>{@code .} FractionPart</i>
 * <dd><i>IntegerPart</i>
 * </dl>
 *
 * <dl>
 * <dt><i>IntegerPart:</i>
 * <dd><i>Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>FractionPart:</i>
 * <dd><i>Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>DecExponent:</i>
 * <dd><i>ExponentIndicator [Sign] Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>ExponentIndicator:</i>
 * <dd>{@code e}
 * <dd>{@code E}
 * </dl>
 *
 * <dl>
 * <dt><i>Sign:</i>
 * <dd>{@code +}+
 * <dd>{@code -}
 * </dl>
 *
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Digit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9}
 * </dl>
 *
 * <dl>
 * <dt><i>HexNumeral:</i>
 * <dd>{@code 0} {@code x} <i>HexDigits</i>
 * <dd>{@code 0} {@code X} <i>HexDigits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigits:</i>
 * <dd><i>HexDigit {HexDigit}</i>
 * </dl>
 *
 * <dl>
 * <dt><i>HexDigit:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code 0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F}
 * </dl>
 *
 * <dl>
 * <dt><i>FloatTypeSuffix:</i>
 * <dd><i>(one of)</i>
 * <dd>{@code f F d D}
 * </dl>
 * </blockquote>
 * <p>
 * Expected character lengths for values produced by {@link Double#toString}:
 * <ul>
 *     <li>{@code DecSignificand} ({@code IntegerPart} + {@code FractionPart}):
 *     1 to 17 digits</li>
 *     <li>{@code IntegerPart}: 1 to 7 digits</li>
 *     <li>{@code FractionPart}: 1 to 16 digits</li>
 *     <li>{@code SignedInteger} in exponent: 1 to 3 digits</li>
 *     <li>{@code FloatingPointLiteral}: 1 to 24 characters, e.g. "-1.2345678901234568E-300"</li>
 * </ul>
 * Maximal input length supported by this parser:
 * <ul>
 *     <li>{@code FloatingPointLiteral} with or without white space around it:
 *     {@link Integer#MAX_VALUE} - 4 = 2,147,483,643 characters.</li>
 * </ul>
 * <p>
 * References:
 * <dl>
 *     <dt>The Java® Language Specification, Java SE 18 Edition, Chapter 3. Lexical Structure, 3.10.2. Floating-Point Literals </dt>
 *     <dd><a href="https://docs.oracle.com/javase/specs/jls/se18/html/jls-3.html#jls-3.10.2">docs.oracle.com</a></dd>
 * </dl>
 */
public final class JavaDoubleParser {

    private static final JavaDoubleBitsFromByteArray BYTE_ARRAY_PARSER = new JavaDoubleBitsFromByteArray();

    private static final JavaDoubleBitsFromCharArray CHAR_ARRAY_PARSER = new JavaDoubleBitsFromCharArray();

    private static final JavaDoubleBitsFromCharSequence CHAR_SEQUENCE_PARSER = new JavaDoubleBitsFromCharSequence();

    /**
     * Don't let anyone instantiate this class.
     */
    private JavaDoubleParser() {

    }

    /**
     * Convenience method for calling {@link #parseDouble(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static double parseDouble(CharSequence str) throws NumberFormatException {
        return parseDouble(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@code double} value.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static double parseDouble(CharSequence str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_SEQUENCE_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Double.longBitsToDouble(bitPattern);
    }



    /**
     * Convenience method for calling {@link #parseDouble(byte[], int, int)}.
     *
     * @param str the string to be parsed, a byte array with characters
     *            in ISO-8859-1, ASCII or UTF-8 encoding
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static double parseDouble(byte[] str) throws NumberFormatException {
        return parseDouble(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code double} value.
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
    public static double parseDouble(byte[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = BYTE_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Double.longBitsToDouble(bitPattern);
    }

    /**
     * Convenience method for calling {@link #parseDouble(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static double parseDouble(char[] str) throws NumberFormatException {
        return parseDouble(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code double} value.
     * <p>
     * See {@link JavaDoubleParser} for the syntax of {@code FloatingPointLiteral}.
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
    public static double parseDouble(char[] str, int offset, int length) throws NumberFormatException {
        long bitPattern = CHAR_ARRAY_PARSER.parseFloatingPointLiteral(str, offset, length);
        if (bitPattern == SYNTAX_ERROR_BITS) throw new NumberFormatException(SYNTAX_ERROR);
        return Double.longBitsToDouble(bitPattern);
    }
}