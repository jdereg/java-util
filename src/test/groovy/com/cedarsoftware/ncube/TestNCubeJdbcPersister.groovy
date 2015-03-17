package com.cedarsoftware.ncube

import com.cedarsoftware.util.IOUtilities
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
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
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }


    @Test
    void testDbApis() throws Exception
    {
        NCubePersister persister = TestingDatabaseHelper.persister

        NCube ncube1 = NCubeBuilder.testNCube3D_Boolean
        NCube ncube2 = NCubeBuilder.getTestNCube2D(true)

        persister.createCube(defaultSnapshotApp, ncube1, USER_ID)
        persister.createCube(defaultSnapshotApp, ncube2, USER_ID)

        Object[] cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%")

        assertTrue(cubeList != null)
        assertTrue(cubeList.length == 2)

        assertTrue(ncube1.numDimensions == 3)
        assertTrue(ncube2.numDimensions == 2)

        ncube1.deleteAxis("bu")
        ApplicationID next = defaultSnapshotApp.createNewSnapshotId("0.2.0");
        persister.updateCube(defaultSnapshotApp, ncube1, USER_ID)
        int numRelease = persister.releaseCubes(defaultSnapshotApp, "0.2.0")
        assertEquals(0, numRelease)

        cubeList = NCubeManager.getCubeRecordsFromDatabase(next, 'test.%')
        // Two cubes at the new 1.2.3 SNAPSHOT version.
        assert cubeList.length == 2

//        String notes1 = persister.getNotes(next, "test.ValidTrailorConfigs")
//        String notes2 = persister.getNotes(next, "test.ValidTrailorConfigs")
//
//        persister.updateNotes(next, "test.ValidTrailorConfigs", null)
//        notes1 = persister.getNotes(next, "test.ValidTrailorConfigs")
//        assertTrue("".equals(notes1))
//
//        persister.updateNotes(next, "test.ValidTrailorConfigs", "Trailer Config Notes")
//        notes1 = persister.getNotes(next, "test.ValidTrailorConfigs")
//        assertTrue("Trailer Config Notes".equals(notes1))
//
//        persister.updateTestData(next, "test.ValidTrailorConfigs", null)
//        String testData = persister.getNonRuntimeData(next, "test.ValidTrailorConfigs")
//        assertTrue("".equals(testData))
//
//        persister.updateTestData(next, "test.ValidTrailorConfigs", "This is JSON data")
//        testData = persister.getNonRuntimeData(next, "test.ValidTrailorConfigs")
//        assertTrue("This is JSON data".equals(testData))

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
        cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%")
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
            new NCubeJdbcPersister().doCubesExist(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    void testGetAppNamesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppNames(c, defaultSnapshotApp)
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
            new NCubeJdbcPersister().getAppVersions(c, defaultSnapshotApp)
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
            assertTrue(e.message.contains("release"))
            assertTrue(e.message.contains("cube"))
        }
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
            assertEquals(e.message, null);
        }
    }

    @Test
    void testRenameCubeThatDoesNotExist() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)

        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        try
        {
            new NCubeJdbcPersister().renameCube(c, defaultSnapshotApp, ncube, "bar")
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("rename"))
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("found"))
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
        when(rs.getString("n_cube_nm")).thenReturn("foo");
        when(rs.getString("branch_id")).thenReturn("HEAD");
        when(rs.getBytes("cube_value_bin")).thenReturn("[                                                     ".getBytes("UTF-8"))

        Object[] nCubes = new NCubeJdbcPersister().getCubeRecords(c, defaultSnapshotApp, "")
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
            new NCubeJdbcPersister().loadCube(c, dto, null)
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
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            NCubeInfoDto[] dtos = new NCubeInfoDto[1];
            dtos[0] = new NCubeInfoDto();
            dtos[0].name = "foo";

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
    void testGetBranchChangesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getBranchChanges(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to fetch"))
            assertTrue(e.message.contains("branch cubes"))
        }
    }

    @Test
    void testReplaceHeadSha1ThatThrowsSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().replaceHeadSha1(c, defaultSnapshotApp, "foo", "FFFFFFF", 1)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to replace"))
        }
    }

    @Test
    void testCopyBranchCubeToHeadWithInvalidRevision() throws Exception
    {
        try
        {
            new NCubeJdbcPersister().copyBranchCubeToHead(null, defaultSnapshotApp, defaultSnapshotApp.asHead(), "foo", USER_ID, null);
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.startsWith("Revision number cannot be null"))
        }
    }

    @Test
    void testCopyBranchCubeToHeadThatThrowsSqlException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().copyBranchCubeToHead(c, defaultSnapshotApp, defaultSnapshotApp.asHead(), "foo", USER_ID, 1);
            fail()
        }
        catch (IllegalStateException e)
        {
            assertEquals(SQLException.class, e.cause.class);
            assertTrue(e.message.startsWith("Unable to copy cube"))
        }
    }

    @Test
    void testCopyBranchCubeToHeadThatDoesntUpdateCorrectly() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(0)
        when(rs.next()).thenReturn(true)
        when(rs.getBytes("cube_value_bin")).thenReturn("".getBytes("UTF-8"));

        try
        {
            new NCubeJdbcPersister().copyBranchCubeToHead(c, defaultSnapshotApp, defaultSnapshotApp.asHead(), "foo", USER_ID, 1);
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.startsWith("Unable to copy cube"))
        }
    }



    @Test
    void testReplaceHeadSha1ThatDoesNotUpdateCorrectly() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(0)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(1)).thenReturn(5L)

        try
        {
            new NCubeJdbcPersister().replaceHeadSha1(c, defaultSnapshotApp, "foo", "FFFFFFF", 1)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.startsWith("error updating"))
        }
    }

    @Test
    void testGetNCubesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getCubeRecords(c, defaultSnapshotApp, null)
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
            new NCubeJdbcPersister().getBranches(c, defaultSnapshotApp)
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
            new NCubeJdbcPersister().deleteCubes(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to delete cubes"))
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
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().renameCube(c, defaultSnapshotApp, ncube, "foo")
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
    void testRestoreCubeThatFailsTheUpdate() throws Exception
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
            assertTrue(e.message.contains("Cannot restore"))
            assertTrue(e.message.contains("rows inserted"))
        }
    }

    @Test
    void testDeleteCubeThatThrowsSQLException() throws Exception
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
        when(rs.getString("branch_id")).thenReturn(ApplicationID.HEAD);

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
    void testDeleteCubeThatFailsTheUpdate() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(9))
        when(rs.getBytes(anyString())).thenReturn("foo".getBytes("UTF-8"));

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
    void testCreateCubeWithWrongUpdateCount() throws Exception
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
    void testUpdateCubeWithWrongUpdateCount() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(rs.next()).thenReturn(true);
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
    void testUpdateTestDataWithDuplicateCubes() throws Exception
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
    void testReleaseCubesWithCubeThatExistsAlready() throws Exception
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
    void testChangeVersionWhenCubeAlreadyExists() throws Exception
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
