package com.cedarsoftware.util;

import java.io.File;

/**
 * A utility class for executing system commands and capturing their output.
 * <p>
 * This class provides a convenient wrapper around Java's {@link Runtime#exec(String)} methods,
 * capturing both standard output and standard error streams. It handles stream management
 * and process cleanup automatically.
 * </p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Executes system commands with various parameter combinations</li>
 *   <li>Captures stdout and stderr output</li>
 *   <li>Supports environment variables</li>
 *   <li>Supports working directory specification</li>
 *   <li>Non-blocking output handling</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * Executor exec = new Executor();
 * int exitCode = exec.exec("ls -l");
 * String output = exec.getOut();      // Get stdout
 * String errors = exec.getError();    // Get stderr
 * }</pre>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class Executor {
    private String _error;
    private String _out;

    /**
     * Executes a command using the system's runtime environment.
     *
     * @param command the command to execute
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String command) {
        try {
            Process proc = Runtime.getRuntime().exec(command);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + command);
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * Executes a command array using the system's runtime environment.
     * <p>
     * This version allows commands with arguments to be specified as separate array elements,
     * avoiding issues with argument quoting and escaping.
     *
     * @param cmdarray array containing the command and its arguments
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String[] cmdarray) {
        try {
            Process proc = Runtime.getRuntime().exec(cmdarray);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + cmdArrayToString(cmdarray));
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * Executes a command with specified environment variables.
     *
     * @param command the command to execute
     * @param envp array of strings, each element of which has environment variable settings in format name=value,
     *             or null if the subprocess should inherit the environment of the current process
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String command, String[] envp) {
        try {
            Process proc = Runtime.getRuntime().exec(command, envp);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + command);
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * Executes a command array with specified environment variables.
     *
     * @param cmdarray array containing the command and its arguments
     * @param envp array of strings, each element of which has environment variable settings in format name=value,
     *             or null if the subprocess should inherit the environment of the current process
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String[] cmdarray, String[] envp) {
        try {
            Process proc = Runtime.getRuntime().exec(cmdarray, envp);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + cmdArrayToString(cmdarray));
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * Executes a command with specified environment variables and working directory.
     *
     * @param command the command to execute
     * @param envp array of strings, each element of which has environment variable settings in format name=value,
     *             or null if the subprocess should inherit the environment of the current process
     * @param dir the working directory of the subprocess, or null if the subprocess should inherit
     *            the working directory of the current process
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String command, String[] envp, File dir) {
        try {
            Process proc = Runtime.getRuntime().exec(command, envp, dir);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + command);
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /**
     * Executes a command array with specified environment variables and working directory.
     *
     * @param cmdarray array containing the command and its arguments
     * @param envp array of strings, each element of which has environment variable settings in format name=value,
     *             or null if the subprocess should inherit the environment of the current process
     * @param dir the working directory of the subprocess, or null if the subprocess should inherit
     *            the working directory of the current process
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String[] cmdarray, String[] envp, File dir) {
        try {
            Process proc = Runtime.getRuntime().exec(cmdarray, envp, dir);
            return runIt(proc);
        } catch (Exception e) {
            System.err.println("Error occurred executing command: " + cmdArrayToString(cmdarray));
            e.printStackTrace(System.err);
            return -1;
        }
    }

    private int runIt(Process proc) throws InterruptedException {
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
     * Returns the content written to standard error by the last executed command.
     *
     * @return the stderr output as a string, or null if no command has been executed
     */
    public String getError() {
        return _error;
    }

    /**
     * Returns the content written to standard output by the last executed command.
     *
     * @return the stdout output as a string, or null if no command has been executed
     */
    public String getOut() {
        return _out;
    }

    private String cmdArrayToString(String[] cmdArray) {
        StringBuilder s = new StringBuilder();
        for (String cmd : cmdArray) {
            s.append(cmd);
            s.append(' ');
        }

        return s.toString();
    }
}