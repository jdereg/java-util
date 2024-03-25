package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

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
final class TimestampConversions {
    private TimestampConversions() {}

    static double toDouble(Object from, Converter converter) {
        Duration d = toDuration(from, converter);
        return BigDecimalConversions.secondsAndNanosToDouble(d.getSeconds(), d.getNano()).doubleValue();
    }
    
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return InstantConversions.toBigDecimal(instant, converter);
    }
    
    static BigInteger toBigInteger(Object from, Converter converter) {
        Duration duration = toDuration(from, converter);
        return DurationConversions.toBigInteger(duration, converter);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return timestamp.toInstant().atZone(converter.getOptions().getZoneId()).toLocalDateTime();
    }
    
    static Duration toDuration(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant timestampInstant = timestamp.toInstant();
        return Duration.between(Instant.EPOCH, timestampInstant);
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), converter.getOptions().getZoneId());
        return zdt.toOffsetDateTime();
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(timestamp.getTime());
        return cal;
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Date date = (Date) from;
        Map<String, Object> map = new CompactLinkedMap<>();
        OffsetDateTime odt = toOffsetDateTime(date, converter);
        map.put(MapConversions.DATE, odt.toLocalDate().toString());
        map.put(MapConversions.TIME, odt.toLocalTime().toString());
        map.put(MapConversions.ZONE, converter.getOptions().getZoneId().toString());
        map.put(MapConversions.EPOCH_MILLIS, date.getTime());
        map.put(MapConversions.NANOS, odt.getNano());
        return map;
    }
}
