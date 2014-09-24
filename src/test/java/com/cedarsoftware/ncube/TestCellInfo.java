package com.cedarsoftware.ncube;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by kpartlow on 9/23/2014.
 */
public class TestCellInfo
{

    @Test
    public void testFormatForEditing() {
        assertEquals("4.56", CellInfo.formatForEditing(4.56));
        assertEquals("0.0", CellInfo.formatForEditing(0.0));
        assertEquals("4.0", CellInfo.formatForEditing(new Float(4)));
        assertEquals("4.0", CellInfo.formatForEditing(new Double(4)));

        assertEquals("4.56", CellInfo.formatForEditing(new BigDecimal("4.56000")));
        assertEquals("4.56", CellInfo.formatForEditing(new BigDecimal("4.56")));
    }

    @Test
    public void testFormatForDisplay() {
        assertEquals("4.56", CellInfo.formatForEditing(4.560));
        assertEquals("4.5", CellInfo.formatForEditing(4.5));

        assertEquals("4.56", CellInfo.formatForEditing(new BigDecimal("4.5600")));
        assertEquals("4", CellInfo.formatForEditing(new BigDecimal("4.00")));
        assertEquals("4", CellInfo.formatForEditing(new BigDecimal("4")));
    }

    @Test
    public void testRecreate() {
        assertNull(new CellInfo(null).recreate());

        performRecreateAssertion(new StringUrlCmd("http://www.google.com", true));
        performRecreateAssertion(new Double(4.56));
        performRecreateAssertion(new Float(4.56));
        performRecreateAssertion(new Short((short)4));
        performRecreateAssertion(new Long(4));
        performRecreateAssertion(new Integer(4));
        performRecreateAssertion(new Byte((byte)4));
        performRecreateAssertion(new BigDecimal("4.56"));
        performRecreateAssertion(new BigInteger("900"));
        performRecreateAssertion(Boolean.TRUE);
        performRecreateAssertion(new GroovyExpression("0", null));
        performRecreateAssertion(new GroovyMethod("0", null));
        performRecreateAssertion(new GroovyTemplate(null, "http://www.google.com", false));
        performRecreateAssertion(new BinaryUrlCmd("http://www.google.com", false));
        performArrayRecreateAssertion(new byte[]{0, 4, 5, 6});
        performRecreateAssertion("foo");

        //  Have to special create this because milliseconds are not saved
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        performRecreateAssertion(c.getTime());
    }

    @Test
    public void testParseJsonValue() {
        assertEquals(Boolean.TRUE, CellInfo.parseJsonValue("boolean", "true"));
        assertEquals(Boolean.FALSE, CellInfo.parseJsonValue("boolean", "false"));
        assertEquals((byte)2, CellInfo.parseJsonValue("byte", "2"));
        assertEquals((short)5, CellInfo.parseJsonValue("short", "5"));
        assertEquals(9L, CellInfo.parseJsonValue("long", "9"));
        assertEquals(9, CellInfo.parseJsonValue("int", "9"));
        assertEquals(9.87d, CellInfo.parseJsonValue("double", "9.87"));
        assertEquals(9.65f, CellInfo.parseJsonValue("float", "9.65"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueBinaryWithOddNumberString() {
        CellInfo.parseJsonValue("binary", "0");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueInvalidHexString() {
        CellInfo.parseJsonValue("binary", "GF");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueWithInvalidBoolean() {
        CellInfo.parseJsonValue("boolean", "yes");

    }

    public void performRecreateAssertion(Object o) {
        assertEquals(o, new CellInfo(o).recreate());
    }

    public void performArrayRecreateAssertion(byte[] o) {
        assertArrayEquals(o, (byte[])new CellInfo(o).recreate());
    }

}
