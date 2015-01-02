package com.cedarsoftware.ncube

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

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
public class TestColumn
{
    @Test
    public void testSetValue()
    {
        Column c = new Column(0, 5)
        assertEquals 0, c.value
        c.value = 5
        assertEquals 5, c.value
    }

    @Test
    public void testMetaProperties()
    {
        Column c = new Column(true, 5)

        assertNull c.removeMetaProperty('foo')
        assertNull c.metaProperties.get('foo')

        c.clearMetaProperties()
        c.setMetaProperty 'foo', 'bar'
        assertEquals 'bar', c.metaProperties.get('foo')
        assertEquals 'bar', c.getMetaProperty('foo')

        c.clearMetaProperties()
        assertNull c.metaProperties.get('foo')
        assertNull c.getMetaProperty('foo')
        c.clearMetaProperties()
        Map map = new HashMap()
        map.put 'BaZ', 'qux'

        c.addMetaProperties(map)
        assertEquals 'qux', c.metaProperties.get('baz')
        assertEquals 'qux', c.getMetaProperty('baz')

        assertEquals 'qux', c.removeMetaProperty('baz')
        assertEquals null, c.removeMetaProperty('baz')
    }
}
