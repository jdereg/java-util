package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The upgrade note at the top of 4.113.0 in changelog.md -- "what can change a result" -- one test per row, so the
 * note says what the code does. Each row's example is the one the note gives; 4.112.0 answered every one of them
 * differently (the note's "4.112.0" column).
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
class UpgradeTo4113NotesTest {

    private static final Converter CONVERTER = new Converter(new DefaultConverterOptions());

    private static <T> T convert(Object from, Class<T> to) {
        return CONVERTER.convert(from, to);
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void stringToByteArrayIsTheTextsBytes() {
        for (String text : new String[] {"SGVsbG8=", "CAFEBABE", "AF DE 92 9F", "[1, 2, 3]"}) {
            assertArrayEquals(utf8(text), convert(text, byte[].class), text);
        }
        assertArrayEquals(utf8("Hello"), ByteUtilities.decode("SGVsbG8="));
        assertArrayEquals(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE},
                ByteUtilities.decode("CAFEBABE"));
    }

    @Test
    void theWrappedFormIsBase64OnlyWhenPaddedCorrectly() {
        assertArrayEquals(utf8("AF"), convert(Collections.singletonMap("value", "AF"), byte[].class));
        assertArrayEquals(new byte[] {1}, convert(Collections.singletonMap("value", "AQ=="), byte[].class));
    }

    @Test
    void aNumberConvertsToANarrowerTypeOnlyIfItFits() {
        assertThrows(IllegalArgumentException.class, () -> convert(300, Byte.class));
        assertThrows(IllegalArgumentException.class, () -> convert(3.0e10, Integer.class));
        assertEquals(1, (int) convert(1.9, Integer.class));     // a fraction still truncates
    }

    @Test
    void blankTextIsNullForATypeWithoutAZero() {
        assertNull(convert("", UUID.class));
        assertNull(convert(" ", Year.class));
        assertNull(convert("", Duration.class));
        assertNull(convert("", TimeUnit.class));
        assertEquals(0, (int) convert("", Integer.class));
        assertFalse(convert("", Boolean.class));
    }

    @Test
    void textConvertsToNumber() {
        assertEquals(42L, convert("42", Number.class));
        assertEquals(new BigInteger("12345678901234567890"), convert("12345678901234567890", Number.class));
        assertEquals(new BigDecimal("1.5"), convert("1.5", Number.class));
    }

    @Test
    void numberTextConvertsToADateAsThatNumber() {
        assertEquals(LocalDate.of(2024, 1, 15), convert("20240115", LocalDate.class));
        assertEquals(new Date(1700000000000L), convert("1700000000000", Date.class));
        assertEquals(Instant.ofEpochMilli(45_500), convert("45.5", Instant.class));
    }

    @Test
    void aFloatOrDoubleConvertsAsTheDecimalItPrints() {
        assertEquals(new BigInteger("340000000000000000000000000000000000000"), convert(3.4e38f, BigInteger.class));
        assertEquals(new BigDecimal("1.0E+23"), convert(1.0E23, BigDecimal.class));    // JDK 8-18 too
    }

    @Test
    void aSourceWithAZoneKeepsItsOwnWallClock() {
        ZonedDateTime tokyo = ZonedDateTime.of(2024, 1, 15, 9, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        assertEquals(LocalDateTime.of(2024, 1, 15, 9, 0), convert(tokyo, LocalDateTime.class));
    }

    @Test
    void anOffsetThatContradictsItsZoneWins() {
        assertEquals(Instant.parse("2024-07-01T07:00:00Z"),
                convert("2024-07-01T12:00+05:00[Europe/Berlin]", Instant.class));
    }

    @Test
    void anUnknownZoneIsRefusedAndAnyCaseIsAccepted() {
        assertThrows(IllegalArgumentException.class, () -> convert("bogus", TimeZone.class));
        assertEquals(ZoneId.of("America/New_York"), convert("america/new_york", ZoneId.class));
    }

    @Test
    void anAbbreviationMeansItsOwnOffset() {
        assertEquals(Instant.parse("2024-07-04T17:00:00Z"),
                DateUtilities.parseDate("2024-07-04 12:00:00 EST", ZoneId.of("UTC"), true).toInstant());
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"),
                DateUtilities.parseDate("Mon Jan 15 06:30:00 AST 2024", ZoneId.of("UTC"), true).toInstant());
    }

    @Test
    void aBitSetConvertsOnlyToAndFromWhatHoldsBits() {
        assertFalse(CONVERTER.isConversionSupportedFor(LocalDate.class, BitSet.class));
        assertThrows(IllegalArgumentException.class, () -> convert("1,3", BitSet.class));
        BitSet bits = new BitSet();
        bits.set(0);
        bits.set(2);
        assertEquals(bits, convert("101", BitSet.class));
    }

    @Test
    void aPatternKeepsItsFlagsAndItsSpaces() {
        assertEquals("(?i)abc", convert(Pattern.compile("abc", Pattern.CASE_INSENSITIVE), String.class));
        assertEquals(" a ", convert(" a ", Pattern.class).pattern());
    }

    @Test
    void aLinkedBlockingQueueMadeByAConversionIsUnbounded() {
        assertEquals(Integer.MAX_VALUE - 2,
                convert(Arrays.asList(1, 2), LinkedBlockingQueue.class).remainingCapacity());
    }

    @Test
    void textToUuidIgnoresSurroundingSpaces() {
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                convert(" 123e4567-e89b-12d3-a456-426614174000 ", UUID.class));
    }

    @Test
    void moreJdkClassesAreBlockedFromInstantiation() {
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(java.util.Timer.class, null));
        // In the temp directory: had PrintStream not been blocked, it would have created the file
        String file = new java.io.File(System.getProperty("java.io.tmpdir"), "java-util-never-created.txt").getPath();
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(java.io.PrintStream.class,
                Collections.singletonList(file)));
    }

    @Test
    void byteUtilitiesDecodeReadsMoreNotations() {
        assertArrayEquals(new byte[] {(byte) 0xAF, (byte) 0xDE}, ByteUtilities.decode("AF:DE"));
        assertArrayEquals(new byte[0], ByteUtilities.decode(" "));
    }

    /** The hash a Java-serialized CaseInsensitiveMap stores changed, so 4.112.0 cannot look up what 4.113.0 wrote. */
    @Test
    void theCaseInsensitiveHashChanged() {
        assertNotEquals("alpha".hashCode(), StringUtilities.hashCodeIgnoreCase("Alpha"));
    }
}
