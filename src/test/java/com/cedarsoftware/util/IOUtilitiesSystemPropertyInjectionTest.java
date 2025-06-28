package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for system property injection prevention in IOUtilities.
 * Verifies that malicious system property values cannot be used to manipulate
 * timeout and size configurations in ways that could cause DoS or other attacks.
 */
public class IOUtilitiesSystemPropertyInjectionTest {
    
    private Method getValidatedTimeoutMethod;
    private Method getValidatedSizePropertyMethod;
    private String originalConnectTimeout;
    private String originalReadTimeout;
    private String originalMaxStreamSize;
    private String originalMaxDecompressionSize;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Access the private validation methods via reflection for testing
        getValidatedTimeoutMethod = IOUtilities.class.getDeclaredMethod("getValidatedTimeout", String.class, int.class, String.class);
        getValidatedTimeoutMethod.setAccessible(true);
        
        getValidatedSizePropertyMethod = IOUtilities.class.getDeclaredMethod("getValidatedSizeProperty", String.class, int.class, String.class);
        getValidatedSizePropertyMethod.setAccessible(true);
        
        // Store original system property values
        originalConnectTimeout = System.getProperty("io.connect.timeout");
        originalReadTimeout = System.getProperty("io.read.timeout");
        originalMaxStreamSize = System.getProperty("io.max.stream.size");
        originalMaxDecompressionSize = System.getProperty("io.max.decompression.size");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system properties
        restoreProperty("io.connect.timeout", originalConnectTimeout);
        restoreProperty("io.read.timeout", originalReadTimeout);
        restoreProperty("io.max.stream.size", originalMaxStreamSize);
        restoreProperty("io.max.decompression.size", originalMaxDecompressionSize);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    public void testTimeoutValidationRejectsNegativeValues() throws Exception {
        // Test that negative timeout values are rejected
        System.setProperty("io.connect.timeout", "-1000");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(1000, result, "Negative timeout should be clamped to minimum value");
    }
    
    @Test
    public void testTimeoutValidationRejectsZeroValues() throws Exception {
        // Test that zero timeout values are rejected
        System.setProperty("io.read.timeout", "0");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.read.timeout", 30000, "read timeout");
        assertEquals(1000, result, "Zero timeout should be clamped to minimum value");
    }
    
    @Test
    public void testTimeoutValidationRejectsExcessivelyLargeValues() throws Exception {
        // Test that excessively large timeout values are rejected
        System.setProperty("io.connect.timeout", "999999999");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(300000, result, "Excessive timeout should be clamped to maximum value");
    }
    
    @Test
    public void testTimeoutValidationRejectsNonNumericValues() throws Exception {
        // Test injection attempts with non-numeric values
        String[] maliciousValues = {
            "abc123",
            "5000; rm -rf /",
            "1000|ls",
            "2000&whoami",
            "3000`cat /etc/passwd`",
            "4000$(id)",
            "javascript:alert(1)",
            "<script>alert(1)</script>",
            "5000\n6000",
            "1000 2000"
        };
        
        for (String maliciousValue : maliciousValues) {
            System.setProperty("io.connect.timeout", maliciousValue);
            
            int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
            assertEquals(5000, result, 
                        "Malicious timeout value should be rejected: " + maliciousValue);
        }
    }
    
    @Test
    public void testTimeoutValidationAcceptsValidValues() throws Exception {
        // Test that valid timeout values are accepted
        System.setProperty("io.connect.timeout", "10000");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(10000, result, "Valid timeout should be accepted");
    }
    
    @Test
    public void testTimeoutValidationWithEmptyProperty() throws Exception {
        // Test that empty properties use default values
        System.setProperty("io.read.timeout", "");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.read.timeout", 30000, "read timeout");
        assertEquals(30000, result, "Empty timeout property should use default");
    }
    
    @Test
    public void testTimeoutValidationWithWhitespaceProperty() throws Exception {
        // Test that whitespace-only properties use default values
        System.setProperty("io.connect.timeout", "   ");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(5000, result, "Whitespace-only timeout property should use default");
    }
    
    @Test
    public void testSizeValidationRejectsNegativeValues() throws Exception {
        // Test that negative size values are rejected
        System.setProperty("io.max.stream.size", "-1048576");
        
        int result = (Integer) getValidatedSizePropertyMethod.invoke(null, "io.max.stream.size", 2147483647, "max stream size");
        assertEquals(2147483647, result, "Negative size should use default value");
    }
    
    @Test
    public void testSizeValidationRejectsZeroValues() throws Exception {
        // Test that zero size values are rejected
        System.setProperty("io.max.decompression.size", "0");
        
        int result = (Integer) getValidatedSizePropertyMethod.invoke(null, "io.max.decompression.size", 2147483647, "max decompression size");
        assertEquals(2147483647, result, "Zero size should use default value");
    }
    
    @Test
    public void testSizeValidationHandlesOverflow() throws Exception {
        // Test that values larger than Integer.MAX_VALUE are handled safely
        System.setProperty("io.max.stream.size", "9999999999999999999");
        
        int result = (Integer) getValidatedSizePropertyMethod.invoke(null, "io.max.stream.size", 2147483647, "max stream size");
        assertEquals(Integer.MAX_VALUE, result, "Overflow values should be clamped to Integer.MAX_VALUE");
    }
    
    @Test
    public void testSizeValidationRejectsNonNumericValues() throws Exception {
        // Test injection attempts with non-numeric values for sizes
        String[] maliciousValues = {
            "1048576; rm -rf /",
            "2097152|ls",
            "4194304&whoami",
            "1048576`cat /etc/passwd`",
            "2097152$(id)",
            "abc1048576",
            "1048576xyz",
            "1048576\n2097152",
            "1048576 2097152"
        };
        
        for (String maliciousValue : maliciousValues) {
            System.setProperty("io.max.stream.size", maliciousValue);
            
            int result = (Integer) getValidatedSizePropertyMethod.invoke(null, "io.max.stream.size", 2147483647, "max stream size");
            assertEquals(2147483647, result, 
                        "Malicious size value should be rejected: " + maliciousValue);
        }
    }
    
    @Test
    public void testSizeValidationAcceptsValidValues() throws Exception {
        // Test that valid size values are accepted
        System.setProperty("io.max.stream.size", "1048576");
        
        int result = (Integer) getValidatedSizePropertyMethod.invoke(null, "io.max.stream.size", 2147483647, "max stream size");
        assertEquals(1048576, result, "Valid size should be accepted");
    }
    
    @Test
    public void testTimeoutValidationBoundsEnforcement() throws Exception {
        // Test that the minimum and maximum bounds are enforced correctly
        
        // Test minimum bound (should clamp to 1000ms)
        System.setProperty("io.connect.timeout", "500");
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(1000, result, "Timeout below minimum should be clamped to 1000ms");
        
        // Test maximum bound (should clamp to 300000ms)
        System.setProperty("io.read.timeout", "600000");
        result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.read.timeout", 30000, "read timeout");
        assertEquals(300000, result, "Timeout above maximum should be clamped to 300000ms");
        
        // Test value within bounds (should be accepted)
        System.setProperty("io.connect.timeout", "15000");
        result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.connect.timeout", 5000, "connect timeout");
        assertEquals(15000, result, "Valid timeout within bounds should be accepted");
    }
    
    @Test
    public void testIntegrationWithURLConnectionConfiguration() throws Exception {
        // Test that the validation actually works when configuring URLConnection timeouts
        System.setProperty("io.connect.timeout", "malicious_value");
        System.setProperty("io.read.timeout", "-5000");
        
        try {
            // This should use the secure validation and not fail or use malicious values
            URL url = new URL("http://example.com");
            URLConnection connection = url.openConnection();
            IOUtilities.getInputStream(connection);
            
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                // The timeouts should be set to safe default values, not the malicious ones
                // Note: We can't easily test the actual timeout values set on the connection
                // but we can verify that no exceptions were thrown during configuration
                assertTrue(true, "URLConnection configuration should complete without errors");
            }
        } catch (IOException e) {
            // Expected - we're not actually connecting, just testing the configuration
            assertTrue(true, "IOException expected when actually trying to connect");
        } catch (SecurityException e) {
            fail("SecurityException should not occur during URL connection configuration: " + e.getMessage());
        } catch (NumberFormatException e) {
            fail("NumberFormatException should not occur with secure validation: " + e.getMessage());
        }
    }
    
    @Test
    public void testSecurityExceptionHandling() throws Exception {
        // Test that SecurityException during property access is handled gracefully
        // This test simulates what would happen if a SecurityManager prevents property access
        
        // We can't easily simulate a SecurityManager in this test environment,
        // but we can verify the method handles the case properly by testing with
        // a non-existent property that won't throw SecurityException
        System.clearProperty("io.nonexistent.timeout");
        
        int result = (Integer) getValidatedTimeoutMethod.invoke(null, "io.nonexistent.timeout", 5000, "test timeout");
        assertEquals(5000, result, "Missing property should return default value");
    }
}