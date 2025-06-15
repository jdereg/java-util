package com.cedarsoftware.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
 *
 * <p><strong>Thread Safety:</strong> Instances of this class are <em>not</em> thread
 * safe. Create a new {@code Executor} per command execution or synchronize
 * externally if sharing across threads.</p>
 */
public class Executor {
    private String _error;
    private String _out;
    private static final long DEFAULT_TIMEOUT_SECONDS = 60L;

    public ExecutionResult execute(String command) {
        return execute(command, null, null);
    }

    public ExecutionResult execute(String[] cmdarray) {
        return execute(cmdarray, null, null);
    }

    public ExecutionResult execute(String command, String[] envp) {
        return execute(command, envp, null);
    }

    public ExecutionResult execute(String[] cmdarray, String[] envp) {
        return execute(cmdarray, envp, null);
    }

    public ExecutionResult execute(String command, String[] envp, File dir) {
        try {
            Process proc = startProcess(command, envp, dir);
            return runIt(proc);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred executing command: " + command);
            e.printStackTrace(System.err);
            return new ExecutionResult(-1, "", e.getMessage());
        }
    }

    public ExecutionResult execute(String[] cmdarray, String[] envp, File dir) {
        try {
            Process proc = startProcess(cmdarray, envp, dir);
            return runIt(proc);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred executing command: " + cmdArrayToString(cmdarray));
            e.printStackTrace(System.err);
            return new ExecutionResult(-1, "", e.getMessage());
        }
    }

    private Process startProcess(String command, String[] envp, File dir) throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        String[] shellCmd = windows ? new String[]{"cmd.exe", "/c", command} : new String[]{"sh", "-c", command};
        return startProcess(shellCmd, envp, dir);
    }

    private Process startProcess(String[] cmdarray, String[] envp, File dir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        if (envp != null) {
            for (String env : envp) {
                int idx = env.indexOf('=');
                if (idx > 0) {
                    pb.environment().put(env.substring(0, idx), env.substring(idx + 1));
                }
            }
        }
        if (dir != null) {
            pb.directory(dir);
        }
        return pb.start();
    }

    /**
     * Executes a command using the system's runtime environment.
     *
     * @param command the command to execute
     * @return the exit value of the process (0 typically indicates success),
     *         or -1 if an error occurred starting the process
     */
    public int exec(String command) {
        ExecutionResult result = execute(command);
        return result.getExitCode();
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
        ExecutionResult result = execute(cmdarray);
        return result.getExitCode();
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
        ExecutionResult result = execute(command, envp);
        return result.getExitCode();
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
        ExecutionResult result = execute(cmdarray, envp);
        return result.getExitCode();
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
        ExecutionResult result = execute(command, envp, dir);
        return result.getExitCode();
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
        ExecutionResult result = execute(cmdarray, envp, dir);
        return result.getExitCode();
    }

    private ExecutionResult runIt(Process proc) throws InterruptedException {
        StreamGobbler errors = new StreamGobbler(proc.getErrorStream());
        Thread errorGobbler = new Thread(errors);
        StreamGobbler out = new StreamGobbler(proc.getInputStream());
        Thread outputGobbler = new Thread(out);
        errorGobbler.start();
        outputGobbler.start();

        boolean finished = proc.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
        }

        errorGobbler.join();
        outputGobbler.join();

        String err = errors.getResult();
        String outStr = out.getResult();

        int exitVal = finished ? proc.exitValue() : -1;
        _error = err;
        _out = outStr;
        return new ExecutionResult(exitVal, outStr, err);
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
        return String.join(" ", cmdArray);
    }
}
