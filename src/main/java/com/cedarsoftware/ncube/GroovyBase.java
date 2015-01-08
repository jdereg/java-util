package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleJump;
import com.cedarsoftware.ncube.exception.RuleStop;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyClassLoader;
import ncube.grv.exp.NCubeGroovyExpression;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    protected transient String cmdHash;
    private volatile transient Class runnableCode = null;
    static final Map<ApplicationID, Map<String, Class>>  compiledClasses = new ConcurrentHashMap<>();
    static final Map<ApplicationID, Map<String, Constructor>> constructorCache = new ConcurrentHashMap<>();
    static final Map<ApplicationID, Map<String, Method>> runMethodCache = new ConcurrentHashMap<>();

    //  Private constructor only for serialization.
    protected GroovyBase() {}

    public GroovyBase(String cmd, String url)
    {
        super(cmd, url);
    }

    public Class getRunnableCode()
    {
        return runnableCode;
    }

    public void setRunnableCode(Class runnableCode)
    {
        this.runnableCode = runnableCode;
    }

    public boolean isCacheable()
    {
        return true;
    }

    public Object execute(Map<String, Object> ctx)
    {
        failOnErrors();

        Object data;

        if (getUrl() == null)
        {
            data = getCmd();
        }
        else
        {
            expandUrl(ctx);
            data = null;
        }

        prepare(data, ctx);
        return executeInternal(ctx);
    }

    protected abstract String buildGroovy(String theirGroovy, String cubeName);

    protected abstract String getMethodToExecute(Map args);

    static void clearCache(ApplicationID appId)
    {
        Map<String, Class> compiledMap = getCompiledClassesCache(appId);
        compiledMap.clear();

        Map<String, Constructor> constructorMap = getConstructorCache(appId);
        constructorMap.clear();

        Map<String, Method> runMethodMap = getRunMethodCache(appId);
        runMethodMap.clear();
    }

    private static Map<String, Class> getCompiledClassesCache(ApplicationID appId)
    {
        Map<String, Class> classesMap = compiledClasses.get(appId);

        if (classesMap == null)
        {
            synchronized (compiledClasses)
            {
                classesMap = compiledClasses.get(appId);
                if (classesMap == null)
                {
                    classesMap = new ConcurrentHashMap<>();
                    compiledClasses.put(appId, classesMap);
                }
            }
        }
        return classesMap;
    }

    private static Map<String, Constructor> getConstructorCache(ApplicationID appId)
    {
        Map<String, Constructor> classesMap = constructorCache.get(appId);

        if (classesMap == null)
        {
            synchronized (constructorCache)
            {
                classesMap = constructorCache.get(appId);
                if (classesMap == null)
                {
                    classesMap = new ConcurrentHashMap<>();
                    constructorCache.put(appId, classesMap);
                }
            }
        }
        return classesMap;
    }

    private static Map<String, Method> getRunMethodCache(ApplicationID appId)
    {
        Map<String, Method> runMethodMap = runMethodCache.get(appId);

        if (runMethodMap == null)
        {
            synchronized (runMethodCache)
            {
                runMethodMap = runMethodCache.get(appId);
                if (runMethodMap == null)
                {
                    runMethodMap = new ConcurrentHashMap<>();
                    runMethodCache.put(appId, runMethodMap);
                }
            }
        }
        return runMethodMap;
    }

    protected Object executeInternal(Map ctx)
    {
        try
        {
            return executeGroovy(ctx);
        }
        catch(Exception e)
        {
            String cubeName = getNCube(ctx).getName();
            Throwable cause = e.getCause();
            if (cause instanceof CoordinateNotFoundException)
            {
                throw (CoordinateNotFoundException) cause;
            }
            else if (cause instanceof RuleStop)
            {
                throw (RuleStop) cause;
            }
            else if (cause instanceof RuleJump)
            {
                throw (RuleJump) cause;
            }
            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(ctx) + "(), n-cube: " + cubeName + ", input: " + getInput(ctx), cause != null ? cause : e) ;
        }
    }

    /**
     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
     */
    protected Object executeGroovy(final Map ctx) throws Exception
    {
        NCube cube = getNCube(ctx);
        Map<String, Constructor> constructorMap = getConstructorCache(cube.getApplicationID());

        // Step 1: Construct the object (use default constructor)
        Constructor c = constructorMap.get(cmdHash);
        if (c == null)
        {
            c = getRunnableCode().getConstructor();
            constructorMap.put(cmdHash, c);
        }

        // Step 2: Assign the input, output, and ncube pointers to the groovy cell instance.
        final Object instance = c.newInstance();
        if (instance instanceof NCubeGroovyExpression)
        {
            NCubeGroovyExpression exp = (NCubeGroovyExpression) instance;
            exp.input = getInput(ctx);
            exp.output = getOutput(ctx);
            exp.ncube = getNCube(ctx);
        }

        // Step 3: Call the run() [for expressions] or run(Signature) [for controllers] method
        Map<String, Method> runMethodMap = getRunMethodCache(cube.getApplicationID());
        Method runMethod = runMethodMap.get(cmdHash);
        if (runMethod == null)
        {
            runMethod = getRunMethod();
            runMethodMap.put(cmdHash, runMethod);
        }

        return invokeRunMethod(runMethod, instance, ctx);
    }

    protected abstract Method getRunMethod() throws NoSuchMethodException;

    protected abstract Object invokeRunMethod(Method runMethod, Object instance, Map args) throws Exception;

    /**
     * Conditionally compile the passed in command.  If it is already compiled, this method
     * immediately returns.  Insta-check because it is just a ref == null check.
     */
    public void prepare(Object data, Map ctx)
    {
        if (getRunnableCode() != null)
        {
            return;
        }

        computeCmdHash(data, ctx);
        NCube cube = getNCube(ctx);
        Map<String, Class> compiledMap = getCompiledClassesCache(cube.getApplicationID());

        if (compiledMap.containsKey(cmdHash))
        {   // Already been compiled, re-use class
            setRunnableCode(compiledMap.get(cmdHash));
            return;
        }

        try
        {
            Class groovyCode = compile(ctx);
            setRunnableCode(groovyCode);
            compiledMap.put(cmdHash, getRunnableCode());
        }
        catch (Exception e)
        {
            String type = e.getClass().getName();
            setErrorMessage("Failed to compile Groovy command: " + getCmd() + ", n-cube: " + cube.getName() + ", error: " + e.getMessage() + " from exception: " + type);
            throw new IllegalArgumentException(getErrorMessage(), e);
        }
    }

    /**
     * Compute SHA1 hash for this CommandCell.  The tricky bit here is that the command can be either
     * defined inline or via a URL.  If defined inline, then the command hash is SHA1(command text).  If
     * defined through a URL, then the command hash is SHA1(command URL + GroovyClassLoader URLs.toString).
s    */
    private void computeCmdHash(Object data, Map ctx)
    {
        String content;
        if (getUrl() == null)
        {
            content = data != null ? data.toString() : "null";
        }
        else
        {   // specified via URL, add classLoader URL strings to URL for SHA1 source.
            NCube cube = getNCube(ctx);
            GroovyClassLoader gcLoader = (GroovyClassLoader) NCubeManager.getUrlClassLoader(cube.getApplicationID(), getInput(ctx));
            URL[] urls = gcLoader.getURLs();
            StringBuilder s = new StringBuilder();
            for (URL url : urls)
            {
                s.append(url.toString());
                s.append('.');
            }
            s.append(getUrl());
            content = s.toString();
        }
        cmdHash = EncryptionUtilities.calculateSHA1Hash(StringUtilities.getBytes(content, "UTF-8"));
    }

    protected Class compile(Map ctx) throws Exception
    {
        NCube cube = getNCube(ctx);
        String url = getUrl();
        boolean isUrlUsed = StringUtilities.hasContent(url);
        if (isUrlUsed && url.endsWith(".groovy"))
        {
            // If a class exists already with the same name as the groovy file (substituting slashes for dots),
            // then attempt to find and return that class without going through the resource location and parsing
            // code.
            try
            {
                String className = url.substring(0, url.indexOf(".groovy"));
                className = className.replace('/', '.');
                return Class.forName(className);
            }
            catch (Exception ignored)
            { }
        }

        String grvSrcCode;
        GroovyClassLoader gcLoader;

        if (isUrlUsed)
        {
            gcLoader = (GroovyClassLoader)NCubeManager.getUrlClassLoader(cube.getApplicationID(), getInput(ctx));
            URL groovySourceUrl = getActualUrl(ctx);
            grvSrcCode = StringUtilities.createString(UrlUtilities.getContentFromUrl(groovySourceUrl, true), "UTF-8");
        }
        else
        {
            gcLoader = (GroovyClassLoader)NCubeManager.getLocalClassloader(cube.getApplicationID());
            grvSrcCode = getCmd();
        }
        String groovySource = expandNCubeShortCuts(buildGroovy(grvSrcCode, cube.getName()));
        return gcLoader.parseClass(groovySource);
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
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyExplicitRunRulePattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = Regexes.groovyExplicitJumpPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
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
        Set<String> importNames = new LinkedHashSet<>();
        while (m.find())
        {
            importNames.add(m.group(0));  // based on Regex pattern - if pattern changes, this could change
        }

        m.reset();
        newGroovy.append(m.replaceAll(""));
        return importNames;
    }
}
