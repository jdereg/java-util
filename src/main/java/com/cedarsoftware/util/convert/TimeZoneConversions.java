package com.cedarsoftware.util.convert;

import java.util.TimeZone;

public class TimeZoneConversions {
    static String toString(Object from, Converter converter) {
        TimeZone timezone = (TimeZone)from;
        return timezone.getID();
    }

}
