/*
 * @(#)JavaBigDecimalParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Parses a {@link BigDecimal} value; the supported syntax is compatible with
 * {@link BigDecimal#BigDecimal(String)}.
 * <p>
 * <b>Syntax</b>
 * <p>
 * Parses a {@code BigDecimalString} that is compatible with
 * the grammar specified in {@link BigDecimal#BigDecimal(String)}.
 * <p>
 * Formal specification of the grammar:
 * <blockquote>
 * <dl>
 * <dt><i>BigDecimalString:</i></dt>
 * <dd><i>[Sign] Significand [Exponent]</i></dd>
 * </dl>
 *
 * <dl>
 * <dt><i>Sign:</i>
 * <dd>{@code +}
 * <dd>{@code -}
 * </dl>
 *
 * <dl>
 * <dt><i>Significand:</i>
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
 * <dd><i>ExponentIndicator SignedInteger</i>
 * </dl>
 *
 * <dl>
 * <dt><i>ExponentIndicator:</i>
 * <dd><i>e</i>
 * <dd><i>E</i>
 * </dl>
 *
 * <dl>
 * <dt><i>SignedInteger:</i>
 * <dd><i>[Sign] Digits</i>
 * </dl>
 *
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 * </blockquote>
 * <p>
 * Character lengths accepted by {@link BigInteger#BigInteger(String)}:
 * <ul>
 *     <li>{@code BigDecimalString}: {@link Integer#MAX_VALUE} - 4.
 * <p>
 *     The resulting value must fit into {@code 2^31 - 1} bits. The decimal
 *     representation of the value {@code 2^31 - 1} has 646,456,993 digits.
 *     Therefore an input String can only contain up to that many significant
 *     digits - the remaining digits must be leading zeroes.
 *     </li>
 *
 *     <li>{@code SignedInteger} in exponent: 1 to 10 digits. Exponents
 *     with more digits would yield to a {@link BigDecimal#scale()} that
 *     does not fit into a {@code int}-value.</li>
 *
 *     <li>{@code BigDecimalString}: 1 to 1,292,782,621+4+10=1,292,782,635
 *     characters,
 *     e.g. "-1.234567890....12345E-2147483647".</li>
 * </ul>
 * <p>
 * References:
 * <dl>
 *     <dt>Java SE 17 &amp; JDK 17, JavaDoc, Class BigDecimal</dt>
 *     <dd><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html#%3Cinit%3E(java.lang.String)">docs.oracle.com</a></dd>
 * </dl>
 */
public final class JavaBigDecimalParser {

    private static final JavaBigDecimalFromByteArray BYTE_ARRAY_PARSER = new JavaBigDecimalFromByteArray();

    private static final JavaBigDecimalFromCharArray CHAR_ARRAY_PARSER = new JavaBigDecimalFromCharArray();

    private static final JavaBigDecimalFromCharSequence CHAR_SEQUENCE_PARSER = new JavaBigDecimalFromCharSequence();

    /**
     * Don't let anyone instantiate this class.
     */
    private JavaBigDecimalParser() {

    }

    /**
     * Convenience method for calling {@link #parseBigDecimal(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigDecimal parseBigDecimal(CharSequence str) throws NumberFormatException {
        return parseBigDecimal(str, 0, str.length());
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@link CharSequence} and converts it
     * into a {@link BigDecimal} value.
     *
     * @param str    the string to be parsed
     * @param offset the start offset of the {@code FloatingPointLiteral} in {@code str}
     * @param length the length of {@code FloatingPointLiteral} in {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static BigDecimal parseBigDecimal(CharSequence str, int offset, int length) throws NumberFormatException {
        return CHAR_SEQUENCE_PARSER.parseBigDecimalString(str, offset, length);
    }

    /**
     * Convenience method for calling {@link #parseBigDecimal(byte[], int, int)}.
     *
     * @param str the string to be parsed, a byte array with characters
     *            in ISO-8859-1, ASCII or UTF-8 encoding
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigDecimal parseBigDecimal(byte[] str) throws NumberFormatException {
        return parseBigDecimal(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@link BigDecimal} value.
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
    public static BigDecimal parseBigDecimal(byte[] str, int offset, int length) throws NumberFormatException {
        return BYTE_ARRAY_PARSER.parseBigDecimalString(str, offset, length);
    }

    /**
     * Convenience method for calling {@link #parseBigDecimal(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigDecimal parseBigDecimal(char[] str) throws NumberFormatException {
        return parseBigDecimal(str, 0, str.length);
    }

    /**
     * Parses a {@code FloatingPointLiteral} from a {@code byte}-Array and converts it
     * into a {@code double} value.
     * <p>
     * See {@link JavaBigDecimalParser} for the syntax of {@code FloatingPointLiteral}.
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
    public static BigDecimal parseBigDecimal(char[] str, int offset, int length) throws NumberFormatException {
        return CHAR_ARRAY_PARSER.parseBigDecimalString(str, offset, length);
    }
}