package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;

public class CharacterConversion {
    public static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        Character c = (Character) from;
        return c != CommonValues.CHARACTER_ZERO;
    }

    public static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    public static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }
}
