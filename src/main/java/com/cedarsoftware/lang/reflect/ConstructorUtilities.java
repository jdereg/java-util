package com.cedarsoftware.lang.reflect;

import java.lang.reflect.Constructor;

public final class ConstructorUtilities
{
	private ConstructorUtilities() 
	{
		super();
	}
	
	public static <T> T callPrivateConstructor(Class<T> c)
	{
		try 
		{
    		Constructor<T> con = c.getDeclaredConstructor();
    		con.setAccessible(true);
    		return con.newInstance();
		} 
		catch (Exception e)
		{
			//  don't instantiate on error and return null.
			return null;
		}
	}
}
