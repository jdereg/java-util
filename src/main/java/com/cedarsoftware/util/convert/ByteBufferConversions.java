package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
final class ByteBufferConversions {

    private ByteBufferConversions() {}
    
    static CharBuffer toCharBuffer(Object from, Converter converter) {
        ByteBuffer buffer = toByteBuffer(from, converter);
        return converter.getOptions().getCharset().decode(buffer);
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter) {
        // Create a readonly buffer so we aren't changing
        // the original buffers mark and position when
        // working with this buffer.  This could be inefficient
        // if constantly fed with writeable buffers so should be documented
        return ((ByteBuffer) from).asReadOnlyBuffer();
    }

    static byte[] toByteArray(Object from, Converter converter) {
        ByteBuffer buffer = toByteBuffer(from, converter);

        if (buffer == null || !buffer.hasRemaining()) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    static String toString(Object from, Converter converter) {
        return toCharBuffer(from, converter).toString();
    }

    static char[] toCharArray(Object from, Converter converter) {
        return CharBufferConversions.toCharArray(toCharBuffer(from, converter), converter);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter) {
        return new StringBuffer(toCharBuffer(from, converter));
    }

    static StringBuilder toStringBuilder(Object from, Converter converter) {
        return new StringBuilder(toCharBuffer(from, converter));
    }
    
    static Map<?, ?> toMap(Object from, Converter converter) {
        ByteBuffer bytes = (ByteBuffer) from;

        // We'll store our final encoded string here
        String encoded;

        if (bytes.hasArray()) {
            // If the buffer is array-backed, we can avoid a copy by using the array offset/length
            int offset = bytes.arrayOffset() + bytes.position();
            int length = bytes.remaining();

            // Java 11+ supports an encodeToString overload with offset/length
            // encoded = Base64.getEncoder().encodeToString(bytes.array(), offset, length);

            // Make a minimal copy of exactly the slice
            byte[] slice = new byte[length];
            System.arraycopy(bytes.array(), offset, slice, 0, length);

            encoded = Base64.getEncoder().encodeToString(slice);
        } else {
            // Otherwise, we have to copy
            // Save the current position so we can restore it later
            int originalPosition = bytes.position();
            try {
                byte[] tmp = new byte[bytes.remaining()];
                bytes.get(tmp);
                encoded = Base64.getEncoder().encodeToString(tmp);
            } finally {
                // Restore the original position to avoid side-effects
                bytes.position(originalPosition);
            }
        }


        Map<String, Object> map = new LinkedHashMap<>();
        map.put(VALUE, encoded);
        return map;
    }
}
