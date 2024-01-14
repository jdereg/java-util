package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Kenny Partlow (kpartlow@gmail.com)
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
public class BooleanConversion {
    public static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    public static Short toShort(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    public static Integer toInteger(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    public static Long toLong(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static AtomicBoolean numberToAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new AtomicBoolean(number.longValue() != 0);
    }

    public static Byte atomicToByte(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    public static Short atomicToShort(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    public static Integer atomicToInteger(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    public static Long atomicToLong(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Long atomicToCharacter(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    public static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    public static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        Boolean b = (Boolean) from;
        return b.booleanValue() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    public static Float atomicToFloat(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    public static Double atomicToDouble(Object from, Converter converter, ConverterOptions options) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }
}
