package com.cedarsoftware.ncube;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertEquals("Incorrect exception message", "Unable to get Connection...", testException.getMessage());
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
    public void testConstructWithMissingDriver() throws Exception
    {
        try
        {
            new NCubeJdbcConnectionProvider("com.some.class.not.found","","","");
            fail();
        }
        catch (RuntimeException e)
        {
            assertEquals("Incorrect Exception Thrown", ClassNotFoundException.class, e.getCause().getClass());
            assertEquals("Unable to locate driver class: com.some.class.not.found", e.getMessage());
        }
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
        assertEquals("Incorrect exception message", "Unable to get Connection...", testException.getMessage());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructWithNullDataSource() {
        new NCubeJdbcConnectionProvider(null);
    }

    @Test
    public void testBeginTransactionWithExceptionSettingAutoCommit() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);

        when(ds.getConnection()).thenReturn(c);
        doThrow(new SQLException()).when(c).setAutoCommit(false);

        NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);

        try {
            p.beginTransaction();
        } catch (RuntimeException e) {
            assertEquals("Unable to set connection auto-commit to false...", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testCommitTransactionWithSQLExceptionOnCommit() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);

        when(ds.getConnection()).thenReturn(c);
        doThrow(new SQLException()).when(c).commit();

        NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);

        try {
            p.commitTransaction(c);
        } catch (RuntimeException e) {
            assertEquals("Unable to commit active transaction...", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testCommitTransactionWithSQLExceptionOnClose() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);

        when(ds.getConnection()).thenReturn(c);
        doThrow(new SQLException()).when(c).close();

        NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);

        try {
            p.commitTransaction(c);
        } catch (RuntimeException e) {
            assertEquals("Unable to close connection after committing transaction...", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testRollbackTransactionWithSQLExceptionOnClose() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);

        when(ds.getConnection()).thenReturn(c);
        doThrow(new SQLException()).when(c).close();

        NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);

        try {
            p.rollbackTransaction(c);
        } catch (RuntimeException e) {
            assertEquals("Unable to close connection after rolling back transaction...", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }


    @Test
    public void testRollbackTransactionWithSQLExceptionOnRollback() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);

        when(ds.getConnection()).thenReturn(c);
        doThrow(new SQLException()).when(c).rollback();

        NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);

        try {
            p.rollbackTransaction(c);
        } catch (RuntimeException e) {
            assertEquals("Unable to rollback active transaction...", e.getMessage());
            assertEquals(SQLException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testCommitTransactionWithNullConnection() throws Exception {
        try {
            DataSource ds = mock(DataSource.class);
            NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);
            p.commitTransaction(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Unable to commit transaction.  Connection is null.", e.getMessage());
        }
    }

    @Test
    public void testRollbackTransactionWithNullConnection() throws Exception {
        try {
            DataSource ds = mock(DataSource.class);
            NCubeConnectionProvider p = new NCubeJdbcConnectionProvider(ds);
            p.rollbackTransaction(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Unable to rollback transaction. Connection is null.", e.getMessage());
        }
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
