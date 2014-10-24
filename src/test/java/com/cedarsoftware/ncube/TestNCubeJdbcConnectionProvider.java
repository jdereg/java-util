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
        
        Connection connection = (Connection)nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be valid...", connection.isValid(1));
        
        nCubeConnectionProvider.commitTransaction(connection);
        assertTrue("Connection must be closed...", connection.isClosed());
    }

    @Test
    public void testConstructWithDataSourceAndRollback() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getValidDataSource());

        Connection connection = (Connection)nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be valid...", connection.isValid(1));

        nCubeConnectionProvider.rollbackTransaction(connection);
        assertTrue("Connection must be closed...", connection.isClosed());
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
    
    @Test
    public void testConstructWithParamsAndCommit() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider("org.hsqldb.jdbc.JDBCDriver","jdbc:hsqldb:mem:testdb","sa","");

        Connection connection = (Connection)nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be valid...", connection.isValid(1));

        nCubeConnectionProvider.commitTransaction(connection);
        assertTrue("Connection must be closed...", connection.isClosed());
    }

    @Test
    public void testConstructWithParamsAndRollback() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider("org.hsqldb.jdbc.JDBCDriver","jdbc:hsqldb:mem:testdb","sa","");

        Connection connection = (Connection)nCubeConnectionProvider.beginTransaction();
        assertTrue("Connection must be valid...", connection.isValid(1));

        nCubeConnectionProvider.rollbackTransaction(connection);
        assertTrue("Connection must be closed...", connection.isClosed());
    }

    @Test
    public void testConstructWithParamsAnException() throws Exception
    {
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider("org.hsqldb.jdbc.JDBCDriver","","","");

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
        assertEquals("Incorrect exception message", "Unable to create connection from input connection parameters...", testException.getMessage());
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
