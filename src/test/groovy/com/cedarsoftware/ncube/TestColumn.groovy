package com.cedarsoftware.ncube

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

/**
 * Created by kpartlow on 8/6/2014.
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
