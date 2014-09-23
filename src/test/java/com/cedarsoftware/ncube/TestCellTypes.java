package com.cedarsoftware.ncube;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by kpartlow on 9/1/2014.
 */
public class TestCellTypes
{
    @Test
    public void testInvalidCellType()
    {
        try
        {
            CellTypes.getType(new StringBuilder(), "cells");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Unsupported type java.lang.StringBuilder found in cells", e.getMessage());
        }
    }

    @Test
    public void testGetType()
    {
        assertEquals("boolean", CellTypes.getType(Boolean.TRUE, "cells"));
        assertEquals("int", CellTypes.getType(Integer.MAX_VALUE, "cells"));
        assertEquals("long", CellTypes.getType(Long.MAX_VALUE, "cells"));
        assertEquals("double", CellTypes.getType(Double.MAX_VALUE, "cells"));
        assertEquals("float", CellTypes.getType(Float.MAX_VALUE, "cells"));
    }

    @Test
    public void testGetTypeFromString() {
        assertEquals(CellTypes.String, CellTypes.getTypeFromString(null));
        assertEquals(CellTypes.String, CellTypes.getTypeFromString("string"));
        assertEquals(CellTypes.Date, CellTypes.getTypeFromString("date"));
        assertEquals(CellTypes.Boolean, CellTypes.getTypeFromString("boolean"));
        assertEquals(CellTypes.Byte, CellTypes.getTypeFromString("byte"));
        assertEquals(CellTypes.Short, CellTypes.getTypeFromString("short"));
        assertEquals(CellTypes.Integer, CellTypes.getTypeFromString("int"));
        assertEquals(CellTypes.Long, CellTypes.getTypeFromString("long"));
        assertEquals(CellTypes.Float, CellTypes.getTypeFromString("float"));
        assertEquals(CellTypes.Double, CellTypes.getTypeFromString("double"));
        assertEquals(CellTypes.BigDecimal, CellTypes.getTypeFromString("bigdec"));
        assertEquals(CellTypes.BigInteger, CellTypes.getTypeFromString("bigint"));
        assertEquals(CellTypes.Binary, CellTypes.getTypeFromString("binary"));
        assertEquals(CellTypes.Exp, CellTypes.getTypeFromString("exp"));
        assertEquals(CellTypes.Method, CellTypes.getTypeFromString("method"));
        assertEquals(CellTypes.Template, CellTypes.getTypeFromString("template"));
        assertEquals(CellTypes.LatLon, CellTypes.getTypeFromString("latlon"));
        assertEquals(CellTypes.Point2D, CellTypes.getTypeFromString("point2d"));
        assertEquals(CellTypes.Point3D, CellTypes.getTypeFromString("point3d"));
    }

}
