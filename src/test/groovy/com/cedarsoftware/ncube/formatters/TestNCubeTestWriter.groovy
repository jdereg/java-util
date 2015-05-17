package com.cedarsoftware.ncube.formatters

import com.cedarsoftware.ncube.CellInfo
import com.cedarsoftware.ncube.NCubeTest
import com.cedarsoftware.ncube.StringValuePair
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
class TestNCubeTestWriter
{
    private NCubeTest verySimpleTest = new NCubeTest("foo", new StringValuePair[0], [] as CellInfo[])

    @Test
    void testVerySimpleCase()
    {
        NCubeTest[] tests = [verySimpleTest] as NCubeTest[]
        String s = new NCubeTestWriter().format tests
        assertEquals '[{"name":"foo","coord":[],"assertions":[]}]', s
    }

    @Test
    void testNullCase()
    {
        String s = new NCubeTestWriter().format null
        assertEquals '[]', s
    }

    @Test
    void testEmptyCase()
    {
        String s = new NCubeTestWriter().format new NCubeTest[0]
        assertEquals '[]', s
    }
}
