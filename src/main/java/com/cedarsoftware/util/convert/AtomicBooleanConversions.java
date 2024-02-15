package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
final class AtomicBooleanConversions {

    private AtomicBooleanConversions() {}

    static Byte toByte(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.BYTE_ONE : CommonValues.BYTE_ZERO;
    }

    static Short toShort(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.SHORT_ONE : CommonValues.SHORT_ZERO;
    }

    static Integer toInt(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.INTEGER_ONE : CommonValues.INTEGER_ZERO;
    }

    static Long toLong(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.LONG_ONE : CommonValues.LONG_ZERO;
    }

    static Float toFloat(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.FLOAT_ONE : CommonValues.FLOAT_ZERO;
    }

    static Double toDouble(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? CommonValues.DOUBLE_ONE : CommonValues.DOUBLE_ZERO;
    }

    static boolean toBoolean(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get();
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return new AtomicBoolean(b.get());
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicInteger(1) : new AtomicInteger (0);
    }


    static AtomicLong toAtomicLong(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? new AtomicLong(1) : new AtomicLong(0);
    }

    static Character toCharacter(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        ConverterOptions options = converter.getOptions();
        return b.get() ? options.trueChar() : options.falseChar();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    public static BigInteger toBigInteger(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get() ? BigInteger.ONE : BigInteger.ZERO;
    }
}
