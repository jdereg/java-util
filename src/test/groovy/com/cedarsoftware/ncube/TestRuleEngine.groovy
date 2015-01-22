package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.util.CaseInsensitiveMap
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * NCube RuleEngine Tests
 *
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
public class TestRuleEngine
{
    @Before
    void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    void testLoadRuleFromUrl() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource 'rule-column-loaded-with-url.json'
        def output = [:]
        n1.getCell([age:17, weight:99], output)

        assertEquals 'light-weight', output.weight
        assertEquals 'young', output.age

        List<NCubeTest> list = n1.generateNCubeTests()
        int weight = 0
        int age = 0
        for (NCubeTest pt : list)
        {
            StringValuePair<CellInfo>[] c = pt.coord

            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>('age', null)))
            {
                age++
            }
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>('weight', null)))
            {
                weight++
            }
        }

        assert 9 == age
        assert 9 == weight
    }

    @Test
    void testContainsCellRuleAxis() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource 'multiRule.json'
        boolean b = n1.containsCell([condition1:'youngster', condition2:'light'])
        assertTrue b

        List<NCubeTest> list = n1.generateNCubeTests()

        int weight = 0
        int age = 0

        for (NCubeTest pt : list)
        {
            StringValuePair<CellInfo>[] c = pt.coord
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>('age', null)))
            {
                age++
            }
            if (Arrays.asList(c).contains(new StringValuePair<CellInfo>('weight', null)))
            {
                weight++
            }
        }

        assert age == 0
        assert weight == 0
    }

    // This test also tests ID-based ncube's specified in simple JSON format
    @Test
    void testRuleCube() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'expressionAxis.json'
        Axis cond = ncube.getAxis 'condition'
        assert cond.columns.get(0).id != 1
        Axis state = ncube.getAxis 'state'
        assert state.columns.get(0).id != 10

        Map output = [:]
        Object out = ncube.getCell([vehiclePrice:5000.0, driveAge:22, gender:'male', vehicleCylinders:8, state:'TX'], output)
        assert out == 10
        assert output.premium == 119.0
    }

    // This test also tests ID-based ncube's specified in simple JSON format
    @Test
    void testExpressionValue() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'expressionAxis.json'
        Axis cond = ncube.getAxis 'condition'
        assert cond.getColumns().get(0).id != 1
        Axis state = ncube.getAxis 'state'
        assert state.getColumns().get(0).id != 10
        assert 'foo' == state.standardizeColumnValue('foo')
    }

    // This test ensures that identical expressions result in a single dynamic Groovy class being generated for them.
    @Test
    void testDuplicateExpression() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'duplicateExpression.json'
        def output = [:]
        def out = ncube.getCell([vehiclePrice:5000.0, driveAge:22, gender:'male', vehicleCylinders:8], output)
        assert out == 10
        assert output.premium == 119.0
    }

    @Test
    void testRequiredScopeRuleAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'expressionAxis.json'

        Set<String> reqScope = ncube.requiredScope
        assert 1 == reqScope.size()
        assert reqScope.contains('state')

        Set<String> optScope = ncube.optionalScope
        assert 6 == optScope.size()
        assert optScope.contains('condition')
        assert optScope.contains('driverAge')
        assert optScope.contains('gender')
        assert optScope.contains('stop')
        assert optScope.contains('vehicleCylinders')
        assert optScope.contains('vehiclePrice')
    }

    @Test
    void testCubeRefFromRuleAxis() throws Exception
    {
        NCube ncube1 = NCubeManager.getNCubeFromResource 'testCube5.json'
        Set reqScope = ncube1.requiredScope
        Set optScope = ncube1.optionalScope
        assert optScope.size() == 1
        assert optScope.contains('Age')
        assert 0 == reqScope.size()

        NCube ncube2 = NCubeManager.getNCubeFromResource 'expressionAxis2.json'
        reqScope = ncube2.requiredScope
        assert reqScope.size() == 1
        assert reqScope.contains('state')
        optScope = ncube2.optionalScope
        assert 2 == optScope.size()
        assert optScope.contains('condition')
        assert optScope.contains('AGE')

        def coord = [age:18,state:'OH'];
        def output = [:]
        ncube2.getCell(coord, output)
        assert 5.0 == output.premium

        coord.state = 'TX'
        output.clear()
        ncube2.getCell(coord, output)
        assert -5.0 == output.premium

        output.clear()
        ncube2.getCell([state:'OH',Age:23], output)
        assert 1.0 == output.premium
    }

    @Test
    void testMultipleRuleAxisBindings() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'multiRule.json'
        def output = [:]
        ncube.getCell([age:10, weight:50], output)

        assert output.weight == 'medium-weight'
        assert output.age == 'adult'
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 4L == ruleInfo.getNumberOfRulesExecuted()

        output.clear()
        ncube.getCell([age:10, weight:150], output)
        assert output.weight == 'medium-weight'
        assert output.age == 'adult'
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 2L == ruleInfo.getNumberOfRulesExecuted()

        output.clear()
        ncube.getCell([age:35, weight:150], output)
        assert output.weight == 'medium-weight'
        assert output.age == 'adult'
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 1L == ruleInfo.getNumberOfRulesExecuted()

        output.clear()
        ncube.getCell([age:42, weight:205], output)
        assert output.weight == 'heavy-weight'
        assert output.age == 'middle-aged'
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 1L == ruleInfo.getNumberOfRulesExecuted()
    }

    @Test
    void testMultipleRuleAxisBindingsOKInMultiDim() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'multiRule2.json'
        def output = [:]
        ncube.getCell([age:10, weight:60], output)
        assert output.weight == 'light-weight'

        // The age is 'adult' because two rules are matching on the age axis (intentional rule error)
        // This test illustrates that I can match 2 or more rules on one rule axis, 1 on a 2nd rule
        // axis.
        assert output.age == 'adult'
    }

    @Test
    void testRuleStopCondition() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'multiRuleHalt.json'
        def output = [:]
        ncube.getCell([age:10, weight:60], output)
        assert output.age == 'young'
        assert output.weight == 'light-weight'
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 1 == ruleInfo.getNumberOfRulesExecuted();
        assertFalse ruleInfo.wasRuleStopThrown()

        output.clear()
        ncube.getCell([age:25,weight:60], output)
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 0 == ruleInfo.getNumberOfRulesExecuted()
        assert ruleInfo.wasRuleStopThrown()

        output.clear()
        ncube.getCell([age:45, weight:60], output)
        assert output.age== 'middle-aged'
        assert output.weight == 'light-weight'
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 1 == ruleInfo.getNumberOfRulesExecuted()
        assertFalse ruleInfo.wasRuleStopThrown()
    }

    @Test
    void testDefaultColumnOnRuleAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleWithDefault.json'

        def output = [:]
        def coord = [:]

        coord.state = 'OH'
        coord.age = 18
        ncube.getCell coord, output
        assert output.text == 'OH 18'
        assert ncube.containsCell(coord)

        coord.state = 'TX'
        ncube.getCell coord, output
        assert output.text == 'TX 18'
        assert ncube.containsCell(coord)

        coord.state = 'GA'
        ncube.getCell coord, output
        assert output.text == 'GA 18'
        assert ncube.containsCell(coord)

        coord.state = 'AZ'
        ncube.getCell coord, output
        assert output.text == 'default 18'
        assert ncube.containsCell(coord)

        coord.state = 'OH'
        coord.age = 50
        ncube.getCell coord, output
        assert output.text == 'OH 50'
        assert ncube.containsCell(coord)

        coord.state = 'TX'
        ncube.getCell coord, output
        assert output.text == 'TX 50'
        assert ncube.containsCell(coord)

        coord.state = 'GA'
        ncube.getCell coord, output
        assert output.text == 'GA 50'
        assert ncube.containsCell(coord)

        coord.state = 'AZ'
        ncube.getCell coord, output
        assert output.text == 'default 50'
        assert ncube.containsCell(coord)

        coord.state = 'OH'
        coord.age = 85
        ncube.getCell coord, output
        assert output.text == 'OH 85'
        assert ncube.containsCell(coord)

        coord.state = 'TX'
        ncube.getCell coord, output
        assert output.text == 'TX 85'
        assert ncube.containsCell(coord)

        coord.state = 'GA'
        ncube.getCell coord, output
        assert output.text == 'GA 85'
        assert ncube.containsCell(coord)

        coord.state = 'AZ'
        ncube.getCell coord, output
        assert output.text == 'default 85'
        assert ncube.containsCell(coord)

        coord.state = 'OH'
        coord.age = 100
        ncube.getCell coord, output
        assert output.text == 'OH default'
        assert ncube.containsCell(coord)

        coord.state = 'TX'
        ncube.getCell coord, output
        assert output.text == 'TX default'
        assert ncube.containsCell(coord)

        coord.state = 'GA'
        ncube.getCell coord, output
        assert output.text == 'GA default'
        assert ncube.containsCell(coord)

        coord.state = 'AZ'
        ncube.getCell coord, output
        assert output.text == 'default default'
        assert ncube.containsCell(coord)
    }

    @Test
    void testRuleAxisWithNoMatchAndNoDefault()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleNoMatch.json'

        def coord = [age:85]
        def output = [:]
        try
        {
            ncube.getCell(coord, output)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            ruleAxisDidNotBind(e)
        }
        assert 1 == output.size()
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 0L == ruleInfo.getNumberOfRulesExecuted()

        coord.age = 22
        ncube.getCell(coord, output)
        assert output.containsKey('adult')
        assert output.containsKey('old')
        ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 2L == ruleInfo.getNumberOfRulesExecuted()
    }

    private void ruleAxisDidNotBind(CoordinateNotFoundException e)
    {
        assert e.message.toLowerCase().contains("no condition")
        assert e.message.toLowerCase().contains("fired")
        assert e.message.toLowerCase().contains("no default")
        assert e.message.toLowerCase().contains("rule axis")
    }

    @Test
    void testContainsCellValueRule()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'containsCellRule.json'

        def coord = [condition:'Male']
        assert ncube.containsCell(coord, true)
        try
        {
            ncube.getCell(coord)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            ruleAxisDidNotBind(e)
        }

        coord.condition = 'Female'
        assert ncube.containsCell(coord)
        try
        {
            ncube.getCell(coord)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            ruleAxisDidNotBind(e)
        }
        coord.gender = 'Female'
        assert 'bar' == ncube.getCell(coord)

        try
        {
            coord.condition = 'GI Joe'
            ncube.containsCell coord
            fail 'should not make it here'
        }
        catch (CoordinateNotFoundException e)
        {
            assert e.message.toLowerCase().contains('not found')
        }

        ncube.defaultCellValue = null

        coord.condition = 'Male'
        assertFalse ncube.containsCell(coord)
        coord.condition = 'Female'
        assert ncube.containsCell(coord)
    }

    @Test
    void testOneRuleSetCallsAnotherRuleSet() throws Exception
    {
        NCubeManager.getNCubeFromResource 'ruleSet2.json'
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleSet1.json'
        def input = [age:10]
        def output = [:]
        ncube.getCell input, output
        assert 1.0 == output.total

        input.age = 48
        ncube.getCell input, output
        assert 8.560 == output.total

        input.age = 84
        ncube.getCell input, output
        assert 5.150 == output.total
    }

    @Test
    void testBasicJump() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource('basicJump.json')
        def input = [age:10]
        def output = [:]
        ncube.getCell(input, output)

        assert 'child' == output.group
        assert 'thang' == output.thing

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 3 == ruleInfo.getNumberOfRulesExecuted()
//        System.out.println('ruleInfo.getRuleExecutionTrace() = ' + ruleInfo.getRuleExecutionTrace())

        input.age = 48
        ncube.getCell(input, output)
        assert 'adult' == output.group

        input.age = 84
        ncube.getCell(input, output)
        assert 'geezer' == output.group
    }

    @Test
    void testMultipleRuleAxesWithMoreThanOneRuleFiring() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'multiRule.json'
        assert 'medium-weight' == ncube.getCell([age:35, weight:99])
    }

    @Test
    void testRuleFalseValues() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleFalseValues.json'
        def input = [state:'OH']
        def output = [:]
        ncube.getCell input, output
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        assert 1 == ruleInfo.getNumberOfRulesExecuted()

        // Groovy style false
        assertFalse NCube.isTrue(null)
        assertFalse NCube.isTrue(false)
        assertFalse NCube.isTrue(Boolean.FALSE)
        assertFalse NCube.isTrue(new Boolean(false))
        assertFalse NCube.isTrue((byte) 0)
        assertFalse NCube.isTrue((short) 0)
        assertFalse NCube.isTrue((int) 0)
        assertFalse NCube.isTrue((long) 0)
        assertFalse NCube.isTrue(0f)
        assertFalse NCube.isTrue(0d)
        assertFalse NCube.isTrue(BigInteger.ZERO)
        assertFalse NCube.isTrue(BigDecimal.ZERO)
        assertFalse NCube.isTrue('')
        assertFalse NCube.isTrue(new HashMap())
        assertFalse NCube.isTrue(new HashMap().keySet().iterator())
        assertFalse NCube.isTrue(new ArrayList())
        assertFalse NCube.isTrue(new ArrayList().iterator())
        assertFalse NCube.isTrue(new Vector().elements())

        // Groovy style true
        assert NCube.isTrue(new Date())
        assert NCube.isTrue(true)
        assert NCube.isTrue(Boolean.TRUE)
        assert NCube.isTrue(new Boolean(true))
        assert NCube.isTrue((byte) 1)
        assert NCube.isTrue((short) 1)
        assert NCube.isTrue((int) 1)
        assert NCube.isTrue((long) 1)
        assert NCube.isTrue(1f)
        assert NCube.isTrue(1d)
        assert NCube.isTrue(BigInteger.ONE)
        assert NCube.isTrue(BigDecimal.ONE)
        assert NCube.isTrue('Yo')
        def map = [foo:'bar']
        assert NCube.isTrue(map)
        assert NCube.isTrue(map.keySet().iterator())
        List list = new ArrayList()
        list.add(new Date())
        assert NCube.isTrue(list)
        assert NCube.isTrue(list.iterator())
        Vector v = new Vector()
        v.add(9)
        assert NCube.isTrue(v.elements())
    }

    @Test
    void testJumpStart()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'basicJumpStart.json'
        def input = [letter:'e']
        def output = [:] as CaseInsensitiveMap
        ncube.getCell input, output

        assert output.containsKey('A')           // condition ran (condition axis was told to start at beginning - null)
        assert output.containsKey('B')
        assert output.containsKey('C')
        assert output.containsKey('D')
        assert output.containsKey('E')
        assert output.containsKey('F')
        assert output.containsKey('G')
        assert 'echo' == output.word

        input.condition = 'e'
        output.clear()
        ncube.getCell input, output

        assertFalse output.containsKey('A')           // condition never ran (condition axis was told to start at a)
        assertFalse output.containsKey('B')           // condition never ran (condition axis was told to start at a)
        assertFalse output.containsKey('C')           // condition never ran (condition axis was told to start at a)
        assertFalse output.containsKey('D')           // condition never ran (condition axis was told to start at a)
        assert output.containsKey('E')            // condition ran
        assert output.containsKey('F')            // condition ran
        assert output.containsKey('G')            // condition ran
        assert 'echo' == output.word
    }

    @Test
    void testJumpStart2D()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'basicJumpStart2D.json'
        def input = [letter:'f', column:'y', condition:'f', condition2:'y']
        def output = [:] as CaseInsensitiveMap
        ncube.getCell input, output

        assertFalse output.containsKey('A')
        // skipped - condition never ran (condition axis was told to start at f)
        assertFalse output.containsKey('B')       // skipped
        assertFalse output.containsKey('C')       // skipped
        assertFalse output.containsKey('D')       // skipped
        assertFalse output.containsKey('E')       // skipped
        assert output.containsKey('F')        // condition ran
        assert output.containsKey('G')        // condition ran
        assert 'foxtrot' == output.word

        assertFalse output.containsKey('W')
        // skipped - condition never ran (condition2 axis was told to start at y)
        assertFalse output.containsKey('X')       // skipped
        assert output.containsKey('Y')        // condition ran
        assert output.containsKey('Z')        // condition ran
        assert 'y' == output.col
    }

    @Test
    void testJumpRestart()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'basicJumpRestart.json'
        def input = [letter:'e']
        def output = [:] as CaseInsensitiveMap
        output.a = 0
        output.b = 0
        output.c = 0
        output.d = 0
        output.e = 0
        output.f = 0
        output.g = 0
        output.word = ''
        ncube.getCell input, output

        assert output.containsKey('A')
        // condition ran (condition axis was told to start at beginning - null)
        assert output.containsKey('B')
        assert output.containsKey('C')
        assert output.containsKey('D')
        assert output.containsKey('E')
        assert output.containsKey('F')
        assert output.containsKey('G')
        assert 'echoecho' == output.word

        assert 1 == output.a
        assert 1 == output.b
        assert 1 == output.c
        assert 1 == output.d
        assert 2 == output.e       // This step is run twice.
        assert 1 == output.f
        // This step is run once (skipped the first time, then after 'e' runs a 2nd time)
        assert 1 == output.g
        // This step is run once (skipped the first time, then after 'e' runs a 2nd time)
    }

    @Test
    void testNoRuleBinding()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleSet2.json'

        def output = [:]
        try
        {
            ncube.getCell([:], output)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            assert e.message.toLowerCase().contains("no condition")
            assert e.message.toLowerCase().contains("rule axis")
            assert e.message.toLowerCase().contains("fired")
            assert e.message.toLowerCase().contains("no default column")
        }
        RuleInfo ruleInfo = (RuleInfo) output[NCube.RULE_EXEC_INFO]
        assert 0 == ruleInfo.getNumberOfRulesExecuted()
    }

    @Test
    void testNCubeGroovyExpressionAPIs()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'expressionTests.json'
        NCubeManager.getNCubeFromResource 'months.json'

        def input = ['Age':10]
        def output = [:]
        ncube.getCell input, output
        assert output.isAxis
        assert output.isColumn
        assert output.isRange
        assert output.colId > 0
        assert output.containsKey(0)
        assert 'sys.classpath' == output[0]
    }

    @Test
    void testRuleStop()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'ruleStop.json'
        def output = [:]
        ncube.getCell([:], output)
        assert 200 == output.price
    }

    @Test
    void testRuleInfo()
    {
        RuleInfo ruleInfo = new RuleInfo()
        assert '' == ruleInfo.getSystemOut()
        ruleInfo.setSystemOut('the quick brown fox')
        assert 'the quick brown fox' == ruleInfo.getSystemOut()
        assert '' == ruleInfo.getSystemErr()
        ruleInfo.setSystemErr('the quick brown dog')
        assert 'the quick brown dog' == ruleInfo.getSystemErr()
        assertNull ruleInfo.getLastExecutedStatementValue()
    }

    @Test
    void testRuleBinding()
    {
        Binding binding = new Binding('fancy', 2)
        assert 'fancy' == binding.cubeName

        String html = binding.toHtml()
        assert html.contains(' fancy')
        assert html.contains('value')
        assert html.contains('null')
    }

    @Test
    void testRuleInfoRuleName()
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'multiRule.json'
        def input = [age:18, weight:125]
        def output = [:]
        def ret = ncube.getCell input, output
        assert 'medium-weight' == ret
        RuleInfo ruleInfo = (RuleInfo) output[NCube.RULE_EXEC_INFO]
        List<Binding> bindings = ruleInfo.getAxisBindings()
        for (Binding binding : bindings)
        {
            String html = binding.toHtml()
            assert html.contains('medium /')
        }
    }

    @Test
    void testRuleSimpleWithDefault()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('ruleSimpleWithDefault.json')
        def input = [state:'OH']
        def output = [:]
        def ret = ncube.getCell(input, output)
        assert ret == 'Ohio'
        assert output.text == 'Ohio'

        input = [state:'OH', rule:'OhioRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Ohio'
        assert output.text == 'Ohio'

        input = [state:'O', rule:'OhioRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'nope'
        assert output.text == 'nope'

        input = [state:'O']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'nope'
        assert output.text == 'nope'

        input = [state:'TX']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Texas'
        assert output.text == 'Texas'

        input = [state:'TX', rule:'TexasRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Texas'
        assert output.text == 'Texas'

        input = [state:'O', rule:'TexasRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'nope'
        assert output.text == 'nope'

        // Starting at 'OhioRule' but input value is TX so we should get Texas
        input = [state:'TX', rule:'OhioRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Texas'
        assert output.text == 'Texas'

        // Starting at 'TexasRule' but input value is OH so we should get 'no state' (because of rule order)
        input = [state:'OH', rule:'TexasRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'nope'
        assert output.text == 'nope'

        input = [state:'OH', rule:'MatchesNoRuleName']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'nope'
        assert output.text == 'nope'
    }

    @Test
    void testRuleSimpleWithNoDefault()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('ruleSimpleWithNoDefault.json')
        def input = [state: 'OH']
        def output = [:]
        def ret = ncube.getCell(input, output)
        assert ret == 'Ohio'
        assert output.text == 'Ohio'

        input = [state:'OH', rule:'OhioRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Ohio'
        assert output.text == 'Ohio'

        input = [state:'O', rule:'OhioRule']
        output = [:]
        try
        {
            ncube.getCell(input, output)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            ruleAxisDidNotBind(e)
        }

        input = [state:'TX', rule:'OhioRule']
        output = [:]
        ret = ncube.getCell(input, output)
        assert ret == 'Texas'
        assert output.text == 'Texas'

        input = [state:'OH', rule:'TexasRule']
        output = [:]
        try
        {
            ncube.getCell(input, output)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            ruleAxisDidNotBind(e)
        }

        input = [state:'OH', rule:'MatchesNoRuleName']
        output = [:]
        try
        {
            ncube.getCell(input, output)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            assert e.message.toLowerCase().contains('rule named')
            assert e.message.toLowerCase().contains('matches no column')
            assert e.message.toLowerCase().contains('no default column')
        }
    }

    @Test
    void testFireOne()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('ruleFireOneVer1D.json')

        // Start at 1st rule
        def input = [:]
        def output = [:]
        def ret = ncube.getCell(input, output)
        assert output.A == 'A fired'
        assert ret == 'A fired'
        assert output.size() == 3   // A, _rule, return

        // Start on 2nd rule
        input = [rule:'SecondRule']
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.B == 'B fired'
        assert ret == 'B fired'
        assert output.size() == 3   // B, _rule, return

        Axis axis = ncube.getAxis('rule')
        axis.fireAll = true
        input.clear()
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.A == 'A fired'
        assert output.B == 'B fired'
        assert ret == 'B fired'
        assert output.size() == 4   // A, B, _rule, return
    }

    @Test
    void testFireOne2D()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('ruleFireOneVer2D.json')
        def input = [:]
        def output = [:]
        def ret = ncube.getCell(input, output)
        assert output.A1 == 'A1 fired'
        assert ret == 'A1 fired'
        assert output.size() == 3   // B, _rule, return

        input = [ruleLetter:'BRule']
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.B1 == 'B1 fired'
        assert ret == 'B1 fired'
        assert output.size() == 3   // B, _rule, return

        input = [ruleNumber:'2Rule']
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.A2 == 'A2 fired'
        assert ret == 'A2 fired'
        assert output.size() == 3   // B, _rule, return

        input = [ruleNumber:'2Rule', ruleLetter:'BRule']
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.B2 == 'B2 fired'
        assert ret == 'B2 fired'
        assert output.size() == 3   // B, _rule, return

        // Switch RuleNumber axis back to fireAll=true
        Axis axis = ncube.getAxis('ruleNumber')
        axis.fireAll = true
        input.clear()
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.size() == 4
        assert output.A1 == 'A1 fired'
        assert output.A2 == 'A2 fired'
        assert ret == 'A2 fired'

        axis.fireAll = false;
        axis = ncube.getAxis('ruleLetter')
        axis.fireAll = true
        input.clear()
        output.clear()
        ret = ncube.getCell(input, output)
        assert output.size() == 4
        assert output.A1 == 'A1 fired'
        assert output.B1 == 'B1 fired'
        assert ret == 'B1 fired'
    }

    @Test
    void testRuleFire()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('rule-test-1.json')
        def input = [bu: 'R', item: new Date()]
        def output = [:]
        ncube.getCell(input, output)
        assert output.initialize == true
        assert output.active == true
    }
}
