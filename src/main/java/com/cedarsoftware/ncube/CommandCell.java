package com.cedarsoftware.ncube;

import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;

import java.io.UnsupportedEncodingException;
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
    private String cmd;
    private transient String cmdHash;
    private volatile transient Class runnableCode = null;
    private volatile transient String compileErrorMsg = null;
    static final String proxyServer;
    static final int proxyPort;
    private static final String nullSHA1 = EncryptionUtilities.calculateSHA1Hash("".getBytes());

    static
    {
        proxyServer = SystemUtilities.getExternalVariable("http.proxy.host");
        String port = SystemUtilities.getExternalVariable("http.proxy.port");
        if (proxyServer != null)
        {
            try
            {
                proxyPort = Integer.parseInt(port);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("http.proxy.port must be an integer: " + port, e);
            }
        }
        else
        {
            proxyPort = 0;
        }
    }

    public CommandCell(String cmd)
	{
        setCmd(cmd);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof CommandCell))
        {
            return false;
        }

        CommandCell that = (CommandCell) other;
        return getCmd().equals(that.getCmd());
    }

    public int hashCode()
    {
        return cmd == null ? 0 : cmd.hashCode();
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
        if (compileErrorMsg != null)
        {   // If the cell failed to compile earlier, do not keep trying to recompile or run it.
            throw new IllegalStateException(compileErrorMsg);
        }

        preRun(args);
        return runFinal(args);
    }

    protected abstract void preRun(Map args);

    protected abstract Object runFinal(Map args);

    protected NCube getNCube(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        if (ncube == null)
        {
            throw new IllegalStateException("NCube not set for CommandCell to execute.  Arguments: " + args);
        }
        return ncube;
    }

    protected Map getInput(Map args)
    {
        Map input = (Map) args.get("input");
        if (input == null)
        {
            throw new IllegalStateException("'input' not set for CommandCell to execute.  Arguments: " + args);
        }
        return input;
    }

    protected Map getOutput(Map args)
    {
        Map output = (Map) args.get("output");
        if (output == null)
        {
            throw new IllegalStateException("'output' not set for CommandCell to execute.  Arguments: " + args);
        }
        return output;

    }

    public String getCmd()
	{
		return cmd;
	}

    public String getCmdHash()
    {
        if (StringUtilities.isEmpty(cmd))
        {
            return nullSHA1;
        }

        if (cmdHash == null)
        {
            try
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(cmd.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(cmd.getBytes());
            }
        }
        return cmdHash;
    }

    public void setCmd(String cmd)
    {
        this.cmd = cmd;
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

    public void getCubeNamesFromCommandText(Set<String> cubeNames) {}

    public int compareTo(CommandCell cmdCell)
    {
        return cmd.compareToIgnoreCase(cmdCell.cmd);
    }

    public void getScopeKeys(Set<String> scopeKeys) {}
}
