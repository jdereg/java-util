package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ProxyFactory;

import java.lang.reflect.InvocationHandler;

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

    public static <T> T createJdbcProxy(int db, Class<T> c, Object o) throws Exception
    {
        InvocationHandler h = null;

        if (db == HSQLDB) {
            h = new JdbcPersistenceProxy(null, "jdbc:hsqldb:mem:testdb", "sa", "", c, o);
        }

        if (db == MYSQL) {
            h = new JdbcPersistenceProxy(null, "jdbc:mysql://127.0.0.1:3306/ncube", "ncube", "ncube", c, o);
        }

        if (db == ORACLE) {
            h = new JdbcPersistenceProxy("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@cvgli59.td.afg:1526:uwdeskd", "ra_desktop", "p0rtal",c, o);
        }

        if (h == null)
        {
            throw new IllegalArgumentException("Unknown Database:  " + db);
        }

        return ProxyFactory.create(c, h);
    }

    public static NCubePersister getPersister(int db) throws Exception
    {
        return createJdbcProxy(db, NCubePersister.class, new NCubeJdbcPersister());
    }

    public static TestingDatabaseManager getTestingDatabaseManager(int db) throws Exception
    {
        return createJdbcProxy(db, TestingDatabaseManager.class, getProxyInstance(db));
    }
}
