package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ExecutionResultTest {

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
