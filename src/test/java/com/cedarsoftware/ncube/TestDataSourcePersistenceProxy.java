package com.cedarsoftware.ncube;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;

import static org.junit.Assert.assertEquals;

/**
 * Created by ken on 8/21/2014.
 */
public class TestDataSourcePersistenceProxy extends TestAbstractJdbcPersistenceProxy
{
    private DataSource getDataSource() {
        JDBCDataSource ds = new JDBCDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:testdb");
        ds.setUser("SA");
        ds.setPassword("");
        return ds;
    }

    @Test
    public void testConstructor() {
        try {
            new DataSourcePersistenceProxy(null, FooService.class, new JdbcFooService());
        } catch (NullPointerException e) {
            assertEquals("DataSource cannot be null...", e.getMessage());
        }
    }


    @Override
    public InvocationHandler buildInvocationHandler(Object o)
    {
        return new DataSourcePersistenceProxy(getDataSource(), FooService.class, o);
    }
}
