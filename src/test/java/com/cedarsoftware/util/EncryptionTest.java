package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


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
public class EncryptionTest
{
    public static final String QUICK_FOX = "The quick brown fox jumps over the lazy dog";

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<EncryptionUtilities> con = EncryptionUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testGetDigest() {
        assertNotNull(EncryptionUtilities.getDigest("MD5"));
    }

    public void testGetDigestWithInvalidDigest() {
        try
        {
            EncryptionUtilities.getDigest("foo");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMD5()
    {
        String hash = EncryptionUtilities.calculateMD5Hash(QUICK_FOX.getBytes());
        assertEquals("9E107D9D372BB6826BD81D3542A419D6", hash);
        assertNull(EncryptionUtilities.calculateMD5Hash((byte[])null));
    }

    @Test
    public void testSHA1()
    {
        String hash = EncryptionUtilities.calculateSHA1Hash(QUICK_FOX.getBytes());
        assertEquals("2FD4E1C67A2D28FCED849EE1BB76E7391B93EB12", hash);
        assertNull(EncryptionUtilities.calculateSHA1Hash(null));
    }

    @Test
    public void testSHA256()
    {
        String hash = EncryptionUtilities.calculateSHA256Hash(QUICK_FOX.getBytes());
        assertEquals("D7A8FBB307D7809469CA9ABCB0082E4F8D5651E46D3CDB762D02D0BF37C9E592", hash);
        assertNull(EncryptionUtilities.calculateSHA256Hash(null));
    }

    @Test
    public void testSHA512()
    {
        String hash = EncryptionUtilities.calculateSHA512Hash(QUICK_FOX.getBytes());
        assertEquals("07E547D9586F6A73F73FBAC0435ED76951218FB7D0C8D788A309D785436BBB642E93A252A954F23912547D1E8A3B5ED6E1BFD7097821233FA0538F3DB854FEE6", hash);
        assertNull(EncryptionUtilities.calculateSHA512Hash(null));
    }

    public void testEncryptWithNull()
    {

        try
        {
            EncryptionUtilities.encrypt("GavynRocks", (String)null);
            fail("Should not make it here.");
        }
        catch (IllegalStateException e)
        {
        }
    }

    @Test
    public void testFastMd5WithIoException()
    {
        assertNull(EncryptionUtilities.fastMD5(new File("foo/bar/file")));
    }

    public void testFastMd5WithNull()
    {
        try
        {
            assertNull(EncryptionUtilities.fastMD5(null));
            fail("should not make it here");
        }
        catch (NullPointerException e)
        {
        }
    }

    @Test
    public void testFastMd50BytesReturned() throws Exception {
        Class<?> c = FileChannel.class;

        FileChannel f = mock(FileChannel.class);
        when(f.read(any(ByteBuffer.class))).thenReturn(0).thenReturn(-1);

        EncryptionUtilities.calculateFileHash(f, EncryptionUtilities.getMD5Digest());
    }



    @Test
    public void testFastMd5()
    {
        URL u = EncryptionTest.class.getClassLoader().getResource("fast-md5-test.txt");
        assertEquals("188F47B5181320E590A6C3C34AD2EE75", EncryptionUtilities.fastMD5(new File(u.getFile())));
    }


    @Test
    public void testEncrypt()
    {
        String res = EncryptionUtilities.encrypt("GavynRocks", QUICK_FOX);
        assertEquals("E68D5CD6B1C0ACD0CC4E2B9329911CF0ADD37A6A18132086C7E17990B933EBB351C2B8E0FAC40B371450FA899C695AA2", res);
        assertEquals(QUICK_FOX, EncryptionUtilities.decrypt("GavynRocks", res));
        try
        {
            assertEquals(QUICK_FOX, EncryptionUtilities.decrypt("NcubeRocks", res));
            fail();
        }
        catch (IllegalStateException ignored) { }
        String diffRes = EncryptionUtilities.encrypt("NcubeRocks", QUICK_FOX);
        assertEquals("2A6EF54E3D1EEDBB0287E6CC690ED3879C98E55942DA250DC5FE0D10C9BD865105B1E0B4F8E8C389BEF11A85FB6C5F84", diffRes);
        assertEquals(QUICK_FOX, EncryptionUtilities.decrypt("NcubeRocks", diffRes));
    }

    @Test
    public void testEncryptBytes()
    {
        String res = EncryptionUtilities.encryptBytes("GavynRocks", QUICK_FOX.getBytes());
        assertEquals("E68D5CD6B1C0ACD0CC4E2B9329911CF0ADD37A6A18132086C7E17990B933EBB351C2B8E0FAC40B371450FA899C695AA2", res);
        assertTrue(DeepEquals.deepEquals(QUICK_FOX.getBytes(), EncryptionUtilities.decryptBytes("GavynRocks", res)));
        try
        {
            assertEquals(QUICK_FOX, EncryptionUtilities.decrypt("NcubeRocks", res));
            fail();
        }
        catch (IllegalStateException ignored) { }
        String diffRes = EncryptionUtilities.encryptBytes("NcubeRocks", QUICK_FOX.getBytes());
        assertEquals("2A6EF54E3D1EEDBB0287E6CC690ED3879C98E55942DA250DC5FE0D10C9BD865105B1E0B4F8E8C389BEF11A85FB6C5F84", diffRes);
        assertTrue(DeepEquals.deepEquals(QUICK_FOX.getBytes(), EncryptionUtilities.decryptBytes("NcubeRocks", diffRes)));
    }

    @Test
    public void testEncryptBytesBadInput()
    {
        try
        {
            EncryptionUtilities.encryptBytes("GavynRocks", null);
            fail();
        }
        catch(IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("rror"));
            assertTrue(e.getMessage().contains("encrypt"));
        }
    }

    @Test
    public void testDecryptBytesBadInput()
    {
        try
        {
            EncryptionUtilities.decryptBytes("GavynRocks", null);
            fail();
        }
        catch(IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("rror"));
            assertTrue(e.getMessage().contains("ecrypt"));
        }
    }
}
