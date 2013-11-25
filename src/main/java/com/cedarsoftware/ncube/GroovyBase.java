package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import groovy.lang.GroovyClassLoader;

import java.util.HashSet;
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
    private static final Pattern groovyRefCubeCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$]([^(]+)[(]([^)]*)[)]");
    private static final Pattern groovyRefCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$][(]([^)]*)[)]");
    private static final Pattern groovyRefCubeCellPattern2 = Pattern.compile("([^a-zA-Z0-9_]|^)[@]([^(]+)[(]([^)]*)[)]");
    private static final Pattern groovyRefCellPattern2 = Pattern.compile("([^a-zA-Z0-9_]|^)[@][(]([^)]*)[)]");
    private static final Pattern groovyUniqueClassPattern = Pattern.compile("~([a-zA-Z0-9_]+)~");
    private static final Pattern groovyExplicitCubeRefPattern = Pattern.compile("ncubeMgr\\.getCube\\(['\"]([^']+)['\"]\\)");

    public GroovyBase(String cmd)
    {
        super(cmd);
    }

    protected static String fixClassName(String name)
    {
        return groovyProgramClassName.matcher(name).replaceAll("_");
    }

    protected abstract void buildGroovy(StringBuilder groovy, String theirGroovy, String cubeName);

    protected void compile(String theirGroovy, String cubeName) throws Exception
    {
        StringBuilder groovy = new StringBuilder();

        Matcher m = groovyUniqueClassPattern.matcher(theirGroovy);
        theirGroovy = m.replaceAll("$1" + UniqueIdGenerator.getUniqueId());

        buildGroovy(groovy, theirGroovy, cubeName);
        m = groovyRefCubeCellPattern.matcher(groovy.toString());
        String exp = m.replaceAll("$1ncubeMgr.getCube('$2').getCell($3,output)");

        m = groovyRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1ncube.getCell($2,output)");

        m = groovyRefCubeCellPattern2.matcher(exp);
        exp = m.replaceAll("$1input.putAll($3);ncubeMgr.getCube('$2').getCell(input,output)");

        m = groovyRefCellPattern2.matcher(exp);
        exp = m.replaceAll("$1input.putAll($2);ncube.getCell(input,output)");

        GroovyClassLoader gcl = new GroovyClassLoader();
        setRunnableCode(gcl.parseClass(exp));
    }

    public Object run(Map args)
    {
        if (getCompileErrorMsg() != null)
        {   // If the cell failed to compile earlier, do not keep trying to recompile it.
            throw new IllegalStateException(getCompileErrorMsg());
        }

        NCube ncube = (NCube) args.get("ncube");
        compileIfNeeded(ncube.getName());
        return super.run(args);
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
                    compile(getCmd(), cubeName);
                }
                catch (Exception e)
                {
                    setCompileErrorMsg("Failed to compile Groovy Command '" + getCmd() + "', NCube '" + cubeName + "'");
                    throw new IllegalArgumentException(getCompileErrorMsg(), e);
                }
            }
        }
    }

    public Set<String> getCubeNamesFromCommandText(String text)
    {
        Matcher m = groovyRefCubeCellPattern.matcher(text);
        Set<String> cubeNames = new HashSet<String>();
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

        return cubeNames;
    }
}
