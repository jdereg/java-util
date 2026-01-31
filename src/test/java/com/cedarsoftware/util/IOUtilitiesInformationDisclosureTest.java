package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for information disclosure prevention in IOUtilities debug logging.
 * Verifies that sensitive path information is not leaked through log messages.
 */
public class IOUtilitiesInformationDisclosureTest {
    
    private Method sanitizePathForLoggingMethod;
    private String originalDebugDetailedPaths;
    private String originalDebugFlag;
    private TestLogHandler testLogHandler;
    private Logger ioUtilitiesLogger;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Access the private sanitizePathForLogging method via reflection for testing
        sanitizePathForLoggingMethod = IOUtilities.class.getDeclaredMethod("sanitizePathForLogging", String.class);
        sanitizePathForLoggingMethod.setAccessible(true);
        
        // Store original system properties
        originalDebugDetailedPaths = System.getProperty("io.debug.detailed.paths");
        originalDebugFlag = System.getProperty("io.debug");
        
        // Set up test logging handler
        ioUtilitiesLogger = Logger.getLogger(IOUtilities.class.getName());
        testLogHandler = new TestLogHandler();
        ioUtilitiesLogger.addHandler(testLogHandler);
        ioUtilitiesLogger.setLevel(Level.FINE);
        
        // Enable debug logging but disable detailed paths by default
        System.setProperty("io.debug", "true");
        System.clearProperty("io.debug.detailed.paths");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system properties
        restoreProperty("io.debug.detailed.paths", originalDebugDetailedPaths);
        restoreProperty("io.debug", originalDebugFlag);
        
        // Remove test log handler
        if (ioUtilitiesLogger != null && testLogHandler != null) {
            ioUtilitiesLogger.removeHandler(testLogHandler);
        }
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    public void testPathSanitizationWithoutDetailedLogging() throws Exception {
        // Test that sensitive paths are masked when detailed logging is disabled
        
        String[] sensitivePaths = {
            "../../../etc/passwd",
            "C:\\Windows\\System32\\config\\sam",
            "/proc/self/mem",
            "/sys/kernel/debug",
            "/home/user/.ssh/id_rsa",
            "file\0.txt",
            "/tmp/.hidden/secret"
        };
        
        String[] expectedPatterns = {
            "[path-with-traversal-pattern]",
            "[windows-system-path]",
            "[unix-system-path]",
            "[unix-system-path]",
            "[hidden-directory-path]",
            "[path-with-null-byte]",
            "[hidden-directory-path]"
        };
        
        for (int i = 0; i < sensitivePaths.length; i++) {
            String result = (String) sanitizePathForLoggingMethod.invoke(null, sensitivePaths[i]);
            assertEquals(expectedPatterns[i], result,
                        "Should mask sensitive path: " + sensitivePaths[i]);
        }
    }
    
    @Test
    public void testPathSanitizationWithDetailedLogging() throws Exception {
        // Test that paths are shown in detail when explicitly enabled
        System.setProperty("io.debug.detailed.paths", "true");
        
        String sensitivePath = "/etc/passwd";
        String result = (String) sanitizePathForLoggingMethod.invoke(null, sensitivePath);
        
        // Should not be masked when detailed logging is enabled
        assertEquals(sensitivePath, result);
    }
    
    @Test
    public void testGenericPathMasking() throws Exception {
        // Test that generic paths are masked without revealing structure
        String normalPath = "/home/user/documents/file.txt";
        String result = (String) sanitizePathForLoggingMethod.invoke(null, normalPath);
        
        // Should show only length information, not actual path
        assertEquals("[file-path:" + normalPath.length() + "-chars]", result);
    }
    
    @Test
    public void testNullPathHandling() throws Exception {
        String result = (String) sanitizePathForLoggingMethod.invoke(null, (String) null);
        assertEquals("[null]", result);
    }
    
    @Test
    public void testLongPathTruncation() throws Exception {
        // Test that very long paths are truncated when detailed logging is enabled
        System.setProperty("io.debug.detailed.paths", "true");
        
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longPath.append("a");
        }
        
        String result = (String) sanitizePathForLoggingMethod.invoke(null, longPath.toString());
        assertTrue(result.contains("...[truncated]"), "Long paths should be truncated");
        assertTrue(result.length() <= 120, "Result should be reasonably short");
    }
    
    @Test
    public void testControlCharacterSanitization() throws Exception {
        // Test that control characters are sanitized when detailed logging is enabled
        System.setProperty("io.debug.detailed.paths", "true");
        
        String pathWithControlChars = "/tmp/file\t\n\r\0test";
        String result = (String) sanitizePathForLoggingMethod.invoke(null, pathWithControlChars);
        
        assertFalse(result.contains("\t"), "Should remove tab characters");
        assertFalse(result.contains("\n"), "Should remove newline characters");
        assertFalse(result.contains("\r"), "Should remove carriage return characters");
        assertFalse(result.contains("\0"), "Should remove null bytes");
    }
    
    @Test
    public void testSymlinkDetectionLoggingDoesNotLeakPaths() throws Exception {
        // Test that symlink detection logs don't expose actual paths
        testLogHandler.clear();
        
        // This should trigger symlink detection logging without exposing paths
        try {
            // Create a file that might trigger symlink detection (this might not actually detect symlinks but will test the logging)
            File testFile = new File("/tmp/test_symlink_detection_12345");
            IOUtilities.transfer(testFile, System.out);
        } catch (Exception e) {
            // Expected to fail, we're just testing the logging
        }
        
        // Check that no actual paths were logged
        for (String logMessage : testLogHandler.getMessages()) {
            assertFalse(logMessage.contains("/tmp/"), "Log messages should not contain actual paths");
            assertFalse(logMessage.contains("symlink_detection"), "Log messages should not contain actual filenames");
        }
    }
    
    @Test
    public void testTimeoutConfigurationLoggingDoesNotLeakSystemProperties() throws Exception {
        // Test that timeout configuration errors don't expose system property values
        testLogHandler.clear();
        
        // Set invalid timeout values to trigger logging
        System.setProperty("io.connect.timeout", "invalid_value");
        System.setProperty("io.read.timeout", "another_invalid_value");
        
        try {
            // This should trigger timeout configuration logging
            java.net.URLConnection conn = new java.net.URL("http://example.com").openConnection();
            IOUtilities.getInputStream(conn);
        } catch (Exception e) {
            // Expected to potentially fail, we're testing the logging
        }
        
        // Check that system property values are not logged
        for (String logMessage : testLogHandler.getMessages()) {
            assertFalse(logMessage.contains("invalid_value"), "Log should not contain system property values");
            assertFalse(logMessage.contains("another_invalid_value"), "Log should not contain system property values");
        }
        
        // Clean up
        System.clearProperty("io.connect.timeout");
        System.clearProperty("io.read.timeout");
    }
    
    @Test
    public void testSensitiveFileTypeDetectionLoggingIsSafe() throws Exception {
        // Test that sensitive file type detection doesn't leak filenames
        testLogHandler.clear();
        
        try {
            // This should trigger sensitive file type logging without exposing the actual filename
            File sensitiveFile = new File("/tmp/test.bak");
            IOUtilities.transfer(sensitiveFile, System.out);
        } catch (Exception e) {
            // Expected to fail, we're testing the logging
        }
        
        // Check log messages for safety
        for (String logMessage : testLogHandler.getMessages()) {
            if (logMessage.contains("sensitive file")) {
                assertFalse(logMessage.contains("test.bak"), "Should not log actual sensitive filename");
                assertFalse(logMessage.contains("/tmp/"), "Should not log actual sensitive path");
            }
        }
    }
    
    /**
     * Test log handler to capture log messages for verification
     */
    private static class TestLogHandler extends Handler {
        private final List<String> messages = new ArrayList<>();
        
        @Override
        public void publish(LogRecord record) {
            if (record != null && record.getMessage() != null) {
                messages.add(record.getMessage());
            }
        }
        
        @Override
        public void flush() {
            // No-op for testing
        }
        
        @Override
        public void close() throws SecurityException {
            messages.clear();
        }
        
        public List<String> getMessages() {
            return new ArrayList<>(messages);
        }
        
        public void clear() {
            messages.clear();
        }
    }
}