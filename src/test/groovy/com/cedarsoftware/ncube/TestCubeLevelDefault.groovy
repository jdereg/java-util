package com.cedarsoftware.ncube

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
class TestCubeLevelDefault
{
    @Test
    void testDefaultExpression()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('TestCubeLevelDefault.json')
        assert 1 == ncube.getCell([age:10, 'state':'OH'])
        assert 2 == ncube.getCell([age:10, 'state':'NJ'])
        assert 3 == ncube.getCell([age:10, 'state':'TX'])
        assert 20 == ncube.getCell([age:10, 'state':'AK'])
        assert 40 == ncube.getCell([age:20, 'state':'ME'])
    }

    @Test
    void testDefaultExpressionWithCaching()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('TestCubeLevelDefaultCache.json')
        assert 1 == ncube.getCell([age:10, 'state':'OH'])
        assert 2 == ncube.getCell([age:10, 'state':'NJ'])
        assert 3 == ncube.getCell([age:10, 'state':'TX'])
        assert 20 == ncube.getCell([age:10, 'state':'AK'])
        assert 20 == ncube.getCell([age:20, 'state':'ME'])
    }

    @Test
    public void testDefaultExpressionSha1()
    {
        assertSha1Calculation('TestCubeLevelDefaultCache.json')
        assertSha1Calculation('TestCubeLevelDefault.json')
    }

    private void assertSha1Calculation(String jsonFile)
    {
        String json = NCubeManager.getResourceAsString(jsonFile)

        NCube x = NCube.fromSimpleJson(json)
        String json2 = x.toFormattedJson()
        NCube y = NCube.fromSimpleJson(json2)
        assert x.sha1() == y.sha1()
    }
}
