package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import com.cedarsoftware.util.CompactLinkedMap;

import static com.cedarsoftware.util.convert.MapConversions.ZONE;

public class TimeZoneConversions {
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
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put(ZONE, tz.getID());
        return target;
    }
}
