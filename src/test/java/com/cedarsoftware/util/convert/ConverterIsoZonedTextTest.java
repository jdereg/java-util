package com.cedarsoftware.util.convert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cedarsoftware.util.DateUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ISO-8601 text with an offset -- what {@code ZonedDateTime.toString()} and the Converter write -- reads back in the zone
 * it names, as {@code ZonedDateTime.parse} reads it: {@code ..+05:00} in the offset +05:00, {@code ..Z[GMT]} in GMT.
 * DateUtilities gave those as GMT+05:00 and Etc/GMT, the same instant in another zone, so the Converter's own text did
 * not read back equal (nor did json-io's, outside a typed field). All other text is read by DateUtilities, as before.
 */
class ConverterIsoZonedTextTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final ZoneId[] ZONES = {ZoneOffset.UTC, ZoneOffset.of("+05:00"), ZoneOffset.of("-03:30"),
            ZoneOffset.of("+05:45:30"), ZoneId.of("UTC"), ZoneId.of("GMT"), ZoneId.of("UT"), ZoneId.of("Etc/GMT"),
            ZoneId.of("GMT+05:00"), ZoneId.of("UTC-03:00"), ZoneId.of("America/New_York"), ZoneId.of("Asia/Kolkata"),
            ZoneId.of("Europe/Berlin")};

    /** Each zone, in winter and summer, with and without a fraction of a second, from either writer's text. */
    @Test
    void theTextReadsBackInTheZoneItNames() {
        List<String> wrong = new ArrayList<>();
        for (ZoneId zone : ZONES) {
            for (ZonedDateTime at : new ZonedDateTime[] {ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, zone),
                    ZonedDateTime.of(2026, 7, 15, 10, 30, 15, 123_000_000, zone)}) {
                for (String text : new String[] {converter.convert(at, String.class), at.toString()}) {
                    ZonedDateTime read = converter.convert(text, ZonedDateTime.class);
                    if (!at.equals(read)) {
                        wrong.add(text + " -> " + read);
                    }
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
        assertEquals(ZoneOffset.of("+05:00"), read("2026-01-15T10:00:00+05:00").getZone());
        assertEquals(ZoneId.of("GMT"), read("2026-01-15T10:00:00Z[GMT]").getZone());
        assertEquals(ZoneId.of("GMT"), read(" 2026-01-15T10:00:00Z[GMT]\n").getZone());
    }

    /** Both hours of a repeated hour keep their offset, on every JDK (JDK 8's ZonedDateTime.parse gives the first). */
    @Test
    void theRepeatedHourKeepsItsOffset() {
        LocalDateTime local = LocalDateTime.of(2024, 10, 27, 2, 30);
        for (String offset : new String[] {"+02:00", "+01:00"}) {
            assertEquals(ZonedDateTime.ofStrict(local, ZoneOffset.of(offset), ZoneId.of("Europe/Berlin")),
                    read("2024-10-27T02:30:00" + offset + "[Europe/Berlin]"), offset);
        }
    }

    /**
     * Other text is read by DateUtilities: a trailing Z, no offset, an offset without its colon, a zone name after a
     * space, other date forms, and an offset the zone does not have at that time (read at the offset, shown in the zone).
     */
    @Test
    void otherTextIsReadAsBefore() {
        String[] texts = {"2026-01-15T10:00:00Z", "2026-01-15T10:00:00.5Z", "2026-01-15T10:00:00",
                "2026-01-15T10:00", "2026-01-15 10:00:00+05:00", "2026-01-15T10:00:00-0500", "2026-01-15T10:00:00 EST",
                "2026-01-15T10:00:00 America/New_York", "Jan 15, 2026 10:00", "2026-01-15",
                "2024-07-01T12:00:00+05:00[Europe/Berlin]", "1880-01-01T12:00:00+00:53[Europe/Berlin]"};
        for (String text : texts) {
            assertEquals(DateUtilities.parseDate(text, converter.getOptions().getZoneId(), true), read(text), text);
        }
        assertEquals(ZoneOffset.UTC, read("2026-01-15T10:00:00Z").getZone());
    }

    private ZonedDateTime read(String text) {
        return converter.convert(text, ZonedDateTime.class);
    }
}
