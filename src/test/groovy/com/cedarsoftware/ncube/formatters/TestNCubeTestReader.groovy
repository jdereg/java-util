package com.cedarsoftware.ncube.formatters

import com.cedarsoftware.ncube.NCubeTest
import com.cedarsoftware.util.IOUtilities
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
class TestNCubeTestReader
{
    @Test
    void testReading() throws Exception
    {
        String s = getResourceAsString 'n-cube-tests/test.json'
        NCubeTestReader reader = new NCubeTestReader()
        List<NCubeTest> list = reader.convert(s)
        assertEquals 17, list.size()
    }

    @Test
    void testEmptyString() throws Exception
    {
        NCubeTestReader reader = new NCubeTestReader()
        List<NCubeTest> list = reader.convert ''
        assertEquals 0, list.size()
    }

    @Test
    void testNullString() throws Exception
    {
        NCubeTestReader reader = new NCubeTestReader()
        List<NCubeTest> list = reader.convert null
        assertEquals 0, list.size()
    }

    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeTestReader.class.getResource('/' + name)
        IOUtilities.transfer new File(url.file), out
        return new String(out.toByteArray(), 'UTF-8')
    }
}
