package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
