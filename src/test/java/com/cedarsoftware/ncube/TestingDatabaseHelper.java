package com.cedarsoftware.ncube;

import java.sql.SQLException;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class TestingDatabaseHelper
{
    public static int MYSQL = 1;
    public static int HSQLDB = 2;
    public static int ORACLE = 3;

    private static Object getProxyInstance(int db) throws Exception
    {
        if (db == HSQLDB) {
            return HsqlTestingDatabaseManager.class.newInstance();
        }

        if (db == MYSQL) {
            return MySqlTestingDatabaseManager.class.newInstance();
        }

        throw new IllegalArgumentException("Unknown Database:  " + db);
    }

    public static NCubePersister getPersister(int db) throws Exception
    {
        return new NCubeJdbcPersisterAdapter(createJdbcConnectionProvider(db));
    }

    public static JdbcConnectionProvider createJdbcConnectionProvider(int db) throws Exception
    {
        if (db == HSQLDB) {
            return new TestingConnectionProvider(null, "jdbc:hsqldb:mem:testdb", "sa", "");
        }

        if (db == MYSQL) {
            return new TestingConnectionProvider(null, "jdbc:mysql://127.0.0.1:3306/ncube?autoCommit=true", "ncube", "ncube");
        }

        if (db == ORACLE) {
            return new TestingConnectionProvider("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd", "ra_desktop", "p0rtal");
        }

        throw new IllegalArgumentException("Unknown Database:  " + db);
    }

    public static TestingDatabaseManager getTestingDatabaseManager(int db) throws Exception
    {
        if (db == HSQLDB) {
            return new HsqlTestingDatabaseManager(createJdbcConnectionProvider(db));
        }

        if (db == MYSQL) {
            return new MySqlTestingDatabaseManager(createJdbcConnectionProvider(db));
        }

        //  Don't manager tables for Oracle
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
        };
    }
}
