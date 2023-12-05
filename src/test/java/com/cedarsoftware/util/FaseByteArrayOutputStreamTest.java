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
    void testReset() {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        outputStream.write(65); // ASCII for 'A'
        outputStream.reset();
        assertEquals(0, outputStream.size());
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
}

