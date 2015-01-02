package com.cedarsoftware.ncube;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class TestingDatabaseHelper
{
    public static int MYSQL = 1;
    public static int HSQL = 2;
    public static int ORACLE = 3;

    public static int test_db = HSQL;

    public static NCube[] getCubesFromDisk(String ...names)
    {
            List<NCube> list = new ArrayList<NCube>(names.length);

            for (String name : names)
            {
                URL url = NCubeManager.class.getResource("/" + name);

                try
                {
                    File jsonFile = new File(url.getFile());

                    try (InputStream in = new FileInputStream(jsonFile))
                    {
                        byte[] data = new byte[(int) jsonFile.length()];
                        in.read(data);

                        String str = new String(data, "UTF-8");
                        // parse cube just to get the name.
                        list.add(NCube.fromSimpleJson(str));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error loading " + name, e);
                }
            }

            return list.toArray(new NCube[list.size()]);
    }

    private static Object getProxyInstance() throws Exception
    {
        if (test_db == HSQL) {
            return HsqlTestingDatabaseManager.class.newInstance();
        }

        if (test_db == MYSQL) {
            return MySqlTestingDatabaseManager.class.newInstance();
        }

        throw new IllegalArgumentException("Unknown Database:  " + test_db);
    }

    public static NCubePersister getPersister() throws Exception
    {
        return new NCubeJdbcPersisterAdapter(createJdbcConnectionProvider());
    }

    public static JdbcConnectionProvider createJdbcConnectionProvider() throws Exception
    {
        if (test_db == HSQL) {
            return new TestingConnectionProvider(null, "jdbc:hsqldb:mem:testdb", "sa", "");
        }

        if (test_db == MYSQL) {
            return new TestingConnectionProvider(null, "jdbc:mysql://127.0.0.1:3306/ncube?autoCommit=true", "ncube", "ncube");
        }

        if (test_db == ORACLE) {
            return new TestingConnectionProvider("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd", "ra_desktop", "p0rtal");
        }

        throw new IllegalArgumentException("Unknown Database:  " + test_db);
    }

    public static TestingDatabaseManager getTestingDatabaseManager() throws Exception
    {
        if (test_db == HSQL) {
            return new HsqlTestingDatabaseManager(createJdbcConnectionProvider());
        }

        if (test_db == MYSQL) {
            return new MySqlTestingDatabaseManager(createJdbcConnectionProvider());
        }

        //  Don't manage tables for Oracle
        return new TestingDatabaseManager()
        {
            @Override
            public void setUp() throws SQLException
            {
            }

            @Override
            public void tearDown() throws SQLException
            {
            }

            @Override
            public void addCubes(ApplicationID appId, String username, NCube[] cubes) throws Exception
            {

            }

            @Override
            public void removeCubes(ApplicationID appId, String username, NCube[] cubes) throws Exception
            {

            }

            @Override
            public void updateCube(ApplicationID appId, String username, NCube cube) throws Exception
            {

            }
        };
    }

    public static void setupDatabase() throws Exception
    {
        getTestingDatabaseManager().setUp();
        NCubeManager.setNCubePersister(TestingDatabaseHelper.getPersister());
        setupTestClassPaths();
    }

    public static void setupTestClassPaths() throws Exception
    {
        NCube cp = NCubeManager.getNCubeFromResource(TestNCubeManager.defaultSnapshotApp, "sys.classpath.tests.json");
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, cp, TestNCubeManager.USER_ID);
        cp = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, "sys.classpath.tests.json");
        NCubeManager.createCube(ApplicationID.defaultAppId, cp, TestNCubeManager.USER_ID);
    }

    public static void tearDownDatabase() throws Exception
    {
        try
        {
            NCubeManager.deleteCube(TestNCubeManager.defaultSnapshotApp, "sys.classpath", TestNCubeManager.USER_ID);
        }
        catch(Exception ignored)
        { }

        try
        {
            NCubeManager.deleteCube(ApplicationID.defaultAppId, "sys.classpath", TestNCubeManager.USER_ID);
        }
        catch(Exception ignored)
        { }

        getTestingDatabaseManager().tearDown();
        NCubeManager.clearCache();
    }
}
