package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A BitSet converts to and from its binary text, the integer types it fits, BigInteger and BigDecimal, Boolean as one
 * bit, and its byte, index and boolean arrays, and nothing else. Every other conversion went through the binary text,
 * read back as a decimal number: BitSet {0, 2} gave 101.0 as a Double, PT1M41S as a Duration and 'e' as a Character.
 */
class ConverterBitSetTest {

    private static final Set<Class<?>> PARTNERS = new HashSet<>(Arrays.asList(
            String.class, StringBuilder.class, StringBuffer.class, CharSequence.class, CharBuffer.class,
            char[].class, Character[].class,
            Long.class, long.class, Integer.class, int.class, Short.class, short.class, Byte.class, byte.class,
            BigInteger.class, BigDecimal.class, Boolean.class, boolean.class,
            AtomicBoolean.class, AtomicInteger.class, AtomicLong.class, LongAdder.class,
            byte[].class, int[].class, boolean[].class, ByteBuffer.class, BitSet.class));

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static BitSet bits(int... indexes) {
        BitSet bitSet = new BitSet();
        for (int index : indexes) {
            bitSet.set(index);
        }
        return bitSet;
    }

    /**
     * Of every class the Converter knows, BitSet converts to exactly the partners above and Map, and from exactly the
     * partners, Map and null (Void). Asked of the Converter itself, so conversions it finds through a supertype count.
     */
    @Test
    void onlyTheseConversionsAreSupported() {
        Set<Class<?>> known = new HashSet<>();
        for (Map.Entry<Class<?>, Set<Class<?>>> entry : Converter.allSupportedConversions().entrySet()) {
            known.add(entry.getKey());
            known.addAll(entry.getValue());
        }
        known.addAll(PARTNERS);
        Set<String> out = new TreeSet<>();
        Set<String> in = new TreeSet<>();
        for (Class<?> other : known) {
            if (converter.isConversionSupportedFor(BitSet.class, other)) {
                out.add(other.getName());
            }
            if (converter.isConversionSupportedFor(other, BitSet.class)) {
                in.add(other.getName());
            }
        }
        Set<String> expectedOut = new TreeSet<>();
        for (Class<?> partner : PARTNERS) {
            expectedOut.add(partner.getName());
        }
        expectedOut.add(Map.class.getName());
        Set<String> expectedIn = new TreeSet<>(expectedOut);
        expectedIn.add(Void.class.getName());
        assertEquals(expectedOut, out, "BitSet -> ...");
        assertEquals(expectedIn, in, "... -> BitSet");
    }

    /** null is null; BitSet to Map is {"value": "&lt;binary string&gt;"}, which Map to BitSet reads back. */
    @Test
    void nullAndTheMapForm() {
        assertEquals(null, converter.convert(null, BitSet.class));
        Map<?, ?> map = converter.convert(bits(0, 2), Map.class);
        assertEquals(Collections.singletonMap("value", "101"), map);
        assertEquals(bits(0, 2), converter.convert(map, BitSet.class));
        assertArrayEquals("101".toCharArray(), converter.convert(bits(0, 2), char[].class));
        assertEquals(bits(0, 2), converter.convert(new Character[] {'1', '0', '1'}, BitSet.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("1x".toCharArray(), BitSet.class));
    }

    /** Binary text: only 0 and 1, trimmed; leading zeros allowed; "" is the empty set. */
    @Test
    void textIsZerosAndOnes() {
        assertEquals(bits(0, 2), converter.convert("101", BitSet.class));
        assertEquals(bits(0, 2), converter.convert(" 00101 ", BitSet.class));
        assertEquals(bits(), converter.convert("", BitSet.class));
        assertEquals("101", converter.convert(bits(0, 2), String.class));
        assertEquals("", converter.convert(bits(), String.class));
        assertEquals(bits(0, 2), converter.convert(new StringBuilder("101"), BitSet.class));
        assertEquals("101", converter.convert(bits(0, 2), StringBuilder.class).toString());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert("10x1", BitSet.class));
        assertTrue(e.getMessage().contains("'10x1'") && e.getMessage().contains("index 2"), e.getMessage());
        for (String bad : new String[] {"hello", "5.0", "1 0", "102", "0b101"}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert(bad, BitSet.class), bad);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", "10x1");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(map, BitSet.class));

        StringBuilder huge = new StringBuilder("x");
        for (int i = 0; i < 1000; i++) {
            huge.append('1');
        }
        String message = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(huge.toString(), BitSet.class)).getMessage();
        assertTrue(message.length() < 250, message.length() + " characters");
    }

    /**
     * An integer type takes a BitSet whose set bits fit its width, as a two's-complement pattern; a bit beyond the
     * width was dropped without a word, and now is an error.
     */
    @Test
    void integerTypesTakeWhatFitsTheirWidth() {
        assertEquals(5L, converter.convert(bits(0, 2), long.class));
        assertEquals(Long.MIN_VALUE, converter.convert(bits(63), long.class));
        assertEquals(-1L, converter.convert(converter.convert(-1L, BitSet.class), long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(64), long.class));

        assertEquals(Integer.MIN_VALUE, converter.convert(bits(31), int.class));
        assertEquals(-1, converter.convert(converter.convert(-1, BitSet.class), int.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(32), Integer.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(40), AtomicInteger.class));

        assertEquals((short) -1, converter.convert(converter.convert((short) -1, BitSet.class), short.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(16), short.class));
        assertEquals((byte) -128, converter.convert(bits(7), byte.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(8), Byte.class));

        assertEquals(5L, converter.convert(bits(0, 2), AtomicLong.class).get());
        assertEquals(bits(0, 2), converter.convert(new AtomicInteger(5), BitSet.class));
    }

    /** BigInteger and BigDecimal: non-negative, any size; a fraction, or a negative number, is an error. */
    @Test
    void bigNumbersAreNonNegativeWholeNumbers() {
        assertEquals(BigInteger.ONE.shiftLeft(100).add(BigInteger.ONE), converter.convert(bits(0, 100), BigInteger.class));
        assertEquals(bits(0, 2), converter.convert(new BigDecimal("5.00"), BitSet.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(new BigDecimal("5.5"), BitSet.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(BigInteger.valueOf(-1), BitSet.class));
    }

    /** Boolean is one bit: {} is false, {0} is true, and a higher bit does not fit. */
    @Test
    void booleanIsOneBit() {
        assertFalse(converter.convert(bits(), Boolean.class));
        assertTrue(converter.convert(bits(0), boolean.class));
        assertEquals(bits(0), converter.convert(true, BitSet.class));
        assertEquals(bits(), converter.convert(false, BitSet.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(1), Boolean.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(0, 1), AtomicBoolean.class));
    }

    /**
     * The Map form also takes the indices of the set bits, {"value": [0, 2]}, as older json-io wrote a BitSet. It
     * failed ("Unsupported conversion, source type [Object[]]"). An index must be a non-negative whole number below
     * 2^24: an index costs memory to its height (bit 2,000,000,000 is 250 MB), so twelve characters of input could
     * take that much.
     */
    @Test
    void theMapFormTakesBitIndices() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", new Object[] {0L, 2L});
        assertEquals(bits(0, 2), converter.convert(map, BitSet.class));
        map.put("value", Arrays.asList(5, 1, 5));
        assertEquals(bits(1, 5), converter.convert(map, BitSet.class));
        map.put("value", new Object[0]);
        assertEquals(bits(), converter.convert(map, BitSet.class));
        map.put("value", new Object[] {(1 << 24) - 1L});
        assertEquals(bits((1 << 24) - 1), converter.convert(map, BitSet.class));

        for (Object bad : new Object[] {-1L, (long) (1 << 24), 2_000_000_000L, 2.5d, "2", null, true}) {
            map.put("value", new Object[] {0L, bad});
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(map, BitSet.class), String.valueOf(bad));
            assertTrue(e.getMessage().contains("index"), e.getMessage());
        }
    }

    /** ByteBuffer holds the same bytes as byte[]; it held the bytes of the binary text. */
    @Test
    void byteBufferMatchesByteArray() {
        ByteBuffer buffer = converter.convert(bits(0, 2), ByteBuffer.class);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        assertArrayEquals(converter.convert(bits(0, 2), byte[].class), bytes);
        assertArrayEquals(new byte[] {5}, bytes);
        assertEquals(bits(0, 2), converter.convert(ByteBuffer.wrap(new byte[] {5}), BitSet.class));
    }

    /** Floating types, dates and times, characters, files, URIs, patterns, locales and time zones: unsupported. */
    @Test
    void everythingElseIsUnsupported() {
        Object[] others = {5.0d, 5.0f, 'A', new Date(0), java.time.Duration.ofSeconds(5), java.time.Year.of(2024),
                java.time.Instant.EPOCH, new java.io.File("101"), java.net.URI.create("101"), java.util.Locale.US,
                TimeZone.getTimeZone("UTC"), java.util.regex.Pattern.compile("101")};
        for (Object other : others) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert(other, BitSet.class),
                    other.getClass().getSimpleName());
            assertThrows(IllegalArgumentException.class, () -> converter.convert(bits(0, 2), other.getClass()),
                    other.getClass().getSimpleName());
        }
    }
}
