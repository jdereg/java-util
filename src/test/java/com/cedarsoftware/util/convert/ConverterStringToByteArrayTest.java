package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * String to byte[] (and ByteBuffer) gives the String's bytes in the Converter's charset, the reverse of byte[] to
 * String. Nothing is guessed from what the String looks like.
 */
class ConverterStringToByteArrayTest {

    private static final String[] TEXTS = {
            "deadbeef", "12345678", "CAFEBABE", "ThisIsAPlainText", "AccountNumber123", "[1, 2, 3]", "ab cd ef",
            "00 01 02 FF", "SGVsbG8=", "prod/2026/backup", "DATA", "hello world", ""};

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * String to byte[] guessed a format from the text: 8+ hex digits were decoded as hex, an alphanumeric word of 16+
     * characters as Base64, "[1, 2, 3]" as a list of numbers, "ab cd ef" as spaced hex. So bytes that spelled such a
     * text did not survive byte[] -> String -> byte[], and text meant as text came back as different bytes.
     */
    @Test
    void bytesRoundTripThroughTheirText() {
        List<String> wrong = new ArrayList<>();
        for (String text : TEXTS) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] back = converter.convert(converter.convert(bytes, String.class), byte[].class);
            if (!Arrays.equals(bytes, back)) {
                wrong.add("\"" + text + "\" -> " + Arrays.toString(back));
            }
            ByteBuffer buffer = converter.convert(text, ByteBuffer.class);
            byte[] fromBuffer = new byte[buffer.remaining()];
            buffer.get(fromBuffer);
            if (!Arrays.equals(bytes, fromBuffer)) {
                wrong.add("ByteBuffer \"" + text + "\" -> " + Arrays.toString(fromBuffer));
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    /** The Converter's charset decides the bytes, as before. */
    @Test
    void theConvertersCharsetIsUsed() {
        Converter latin1 = new Converter(new DefaultConverterOptions() {
            @Override
            public Charset getCharset() {
                return StandardCharsets.ISO_8859_1;
            }
        });
        assertArrayEquals(new byte[] {(byte) 0xE9}, latin1.convert("é", byte[].class));
        assertArrayEquals(new byte[] {(byte) 0xC3, (byte) 0xA9}, converter.convert("é", byte[].class));
    }

    /**
     * The Map form is where Base64 is read: {"value": "&lt;base64&gt;"}, as json-io writes a byte[]. A value that is not
     * Base64 gives its bytes.
     */
    @Test
    void theMapFormReadsBase64() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("value", "AQID");
        assertArrayEquals(new byte[] {1, 2, 3}, converter.convert(map, byte[].class));
        map.put("value", "[1, 2, 3]");
        assertArrayEquals("[1, 2, 3]".getBytes(StandardCharsets.UTF_8), converter.convert(map, byte[].class));
        assertEquals(3, converter.convert(java.util.Collections.singletonMap("_v", "AQID"), ByteBuffer.class)
                .remaining());
    }
}
