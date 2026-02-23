package com.cedarsoftware.util;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorAdditionalTest {
    private String originalExecutorEnabled;

    @BeforeEach
    void setUp() {
        // Save original executor.enabled state
        originalExecutorEnabled = System.getProperty("executor.enabled");
        // Enable executor for tests
        System.setProperty("executor.enabled", "true");
    }

    @AfterEach
    void tearDown() {
        // Restore original executor.enabled state
        if (originalExecutorEnabled == null) {
            System.clearProperty("executor.enabled");
        } else {
            System.setProperty("executor.enabled", originalExecutorEnabled);
        }
    }

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

    @Test
    public void testExecArrayMissingCommandReturnsMinusOneAndError() {
        Executor executor = new Executor();
        int exitCode = executor.exec(new String[]{"definitely_missing_command_12345"});
        assertEquals(-1, exitCode);
        assertEquals("", executor.getOut());
        assertNotNull(executor.getError());
        assertTrue(executor.getError().contains("Cannot run program"));
    }

    @Test
    public void testInterruptedExecutionClearsOutputAndTerminatesProcess() throws Exception {
        Executor executor = new Executor();
        executor.execute(shellArray("echo first"));
        assertEquals("first", executor.getOut().trim());

        File marker = new File(System.getProperty("java.io.tmpdir"),
                "executor-interrupt-" + UniqueIdGenerator.getUniqueId() + ".txt");
        if (marker.exists()) {
            marker.delete();
        }

        String command;
        if (isWindows()) {
            String path = marker.getAbsolutePath().replace("\"", "\\\"");
            command = "ping -n 4 127.0.0.1 > nul && echo leaked > \"" + path + "\"";
        } else {
            String path = marker.getAbsolutePath().replace("'", "'\"'\"'");
            command = "sleep 2; echo leaked > '" + path + "'";
        }

        Thread testThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            testThread.interrupt();
        });
        interrupter.setDaemon(true);
        interrupter.start();

        try {
            ExecutionResult result = executor.execute(command);
            assertEquals(-1, result.getExitCode());
            assertEquals("", result.getOut());
            assertNotNull(result.getError());
            assertEquals("", executor.getOut());
            assertEquals(result.getError(), executor.getError());
        } finally {
            Thread.interrupted(); // clear interrupt status for remaining tests
        }

        Thread.sleep(2500);
        assertFalse(marker.exists(), "Interrupted execution should terminate spawned process");
        marker.delete();
    }
}
