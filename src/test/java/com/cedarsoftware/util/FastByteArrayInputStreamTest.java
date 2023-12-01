package com.cedarsoftware.util;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FastByteArrayInputStreamTest {

    @Test
    void testReadSingleByte() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        assertEquals(-1, stream.read()); // End of stream
    }

    @Test
    void testReadArray() {
        byte[] data = {4, 5, 6, 7};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[4];

        int bytesRead = stream.read(buffer, 0, buffer.length);
        assertArrayEquals(new byte[]{4, 5, 6, 7}, buffer);
        assertEquals(4, bytesRead);
    }

    @Test
    void testReadArrayWithOffset() {
        byte[] data = {8, 9, 10, 11, 12};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[5];

        stream.read(buffer, 1, 2);
        assertArrayEquals(new byte[]{0, 8, 9, 0, 0}, buffer);
    }

    @Test
    void testSkip() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        long skipped = stream.skip(2);
        assertEquals(2, skipped);
        assertEquals(3, stream.read());
    }

    @Test
    void testAvailable() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(3, stream.available());
        stream.read();
        assertEquals(2, stream.available());
    }

    @Test
    void testMarkAndReset() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertTrue(stream.markSupported());
        stream.mark(0);
        stream.read();
        stream.read();
        stream.reset();
        assertEquals(1, stream.read());
    }

    @Test
    void testClose() throws IOException {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        stream.close();
        assertEquals(3, stream.available()); // Stream should still be readable after close
    }

    @Test
    void testReadFromEmptyStream() {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, stream.read());
    }

    @Test
    void testSkipPastEndOfStream() {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        assertEquals(3, stream.skip(10));
        assertEquals(-1, stream.read());
    }

    @Test
    void testReadWithInvalidParameters() {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], -1, 4));
    }
}
