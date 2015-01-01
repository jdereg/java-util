package com.cedarsoftware.ncube.formatters

import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
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
public class TestHtmlFormatter
{
    @Test
    public void testGetCellValueAsString()
    {
        assertEquals "null", HtmlFormatter.getCellValueAsString(null)
        assertEquals "foo", HtmlFormatter.getCellValueAsString("foo")
        assertEquals "[0, 1, 2, 3]", HtmlFormatter.getCellValueAsString([0, 1, 2, 3 ] as int[])
        assertEquals "[true, false]", HtmlFormatter.getCellValueAsString([Boolean.TRUE, Boolean.FALSE] as Object[])
        assertEquals "5.0", HtmlFormatter.getCellValueAsString(5.0d)
    }
}
