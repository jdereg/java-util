package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.CompactLinkedMap;

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
final class LocalTimeConversions {
    static final BigDecimal BILLION = BigDecimal.valueOf(1_000_000_000);

    private LocalTimeConversions() {}

    static Map<String, Object> toMap(Object from, Converter converter) {
        LocalTime localTime = (LocalTime) from;
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put("hour", localTime.getHour());
        target.put("minute", localTime.getMinute());
        if (localTime.getNano() != 0) {  // Only output 'nano' when not 0 (and then 'second' is required).
            target.put("nano", localTime.getNano());
            target.put("second", localTime.getSecond());
        } else {    // 0 nano, 'second' is optional if 0
            if (localTime.getSecond() != 0) {
                target.put("second", localTime.getSecond());
            }
        }
        return target;
    }

    static int toInteger(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return (int) (lt.toNanoOfDay() / 1_000_000); // Convert nanoseconds to milliseconds.
    }

    static long toLong(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return lt.toNanoOfDay() / 1_000_000; // Convert nanoseconds to milliseconds.
    }

    static double toDouble(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return lt.toNanoOfDay() / 1_000_000_000.0;
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return BigInteger.valueOf(lt.toNanoOfDay());
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return new BigDecimal(lt.toNanoOfDay()).divide(BILLION, 9, RoundingMode.HALF_UP);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new AtomicInteger((int)toLong(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static String toString(Object from, Converter converter) {
        LocalTime localTime = (LocalTime) from;
        return localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
