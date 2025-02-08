package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;

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
final class YearConversions {
    private YearConversions() {}

    static long toLong(Object from, Converter converter) {
        return toInt(from, converter);
    }

    static short toShort(Object from, Converter converter) {
        return (short) toInt(from, converter);
    }

    static int toInt(Object from, Converter converter) {
        return ((Year) from).getValue();
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new AtomicInteger(toInt(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toInt(from, converter));
    }

    static double toDouble(Object from, Converter converter) {
        return toInt(from, converter);
    }

    static float toFloat(Object from, Converter converter) {
        return toInt(from, converter);
    }
    
    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new AtomicBoolean(toInt(from, converter) != 0);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(toInt(from, converter));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toInt(from, converter));
    }

    static String toString(Object from, Converter converter) {
        return ((Year)from).toString();
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Year year = (Year) from;
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(YEAR, year.getValue());
        return map;
    }
}
