package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;

public class BooleanConversion {
    public static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    public static Short toShort(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    public static Integer toInteger(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }


    public static Long toLong(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Byte atomicToByte(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    public static Short atomicToShort(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    public static Integer atomicToInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    public static Long atomicToLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Long atomicToCharacter(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    public static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    public static Float atomicToFloat(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    public static Double atomicToDouble(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }


}
