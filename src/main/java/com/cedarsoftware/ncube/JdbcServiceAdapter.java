package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ken on 8/22/2014.
 */
public class JdbcServiceAdapter implements InvocationHandler {

    private Object _adapter;
    private DataSource _source;
    private Map<Method, Method> methods = new HashMap<Method, Method>();

    public JdbcServiceAdapter(DataSource source, Class service, Object adapter) {
        _source = source;
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
        adaptedParameters[0] = Connection.class;
        System.arraycopy(classes, 0, adaptedParameters, 1, classes.length);
        return adaptedParameters;
    }

    public Object[] getAdaptedArguments(Object[] args, Connection c) {
        Object[] adaptedArgs = new Object[args.length+1];
        adaptedArgs[0] = c;
        System.arraycopy(args, 0, adaptedArgs, 1, args.length);
        return adaptedArgs;
    }


    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try {
            try (Connection c = _source.getConnection()) {
                return methods.get(m).invoke(_adapter, getAdaptedArguments(args, c));
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
