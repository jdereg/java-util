package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

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
