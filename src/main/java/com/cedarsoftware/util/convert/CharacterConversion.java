package com.cedarsoftware.util.convert;

public class CharacterConversion {
    static boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        Character c = (Character) from;
        return c != CommonValues.CHARACTER_ZERO;
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }

    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return (char) from;
    }
}
