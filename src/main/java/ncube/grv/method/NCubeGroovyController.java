package ncube.grv.method;

import com.cedarsoftware.ncube.Advice;
import ncube.grv.exp.NCubeGroovyExpression;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class NCubeGroovyController extends NCubeGroovyExpression
{
    // LRU Cache reflective method look ups
    private static final Map<String, Method> methodCache = new LinkedHashMap<String, Method>()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };

    public static void clearCache()
    {
        methodCache.clear();
    }
    /**
     * Run the groovy method named by the column on the 'method' axis.
     * @param signature String SHA1 of the source file.  This is used to
     * ensure the method cache 'key' is unique.  If someone uses the same
     * package and class name for two classes, but their source is different,
     * their methods will be keyed uniquely in the cache.
     */
    public Object run(String signature) throws Exception
    {
        String methodName = (String) input.get("method");
        String methodKey = methodName + '.' + signature;
        Method method = methodCache.get(methodKey);

        if (method == null)
        {
            synchronized (NCubeGroovyController.class)
            {
                method = methodCache.get(methodKey);
                if (method == null)
                {
                    method = getClass().getMethod(methodName);
                    methodCache.put(methodKey, method);
                }
            }
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
        Object ret = method.invoke(this);

        // If 'around' Advice has been added to n-cube, invoke it after calling Groovy method
        // or expression
        int len = advices.size();
        for (int i = len - 1; i >= 0; i--)
        {
            Advice advice = advices.get(i);
            advice.after(method, ncube, input, output, ret);
        }
        return ret;
    }
}