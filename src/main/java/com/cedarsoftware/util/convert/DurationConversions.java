package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.DURATION;

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
final class DurationConversions {
    
    // Feature option constants for Duration precision control
    public static final String DURATION_LONG_PRECISION = "duration.long.precision";

    private DurationConversions() {}

    static Map toMap(Object from, Converter converter) {
        Duration duration = (Duration) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(DURATION, duration.toString());
        return target;
    }

    static long toLong(Object from, Converter converter) {
        Duration duration = (Duration) from;
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter." + DURATION_LONG_PRECISION);
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption(DURATION_LONG_PRECISION);
        }
        
        // Default to milliseconds if no override specified
        if (Converter.PRECISION_NANOS.equals(precision)) {
            return duration.toNanos();
        } else {
            return duration.toMillis(); // Default: milliseconds
        }
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }
    
    static BigInteger toBigInteger(Object from, Converter converter) {
        Duration duration = (Duration) from;
        BigInteger epochSeconds = BigInteger.valueOf(duration.getSeconds());
        BigInteger nanos = BigInteger.valueOf(duration.getNano());

        // Convert seconds to nanoseconds and add the nanosecond part
        return epochSeconds.multiply(BigIntegerConversions.BILLION).add(nanos);
    }

    static double toDouble(Object from, Converter converter) {
        Duration duration = (Duration) from;
        return BigDecimalConversions.secondsAndNanosToDouble(duration.getSeconds(), duration.getNano()).doubleValue();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Duration duration = (Duration) from;
        return BigDecimalConversions.secondsAndNanosToDouble(duration.getSeconds(), duration.getNano());
    }
    
    static Timestamp toTimestamp(Object from, Converter converter) {
        Duration duration = (Duration) from;
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return Timestamp.from(timeAfterDuration);
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter) {
        Duration duration = (Duration) from;
        
        // Add duration to epoch to get the target instant
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        
        // Convert to LocalDate in UTC to get day boundary alignment
        // This ensures the result is always at 00:00:00 (start of day)
        LocalDate localDate = timeAfterDuration.atOffset(ZoneOffset.UTC).toLocalDate();
        
        // Convert back to java.sql.Date
        return java.sql.Date.valueOf(localDate);
    }
}
