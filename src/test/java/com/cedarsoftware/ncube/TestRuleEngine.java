package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NCube RuleEngine Tests
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
public class TestRuleEngine
{
    @After
    public void tearDown() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    @Test
    public void testLoadRuleFromUrl() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("rule-column-loaded-with-url.json");

        Map coord = new HashMap();
        coord.put("age", 17);
        coord.put("weight", 99);
        Map output = new HashMap();

        n1.getCells(coord, output);

        assertEquals("light-weight", output.get("weight"));
        assertEquals("young", output.get("age"));

        List<Map<String, Object>> list = n1.getCoordinatesForCells();
        int weight = 0;
        int age = 0;
        for (Map<String, Object> pt : list)
        {
            if (pt.containsKey("age"))
            {
                age++;
            }
            if (pt.containsKey("weight"))
            {
                weight++;
            }
        }

        assertEquals(3, age);
        assertEquals(9, weight);
    }

    @Test
    public void testContainsCellRuleAxis() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("multiRule.json");
        Map coord = new HashMap();
        coord.put("age", 17);
        coord.put("weight", 99);
        boolean b = n1.containsCell(coord, false);
        assertTrue(b);
        b = n1.containsCell(coord, true);
        assertTrue(b);

        List<Map<String, Object>> list = n1.getCoordinatesForCells();
        int weight = 0;
        int age = 0;
        for (Map<String, Object> pt : list)
        {
            if (pt.containsKey("age"))
            {
                age++;
            }
            if (pt.containsKey("weight"))
            {
                weight++;
            }
        }

        assertEquals(9, age);
        assertEquals(9, weight);
    }

    // This test also tests ID-based ncube's specified in simple JSON format
    @Test
    public void testRuleCube() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("expressionAxis.json");
        Axis cond = ncube.getAxis("condition");
        assertTrue(cond.getColumns().get(0).getId() != 1);
        Axis state = ncube.getAxis("state");
        assertTrue(state.getColumns().get(0).getId() != 10);

        Map coord = new HashMap();
        coord.put("vehiclePrice", 5000.0);
        coord.put("driverAge", 22);
        coord.put("gender", "male");
        coord.put("vehicleCylinders", 8);
        coord.put("state", "TX");
        Map output = new HashMap();
        Map<Map<String, Column>, ?> steps = ncube.getCells(coord, output);
        assertEquals(new BigDecimal("119.0"), output.get("premium"));
        assertTrue(steps.size() == 4);
    }

    // This test ensures that identical expressions result in a single dynamic Groovy class being generated for them.
    @Test
    public void testDuplicateExpression() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("duplicateExpression.json");

        Map coord = new HashMap();
        coord.put("vehiclePrice", 5000.0);
        coord.put("driverAge", 22);
        coord.put("gender", "male");
        coord.put("vehicleCylinders", 8);
        Map output = new HashMap();
        Map<Map<String, Column>, ?> steps = ncube.getCells(coord, output);
        assertEquals(new BigDecimal("119.0"), output.get("premium"));
        assertTrue(steps.size() == 4);
    }

    @Test
    public void testCONDITIONnoSort()
    {
        try
        {
            new Axis("sorted", AxisType.RULE, AxisValueType.EXPRESSION, true, Axis.SORTED);
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try
        {
            new Axis("sorted", AxisType.RULE, AxisValueType.BIG_DECIMAL, true, Axis.DISPLAY);
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        Axis axis = new Axis("sorted", AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY);
        try
        {
            axis.addColumn(10);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        axis = new Axis("sorted", AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY);
        try
        {
            axis.findColumn(null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testRequiredScopeRuleAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("expressionAxis.json");

        Set<String> requiredScope = ncube.getRequiredScope();
        assertTrue(requiredScope.size() == 6);
        assertTrue(requiredScope.contains("driverAge"));
        assertTrue(requiredScope.contains("gender"));
        assertTrue(requiredScope.contains("state"));
        assertTrue(requiredScope.contains("stop"));
        assertTrue(requiredScope.contains("vehicleCylinders"));
        assertTrue(requiredScope.contains("vehiclePrice"));
        Object x = ncube.getRequiredScope();
        assertEquals(requiredScope, x);
        assertTrue(requiredScope != x);

        Set scopeValues = ncube.getRequiredScope();
        assertTrue(scopeValues.size() == 6);
    }

    @Test
    public void testCubeRefFromRuleAxis() throws Exception
    {
        NCube ncube1 = NCubeManager.getNCubeFromResource("testCube5.json");
        Set reqScope = ncube1.getRequiredScope();
        assertTrue(reqScope.size() == 1);
        assertTrue(reqScope.contains("Age"));

        NCube ncube2 = NCubeManager.getNCubeFromResource("expressionAxis2.json");
        reqScope = ncube2.getRequiredScope();
        assertTrue(reqScope.size() == 2);
        assertTrue(reqScope.contains("Age"));
        assertTrue(reqScope.contains("state"));

        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");
        Map output = new LinkedHashMap();
        ncube2.getCells(coord, output);
        assertEquals(new BigDecimal("5.0"), output.get("premium"));

        coord.put("state", "TX");
        output.clear();
        ncube2.getCells(coord, output);
        assertEquals(new BigDecimal("-5.0"), output.get("premium"));

        coord.clear();
        coord.put("state", "OH");
        coord.put("Age", 23);
        output.clear();
        ncube2.getCells(coord, output);
        assertEquals(new BigDecimal("1.0"), output.get("premium"));
    }

    @Test
    public void testMultipleRuleAxisBindings() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("multiRule.json");
        Map coord = new HashMap();
        coord.put("age", 10);
        coord.put("weight", 50);
        Map output = new HashMap();
        ncube.getCells(coord, output);

        assertEquals(output.get("weight"), "medium-weight");
        assertEquals(output.get("age"), "adult");
        Map ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(4L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));

        output.clear();
        coord.put("age", 10);
        coord.put("weight", 150);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "medium-weight");
        assertEquals(output.get("age"), "adult");
        ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(2L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));

        output.clear();
        coord.put("age", 35);
        coord.put("weight", 150);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "medium-weight");
        assertEquals(output.get("age"), "adult");
        ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));

        output.clear();
        coord.put("age", 42);
        coord.put("weight", 205);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "heavy-weight");
        assertEquals(output.get("age"), "middle-aged");
        ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));
    }

    @Test
    public void testMultipleRuleAxisBindingsOKInMultiDim() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("multiRule2.json");
        Map coord = new HashMap();
        coord.put("age", 10);
        coord.put("weight", 60);
        Map output = new HashMap();
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "light-weight");

        // The age is 'adult' because two rules are matching on the age axis (intentional rule error)
        // This test illustrates that I can match 2 or more rules on one rule axis, 1 on a 2nd rule
        // axis, and it does not violate 'ruleMode'.
        assertEquals(output.get("age"), "adult");
    }

    @Test
    public void testRuleStopCondition() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("multiRuleHalt.json");
        Map coord = new HashMap();
        coord.put("age", 10);
        coord.put("weight", 60);
        Map output = new HashMap();
        ncube.getCells(coord, output);
        assertEquals(output.get("age"), "young");
        assertEquals(output.get("weight"), "light-weight");
        Map ruleOut = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertFalse((Boolean) ruleOut.get(RuleMetaKeys.RULE_STOP));

        coord.put("age", 25);
        coord.put("weight", 60);
        output.clear();
        ncube.getCells(coord, output);
        ruleOut = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertTrue((Boolean) ruleOut.get(RuleMetaKeys.RULE_STOP));

        coord.put("age", 45);
        coord.put("weight", 60);
        output.clear();
        ncube.getCells(coord, output);
        assertEquals(output.get("age"), "middle-aged");
        assertEquals(output.get("weight"), "light-weight");
        ruleOut = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertFalse((Boolean) ruleOut.get(RuleMetaKeys.RULE_STOP));
    }

    @Test
    public void testDefaultColumnOnRuleAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("ruleWithDefault.json");

        Map output = new HashMap();
        Map coord = new HashMap();

        coord.put("state", "OH");
        coord.put("age", 18);
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "OH 18");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "TX");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "TX 18");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "GA");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "GA 18");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "AZ");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "default 18");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "OH");
        coord.put("age", 50);
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "OH 50");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "TX");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "TX 50");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "GA");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "GA 50");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "AZ");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "default 50");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "OH");
        coord.put("age", 85);
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "OH 85");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "TX");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "TX 85");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "GA");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "GA 85");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "AZ");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "default 85");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "OH");
        coord.put("age", 100);
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "OH default");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "TX");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "TX default");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "GA");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "GA default");
        assertTrue(ncube.containsCell(coord));

        coord.put("state", "AZ");
        ncube.getCell(coord, output);
        assertEquals(output.get("text"), "default default");
        assertTrue(ncube.containsCell(coord));
    }

    @Test
    public void testRuleAxisWithNoMatchAndNoDefault()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("ruleNoMatch.json");

        Map coord = new HashMap();
        Map output = new HashMap();
        coord.put("age", 85);
        ncube.getCells(coord, output);
        assertEquals(1, output.size());
        Map ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(0L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));

        coord.put("age", 22);
        ncube.getCells(coord, output);
        assertTrue(output.containsKey("adult"));
        assertTrue(output.containsKey("old"));
        ruleExecInfo = (Map) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(2L, ruleExecInfo.get(RuleMetaKeys.NUM_RESOLVED_CELLS));
    }

    @Test
    public void testContainsCellValueRule()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCellRule.json");

        Map coord = new HashMap();
        coord.put("gender", "Male");
        assertTrue(ncube.containsCell(coord));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCell(coord));

        coord.put("gender", "Male");
        assertFalse(ncube.containsCellValue(coord, true));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCellValue(coord, true));

        coord.put("gender", "Male");
        assertFalse(ncube.containsCellValue(coord, false));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCellValue(coord, false));

        coord.put("gender", "GI Joe");
        assertFalse(ncube.containsCell(coord));
        assertFalse(ncube.containsCellValue(coord, false));

        ncube.setDefaultCellValue(null);

        coord.put("gender", "Male");
        assertFalse(ncube.containsCell(coord));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCell(coord));

        coord.put("gender", "Male");
        assertFalse(ncube.containsCellValue(coord, true));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCellValue(coord, true));

        coord.put("gender", "Male");
        assertFalse(ncube.containsCellValue(coord, false));
        coord.put("gender", "Female");
        assertTrue(ncube.containsCellValue(coord, false));

        coord.put("gender", "GI Joe");
        assertFalse(ncube.containsCell(coord));

        assertFalse(ncube.containsCellValue(coord, false));
    }

    @Test
    public void testOneRuleSetCallsAnotherRuleSet() throws Exception
    {
        NCubeManager.getNCubeFromResource("ruleSet2.json");
        NCube ncube = NCubeManager.getNCubeFromResource("ruleSet1.json");
        Map input = new HashMap();
        input.put("age", 10);
        Map output = new HashMap();
        ncube.getCells(input, output);
        assertEquals(new BigDecimal("1.0"), output.get("total"));

        input.put("age", 48);
        ncube.getCells(input, output);
        assertEquals(new BigDecimal("8.560"), output.get("total"));

        input.put("age", 84);
        ncube.getCells(input, output);
        assertEquals(new BigDecimal("5.150"), output.get("total"));
    }

    @Test
    public void testRuleFalseValues() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("ruleFalseValues.json");
        Map input = new HashMap();
        input.put("state", "OH");
        Map output = new HashMap();
        ncube.getCells(input, output);
        Map stats = (Map) output.get("_rule");
        assertEquals(1, ((Map) stats.get(RuleMetaKeys.RULES_EXECUTED)).size());

        // Groovy style false
        assertFalse(NCube.didRuleFire(null));
        assertFalse(NCube.didRuleFire(false));
        assertFalse(NCube.didRuleFire(Boolean.FALSE));
        assertFalse(NCube.didRuleFire(new Boolean(false)));
        assertFalse(NCube.didRuleFire((byte) 0));
        assertFalse(NCube.didRuleFire((short) 0));
        assertFalse(NCube.didRuleFire((int) 0));
        assertFalse(NCube.didRuleFire((long) 0));
        assertFalse(NCube.didRuleFire(0f));
        assertFalse(NCube.didRuleFire(0d));
        assertFalse(NCube.didRuleFire(BigInteger.ZERO));
        assertFalse(NCube.didRuleFire(BigDecimal.ZERO));
        assertFalse(NCube.didRuleFire(""));
        assertFalse(NCube.didRuleFire(new HashMap()));
        assertFalse(NCube.didRuleFire(new HashMap().keySet().iterator()));
        assertFalse(NCube.didRuleFire(new ArrayList()));
        assertFalse(NCube.didRuleFire(new ArrayList().iterator()));
        assertFalse(NCube.didRuleFire(new Vector().elements()));

        // Groovy style true
        assertTrue(NCube.didRuleFire(new Date()));
        assertTrue(NCube.didRuleFire(true));
        assertTrue(NCube.didRuleFire(Boolean.TRUE));
        assertTrue(NCube.didRuleFire(new Boolean(true)));
        assertTrue(NCube.didRuleFire((byte) 1));
        assertTrue(NCube.didRuleFire((short) 1));
        assertTrue(NCube.didRuleFire((int) 1));
        assertTrue(NCube.didRuleFire((long) 1));
        assertTrue(NCube.didRuleFire(1f));
        assertTrue(NCube.didRuleFire(1d));
        assertTrue(NCube.didRuleFire(BigInteger.ONE));
        assertTrue(NCube.didRuleFire(BigDecimal.ONE));
        assertTrue(NCube.didRuleFire("Yo"));
        Map map = new HashMap();
        map.put("foo","bar");
        assertTrue(NCube.didRuleFire(map));
        assertTrue(NCube.didRuleFire(map.keySet().iterator()));
        List list = new ArrayList();
        list.add(new Date());
        assertTrue(NCube.didRuleFire(list));
        assertTrue(NCube.didRuleFire(list.iterator()));
        Vector v = new Vector();
        v.add(9);
        assertTrue(NCube.didRuleFire(v.elements()));
    }
}
