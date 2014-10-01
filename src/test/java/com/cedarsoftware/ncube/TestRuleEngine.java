package com.cedarsoftware.ncube;

import com.cedarsoftware.util.CaseInsensitiveMap;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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

        List<NCubeTest> list = n1.generateNCubeTests();
        int weight = 0;
        int age = 0;
        for (NCubeTest pt : list)
        {
            StringValuePair<CellInfo>[] c = pt.getCoord();

            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>("age", null)))
            {
                age++;
            }
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>("weight", null)))
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
        coord.put("condition1", "youngster");
        coord.put("condition2", "light");
        boolean b = n1.containsCell(coord);
        assertTrue(b);

        List<NCubeTest> list = n1.generateNCubeTests();
        int weight = 0;
        int age = 0;
        for (NCubeTest pt : list)
        {
            StringValuePair<CellInfo>[] c = pt.getCoord();
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>("age", null)))
            {
                age++;
            }
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>("weight", null)))
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
        Object out = ncube.getCells(coord, output);
        assertEquals(10, out);
        assertEquals(new BigDecimal("119.0"), output.get("premium"));
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
        Object out = ncube.getCells(coord, output);
        assertEquals(10, out);
        assertEquals(new BigDecimal("119.0"), output.get("premium"));
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

        Set<String> reqScope = ncube.getRequiredScope();
        assertEquals(2, reqScope.size());
        assertTrue(reqScope.contains("state"));
        assertTrue(reqScope.contains("condition"));

        Set<String> optScope = ncube.getOptionalScope();
        assertEquals(5, optScope.size());
        assertTrue(optScope.contains("driverAge"));
        assertTrue(optScope.contains("gender"));
        assertTrue(optScope.contains("stop"));
        assertTrue(optScope.contains("vehicleCylinders"));
        assertTrue(optScope.contains("vehiclePrice"));
    }

    @Test
    public void testCubeRefFromRuleAxis() throws Exception
    {
        NCube ncube1 = NCubeManager.getNCubeFromResource("testCube5.json");
        Set reqScope = ncube1.getRequiredScope();
        Set optScope = ncube1.getOptionalScope();
        assertTrue(optScope.size() == 1);
        assertTrue(optScope.contains("Age"));
        assertEquals(0, reqScope.size());

        NCube ncube2 = NCubeManager.getNCubeFromResource("expressionAxis2.json");
        reqScope = ncube2.getRequiredScope();
        assertTrue(reqScope.size() == 2);
        assertTrue(reqScope.contains("condition"));
        assertTrue(reqScope.contains("state"));
        optScope = ncube2.getOptionalScope();
        assertEquals(1, optScope.size());
        assertTrue(optScope.contains("AGE"));

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
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(4L, ruleInfo.getNumberOfRulesExecuted());

        output.clear();
        coord.put("age", 10);
        coord.put("weight", 150);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "medium-weight");
        assertEquals(output.get("age"), "adult");
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(2L, ruleInfo.getNumberOfRulesExecuted());

        output.clear();
        coord.put("age", 35);
        coord.put("weight", 150);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "medium-weight");
        assertEquals(output.get("age"), "adult");
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1L, ruleInfo.getNumberOfRulesExecuted());

        output.clear();
        coord.put("age", 42);
        coord.put("weight", 205);
        ncube.getCells(coord, output);
        assertEquals(output.get("weight"), "heavy-weight");
        assertEquals(output.get("age"), "middle-aged");
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1L, ruleInfo.getNumberOfRulesExecuted());
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
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1, ruleInfo.getNumberOfRulesExecuted());

        coord.put("age", 25);
        coord.put("weight", 60);
        output.clear();
        ncube.getCells(coord, output);
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(0, ruleInfo.getNumberOfRulesExecuted());

        coord.put("age", 45);
        coord.put("weight", 60);
        output.clear();
        ncube.getCells(coord, output);
        assertEquals(output.get("age"), "middle-aged");
        assertEquals(output.get("weight"), "light-weight");
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(1, ruleInfo.getNumberOfRulesExecuted());
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
        assertEquals(2, output.size());
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(0L, ruleInfo.getNumberOfRulesExecuted());

        coord.put("age", 22);
        ncube.getCells(coord, output);
        assertTrue(output.containsKey("adult"));
        assertTrue(output.containsKey("old"));
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(2L, ruleInfo.getNumberOfRulesExecuted());
    }

    @Test
    public void testContainsCellValueRule()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("containsCellRule.json");

        Map coord = new HashMap();
        coord.put("condition", "Male");
        assertTrue(ncube.containsCell(coord, true));
        coord.put("condition", "Female");
        assertTrue(ncube.containsCell(coord));

        try
        {
            coord.put("condition", "GI Joe");
            ncube.containsCell(coord);
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        ncube.setDefaultCellValue(null);

        coord.put("condition", "Male");
        assertFalse(ncube.containsCell(coord));
        coord.put("condition", "Female");
        assertTrue(ncube.containsCell(coord));
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
    public void testBasicJump() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("basicJump.json");
        Map input = new HashMap();
        input.put("age", 10);
        Map output = new HashMap();
        ncube.getCells(input, output);

        assertEquals("child", output.get("group"));
        assertEquals("thang", output.get("thing"));

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        assertEquals(3, ruleInfo.getNumberOfRulesExecuted());
//        System.out.println("ruleInfo.getRuleExecutionTrace() = " + ruleInfo.getRuleExecutionTrace());

        input.put("age", 48);
        ncube.getCells(input, output);
        assertEquals("adult", output.get("group"));

        input.put("age", 84);
        ncube.getCells(input, output);
        assertEquals("geezer", output.get("group"));
    }

    @Test
    public void testMultipleRuleAxesWithMoreThanOneRuleFiring() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("multiRule.json");
        Map input = new HashMap();
        input.put("age", 35);
        input.put("weight", 99);
        Map output = new HashMap();
        assertEquals("medium-weight", ncube.getCell(input, output));
    }

    @Test
    public void testRuleFalseValues() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("ruleFalseValues.json");
        Map input = new HashMap();
        input.put("state", "OH");
        Map output = new HashMap();
        ncube.getCells(input, output);
        RuleInfo ruleInfo = (RuleInfo) output.get("_rule");
        assertEquals(1, ruleInfo.getNumberOfRulesExecuted());

        // Groovy style false
        assertFalse(NCube.isTrue(null));
        assertFalse(NCube.isTrue(false));
        assertFalse(NCube.isTrue(Boolean.FALSE));
        assertFalse(NCube.isTrue(new Boolean(false)));
        assertFalse(NCube.isTrue((byte) 0));
        assertFalse(NCube.isTrue((short) 0));
        assertFalse(NCube.isTrue((int) 0));
        assertFalse(NCube.isTrue((long) 0));
        assertFalse(NCube.isTrue(0f));
        assertFalse(NCube.isTrue(0d));
        assertFalse(NCube.isTrue(BigInteger.ZERO));
        assertFalse(NCube.isTrue(BigDecimal.ZERO));
        assertFalse(NCube.isTrue(""));
        assertFalse(NCube.isTrue(new HashMap()));
        assertFalse(NCube.isTrue(new HashMap().keySet().iterator()));
        assertFalse(NCube.isTrue(new ArrayList()));
        assertFalse(NCube.isTrue(new ArrayList().iterator()));
        assertFalse(NCube.isTrue(new Vector().elements()));

        // Groovy style true
        assertTrue(NCube.isTrue(new Date()));
        assertTrue(NCube.isTrue(true));
        assertTrue(NCube.isTrue(Boolean.TRUE));
        assertTrue(NCube.isTrue(new Boolean(true)));
        assertTrue(NCube.isTrue((byte) 1));
        assertTrue(NCube.isTrue((short) 1));
        assertTrue(NCube.isTrue((int) 1));
        assertTrue(NCube.isTrue((long) 1));
        assertTrue(NCube.isTrue(1f));
        assertTrue(NCube.isTrue(1d));
        assertTrue(NCube.isTrue(BigInteger.ONE));
        assertTrue(NCube.isTrue(BigDecimal.ONE));
        assertTrue(NCube.isTrue("Yo"));
        Map map = new HashMap();
        map.put("foo","bar");
        assertTrue(NCube.isTrue(map));
        assertTrue(NCube.isTrue(map.keySet().iterator()));
        List list = new ArrayList();
        list.add(new Date());
        assertTrue(NCube.isTrue(list));
        assertTrue(NCube.isTrue(list.iterator()));
        Vector v = new Vector();
        v.add(9);
        assertTrue(NCube.isTrue(v.elements()));
    }

    @Test
    public void testJumpStart()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("basicJumpStart.json");
        Map input = new HashMap();
        input.put("letter", "e");
        Map output = new CaseInsensitiveMap();
        ncube.getCells(input, output);

        assertTrue(output.containsKey("A"));           // condition ran (condition axis was told to start at beginning - null)
        assertTrue(output.containsKey("B"));
        assertTrue(output.containsKey("C"));
        assertTrue(output.containsKey("D"));
        assertTrue(output.containsKey("E"));
        assertTrue(output.containsKey("F"));
        assertTrue(output.containsKey("G"));
        assertEquals("echo", output.get("word"));

        input.put("condition", "e");
        output = new CaseInsensitiveMap();
        ncube.getCells(input, output);

        assertFalse(output.containsKey("A"));           // condition never ran (condition axis was told to start at a)
        assertFalse(output.containsKey("B"));           // condition never ran (condition axis was told to start at a)
        assertFalse(output.containsKey("C"));           // condition never ran (condition axis was told to start at a)
        assertFalse(output.containsKey("D"));           // condition never ran (condition axis was told to start at a)
        assertTrue(output.containsKey("E"));            // condition ran
        assertTrue(output.containsKey("F"));            // condition ran
        assertTrue(output.containsKey("G"));            // condition ran
        assertEquals("echo", output.get("word"));
    }

    @Test
    public void testJumpStart2D()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("basicJumpStart2D.json");
        Map input = new HashMap();
        input.put("letter", "f");
        input.put("column", "y");
        input.put("condition", "f");
        input.put("condition2", "y");
        Map output = new CaseInsensitiveMap();
        ncube.getCells(input, output);

        assertFalse(output.containsKey("A"));       // skipped - condition never ran (condition axis was told to start at f)
        assertFalse(output.containsKey("B"));       // skipped
        assertFalse(output.containsKey("C"));       // skipped
        assertFalse(output.containsKey("D"));       // skipped
        assertFalse(output.containsKey("E"));       // skipped
        assertTrue(output.containsKey("F"));        // condition ran
        assertTrue(output.containsKey("G"));        // condition ran
        assertEquals("foxtrot", output.get("word"));

        assertFalse(output.containsKey("W"));       // skipped - condition never ran (condition2 axis was told to start at y)
        assertFalse(output.containsKey("X"));       // skipped
        assertTrue(output.containsKey("Y"));        // condition ran
        assertTrue(output.containsKey("Z"));        // condition ran
        assertEquals("y", output.get("col"));
    }

    @Test
    public void testJumpRestart()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("basicJumpRestart.json");
        Map input = new HashMap();
        input.put("letter", "e");
        Map output = new CaseInsensitiveMap();
        output.put("a", 0);
        output.put("b", 0);
        output.put("c", 0);
        output.put("d", 0);
        output.put("e", 0);
        output.put("f", 0);
        output.put("g", 0);
        output.put("word", "");
        ncube.getCells(input, output);

        assertTrue(output.containsKey("A"));           // condition ran (condition axis was told to start at beginning - null)
        assertTrue(output.containsKey("B"));
        assertTrue(output.containsKey("C"));
        assertTrue(output.containsKey("D"));
        assertTrue(output.containsKey("E"));
        assertTrue(output.containsKey("F"));
        assertTrue(output.containsKey("G"));
        assertEquals("echoecho", output.get("word"));

        assertEquals(1, output.get("a"));
        assertEquals(1, output.get("b"));
        assertEquals(1, output.get("c"));
        assertEquals(1, output.get("d"));
        assertEquals(2, output.get("e"));       // This step is run twice.
        assertEquals(1, output.get("f"));       // This step is run once (skipped the first time, then after 'e' runs a 2nd time)
        assertEquals(1, output.get("g"));       // This step is run once (skipped the first time, then after 'e' runs a 2nd time)
    }
}
