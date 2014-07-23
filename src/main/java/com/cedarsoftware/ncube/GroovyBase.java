package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleStop;
import com.cedarsoftware.util.StringUtilities;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Base class for Groovy CommandCells.
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
public abstract class GroovyBase extends UrlCommandCell
{
    static final Map<String, Class> compiledClasses = new LinkedHashMap<String, Class>();
    static final Map<String, Constructor> constructorMap = new LinkedHashMap<String, Constructor>()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };
    static final Map<String, Method> initMethodMap = new LinkedHashMap<String, Method>()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };
    static final Map<String, Method> methodMap = new LinkedHashMap<String, Method>()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };

    public GroovyBase(String cmd, String url)
    {
        super(cmd, url, true);
    }

    protected abstract String buildGroovy(String theirGroovy, String cubeName, String cmdHash);

    protected abstract String getMethodToExecute(Map args);

    public static void clearCache()
    {
        compiledClasses.clear(); // Free up stored references to Compiled Classes
        constructorMap.clear();  // free up stored references to Compiled Constructors
        methodMap.clear(); // free up stored references to Compiled Methods
        initMethodMap.clear();  // free up stored references to NCubeGroovyExpression.init() methods
    }

    protected Object executeInternal(Object data, Map args)
    {
        String cubeName = getNCube(args).getName();
        try
        {
            return executeGroovy(args, getCmdHash(data == null ? getUrl() : data.toString()));
        }
        catch(InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof CoordinateNotFoundException)
            {
                throw (CoordinateNotFoundException) cause;
            }
            else if (cause instanceof RuleStop)
            {
                throw (RuleStop) cause;
            }
            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(args) + "(), n-cube '" + cubeName + "', input: " + args.get("input"), e) ;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error occurred invoking method " + getMethodToExecute(args) + "(), n-cube '" + cubeName + "', input: " + args.get("input"), e);
        }
    }

    /**
     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
     */
    protected Object executeGroovy(final Map args, String cmdHash) throws Exception
    {
        // Step 1: Construct the object (use default constructor)
        final String cacheKey = cmdHash;
        Constructor c = constructorMap.get(cacheKey);
        if (c == null)
        {
            synchronized (GroovyBase.class)
            {
                c = constructorMap.get(cacheKey);
                if (c == null)
                {
                    c = getRunnableCode().getConstructor();
                    constructorMap.put(cacheKey, c);
                }
            }
        }

        final Object instance = c.newInstance();

        // Step 2: Call the inherited 'init(Map args)' method.  This technique saves the subclasses from having
        // to implement a duplicate constructor that routes the Map up (Constructors are not inherited).
        Method initMethod = initMethodMap.get(cacheKey);
        if (initMethod == null)
        {
            synchronized (GroovyBase.class)
            {
                initMethod = initMethodMap.get(cacheKey);
                if (initMethod == null)
                {
                    initMethod = getRunnableCode().getMethod("init", Map.class);
                    initMethodMap.put(cacheKey, initMethod);
                }
            }
        }

        initMethod.invoke(instance, args);

        // Step 3: Call the run() [for expressions] or run(Signature) [for controllers] method
        Method runMethod = methodMap.get(cacheKey);

        if (runMethod == null)
        {
            synchronized (GroovyBase.class)
            {
                runMethod = methodMap.get(cacheKey);
                if (runMethod == null)
                {
                    runMethod = getRunMethod();
                    methodMap.put(cacheKey, runMethod);
                }
            }
        }

        return invokeRunMethod(runMethod, instance, args, cmdHash);
    }

    protected abstract Method getRunMethod() throws NoSuchMethodException;

    protected abstract Object invokeRunMethod(Method runMethod, Object instance, Map args, String cmdHash) throws Exception;

    public Object fetch(Map args)
    {
        return null;
    }

    /**
     * Conditionally compile the passed in command.  If it is already compiled, this method
     * immediately returns.  Insta-check because it is just a ref == null check.
     */
    public void prepare(Object data, Map ctx)
    {
        if (getRunnableCode() == null)
        {   // Not yet compiled, compile the cell (Lazy compilation)
            synchronized(GroovyBase.class)
            {
                if (getRunnableCode() != null)
                {   // More than one thread saw the empty code, but only let the first thread
                    // call setRunnableCode().
                    return;
                }

                //  This order is important because data can be null before the url is loaded
                //  and then be present afterwards.  we'd have two different hashes for the same object.
                String cmdHash = getCmdHash(getUrl() == null ? data.toString() : getUrl());
                if (compiledClasses.containsKey(cmdHash))
                {   // Already been compiled, re-use class
                    setRunnableCode(compiledClasses.get(cmdHash));
                    return;
                }
                NCube cube = getNCube(ctx);
                try
                {
                    compile(cube, cmdHash);
                }
                catch (Exception e)
                {
                    setErrorMessage("Failed to compile Groovy Command '" + getCmd() + "', NCube '" + cube.getName() + "'");
                    throw new IllegalArgumentException(getErrorMessage(), e);
                }
            }
        }
    }

    protected void compile(NCube cube, String cmdHash) throws Exception
    {
        String url = getUrl();
        boolean isUrlUsed = StringUtilities.hasContent(url);
        GroovyClassLoader urlLoader = (GroovyClassLoader)NCubeManager.getUrlClassLoader(cube.getVersion());

        if (isUrlUsed)
        {
            URL groovySourceUrl = urlLoader.getResource(url);

            if (groovySourceUrl == null)
            {
                throw new IllegalArgumentException("Groovy code source URL is non-relative, add base url to GroovyClassLoader on NCubeManager.setUrlClassLoader(): " + url);
            }

            GroovyCodeSource gcs = new GroovyCodeSource(groovySourceUrl);
            gcs.setCachable(false);

            setRunnableCode(urlLoader.parseClass(gcs));
        }
        else
        {
            String groovySource = expandNCubeShortCuts(buildGroovy(getCmd(), cube.getName(), cmdHash));
            setRunnableCode(urlLoader.parseClass(groovySource));
        }
        compiledClasses.put(cmdHash, getRunnableCode());
    }

    static String expandNCubeShortCuts(String groovy)
    {
        Matcher m = Regexes.groovyAbsRefCubeCellPattern.matcher(groovy);
        String exp = m.replaceAll("$1getFixedCubeCell('$2',$3)");

        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getFixedCubeCell('$2',$3)");

        m = Regexes.groovyAbsRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1getFixedCell($2)");

        m = Regexes.groovyAbsRefCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getFixedCell($2)");

        m = Regexes.groovyRelRefCubeCellPattern.matcher(exp);
        exp = m.replaceAll("$1getRelativeCubeCell('$2',$3)");

        m = Regexes.groovyRelRefCubeCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getRelativeCubeCell('$2',$3)");

        m = Regexes.groovyRelRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1getRelativeCell($2)");

        m = Regexes.groovyRelRefCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getRelativeCell($2)");
        return exp;
    }

    public void getCubeNamesFromCommandText(final Set<String> cubeNames)
    {
        getCubeNamesFromText(cubeNames, getCmd());
    }

    // TODO: Need to check for cubeName reference in runRuleCube(cubeName, input)
    static void getCubeNamesFromText(final Set<String> cubeNames, final String text)
    {
        if (StringUtilities.isEmpty(text))
        {
            return;
        }

        Matcher m = Regexes.groovyAbsRefCubeCellPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyRelRefCubeCellPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyRelRefCubeCellPatternA.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyExplicitCubeRefPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(1));  // based on Regex pattern - if pattern changes, this could change
        }
    }

    /**
     * Find all occurrences of 'input.variableName' in the Groovy code
     * and add the variableName as a scope (key).
     * @param scopeKeys Set to add required scope keys to.
     */
    public void getScopeKeys(Set<String> scopeKeys)
    {
        Matcher m = Regexes.inputVar.matcher(getCmd());
        while (m.find())
        {
            scopeKeys.add(m.group(2));
        }
    }

    public static Set<String> getImports(String text, StringBuilder newGroovy)
    {
        Matcher m = Regexes.importPattern.matcher(text);
        Set<String> importNames = new LinkedHashSet();
        while (m.find())
        {
            importNames.add(m.group(0));  // based on Regex pattern - if pattern changes, this could change
        }

        m.reset();
        newGroovy.append(m.replaceAll(""));
        return importNames;
    }
}
