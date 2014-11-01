package com.cedarsoftware.ncube;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by ken on 8/22/2014.
 */
public abstract class AbstractJdbcPersistenceProxy extends AbstractPersistenceProxy {

    public AbstractJdbcPersistenceProxy(Class c, Object o) {
        super(c, o);
    }

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try (Connection c = getConnectionAndSetAutoCommit())
        {
            try
            {
                Object o = methods.get(m).invoke(adapter, getAdaptedArguments(args, c));
                commit(c);
                return o;
            }
            catch (InvocationTargetException e)
            {
                rollback(c);
                throw e.getTargetException();
            }
        }
    }


    public Connection getConnectionAndSetAutoCommit() {
        try
        {
            Connection c = getConnection();
            c.setAutoCommit(false);
            return c;
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to get Connection...", e);
        }
    }

    public abstract Connection getConnection() throws SQLException;

    public Class getAddedClass() { return Connection.class; }

    /**
     * @see NCubeJdbcConnectionProvider#commitTransaction(java.sql.Connection)
     *
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    public static void commit(Connection c)
    {
        try
        {
            c.commit();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to commit active transaction...", e);
        }
    }

    /**
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    public static void rollback(Connection connection)
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to rollback active transaction...", e);
        }
    }


}
