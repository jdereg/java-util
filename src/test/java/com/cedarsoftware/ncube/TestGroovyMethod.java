package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
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
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception {
        Class c = GroovyMethod.class;
        Constructor<GroovyMethod> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);
        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testGetCubeNamesFromTestWhenEmpty()
    {
        Set set = new HashSet();
        GroovyBase.getCubeNamesFromText(set, "");
        assertEquals(0, set.size());
    }

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

    @Test
    public void testGroovyMethodClearCache() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "GroovyMethodCP", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());

        NCube cpCube = NCubeManager.getNCubeFromResource(appId, "sys.classpath.cp1.json");
        NCubeManager.createCube(appId, cpCube, TestNCubeManager.USER_ID);

        NCube cube = NCubeManager.getNCubeFromResource(appId, "GroovyMethodClassPath1.json");
        NCubeManager.createCube(appId, cube, TestNCubeManager.USER_ID);

        NCubeManager.clearCache();
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        Map input = new HashMap();
        input.put("method", "foo");
        Object x = cube.getCell(input);
        assertEquals("foo", x);

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("foo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("Bar", x);

        cpCube = NCubeManager.getNCubeFromResource(appId, "sys.classpath.cp2.json");
        NCubeManager.updateCube(appId, cpCube, TestNCubeManager.USER_ID);

        NCubeManager.clearCache();
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        input = new HashMap();
        input.put("method", "foo");
        x = cube.getCell(input);
        assertEquals("boo", x);

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("boo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("far", x);
        TestingDatabaseHelper.tearDownDatabase();
    }

    @Test
    public void testGroovyMethodClearCacheExplicitly() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "GroovyMethodCP", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());

        NCube cpCube = NCubeManager.getNCubeFromResource(appId, "sys.classpath.cp1.json");
        NCubeManager.createCube(appId, cpCube, TestNCubeManager.USER_ID);

        NCube cube = NCubeManager.getNCubeFromResource(appId, "GroovyMethodClassPath1.json");
        NCubeManager.createCube(appId, cube, TestNCubeManager.USER_ID);

        NCubeManager.clearCache(appId);
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        Map input = new HashMap();
        input.put("method", "foo");
        Object x = cube.getCell(input);
        assertEquals("foo", x);

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("foo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("Bar", x);

        cpCube = NCubeManager.getNCubeFromResource(appId, "sys.classpath.cp2.json");
        NCubeManager.updateCube(appId, cpCube, TestNCubeManager.USER_ID);

        NCubeManager.clearCache(appId);
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        input = new HashMap();
        input.put("method", "foo");
        x = cube.getCell(input);
        assertEquals("boo", x);

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("boo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("far", x);
        TestingDatabaseHelper.tearDownDatabase();
    }
}
