package com.cedarsoftware.ncube;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * A 'CommandCell' represents an executable cell. NCube ships
 * with support for Groovy CommandCells that allow the NCube author
 * to write cells that contain Groovy expressions, Groovy methods, or
 * Groovy classes.  Javascript executable cells, as well as Java
 * executable cells can be added to NCube.  The CommandCell expects
 * to call the method "Object run(Map args)" on whatever object is assigned
 * to the runnableCode member.
 *
 * Subclasses must implement 'getShortType()' which returns a fixed String
 * so that the NCube JSON reader can figure out what type of CommandCell to
 * instantiate when the CommandCell is specified in NCube's simpleJson format.
 *
 * Subclasses must also implement 'getCubeNamesFromCommandText()' which returns
 * any NCube names the subclass may reference.  For example, if NCubes are referenced
 * in the Groovy code, the Groovy CommandCell subclasses would return any NCube
 * names they reference.  This is required so that when an NCube is loaded,
 * it can find referenced NCubes and load those as well.
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
public abstract class CommandCell implements Comparable<CommandCell>
{
    private volatile transient Class runnableCode = null;
	private final String cmd;
    private volatile transient String compileErrorMsg = null;

	public CommandCell(String cmd)
	{
		this.cmd = cmd;
	}

    public Class getRunnableCode()
    {
        return runnableCode;
    }

    public void setRunnableCode(Class runnableCode)
    {
        this.runnableCode = runnableCode;
    }

    public Object run(Map args)
    {
        if (getCompileErrorMsg() != null)
        {   // If the cell failed to compile earlier, do not keep trying to recompile or run it.
            throw new IllegalStateException(getCompileErrorMsg());
        }

        preRun(args);
        return runFinal(args);
    }

    protected Object runFinal(Map args)
    {
        try
        {
            Method m = runnableCode.getDeclaredMethod("run", null);
            Constructor target = runnableCode.getConstructor(Map.class);
            return m.invoke(target.newInstance(args));
        }
        catch(InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Checked exception occurred invoking run() method on Groovy code.", cause) ;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error occurred invoking run() method on Groovy code.", e);
        }
    }

    protected void preRun(Map args) {}

    public String getCmd()
	{
		return cmd;
	}

    public String toString()
    {
        return cmd;
    }

    public String getCompileErrorMsg()
    {
        return compileErrorMsg;
    }

    public void setCompileErrorMsg(String compileErrorMsg)
    {
        this.compileErrorMsg = compileErrorMsg;
    }

    public abstract Set<String> getCubeNamesFromCommandText(String text);

    public int compareTo(CommandCell cmdCell)
    {
        return cmd.compareToIgnoreCase(cmdCell.cmd);
    }
}
