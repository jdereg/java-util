package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.ZONE;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Kenny Partlow (kpartlow@gmail.com)
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
final class TimeZoneConversions {
    static String toString(Object from, Converter converter) {
        TimeZone timezone = (TimeZone)from;
        return timezone.getID();
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        TimeZone tz = (TimeZone) from;
        return tz.toZoneId();
    }
    
    static Map<String, Object> toMap(Object from, Converter converter) {
        TimeZone tz = (TimeZone) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(ZONE, tz.getID());
        return target;
    }

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        TimeZone tz = (TimeZone) from;
        // Convert the raw offset (in milliseconds) to total seconds
        int offsetSeconds = tz.getRawOffset() / 1000;
        return ZoneOffset.ofTotalSeconds(offsetSeconds);
    }
}
