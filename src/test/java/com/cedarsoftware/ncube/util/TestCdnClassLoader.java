package com.cedarsoftware.ncube.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 8/2/2014.
 */
public class TestCdnClassLoader
{
    @Test
    public void testLocalResources() {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.getClassLoader(), true, true);
        assertTrue(testLoader1.isLocalOnlyResource("org.codehaus.groovy.transform.ASTTransformation"));
        assertTrue(testLoader1.isLocalOnlyResource("ncube/grv/exp/GroovyExpression"));
        assertTrue(testLoader1.isLocalOnlyResource("ncube/grv/method/GroovyMethod"));
        assertTrue(testLoader1.isLocalOnlyResource("FooBeanInfo.groovy"));
        assertTrue(testLoader1.isLocalOnlyResource("FooCustomizer.groovy"));
        assertTrue(testLoader1.isLocalOnlyResource("AnyThing.class"));
        assertFalse(testLoader1.isLocalOnlyResource("AnyThing.groovy"));

        CdnClassLoader testLoader2 = new CdnClassLoader(TestCdnClassLoader.class.getClassLoader(), false, false);
        assertTrue(testLoader2.isLocalOnlyResource("org.codehaus.groovy.transform.ASTTransformation"));
        assertTrue(testLoader2.isLocalOnlyResource("ncube/grv/exp/GroovyExpression"));
        assertTrue(testLoader2.isLocalOnlyResource("ncube/grv/method/GroovyMethod"));
        assertFalse(testLoader2.isLocalOnlyResource("FooBeanInfo.groovy"));
        assertFalse(testLoader2.isLocalOnlyResource("FooCustomizer.groovy"));
        assertTrue(testLoader2.isLocalOnlyResource("AnyThing.class"));
        assertFalse(testLoader2.isLocalOnlyResource("AnyThing.groovy"));
    }
}
