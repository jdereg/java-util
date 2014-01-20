package com.cedarsoftware.ncube;

import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.UniqueIdGenerator;
import groovy.lang.GroovyClassLoader;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public abstract class GroovyBase extends CommandCell
{
    private static final Pattern groovyProgramClassName = Pattern.compile("([^a-zA-Z0-9_])");
    static final Pattern groovyRefCubeCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$]([^(]+)[(]([^)]*)[)]");
    static final Pattern groovyRefCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$][(]([^)]*)[)]");
    static final Pattern groovyRefCubeCellPattern2 = Pattern.compile("([^a-zA-Z0-9_]|^)@([^(]+)[(]([^)]*)[)]");
    static final Pattern groovyRefCellPattern2 = Pattern.compile("([^a-zA-Z0-9_]|^)@[(]([^)]*)[)]");
    private static final Pattern groovyUniqueClassPattern = Pattern.compile("~([a-zA-Z0-9_]+)~");
    private static final Pattern groovyExplicitCubeRefPattern = Pattern.compile("ncubeMgr\\.getCube\\(['\"]([^']+)['\"]\\)");
    private static final Pattern importPattern = Pattern.compile("import[\\s]+[^;]+?;");
    static GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    static final Class groovyCell;

    static
    {
        InputStream in = GroovyBase.class.getClassLoader().getResourceAsStream("NCubeGroovyCell");
        String groovy = new String(IOUtilities.inputStreamToBytes(in));
        groovyCell = groovyClassLoader.parseClass(groovy);
    }

    public GroovyBase(String cmd)
    {
        super(cmd);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GroovyBase))
        {
            return false;
        }

        GroovyBase that = (GroovyBase) other;
        return getCmd().equals(that.getCmd());
    }

    protected static String fixClassName(String name)
    {
        return groovyProgramClassName.matcher(name).replaceAll("_");
    }

    protected abstract String buildGroovy(String theirGroovy, String cubeName);

    protected void preRun(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        compileIfNeeded(ncube.getName());
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
        Matcher m = groovyUniqueClassPattern.matcher(getCmd());
        String theirGroovy = m.replaceAll("$1" + UniqueIdGenerator.getUniqueId());

        String groovy = buildGroovy(theirGroovy, cubeName);
        String exp = expandNCubeShortCuts(groovy);

        setRunnableCode(groovyClassLoader.parseClass(exp));
    }

    static String expandNCubeShortCuts(String groovy)
    {
        Matcher m = groovyRefCubeCellPattern.matcher(groovy);
        String exp = m.replaceAll("$1getFixedCell('$2',$3)");

        m = groovyRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1ncube.getCell($2,output)");

        m = groovyRefCubeCellPattern2.matcher(exp);
        exp = m.replaceAll("$1getRelativeCubeCell('$2',$3)");

        m = groovyRefCellPattern2.matcher(exp);
        exp = m.replaceAll("$1getRelativeCell($2)");
        return exp;
    }

    public void getCubeNamesFromCommandText(final Set<String> cubeNames)
    {
        getCubeNamesFromText(cubeNames, getCmd());
    }

    static void getCubeNamesFromText(final Set<String> cubeNames, final String text)
    {
        Matcher m = groovyRefCubeCellPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyRefCubeCellPattern2.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyExplicitCubeRefPattern.matcher(text);
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
        Matcher m = inputVar.matcher(getCmd());
        while (m.find())
        {
            scopeKeys.add(m.group(2));
        }
    }

    public Set<String> getImports(String text, StringBuilder newGroovy)
    {
        Matcher m = importPattern.matcher(text);
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
