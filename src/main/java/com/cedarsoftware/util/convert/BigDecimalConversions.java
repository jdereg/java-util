package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

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
final class BigDecimalConversions {
    static Instant toInstant(Object from, Converter converter) {
        BigDecimal time = (BigDecimal) from;
        long seconds = time.longValue() / 1000;
        int nanos = time.remainder(BigDecimal.valueOf(1000)).multiply(BigDecimal.valueOf(1_000_000)).intValue();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return Timestamp.from(toInstant(from, converter));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return ((BigDecimal)from).toBigInteger();
    }

    static String toString(Object from, Converter converter) {
        return ((BigDecimal) from).stripTrailingZeros().toPlainString();
    }

    static UUID toUUID(Object from, Converter converter) {
        BigInteger bigInt = ((BigDecimal) from).toBigInteger();
        return BigIntegerConversions.toUUID(bigInt, converter);
    }
}
