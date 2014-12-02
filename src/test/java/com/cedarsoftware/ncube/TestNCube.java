package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.AxisOverlapException;
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.CaseInsensitiveMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NCube tests.
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
public class TestNCube
{
    private static final boolean _debug = false;

    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }

    @Test
    public void testPopulateProductLineCube() throws Exception
    {
        NCube<Object> ncube = new NCube<>("ProductLine");
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Axis prodLine = new Axis("PROD_LINE", AxisType.DISCRETE, AxisValueType.STRING, false);
        prodLine.addColumn("CommAuto");
        prodLine.addColumn("CommGL");
        prodLine.addColumn("CommIM");
        prodLine.addColumn("SBPProperty");
        ncube.addAxis(prodLine);

        Axis bu = new Axis("BU", AxisType.DISCRETE, AxisValueType.STRING, true);
        ncube.addAxis(bu);

        NCube<String> commAuto = new NCube<>("CommAuto");
        NCubeManager.addCube(commAuto.getApplicationID(), commAuto);
        Axis caAttr = new Axis("Attribute", AxisType.DISCRETE, AxisValueType.STRING, false);
        caAttr.addColumn("busType");
        caAttr.addColumn("riskType");
        caAttr.addColumn("longNm");
        caAttr.addColumn("policySymbol");
        commAuto.addAxis(caAttr);

        NCube<String> commGL = new NCube<>("CommGL");
        NCubeManager.addCube(commGL.getApplicationID(), commGL);
        Axis glAttr = new Axis("Attribute", AxisType.DISCRETE, AxisValueType.STRING, false);
        glAttr.addColumn("busType");
        glAttr.addColumn("riskType");
        glAttr.addColumn("longNm");
        glAttr.addColumn("policySymbol");
        commGL.addAxis(glAttr);

        NCube<String> commIM = new NCube<>("CommIM");
        NCubeManager.addCube(commIM.getApplicationID(), commIM);
        Axis imAttr = new Axis("Attribute", AxisType.DISCRETE, AxisValueType.STRING, false);
        imAttr.addColumn("busType");
        imAttr.addColumn("riskType");
        imAttr.addColumn("longNm");
        imAttr.addColumn("policySymbol");
        imAttr.addColumn("parentRiskType");
        commIM.addAxis(imAttr);

        NCube<String> commSBP = new NCube<>("SBPProperty");
        NCubeManager.addCube(commSBP.getApplicationID(), commSBP);
        Axis sbpAttr = new Axis("Attribute", AxisType.DISCRETE, AxisValueType.STRING, false);
        sbpAttr.addColumn("busType");
        sbpAttr.addColumn("riskType");
        sbpAttr.addColumn("longNm");
        sbpAttr.addColumn("policySymbol");
        sbpAttr.addColumn("busLobCd");
        commSBP.addAxis(sbpAttr);

        // Add cells to main table
        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("BU", null);    // default column
        coord.put("PROD_LINE", "CommAuto");
        ncube.setCell(new GroovyExpression("@CommAuto([:])", null), coord);
        coord.put("PROD_LINE", "CommGL");
        ncube.setCell(new GroovyExpression("@CommGL(input)", null), coord);
        coord.put("PROD_LINE", "CommIM");
        ncube.setCell(new GroovyExpression("$CommIM(input)", null), coord);
        coord.put("PROD_LINE", "SBPProperty");
        ncube.setCell(new GroovyExpression("$SBPProperty(input)", null), coord);

        coord.clear();
        coord.put("Attribute", "busType");
        commAuto.setCell("COB", coord);
        coord.put("Attribute", "riskType");
        commAuto.setCell("AUTOPS", coord);
        coord.put("Attribute", "longNm");
        commAuto.setCell("Commercial Auto", coord);
        coord.put("Attribute", "policySymbol");
        commAuto.setCell("CAP", coord);

        coord.clear();
        coord.put("Attribute", "busType");
        commGL.setCell("COB", coord);
        coord.put("Attribute", "riskType");
        commGL.setCell("CGLOPS", coord);
        coord.put("Attribute", "longNm");
        commGL.setCell("Commercial General Liability", coord);
        coord.put("Attribute", "policySymbol");
        commGL.setCell("GLP", coord);

        coord.clear();
        coord.put("Attribute", "busType");
        commIM.setCell("COB", coord);
        coord.put("Attribute", "riskType");
        commIM.setCell("EQPT", coord);
        coord.put("Attribute", "longNm");
        commIM.setCell("Contractors Equipment", coord);
        coord.put("Attribute", "policySymbol");
        commIM.setCell("MAC", coord);
        coord.put("Attribute", "parentRiskType");
        commIM.setCell("IMOPS", coord);

        coord.clear();
        coord.put("Attribute", "busType");
        commSBP.setCell("COB", coord);
        coord.put("Attribute", "riskType");
        commSBP.setCell("SBPOPS", coord);
        coord.put("Attribute", "longNm");
        commSBP.setCell("Select Business Policy", coord);
        coord.put("Attribute", "policySymbol");
        commSBP.setCell("MAC", coord);
        coord.put("Attribute", "busLobCd");
        commSBP.setCell("PPTY-SBP", coord);

        assertTrue(ncube.toHtml() != null);

        // ------------ Lookup into the Main table, and let it cascade to the children tables -------
        coord.clear();
        coord.put("BU", "Agri");
        coord.put("PROD_LINE", "CommAuto");
        coord.put("Attribute", "riskType");
        String riskType = (String) ncube.getCell(coord);
        assertTrue("AUTOPS".equals(riskType));

        Set<String> optionalScope = ncube.getOptionalScope();
        optionalScope = ncube.getOptionalScope();   // 2nd time to force fetch from cache
        assertEquals(1, optionalScope.size());
        assertTrue(optionalScope.contains("bu"));

        Set<String> requiredScope = ncube.getRequiredScope();
        println("requiredScope 2 cubes = " + requiredScope);
        assertTrue(requiredScope.size() == 1);
        assertTrue(requiredScope.contains("PROD_LINE"));

        requiredScope = commAuto.getRequiredScope();
        assertEquals(1, requiredScope.size());
        assertTrue(requiredScope.contains("attribute"));
        optionalScope = commAuto.getOptionalScope();
        assertEquals(0, optionalScope.size());

        coord.clear();
        coord.put("BU", "Agri");
        coord.put("PROD_LINE", "CommGL");
        coord.put("Attribute", "riskType");

        requiredScope = ncube.getRequiredScope();
        println("requiredScope 2 cubes = " + requiredScope);
        assertTrue(requiredScope.size() == 1);
        assertTrue(requiredScope.contains("PROD_LINE"));
    }

    @Test
    public void testDuplicateAxisName()
    {
        NCube<Byte> ncube = new NCube<Byte>("Byte.Cube");
        ncube.setDefaultCellValue((byte) -1);
        Axis axis1 = getGenderAxis(true);
        ncube.addAxis(axis1);
        Axis axis2 = getShortMonthsOfYear();
        ncube.addAxis(axis2);
        Axis axis3 = getGenderAxis(false);

        try
        {
            ncube.addAxis(axis3);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue("should throw exception", true);
        }

        ncube.deleteAxis("miss");
        ncube.deleteAxis("Gender");
        ncube.addAxis(getGenderAxis(true));
        ncube.toString();    // Force some APIs to be called during toString()
        assertTrue(ncube.toHtml() != null);
        assertTrue(ncube.getNumDimensions() == 2);
    }

    @Test
    public void testDefaultColumnOnly()
    {
        // 1D: 1 cell
        NCube<String> ncube = new NCube<String>("defaultOnly");
        ncube.addAxis(new Axis("BU", AxisType.DISCRETE, AxisValueType.STRING, true));

        Map coord = new HashMap();
        coord.put("BU", "foo");

        ncube.setCell("financial", coord);
        String s = ncube.getCell(coord);
        coord.put("BU", "bar");
        String t = ncube.getCell(coord);
        assertTrue("financial".equals(s));
        assertTrue(s.equals(t));

        // 2D: 1 cell (both axis only have default column)
        NCube<String> ncube2 = new NCube<String>("defaultOnly");
        ncube2.addAxis(new Axis("BU", AxisType.DISCRETE, AxisValueType.STRING, true));
        ncube2.addAxis(new Axis("age", AxisType.RANGE, AxisValueType.LONG, true));

        coord.clear();
        coord.put("BU", "foo");
        coord.put("age", 25);

        ncube2.setCell("bank", coord);
        s = ncube2.getCell(coord);
        coord.put("BU", "bar");
        t = ncube2.getCell(coord);
        coord.put("age", 19);
        String u = ncube2.getCell(coord);
        assertTrue("bank".equals(s));
        assertTrue(s.equals(t));
        assertTrue(t.equals(u));
        assertTrue(ncube2.toHtml() != null);
    }

    @Test
    public void testAllCellsInBigCube()
    {
        long start = System.nanoTime();
        NCube<Long> ncube = new NCube("bigCube");

        for (int i = 0; i < 5; i++)
        {
            Axis axis = new Axis("axis" + i, AxisType.DISCRETE, AxisValueType.LONG, i % 2 == 0);
            ncube.addAxis(axis);
            for (int j = 0; j < 10; j++)
            {
                if (j % 2 == 0)
                {
                    axis.addColumn(j);
                }
                else
                {
                    ncube.addColumn("axis" + i, j);
                }
            }
        }

        Map coord = new HashMap();
        for (int a = 1; a <= 11; a++)
        {
            coord.put("axis0", a - 1);
            for (int b = 1; b <= 10; b++)
            {
                coord.put("axis1", b - 1);
                for (int c = 1; c <= 11; c++)
                {
                    coord.put("axis2", c - 1);
                    for (int d = 1; d <= 10; d++)
                    {
                        coord.put("axis3", d - 1);
                        for (long e = 1; e <= 11; e++)
                        {
                            coord.put("axis4", e - 1);
                            ncube.setCell(a * b * c * d * e, coord);
                        }
                    }
                }
            }
        }

        for (int a = 1; a <= 11; a++)
        {
            coord.put("axis0", a - 1);
            for (int b = 1; b <= 10; b++)
            {
                coord.put("axis1", b - 1);
                for (int c = 1; c <= 11; c++)
                {
                    coord.put("axis2", c - 1);
                    for (int d = 1; d <= 10; d++)
                    {
                        coord.put("axis3", d - 1);
                        for (long e = 1; e <= 11; e++)
                        {
                            coord.put("axis4", e - 1);
                            long v = ncube.getCell(coord);
                            assertTrue(v == a * b * c * d * e);
                        }
                    }
                }
            }
        }
        long stop = System.nanoTime();
        double diff = (stop - start) / 1000.0;
//        println("time to build and read allCellsInBigCube = " + diff / 1000.0);
//        assertTrue(ncube.toHtml() != null);
    }

    @Test
    public void testRangeOverlap()
    {
        Axis axis = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true);
        axis.addColumn(new Range(0, 18));
        axis.addColumn(new Range(18, 30));
        axis.addColumn(new Range(65, 80));

        assertFalse(isValidRange(axis, new Range(17, 20)));
        assertFalse(isValidRange(axis, new Range(18, 20)));
        assertTrue(isValidRange(axis, new Range(30, 65)));
        assertFalse(isValidRange(axis, new Range(40, 50)));
        assertTrue(isValidRange(axis, new Range(80, 100)));
        assertFalse(isValidRange(axis, new Range(-150, 150)));
        assertTrue(axis.size() == 6);

        // Edge and Corner cases
        try
        {
            axis.addColumn(17);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            axis.addColumn(new Range(-10, -10));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Using Long's as Dates (Longs, Date, or Calendar allowed)
        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DATE, true);
        axis.addColumn(new Range(0L, 18L));
        axis.addColumn(new Range(18L, 30L));
        axis.addColumn(new Range(65L, 80L));

        assertFalse(isValidRange(axis, new Range(17L, 20L)));
        assertFalse(isValidRange(axis, new Range(18L, 20L)));
        assertTrue(isValidRange(axis, new Range(30L, 65L)));
        assertFalse(isValidRange(axis, new Range(40L, 50L)));
        assertTrue(isValidRange(axis, new Range(80L, 100L)));
        assertFalse(isValidRange(axis, new Range(-150L, 150L)));
        assertTrue(axis.size() == 6);

        // Edge and Corner cases
        try
        {
            axis.addColumn(17);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            axis.addColumn(new Range(-10L, -10L));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DOUBLE, true);
        axis.addColumn(new Range(0, 18));
        axis.addColumn(new Range(18, 30));
        axis.addColumn(new Range(65, 80));

        assertFalse(isValidRange(axis, new Range(17, 20)));
        assertFalse(isValidRange(axis, new Range(18, 20)));
        assertTrue(isValidRange(axis, new Range(30, 65)));
        assertFalse(isValidRange(axis, new Range(40, 50)));
        assertTrue(isValidRange(axis, new Range(80, 100)));
        assertFalse(isValidRange(axis, new Range(-150, 150)));
        assertTrue(axis.size() == 6);

        // Edge and Corner cases
        try
        {
            axis.addColumn(17);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            axis.addColumn(new Range(-10, -10));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException e)
        {
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.BIG_DECIMAL, true);
        axis.addColumn(new Range(0, 18));
        axis.addColumn(new Range(18, 30));
        axis.addColumn(new Range(65, 80));

        assertFalse(isValidRange(axis, new Range(17, 20)));
        assertFalse(isValidRange(axis, new Range(18, 20)));
        assertTrue(isValidRange(axis, new Range(30, 65)));
        assertFalse(isValidRange(axis, new Range(40, 50)));
        assertTrue(isValidRange(axis, new Range(80, 100)));
        assertFalse(isValidRange(axis, new Range(-150, 150)));
        assertTrue(axis.size() == 6);

        // Edge and Corner cases
        try
        {
            axis.addColumn(17);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            axis.addColumn(new Range(-10, -10));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    private boolean isValidRange(Axis axis, Range range)
    {
        try
        {
            axis.addColumn(range);
            return true;
        }
        catch (AxisOverlapException e)
        {
            return false;
        }
    }

    @Test
    public void testNoDefaultColumn()
    {
        NCube<Boolean> ncube = getTestNCube3D_Boolean();

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Trailers", "S1A");
        coord.put("Vehicles", "car");
        coord.put("BU", "Agri");
        Boolean v = ncube.getCell(coord);
        assertNull(v);
        ncube.setCell(true, coord);
        v = ncube.getCell(coord);
        assertTrue(v);
        ncube.toHtml(); // Use to test 3D visually

        try
        {
            coord.put("BU", "bogus");
            ncube.getCell(coord);
            fail("should throw exception");
        }
        catch (CoordinateNotFoundException expected)
        {
            assertTrue(expected.getMessage().contains("alue"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("found"));
            assertTrue(expected.getMessage().contains("axis"));
        }
    }

    @Test
    public void testDefaultColumn()
    {
        NCube<Boolean> ncube = new NCube<Boolean>("Test.Default.Column");
        Axis axis = getGenderAxis(true);
        ncube.addAxis(axis);

        Map male = new HashMap();
        male.put("Gender", "Male");
        Map female = new HashMap();
        female.put("Gender", "Female");
        Map nullGender = new HashMap();
        nullGender.put("Gender", null);

        ncube.setCell(true, male);
        ncube.setCell(false, female);
        ncube.setCell(true, nullGender);

        assertTrue(ncube.getCell(male));
        assertFalse(ncube.getCell(female));
        assertTrue(ncube.getCell(nullGender));

        ncube.setCell(false, male);
        ncube.setCell(true, female);
        ncube.setCell(null, nullGender);

        assertFalse(ncube.getCell(male));
        assertTrue(ncube.getCell(female));
        assertNull(ncube.getCell(nullGender));

        Map coord = new HashMap();
        coord.put("Gender", "missed");
        ncube.setCell(true, coord);
        coord.put("Gender", "yes missed");
        assertTrue(ncube.getCell(coord));
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);
    }

    @Test
    public void testBig5D() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("big5D.json");
        long start = System.nanoTime();
        List<Map<String, Object>> list = ncube.generateNCubeTests();
        long end = System.nanoTime();
        assertTrue((end - start) / 1000000.0 < 1000);   // verify that it runs in under 1 second (actual 87ms)
        NCubeTest test = (NCubeTest)list.get(0);
        assertEquals(5, test.getCoord().length);
        assertEquals("test-001", test.getName());
    }

    @Test
    public void testClearCells()
    {
        NCube<Boolean> ncube = new NCube<Boolean>("Test.Default.Column");
        Axis axis = getGenderAxis(true);
        ncube.addAxis(axis);

        Map male = new HashMap();
        male.put("Gender", "Male");
        Map female = new HashMap();
        female.put("Gender", "Female");
        Map nullGender = new HashMap();
        nullGender.put("Gender", null);

        ncube.setCell(true, male);
        ncube.setCell(false, female);
        ncube.setCell(true, nullGender);

        ncube.clearCells();

        assertNull(ncube.getCell(male));
        assertNull(ncube.getCell(female));
        assertNull(ncube.getCell(nullGender));
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);
    }


    @Test
    public void testIllegalArrayExceptions()
    {
        NCube<Object> ncube = getTestNCube2D(true);
        try
        {
            ncube.setCell(new Object[]{}, new HashMap());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot set"));
            assertTrue(e.getMessage().contains("array type"));
        }

        try
        {
            ncube.setCellById(new Object[]{}, new HashSet<Long>());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot set"));
            assertTrue(e.getMessage().contains("array type"));
        }
    }


    @Test
    public void testDefaultNCubeCellValue()
    {
        NCube<Double> ncube = getTestNCube2D(true);
        ncube.setDefaultCellValue(3.0);        // Non-set cells will return this value

        Map coord = new HashMap();
        coord.put("Gender", "Male");
        coord.put("Age", 18);

        ncube.setCell(21.0, coord);
        Double x = ncube.getCell(coord);
        assertTrue(x == 21.0);
        coord.put("Age", 65);
        x = ncube.getCell(coord);
        assertTrue(x == 3.0);
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testStringAxis()
    {
        NCube<Integer> ncube = new NCube<Integer>("SingleStringAxis");
        Axis genderAxis = getGenderAxis(false);
        ncube.addAxis(genderAxis);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Gender", "Male");
        ncube.setCell(0, coord);
        coord.put("Gender", "Female");
        ncube.setCell(1, coord);

        try
        {
            genderAxis.addColumn(new Date());
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        coord.put("Gender", "Male");
        assertTrue(ncube.getCell(coord) == 0);
        coord.put("Gender", "Female");
        assertTrue(ncube.getCell(coord) == 1);
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 3);

        try
        {
            coord.put("Gender", "Jones");
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (CoordinateNotFoundException expected)
        {
            assertTrue(expected.getMessage().contains("alue"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("found"));
            assertTrue(expected.getMessage().contains("axis"));
        }

        // 'null' value to find on String axis:
        try
        {
            coord.put("Gender", null);
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Illegal value to find on String axis:
        try
        {
            coord.put("Gender", 8);
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (CoordinateNotFoundException expected)
        {
        }

        // 'null' for coordinate
        try
        {
            ncube.getCell((Map)null);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // 0-length coordinate
        try
        {
            coord.clear();
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // coordinate / table dimension mismatch
        try
        {
            coord.clear();
            coord.put("State", "OH");
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void testLongAxis()
    {
        NCube<String> ncube = new NCube<String>("Long.test");
        ncube.addAxis(getEvenAxis(false));

        Map coord = new HashMap();
        coord.put("Even", (byte) 0);
        ncube.setCell("zero", coord);
        coord.put("Even", (short) 2);
        ncube.setCell("two", coord);
        coord.put("Even", (int) 4);
        ncube.setCell("four", coord);
        coord.put("Even", (long) 6);
        ncube.setCell("six", coord);
        coord.put("Even", "8");
        ncube.setCell("eight", coord);
        coord.put("Even", new BigInteger("10"));
        ncube.setCell("ten", coord);

        coord.put("Even", 0);
        assertTrue("zero".equals(ncube.getCell(coord)));
        coord.put("Even", 2L);
        assertTrue("two".equals(ncube.getCell(coord)));
        coord.put("Even", (short) 4);
        assertTrue("four".equals(ncube.getCell(coord)));
        coord.put("Even", (byte) 6);
        assertTrue("six".equals(ncube.getCell(coord)));
        coord.put("Even", new BigInteger("8"));
        assertTrue("eight".equals(ncube.getCell(coord)));
        coord.put("Even", "10");
        assertTrue("ten".equals(ncube.getCell(coord)));

        // Value not on axis
        try
        {
            coord.put("Even", 1);
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (CoordinateNotFoundException expected)
        {
            assertTrue(expected.getMessage().contains("alue"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("found"));
            assertTrue(expected.getMessage().contains("axis"));
        }

        // Illegal value to find on LONG axis:
        try
        {
            coord.put("Even", new File("foo"));
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        ncube.toString(); // force code in toString() to execute
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 7);
    }

    @Test
    public void testLongAxis2()
    {
        Axis axis = new Axis("Long axis", AxisType.DISCRETE, AxisValueType.LONG, true);
        axis.addColumn((byte) 0);
        axis.addColumn((short) 1);
        axis.addColumn((int) 2);
        axis.addColumn((long) 3);
        axis.addColumn("4");
        axis.addColumn(new BigInteger("5"));
        axis.addColumn(new BigDecimal("6"));

        try
        {
            axis.addColumn(new Date());
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void testBigDecimalRangeAxis()
    {
        NCube<String> ncube = new NCube<String>("Big.Decimal.Range");
        Axis axis = getDecimalRangeAxis(false);
        ncube.addAxis(axis);

        subTestErrorCases(axis);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("bigD", (byte) -10);
        ncube.setCell("JSON", coord);
        coord.put("bigD", (short) 20);
        ncube.setCell("XML", coord);
        coord.put("bigD", (int) 100);
        ncube.setCell("YAML", coord);
        coord.put("bigD", 10000L);
        ncube.setCell("PNG", coord);
        coord.put("bigD", "100000");
        ncube.setCell("JPEG", coord);

        coord = new HashMap();
        coord.put("bigD", (byte) -10);
        assertTrue("JSON".equals(ncube.getCell(coord)));
        coord.put("bigD", (short) 20);
        assertTrue("XML".equals(ncube.getCell(coord)));
        coord.put("bigD", (int) 100);
        assertTrue("YAML".equals(ncube.getCell(coord)));
        coord.put("bigD", 10000L);
        assertTrue("PNG".equals(ncube.getCell(coord)));
        coord.put("bigD", "100000");
        assertTrue("JPEG".equals(ncube.getCell(coord)));

        coord.put("bigD", (double) -10);
        ncube.setCell("JSON", coord);
        coord.put("bigD", (float) 20);
        ncube.setCell("XML", coord);
        coord.put("bigD", new BigInteger("100"));
        ncube.setCell("YAML", coord);
        coord.put("bigD", new BigDecimal("10000"));
        ncube.setCell("PNG", coord);
        coord.put("bigD", "100000");
        ncube.setCell("JPEG", coord);

        coord.put("bigD", (double) -10);
        assertTrue("JSON".equals(ncube.getCell(coord)));
        coord.put("bigD", (float) 20);
        assertTrue("XML".equals(ncube.getCell(coord)));
        coord.put("bigD", 100);
        assertTrue("YAML".equals(ncube.getCell(coord)));
        coord.put("bigD", 10000L);
        assertTrue("PNG".equals(ncube.getCell(coord)));
        coord.put("bigD", "100000");
        assertTrue("JPEG".equals(ncube.getCell(coord)));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 6);
        subTestEdgeCases(ncube, "bigD");
    }

    @Test
    public void testDoubleRangeAxis()
    {
        NCube<String> ncube = new NCube<String>("Double.Range");
        Axis axis = getDoubleRangeAxis(false);
        ncube.addAxis(axis);

        subTestErrorCases(axis);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("doubleRange", (byte) -10);
        ncube.setCell("JSON", coord);
        coord.put("doubleRange", (short) 20);
        ncube.setCell("XML", coord);
        coord.put("doubleRange", (int) 100);
        ncube.setCell("YAML", coord);
        coord.put("doubleRange", 10000L);
        ncube.setCell("PNG", coord);
        coord.put("doubleRange", "100000");
        ncube.setCell("JPEG", coord);

        coord.put("doubleRange", (byte) -10);
        assertTrue("JSON".equals(ncube.getCell(coord)));
        coord.put("doubleRange", (short) 20);
        assertTrue("XML".equals(ncube.getCell(coord)));
        coord.put("doubleRange", (int) 100);
        assertTrue("YAML".equals(ncube.getCell(coord)));
        coord.put("doubleRange", 10000L);
        assertTrue("PNG".equals(ncube.getCell(coord)));
        coord.put("doubleRange", "100000");
        assertTrue("JPEG".equals(ncube.getCell(coord)));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 6);
        subTestEdgeCases(ncube, "doubleRange");
    }

    @Test
    public void testLongRangeAxis()
    {
        NCube<String> ncube = new NCube<String>("Long.Range");
        Axis axis = getLongRangeAxis(false);
        ncube.addAxis(axis);

        subTestErrorCases(axis);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("longRange", (byte) -10);
        ncube.setCell("JSON", coord);
        coord.put("longRange", (short) 20);
        ncube.setCell("XML", coord);
        coord.put("longRange", (int) 100);
        ncube.setCell("YAML", coord);
        coord.put("longRange", 10000L);
        ncube.setCell("PNG", coord);
        coord.put("longRange", "100000");
        ncube.setCell("JPEG", coord);

        coord.put("longRange", (byte) -10);
        assertTrue("JSON".equals(ncube.getCell(coord)));
        coord.put("longRange", (short) 20);
        assertTrue("XML".equals(ncube.getCell(coord)));
        coord.put("longRange", (int) 100);
        assertTrue("YAML".equals(ncube.getCell(coord)));
        coord.put("longRange", 10000L);
        assertTrue("PNG".equals(ncube.getCell(coord)));
        coord.put("longRange", "100000");
        assertTrue("JPEG".equals(ncube.getCell(coord)));
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 6);

        subTestEdgeCases(ncube, "longRange");
    }

    @Test
    public void testDateRangeAxis()
    {
        NCube<String> ncube = new NCube<>("Date.Range");
        Axis axis = getDateRangeAxis(false);
        ncube.addAxis(axis);

        subTestErrorCases(axis);

        Calendar cal = Calendar.getInstance();
        cal.set(1990, 5, 10, 13, 5, 25);
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2000, 0, 1, 0, 0, 0);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2002, 11, 17, 0, 0, 0);
        Calendar cal3 = Calendar.getInstance();
        cal3.set(2008, 11, 24, 0, 0, 0);
        Calendar cal4 = Calendar.getInstance();
        cal4.set(2010, 0, 1, 12, 0, 0);
        Calendar cal5 = Calendar.getInstance();
        cal5.set(2014, 7, 1, 12, 59, 58);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("dateRange", cal);
        ncube.setCell("JSON", coord);
        coord.put("dateRange", cal1.getTime());
        ncube.setCell("XML", coord);
        coord.put("dateRange", cal2.getTime().getTime());
        ncube.setCell("YAML", coord);
        coord.put("dateRange", cal4);
        ncube.setCell("PNG", coord);

        coord.put("dateRange", cal);
        assertTrue("JSON".equals(ncube.getCell(coord)));
        coord.put("dateRange", cal1);
        assertTrue("XML".equals(ncube.getCell(coord)));
        coord.put("dateRange", cal2);
        assertTrue("YAML".equals(ncube.getCell(coord)));
        coord.put("dateRange", cal4);
        assertTrue("PNG".equals(ncube.getCell(coord)));

        assertFalse(axis.contains(99));
        assertTrue(axis.contains(cal5));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
        subTestEdgeCases(ncube, "dateRange");
    }

    private void subTestEdgeCases(NCube<String> cube, String axis)
    {
        Map coord = new HashMap();
        try
        {
            coord.put(axis, -20);
            cube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
        }

        // 'null' value to find on String axis:
        try
        {
            coord.put(axis, null);
            cube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Illegal value to find on String axis:
        try
        {
            coord.put(axis, new File("foo"));
            cube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException eexpected)
        {
        }
    }

    private void subTestErrorCases(Axis axis)
    {
        // non-range being added
        try
        {
            axis.addColumn(new Long(7));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with null low
        try
        {
            axis.addColumn(new Range(null, 999));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with null high
        try
        {
            axis.addColumn(new Range(777, null));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with bad low
        try
        {
            axis.addColumn(new Range("no", "999"));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with bad high
        try
        {
            axis.addColumn(new Range("999", "yes"));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with bad low class type
        try
        {
            axis.addColumn(new Range(new File("foo"), "999"));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Range with bad high
        try
        {
            axis.addColumn(new Range("999", new File("foo")));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void testRange()
    {
        new Range(); // test default constructor

        Range x = new Range(0, 1);
        x.toString();    // so it gets called at least once.

        NCube<Double> ncube = new NCube<Double>("RangeTest");
        Axis axis = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true);
        axis.addColumn(new Range(22, 18));
        axis.addColumn(new Range(30, 22));
        ncube.addAxis(axis);
        Map<String, Object> coord = new TreeMap<String, Object>();
        coord.put("Age", 17);
        ncube.setCell(1.1, coord);    // set in default column
        assertTrue(ncube.getCell(coord) == 1.1);
        coord.put("Age", 18);
        ncube.setCell(2.0, coord);
        assertTrue(ncube.getCell(coord) == 2.0);
        coord.put("Age", 21);
        assertTrue(ncube.getCell(coord) == 2.0);
        coord.put("Age", 22);
        assertTrue(ncube.getCell(coord) == null);    // cell not set, therefore it should return null
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);

        try
        {
            new Range(null, 1);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("value"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("null"));
        }
        try
        {
            new Range(1, null);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("value"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("null"));
        }
    }

    @Test
    public void testRangeWithDefault()
    {
        NCube<Double> ncube = new NCube<Double>("RangeTest");
        Axis axis = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true);
        ncube.addAxis(axis);

        Map coord = new HashMap();
        coord.put("age", 1);
        ncube.setCell(1.0, coord);
        assertEquals((Object) 1.0, ncube.getCell(coord));

        axis.addColumn(new Range(18, 22));
        coord.put("age", 18);
        ncube.setCell(2.0, coord);
        assertEquals((Object) 2.0, ncube.getCell(coord));

        axis.addColumn(new Range(5, 8));
        coord.put("age", 6);
        ncube.setCell(3.0, coord);
        assertEquals((Object) 3.0, ncube.getCell(coord));

        axis.addColumn(new Range(30, 40));
        coord.put("age", 35);
        ncube.setCell(4.0, coord);
        assertEquals((Object) 4.0, ncube.getCell(coord));

        axis.addColumn(new Range(1, 4));
        coord.put("age", 1);
        ncube.setCell(5.0, coord);
        assertEquals((Object) 5.0, ncube.getCell(coord));

        axis.addColumn(new Range(40, 50));
        coord.put("age", 40);
        ncube.setCell(6.0, coord);
        assertEquals((Object) 6.0, ncube.getCell(coord));
    }

    @Test
    public void testGetCellWithMap()
    {
        NCube<Double> ncube = getTestNCube2D(false);
        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Gender", "Male");
        coord.put("Age", 39);
        ncube.setCell(9.9, coord);
        assertTrue(ncube.getCell(coord) == 9.9);
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);

        coord.put("Gender", "Fmale");    // intentional
        try
        {
            ncube.setCell(9.9, coord);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("found"));
            assertTrue(expected.getMessage().contains("axis"));
            assertTrue(expected.getMessage().contains("Gender"));
        }
    }

    @Test
    public void testWithImproperMapCoordinate()
    {
        // 'null' Map
        NCube<Double> ncube = getTestNCube2D(false);
        try
        {
            ncube.setCell(9.9, null);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("null"));
            assertTrue(expected.getMessage().contains("coordinate"));
        }

        // Empty Map
        Map<String, Object> coord = new HashMap<String, Object>();
        try
        {
            ncube.setCell(9.9, coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("contain"));
            assertTrue(expected.getMessage().contains("required"));
        }

        // Map with not enough dimensions
        coord.put("Gender", "Male");
        try
        {
            ncube.setCell(9.9, coord);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("contain"));
            assertTrue(expected.getMessage().contains("Age"));
        }
    }

    @Test
    public void testNullCoordinate()
    {
        NCube<Boolean> ncube = getTestNCube3D_Boolean();
        ncube.setDefaultCellValue(false);

        Map coord = new HashMap();
        coord.put("Trailers", "L1A");
        coord.put("Vehicles", "car");
        coord.put("BU", "Agri");
        ncube.setCell(true, coord);

        coord.put("Trailers", "M3A");
        coord.put("Vehicles", "med truck");
        coord.put("BU", "SHS");
        ncube.setCell(true, coord);
        try
        {
            ncube.getCell((Map)null);    // (Object[]) cast makes it the whole argument list
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("null"));
            assertTrue(expected.getMessage().contains("coordinate"));
        }

        try
        {
            coord.remove("BU");
            ncube.getCell(coord);        // (Object) cast makes it one argument
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("contain"));
            assertTrue(expected.getMessage().contains("key"));
        }

        try
        {
            coord.put("Trailers", null);
            coord.put("Vehicles", null);
            coord.put("BU", null);
            ncube.getCell(coord);        // Valid 3D coordinate (if table had default col on all axis)
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("null"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("default"));
            assertTrue(expected.getMessage().contains("column"));
        }
    }

    @Test
    public void testCommandCellLookup()
    {
        NCube<Object> continentCounty = new NCube<Object>("ContinentCountries");
        NCubeManager.addCube(continentCounty.getApplicationID(), continentCounty);
        continentCounty.addAxis(getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<Object>("Provinces");
        NCubeManager.addCube(canada.getApplicationID(), canada);
        canada.addAxis(getProvincesAxis());

        NCube<Object> usa = new NCube<Object>("States");
        NCubeManager.addCube(usa.getApplicationID(), usa);
        usa.addAxis(getStatesAxis());

        Map<String, Object> coord1 = new HashMap<String, Object>();
        coord1.put("Continent", "North America");
        coord1.put("Country", "USA");
        coord1.put("State", "OH");

        Map<String, Object> coord2 = new HashMap<String, Object>();
        coord2.put("Continent", "North America");
        coord2.put("Country", "Canada");
        coord2.put("Province", "Quebec");

        continentCounty.setCell(new GroovyExpression("$States(input)", null), coord1);
        continentCounty.setCell(new GroovyExpression("$Provinces(input)", null), coord2);

        usa.setCell(1.0, coord1);
        canada.setCell(0.78, coord2);

        assertTrue((Double) continentCounty.getCell(coord1) == 1.0);
        assertTrue((Double) continentCounty.getCell(coord2) == 0.78);
        assertTrue(countMatches(continentCounty.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testBadCommandCellLookup()
    {
        NCube<Object> continentCounty = new NCube<Object>("ContinentCountries");
        NCubeManager.addCube(continentCounty.getApplicationID(), continentCounty);
        continentCounty.addAxis(getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<Object>("Provinces");
        NCubeManager.addCube(canada.getApplicationID(), canada);
        canada.addAxis(getProvincesAxis());

        NCube<Object> usa = new NCube<Object>("States");
        NCubeManager.addCube(usa.getApplicationID(), usa);
        usa.addAxis(getStatesAxis());

        Map<String, Object> coord1 = new HashMap<String, Object>();
        coord1.put("Continent", "North America");
        coord1.put("Country", "USA");
        coord1.put("State", "OH");

        Map<String, Object> coord2 = new HashMap<String, Object>();
        coord2.put("Continent", "North America");
        coord2.put("Country", "Canada");
        coord2.put("Province", "Quebec");

        continentCounty.setCell(new GroovyExpression("$StatesX(input)", null), coord1);
        continentCounty.setCell(new GroovyExpression("$Provinces(input)", null), coord2);

        usa.setCell(1.0, coord1);
        canada.setCell(0.78, coord2);

        try
        {
            assertTrue((Double) continentCounty.getCell(coord1) == 1.0);
            fail("should throw exception");
        }
        catch (Exception expected)
        {
        }
        assertTrue((Double) continentCounty.getCell(coord2) == 0.78);
    }

    @Test
    public void testAddingDeletingColumn1D()
    {
        NCube<Long> ncube = new NCube<Long>("1D.Delete.Test");
        Axis states = new Axis("States", AxisType.DISCRETE, AxisValueType.STRING, true);
        states.addColumn("IN");
        states.addColumn("OH");
        states.addColumn("WY");
        ncube.addAxis(states);
        Map<String, Object> coord = new HashMap<String, Object>();

        coord.put("States", "IN");
        ncube.setCell(1111L, coord);
        assertTrue(ncube.getCell(coord) == 1111L);
        coord.put("States", "OH");
        ncube.setCell(2222L, coord);
        assertTrue(ncube.getCell(coord) == 2222L);
        coord.put("States", "WY");
        ncube.setCell(3333L, coord);
        assertTrue(ncube.getCell(coord) == 3333L);
        coord.put("States", null);
        ncube.setCell(9999L, coord);

        // Add new Column
        states.addColumn("AZ");

        coord.put("States", "IN");
        assertTrue(ncube.getCell(coord) == 1111L);
        coord.put("States", "OH");
        assertTrue(ncube.getCell(coord) == 2222L);
        coord.put("States", "WY");
        assertTrue(ncube.getCell(coord) == 3333L);

        coord.put("States", "AZ");
        ncube.setCell(4444L, coord);
        assertTrue(ncube.getCell(coord) == 4444L);

        coord.put("States", "IN");
        int numCells = ncube.getNumCells();
        assertTrue(ncube.deleteColumn("States", "IN"));
        assertTrue(numCells == ncube.getNumCells() + 1);

        assertTrue(ncube.getCell(coord) == 9999L);
        coord.put("States", "OH");
        assertTrue(ncube.getCell(coord) == 2222L);
        coord.put("States", "WY");
        assertTrue(ncube.getCell(coord) == 3333L);

        coord.put("States", "AZ");
        ncube.setCell(4444L, coord);
        assertTrue(ncube.getCell(coord) == 4444L);

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testAddingDeletingColumn2D()
    {
        NCube<Double> ncube = new NCube<Double>("2D.Delete.Test");
        Axis states = new Axis("States", AxisType.DISCRETE, AxisValueType.STRING, true);
        states.addColumn("IN");
        states.addColumn("OH");
        ncube.addAxis(states);
        Axis age = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true);
        age.addColumn(new Range(18, 30));
        age.addColumn(new Range(30, 50));
        age.addColumn(new Range(50, 80));
        ncube.addAxis(age);

        Map<String, Object> coord = new HashMap<String, Object>();

        coord.put("States", "IN");
        coord.put("Age", "18");
        ncube.setCell(1.0, coord);
        coord.put("Age", "30");
        ncube.setCell(2.0, coord);
        coord.put("Age", "50");
        ncube.setCell(3.0, coord);
        coord.put("Age", "90");
        ncube.setCell(4.0, coord);

        coord.put("States", "OH");
        coord.put("Age", 29);
        ncube.setCell(10.0, coord);
        coord.put("Age", 30);
        ncube.setCell(20.0, coord);
        coord.put("Age", 50);
        ncube.setCell(30.0, coord);
        coord.put("Age", 80);
        ncube.setCell(40.0, coord);

        coord.put("States", "WY");        // default col
        coord.put("Age", 20.0);
        ncube.setCell(100.0, coord);
        coord.put("Age", 40.0);
        ncube.setCell(200.0, coord);
        coord.put("Age", 60.0);
        ncube.setCell(300.0, coord);
        coord.put("Age", 80.0);
        ncube.setCell(400.0, coord);

        ncube.deleteColumn("Age", 90);
        assertTrue(age.size() == 3);
        assertTrue(age.hasDefaultColumn() == false);    // default column was deleted.

        assertTrue(ncube.getNumCells() == 9);
        assertTrue(ncube.deleteColumn("Age", 18));
        assertTrue(ncube.getNumCells() == 6);
        assertTrue(age.size() == 2);
        assertTrue(ncube.deleteColumn("States", "IN"));
        assertTrue(ncube.getNumCells() == 4);
        assertTrue(states.size() == 2);

        coord.put("States", "OH");
        coord.put("Age", 30);
        assertTrue(ncube.getCell(coord) == 20.0);
        coord.put("Age", 50);
        assertTrue(ncube.getCell(coord) == 30.0);

        coord.put("States", "WY");
        coord.put("Age", 40.0);
        assertTrue(ncube.getCell(coord) == 200.0);
        coord.put("Age", 60.0);
        assertTrue(ncube.getCell(coord) == 300.0);

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);
    }

    @Test
    public void testDeleteColumnNotFound()
    {
        NCube<Boolean> ncube = new NCube("yo");
        Axis axis = getGenderAxis(false);
        ncube.addAxis(axis);
        assertFalse(ncube.deleteColumn("Gender", "blah"));
    }

    @Test
    public void testColumnOrder() throws Exception
    {
        NCube ncube = new NCube("columnOrder");
        Axis axis = getShortDaysOfWeekAxis();
        axis.setColumnOrder(Axis.SORTED);
        ncube.addAxis(axis);
        List<Column> cols = axis.getColumns();
        assertTrue(cols.get(0).getValue().equals("Fri"));
        assertTrue(cols.get(1).getValue().equals("Mon"));
        assertTrue(cols.get(2).getValue().equals("Sat"));
        assertTrue(cols.get(3).getValue().equals("Sun"));
        assertTrue(cols.get(4).getValue().equals("Thu"));
        assertTrue(cols.get(5).getValue().equals("Tue"));
        assertTrue(cols.get(6).getValue().equals("Wed"));

        axis.setColumnOrder(Axis.DISPLAY);
        List<Column> cols2 = axis.getColumns();
        assertTrue(cols2.get(0).getValue().equals("Mon"));
        assertTrue(cols2.get(1).getValue().equals("Tue"));
        assertTrue(cols2.get(2).getValue().equals("Wed"));
        assertTrue(cols2.get(3).getValue().equals("Thu"));
        assertTrue(cols2.get(4).getValue().equals("Fri"));
        assertTrue(cols2.get(5).getValue().equals("Sat"));
        assertTrue(cols2.get(6).getValue().equals("Sun"));

        // Delete middle
        ncube.deleteColumn("Days", "Wed");

        axis.setColumnOrder(Axis.SORTED);
        cols = axis.getColumns();
        assertTrue(cols.get(0).getValue().equals("Fri"));
        assertTrue(cols.get(1).getValue().equals("Mon"));
        assertTrue(cols.get(2).getValue().equals("Sat"));
        assertTrue(cols.get(3).getValue().equals("Sun"));
        assertTrue(cols.get(4).getValue().equals("Thu"));
        assertTrue(cols.get(5).getValue().equals("Tue"));

        axis.setColumnOrder(Axis.DISPLAY);
        cols2 = axis.getColumns();
        assertTrue(cols2.get(0).getValue().equals("Mon"));
        assertTrue(cols2.get(1).getValue().equals("Tue"));
        assertTrue(cols2.get(2).getValue().equals("Thu"));
        assertTrue(cols2.get(3).getValue().equals("Fri"));
        assertTrue(cols2.get(4).getValue().equals("Sat"));
        assertTrue(cols2.get(5).getValue().equals("Sun"));

        // Ensure no gaps left in display order after column is removed
        assertTrue(cols2.get(0).getDisplayOrder() == 0);
        assertTrue(cols2.get(1).getDisplayOrder() == 1);
        assertTrue(cols2.get(2).getDisplayOrder() == 3);
        assertTrue(cols2.get(3).getDisplayOrder() == 4);
        assertTrue(cols2.get(4).getDisplayOrder() == 5);
        assertTrue(cols2.get(5).getDisplayOrder() == 6);

        // Delete First
        ncube.deleteColumn("Days", "Mon");
        cols2 = axis.getColumns();
        assertTrue(cols2.get(0).getValue().equals("Tue"));
        assertTrue(cols2.get(1).getValue().equals("Thu"));
        assertTrue(cols2.get(2).getValue().equals("Fri"));
        assertTrue(cols2.get(3).getValue().equals("Sat"));
        assertTrue(cols2.get(4).getValue().equals("Sun"));

        // Ensure no gaps left in display order after column is removed
        assertTrue(cols2.get(0).getDisplayOrder() == 1);
        assertTrue(cols2.get(1).getDisplayOrder() == 3);
        assertTrue(cols2.get(2).getDisplayOrder() == 4);
        assertTrue(cols2.get(3).getDisplayOrder() == 5);
        assertTrue(cols2.get(4).getDisplayOrder() == 6);

        // Delete Last
        ncube.deleteColumn("Days", "Sun");
        cols2 = axis.getColumns();
        assertTrue(cols2.get(0).getValue().equals("Tue"));
        assertTrue(cols2.get(1).getValue().equals("Thu"));
        assertTrue(cols2.get(2).getValue().equals("Fri"));
        assertTrue(cols2.get(3).getValue().equals("Sat"));

        // Ensure no gaps left in display order after column is removed
        assertTrue(cols2.get(0).getDisplayOrder() == 1);
        assertTrue(cols2.get(1).getDisplayOrder() == 3);
        assertTrue(cols2.get(2).getDisplayOrder() == 4);
        assertTrue(cols2.get(3).getDisplayOrder() == 5);
    }

    @Test
    public void testMoveColumn()
    {
        NCube<Integer> ncube = new NCube<Integer>("moveColTest");
        Axis days = getShortDaysOfWeekAxis();
        Axis gender = getGenderAxis(true);
        ncube.addAxis(days);
        ncube.addAxis(gender);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Gender", "Female");
        coord.put("Days", "Mon");
        ncube.setCell(1, coord);
        coord.put("Days", "Tue");
        ncube.setCell(2, coord);
        coord.put("Days", "Wed");
        ncube.setCell(3, coord);
        coord.put("Days", "Thu");
        ncube.setCell(4, coord);
        coord.put("Days", "Fri");
        ncube.setCell(5, coord);
        coord.put("Days", "Sat");
        ncube.setCell(6, coord);
        coord.put("Days", "Sun");
        ncube.setCell(7, coord);

        coord.put("Gender", "Male");
        coord.put("Days", "Mon");
        ncube.setCell(10, coord);
        coord.put("Days", "Tue");
        ncube.setCell(20, coord);
        coord.put("Days", "Wed");
        ncube.setCell(30, coord);
        coord.put("Days", "Thu");
        ncube.setCell(40, coord);
        coord.put("Days", "Fri");
        ncube.setCell(50, coord);
        coord.put("Days", "Sat");
        ncube.setCell(60, coord);
        coord.put("Days", "Sun");
        ncube.setCell(70, coord);

        days.moveColumn(6, 0);
        List<Column> cols = days.getColumns();
        assertTrue(cols.get(0).getDisplayOrder() == 0);
        assertTrue(cols.get(1).getDisplayOrder() == 1);
        assertTrue(cols.get(2).getDisplayOrder() == 2);
        assertTrue(cols.get(3).getDisplayOrder() == 3);
        assertTrue(cols.get(4).getDisplayOrder() == 4);
        assertTrue(cols.get(5).getDisplayOrder() == 5);
        assertTrue(cols.get(6).getDisplayOrder() == 6);

        assertTrue("Sun".equals(cols.get(0).getValue()));
        assertTrue("Mon".equals(cols.get(1).getValue()));
        assertTrue("Tue".equals(cols.get(2).getValue()));
        assertTrue("Wed".equals(cols.get(3).getValue()));
        assertTrue("Thu".equals(cols.get(4).getValue()));
        assertTrue("Fri".equals(cols.get(5).getValue()));
        assertTrue("Sat".equals(cols.get(6).getValue()));

        coord.put("Gender", "Female");
        coord.put("Days", "Mon");
        assertTrue(ncube.getCell(coord) == 1);
        coord.put("Days", "Tue");
        assertTrue(ncube.getCell(coord) == 2);
        coord.put("Days", "Wed");
        assertTrue(ncube.getCell(coord) == 3);
        coord.put("Days", "Thu");
        assertTrue(ncube.getCell(coord) == 4);
        coord.put("Days", "Fri");
        assertTrue(ncube.getCell(coord) == 5);
        coord.put("Days", "Sat");
        assertTrue(ncube.getCell(coord) == 6);
        coord.put("Days", "Sun");
        assertTrue(ncube.getCell(coord) == 7);

        coord.put("Gender", "Male");
        coord.put("Days", "Mon");
        assertTrue(ncube.getCell(coord) == 10);
        coord.put("Days", "Tue");
        assertTrue(ncube.getCell(coord) == 20);
        coord.put("Days", "Wed");
        assertTrue(ncube.getCell(coord) == 30);
        coord.put("Days", "Thu");
        assertTrue(ncube.getCell(coord) == 40);
        coord.put("Days", "Fri");
        assertTrue(ncube.getCell(coord) == 50);
        coord.put("Days", "Sat");
        assertTrue(ncube.getCell(coord) == 60);
        coord.put("Days", "Sun");
        assertTrue(ncube.getCell(coord) == 70);

        try
        {
            gender.moveColumn(-1, 1);
            assertTrue("should throw exception", false);
        }
        catch (IllegalStateException expected)
        {
            assertTrue(expected.getMessage().contains("must"));
            assertTrue(expected.getMessage().contains("DISPLAY"));
            assertTrue(expected.getMessage().contains("order"));
        }

        assertTrue(ncube.moveColumn("Days", 2, 2));

        try
        {
            days.moveColumn(-1, 1);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains(">= 0"));
            assertTrue(expected.getMessage().contains("< number"));
        }
    }

    @Test
    public void testColumnApis()
    {
        NCube ncube = new NCube("columnApis");
        Axis axis = getShortMonthsOfYear();
        ncube.addAxis(axis);
        try
        {
            ncube.addColumn("foo", "13th month");
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("add"));
            assertTrue(expected.getMessage().contains("column"));
        }

        try
        {
            ncube.deleteColumn("foo", "13th month");
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("delete"));
            assertTrue(expected.getMessage().contains("column"));
        }

        try
        {
            ncube.moveColumn("foo", 0, 1);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("move"));
            assertTrue(expected.getMessage().contains("column"));
        }
    }

    @Test
    public void testGenericComparables()
    {
        NCube<String> ncube = new NCube<String>("Test.BigInteger");
        Axis age = new Axis("Age", AxisType.DISCRETE, AxisValueType.COMPARABLE, true);
        age.addColumn(new BigInteger("1"));
        age.addColumn(new BigInteger("2"));
        age.addColumn(new BigInteger("4"));
        age.addColumn(new BigInteger("7"));
        age.addColumn(new BigInteger("10"));
        ncube.addAxis(age);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Age", new BigInteger("1"));
        ncube.setCell("alpha", coord);
        coord.put("Age", new BigInteger("2"));
        ncube.setCell("bravo", coord);
        coord.put("Age", new BigInteger("3"));    // should land it default column
        ncube.setCell("charlie", coord);
        coord.put("Age", new BigInteger("4"));
        ncube.setCell("delta", coord);

        coord.put("Age", new BigInteger("1"));
        assertTrue("alpha".equals(ncube.getCell(coord)));
        coord.put("Age", new BigInteger("2"));
        assertTrue("bravo".equals(ncube.getCell(coord)));
        coord.put("Age", new BigInteger("5"));        // Verify default column
        assertTrue("charlie".equals(ncube.getCell(coord)));
        coord.put("Age", new BigInteger("4"));
        assertTrue("delta".equals(ncube.getCell(coord)));
    }

    @Test
    public void testGenericRangeComparables()
    {
        NCube<String> ncube = new NCube<String>("Test.Character");
        Axis codes = new Axis("codes", AxisType.RANGE, AxisValueType.COMPARABLE, true);
        codes.addColumn(new Range('a', 'd'));
        codes.addColumn(new Range('d', 'm'));
        codes.addColumn(new Range('m', 'y'));
        ncube.addAxis(codes);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("codes", 'a');
        ncube.setCell("alpha", coord);
        coord.put("codes", 'd');
        ncube.setCell("bravo", coord);
        coord.put("codes", 't');    // should land it default column
        ncube.setCell("charlie", coord);
        coord.put("codes", 'z');
        ncube.setCell("delta", coord);

        coord.put("codes", 'a');
        assertTrue("alpha".equals(ncube.getCell(coord)));
        coord.put("codes", 'd');
        assertTrue("bravo".equals(ncube.getCell(coord)));
        coord.put("codes", 't');    // Verify default column
        assertTrue("charlie".equals(ncube.getCell(coord)));
        coord.put("codes", '@');
        assertTrue("delta".equals(ncube.getCell(coord)));

        Range range = new Range(10, 50);
        assertTrue(range.isWithin(null) == 1);
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testRangeSet()
    {
        NCube<Double> ncube = new NCube<Double>("RangeSetTest");
        Axis age = new Axis("Age", AxisType.SET, AxisValueType.LONG, true);
        RangeSet set = new RangeSet(1);
        set.add(3.0);
        set.add(new Range(10, 20));
        set.add(25);
        assertTrue(set.size() == 4);
        age.addColumn(set);

        set = new RangeSet(2);
        set.add(20L);
        set.add((byte) 35);
        assertTrue(set.size() == 3);
        age.addColumn(set);
        ncube.addAxis(age);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Age", 1);
        ncube.setCell(1.0, coord);
        coord.put("Age", 2);
        ncube.setCell(2.0, coord);
        coord.put("Age", 99);
        ncube.setCell(99.9, coord);

        coord.clear();
        coord.put("age", 1);        // intentional case mismatch
        assertTrue(ncube.getCell(coord) == 1.0);
        coord.put("age", 2);        // intentional case mismatch
        assertTrue(ncube.getCell(coord) == 2.0);

        coord.clear();
        coord.put("Age", 3);
        ncube.setCell(3.0, coord);
        coord.put("Age", 1);
        assertTrue(ncube.getCell(coord) == 3.0);  // 1 & 3 share same cell

        coord.put("Age", 35);
        ncube.setCell(35.0, coord);
        coord.put("Age", 20);
        assertTrue(ncube.getCell(coord) == 35.0);

        coord.put("Age", "10");
        ncube.setCell(10.0, coord);
        coord.put("Age", 1);
        assertTrue(ncube.getCell(coord) == 10.0);

        coord.put("Age", 80);
        assertTrue(ncube.getCell(coord) == 99.9);

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 4);
    }

    @Test
    public void testRangeSetOverlap()
    {
        RangeSet set = new RangeSet(3);
        set.add(1);
        set.add(new Range(10, 20));
        set.add(25);
        assertTrue(set.size() == 4);

        RangeSet set1 = new RangeSet(15);
        assertTrue(set.overlap(set1));

        set1.clear();
        set1.add(new Range(5, 15));
        assertTrue(set.overlap(set1));

        set1.clear();
        set1.add(new Range(2, 5));
        assertTrue(set.overlap(set1));

        set1.clear();
        set1.add(25);
        assertTrue(set.overlap(set1));

        set1.clear();
        set1.add(2);
        set1.add(4);
        set1.add(new Range(20, 25));
        assertFalse(set.overlap(set1));
    }

    @Test
    public void testNearestAxisType()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("point2d.json");

        Map<String, Object> coord = new HashMap<String, Object>();

        coord.put("Point", new Point2D(0.0, 0.0));
        Assert.assertEquals("0.0, 0.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(-0.1, 0.1));
        Assert.assertEquals("0.0, 0.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(0.49, 0.49));
        Assert.assertEquals("0.0, 0.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(0.55, 0.0));
        Assert.assertEquals("1.0, 0.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(-1.0, 50));
        Assert.assertEquals("0.0, 1.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(-1.5, -0.4));
        Assert.assertEquals("-1.0, 0.0", ncube.getCell(coord));

        coord.put("Point", new Point2D(0.5, -0.6));
        Assert.assertEquals("0.0, -1.0", ncube.getCell(coord));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 6);

        Axis points = null;
        try
        {
            points = new Axis("Point", AxisType.NEAREST, AxisValueType.COMPARABLE, true);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        points = new Axis("Point", AxisType.NEAREST, AxisValueType.COMPARABLE, false);
        points.addColumn(new Point2D(0.0, 0.0));
        points.addColumn(new Point2D(1.0, 0.0));
        points.addColumn(new Point2D(0.0, 1.0));
        points.addColumn(new Point2D(-1.0, 0.0));
        points.addColumn(new Point2D(0.0, -1.0));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 6);

        try
        {
            points.addColumn(new Point3D(1.0, 2.0, 3.0));
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("cannot"));
            assertTrue(expected.getMessage().contains("add"));
            assertTrue(expected.getMessage().contains("axis"));
        }

        try
        {
            points.addColumn(new Point2D(0.0, 0.0));
            assertTrue("should throw exception", false);
        }
        catch (AxisOverlapException expected)
        {
            assertTrue(expected.getMessage().contains("matches"));
            assertTrue(expected.getMessage().contains("value"));
            assertTrue(expected.getMessage().contains("already"));
        }

        try
        {
            points.addColumn("12");
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("cannot"));
            assertTrue(expected.getMessage().contains("add"));
            assertTrue(expected.getMessage().contains("oximity"));
        }

        Point2D p1 = new Point2D(24.0, 36.0);
        Point2D p2 = new Point2D(24.0, 36.0);
        Point2D p3 = new Point2D(36.0, 24.0);
        assertTrue(p1.equals(p2));
        assertTrue(p1.compareTo(p2) == 0);
        assertEquals(p1.getX(), p2.getX(), .0001);
        assertEquals(p1.getY(), p2.getY(), .0001);
        assertFalse(p2.equals(p3));
        assertFalse(p1.equals("string"));
    }

    @Test
    public void testProximityBigDecimal()
    {
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal b = new BigDecimal("101.0");
        double d = Proximity.distance(b, a);
        assertTrue(d == 100.0);
    }

    @Test
    public void testNearestAxisTypePoint3D()
    {
        NCube<String> ncube = new NCube<String>("Nearest3D");

        Axis points = new Axis("Point", AxisType.NEAREST, AxisValueType.COMPARABLE, false);
        points.addColumn(new Point3D(0.0, 0.0, 0.0));
        points.addColumn(new Point3D(1.0, 0.0, 0.0));
        points.addColumn(new Point3D(0.0, 1.0, 0.0));
        points.addColumn(new Point3D(-1.0, 0.0, 0.0));
        points.addColumn(new Point3D(0.0, -1.0, 0.0));
        points.addColumn(new Point3D(0.0, 0.0, 1.0));
        points.addColumn(new Point3D(0.0, 0.0, -1.0));
        ncube.addAxis(points);

        Map<String, Object> coord = new HashMap<>();
        coord.put("Point", new Point3D(0.0, 0.0, 0.0));
        ncube.setCell("0.0, 0.0, 0.0", coord);
        coord.put("Point", new Point3D(1.0, 0.0, 0.0));
        ncube.setCell("1.0, 0.0, 0.0", coord);
        coord.put("Point", new Point3D(0.0, 1.0, 0.0));
        ncube.setCell("0.0, 1.0, 0.0", coord);
        coord.put("Point", new Point3D(-1.0, 0.0, 0.0));
        ncube.setCell("-1.0, 0.0, 0.0", coord);
        coord.put("Point", new Point3D(0.0, -1.0, 0.0));
        ncube.setCell("0.0, -1.0, 0.0", coord);
        coord.put("Point", new Point3D(0.0, 0.0, 1.0));
        ncube.setCell("0.0, 0.0, 1.0", coord);
        coord.put("Point", new Point3D(0.0, 0.0, -1.0));
        ncube.setCell("0.0, 0.0, -1.0", coord);

        coord.put("Point", new Point3D(0.0, 0.0, 0.0));
        String s = ncube.getCell(coord);
        assertTrue("0.0, 0.0, 0.0".equals(s));

        coord.put("Point", new Point3D(-0.1, 0.1, 0.1));
        s = ncube.getCell(coord);
        assertTrue("0.0, 0.0, 0.0".equals(s));

        coord.put("Point", new Point3D(0.49, 0.49, 0.49));
        s = ncube.getCell(coord);
        assertTrue("0.0, 0.0, 0.0".equals(s));

        coord.put("Point", new Point3D(2.0, 100.0, 3.0));
        s = ncube.getCell(coord);
        assertTrue("0.0, 1.0, 0.0".equals(s));

        coord.put("Point", new Point3D(0.1, -0.2, -63.0));
        s = ncube.getCell(coord);
        assertTrue("0.0, 0.0, -1.0".equals(s));

        Point3D p1 = new Point3D(1.0, 2.0, 3.0);
        s = p1.toString();
        assertEquals("1.0, 2.0, 3.0", s);
        assertFalse(p1.equals("string"));
        Point3D p2 = new Point3D(1.0, 2.0, 3.0);
        assertTrue(p1.compareTo(p2) == 0);

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 8);
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeMissingColumnParserError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-column-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeEmptyColumnsError()
    {
        NCubeManager.getNCubeFromResource("ncube-column-not-array-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeEmptyAxesParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-empty-axes-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeMissingAxesParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-axes-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeMissingNameParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-name-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testLatLongParseError()
    {
        NCubeManager.getNCubeFromResource("lat-lon-parse-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testDateParseError()
    {
        NCubeManager.getNCubeFromResource("date-parse-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testPoint2dParseError()
    {
        NCubeManager.getNCubeFromResource("point2d-parse-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testPoint3dParseError()
    {
        NCubeManager.getNCubeFromResource("point3d-parse-error.json");
    }

    @Test
    public void testLatLonAxisType()
    {
        Map<String, Object> coord = new HashMap<String, Object>();
        NCube cube = NCubeManager.getNCubeFromResource("latlon.json");

        String axisName = "Lat / Lon";

        coord = new HashMap<String, Object>();
        coord.put("Lat / Lon", new LatLon(25, -112));
        Assert.assertEquals("Austin", cube.getCell(coord));

        coord = new HashMap<String, Object>();
        coord.put("Lat / Lon", new LatLon(35, -90));
        Assert.assertEquals("Springboro", cube.getCell(coord));

        LatLon newYork = new LatLon(40.714353, -74.005973);
        LatLon losAngeles = new LatLon(34.052234, -118.243685);
        LatLon phoenix = new LatLon(33.448377, -112.074037);
        LatLon elpaso = new LatLon(31.75872, -106.486931);

        coord.put(axisName, newYork);
        Assert.assertEquals("Springboro", cube.getCell(coord));

        coord.put(axisName, losAngeles);
        Assert.assertEquals("Breckenridge", cube.getCell(coord));

        coord.put(axisName, phoenix);
        Assert.assertEquals("Breckenridge", cube.getCell(coord));

        coord.put(axisName, elpaso);
        Assert.assertEquals("Austin", cube.getCell(coord));
    }

    @Test
    public void testSimpleJson1() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube6.json");
        assertTrue("TestCube".equals(ncube.getName()));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2012, Calendar.DECEMBER, 17, 0, 11, 22);
        assertTrue(cal.getTime().getTime() == ((Date) ncube.getDefaultCellValue()).getTime());
        List<Axis> axes = ncube.getAxes();
        assertTrue(axes.size() == 1);
        Axis gender = axes.get(0);
        assertTrue("Gender".equals(gender.getName()));
        assertTrue(gender.getType() == AxisType.DISCRETE);
        assertTrue(gender.getValueType() == AxisValueType.STRING);
        assertTrue(gender.getColumnOrder() == Axis.SORTED);
        List<Column> columns = gender.getColumns();
        assertTrue(columns.size() == 3);
        assertTrue(gender.size() == 3);   // default column = true
        assertTrue(columns.get(0).getValue().equals("Female"));
        assertTrue(columns.get(1).getValue().equals("Male"));

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Gender", "Male");
        assertTrue((Double) ncube.getCell(coord) == 1.0);
        coord.put("Gender", "Female");
        assertTrue((Double) ncube.getCell(coord) == 1.1);
    }

    @Test
    public void testSimpleJson2() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube5.json");
        Map coord = new HashMap();
        coord.put("Age", 10);
        assertTrue((Double) ncube.getCell(coord) == 9.0);
        coord.put("Age", 22);
        assertTrue((Double) ncube.getCell(coord) == 5.0);
        coord.put("Age", 28);
        assertTrue((Double) ncube.getCell(coord) == 2.7);
        coord.put("Age", 50);
        assertTrue((Double) ncube.getCell(coord) == 1.5);
        coord.put("Age", 69);
        assertTrue((Double) ncube.getCell(coord) == 1.8);
        coord.put("Age", 75);
        assertTrue((Double) ncube.getCell(coord) == 9.0);
    }

    @Test
    public void testSimpleJson3() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube4.json");
        Map coord = new HashMap();

        coord.put("Code", "a");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "o");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "t");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "y");
        assertTrue("ABC".equals(ncube.getCell(coord)));

        coord.put("Code", "b");
        assertTrue("DEF".equals(ncube.getCell(coord)));
        coord.put("Code", "d");
        assertTrue("DEF".equals(ncube.getCell(coord)));

        coord.put("Code", "h");
        assertTrue("ZZZ".equals(ncube.getCell(coord)));
        coord.put("Code", "i");
        assertTrue("ZZZ".equals(ncube.getCell(coord)));
        coord.put("Code", "w");
        assertTrue("ZZZ".equals(ncube.getCell(coord)));

        coord.put("Code", "mic");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "november");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "oscar");
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", "xray");
        assertTrue("ABC".equals(ncube.getCell(coord)));

        try
        {
            coord.put("Code", "p");
            ncube.getCell(coord);
            assertTrue("should throw exception", false);
        }
        catch (CoordinateNotFoundException expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("found"));
            assertTrue(expected.getMessage().contains("axis"));
        }
    }

    @Test
    public void testNearestAxisStringType()
    {
        NCube<String> ncube = new NCube<String>("NearestString");

        // The last parameter below is true on purpose, even though NEAREST axes cannot have a default column.
        // The test ensures that it does not blow up with a default column set (NCube sets it to false).
        Axis points = new Axis("Point", AxisType.NEAREST, AxisValueType.COMPARABLE, false);
        points.addColumn("Alpha");
        points.addColumn("Bravo");
        points.addColumn("Charlie");
        points.addColumn("Delta");
        points.addColumn("Echo");
        points.addColumn("ABC");
        ncube.addAxis(points);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("Point", "Alpha");
        ncube.setCell("alpha", coord);
        coord.put("Point", "Bravo");
        ncube.setCell("bravo", coord);
        coord.put("Point", "Charlie");
        ncube.setCell("charlie", coord);
        coord.put("Point", "Delta");
        ncube.setCell("delta", coord);
        coord.put("Point", "Echo");
        ncube.setCell("echo", coord);
        coord.put("Point", "ABC");
        ncube.setCell("abc", coord);

        coord.put("Point", "alfa");
        assertTrue("alpha".equals(ncube.getCell(coord)));
        coord.put("Point", "Alpha");
        assertTrue("alpha".equals(ncube.getCell(coord)));
        coord.put("Point", "calpa");
        assertTrue("alpha".equals(ncube.getCell(coord)));

        coord.put("Point", "brave");
        assertTrue("bravo".equals(ncube.getCell(coord)));
        coord.put("Point", "ehavo");
        assertTrue("bravo".equals(ncube.getCell(coord)));
        coord.put("Point", "rbavo");
        assertTrue("bravo".equals(ncube.getCell(coord)));

        coord.put("Point", "charpie");
        assertTrue("charlie".equals(ncube.getCell(coord)));
        coord.put("Point", "carpie");
        assertTrue("charlie".equals(ncube.getCell(coord)));
        coord.put("Point", "carlie");
        assertTrue("charlie".equals(ncube.getCell(coord)));

        coord.put("Point", "detla");
        assertTrue("delta".equals(ncube.getCell(coord)));
        coord.put("Point", "desert");
        assertTrue("delta".equals(ncube.getCell(coord)));
        coord.put("Point", "belta");
        assertTrue("delta".equals(ncube.getCell(coord)));

        coord.put("Point", "ecko");
        assertTrue("echo".equals(ncube.getCell(coord)));
        coord.put("Point", "heco");
        assertTrue("echo".equals(ncube.getCell(coord)));
        coord.put("Point", "ehco");
        assertTrue("echo".equals(ncube.getCell(coord)));

        coord.put("Point", "AC");
        assertTrue("abc".equals(ncube.getCell(coord)));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 7);
    }

    @Test
    public void testNearestLong() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube3.json");
        Map coord = new HashMap();
        coord.put("Code", 1);
        assertTrue("DEF".equals(ncube.getCell(coord)));
        coord.put("Code", (byte) -8);
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", (short) 8);
        assertTrue("GHI".equals(ncube.getCell(coord)));
    }

    @Test
    public void testNearestDouble() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube2.json");
        Map coord = new HashMap();
        coord.put("Code", 1.0f);
        assertTrue("DEF".equals(ncube.getCell(coord)));
        coord.put("Code", -8.0f);
        assertTrue("ABC".equals(ncube.getCell(coord)));
        coord.put("Code", 8.0);
        assertTrue("GHI".equals(ncube.getCell(coord)));
    }

    @Test
    public void testNearestDate() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testCube1.json");
        Map coord = new HashMap();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1984, 6, 9, 2, 2, 2);
        coord.put("Code", cal.getTime());
        assertTrue("ABC".equals(ncube.getCell(coord)));
        cal.set(2001, 4, 22, 3, 3, 3);
        coord.put("Code", cal.getTime());
        assertTrue("DEF".equals(ncube.getCell(coord)));
        cal.set(2009, 2, 8, 4, 4, 4);
        coord.put("Code", cal.getTime());
        assertTrue("GHI".equals(ncube.getCell(coord)));

    }

    @Test
    public void testClearCell()
    {
        NCube ncube = new NCube("TestClearCell");
        ncube.setDefaultCellValue("DEFAULT VALUE");
        Axis gender = getGenderAxis(true);
        ncube.addAxis(gender);
        Map coord = new HashMap();
        coord.put("Gender", "Male");
        ncube.setCell("m", coord);
        coord.put("Gender", "Female");
        ncube.setCell("f", coord);

        assertTrue("f".equals(ncube.getCell(coord)));
        ncube.removeCell(coord);
        assertTrue("DEFAULT VALUE".equals(ncube.getCell(coord)));
    }

    @Test
    public void testGetMap()
    {
        NCube ncube = new NCube("TestGetMap");
        ncube.setDefaultCellValue("DEFAULT VALUE");
        Axis gender = getGenderAxis(true);
        ncube.addAxis(gender);
        Map coord = new HashMap();
        coord.put("Gender", "Male");
        ncube.setCell("m", coord);
        coord.put("Gender", "Female");
        ncube.setCell("f", coord);

        Set set = new HashSet();
        coord.put("Gender", set);
        Map result = ncube.getMap(coord);
        assertTrue("f".equals(result.get("Female")));
        assertTrue("m".equals(result.get("Male")));

        set.clear();
        set.add("Male");
        coord.put("Gender", set);
        result = ncube.getMap(coord);
        assertFalse("f".equals(result.get("Female")));
        assertTrue("m".equals(result.get("Male")));

        set.clear();
        set.add("Snail");
        coord.put("Gender", set);
        result = ncube.getMap(coord);
        assertTrue(result.size() == 1);
        assertTrue("DEFAULT VALUE".equals(result.get(null)));
    }

    @Test
    public void testGetMapErrorHandling()
    {
        NCube ncube = new NCube("TestGetMap");
        ncube.setDefaultCellValue("DEFAULT VALUE");
        Axis gender = getGenderAxis(true);
        Axis days = getShortDaysOfWeekAxis();
        ncube.addAxis(gender);
        ncube.addAxis(days);
        Map coord = new HashMap();

        try
        {
            ncube.getMap(coord);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("contain"));
            assertTrue(expected.getMessage().contains("key"));
        }

        try
        {
            coord.put("Gender", new HashSet());
            coord.put("Days", new TreeSet());
            ncube.getMap(coord);
            assertTrue("should throw exception", false);
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("than"));
            assertTrue(expected.getMessage().contains("one"));
            assertTrue(expected.getMessage().contains("coord"));
        }
    }

    @Test
    public void testGetMapWithRangeColumn()
    {
        NCube ncube = new NCube("TestGetMapWithRange");
        Axis range = getDateRangeAxis(false);
        ncube.addAxis(range);

        Map coord = new HashMap();
        Set set = new HashSet();
        coord.put("dateRange", set);
        Map result = ncube.getMap(coord);
        for (Object o : result.entrySet())
        {
            Map.Entry entry = (Map.Entry) o;
            assertTrue(entry.getKey() instanceof Range);
            Range r = (Range) entry.getKey();
            assertTrue(r.low instanceof Date);
        }
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testGetMapWithRangeSetColumn()
    {
        NCube ncube = new NCube("TestGetMapWithRangeSet");
        Axis age = new Axis("Age", AxisType.SET, AxisValueType.LONG, false);
        ncube.addAxis(age);
        RangeSet rs = new RangeSet(new Range(60, 80));
        rs.add(10);
        age.addColumn(rs);

        Map coord = new HashMap();
        coord.put("age", 10);
        ncube.setCell("young", coord);
        coord.put("age", 60);
        ncube.setCell("old", coord);        // overwrite 'young'

        Set set = new HashSet();
        coord.put("age", set);
        Map result = ncube.getMap(coord);
        Iterator i = result.entrySet().iterator();
        if (i.hasNext())
        {
            Map.Entry entry = (Map.Entry) i.next();
            assertTrue(entry.getKey() instanceof RangeSet);
            rs = (RangeSet) entry.getKey();
            assertTrue(rs.get(0) instanceof Range);
            Range range = (Range) rs.get(0);
            assertTrue((Long) range.low == 60L);
            assertTrue((Long) range.high == 80L);
            assertTrue("old".equals(entry.getValue()));
            assertTrue((Long) rs.get(1) == 10l);
        }
        else
        {
            assertTrue("Should have 2 items", false);
        }
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 2);
    }

    @Test
    public void test2DSimpleJson() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("2DSimpleJson.json");
        Map coord = new HashMap();
        coord.put("businessDivisionCode", "ALT");
        coord.put("attribute", "workflowAppCode");
        assertTrue("AMWRKFLW".equals(ncube.getCell(coord)));

        coord.put("businessDivisionCode", "FIDCR");
        coord.put("attribute", "longName");
        assertTrue("Fidelity/Crime Division".equals(ncube.getCell(coord)));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 7);
    }

    @Test
    public void testContainsCell()
    {
        NCube<Date> ncube = new NCube<Date>("Dates");
        ncube.addAxis(getShortMonthsOfYear());

        Map coord = new HashMap();
        coord.put("Months", "Jun");
        Date now = new Date();
        ncube.setCell(now, coord);

        assertTrue(ncube.getCell(coord).equals(now));
        assertTrue(ncube.containsCell(coord));

        coord.put("Months", "Jan");
        assertFalse(ncube.containsCell(coord));
        coord.put("Months", "Jul");
        assertFalse(ncube.containsCell(coord));
        coord.put("Months", "Dec");
        assertFalse(ncube.containsCell(coord));
    }

    @Test
    public void testApprovalLimits() throws Exception
    {
        NCube approvalLimits = NCubeManager.getNCubeFromResource("approvalLimits.json");
        assertTrue(countMatches(approvalLimits.toHtml(), "<tr>") == 16);
    }

    @Test
    public void testEmptyToHtml()
    {
        NCube ncube = new NCube("Empty");
        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 0);
    }

    @Test
    public void testInternalColumnPointers()
    {
        NCube<String> ncube = new NCube<>("TestColumnPointers");
        ncube.addAxis(getGenderAxis(true));
        Axis triAxis = new Axis("Tristate", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 2);
        triAxis.addColumn("true");
        triAxis.addColumn("false");
        ncube.addAxis(triAxis);

        Map coord = new HashMap();
        coord.put("Gender", "Male");
        coord.put("TriState", "true");
        ncube.setCell("male-true", coord);

        coord.put("TriState", "false");
        ncube.setCell("male-false", coord);

        coord.put("TriState", null);
        ncube.setCell("male-default", coord);

        coord.put("Gender", "Female");
        coord.put("TriState", "true");
        ncube.setCell("female-true", coord);

        coord.put("TriState", "false");
        ncube.setCell("female-false", coord);

        coord.put("TriState", null);
        ncube.setCell("female-default", coord);

        coord.put("Gender", null);
        coord.put("TriState", "true");
        ncube.setCell("default-true", coord);

        coord.put("TriState", "false");
        ncube.setCell("default-false", coord);

        coord.put("TriState", null);
        ncube.setCell("default-default", coord);

        coord.put("Gender", "Male");
        coord.put("TriState", "true");
        assertTrue("male-true".equals(ncube.getCell(coord)));

        coord.put("TriState", "false");
        assertTrue("male-false".equals(ncube.getCell(coord)));

        coord.put("TriState", null);
        assertTrue("male-default".equals(ncube.getCell(coord)));

        coord.put("Gender", "Female");
        coord.put("TriState", "true");
        assertTrue("female-true".equals(ncube.getCell(coord)));

        coord.put("TriState", "false");
        assertTrue("female-false".equals(ncube.getCell(coord)));

        coord.put("TriState", null);
        assertTrue("female-default".equals(ncube.getCell(coord)));

        coord.put("Gender", null);
        coord.put("TriState", "true");
        assertTrue("default-true".equals(ncube.getCell(coord)));

        coord.put("TriState", "false");
        assertTrue("default-false".equals(ncube.getCell(coord)));

        coord.put("TriState", null);
        assertTrue("default-default".equals(ncube.getCell(coord)));

        assertTrue(countMatches(ncube.toHtml(), "<tr>") == 5);
    }

    @Test
    public void testStackTrace()
    {
        NCube<CommandCell> continents = new NCube<CommandCell>("Continents");
        Axis continent = getContinentAxis();
        continents.addAxis(continent);

        Map coord = new HashMap();
        coord.put("Continent", "Africa");
        continents.setCell(new GroovyExpression("$AfricaCountries(input)", null), coord);
        coord.put("Continent", "Antarctica");
        continents.setCell(new GroovyExpression("$AntarticaCountries(input)", null), coord);
        coord.put("Continent", "Asia");
        continents.setCell(new GroovyExpression("$AsiaCountries(input)", null), coord);
        coord.put("Continent", "Australia");
        continents.setCell(new GroovyExpression("$AustraliaCountries(input)", null), coord);
        coord.put("Continent", "Europe");
        continents.setCell(new GroovyExpression("$EuropeanCountries(input)", null), coord);
        coord.put("Continent", "North America");
        continents.setCell(new GroovyExpression("$NorthAmericaCountries(input)", null), coord);
        coord.put("Continent", "South America");
        continents.setCell(new GroovyExpression("$SouthAmericaCountries(input)", null), coord);

        coord.put("Continent", "North America");
        coord.put("Country", "USA");
        coord.put("State", "OH");

        NCube<CommandCell> naCountries = new NCube<CommandCell>("NorthAmericaCountries");
        Axis country = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, false);
        country.addColumn("Canada");
        country.addColumn("USA");
        country.addColumn("Mexico");
        naCountries.addAxis(country);

        naCountries.setCell(new GroovyExpression("$UsaStates(input)", null), coord);
        NCubeManager.addCube(continents.getApplicationID(), continents);
        NCubeManager.addCube(naCountries.getApplicationID(), naCountries);

        try
        {
            continents.getCell(coord);
            fail("should throw exception");
        }
        catch (Exception expected)
        {
        }
    }

    @Test
    public void testRenameAxis()
    {
        NCube<String> ncube = new NCube("RenameAxisTest");
        Axis days = getShortDaysOfWeekAxis();
        ncube.addAxis(days);

        Map coord = new HashMap();
        coord.put("days", "Mon");
        ncube.setCell("Monday", coord);
        coord.clear();
        coord.put("DAYS", "Wed");
        ncube.setCell("Wednesday", coord);
        coord.clear();
        coord.put("Days", "Fri");
        ncube.setCell("Friday", coord);

        ncube.renameAxis("DAYS", "DAYS-OF-WEEK");

        coord.clear();
        coord.put("DAYS-OF-WEEK", "Mon");
        assertTrue("Monday".equals(ncube.getCell(coord)));
        coord.clear();
        coord.put("DAYS-of-WEEK", "Wed");
        assertTrue("Wednesday".equals(ncube.getCell(coord)));
        coord.clear();
        coord.put("DAYS-OF-week", "Fri");
        assertTrue("Friday".equals(ncube.getCell(coord)));

        try
        {
            ncube.renameAxis(null, "DAYS-OF-WEEK");
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("name"));
            assertTrue(expected.getMessage().contains("cannot"));
            assertTrue(expected.getMessage().contains("empty"));
        }

        try
        {
            ncube.renameAxis("days", null);
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("name"));
            assertTrue(expected.getMessage().contains("cannot"));
            assertTrue(expected.getMessage().contains("empty"));
        }

        try
        {
            ncube.renameAxis("days-OF-week", "Days-of-week");
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("already"));
            assertTrue(expected.getMessage().contains("axis"));
            assertTrue(expected.getMessage().contains("named"));
        }

        try
        {
            ncube.renameAxis("jojo", "mojo");
            assertTrue("should throw exception", false);
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("xis"));
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("on"));
            assertTrue(expected.getMessage().contains("NCube"));
        }
    }

    @Test(expected=RuntimeException.class)
    public void testInvalidTemplate() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("template-with-error.json");
        Map map = new HashMap<String, Object>();
        map.put("State", "TX");
        n1.getCell(map);
    }

    @Test
    public void testUpdateColumnValue()
    {
        NCube n1 = NCubeManager.getNCubeFromResource("updateColumns.json");
        Axis state = n1.getAxis("state");
        Column col = state.findColumn("WY");

        Map coord = new HashMap();
        coord.put("code", 1);
        coord.put("state", "WY");
        String val = (String) n1.getCell(coord);
        assertEquals("1 WY", val);

        n1.updateColumn(col.id, "ZZ");

        coord.put("state", "ZZ");
        val = (String) n1.getCell(coord);
        assertEquals("1 WY", val);

        assertNull(n1.getAxisFromColumnId(100));

        try
        {
            // bogus column
            n1.updateColumn(1234567, "zz");
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testBinaryUrl() throws Exception
    {
        NCubeManager.getNCubeFromResource("template1.json");
        NCubeManager.getNCubeFromResource("template2.json");
        NCube n1 = NCubeManager.getNCubeFromResource("urlContent.json");
        Map coord = new HashMap();
        coord.put("sites", "BinaryFromLocalUrl");
        byte[] localBinaryBytes = (byte[]) n1.getCell(coord);
        assertEquals(77383, localBinaryBytes.length);

        coord.put("sites", "BinaryFromRemoteUrl");
        byte[] remoteBinaryBytes = (byte[]) n1.getCell(coord);
        assertEquals(77383, remoteBinaryBytes.length);

        coord.put("sites", "StringFromLocalUrl");
        assertEquals("CAFEBABE", n1.getCell(coord));

        coord.put("sites", "StringFromValue");
        assertEquals("return \"Local Hello, world.\"", n1.getCell(coord));

        coord.put("sites", "StringFromRemoteUrl");
        assertEquals("CAFEBABE", n1.getCell(coord));

        coord.put("sites", "TemplateFromLocalUrl");
        assertEquals("You saved 0.12 on your plane insurance. Does this 0.12 work?", n1.getCell(coord));

        coord.put("sites", "TemplateFromRemoteUrl");
        assertEquals("You saved 0.12 on your plane insurance. Does this 0.12 work?", n1.getCell(coord));
    }

    @Test
    public void testProximity()
    {
        try
        {
            Proximity.distance(null, "hey");
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            Proximity.distance("yo", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }

        try
        {
            Proximity.distance("yo", 16);
            fail("should not make it here");
        }
        catch (Exception e)
        {
        }
    }

    @Test
    public void testWildcardSet()
    {
        NCube<String> ncube = new NCube("test.WildcardSet");
        Axis attributes = new Axis("attribute", AxisType.DISCRETE, AxisValueType.STRING, false);
        Axis busDivCode = new Axis("businessDivisionCode", AxisType.DISCRETE, AxisValueType.STRING, false);

        busDivCode.addColumn("AGR");
        busDivCode.addColumn("ALT");
        busDivCode.addColumn("EQM");
        busDivCode.addColumn("FIDCR");
        busDivCode.addColumn("PIM");
        busDivCode.addColumn("SHS");

        attributes.addColumn("businessDivisionId");
        attributes.addColumn("longName");
        attributes.addColumn("underwriterLdapGroup");
        attributes.addColumn("assignToLdapGroup");
        attributes.addColumn("workflowAppCode");
        attributes.addColumn("divisionId");

        ncube.addAxis(attributes);
        ncube.addAxis(busDivCode);

        Map<String, Object> coord = new HashMap<String, Object>();
        coord.put("attribute", "longName");
        coord.put("businessDivisionCode", new LinkedHashSet());
        Map slice = ncube.getMap(coord);
        assertTrue(slice.size() == 6);

        coord.clear();
        Set wild = new TreeSet();
        wild.add("AGR");
        wild.add("PIM");
        coord.put("attribute", "longName");
        coord.put("businessDivisionCode", wild);
        slice = ncube.getMap(coord);
        assertTrue(slice.size() == 2);

        coord.put("businessDivisionCode", null);
        try
        {
            ncube.getMap(coord);
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testJsonInJson()
    {
        String jsonInner = "{\n" +
                "    \\\"array\\\": [\n" +
                "        1,\n" +
                "        2,\n" +
                "        3\n" +
                "    ]\n" +
                "}";
        String jsonOuter = "{\n" +
                "   \"ncube\":\"TestCube\",\n" +
                "   \"defaultCellValue\":\"ZZZ\",\n" +
                "   \"axes\":[\n" +
                "      {\n" +
                "         \"name\":\"Code\",\n" +
                "         \"type\":\"NEAREST\",\n" +
                "         \"valueType\":\"DATE\",\n" +
                "         \"hasDefault\":false,\n" +
                "         \"preferredOrder\":0,\n" +
                "         \"columns\":[\n" +
                "            {\n" +
                "               \"value\":\"1990-01-01T02:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"value\":\"2000-01-01T02:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"value\":\"2012-01-01T02:00:00\"\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   ],\n" +
                "   \"cells\":[\n" +
                "      {\n" +
                "         \"key\":{\"Code\":\"1993-06-04T02:22:44\"},\n" +
                "         \"value\":\"ABC\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"key\":{\"Code\":\"1998-11-17T11:05:20\"},\n" +
                "         \"value\":\"DEF\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"key\":{\"Code\":\"2014-09-14T16:07:01\"},\n" +
                "         \"value\":\"" + jsonInner + "\"" +
                "      }\n" +
                "   ]\n" +
                "}";
        NCube ncube = NCube.fromSimpleJson(jsonOuter);
        Map coord = new HashMap();
        coord.put("code", new Date());
        Object value = ncube.getCell(coord);
        assertTrue(value instanceof String);
    }

    @Test
    public void testGroovyCallingNCube() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        axis.addColumn("scalar");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("type", "good");
        ncube.setCell(new GroovyExpression("output.out='dog'; output['ncube']=ncube; return 'great'", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("input.type='scalar'; return $(input)", null), coord);
        coord.put("type", "scalar");
        ncube.setCell(16, coord);

        Map output = new HashMap();
        coord.put("type", "good");
        Object o = ncube.getCell(coord, output);
        assertEquals("great", o);
        assertEquals(output.get("out"), "dog");
        assertEquals(ncube, output.get("ncube"));  // ncube was passed in

        output.clear();
        coord.put("type", "bad");
        o = ncube.getCell(coord, output);
        assertEquals(16, o);
    }

    @Test
    public void testGroovyModifyingInput() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        axis.addColumn("scalar");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map input = new HashMap();
        input.put("type", "bad");
        ncube.setCell(new GroovyExpression("input['type']='scalar'; output.funny = 'bone'; return 5", null), input);

        Map output = new HashMap();
        input.put("type", "bad");
        Object ret = ncube.getCell(input, output);
        assertEquals(5, ret);
        assertEquals(input.get("type"), "bad"); // input coord does not change
    }

    @Test
    public void testGroovyExpressionUrlNotFound()
    {

    }

    @Test
    public void testGroovyNCubeMgr() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        axis.addColumn("property");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("type", "good");
        ncube.setCell(new GroovyExpression("$GroovyCube([type:'property'])", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("def total = 0; (1..10).each { i -> total += i}; return total", null), coord);
        coord.put("type", "property");
        ncube.setCell(new GroovyExpression("9", null), coord);

        Map output = new HashMap();
        coord.put("type", "good");
        assertEquals(9, ncube.getCell(coord, output));

        output = new HashMap();
        coord.put("type", "bad");
        assertEquals(55, ncube.getCell(coord, output));

        output = new HashMap();
        coord.put("type", "property");
        assertEquals(9, ncube.getCell(coord, output));
    }

    @Test
    public void testGroovyMath() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("age", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.addColumn(25);
        axis.addColumn(35);
        axis.addColumn(45);
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("age", 25);
        ncube.setCell(new GroovyExpression("def age=input['age']; return Math.abs(age - 100)", null), coord);

        Map output = new HashMap();
        coord.put("age", 25);
        Object o = ncube.getCell(coord, output);
        assertEquals(o, 75);
    }

    @Test
    public void testGroovyTwoMethods() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("method", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("doIt");
        axis.addColumn("bar");
        axis.addColumn("baz");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("method", "doIt");
        coord.put("age", 25);
        ncube.setCell(new GroovyMethod(
                "package ncube.grv.method; class Junk extends NCubeGroovyController " +
                        "{\n" +
                        "def doIt() {\n" +
                        " int x = input.age * 10;" +
                        " jump(x)" +
                        "}\n" +
                        "int jump(int x) { x * 2; }" +
                        "}", null), coord);

        Map output = new HashMap();
        coord.put("method", "doIt");
        coord.put("age", 25);
        long start = System.currentTimeMillis();
        Object o = null;
        for (int i = 0; i < 10000; i++)
        {
            o = ncube.getCell(coord, output);
        }
        long stop = System.currentTimeMillis();
        println("execute GroovyMethod 10,000 times = " + (stop - start));
        assertEquals(o, 500);
    }

    @Test
    public void testGroovyTwoMethodsAndClass() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("age", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.addColumn(25);
        axis.addColumn(35);
        axis.addColumn(45);
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("age", 25);
        coord.put("method", "doIt");
        ncube.setCell(new GroovyMethod(
                "package ncube.grv.method; class Junk extends NCubeGroovyController {" +
                        "def doIt()" +
                        "{" +
                        " int x = input['age'] * 10;" +
                        " return Fargo.freeze(jump(x))" +
                        "}\n" +
                        "int jump(int x) { x * 2; }\n" +
                        "\n" +
                        "static class Fargo {" +
                        "static int freeze(int d) {" +
                        "  -d" +
                        "}}}", null), coord);

        Map output = new HashMap();
        coord.put("age", 25);
        coord.put("method", "doIt");
        long start = System.currentTimeMillis();
        Object o = null;
        for (int i = 0; i < 1000; i++)
        {
            o = ncube.getCell(coord, output);
            assertEquals(o, -500);
        }
        long stop = System.currentTimeMillis();
        println("execute GroovyMethod 1000 times = " + (stop - start));
        assertEquals(o, -500);
    }

    @Test
    public void testGroovy() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("age", AxisType.DISCRETE, AxisValueType.LONG, false);
        axis.addColumn(25);
        axis.addColumn(35);
        axis.addColumn(45);
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        // Bad command (CommandCell not GroovyProg used)
        Map coord = new HashMap();
        coord.put("age", 25);

        // Bad Groovy (Compile error)
        try
        {
            ncube.setCell(new GroovyMethod(
                    "Object run(Map args whoops) " +
                            "{ 1 }", null), coord);

            ncube.getCell(coord, new HashMap());
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }

        // Bad Groovy (NCube cmd syntax error)
        try
        {
            ncube.setCell(new GroovyMethod(
                    "def run(Map args whoops) " +
                            "{ 1 }", null), coord);

            ncube.getCell(coord, new HashMap());
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }

        // Repeat error...should just throw it again (not attempt to recompile)
        try
        {
            ncube.getCell(coord, new HashMap());
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }

        coord = new HashMap();
        coord.put("age", 25);
        coord.put("method", "oldify");
        ncube.setCell(new GroovyMethod(
                "import ncube.grv.method.NCubeGroovyController; " +
                        "class Chicken extends NCubeGroovyController" +
                        "{" +
                        "def oldify() " +
                        "{" +
                        " input['age'] * 10;" +
                        "}}", null), coord);

        Map output = new HashMap();
        coord.put("age", 25);
        coord.put("method", "oldify");
        long start = System.currentTimeMillis();
        Object o = null;
        for (int i = 0; i < 1000; i++)
        {
            o = ncube.getCell(coord, output);
            assertEquals(o, 250);
        }
        long stop = System.currentTimeMillis();
        println("execute GroovyMethod 1,000 times = " + (stop - start));
        assertEquals(o, 250);
    }

    public static class CallJavaTest
    {
        public static Object testInput(Map input, Map output, String type)
        {
            if ("good".equalsIgnoreCase(type))
            {
                output.put("out", "dog");
                return "great";
            }
            else
            {
                output.put("out", "cat");
                return "terrible";
            }
        }
    }

    @Test
    public void testGroovyExpThatCallsJava() throws Exception
    {
        NCube ncube = new NCube("CallCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        // Illustrates that return is optional in expressions
        Map coord = new HashMap();
        coord.put("type", "good");
        String className = TestNCube.class.getName();
        ncube.setCell(new GroovyExpression(className + "$CallJavaTest.testInput(input, output, input.type)", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("return " + className + "$CallJavaTest.testInput(input, output, input.type)", null), coord);

        Map output = new HashMap();
        coord.put("type", "good");
        Object o = ncube.getCell(coord, output);
        assertEquals("great", o);
        assertEquals(output.get("out"), "dog");

        coord.put("type", "bad");
        o = ncube.getCell(coord, output);
        assertEquals("terrible", o);
        assertEquals(output.get("out"), "cat");
    }

    @Test
    public void testShorthandNotation() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        axis.addColumn("alpha");
        axis.addColumn("beta");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("type", "good");
        ncube.setCell(new GroovyExpression("$GroovyCube([type:'alpha'])", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("$([type:'beta'])", null), coord);
        coord.put("type", "alpha");
        ncube.setCell(16, coord);
        coord.put("type", "beta");
        ncube.setCell(26, coord);

        coord.put("type", "good");
        Object o = ncube.getCell(coord);
        assertEquals(16, o);

        coord.put("type", "bad");
        o = ncube.getCell(coord);
        assertEquals(26, o);
    }

    @Test
    public void testShorthandNotationWithOutput() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        axis.addColumn("alpha");
        axis.addColumn("beta");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("type", "good");
        ncube.setCell(new GroovyExpression("$GroovyCube([type:'alpha'])", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("$([type:'beta'])", null), coord);
        coord.put("type", "alpha");
        ncube.setCell(new GroovyExpression("output['stack'] = 'foo'; output.good=16", null), coord);
        coord.put("type", "beta");
        ncube.setCell(new GroovyExpression("output.stack = 'foo'; output.bad=26", null), coord);

        coord.put("type", "good");
        Map output = new HashMap();
        Object o = ncube.getCell(coord, output);
        assertEquals(16, o);
        assertEquals(16, output.get("good"));
        assertEquals(4, output.size());
        String foo = (String) output.get("stack");
        assertEquals("foo", foo) ;

        coord.put("type", "bad");
        output.clear();
        o = ncube.getCell(coord, output);
        assertEquals(26, o);
        assertEquals(26, output.get("bad"));
    }

    @Test
    public void testSupportDeprecatedJoinCommand() throws Exception
    {
        NCube ncube = new NCube("GroovyCube");
        Axis axis = new Axis("type", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis.addColumn("good");
        axis.addColumn("bad");
        ncube.addAxis(axis);
        NCubeManager.addCube(ncube.getApplicationID(), ncube);

        Map coord = new HashMap();
        coord.put("type", "good");
        ncube.setCell(new GroovyExpression("@JoinedCube([:])", null), coord);
        coord.put("type", "bad");
        ncube.setCell(new GroovyExpression("@JoinedCube([])", null), coord);      // Can't pass an array

        NCube cube2 = new NCube("JoinedCube");
        axis = new Axis("state", AxisType.DISCRETE, AxisValueType.LONG.STRING, false);
        axis.addColumn("OH");
        axis.addColumn("TX");
        cube2.addAxis(axis);
        NCubeManager.addCube(cube2.getApplicationID(), cube2);

        coord.clear();
        coord.put("type", "good");
        coord.put("state", "OH");
        cube2.setCell("Cincinnati", coord);
        coord.put("state", "TX");
        cube2.setCell("Austin", coord);

        coord.clear();
        coord.put("type", "good");
        coord.put("state", "OH");
        Object o = ncube.getCell(coord);
        assertEquals("Cincinnati", o);

        coord.put("type", "bad");
        coord.put("state", "TX");
        coord.put("state", "TX");
        try
        {
            o = ncube.getCell(coord);
            fail("Should not get here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }

        Set<String> names = ncube.getRequiredScope();
        assertTrue(names.size() == 1);
        assertTrue(names.contains("type"));
        names = ncube.getOptionalScope();
        assertTrue(names.size() == 0);
    }

    @Test
    public void testNullCommand() throws Exception
    {
        try
        {
            new GroovyMethod(null, null);
            fail("Should not make it here.");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    public void testSimpleJsonArray() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonArrayTest.json");
        Map coord = new HashMap();
        coord.put("Code", "ints");
        Object[] ints = (Object[]) ncube.getCell(coord);
        assertEquals(ints[0], 0L);
        assertEquals(ints[1], 1);
        assertEquals(ints[2], 4L);

        coord.put("Code", "strings");
        Object[] strings = (Object[]) ncube.getCell(coord);
        assertEquals(strings[0], "alpha");
        assertEquals(strings[1], "bravo");
        assertEquals(strings[2], "charlie");

        coord.put("Code", "arrays");
        Object[] arrays = (Object[]) ncube.getCell(coord);

        Object[] sub1 = (Object[]) arrays[0];
        assertEquals(sub1[0], 0L);
        assertEquals(sub1[1], 1L);
        assertEquals(sub1[2], 6L);

        Object[] sub2 = (Object[]) arrays[1];
        assertEquals(sub2[0], "a");
        assertEquals(sub2[1], "b");
        assertEquals(sub2[2], "c");

        coord.clear();
        coord.put("Code", "crazy");
        arrays = (Object[]) ncube.getCell(coord);

        assertEquals("1.0", arrays[0]);
        List sub = (List) arrays[1];
        assertEquals("1.a", sub.get(0));
        sub = (List) arrays[2];
        assertEquals("1.b", sub.get(0));
        assertEquals("2.0", arrays[3]);
    }

    @Test
    public void testSimpleJsonExpression() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        Map coord = new HashMap();
        coord.put("code", "exp");
        Object ans = ncube.getCell(coord);
        assertEquals(6.28, ans);
        assertEquals(coord.get("code"), "exp");

        // Type promotion from double to BigDecimal
        coord.put("CODE", "bigdec");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof BigDecimal);
        assertTrue(((BigDecimal) ans).doubleValue() > 3.13);
        assertTrue(((BigDecimal) ans).doubleValue() < 3.15);

        // Type promotion from double to float
        coord.put("CODE", "floatVal");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof Float);
        assertTrue(((Float) ans).doubleValue() > 3.13);
        assertTrue(((Float) ans).doubleValue() < 3.15);

        // Type promotion from long to int
        coord.put("CODE", "integerVal");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof Integer);
        assertEquals(16, ans);

        // Type promotion from long to BigInteger
        coord.put("CODE", "bigintVal");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof BigInteger);
        assertTrue(((BigInteger) ans).intValue() == -16);

        // Type promotion from long to byte
        coord.put("CODE", "byteVal");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof Byte);
        assertEquals((byte) 101, ans);

        // Type promotion from long to short
        coord.put("CODE", "shortVal");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof Short);
        assertEquals((short) -101, ans);

        // Date format (date + time)
        coord.put("CODE", "date1Val");
        ans = ncube.getCell(coord);
        assertTrue(ans instanceof Date);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime((Date) ans);

        assertEquals(cal.get(Calendar.YEAR), 2013);
        assertEquals(cal.get(Calendar.MONTH), 7);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 30);
        assertEquals(cal.get(Calendar.HOUR_OF_DAY), 22);
        assertEquals(cal.get(Calendar.MINUTE), 0);
        assertEquals(cal.get(Calendar.SECOND), 1);

        // Date format (date)
        coord.put("CODE", "date2Val");
        ans = ncube.getCell(coord);
        cal.clear();
        cal.setTime((Date) ans);
        assertTrue(ans instanceof Date);
        assertEquals(cal.get(Calendar.YEAR), 2013);
        assertEquals(cal.get(Calendar.MONTH), 7);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
    }

    @Test(expected=CoordinateNotFoundException.class)
    public void testNoColumnsNoCellsNoDefault() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("nocolumns-nocells-nodefault-error.json");

        Map<String, Object> coord = new HashMap<>();
        coord.put("test", "foo");

        ncube.getCell(coord);
    }

    @Test
    public void testNoColumnsNoCellsHasDefault() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("nocolumns-nocells-hasdefault.json");

        Map<String, Object> coord = new HashMap<>();
        coord.put("test", "foo");

        assertEquals("bar", ncube.getCell(coord));
    }

    @Test(expected=RuntimeException.class)
    public void testIdInCellDoesNotMatch() throws Exception
    {
        NCubeManager.getNCubeFromResource("id-in-cell-does-not-match-columns-error.json");
    }

    @Test(expected=RuntimeException.class)
    public void testUrlCommandWithoutValueAndUrl() throws Exception
    {
        NCubeManager.getNCubeFromResource("url-command-without-value-and-url-error.json");
    }

    @Test
    public void testCaseInsensitiveCoordinate() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonArrayTest.json");
        Map coord = new HashMap();
        coord.put("c0dE", "ints");
        try
        {
            ncube.getCell(coord);
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        coord.clear();
        coord.put("codE", "ints");
        assertNotNull(ncube.getCell(coord));
    }

    @Test
    public void testLargeNumberOfColumns() throws Exception
    {
        NCube ncube = new NCube("BigDaddy");
        Axis axis = new Axis("numbers", AxisType.SET, AxisValueType.LONG, true, Axis.DISPLAY);
        ncube.addAxis(axis);
        Map coord = new HashMap();

        long start = System.nanoTime();
        for (int i = 0; i < 10000; i += 10)
        {
            RangeSet set = new RangeSet(i);
            Range range = new Range(i + 1, i + 4);
            set.add(range);
            axis.addColumn(set);
            coord.put("numbers", i);
            ncube.setCell(i * 2, coord);
        }

        long stop = System.nanoTime();

        double diff = (stop - start) / 1000.0;  // usec
        println("build 1,000 columns = " + (diff / 1000.0) + " ms");

        start = System.nanoTime();
        for (int i = 0; i < 10000; i += 10)
        {
            coord.put("numbers", i);
            Integer ans = (Integer) ncube.getCell(coord);
            assertEquals(i * 2, ans.intValue());
        }
        stop = System.nanoTime();

        diff = (stop - start) / 1000.0;  // usec
        println("lookup 1,000 times large number of columns = " + (diff / 1000.0) + " ms");
    }

    @Test
    public void testAtCommand() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testAtCommand.json");
        NCubeManager.addCube(ncube.getApplicationID(), ncube);
        Map coord = new CaseInsensitiveMap();
        coord.put("Bu", "PIM");
        coord.put("State", "GA");
        String x = (String) ncube.getCell(coord);
        assertEquals("1", x);

        coord.put("state", "OH");
        x = (String) ncube.getCell(coord);
        assertEquals("2", x);

        coord.put("STATE", "TX");
        x = (String) ncube.getCell(coord);
        assertEquals("3", x);

        coord.put("state", "WY");
        x = (String) ncube.getCell(coord);
        assertEquals("4", x);

        coord.put("bu", "EQM");
        x = (String) ncube.getCell(coord);
        assertEquals("1", x);

        Set<String> scope = ncube.getRequiredScope();
        assertTrue(scope.size() == 2);
    }

    @Test
    public void testOverlappingRangeCubeError() throws Exception
    {
        try
        {
            NCubeManager.getNCubeFromResource("idBasedCubeError.json");
            fail("should not get here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    public void testRangeEquality() throws Exception
    {
        Range a = new Range(5, 10);
        Range b = new Range(5, 10);
        Range c = new Range(1, 10);
        Range d = new Range(0, 11);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(5));
    }

    @Test
    public void testEqualsMetaPropsMismatch()  throws Exception
    {
        NCube cube1 = getTestNCube2D(false);
        NCube cube2 = getTestNCube2D(false);

        assertEquals(cube1, cube2);

        cube1.setMetaProperty("foo", "bar");
        assertNotEquals(cube1, cube2);
        cube2.setMetaProperty("foo", "bar");
        assertEquals(cube1, cube2);
        cube1.removeMetaProperty("foo");
        assertNotEquals(cube1, cube2);
        cube1.setMetaProperty("foo", "baz");
        assertNotEquals(cube1, cube2);
        cube1.clearMetaProperties();
        cube2.removeMetaProperty("foo");
        assertEquals(cube1, cube2);
    }

    @Test
    public void testEqualsAxisNameMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foot", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube2.addAxis(axis2);

        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsAxisMetaMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        axis1.setMetaProperty("fingers", 4);
        axis1.setMetaProperty("thumb", 1);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsAxisTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.RANGE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsAxisValueTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.LONG, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsAxisDefaultMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsColumnCountMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube1.addColumn("foo", "qux");

        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsColumnTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube1.addColumn("foo", "qux");

        cube2.addAxis(axis2);
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsColumnValueMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube1.addColumn("foo", "baz");

        cube2.addAxis(axis2);
        cube2.addColumn("foo", "qux");
        assertNotEquals(cube1, cube2);
    }

    @Test
    public void testEqualsColumnMetaPropertiesMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true);
        NCube cube1 = new NCube("bar");
        NCube cube2 = new NCube("bar");

        cube1.addAxis(axis1);
        cube1.addColumn("foo", "baz");
        Column col = axis1.findColumn("baz");
        col.setMetaProperty("Glock", "23");

        cube2.addAxis(axis2);
        cube2.addColumn("foo", "baz");
        assertNotEquals(cube1, cube2);
    }


    @Test
    public void testMaxAxisId()  throws Exception
    {
        NCube cube = new NCube("fourD");
        long maxId = cube.getMaxAxisId();
        assertEquals(0, maxId);

        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.getMaxAxisId() + 1);
        cube.addAxis(axis1);
        maxId = cube.getMaxAxisId();
        assertEquals(1, maxId);

        Axis axis2 = new Axis("bar", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.getMaxAxisId() + 1);
        cube.addAxis(axis2);
        maxId = cube.getMaxAxisId();
        assertEquals(2, maxId);

        Axis axis3 = new Axis("baz", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.getMaxAxisId() + 1);
        cube.addAxis(axis3);
        maxId = cube.getMaxAxisId();
        assertEquals(3, maxId);

        Axis axis4 = new Axis("qux", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.getMaxAxisId() + 1);
        cube.addAxis(axis4);
        maxId = cube.getMaxAxisId();
        assertEquals(4, maxId);
    }

    @Test
    public void testRangeSetEquality() throws Exception
    {
        RangeSet a = new RangeSet(1);
        a.add(new Range(5, 10));

        RangeSet b = new RangeSet(1);
        b.add(new Range(5, 10));

        assertTrue(a.equals(b));

        RangeSet c = new RangeSet(1);
        c.add(new Range(5, 11));
        assertFalse(a.equals(c));

        assertFalse(a.equals(1));
    }

    @Test
    public void testMultiThreadedCellExecution() throws Exception
    {
        final NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        final Map coord = new HashMap();
        coord.put("code", "exp");

        Thread t1 = new Thread(new Runnable()
        {
            public void run()
            {
                Object x = ncube.getCell(coord);
            }
        });
        Thread t2 = new Thread(new Runnable()
        {
            public void run()
            {
                Object y = ncube.getCell(coord);
            }
        });
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    @Test
    public void testRemoveCellById()
    {
        NCube ncube = getTestNCube2D(true);
        Axis age = ncube.getAxis("age");
        Axis gender = ncube.getAxis("gender");
        Column ageCol = age.getColumns().get(0);
        long ageCol0 = ageCol.id;
        Column genderCol = gender.getColumns().get(0);
        long genderCol0 = genderCol.id;
        assertTrue(ageCol0 != 0);
        assertTrue(genderCol0 != 0);

        Set colIds = new HashSet();
        colIds.add(ageCol0);
        colIds.add(genderCol0);
        ncube.setCellById(1.1, colIds);

        Map coord = new HashMap();
        coord.put("AGE", ageCol.getValueThatMatches());
        coord.put("GENDER", genderCol.getValueThatMatches());
        Double x = (Double) ncube.getCell(coord);
        assertTrue(x == 1.1);

        assertTrue(ncube.containsCellById(colIds));
        ncube.removeCellById(colIds);
        assertFalse(ncube.containsCellById(colIds));
    }

    @Test
    public void testStandardizeColumnValueErrorHandling()
    {
        Axis states = getStatesAxis();
        try
        {
            states.standardizeColumnValue(null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testReadCubeList() throws Exception
    {
        List<NCube> ncubes = NCubeManager.getNCubesFromResource("testCubeList.json");
        assertTrue(ncubes.size() == 2);
        NCube ncube1 = ncubes.get(0);
        assertEquals(ncube1.getName(), "TestCube");
        NCube ncube2 = ncubes.get(1);
        assertEquals(ncube2.getName(), "idTest");
    }

    @Test
    public void testTemplate()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        Map coord = new HashMap();
        coord.put("code", "stdTemplate");
        coord.put("overdue", "not overdue");
        String str = (String) ncube.getCell(coord);
        assertEquals("Dear 2, Your balance of 3.14 is not overdue.", str);
        str = (String) ncube.getCell(coord);
        assertEquals("Dear 2, Your balance of 3.14 is not overdue.", str);

        coord.put("code", "stdTemplate2");
        coord.put("overdue", "overdue");
        str = (String) ncube.getCell(coord);
        assertEquals("2, Your balance is overdue 3.14", str);
        str = (String) ncube.getCell(coord);
        assertEquals("2, Your balance is overdue 3.14", str);

        coord.put("code", "stdTemplate3");
        str = (String) ncube.getCell(coord);
        assertEquals("Nothing to replace", str);
        str = (String) ncube.getCell(coord);
        assertEquals("Nothing to replace", str);
    }

    @Test
    public void testTemplateRefOtherCube()
    {
        NCubeManager.getNCubeFromResource("template2.json");   // Get it loaded
        NCube ncube = NCubeManager.getNCubeFromResource("template1.json");
        Map coord = new HashMap();
        coord.put("state", "GA");
        coord.put("code", 1);
        long start = System.nanoTime();
        String str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.15 on your car insurance. Does this 0.12 work?", str);
        long stop = System.nanoTime();
        //        System.out.println("str = " + str);
        //        System.out.println((stop - start)/1000000);
        coord.put("state", "OH");
        coord.put("code", 1);
        start = System.nanoTime();
        str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.14 on your boat insurance. Does this 0.15 work?", str);
        stop = System.nanoTime();
        //        System.out.println("str = " + str);
        //        System.out.println((stop - start)/1000000);

        coord.put("state", "AL");
        coord.put("code", 1);
        str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.15 on your car insurance. Does this 0.12 work?", str);

        coord.put("state", "AR");
        coord.put("code", 1);
        str = (String) ncube.getCell(coord);
        assertEquals("Dear Bitcoin, please continue your upward growth trajectory.", str);
    }

    @Test
    public void testTemplateWithEquivalentCube()
    {
        NCubeManager.getNCubeFromResource("template2-equivalent.json");   // Get it loaded
        NCube ncube = NCubeManager.getNCubeFromResource("template1.json");
        Map coord = new HashMap();
        coord.put("state", "GA");
        coord.put("code", 1);
        long start = System.nanoTime();
        String str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.15 on your car insurance. Does this 0.12 work?", str);
        long stop = System.nanoTime();
        //        System.out.println("str = " + str);
        //        System.out.println((stop - start)/1000000);
        coord.put("state", "OH");
        coord.put("code", 1);
        start = System.nanoTime();
        str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.14 on your boat insurance. Does this 0.15 work?", str);
        stop = System.nanoTime();
        //        System.out.println("str = " + str);
        //        System.out.println((stop - start)/1000000);

        coord.put("state", "AL");
        coord.put("code", 1);
        str = (String) ncube.getCell(coord);
        assertEquals("You saved 0.15 on your car insurance. Does this 0.12 work?", str);

        coord.put("state", "AR");
        coord.put("code", 1);
        str = (String) ncube.getCell(coord);
        assertEquals("Dear Bitcoin, please continue your upward growth trajectory.", str);
    }

    @Test
    public void testClassLoader() {
        NCube ncube = NCubeManager.getNCubeFromResource("ncube-class-loader-test.json");
        Map coord = new HashMap();

        coord.put("code", "local");
        assertEquals("Successful test run of local url classloader.", ncube.getCell(coord));

        coord.put("code", "remote");
        assertEquals("Successful test run of remote url classloader.", ncube.getCell(coord));
    }

    @Test
    public void testExpressionWithImports()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        Map coord = new HashMap();
        coord.put("code", "expWithImport");
        String str = ncube.getCell(coord);
        assertEquals(str, "I love Bitcoin");
    }

    @Test
    public void testTemplateRequiredScope()
    {
        NCubeManager.getNCubeFromResource("stringIds.json");
        NCube<String> ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        Set<String> scope = ncube.getRequiredScope();
        assertEquals(1, scope.size());
        assertTrue(scope.contains("CODe"));

        scope = ncube.getOptionalScope();
        assertEquals(2, scope.size());
        assertTrue(scope.contains("AGe"));
        assertTrue(scope.contains("OVERDUe"));

        NCubeManager.getNCubeFromResource("template2.json");   // Get it loaded
        ncube = NCubeManager.getNCubeFromResource("template1.json");
        scope = ncube.getRequiredScope();
        assertEquals(2, scope.size());
        assertTrue(scope.contains("coDe"));
        assertTrue(scope.contains("staTe"));
    }

//    @Test
//    public void testXmlTemplateRefOtherCube()
//    {
//        NCubeManager.getNCubeFromResource("template2.json");   // Get it loaded
//        NCube ncube = NCubeManager.getNCubeFromResource("template1.json");
//        Map coord = new HashMap();
//        coord.put("state", "ID");
//        coord.put("code", 1);
//        coord.put("firstname", "John");
//        coord.put("nickname", "Jack");
//        coord.put("lastname", "Kennedy");
//        coord.put("salutation", "Dear");
//
//        long start = System.nanoTime();
//        String str = (String) ncube.getCell(coord);
//        assertEquals("<document type='letter'>\n" +
//                " 0.15\n" +
//                " <foo:to xmlns:foo='baz'>\n" +
//                "  John &apos;Jack&apos; Kennedy\n" +
//                " </foo:to>\n" +
//                " How are you today?\n" +
//                "</document>", str);
//        long stop = System.nanoTime();
//        System.out.println("str = " + str);
//        System.out.println((stop - start)/1000000);
//    }

    @Test
    public void testStringIds() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("stringIds.json");

        Map coord = new HashMap();
        coord.put("age",15);
        coord.put("state", "CA");
        assertEquals("young CA", ncube.getCell(coord));
        coord.put("age",18);
        coord.put("state", "OH");
        assertEquals("adult OH", ncube.getCell(coord));
        coord.put("age",60);
        coord.put("state", "TX");
        assertEquals("old TX", ncube.getCell(coord));
        coord.put("age",99);
        coord.put("state", "TX");
        assertEquals("def TX", ncube.getCell(coord));
    }

    @Test
    public void testEmptyCube()
    {
        NCube ncube = new NCube("Empty");
        assertNotNull(ncube.toHtml());  // Ensure it does not blow up with exception on completely empty n-cube.
    }

    @Test
    public void testDuplicateEqualsAndHashCode()
    {
        simpleJsonCompare("2DSimpleJson.json");
        simpleJsonCompare("approvalLimits.json");
        simpleJsonCompare("big5D.json");
        simpleJsonCompare("expressionAxis.json");
        simpleJsonCompare("expressionAxis2.json");
        simpleJsonCompare("idBasedCube.json");
        simpleJsonCompare("simpleJsonArrayTest.json");
        simpleJsonCompare("simpleJsonExpression.json");
        simpleJsonCompare("stringIds.json");
        simpleJsonCompare("template1.json");
        simpleJsonCompare("template2.json");
        simpleJsonCompare("testAtCommand.json");
        simpleJsonCompare("testCube1.json");
        simpleJsonCompare("testCube2.json");
        simpleJsonCompare("testCube3.json");
        simpleJsonCompare("testCube4.json");
        simpleJsonCompare("testCube5.json");
        simpleJsonCompare("testCube6.json");
        simpleJsonCompare("urlContent.json");
    }

    @Test
    public void testIdNoValue()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource("idNoValue.json");
        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");
        String s = ncube.getCell(coord);
        assertEquals("18 OH", s);

        coord.put("age", 19);
        coord.put("state", "TX");
        s = ncube.getCell(coord);
        assertEquals("TX", s);
    }

    @Test
    public void testUpdateColumn()
    {
        Axis dow = getShortDaysOfWeekAxis();
        Column wed = dow.findColumn("Wed");
        dow.updateColumn(wed.id, "aWed");
        wed = dow.getColumns().get(2);
        assertEquals(wed.getValue(), "aWed");

        Column mon = dow.findColumn("Mon");
        dow.updateColumn(mon.id, "aMon");
        mon = dow.getColumns().get(0);
        assertEquals(mon.getValue(), "aMon");

        Column sun = dow.findColumn("Sun");
        dow.updateColumn(sun.id, "aSun");
        sun = dow.getColumns().get(6);
        assertEquals(sun.getValue(), "aSun");

        List<Column> cols = dow.getColumnsWithoutDefault();
        assertEquals(cols.get(4).getValue(), "aMon");
        assertEquals(cols.get(5).getValue(), "aSun");
        assertEquals(cols.get(6).getValue(), "aWed");

        assertEquals(-1, cols.get(4).compareTo(new Column(null, dow.getNextColId())));
    }

    @Test
    public void testUpdateColumns()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource("updateColumns.json");
        assertEquals(30, ncube.cells.size());

        // Delete 1st, middle, and last column
        Map<Object, Long> valueToId = new HashMap<Object, Long>();
        Axis code = ncube.getAxis("code");
        for (Column column : code.getColumns())
        {
            valueToId.put(column.getValue(), column.getId());
        }
        Axis axisDto = new Axis("code", AxisType.DISCRETE, AxisValueType.LONG, true);
        axisDto.addColumn(2);
        axisDto.addColumn(4);
        List<Column> cols = axisDto.getColumns();
        for (Column column : cols)
        {
            long id = valueToId.get(column.getValue());
            column.setId(id);
        }
        // 1,3,5 deleted
        ncube.updateColumns(axisDto);
        assertEquals(15, ncube.cells.size());

        // Delete 1st, middle, last on state axis
        code = ncube.getAxis("state");
        for (Column column : code.getColumns())
        {
            valueToId.put(column.getValue(), column.getId());
        }
        axisDto = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, true);
        axisDto.addColumn("CA");
        axisDto.addColumn("TX");
        cols = axisDto.getColumns();
        for (Column column : cols)
        {
            long id = valueToId.get(column.getValue());
            column.setId(id);
        }

        ncube.updateColumns(axisDto);
        assertEquals(6, ncube.cells.size());

        ncube.deleteColumn("code", null);
        assertEquals(4, ncube.cells.size());

        try
        {
            ncube.updateColumns(null);
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }

        try
        {
            Axis fake = new Axis("fake", AxisType.DISCRETE, AxisValueType.DOUBLE, false);
            ncube.updateColumns(fake);
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testProveDefaultLast()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED);
        axis.addColumn("alpha");
        axis.addColumn("charlie");
        axis.addColumn("bravo");
        List<Column> cols = axis.getColumns();
        assertEquals(cols.get(0).getValue(), "alpha");
        assertEquals(cols.get(1).getValue(), "bravo");
        assertEquals(cols.get(2).getValue(), "charlie");
        assertEquals(cols.get(3).getValue(), null);

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED);
        axis.addColumn("alpha");
        axis.addColumn("charlie");
        axis.addColumn("bravo");
        cols = axis.getColumns();
        assertEquals(3, cols.size());
        assertEquals(cols.get(0).getValue(), "alpha");
        assertEquals(cols.get(1).getValue(), "bravo");
        assertEquals(cols.get(2).getValue(), "charlie");

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY);
        axis.addColumn("alpha");
        axis.addColumn("charlie");
        axis.addColumn("bravo");
        cols = axis.getColumns();
        assertEquals(cols.get(0).getValue(), "alpha");
        assertEquals(cols.get(1).getValue(), "charlie");
        assertEquals(cols.get(2).getValue(), "bravo");
        assertEquals(cols.get(3).getValue(), null);

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("alpha");
        axis.addColumn("charlie");
        axis.addColumn("bravo");
        cols = axis.getColumns();
        assertEquals(3, cols.size());
        assertEquals(cols.get(0).getValue(), "alpha");
        assertEquals(cols.get(1).getValue(), "charlie");
        assertEquals(cols.get(2).getValue(), "bravo");
    }

    @Test
    public void testShortHandReferences() throws Exception
    {
        NCubeManager.getNCubeFromResource("stringIds.json");
        NCube ncube = NCubeManager.getNCubeFromResource("simpleJsonExpression.json");
        Map coord = new HashMap();
        coord.put("code", "FixedExp");
        assertEquals(6.28, ncube.getCell(coord));

        coord.put("code", "FixedExtExp");
        assertEquals("young OH", ncube.getCell(coord));

        coord.put("code", "RelativeExp");
        assertEquals(32, ncube.getCell(coord));

        coord.put("code", "RelativeExtExp");
        assertEquals("adult TX", ncube.getCell(coord));
    }

    @Test(expected=RuntimeException.class)
    public void testGroovyExpressionThatDoesntExist() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testExpressionAxisUrl.json");
        Map coord = new HashMap();
        coord.put("code", "exp");
        assertEquals(6.28, ncube.getCell(coord));

    }

    @Test(expected=RuntimeException.class)
    public void testNullCube()
    {
        NCubeManager.getNCubeFromResource("null-error.json");
    }

    @Test
    public void testCoordinateNotFoundException()
    {
        RuntimeException r = new RuntimeException();
        try
        {
            throw new CoordinateNotFoundException("foo", r);
        }
        catch (Exception e)
        {
            assertEquals("foo", e.getMessage());
            assertSame(r, e.getCause());
        }
    }

    @Test
    public void testExpandableUrlRef()
    {
        NCubeManager.getNCubeFromResource("urlPieces.json");
        NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json");

        Map coord = new HashMap();
        coord.put("env_level", "local");
        coord.put("protocol", "http");
        coord.put("content", "ai");
        String html = (String) ncube.getCell(coord);
        assertNotNull(html);

        coord.put("protocol", "https");
        coord.put("content", "ai");
        String html1 = (String) ncube.getCell(coord);
        assertEquals(html, html1);

        coord.put("protocol", "http");
        coord.put("content", "lwt");
        String html2 = (String) ncube.getCell(coord);
        assertNotEquals(html, html2);
    }

    @Test
    public void testTemplateFromUrl()
    {
        NCubeManager.getNCubeFromResource("urlPieces.json");
        NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json");

        Map coord = new HashMap();
        coord.put("env_level", "local");
        coord.put("protocol", "http");
        coord.put("content", "ai");
        String html = (String) ncube.getCell(coord);
        assertNotNull(html);

        coord.put("protocol", "https");
        coord.put("content", "ai");
        String html1 = (String) ncube.getCell(coord);
        assertEquals(html, html1);

        coord.put("protocol", "http");
        coord.put("content", "vend");
        String html2 = (String) ncube.getCell(coord);
        assertNotEquals(html, html2);
    }

    @Test
    public void testExpressionFromUrl()
    {
        NCubeManager.getNCubeFromResource("urlPieces.json");
        NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json");

        Map coord = new HashMap();
        coord.put("env_level", "local");
        coord.put("protocol", "http");
        coord.put("content", "hello");
        String html = (String) ncube.getCell(coord);
        assertNotNull(html);
        assertEquals("Hello, world.", html);

        coord.put("protocol", "https");
        coord.put("content", "hello");
        String html1 = (String) ncube.getCell(coord);
        assertEquals(html, html1);

        coord.put("protocol", "http");
        coord.put("content", "hello2");
        html = (String) ncube.getCell(coord);
        assertNotNull(html);
        assertEquals("Hello, world 2.", html);

        coord.put("protocol", "http");
        coord.put("content", "95");
        Integer num = (Integer) ncube.getCell(coord);
        assertEquals(95, num.intValue());
    }

    @Test
    public void testNCubeNameParser()
    {
        String name = "['Less than $10,000':['startIncurredAmount':'0','endIncurredAmount':'10000'],'$10,000 - $25,000':['startIncurredAmount':'10000','endIncurredAmount':'25000'],'$25,000 - $50,000':['startIncurredAmount':'25000','endIncurredAmount':'50000'],'More than $50,000':['startIncurredAmount':'50000','endIncurredAmount':'0']]";
        Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(name);
        assertFalse(m.find());

        m = Regexes.groovyRelRefCubeCellPattern.matcher(name);
        assertFalse(m.find());

        m = Regexes.groovyAbsRefCubeCellPattern.matcher(name);
        assertFalse(m.find());

        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(name);
        assertFalse(m.find());

        name = "@Foo([:])";

        m = Regexes.groovyRelRefCubeCellPattern.matcher(name);
        m.find();
        assertEquals("Foo", m.group(2));

        name = "@Foo([:])";
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name);
        m.find();
        assertEquals("Foo", m.group(2));

        name = "$Foo([alpha:'bravo'])";
        m = Regexes.groovyAbsRefCubeCellPattern.matcher(name);
        m.find();
        assertEquals("Foo", m.group(2));

        name = "$Foo[:]";
        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(name);
        m.find();
        assertEquals("Foo", m.group(2));
    }

    @Test
    public void testGroovyMethods()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods1.json");
        Map coord = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        assertEquals(2, ncube.getCell(coord));

        coord.put("method", "bar");
        assertEquals(4, ncube.getCell(coord));

        coord.put("method", "baz");
        assertEquals(8, ncube.getCell(coord));

        coord.put("method", "qux");
        assertEquals(16, ncube.getCell(coord));

        coord.put("method", "foo");
        coord.put("state", "TX");
        assertEquals(3, ncube.getCell(coord));

        coord.put("method", "bar");
        assertEquals(9, ncube.getCell(coord));

        coord.put("method", "baz");
        assertEquals(27, ncube.getCell(coord));

        coord.put("method", "qux");
        assertEquals(81, ncube.getCell(coord));

        coord.put("method", "foo");
        coord.put("state", "OH");
        assertEquals(2, ncube.getCell(coord));

        coord.put("method", "bar");
        assertEquals(4, ncube.getCell(coord));

        coord.put("method", "baz");
        assertEquals(8, ncube.getCell(coord));

        coord.put("method", "qux");
        assertEquals(16, ncube.getCell(coord));
    }

    @Test(expected = RuntimeException.class)
    public void testCommandCellReferencedCubeNotFoundOnExpandUrl() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("expand-url-cube-not-found-error.json");
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("Sites", "StringFromLocalUrl");
        ncube.getCell(map);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgumentsToConstructor() throws Exception
    {
        new GroovyTemplate(null, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgumentsToConstructor2() throws Exception
    {
        new GroovyMethod(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidColumn() throws Exception
    {
        new GroovyTemplate(null, null, false);
    }

    @Test
    public void testContainsCellValue()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCell.json");

        Map coord = new HashMap();
        coord.put("Gender", "Male");
        assertTrue(ncube.containsCell(coord, true));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCell(coord));

        coord.put("gender", "GI Joe");
        try
        {
            ncube.containsCell(coord);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof CoordinateNotFoundException);
        }

        ncube.setDefaultCellValue(null);

        coord.put("gender", "Male");
        assertFalse(ncube.containsCell(coord));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCell(coord));

        coord.put("gender", "GI Joe");
        try
        {
            ncube.containsCell(coord);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof CoordinateNotFoundException);
        }
    }

    @Test
    public void testMetaPropsRead() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCell.json");
        assertTrue(ncube.getMetaProperties().size() > 0);
        assertEquals("y", ncube.getMetaProperties().get("x"));

        Axis axis = ncube.getAxis("gender");
        assertTrue(axis.getMetaProperties().size() > 0);
        assertEquals(2L, axis.getMetaProperties().get("feet"));

        Column col = axis.findColumn("Female");
        assertNotNull(col);
        assertTrue(col.getMetaProperties().size() > 0);
        assertEquals("Jane", col.getMetaProperties().get("Name"));  // intentional mismatch on case
        assertEquals(36L, col.getMetaProperties().get("age"));

        String json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        assertTrue(ncube.getMetaProperties().size() > 0);
        assertEquals("y", ncube.getMetaProperties().get("x"));

        axis = ncube.getAxis("gender");
        assertTrue(axis.getMetaProperties().size() > 0);
        assertEquals(2L, axis.getMetaProperties().get("feet"));

        col = axis.findColumn("Female");
        assertNotNull(col);
        assertTrue(col.getMetaProperties().size() > 0);
        assertEquals("Jane", col.getMetaProperties().get("Name"));  // intentional mismatch on case
        assertEquals(36L, col.getMetaProperties().get("age"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueException() throws Exception {
        CellInfo.parseJsonValue(Boolean.TRUE, "http://www.foo.com", "foo", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueNonUrlException() throws Exception {
        CellInfo.parseJsonValue("blah blah blah", null, "foo", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonValueWithUnknownType() throws Exception {
        CellInfo.parseJsonValue(new Object(), null, "foo", true);
    }

    @Test
    public void testParseJsonValueGroovyMethod() throws Exception {
        GroovyMethod method = (GroovyMethod)CellInfo.parseJsonValue("def [5]", null, "method", true);
        assertEquals(new GroovyMethod("def [5]", null), method);
        //method.execute(new HashMap()):
    }

    @Test
    public void testMetaPropAPIs() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCell.json");

        Axis axis = ncube.getAxis("gender");
        assertTrue(axis.getMetaProperties().size() > 0);
        assertEquals(2L, axis.getMetaProperties().get("feet"));

        Column col = axis.findColumn("Female");
        assertNotNull(col);

        ncube.setMetaProperty("language", "groovy");
        axis.setMetaProperty("car", "cruze");
        col.setMetaProperty("one", 1);

        assertTrue(ncube.getMetaProperties().size() == 3);
        assertTrue(axis.getMetaProperties().size() == 2);
        assertTrue(col.getMetaProperties().size() == 3);

        Map metaProps = new HashMap();
        metaProps.put("sport", "football");
        metaProps.put("currency", "Bitcoin");

        ncube.addMetaProperties(metaProps);
        axis.addMetaProperties(metaProps);
        col.addMetaProperties(metaProps);
        ncube.removeMetaProperty("sha1");

        String json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        axis = ncube.getAxis("gender");
        col = axis.findColumn("Female");

        assertTrue(ncube.getMetaProperties().size() == 5);
        assertTrue(axis.getMetaProperties().size() == 4);
        assertTrue(col.getMetaProperties().size() == 5);

        ncube.clearMetaProperties();
        axis.clearMetaProperties();
        col.clearMetaProperties();

        assertTrue(ncube.getMetaProperties().size() == 0);
        assertTrue(axis.getMetaProperties().size() == 0);
        assertTrue(col.getMetaProperties().size() == 0);

        json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        axis = ncube.getAxis("gender");
        col = axis.findColumn("Female");

        assertTrue(ncube.getMetaProperties().size() == 1);
        assertTrue(axis.getMetaProperties().size() == 0);
        assertTrue(col.getMetaProperties().size() == 0);
    }

    @Test
    public void testHtmlCubeTitle() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("debugExp.json");
        String html = ncube.toHtml();
        assertNotNull(html);
//        System.out.println("html = " + html);
    }

    @Test
    public void testHtml2DCubeTitle() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("debugExp2D.json");
        String html = ncube.toHtml();
        assertNotNull(html);
//        System.out.println("html = " + html);
    }

    /**
     * Must set the URL to the path containing the test groovy code.  Do not include
     * the com/... in the path.  This is a RESOURCE URL location, which points to the
     * root of a resource hierarchy, in this case, the resources are groovy source code.
     *
     * This test is a 'spot' test, and should not be run with all the other tests. It should
     * be ignored by default, unless you are testing the single-step debugging
     * capabilities of n-cube Groovy.
     */
    @Test
    public void testDebugExpression() throws Exception
    {
//        List urls = new ArrayList();
//        URL url = NCubeManager.class.getResource("/");
//        urls.add(url.toString());
//        urls.add("http://www.cedarsoftware.com");

//        NCube classpathCube = NCubeManager.getNCubeFromResource("sys.classpath.testing");
//        NCubeManager.addCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name()), cube);
//        NCubeManager.addCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, "ncube.test", "1.0.0", ReleaseStatus.SNAPSHOT.name()), cube);

//        ApplicationID appId1 = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
//        NCubeManager.addBaseResourceUrls(appId1, urls);
//        ApplicationID appId2 = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, "1.0.0", ReleaseStatus.SNAPSHOT.name());
//        NCubeManager.addBaseResourceUrls(appId2, urls);

        NCube ncube = NCubeManager.getNCubeFromResource("debugExp.json");
        Map coord = new HashMap();
        int age = 9;
        coord.put("age", age);
        assertEquals(Math.pow(age, 2), ncube.getCell(coord));
    }

    public static NCube createTempDirClassPathCube() throws Exception
    {
        NCube cpCube = new NCube<Double>("sys.classpath");

        Axis axis = new Axis("environment", AxisType.DISCRETE, AxisValueType.STRING, true);
        cpCube.addAxis(axis);

        String base = System.getProperty("java.io.tmpdir");
        cpCube.setCell(new GroovyExpression("['" + new File(base).toURI().toURL().toString() + "','http://www.cedarsoftware.com']", null), new HashMap());
        return cpCube;
    }

    @Test
    public void testReloadGroovyClass() throws Exception
    {
        String base = System.getProperty("java.io.tmpdir");

        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "reloadGroovyTest", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        NCube cpCube = createTempDirClassPathCube();

        // manually set classpath cube
        NCubeManager.addCube(appId, cpCube);

        FileOutputStream fo = new FileOutputStream(base + "Abc.groovy");
        String code = "import ncube.grv.exp.NCubeGroovyExpression; class Abc extends NCubeGroovyExpression { def run() { return 10 } }";
        fo.write(code.getBytes());
        fo.close();

        NCubeManager.getNCubeFromResource(appId, "testReloadGroovyClass.json");
        NCube ncube = NCubeManager.getCube(appId, "testReloadGroovy");

        Map coord = new HashMap();
        coord.put("state", "OH");
        Map output = new LinkedHashMap();
        Object out = ncube.getCell(coord, output);
        assertEquals(10, out);

        NCubeManager.clearCache(appId);

        // add classpath cube back in so everything is found correctly
        NCubeManager.addCube(appId, cpCube);

        fo = new FileOutputStream(base + "Abc.groovy");
        code = "import ncube.grv.exp.NCubeGroovyExpression; class Abc extends NCubeGroovyExpression { def run() { return 20 } }";
        fo.write(code.getBytes());
        fo.close();
        fo.flush();

        ncube = NCubeManager.getNCubeFromResource(appId, "testReloadGroovyClass.json");
        out = ncube.getCell(coord, output);
        assertEquals(20, out);

        coord.put("state", "IN");
        String gcode = (String) ncube.getCell(coord, output);
        assertEquals(code, gcode);
    }

    @Test
    public void testCoordinateGetter()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("arrays.json");
        for (Set<Column> cols : (Iterable<Set<Column>>) ncube.getCellMap().keySet())
        {
            // This code is not generalized and intentionally relies on arrays.json being 1-dimensional
            Column col = cols.iterator().next();
            Set<Long> coord = new HashSet<>();
            coord.add(col.getId());
            Map<String, CellInfo> coordinate = new CaseInsensitiveMap<>();
            ncube.getColumnsAndCoordinateFromIds(coord, coordinate);
            assertTrue(coordinate.containsKey("code"));
        }
    }

    @Test
    public void testObjectToMapWithNull()
    {
        try
        {
            NCube.objectToMap(null);
            fail("should not make it here");
        }
        catch(IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("convert null into a Map"));
            assertTrue(e.getMessage().contains("null passed"));
        }
    }
    @Test
    public void testObjectToMap()
    {
        class dto
        {
            private Date when = new Date();
            String fname = "Albert";
            String lname = "Einstein";
        }
        dto d = new dto();
        Map coord = NCube.objectToMap(d);

        // test case-insensitivity
        assertEquals("Albert", coord.get("FName"));
        assertEquals("Einstein", coord.get("LName"));
        assertEquals("Albert", coord.get("fname"));
        assertEquals("Einstein", coord.get("lname"));
        assertEquals(d.when, coord.get("when"));
    }

    @Test
    public void testObjectToMapWithConflictingFieldNameInParent()
    {
        class parentDto
        {
            String fname = "Foo";
        }

        class childDto extends parentDto
        {
            private Date when = new Date();
            String fname = "Albert";
            String lname = "Einstein";
        }

        childDto dto = new childDto();
        Map coord = NCube.objectToMap(dto);
        assertTrue(coord.containsKey("com.cedarsoftware.ncube.TestNCube$1parentDto.fname"));
        assertEquals("Albert", coord.get("FName"));
        assertEquals("Foo", coord.get("com.cedarsoftware.ncube.TestNCube$1parentDto.fname"));
        assertEquals("Einstein", coord.get("LName"));
        assertEquals(dto.when, coord.get("when"));
    }

    @Test
    public void testContainsCellWithDefault() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCellNoDefault.json");

        Map input = new HashMap();
        input.put("gender", "Female");
        assertTrue(ncube.containsCell(input, true));
        input.put("gender", "Male");
        assertFalse(ncube.containsCell(input, true));
    }

    @Test
    public void testMetaProps()
    {
        NCube ncube = new NCube("dude");
        assertNull(ncube.removeMetaProperty("test"));
        ncube.setMetaProperty("test", true);
        assertTrue((Boolean) ncube.getMetaProperties().get("test"));
        assertEquals(1, ncube.getMetaProperties().size());

        Map metaProps = new HashMap();
        metaProps.put("foo", "bar");
        ncube.addMetaProperties(metaProps);
        assertTrue((Boolean) ncube.getMetaProperties().get("test"));
        assertEquals("bar", ncube.getMetaProperties().get("foo"));
        assertEquals(2, ncube.getMetaProperties().size());

        ncube = new NCube("dude");
        ncube.addMetaProperties(metaProps);
        assertEquals("bar", ncube.getMetaProperties().get("foo"));
        assertEquals(1, ncube.getMetaProperties().size());
    }

    @Test
    public void testGetLong()
    {
        Map map = new HashMap();
        map.put("food", 'w');
        try
        {
            NCube.getLong(map, "food");
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testGetBoolean()
    {
        Map map = new HashMap();
        map.put("food", null);
        assertFalse(NCube.getBoolean(map, "food"));

        try
        {
            map.put("food", 9);
            NCube.getBoolean(map, "food");
            fail("should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testRequiredScope()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("requiredScopeKeys.json");
        Set<String> scope = ncube.getRequiredScope();
        assertEquals(3, scope.size());
        assertTrue(scope.contains("codE"));
        assertTrue(scope.contains("bU"));
        assertTrue(scope.contains("sTaTe"));

        Axis axis = ncube.getAxis("codE");
        Object b = ncube.extractMetaPropertyValue(axis.getMetaProperty("extraByte"));
        assertTrue(b instanceof Byte);
        assertEquals((byte)8, b);

        List<Column> columns = axis.getColumns();
        assertEquals(3, columns.size());
        Column col1 = columns.get(0);
        Map map = (Map) ncube.extractMetaPropertyValue(col1.getMetaProperty("colProp"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("dude"));
        assertEquals("male", map.get("dude"));

        Column col2 = columns.get(1);
        map = (Map) ncube.extractMetaPropertyValue(col2.getMetaProperty("colProp"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("chick"));
        assertEquals("female", map.get("chick"));

        Column col3 = columns.get(2);
        map = (Map) ncube.extractMetaPropertyValue(col3.getMetaProperty("colProp"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("42"));
        assertEquals("meaning of life", map.get("42"));
    }

    @Test
    public void testRequiredScopeSetCell()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("requiredScopeKeys.json");

        Map input = new HashMap();
        input.put("code", 0);
        ncube.setCell("123", input);
        assertTrue(ncube.containsCell(input));

        try
        {
            ncube.getCell(input);
            fail("Should not make it here");
        }
        catch (Exception expected)
        {
            assertTrue(expected.getMessage().contains("not"));
            assertTrue(expected.getMessage().contains("contain"));
            assertTrue(expected.getMessage().contains("scope"));
            assertTrue(expected.getMessage().contains("key"));
        }

        input.put("bU", "blah");
        input.put("StAtE", "blah");
        Object value = ncube.getCell(input);
        assertEquals("123", value);

        input.put("code", -10);
        value = ncube.getCell(input);
        assertEquals("ABC", value);

        input.put("code", 10);
        value = ncube.getCell(input);
        assertEquals("GHI", value);

        input.clear();
        input.put("code", 0);
        ncube.removeCell(input);
        assertFalse(ncube.containsCell(input));
        assertTrue(ncube.containsCell(input, true));

        input.put("bU", "blah");
        input.put("StAtE", "blah");
        value = ncube.getCell(input);
        assertEquals("f", value);   // The default n-cube value (cell no longer exists)
    }

    @Test
    public void testNoRequiredScope()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("noRequiredScope.json");
        Set<String> scope = ncube.getRequiredScope();
        assertEquals(0, scope.size());

        Object value = ncube.getCell(new HashMap());
        assertEquals("XYZ", value);
    }

    @Test
    public void testCubeEquals()
    {
        NCube c1 = NCubeManager.getNCubeFromResource("testCube6.json");
        NCube c2 = c1.duplicate("TestCube");
        String sha1_a = c1.sha1();
        String sha1_b = c2.sha1();
        assertEquals(c1, c2);
        assertEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("Joe");
        assertNotEquals(c1, c2);
        assertNotEquals(c1, "not a cube");
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        Axis a = getStatesAxis();
        c2 = c1.duplicate("TestCube");
        c2.addAxis(a);
        assertNotEquals(c1, c2);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("TestCube");
        a = c2.getAxis("gender");
        c2.deleteAxis("gender");
        a.setName("foo");
        c2.addAxis(a);
        assertNotEquals(c1, c2);
        assertNotEquals(c2, c1);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("TestCube");
        a = c2.getAxis("gender");
        a.setColumnOrder(Axis.DISPLAY);
        assertNotEquals(c1, c2);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("TestCube");
        c2.clearCells();
        assertNotEquals(c1, c2);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("TestCube");
        Map input = new HashMap();
        input.put("gender", "Female");
        c2.setCell(9, input);
        assertNotEquals(c1, c2);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);

        c2 = c1.duplicate("TestCube");
        c2.setDefaultCellValue(null);
        assertNotEquals(c1, c2);
        assertNotEquals(c2, c1);
        sha1_a = c1.sha1();
        sha1_b = c2.sha1();
        assertNotEquals(sha1_a, sha1_b);
    }

    @Test
    public void testDuplicateMetaProperties()
    {
        NCube c1 = NCubeManager.getNCubeFromResource("testDuplicate.json");
        NCube c2 = c1.duplicate("DupeTest");
        assertTrue(c2.getMetaProperties().containsKey("money"));
        assertEquals(100.0, c2.getMetaProperties().get("money"));
        assertTrue(c1.getMetaProperties().equals(c2.getMetaProperties()));

        Axis gender = (Axis) c1.getAxes().get(0);
        Axis sex = (Axis) c2.getAxes().get(0);
        assertTrue(gender.getMetaProperties().size() == 1);
        assertTrue(gender.getMetaProperties().equals(sex.getMetaProperties()));
        assertEquals("gender", sex.getMetaProperty("sex"));

        Column female = sex.findColumn("Female");
        assertTrue((Boolean)female.getMetaProperty("chick"));
        assertFalse((Boolean)female.getMetaProperty("dude"));

        Column male = sex.findColumn("Male");
        assertFalse((Boolean) male.getMetaProperty("chick"));
        assertTrue((Boolean) male.getMetaProperty("dude"));
    }

    @Test
    public void testAbsoluteHttpUrlToGroovy()
    {
        NCube cube = NCubeManager.getNCubeFromResource("urlContent.json");
        Map coord = new HashMap();
        coord.put("sites", "AbsoluteHttpUrl");
        String s = (String) cube.getCell(coord);
        assertEquals("Hello, world.", s);
    }

    @Test
    public void testValidateCubeNames()
    {
        NCube.validateCubeName("This:is.legal_but-hard_to.read");
        try
        {
            NCube.validateCubeName("This:is.not/legal#and-hard_to|read");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        { }
        try
        {
            NCube.validateCubeName(" NotValid");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        { }
    }

    @Test
    public void testValidateCubeName() throws Exception
    {
        NCube.validateCubeName("Joe");
        NCube.validateCubeName("Joe.Dirt");
        NCube.validateCubeName(NCube.validCubeNameChars);
        try
        {
            NCube.validateCubeName("");
            fail("should not make it here");
        }
        catch (Exception e)
        { }

        try
        {
            NCube.validateCubeName(null);
            fail("should not make it here");
        }
        catch (Exception e)
        { }
    }

    @Test
    public void testToJson() throws Exception
    {
        assertEquals("null", NCube.toJson(null));
    }

    @Test
    public void testNCubeApplicationIdParts()
    {
        ApplicationID appId = new ApplicationID("foo", "bar", "0.0.1", ReleaseStatus.SNAPSHOT.name());
        NCube ncube = getTestNCube3D_Boolean();
        ncube.setApplicationID(appId);
        assertEquals(appId.getStatus(), ncube.getStatus());
        assertEquals(appId.getVersion(), ncube.getVersion());
    }

    @Test
    public void testGetColumnsAndCoordinateFromIds()
    {
        NCube cube = getTestNCube3D_Boolean();

        Axis trailor = cube.getAxis("Trailers");
        Column t = trailor.findColumn("M2A");

        Axis vehicles = cube.getAxis("Vehicles");
        Column v = vehicles.findColumn("van");

        Axis bu = cube.getAxis("BU");
        Column b = bu.findColumn("SHS");

        Set<Long> longCoord = new HashSet<>();
        longCoord.add(t.id);
        longCoord.add(v.id);
        longCoord.add(b.id);

        // Make sure all columns are bound correctly
        Map<String, CellInfo> coord = new CaseInsensitiveMap<>();
        Set<Column> boundCols = cube.getColumnsAndCoordinateFromIds(longCoord, coord);
        for (Column column : boundCols)
        {
            assertTrue(column.id == t.id || column.id == v.id || column.id == b.id);
        }

        for (Map.Entry<String, CellInfo> entry : coord.entrySet())
        {
            CellInfo info = entry.getValue();
            assertTrue("M2A".equals(info.value) || "van".equals(info.value) || "SHS".equals(info.value));
        }

        Column t2 = trailor.findColumn("L3A");
        longCoord.add(t2.id);
        try
        {
            cube.getColumnsAndCoordinateFromIds(longCoord, coord);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("more than one column"));
            assertTrue(e.getMessage().contains("per axis"));
        }

        try
        {
            longCoord.remove(t2.id);
            longCoord.remove(t.id);
            cube.getColumnsAndCoordinateFromIds(longCoord, coord);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("column id"));
            assertTrue(e.getMessage().toLowerCase().contains("missing"));
        }
    }

    @Test
    public void testSha1NotSensitiveToAxisNameCase()
    {
        NCube cube1 = new NCube("foo");
        NCube cube2 = new NCube("foo");
        NCube cube3 = new NCube("foo");
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube1.getMaxAxisId());
        Axis axis2 = new Axis("STATE", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube2.getMaxAxisId());
        Axis axis3 = new Axis("state", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube3.getMaxAxisId());
        cube1.addAxis(axis1);
        cube2.addAxis(axis2);
        assertEquals(cube1.sha1(), cube2.sha1());

        cube3.addAxis(axis3);
        assertEquals(cube1.sha1(), cube3.sha1());
    }

    @Test
    public void testDeltaDescriptionCellValue()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        List<String> delta = cube.getDeltaDescription(cube2);
        assertEquals(0, delta.size());

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", 48);
        cube2.setCell(2, coord);
        delta = cube2.getDeltaDescription(cube);
        assertEquals(1, delta.size());
        assertTrue(delta.get(0).toLowerCase().contains("cell changed"));
        assertTrue(delta.get(0).toLowerCase().contains("gender"));
        assertTrue(delta.get(0).toLowerCase().contains("male"));
        assertTrue(delta.get(0).toLowerCase().contains("age"));
        assertTrue(delta.get(0).toLowerCase().contains("30"));
        assertTrue(delta.get(0).toLowerCase().contains("from"));
        assertTrue(delta.get(0).toLowerCase().contains("1"));
        assertTrue(delta.get(0).toLowerCase().contains("to"));
        assertTrue(delta.get(0).toLowerCase().contains("2"));

        coord.put("gender", "male");
        coord.put("age", 84);
        cube2.setCell(3.1, coord);
        delta = cube2.getDeltaDescription(cube);
        assertEquals(2, delta.size());
        assertTrue(delta.get(1).toLowerCase().contains("cell changed"));
        assertTrue(delta.get(1).toLowerCase().contains("gender"));
        assertTrue(delta.get(1).toLowerCase().contains("male"));
        assertTrue(delta.get(1).toLowerCase().contains("age"));
        assertTrue(delta.get(1).toLowerCase().contains("default col"));
        assertTrue(delta.get(1).toLowerCase().contains("from"));
        assertTrue(delta.get(1).toLowerCase().contains("1"));
        assertTrue(delta.get(1).toLowerCase().contains("to"));
        assertTrue(delta.get(1).toLowerCase().contains("3.1"));
    }

    @Test
    public void testDeltaDescriptionCubeName()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.name = "funkey";
        List<String> delta = cube2.getDeltaDescription(cube);
        assertEquals(1, delta.size());
        assertTrue(delta.get(0).toLowerCase().contains("name changed"));
        assertTrue(delta.get(0).toLowerCase().contains("from"));
        assertTrue(delta.get(0).toLowerCase().contains("delta"));
        assertTrue(delta.get(0).toLowerCase().contains("to"));
        assertTrue(delta.get(0).toLowerCase().contains("funkey"));
    }

    @Test
    public void testDeltaDescriptionCubeMetaProp()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.setMetaProperty("foo", "bar");
        List<String> delta = cube2.getDeltaDescription(cube);
        assertEquals(1, delta.size());
        assertTrue(delta.get(0).toLowerCase().contains("meta"));
        assertTrue(delta.get(0).toLowerCase().contains("prop"));
        assertTrue(delta.get(0).toLowerCase().contains("changed"));
        assertTrue(delta.get(0).toLowerCase().contains("from"));
        assertTrue(delta.get(0).toLowerCase().contains("foo"));
        assertTrue(delta.get(0).toLowerCase().contains(" to"));
        assertTrue(delta.get(0).toLowerCase().contains("bar"));
    }

    @Test
    public void testDeltaDescriptionDimMismatchAdded()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.addAxis(getStatesAxis());
        List<String> delta = cube2.getDeltaDescription(cube);
        assertEquals(1, delta.size());
        assertTrue(delta.get(0).toLowerCase().contains("new"));
        assertTrue(delta.get(0).toLowerCase().contains("axis"));
        assertTrue(delta.get(0).toLowerCase().contains("state"));
    }

    @Test
    public void testDeltaDescriptionDimMismatchRemoved()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.deleteAxis("gender");
        List<String> delta = cube2.getDeltaDescription(cube);
        System.out.println("delta = " + delta);
        assertEquals(1, delta.size());
        assertTrue(delta.get(0).toLowerCase().contains("removed"));
        assertTrue(delta.get(0).toLowerCase().contains("axis"));
        assertTrue(delta.get(0).toLowerCase().contains("gender"));
    }

    @Test
    public void testDeltaDescriptionDimMismatchAddRemove()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.deleteAxis("gender");
        cube2.addAxis(getStatesAxis());
        List<String> delta = cube2.getDeltaDescription(cube);
        assertEquals(2, delta.size());

        assertTrue(delta.get(0).toLowerCase().contains("new"));
        assertTrue(delta.get(0).toLowerCase().contains("axis"));
        assertTrue(delta.get(0).toLowerCase().contains("state"));

        assertTrue(delta.get(1).toLowerCase().contains("removed"));
        assertTrue(delta.get(1).toLowerCase().contains("axis"));
        assertTrue(delta.get(1).toLowerCase().contains("gender"));
    }

    @Test
    public void testDeltaDescriptionColumnAdded()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.addColumn("age", new Range(55, 70));
        List<String> delta = cube2.getDeltaDescription(cube);
        assertEquals(1, delta.size());

        assertTrue(delta.get(0).toLowerCase().contains("column"));
        assertTrue(delta.get(0).toLowerCase().contains("55"));
        assertTrue(delta.get(0).toLowerCase().contains("70"));
        assertTrue(delta.get(0).toLowerCase().contains("added"));
    }

    @Test
    public void testDeltaDescriptionColumnDeleted()
    {
        NCube cube = NCubeManager.getNCubeFromResource("delta.json");
        NCube cube2 = NCubeManager.getNCubeFromResource("delta.json");
        cube2.deleteColumn("gender", "male");
        List<String> delta = cube2.getDeltaDescription(cube);
        System.out.println("delta = " + delta);
        assertEquals(1, delta.size());

        assertTrue(delta.get(0).toLowerCase().contains("column"));
        assertTrue(delta.get(0).toLowerCase().contains("male"));
        assertTrue(delta.get(0).toLowerCase().contains("removed"));
    }

    // ---------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------

    private void simpleJsonCompare(String name)
    {
        NCube<?> ncube = NCubeManager.getNCubeFromResource(name);
        int h1 = ncube.hashCode();
        NCube dupe = ncube.duplicate(ncube.getName());
        int h2 = dupe.hashCode();
        assertEquals(ncube, dupe);
        assertEquals(h1, h2);

        // Verify that all Axis and Column IDs are different
        for (Axis axis : ncube.getAxes())
        {
            Axis dupeAxis = dupe.getAxis(axis.getName());
            assertEquals(axis.getId(), dupeAxis.getId());

            Iterator<Column> iThisCol = axis.getColumns().iterator();
            Iterator<Column> iThatCol = dupeAxis.getColumns().iterator();
            while (iThisCol.hasNext())
            {
                Column thisCol = iThisCol.next();
                Column thatCol = iThatCol.next();
                assertEquals(thisCol.getId(), thatCol.getId());
            }
        }
    }

    static Axis getStatesAxis()
    {
        Axis states = new Axis("State", AxisType.DISCRETE, AxisValueType.STRING, false);
        states.addColumn("AL");
        states.addColumn("AK");
        states.addColumn("AZ");
        states.addColumn("AR");
        states.addColumn("CA");
        states.addColumn("CO");
        states.addColumn("CT");
        states.addColumn("DE");
        states.addColumn("FL");
        states.addColumn("GA");
        states.addColumn("HI");
        states.addColumn("ID");
        states.addColumn("IL");
        states.addColumn("IN");
        states.addColumn("IA");
        states.addColumn("KS");
        states.addColumn("KY");
        states.addColumn("LA");
        states.addColumn("ME");
        states.addColumn("MD");
        states.addColumn("MA");
        states.addColumn("MI");
        states.addColumn("MN");
        states.addColumn("MS");
        states.addColumn("MO");
        states.addColumn("MT");
        states.addColumn("NE");
        states.addColumn("NV");
        states.addColumn("NH");
        states.addColumn("NJ");
        states.addColumn("NM");
        states.addColumn("NY");
        states.addColumn("NC");
        states.addColumn("ND");
        states.addColumn("OH");
        states.addColumn("OK");
        states.addColumn("OR");
        states.addColumn("PA");
        states.addColumn("RI");
        states.addColumn("SC");
        states.addColumn("SD");
        states.addColumn("TN");
        states.addColumn("TX");
        states.addColumn("UT");
        states.addColumn("VT");
        states.addColumn("VA");
        states.addColumn("WA");
        states.addColumn("WI");
        states.addColumn("WV");
        states.addColumn("WY");
        return states;
    }

    static Axis getProvincesAxis()
    {
        Axis provinces = new Axis("Province", AxisType.DISCRETE, AxisValueType.STRING, false);
        provinces.addColumn("Quebec");
        provinces.addColumn("New Brunswick");
        provinces.addColumn("Nova Scotia");
        provinces.addColumn("Ontario");
        provinces.addColumn("Manitoba");
        provinces.addColumn("Saskatchewan");
        provinces.addColumn("Alberta");
        provinces.addColumn("British Columbia");
        provinces.addColumn("Yukon");
        provinces.addColumn("Northwest Territories");
        provinces.addColumn("Nunavut");
        provinces.addColumn("Newfoundland");
        return provinces;
    }

    static Axis getContinentAxis()
    {
        Axis continent = new Axis("Continent", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        continent.addColumn("Africa");
        continent.addColumn("Antarctica");
        continent.addColumn("Asia");
        continent.addColumn("Australia");
        continent.addColumn("Europe");
        continent.addColumn("North America");
        continent.addColumn("South America");
        return continent;
    }

    static Axis getDecimalRangeAxis(boolean defCol)
    {
        Axis axis = new Axis("bigD", AxisType.RANGE, AxisValueType.BIG_DECIMAL, defCol);
        axis.addColumn(new Range(-10.0, 10.0));
        axis.addColumn(new Range("20.0", "30.0"));
        axis.addColumn(new Range((byte) 100, (short) 1000));
        axis.addColumn(new Range(10000, 100000L));
        axis.addColumn(new Range(100000L, 9900000L));
        return axis;
    }

    static Axis getDoubleRangeAxis(boolean defCol)
    {
        Axis axis = new Axis("doubleRange", AxisType.RANGE, AxisValueType.DOUBLE, defCol);
        axis.addColumn(new Range(-10.0, 10.0));
        axis.addColumn(new Range("20.0", "30.0"));
        axis.addColumn(new Range((byte) 100, (short) 1000));
        axis.addColumn(new Range(10000, 100000L));
        axis.addColumn(new Range(100000L, 9900000L));
        return axis;
    }

    static Axis getLongRangeAxis(boolean defCol)
    {
        Axis axis = new Axis("longRange", AxisType.RANGE, AxisValueType.LONG, defCol);
        axis.addColumn(new Range(-10.0, 10.0));
        axis.addColumn(new Range("20", "30"));
        axis.addColumn(new Range((byte) 100, (short) 1000));
        axis.addColumn(new Range(10000, 100000L));
        axis.addColumn(new Range(100000L, 9900000L));
        return axis;
    }

    static Axis getDateRangeAxis(boolean defCol)
    {
        Axis axis = new Axis("dateRange", AxisType.RANGE, AxisValueType.DATE, defCol);
        Calendar cal = Calendar.getInstance();
        cal.set(1990, 5, 10, 13, 5, 25);
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2000, 0, 1, 0, 0, 0);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2002, 11, 17, 0, 0, 0);
        Calendar cal3 = Calendar.getInstance();
        cal3.set(2008, 11, 24, 0, 0, 0);
        Calendar cal4 = Calendar.getInstance();
        cal4.set(2010, 0, 1, 12, 0, 0);
        Calendar cal5 = Calendar.getInstance();
        cal5.set(2014, 7, 1, 12, 59, 59);

        axis.addColumn(new Range(cal, cal1.getTime()));
        axis.addColumn(new Range(cal1, cal2.getTime()));
        axis.addColumn(new Range(cal2, cal3));
        axis.addColumn(new Range(cal4, cal5));
        return axis;
    }

    static Axis getLongDaysOfWeekAxis()
    {
        Axis axis = new Axis("Days", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("Monday");
        axis.addColumn("Tuesday");
        axis.addColumn("Wednesday");
        axis.addColumn("Thursday");
        axis.addColumn("Friday");
        axis.addColumn("Saturday");
        axis.addColumn("Sunday");
        return axis;
    }

    static Axis getShortDaysOfWeekAxis()
    {
        Axis axis = new Axis("Days", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("Mon");
        axis.addColumn("Tue");
        axis.addColumn("Wed");
        axis.addColumn("Thu");
        axis.addColumn("Fri");
        axis.addColumn("Sat");
        axis.addColumn("Sun");
        return axis;
    }

    static Axis getLongMonthsOfYear()
    {
        Axis axis = new Axis("Months", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("Janurary");
        axis.addColumn("February");
        axis.addColumn("March");
        axis.addColumn("April");
        axis.addColumn("May");
        axis.addColumn("June");
        axis.addColumn("July");
        axis.addColumn("August");
        axis.addColumn("September");
        axis.addColumn("October");
        axis.addColumn("November");
        axis.addColumn("December");
        return axis;
    }

    static Axis getShortMonthsOfYear()
    {
        Axis axis = new Axis("Months", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("Jan");
        axis.addColumn("Feb");
        axis.addColumn("Mar");
        axis.addColumn("Apr");
        axis.addColumn("May");
        axis.addColumn("Jun");
        axis.addColumn("Jul");
        axis.addColumn("Aug");
        axis.addColumn("Sep");
        axis.addColumn("Oct");
        axis.addColumn("Nov");
        axis.addColumn("Dec");
        return axis;
    }

    static Axis getGenderAxis(boolean defCol)
    {
        Axis axis = new Axis("Gender", AxisType.DISCRETE, AxisValueType.STRING, defCol);
        axis.addColumn("Male");
        axis.addColumn("Female");
        return axis;
    }

    static Axis getFullGenderAxis()
    {
        Axis axis = new Axis("Gender", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis.addColumn("Male");
        axis.addColumn("Female");
        axis.addColumn("Trans Female->Male");
        axis.addColumn("Trans Male->Female");
        axis.addColumn("Hermaphrodite");
        return axis;
    }

    static Axis getEvenAxis(boolean defCol)
    {
        Axis axis = new Axis("Even", AxisType.DISCRETE, AxisValueType.LONG, defCol);
        axis.addColumn(0L);
        axis.addColumn(2L);
        axis.addColumn(4L);
        axis.addColumn(6L);
        axis.addColumn(8L);
        axis.addColumn(10L);
        return axis;
    }

    static Axis getOddAxis(boolean defCol)
    {
        Axis axis = new Axis("Odd", AxisType.DISCRETE, AxisValueType.LONG, defCol);
        axis.addColumn(1L);
        axis.addColumn(3L);
        axis.addColumn(5L);
        axis.addColumn(7L);
        axis.addColumn(9L);
        return axis;
    }

    static NCube getTestNCube2D(boolean defCol)
    {
        NCube<Double> ncube = new NCube<>("test.Age-Gender");
        Axis axis1 = getGenderAxis(defCol);

        Axis axis2 = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, defCol);
        axis2.addColumn(new Range(0, 18));
        axis2.addColumn(new Range(18, 30));
        axis2.addColumn(new Range(30, 40));
        axis2.addColumn(new Range(40, 65));
        axis2.addColumn(new Range(65, 80));

        ncube.addAxis(axis1);
        ncube.addAxis(axis2);

        return ncube;
    }

    static NCube getSysClassPathCube()
    {
        return NCubeManager.getNCubeFromResource("sys.classpath.tests.json");
    }

    static NCube getTestNCube3D_Boolean()
    {
        NCube<Boolean> ncube = new NCube<>("test.ValidTrailorConfigs");
        Axis axis1 = new Axis("Trailers", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY);
        axis1.addColumn("S1A");
        axis1.addColumn("M1A");
        axis1.addColumn("L1A");
        axis1.addColumn("S2A");
        axis1.addColumn("M2A");
        axis1.addColumn("L2A");
        axis1.addColumn("M3A");
        axis1.addColumn("L3A");
        Axis axis2 = new Axis("Vehicles", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis2.addColumn("car");
        axis2.addColumn("small truck");
        axis2.addColumn("med truck");
        axis2.addColumn("large truck");
        axis2.addColumn("van");
        axis2.addColumn("motorcycle");
        axis2.addColumn("limousine");
        axis2.addColumn("tractor");
        axis2.addColumn("golf cart");
        Axis axis3 = new Axis("BU", AxisType.DISCRETE, AxisValueType.STRING, false);
        axis3.addColumn("Agri");
        axis3.addColumn("SHS");

        ncube.addAxis(axis1);
        ncube.addAxis(axis2);
        ncube.addAxis(axis3);

        return ncube;
    }

    static int countMatches(String s, String pattern)
    {
        int lastIndex = 0;
        int count = 0;

        while (lastIndex != -1)
        {
            lastIndex = s.indexOf(pattern, lastIndex);

            if (lastIndex != -1)
            {
                count++;
                lastIndex += pattern.length();
            }
        }
        return count;
    }

    private static void println(Object... args)
    {
        if (_debug)
        {
            for (Object arg : args)
            {
                System.out.println(arg);
            }
        }
    }
}
