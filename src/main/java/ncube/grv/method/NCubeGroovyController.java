package ncube.grv.method;

import ncube.grv.exp.NCubeGroovyExpression;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 * @author John DeRegnaucourt
 */
public class NCubeGroovyController extends NCubeGroovyExpression
{
    // LRU Cache reflective method look ups
    private static final Map<String, Method> methodCache = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };

    public NCubeGroovyController(){}

    /**
     * Run the groovy method named by the column on the 'method' axis.
     *  The args passed in contain the 'input' Map, 'output' Map, 'ncube',
     *  and the 'stack.'
     */
    public Object run(Map args, String signature) throws Exception
    {
        super.run(args, signature);
        String methodKey = getClass().getName() + '.' + input.get("method") + '.' + signature;
        Method method = methodCache.get(methodKey);

        if (method == null)
        {
            synchronized (NCubeGroovyController.class)
            {
                method = methodCache.get(methodKey);
                if (method == null)
                {
                    method = getClass().getMethod((String) input.get("method"));
                    methodCache.put(methodKey, method);
                }
            }
        }
        // Invoke the Groovy method named in the input Map at the key 'method'.
        return method.invoke(this);
    }
}