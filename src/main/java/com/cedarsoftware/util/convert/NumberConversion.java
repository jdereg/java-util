package com.cedarsoftware.util.convert;

import java.math.BigDecimal;

public class NumberConversion {

    public static byte toByte(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).byteValue();
    }

    public static short toShort(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).shortValue();
    }

    public static int toInt(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).intValue();
    }

    public static long toLong(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue();
    }

    public static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).floatValue();
    }

    public static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue();
    }

    public static double toDoubleZero(Object from, Converter converter, ConverterOptions options) {
        return 0.0d;
    }

    public static BigDecimal numberToBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(((Number) from).longValue());
    }

    public static boolean isIntTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue() != 0;
    }

    public static boolean isFloatTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue() != 0;
    }

    /**
     * @param number Number instance to convert to char.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    public static char numberToCharacter(Number number) {
        long value = number.longValue();
        if (value >= 0 && value <= Character.MAX_VALUE) {
            return (char) value;
        }
        throw new IllegalArgumentException("Value: " + value + " out of range to be converted to character.");
    }

    /**
     * @param from      - object that is a number to be converted to char
     * @param converter - instance of converter mappings to use.
     * @param options   - optional conversion options, not used here.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    public static char numberToCharacter(Object from, Converter converter, ConverterOptions options) {
        return numberToCharacter((Number) from);
    }
}

