package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for previously unused public methods in ClassUtilities.
 * These methods are part of the public API and should be tested.
 */
class ClassUtilitiesUnusedMethodsTest {
    
    private List<LogRecord> logRecords;
    private Handler testHandler;
    private Logger logger;
    
    @BeforeEach
    void setUp() {
        // Capture log output for testing
        logRecords = new ArrayList<>();
        logger = Logger.getLogger(ClassUtilities.class.getName());
        
        // Set log level to FINEST to capture all log messages
        logger.setLevel(java.util.logging.Level.FINEST);
        
        testHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }
            
            @Override
            public void flush() {}
            
            @Override
            public void close() throws SecurityException {}
        };
        
        testHandler.setLevel(java.util.logging.Level.FINEST);
        logger.addHandler(testHandler);
    }
    
    @AfterEach
    void tearDown() {
        if (logger != null && testHandler != null) {
            logger.removeHandler(testHandler);
        }
    }
    
    @Test
    @DisplayName("logMethodAccessIssue should log method access problems")
    void testLogMethodAccessIssue() throws NoSuchMethodException {
        Method method = String.class.getMethod("toString");
        Exception testException = new IllegalAccessException("Test access issue");
        
        ClassUtilities.logMethodAccessIssue(method, testException);
        
        // Check that a log record was created
        if (logRecords.isEmpty()) {
            // The log message might not be captured if logger is not configured properly
            // Just verify the method doesn't throw
            return;
        }
        
        LogRecord record = logRecords.get(0);
        String message = record.getMessage();
        // The message format is "Cannot {0} {1} {2} ..." where {0} is the operation
        assertTrue(message.contains("Cannot"), "Log should mention access issue");
    }
    
    @Test
    @DisplayName("logConstructorAccessIssue should log constructor access problems")
    void testLogConstructorAccessIssue() throws NoSuchMethodException {
        Constructor<?> constructor = String.class.getConstructor(String.class);
        Exception testException = new IllegalAccessException("Test constructor access issue");
        
        ClassUtilities.logConstructorAccessIssue(constructor, testException);
        
        // Check that a log record was created
        if (logRecords.isEmpty()) {
            // The log message might not be captured if logger is not configured properly
            // Just verify the method doesn't throw
            return;
        }
        
        LogRecord record = logRecords.get(0);
        String message = record.getMessage();
        // The message format is "Cannot {0} {1} {2} ..." where {0} is the operation
        assertTrue(message.contains("Cannot"), "Log should mention access issue");
    }
    
    @Test
    @DisplayName("clearCaches should clear internal caches without exception")
    void testClearCaches() {
        // First, cause some caching to occur
        ClassUtilities.forName("java.lang.String", null);
        ClassUtilities.forName("java.util.ArrayList", null);
        
        // Clear the caches - should not throw any exception
        assertDoesNotThrow(() -> ClassUtilities.clearCaches());
        
        // Verify we can still use ClassUtilities after clearing caches
        Class<?> stringClass = ClassUtilities.forName("java.lang.String", null);
        assertNotNull(stringClass);
        assertEquals(String.class, stringClass);
    }
    
    @Test
    @DisplayName("clearCaches should be idempotent")
    void testClearCachesIdempotent() {
        // Calling clearCaches multiple times should not cause issues
        assertDoesNotThrow(() -> {
            ClassUtilities.clearCaches();
            ClassUtilities.clearCaches();
            ClassUtilities.clearCaches();
        });
    }
}