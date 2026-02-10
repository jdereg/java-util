package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

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
final class OffsetTimeConversions {
    private OffsetTimeConversions() {}

    static LocalTime toLocalTime(Object from, Converter converter) {
        return ((OffsetTime) from).toLocalTime();
    }

    static String toString(Object from, Converter converter) {
        OffsetTime offsetTime = (OffsetTime) from;
        return offsetTime.format(DateTimeFormatter.ISO_OFFSET_TIME);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        OffsetTime ot = (OffsetTime) from;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.OFFSET_TIME, ot.toString());
        return map;
    }

    static long toLong(Object from, Converter converter) {
        OffsetTime ot = (OffsetTime) from;
        return ot.atDate(LocalDate.of(1970, 1, 1))
                .toInstant()
                .toEpochMilli();
    }

    static double toDouble(Object from, Converter converter) {
        OffsetTime ot = (OffsetTime) from;
        Instant epoch = getEpoch(ot);
        return epoch.getEpochSecond() + (epoch.getNano() / 1_000_000_000.0);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        OffsetTime ot = (OffsetTime) from;
        Instant epoch = getEpoch(ot);
        return BigInteger.valueOf(epoch.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(epoch.getNano()));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        OffsetTime ot = (OffsetTime) from;
        Instant epoch = getEpoch(ot);
        BigDecimal seconds = BigDecimal.valueOf(epoch.getEpochSecond());
        BigDecimal nanos = BigDecimal.valueOf(epoch.getNano())
                .divide(BigDecimalConversions.BILLION);
        return seconds.add(nanos);
    }

    private static Instant getEpoch(OffsetTime ot) {
        return ot.atDate(LocalDate.of(1970, 1, 1)).toInstant();
    }
}