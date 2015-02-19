package com.cedarsoftware.ncube

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestStringValuePair
{
    @Test
    void testStringValuePair()
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

    @Test
    void testEquals()
    {
        StringValuePair one = new StringValuePair('foo', 'bar')
        assert one.equals(one)

        assert one == 'foo'
    }
}
