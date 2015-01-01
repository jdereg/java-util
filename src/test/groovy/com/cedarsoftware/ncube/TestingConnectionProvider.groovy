package com.cedarsoftware.ncube

import java.sql.Connection
import java.sql.DriverManager

/**
 * Created by kpartlow on 11/3/2014.
 */
public class TestingConnectionProvider implements JdbcConnectionProvider
{
    private String databaseUrl
    private String user
    private String password

    public TestingConnectionProvider(String driverClass, String databaseUrl, String user, String password)
    {
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass)
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException('Could not load:  ' + driverClass, e)
            }
        }

        this.databaseUrl = databaseUrl
        this.user = user
        this.password = password
    }

    public Connection getConnection()
    {
        try
        {
            return DriverManager.getConnection(databaseUrl, user, password)
        }
        catch (Exception e)
        {
            throw new IllegalStateException('Could not crete connection: ' + databaseUrl, e)
        }
    }

    public void releaseConnection(Connection c)
    {
        try
        {
            c.close()
        }
        catch (Exception ignore)
        { }
    }
}
