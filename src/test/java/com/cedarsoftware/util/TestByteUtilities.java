package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestByteUtilities
{
	private byte[] _array1 = new byte[] { -1, 0};
	private byte[] _array2 = new byte[] { 0x01, 0x23, 0x45, 0x67 };
	
	private String _str1 = "FF00";
	private String _str2 = "01234567";

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class c = ByteUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<ByteUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test
	public void testDecode() 
	{
		Assert.assertArrayEquals(_array1, ByteUtilities.decode(_str1));
		Assert.assertArrayEquals(_array2, ByteUtilities.decode(_str2));
		Assert.assertArrayEquals(null, ByteUtilities.decode("456"));

	}
	
	@Test
	public void testEncode() 
	{
		Assert.assertEquals(_str1, ByteUtilities.encode(_array1));
		Assert.assertEquals(_str2, ByteUtilities.encode(_array2));
	}
}
