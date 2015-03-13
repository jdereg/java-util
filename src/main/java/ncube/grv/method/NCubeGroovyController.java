package ncube.grv.method;

import com.cedarsoftware.ncube.Advice;
import com.cedarsoftware.ncube.ApplicationID;
import ncube.grv.exp.NCubeGroovyExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 * @see com.cedarsoftware.ncube.GroovyBase
 */
public class NCubeGroovyController extends NCubeGroovyExpression
{
    private static final Logger LOG = LogManager.getLogger(NCubeGroovyController.class);

    // Cache reflective method look ups
    private static final Map<ApplicationID, Map<String, Method>> methodCache = new ConcurrentHashMap<>();

    public static void clearCache(ApplicationID appId)
    {
        Map<String, Method> methodMap = getMethodCache(appId);
        methodMap.clear();
    }

    /**
     * Fetch the Map of n-cubes for the given ApplicationID.  If no
     * cache yet exists, a new empty cache is added.
     */
    private static Map<String, Method> getMethodCache(ApplicationID appId)
    {
        Map<String, Method> methodMap = methodCache.get(appId);

        if (methodMap == null)
        {
            synchronized (methodCache)
            {
                methodMap = methodCache.get(appId);
                if (methodMap == null)
                {
                    methodMap = new ConcurrentHashMap<>();
                    methodCache.put(appId, methodMap);
                }
            }
        }
        return methodMap;
    }

    /**
     * Run the groovy method named by the column on the 'method' axis.
     *
     * @param signature String SHA1 of the source file.  This is used to
     *                  ensure the method cache 'key' is unique.  If someone uses the same
     *                  package and class name for two classes, but their source is different,
     *                  their methods will be keyed uniquely in the cache.
     */
    public Object run(String signature) throws Throwable
    {
        String methodName = (String) input.get("method");
        String methodKey = methodName + '.' + signature;
        Map<String, Method> methodMap = getMethodCache(ncube.getApplicationID());
        Method method = methodMap.get(methodKey);

        if (method == null)
        {
            method = getClass().getMethod(methodName);
            methodMap.put(methodKey, method);
        }

        // If 'around' Advice has been added to n-cube, invoke it before calling Groovy method
        // or expression
        List<Advice> advices = ncube.getAdvices(methodName);
        for (Advice advice : advices)
        {
            if (!advice.before(method, ncube, input, output))
            {
                return null;
            }
        }

        // Invoke the Groovy method named in the input Map at the key 'method'.
        Throwable t = null;
        Object ret = null;

        try
        {
            ret = method.invoke(this);
        }
        catch (ThreadDeath e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            t = e;  // Save exception thrown by method call
        }

        // If 'around' Advice has been added to n-cube, invoke it after calling Groovy method
        // or expression
        int len = advices.size();
        for (int i = len - 1; i >= 0; i--)
        {
            Advice advice = advices.get(i);
            try
            {
                advice.after(method, ncube, input, output, ret, t);  // pass exception (t) to advice (or null)
            }
            catch (Exception e)
            {
                LOG.error("An exception occurred calling advice: " + advice.getName() + " on method: " + method.getName(), e);
            }
        }
        if (t == null)
        {
            return ret;
        }
        throw t;
    }
}