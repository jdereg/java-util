package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastByteArrayInputStreamTest {

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

    @Test
    void testConstructor() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        assertNotNull(stream);
    }

    @Test
    void testReadSingleByte() {
        byte[] data = {10, 20, 30, 40, 50};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
    }

    @Test
    void testReadByteArray() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] buffer = new byte[4];
        int bytesRead = stream.read(buffer, 0, buffer.length);

        assertEquals(4, bytesRead);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, buffer);

        // Read next chunk
        bytesRead = stream.read(buffer, 0, buffer.length);
        assertEquals(4, bytesRead);
        assertArrayEquals(new byte[] {5, 6, 7, 8}, buffer);

        // Should return -1 at EOF
        bytesRead = stream.read(buffer, 0, buffer.length);
        assertEquals(-1, bytesRead);
    }

    @Test
    void testReadEndOfStream() {
        byte[] data = {1, 2};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertEquals(-1, stream.read()); // EOF indicator
    }

    @Test
    void testReadToNullArray() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertThrows(NullPointerException.class, () -> stream.read(null, 0, 1));
    }

    @Test
    void testReadWithNegativeOffset() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[2];

        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, -1, 1));
    }

    @Test
    void testReadWithNegativeLength() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[2];

        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, 0, -1));
    }

    @Test
    void testReadWithTooLargeLength() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[4];

        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, 2, 3));
    }

    @Test
    void testReadWithZeroLength() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        byte[] buffer = new byte[2];

        int bytesRead = stream.read(buffer, 0, 0);
        assertEquals(0, bytesRead);
        assertArrayEquals(new byte[] {0, 0}, buffer); // Buffer unchanged
    }

    @Test
    void testReadLessThanAvailable() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] buffer = new byte[3];
        int bytesRead = stream.read(buffer, 0, 2); // Only read 2 bytes

        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[] {1, 2, 0}, buffer);
    }

    @Test
    void testReadMoreThanAvailable() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] buffer = new byte[5];
        int bytesRead = stream.read(buffer, 0, 5); // Try to read 5, but only 3 available

        assertEquals(3, bytesRead);
        assertArrayEquals(new byte[] {1, 2, 3, 0, 0}, buffer);
    }

    @Test
    void testReadWithOffset() {
        byte[] data = {1, 2, 3, 4};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] buffer = new byte[5];
        int bytesRead = stream.read(buffer, 2, 3); // Read into buffer starting at index 2

        assertEquals(3, bytesRead);
        assertArrayEquals(new byte[] {0, 0, 1, 2, 3}, buffer);
    }

    @Test
    void testSkipPositive() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        long skipped = stream.skip(3);
        assertEquals(3, skipped);
        assertEquals(4, stream.read()); // Should read 4th byte
    }

    @Test
    void testSkipNegative() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        // Skip should return 0 for negative values
        long skipped = stream.skip(-10);
        assertEquals(0, skipped);
        assertEquals(1, stream.read()); // Position unchanged
    }

    @Test
    void testSkipZero() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        long skipped = stream.skip(0);
        assertEquals(0, skipped);
        assertEquals(1, stream.read()); // Position unchanged
    }

    @Test
    void testSkipMoreThanAvailable() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        long skipped = stream.skip(10); // Try to skip 10, but only 5 available
        assertEquals(5, skipped);
        assertEquals(-1, stream.read()); // At end of stream
    }

    @Test
    void testSkipAfterReading() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        // Read some data first
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());

        // Now skip
        long skipped = stream.skip(3);
        assertEquals(3, skipped);
        assertEquals(6, stream.read()); // Should read 6th byte
    }

    @Test
    void testEmptyStream() {
        byte[] data = {};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(-1, stream.read()); // Empty stream returns EOF immediately
        byte[] buffer = new byte[5];
        assertEquals(-1, stream.read(buffer, 0, 5)); // Array read also returns EOF
    }

    @Test
    void testMarkAndReset() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        // Read a couple bytes
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());

        // Mark position
        stream.mark(0); // Parameter is ignored in FastByteArrayInputStream

        // Read more
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());

        // Reset to marked position
        stream.reset();

        // Should be back at the marked position
        assertEquals(3, stream.read());
    }

    @Test
    void testAvailableMethod() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertEquals(5, stream.available());

        // Read some
        stream.read();
        stream.read();

        assertEquals(3, stream.available());

        // Skip some
        stream.skip(2);

        assertEquals(1, stream.available());
    }

    @Test
    void testIsMarkSupported() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        assertTrue(stream.markSupported());
    }

    // ==================== New Tests ====================

    @Test
    void testConstructorWithNull() {
        assertThrows(NullPointerException.class, () -> new FastByteArrayInputStream(null));
    }

    @Test
    void testReadAllBytes() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] result = stream.readAllBytes();
        assertArrayEquals(data, result);
        assertEquals(-1, stream.read()); // Should be at EOF
    }

    @Test
    void testReadAllBytesAfterPartialRead() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        // Read first two bytes
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());

        // Read remaining bytes
        byte[] remaining = stream.readAllBytes();
        assertArrayEquals(new byte[]{3, 4, 5}, remaining);
        assertEquals(-1, stream.read());
    }

    @Test
    void testReadAllBytesOnEmptyStream() {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[0]);
        byte[] result = stream.readAllBytes();
        assertEquals(0, result.length);
    }

    @Test
    void testReadAllBytesAtEOF() {
        byte[] data = {1, 2};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        // Read to EOF
        stream.read();
        stream.read();

        byte[] result = stream.readAllBytes();
        assertEquals(0, result.length);
    }

    @Test
    void testReadNBytes() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] result = stream.readNBytes(3);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(2, stream.available());
    }

    @Test
    void testReadNBytesMoreThanAvailable() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] result = stream.readNBytes(10);
        assertArrayEquals(data, result);
        assertEquals(-1, stream.read());
    }

    @Test
    void testReadNBytesZero() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        byte[] result = stream.readNBytes(0);
        assertEquals(0, result.length);
        assertEquals(3, stream.available()); // Position unchanged
    }

    @Test
    void testReadNBytesNegative() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertThrows(IllegalArgumentException.class, () -> stream.readNBytes(-1));
    }

    @Test
    void testTransferTo() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long transferred = stream.transferTo(out);

        assertEquals(5, transferred);
        assertArrayEquals(data, out.toByteArray());
        assertEquals(-1, stream.read()); // Should be at EOF
    }

    @Test
    void testTransferToAfterPartialRead() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Read first two bytes
        stream.read();
        stream.read();

        long transferred = stream.transferTo(out);

        assertEquals(3, transferred);
        assertArrayEquals(new byte[]{3, 4, 5}, out.toByteArray());
    }

    @Test
    void testTransferToEmptyStream() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long transferred = stream.transferTo(out);

        assertEquals(0, transferred);
        assertEquals(0, out.size());
    }

    @Test
    void testTransferToNull() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);

        assertThrows(NullPointerException.class, () -> stream.transferTo(null));
    }

    @Test
    void testTransferToAtEOF() throws IOException {
        byte[] data = {1, 2};
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Read to EOF
        stream.read();
        stream.read();

        long transferred = stream.transferTo(out);
        assertEquals(0, transferred);
    }
}
