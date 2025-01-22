package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import com.cedarsoftware.util.CompactMap;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;

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
final class ZoneOffsetConversions {

    private ZoneOffsetConversions() {
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        ZoneOffset offset = (ZoneOffset) from;
        Map<String, Object> target = CompactMap.<String, Object>builder().insertionOrder().build();
        int totalSeconds = offset.getTotalSeconds();

        // Calculate hours, minutes, and seconds
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        target.put(HOURS, hours);
        target.put(MINUTES, minutes);
        if (seconds != 0) {
            target.put(SECONDS, seconds);
        }
        return target;
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        return (ZoneId) from;
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        ZoneOffset offset = (ZoneOffset) from;
        // Ensure we create the TimeZone with the correct GMT offset format
        String id = offset.equals(ZoneOffset.UTC) ? "GMT" : "GMT" + offset.getId();
        return TimeZone.getTimeZone(id);
    }
}
