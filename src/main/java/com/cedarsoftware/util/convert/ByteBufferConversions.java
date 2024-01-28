package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.StringUtilities;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

public class ByteBufferConversions {

    static ByteBuffer asReadOnlyBuffer(Object from) {
        // Create a readonly buffer so we aren't changing
        // the original buffers mark and position when
        // working with this buffer.  This could be inefficient
        // if constantly fed with writeable buffers so should be documented
        return ((ByteBuffer)from).asReadOnlyBuffer();
    }

    static byte[] toByteArray(Object from) {
        ByteBuffer buffer = asReadOnlyBuffer(from);

        if (buffer == null || !buffer.hasRemaining()) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    static CharBuffer toCharBuffer(Object from, ConverterOptions options) {
        ByteBuffer buffer = asReadOnlyBuffer(from);
        return options.getCharset().decode(buffer);
    }


    static CharBuffer toCharBuffer(Object from, Converter converter, ConverterOptions options) {
        return toCharBuffer(from, options);
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter, ConverterOptions options) {
        return asReadOnlyBuffer(from);
    }

    static byte[] toByteArray(Object from, Converter converter, ConverterOptions options) {
        return toByteArray(from);
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return toCharBuffer(from, options).toString();
    }

    static char[] toCharArray(Object from, Converter converter, ConverterOptions options) {
        return CharBufferConversions.toCharArray(toCharBuffer(from, options));
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return new StringBuffer(toCharBuffer(from, options));
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return new StringBuilder(toCharBuffer(from, options));
    }

}
