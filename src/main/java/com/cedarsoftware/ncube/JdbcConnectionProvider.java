package com.cedarsoftware.ncube;

import java.sql.Connection;

/**
 * Created by kpartlow on 11/3/2014.
 */
public interface JdbcConnectionProvider
{

    /**
     * Obtains the connection.
     */
    public Connection getConnection();

    public void releaseConnection(Connection c);
}
