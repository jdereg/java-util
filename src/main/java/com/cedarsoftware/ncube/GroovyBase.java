package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleStop;
import groovy.lang.GroovyClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    static final GroovyClassLoader groovyClassLoader = new GroovyClassLoader(GroovyBase.class.getClassLoader());
    static final Map<String, Class> compiledClasses = new LinkedHashMap<String, Class>();
    static final Map<String, Constructor> constructorMap = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };
    static final Map<String, Method> methodMap = new LinkedHashMap()
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            return size() > 500;
        }
    };

    public GroovyBase(String cmd, boolean cache)
    {
        super(cmd, cache);
    }

    protected abstract String buildGroovy(String theirGroovy, String cubeName);

    protected abstract String getMethodToExecute(Map args);

    protected void preRun(Map args)
    {
        super.preRun(args);
        NCube ncube = (NCube) args.get("ncube");
        compileIfNeeded(ncube.getName());
    }

    protected Object runFinal(Map args)
    {
        try
        {
            return executeGroovy(args);
        }
        catch(InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof CoordinateNotFoundException)
            {
                throw (RuntimeException) cause;
            }
            else if (cause instanceof RuleStop)
            {
                throw (RuleStop) cause;
            }
            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(args) + "()", e) ;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error occurred invoking method " + getMethodToExecute(args) + "()", e);
        }
    }

    /**
     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
     * Keeping around for quick no-cache testing.
     */

//    protected Object executeGroovy(final Map args) throws Exception
//    {
//        Constructor c = getRunnableCode().getConstructor();
//        final Object exp = c.newInstance();
//        Method runMethod = getRunnableCode().getMethod("run", Map.class, String.class);
//        return runMethod.invoke(exp, args, getCmdHash());
//    }

    /**
     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
     */
    protected Object executeGroovy(final Map args) throws Exception
    {
        final String cacheKey = getCmdHash();
        Constructor c = constructorMap.get(cacheKey);
        if (c == null)
        {
            synchronized(GroovyBase.class)
            {
                c = constructorMap.get(cacheKey);
                if (c == null)
                {
                    c = getRunnableCode().getConstructor();
                    constructorMap.put(cacheKey, c);
                }
            }
        }

        final Object exp = c.newInstance();
        Method runMethod = methodMap.get(cacheKey);

        if (runMethod == null)
        {
            synchronized(GroovyBase.class)
            {
                runMethod = methodMap.get(cacheKey);
                if (runMethod == null)
                {
                    runMethod = getRunnableCode().getMethod("run", Map.class, String.class);
                    methodMap.put(cacheKey, runMethod);
                }
            }
        }
        return runMethod.invoke(exp, args, getCmdHash());
    }

    /**
     * Conditionally compile the passed in command.  If it is already compiled, this method
     * immediately returns.  Insta-check because it is just a ref == null check.
     */
    private void compileIfNeeded(String cubeName)
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

                if (compiledClasses.containsKey(getCmdHash()))
                {   // Already been compiled, re-use class
                    setRunnableCode(compiledClasses.get(getCmdHash()));
                    return;
                }
                try
                {
                    compile(cubeName);
                }
                catch (Exception e)
                {
                    setCompileErrorMsg("Failed to compile Groovy Command '" + getCmd() + "', NCube '" + cubeName + "'");
                    throw new IllegalArgumentException(getCompileErrorMsg(), e);
                }
            }
        }
    }

    protected void compile(String cubeName) throws Exception
    {
        String groovySource = expandNCubeShortCuts(buildGroovy(getCmd(), cubeName));
        setRunnableCode(groovyClassLoader.parseClass(groovySource));
        compiledClasses.put(getCmdHash(), getRunnableCode());
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

    static void getCubeNamesFromText(final Set<String> cubeNames, final String text)
    {
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

    public Set<String> getImports(String text, StringBuilder newGroovy)
    {
        Matcher m = Regexes.importPattern.matcher(text);
        Set<String> importNames = new LinkedHashSet<String>();
        while (m.find())
        {
            importNames.add(m.group(0));  // based on Regex pattern - if pattern changes, this could change
        }

        m.reset();
        newGroovy.append(m.replaceAll(""));
        return importNames;
    }
}
