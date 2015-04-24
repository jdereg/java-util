package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.BranchMergeException
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.util.CdnClassLoader
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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
abstract class TestWithPreloadedDatabase
{
    public static String USER_ID = TestNCubeManager.USER_ID

    public static ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "preloaded", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
    private static final ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
    private static final ApplicationID branch1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")
    private static final ApplicationID branch2 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "BAR")
    private static final ApplicationID branch3 = new ApplicationID('NONE', "test", "1.29.0", "SNAPSHOT", "FOO")
    private static final ApplicationID boot = new ApplicationID('NONE', "test", "0.0.0", "SNAPSHOT", ApplicationID.HEAD)

    ApplicationID[] branches = [head, branch1, branch2, branch3, appId, boot] as ApplicationID[];

    private TestingDatabaseManager manager;

    @Before
    public void setup() throws Exception
    {
        manager = getTestingDatabaseManager();
        manager.setUp()

        NCubeManager.NCubePersister = getNCubePersister();
    }

    @After
    public void tearDown() throws Exception
    {
        manager.removeBranches(branches)
        manager.tearDown()
        manager = null;

        NCubeManager.clearCache()

    }

    abstract TestingDatabaseManager getTestingDatabaseManager();
    abstract NCubePersister getNCubePersister();

    private preloadCubes(ApplicationID id, String ...names) {
        manager.addCubes(id, USER_ID, TestingDatabaseHelper.getCubesFromDisk(names))
    }


    @Test
    void testToMakeSureOldStyleSysClasspathThrowsException() throws Exception {
        preloadCubes(appId, "sys.classpath.old.style.json")

        // nothing in cache until we try and get the classloader or load a cube.
        assertEquals(0, NCubeManager.getCacheForApp(appId).size())

        //  url classloader has 1 item
        try {
            Map input = [:]
            URLClassLoader loader = NCubeManager.getUrlClassLoader(appId, input)
        } catch (IllegalStateException e) {
            assertTrue(e.message.contains('sys.classpath cube'));
            assertTrue(e.message.contains('exists'));
            assertTrue(e.message.toLowerCase().contains('urlclassloader'));
        }
    }

    @Test
    void testUrlClassLoader() throws Exception {
        preloadCubes(appId, "sys.classpath.cp1.json")

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
    }

    @Test
    void testCoordinateNotFoundExceptionThrown() throws Exception
    {
        preloadCubes(appId, "test.coordinate.not.found.exception.json")

        NCube cube = NCubeManager.getCube(appId, "test.coordinate.not.found.exception")

        try {
            cube.getCell([:])
            fail()
        } catch (CoordinateNotFoundException e) {
            assertTrue(e.getMessage().contains("not found"))
        }
    }

    @Test
    void testGetBranches() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code":-7]))
        cube = NCubeManager.getCube(head, "TestAge")
        assertEquals("youth", cube.getCell(["Code":5]))

        // load cube with same name, but different structure in TEST branch
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "kenny")
        preloadCubes(branch1, "test.branch.2.json")

        // showing we only rely on tenant to get branches.
        assertEquals(2, NCubeManager.getBranches('NONE').size())
        assertEquals(2, NCubeManager.getBranches('NONE').size())

        ApplicationID branch2 = new ApplicationID('NONE', 'foo', '1.29.0', 'SNAPSHOT', 'someoneelse')
        preloadCubes(branch2, "test.branch.1.json", "test.branch.age.1.json")
        assertEquals(3, NCubeManager.getBranches('NONE').size())
        assertEquals(3, NCubeManager.getBranches('NONE').size())
        assertEquals(3, NCubeManager.getBranches('NONE').size())
    }

    @Test
    void testGetAppNames() throws Exception {
        ApplicationID app1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID app2 = new ApplicationID('NONE', "foo", "1.29.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID app3 = new ApplicationID('NONE', "bar", "1.29.0", "SNAPSHOT", ApplicationID.HEAD)
        preloadCubes(app1, "test.branch.1.json", "test.branch.age.1.json")
        preloadCubes(app2, "test.branch.1.json", "test.branch.age.1.json")
        preloadCubes(app3, "test.branch.1.json", "test.branch.age.1.json")

        ApplicationID branch1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", 'kenny')
        ApplicationID branch2 = new ApplicationID('NONE', 'foo', '1.29.0', 'SNAPSHOT', 'kenny')
        ApplicationID branch3 = new ApplicationID('NONE', 'test', '1.29.0', 'SNAPSHOT', 'someoneelse')
        ApplicationID branch4 = new ApplicationID('NONE', 'test', '1.28.0', 'SNAPSHOT', 'someoneelse')

        assertEquals(2, NCubeManager.createBranch(branch1))
        assertEquals(2, NCubeManager.createBranch(branch2))
        // version doesn't match one in head, nothing created.
        assertEquals(0, NCubeManager.createBranch(branch3))
        assertEquals(2, NCubeManager.createBranch(branch4))

        // showing we only rely on tenant and branch to get app names.
        assertEquals(3, NCubeManager.getAppNames('NONE', 'SNAPSHOT', ApplicationID.HEAD).size())
        assertEquals(2, NCubeManager.getAppNames('NONE', 'SNAPSHOT', 'kenny').size())
        assertEquals(1, NCubeManager.getAppNames('NONE', 'SNAPSHOT', 'someoneelse').size())

        manager.removeBranches([app1, app2, app3, branch1, branch2, branch3, branch4] as ApplicationID[])
    }

    @Test
    void testCommitBranchOnCubeCreatedInBranch() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)

        // ensure that there are no more branch changes after create
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(0, dtos.length);

        ApplicationID headId = branch1.asHead();
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(headId, null, true).length)

    }

    @Test
    void testGetBranchChangesOnceBranchIsDeleted() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertTrue(NCubeManager.deleteBranch(branch1))

        // ensure that there are no more branch changes after delete
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(0, dtos.length);
    }

    @Test
    void testUpdateBranchOnCubeCreatedInBranch() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        Object[] objects = NCubeManager.updateBranch(branch1, USER_ID)
        assertEquals(0, objects.length)

        //  update didn't affect item added locally
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(1, dtos.length);
    }

    @Test
    void testRollbackBranchWithPendingAdd() throws Exception {
        preloadCubes(head, "test.branch.1.json");

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        NCubeManager.rollbackBranch(branch1, dtos)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        assertEquals(0, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)
    }

    @Test
    void testRollbackBranchWithDeletedCube() throws Exception {
        preloadCubes(head, "test.branch.1.json");

        NCubeManager.createBranch(branch1);

        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(head, null, true).length);
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(branch1, null, true).length);

        NCubeManager.deleteCube(branch1, "TestBranch", USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        // undo delete
        NCubeManager.rollbackBranch(branch1, dtos)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        assertEquals(0, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)
    }

    @Test
    void testCommitBranchOnCreateThenDeleted() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube, 'kenny')
        NCubeManager.deleteCube(branch1, "TestAge", 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        assertEquals(0, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)

        ApplicationID headId = branch1.asHead()
        assertEquals(0, NCubeManager.getCubeRecordsFromDatabase(headId, null, false).length)
    }

    @Test
    void testUpdateBranchOnCreateThenDeleted() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube, 'kenny')
        NCubeManager.deleteCube(branch1, "TestAge", 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        Object[] result = NCubeManager.updateBranch(branch1, USER_ID)
        assertEquals(0, result.length)

        ApplicationID headId = branch1.asHead()
        assertEquals(0, NCubeManager.getCubeRecordsFromDatabase(headId, null, false).length)
    }

    @Test
    void testUpdateBranchWhenCubeWasDeletedInDifferentBranchAndNotChangedInOurBranch() throws Exception {
        preloadCubes(head, "test.branch.1.json")

        NCubeManager.createBranch(branch1)
        NCubeManager.createBranch(branch2)
        NCubeManager.deleteCube(branch2, "TestBranch", USER_ID);

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2);

        assertEquals(1, dtos.length);
        assertEquals(1, NCubeManager.commitBranch(branch2, dtos, USER_ID).length);

        assertEquals(1, NCubeManager.updateBranch(branch1, USER_ID).length);
    }

    @Test
    void testUpdateBranchWhenCubeWasDeletedInDifferentBranchAndDeletedInOurBranch() throws Exception {
        preloadCubes(head, "test.branch.1.json")

        NCubeManager.createBranch(branch1)
        NCubeManager.createBranch(branch2)
        NCubeManager.deleteCube(branch2, "TestBranch", USER_ID);
        NCubeManager.deleteCube(branch1, "TestBranch", USER_ID);

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2);

        assertEquals(1, dtos.length);
        assertEquals(1, NCubeManager.commitBranch(branch2, dtos, USER_ID).length);

        assertEquals(0, NCubeManager.updateBranch(branch1, USER_ID).length);
    }

    @Test
    void testCreateBranch() throws Exception
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        testValuesOnBranch(head)

        def cube1Sha1 = NCubeManager.getCube(head, "TestBranch").sha1()
        def cube2Sha1 = NCubeManager.getCube(head, "TestAge").sha1()

        Object[] objects = NCubeManager.getCubeRecordsFromDatabase(head, "*");
        for (NCubeInfoDto dto : objects) {
            assertNull(dto.headSha1);
        }

        assertEquals(2, NCubeManager.createBranch(branch1))

        assertEquals(cube1Sha1, NCubeManager.getCube(branch1, "TestBranch").sha1())
        assertEquals(cube2Sha1, NCubeManager.getCube(branch1, "TestAge").sha1())

        objects = NCubeManager.getCubeRecordsFromDatabase(branch1, "*");
        for (NCubeInfoDto dto : objects) {
            assertNotNull(dto.headSha1);
        }


        testValuesOnBranch(head)
        testValuesOnBranch(branch1)
    }

    @Test
    void testCommitBranchWithItemCreatedInBranchOnly() throws Exception
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.createBranch(branch1))

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        assertNotNull(cube)
        NCubeManager.createCube(branch1, cube, "jdirt")


        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)
        assertNull(NCubeManager.getCube(head, "TestAge"))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)
    }

    @Test
    void testUpdateBranchWithUpdateOnBranch() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(head, "TestAge"));
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertNull(NCubeManager.getCube(branch2, "TestBranch"));
        assertNull(NCubeManager.getCube(branch2, "TestAge"));

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch1))
        assertEquals(1, NCubeManager.createBranch(branch2))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestBranch").length)

        NCube cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))

        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cubes[0], USER_ID);

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length)
        NCubeManager.commitBranch(branch1, dtos, USER_ID);

        assertEquals(2, NCubeManager.updateBranch(branch2, USER_ID).length)

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch2, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestAge").length)

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch2, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
    }

    @Test
    void testCommitBranchOnUpdate() throws Exception {

        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        //  test values on branch
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // commit the branch
        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)
        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // both should be updated now.
        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
    }

    @Test
    void testGetCubeNamesWithoutBeingAddedToDatabase()
    {
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json", "test.branch.age.1.json")
        NCubeManager.addCube(branch1, cubes[0])
        NCubeManager.addCube(branch1, cubes[1])
        Set<String> set = NCubeManager.getCubeNames(branch1)
        assertEquals(2, set.size());
        assertTrue(set.contains("TestBranch"))
        assertTrue(set.contains("TestAge"))
    }

    @Test
    void testCommitBranchOnUpdateWithOldInvalidSha1() throws Exception {
        // load cube with same name, but different structure in TEST branch
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json")

        manager.insertCubeWithNoSha1(head, USER_ID, cubes[0])

        //assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        assertEquals(1, NCubeManager.createBranch(branch1))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID);

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch2"))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size())
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").length)

        // No changes have happened yet, even though sha1 is incorrect,
        // we just copy the sha1 when we create the branch so the headsha1 won't
        // differ until we make a change.
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        assertEquals(0, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)
    }

    @Test
    void testCommitBranchWithUpdateAndWrongRevisionNumber() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)
        ((NCubeInfoDto)dtos[0]).revision = Long.toString(100)

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)
    }



    @Test
    void testRollback() throws Exception
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        //  test values on branch
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        cube.setCell("FOO", [Code : 10.0])
        assertEquals(3, cube.getCellMap().size())
        assertEquals("FOO", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))
        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))
        assertEquals(4, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        cube.setCell("FOO", [Code : 10.0])
        assertEquals(3, cube.getCellMap().size())
        assertEquals("FOO", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))
        assertEquals(5, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.rollbackBranch(branch1, dtos))

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)
    }

    @Test
    void testCommitBranchOnDelete() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        //  test values on branch
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // cube is deleted
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch though only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).length)

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // both should be updated now.
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(head, "TestBranch"))
    }

    @Test
    void testSearch() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")
        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.search(head, "Test*", "zzz").length);
        assertEquals(1, NCubeManager.search(head, "*TestBranch*", "ZZZ").length);
        assertEquals(1, NCubeManager.search(head, "Test*", "baby").length);
        assertEquals(0, NCubeManager.search(head, "TestBranch*", "baby").length);
        assertEquals(1, NCubeManager.search(head, "TestAge", "BABY").length);
        assertEquals(1, NCubeManager.search(head, null, "baby").length);
    }

    @Test
    void testSystemParamsCube() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")
        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.search(head, "Test*", "zzz").length);
        assertEquals(1, NCubeManager.search(head, "*TestBranch*", "ZZZ").length);
        assertEquals(1, NCubeManager.search(head, "Test*", "baby").length);
        assertEquals(0, NCubeManager.search(head, "TestBranch*", "baby").length);
        assertEquals(1, NCubeManager.search(head, "TestAge", "BABY").length);
        assertEquals(1, NCubeManager.search(head, null, "baby").length);
    }

    @Test
    void testUpdateBranchAfterDelete() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        //  test values on branch
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // cube is deleted
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch though only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        Object[] result = NCubeManager.updateBranch(branch1, USER_ID)
        assertEquals(0, result.length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(head, "TestBranch"))
    }

    @Test
    void testCreateBranchThatAlreadyExists() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        //1) should work
        NCubeManager.createBranch(branch1)

        try {
            //2) should already be created.
            NCubeManager.createBranch(branch1)
            fail()
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("already exists"))
        }
    }

    @Test
    void testReleaseCubes() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.2.json")
        assertNotNull(cube)
        NCubeManager.updateCube(branch1, cube, "jdirt")

        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        testValuesOnBranch(branch1, "FOO")

        assertEquals(2, NCubeManager.releaseCubes(head, "1.29.0"))

        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(head, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestBranch"))

//        ApplicationID newSnapshot = head.createNewSnapshotId("1.29.0")
        ApplicationID newBranchSnapshot = branch1.createNewSnapshotId("1.29.0")

        ApplicationID release = head.asRelease()

        testValuesOnBranch(release)
        testValuesOnBranch(newBranchSnapshot, "FOO")

        manager.removeBranches([release, newBranchSnapshot] as ApplicationID[])
    }


    @Test
    void testDuplicateCubeChanges() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        NCubeManager.duplicate(head, branch2, "TestBranch", "TestBranch2", USER_ID);
        NCubeManager.duplicate(head, branch2, "TestAge", "TestAge", USER_ID);

        // assert head and branch are still there
        testValuesOnBranch(head);
        testValuesOnBranch(branch1);

        //  Test with new name.
        NCube cube = NCubeManager.getCube(branch2, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(branch2, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))
    }

    @Test
    void testDuplicateCubeGoingToDifferentApp() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertNull(NCubeManager.getCube(appId, "TestBranch"))
        assertNull(NCubeManager.getCube(appId, "TestAge"))

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)
        assertNull(NCubeManager.getCube(appId, "TestBranch"))
        assertNull(NCubeManager.getCube(appId, "TestAge"))

        NCubeManager.duplicate(branch1, appId, "TestBranch", "TestBranch", USER_ID);
        NCubeManager.duplicate(head, appId, "TestAge", "TestAge", USER_ID);

        // assert head and branch are still there
        testValuesOnBranch(head);
        testValuesOnBranch(branch1);
        testValuesOnBranch(appId);
    }

    @Test
    void testDuplicateCubeOnDeletedCube() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        assertEquals(1, NCubeManager.createBranch(branch1))
        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))

        try
        {
            NCubeManager.duplicate(branch1, appId, "TestBranch", "TestBranch", USER_ID);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("Unable to duplicate"));
            assertTrue(e.message.contains("deleted"));
        }
    }

    @Test
    void testRenameCubeOnDeletedCube() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        assertEquals(1, NCubeManager.createBranch(branch1))
        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))

        try
        {
            NCubeManager.renameCube(branch1, "TestBranch", "Foo", USER_ID);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("cannot be rename"));
            assertTrue(e.message.contains("Deleted cubes"));
        }
    }

    @Test
    void testDuplicateWhenCubeWithNameAlreadyExists() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        try
        {
            NCubeManager.duplicate(branch1, branch1, "TestBranch", "TestAge", USER_ID);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to duplicate"))
            assertTrue(e.message.contains("already exists"))
        }
    }


    @Test
    void testRenameCubeWhenNewNameAlreadyExists() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        try
        {
            NCubeManager.renameCube(branch1, "TestBranch", "TestAge", USER_ID);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to rename"))
            assertTrue(e.message.contains("already exists"))
        }
    }

    @Test
    void testRenameCubeWithHeadHavingCubeAAndCubeBDeleted() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").size())
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, "*").length);

        assertEquals(2, NCubeManager.createBranch(branch1));

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);

        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);
        assertTrue(NCubeManager.deleteCube(branch1, "TestAge", USER_ID))
        assertEquals(2, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);

        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestAge").size())

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);


        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertNull(NCubeManager.getCube(head, "TestBranch"))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestAge").size())
        assertEquals(2, NCubeManager.getDeletedCubesFromDatabase(head, "*").length);


        NCubeManager.restoreCube(branch1, ["TestBranch"] as Object[], USER_ID);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);
        assertNull(NCubeManager.getCube(branch1, "TestAge"));
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"))

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestAge", USER_ID));
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch1, "TestAge"))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(1, dtos.length);

        assertEquals(1, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertNull(NCubeManager.getCube(head, "TestBranch"))
        assertNotNull(NCubeManager.getCube(head, "TestAge"))

        assertTrue(NCubeManager.renameCube(branch1, "TestAge", "TestBranch", USER_ID));
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").length);
        assertNull(NCubeManager.getCube(branch1, "TestAge"));
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());
    }

    @Test
    void testRenameCubeWithBothCubesCreatedOnBranch() throws Exception {
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json", "test.branch.age.1.json");
        NCubeManager.createCube(branch1, cubes[0], USER_ID);
        NCubeManager.createCube(branch1, cubes[1], USER_ID);

        assertNull(NCubeManager.getCube(head, "TestBranch"));
        assertNull(NCubeManager.getCube(head, "TestAge"));
        assertNotNull(NCubeManager.getCube(branch1, "TestAge"));
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch1, "TestAge"))


        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertNull(NCubeManager.getCube(head, "TestBranch"))
        assertNotNull(NCubeManager.getCube(head, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch2").size())
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").size())
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, "*").length);

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch2", "TestBranch", USER_ID));

        assertNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch1, "TestAge"))

        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").size())

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);
        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch2").size())
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(head, "*").length);
    }



    @Test
    void testRenameCubeWhenNewNameAlreadyExistsButIsInactive() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        NCubeManager.deleteCube(branch1, "TestAge", USER_ID)

        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())

        //  cube is deleted so won't throw exception
        NCubeManager.renameCube(branch1, "TestBranch", "TestAge", USER_ID)

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())
    }

    @Test
    void testDuplicateCubeWhenNewNameAlreadyExistsButIsInactive() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        NCubeManager.deleteCube(branch1, "TestAge", USER_ID)

        assertNull(NCubeManager.getCube(branch1, "TestAge"))
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())

        //  cube is deleted so won't throw exception
        NCubeManager.duplicate(branch1, branch1, "TestBranch", "TestAge", USER_ID)

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestAge").size())
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())
    }

    @Test
    void testRenameAndThenRenameAgainThenRollback()
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())
        assertEquals(2, NCubeManager.getBranchChangesFromDatabase(branch1).length)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch2", "TestBranch", USER_ID));
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(0, dtos.length);

        assertNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertEquals(0, NCubeManager.rollbackBranch(branch1, dtos));

        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));
        assertNull(NCubeManager.getCube(branch1, "TestBranch2"));
    }

    @Test
    void testRenameAndThenRollback()
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())
        assertEquals(2, NCubeManager.getBranchChangesFromDatabase(branch1).length)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.rollbackBranch(branch1, dtos));

        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestBranch2"))
    }

    @Test
    void testRenameAndThenCommitAndThenRenameAgainWithCommit()
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        assertTrue(NCubeManager.renameCube(branch, "TestBranch", "TestBranch2", USER_ID));

        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch, "TestAge"))

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch2").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch, "*").size())
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch, dtos, USER_ID).size());

        assertNull(NCubeManager.getCube(head, "TestBranch"))
        assertNotNull(NCubeManager.getCube(head, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(head, "TestAge"))

        assertTrue(NCubeManager.renameCube(branch, "TestBranch2", "TestBranch", USER_ID));

        assertNull(NCubeManager.getCube(branch, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch, "TestAge"))

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch2").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch, "TestBranch").size())
        dtos = NCubeManager.getBranchChangesFromDatabase(branch);

        assertEquals(2, dtos.length);
        assertEquals(2, NCubeManager.commitBranch(branch, dtos, USER_ID).size());

        assertNull(NCubeManager.getCube(head, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(head, "TestBranch"))
        assertNotNull(NCubeManager.getCube(head, "TestAge"))
    }

    @Test
    void testRenameAndThenRenameAgainThenCommit()
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        assertTrue(NCubeManager.renameCube(branch, "TestBranch", "TestBranch2", USER_ID));

        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch, "TestAge"))

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch2").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch, "*").size())
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(2, dtos.length);

        assertTrue(NCubeManager.renameCube(branch, "TestBranch2", "TestBranch", USER_ID));

        assertNull(NCubeManager.getCube(branch, "TestBranch2"))
        assertNotNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNotNull(NCubeManager.getCube(branch, "TestAge"))

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch2").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch, "TestBranch").size())
        dtos = NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(0, dtos.length);

        //  techniacally don't have to do this since there aren't any changes,
        //  but we should verify we work with 0 dtos passed in, too.  :)
        assertEquals(0, NCubeManager.commitBranch(branch, dtos, USER_ID).size());

        assertNotNull(NCubeManager.getCube(branch, "TestBranch"));
        assertNull(NCubeManager.getCube(branch, "TestBranch2"));
    }

    @Test
    void testRenameAndThenRenameAgainThenCommitWhenNotCreatedFromBranch()
    {
        // load cube with same name, but different structure in TEST branch
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json", "test.branch.age.1.json");

        NCubeManager.createCube(branch1, cubes[0], USER_ID)
        NCubeManager.createCube(branch1, cubes[1], USER_ID)

        testValuesOnBranch(branch1)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, "*").size())
        assertEquals(2, NCubeManager.getBranchChangesFromDatabase(branch1).length)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch2", "TestBranch", USER_ID));
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch2").size())
        assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").size())
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));
        assertNull(NCubeManager.getCube(branch1, "TestBranch2"));

        assertNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertNotNull(NCubeManager.getCube(head, "TestBranch"));
        assertNotNull(NCubeManager.getCube(head, "TestBranch"));
        assertNull(NCubeManager.getCube(head, "TestBranch2"));
    }


    @Test
    void testRenameCubeBasicCase() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertNotNull(NCubeManager.getCube(branch1, "TestBranch2"))
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        testValuesOnBranch(head);

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertNotNull(NCubeManager.getCube(head, "TestBranch2"));
        assertNotNull(NCubeManager.getCube(head, "TestAge"));

        //  Test with new name.
        NCube cube = NCubeManager.getCube(branch1, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(branch1, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
    }

    @Test
    void testRenameCubeBasicCaseWithNoHead() throws Exception {
        // load cube with same name, but different structure in TEST branch
        NCube cube1 = NCubeManager.getNCubeFromResource("test.branch.1.json")
        NCube cube2 = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        NCubeManager.createCube(branch1, cube1, USER_ID);
        NCubeManager.createCube(branch1, cube2, USER_ID);
        testValuesOnBranch(branch1)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        //  Test with new name.
        NCube cube = NCubeManager.getCube(branch1, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(branch1, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        cube = NCubeManager.getCube(head, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(head, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))
        assertNull(NCubeManager.getCube(head, "TestBranch"))
    }

    @Test
    void testRenameCubeFunctionality() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            NCubeManager.getRevisionHistory(branch1, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);

        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);


        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }


        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.rollbackBranch(branch1, dtos));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);

        assertEquals(0, NCubeManager.getBranchChangesFromDatabase(branch1).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            NCubeManager.getRevisionHistory(branch1, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }


        assertTrue(NCubeManager.renameCube(branch1, "TestBranch", "TestBranch2", USER_ID));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch1, dtos, USER_ID).size());

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch2").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch2").length);

        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);
    }


    @Test
    void testDuplicateCubeFunctionality() throws Exception {

        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch1))

        testValuesOnBranch(head)
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            NCubeManager.getRevisionHistory(branch1, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch2, null).length);

        NCubeManager.duplicate(branch1, branch2, "TestBranch", "TestBranch2", USER_ID);
        NCubeManager.duplicate(branch1, branch2, "TestAge", "TestAge", USER_ID);

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch2, null).length);


        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestBranch2").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            NCubeManager.getRevisionHistory(branch2, "TestBranch");
            fail();
        } catch (IllegalArgumentException e) {
        }


        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2);
        assertEquals(1, dtos.length);

        assertEquals(1, NCubeManager.rollbackBranch(branch2, dtos));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);

        assertEquals(0, NCubeManager.getBranchChangesFromDatabase(branch1).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            NCubeManager.getRevisionHistory(branch1, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }


        NCubeManager.duplicate(branch1, branch2, "TestBranch", "TestBranch2", USER_ID);

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch2, null).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        dtos = NCubeManager.getBranchChangesFromDatabase(branch2);
        assertEquals(1, dtos.length);

        assertEquals(1, NCubeManager.commitBranch(branch2, dtos, USER_ID).size());

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch2").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch2, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(branch2, "TestBranch");
            fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch1, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch2, null).length);
    }


    @Test
    void testDuplicateCubeWithNonExistentSource()
    {
        try
        {
            NCubeManager.duplicate(head, branch1, "foo", "bar", USER_ID);
            fail();
        } catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to duplicate"));
            assertTrue(e.message.contains("does not exist"));
        }
    }

    @Test
    void testDuplicateCubeWhenTargetExists()
    {
        preloadCubes(head, "test.branch.1.json");
        NCubeManager.createBranch(branch1);

        try
        {
            NCubeManager.duplicate(head, branch1, "TestBranch", "TestBranch", USER_ID);
            fail();
        } catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to duplicate"));
            assertTrue(e.message.contains("already exists"));
        }
    }

    @Test
    void testOverwriteHeadWhenHeadDoesntExist()
    {
        preloadCubes(head, "test.branch.1.json");
        NCubeManager.createBranch(branch1);
        NCubeManager.deleteCube(branch1, "TestBranch", USER_ID);

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        try
        {
            NCubeManager.duplicate(head, branch1, "TestBranch", "TestBranch", USER_ID);
            assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));
            assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);
        } catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to duplicate"));
            assertTrue(e.message.contains("already exists"));
        }
    }



    @Test
    void testDuplicateCubeWhenSourceCubeIsADeletedCube()
    {
        preloadCubes(head, "test.branch.1.json");
        NCubeManager.createBranch(branch1);
        NCubeManager.deleteCube(branch1, "TestBranch", USER_ID);

        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        try
        {
            NCubeManager.duplicate(head, branch1, "TestBranch", "TestBranch", USER_ID);
            assertNotNull(NCubeManager.getCube(branch1, "TestBranch"));
            assertEquals(3, NCubeManager.getRevisionHistory(branch1, "TestBranch").length);
        } catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("Unable to duplicate"));
            assertTrue(e.message.contains("already exists"));
        }
    }

    @Test
    void testDeleteCubeAndThenDeleteCubeAgain()
    {
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json");

        NCubeManager.createCube(branch1, cubes[0], USER_ID);
        assertNotNull(NCubeManager.getCube(branch1, "TestBranch"))

        assertTrue(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))

        //  delete on deleted just returns false, no exception.
        assertFalse(NCubeManager.deleteCube(branch1, "TestBranch", USER_ID))
    }


    private void testValuesOnBranch(ApplicationID appId, String code1 = "ABC", String code2 = "youth") {
        NCube cube = NCubeManager.getCube(appId, "TestBranch")
        assertEquals(code1, cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(appId, "TestAge")
        assertEquals(code2, cube.getCell(["Code": 5]))
    }


    @Test
    void testCommitBranchWithItemCreatedLocallyAndOnHead() {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch1))
        assertEquals(1, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.2.json")
        NCubeManager.createCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)
        NCubeManager.commitBranch(branch2, dtos, USER_ID)


        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cube, USER_ID)



        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.commitBranch(branch1, dtos, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) committing branch")
            assert e.errors.TestAge.message.toLowerCase().contains('conflict merging')
            assert e.errors.TestAge.message.toLowerCase().contains('same name')
            assertEquals("1B45FBA9BD25EDE58049F0BD0CFAF1FBE7C8C0BD", e.errors.TestAge.sha1)
            assertEquals("E38F308922AFF48EEA589C321144F2004BD9BFAC", e.errors.TestAge.headSha1)
        }
    }

    @Test
    void testOverwriteHeadCubeWhenBranchDoesNotExist()
    {
        try {
            NCubeManager.mergeOverwriteHeadCube(appId, "TestBranch", "foo", USER_ID);
            fail();
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to overwrite"));
            assertTrue(e.message.toLowerCase().contains("does not exist"));
        }
    }

    @Test
    void testOverwriteHeadCubeWhenHEADdoesNotExist()
    {
        try {
            preloadCubes(branch1, "test.branch.1.json")
            NCubeManager.mergeOverwriteHeadCube(appId, "TestBranch", "foo", USER_ID);
            fail();
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to overwrite"));
            assertTrue(e.message.toLowerCase().contains("does not exist"));
        }
    }

    @Test
    void testOverwriteBranchCubeWhenBranchDoesNotExist()
    {
        try {
            NCubeManager.mergeOverwriteBranchCube(appId, "TestBranch", "foo", USER_ID);
            fail();
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to overwrite"));
            assertTrue(e.message.toLowerCase().contains("does not exist"));
        }
    }

    @Test
    void testOverwriteBranchCubeWhenHEADDoesNotExist()
    {
        try {
            preloadCubes(branch1, "test.branch.1.json")
            NCubeManager.mergeOverwriteBranchCube(appId, "TestBranch", "foo", USER_ID);
            fail();
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("failed to overwrite"));
            assertTrue(e.message.toLowerCase().contains("does not exist"));
        }
    }

    @Test
    void testOverwriteHeadOnBranchMergeException() {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch1))
        assertEquals(1, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.2.json")
        NCubeManager.createCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)
        NCubeManager.commitBranch(branch2, dtos, USER_ID)


        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cube, USER_ID)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)
        String newSha1 = dtos[0].sha1;


        try
        {
            NCubeManager.commitBranch(branch1, dtos, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) committing branch")
            assert e.errors.TestAge.message.toLowerCase().contains('conflict merging')
            assert e.errors.TestAge.message.toLowerCase().contains('same name')
            assertEquals("1B45FBA9BD25EDE58049F0BD0CFAF1FBE7C8C0BD", e.errors.TestAge.sha1)
            assertEquals("E38F308922AFF48EEA589C321144F2004BD9BFAC", e.errors.TestAge.headSha1)
        }
        NCubeInfoDto[] dto = (NCubeInfoDto[])NCubeManager.getCubeRecordsFromDatabase(head, "TestAge");
        String sha1 = dto[0].sha1;
        assertNotEquals(sha1, newSha1);

        NCubeManager.mergeOverwriteHeadCube(branch1, "TestAge", sha1, USER_ID);

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        dtos = NCubeManager.getCubeRecordsFromDatabase(head, "TestAge");
        assertEquals(newSha1, dtos[0].sha1);
    }

    @Test
    void testUpdateBranchWithItemCreatedLocallyAndOnHead() {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch1))
        assertEquals(1, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.2.json")
        NCubeManager.createCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)
        NCubeManager.commitBranch(branch2, dtos, USER_ID)


        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cube, USER_ID)



        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.updateBranch(branch1, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) updating branch")
            assert e.errors.TestAge.message.toLowerCase().contains('cube was changed')
            assertEquals("1B45FBA9BD25EDE58049F0BD0CFAF1FBE7C8C0BD", e.errors.TestAge.sha1)
            assertEquals("E38F308922AFF48EEA589C321144F2004BD9BFAC", e.errors.TestAge.headSha1)
        }
    }

    @Test
    void testMergeOverwriteBranchWithItemsCreatedInBothPlaces() {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch1))
        assertEquals(1, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.2.json")
        NCubeManager.createCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)
        NCubeManager.commitBranch(branch2, dtos, USER_ID)


        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch1, cube, USER_ID)



        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.updateBranch(branch1, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) updating branch")
            assert e.errors.TestAge.message.toLowerCase().contains('cube was changed')
            assertEquals("1B45FBA9BD25EDE58049F0BD0CFAF1FBE7C8C0BD", e.errors.TestAge.sha1)
            assertEquals("E38F308922AFF48EEA589C321144F2004BD9BFAC", e.errors.TestAge.headSha1)
        }

        dtos = NCubeManager.getCubeRecordsFromDatabase(branch1, "TestAge");
        String sha1 = dtos[0].sha1;

        NCubeManager.mergeOverwriteBranchCube(branch1, "TestAge", sha1, USER_ID);

        assertEquals(0, NCubeManager.getBranchChangesFromDatabase(branch1).length);

    }

    @Test
    void testCommitBranchWithItemThatWasChangedOnHeadAndInBranch()
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))
        assertEquals(2, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getCube(branch2, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)

        NCubeManager.commitBranch(branch2, dtos, USER_ID)

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : -10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(branch1, cube, USER_ID)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.commitBranch(branch1, dtos, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) committing branch")
            assert e.errors.TestBranch.message.toLowerCase().contains('conflict merging')
            assert e.errors.TestBranch.message.toLowerCase().contains('cube has changed')
            assertEquals("5CA932980E050E97E09543F8B79BE08696E0A1A4", e.errors.TestBranch.sha1)
            assertEquals("75EE6BA78989BD3563B9091FFF458E620FEAFDE8", e.errors.TestBranch.headSha1)
        }
    }

    @Test
    void testUpdateBranchWithItemThatWasChangedOnHeadAndInBranch()
    {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))
        assertEquals(2, NCubeManager.createBranch(branch2))

        NCube cube = NCubeManager.getCube(branch2, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(branch2, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch2)
        assertEquals(1, dtos.length)

        NCubeManager.commitBranch(branch2, dtos, USER_ID)

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : -10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(branch1, cube, USER_ID)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.updateBranch(branch1, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) updating branch")
            assert e.errors.TestBranch.message.toLowerCase().contains('cube')
            assert e.errors.TestBranch.message.toLowerCase().contains('changed in head')
            assertEquals("5CA932980E050E97E09543F8B79BE08696E0A1A4", e.errors.TestBranch.sha1)
            assertEquals("75EE6BA78989BD3563B9091FFF458E620FEAFDE8", e.errors.TestBranch.headSha1)
        }
    }

    @Test
    void testGetBranchChanges() throws Exception {
        // load cube with same name, but different structure in TEST branch
        preloadCubes(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch1, "TestBranch"))
        assertNull(NCubeManager.getCube(branch1, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch1))

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        //  test values on branch
        testValuesOnBranch(branch1)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch1, cube, USER_ID))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)


        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // commit the branch
        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        Object[] values = NCubeManager.commitBranch(branch1, dtos, USER_ID)

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch1, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch1, "TestAge").length)

        // both should be updated now.
        cube = NCubeManager.getCube(branch1, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(0, dtos.length)

        assertEquals(1, values.length);
    }

    @Test
    void testBootstrapWithOverrides() throws Exception {
        ApplicationID id = ApplicationID.getBootVersion('none', 'example')
        assertEquals(new ApplicationID('NONE', 'EXAMPLE', '0.0.0', ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD), id)

        preloadCubes(id, "sys.bootstrap.user.overloaded.json")

        NCube cube = NCubeManager.getCube(id, 'sys.bootstrap')
        // ensure properties are cleared
        System.setProperty('NCUBE_PARAMS', '')
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.28.0', 'SNAPSHOT', 'HEAD'), cube.getCell([env:'DEV']))
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.25.0', 'RELEASE', 'HEAD'), cube.getCell([env:'PROD']))
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.29.0', 'SNAPSHOT', 'baz'), cube.getCell([env:'SAND']))

        System.setProperty("NCUBE_PARAMS", '{"status":"RELEASE", "app":"UD", "tenant":"foo", "branch":"bar"}')
        assertEquals(new ApplicationID('foo', 'UD', '1.28.0', 'RELEASE', 'bar'), cube.getCell([env:'DEV']))
        assertEquals(new ApplicationID('foo', 'UD', '1.25.0', 'RELEASE', 'bar'), cube.getCell([env:'PROD']))
        assertEquals(new ApplicationID('foo', 'UD', '1.29.0', 'RELEASE', 'bar'), cube.getCell([env:'SAND']))

        System.setProperty("NCUBE_PARAMS", '{"branch":"bar"}')
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.28.0', 'SNAPSHOT', 'bar'), cube.getCell([env:'DEV']))
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.25.0', 'RELEASE', 'bar'), cube.getCell([env:'PROD']))
        assertEquals(new ApplicationID('NONE', 'UD.REF.APP', '1.29.0', 'SNAPSHOT', 'bar'), cube.getCell([env:'SAND']))
    }

    @Test
    public void testUserOverloadedClassPath() throws Exception {
        preloadCubes(appId, "sys.classpath.user.overloaded.json", "sys.versions.json")

        // Check DEV
        NCube cube = NCubeManager.getCube(appId, "sys.classpath")
        // ensure properties are cleared.
        System.setProperty('NCUBE_PARAMS', '')

        CdnClassLoader devLoader = cube.getCell([env:"DEV"]);
        assertEquals('https://www.foo.com/tests/ncube/cp1/public/', devLoader.URLs[0].toString())
        assertEquals('https://www.foo.com/tests/ncube/cp1/private/', devLoader.URLs[1].toString())
        assertEquals('https://www.foo.com/tests/ncube/cp1/private/groovy/', devLoader.URLs[2].toString())

        // Check INT
        CdnClassLoader intLoader = cube.getCell([env:"INT"]);
        assertEquals('https://www.foo.com/tests/ncube/cp2/public/', intLoader.URLs[0].toString())
        assertEquals('https://www.foo.com/tests/ncube/cp2/private/', intLoader.URLs[1].toString())
        assertEquals('https://www.foo.com/tests/ncube/cp2/private/groovy/', intLoader.URLs[2].toString())

        // Check with overload
        System.setProperty("NCUBE_PARAMS", '{"cpBase":"file://C:/Development/"}')

        // int loader is not marked as cached so we recreate this one each time.
        CdnClassLoader differentIntLoader = cube.getCell([env:"INT"]);
        assertNotSame(intLoader, differentIntLoader);
        assertEquals('file://C:/Development/public/', differentIntLoader.URLs[0].toString())
        assertEquals('file://C:/Development/private/', differentIntLoader.URLs[1].toString())
        assertEquals('file://C:/Development/private/groovy/', differentIntLoader.URLs[2].toString())

        // devLoader is marked as cached so we would get the same one until we clear the cache.
        URLClassLoader devLoaderAgain = cube.getCell([env:"DEV"]);
        assertSame(devLoader, devLoaderAgain)

        assertNotEquals('file://C:/Development/public/', devLoaderAgain.URLs[0].toString())
        assertNotEquals('file://C:/Development/private/', devLoaderAgain.URLs[1].toString())
        assertNotEquals('file://C:/Development/private/groovy/', devLoaderAgain.URLs[2].toString())

        //  force cube clear so it will auto next time we get cube
        NCubeManager.clearCache(appId);
        cube = NCubeManager.getCube(appId, "sys.classpath")
        devLoaderAgain = cube.getCell([env:"DEV"]);

        assertEquals('file://C:/Development/public/', devLoaderAgain.URLs[0].toString())
        assertEquals('file://C:/Development/private/', devLoaderAgain.URLs[1].toString())
        assertEquals('file://C:/Development/private/groovy/', devLoaderAgain.URLs[2].toString())

        // Check version overload only
        System.setProperty("NCUBE_PARAMS", '{"version":"1.28.0"}')
        // SAND hasn't been loaded yet so it should give us updated values based on the system params.
        URLClassLoader loader = cube.getCell([env:"SAND"]);
        assertEquals('https://www.foo.com/1.28.0/public/', loader.URLs[0].toString())
        assertEquals('https://www.foo.com/1.28.0/private/', loader.URLs[1].toString())
        assertEquals('https://www.foo.com/1.28.0/private/groovy/', loader.URLs[2].toString())
    }

    @Test
    public void testSystemParamsOverloads() throws Exception {
        preloadCubes(appId, "sys.classpath.system.params.user.overloaded.json", "sys.versions.2.json", "sys.resources.base.url.json")


        // Check DEV
        NCube cube = NCubeManager.getCube(appId, "sys.classpath")
        // ensure properties are cleared.
        System.setProperty('NCUBE_PARAMS', '')

        CdnClassLoader devLoader = cube.getCell([env:"DEV"]);
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/public/', devLoader.URLs[0].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/private/', devLoader.URLs[1].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/private/groovy/', devLoader.URLs[2].toString())

        // Check INT
        CdnClassLoader intLoader = cube.getCell([env:"INT"]);
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/public/', intLoader.URLs[0].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/private/', intLoader.URLs[1].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.31.0-SNAPSHOT/private/groovy/', intLoader.URLs[2].toString())

        // Check with overload


        cube = NCubeManager.getCube(appId, "sys.classpath")
        System.setProperty("NCUBE_PARAMS", '{"cpBase":"file://C:/Development/"}')

        // int loader is not marked as cached so we recreate this one each time.
        NCubeManager.systemParams = null;
        CdnClassLoader differentIntLoader = cube.getCell([env:"INT"]);

        assertNotSame(intLoader, differentIntLoader);
        assertEquals('file://C:/Development/public/', differentIntLoader.URLs[0].toString())
        assertEquals('file://C:/Development/private/', differentIntLoader.URLs[1].toString())
        assertEquals('file://C:/Development/private/groovy/', differentIntLoader.URLs[2].toString())

        // devLoader is marked as cached so we would get the same one until we clear the cache.
        URLClassLoader devLoaderAgain = cube.getCell([env:"DEV"]);
        assertSame(devLoader, devLoaderAgain)

        assertNotEquals('file://C:/Development/public/', devLoaderAgain.URLs[0].toString())
        assertNotEquals('file://C:/Development/private/', devLoaderAgain.URLs[1].toString())
        assertNotEquals('file://C:/Development/private/groovy/', devLoaderAgain.URLs[2].toString())

        //  force cube clear so it will auto next time we get cube
        NCubeManager.clearCache(appId);
        cube = NCubeManager.getCube(appId, "sys.classpath")
        devLoaderAgain = cube.getCell([env:"DEV"]);

        assertEquals('file://C:/Development/public/', devLoaderAgain.URLs[0].toString())
        assertEquals('file://C:/Development/private/', devLoaderAgain.URLs[1].toString())
        assertEquals('file://C:/Development/private/groovy/', devLoaderAgain.URLs[2].toString())

        // Check version overload only
        NCubeManager.clearCache(appId);
        NCubeManager.systemParams = null;
        System.setProperty("NCUBE_PARAMS", '{"version":"1.28.0"}')
        // SAND hasn't been loaded yet so it should give us updated values based on the system params.
        URLClassLoader loader = cube.getCell([env:"SAND"]);
        assertEquals('http://www.cedarsoftware.com/foo/1.28.0/public/', loader.URLs[0].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.28.0/private/', loader.URLs[1].toString())
        assertEquals('http://www.cedarsoftware.com/foo/1.28.0/private/groovy/', loader.URLs[2].toString())
    }

    @Test
    public void testClearCacheWithClassLoaderLoadedByCubeRequest() throws Exception {

        preloadCubes(appId, "sys.classpath.cp1.json", "GroovyMethodClassPath1.json")

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
    }

    @Test
    void testMultiCubeClassPath() throws Exception {

        preloadCubes(appId, "sys.classpath.base.json", "sys.classpath.json", "sys.status.json", "sys.versions.json", "sys.version.json", "GroovyMethodClassPath1.json")

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
    }

    // Ken: Can you lot at keeping this test, but doing it in terms of a non-sys.classpath cube?
    @Ignore
    void testTwoClasspathsSameAppId() throws Exception
    {
        preloadCubes(appId, "sys.classpath.2per.app.json", "GroovyExpCp1.json")

        assertEquals(0, NCubeManager.getCacheForApp(appId).size())
        NCube cube = NCubeManager.getCube(appId, "GroovyExpCp1")

        // classpath isn't loaded at this point.
        assertEquals(1, NCubeManager.getCacheForApp(appId).size())

        def input = [:]
        input.env = "a"
        input.state = "OH"
        def x = cube.getCell(input)
        assert 'Hello, world.' == x

        // GroovyExpCp1 and sys.classpath are now both loaded.
        assertEquals(2, NCubeManager.getCacheForApp(appId).size())

        input.env = "b"
        input.state = "TX"
        def y = cube.getCell(input)
        assert 'Goodbye, world.' == y

        // Test JsonFormatter - that it properly handles the URLClassLoader in the sys.classpath cube
        NCube cp1 = NCubeManager.getCube(appId, "sys.classpath")
        String json = cp1.toFormattedJson()

        NCube cp2 = NCube.fromSimpleJson(json)
        cp1.clearSha1()
        cp2.clearSha1()
        String json1 = cp1.toFormattedJson()
        String json2 = cp2.toFormattedJson()
        assertEquals(json1, json2)

        // Test HtmlFormatter - that it properly handles the URLClassLoader in the sys.classpath cube
        String html = cp1.toHtml()
        assert html.contains('http://www.cedarsoftware.com')
    }

    @Test
    void testMathControllerUsingExpressions() throws Exception
    {
        preloadCubes(appId, "sys.classpath.2per.app.json", "math.controller.json")

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
        preloadCubes(appId, "sys.classpath.cedar.json", "cedar.hello.json")

        Map input = new HashMap()
        NCube cube = NCubeManager.getCube(appId, 'hello')
        Object out = cube.getCell(input)
        assertEquals('Hello, world.', out)
        NCubeManager.clearCache(appId)

        cube = NCubeManager.getCube(appId, 'hello')
        out = cube.getCell(input)
        assertEquals('Hello, world.', out)
    }

    @Test
    void testMultiTenantApplicationIdBootstrap()
    {
        preloadCubes(appId, "sys.bootstrap.multi.api.json", "sys.bootstrap.version.json")

        def input = [:];
        input.env = "SAND";

        NCube cube = NCubeManager.getCube(appId, 'sys.bootstrap')
        Map<String, ApplicationID> map = cube.getCell(input)
        assertEquals(new ApplicationID("NONE", "APP", "1.15.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("A"))
        assertEquals(new ApplicationID("NONE", "APP", "1.19.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("B"))
        assertEquals(new ApplicationID("NONE", "APP", "1.28.0", "SNAPSHOT", ApplicationID.TEST_BRANCH), map.get("C"))

        input.env = "INT"
        map = cube.getCell(input)

        assertEquals(new ApplicationID("NONE", "APP", "1.25.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("A"))
        assertEquals(new ApplicationID("NONE", "APP", "1.26.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("B"))
        assertEquals(new ApplicationID("NONE", "APP", "1.27.0", "RELEASE", ApplicationID.TEST_BRANCH), map.get("C"))
    }

    @Test
    void testBootstrapWihMismatchedTenantAndAppForcesWarning() throws Exception
    {
        ApplicationID zero = new ApplicationID('FOO', 'TEST', '0.0.0', 'SNAPSHOT', 'HEAD')

        preloadCubes(zero, "sys.bootstrap.test.1.json")

        ApplicationID appId = NCubeManager.getApplicationID('FOO', 'TEST', null)
        // ensure cube on disk tenant and app are not loaded (saved as NONE and ncube.test
        assertEquals('FOO', appId.tenant);
        assertEquals('TEST', appId.app);
        assertEquals('1.28.0', appId.version);
        assertEquals('RELEASE', appId.status);
        assertEquals('HEAD', appId.branch);

        manager.removeBranches([zero, head] as ApplicationID[]);
    }



}
