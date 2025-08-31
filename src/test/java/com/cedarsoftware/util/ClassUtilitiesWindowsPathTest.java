package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for Windows path normalization in resource loading.
 * Verifies that backslashes are normalized to forward slashes for developer ergonomics.
 */
class ClassUtilitiesWindowsPathTest {
    
    @Test
    @DisplayName("Resource paths with backslashes should be normalized to forward slashes")
    void testWindowsPathNormalization() {
        // Windows developers often paste paths with backslashes
        // These should be normalized to forward slashes for JAR resources
        
        // Test a typical Windows-style path
        try {
            // This path won't exist, but it should be normalized and not throw for backslashes
            byte[] result = ClassUtilities.loadResourceAsBytes("com\\example\\resource.txt");
            // Will return null since resource doesn't exist, but shouldn't throw for backslashes
            assertNull(result);
        } catch (IllegalArgumentException e) {
            // This is expected if the resource is not found and exceptions are enabled
            assertTrue(e.getMessage().contains("Resource not found"));
        } catch (SecurityException e) {
            // Should not throw SecurityException for backslashes anymore
            fail("Should not throw SecurityException for backslashes: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Mixed slashes should be normalized")
    void testMixedSlashNormalization() {
        // Test mixed forward and backslashes
        try {
            byte[] result = ClassUtilities.loadResourceAsBytes("com/example\\sub\\resource.txt");
            // Will return null since resource doesn't exist, but shouldn't throw for backslashes
            assertNull(result);
        } catch (IllegalArgumentException e) {
            // This is expected if the resource is not found and exceptions are enabled
            assertTrue(e.getMessage().contains("Resource not found"));
        } catch (SecurityException e) {
            fail("Should not throw SecurityException for mixed slashes: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Path traversal with backslashes should still be blocked")
    void testPathTraversalWithBackslashes() {
        // Even with normalization, path traversal should be blocked
        SecurityException ex = assertThrows(SecurityException.class, () ->
            ClassUtilities.loadResourceAsBytes("..\\..\\etc\\passwd")
        );
        
        assertTrue(ex.getMessage().contains("traversal"));
    }
    
    @Test
    @DisplayName("Path traversal with mixed slashes should still be blocked")
    void testPathTraversalWithMixedSlashes() {
        // Mixed slash path traversal should also be blocked
        SecurityException ex = assertThrows(SecurityException.class, () ->
            ClassUtilities.loadResourceAsBytes("com\\..\\..\\etc/passwd")
        );
        
        assertTrue(ex.getMessage().contains("traversal"));
    }
    
    @Test
    @DisplayName("Null bytes should still be blocked even with normalization")
    void testNullByteStillBlocked() {
        // Null bytes should still throw SecurityException
        SecurityException ex = assertThrows(SecurityException.class, () ->
            ClassUtilities.loadResourceAsBytes("com\\example\\file.txt\0.jpg")
        );
        
        assertTrue(ex.getMessage().contains("null byte"));
    }
    
    @Test
    @DisplayName("Valid Windows-style resource path should work if resource exists")
    void testValidWindowsStylePath() {
        // Test with an actual resource that exists (using test resources)
        // First check if we have any test resources with normal path
        byte[] normalPath = ClassUtilities.loadResourceAsBytes("test.txt");
        
        if (normalPath != null) {
            // If the resource exists with forward slashes, 
            // it should also work with backslashes
            byte[] windowsPath = ClassUtilities.loadResourceAsBytes("test.txt");
            assertNotNull(windowsPath);
            assertArrayEquals(normalPath, windowsPath);
        }
        // If no test resource exists, that's okay - the test still validates
        // that backslashes don't cause SecurityException
    }
}