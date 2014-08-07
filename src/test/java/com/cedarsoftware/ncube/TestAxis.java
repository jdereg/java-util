package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.AxisOverlapException;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NCube Axis Tests
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestAxis
{
    @After
    public void tearDown() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    private static boolean isValidPoint(Axis axis, Comparable value)
    {
        try
        {
            axis.addColumn(value);
            return true;
        }
        catch (AxisOverlapException e)
        {
            return false;
        }
    }

    @Test
    public void testAxisNameChange()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.setName("bar");
        assertTrue("bar".equals(axis.getName()));
    }

    @Test
    public void testAxisValueOverlap()
    {
        Axis axis = new Axis("test axis", AxisType.DISCRETE, AxisValueType.LONG, true);
        axis.addColumn(0);
        axis.addColumn(10);
        axis.addColumn(100);

        assertTrue(isValidPoint(axis, -1));
        assertFalse(isValidPoint(axis, 0));
        assertFalse(isValidPoint(axis, 10));
        assertTrue(isValidPoint(axis, 11));
        assertFalse(isValidPoint(axis, 100));
        assertTrue(isValidPoint(axis, 101));

        try
        {
            axis.addColumn(new Range(3, 9));
        }
        catch (IllegalArgumentException expected)
        {
        }

        axis = new Axis("test axis", AxisType.DISCRETE, AxisValueType.STRING, true);
        axis.addColumn("echo");
        axis.addColumn("juliet");
        axis.addColumn("tango");

        assertTrue(isValidPoint(axis, "alpha"));
        assertFalse(isValidPoint(axis, "echo"));
        assertFalse(isValidPoint(axis, "juliet"));
        assertTrue(isValidPoint(axis, "kilo"));
        assertFalse(isValidPoint(axis, "tango"));
        assertTrue(isValidPoint(axis, "uniform"));

        try
        {
            axis.addColumn(new Range(3, 9));
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void testAxisInsertAtFront()
    {
        Axis states = new Axis("States", AxisType.SET, AxisValueType.STRING, false, Axis.SORTED);
        RangeSet set = new RangeSet("GA");
        set.add("OH");
        set.add("TX");
        states.addColumn(set);
        set = new RangeSet("AL");
        set.add("WY");
        states.addColumn(set);
    }

    @Test
    public void testAxisType()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.addColumn(1);
        axis.addColumn(2L);
        axis.addColumn((byte) 3);
        axis.addColumn((short) 4);
        axis.addColumn("5");
        axis.addColumn(new BigDecimal("6"));
        axis.addColumn(new BigInteger("7"));
        assertTrue(AxisType.DISCRETE.equals(axis.getType()));
        assertTrue(AxisValueType.LONG.equals(axis.getValueType()));
        assertTrue(axis.size() == 7);
    }

    @Test
    public void testAddingNullToAxis()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.addColumn(null);   // Add default column
        assertTrue(axis.hasDefaultColumn());
        try
        {
            axis.addColumn(null);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("add"));
            assertTrue(expected.getMessage().contains("default"));
            assertTrue(expected.getMessage().contains("already"));
        }
        axis.deleteColumn(null);
        assertFalse(axis.hasDefaultColumn());
    }

    @Test
    public void testAxisGetValues()
    {
        NCube ncube = new NCube("foo");
        ncube.addAxis(TestNCube.getLongDaysOfWeekAxis());
        ncube.addAxis(TestNCube.getLongMonthsOfYear());
        ncube.addAxis(TestNCube.getOddAxis(true));
        Axis axis = (Axis) ncube.getAxes().get(0);
        List values = axis.getColumns();
        assertTrue(values.size() == 7);
        assertTrue(TestNCube.countMatches(ncube.toHtml(), "<tr>") == 44);
    }

    @Test
    public void testAxisCaseInsensitivity()
    {
        NCube<String> ncube = new NCube<String>("TestAxisCase");
        Axis gender = TestNCube.getGenderAxis(true);
        ncube.addAxis(gender);
        Axis gender2 = new Axis("gender", AxisType.DISCRETE, AxisValueType.STRING, true);

        try
        {
            ncube.addAxis(gender2);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("axis"));
            assertTrue(expected.getMessage().contains("already"));
            assertTrue(expected.getMessage().contains("exists"));
        }

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("gendeR", null);
        ncube.setCell("1", coord);
        assertTrue("1".equals(ncube.getCell(coord)));

        coord.put("GendeR", "Male");
        ncube.setCell("2", coord);
        assertTrue("2".equals(ncube.getCell(coord)));

        coord.put("GENdeR", "Female");
        ncube.setCell("3", coord);
        assertTrue("3".equals(ncube.getCell(coord)));

        Axis axis = ncube.getAxis("genDER");
        assertTrue(axis.getName().equals("Gender"));
        ncube.deleteAxis("GeNdEr");
        assertTrue(ncube.getNumDimensions() == 0);
    }

    @Test
    public void testRangeSetAxisErrors()
    {
        Axis age = new Axis("Age", AxisType.SET, AxisValueType.LONG, true);
        RangeSet set = new RangeSet(1);
        set.add(3.0);
        set.add(new Range(10, 20));
        set.add(25);
        age.addColumn(set);

        set = new RangeSet(2);
        set.add(20L);
        set.add((byte) 35);
        age.addColumn(set);

        try
        {
            set = new RangeSet(12);
            age.addColumn(set);
            fail("should throw exception");
        }
        catch (AxisOverlapException expected)
        {
            assertTrue(expected.getMessage().contains("RangeSet"));
            assertTrue(expected.getMessage().contains("overlap"));
            assertTrue(expected.getMessage().contains("exist"));
        }

        try
        {
            set = new RangeSet(15);
            age.addColumn(set);
            fail("should throw exception");
        }
        catch (AxisOverlapException expected)
        {
            assertTrue(expected.getMessage().contains("RangeSet"));
            assertTrue(expected.getMessage().contains("overlap"));
            assertTrue(expected.getMessage().contains("exist"));
        }

        try
        {
            set = new RangeSet(new Character('c')); // not a valid type for a LONG axis
            age.addColumn(set);
            fail("should throw exception");
        }
        catch (Exception expected)
        {
            assertTrue(expected instanceof IllegalArgumentException);
        }

        try
        {
            Range range = new Range(0, 10);
            age.addColumn(range);
            fail("should throw exception");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("only"));
            assertTrue(expected.getMessage().contains("add"));
            assertTrue(expected.getMessage().contains("RangeSet"));
        }

        RangeSet a = new RangeSet();
        RangeSet b = new RangeSet();
        assertTrue(a.compareTo(b) == 0);
    }

    @Test
    public void testDeleteColumnFromRangeSetAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube4.json");
        ncube.deleteColumn("code", "b");
        Axis axis = ncube.getAxis("code");
        assertTrue(axis.getId() != 0);
        assertTrue(axis.getColumns().size() == 2);
        axis.deleteColumn("o");
        assertTrue(axis.getColumns().size() == 1);
        assertTrue(axis.idToCol.size() == 1);
        assertNull(axis.deleteColumnById(9));
    }

    @Test
    public void testDupeIdsOnAxis() throws Exception
    {
        try
        {
            NCubeManager.getNCubeFromResource("idBasedCubeError2.json");
            fail("should not make it here");
        }
        catch(Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    public void testAddDefaultToNearestAxis()
    {
        Axis nearest = new Axis("points", AxisType.NEAREST, AxisValueType.COMPARABLE, false);
        try
        {
            nearest.addColumn(null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testMetaProperties() {
        Axis c = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        assertNull(c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        c.setMetaProperty("foo", "bar");
        assertEquals("bar", c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        assertNull(c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        Map map = new HashMap();
        map.put("BaZ", "qux");

        c.addMetaProperties(map);
        assertEquals("qux", c.getMetaProperties().get("baz"));
    }

    @Test
    public void convertStringToDiscreteValue()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);

        try
        {
            axis.convertStringToDiscreteValue(null, AxisValueType.DOUBLE);
            fail();
        } catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            axis.convertStringToDiscreteValue(null, AxisValueType.BIG_DECIMAL);
            fail();
        } catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            axis.convertStringToDiscreteValue(null, AxisValueType.COMPARABLE);
            fail();
        } catch (IllegalArgumentException ignore)
        {
        }


    }

    @Test
    public void testGetString() {

        assertEquals("1", Axis.getString(1));
        assertEquals("true", Axis.getString(true));
        assertSame("foo", Axis.getString("foo"));
    }
}
