package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An unsupported conversion names the source's type and the start of its value, not the whole value.
 */
class ConverterUnsupportedMessageTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * The message embedded the source's whole toString(): 128,963 characters for a list of 20,000 numbers, into every
     * log that records the exception. It now shows the first 100 characters.
     */
    @Test
    void aLargeSourceIsCutShort() {
        List<Integer> big = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            big.add(i);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(big, UUID.class));
        String message = e.getMessage();
        assertTrue(message.length() < 200, message.length() + " characters");
        assertTrue(message.startsWith("Unsupported conversion, source type [ArrayList ([0, 1, 2, 3, "), message);
        assertTrue(message.endsWith("...)] target type 'UUID'"), message);
    }

    /** The cut never splits a character outside the BMP (an emoji is two chars): it falls before it. */
    @Test
    void theCutDoesNotSplitASurrogatePair() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 98; i++) {
            text.append('x');
        }
        text.append("\uD83D\uDE00 and more");        // "[" + 98 x's puts the emoji's high surrogate at index 99
        List<String> list = new ArrayList<>();
        list.add(text.toString());
        String message = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(list, UUID.class)).getMessage();
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isHighSurrogate(c)) {
                assertTrue(i + 1 < message.length() && Character.isLowSurrogate(message.charAt(i + 1)), message);
            }
        }
        assertTrue(message.contains("xxx...)]"), message);
    }

    /** A short source reads as before. */
    @Test
    void aShortSourceIsShownWhole() {
        List<Integer> small = new ArrayList<>();
        small.add(7);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(small, UUID.class));
        assertEquals("Unsupported conversion, source type [ArrayList ([7])] target type 'UUID'", e.getMessage());
    }
}
