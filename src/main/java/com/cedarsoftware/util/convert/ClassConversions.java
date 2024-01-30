package com.cedarsoftware.util.convert;

public final class ClassConversions {

    private ClassConversions() {}

    static String toString(Object from, Converter converter, ConverterOptions options) {
        Class<?> cls = (Class<?>) from;
        return cls.getName();
    }
}
