package com.cedarsoftware.ncube.formatters

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.CellInfo
import com.cedarsoftware.ncube.GroovyExpression
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.NCubeTest
import com.cedarsoftware.ncube.RuleInfo
import com.cedarsoftware.ncube.StringValuePair
import com.cedarsoftware.ncube.TestingDatabaseHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

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
public class TestTestResultsFormatter
{
    @Before
    public void init() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    public void testResultsFromNCube()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, 'idNoValue.json')
        def coord = [age:18, state:'OH']
        def output = [:]
        ncube.getCell coord, output
        String s = new TestResultsFormatter(output).format()
        assert s.contains('idNoValue')
        assert s.contains('Age: 18')
        assert s.contains('State: OH')
        assert s.contains('value = 18 OH')
        assert s.contains('Assertions')
        assert s.contains('No assertion failures')
        assert s.contains('Output Map')
        assert s.contains('No output')
        assert s.contains('System.out')
        assert s.contains('System.err')
    }

    @Test
    public void testResultsWithOutputAndError() throws Exception
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, 'idNoValue.json')
        def coord = [age:18, state:'OH']
        def output = ['foo.age':'56', 'foo.name':'John']
        ncube.getCell coord, output

        Set<String> assertionFailures = new HashSet<>()
        assertionFailures.add '[some assertion happened]'

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO)
        ruleInfo.setAssertionFailures(assertionFailures)

        String s = new TestResultsFormatter(output).format()
        assert s.contains('idNoValue')
        assert s.contains('Age: 18')
        assert s.contains('State: OH')
        assert s.contains('value = 18 OH')
        assert s.contains('Assertions')
        assert s.contains('[some assertion happened]')
        assert s.contains('Output Map')
        assert s.contains('foo.name = John')
        assert s.contains('foo.age = 56')
        assert s.contains('return = 18 OH')
        assert s.contains('System.out')
        assert s.contains('System.err')
    }

    @Test
    public void testOutput() throws Exception
    {
        StringValuePair<CellInfo>[] coord = new StringValuePair[0]
        CellInfo[] expected = new CellInfo[3]
        expected[0] = new CellInfo(3.0d)
        expected[1] = new CellInfo(3.0f)
        expected[2] = new CellInfo(new GroovyExpression('help me', null))
        NCubeTest test = new NCubeTest('testName', coord, expected)
        assert test.name == 'testName'
    }
}
