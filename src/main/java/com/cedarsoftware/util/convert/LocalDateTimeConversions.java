package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
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
final class LocalDateTimeConversions {

    private LocalDateTimeConversions() {}

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        LocalDateTime ldt = (LocalDateTime) from;
        return ZonedDateTime.of(ldt, converter.getOptions().getZoneId());
    }

    static String toString(Object from, Converter converter) {
        LocalDateTime localDateTime = (LocalDateTime) from;
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        LocalDateTime localDateTime = (LocalDateTime) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.LOCAL_DATE_TIME, localDateTime.toString());
        return target;
    }
}
