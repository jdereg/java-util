package com.cedarsoftware.ncube.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        assertTrue(testLoader2.isLocalOnlyResource("ncube/grv/exp/NCubeGroovyExpression"));
        assertTrue(testLoader2.isLocalOnlyResource("ncube/grv/method/NCubeGroovyController"));
        assertFalse(testLoader2.isLocalOnlyResource("FooBeanInfo.groovy"));
        assertFalse(testLoader2.isLocalOnlyResource("FooCustomizer.groovy"));
        assertTrue(testLoader2.isLocalOnlyResource("AnyThing.class"));
        assertFalse(testLoader2.isLocalOnlyResource("AnyThing.groovy"));
    }

    @Test
    public void testGetResource() {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.getClassLoader(), true, true);
        assertNotNull(testLoader1.getResource("cdnRouter.json"));
        assertNull(testLoader1.getResource("ncube/grv/method/NCubeGroovyController.class"));
        assertNotNull(TestCdnClassLoader.class.getClassLoader().getResource("ncube/grv/method/NCubeGroovyController.class"));
    }

    @Test
    public void testGetResources() throws Exception {
        CdnClassLoader testLoader1 = new CdnClassLoader(TestCdnClassLoader.class.getClassLoader(), true, true);
        assertTrue(testLoader1.getResources("cdnRouter.json").hasMoreElements());
        assertFalse(testLoader1.getResources("ncube/grv/method/NCubeGroovyController.class").hasMoreElements());
        assertTrue(TestCdnClassLoader.class.getClassLoader().getResources("ncube/grv/method/NCubeGroovyController.class").hasMoreElements());
    }
}