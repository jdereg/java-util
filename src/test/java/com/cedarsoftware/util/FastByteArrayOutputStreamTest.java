package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Faster version of ByteArrayOutputStream that does not have synchronized methods and
 * also provides direct access to its internal buffer so that it does not need to be
 * duplicated when read.
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
class FastByteArrayOutputStreamTest {

    @Test
    void testDefaultConstructor() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        assertNotNull(outputStream);
        assertEquals(0, outputStream.size());
    }

    @Test
    void testConstructorWithInitialSize() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream(100);
        assertNotNull(outputStream);
        assertEquals(0, outputStream.size());
    }

    @Test
    void testConstructorWithNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new FastByteArrayOutputStream(-1));
    }

    @Test
    void testWriteSingleByte() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        outputStream.write(65); // ASCII for 'A'
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[]{(byte) 65}, outputStream.toByteArray());
    }

    @Test
    void testWriteByteArrayWithOffsetAndLength() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        byte[] data = "Hello".getBytes();
        outputStream.write(data, 1, 3); // "ell"
        assertEquals(3, outputStream.size());
        assertArrayEquals("ell".getBytes(), outputStream.toByteArray());
    }

    @Test
    void testWriteByteArray() throws IOException {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        byte[] data = "Hello World".getBytes();
        outputStream.write(data);
        assertEquals(data.length, outputStream.size());
        assertArrayEquals(data, outputStream.toByteArray());
    }

    @Test
    void testToByteArray() throws IOException {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        byte[] data = "Test".getBytes();
        outputStream.write(data);
        assertArrayEquals(data, outputStream.toByteArray());
        assertEquals(data.length, outputStream.size());
    }

    @Test
    void testSize() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        assertEquals(0, outputStream.size());
        outputStream.write(65); // ASCII for 'A'
        assertEquals(1, outputStream.size());
    }

    @Test
    void testToString() throws IOException {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        String str = "Hello";
        outputStream.write(str.getBytes());
        assertEquals(str, outputStream.toString());
    }

    @Test
    void testWriteTo() throws IOException {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        byte[] data = "Hello World".getBytes();
        outputStream.write(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        outputStream.writeTo(baos);

        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    void testClose() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        assertDoesNotThrow(outputStream::close);
    }
    
    @Test
    void testSizeConstructor() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(50);
        assertNotNull(stream);
        assertEquals(0, stream.toByteArray().length);
    }

    @Test
    void testNegativeSizeConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new FastByteArrayOutputStream(-10));
    }

    @Test
    void testWriteSingleByte2() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(65); // 'A'
        stream.write(66); // 'B'
        stream.write(67); // 'C'

        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] {65, 66, 67}, result);
    }
    
    @Test
    void testWriteByteArrayWithOffset() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = {10, 20, 30, 40, 50};
        stream.write(data, 1, 3);

        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] {20, 30, 40}, result);
    }

    @Test
    void testWriteNull() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(null, 0, 5));
    }

    @Test
    void testWriteNegativeOffset() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = {10, 20, 30};
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, -1, 2));
    }

    @Test
    void testWriteNegativeLength() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = {10, 20, 30};
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 0, -1));
    }

    @Test
    void testWriteInvalidBounds() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = {10, 20, 30};

        // Test offset > array length
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 4, 1));

        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 1, 3));

        // Test integer overflow in offset + length
        assertThrows(IndexOutOfBoundsException.class,
                () -> stream.write(data, Integer.MAX_VALUE, 10));
    }

    @Test
    void testWriteBytes() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = {10, 20, 30, 40, 50};
        stream.writeBytes(data);

        byte[] result = stream.toByteArray();
        assertArrayEquals(data, result);
    }

    @Test
    void testReset() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(65);
        stream.write(66);

        // Should have two bytes
        assertEquals(2, stream.toByteArray().length);

        // Reset and check
        stream.reset();
        assertEquals(0, stream.toByteArray().length);

        // Write more after reset
        stream.write(67);
        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] {67}, result);
    }

    @Test
    void testToByteArray2() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(10);
        stream.write(20);
        stream.write(30);

        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] {10, 20, 30}, result);

        // Verify that we get a copy of the data
        result[0] = 99;
        byte[] result2 = stream.toByteArray();
        assertEquals(10, result2[0]); // Original data unchanged
    }

    @Test
    void testGetBuffer() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(10);
        stream.write(20);
        stream.write(30);

        byte[] buffer = stream.getBuffer();
        assertArrayEquals(new byte[] {10, 20, 30}, buffer);

        // Verify it's the same data as toByteArray()
        byte[] array = stream.toByteArray();
        assertArrayEquals(array, buffer);
    }

    @Test
    void testGrowBufferAutomatically() {
        // Start with a small buffer
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(2);

        // Write enough bytes to force growth
        for (int i = 0; i < 20; i++) {
            stream.write(i);
        }

        // Verify all bytes were written
        byte[] result = stream.toByteArray();
        assertEquals(20, result.length);
        for (int i = 0; i < 20; i++) {
            assertEquals(i, result[i] & 0xFF);
        }
    }

    @Test
    void testGrowBufferSpecificCase() {
        // This test targets the specific growth logic in the grow method
        // including the case where newCapacity - minCapacity < 0

        // Start with a buffer of 4 bytes
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(4);

        // Now write data that will force ensureCapacity with a large minCapacity
        // This will make the growth logic use the minCapacity directly
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte)i;
        }

        stream.write(largeData, 0, largeData.length);

        // Verify all data was written correctly
        byte[] result = stream.toByteArray();
        assertEquals(1000, result.length);
        for (int i = 0; i < 1000; i++) {
            assertEquals(i & 0xFF, result[i] & 0xFF);
        }
    }

    @Test
    void testWriteArrayThatTriggersGrowth() {
        // Start with small buffer
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(10);

        // Write a few bytes
        stream.write(1);
        stream.write(2);

        // Now write an array that requires growth
        byte[] largeData = new byte[20];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte)(i + 10);
        }

        stream.write(largeData, 0, largeData.length);

        // Verify everything was written
        byte[] result = stream.toByteArray();
        assertEquals(22, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 10, result[i + 2] & 0xFF);
        }
    }

    @Test
    void testBufferDoublingGrowthStrategy() {
        // Test the buffer doubling growth strategy (oldCapacity << 1)
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(4);

        // Fill the buffer exactly
        stream.write(1);
        stream.write(2);
        stream.write(3);
        stream.write(4);

        // Add one more byte to trigger growth to 8 bytes
        stream.write(5);

        // Add enough bytes to trigger growth to 16 bytes
        for (int i = 0; i < 4; i++) {
            stream.write(10 + i);
        }

        // Verify all bytes were written
        byte[] result = stream.toByteArray();
        assertEquals(9, result.length);

        int[] expected = {1, 2, 3, 4, 5, 10, 11, 12, 13};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i] & 0xFF);
        }
    }

    @Test
    void testIntegerOverflowInBoundsCheck() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();

        // Create a large byte array
        byte[] data = new byte[10];

        // The key is to pass all the earlier conditions:
        // 1. b != null (using non-null array)
        // 2. off >= 0 (using positive offset)
        // 3. len >= 0 (using positive length)
        // 4. off <= b.length (using offset within bounds)
        // 5. off + len <= b.length (calculating this carefully)

        // Integer.MAX_VALUE is well above b.length, so we need a valid offset
        // that will still cause overflow when added to length
        int offset = 5; // Valid offset within the array

        // We need this special value to pass (off + len <= b.length)
        // but fail with (off + len < 0) due to overflow
        int length = Integer.MAX_VALUE;

        // This should trigger ONLY the (off + len < 0) condition
        // because offset + length will overflow to a negative number
        assertThrows(IndexOutOfBoundsException.class,
                () -> stream.write(data, offset, length));
    }
}

