package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for ByteUtilities class.
 * Tests configurable security controls to prevent resource exhaustion attacks.
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class ByteUtilitiesSecurityTest {
    
    private String originalSecurityEnabled;
    private String originalMaxHexStringLength;
    private String originalMaxArraySize;
    
    @BeforeEach
    void setUp() {
        // Save original system property values
        originalSecurityEnabled = System.getProperty("byteutilities.security.enabled");
        originalMaxHexStringLength = System.getProperty("byteutilities.max.hex.string.length");
        originalMaxArraySize = System.getProperty("byteutilities.max.array.size");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property values
        restoreProperty("byteutilities.security.enabled", originalSecurityEnabled);
        restoreProperty("byteutilities.max.hex.string.length", originalMaxHexStringLength);
        restoreProperty("byteutilities.max.array.size", originalMaxArraySize);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    void testSecurityDisabledByDefault() {
        // Clear all security properties to test default behavior
        System.clearProperty("byteutilities.security.enabled");
        System.clearProperty("byteutilities.max.hex.string.length");
        System.clearProperty("byteutilities.max.array.size");
        
        // Create large hex string that would exceed default limits if security was enabled
        StringBuilder largeHex = new StringBuilder();
        for (int i = 0; i < 2000000; i++) { // 2M characters
            largeHex.append("A");
        }
        
        // Should work without throwing SecurityException when security disabled
        assertDoesNotThrow(() -> {
            ByteUtilities.decode(largeHex.toString());
        }, "ByteUtilities should work without security limits by default");
        
        // Create large byte array that would exceed default limits if security was enabled
        byte[] largeArray = new byte[20000000]; // 20MB
        
        // Should work without throwing SecurityException when security disabled
        assertDoesNotThrow(() -> {
            ByteUtilities.encode(largeArray);
        }, "ByteUtilities should work without security limits by default");
    }
    
    @Test
    void testHexStringLengthLimiting() {
        // Enable security with hex string length limit
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.hex.string.length", "100");
        
        // Create hex string that exceeds the limit
        StringBuilder longHex = new StringBuilder();
        for (int i = 0; i < 102; i++) { // 102 characters > 100 limit
            longHex.append("A");
        }
        
        // Should throw SecurityException for oversized hex string
        SecurityException e = assertThrows(SecurityException.class, () -> {
            ByteUtilities.decode(longHex.toString());
        }, "Should throw SecurityException when hex string length exceeded");
        
        assertTrue(e.getMessage().contains("Hex string length exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("100"));
    }
    
    @Test
    void testByteArraySizeLimiting() {
        // Enable security with byte array size limit
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.array.size", "50");
        
        // Create byte array that exceeds the limit
        byte[] largeArray = new byte[60]; // 60 bytes > 50 limit
        
        // Should throw SecurityException for oversized byte array
        SecurityException e = assertThrows(SecurityException.class, () -> {
            ByteUtilities.encode(largeArray);
        }, "Should throw SecurityException when byte array size exceeded");
        
        assertTrue(e.getMessage().contains("Byte array size exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("50"));
    }
    
    @Test
    void testSecurityEnabledWithDefaultLimits() {
        // Enable security without specifying custom limits (should use defaults)
        System.setProperty("byteutilities.security.enabled", "true");
        System.clearProperty("byteutilities.max.hex.string.length");
        System.clearProperty("byteutilities.max.array.size");
        
        // Test reasonable sizes that should work with default limits
        String reasonableHex = "0123456789ABCDEF"; // 16 characters - well under 1M default
        byte[] reasonableArray = new byte[1000]; // 1KB - well under 10MB default
        
        // Should work fine with reasonable sizes
        assertDoesNotThrow(() -> {
            byte[] decoded = ByteUtilities.decode(reasonableHex);
            assertNotNull(decoded);
        }, "Reasonable hex string should work with default limits");
        
        assertDoesNotThrow(() -> {
            String encoded = ByteUtilities.encode(reasonableArray);
            assertNotNull(encoded);
        }, "Reasonable byte array should work with default limits");
    }
    
    @Test
    void testZeroLimitsDisableChecks() {
        // Enable security but set limits to 0 (disabled)
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.hex.string.length", "0");
        System.setProperty("byteutilities.max.array.size", "0");
        
        // Create large structures that would normally trigger limits
        StringBuilder largeHex = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeHex.append("FF");
        }
        byte[] largeArray = new byte[10000];
        
        // Should NOT throw SecurityException when limits set to 0
        assertDoesNotThrow(() -> {
            ByteUtilities.decode(largeHex.toString());
        }, "Should not enforce limits when set to 0");
        
        assertDoesNotThrow(() -> {
            ByteUtilities.encode(largeArray);
        }, "Should not enforce limits when set to 0");
    }
    
    @Test
    void testNegativeLimitsDisableChecks() {
        // Enable security but set limits to negative values (disabled)
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.hex.string.length", "-1");
        System.setProperty("byteutilities.max.array.size", "-5");
        
        // Create structures that would trigger positive limits
        String hex = "ABCDEF1234567890"; // 16 characters
        byte[] array = new byte[100]; // 100 bytes
        
        // Should NOT throw SecurityException when limits are negative
        assertDoesNotThrow(() -> {
            ByteUtilities.decode(hex);
        }, "Should not enforce negative limits");
        
        assertDoesNotThrow(() -> {
            ByteUtilities.encode(array);
        }, "Should not enforce negative limits");
    }
    
    @Test
    void testInvalidLimitValuesUseDefaults() {
        // Enable security with invalid limit values
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.hex.string.length", "invalid");
        System.setProperty("byteutilities.max.array.size", "not_a_number");
        
        // Should use default values when parsing fails
        // Test with structures that are small and should work with defaults
        String smallHex = "ABCD"; // 4 characters - well under default 1M
        byte[] smallArray = new byte[100]; // 100 bytes - well under default 10MB
        
        // Should work normally (using default values when invalid limits provided)
        assertDoesNotThrow(() -> {
            byte[] decoded = ByteUtilities.decode(smallHex);
            assertNotNull(decoded);
        }, "Should use default values when invalid property values provided");
        
        assertDoesNotThrow(() -> {
            String encoded = ByteUtilities.encode(smallArray);
            assertNotNull(encoded);
        }, "Should use default values when invalid property values provided");
    }
    
    @Test
    void testSecurityDisabledIgnoresLimits() {
        // Disable security but set strict limits
        System.setProperty("byteutilities.security.enabled", "false");
        System.setProperty("byteutilities.max.hex.string.length", "10");
        System.setProperty("byteutilities.max.array.size", "5");
        
        // Create structures that would exceed the limits if security was enabled
        String longHex = "0123456789ABCDEF0123456789ABCDEF"; // 32 characters > 10 limit
        byte[] largeArray = new byte[20]; // 20 bytes > 5 limit
        
        // Should work normally when security is disabled regardless of limit settings
        assertDoesNotThrow(() -> {
            ByteUtilities.decode(longHex);
        }, "Should ignore limits when security disabled");
        
        assertDoesNotThrow(() -> {
            ByteUtilities.encode(largeArray);
        }, "Should ignore limits when security disabled");
    }
    
    @Test
    void testSmallStructuresWithinLimits() {
        // Enable security with reasonable limits
        System.setProperty("byteutilities.security.enabled", "true");
        System.setProperty("byteutilities.max.hex.string.length", "1000");
        System.setProperty("byteutilities.max.array.size", "500");
        
        // Create small structures that are well within limits
        String smallHex = "0123456789ABCDEF"; // 16 characters < 1000 limit
        byte[] smallArray = new byte[100]; // 100 bytes < 500 limit
        
        // Should work normally for structures within limits
        assertDoesNotThrow(() -> {
            byte[] decoded = ByteUtilities.decode(smallHex);
            assertNotNull(decoded);
            assertEquals(8, decoded.length);
        }, "Should work normally for structures within limits");
        
        assertDoesNotThrow(() -> {
            String encoded = ByteUtilities.encode(smallArray);
            assertNotNull(encoded);
            assertEquals(200, encoded.length()); // 100 bytes * 2 hex chars per byte
        }, "Should work normally for structures within limits");
    }
    
    @Test
    void testBackwardCompatibilityPreserved() {
        // Clear all security properties to test default behavior
        System.clearProperty("byteutilities.security.enabled");
        System.clearProperty("byteutilities.max.hex.string.length");
        System.clearProperty("byteutilities.max.array.size");
        
        // Test normal functionality still works
        String testHex = "DEADBEEF";
        byte[] expectedBytes = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};
        
        // Should work normally without any security restrictions
        assertDoesNotThrow(() -> {
            byte[] decoded = ByteUtilities.decode(testHex);
            assertArrayEquals(expectedBytes, decoded);
            
            String encoded = ByteUtilities.encode(expectedBytes);
            assertEquals(testHex, encoded);
        }, "Should preserve backward compatibility");
    }
}