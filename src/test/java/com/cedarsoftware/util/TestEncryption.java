package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestEncryption
{
    public static final String QUICK_FOX = "The quick brown fox jumps over the lazy dog";

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<EncryptionUtilities> con = EncryptionUtilities.class.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testGetDigest() {
        assertNotNull(EncryptionUtilities.getDigest("MD5"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetDigestWithInvalidDigest() {
        EncryptionUtilities.getDigest("foo");
    }

    @Test
    public void testMD5()
    {
        String hash = EncryptionUtilities.calculateMD5Hash(QUICK_FOX.getBytes());
        assertEquals("9E107D9D372BB6826BD81D3542A419D6", hash);
        assertNull(EncryptionUtilities.calculateMD5Hash(null));
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
}
