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
        assert 20 == ncube.getCell([age:10, 'state':'AK'])
        assert 40 == ncube.getCell([age:20, 'state':'KS'])

        String json = NCubeManager.getResourceAsString('TestCubeLevelDefault.json')

        NCube x = NCube.fromSimpleJson(json)
        String json2 = x.toFormattedJson()
        NCube y = NCube.fromSimpleJson(json2)
        assert x.sha1() == y.sha1()
    }

    @Test
    void testDefaultExpressionWithCacheTrue()
    {
        NCube ncube = NCubeManager.getNCubeFromResource('TestCubeLevelDefaultTrue.json')
        assert 1 == ncube.getCell([age:10, 'state':'OH'])
        assert 20 == ncube.getCell([age:10, 'state':'AK'])
        assert 20 == ncube.getCell([age:20, 'state':'KS'])

        String json = NCubeManager.getResourceAsString('TestCubeLevelDefaultTrue.json')

        NCube x = NCube.fromSimpleJson(json)
        String json2 = x.toFormattedJson()
        NCube y = NCube.fromSimpleJson(json2)
        assert x.sha1() == y.sha1()
    }
}
