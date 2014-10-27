package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Created by kpartlow on 10/25/2014.
 */
public class BasicDataSource implements DataSource
{
    private String _databaseUrl;
    private String _userName;
    private String _password;

    private PrintWriter _writer;
    private int _loginTimeout;

    public BasicDataSource(String driverClass, String databaseUrl, String userName, String password)
    {
        //  If using a JDBC 4.0 driver we don't need to load the driver Class into memory anymore
        //  because they will have a META-INF/services file specifying the driver classname to be loaded.
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to locate driver class: " + driverClass, e);
            }
        }

        _databaseUrl = databaseUrl;
        _userName = userName;
        _password = password;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(_databaseUrl, _userName, _password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return DriverManager.getConnection(_databaseUrl, username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return _writer;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        _writer = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
        _loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return _loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException("parent logger is not supported");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("BasicDataSource is not a wrapper.");
    }
}
