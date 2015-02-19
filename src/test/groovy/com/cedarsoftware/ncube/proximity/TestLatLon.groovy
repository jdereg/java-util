package com.cedarsoftware.ncube.proximity

import org.junit.Test

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
class TestLatLon
{
    @Test
    void testEquals()
    {
        LatLon l = new LatLon(1.0, 2.0)
        assert l != new Long(5)
        assert l == new LatLon(1, 2)
    }

    @Test
    void testCompareTo()
    {
        LatLon l = new LatLon(10.0, 10.0)
        assert l.compareTo(new LatLon(1, 10)) > 0
        assert l.compareTo(new LatLon(10, 0)) > 0
        assert l.compareTo(new LatLon(20, 10)) < 0
        assert l.compareTo(new LatLon(10, 20)) < 0
        assert l.compareTo(new LatLon(10, 10)) == 0

        assert '10.0, 10.0' == l.toString()
    }
}
