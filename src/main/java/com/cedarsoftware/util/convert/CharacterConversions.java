package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CharacterConversions {

    private CharacterConversions() {
    }

    static boolean toBoolean(Object from) {
        char c = (char) from;
        return (c == 1) || (c == 't') || (c == 'T') || (c == '1') || (c == 'y') || (c == 'Y');
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return "" + from;
    }

    static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        return toBoolean(from);
    }

    // downcasting -- not always a safe conversino
    static byte toByte(Object from, Converter converter, ConverterOptions options) {
        return (byte) (char) from;
    }

    static short toShort(Object from, Converter converter, ConverterOptions options) {
        return (short) (char) from;
    }

    static int toInt(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        return new AtomicInteger((char) from);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong((char) from);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        return new AtomicBoolean(toBoolean(from));
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf((char) from);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf((char) from);
    }
}
