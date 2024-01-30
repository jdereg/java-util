package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public final class CharArrayConversions {

    private CharArrayConversions() {}

    static String toString(Object from) {
        char[] chars = (char[])from;
        return new String(chars);
    }

    static CharBuffer toCharBuffer(Object from) {
        char[] chars = (char[])from;
        return CharBuffer.wrap(chars);
    }

    static ByteBuffer toByteBuffer(Object from, ConverterOptions options) {
        return options.getCharset().encode(toCharBuffer(from));
    }


    static String toString(Object from, Converter converter, ConverterOptions options) {
        return toString(from);
    }

    static CharBuffer toCharBuffer(Object from, Converter converter, ConverterOptions options) {
        return toCharBuffer(from);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return new StringBuffer(toCharBuffer(from));
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return new StringBuilder(toCharBuffer(from));
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter, ConverterOptions options) {
        return toByteBuffer(from, options);
    }

    static byte[] toByteArray(Object from, Converter converter, ConverterOptions options) {
        ByteBuffer buffer = toByteBuffer(from, options);
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return byteArray;
    }

    static char[] toCharArray(Object from, Converter converter, ConverterOptions options) {
        char[] chars = (char[])from;
        if (chars == null) {
            return null;
        }
        return Arrays.copyOf(chars, chars.length);
    }
}
