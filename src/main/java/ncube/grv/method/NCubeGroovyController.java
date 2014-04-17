package ncube.grv.method;

import ncube.grv.exp.NCubeGroovyExpression;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
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
    private static final Map<String, Method> methodCache = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };

    /**
     * Run the groovy method named by the column on the 'method' axis.
     * @param signature String SHA1 of the source file.  This is used to
     * ensure the method cache 'key' is unique.  If someone uses the same
     * package and class name for two classes, but their source is different,
     * their methods will be keyed uniquely in the cache.
     */
    public Object run(String signature) throws Exception
    {
        String methodKey = (String) input.get("method") + '.' + signature;
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