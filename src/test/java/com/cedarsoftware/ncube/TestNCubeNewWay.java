package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by kpartlow on 12/23/2014.
 */
public class TestNCubeNewWay
{
    public static String USER_ID = TestNCubeManager.USER_ID;
    private TestingDatabaseManager manager;

    @Before
    public void setup() throws Exception {
        manager = TestingDatabaseHelper.getTestingDatabaseManager();
        manager.setUp();

        NCubeManager.setNCubePersister(TestingDatabaseHelper.getPersister());
    }

    @After
    public void tearDown() throws Exception {
        manager.tearDown();
        manager = null;

        NCubeManager.clearCache();
    }

    @Test
    public void testUrlClassLoader() throws Exception {
        final String name = "SomeName";

        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.cp1.json");

        // add cubes for this test.
        ApplicationID customId = new ApplicationID("NONE", "updateCubeSys", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        manager.addCubes(customId, USER_ID, ncubes);

        // nothing in cache until we try and get the classloader or load a cube.
        assertEquals(0, NCubeManager.getCacheForApp(customId).size());

        //  url classloader has 1 item
        URLClassLoader loader = NCubeManager.getUrlClassLoader(customId, name);
        assertEquals(1, loader.getURLs().length);
        assertEquals(1, NCubeManager.getCacheForApp(customId).size());
        assertEquals(new URL("http://www.cedarsoftware.com/tests/ncube/cp1/"), loader.getURLs()[0]);

        Map<String, Object> cache = NCubeManager.getCacheForApp(customId);
        assertEquals(1, cache.size());

        assertNotNull(NCubeManager.getUrlClassLoader(customId, name));
        assertEquals(1, NCubeManager.getCacheForApp(customId).size());

        NCubeManager.clearCache();
        assertEquals(0, NCubeManager.getCacheForApp(customId).size());

        cache = NCubeManager.getCacheForApp(customId);
        assertEquals(1, cache.size());
        assertEquals(1, NCubeManager.getUrlClassLoader(customId, name).getURLs().length);


        manager.removeCubes(customId, USER_ID, ncubes);
    }

    @Test
    public void testClearCacheWithClassLoaderLoadedByCubeRequest() throws Exception {

        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.cp1.json", "GroovyMethodClassPath1.json");

        // add cubes for this test.
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "GroovyMethodCP", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        manager.addCubes(appId, USER_ID, ncubes);

        assertEquals(0, NCubeManager.getCacheForApp(appId).size());
        NCube cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");
        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

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

        // change classpath in database only
        NCube[] cp2 = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.cp2.json");
        manager.updateCube(appId, USER_ID, cp2[0]);
        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

        // reload hasn't happened in cache so we get same answers as above
        input = new HashMap();
        input.put("method", "foo");
        x = cube.getCell(input);
        assertEquals("foo", x);

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("foo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("Bar", x);


        //  clear cache so we get different answers this time.  classpath 2 has already been loaded in database.
        NCubeManager.clearCache(appId);

        // even though you think this might need to be 0 it is 1 because clearCache() calls resolveClassPath()
        assertEquals(1, NCubeManager.getCacheForApp(appId).size());


        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");
        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

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

        manager.removeCubes(appId, USER_ID, ncubes);
    }

}
