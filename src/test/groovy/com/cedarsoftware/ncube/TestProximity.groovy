package com.cedarsoftware.ncube

import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

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

public class TestProximity
{
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgument()
    {
        Proximity.distance new BigInteger('9'), new BigInteger('11')
    }

    @Test
    public void testProximityBigDecimal()
    {
        BigDecimal a = 1.0g
        BigDecimal b = 101.0g
        double d = Proximity.distance(b, a)
        assertTrue(d == 100.0)
    }

    @Test
    public void testConstructorIsPrivate() throws Exception
    {
        Class c = Proximity.class;
        assertEquals Modifier.FINAL, c.modifiers & Modifier.FINAL

        Constructor<Proximity> con = c.getDeclaredConstructor();
        assertEquals Modifier.PRIVATE, con.modifiers & Modifier.PRIVATE
        con.accessible = true;

        assertNotNull con.newInstance()
    }

    @Test
    public void testProximity()
    {
        try
        {
            Proximity.distance(null, "hey")
            fail("should not make it here")
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('neither source nor target can be null')
            assert e.message.toLowerCase().contains('nearest')
        }

        try
        {
            Proximity.distance("yo", null)
            fail("should not make it here")
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('neither source nor target can be null')
            assert e.message.toLowerCase().contains('nearest')
        }

        try
        {
            Proximity.distance("yo", 16)
            fail("should not make it here")
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('source and target data types')
            assert e.message.toLowerCase().contains('must be the same')
            assert e.message.toLowerCase().contains('nearest')
        }
    }
}
