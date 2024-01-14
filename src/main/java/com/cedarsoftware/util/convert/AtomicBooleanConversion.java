package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicBooleanConversion {

    static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    static Short toShort(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    static Integer toInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get();
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicInteger(1) : new AtomicInteger (0);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicLong(1) : new AtomicLong(0);
    }

    static Character toCharacter(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.CHARACTER_ONE : CommonValues.CHARACTER_ZERO;
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}
