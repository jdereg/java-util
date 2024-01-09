package com.cedarsoftware.util.convert;

public class VoidConversion {

    private static final Byte ZERO = (byte) 0;

    public static Object toNull(Object from, Converter converter, ConverterOptions options) {
        return null;
    }

    public static Boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        return Boolean.FALSE;
    }

    public static Object toInt(Object from, Converter converter, ConverterOptions options) {
        return ZERO;
    }

}
