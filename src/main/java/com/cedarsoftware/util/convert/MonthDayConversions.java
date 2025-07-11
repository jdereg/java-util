package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.MONTH_DAY;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
final class MonthDayConversions {

    private MonthDayConversions() {}

    static Map toMap(Object from, Converter converter) {
        MonthDay monthDay = (MonthDay) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MONTH_DAY, monthDay.toString());  // MonthDay.toString() already uses --MM-dd format
        return target;
    }

    /**
     * Convert MonthDay to int in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.
     */
    static int toInt(Object from, Converter converter) {
        MonthDay monthDay = (MonthDay) from;
        return monthDay.getMonthValue() * 100 + monthDay.getDayOfMonth();
    }

    /**
     * Convert MonthDay to Integer in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.
     */
    static Integer toInteger(Object from, Converter converter) {
        return toInt(from, converter);
    }

    /**
     * Convert MonthDay to short in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.
     */
    static short toShort(Object from, Converter converter) {
        return (short) toInt(from, converter);
    }

    /**
     * Convert MonthDay to Long in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.
     */
    static Long toLong(Object from, Converter converter) {
        return (long) toInt(from, converter);
    }

    /**
     * Convert MonthDay to Double in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.0.
     */
    static Double toDouble(Object from, Converter converter) {
        return (double) toInt(from, converter);
    }

    /**
     * Convert MonthDay to Float in MMDD format.
     * For example, MonthDay.of(12, 25) becomes 1225.0f.
     */
    static Float toFloat(Object from, Converter converter) {
        return (float) toInt(from, converter);
    }

    /**
     * Convert MonthDay to BigInteger in MMDD format.
     * For example, MonthDay.of(12, 25) becomes BigInteger.valueOf(1225).
     */
    static java.math.BigInteger toBigInteger(Object from, Converter converter) {
        return java.math.BigInteger.valueOf(toInt(from, converter));
    }

    /**
     * Convert MonthDay to BigDecimal in MMDD format.
     * For example, MonthDay.of(12, 25) becomes BigDecimal.valueOf(1225).
     */
    static java.math.BigDecimal toBigDecimal(Object from, Converter converter) {
        return java.math.BigDecimal.valueOf(toInt(from, converter));
    }

    /**
     * Convert MonthDay to boolean.
     * All MonthDay values are true (since they represent valid dates).
     */
    static boolean toBoolean(Object from, Converter converter) {
        return toInt(from, converter) != 0;  // Should always be true for valid MonthDay
    }

    /**
     * Convert MonthDay to Byte in MMDD format.
     * For example, MonthDay.of(1, 1) becomes (byte) 101.
     */
    static Byte toByte(Object from, Converter converter) {
        return (byte) toInt(from, converter);
    }

    /**
     * Convert MonthDay to AtomicInteger in MMDD format.
     */
    static java.util.concurrent.atomic.AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicInteger(toInt(from, converter));
    }

    /**
     * Convert MonthDay to AtomicLong in MMDD format.
     */
    static java.util.concurrent.atomic.AtomicLong toAtomicLong(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicLong(toInt(from, converter));
    }

    /**
     * Convert MonthDay to AtomicBoolean.
     * All MonthDay values are true (since they represent valid dates).
     */
    static java.util.concurrent.atomic.AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicBoolean(toBoolean(from, converter));
    }
}
