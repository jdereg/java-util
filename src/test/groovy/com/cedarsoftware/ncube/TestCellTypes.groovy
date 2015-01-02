package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point2D
import com.cedarsoftware.ncube.proximity.Point3D
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

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
public class TestCellTypes
{
    @Test
    public void testInvalidCellType()
    {
        try
        {
            CellTypes.getType new StringBuilder(), 'cells'
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertEquals 'Unsupported type java.lang.StringBuilder found in cells', e.message
        }
    }

    @Test
    public void testGetType()
    {
        assertEquals 'boolean', CellTypes.getType(Boolean.TRUE, 'cells')
        assertEquals 'int', CellTypes.getType(Integer.MAX_VALUE, 'cells')
        assertEquals 'long', CellTypes.getType(Long.MAX_VALUE, 'cells')
        assertEquals 'double', CellTypes.getType(Double.MAX_VALUE, 'cells')
        assertEquals 'float', CellTypes.getType(Float.MAX_VALUE, 'cells')
        assertNull 'float', CellTypes.getType(null, 'cells')
    }

    @Test
    public void testGetTypeFromString()
    {
        assertEquals CellTypes.String, CellTypes.getTypeFromString(null)
        assertEquals CellTypes.String, CellTypes.getTypeFromString('string')
        assertEquals CellTypes.Date, CellTypes.getTypeFromString('date')
        assertEquals CellTypes.Boolean, CellTypes.getTypeFromString('boolean')
        assertEquals CellTypes.Byte, CellTypes.getTypeFromString('byte')
        assertEquals CellTypes.Short, CellTypes.getTypeFromString('short')
        assertEquals CellTypes.Integer, CellTypes.getTypeFromString('int')
        assertEquals CellTypes.Long, CellTypes.getTypeFromString('long')
        assertEquals CellTypes.Float, CellTypes.getTypeFromString('float')
        assertEquals CellTypes.Double, CellTypes.getTypeFromString('double')
        assertEquals CellTypes.BigDecimal, CellTypes.getTypeFromString('bigdec')
        assertEquals CellTypes.BigInteger, CellTypes.getTypeFromString('bigint')
        assertEquals CellTypes.Binary, CellTypes.getTypeFromString('binary')
        assertEquals CellTypes.Exp, CellTypes.getTypeFromString('exp')
        assertEquals CellTypes.Method, CellTypes.getTypeFromString('method')
        assertEquals CellTypes.Template, CellTypes.getTypeFromString('template')
        assertEquals CellTypes.LatLon, CellTypes.getTypeFromString('latlon')
        assertEquals CellTypes.Point2D, CellTypes.getTypeFromString('point2d')
        assertEquals CellTypes.Point3D, CellTypes.getTypeFromString('point3d')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidType()
    {
        CellTypes.getTypeFromString('foo')
    }

    @Test
    public void testRecreate()
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
        performRecreateAssertion 'foo'

        performRecreateAssertion new LatLon(5.5, 5.8)
        performRecreateAssertion new Point2D(5.5, 5.8)
        performRecreateAssertion new Point3D(5.5, 5.8, 5.9)

        //  Have to special create this because milliseconds are not saved
        Calendar c = Calendar.instance
        c.set Calendar.MILLISECOND, 0
        performRecreateAssertion c.time
    }

    @Test
    public void testRecreateExceptions()
    {
        try
        {
            CellTypes.LatLon.recreate 'foo', false, false
        }
        catch (Exception e)
        {
            assertTrue e.message.contains('Invalid Lat/Long')
        }

        try
        {
            CellTypes.Point2D.recreate 'foo', false, false
        }
        catch (Exception e)
        {
            assertTrue e.message.contains('Invalid Point2D')
        }

        try
        {
            CellTypes.Point3D.recreate 'foo', false, false
        }
        catch (Exception e)
        {
            assertTrue e.message.contains('Invalid Point3D')
        }
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
}
