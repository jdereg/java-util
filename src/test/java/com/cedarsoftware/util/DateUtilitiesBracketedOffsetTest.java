package com.cedarsoftware.util;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DateUtilities reads a zone in brackets that is a bare offset -- 2026-01-15T10:00:00+05:00[+05:00] -- as java.time's
 * {@code appendZoneId()} writes a ZonedDateTime whose zone is a ZoneOffset, and as json-io wrote one (4.x): its
 * {@code {"zonedDateTime":"..+05:00[+05:00]"}} could not be read back ("other characters present: [+05:00]"). It reads
 * as the same text without the bracket does: zone GMT+05:00, the same instant.
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
class DateUtilitiesBracketedOffsetTest {
    private static final DateTimeFormatter WITH_ZONE_ID = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME).appendLiteral('[').appendZoneId().appendLiteral(']')
            .toFormatter();

    @Test
    void readAsTheOffsetWithoutTheBracket() {
        String[][] cases = {
                {"2026-01-15T10:00:00+05:00[+05:00]", "2026-01-15T10:00:00+05:00"},
                {"2026-01-15T10:00:00-03:30[-03:30]", "2026-01-15T10:00:00-03:30"},
                {"2026-01-15T10:00:00+05:00:30[+05:00:30]", "2026-01-15T10:00:00+05:00:30"},
                {"2026-01-15T10:00:00[+05:00]", "2026-01-15T10:00:00+05:00"},
                {"2026-01-15 10:00:00 [+05:00]", "2026-01-15T10:00:00+05:00"}};
        for (String[] c : cases) {
            ZonedDateTime bracketed = DateUtilities.parseDate(c[0], ZoneId.of("UTC"), true);
            assertEquals(DateUtilities.parseDate(c[1], ZoneId.of("UTC"), true), bracketed, c[0]);
        }
        ZonedDateTime read = DateUtilities.parseDate("2026-01-15T10:00:00+05:00[+05:00]", ZoneId.of("UTC"), true);
        assertEquals(ZoneId.of("GMT+05:00"), read.getZone());
        assertEquals(ZonedDateTime.parse("2026-01-15T10:00:00+05:00").toInstant(), read.toInstant());
    }

    /** java.time's own form for an offset zone reads back, through the Converter too. */
    @Test
    void javaTimesFormReadsBack() {
        Converter converter = new Converter(new DefaultConverterOptions());
        for (String offset : new String[] {"+05:00", "-03:30", "+05:45", "-12:00", "+14:00"}) {
            ZonedDateTime zdt = ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.of(offset));
            String text = WITH_ZONE_ID.format(zdt);
            ZonedDateTime back = converter.convert(text, ZonedDateTime.class);
            assertEquals(zdt.toInstant(), back.toInstant(), text);
            assertEquals(zdt.getOffset(), back.getOffset(), text);
        }
    }

    /** An offset that is no offset is refused, as it is without the bracket. */
    @Test
    void anOffsetThatIsNone() {
        assertThrows(DateTimeException.class,
                () -> DateUtilities.parseDate("2026-01-15T10:00:00[+25:00]", ZoneId.of("UTC"), true));
        assertThrows(DateTimeException.class,
                () -> DateUtilities.parseDate("2026-01-15T10:00:00+25:00", ZoneId.of("UTC"), true));
    }
}
