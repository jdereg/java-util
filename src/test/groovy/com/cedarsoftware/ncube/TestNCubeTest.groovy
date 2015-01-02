package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

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
public class TestNCubeTest
{
    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    public void testNCubeTest()
    {
        String name = 'test-1';
        StringValuePair[] params = new StringValuePair[3];
        params[0] = new StringValuePair('int', new CellInfo('int', '5', false, false))
        params[1] = new StringValuePair('double', new CellInfo('double', '5.75', false, false))
        params[2] = new StringValuePair('key1', new CellInfo('string', 'foo', false, false))

        CellInfo[] info = new CellInfo[1];
        info[0] = new CellInfo('exp', "'bar'", false, false)

        NCubeTest test = new NCubeTest(name, params, info)

        CellInfo[] assertions = test.assertions
        assert 1 == assertions.length

        StringValuePair<CellInfo>[] coord = test.coord
        assert 3 == coord.length

        def coord1 = test.createCoord()
        assert 5 == coord1.int
        assertEquals 5.75, coord1.double, 0.00001d
        assert 'foo' == coord1.key1

        final List<GroovyExpression> testAssertions = test.createAssertions()

        // unfortunately you have to pass in ncube to the execute.
        def map = [ncube:new NCube('hello'), input:[:],output:[:]]
        assert 'bar' == testAssertions[0].execute(map)
    }
}
