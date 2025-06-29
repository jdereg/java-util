package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ExecutionResultTest {
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

    @Test
    public void testGetOutAndErrorSuccess() {
        Executor executor = new Executor();
        ExecutionResult result = executor.execute("echo HelloWorld");
        assertEquals(0, result.getExitCode());
        assertEquals("HelloWorld", result.getOut().trim());
        assertEquals("", result.getError());
    }

    @Test
    public void testGetOutAndErrorFailure() {
        Executor executor = new Executor();
        ExecutionResult result = executor.execute("thisCommandShouldNotExist123");
        assertNotEquals(0, result.getExitCode());
        assertFalse(result.getError().isEmpty());
    }
}
