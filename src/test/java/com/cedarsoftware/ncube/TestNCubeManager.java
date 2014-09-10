package com.cedarsoftware.ncube;

import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.io.JsonWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NCubeManager Tests
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestNCubeManager
{
    private static int MYSQL = 1;
    private static int HSQLDB = 2;
    private static int ORACLE = 3;

    private int test_db = HSQLDB;            // CHANGE to suit test needs (should be HSQLDB for normal JUnit testing)
    static final String APP_ID = "ncube.test";

    @BeforeClass
    public static void init() throws Exception
    {
        TestNCube.initialize();
    }

    @Before
    public void setUp() throws Exception
    {
        prepareSchema();
    }

    private void prepareSchema() throws Exception
    {
        if (test_db == HSQLDB)
        {
            Connection conn = getConnection();
            // Using HSQLDB syntax.
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE n_cube ( " +
                    "n_cube_id bigint NOT NULL, " +
                    "n_cube_nm VARCHAR(100) NOT NULL, " +
                    "tenant_id CHAR(64), " +
                    "cube_value_bin varbinary(999999), " +
                    "create_dt DATE NOT NULL, " +
                    "update_dt DATE DEFAULT NULL, " +
                    "create_hid VARCHAR(20), " +
                    "update_hid VARCHAR(20), " +
                    "version_no_cd VARCHAR(16) DEFAULT '0.1.0' NOT NULL, " +
                    "status_cd VARCHAR(16) DEFAULT 'SNAPSHOT' NOT NULL, " +
                    "sys_effective_dt DATE DEFAULT SYSDATE NOT NULL, " +
                    "sys_expiration_dt DATE, " +
                    "business_effective_dt DATE DEFAULT SYSDATE, " +
                    "business_expiration_dt DATE, " +
                    "app_cd VARCHAR(20), " +
                    "test_data_bin varbinary(999999), " +
                    "notes_bin varbinary(999999), " +
                    "tags varbinary(999999), " +
                    "PRIMARY KEY (n_cube_id), " +
                    "UNIQUE (n_cube_nm, version_no_cd, app_cd, status_cd) " +
                    ");");
            stmt.close();
            conn.close();
        }
        else if (test_db == MYSQL)
        {
        /*  Schema for MYSQL

        drop table if exists `ncube`.n_cube;
CREATE TABLE `ncube`.n_cube (
n_cube_id bigint NOT NULL,
n_cube_nm varchar(100) NOT NULL,
n_tenant_id char(64),
cube_value_bin longtext,
create_dt date NOT NULL,
update_dt date DEFAULT NULL,
create_hid varchar(20),
update_hid varchar(20),
version_no_cd varchar(16) NOT NULL,
status_cd varchar(16) DEFAULT 'SNAPSHOT' NOT NULL,
sys_effective_dt date,
sys_expiration_dt date,
business_effective_dt date,
business_expiration_dt date,
app_cd varchar(20),
test_data_bin longtext,
notes_bin longtext,
tags longtext,
PRIMARY KEY (n_cube_id),
UNIQUE (n_cube_nm, version_no_cd, app_cd, status_cd)
);

drop trigger if exists `ncube`.sysEffDateTrigger;
DELIMITER ;;
CREATE trigger `ncube`.sysEffDateTrigger BEFORE INSERT ON `ncube`.n_cube
FOR EACH ROW
BEGIN
    SET NEW.sys_effective_dt = NOW();
END ;;
DELIMITER ;
         */}
    }

    @After
    public void tearDown() throws Exception
    {
        initManager();
        if (test_db == HSQLDB)
        {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE n_cube;");
            stmt.close();
            conn.close();
        }
    }

    public static void initManager() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    private Connection getConnection() throws Exception
    {
        Connection conn = null;
        if (test_db == MYSQL)
        {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/ncube?autoCommit=true", "ncube", "ncube");
        }
        else if (test_db == HSQLDB)
        {
            conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "");
        }
        else if (test_db == ORACLE)
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd", "ra_desktop", "p0rtal");
        }
        return conn;
    }

    private Connection getJdbcConnection() throws Exception
    {
        Connection conn = null;
        if (test_db == MYSQL)
        {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/ncube?autoCommit=true", "ncube", "ncube");
        }
        else if (test_db == HSQLDB)
        {
            conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "");
        }
        else if (test_db == ORACLE)
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd", "ra_desktop", "p0rtal");
        }
        return conn;
    }

    private void closeJdbcConnection(Connection connection)
    {
        try
        {
            if (connection != null)
                connection.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private NCube createCube() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        NCubeManager.createCube(getConnection(), APP_ID, ncube, version);
        NCubeManager.updateTestData(getConnection(), APP_ID, ncube.getName(), version, JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(getConnection(), APP_ID, ncube.getName(), version, "notes follow");
        return ncube;
    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testUpdateNotesWithDuplicateCubeUpdated() throws Exception {
        Connection c = mock(Connection.class);
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement("UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?")).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(2);
        when(c.isValid(anyInt())).thenReturn(true);

        try
        {
            NCubeManager.updateNotes(c, APP_ID, "foo", "0.1.0", "notes");
            fail();
        } catch(IllegalStateException e) {
        }
    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testUpdateNotesWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement("UPDATE n_cube SET notes_bin = ?, update_dt = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ?")).thenThrow(SQLException.class);
        when(ps.executeUpdate()).thenReturn(2);
        when(c.isValid(anyInt())).thenReturn(true);
        try
        {
            NCubeManager.updateNotes(c, APP_ID, "foo", "0.1.0", "notes");
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }


    @Test
    public void testLoadCubes() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube.getName();
        NCubeManager.createCube(getConnection(), APP_ID, ncube, version);
        NCubeManager.updateTestData(getConnection(), APP_ID, ncube.getName(), version, JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(getConnection(), APP_ID, ncube.getName(), version, "notes follow");

        ncube = TestNCube.getTestNCube3D_Boolean();
        String name2 = ncube.getName();
        NCubeManager.createCube(getConnection(), APP_ID, ncube, version);

        NCubeManager.clearCubeList();
        NCubeManager.loadCubes(getConnection(), APP_ID, version, ReleaseStatus.SNAPSHOT.name());

        ApplicationID appId = new ApplicationID(null, APP_ID, version);
        NCube ncube1 = NCubeManager.getCube(name1, appId);
        NCube ncube2 = NCubeManager.getCube(name2, appId);
        assertNotNull(ncube1);
        assertNotNull(ncube2);
        assertEquals(name1, ncube1.getName());
        assertEquals(name2, ncube2.getName());
        NCubeManager.clearCubeList();
        assertNull(NCubeManager.getCube(name1, appId));
        assertNull(NCubeManager.getCube(name2, appId));

        NCubeManager.deleteCube(getConnection(), APP_ID, name1, version, true);
        NCubeManager.deleteCube(getConnection(), APP_ID, name2, version, true);
    }

    @Test
    public void testLoadCubesWithJdbcConnectionProvider() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube.getName();
        Connection ncubeSetupConn = null;

        try
        {
            ncubeSetupConn = getJdbcConnection();
            NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube, version);
            NCubeManager.updateTestData(ncubeSetupConn, APP_ID, ncube.getName(), version, JsonWriter.objectToJson(coord));
            NCubeManager.updateNotes(ncubeSetupConn, APP_ID, ncube.getName(), version, "notes follow");

            ncube = TestNCube.getTestNCube3D_Boolean();
            String name2 = ncube.getName();
            NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube, version);

            NCubeManager.clearCubeList();
            NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getJdbcConnection());
            ApplicationID appId = new ApplicationID(null, APP_ID, version);
            NCubeManager.loadCubes(nCubeConnectionProvider, appId, ReleaseStatus.SNAPSHOT.name());
            nCubeConnectionProvider.commitTransaction();

            appId = new ApplicationID(null, APP_ID, version);
            NCube ncube1 = NCubeManager.getCube(name1, appId);
            NCube ncube2 = NCubeManager.getCube(name2, appId);
            assertNotNull(ncube1);
            assertNotNull(ncube2);
            assertEquals(name1, ncube1.getName());
            assertEquals(name2, ncube2.getName());
            NCubeManager.clearCubeList();
            assertNull(NCubeManager.getCube(name1, appId));
            assertNull(NCubeManager.getCube(name2, appId));

            NCubeManager.deleteCube(ncubeSetupConn, APP_ID, name1, version, true);
            NCubeManager.deleteCube(ncubeSetupConn, APP_ID, name2, version, true);
        }
        finally
        {
            closeJdbcConnection(ncubeSetupConn);
        }
    }

    @Test
    public void testLoadCubesWithConnectionProviderException()
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        Connection ncubeSetupConn = null;

        try
        {
            try
            {
                ncubeSetupConn = getJdbcConnection();
                NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube, version);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail("Unable to create test ncube...");
            }

            NCubeConnectionProvider nCubeConnectionProvider;
            Exception testException = null;

            //test with null connection
            try
            {
                nCubeConnectionProvider = new NCubeJdbcConnectionProvider(null);
            }
            catch (Exception e)
            {
                testException = e;
            }

            assertTrue(testException != null && testException instanceof IllegalStateException);

            //test when jdbc connection is closed
            Connection jdbcConn = null;
            try
            {
                jdbcConn = getJdbcConnection();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail("Unable to create jdbc connection...");
            }

            nCubeConnectionProvider = new NCubeJdbcConnectionProvider(jdbcConn);
            nCubeConnectionProvider.commitTransaction();
            testException = null;

            try
            {
                ApplicationID appId = new ApplicationID(null, APP_ID, version);
                NCubeManager.loadCubes(nCubeConnectionProvider, appId, ReleaseStatus.SNAPSHOT.name(), null);
            }
            catch (Exception e)
            {
                testException = e;
            }

            assertTrue(testException != null && testException instanceof IllegalArgumentException);
        }
        finally
        {
            closeJdbcConnection(ncubeSetupConn);
        }
    }

    @Test
    public void testLoadCubesException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);

        try
        {
            NCubeManager.loadCubes(c, APP_ID, "0.1.0", ReleaseStatus.SNAPSHOT.name());
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testValidateConnectionFalse() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenThrow(SQLException.class);

        try
        {
            NCubeManager.validateConnection(c);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testLoadCubeThatReturnsTwoCubes() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBytes("cube_value_bin")).thenReturn("{\"ncube\":\"containsCell\",\"defaultCellValue\":\"foo\",\"x\":\"y\",\"axes\":[{\"name\":\"Gender\",\"type\":\"DISCRETE\",\"valueType\":\"STRING\",\"hasDefault\":false, \"preferredOrder\":0,\"columns\":[{\"id\":\"Female\",\"name\":\"Jane\",\"age\":36},{\"id\":\"Male\"}],\"feet\":2}],\"cells\":[{\"id\":[\"Female\"],\"value\":\"bar\"}]}\n".getBytes("UTF-8"));
        try
        {
            NCubeManager.loadCube(c, APP_ID, "Name", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null, true);
            fail();
        } catch(RuntimeException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testLoadCubeWithException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.loadCube(c, APP_ID, "AnyName", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null, true);
            fail();
        } catch(RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testLoadCubeWithTestData() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getBytes("cube_value_bin")).thenReturn("{\"ncube\":\"containsCell\",\"defaultCellValue\":\"foo\",\"x\":\"y\",\"axes\":[{\"name\":\"Gender\",\"type\":\"DISCRETE\",\"valueType\":\"STRING\",\"hasDefault\":false, \"preferredOrder\":0,\"columns\":[{\"id\":\"Female\",\"name\":\"Jane\",\"age\":36},{\"id\":\"Male\"}],\"feet\":2}],\"cells\":[{\"id\":[\"Female\"],\"value\":\"bar\"}]}\n".getBytes("UTF-8"));
        when(rs.getBytes("test_data_bin")).thenReturn("foo".getBytes("UTF-8"));
        when(c.isValid(anyInt())).thenReturn(true);

        NCube result = NCubeManager.loadCubeWithTests(c, APP_ID, "AnyName", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null);
        assertEquals("foo", result.getTestData());
        assertEquals("containsCell", result.getName());
    }

    @Test
    public void testLoadCubeWithJdbcConnectionProvider() throws Exception
    {
        NCube<Double> ncube1 = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube1.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube1.setCell(1.1, coord);

        coord.put("age", 16);
        ncube1.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube1.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube1.getName();

        NCube ncube2 = TestNCube.getTestNCube3D_Boolean();
        String name2 = ncube2.getName();
        Connection ncubeSetupConn = null;

        try
        {
            ncubeSetupConn = getJdbcConnection();
            NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube1, version);
            NCubeManager.updateTestData(ncubeSetupConn, APP_ID, ncube1.getName(), version, JsonWriter.objectToJson(coord));
            NCubeManager.updateNotes(ncubeSetupConn, APP_ID, ncube1.getName(), version, "notes follow");

            NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube2, version);

            NCubeManager.clearCubeList();
            NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getJdbcConnection());
            ApplicationID appId = new ApplicationID(null, APP_ID, version);
            NCube loadedCube1 = NCubeManager.loadCube(nCubeConnectionProvider, appId, name1, ReleaseStatus.SNAPSHOT.name(), null);
            NCube loadedCube2 = NCubeManager.loadCube(nCubeConnectionProvider, appId, name2, ReleaseStatus.SNAPSHOT.name(), null);
            nCubeConnectionProvider.commitTransaction();

            assertNotNull(loadedCube1);
            assertNotNull(loadedCube2);
            assertEquals(name1, loadedCube1.getName());
            assertEquals(name2, loadedCube2.getName());
            NCubeManager.clearCubeList();
            appId = new ApplicationID(null, APP_ID, version);
            assertNull(NCubeManager.getCube(name1, appId));
            assertNull(NCubeManager.getCube(name2, appId));

            NCubeManager.deleteCube(ncubeSetupConn, APP_ID, name1, version, true);
            NCubeManager.deleteCube(ncubeSetupConn, APP_ID, name2, version, true);
        }
        finally
        {
            closeJdbcConnection(ncubeSetupConn);
        }
    }

    @Test
    public void testLoadCubeWithConnectionProviderException()
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube.getName();
        Connection ncubeSetupConn = null;

        try
        {
            try
            {
                ncubeSetupConn = getJdbcConnection();
                NCubeManager.createCube(ncubeSetupConn, APP_ID, ncube, version);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail("Unable to create test ncube...");
            }

            NCubeConnectionProvider nCubeConnectionProvider;
            Exception testException = null;

            //test with null connection
            try
            {
                nCubeConnectionProvider = new NCubeJdbcConnectionProvider(null);
            }
            catch (Exception e)
            {
                testException = e;
            }

            assertTrue(testException != null && testException instanceof IllegalStateException);

            //test when jdbc connection is closed
            Connection jdbcConn = null;
            try
            {
                jdbcConn = getJdbcConnection();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail("Unable to create jdbc connection...");
            }

            nCubeConnectionProvider = new NCubeJdbcConnectionProvider(jdbcConn);
            nCubeConnectionProvider.commitTransaction();
            testException = null;

            try
            {
                ApplicationID appId = new ApplicationID(null, APP_ID, version);
                NCubeManager.loadCube(nCubeConnectionProvider, appId, name1, ReleaseStatus.SNAPSHOT.name(), null);
            }
            catch (Exception e)
            {
                testException = e;
            }

            assertTrue(testException != null && testException instanceof IllegalArgumentException);
        }
        finally
        {
            closeJdbcConnection(ncubeSetupConn);
        }
    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testGetReferencedCubesThatLoadsTwoCubes() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(true);
        when(rs.getBytes("cube_value_bin")).thenReturn("{\"ncube\":\"containsCell\",\"defaultCellValue\":\"foo\",\"x\":\"y\",\"axes\":[{\"name\":\"Gender\",\"type\":\"DISCRETE\",\"valueType\":\"STRING\",\"hasDefault\":false, \"preferredOrder\":0,\"columns\":[{\"id\":\"Female\",\"name\":\"Jane\",\"age\":36},{\"id\":\"Male\"}],\"feet\":2}],\"cells\":[{\"id\":[\"Female\"],\"value\":\"bar\"}]}\n".getBytes("UTF-8"));

        try
        {
            Set<String> set = new HashSet<String>();
            NCubeManager.getReferencedCubeNames(c, APP_ID, "AnyCube", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null, set);
            fail();
        } catch(IllegalStateException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testGetReferencedCubesWithSqlException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(SQLException.class);

        try
        {
            Set<String> set = new HashSet<String>();
            NCubeManager.getReferencedCubeNames(c, APP_ID, "AnyCube", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null, set);
            fail();
        } catch(Exception e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }


    @Test
    public void testGetAppNamesWithException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);

        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.getAppNames(c, new Date());
            fail();
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }

        //  Test with null date, too.  Auto creates current date.
        try
        {
            NCubeManager.getAppNames(c, null);
            fail();
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetReferencedCubeNamesWithSqlException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);

        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            Set<String> set = new HashSet<String>();
            NCubeManager.getReferencedCubeNames(c, APP_ID, "cubeName", "0.1.0", ReleaseStatus.SNAPSHOT.name(), null, set);
            fail();
        } catch (RuntimeException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetNCubesWithSQLException() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);

        when(c.prepareStatement(anyString())).thenThrow(SQLException.class);
        try
        {
            NCubeManager.getNCubes(c, "test", "0.1.0", ReleaseStatus.RELEASE.name(), null, null);
            fail();
        } catch (RuntimeException e) {
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
            NCubeManager.updateCube(c, APP_ID, ncube, "0.1.0");
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

    //  Impossible to test without mocks
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

    @Test
    public void testRenameWithMatchingNames() throws Exception {
        Connection c = mock(Connection.class);
        when(c.isValid(anyInt())).thenReturn(true);
        try
        {
            NCubeManager.renameCube(c, "foo", "foo", APP_ID, "0.1.0");
            fail();
        } catch(IllegalArgumentException e) {
            assertNull(e.getCause());
        }
    }

    // TODO: Ken, this test needs updated now that renameCube renames the JSON n-cube
//    @Test
//    public void testRenameCubeThatDoesNotExists() throws Exception
//    {
//        NCube<Double> ncube = TestNCube.getTestNCube2D(true);
//
//        Connection c = getMockConnectionWithExistenceCheck("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND status_cd = ?", true);
//
//        PreparedStatement ps = mock(PreparedStatement.class);
//        when(c.prepareStatement("UPDATE n_cube SET n_cube_nm = ?, cube_value_bin = ? WHERE app_cd = ? AND version_no_cd = ? AND n_cube_nm = ? AND status_cd = '" + ReleaseStatus.SNAPSHOT + "'")).thenReturn(ps);
//        when(ps.executeUpdate()).thenReturn(0);
//        try
//        {
//            NCubeManager.renameCube(c, "foo", "bar", APP_ID, "0.1.0");
//            fail();
//        }
//        catch(IllegalArgumentException ignored) { }
//    }

    @Test
    public void testBadCommandCellCommand() throws Exception
    {
        NCube<Object> continentCounty = new NCube<>("test.ContinentCountries");
        ApplicationID appId = continentCounty.getApplicationID();
        appId.setApp(APP_ID);
        appId.setVersion("1.0.0");
        NCubeManager.addCube(continentCounty, appId);
        continentCounty.addAxis(TestNCube.getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        countries.addColumn("Mexico");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<>("test.Provinces");
        appId = canada.getApplicationID();
        appId.setApp(APP_ID);
        appId.setVersion("1.0.0");
        NCubeManager.addCube(canada, appId);
        canada.addAxis(TestNCube.getProvincesAxis());

        NCube<Object> usa = new NCube<>("test.States");
        appId = usa.getApplicationID();
        appId.setApp(APP_ID);
        appId.setVersion("1.0.0");
        NCubeManager.addCube(usa, appId);
        usa.addAxis(TestNCube.getStatesAxis());

        Map coord1 = new HashMap();
        coord1.put("Continent", "North America");
        coord1.put("Country", "USA");
        coord1.put("State", "OH");

        Map coord2 = new HashMap();
        coord2.put("Continent", "North America");
        coord2.put("Country", "Canada");
        coord2.put("Province", "Quebec");

        continentCounty.setCell(new GroovyExpression("@test.States([:])", null), coord1);
        continentCounty.setCell(new GroovyExpression("$test.Provinces(crunch)", null), coord2);

        usa.setCell(1.0, coord1);
        canada.setCell(0.78, coord2);

        assertTrue((Double) continentCounty.getCell(coord1) == 1.0);

        try
        {
            assertTrue((Double) continentCounty.getCell(coord2) == 0.78);
            fail("should throw exception");
        }
        catch (Exception expected)
        {
        }

        Connection conn = getConnection();
        NCubeManager.createCube(conn, APP_ID, continentCounty, "1.0.0");
        NCubeManager.createCube(conn, APP_ID, usa, "1.0.0");
        NCubeManager.createCube(conn, APP_ID, canada, "1.0.0");

        assertTrue(NCubeManager.getCachedNCubes().size() == 3);
        initManager();
        NCube test = NCubeManager.loadCube(conn, APP_ID, "test.ContinentCountries", "1.0.0", ReleaseStatus.SNAPSHOT.name(), new Date());
        assertTrue((Double) test.getCell(coord1) == 1.0);

        NCubeManager.deleteCube(conn, APP_ID, "test.ContinentCountries", "1.0.0", false);
        NCubeManager.deleteCube(conn, APP_ID, "test.States", "1.0.0", false);
        NCubeManager.deleteCube(conn, APP_ID, "test.Provinces", "1.0.0", false);
        assertTrue(NCubeManager.getCachedNCubes().size() == 0);
        conn.close();
    }

    @Test
    public void testGetReferencedCubeNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("template1.json");
        NCube n2 = NCubeManager.getNCubeFromResource("template2.json");

        String ver = "1.1.1";
        NCubeManager.createCube(getConnection(), APP_ID, n1, ver);
        NCubeManager.createCube(getConnection(), APP_ID, n2, ver);

        Set refs = new TreeSet();
        NCubeManager.getReferencedCubeNames(getConnection(), APP_ID, n1.getName(), ver, ReleaseStatus.SNAPSHOT.name(), null, refs);
        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template2Cube"));

        refs.clear();
        NCubeManager.getReferencedCubeNames(getConnection(), APP_ID, n2.getName(), ver, ReleaseStatus.SNAPSHOT.name(), null, refs);
        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template1Cube"));

        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, n1.getName(), ver, true));
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, n2.getName(), ver, true));

        try
        {
            NCubeManager.getReferencedCubeNames(getConnection(), APP_ID, n2.getName(), ver, ReleaseStatus.SNAPSHOT.name(), null, null);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testDuplicateNCube() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String ver = "1.1.1";
        Connection conn = getConnection();
        NCubeManager.createCube(conn, APP_ID, n1, ver);
        NCubeManager.duplicate(conn, n1.getName(), n1.getName(), APP_ID, APP_ID, "1.1.2", ver, ReleaseStatus.SNAPSHOT.name(), null);
        NCube n2 = NCubeManager.loadCube(conn, APP_ID, n1.getName(), ver, ReleaseStatus.SNAPSHOT.name(), null);

        assertTrue(NCubeManager.deleteCube(conn, APP_ID, n1.getName(), ver, true));
        assertTrue(NCubeManager.deleteCube(conn, APP_ID, n2.getName(), "1.1.2", true));
        assertTrue(n1.equals(n2));
    }

    @Test
    public void testGetAppNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String version = "1.1.99";
        NCubeManager.createCube(getConnection(), APP_ID, n1, version);

        Object[] names = NCubeManager.getAppNames(getConnection(), null);
        boolean foundName = false;
        for (Object name : names)
        {
            if ("ncube.test".equals(name))
            {
                foundName = true;
                break;
            }
        }

        Object[] vers = NCubeManager.getAppVersions(getConnection(), APP_ID, ReleaseStatus.SNAPSHOT.name(), null);
        boolean foundVer = false;
        for (Object ver : vers)
        {
            if (version.equals(ver))
            {
                foundVer = true;
                break;
            }
        }

        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, n1.getName(), version, true));
        assertTrue(foundName);
        assertTrue(foundVer);
    }

    @Test
    public void testChangeVersionValue() throws Exception {
        Connection conn = getConnection();
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String version = "1.1.99";
        NCubeManager.createCube(conn, APP_ID, n1, version);

        NCubeManager.changeVersionValue(conn, APP_ID, version, "1.1.20");

        NCube n2 = NCubeManager.loadCube(conn, APP_ID, n1.getName(), "1.1.20", ReleaseStatus.SNAPSHOT.name(), new Date());

        try
        {
            NCubeManager.changeVersionValue(conn, APP_ID, version, "1.1.20");
            fail();
        } catch (IllegalStateException e) {

        }

        assertTrue(NCubeManager.deleteCube(conn, APP_ID, n1.getName(), "1.1.20", true));
        assertEquals(n1, n2);
    }

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

        NCubeJdbcConnectionProvider nCubeJdbcConnectionProvider = new NCubeJdbcConnectionProvider(getJdbcConnection());
        ApplicationID appId = new ApplicationID(null, APP_ID, version);
        assertTrue(NCubeManager.doesCubeExist(nCubeJdbcConnectionProvider, appId, name, ReleaseStatus.SNAPSHOT.name(), new Date()));
        nCubeJdbcConnectionProvider.commitTransaction();

        NCube<String> cube = (NCube<String>) NCubeManager.loadCube(conn, APP_ID, name, version, ReleaseStatus.SNAPSHOT.name(), new Date());
        cube.removeMetaProperty("sha1");
        ncube.removeMetaProperty("sha1");
        assertTrue(DeepEquals.deepEquals(ncube, cube));

        ncube.setCell("Lija", coord);
        NCubeManager.updateCube(getConnection(), APP_ID, ncube, version);
        assertTrue(1 == NCubeManager.releaseCubes(conn, APP_ID, version));

        cube = (NCube<String>) NCubeManager.loadCube(conn, APP_ID, name, version, ReleaseStatus.RELEASE.name(), new Date());
        assertTrue("Lija".equals(cube.getCell(coord)));

        assertFalse(NCubeManager.deleteCube(conn, APP_ID, name, version, false));
        assertTrue(NCubeManager.deleteCube(conn, APP_ID, name, version, true));
        cube = NCubeManager.loadCube(conn, APP_ID, name, version, ReleaseStatus.SNAPSHOT.name(), new Date());
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


    @Test
    public void testGetNCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        String version = "0.1.1";
        NCubeManager.createCube(getConnection(), APP_ID, ncube1, version);
        NCubeManager.createCube(getConnection(), APP_ID, ncube2, version);

        Object[] cubeList = NCubeManager.getNCubes(getConnection(), APP_ID, version, ReleaseStatus.SNAPSHOT.name(), "test.%", new Date());

        assertTrue(cubeList != null);
        assertTrue(cubeList.length == 2);

        assertTrue(ncube1.getNumDimensions() == 3);
        assertTrue(ncube2.getNumDimensions() == 2);

        ncube1.deleteAxis("bu");
        NCubeManager.updateCube(getConnection(), APP_ID, ncube1, version);
        NCube cube1 = NCubeManager.loadCube(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", ReleaseStatus.SNAPSHOT.name(), new Date());
        assertTrue(cube1.getNumDimensions() == 2);    // used to be 3

        assertTrue(2 == NCubeManager.releaseCubes(getConnection(), APP_ID, version));

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        NCubeManager.createSnapshotCubes(getConnection(), APP_ID, version, "0.2.0");

        String notes1 = NCubeManager.getNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", null);
        String notes2 = NCubeManager.getNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.2.0", null);

        NCubeManager.updateNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", null);
        notes1 = NCubeManager.getNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", null);
        assertTrue("".equals(notes1));

        NCubeManager.updateNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", "Trailer Config Notes");
        notes1 = NCubeManager.getNotes(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.1.1", null);
        assertTrue("Trailer Config Notes".equals(notes1));

        NCubeManager.updateTestData(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.2.0", null);
        String testData = NCubeManager.getTestData(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.2.0", null);
        assertTrue("".equals(testData));

        NCubeManager.updateTestData(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.2.0", "This is JSON data");
        testData = NCubeManager.getTestData(getConnection(), APP_ID, "test.ValidTrailorConfigs", "0.2.0", null);
        assertTrue("This is JSON data".equals(testData));

        // Verify that you cannot delete a RELEASE ncube
        assertFalse(NCubeManager.deleteCube(getConnection(), APP_ID, ncube1.getName(), version, false));
        assertFalse(NCubeManager.deleteCube(getConnection(), APP_ID, ncube2.getName(), version, false));

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, ncube1.getName(), version, true));
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, ncube2.getName(), version, true));

        // Delete new SNAPSHOT cubes
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, ncube1.getName(), "0.2.0", false));
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, ncube2.getName(), "0.2.0", false));

        // Ensure that all test ncubes are deleted
        cubeList = NCubeManager.getNCubes(getConnection(), APP_ID, version, ReleaseStatus.RELEASE.name(), "test.%", new Date());
        assertTrue(cubeList.length == 0);
    }

    @Test
    public void testRenameNCube() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        String version = "0.1.1";
        NCubeManager.createCube(getConnection(), APP_ID, ncube1, version);
        NCubeManager.createCube(getConnection(), APP_ID, ncube2, version);

        NCubeManager.renameCube(getConnection(), ncube1.getName(), "test.Floppy", APP_ID, version);

        Object[] cubeList = NCubeManager.getNCubes(getConnection(), APP_ID, version, ReleaseStatus.SNAPSHOT.name(), "test.%", new Date());

        assertTrue(cubeList != null);
        assertTrue(cubeList.length == 2);

        NCubeInfoDto nc1 = (NCubeInfoDto) cubeList[0];
        NCubeInfoDto nc2 = (NCubeInfoDto) cubeList[1];

        assertTrue(nc1.name.equals("test.Floppy") || nc2.name.equals("test.Floppy"));
        assertFalse(nc1.name.equals("test.Floppy") && nc2.name.equals("test.Floppy"));

        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, "test.Floppy", version, true));
        assertTrue(NCubeManager.deleteCube(getConnection(), APP_ID, ncube2.getName(), version, true));
    }

    @Test
    public void testNCubeManagerLoadCube() throws Exception
    {
        try
        {
            NCubeManager.loadCube(getConnection(), null, "Security", "0.1.0", ReleaseStatus.RELEASE.name(), new Date());
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNCubeManagerGetCubes() throws Exception
    {
        // This proves that null is turned into '%' (no exception thrown)
        NCubeManager.getNCubes(getConnection(), APP_ID, "0.0.1", ReleaseStatus.SNAPSHOT.name(), null, new Date());
    }

    @Test
    public void testNCubeManagerUpdateCube() throws Exception
    {
        try
        {
            NCubeManager.updateCube(getConnection(), "DASHBOARD", null, "0.1.0");
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCube testCube = TestNCube.getTestNCube2D(false);
        try
        {
            NCubeManager.updateCube(getConnection(), "DASHBOARD", testCube, null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNCubeManagerCreateCubes() throws Exception
    {
        try
        {
            NCubeManager.createCube(getConnection(), "DASHBOARD", null, "0.1.0");
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCube testCube = TestNCube.getTestNCube2D(false);
        try
        {
            NCubeManager.createCube(getConnection(), "DASHBOARD", testCube, null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCube ncube1 = createCube();
        try
        {
            NCube ncube2 = createCube();
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalStateException);
        }

        NCubeManager.deleteCube(getConnection(), APP_ID, ncube1.getName(), "0.1.0", true);
    }

    @Test
    public void testNCubeManagerReleaseCubes() throws Exception
    {
        try
        {
            NCubeManager.releaseCubes(getConnection(), null, "0.1.0");
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNCubeManagerCreateSnapshots() throws Exception
    {
        try
        {
            NCubeManager.createSnapshotCubes(null, "DASHBOARD", "0.1.0", "0.1.0");
            fail("should not make it here");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            NCubeManager.createSnapshotCubes(getConnection(), "DASHBOARD", "0.1.0", "0.1.0");
            fail("versions are not allowed to match");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            NCube ncube2 = createCube();
            NCubeManager.releaseCubes(getConnection(), APP_ID, "0.1.0");
            NCubeManager.createSnapshotCubes(getConnection(), APP_ID, "0.1.0", "0.1.1");
            NCubeManager.createSnapshotCubes(getConnection(), APP_ID, "0.1.0", "0.1.1");
            fail("should not make it here");
        }
        catch (IllegalStateException ignore)
        {
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

    }

    @Test
    public void testNCubeManagerDelete() throws Exception
    {
        try
        {
            NCubeManager.deleteCube(null, "DASHBOARD", "DashboardRoles", "0.1.0", true);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    @Test
    public void testNCubeManagerNotesData() throws Exception
    {
        try
        {
            NCubeManager.getNotes(null, "DASHBOARD", "DashboardRoles", "0.1.0", null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }

        createCube();
        String notes = NCubeManager.getNotes(getConnection(), APP_ID, "test.Age-Gender", "0.1.0", null);
        assertNotNull(notes);
        assertTrue(notes.length() > 0);

        try
        {
            NCubeManager.updateNotes(getConnection(), APP_ID, "test.funky", "0.1.0", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalStateException);
        }

        try
        {
            NCubeManager.updateNotes(getConnection(), null, "test.funky", "0.1.0", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try
        {
            NCubeManager.getNotes(getConnection(), APP_ID, "test.Age-Gender", "0.1.1", null);
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCubeManager.deleteCube(getConnection(), APP_ID, "test.Age-Gender", "0.1.0", true);
    }

    @Test
    public void testNCubeManagerTestData() throws Exception
    {
        try
        {
            NCubeManager.getTestData(null, "DASHBOARD", "DashboardRoles", "0.1.0", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        createCube();
        String testData = NCubeManager.getTestData(getConnection(), APP_ID, "test.Age-Gender", "0.1.0", null);
        assertNotNull(testData);
        assertTrue(testData.length() > 0);

        try
        {
            NCubeManager.updateTestData(getConnection(), APP_ID, "test.funky", "0.1.0", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalStateException);
        }

        try
        {
            NCubeManager.updateTestData(getConnection(), null, "test.funky", "0.1.0", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try
        {
            NCubeManager.getTestData(getConnection(), APP_ID, "test.Age-Gender", "0.1.1", null);
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCubeManager.deleteCube(getConnection(), APP_ID, "test.Age-Gender", "0.1.0", true);
    }

    @Test
    public void testEmptyNCubeMetaProps() throws Exception
    {
        NCube ncube = createCube();
        String json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        assertTrue(ncube.getMetaProperties().size() == 1);  // sha1

        List<Axis> axes = ncube.getAxes();
        for (Axis axis : axes)
        {
            assertTrue(axis.getMetaProperties().size() == 0);

            for (Column column : axis.getColumns())
            {
                assertTrue(column.getMetaProperties().size() == 0);
            }
        }
        NCubeManager.deleteCube(getConnection(), APP_ID, ncube.getName(), "0.1.0", true);
    }

    @Test
    public void testBadUrlsAddedToClassLoader() throws Exception
    {
        String url = "htp://this wont work";
        List urls = new ArrayList();
        urls.add(url);
        try
        {
            NCubeManager.addBaseResourceUrls(urls, "2");
            fail("Should not make it here");
        }
        catch (Exception expected)
        { }
    }

    @Test
    public void testValidateCubeName() throws Exception
    {
        NCubeManager.validateCubeName("Joe");
        NCubeManager.validateCubeName("Joe.Dirt");
        NCubeManager.validateCubeName(NCube.validCubeNameChars);
        try
        {
            NCubeManager.validateCubeName("");
            fail("should not make it here");
        }
        catch (Exception e)
        { }

        try
        {
            NCubeManager.validateCubeName(null);
            fail("should not make it here");
        }
        catch (Exception e)
        { }
    }

    @Test
    public void testValidateStatus() throws Exception
    {
        NCubeManager.validateStatus(ReleaseStatus.SNAPSHOT.name());
        NCubeManager.validateStatus(ReleaseStatus.RELEASE.name());
        try
        {
            NCubeManager.validateStatus("fubar");
            fail("should not make it here");
        }
        catch (Exception e)
        { }
    }

    @Test(expected=RuntimeException.class)
    public void testGetNCubesFromResourceException() throws Exception
    {
        NCubeManager.getNCubesFromResource(null);
    }

    @Test
    public void testDeprecatedApisUntilTheyAreGone() {

        List<String> strings = new ArrayList<>();
        strings.add("http://www.cedarsoftware.com");

        assertNull(NCubeManager.getUrlClassLoader("foo"));
        NCubeManager.setUrlClassLoader(strings, "foo");
        assertNotNull(NCubeManager.getUrlClassLoader("foo"));

        assertNull(NCubeManager.getUrlClassLoader("bar"));
        NCubeManager.setBaseResourceUrls(strings, "bar");
        assertNotNull(NCubeManager.getUrlClassLoader("bar"));
    }

    @Test
    public void testValidateConnection() throws Exception
    {
        Connection c = getConnection();
        NCubeManager.validateConnection(c);
        c.close();
        try
        {
            NCubeManager.validateConnection(c);
            fail("should not make it here");
        }
        catch (Exception e)
        { }
    }

}
