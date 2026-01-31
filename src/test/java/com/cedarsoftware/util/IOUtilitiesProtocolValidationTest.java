package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for URL protocol validation in IOUtilities.
 * Verifies that dangerous protocols are blocked and only safe protocols are allowed.
 */
public class IOUtilitiesProtocolValidationTest {
    
    private Method validateUrlProtocolMethod;
    private String originalProtocolValidationDisabled;
    private String originalAllowedProtocols;
    private String originalDetailedUrls;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Access the private validateUrlProtocol method via reflection for testing
        validateUrlProtocolMethod = IOUtilities.class.getDeclaredMethod("validateUrlProtocol", URLConnection.class);
        validateUrlProtocolMethod.setAccessible(true);
        
        // Store original system properties
        originalProtocolValidationDisabled = System.getProperty("io.url.protocol.validation.disabled");
        originalAllowedProtocols = System.getProperty("io.allowed.protocols");
        originalDetailedUrls = System.getProperty("io.debug.detailed.urls");
        
        // Ensure validation is enabled for tests
        System.clearProperty("io.url.protocol.validation.disabled");
        System.clearProperty("io.allowed.protocols"); // Use defaults
        System.clearProperty("io.debug.detailed.urls");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system properties
        restoreProperty("io.url.protocol.validation.disabled", originalProtocolValidationDisabled);
        restoreProperty("io.allowed.protocols", originalAllowedProtocols);
        restoreProperty("io.debug.detailed.urls", originalDetailedUrls);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    public void testHttpProtocolIsAllowed() throws Exception {
        URL url = new URL("http://example.com");
        URLConnection connection = url.openConnection();
        
        // Should not throw any exception
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "HTTP protocol should be allowed by default");
    }
    
    @Test
    public void testHttpsProtocolIsAllowed() throws Exception {
        URL url = new URL("https://example.com");
        URLConnection connection = url.openConnection();
        
        // Should not throw any exception
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "HTTPS protocol should be allowed by default");
    }
    
    @Test
    public void testDangerousProtocolsAreBlocked() throws Exception {
        // Configure to only allow HTTP and HTTPS to test dangerous protocol blocking
        System.setProperty("io.allowed.protocols", "http,https");
        
        // Test protocols that Java URL class supports but should be blocked
        String[] dangerousProtocols = {
            "ftp://malicious.server/"
        };
        
        for (String urlString : dangerousProtocols) {
            try {
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                
                Exception exception = assertThrows(Exception.class, () -> {
                    validateUrlProtocolMethod.invoke(null, connection);
                }, "Should block dangerous protocol: " + urlString);
                
                // Unwrap InvocationTargetException to get the actual SecurityException
                Throwable cause = exception.getCause();
                assertTrue(cause instanceof SecurityException, 
                          "Expected SecurityException but got: " + cause.getClass().getSimpleName());
                assertTrue(cause.getMessage().contains("not allowed") || cause.getMessage().contains("forbidden"), 
                          "Should indicate protocol is not allowed: " + cause.getMessage());
            } catch (java.net.MalformedURLException e) {
                // Some protocols might not be supported by the JVM, which is expected and fine
                // This just means the JVM itself protects against these protocols
                assertTrue(true, "JVM already blocks unsupported protocol: " + urlString);
            }
        }
    }
    
    @Test
    public void testDangerousFilePathsAreBlocked() throws Exception {
        // Reset to default allowed protocols that include file
        System.clearProperty("io.allowed.protocols");
        
        // Test that dangerous file paths are blocked even though file protocol is allowed
        String[] dangerousFilePaths = {
            "file:///etc/passwd",
            "file:///proc/self/mem",
            "file:///C:/Windows/System32/config/sam"
        };
        
        for (String urlString : dangerousFilePaths) {
            try {
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                
                Exception exception = assertThrows(Exception.class, () -> {
                    validateUrlProtocolMethod.invoke(null, connection);
                }, "Should block dangerous file path: " + urlString);
                
                // Unwrap InvocationTargetException to get the actual SecurityException
                Throwable cause = exception.getCause();
                assertTrue(cause instanceof SecurityException, 
                          "Expected SecurityException but got: " + cause.getClass().getSimpleName());
                assertTrue(cause.getMessage().contains("system path") || cause.getMessage().contains("Suspicious"), 
                          "Should indicate dangerous file path: " + cause.getMessage());
            } catch (java.net.MalformedURLException e) {
                // Some protocols might not be supported by the JVM, which is expected and fine
                assertTrue(true, "JVM already blocks unsupported protocol: " + urlString);
            }
        }
    }
    
    @Test
    public void testDangerousFilePathsBlocked() throws Exception {
        // Test that specific dangerous file paths are blocked
        URL url = new URL("file:///etc/passwd");
        URLConnection connection = url.openConnection();
        
        Exception exception = assertThrows(Exception.class, () -> {
            validateUrlProtocolMethod.invoke(null, connection);
        });
        
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof SecurityException);
        assertTrue(cause.getMessage().contains("system path"));
    }
    
    @Test
    public void testCustomAllowedProtocols() throws Exception {
        // Configure custom allowed protocols
        System.setProperty("io.allowed.protocols", "http,https,ftp");
        
        // FTP should now be allowed
        URL url = new URL("ftp://example.com/file.txt");
        URLConnection connection = url.openConnection();
        
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "FTP should be allowed when configured");
    }
    
    @Test
    public void testProtocolValidationCanBeDisabled() throws Exception {
        System.setProperty("io.url.protocol.validation.disabled", "true");
        
        // Even dangerous protocols should be allowed when validation is disabled
        URL url = new URL("file:///etc/passwd");
        URLConnection connection = url.openConnection();
        
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "All protocols should be allowed when validation is disabled");
    }
    
    @Test
    public void testProtocolCaseInsensitivity() throws Exception {
        // Test that protocol validation is case-insensitive
        URL url = new URL("HTTP://example.com");
        URLConnection connection = url.openConnection();
        
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "Protocol validation should be case-insensitive");
    }
    
    @Test
    public void testProtocolInjectionPrevention() throws Exception {
        // Test protocols with injection attempts in configuration
        System.setProperty("io.allowed.protocols", "http:evil,https");
        
        URL url = new URL("http://example.com");
        URLConnection connection = url.openConnection();
        
        // Should fail because "http" doesn't match "http:evil"
        Exception exception = assertThrows(Exception.class, () -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "Should reject protocol due to injection in configuration");
        
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof SecurityException);
        assertTrue(cause.getMessage().contains("not allowed"));
        
        // But pure HTTP should work when properly configured
        System.setProperty("io.allowed.protocols", "http,https");
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "Normal HTTP should work with clean configuration");
    }
    
    @Test
    public void testNullConnectionHandling() throws Exception {
        // Test null connection
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, (URLConnection) null);
        }, "Null connection should be handled gracefully");
    }
    
    @Test
    public void testGetInputStreamWithValidProtocol() throws Exception {
        // Test the actual getInputStream method with valid protocol
        URL url = new URL("http://httpbin.org/get");
        URLConnection connection = url.openConnection();
        
        try {
            // This should work without throwing protocol validation errors
            IOUtilities.getInputStream(connection);
            assertTrue(true, "getInputStream should work with valid HTTP protocol");
        } catch (Exception e) {
            // Network errors are expected in test environment, but not protocol validation errors
            assertFalse(e.getMessage().contains("protocol"), 
                       "Should not fail due to protocol validation: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetInputStreamWithDangerousFilePath() throws Exception {
        // Test the actual getInputStream method with dangerous file path
        URL url = new URL("file:///etc/passwd");
        URLConnection connection = url.openConnection();
        
        Exception exception = assertThrows(Exception.class, () -> {
            IOUtilities.getInputStream(connection);
        });
        
        // Should be a SecurityException from protocol validation
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof SecurityException, 
                  "Expected SecurityException but got: " + cause.getClass().getSimpleName());
        assertTrue(cause.getMessage().contains("system path"), 
                  "Should indicate dangerous file path is blocked");
    }
    
    @Test
    public void testLegitimateFileProtocolAllowed() throws Exception {
        // Test that legitimate file paths (like classpath resources) are allowed
        URL url = new URL("file:///tmp/legitimate_test_file.txt");
        URLConnection connection = url.openConnection();
        
        // Should not throw security exception (though might throw IO exception if file doesn't exist)
        try {
            validateUrlProtocolMethod.invoke(null, connection);
            assertTrue(true, "Legitimate file path should be allowed");
        } catch (Exception e) {
            // If there's an InvocationTargetException, check the cause
            Throwable cause = e.getCause();
            assertFalse(cause instanceof SecurityException, 
                       "Legitimate file path should not be blocked: " + (cause != null ? cause.getMessage() : e.getMessage()));
        }
    }
    
    @Test
    public void testUrlSanitizationForLogging() throws Exception {
        // Test that URL sanitization works properly
        URL url = new URL("http://user:password@example.com/sensitive/path");
        URLConnection connection = url.openConnection();
        
        // This test verifies that even when validation fails, 
        // sensitive information isn't leaked in error messages
        Exception exception = assertThrows(Exception.class, () -> {
            System.setProperty("io.allowed.protocols", "https"); // Block HTTP
            validateUrlProtocolMethod.invoke(null, connection);
        });
        
        Throwable cause = exception.getCause();
        String message = cause.getMessage();
        
        // Verify sensitive information is not in the error message
        assertFalse(message.contains("password"), "Error message should not contain password");
        assertFalse(message.contains("user:password"), "Error message should not contain credentials");
        assertFalse(message.contains("sensitive"), "Error message should not contain sensitive path parts");
    }
    
    @Test
    public void testWhitespaceInAllowedProtocols() throws Exception {
        // Test that whitespace in allowed protocols configuration is handled
        System.setProperty("io.allowed.protocols", " http , https , ftp ");
        
        URL url = new URL("http://example.com");
        URLConnection connection = url.openConnection();
        
        assertDoesNotThrow(() -> {
            validateUrlProtocolMethod.invoke(null, connection);
        }, "Should handle whitespace in allowed protocols configuration");
    }
    
    @Test
    public void testEmptyAllowedProtocols() throws Exception {
        // Test with empty allowed protocols (should block everything)
        System.setProperty("io.allowed.protocols", "");
        
        URL url = new URL("http://example.com");
        URLConnection connection = url.openConnection();
        
        Exception exception = assertThrows(Exception.class, () -> {
            validateUrlProtocolMethod.invoke(null, connection);
        });
        
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof SecurityException);
        assertTrue(cause.getMessage().contains("not allowed"));
    }
    
    @Test
    public void testProtocolValidationPerformance() throws Exception {
        // Test that protocol validation doesn't significantly impact performance
        URL url = new URL("https://example.com");
        URLConnection connection = url.openConnection();
        
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            validateUrlProtocolMethod.invoke(null, connection);
        }
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "Protocol validation should be fast (took " + durationMs + "ms for 1000 calls)");
    }
}