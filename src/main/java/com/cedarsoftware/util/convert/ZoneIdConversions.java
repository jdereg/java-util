package com.cedarsoftware.util.convert;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

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
final class ZoneIdConversions {

    private ZoneIdConversions() {}

    static Map<String, Object> toMap(Object from, Converter converter) {
        ZoneId zoneID = (ZoneId) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("zone", zoneID.toString());
        return target;
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        return timeZone((ZoneId) from);
    }

    /**
     * The TimeZone of a ZoneId, as JDK 9+ makes it, on every JDK: JDK 8's TimeZone.getTimeZone(ZoneId) does not read
     * the prefixes "UTC" and "UT" before an offset, and gave GMT for "UTC+05:00".
     */
    static TimeZone timeZone(ZoneId zoneId) {
        String id = zoneId.getId();
        if (id.startsWith("UT") && !"UTC".equals(id)) {
            return TimeZone.getTimeZone("GMT" + id.substring(id.startsWith("UTC") ? 3 : 2));
        }
        return TimeZone.getTimeZone(zoneId);
    }

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        ZoneId zoneId = (ZoneId) from;
        return zoneId.getRules().getOffset(Instant.now());
    }
}
