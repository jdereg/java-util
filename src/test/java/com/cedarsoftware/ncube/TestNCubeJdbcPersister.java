package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Before;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class TestNCubeJdbcPersister
{
    private int test_db = TestingDatabaseHelper.HSQLDB;            // CHANGE to suit test needs (should be HSQLDB for normal JUnit testing)
    static final String APP_ID = "ncube.test";

    TestingDatabaseManager manager;
    NCubePersister persister;

    @Before
    public void setUp() throws Exception
    {
        manager = TestingDatabaseHelper.getTestingDatabaseManager(test_db);
        manager.setUp();
        persister = TestingDatabaseHelper.getPersister(test_db);
    }

    @After
    public void tearDown() throws Exception
    {
        manager.tearDown();
        manager = null;
        persister = null;
    }

    /*
    @Test
    public void testDbApis() throws Exception
    {
        Connection conn = getConnection();
        String name = "test.NCube" + System.currentTimeMillis();

        NCube<String> ncube = new NCube<>(name);
        ncube.addAxis(TestNCube.getStatesAxis());
        ncube.addAxis(TestNCube.getFullGenderAxis());

        Map coord = new HashMap();
        coord.put("State", "OH");
        coord.put("Gender", "Male");
        ncube.setCell("John", coord);

        coord.put("State", "OH");
        coord.put("Gender", "Female");
        ncube.setCell("Alexa", coord);

        String version = "0.1.0";

        assertFalse(NCubeManager.doesCubeExist(conn, APP_ID, name, version, ReleaseStatus.SNAPSHOT.name(), new Date()));

        NCubeManager.createCube(conn, APP_ID, ncube, version);

        assertTrue(NCubeManager.doesCubeExist(conn, APP_ID, name, version, ReleaseStatus.SNAPSHOT.name(), new Date()));

//        NCubeJdbcConnectionProvider nCubeJdbcConnectionProvider = new NCubeJdbcConnectionProvider(getJdbcConnection());
//        ApplicationID appId = new ApplicationID(null, APP_ID, version, ReleaseStatus.SNAPSHOT.name());
//        assertTrue(NCubeManager.doesCubeExist(nCubeJdbcConnectionProvider, appId, name));
//        nCubeJdbcConnectionProvider.commitTransaction();

        NCube<String> cube = (NCube<String>) NCubeManager.getCube(name, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, version, ReleaseStatus.SNAPSHOT.name()));

        cube.removeMetaProperty("sha1");
        ncube.removeMetaProperty("sha1");
        assertTrue(DeepEquals.deepEquals(ncube, cube));

        ncube.setCell("Lija", coord);
        NCubeManager.updateCube(getConnection(), APP_ID, ncube, version);
        assertTrue(1 == NCubeManager.releaseCubes(conn, APP_ID, version));

        cube = (NCube<String>) NCubeManager.getCube(name, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, version, ReleaseStatus.SNAPSHOT.name()));
        assertTrue("Lija".equals(cube.getCell(coord)));

        assertFalse(NCubeManager.deleteCube(conn, APP_ID, name, version, false));
        assertTrue(NCubeManager.deleteCube(conn, APP_ID, name, version, true));
        cube = NCubeManager.getCube(name, new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, version, ReleaseStatus.SNAPSHOT.name()));
        assertNull(cube);

        conn.close();
    }

    @Test
    public void testDoesCubeExistWithException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.doesCubeExist(c, APP_ID, "name", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }
*/

        /*
    //  Impossible to test without mocks
    @Test
    public void testLoadCubesException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);

        try
        {
            NCubeManager.loadCubes(defaultSnapshotApp);
            fail();
        } catch(RuntimeException e) {
            assertEquals("Unable to update notes for NCube: foo, app: ncube.test, version: 999.99.9", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testUpdateCubeWithSqlException() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);

        try
        {
            NCubeManager.updateCube(c, APP_ID, ncube, "0.1.0");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testCreateCubeWithWrongUpdateCount() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);
        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND n_cube_nm = ?", false);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);
        try
        {
            NCubeManager.createCube(c, APP_ID, ncube, "0.1.0");
            fail();
        } catch(RuntimeException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testUpdateCubeWithWrongUpdateCount() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);
        try
        {
            NCubeManager.updateCube(new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name()), ncube);
            fail();
        } catch(RuntimeException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testGetNotesWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.getNotes(c, APP_ID, "foo", "0.1.0", null);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetTestDataWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.getTestData(c, APP_ID, "foo", "0.1.0", null);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testUpdateTestDataWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.updateTestData(c, APP_ID, "foo", "0.1.0", "foo");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testUpdateTestDataWithDuplicateCubes() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(2);

        try
        {
            NCubeManager.updateTestData(c, APP_ID, "foo", "0.1.0", "foo");
            fail();
        } catch(IllegalStateException e) {
        }
    }

    @Test
    public void testGetAppVersionsWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.getAppVersions(c, APP_ID, ReleaseStatus.RELEASE.name(), null);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testCreateCubeWithSqlException() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);
        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND n_cube_nm = ?", false);
        when(c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)")).thenThrow(SQLException.class);

        try
        {
            NCubeManager.createCube(c, APP_ID, ncube, "0.1.0");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testReleaseCubesWithCubeThatExistsAlready() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND status_cd = ?", true);

        try
        {
            NCubeManager.releaseCubes(c, APP_ID, "0.1.0");
            fail();
        } catch(RuntimeException e) {
        }
    }

    @Test
    public void testReleaseCubesWithCubeWithSqlException() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND status_cd = ?", false);

        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.releaseCubes(c, APP_ID, "0.1.0");
            fail();
        } catch(RuntimeException e) {
        }
    }

    @Test
    public void testChangeVersionWhenCubeAlreadyExists() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND status_cd = ?", true);
        try
        {
            NCubeManager.changeVersionValue(c, APP_ID, "0.1.0", "1.1.1");
            fail();
        } catch(RuntimeException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testChangeVersionValueWithCubeWithSqlException() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND status_cd = ?", false);
        when(c.prepareStatement("UPDATE n_cube SET update_dt = ?, version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'")).thenThrow(SQLException.class);
        try
        {
            NCubeManager.changeVersionValue(c, APP_ID, "0.1.0", "1.1.1");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    private Connection getMockConnectionWithExistenceCheck(String query, boolean ret) throws SQLException
    {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(query)).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(ret);
        return c;
    }

    @Test
    public void testDeleteCubeWithSQLException() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.deleteCube(c, APP_ID, "foo", "0.1.0", true);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testRenameCubeThatThrowsSQLEXception() throws Exception {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.renameCube(c, "foo", "bar", APP_ID, "0.1.0");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }


        try
        {

            Connection c = mock(Connection.class);
            when(c.isValid(anyInt())).thenReturn(true);

            PreparedStatement psVerify = mock(PreparedStatement.class);
            ResultSet rsVerify = mock(ResultSet.class);

            when(c.prepareStatement("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)")).thenReturn(psVerify);
            when(psVerify.executeQuery()).thenReturn(rsVerify);
            when(rsVerify.next()).thenReturn(false);


            when(c.prepareStatement("SELECT n_cube_nm, cube_value_bin, create_dt, update_dt, create_hid, update_hid, version_no_cd, status_cd, sys_effective_dt, sys_expiration_dt, business_effective_dt, business_expiration_dt, app_cd, test_data_bin, notes_bin\n" +
                    "FROM n_cube\n" +
                    "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ?")).thenThrow(SQLException.class);

            NCubeManager.createSnapshotCubes(c, APP_ID, "0.1.0", "0.1.1");
            fail("should not make it here");
        }
        catch (RuntimeException e)
        {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

    */





}
