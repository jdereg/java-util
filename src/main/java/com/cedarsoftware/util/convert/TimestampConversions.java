package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.sql.Timestamp;

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
        Timestamp timestamp = (Timestamp) from;
        long timeInMilliseconds = timestamp.getTime();
        int nanoseconds = timestamp.getNanos();
        // Subtract the milliseconds part of the nanoseconds to avoid double counting
        double additionalNanos = nanoseconds % 1_000_000 / 1_000_000.0;
        return timeInMilliseconds + additionalNanos;
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        long epochMillis = timestamp.getTime();

        // Get nanoseconds part (fraction of the current millisecond)
        int nanoPart = timestamp.getNanos() % 1_000_000;

        // Convert time to fractional milliseconds
        return BigDecimal.valueOf(epochMillis).add(BigDecimal.valueOf(nanoPart, 6)); // Dividing by 1_000_000 with scale 6
    }
}
