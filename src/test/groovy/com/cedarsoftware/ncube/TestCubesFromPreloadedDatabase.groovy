package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.BranchMergeException
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.lang.reflect.Field
import java.sql.SQLException

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
            fail()
        } catch (CoordinateNotFoundException e) {
            assertTrue(e.getMessage().contains("not found"))
        }

        manager.removeCubes(appId)
    }

    @Test
    void testGetBranches() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code":-7]))
        cube = NCubeManager.getCube(head, "TestAge")
        assertEquals("youth", cube.getCell(["Code":5]))

        // load cube with same name, but different structure in TEST branch
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "kenny")
        loadCubesToDatabase(branch, "test.branch.2.json")

        // showing we only rely on tenant to get branches.
        assertEquals(2, NCubeManager.getBranches('NONE').size())
        assertEquals(2, NCubeManager.getBranches('NONE').size())

        ApplicationID branch2 = new ApplicationID('NONE', 'foo', '1.29.0', 'SNAPSHOT', 'someoneelse')
        loadCubesToDatabase(branch2, "test.branch.1.json", "test.branch.age.1.json")
        assertEquals(3, NCubeManager.getBranches('NONE').size())
        assertEquals(3, NCubeManager.getBranches('NONE').size())
        assertEquals(3, NCubeManager.getBranches('NONE').size())
    }

    @Test
    void testGetAppNames() throws Exception {
        ApplicationID app1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID app2 = new ApplicationID('NONE', "foo", "1.29.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID app3 = new ApplicationID('NONE', "bar", "1.29.0", "SNAPSHOT", ApplicationID.HEAD)
        loadCubesToDatabase(app1, "test.branch.1.json", "test.branch.age.1.json")
        loadCubesToDatabase(app2, "test.branch.1.json", "test.branch.age.1.json")
        loadCubesToDatabase(app3, "test.branch.1.json", "test.branch.age.1.json")

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

    }

    @Test
    void testCommitBranchOnCreatedCube() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        ApplicationID branch1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", USER_ID)
        NCubeManager.createCube(branch1, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch1, dtos, USER_ID)
        assertEquals(1, map.size())

        // ensure that there are no more branch changes after create
        dtos = NCubeManager.getBranchChangesFromDatabase(branch1);
        assertEquals(0, dtos.length);

        //map = NCubeManager.commitBranch(branch1, dtos, USER_ID)
        //assertEquals(1, map.size());

        ApplicationID headId = branch1.asHead();
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(headId, null).length)

    }

    @Test
    void testRollbackBranchWithPendingAdd() throws Exception {
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "branch")
        ApplicationID headId = branch.asHead()

        loadCubesToDatabase(headId, "test.branch.1.json");

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch, cube, 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        NCubeManager.rollbackBranch(branch, dtos)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertEquals(0, map.size())

        manager.removeCubes(branch)
        manager.removeCubes(headId)
    }

    @Test
    void testRollbackBranchWithDeletedCube() throws Exception {
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "branch")
        ApplicationID headId = branch.asHead()

        loadCubesToDatabase(headId, "test.branch.1.json");

        NCubeManager.createBranch(branch);

        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(headId, "*").length);
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(branch, "*").length);

        NCubeManager.deleteCube(branch, "TestBranch", USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        // undo delete
        NCubeManager.rollbackBranch(branch, dtos)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertEquals(0, map.size())

        manager.removeCubes(branch)
        manager.removeCubes(headId)
    }


    @Test
    void testCommitBranchOnCreateThenDeleted() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")

        ApplicationID branch1 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", USER_ID)
        NCubeManager.createCube(branch1, cube, 'kenny')
        NCubeManager.deleteCube(branch1, "TestAge", 'kenny')

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch1)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch1, dtos, USER_ID)
        assertEquals(0, map.size())

        ApplicationID headId = branch1.asHead()
        assertEquals(0, NCubeManager.getCubeRecordsFromDatabase(headId, null).length)

        manager.removeCubes(branch1)
        manager.removeCubes(headId)
    }

    @Test
    void testCreateBranch() throws Exception
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        testValuesOnBranch(head)

        def cube1Sha1 = NCubeManager.getCube(head, "TestBranch").sha1()
        def cube2Sha1 = NCubeManager.getCube(head, "TestAge").sha1()

        assertNull(NCubeManager.getCube(head, "TestBranch").getHeadSha1())
        assertNull(NCubeManager.getCube(head, "TestAge").getHeadSha1())

        assertEquals(2, NCubeManager.createBranch(branch))

        assertEquals(cube1Sha1, NCubeManager.getCube(branch, "TestBranch").sha1())
        assertEquals(cube1Sha1, NCubeManager.getCube(branch, "TestBranch").getHeadSha1())
        assertEquals(cube2Sha1, NCubeManager.getCube(branch, "TestAge").sha1())
        assertEquals(cube2Sha1, NCubeManager.getCube(branch, "TestAge").getHeadSha1())

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranchWithItemCreatedInBranchOnly() throws Exception
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.createBranch(branch))

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertNull(NCubeManager.getCube(branch, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestAge"))

        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        assertNotNull(cube)
        NCubeManager.createCube(branch, cube, "jdirt")


        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)
        assertNull(NCubeManager.getCube(head, "TestAge"))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        assertTrue(!map.isEmpty())

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testUpdateBranchWithUpdateOnBranch() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        //  test values on branch
        testValuesOnBranch(branch)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // commit the branch
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        assertEquals(0, NCubeManager.updateBranch(branch).length)

        // shouldn't have changed
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }



    @Test
    void testCommitBranchOnUpdate() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        //  test values on branch
        testValuesOnBranch(branch)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // commit the branch
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertTrue(!map.isEmpty())


        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // both should be updated now.
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranchOnUpdateWithOldInvalidSha1() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk("test.branch.1.json", "test.branch.age.1.json")



        Field f = NCube.class.getDeclaredField("sha1")
        f.setAccessible(true);
        f.set(cubes[0], "F00F00F00F00F00");

        //assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        //assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        manager.addCubes(head, USER_ID, cubes);

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertTrue(map.isEmpty())

        NCube cube = NCubeManager.getCube(branch, "TestBranch")
        cube.removeCell([Code:10.0])

        NCubeManager.updateCube(branch, cube, USER_ID);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // No changes have happened yet, even though sha1 is incorrect,
        // we just copy the sha1 when we create the branch so the headsha1 won't
        // differ until we make a change.
        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertTrue(!map.isEmpty())

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranchWithUpdateAndWrongRevisionNumber() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)
        ((NCubeInfoDto)dtos[0]).revision = Long.toString(100)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)
        assertTrue(!map.isEmpty())

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }



    @Test
    void testRollback() throws Exception
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        //  test values on branch
        testValuesOnBranch(branch)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)

        cube.setCell("FOO", [Code : 10.0])
        assertEquals(3, cube.getCellMap().size())
        assertEquals("FOO", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))
        assertEquals(3, NCubeManager.getRevisionHistory(branch, "TestBranch").length)

        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))
        assertEquals(4, NCubeManager.getRevisionHistory(branch, "TestBranch").length)

        cube.setCell("FOO", [Code : 10.0])
        assertEquals(3, cube.getCellMap().size())
        assertEquals("FOO", cube.getCell([Code : 10.0]))

        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))
        assertEquals(5, NCubeManager.getRevisionHistory(branch, "TestBranch").length)

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        assertEquals(1, NCubeManager.rollbackBranch(branch, dtos))

        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranchOnDelete() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        //  test values on branch
        testValuesOnBranch(branch)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.deleteCube(branch, "TestBranch", USER_ID))

        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // cube is deleted
        assertNull(NCubeManager.getCube(branch, "TestBranch"))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch though only TestBranch has changed.
        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // both should be updated now.
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(head, "TestBranch"))

        assertTrue(!map.isEmpty())

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCreateBranchThatAlreadyExists() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        //1) should work
        NCubeManager.createBranch(branch)

        try {
            //2) should already be created.
            NCubeManager.createBranch(branch)
            fail()
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("already exists"))
        }

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testReleaseCubes() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.2.json")
        assertNotNull(cube)
        NCubeManager.updateCube(branch, cube, "jdirt")

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size())
        testValuesOnBranch(branch, "FOO")

        assertEquals(2, NCubeManager.releaseCubes(head, "1.29.0"))

        assertNull(NCubeManager.getCube(branch, "TestAge"))
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(head, "TestAge"))
        assertNull(NCubeManager.getCube(head, "TestBranch"))

        head = new ApplicationID('NONE', "test", "1.29.0", "SNAPSHOT", ApplicationID.HEAD)
        branch = new ApplicationID('NONE', "test", "1.29.0", "SNAPSHOT", "FOO")

        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").size())

        head = new ApplicationID('NONE', "test", "1.28.0", "RELEASE", ApplicationID.HEAD)

        testValuesOnBranch(head)
        testValuesOnBranch(branch, "FOO")

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }


    @Test
    void testDuplicateCubeChanges() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")
        ApplicationID branch2 = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "BAR")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        NCubeManager.duplicate(head, branch2, "TestBranch", "TestBranch2", USER_ID);
        NCubeManager.duplicate(head, branch2, "TestAge", "TestAge", USER_ID);

        // assert head and branch are still there
        testValuesOnBranch(head);
        testValuesOnBranch(branch);

        //  Test with new name.
        NCube cube = NCubeManager.getCube(branch2, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(branch2, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))

        manager.removeCubes(branch)
        manager.removeCubes(branch2)
        manager.removeCubes(head)
    }


    @Test
    void testRenameCubeWhenNewNameAlreadyExists() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        try
        {
            NCubeManager.renameCube(branch, "TestBranch", "TestAge", USER_ID);
            fail();
        }
        catch (RuntimeException e)
        {
            assertTrue(e.message.contains("Unable to rename"))
            assertTrue(e.getCause() instanceof SQLException)

        }

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }


    @Test
    void testRenameCubeBasicCase() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

        assertTrue(NCubeManager.renameCube(branch, "TestBranch", "TestBranch2", USER_ID));

        testValuesOnBranch(head);

        //  Test with new name.
        NCube cube = NCubeManager.getCube(branch, "TestBranch2")
        assertEquals("ABC", cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(branch, "TestAge")
        assertEquals("youth", cube.getCell(["Code": 5]))
        assertNull(NCubeManager.getCube(branch, "TestBranch"))

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testRenameCubeThenRollback() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        testValuesOnBranch(head)

        assertEquals(2, NCubeManager.createBranch(branch))

        testValuesOnBranch(head)
        testValuesOnBranch(branch)

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
            NCubeManager.getRevisionHistory(branch, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch, null).length);

        assertTrue(NCubeManager.renameCube(branch, "TestBranch", "TestBranch2", USER_ID));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch, null).length);


        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }


        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.rollbackBranch(branch, dtos));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(branch, null).length);

        assertEquals(0, NCubeManager.getBranchChangesFromDatabase(branch).length);

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
            NCubeManager.getRevisionHistory(branch, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }


        assertTrue(NCubeManager.renameCube(branch, "TestBranch", "TestBranch2", USER_ID));

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch, null).length);

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch2").length);

        try {
            NCubeManager.getRevisionHistory(head, "TestBranch2");
            fail();
        } catch (IllegalArgumentException e) {
        }

        dtos = NCubeManager.getBranchChangesFromDatabase(branch);
        assertEquals(2, dtos.length);

        assertEquals(2, NCubeManager.commitBranch(branch, dtos, USER_ID).size());

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch2").length);

        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length);
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length);
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch2").length);

        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(head, null).length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(branch, null).length);

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }




    private void testValuesOnBranch(ApplicationID appId, String code1 = "ABC", String code2 = "youth") {
        NCube cube = NCubeManager.getCube(appId, "TestBranch")
        assertEquals(code1, cube.getCell(["Code": -7]))
        cube = NCubeManager.getCube(appId, "TestAge")
        assertEquals(code2, cube.getCell(["Code": 5]))
    }


    @Test
    void testCommitBranchWithItemCreatedLocallyAndOnHead() {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID preemptiveBranch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "PREEMPT")
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(1, NCubeManager.createBranch(branch))
        assertEquals(1, NCubeManager.createBranch(preemptiveBranch))

        NCube cube = NCubeManager.getNCubeFromResource("test.branch.age.2.json")
        NCubeManager.createCube(preemptiveBranch, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(preemptiveBranch)
        assertEquals(1, dtos.length)
        NCubeManager.commitBranch(preemptiveBranch, dtos, USER_ID)


        cube = NCubeManager.getNCubeFromResource("test.branch.age.1.json")
        NCubeManager.createCube(branch, cube, USER_ID)



        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.commitBranch(branch, dtos, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assert e.message.toLowerCase().contains("conflict(s) committing branch")
            assert !e.errors.isEmpty()
        }

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testCommitBranchWithItemThatWasChangedOnHeadAndInBranch()
    {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID preemptiveBranch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "PREEMPT")
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))
        assertEquals(2, NCubeManager.createBranch(preemptiveBranch))

        NCube cube = NCubeManager.getCube(preemptiveBranch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(preemptiveBranch, cube, USER_ID)

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(preemptiveBranch)
        assertEquals(1, dtos.length)

        NCubeManager.commitBranch(preemptiveBranch, dtos, USER_ID)

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        cube.removeCell([Code : -10.0])
        assertEquals(2, cube.getCellMap().size())
        NCubeManager.updateCube(branch, cube, USER_ID)

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        try
        {
            NCubeManager.commitBranch(branch, dtos, USER_ID)
            fail()
        }
        catch (BranchMergeException e)
        {
            assertTrue(e.message.toLowerCase().contains("conflict(s) committing branch"))
            assertTrue(!e.errors.isEmpty())
        }

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testGetBranchChanges() throws Exception {
        ApplicationID head = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", ApplicationID.HEAD)
        ApplicationID branch = new ApplicationID('NONE', "test", "1.28.0", "SNAPSHOT", "FOO")

        // load cube with same name, but different structure in TEST branch
        loadCubesToDatabase(head, "test.branch.1.json", "test.branch.age.1.json")

        // cubes were preloaded
        testValuesOnBranch(head)

        // pre-branch, cubes don't exist
        assertNull(NCubeManager.getCube(branch, "TestBranch"))
        assertNull(NCubeManager.getCube(branch, "TestAge"))

        NCube cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())

        //  create the branch (TestAge, TestBranch)
        assertEquals(2, NCubeManager.createBranch(branch))

        Object[] dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        //  test values on branch
        testValuesOnBranch(branch)

        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        // edit branch cube
        cube.removeCell([Code : 10.0])
        assertEquals(2, cube.getCellMap().size())

        // default now gets loaded
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // update the new edited cube.
        assertTrue(NCubeManager.updateCube(branch, cube, USER_ID))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)


        // Only Branch "TestBranch" has been updated.
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // commit the branch
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals(2, cube.getCellMap().size())
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        // check head hasn't changed.
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals(3, cube.getCellMap().size())
        assertEquals("GHI", cube.getCell([Code : 10.0]))

        //  loads in both TestAge and TestBranch through only TestBranch has changed.
        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(1, dtos.length)

        Map map = NCubeManager.commitBranch(branch, dtos, USER_ID)

        assertEquals(2, NCubeManager.getRevisionHistory(head, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(head, "TestAge").length)
        assertEquals(2, NCubeManager.getRevisionHistory(branch, "TestBranch").length)
        assertEquals(1, NCubeManager.getRevisionHistory(branch, "TestAge").length)

        // both should be updated now.
        cube = NCubeManager.getCube(branch, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))
        cube = NCubeManager.getCube(head, "TestBranch")
        assertEquals("ZZZ", cube.getCell([Code : 10.0]))

        dtos = NCubeManager.getBranchChangesFromDatabase(branch)
        assertEquals(0, dtos.length)

        assertTrue(!map.isEmpty())

        manager.removeCubes(branch)
        manager.removeCubes(head)
    }

    @Test
    void testBootstrapWithOverrides() throws Exception {
        ApplicationID id = ApplicationID.getBootVersion('none', 'example')
        assertEquals(new ApplicationID('NONE', 'EXAMPLE', '0.0.0', ReleaseStatus.SNAPSHOT.name(), ApplicationID.HEAD), id)

        loadCubesToDatabase(id, "sys.bootstrap.user.overloaded.json")

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

        manager.removeCubes(appId)
    }

    @Test
    public void testUserOverloadedClassPath() throws Exception {
        loadCubesToDatabase(appId, "sys.classpath.user.overloaded.json", "sys.versions.json")

        // Check DEV
        NCube cube = NCubeManager.getCube(appId, "sys.classpath")
        // ensure properties are cleared.
        System.setProperty('NCUBE_PARAMS', '')
        assertEquals(['https://www.foo.com/tests/ncube/cp1/public/', 'https://www.foo.com/tests/ncube/cp1/private/', 'https://www.foo.com/tests/ncube/cp1/private/groovy/'], cube.getCell([env:"DEV"]))

        // Check INT
        assertEquals(['https://www.foo.com/tests/ncube/cp2/public/', 'https://www.foo.com/tests/ncube/cp2/private/', 'https://www.foo.com/tests/ncube/cp2/private/groovy/'], cube.getCell([env:"INT"]))

        // Check with overload
        System.setProperty("NCUBE_PARAMS", '{"cpBase":"C:/Development/"}')
        assertEquals(['C:/Development/public/', 'C:/Development/private/', 'C:/Development/private/groovy/'], cube.getCell([env:"DEV"]))
        assertEquals(['C:/Development/public/', 'C:/Development/private/', 'C:/Development/private/groovy/'], cube.getCell([env:"INT"]))

        // Check version overload only
        System.setProperty("NCUBE_PARAMS", '{"version":"1.28.0"}')
        assertEquals(['https://www.foo.com/1.28.0/public/', 'https://www.foo.com/1.28.0/private/', 'https://www.foo.com/1.28.0/private/groovy/'], cube.getCell([env:"DEV"]))


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

    // Ken: Can you lot at keeping this test, but doing it in terms of a non-sys.classpath cube?
    @Ignore
    void testTwoClasspathsSameAppId() throws Exception
    {
        loadCubesToDatabase(appId, "sys.classpath.2per.app.json", "GroovyExpCp1.json")

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
        NCube cube = NCubeManager.getCube(appId, 'hello')
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


        // remove cubes for this test.
        manager.removeCubes(appId)
    }

    private loadCubesToDatabase(ApplicationID id, String ...names) {
        manager.addCubes(id, USER_ID, TestingDatabaseHelper.getCubesFromDisk(names))
    }
}
