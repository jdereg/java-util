package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.LOCAL_TIME, localTime.toString());
        return target;
    }

    static int toInteger(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return (int) lt.toNanoOfDay(); // Return nanoseconds for modern time class precision consistency
    }

    static long toLong(Object from, Converter converter) {
        LocalTime lt = (LocalTime) from;
        return lt.toNanoOfDay(); // Return nanoseconds for modern time class precision consistency
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
        return new AtomicInteger(toInteger(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static String toString(Object from, Converter converter) {
        LocalTime localTime = (LocalTime) from;
        return localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    static Calendar toCalendar(Object from, Converter converter) {
        LocalTime localTime = (LocalTime) from;
        // Obtain the current date in the specified TimeZone
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());

        // Set the calendar instance to have the same time as the LocalTime passed in
        cal.set(Calendar.HOUR_OF_DAY, localTime.getHour());
        cal.set(Calendar.MINUTE, localTime.getMinute());
        cal.set(Calendar.SECOND, localTime.getSecond());
        cal.set(Calendar.MILLISECOND, localTime.getNano() / 1_000_000); // Convert nanoseconds to milliseconds
        return cal;
    }
}
