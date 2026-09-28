package com.cedarsoftware.util.convert;

import java.sql.Time;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversions from {@link java.sql.Time}. A Time is JDBC's time of day: its date part is 1970-01-01, and it reads its
 * hours, minutes and seconds in the JVM's time zone, as {@link Time#toString()} and {@link Time#valueOf(LocalTime)}
 * do. Otherwise it is a {@link java.util.Date} and converts as one (to Long, its epoch milliseconds).
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

    /** HH:mm:ss, as Time.toString() writes it, followed by .SSS when there are milliseconds. */
    static String toString(Object from, Converter converter) {
        Time time = (Time) from;
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
