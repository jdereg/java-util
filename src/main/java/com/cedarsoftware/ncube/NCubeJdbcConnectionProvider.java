package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for JDBC using a JDBC connection.
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
    Map<ContextKey,Object> connectionContext = new HashMap<>();

    /**
     * Constructs a new NCubeJdbcConnectionProvider with the input connection added to the connection context Map.
     *
     * @param connection - java.sql.Connection
     * @throws java.lang.IllegalArgumentException - if connection is not a valid connection
     */
    public NCubeJdbcConnectionProvider(Connection connection)
    {
        if (!isActiveConnection(connection))
            throw new IllegalStateException("Input jdbc connection is not valid, check state of connection prior to instantiating connection provider...");

        connectionContext.put(ContextKey.JDBC_CONNECTION, connection);
    }

    /**
     * @see NCubeConnectionProvider#getConnectionContext()
     *
     * @return Map - jdbc connection context
     */
    @Override
    public Map<ContextKey, Object> getConnectionContext()
    {
        return connectionContext;
    }

    /**
     * @see NCubeJdbcConnectionProvider#commitTransaction()
     *
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    @Override
    public void commitTransaction()
    {
        if (!isActiveConnection(connectionContext.get(ContextKey.JDBC_CONNECTION)))
            throw new IllegalStateException("Unable to commit transaction. Current jdbc connection is invalid, provide active connection to connection context.");

        Connection connection = (Connection)(connectionContext.get(ContextKey.JDBC_CONNECTION));

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
        if (!isActiveConnection(connectionContext.get(ContextKey.JDBC_CONNECTION)))
            throw new IllegalStateException("Unable to rollback transaction. Current jdbc connection is invalid and may have been previously closed.");

        Connection connection = (Connection)(connectionContext.get(ContextKey.JDBC_CONNECTION));

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
