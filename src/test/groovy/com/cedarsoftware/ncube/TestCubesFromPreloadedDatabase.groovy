package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

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
class TestCubesFromPreloadedDatabase
{
    public static String USER_ID = TestNCubeManager.USER_ID
    public static ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "preloaded", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

    private TestingDatabaseManager manager;

    @Before
    public void setup() throws Exception
    {
        manager = TestingDatabaseHelper.testingDatabaseManager
        manager.setUp()
        NCubeManager.NCubePersister = TestingDatabaseHelper.persister
    }

    @After
    public void tearDown() throws Exception
    {
        manager.tearDown()
        manager = null;
        NCubeManager.clearCache()
    }

    @Test
    void testUrlClassLoader() throws Exception {
        loadCubesToDatabase(appId, "sys.classpath.cp1.json")

        // nothing in cache until we try and get the classloader or load a cube.
        assertEquals(0, NCubeManager.getCacheForApp(appId).size())

        //  url classloader has 1 item
        Map input = [:]
        URLClassLoader loader = NCubeManager.getUrlClassLoader(appId, input)
        assertEquals(1, loader.URLs.length)
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())
        assertEquals(new URL("http://www.cedarsoftware.com/tests/ncube/cp1/"), loader.URLs[0])

        Map<String, Object> cache = NCubeManager.getCacheForApp(appId)
        assertEquals(1, cache.size())

        assertNotNull(NCubeManager.getUrlClassLoader(appId, input))
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        NCubeManager.clearCache()
        assertEquals(0, NCubeManager.getCacheForApp(appId).size())

        cache = NCubeManager.getCacheForApp(appId)
        assertEquals(1, NCubeManager.getUrlClassLoader(appId, input).URLs.length)
        assertEquals(1, cache.size())


        manager.removeCubes(appId)
    }

    @Test
    void testCoordinateNotFoundExceptionThrown() throws Exception {
        loadCubesToDatabase(appId, "test.coordinate.not.found.exception.json")

        NCube cube = NCubeManager.getCube(appId, "test.coordinate.not.found.exception")

        try {
            cube.getCell([:])
            fail();
        } catch (CoordinateNotFoundException e) {
            assertTrue(e.getMessage().contains("not found"));
        }

        manager.removeCubes(appId)
    }

    @Test
    void testGetBranches() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch");
        assertEquals("ABC", cube.getCell(["Code":-7]));
        cube = NCubeManager.getCube(head, "TestAge");
        assertEquals("youth", cube.getCell(["Code":5]));

        // load cube with same name, but different structure in TEST branch
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "kenny");
        loadCubesToDatabase(branch, "test.branch.2.json")

        List<String> list = NCubeManager.getBranches(head);
        assertEquals(2, list.size());
    }


    @Test
    void testRetrieveBranchesAtSameTimeToTestMergeLogic() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");
        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(branch, "test.branch.2.json")
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCreateBranch() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"));
        assertNull(NCubeManager.getCube(branch, "TestAge"));

        testValuesOnBranch(head)

        def cube1Sha1 = NCubeManager.getCube(head, "TestBranch").getMetaProperty("sha1");
        def cube2Sha1 = NCubeManager.getCube(head, "TestAge").getMetaProperty("sha1");

        assertNull(NCubeManager.getCube(head, "TestBranch").getMetaProperty("headSha1"));
        assertNull(NCubeManager.getCube(head, "TestAge").getMetaProperty("headSha1"));

        assertEquals(2, NCubeManager.createBranch(branch));

        assertEquals(cube1Sha1, NCubeManager.getCube(branch, "TestBranch").getMetaProperty("sha1"));
        assertEquals(cube1Sha1, NCubeManager.getCube(branch, "TestBranch").getMetaProperty("headSha1"));
        assertEquals(cube2Sha1, NCubeManager.getCube(branch, "TestAge").getMetaProperty("sha1"));
        assertEquals(cube2Sha1, NCubeManager.getCube(branch, "TestAge").getMetaProperty("headSha1"));

        testValuesOnBranch(head);
        testValuesOnBranch(branch);

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranch() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"));
        assertNull(NCubeManager.getCube(branch, "TestAge"));

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch));

        testValuesOnBranch(branch);

        NCube cube = NCubeManager.getCube(branch, "TestBranch");
        assertEquals("GHI", cube.getCell([Code : 10.0]));
        cube = NCubeManager.getCube(branch, "TestBranch");
        assertEquals("GHI", cube.getCell([Code : 10.0]));

        // edit cube
        cube.removeCell([Code : 10.0]);

        cube = NCubeManager.getCube(branch, "TestBranch");
        assertEquals("ZZZ", cube.getCell([Code : 10.0]));

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID));

        // commit the branch
        cube = NCubeManager.getCube(branch, "TestBranch");
        assertEquals("ZZZ", cube.getCell([Code : 10.0]));

        Object[] dtos = NCubeManager.getCubeRecordsFromDatabase(branch, "TestBranch");

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID);

        // both should be updated now.
        cube = NCubeManager.getCube(branch, "TestBranch");
        assertEquals("ZZZ", cube.getCell([Code : 10.0]));
        cube = NCubeManager.getCube(head, "TestBranch");
        assertEquals("ZZZ", cube.getCell([Code : 10.0]));

        def history = NCubeManager.getRevisionHistory(head, "TestBranch");
        assertEquals(2, history.length);

        //  this test will break after first commit change.
        assertTrue(map.isEmpty());


        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCreateBranchThatAlreadyExists() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        //1) should work
        NCubeManager.createBranch(branch);

        try {
            //2) should already be created.
            NCubeManager.createBranch(branch);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testReleaseCubes() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        assertNull(NCubeManager.getCube(branch, "TestBranch"));
        assertNull(NCubeManager.getCube(branch, "TestAge"));

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch));

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.2.json")
        assertNotNull(cube);
        NCubeManager.updateCube(branch, cube, "jdirt");

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size());
        testValuesOnBranch(branch, "FOO");

        assertEquals(2, NCubeManager.releaseCubes(head, "1.29.0"));

        assertNull(NCubeManager.getCube(branch, "TestAge"));
        assertNull(NCubeManager.getCube(branch, "TestBranch"));
        assertNull(NCubeManager.getCube(head, "TestAge"));
        assertNull(NCubeManager.getCube(head, "TestBranch"));

        head = new ApplicationID('NONE', "test", "1.29.0", "SNAPSHOT", ApplicationID.HEAD);
        branch = new ApplicationID('NONE', "test", "1.29.0", "SNAPSHOT", "FOO");

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size());

        head = new ApplicationID('NONE', "test", "1.28.0", "RELEASE", ApplicationID.HEAD);

        testValuesOnBranch(head);
        testValuesOnBranch(branch, "FOO");

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    private void testValuesOnBranch(ApplicationID appId, String code1 = "ABC", String code2 = "youth") {
        NCube cube = NCubeManager.getCube(appId, "TestBranch");
        assertEquals(code1, cube.getCell(["Code": -7]));
        cube = NCubeManager.getCube(appId, "TestAge");
        assertEquals(code2, cube.getCell(["Code": 5]));
    }

    @Test
    void testGetBranchesCode() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD);
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO");

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(branch, "test.branch.2.json")
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        try {
            NCubeManager.getBranchChangesFromDatabase(head)
        } catch (IllegalArgumentException e) {
            assertTrue(e.message.contains('from HEAD'));
        }

        NCubeInfoDto[] dtos = (NCubeInfoDto[]) NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(1, dtos.length);

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testBootstrapWithOverrides() throws Exception {
        ApplicationID id = ApplicationID.getBootVersion('none', 'example')
        assertEquals(new ApplicationID('NONE', 'EXAMPLE', '0.0.0', ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD), id);

        loadCubesToDatabase(id, "sys.bootstrap.user.overloaded.json")

        NCube cube = NCubeManager.getCube(id, 'sys.bootstrap')
        // ensure properties are cleared
        System.setProperty('NCUBE_PARAMS', '');
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.28.0', 'SNAPSHOT', 'HEAD'), cube.getCell([env:'DEV']));
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.25.0', 'RELEASE', 'HEAD'), cube.getCell([env:'PROD']));
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.29.0', 'SNAPSHOT', 'baz'), cube.getCell([env:'SAND']));

        System.setProperty("NCUBE_PARAMS", '{"status":"RELEASE", "app":"UD", "tenant":"foo", "branch":"bar"}')
        assertEquals(new ApplicationID('foo', 'UD', '1.28.0', 'RELEASE', 'bar'), cube.getCell([env:'DEV']));
        assertEquals(new ApplicationID('foo', 'UD', '1.25.0', 'RELEASE', 'bar'), cube.getCell([env:'PROD']));
        assertEquals(new ApplicationID('foo', 'UD', '1.29.0', 'RELEASE', 'bar'), cube.getCell([env:'SAND']));

        System.setProperty("NCUBE_PARAMS", '{"branch":"bar"}')
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.28.0', 'SNAPSHOT', 'bar'), cube.getCell([env:'DEV']));
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.25.0', 'RELEASE', 'bar'), cube.getCell([env:'PROD']));
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.29.0', 'SNAPSHOT', 'bar'), cube.getCell([env:'SAND']));

        manager.removeCubes(appId)
    }

    @Test
    public void testUserOverloadedClassPath() throws Exception {
        loadCubesToDatabase(appId, "sys.classpath.user.overloaded.json", "sys.versions.json");

        // Check DEV
        NCube cube = NCubeManager.getCube(appId, "sys.classpath");
        // ensure properties are cleared.
        System.setProperty('NCUBE_PARAMS', '');
        assertEquals(['https://www.foo.com/tests/ncube/cp1/public/', 'https://www.foo.com/tests/ncube/cp1/private/', 'https://www.foo.com/tests/ncube/cp1/private/groovy/'], cube.getCell([env:"DEV"]));

        // Check INT
        assertEquals(['https://www.foo.com/tests/ncube/cp2/public/', 'https://www.foo.com/tests/ncube/cp2/private/', 'https://www.foo.com/tests/ncube/cp2/private/groovy/'], cube.getCell([env:"INT"]));

        // Check with overload
        System.setProperty("NCUBE_PARAMS", '{"cpBase":"C:/Development/"}')
        assertEquals(['C:/Development/public/', 'C:/Development/private/', 'C:/Development/private/groovy/'], cube.getCell([env:"DEV"]));
        assertEquals(['C:/Development/public/', 'C:/Development/private/', 'C:/Development/private/groovy/'], cube.getCell([env:"INT"]));

        // Check version overload only
        System.setProperty("NCUBE_PARAMS", '{"version":"1.28.0"}')
        assertEquals(['https://www.foo.com/1.28.0/public/', 'https://www.foo.com/1.28.0/private/', 'https://www.foo.com/1.28.0/private/groovy/'], cube.getCell([env:"DEV"]));


    }

    @Test
    public void testClearCacheWithClassLoaderLoadedByCubeRequest() throws Exception {

        loadCubesToDatabase(appId, "sys.classpath.cp1.json", "GroovyMethodClassPath1.json")

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())
        NCube cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1")
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        Map input = new HashMap()
        input.put("method", "foo")
        Object x = cube.getCell(input)
        assertEquals("foo", x)

        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.put("method", "foo2")
        x = cube.getCell(input)
        assertEquals("foo2", x)

        input.put("method", "bar")
        x = cube.getCell(input)
        assertEquals("Bar", x)


        // change classpath in database only
        NCube[] cp2 = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.cp2.json")
        manager.updateCube(appId, USER_ID, cp2[0])
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        // reload hasn't happened in cache so we get same answers as above
        input = new HashMap()
        input.put("method", "foo")
        x = cube.getCell(input)
        assertEquals("foo", x)

        input.put("method", "foo2")
        x = cube.getCell(input)
        assertEquals("foo2", x)

        input.put("method", "bar")
        x = cube.getCell(input)
        assertEquals("Bar", x)


        //  clear cache so we get different answers this time.  classpath 2 has already been loaded in database.
        NCubeManager.clearCache(appId)

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())

        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1")
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        input = new HashMap()
        input.put("method", "foo")
        x = cube.getCell(input)
        assertEquals("boo", x)

        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.put("method", "foo2")
        x = cube.getCell(input)
        assertEquals("boo2", x)

        input.put("method", "bar")
        x = cube.getCell(input)
        assertEquals("far", x)

        manager.removeCubes(appId)
    }

    @Test
    void testMultiCubeClassPath() throws Exception {

        loadCubesToDatabase(appId, "sys.classpath.base.json", "sys.classpath.json", "sys.status.json", "sys.versions.json", "sys.version.json", "GroovyMethodClassPath1.json")

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())
        NCube cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1")

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        def input = [:]
        input.env = "DEV";
        input.put("method", "foo")
        Object x = cube.getCell(input)
        assertEquals("foo", x)

        assertEquals(4, NCubeManager.getCacheForApp(appId).size())

        // cache hasn't been cleared yet.
        input.put("method", "foo2")
        x = cube.getCell(input)
        assertEquals("foo2", x)

        input.put("method", "bar")
        x = cube.getCell(input)
        assertEquals("Bar", x)

        NCubeManager.clearCache(appId)

        // Had to reget cube so I had a new classpath
        cube = NCubeManager.getCube(appId, "GroovyMethodClassPath1")

        input.env = 'UAT';
        input.put("method", "foo")
        x = cube.getCell(input)

        assertEquals("boo", x)

        assertEquals(4, NCubeManager.getCacheForApp(appId).size())

        input.put("method", "foo2")
        x = cube.getCell(input)
        assertEquals("boo2", x)

        input.put("method", "bar")
        x = cube.getCell(input)
        assertEquals("far", x)

        //  clear cache so we get different answers this time.  classpath 2 has already been loaded in database.
        NCubeManager.clearCache(appId)
        assertEquals(0, NCubeManager.getCacheForApp(appId).size())

        manager.removeCubes(appId)
    }

    @Test
    void testTwoClasspathsSameAppId() throws Exception
    {
        loadCubesToDatabase(appId, "sys.classpath.2per.app.json", "GroovyExpCp1.json")

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())
        NCube cube = NCubeManager.getCube(appId, "GroovyExpCp1")

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        def input = [:]
        input.env = "a";
        input.put("state", "OH")
        def x = cube.getCell(input)
        assert 'Hello, world.' == x

        // GroovyExpCp1 and sys.classpath are now both loaded.
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.env = "b";
        input.put("state", "TX")
        def y = cube.getCell(input)
        assert 'Goodbye, world.' == y

        // Test JsonFormatter - that it properly handles the URLClassLoader in the sys.classpath cube
        NCube cp = NCubeManager.getCube(appId, "sys.classpath");
        String json = cp.toFormattedJson();

        NCube cp2 = NCube.fromSimpleJson(json)
        cp.removeMetaProperty("sha1")
        cp2.removeMetaProperty("sha1")
        assert cp.toFormattedJson() == cp2.toFormattedJson()

        // Test HtmlFormatter - that it properly handles the URLClassLoader in the sys.classpath cube
        String html = cp.toHtml()
        assert html.contains('http://www.cedarsoftware.com')

        manager.removeCubes(appId)
    }

    @Test
    void testMathControllerUsingExpressions() throws Exception
    {
        loadCubesToDatabase(appId, "sys.classpath.2per.app.json", "math.controller.json")

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())
        NCube cube = NCubeManager.getCube(appId, "MathController")

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())
        def input = [:]
        input.env = "a"
        input.x = 5
        input.method = 'square'

        assertEquals(1, NCubeManager.getCacheForApp(appId).size())
        assertEquals(25, cube.getCell(input))
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.method = 'factorial'
        assertEquals(120, cube.getCell(input))

        // same number of cubes, different cells
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        // test that shows you can add an axis to a controller to selectively choose a new classpath
        input.env = "b"
        input.method = 'square'
        assertEquals(5, cube.getCell(input))
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.method = 'factorial'
        assertEquals(5, cube.getCell(input))
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())
    }

    @Test
    void testClearCache()
    {
        loadCubesToDatabase(appId, "sys.classpath.cedar.json", "cedar.hello.json")

        Map input = new HashMap()
        NCube cube = NCubeManager.getCube(appId, 'hello');
        Object out = cube.getCell(input)
        assertEquals('Hello, world.', out)
        NCubeManager.clearCache(appId)

        cube = NCubeManager.getCube(appId, 'hello')
        out = cube.getCell(input)
        assertEquals('Hello, world.', out)

        // remove cubes for this test.
        manager.removeCubes(appId)
    }

    @Test
    void testMultiTenantApplicationIdBootstrap()
    {
        loadCubesToDatabase(appId, "sys.bootstrap.multi.api.json", "sys.bootstrap.version.json")

        def input = [:];
        input.env = "SAND";

        NCube cube = NCubeManager.getCube(appId, 'sys.bootstrap');
        Map<String, ApplicationID> map = cube.getCell(input)
        assertEquals(new ApplicationID("NONE", "APP", "1.15.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("A"));
        assertEquals(new ApplicationID("NONE", "APP", "1.19.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("B"));
        assertEquals(new ApplicationID("NONE", "APP", "1.28.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("C"));

        input.env = "INT"
        map = cube.getCell(input)

        assertEquals(new ApplicationID("NONE", "APP", "1.25.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("A"));
        assertEquals(new ApplicationID("NONE", "APP", "1.26.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("B"));
        assertEquals(new ApplicationID("NONE", "APP", "1.27.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("C"));


        // remove cubes for this test.
        manager.removeCubes(appId)
    }

    private loadCubesToDatabase(ApplicationID id, String ...names) {
        manager.addCubes(id, USER_ID, TestingDatabaseHelper.getCubesFromDisk(names))
    }
}
