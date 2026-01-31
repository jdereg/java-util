package com.cedarsoftware.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DataGeneratorInputStream
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
class DataGeneratorInputStreamTest {

    // ========== Tests for Random Bytes Mode ==========

    @Test
    void testRandomBytesBasicRead() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(10);

        for (int i = 0; i < 10; i++) {
            int b = stream.read();
            assertTrue(b >= 0 && b <= 255, "Byte should be in range 0-255, got: " + b);
        }

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read());
    }

    @Test
    void testRandomBytesWithoutZero() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(10000, 42L, false);

        for (int i = 0; i < 10000; i++) {
            int b = stream.read();
            assertNotEquals(0, b, "Stream should never return 0 at position " + i);
            assertTrue(b >= 1 && b <= 255, "Byte should be in range 1-255");
        }
    }

    @Test
    void testRandomBytesZeroSize() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(0);
        assertEquals(-1, stream.read());
    }

    @Test
    void testRandomBytesNegativeSizeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataGeneratorInputStream.withRandomBytes(-1);
        });
    }

    @Test
    void testRandomBytesRepeatableWithSameSeed() throws IOException {
        DataGeneratorInputStream stream1 = DataGeneratorInputStream.withRandomBytes(100, 42L);
        DataGeneratorInputStream stream2 = DataGeneratorInputStream.withRandomBytes(100, 42L);

        for (int i = 0; i < 100; i++) {
            int b1 = stream1.read();
            int b2 = stream2.read();
            assertEquals(b1, b2, "Same seed should produce same sequence at position " + i);
        }
    }

    @Test
    void testRandomBytesDifferentSeedsProduceDifferentSequences() throws IOException {
        DataGeneratorInputStream stream1 = DataGeneratorInputStream.withRandomBytes(100, 42L);
        DataGeneratorInputStream stream2 = DataGeneratorInputStream.withRandomBytes(100, 99L);

        int differences = 0;
        for (int i = 0; i < 100; i++) {
            int b1 = stream1.read();
            int b2 = stream2.read();
            if (b1 != b2) {
                differences++;
            }
        }

        assertTrue(differences > 50, "Different seeds should produce different sequences, found " + differences + " differences");
    }

    @Test
    void testRandomBytesReadArray() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100, 42L, false);
        byte[] buffer = new byte[50];

        int read1 = stream.read(buffer, 0, 50);
        assertEquals(50, read1);

        for (int i = 0; i < 50; i++) {
            assertNotEquals(0, buffer[i], "Byte at position " + i + " should not be 0");
        }

        int read2 = stream.read(buffer, 0, 50);
        assertEquals(50, read2);

        int read3 = stream.read(buffer, 0, 50);
        assertEquals(-1, read3);
    }

    @Test
    void testRandomBytesReadArrayPartial() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(25);
        byte[] buffer = new byte[100];

        int read = stream.read(buffer, 0, 100);
        assertEquals(25, read);

        assertEquals(-1, stream.read(buffer, 0, 100));
    }

    // ========== Tests for Repeating Pattern Mode ==========

    @Test
    void testRepeatingPatternString() throws IOException {
        String pattern = "Hello";
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRepeatingPattern(15, pattern);

        byte[] expected = "HelloHelloHello".getBytes(StandardCharsets.UTF_8);
        byte[] actual = new byte[15];

        int read = stream.read(actual, 0, 15);
        assertEquals(15, read);
        assertArrayEquals(expected, actual);

        assertEquals(-1, stream.read());
    }

    @Test
    void testRepeatingPatternBytes() throws IOException {
        byte[] pattern = {0x01, 0x02, 0x03};
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRepeatingPattern(10, pattern);

        byte[] expected = {0x01, 0x02, 0x03, 0x01, 0x02, 0x03, 0x01, 0x02, 0x03, 0x01};
        byte[] actual = new byte[10];

        int read = stream.read(actual, 0, 10);
        assertEquals(10, read);
        assertArrayEquals(expected, actual);
    }

    @Test
    void testRepeatingPatternSingleByte() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRepeatingPattern(5, "X");

        for (int i = 0; i < 5; i++) {
            assertEquals('X', stream.read());
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testRepeatingPatternNullThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            DataGeneratorInputStream.withRepeatingPattern(10, (String) null);
        });

        assertThrows(NullPointerException.class, () -> {
            DataGeneratorInputStream.withRepeatingPattern(10, (byte[]) null);
        });
    }

    @Test
    void testRepeatingPatternEmptyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            DataGeneratorInputStream.withRepeatingPattern(10, "");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DataGeneratorInputStream.withRepeatingPattern(10, new byte[0]);
        });
    }

    // ========== Tests for Constant Byte Mode ==========

    @Test
    void testConstantByte() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withConstantByte(10, 'A');

        for (int i = 0; i < 10; i++) {
            assertEquals('A', stream.read());
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testConstantByteZero() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withConstantByte(5, 0);

        for (int i = 0; i < 5; i++) {
            assertEquals(0, stream.read());
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testConstantByteMaxValue() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withConstantByte(5, 255);

        for (int i = 0; i < 5; i++) {
            assertEquals(255, stream.read());
        }
        assertEquals(-1, stream.read());
    }

    // ========== Tests for Sequential Bytes Mode ==========

    @Test
    void testSequentialBytesCountingUp() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withSequentialBytes(15, 10, 14);

        // Should generate: 10, 11, 12, 13, 14, 10, 11, 12, 13, 14, 10, 11, 12, 13, 14
        byte[] expected = {10, 11, 12, 13, 14, 10, 11, 12, 13, 14, 10, 11, 12, 13, 14};
        byte[] actual = new byte[15];

        int read = stream.read(actual, 0, 15);
        assertEquals(15, read);
        assertArrayEquals(expected, actual);
    }

    @Test
    void testSequentialBytesCountingDown() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withSequentialBytes(15, 20, 16);

        // Should generate: 20, 19, 18, 17, 16, 20, 19, 18, 17, 16, 20, 19, 18, 17, 16
        byte[] expected = {20, 19, 18, 17, 16, 20, 19, 18, 17, 16, 20, 19, 18, 17, 16};
        byte[] actual = new byte[15];

        int read = stream.read(actual, 0, 15);
        assertEquals(15, read);
        assertArrayEquals(expected, actual);
    }

    @Test
    void testSequentialBytesSingleValue() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withSequentialBytes(10, 42, 42);

        for (int i = 0; i < 10; i++) {
            assertEquals(42, stream.read());
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testSequentialBytesFullRange() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withSequentialBytes(260, 0, 255);

        // First 256 bytes should be 0-255
        for (int i = 0; i <= 255; i++) {
            assertEquals(i, stream.read());
        }
        // Then it wraps: 0, 1, 2, 3
        assertEquals(0, stream.read());
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());

        assertEquals(-1, stream.read());
    }

    // ========== Tests for Random Strings Mode ==========

    @Test
    void testRandomStrings() throws IOException {
        Random random = new Random(42L);
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomStrings(100, random, 3, 8, ' ');

        byte[] buffer = new byte[100];
        int read = stream.read(buffer, 0, 100);
        assertEquals(100, read);

        String result = new String(buffer, 0, read, StandardCharsets.UTF_8);

        // Should contain spaces (separators)
        assertTrue(result.contains(" "), "Result should contain space separators");

        // Should be proper case (uppercase followed by lowercase)
        String[] words = result.split(" ");
        for (String word : words) {
            if (word.length() > 0) {
                char first = word.charAt(0);
                assertTrue(Character.isUpperCase(first), "First char should be uppercase: " + word);
            }
        }
    }

    @Test
    void testRandomStringsRepeatable() throws IOException {
        Random random1 = new Random(42L);
        DataGeneratorInputStream stream1 = DataGeneratorInputStream.withRandomStrings(100, random1, 5, 10, '\n');

        Random random2 = new Random(42L);
        DataGeneratorInputStream stream2 = DataGeneratorInputStream.withRandomStrings(100, random2, 5, 10, '\n');

        byte[] buffer1 = new byte[100];
        byte[] buffer2 = new byte[100];

        stream1.read(buffer1, 0, 100);
        stream2.read(buffer2, 0, 100);

        assertArrayEquals(buffer1, buffer2, "Same seed should produce same strings");
    }

    @Test
    void testRandomStringsNullRandomThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            DataGeneratorInputStream.withRandomStrings(100, null, 3, 8, ' ');
        });
    }

    // ========== Tests for Custom Generator Mode ==========

    @Test
    void testCustomGeneratorWithLambda() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withGenerator(5, () -> 42);

        for (int i = 0; i < 5; i++) {
            assertEquals(42, stream.read());
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testCustomGeneratorAlternating() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withGenerator(10, new IntSupplier() {
            private boolean toggle = false;
            public int getAsInt() {
                toggle = !toggle;
                return toggle ? 0xFF : 0x00;
            }
        });

        byte[] expected = {(byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00};
        byte[] actual = new byte[10];

        stream.read(actual, 0, 10);
        assertArrayEquals(expected, actual);
    }

    @Test
    void testCustomGeneratorNullThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            DataGeneratorInputStream.withGenerator(100, null);
        });
    }

    // ========== Tests for Common Stream Operations ==========

    @Test
    void testReadArrayWithOffset() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withConstantByte(50, 'X');
        byte[] buffer = new byte[100];

        int read = stream.read(buffer, 25, 50);
        assertEquals(50, read);

        for (int i = 0; i < 25; i++) {
            assertEquals(0, buffer[i], "Bytes before offset should be untouched");
        }
        for (int i = 25; i < 75; i++) {
            assertEquals('X', buffer[i], "Bytes at offset should be filled");
        }
        for (int i = 75; i < 100; i++) {
            assertEquals(0, buffer[i], "Bytes after read should be untouched");
        }
    }

    @Test
    void testReadArrayInvalidParameters() {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100);
        byte[] buffer = new byte[50];

        assertThrows(NullPointerException.class, () -> {
            stream.read(null, 0, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stream.read(buffer, -1, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stream.read(buffer, 0, -1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stream.read(buffer, 0, 51);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stream.read(buffer, 45, 10);
        });
    }

    @Test
    void testReadArrayZeroLength() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100);
        byte[] buffer = new byte[50];

        int read = stream.read(buffer, 0, 0);
        assertEquals(0, read);

        assertEquals(100, stream.available());
    }

    @Test
    void testAvailable() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100);

        assertEquals(100, stream.available());

        stream.read();
        assertEquals(99, stream.available());

        byte[] buffer = new byte[50];
        stream.read(buffer, 0, 50);
        assertEquals(49, stream.available());

        stream.read(buffer, 0, 50);
        assertEquals(0, stream.available());
    }

    @Test
    void testAvailableWithLargeSize() {
        long largeSize = 3L * Integer.MAX_VALUE;
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(largeSize);

        assertEquals(Integer.MAX_VALUE, stream.available());
    }

    @Test
    void testSkip() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100);

        long skipped = stream.skip(25);
        assertEquals(25, skipped);
        assertEquals(75, stream.available());

        skipped = stream.skip(100);
        assertEquals(75, skipped);
        assertEquals(0, stream.available());

        skipped = stream.skip(10);
        assertEquals(0, skipped);
    }

    @Test
    void testSkipNegativeOrZero() throws IOException {
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(100);

        assertEquals(0, stream.skip(0));
        assertEquals(0, stream.skip(-1));
        assertEquals(100, stream.available());
    }

    @Test
    void testSkipMaintainsRandomConsistency() throws IOException {
        DataGeneratorInputStream stream1 = DataGeneratorInputStream.withRandomBytes(1000, 42L);
        DataGeneratorInputStream stream2 = DataGeneratorInputStream.withRandomBytes(1000, 42L);

        byte[] buffer1a = new byte[100];
        stream1.read(buffer1a, 0, 100);
        stream1.skip(400);
        byte[] buffer1b = new byte[100];
        stream1.read(buffer1b, 0, 100);

        byte[] buffer2a = new byte[100];
        stream2.read(buffer2a, 0, 100);
        byte[] discard = new byte[400];
        stream2.read(discard, 0, 400);
        byte[] buffer2b = new byte[100];
        stream2.read(buffer2b, 0, 100);

        assertArrayEquals(buffer1a, buffer2a);
        assertArrayEquals(buffer1b, buffer2b);
    }

    @Test
    void testSkipWithSequentialBytes() throws IOException {
        DataGeneratorInputStream stream1 = DataGeneratorInputStream.withSequentialBytes(20, 0, 9);
        DataGeneratorInputStream stream2 = DataGeneratorInputStream.withSequentialBytes(20, 0, 9);

        // Stream 1: skip 5, then read 5
        stream1.skip(5);
        byte[] buffer1 = new byte[5];
        stream1.read(buffer1, 0, 5);

        // Stream 2: read 5, then read 5
        byte[] discard = new byte[5];
        stream2.read(discard, 0, 5);
        byte[] buffer2 = new byte[5];
        stream2.read(buffer2, 0, 5);

        assertArrayEquals(buffer1, buffer2, "Skip should maintain sequence consistency");
    }

    @Test
    void testLargeStream() throws IOException {
        long oneGB = 1024L * 1024L * 1024L;
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(oneGB);

        byte[] buffer = new byte[1024];
        int read = stream.read(buffer, 0, 1024);
        assertEquals(1024, read);

        long skipped = stream.skip(oneGB - 2048);
        assertEquals(oneGB - 2048, skipped);

        read = stream.read(buffer, 0, 1024);
        assertEquals(1024, read);

        assertEquals(-1, stream.read());
    }

    @Test
    void testMemoryEfficiency() throws IOException {
        long tenGB = 10L * 1024L * 1024L * 1024L;
        DataGeneratorInputStream stream = DataGeneratorInputStream.withRandomBytes(tenGB);

        assertNotEquals(-1, stream.read());
        assertEquals(Integer.MAX_VALUE, stream.available());
    }
}