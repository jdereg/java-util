package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.StringUtilities;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteArrayConversions {

    static String toString(Object from, ConverterOptions options) {
        byte[] bytes = (byte[])from;
        return (bytes == null) ? StringUtilities.EMPTY : new String(bytes, options.getCharset());
    }

    static ByteBuffer toByteBuffer(Object from) {
        return ByteBuffer.wrap((byte[])from);
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return toString(from, options);
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter, ConverterOptions options) {
        return toByteBuffer(from);
    }

    static CharBuffer toCharBuffer(Object from, Converter converter, ConverterOptions options) {
        return CharBuffer.wrap(toString(from, options));
    }

    static char[] toCharArray(Object from, Converter converter, ConverterOptions options) {
        return toString(from, options).toCharArray();
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return new StringBuffer(toString(from, options));
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return new StringBuilder(toString(from, options));
    }

}
