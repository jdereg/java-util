package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Implementation for JDBC using a JDBC connection and transactions.
 *
 * @author Chuck Rowland (pittsflyr@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */

public class NCubeJdbcConnectionProvider implements NCubeConnectionProvider
{
    private DataSource dataSource;
    private String databaseUrl;
    private String username;
    private String password;

    //TODO - better way to track connections than using a thread local
    private static final ThreadLocal<Connection> CONNECTION = new ThreadLocal<Connection>();

    /**
     * Constructs a new NCubeJdbcConnectionProvider with an initialized Datasource.
     */
    public NCubeJdbcConnectionProvider(DataSource datasource)
    {
        this.dataSource = datasource;
    }    

    /**
     * Constructs a new NCubeJdbcConnectionProvider with the input parameters needed to create a single database connection.
     *
     * @param driverClass - name of the database driver class
     * @param databaseUrl - database connection url
     * @param username - username of the account to be used to log into the database
     * @param password - password for the account to be used to log into the database
     * @throws java.lang.IllegalArgumentException - if connection is not a valid connection
     */
    public NCubeJdbcConnectionProvider(String driverClass, String databaseUrl, String username, String password)
    {
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to initialize driver class for input: " + driverClass, e);
            }
        }
    }

    @Override
    public Object beginTransaction()
    {
        //if this thread already has an active connection use
        if (CONNECTION.get() != null && isActiveConnection(CONNECTION.get()))
        {
            return CONNECTION.get();
        }        
            
        Connection connection = null;
        
        if (dataSource != null)
        {
            //using a datasource
            try
            {
                connection = dataSource.getConnection();
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Unable to create connection from DataSource...", e);
            }
        }
        else
        {
            //single connection from input connection params
            try
            {
                connection = DriverManager.getConnection(databaseUrl, username, password);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Unable to create connection from input connection parameters...", e);
            }
        }
        
        if (connection != null)
        {
            try
            {
                //auto commit always to false;
                //TODO support a parameter to set this property?
                connection.setAutoCommit(false);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Unable to set connection auto-commit to false...", e);
            }
            
            //set connection to thread local
            CONNECTION.set(connection);
        }
        
        return connection;
    }

    /**
     * @see NCubeJdbcConnectionProvider#commitTransaction()
     *
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    @Override
    public void commitTransaction()
    {
        Connection connection = CONNECTION.get();
        CONNECTION.set(null);

        if (!isActiveConnection(connection))
            throw new IllegalStateException("Unable to commit transaction. Current jdbc connection is invalid.");

        try
        {            
            connection.commit();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to commit active transaction...", e);
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Unable to close connection after committing transaction...", e);
            }
        }
    }

    /**
     * @see NCubeJdbcConnectionProvider#rollbackTransaction()
     *
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    @Override
    public void rollbackTransaction()
    {
        Connection connection = CONNECTION.get();
        CONNECTION.set(null);

        if (!isActiveConnection(connection))
            throw new IllegalStateException("Unable to rollback transaction. Current jdbc connection is invalid.");

        try
        {
            connection.rollback();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to rollback active transaction...", e);
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Unable to close connection after rolling back transaction...", e);
            }
        }
    }



    //-------------------- private -------------------------------

    private boolean isActiveConnection(Object connection)
    {
        if (connection == null || !(connection instanceof Connection))
            throw new IllegalStateException("Input connection is null and not valid...");

        try
        {
            return ((Connection)connection).isValid(1);
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to determine validity of jdbc connection...", e);
        }
    }
}
