package ncube.grv.method

import ncube.grv.exp.NCubeGroovyExpression

import java.lang.reflect.Method;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 * @author John DeRegnaucourt
 */
class NCubeGroovyController extends NCubeGroovyExpression
{
    // LRU Cache reflective method look ups
    static class MethodMap<K, V> extends LinkedHashMap<K, V>
    {
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
        {
            return size() > 500;
        }
    };

    private static final MethodMap<String, Method> methodCache = new MethodMap();

    NCubeGroovyController(){}

    def run(Map args, String signature) throws Exception
    {
        super.run(args, signature);
        String methodKey = getClass().getName() + '.' + input.method + '.' + signature;
        Method method = methodCache.get(methodKey);

        if (method == null)
        {
            synchronized (NCubeGroovyController.class)
            {
                method = methodCache.get(methodKey);
                if (method == null)
                {
                    method = getClass().getMethod(input.method);
                    methodCache.put(methodKey, method);
                }
            }
        }
        // Invoke the Groovy method named in the input Map at the key 'method'.
        return method.invoke(this);
    }
}