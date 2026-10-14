package com.cedarsoftware.util.convert;

import java.sql.Time;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Conversions from {@link java.sql.Time}. A Time is JDBC's time of day: its date part is 1970-01-01, and it reads its
 * hours, minutes and seconds in the JVM's time zone, as {@link Time#toString()} and {@link Time#valueOf(LocalTime)}
 * do. Otherwise it is a {@link java.util.Date} and converts as one (to Long, its epoch milliseconds). A Time that
 * holds another date ({@code new Time(millis)} takes any instant) keeps it through every conversion.
 *
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
public final class SqlTimeConversions {

    private static final long MILLIS_PER_DAY = 86_400_000L;

    private SqlTimeConversions() {
    }

    private static int millisOf(Time time) {
        return (int) Math.floorMod(time.getTime(), 1000L);
    }

    /** The time of day, milliseconds included (Time.toLocalTime() drops them). */
    static LocalTime toLocalTime(Object from, Converter converter) {
        Time time = (Time) from;
        return time.toLocalTime().withNano(millisOf(time) * 1_000_000);
    }

    /**
     * True when the Time is on 1970-01-01 in the JVM's zone: a time of day, as Time.valueOf() and a JDBC driver make
     * it. A Time is a Date, though, and can hold any instant ({@code new Time(millis)}).
     */
    private static boolean isTimeOfDay(Time time) {
        long millis = time.getTime();
        long local = millis + TimeZone.getDefault().getOffset(millis);
        return local >= 0 && local < MILLIS_PER_DAY;
    }

    /**
     * A time of day is HH:mm:ss, as Time.toString() writes it, followed by .SSS when there are milliseconds. A Time on
     * any other day is written as the instant it is, like a Date, so that its date is not lost. String -> Time reads
     * both forms back to the same Time.
     */
    static String toString(Object from, Converter converter) {
        Time time = (Time) from;
        if (!isTimeOfDay(time)) {
            return DateConversions.toString(from, converter);
        }
        int millis = millisOf(time);
        if (millis == 0) {
            return time.toString();
        }
        StringBuilder text = new StringBuilder(12).append(time).append('.');
        if (millis < 100) {
            text.append('0');
        }
        if (millis < 10) {
            text.append('0');
        }
        return text.append(millis).toString();
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.SQL_TIME, toString(from, converter));
        return map;
    }

    static Time toSqlTime(Object from, Converter converter) {
        return new Time(((Time) from).getTime());
    }
}
