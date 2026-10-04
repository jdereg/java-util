package com.cedarsoftware.util.convert;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * String to TimeZone reads a zone ID, or an offset that ZoneId reads.
 */
class ConverterTimeZoneTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * An offset such as "+05:00", or "Z", gave GMT (offset 0): TimeZone.getTimeZone answers GMT for any ID it does not
     * know, and it does not read a bare offset. An ID neither TimeZone nor ZoneId knows, in any case, is refused
     * (ConverterZoneIdCaseTest).
     */
    @Test
    void anOffsetIsReadAsThatOffset() {
        assertEquals(5 * 3_600_000, converter.convert("+05:00", TimeZone.class).getRawOffset());
        assertEquals(-(3 * 3_600_000 + 1_800_000), converter.convert("-03:30", TimeZone.class).getRawOffset());
        assertEquals(0, converter.convert("Z", TimeZone.class).getRawOffset());
        assertEquals("America/New_York", converter.convert("America/New_York", TimeZone.class).getID());
        assertEquals("GMT", converter.convert("GMT", TimeZone.class).getID());
        assertEquals("UTC", converter.convert("UTC", TimeZone.class).getID());
        assertEquals("GMT+05:00", converter.convert("GMT+5", TimeZone.class).getID());
        assertEquals("EST", converter.convert("EST", TimeZone.class).getID());
        assertThrows(IllegalArgumentException.class, () -> converter.convert("Amercia/New_York", TimeZone.class));
    }
}
