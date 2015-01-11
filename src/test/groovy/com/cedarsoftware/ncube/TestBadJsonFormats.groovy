package com.cedarsoftware.ncube

import org.junit.Test;

/**
 * Test improper JSON formats
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
public class TestBadJsonFormats
{
    @Test(expected=RuntimeException.class)
    public void testNCubeMissingColumnParserError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-column-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeEmptyColumnsError()
    {
        NCubeManager.getNCubeFromResource("ncube-column-not-array-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeEmptyAxesParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-empty-axes-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeMissingAxesParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-axes-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testNCubeMissingNameParseError()
    {
        NCubeManager.getNCubeFromResource("ncube-missing-name-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testLatLongParseError()
    {
        NCubeManager.getNCubeFromResource("lat-lon-parse-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testDateParseError()
    {
        NCubeManager.getNCubeFromResource("date-parse-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testPoint2dParseError()
    {
        NCubeManager.getNCubeFromResource("point2d-parse-error.json")
    }

    @Test(expected=RuntimeException.class)
    public void testPoint3dParseError()
    {
        NCubeManager.getNCubeFromResource("point3d-parse-error.json")
    }

    @Test
    public void testNoCells()
    {
        NCube cube = NCubeManager.getNCubeFromResource("no-cells.json")
        assert cube.sha1().length() == 40
        assert cube.toFormattedJson().contains("cells")
    }
}
