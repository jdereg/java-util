package com.cedarsoftware.ncube;

import java.sql.Connection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.cedarsoftware.ncube.NCubeConnectionProvider.ContextKey;

public class TestNCubeJdbcConnectionProvider
{
    @Test
    public void testConstruct()
    {
        //test empty constructor
        NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider();
        assertNotNull("Connection context must not be null...", nCubeConnectionProvider.getConnectionContext());

        //test constructor with connection
        nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getValidMockConnection());
        assertNotNull("Connection context must not be null...", nCubeConnectionProvider.getConnectionContext());
        assertNotNull("Connection context must have jdbc connection...", nCubeConnectionProvider.getConnectionContext().get(ContextKey.JDBC_CONNECTION));
    }

    @Test
    public void testConstructWithException()
    {
        Exception testException = null;

        //test construct
        try
        {
            NCubeConnectionProvider nCubeConnectionProvider = new NCubeJdbcConnectionProvider(getInvalidMockConnection());
        }
        catch (Exception e)
        {
            testException = e;
        }

        assertTrue(testException != null && testException instanceof IllegalArgumentException);
    }

    @Test
    public void testCommitTransaction()
    {

    }

    @Test
    public void testRollbackTransaction()
    {

    }


    //------------------ private methods ---------------------

    private static Connection getValidMockConnection()
    {
        Connection connection = null;
        try
        {
            connection = mock(Connection.class);
            when(connection.isValid(anyInt())).thenReturn(true);
            return connection;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        return connection;
    }

    private static Connection getInvalidMockConnection()
    {
        Connection connection = null;
        try
        {
            connection = mock(Connection.class);
            when(connection.isValid(anyInt())).thenReturn(false);
            return connection;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        return connection;
    }
}
