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
public class TestNCubeJdbcPersister
{
    static final String APP_ID = "ncube.test";
    static final String USER_ID = "jdirt";

    private ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ReleaseStatus.SNAPSHOT.name())

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
    public void testDbApis() throws Exception
    {
        NCubePersister persister = TestingDatabaseHelper.persister

        NCube ncube1 = NCubeBuilder.getTestNCube3D_Boolean()
        NCube ncube2 = NCubeBuilder.getTestNCube2D(true)

        persister.createCube(defaultSnapshotApp, ncube1, USER_ID)
        persister.createCube(defaultSnapshotApp, ncube2, USER_ID)

        Object[] cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%")

        assertTrue(cubeList != null)
        assertTrue(cubeList.length == 2)

        assertTrue(ncube1.numDimensions == 3)
        assertTrue(ncube2.numDimensions == 2)

        ncube1.deleteAxis("bu")
        persister.updateCube(defaultSnapshotApp, ncube1, USER_ID)
        int numRelease = persister.releaseCubes(defaultSnapshotApp)
        assertEquals(4, numRelease)

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        persister.createSnapshotVersion(defaultSnapshotApp, "0.2.0")

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.2.0")

        String notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs")
        String notes2 = persister.getNotes(newId, "test.ValidTrailorConfigs")

        persister.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", null)
        notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs")
        assertTrue("".equals(notes1))

        persister.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", "Trailer Config Notes")
        notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs")
        assertTrue("Trailer Config Notes".equals(notes1))

        persister.updateTestData(newId, "test.ValidTrailorConfigs", null)
        String testData = persister.getTestData(newId, "test.ValidTrailorConfigs")
        assertTrue("".equals(testData))

        persister.updateTestData(newId, "test.ValidTrailorConfigs", "This is JSON data")
        testData = persister.getTestData(newId, "test.ValidTrailorConfigs")
        assertTrue("This is JSON data".equals(testData))

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

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        assertTrue(persister.deleteCube(defaultSnapshotApp, ncube1.name, true, USER_ID))
        assertTrue(persister.deleteCube(defaultSnapshotApp, ncube2.name, true, USER_ID))

        // Delete new SNAPSHOT cubes
        assertTrue(persister.deleteCube(newId, ncube1.name, false, USER_ID))
        assertTrue(persister.deleteCube(newId, ncube2.name, false, USER_ID))

        // Ensure that all test ncubes are deleted
        cubeList = persister.getCubeRecords(defaultSnapshotApp, "test.%")
        assertTrue(cubeList.length == 0)
    }


    @Test
    public void testDoesCubeExistWithException() throws Exception
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
    public void testDoCubesExistWithException() throws Exception
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
    public void testGetAppNamesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppNames(c, defaultSnapshotApp.tenant)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    public void testUpdateCubeWithSqlException() throws Exception
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
    public void testGetAppVersionsWithSQLException() throws Exception
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
    public void testGetNotesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getNotes(c, defaultSnapshotApp, "name")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    public void testDoesCubeExistWithSQLException() throws Exception
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
    public void testDoReleaseCubesExist() throws Exception
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
    public void testGetTestDataWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getTestData(c, defaultSnapshotApp, "name")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }

    @Test
    public void testUpdateTestDataWithSQLException() throws Exception
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
    public void testChangeVersionValueWithSqlException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(false)

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
    public void testReleaseCubesWhereReleaseCubesDontExist() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(true)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("already exists"))
        }
    }

    @Test
    public void testReleaseCubesWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to release"))
        }
    }

    @Test
    public void testRenameCubeThatDoesNotExist() throws Exception
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
    public void testLoadCubesWithInvalidCube() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true).thenReturn(false)
        when(rs.getBytes("cube_value_bin")).thenReturn("[                                                     ".getBytes("UTF-8"))

        Object[] nCubes = new NCubeJdbcPersister().getCubeRecords(c, defaultSnapshotApp, "")
        assertNotNull(nCubes)
        assertEquals(1, nCubes.length)     // Won't know it fails until getCube() is called.
    }

    @Test
    public void testLoadCubesWithSQLException() throws Exception
    {

        Connection c = getConnectionThatThrowsSQLException()

        NCubeInfoDto dto = new NCubeInfoDto()
        dto.name = "foo";

        try
        {
            new NCubeJdbcPersister().loadCube(c, dto)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Unable to load cube"))
        }
    }

    @Test
    public void testChangeVersionWithNoUpdate() throws Exception
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
    public void testUpdateNotesWithDuplicateCubeUpdated() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(2)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateNotes(c, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name()), "foo", "notes")
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("exist"))
        }
    }

    @Test
    public void testUpdateNotesWithNoCubesUpdated() throws Exception
    {
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeUpdate()).thenReturn(0)
        when(ps.executeQuery()).thenReturn(rs)

        try
        {
            new NCubeJdbcPersister().updateNotes(c, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name()), "foo", "notes")
            fail()
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("exist"))
        }
    }

    @Test
    public void testUpdateNotesWithSQLException() throws Exception
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
    public void testCreateSnapshotVersionWithSQLException() throws Exception
    {
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(false)

        try
        {
            new NCubeJdbcPersister().createSnapshotVersion(c, defaultSnapshotApp, "1.1.1")
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
            assertTrue(e.message.startsWith("Unable to create SNAPSHOT"))
        }
    }

    @Test
    public void testGetNCubesWithSQLException() throws Exception
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

    private Connection getConnectionThatThrowsSQLException() throws SQLException
    {
        Connection c = mock(Connection.class)
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class)
        return c;
    }

    private Connection getConnectionThatThrowsSQLExceptionAfterExistenceCheck(boolean exists) throws SQLException
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(exists)
        return c;
    }

    @Test
    public void testCreateCubeWithSqlException() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(false)
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
    public void testCreateCubeWhenOneAlreadyExists() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(true)

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
    public void testCreateCubeThatDoesntCreateCube() throws Exception
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
    public void testDeleteCubeWithSQLException() throws Exception
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
            new NCubeJdbcPersister().deleteCube(c, defaultSnapshotApp, "foo", true, USER_ID)
            fail()
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.cause.class)
        }
    }


    @Test
    public void testRenameCubeThatThrowsSQLEXception() throws Exception
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
    public void testGetDeletedCubeRecordsThatThrowsSQLException() throws Exception
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
    public void testGetRevisionsThatThrowsSQLException() throws Exception
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
    public void testRestoreCubeThatThrowsSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(-9))

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
            assertTrue(e.message.contains("Unable to restore cube"))
        }
    }

    @Test
    public void testRestoreCubeThatFailsTheUpdate() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(-9))

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
    public void testDeleteCubeThatThrowsSQLException() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenThrow(SQLException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(9))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes(anyString())).thenReturn(out.toByteArray())
        when(rs.getBytes(anyInt())).thenReturn(null)
        when(rs.getString("status_cd")).thenReturn(ReleaseStatus.SNAPSHOT.name())

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
    public void testDeleteCubeThatFailsTheUpdate() throws Exception
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)

        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(true).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(true)
        when(rs.getLong(anyInt())).thenReturn(new Long(9))

        ByteArrayOutputStream out = new ByteArrayOutputStream(8192)
        URL url = TestNCubeJdbcPersister.class.getResource("/2DSimpleJson.json")
        IOUtilities.transfer(new File(url.file), out)
        when(rs.getBytes(anyString())).thenReturn(out.toByteArray())
        when(rs.getBytes(anyInt())).thenReturn(null)
        when(rs.getString("status_cd")).thenReturn(ReleaseStatus.SNAPSHOT.name())
        when(ps.executeUpdate()).thenReturn(0)

        try
        {
            new NCubeJdbcPersister().deleteCube(c, defaultSnapshotApp, "foo", false, USER_ID)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("Cannot delete"))
            assertTrue(e.message.contains("rows inserted"))
        }
    }

    @Test
    public void testCreateCubeWithWrongUpdateCount() throws Exception
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
    public void testUpdateCubeWithWrongUpdateCount() throws Exception
    {
        NCube<Double> ncube = NCubeBuilder.getTestNCube2D(true)
        Connection c = mock(Connection.class)
        ResultSet rs = mock(ResultSet.class)
        PreparedStatement ps = mock(PreparedStatement.class)
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
            assertTrue(e.message.contains("non-exist"))
            assertTrue(e.message.contains("updat"))
            assertTrue(e.message.contains("ube"))
        }
    }

    @Test
    public void testUpdateTestDataWithDuplicateCubes() throws Exception
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
    public void testReleaseCubesWithCubeThatExistsAlready() throws Exception
    {
        NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(true)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.message.contains("already exists"))
        }
    }

    @Test
    public void testChangeVersionWhenCubeAlreadyExists() throws Exception
    {
        NCubeBuilder.getTestNCube2D(true)
        Connection c = getConnectionThatThrowsSQLExceptionAfterExistenceCheck(true)
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
