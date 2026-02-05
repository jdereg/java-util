package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
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
public class ByteUtilitiesTest
{
	private byte[] _array1 = new byte[] { -1, 0};
	private byte[] _array2 = new byte[] { 0x01, 0x23, 0x45, 0x67 };
	
	private String _str1 = "FF00";
    private String _str2 = "01234567";

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class<?> c = ByteUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
        public void testDecode()
        {
                assertArrayEquals(_array1, ByteUtilities.decode(_str1));
                assertArrayEquals(_array2, ByteUtilities.decode(_str2));
                assertNull(ByteUtilities.decode("456"));
                assertArrayEquals(new byte[]{-1, 0}, ByteUtilities.decode("ff00"));
                assertNull(ByteUtilities.decode("GG"));
                assertNull(ByteUtilities.decode((String) null));
                StringBuilder sb = new StringBuilder(_str1);
                assertArrayEquals(_array1, ByteUtilities.decode(sb));

        }

        @Test
        public void testEncode()
        {
                assertEquals(_str1, ByteUtilities.encode(_array1));
                assertEquals(_str2, ByteUtilities.encode(_array2));
                assertNull(ByteUtilities.encode(null));
        }

    @Test
    public void testIsGzipped() {
        byte[] gzipped = {(byte)0x1f, (byte)0x8b, 0x08};
        byte[] notGzip = {0x00, 0x00, 0x00};
        byte[] embedded = {0x00, (byte)0x1f, (byte)0x8b};
        assertTrue(ByteUtilities.isGzipped(gzipped));
        assertFalse(ByteUtilities.isGzipped(notGzip));
        assertTrue(ByteUtilities.isGzipped(embedded, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    public void testToHexCharWithinRange(int value) {
        char expected = "0123456789ABCDEF".charAt(value);
        assertEquals(expected, ByteUtilities.toHexChar(value));
    }

    @Test
    public void testToHexCharMasksInput() {
        assertEquals('F', ByteUtilities.toHexChar(-1));
        assertEquals('0', ByteUtilities.toHexChar(16));
        assertEquals('5', ByteUtilities.toHexChar(0x15));
    }

    // ============ indexOf tests ============

    @Test
    public void testIndexOfBasic() {
        byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(2, ByteUtilities.indexOf(data, pattern, 0));
        assertEquals(5, ByteUtilities.indexOf(data, pattern, 3));
    }

    @Test
    public void testIndexOfSingleByte() {
        byte[] data = {0x00, 0x01, 0x02, 0x03, 0x02};
        byte[] pattern = {0x02};
        assertEquals(2, ByteUtilities.indexOf(data, pattern, 0));
        assertEquals(4, ByteUtilities.indexOf(data, pattern, 3));
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, 5));
    }

    @Test
    public void testIndexOfNotFound() {
        byte[] data = {0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x05, 0x06};
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, 0));
    }

    @Test
    public void testIndexOfAtStart() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] pattern = {0x01, 0x02};
        assertEquals(0, ByteUtilities.indexOf(data, pattern, 0));
    }

    @Test
    public void testIndexOfAtEnd() {
        byte[] data = {0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(2, ByteUtilities.indexOf(data, pattern, 0));
    }

    @Test
    public void testIndexOfNullInputs() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01};
        assertEquals(-1, ByteUtilities.indexOf(null, pattern, 0));
        assertEquals(-1, ByteUtilities.indexOf(data, null, 0));
        assertEquals(-1, ByteUtilities.indexOf(null, null, 0));
    }

    @Test
    public void testIndexOfEmptyPattern() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {};
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, 0));
    }

    @Test
    public void testIndexOfNegativeStart() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01};
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, -1));
    }

    @Test
    public void testIndexOfPatternLongerThanData() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01, 0x02, 0x03};
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, 0));
    }

    @Test
    public void testIndexOfStartBeyondValidRange() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(-1, ByteUtilities.indexOf(data, pattern, 3));  // Can't fit pattern starting at position 3
    }

    // ============ lastIndexOf tests ============

    @Test
    public void testLastIndexOfBasic() {
        byte[] data = {0x02, 0x03, 0x00, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(3, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
        assertEquals(0, ByteUtilities.lastIndexOf(data, pattern, 2));
    }

    @Test
    public void testLastIndexOfNoStartParam() {
        byte[] data = {0x02, 0x03, 0x00, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(3, ByteUtilities.lastIndexOf(data, pattern));
    }

    @Test
    public void testLastIndexOfSingleByte() {
        byte[] data = {0x02, 0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x02};
        assertEquals(3, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
        assertEquals(0, ByteUtilities.lastIndexOf(data, pattern, 2));
        assertEquals(-1, ByteUtilities.lastIndexOf(data, pattern, -1));
    }

    @Test
    public void testLastIndexOfNotFound() {
        byte[] data = {0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x05, 0x06};
        assertEquals(-1, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
    }

    @Test
    public void testLastIndexOfAtStart() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        byte[] pattern = {0x01, 0x02};
        assertEquals(0, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
    }

    @Test
    public void testLastIndexOfAtEnd() {
        byte[] data = {0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(2, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
    }

    @Test
    public void testLastIndexOfNullInputs() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01};
        assertEquals(-1, ByteUtilities.lastIndexOf(null, pattern, 0));
        assertEquals(-1, ByteUtilities.lastIndexOf(data, null, 0));
        assertEquals(-1, ByteUtilities.lastIndexOf(null, null, 0));
        assertEquals(-1, ByteUtilities.lastIndexOf(null, pattern));
    }

    @Test
    public void testLastIndexOfEmptyPattern() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {};
        assertEquals(-1, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01};
        assertEquals(-1, ByteUtilities.lastIndexOf(data, pattern, -1));
    }

    @Test
    public void testLastIndexOfPatternLongerThanData() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01, 0x02, 0x03};
        assertEquals(-1, ByteUtilities.lastIndexOf(data, pattern, data.length - 1));
    }

    @Test
    public void testLastIndexOfStartBeyondDataLength() {
        // Start is beyond array length - should still work (clamped to valid range)
        byte[] data = {0x01, 0x02, 0x03};
        byte[] pattern = {0x02, 0x03};
        assertEquals(1, ByteUtilities.lastIndexOf(data, pattern, 100));
    }

    // ============ contains tests ============

    @Test
    public void testContainsFound() {
        byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04};
        byte[] pattern = {0x01, 0x02};
        assertTrue(ByteUtilities.contains(data, pattern));
    }

    @Test
    public void testContainsNotFound() {
        byte[] data = {0x00, 0x01, 0x02, 0x03};
        byte[] pattern = {0x05, 0x06};
        assertFalse(ByteUtilities.contains(data, pattern));
    }

    @Test
    public void testContainsSingleByte() {
        byte[] data = {0x00, 0x01, 0x02};
        assertTrue(ByteUtilities.contains(data, new byte[]{0x01}));
        assertFalse(ByteUtilities.contains(data, new byte[]{0x05}));
    }

    @Test
    public void testContainsNullInputs() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01};
        assertFalse(ByteUtilities.contains(null, pattern));
        assertFalse(ByteUtilities.contains(data, null));
        assertFalse(ByteUtilities.contains(null, null));
    }

    @Test
    public void testContainsEmptyPattern() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {};
        assertFalse(ByteUtilities.contains(data, pattern));
    }

    @Test
    public void testContainsEntireArray() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] pattern = {0x01, 0x02, 0x03};
        assertTrue(ByteUtilities.contains(data, pattern));
    }

    @Test
    public void testContainsPatternLongerThanData() {
        byte[] data = {0x01, 0x02};
        byte[] pattern = {0x01, 0x02, 0x03};
        assertFalse(ByteUtilities.contains(data, pattern));
    }

    // ============ isGzipped edge cases ============

    @Test
    public void testIsGzippedNullInput() {
        assertFalse(ByteUtilities.isGzipped(null));
        assertFalse(ByteUtilities.isGzipped(null, 0));
    }

    @Test
    public void testIsGzippedInvalidOffset() {
        byte[] data = {(byte)0x1f, (byte)0x8b};
        assertFalse(ByteUtilities.isGzipped(data, -1));
        assertFalse(ByteUtilities.isGzipped(data, 2));  // Offset at end
        assertFalse(ByteUtilities.isGzipped(data, 1));  // Not enough bytes after offset
    }

    @Test
    public void testIsGzippedTooShort() {
        byte[] data = {(byte)0x1f};  // Only 1 byte
        assertFalse(ByteUtilities.isGzipped(data));
    }
}
