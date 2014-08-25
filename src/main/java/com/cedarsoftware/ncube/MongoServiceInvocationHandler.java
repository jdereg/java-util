package com.cedarsoftware.ncube;

import com.mongodb.Mongo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/23/2014.
 */
public class MongoServiceInvocationHandler implements InvocationHandler
{
    private Object _adapter;
    private Mongo _client;
    private Map<Method, Method> methods = new HashMap<Method, Method>();

    public MongoServiceInvocationHandler(Mongo client, Class service, Object adapter) {
        _client = client;
        _adapter = adapter;

        Method[] declaredMethods = service.getDeclaredMethods();

        //  Verify all adapted methods are available in our proxied class.
        for (Method m : declaredMethods) {
            Class[] adaptedParameters = getAdaptedParameters(m.getParameterTypes());
            try
            {
                Method adaptedMethod = adapter.getClass().getMethod(m.getName(), adaptedParameters);
                methods.put(m, adaptedMethod);
            } catch (NoSuchMethodException e) {
                String s = String.format("Adapter class '%s' does not implement '%s' with the parameters: %s", adapter.getClass().getName(), m.getName(), adaptedParameters.toString());
                throw new IllegalArgumentException(s, e);
            }
        }
    }

    public Class[] getAdaptedParameters(Class[] classes) {
        Class[] adaptedParameters = new Class[classes.length+1];
        adaptedParameters[0] = Mongo.class;
        System.arraycopy(classes, 0, adaptedParameters, 1, classes.length);
        return adaptedParameters;
    }

    public Object[] getAdaptedArguments(Object[] args) {
        Object[] adaptedArgs = new Object[args.length+1];
        adaptedArgs[0] = _client;
        System.arraycopy(args, 0, adaptedArgs, 1, args.length);
        return adaptedArgs;
    }


    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try {
            return methods.get(m).invoke(_adapter, getAdaptedArguments(args));
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
