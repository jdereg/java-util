package com.cedarsoftware.util.convert;

public class ClassConversions {
    static String toString(Object from, Converter converter, ConverterOptions options) {
        Class<?> cls = (Class<?>) from;
        return cls.getName();
    }
}
