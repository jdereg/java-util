package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;

public class CharBufferConversions {
    static CharBuffer asReadOnlyBuffer(Object from) {
        // Create a readonly buffer so we aren't changing
        // the original buffers mark and position when
        // working with this buffer.  This could be inefficient
        // if constantly fed with writeable buffers so should be documented
        return ((CharBuffer)from).asReadOnlyBuffer();
    }

    static char[] toCharArray(Object from) {
        CharBuffer buffer = asReadOnlyBuffer(from);

        if (buffer == null || !buffer.hasRemaining()) {
            return EMPTY_CHAR_ARRAY;
        }

        char[] chars = new char[buffer.remaining()];
        buffer.get(chars);
        return chars;
    }

    static CharBuffer toCharBuffer(Object from, Converter converter, ConverterOptions options) {
        return asReadOnlyBuffer(from);
    }

    static byte[] toByteArray(Object from, Converter converter, ConverterOptions options) {
        return ByteBufferConversions.toByteArray(toByteBuffer(from, converter, options));
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter, ConverterOptions options) {
        return options.getCharset().encode(asReadOnlyBuffer(from));
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return asReadOnlyBuffer(from).toString();
    }

    static char[] toCharArray(Object from, Converter converter, ConverterOptions options) {
        return toCharArray(from);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return new StringBuffer(asReadOnlyBuffer(from));
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return new StringBuilder(asReadOnlyBuffer(from));
    }
}
