package com.cedarsoftware.ncube

import com.cedarsoftware.util.IOUtilities
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

import static org.junit.Assert.*
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

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
class TestNCubeJdbcPersister
{
    static final String APP_ID = "ncube.test";
    static final String USER_ID = "jdirt";

    private ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

    @Before
    void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    void testSelectCubesStatementWithActiveOnlyAndDeletedOnlyBothSetToTrue()
    {
        try
        {
            new NCubeJdbcPersister().createSelectCubesStatement(null, null, null, false, true, true, true, false);
            fail();
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("cannot both be true"));
        }
    }

    @Test
    void testDbApis() throws Exception
    {
        NCubePersister persister = TestingDatabaseHelper.persister

        NCube ncube1 = NCubeBuilder.testNCube3D_Boolean
        NCube ncube2 = NCubeBuilder.getTestNCube2D(true)

        persister.createCube(defaultSnapshotApp, ncube1, USER_ID)
        persister.createCube(defaultSnapshotApp, ncube2, USER_ID)

        Object[] cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%", true)

        assertTrue(cubeList != null)
        assertTrue(cubeList.length == 2)

        assertTrue(ncube1.numDimensions == 3)
        assertTrue(ncube2.numDimensions == 2)

        ncube1.deleteAxis("bu")
        ApplicationID next = defaultSnapshotApp.createNewSnapshotId("0.2.0")
        persister.updateCube(defaultSnapshotApp, ncube1, USER_ID)
        int numRelease = persister.releaseCubes(defaultSnapshotApp, "0.2.0")
        assertEquals(0, numRelease)

        cubeList = NCubeManager.getCubeRecordsFromDatabase(next, 'test.%')
        // Two cubes at the new 1.2.3 SNAPSHOT version.
        assert cubeList.length == 2

        // Verify that you cannot delete a RELEASE ncube
        try
        {
            assertFalse(persister.deleteCube(defaultSnapshotApp, ncube1.name, false, USER_ID))
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("delete"))
            assertTrue(e.message.contains("nable"))
            assertTrue(e.message.contains("find"))
        }
        try
        {
            assertFalse(persister.deleteCube(defaultSnapshotApp, ncube2.name, false, USER_ID))
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("delete"))
            assertTrue(e.message.contains("nable"))
            assertTrue(e.message.contains("find"))
        }

        // Delete new SNAPSHOT cubes
        assertTrue(persister.deleteCube(next, ncube1.name, false, USER_ID))
        assertTrue(persister.deleteCube(next, ncube2.name, false, USER_ID))

        // Ensure that all test ncubes are deleted
        cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%", true)
        assertTrue(cubeList.length == 0)
    }

    @Test
    void testDoesCubeExistWithException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().doesCubeExist(c, defaultSnapshotApp, "name")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testDoCubesExistWithException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().doCubesExist(c, defaultSnapshotApp, true)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("rror"))
            assertTrue(e.message.contains("check"))
            assertTrue(e.message.contains("exist"))
            assertTrue(e.message.contains("cube"))
        }
    }

    @Test
    void testGetAppNamesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppNames(c, defaultSnapshotApp.DEFAULT_TENANT, 'SNAPSHOT', ApplicationID.TEST_BRANCH)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testGetMinRevisionWithSqlException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getMinRevision(c, defaultSnapshotApp, "foo");
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testUpdateCubeWithSqlException() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().updateCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("nable"))
            assertTrue(e.message.contains("ube"))
        }
    }

    @Test
    void testGetAppVersionsWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppVersions(c, "DEFAULT", "FOO", "SNAPSHOT", "BRANCH")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testDoesCubeExistWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().doesCubeExist(c, defaultSnapshotApp, "name")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("rror"))
            assertTrue(e.message.contains("check"))
            assertTrue(e.message.contains("cube"))
        }
    }

    @Test
    void testDoReleaseCubesExist() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().doReleaseCubesExist(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("rror"))
            assertTrue(e.message.contains("check"))
            assertTrue(e.message.contains("exis"))
            assertTrue(e.message.contains("cube"))
        }
    }

    @Test
    void testUpdateBranchCube() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().updateBranchCube(c, 0, defaultSnapshotApp, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.toLowerCase().contains("unable"))
            assertTrue(e.message.contains("update cube"))
        }
    }

    @Test
    void testUpdateBranchThatFailsToUpdate() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(0);
        when(rs.next()).thenReturn(true).thenReturn(true)
        when(rs.getLong(1)).thenReturn(5L)
        when(rs.getDate(anyString())).thenReturn(new java.sql.Date(System.currentTimeMillis()))

        try
        {
            new NCubeJdbcPersister().updateBranchCube(c, 0, defaultSnapshotApp, USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("unable"))
            assertTrue(e.message.contains("update cube"))
        }
    }

    @Test
    void testUpdateBranchThatIsNotFound() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        when(rs.getLong(1)).thenReturn(5L)
        when(rs.getDate(anyString())).thenReturn(new java.sql.Date(System.currentTimeMillis()))

        assertNull(new NCubeJdbcPersister().updateBranchCube(c, 0, defaultSnapshotApp, USER_ID))
    }

    @Test
    void testUpdateTestDataWithSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(1)).thenReturn(5L)

        try
        {
            new NCubeJdbcPersister().updateTestData(c, defaultSnapshotApp, "foo", "test data")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("Unable to update test data"))
        }
    }

    @Test
    void testChangeVersionValueWithSqlException() throws Exception
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false)

        try
        {
            new NCubeJdbcPersister().changeVersionValue(c, defaultSnapshotApp, "1.1.1")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to change"))
        }
    }

    @Test
    void testReleaseCubesWhereReleaseCubesDontExist() throws Exception
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("already exists"))
        }
    }

    @Test
    void testReleaseCubesWithSQLExceptionOnMovingOfBranchCubes() throws Exception
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (RuntimeException e)
        {
            println e.message
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to move"))
        }
    }

    @Test
    void testReleaseCubesWithSQLExceptionOnRelease() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (RuntimeException e)
        {
            println e.message
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to release"))
        }
    }

    @Test
    void testReleaseCubesWithSQLExceptionWhileCreatingNewSnapshot() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to create"))
        }
    }

    @Test
    void testReleaseCubesWithRuntimeExceptionWhileCreatingNewSnapshot() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenThrow(NullPointerException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (NullPointerException e)
        {
            assertEquals(e.message, null)
        }
    }

    @Test
    void testLoadCubesWithInvalidCube() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true).thenReturn(false)
        when(rs.getString("n_cube_nm")).thenReturn("foo")
        when(rs.getString("branch_id")).thenReturn("HEAD")
        when(rs.getBytes("cube_value_bin")).thenReturn("[                                                     ".getBytes("UTF-8"))

        Object[] nCubes = new NCubeJdbcPersister().getCubeRecords(c, defaultSnapshotApp, "", true)
        assertNotNull(nCubes)
        assertEquals(1, nCubes.length)     // Won't know it fails until getCube() is called.
    }

    @Test
    void testLoadCubesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        NCubeInfoDto dto = new NCubeInfoDto()
        dto.tenant = ApplicationID.DEFAULT_TENANT
        dto.app = ApplicationID.DEFAULT_APP
        dto.version = ApplicationID.DEFAULT_VERSION
        dto.status = 'SNAPSHOT'
        dto.name = 'foo'
        dto.branch = ApplicationID.HEAD

        try
        {
            new NCubeJdbcPersister().loadCube(c, 0)
            fail()
        }
        catch (RuntimeException e)
        {
            assert e.message.toLowerCase().contains("unable to load")
        }
    }

    @Test
    void testChangeVersionWithNoUpdate() throws Exception
    {
        NCubeBuilder.getTestNCube2D(true)

        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        when(ps.executeUpdate()).thenReturn(0)
        try
        {
            new NCubeJdbcPersister().changeVersionValue(c, defaultSnapshotApp, "1.1.1")
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains(" no "))
            assertTrue(e.message.contains("update"))
        }
    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    void testUpdateNotesWithDuplicateCubeUpdated() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(2)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateNotes(c, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH), "foo", "notes")
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("exist"))
        }
    }

    @Test
    void testUpdateNotesWithNoCubesUpdated() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateNotes(c, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH), "foo", "notes")
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("exist"))
        }
    }

    @Test
    void testUpdateNotesWithSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(1)).thenReturn(5L)

        try
        {
            new NCubeJdbcPersister().updateNotes(c, defaultSnapshotApp, "foo", "test data")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("Unable to update notes"))
        }
    }

    @Test
    void testGetTestDataWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getTestData(c, defaultSnapshotApp, "foo")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch"))
            assertTrue(e.message.contains("test data"))
        }
    }

    @Test
    void testGetNotesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getNotes(c, defaultSnapshotApp, "foo")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch"))
            assertTrue(e.message.contains("notes"))
        }
    }

    @Test
    void testRollbackBranchWithSqlException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(0)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(5L);

        try
        {
            NCubeInfoDto[] dtos = new NCubeInfoDto[1];
            dtos[0] = new NCubeInfoDto()
            dtos[0].name = "foo";
            dtos[0].headSha1 = "F0F0F0F0"

            new NCubeJdbcPersister().rollbackBranch(c, defaultSnapshotApp, dtos)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to rollback cube"))
        }
    }

    @Test
    void testGetMaxRevisionWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getMaxRevision(c, defaultSnapshotApp, "foo")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to get"))
            assertTrue(e.message.contains("revision number"))
        }
    }

    @Test
    void testCommitCubeWithInvalidRevision() throws Exception
    {
        try
        {
            new NCubeJdbcPersister().commitCube(null, null, defaultSnapshotApp.asHead(), USER_ID)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("cannot be null"))
        }
    }

    @Test
    void testCommitCubeThatThrowsSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().commitCube(c, 1L, defaultSnapshotApp.asHead(), USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("Unable"))
            assertTrue(e.message.contains("commit"))
        }
    }

    @Test
    void testCommitCubeThatDoesntUpdateCorrectly() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(0)
        when(rs.next()).thenReturn(true)
        when(rs.getBytes("cube_value_bin")).thenReturn("".getBytes("UTF-8"))

        try
        {
            new NCubeJdbcPersister().commitCube(c, 1L, defaultSnapshotApp.asHead(), USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Unable to commit"))
        }
    }

    @Test
    void testCommitCubeThatThrowsExceptionOnAdd() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(1).thenReturn(0);
        when(rs.next()).thenReturn(true)
        when(rs.getBytes("cube_value_bin")).thenReturn("".getBytes("UTF-8"))

        try
        {
            new NCubeJdbcPersister().commitCube(c, 1L, defaultSnapshotApp.asHead(), USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("error"))
            assertTrue(e.message.contains("updating"))
            assertTrue(e.message.contains("not updated"))
        }
    }

    @Test
    void testCommitCubeThatDoesntExist() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(1).thenReturn(0);
        when(rs.next()).thenReturn(false)
        when(rs.getBytes("cube_value_bin")).thenReturn("".getBytes("UTF-8"))

        assertNull(new NCubeJdbcPersister().commitCube(c, 1L, defaultSnapshotApp.asHead(), USER_ID))
    }



    @Test
    void testGetNCubesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getCubeRecords(c, defaultSnapshotApp, null, true)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch"))
        }
    }

    @Test
    void testGetChangedRecordsWithSqlException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getChangedRecords(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch"))
        }
    }

    @Test
    void testCreateBranchWithNullPointerException() throws Exception
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false, NullPointerException.class)

        try
        {
            new NCubeJdbcPersister().createBranch(c, defaultSnapshotApp)
            fail()
        }
        catch (NullPointerException e)
        {
        }
    }


    @Test
    void testCreateBranchWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false)

        try
        {
            new NCubeJdbcPersister().createBranch(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to create"))
        }
    }

    @Test
    void testGetBranchesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getBranches(c, 'NONE')
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch all branches"))
        }
    }

    @Test
    void testDeleteCubesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().deleteBranch(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to delete branch"))
        }
    }

    private static getConnectionThatThrowsSQLException = { ->
        Connection c = mock(Connection.class)
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class)
        return c;
    }

    private static Connection getConnectionThatThrowsExceptionAfterExistenceCheck(boolean exists, Class exceptionClass = SQLException.class) throws SQLException
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(exceptionClass)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(exists)
        return c;
    }

    @Test
    void testCreateCubeWithSqlException() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false)
        try
        {
            new NCubeJdbcPersister().createCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to insert"))
        }
    }

    @Test
    void testCreateCubeWhenOneAlreadyExists() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)

        try
        {
            new NCubeJdbcPersister().createCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch(IllegalStateException e)
        {
            assertTrue(e.message.toLowerCase().contains("cube"))
            assertTrue(e.message.toLowerCase().contains("already exists"))
        }
    }

    @Test
    void testCreateCubeThatDoesntCreateCube() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)

        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        when(ps.executeUpdate()).thenReturn(0)

        try
        {
            new NCubeJdbcPersister().createCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("error inserting"))
        }
    }

    @Test
    void testDeleteCubeWithSQLException() throws Exception
    {

        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(1)).thenReturn(5L)

        try
        {
            new NCubeJdbcPersister().deleteCube(c, defaultSnapshotApp, "foo", true, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }


    @Test
    void testRenameCubeThatThrowsSQLEXception() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().renameCube(c, defaultSnapshotApp, "foo", "bar", USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testGetDeletedCubeRecordsThatThrowsSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getDeletedCubeRecords(c, defaultSnapshotApp, null)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testGetRevisionsThatThrowsSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenThrow(SQLException.class)

        try
        {
            new NCubeJdbcPersister().getRevisions(c, defaultSnapshotApp, "foo")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testRestoreCubeThatThrowsSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyString())).thenReturn(new Long(-9))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes(anyString())).thenReturn(out.toByteArray())
        when(rs.getBytes(anyInt())).thenReturn(null)

        try
        {
            new NCubeJdbcPersister().restoreCube(c, defaultSnapshotApp, "foo", USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.toLowerCase().contains("unable to restore cube"))
        }
    }

    @Test
    void testRestoreCubeThatFailsTheUpdate()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyString())).thenReturn(new Long(-9))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes(anyString())).thenReturn(out.toByteArray())
        when(rs.getBytes(anyInt())).thenReturn(null)
        when(ps.executeUpdate()).thenReturn(0)

        try
        {
            new NCubeJdbcPersister().restoreCube(c, defaultSnapshotApp, "foo", USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Could not restore"))
        }
    }

    @Test
    void testDeleteCubeThatThrowsSQLException()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(9))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes(anyString())).thenReturn(out.toByteArray())
        when(rs.getBytes(anyInt())).thenReturn(null)
        when(rs.getString("status_cd")).thenReturn(ReleaseStatus.SNAPSHOT.name())
        when(rs.getString("n_cube_nm")).thenReturn("foo")
        when(rs.getString("branch_id")).thenReturn(ApplicationID.HEAD)

        try
        {
            new NCubeJdbcPersister().deleteCube(c, defaultSnapshotApp, "foo", false, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.contains("Unable to delete cube"))
        }
    }

    @Test
    void testDeleteCubeThatFailsTheUpdate()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(9))
        when(rs.getBytes(anyString())).thenReturn("foo".getBytes("UTF-8"))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(ps.executeUpdate()).thenReturn(0)

        try
        {
            new NCubeJdbcPersister().deleteCube(c, defaultSnapshotApp, "foo", false, USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Cannot delete"))
            assertTrue(e.message.contains("not deleted"))
        }
    }

    @Test
    void testCreateCubeWithWrongUpdateCount()
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        when(rs.getLong(anyInt())).thenReturn(new Long(-9))
        when(ps.executeUpdate()).thenReturn(0)
        try
        {
            new NCubeJdbcPersister().createCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertTrue(e.message.contains("ube"))
            assertTrue(e.message.contains("Unable to insert"))
        }
    }

    @Test
    void testUpdateBranchCubeWithNull()
    {
        try
        {
            new NCubeJdbcPersister().updateBranchCube(null, null, null, null);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains('cannot be null'));
        }
    }

    @Test
    void testUpdateCubeWithWrongUpdateCount()
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(rs.next()).thenReturn(true)
        when(rs.getDate(anyString())).thenReturn(new java.sql.Date(System.currentTimeMillis()));
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateCube(c, defaultSnapshotApp, ncube, USER_ID)
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("error updating"))
            assertTrue(e.message.contains("not updated"))
        }
    }


    @Test
    void testRenameCubeThatDidntUpdateBecauseOfDatabaseError()
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.getLong("revision_number")).thenReturn(0L);
        when(rs.next()).thenReturn(true).thenReturn(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes("cube_value_bin")).thenReturn(out.toByteArray())

        try
        {
            new NCubeJdbcPersister().renameCube(c, defaultSnapshotApp, "foo", "bar", USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Unable to rename cube"))
        }

    }

    @Test
    void testRenameCubeThatDidntUpdateBecauseOfDatabaseError2()
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(1).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.getLong("revision_number")).thenReturn(0L);
        when(rs.next()).thenReturn(true).thenReturn(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes("cube_value_bin")).thenReturn(out.toByteArray())

        try
        {
            new NCubeJdbcPersister().renameCube(c, defaultSnapshotApp, "foo", "bar", USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Unable to rename cube"))
        }

    }

    @Test
    void testDuplicateCubeThatDidntUpdate()
    {
        ApplicationID head = defaultSnapshotApp.asHead();

        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.getLong("revision_number")).thenReturn(0L);
        when(rs.next()).thenReturn(true).thenReturn(false);
        try
        {
            new NCubeJdbcPersister().duplicateCube(c, head, defaultSnapshotApp, "name", "name", USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Unable to duplicate cube"))
        }

    }

    @Test
    void testDuplicateCubeWithSqlException()
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().duplicateCube(c, defaultSnapshotApp, defaultSnapshotApp, "name", "foo", USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }

    }

    @Test
    void testUpdateTestDataWithDuplicateCubes()
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.isValid(anyInt())).thenReturn(true)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(2)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateTestData(c, defaultSnapshotApp, "foo", "foo")
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("test"))
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("exist"))
        }
    }

    @Test
    void testReleaseCubesWithCubeThatExistsAlready()
    {
        NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("already exists"))
        }
    }

    @Test
    void testChangeVersionWhenCubeAlreadyExists()
    {
        NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)
        try
        {
            new NCubeJdbcPersister().changeVersionValue(c, defaultSnapshotApp, "1.1.1")
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("already"))
            assertTrue(e.message.contains("exist"))
        }
    }
}
