package com.cedarsoftware.util.convert;

public class CharacterArrayConversions {

    static StringBuilder toStringBuilder(Object from) {
        Character[] chars = (Character[]) from;
        StringBuilder builder = new StringBuilder(chars.length);
        for (Character ch : chars) {
            builder.append(ch);
        }
        return builder;
    }

    static StringBuffer toStringBuffer(Object from) {
        Character[] chars = (Character[]) from;
        StringBuffer buffer = new StringBuffer(chars.length);
        for (Character ch : chars) {
            buffer.append(ch);
        }
        return buffer;
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return toStringBuilder(from).toString();
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return toStringBuilder(from);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return toStringBuffer(from);
    }

}
