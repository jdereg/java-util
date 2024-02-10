package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

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
}
