package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

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
final class CharacterConversions {

    private CharacterConversions() {}
    
    static String toString(Object from, Converter converter) {
        return "" + from;
    }

    static boolean toBoolean(Object from, Converter converter) {
        char c = (char) from;
        return (c == 1) || (c == 't') || (c == 'T') || (c == '1') || (c == 'y') || (c == 'Y');
    }

    // down casting -- not always a safe conversion
    static byte toByte(Object from, Converter converter) {
        return (byte) (char) from;
    }

    static short toShort(Object from, Converter converter) {
        return (short) (char) from;
    }

    static int toInt(Object from, Converter converter) {
        return (char) from;
    }

    static long toLong(Object from, Converter converter) {
        return (char) from;
    }

    static float toFloat(Object from, Converter converter) {
        return (char) from;
    }

    static double toDouble(Object from, Converter converter) {
        return (char) from;
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf((char) from);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf((char) from);
    }
}
