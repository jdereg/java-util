/*
 * @(#)JavaBigIntegerParser.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;

import java.math.BigInteger;

/**
 * Parses a {@link BigInteger} value; the supported syntax is compatible
 * with {@link BigInteger#BigInteger(String, int)}.
 * <p>
 * <b>Syntax</b>
 * <p>
 * Formal specification of the grammar:
 * <blockquote>
 * <dl>
 * <dt><i>BigIntegerString:</i></dt>
 * <dd><i>[Sign] Digits</i></dd>
 * </dl>
 * <dl>
 * <dt><i>Sign:</i>
 * <dd><i>(one of)</i>
 * <br>{@code + -}
 * </dd>
 * </dl>
 * <dl>
 * <dt><i>Digits:</i>
 * <dd><i>Digit {Digit}</i>
 * </dl>
 * <dl>
 * <dt><i>Digit:</i>
 * <dd><i>(one of)</i>
 * <br>{@code 0 1 2 3 4 5 6 7 8 9}
 * <br>{@code a b c d e f g h i j k l m n o p q r s t u v w x y z}
 * <br>{@code A B C D E F G H I J K L M N O P Q R S T U V W X Y Z}
 * </dd>
 * </dl>
 * </blockquote>
 * <p>
 * Character lengths accepted by {@link BigInteger#BigInteger(String)}:
 * <ul>
 *     <li>{@code BigIntegerString}: {@link Integer#MAX_VALUE} - 4.
 *     <p>
 *     If the significand consists only of zero digits, the length is
 *     only limited by the maximal supported length of a Java array.
 *     </li>
 *     <li>{@code radix=10}
 *     <p>
 *     If the significand contains at least one non-zero digit,
 *     the length is limited to {@code index of first non-zero digit + 646_456_993}.
 *     This is because the resulting value must fit into {@code 2^31 - 1} bits. The decimal
 *     representation of the value {@code 2^31 - 1} needs 646,456,993 digits.
 *     Therefore an input String can only contain up to that many significant
 *     digits - the remaining digits must be leading zeroes.
 *     </li>
 *     <li>{@code radix=16}
 *     <p>
 *     If the significand contains at least one non-zero digit,
 *     the length is limited to {@code index of first non-zero digit + 536_870_912}.
 *     This is because the resulting value must fit into {@code 2^31 - 1} bits. The hexa-decimal
 *     representation of the value {@code 2^31 - 1} needs 536,870,912 digits.
 *     Therefore an input String can only contain up to that many significant
 *     digits - the remaining digits must be leading zeroes.
 *     </li>
 * </ul>
 * <p>
 * References:
 * <dl>
 *     <dt>Java SE 17 &amp; JDK 17, JavaDoc, Class BigInteger</dt>
 *     <dd><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigInteger.html#%3Cinit%3E(java.lang.String,int)">docs.oracle.com</a></dd>
 * </dl> */
public final class JavaBigIntegerParser {

    private static final JavaBigIntegerFromByteArray BYTE_ARRAY_PARSER = new JavaBigIntegerFromByteArray();

    private static final JavaBigIntegerFromCharArray CHAR_ARRAY_PARSER = new JavaBigIntegerFromCharArray();

    private static final JavaBigIntegerFromCharSequence CHAR_SEQUENCE_PARSER = new JavaBigIntegerFromCharSequence();

    /**
     * Don't let anyone instantiate this class.
     */
    private JavaBigIntegerParser() {
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(CharSequence, int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(CharSequence str) {
        return CHAR_SEQUENCE_PARSER.parseBigIntegerString(str, 0, str.length(), 10);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(CharSequence, int, int, int)}.
     *
     * @param str   the string to be parsed
     * @param radix the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(CharSequence str, int radix) {
        return CHAR_SEQUENCE_PARSER.parseBigIntegerString(str, 0, str.length(), radix);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(CharSequence, int, int, int)}.
     *
     * @param str    the string to be parsed
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(CharSequence str, int offset, int length) {
        return CHAR_SEQUENCE_PARSER.parseBigIntegerString(str, offset, length, 10);
    }

    /**
     * Parses a {@code BigIntegerString} from a {@link CharSequence} and converts it
     * into a {@link BigInteger} value.
     *
     * @param str    the string to be parsed
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @param radix  the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(CharSequence str, int offset, int length, int radix) {
        return CHAR_SEQUENCE_PARSER.parseBigIntegerString(str, offset, length, radix);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(byte[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(byte[] str) {
        return BYTE_ARRAY_PARSER.parseBigIntegerString(str, 0, str.length, 10);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(byte[], int, int, int)}.
     *
     * @param str   the string to be parsed
     * @param radix the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(byte[] str, int radix) {
        return BYTE_ARRAY_PARSER.parseBigIntegerString(str, 0, str.length, radix);
    }

    /**
     * Parses a {@code BigIntegerString} from a {@code byte}-Array and converts it
     * into a {@link BigInteger} value.
     * <p>
     * See {@link JsonDoubleParser} for the syntax of {@code FloatingPointLiteral}.
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
    public static BigInteger parseBigInteger(byte[] str, int offset, int length) {
        return BYTE_ARRAY_PARSER.parseBigIntegerString(str, offset, length, 10);
    }

    /**
     * Parses a {@code BigIntegerString} from a {@code byte}-Array and converts it
     * into a {@link BigInteger} value.
     * <p>
     * See {@link JsonDoubleParser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @param radix  the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(byte[] str, int offset, int length, int radix) {
        return BYTE_ARRAY_PARSER.parseBigIntegerString(str, offset, length, radix);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(char[], int, int)}.
     *
     * @param str the string to be parsed
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(char[] str) {
        return CHAR_ARRAY_PARSER.parseBigIntegerString(str, 0, str.length, 10);
    }

    /**
     * Convenience method for calling {@link #parseBigInteger(char[], int, int, int)}.
     *
     * @param str   the string to be parsed
     * @param radix the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(char[] str, int radix) {
        return CHAR_ARRAY_PARSER.parseBigIntegerString(str, 0, str.length, radix);
    }

    /**
     * Parses a {@code BigIntegerString} from a {@code char}-Array and converts it
     * into a {@link BigInteger} value.
     * <p>
     * See {@link JsonDoubleParser} for the syntax of {@code FloatingPointLiteral}.
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
    public static BigInteger parseBigInteger(char[] str, int offset, int length) {
        return CHAR_ARRAY_PARSER.parseBigIntegerString(str, offset, length, 10);
    }

    /**
     * Parses a {@code BigIntegerString} from a {@code char}-Array and converts it
     * into a {@link BigInteger} value.
     * <p>
     * See {@link JsonDoubleParser} for the syntax of {@code FloatingPointLiteral}.
     *
     * @param str    the string to be parsed, a byte array with characters
     *               in ISO-8859-1, ASCII or UTF-8 encoding
     * @param offset The index of the first character to parse
     * @param length The number of characters to parse
     * @param radix  the radix to be used in interpreting the {@code str}
     * @return the parsed value
     * @throws NullPointerException     if the string is null
     * @throws IllegalArgumentException if offset or length are illegal
     * @throws NumberFormatException    if the string can not be parsed successfully
     */
    public static BigInteger parseBigInteger(char[] str, int offset, int length, int radix) {
        return CHAR_ARRAY_PARSER.parseBigIntegerString(str, offset, length, radix);
    }
}
