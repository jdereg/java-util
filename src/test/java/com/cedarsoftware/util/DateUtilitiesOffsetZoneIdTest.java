package com.cedarsoftware.util;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DateUtilities reads a zone in brackets that is an offset with a prefix -- [UTC+05:00], [UT-03:00], [UTC+05:00:30] --
 * as java.time writes it for ZoneId.of("UTC+05:00"). Only [GMT+hh:mm] was read; [UTC+05:00] failed with "other
 * characters present", so the Converter could not read back the text it wrote for a ZonedDateTime in such a zone.
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
class DateUtilitiesOffsetZoneIdTest {

    private static ZonedDateTime parse(String text) {
        return DateUtilities.parseDate(text, ZoneId.of("UTC"), true);
    }

    @Test
    void anOffsetZoneWithAPrefixIsRead() {
        for (String zone : new String[] {"UTC+05:00", "UTC-03:00", "UT+05:00", "UT-11:30", "GMT+05:30",
                "UTC+05:00:30"}) {
            ZoneId id = ZoneId.of(zone);
            ZoneOffset offset = id.getRules().getOffset(java.time.Instant.EPOCH);
            String text = "2024-07-01T12:00" + offset.getId() + "[" + zone + "]";
            ZonedDateTime zdt = parse(text);
            assertEquals(id, zdt.getZone(), text);
            assertEquals(ZonedDateTime.of(2024, 7, 1, 12, 0, 0, 0, id), zdt, text);
        }
    }

    /** The Converter reads back what it writes for a ZonedDateTime in an offset zone. */
    @Test
    void theConverterRoundTripsAZonedDateTimeInAnOffsetZone() {
        Converter converter = new Converter(new DefaultConverterOptions());
        for (String zone : new String[] {"UTC+05:00", "UT-03:00", "UTC+05:00:30", "GMT+05:30"}) {
            ZonedDateTime zdt = ZonedDateTime.of(2024, 7, 1, 12, 34, 56, 789_000_000, ZoneId.of(zone));
            String text = converter.convert(zdt, String.class);
            assertEquals(zdt, converter.convert(text, ZonedDateTime.class), text);
        }
    }
}
