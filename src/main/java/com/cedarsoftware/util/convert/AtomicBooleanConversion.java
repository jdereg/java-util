package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicBooleanConversion {

    public static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    public static Short toShort(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    public static Integer toInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    public static Long toLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    public static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    public static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get();
    }

    public static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicInteger(1) : new AtomicInteger (0);
    }

    public static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicLong(1) : new AtomicLong(0);
    }

    public static Character toCharacter(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.CHARACTER_ONE : CommonValues.CHARACTER_ZERO;
    }

    public static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}
