package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

public class TestByteUtilities
{
	private byte[] _array1 = new byte[] { -1, 0};
	private byte[] _array2 = new byte[] { 0x01, 0x23, 0x45, 0x67 };
	
	private String _str1 = "FF00";
	private String _str2 = "01234567";
	
	@Test
	public void testDecode() 
	{
		Assert.assertArrayEquals(_array1, CaseInsensitiveSet.ByteUtilities.decode(_str1));
		Assert.assertArrayEquals(_array2, CaseInsensitiveSet.ByteUtilities.decode(_str2));
		Assert.assertArrayEquals(null, CaseInsensitiveSet.ByteUtilities.decode("456"));

	}
	
	@Test
	public void testEncode() 
	{
		Assert.assertEquals(_str1, CaseInsensitiveSet.ByteUtilities.encode(_array1));
		Assert.assertEquals(_str2, CaseInsensitiveSet.ByteUtilities.encode(_array2));
	}
}
