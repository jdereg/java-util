package com.cedarsoftware.ncube

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals

/**
 * Created by kpartlow on 10/24/2014.
 */
public class TestStringValuePair
{
    @Test
    public void testStringValuePair()
    {
        StringValuePair one = new StringValuePair('foo', 'bar')
        StringValuePair two = new StringValuePair('boo', 'far')

        assertEquals 'foo', one.key
        assertEquals 'bar', one.value

        assertEquals 'boo', two.key
        assertEquals 'far', two.value

        assertEquals 'foo:bar', one.toString()

        assertNotEquals one, two
        two.value = 'bar'
        assertNotEquals one, two
        two.key = 'foo'
        assertEquals one, two
        assertNotEquals one, new Integer(3)

        // keys only need to be the same.
        two.value = 'far'
        assertEquals one, two

        assertEquals two.key.hashCode(), two.hashCode()

        two.key = null
        assertEquals 0xbabe, two.hashCode()
    }
}
