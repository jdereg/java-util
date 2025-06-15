package com.cedarsoftware.util;

/**
 * Captures the result of executing a command.
 */
public class ExecutionResult {
    private final int exitCode;
    private final String out;
    private final String error;

    ExecutionResult(int exitCode, String out, String error) {
        this.exitCode = exitCode;
        this.out = out;
        this.error = error;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOut() {
        return out;
    }

    public String getError() {
        return error;
    }
}

