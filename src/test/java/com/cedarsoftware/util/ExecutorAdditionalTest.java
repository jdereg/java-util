package com.cedarsoftware.util;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutorAdditionalTest {

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static String[] shellArray(String command) {
        if (isWindows()) {
            return new String[]{"cmd.exe", "/c", command};
        }
        return new String[]{"sh", "-c", command};
    }

    @Test
    public void testExecuteArray() {
        Executor executor = new Executor();
        ExecutionResult result = executor.execute(shellArray("echo hello"));
        assertEquals(0, result.getExitCode());
        assertEquals("hello", result.getOut().trim());
        assertEquals("", result.getError());
    }

    @Test
    public void testExecuteCommandWithEnv() {
        Executor executor = new Executor();
        String command = isWindows() ? "echo %FOO%" : "echo $FOO";
        ExecutionResult result = executor.execute(command, new String[]{"FOO=bar"});
        assertEquals(0, result.getExitCode());
        assertEquals("bar", result.getOut().trim());
    }

    @Test
    public void testExecuteArrayWithEnv() {
        Executor executor = new Executor();
        String echoVar = isWindows() ? "echo %FOO%" : "echo $FOO";
        ExecutionResult result = executor.execute(shellArray(echoVar), new String[]{"FOO=baz"});
        assertEquals(0, result.getExitCode());
        assertEquals("baz", result.getOut().trim());
    }

    @Test
    public void testExecuteArrayWithEnvAndDir() throws Exception {
        Executor executor = new Executor();
        File dir = SystemUtilities.createTempDirectory("exec-test");
        try {
            String pwd = isWindows() ? "cd" : "pwd";
            ExecutionResult result = executor.execute(shellArray(pwd), null, dir);
            assertEquals(0, result.getExitCode());
            String actualPath = new File(result.getOut().trim()).getCanonicalPath();
            assertEquals(dir.getCanonicalPath(), actualPath);
        } finally {
            if (dir != null) {
                dir.delete();
            }
        }
    }

    @Test
    public void testExecVariantsAndGetError() throws Exception {
        Executor executor = new Executor();

        assertEquals(0, executor.exec(shellArray("echo hi")));
        assertEquals("hi", executor.getOut().trim());

        String varCmd = isWindows() ? "echo %VAR%" : "echo $VAR";
        assertEquals(0, executor.exec(varCmd, new String[]{"VAR=val"}));
        assertEquals("val", executor.getOut().trim());

        assertEquals(0, executor.exec(shellArray(varCmd), new String[]{"VAR=end"}));
        assertEquals("end", executor.getOut().trim());

        File dir = SystemUtilities.createTempDirectory("exec-test2");
        try {
            String pwd = isWindows() ? "cd" : "pwd";
            assertEquals(0, executor.exec(pwd, null, dir));
            String outPath = new File(executor.getOut().trim()).getCanonicalPath();
            assertEquals(dir.getCanonicalPath(), outPath);

            assertEquals(0, executor.exec(shellArray(pwd), null, dir));
            outPath = new File(executor.getOut().trim()).getCanonicalPath();
            assertEquals(dir.getCanonicalPath(), outPath);
        } finally {
            if (dir != null) {
                dir.delete();
            }
        }

        String errCmd = isWindows() ? "echo err 1>&2" : "echo err 1>&2";
        executor.exec(shellArray(errCmd));
        assertEquals("err", executor.getError().trim());
    }
}
