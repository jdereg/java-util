package com.cedarsoftware.ncube;

import com.mongodb.Mongo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by kpartlow on 8/23/2014.
 */
public class MongoServiceInvocationHandler extends AbstractPersistenceProxy
{
    private Mongo _client;

    public MongoServiceInvocationHandler(Mongo client, Class service, Object adapter) {
        super(service, adapter);
        _client = client;
    }

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try {
            return methods.get(m).invoke(_adapter, getAdaptedArguments(args, _client));
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public Class getAddedClass() { return Mongo.class; }

}
