package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

public class TestNCubeJdbcConnectionProvider
{
    @Test
    public void testConstructWithDataSourceAndCommit() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getValidDataSource());
        
        Object connection = nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be a jdbc connection...", connection instanceof Connection);
        assertTrue("Connection must be valid...", ((Connection)connection).isValid(1));
        
        nCubeConnectionProvider.commitTransaction();
        assertTrue("Connection must be closed...", ((Connection) connection).isClosed());
    }

    @Test
    public void testConstructWithDataSourceAndRollback() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getValidDataSource());

        Object connection = nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be a jdbc connection...", connection instanceof Connection);
        assertTrue("Connection must be valid...", ((Connection)connection).isValid(1));

        nCubeConnectionProvider.rollbackTransaction();
        assertTrue("Connection must be closed...", ((Connection)connection).isClosed());
    }

    @Test
    public void testConstructWithDataSourceAndException() throws Exception
    {       
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getInvalidMockDataSource());
        
        Exception testException = null;

        try
        {
            nCubeConnectionProvider.beginTransaction();
        }
        catch (Exception e)
        {
            testException = e;
        }
        
        assertNotNull("Must throw an exception when using an invalid DataSource...", testException);
        assertEquals("Incorrect exception message", "Unable to create connection from DataSource...", testException.getMessage());
    }


    //------------------ private methods ---------------------
    
    private DataSource getValidDataSource()
    {
        PoolProperties props = new PoolProperties();
        props.setUrl("jdbc:hsqldb:mem:testdb");
        props.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        props.setUsername("sa");
        props.setPassword("");
        
        org.apache.tomcat.jdbc.pool.DataSource tcDatasource = new org.apache.tomcat.jdbc.pool.DataSource(props);
        return tcDatasource;
    }   

    private static DataSource getInvalidMockDataSource()
    {
        DataSource ds = null;
        try
        {
            ds = mock(DataSource.class);
            when(ds.getConnection()).thenThrow(new SQLException());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        return ds;
    }
}
