package com.cedarsoftware.ncube

import com.cedarsoftware.util.IOUtilities

import java.sql.SQLException

/**
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
class TestingDatabaseHelper
{
    public static int MYSQL = 1
    public static int HSQL = 2
    public static int ORACLE = 3
    public static int test_db = HSQL

    public static NCube[] getCubesFromDisk(String ...names) throws IOException
    {
        List<NCube> list = new ArrayList<NCube>(names.length);

        for (String name : names) {
            URL url = NCubeManager.class.getResource("/" + name);
            File jsonFile = new File(url.file);

            InputStream input = null;
            try {
                input = new FileInputStream(jsonFile);
                byte[] data = new byte[(int) jsonFile.length()];
                input.read(data);

                String str = new String(data, "UTF-8");
                list.add(NCube.fromSimpleJson(str));
            } finally {
                IOUtilities.close(input);
            }
        }

        return list.toArray(new NCube[list.size()]);
    }

    public static NCubePersister getPersister() throws Exception
    {
        return new NCubeJdbcPersisterAdapter(createJdbcConnectionProvider())
    }

    public static JdbcConnectionProvider createJdbcConnectionProvider() throws Exception
    {
        if (test_db == HSQL)
        {
            return new TestingConnectionProvider(null, 'jdbc:hsqldb:mem:testdb', 'sa', '')
        }

        if (test_db == MYSQL)
        {
            return new TestingConnectionProvider(null, 'jdbc:mysql://127.0.0.1:3306/ncube?autoCommit=true', 'ncube', 'ncube')
        }

        if (test_db == ORACLE)
        {
            return new TestingConnectionProvider('oracle.jdbc.driver.OracleDriver', 'jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd', 'ra_desktop', 'xxxxxx')
        }

        throw new IllegalArgumentException('Unknown Database:  ' + test_db)
    }

    public static TestingDatabaseManager getTestingDatabaseManager() throws Exception
    {
        if (test_db == HSQL)
        {
            return new HsqlTestingDatabaseManager(createJdbcConnectionProvider())
        }

        if (test_db == MYSQL)
        {
            return new MySqlTestingDatabaseManager(createJdbcConnectionProvider())
        }

        //  Don't manage tables for Oracle
        return new EmptyTestDatabaseManager()
    }

    private static class EmptyTestDatabaseManager implements TestingDatabaseManager
    {
        void setUp() throws SQLException
        {
        }

        void tearDown() throws SQLException
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
    }

    public static void setupDatabase() throws Exception
    {
        testingDatabaseManager.setUp()
        NCubeManager.NCubePersister = persister
        setupTestClassPaths()
    }

    public static void setupTestClassPaths() throws Exception
    {
        NCube cp = NCubeManager.getNCubeFromResource(TestNCubeManager.defaultSnapshotApp, 'sys.classpath.tests.json')
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, cp, TestNCubeManager.USER_ID)
        cp = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, 'sys.classpath.tests.json')
        NCubeManager.createCube(ApplicationID.defaultAppId, cp, TestNCubeManager.USER_ID)
    }

    public static void tearDownDatabase() throws Exception
    {
        try
        {
            NCubeManager.deleteCube TestNCubeManager.defaultSnapshotApp, 'sys.classpath', TestNCubeManager.USER_ID
        }
        catch (Exception ignored)
        { }

        try
        {
            NCubeManager.deleteCube ApplicationID.defaultAppId, 'sys.classpath', TestNCubeManager.USER_ID
        }
        catch (Exception ignored)
        { }

        testingDatabaseManager.tearDown()
        NCubeManager.clearCache()
    }
}
