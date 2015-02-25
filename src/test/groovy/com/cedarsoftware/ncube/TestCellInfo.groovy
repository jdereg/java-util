package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point2D
import com.cedarsoftware.ncube.proximity.Point3D
import com.cedarsoftware.util.io.JsonObject
import org.junit.Test

import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
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
class TestCellInfo
{
    @Test
    void testFormatForEditing()
    {
        assertEquals '4.56', CellInfo.formatForEditing(4.56)
        assertEquals '0.0', CellInfo.formatForEditing(0.0)
        assertEquals '4.0', CellInfo.formatForEditing(new Float(4))
        assertEquals '4.0', CellInfo.formatForEditing(new Double(4))

        assertEquals '4.56', CellInfo.formatForEditing(new BigDecimal('4.56000'))
        assertEquals '4.56', CellInfo.formatForEditing(new BigDecimal('4.56'))

        Calendar c = Calendar.instance
        c.set 2005, 10, 21, 12, 15, 19
        assertEquals '\"2005-11-21 12:15:19\"', CellInfo.formatForEditing(c.time)
    }

    @Test
    void testCollapseToUISupportedTypes()
    {
        CellInfo info = new CellInfo(5)
        assertEquals CellTypes.Integer.desc(), info.dataType
        info.collapseToUiSupportedTypes()
        assertEquals CellTypes.Long.desc(), info.dataType

        info = new CellInfo(new Short((short) 5))
        assertEquals CellTypes.Short.desc(), info.dataType
        info.collapseToUiSupportedTypes()
        assertEquals CellTypes.Long.desc(), info.dataType

        info = new CellInfo(new Byte((byte) 5))
        assertEquals CellTypes.Byte.desc(), info.dataType
        info.collapseToUiSupportedTypes()
        assertEquals CellTypes.Long.desc(), info.dataType

        info = new CellInfo(new Float(5))
        assertEquals CellTypes.Float.desc(), info.dataType
        info.collapseToUiSupportedTypes()
        assertEquals CellTypes.Double.desc(), info.dataType

        info = new CellInfo(new BigInteger('100', 10))
        assertEquals CellTypes.BigInteger.desc(), info.dataType
        info.collapseToUiSupportedTypes()
        assertEquals CellTypes.BigDecimal.desc(), info.dataType
    }

    @Test
    void testFormatForDisplay()
    {
        assertEquals '4.56', CellInfo.formatForEditing(4.560)
        assertEquals '4.5', CellInfo.formatForEditing(4.5)

        assertEquals '4.56', CellInfo.formatForEditing(new BigDecimal('4.5600'))
        assertEquals '4', CellInfo.formatForEditing(new BigDecimal('4.00'))
        assertEquals '4', CellInfo.formatForEditing(new BigDecimal('4'))

        assertEquals '4.56', CellInfo.formatForDisplay(new BigDecimal('4.5600'))
        assertEquals '4', CellInfo.formatForDisplay(new BigDecimal('4.00'))
        assertEquals '4', CellInfo.formatForDisplay(new BigDecimal('4'))
    }

    @Test
    void testRecreate()
    {
        assertNull new CellInfo(null).recreate()

        performRecreateAssertion new StringUrlCmd('http://www.google.com', true)
        performRecreateAssertion new Double(4.56)
        performRecreateAssertion new Float(4.56)
        performRecreateAssertion new Short((short) 4)
        performRecreateAssertion new Long(4)
        performRecreateAssertion new Integer(4)
        performRecreateAssertion new Byte((byte) 4)
        performRecreateAssertion new BigDecimal('4.56')
        performRecreateAssertion new BigInteger('900')
        performRecreateAssertion Boolean.TRUE
        performRecreateAssertion new GroovyExpression('0', null)
        performRecreateAssertion new GroovyMethod('0', null)
        performRecreateAssertion new GroovyTemplate(null, 'http://www.google.com', false)
        performRecreateAssertion new BinaryUrlCmd('http://www.google.com', false)
        performArrayRecreateAssertion([0, 4, 5, 6] as byte[])
        performRecreateAssertion 'foo'

        //  Have to special create this because milliseconds are not saved
        Calendar c = Calendar.instance
        c.set Calendar.MILLISECOND, 0
        performRecreateAssertion c.time
    }

    @Test
    void testBooleanValue()
    {
        assertTrue CellInfo.booleanValue('true')
        assertFalse CellInfo.booleanValue('false')
    }

    @Test
    void testConstructor()
    {
        CellInfo info = new CellInfo(new Point2D(5.0, 6.0))
        assertEquals CellTypes.Point2D.desc(), info.dataType

        info = new CellInfo(new Point3D(5.0, 6.0, 7.0))
        assertEquals CellTypes.Point3D.desc(), info.dataType

        info = new CellInfo(new LatLon(5.5, 5.9))
        assertEquals CellTypes.LatLon.desc(), info.dataType

        info = new CellInfo(new Range(5.5, 5.9))
        assertNull info.dataType
        assertFalse info.isCached
        assertFalse info.isUrl

        RangeSet set = new RangeSet()
        set.add new Range(0, 5)
        set.add new Range(10, 20)
        set.add 50
        info = new CellInfo(set)
        assertNull info.dataType
        assertFalse info.isUrl
    }

    @Test
    void testConstructorWithUnrecognizedType()
    {
        try
        {
            new CellInfo(new UnrecognizedConstructorObject())
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.contains('Unknown cell value type')
        }
    }

    @Test
    void testParseJsonValue()
    {
        assertEquals Boolean.TRUE, CellInfo.parseJsonValue('boolean', 'true')
        assertEquals Boolean.FALSE, CellInfo.parseJsonValue('boolean', 'false')
        assertEquals 2 as byte, CellInfo.parseJsonValue('byte', '2')
        assertEquals 5 as short, CellInfo.parseJsonValue('short', '5')
        assertEquals 9L, CellInfo.parseJsonValue('long', '9')
        assertEquals 9, CellInfo.parseJsonValue('int', '9')
        assertEquals 9.87d, CellInfo.parseJsonValue('double', '9.87'), 0.000001d
        assertEquals 9.65f, CellInfo.parseJsonValue('float', '9.65'), 0.000001f
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueBinaryWithOddNumberString()
    {
        CellInfo.parseJsonValue 'binary', '0'
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueInvalidHexString()
    {
        CellInfo.parseJsonValue 'binary', 'GF'
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueWithInvalidBoolean()
    {
        CellInfo.parseJsonValue 'boolean', 'yes'
    }

    @Test
    void testInvalidJsonObjectType()
    {
        try
        {
            JsonObject o = new JsonObject()
            Object[] items = [1]
            items[0] = new CellInfo('string', null, false, false)
            o.put('@items', items)

            CellInfo.javaToGroovySource(o)
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.contains('Unknown Groovy Type')
        }
    }

    @Test
    void testNullItemOnFormatForDisplay()
    {
        assertEquals 'Default', CellInfo.formatForDisplay(null)
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueException() throws Exception
    {
        CellInfo.parseJsonValue(Boolean.TRUE, "http://www.foo.com", "foo", true)
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueNonUrlException() throws Exception
    {
        CellInfo.parseJsonValue("blah blah blah", null, "foo", true)
    }

    @Test(expected = IllegalArgumentException.class)
    void testParseJsonValueWithUnknownType() throws Exception
    {
        CellInfo.parseJsonValue(new Object(), null, "foo", true)
    }

    @Test
    void testParseJsonValueGroovyMethod() throws Exception
    {
        GroovyMethod method = (GroovyMethod) CellInfo.parseJsonValue("def [5]", null, "method", true)
        assertEquals(new GroovyMethod("def [5]", null), method)
    }

    public void performRecreateAssertion(Object o)
    {
        if (o instanceof Float || o instanceof Double)
        {
            assertEquals o, new CellInfo(o).recreate(), 0.00001d
        }
        else
        {
            assertEquals o, new CellInfo(o).recreate()
        }
    }

    public static void performArrayRecreateAssertion(byte[] o)
    {
        assertArrayEquals o, new CellInfo(o).recreate() as byte[]
    }

    public static class UnrecognizedConstructorObject
    {
    }
}
