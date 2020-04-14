package com.cedarsoftware.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * This class is used in conjunction with the Executor class.  Example
 * usage:<pre>
 * Executor exec = new Executor()
 * exec.execute("ls -l")
 * String result = exec.getOut()
 * </pre>
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
public class Executor
{
    private String _error;
    private String _out;
    private static final Logger LOG = LogManager.getLogger(Executor.class);

    public int exec(String command)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(command);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + command, e);
            return -1;
        }
    }

    public int exec(String[] cmdarray)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(cmdarray);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + cmdArrayToString(cmdarray), e);
            return -1;
        }
    }

    public int exec(String command, String[] envp)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(command, envp);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + command, e);
            return -1;
        }
    }

    public int exec(String[] cmdarray, String[] envp)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(cmdarray, envp);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + cmdArrayToString(cmdarray), e);
            return -1;
        }
    }

    public int exec(String command, String[] envp, File dir)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(command, envp, dir);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + command, e);
            return -1;
        }
    }

    public int exec(String[] cmdarray, String[] envp, File dir)
    {
        try
        {
            Process proc = Runtime.getRuntime().exec(cmdarray, envp, dir);
            return runIt(proc);
        }
        catch (Exception e)
        {
            LOG.warn("Error occurred executing command: " + cmdArrayToString(cmdarray), e);
            return -1;
        }
    }

    private int runIt(Process proc) throws InterruptedException
    {
        StreamGobbler errors = new StreamGobbler(proc.getErrorStream());
        Thread errorGobbler = new Thread(errors);
        StreamGobbler out = new StreamGobbler(proc.getInputStream());
        Thread outputGobbler = new Thread(out);
        errorGobbler.start();
        outputGobbler.start();
        int exitVal = proc.waitFor();
        errorGobbler.join();
        outputGobbler.join();
        _error = errors.getResult();
        _out = out.getResult();
        return exitVal;
    }

    /**
     * @return String content written to StdErr
     */
    public String getError()
    {
        return _error;
    }

    /**
     * @return String content written to StdOut
     */
    public String getOut()
    {
        return _out;
    }

    private String cmdArrayToString(String[] cmdArray)
    {
        StringBuilder s = new StringBuilder();
        for (String cmd : cmdArray)
        {
            s.append(cmd);
            s.append(' ');
        }

        return s.toString();
    }
}