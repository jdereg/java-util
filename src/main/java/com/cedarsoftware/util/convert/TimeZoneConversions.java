package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.util.TimeZone;

public class TimeZoneConversions {
    static String toString(Object from, Converter converter) {
        TimeZone timezone = (TimeZone)from;
        return timezone.getID();
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        TimeZone tz = (TimeZone) from;
        return tz.toZoneId();
    }
}
