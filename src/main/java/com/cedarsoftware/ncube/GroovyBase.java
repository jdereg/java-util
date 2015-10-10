package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleJump;
import com.cedarsoftware.ncube.exception.RuleStop;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import ncube.grv.exp.NCubeGroovyExpression;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

/**
 * Base class for Groovy CommandCells.
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
 */
public abstract class GroovyBase extends UrlCommandCell
{
    protected transient String cmdHash;
    private volatile transient Class runnableCode = null;
    static final ConcurrentMap<ApplicationID, ConcurrentMap<String, Class>>  compiledClasses = new ConcurrentHashMap<>();
    static final ConcurrentMap<ApplicationID, ConcurrentMap<String, Constructor>> constructorCache = new ConcurrentHashMap<>();
    static final ConcurrentMap<ApplicationID, ConcurrentMap<String, Method>> runMethodCache = new ConcurrentHashMap<>();

    //  Private constructor only for serialization.
    protected GroovyBase() {}

    public GroovyBase(String cmd, String url, boolean cache)
    {
        super(cmd, url, cache);
    }

    public Class getRunnableCode()
    {
        return runnableCode;
    }

    public void setRunnableCode(Class runnableCode)
    {
        this.runnableCode = runnableCode;
    }

    protected Object fetchResult(Map<String, Object> ctx)
    {
        Object data = null;

        if (getUrl() == null)
        {
            data = getCmd();
        }
        else
        {
            expandUrl(ctx);
        }

        prepare(data, ctx);
        Object result = executeInternal(ctx);
        if (isCacheable())
        {
            // Remove the compiled class from Groovy's internal cache after executing it.
            // This is because the cell is marked as cacheable, so there is no need to
            // hold a reference to the compiled class.  Also remove our reference
            // (runnableCode = null). Internally, the class, constructor, and run() method
            // are not cached when the cell is marked cache:true.
            GroovySystem.getMetaClassRegistry().removeMetaClass(getRunnableCode());
            setRunnableCode(null);
        }
        return result;
    }

    protected abstract String buildGroovy(String theirGroovy);

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
        return getCache(appId, compiledClasses);
    }

    private static Map<String, Constructor> getConstructorCache(ApplicationID appId)
    {
        return getCache(appId, constructorCache);
    }

    private static Map<String, Method> getRunMethodCache(ApplicationID appId)
    {
        return getCache(appId, runMethodCache);
    }

    private static <T> Map<String, T> getCache(ApplicationID appId, ConcurrentMap<ApplicationID, ConcurrentMap<String, T>> container) {
        ConcurrentMap<String, T> map = container.get(appId);

        if (map == null) {
            map = new ConcurrentHashMap<>();
            ConcurrentMap mapRef = container.putIfAbsent(appId, map);
            if (mapRef != null) {
                map = mapRef;
            }
        }
        return map;
    }

    protected Object executeInternal(Map ctx)
    {
        try
        {
            return executeGroovy(ctx);
        }
        catch (ThreadDeath t)
        {
            throw t;
        }
        catch(Throwable e)
        {
            String cubeName = getNCube(ctx).getName();
            Throwable cause = e.getCause();
            if (cause instanceof CoordinateNotFoundException || cause instanceof RuleStop || cause instanceof RuleJump)
            {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(ctx) + "(), n-cube: " + cubeName + ", input: " + getInput(ctx), cause != null ? cause : e) ;
        }
    }

    /**
     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
     */
    protected Object executeGroovy(final Map ctx) throws Throwable
    {
        NCube cube = getNCube(ctx);
        Map<String, Constructor> constructorMap = getConstructorCache(cube.getApplicationID());

        // Step 1: Construct the object (use default constructor)
        Constructor c = constructorMap.get(cmdHash);
        if (c == null)
        {
            c = getRunnableCode().getConstructor();
            if (!isCacheable())
            {
                // Do NOT cache the constructor when the entire cell value is cache:true, because
                // the class is going to be dropped and the return value cached.
                constructorMap.put(cmdHash, c);
            }
        }

        // Step 2: Assign the input, output, and ncube pointers to the groovy cell instance.
        final Object instance = c.newInstance();
        if (instance instanceof NCubeGroovyExpression)
        {
            NCubeGroovyExpression exp = (NCubeGroovyExpression) instance;
            exp.input = getInput(ctx);
            exp.output = getOutput(ctx);
            exp.ncube = cube;
        }

        // Step 3: Call the run() [for expressions] or run(Signature) [for controllers] method
        Map<String, Method> runMethodMap = getRunMethodCache(cube.getApplicationID());
        Method runMethod = runMethodMap.get(cmdHash);
        if (runMethod == null)
        {
            runMethod = getRunMethod();
            if (!isCacheable())
            {
                // Do NOT cache the run() method when the entire cell value is cache:true, because
                // the class is going to be dropped and the return value cached.
                runMethodMap.put(cmdHash, runMethod);
            }
        }

        return invokeRunMethod(runMethod, instance, ctx);
    }

    protected abstract Method getRunMethod() throws NoSuchMethodException;

    protected abstract Object invokeRunMethod(Method runMethod, Object instance, Map args) throws Throwable;

    /**
     * Conditionally compile the passed in command.  If it is already compiled, this method
     * immediately returns.  Insta-check because it is just a ref == null check.
     */
    public void prepare(Object data, Map ctx)
    {
        if (getRunnableCode() != null)
        {   // If the code for the cell has already been compiled, return the compiled class.
            return;
        }

        computeCmdHash(data, ctx);
        NCube cube = getNCube(ctx);
        Map<String, Class> compiledMap = getCompiledClassesCache(cube.getApplicationID());

        if (compiledMap.containsKey(cmdHash))
        {   // Already been compiled, re-use class (different cell, but has identical source or URL as other expression).
            setRunnableCode(compiledMap.get(cmdHash));
            return;
        }

        try
        {
            Class groovyCode = compile(ctx);
            setRunnableCode(groovyCode);
            if (!isCacheable())
            {
                compiledMap.put(cmdHash, getRunnableCode());
            }
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

    protected Class compile(Map ctx)
    {
        NCube cube = getNCube(ctx);
        String url = getUrl();
        boolean isUrlUsed = StringUtilities.hasContent(url);
        if (isUrlUsed && url.endsWith(".groovy"))
        {
            // If a class exists already with the same name as the groovy file (substituting slashes for dots),
            // then attempt to find and return that class without going through the resource location and parsing
            // code. This can be useful, for example, if a build process pre-builds and load coverage enhanced
            // versions of the classes.
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
        String groovySource = expandNCubeShortCuts(buildGroovy(grvSrcCode));
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
