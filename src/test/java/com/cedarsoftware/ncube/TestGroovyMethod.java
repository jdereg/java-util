package com.cedarsoftware.ncube;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
}
