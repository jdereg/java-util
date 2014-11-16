package com.cedarsoftware.ncube;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 4/10/2014.
 */
public class TestGroovyMethod
{
    @Test
    public void testGroovyMethod() {
        GroovyMethod m = new GroovyMethod("cmd", null);
        Map input = new HashMap();
        input.put("method", "foo");
        Map coord = new HashMap();
        coord.put("input", input);
        assertEquals("foo", m.getMethodToExecute(coord));
    }

    @Test
    public void testGetCubeNamesFromTestWithEmptyString() {
        GroovyMethod m = new GroovyMethod("cmd", null);
        Set<String> set = new HashSet<String>();
        m.getCubeNamesFromCommandText(set);
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructionState() {
        GroovyMethod m = new GroovyMethod("cmd", "com/foo/not/found/bar.groovy");
        assertEquals("cmd", m.getCmd());
        assertEquals("com/foo/not/found/bar.groovy", m.getUrl());
        assertEquals(true, m.isCacheable());
    }
}
