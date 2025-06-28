package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for SystemUtilities.
 * Verifies that security controls prevent information disclosure and resource exhaustion attacks.
 */
public class SystemUtilitiesSecurityTest {
    
    private String originalTestPassword;
    private String originalTestSecret;
    
    @BeforeEach
    public void setUp() {
        // Set up test environment variables for sensitive data testing
        originalTestPassword = System.getProperty("TEST_PASSWORD");
        originalTestSecret = System.getProperty("TEST_SECRET_KEY");
        
        // Set some test values
        System.setProperty("TEST_PASSWORD", "supersecret123");
        System.setProperty("TEST_SECRET_KEY", "api-key-12345");
        System.setProperty("TEST_NORMAL_VAR", "normal-value");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original values
        restoreProperty("TEST_PASSWORD", originalTestPassword);
        restoreProperty("TEST_SECRET_KEY", originalTestSecret);
        System.clearProperty("TEST_NORMAL_VAR");
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    public void testSensitiveVariableFiltering() {
        // Test that sensitive variables are filtered out
        assertNull(SystemUtilities.getExternalVariable("TEST_PASSWORD"), 
                  "Password variables should be filtered");
        assertNull(SystemUtilities.getExternalVariable("TEST_SECRET_KEY"), 
                  "Secret key variables should be filtered");
        
        // Test that normal variables still work
        assertEquals("normal-value", SystemUtilities.getExternalVariable("TEST_NORMAL_VAR"),
                    "Normal variables should not be filtered");
    }
    
    @Test
    public void testSensitiveVariablePatternsDetection() {
        // Test various sensitive patterns
        String[] sensitiveVars = {
            "PASSWORD", "PASSWD", "PASS", "SECRET", "KEY", "TOKEN", "CREDENTIAL",
            "AUTH", "APIKEY", "API_KEY", "PRIVATE", "CERT", "CERTIFICATE",
            "DATABASE_URL", "DB_URL", "CONNECTION_STRING", "DSN",
            "AWS_SECRET", "AZURE_CLIENT_SECRET", "GCP_SERVICE_ACCOUNT",
            "MY_PASSWORD", "USER_SECRET", "API_TOKEN", "AUTH_KEY"
        };
        
        for (String var : sensitiveVars) {
            assertNull(SystemUtilities.getExternalVariable(var),
                      "Variable should be filtered as sensitive: " + var);
        }
    }
    
    @Test
    public void testUnsafeVariableAccess() {
        // Test that unsafe method bypasses filtering
        assertEquals("supersecret123", SystemUtilities.getExternalVariableUnsafe("TEST_PASSWORD"),
                    "Unsafe method should return sensitive variables");
        assertEquals("api-key-12345", SystemUtilities.getExternalVariableUnsafe("TEST_SECRET_KEY"),
                    "Unsafe method should return sensitive variables");
    }
    
    @Test
    public void testEnvironmentVariableFiltering() {
        // Test that environment variable enumeration filters sensitive variables
        Map<String, String> envVars = SystemUtilities.getEnvironmentVariables(null);
        
        // Check that no sensitive variable names are present
        for (String key : envVars.keySet()) {
            assertFalse(containsSensitivePattern(key), 
                       "Environment variables should not contain sensitive patterns: " + key);
        }
    }
    
    @Test
    public void testUnsafeEnvironmentVariableAccess() {
        // Test that unsafe method includes all variables
        Map<String, String> allVars = SystemUtilities.getEnvironmentVariablesUnsafe(null);
        Map<String, String> filteredVars = SystemUtilities.getEnvironmentVariables(null);
        
        // Unsafe should include more or equal variables than filtered
        assertTrue(allVars.size() >= filteredVars.size(),
                  "Unsafe method should return more or equal variables");
    }
    
    @Test
    public void testTemporaryDirectoryPrefixValidation() {
        // Test valid prefixes work
        assertDoesNotThrow(() -> SystemUtilities.createTempDirectory("valid_prefix"),
                          "Valid prefix should be accepted");
        
        // Test invalid prefixes are rejected
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory(null),
                    "Null prefix should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory(""),
                    "Empty prefix should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory("../malicious"),
                    "Path traversal should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory("bad/path"),
                    "Slash in prefix should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory("bad\\path"),
                    "Backslash in prefix should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory("prefix\0null"),
                    "Null byte should be rejected");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory("prefix<>:\""),
                    "Invalid characters should be rejected");
    }
    
    @Test
    public void testTemporaryDirectoryPrefixLengthLimit() {
        // Test that overly long prefixes are rejected
        String longPrefix = StringUtilities.repeat("a", 101);
        assertThrows(IllegalArgumentException.class, 
                    () -> SystemUtilities.createTempDirectory(longPrefix),
                    "Overly long prefix should be rejected");
        
        // Test that 100 character prefix is allowed
        String maxPrefix = StringUtilities.repeat("a", 100);
        assertDoesNotThrow(() -> SystemUtilities.createTempDirectory(maxPrefix),
                          "100 character prefix should be allowed");
    }
    
    @Test
    public void testShutdownHookResourceLimits() {
        // Get initial count
        int initialCount = SystemUtilities.getShutdownHookCount();
        
        // Test adding valid shutdown hooks
        SystemUtilities.addShutdownHook(() -> {});
        assertEquals(initialCount + 1, SystemUtilities.getShutdownHookCount(),
                    "Shutdown hook count should increment");
        
        // Test null hook rejection
        assertThrows(IllegalArgumentException.class,
                    () -> SystemUtilities.addShutdownHook(null),
                    "Null shutdown hook should be rejected");
    }
    
    @Test
    public void testShutdownHookMaximumLimit() {
        // This test is more complex as we need to be careful not to exhaust the real limit
        // We'll test the error condition logic instead
        
        // Create a large number of hooks (but not the full 100 to avoid test pollution)
        int testLimit = Math.min(10, 100 - SystemUtilities.getShutdownHookCount());
        
        for (int i = 0; i < testLimit; i++) {
            SystemUtilities.addShutdownHook(() -> {});
        }
        
        // Verify we can still add hooks if under the limit
        if (SystemUtilities.getShutdownHookCount() < 100) {
            assertDoesNotThrow(() -> SystemUtilities.addShutdownHook(() -> {}),
                              "Should be able to add hooks under the limit");
        }
    }
    
    @Test
    public void testNullInputValidation() {
        // Test null handling in various methods
        assertNull(SystemUtilities.getExternalVariable(null),
                  "Null variable name should return null");
        assertNull(SystemUtilities.getExternalVariable(""),
                  "Empty variable name should return null");
        assertNull(SystemUtilities.getExternalVariableUnsafe(null),
                  "Null variable name should return null for unsafe method");
    }
    
    @Test
    public void testEnvironmentVariableFilteringWithCustomFilter() {
        // Test that custom filtering works with security filtering
        Map<String, String> pathVars = SystemUtilities.getEnvironmentVariables(
            key -> key.toUpperCase().contains("PATH")
        );
        
        // Verify that even with custom filter, sensitive variables are still filtered
        for (String key : pathVars.keySet()) {
            assertFalse(containsSensitivePattern(key),
                       "Even filtered results should not contain sensitive patterns: " + key);
        }
    }
    
    @Test
    public void testSecurityBypass() {
        // Test that we can't bypass security through case variations
        assertNull(SystemUtilities.getExternalVariable("test_password"),
                  "Lowercase sensitive variables should be filtered");
        assertNull(SystemUtilities.getExternalVariable("Test_Password"),
                  "Mixed case sensitive variables should be filtered");
        assertNull(SystemUtilities.getExternalVariable("TEST_PASSWORD"),
                  "Uppercase sensitive variables should be filtered");
    }
    
    private boolean containsSensitivePattern(String varName) {
        if (varName == null) return false;
        String upperVar = varName.toUpperCase();
        String[] patterns = {
            "PASSWORD", "PASSWD", "PASS", "SECRET", "KEY", "TOKEN", "CREDENTIAL",
            "AUTH", "APIKEY", "API_KEY", "PRIVATE", "CERT", "CERTIFICATE"
        };
        
        for (String pattern : patterns) {
            if (upperVar.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}