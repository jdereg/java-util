package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
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
final class CharBufferConversions {

    private CharBufferConversions() {}

    static CharBuffer toCharBuffer(Object from, Converter converter) {
        // Create a readonly buffer, so we aren't changing
        // the original buffers mark and position when
        // working with this buffer.  This could be inefficient
        // if constantly fed with writeable buffers so should be documented
        return ((CharBuffer) from).asReadOnlyBuffer();
    }

    static byte[] toByteArray(Object from, Converter converter) {
        return ByteBufferConversions.toByteArray(toByteBuffer(from, converter), converter);
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter) {
        return converter.getOptions().getCharset().encode(toCharBuffer(from, converter));
    }

    static String toString(Object from, Converter converter) {
        return toCharBuffer(from, converter).toString();
    }

    static char[] toCharArray(Object from, Converter converter) {
        CharBuffer buffer = toCharBuffer(from, converter);

        if (!buffer.hasRemaining()) {
            return EMPTY_CHAR_ARRAY;
        }

        char[] chars = new char[buffer.remaining()];
        buffer.get(chars);
        return chars;
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(VALUE, toString(from, converter));
        return map;
    }
}
