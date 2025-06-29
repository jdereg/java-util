package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for EncryptionUtilities configurable security features.
 * Tests all security controls including file size validation, buffer size validation,
 * cryptographic parameter validation, and PBKDF2 iteration validation.
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
public class EncryptionSecurityTest {
    
    @TempDir
    Path tempDir;
    
    private File testFile;
    private File largeFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // Clear all security-related system properties to start with clean state
        clearSecurityProperties();
        
        // Create test files
        testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "The quick brown fox jumps over the lazy dog".getBytes());
        
        largeFile = tempDir.resolve("large.txt").toFile();
        // Create a 1MB test file
        byte[] data = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        Files.write(largeFile.toPath(), data);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up system properties after each test
        clearSecurityProperties();
    }
    
    private void clearSecurityProperties() {
        System.clearProperty("encryptionutilities.security.enabled");
        System.clearProperty("encryptionutilities.file.size.validation.enabled");
        System.clearProperty("encryptionutilities.buffer.size.validation.enabled");
        System.clearProperty("encryptionutilities.crypto.parameters.validation.enabled");
        System.clearProperty("encryptionutilities.max.file.size");
        System.clearProperty("encryptionutilities.max.buffer.size");
        System.clearProperty("encryptionutilities.min.pbkdf2.iterations");
        System.clearProperty("encryptionutilities.max.pbkdf2.iterations");
        System.clearProperty("encryptionutilities.min.salt.size");
        System.clearProperty("encryptionutilities.max.salt.size");
        System.clearProperty("encryptionutilities.min.iv.size");
        System.clearProperty("encryptionutilities.max.iv.size");
    }
    
    // ===== FILE SIZE VALIDATION TESTS =====
    
    @Test
    void testFileHashingWorksWhenSecurityDisabled() {
        // Security disabled by default - should work with any file size
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        assertNotNull(EncryptionUtilities.fastMD5(largeFile));
        assertNotNull(EncryptionUtilities.fastSHA1(testFile));
        assertNotNull(EncryptionUtilities.fastSHA256(testFile));
        assertNotNull(EncryptionUtilities.fastSHA384(testFile));
        assertNotNull(EncryptionUtilities.fastSHA512(testFile));
        assertNotNull(EncryptionUtilities.fastSHA3_256(testFile));
        assertNotNull(EncryptionUtilities.fastSHA3_512(testFile));
    }
    
    @Test
    void testFileHashingWithFileSizeValidationEnabled() {
        // Enable security and file size validation with reasonable limits
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.file.size", "2097152"); // 2MB
        
        // Small file should work
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        assertNotNull(EncryptionUtilities.fastSHA1(testFile));
        assertNotNull(EncryptionUtilities.fastSHA256(testFile));
        assertNotNull(EncryptionUtilities.fastSHA384(testFile));
        assertNotNull(EncryptionUtilities.fastSHA512(testFile));
        assertNotNull(EncryptionUtilities.fastSHA3_256(testFile));
        assertNotNull(EncryptionUtilities.fastSHA3_512(testFile));
        
        // Large file (1MB) should still work under 2MB limit
        assertNotNull(EncryptionUtilities.fastMD5(largeFile));
    }
    
    @Test
    void testFileHashingRejectsOversizedFiles() {
        // Enable security and file size validation with very small limit
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.file.size", "1000"); // 1KB limit
        
        // Small file should work
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        
        // Large file should be rejected
        SecurityException e1 = assertThrows(SecurityException.class, 
            () -> EncryptionUtilities.fastMD5(largeFile));
        assertTrue(e1.getMessage().contains("File size too large"));
        
        SecurityException e2 = assertThrows(SecurityException.class, 
            () -> EncryptionUtilities.fastSHA256(largeFile));
        assertTrue(e2.getMessage().contains("File size too large"));
        
        SecurityException e3 = assertThrows(SecurityException.class, 
            () -> EncryptionUtilities.fastSHA3_512(largeFile));
        assertTrue(e3.getMessage().contains("File size too large"));
    }
    
    @Test
    void testFileHashingWithFileSizeValidationDisabled() {
        // Enable master security but disable file size validation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "false");
        System.setProperty("encryptionutilities.max.file.size", "1000"); // Very small limit
        
        // Should still work because file size validation is disabled
        assertNotNull(EncryptionUtilities.fastMD5(largeFile));
        assertNotNull(EncryptionUtilities.fastSHA256(largeFile));
    }
    
    // ===== CRYPTO PARAMETER VALIDATION TESTS =====
    
    @Test
    void testEncryptionWorksWhenSecurityDisabled() {
        // Security disabled by default - should work with standard parameters
        String encrypted = EncryptionUtilities.encrypt("testKey", "test data");
        assertNotNull(encrypted);
        assertEquals("test data", EncryptionUtilities.decrypt("testKey", encrypted));
        
        String encryptedBytes = EncryptionUtilities.encryptBytes("testKey", "test data".getBytes());
        assertNotNull(encryptedBytes);
        assertArrayEquals("test data".getBytes(), EncryptionUtilities.decryptBytes("testKey", encryptedBytes));
    }
    
    @Test
    void testEncryptionWithCryptoParameterValidationEnabled() {
        // Enable security and crypto parameter validation with reasonable limits
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.crypto.parameters.validation.enabled", "true");
        System.setProperty("encryptionutilities.min.salt.size", "8");
        System.setProperty("encryptionutilities.max.salt.size", "64");
        System.setProperty("encryptionutilities.min.iv.size", "8");
        System.setProperty("encryptionutilities.max.iv.size", "32");
        System.setProperty("encryptionutilities.min.pbkdf2.iterations", "10000");
        System.setProperty("encryptionutilities.max.pbkdf2.iterations", "1000000");
        
        // Standard encryption should work (16-byte salt, 12-byte IV, 65536 iterations)
        String encrypted = EncryptionUtilities.encrypt("testKey", "test data");
        assertNotNull(encrypted);
        assertEquals("test data", EncryptionUtilities.decrypt("testKey", encrypted));
        
        String encryptedBytes = EncryptionUtilities.encryptBytes("testKey", "test data".getBytes());
        assertNotNull(encryptedBytes);
        assertArrayEquals("test data".getBytes(), EncryptionUtilities.decryptBytes("testKey", encryptedBytes));
    }
    
    @Test
    void testEncryptionWithCryptoParameterValidationDisabled() {
        // Enable master security but disable crypto parameter validation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.crypto.parameters.validation.enabled", "false");
        System.setProperty("encryptionutilities.min.salt.size", "100"); // Unrealistic limits
        System.setProperty("encryptionutilities.max.salt.size", "200");
        
        // Should still work because crypto parameter validation is disabled
        String encrypted = EncryptionUtilities.encrypt("testKey", "test data");
        assertNotNull(encrypted);
        assertEquals("test data", EncryptionUtilities.decrypt("testKey", encrypted));
    }
    
    // ===== PBKDF2 ITERATION VALIDATION TESTS =====
    
    @Test
    void testDeriveKeyWithValidIterationCount() {
        // Enable security and crypto parameter validation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.crypto.parameters.validation.enabled", "true");
        System.setProperty("encryptionutilities.min.pbkdf2.iterations", "10000");
        System.setProperty("encryptionutilities.max.pbkdf2.iterations", "1000000");
        
        // Standard iteration count (65536) should work
        byte[] salt = new byte[16];
        byte[] key = EncryptionUtilities.deriveKey("password", salt, 128);
        assertNotNull(key);
        assertEquals(16, key.length); // 128 bits = 16 bytes
    }
    
    // ===== BUFFER SIZE VALIDATION TESTS =====
    
    @Test
    void testFileHashingWithBufferSizeValidation() {
        // Enable security and buffer size validation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.buffer.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.buffer.size", "1048576"); // 1MB
        
        // Standard 64KB buffer should work
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        assertNotNull(EncryptionUtilities.fastSHA256(testFile));
    }
    
    @Test
    void testFileHashingWithBufferSizeValidationDisabled() {
        // Enable master security but disable buffer size validation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.buffer.size.validation.enabled", "false");
        System.setProperty("encryptionutilities.max.buffer.size", "1024"); // Very small limit
        
        // Should still work because buffer size validation is disabled
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        assertNotNull(EncryptionUtilities.fastSHA256(testFile));
    }
    
    // ===== PROPERTY VALIDATION TESTS =====
    
    @Test
    void testInvalidPropertyValuesHandledGracefully() {
        // Test with invalid numeric values - should fall back to defaults
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.file.size", "invalid");
        System.setProperty("encryptionutilities.max.buffer.size", "not-a-number");
        System.setProperty("encryptionutilities.min.pbkdf2.iterations", "abc");
        
        // Should still work with default values
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        
        String encrypted = EncryptionUtilities.encrypt("testKey", "test data");
        assertNotNull(encrypted);
        assertEquals("test data", EncryptionUtilities.decrypt("testKey", encrypted));
    }
    
    @Test
    void testSecurityCanBeCompletelyDisabled() {
        // Explicitly disable security
        System.setProperty("encryptionutilities.security.enabled", "false");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.file.size", "1"); // 1 byte limit
        
        // Should work because master security switch is disabled
        assertNotNull(EncryptionUtilities.fastMD5(largeFile));
        
        String encrypted = EncryptionUtilities.encrypt("testKey", "test data");
        assertNotNull(encrypted);
        assertEquals("test data", EncryptionUtilities.decrypt("testKey", encrypted));
    }
    
    // ===== EDGE CASES AND ERROR CONDITIONS =====
    
    @Test
    void testNullInputsHandledProperly() {
        // Test null inputs are properly validated before security checks
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.encrypt(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.encrypt("key", null));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.encryptBytes(null, "data".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.encryptBytes("key", null));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.decrypt(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.decrypt("key", null));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.decryptBytes(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.decryptBytes("key", null));
    }
    
    @Test
    void testSecurityValidationPreservesOriginalFunctionality() {
        // Test that enabling security doesn't break normal operation
        System.setProperty("encryptionutilities.security.enabled", "true");
        System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
        System.setProperty("encryptionutilities.crypto.parameters.validation.enabled", "true");
        System.setProperty("encryptionutilities.max.file.size", "10485760"); // 10MB
        
        // Original functionality should work
        String testData = "The quick brown fox jumps over the lazy dog";
        
        // Test hashing
        String md5 = EncryptionUtilities.fastMD5(testFile);
        String sha256 = EncryptionUtilities.fastSHA256(testFile);
        assertNotNull(md5);
        assertNotNull(sha256);
        assertNotEquals(md5, sha256);
        
        // Test encryption/decryption
        String encrypted = EncryptionUtilities.encrypt("testPassword", testData);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt("testPassword", encrypted);
        assertEquals(testData, decrypted);
        
        // Test byte encryption/decryption
        String encryptedBytes = EncryptionUtilities.encryptBytes("testPassword", testData.getBytes());
        assertNotNull(encryptedBytes);
        byte[] decryptedBytes = EncryptionUtilities.decryptBytes("testPassword", encryptedBytes);
        assertArrayEquals(testData.getBytes(), decryptedBytes);
        
        // Verify consistency
        assertNotEquals(encrypted, encryptedBytes); // Different formats
        assertEquals(decrypted, new String(decryptedBytes)); // Same data
    }
    
    @Test
    void testBackwardCompatibilityPreserved() {
        // Ensure existing code continues to work when security is disabled (default)
        String testData = "Legacy test data";
        
        // These should work exactly as before
        String encrypted = EncryptionUtilities.encrypt("legacyKey", testData);
        String decrypted = EncryptionUtilities.decrypt("legacyKey", encrypted);
        assertEquals(testData, decrypted);
        
        // File operations should work
        assertNotNull(EncryptionUtilities.fastMD5(testFile));
        assertNotNull(EncryptionUtilities.fastSHA1(testFile));
        
        // Hash calculations should work
        assertNotNull(EncryptionUtilities.calculateMD5Hash(testData.getBytes()));
        assertNotNull(EncryptionUtilities.calculateSHA256Hash(testData.getBytes()));
    }
}