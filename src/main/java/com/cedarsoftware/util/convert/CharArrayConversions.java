package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

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
final class CharArrayConversions {

    private CharArrayConversions() {}
    
    static ByteBuffer toByteBuffer(Object from, Converter converter) {
        return converter.getOptions().getCharset().encode(toCharBuffer(from, converter));
    }
    
    static String toString(Object from, Converter converter) {
        char[] chars = (char[]) from;
        return new String(chars);
    }

    static CharBuffer toCharBuffer(Object from, Converter converter) {
        char[] chars = (char[]) from;
        return CharBuffer.wrap(chars);
    }

    static char[] toCharArray(Object from, Converter converter) {
        char[] chars = (char[])from;
        return Arrays.copyOf(chars, chars.length);
    }
}
