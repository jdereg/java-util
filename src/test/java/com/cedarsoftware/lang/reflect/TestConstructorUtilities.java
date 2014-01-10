package com.cedarsoftware.lang.reflect;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

public class TestConstructorUtilities
{

	@Test
	public void testCallPrivateConstructor()
	{
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(ConstructorUtilities.class);
	}

	@Test
	public void testExceptionThrownFromConstructorReturnsNull()
	{
		Assert.assertNull(ConstructorUtilities.callPrivateConstructor(TestCallPrivateConstructorTestClass.class));
	}
	
	private class TestCallPrivateConstructorTestClass {
		private TestCallPrivateConstructorTestClass() {
			throw new RuntimeException();
		}
	}

}
