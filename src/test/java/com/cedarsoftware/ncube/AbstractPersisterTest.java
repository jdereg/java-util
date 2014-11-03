package com.cedarsoftware.ncube;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kpartlow on 11/2/2014.
 */
public abstract class AbstractPersisterTest
{
    private int test_db = TestingDatabaseHelper.HSQLDB;            // CHANGE to suit test needs (should be HSQLDB for normal JUnit testing)
    static final String APP_ID = "ncube.test";
    private ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ReleaseStatus.SNAPSHOT.name());

    TestingDatabaseManager manager;

    @Before
    public void setUp() throws Exception
    {
        manager = TestingDatabaseHelper.getTestingDatabaseManager(test_db);
        manager.setUp();
    }

    @After
    public void tearDown() throws Exception
    {
        manager.tearDown();
        manager = null;
    }

    public abstract NCubePersister getPersister(int db) throws Exception;

    @Test
    public void testDbApis() throws Exception
    {
        NCubePersister persister = getPersister(test_db);

        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        persister.createCube(defaultSnapshotApp, ncube1);
        persister.createCube(defaultSnapshotApp, ncube2);

        Object[] cubeList = persister.getNCubes(defaultSnapshotApp, "test.%");

        assertTrue(cubeList.length == 2);

        assertTrue(ncube1.getNumDimensions() == 3);
        assertTrue(ncube2.getNumDimensions() == 2);

        ncube1.deleteAxis("bu");
        persister.updateCube(defaultSnapshotApp, ncube1);
        NCube cube1 = persister.findCube(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue(cube1.getNumDimensions() == 2);    // used to be 3

        //  Shouldn't exist we've released the cubes
        List<NCube> cubes = persister.loadCubes(defaultSnapshotApp);
        assertEquals(2, cubes.size());


        assertTrue(2 == persister.releaseCubes(defaultSnapshotApp));

        //  Shouldn't exist as snapshot we've released the cubes
        cubes = persister.loadCubes(defaultSnapshotApp);
        assertEquals(0, cubes.size());

        ApplicationID releaseId = defaultSnapshotApp.createReleaseId();

        //  should now exist as release id since we've released the cubes.
        cubes = persister.loadCubes(releaseId);
        assertEquals(2, cubes.size());

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        persister.createSnapshotVersion(defaultSnapshotApp, "0.2.0");

        ApplicationID newSnapshotId = defaultSnapshotApp.createNewSnapshotId("0.2.0");

        //  should exist under new ID since we've released and create a new snapshot.
        cubes = persister.loadCubes(newSnapshotId);
        assertEquals(2, cubes.size());


        String notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        String notes2 = persister.getNotes(newSnapshotId, "test.ValidTrailorConfigs");

        persister.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", null);
        notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("".equals(notes1));

        persister.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", "Trailer Config Notes");
        notes1 = persister.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("Trailer Config Notes".equals(notes1));

        persister.updateTestData(newSnapshotId, "test.ValidTrailorConfigs", null);
        String testData = persister.getTestData(newSnapshotId, "test.ValidTrailorConfigs");
        assertTrue("".equals(testData));

        persister.updateTestData(newSnapshotId, "test.ValidTrailorConfigs", "This is JSON data");
        testData = persister.getTestData(newSnapshotId, "test.ValidTrailorConfigs");
        assertTrue("This is JSON data".equals(testData));


        // Verify that you cannot delete a RELEASE ncube
        assertFalse(persister.deleteCube(defaultSnapshotApp, ncube1.getName(), false));
        assertFalse(persister.deleteCube(defaultSnapshotApp, ncube2.getName(), false));

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        assertTrue(persister.deleteCube(defaultSnapshotApp, ncube1.getName(), true));
        assertTrue(persister.deleteCube(defaultSnapshotApp, ncube2.getName(), true));

        // Delete new SNAPSHOT cubes
        assertTrue(persister.deleteCube(newSnapshotId, ncube1.getName(),  false));
        assertTrue(persister.deleteCube(newSnapshotId, ncube2.getName(), false));

        // Ensure that all test ncubes are deleted
        cubeList = persister.getNCubes(defaultSnapshotApp, "test.%");
        assertTrue(cubeList.length == 0);
    }

    @Test
    public void testGetCubeList() throws Exception {
        NCubePersister persister = getPersister(test_db);

        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        Object[] cubeList = persister.getNCubes(defaultSnapshotApp, "test.%");
        assertEquals(0, cubeList.length);

        persister.createCube(defaultSnapshotApp, ncube1);
        persister.createCube(defaultSnapshotApp, ncube2);

        cubeList = persister.getNCubes(defaultSnapshotApp, "test.%");
        assertEquals(2, cubeList.length);

        cubeList = persister.getNCubes(defaultSnapshotApp, "test.V%");
        assertEquals(1, cubeList.length);
        assertEquals("test.ValidTrailorConfigs", ((NCubeInfoDto)cubeList[0]).name);
    }

    @Test
    public void testDoesCubeExist() throws Exception {
        NCubePersister persister = getPersister(test_db);

        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        assertFalse(persister.doesCubeExist(defaultSnapshotApp, ncube1.getName()));
        assertFalse(persister.doesCubeExist(defaultSnapshotApp, ncube2.getName()));

        persister.createCube(defaultSnapshotApp, ncube1);
        persister.createCube(defaultSnapshotApp, ncube2);

        assertTrue(persister.doesCubeExist(defaultSnapshotApp, ncube1.getName()));
        assertTrue(persister.doesCubeExist(defaultSnapshotApp, ncube2.getName()));
    }

    @Test
    public void testGetAppNames() throws Exception {
        NCubePersister persister = getPersister(test_db);

        Object[] names = persister.getAppNames();
        assertEquals(0, names.length);

        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        persister.createCube(defaultSnapshotApp, ncube1);

        names = persister.getAppNames();
        assertEquals(1, names.length);
        assertEquals("ncube.test", names[0]);

        ApplicationID newApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, "1.0.0", ReleaseStatus.SNAPSHOT.name());


        persister.createCube(newApp, ncube1);
        names = persister.getAppNames();
        assertEquals(2, names.length);
        assertEquals(ApplicationID.DEFAULT_APP, names[0]);
        assertEquals("ncube.test", names[1]);
    }

    @Test
    public void testGetAppVersions() throws Exception {
        NCubePersister persister = getPersister(test_db);

        Object[] vers = persister.getAppVersions(defaultSnapshotApp);
        assertEquals(0, vers.length);

        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        persister.createCube(defaultSnapshotApp, ncube1);

        vers = persister.getAppVersions(defaultSnapshotApp);
        assertEquals(1, vers.length);

        ApplicationID newSnapshotId = defaultSnapshotApp.createNewSnapshotId("7.0.0");
        persister.createCube(newSnapshotId, ncube1);

        vers = persister.getAppVersions(defaultSnapshotApp);
        assertEquals(2, vers.length);

        // different app shouldn't affect the number
        ApplicationID newApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, "1.0.0", ReleaseStatus.SNAPSHOT.name());
        persister.createCube(newApp, ncube1);
        assertEquals(2, vers.length);
    }

    @Test
    public void testBeginTransactionWithSqlException() {
        JdbcPersister persister = new JdbcPersister(null, "foo", "SA", "");
        try {
            persister.beginTransaction();
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to begin transaction...", e.getMessage());
        }
    }

    @Test
    public void testCommitTransactionWithSqlException() throws Exception {
        JdbcPersister persister = new JdbcPersister(null, "foo", "SA", "");
        Connection c = mock(Connection.class);
        doThrow(SQLException.class).when(c).commit();
        try {
            persister.commitTransaction(c);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to commit active transaction...", e.getMessage());
        }
    }

    @Test
    public void testCommitWithNullTransaction() throws Exception {
        JdbcPersister persister = new JdbcPersister(null, "foo", "SA", "");
        try {
            persister.commitTransaction(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Unable to commit transaction.  Connection is null.", e.getMessage());
        }
    }

    @Test
    public void testRollbackTransactionWithSqlException() throws Exception {
        JdbcPersister persister = new JdbcPersister(null, "foo", "SA", "");
        Connection c = mock(Connection.class);
        doThrow(SQLException.class).when(c).rollback();
        try {
            persister.rollbackTransaction(c);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Unable to rollback active transaction...", e.getMessage());
        }
    }

    @Test
    public void testRollbackWithNullTransaction() throws Exception {
        JdbcPersister persister = new JdbcPersister(null, "foo", "SA", "");
        try {
            persister.rollbackTransaction(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Unable to rollback transaction. Connection is null.", e.getMessage());
        }
    }

    @Test
    public void testDataSourceConstructor() throws Exception {
        JDBCDataSource source = new JDBCDataSource();
        source.setUrl("jdbc:hsqldb:mem:testdb");
        source.setUser("SA");
        source.setPassword("");

        try (Connection c = new JdbcPersister(source).getConnection()) {
            assertNotNull(c);
        }

        JdbcPersister persister = new JdbcPersister("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:testdb", "SA", "");
        try (Connection c = persister.getConnection()) {
            assertNotNull(c);
        }

    }

    @Test
    public void testConstructor() throws Exception {
        try {
            new JdbcPersister("org.does.not.exist", "jdbc:hsqldb:mem:testdb", "SA", "");
        } catch (RuntimeException e) {
            assertEquals(ClassNotFoundException.class, e.getCause().getClass());
            assertTrue(e.getMessage().startsWith("Unable to locate driver"));
        }

        try {
            new JdbcPersister(null, null, "SA", "");
        } catch (NullPointerException e) {
            assertEquals("database url cannot be null...", e.getMessage());
        }

        try {
            new JdbcPersister(null);
        } catch (NullPointerException e) {
            assertEquals("datasource cannot be null", e.getMessage());
        }

        try {
            new JdbcPersister(null, "jdbc:hsqldb:mem:testdb", null, "");
        } catch (NullPointerException e) {
            assertEquals("database user cannot be null...", e.getMessage());
        }

        try {
            new JdbcPersister(null, "jdbc:hsqldb:mem:testdb", "SA", null);
        } catch (NullPointerException e) {
            assertEquals("database password cannot be null...", e.getMessage());
        }
    }

    @Test
    public void testForErrorsGettingConnection() throws Exception {
        NCube cube = TestNCube.getTestNCube2D(true);

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(true);
            new MockJdbcPersister(defaultConnection).doesCubeExist(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).createSnapshotVersion(defaultSnapshotApp, "1.1.1");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).changeVersionValue(defaultSnapshotApp, "1.1.1");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).createCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(true);
            new MockJdbcPersister(defaultConnection).deleteCube(defaultSnapshotApp, "foo", false);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).findCube(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).getAppNames();
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).getAppVersions(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).getNCubes(defaultSnapshotApp, null);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBytes(anyString())).thenReturn(null);
            doThrow(SQLException.class).when(defaultConnection).close();
            new MockJdbcPersister(defaultConnection).getNotes(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBytes(anyString())).thenReturn(null);
            doThrow(SQLException.class).when(defaultConnection).close();
            new MockJdbcPersister(defaultConnection).getTestData(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).updateTestData(defaultSnapshotApp, "foo", "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).updateNotes(defaultSnapshotApp, "foo", "foo");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).createCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).updateCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).releaseCubes(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).renameCube(defaultSnapshotApp, cube, "bar");
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCloseIssue(false);
            new MockJdbcPersister(defaultConnection).loadCubes(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }


    }

    @Test
    public void testForErrorsCommitting() throws Exception {
        NCube cube = TestNCube.getTestNCube2D(true);

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(true);
            new MockJdbcPersister(defaultConnection).doesCubeExist(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).createSnapshotVersion(defaultSnapshotApp, "1.1.1");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).changeVersionValue(defaultSnapshotApp, "1.1.1");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).createCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(true);
            new MockJdbcPersister(defaultConnection).deleteCube(defaultSnapshotApp, "foo", false);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).findCube(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).getAppNames();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).getAppVersions(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).getNCubes(defaultSnapshotApp, null);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBytes(anyString())).thenReturn(null);
            doThrow(SQLException.class).when(defaultConnection).commit();
            new MockJdbcPersister(defaultConnection).getNotes(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getBytes(anyString())).thenReturn(null);
            doThrow(SQLException.class).when(defaultConnection).commit();
            new MockJdbcPersister(defaultConnection).getTestData(defaultSnapshotApp, "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).updateTestData(defaultSnapshotApp, "foo", "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).updateNotes(defaultSnapshotApp, "foo", "foo");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).createCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).updateCube(defaultSnapshotApp, cube);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).releaseCubes(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).renameCube(defaultSnapshotApp, cube, "bar");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }

        try {
            Connection defaultConnection = getDefaultConnectionWithCommitIssue(false);
            new MockJdbcPersister(defaultConnection).loadCubes(defaultSnapshotApp);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unable to commit"));
        }


    }

    private Connection getDefaultConnectionWithCloseIssue(boolean rsReturn) throws SQLException
    {
        Connection defaultConnection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(rsReturn);
        doThrow(SQLException.class).when(defaultConnection).close();
        return defaultConnection;
    }

    private Connection getDefaultConnectionWithCommitIssue(boolean rsReturn) throws SQLException
    {
        Connection defaultConnection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(defaultConnection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(rsReturn);
        doThrow(SQLException.class).when(defaultConnection).commit();
        return defaultConnection;
    }


    public static class MockJdbcPersister extends JdbcPersister {
        private Connection mockConnection;
        public MockJdbcPersister(Connection c) {
            super(null, "jdbc:hsqldb:mem:testdb", "SA", "");
            mockConnection = c;
        }

        @Override
        public Connection beginTransaction()  {
            return mockConnection;
        }
    }
}
