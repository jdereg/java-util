package com.cedarsoftware.util.convert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Universal conversion bridges that can handle multiple types through common patterns.
 * This class implements the bridge pattern to reduce code duplication while maintaining
 * full conversion functionality.
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
final class UniversalConversions {

    private UniversalConversions() {}

    /**
     * Universal toString bridge for any object that has a meaningful toString() implementation.
     * This replaces dozens of individual toString() conversions with a single bridge.
     * 
     * For objects that need specialized string formatting, use their dedicated conversion class.
     * This bridge is suitable for:
     * - Primitive wrappers (Boolean, Integer, Long, etc.)
     * - Atomic types (AtomicBoolean, AtomicInteger, AtomicLong)
     * - Simple value objects (UUID, BigInteger, BigDecimal)
     * - Time types that have ISO-8601 toString() (Duration, Period, etc.)
     */
    static String toString(Object from, Converter converter) {
        if (from == null) {
            return null;
        }
        return from.toString();
    }

    /**
     * Universal toMap bridge for simple value objects.
     * Creates a Map with a single "_v" key containing the object.
     * This replaces dozens of individual MapConversions::initMap calls.
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.V, from);
        return target;
    }

    // ========================================
    // Atomic → Primitive Bridge Methods
    // ========================================

    /**
     * Universal bridge: AtomicInteger → primitive int.
     * Extracts the int value from AtomicInteger and uses it for further conversion.
     */
    static int atomicIntegerToInt(Object from, Converter converter) {
        AtomicInteger atomic = (AtomicInteger) from;
        return atomic.get();
    }

    /**
     * Universal bridge: AtomicLong → primitive long.
     * Extracts the long value from AtomicLong and uses it for further conversion.
     */
    static long atomicLongToLong(Object from, Converter converter) {
        AtomicLong atomic = (AtomicLong) from;
        return atomic.get();
    }

    /**
     * Universal bridge: AtomicBoolean → primitive boolean.
     * Extracts the boolean value from AtomicBoolean and uses it for further conversion.
     */
    static boolean atomicBooleanToBoolean(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return atomic.get();
    }
}