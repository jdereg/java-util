package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Charset conversions (4.113.0). A Charset is identified by its canonical name().
 * <p>
 * Tested here, not yet in ConverterEverythingTest: that test also round-trips every pair through the
 * test-scope json-io (4.111.0), which writes a Charset as a POJO of its JDK-internal class and cannot read
 * it back. json-io's fix needs these conversions, so it ships after this release; add Charset to
 * ConverterEverythingTest once version.json-io includes it.
 */
class CharsetConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testStringToCharset() {
        assertSame(StandardCharsets.UTF_8, converter.convert("UTF-8", Charset.class));
        assertSame(StandardCharsets.US_ASCII, converter.convert("US-ASCII", Charset.class));
        assertSame(StandardCharsets.ISO_8859_1, converter.convert("ISO-8859-1", Charset.class));
        assertSame(StandardCharsets.UTF_16, converter.convert("UTF-16", Charset.class));
        assertEquals(Charset.forName("windows-1252"), converter.convert("windows-1252", Charset.class));

        // An alias, and surrounding whitespace
        assertSame(StandardCharsets.UTF_8, converter.convert("utf8", Charset.class));
        assertSame(StandardCharsets.UTF_8, converter.convert(" UTF-8 ", Charset.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testBlankStringToCharsetIsNull(String blank) {
        assertNull(converter.convert(blank, Charset.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-such-charset", "bad name!"})
    void testUnknownCharsetIsRejected(String name) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(name, Charset.class));
        assertEquals("Unknown or unsupported Charset: '" + name + "'", e.getMessage());
    }

    @Test
    void testCharsetToString() {
        assertEquals("UTF-8", converter.convert(StandardCharsets.UTF_8, String.class));
        assertEquals("ISO-8859-1", converter.convert(StandardCharsets.ISO_8859_1, String.class));
        assertEquals("UTF-8", converter.convert(Charset.forName("utf8"), String.class));   // canonical name
    }

    @Test
    void testMapToCharset() {
        Map<String, Object> map = new HashMap<>();
        map.put("charset", "UTF-8");
        assertSame(StandardCharsets.UTF_8, converter.convert(map, Charset.class));

        map.clear();
        map.put("value", "ISO-8859-1");
        assertSame(StandardCharsets.ISO_8859_1, converter.convert(map, Charset.class));

        map.clear();
        map.put("_v", "UTF-16");
        assertSame(StandardCharsets.UTF_16, converter.convert(map, Charset.class));

        Map<String, Object> noKey = new HashMap<>();
        noKey.put("name", "UTF-8");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(noKey, Charset.class));
    }

    @Test
    void testCharsetToMap() {
        Map<?, ?> map = converter.convert(StandardCharsets.US_ASCII, Map.class);
        assertEquals(1, map.size());
        assertEquals("US-ASCII", map.get("charset"));
        assertSame(StandardCharsets.US_ASCII, converter.convert(map, Charset.class));
    }

    @Test
    void testCharsetToCharset() {
        assertSame(StandardCharsets.UTF_8, converter.convert(StandardCharsets.UTF_8, Charset.class));
        assertNull(converter.convert(null, Charset.class));
    }

    @Test
    void testEveryAvailableCharsetRoundTripsThroughItsName() {
        for (Charset cs : Charset.availableCharsets().values()) {
            String name = converter.convert(cs, String.class);
            assertEquals(cs, converter.convert(name, Charset.class), name);
        }
    }

    /** json-io treats a type as a String-backed value exactly when this is true. */
    @Test
    void testCharsetIsASimpleStringConvertibleType() {
        assertTrue(com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(String.class, Charset.class));
        assertTrue(com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(Charset.class, String.class));
        assertTrue(com.cedarsoftware.util.Converter.isConversionSupportedFor(
                StandardCharsets.UTF_8.getClass(), String.class),
                "the JDK's internal Charset subclass converts too");
    }
}
