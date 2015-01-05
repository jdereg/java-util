package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

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
        Map input = [:]
        URLClassLoader loader = NCubeManager.getUrlClassLoader(customId, input);
        assertEquals(1, loader.getURLs().length);
        assertEquals(1, NCubeManager.getCacheForApp(customId).size());
        assertEquals(new URL("http://www.cedarsoftware.com/tests/ncube/cp1/"), loader.getURLs()[0]);

        Map<String, Object> cache = NCubeManager.getCacheForApp(customId);
        assertEquals(1, cache.size());

        assertNotNull(NCubeManager.getUrlClassLoader(customId, input));
        assertEquals(1, NCubeManager.getCacheForApp(customId).size());

        NCubeManager.clearCache();
        assertEquals(0, NCubeManager.getCacheForApp(customId).size());

        cache = NCubeManager.getCacheForApp(customId);
        assertEquals(1, NCubeManager.getUrlClassLoader(customId, input).getURLs().length);
        assertEquals(1, cache.size());


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
        assertEquals(1, NCubeManager.getCacheForApp(appId).size());

        Map input = new HashMap();
        input.put("method", "foo");
        Object x = cube.getCell(input);
        assertEquals("foo", x);

        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

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

        assertEquals(0, NCubeManager.getCacheForApp(appId).size());

        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");
        assertEquals(1, NCubeManager.getCacheForApp(appId).size());

        input = new HashMap();
        input.put("method", "foo");
        x = cube.getCell(input);
        assertEquals("boo", x);

        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("boo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("far", x);

        manager.removeCubes(appId, USER_ID, ncubes);
    }

    @Test
    public void testMultiCubeClassPath() throws Exception {

        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.base.json", "sys.classpath.json", "sys.status.json", "sys.versions.json", "sys.version.json", "GroovyMethodClassPath1.json");

        // add cubes for this test.
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "GroovyMethodCP", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        manager.addCubes(appId, USER_ID, ncubes);

        assertEquals(0, NCubeManager.getCacheForApp(appId).size());
        NCube cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size());

        def input = [:]
        input.env = "DEV";
        input.put("method", "foo");
        Object x = cube.getCell(input);
        assertEquals("foo", x);

        assertEquals(4, NCubeManager.getCacheForApp(appId).size());

        // cache hasn't been cleared yet.
        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("foo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("Bar", x);


        //TODO:  Uncomment this to make test work.
        NCubeManager.clearCache(appId);



        // Had to reget cube so I had a new classpath
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1");

        //TODO: Move these two lines above the previous line to make this work.  The classpath is already cached by the time it gets here.
        input.env = 'UAT';
        input.put("method", "foo");
        x = cube.getCell(input);

        assertEquals("boo", x);

        assertEquals(4, NCubeManager.getCacheForApp(appId).size());

        input.put("method", "foo2");
        x = cube.getCell(input);
        assertEquals("boo2", x);

        input.put("method", "bar");
        x = cube.getCell(input);
        assertEquals("far", x);

        //  clear cache so we get different answers this time.  classpath 2 has already been loaded in database.
        NCubeManager.clearCache(appId);
        assertEquals(0, NCubeManager.getCacheForApp(appId).size());

        manager.removeCubes(appId, USER_ID, ncubes);
    }

    @Test
    public void testTwoClasspathsSameAppId() throws Exception
    {
        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.2per.app.json", "GroovyExpCp1.json");

        // add cubes for this test.
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "GroovyMethodCP", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        manager.addCubes(appId, USER_ID, ncubes);

        assertEquals(0, NCubeManager.getCacheForApp(appId).size());
        NCube cube = NCubeManager.getCube(appId, "GroovyExpCp1");

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size());

        def input = [:]
        input.env = "a";
        input.put("state", "OH");
        def x = cube.getCell(input);
        assert 'Hello, world.' == x

        // GroovyExpCp1 and sys.classpath are now both loaded.
        assertEquals(2, NCubeManager.getCacheForApp(appId).size());

        input.env = "b";
        input.put("state", "TX");
        def y = cube.getCell(input);
        assert 'Goodbye, world.' == y

        manager.removeCubes(appId, USER_ID, ncubes);
    }
}
