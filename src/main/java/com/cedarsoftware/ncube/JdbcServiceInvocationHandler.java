package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;

/**
 * Created by ken on 8/22/2014.
 */
public class JdbcServiceInvocationHandler extends AbstractPersistenceProxy {

    private DataSource _source;

    public JdbcServiceInvocationHandler(DataSource source, Class service, Object adapter) {
        super(service, adapter);
        _source = source;
    }

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try (Connection c = _source.getConnection())
        {
            return methods.get(m).invoke(_adapter, getAdaptedArguments(args, c));
        }
        catch (InvocationTargetException e)
        {
            throw e.getTargetException();
        }
    }

    @Override
    public Class getAddedClass() { return Connection.class; }
}
