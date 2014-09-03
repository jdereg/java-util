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
}
