package com.cedarsoftware.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases for percent-encoded path traversal blocking in resource loading
 * based on GPT-5 security review suggestion.
 */
class ClassUtilitiesPercentEncodedTraversalTest {
    
    @Test
    @DisplayName("Should block percent-encoded .. traversal sequences")
    void testPercentEncodedDoubleDotBlocked() {
        // Test various percent-encoded .. patterns
        String[] blockedPaths = {
            "%2e%2e/etc/passwd",           // %2e%2e = ..
            "%2E%2E/etc/passwd",           // uppercase variant
            "%2e%2E/etc/passwd",           // mixed case
            "config/%2e%2e/secret.key",    // embedded
            "../%2e%2e/../../etc/passwd",  // mixed encoded and literal
            "%252e%252e/etc/passwd"        // double-encoded (% itself encoded)
        };
        
        for (String path : blockedPaths) {
            SecurityException exception = assertThrows(SecurityException.class, 
                () -> ClassUtilities.loadResourceAsBytes(path),
                "Should block percent-encoded traversal: " + path);
            assertTrue(exception.getMessage().contains("encoded traversal"),
                "Exception message should indicate encoded traversal blocking");
        }
    }
    
    @Test
    @DisplayName("Should block mixed percent-encoded and literal dot patterns")
    void testMixedEncodedPatterns() {
        // Test patterns that mix encoded and literal dots
        String[] blockedPaths = {
            "%2e./secret",      // %2e. = ..
            ".%2e/secret",      // .%2e = ..
            "%2E./secret",      // uppercase variant
            ".%2E/secret",      // uppercase variant
            "path/%2e./../../secret",
            "path/.%2e/../../secret"
        };
        
        for (String path : blockedPaths) {
            SecurityException exception = assertThrows(SecurityException.class, 
                () -> ClassUtilities.loadResourceAsBytes(path),
                "Should block mixed encoded pattern: " + path);
            assertTrue(exception.getMessage().contains("encoded traversal"),
                "Exception message should indicate encoded traversal blocking");
        }
    }
    
    @Test
    @DisplayName("Should allow legitimate paths with %2e in different contexts")
    void testLegitimatePercentPaths() {
        // These paths should NOT be blocked as they don't form traversal patterns
        String[] allowedPaths = {
            "file%2ename.txt",          // %2e not forming ..
            "%2e",                      // single encoded dot
            "%2efolder/file.txt",       // encoded dot at start (not ..)
            "folder%2e/file.txt",       // encoded dot at end (not ..)
            "my%2econfig%2exml",        // dots in filename
            "%2d%2e%2d",                // not a traversal pattern
            "test%20%2e%20file.txt"     // spaces around dot
        };
        
        for (String path : allowedPaths) {
            // These should not throw SecurityException for encoded traversal
            // (they might fail for other reasons like resource not found)
            try {
                ClassUtilities.loadResourceAsBytes(path);
                // If we get here, the resource was actually found (unlikely in test)
            } catch (SecurityException e) {
                if (e.getMessage().contains("encoded traversal")) {
                    fail("Should not block legitimate path as encoded traversal: " + path);
                }
                // Other security exceptions are fine
            } catch (IllegalArgumentException e) {
                // Resource not found is expected
                assertTrue(e.getMessage().contains("Resource not found"),
                    "Expected 'resource not found' but got: " + e.getMessage());
            }
        }
    }
    
    @Test
    @DisplayName("Should block case-insensitive percent encoding")
    void testCaseInsensitiveEncoding() {
        // Test that detection is case-insensitive for hex digits
        String[] blockedPaths = {
            "%2e%2e/secret",    // lowercase
            "%2E%2E/secret",    // uppercase  
            "%2e%2E/secret",    // mixed case 1
            "%2E%2e/secret",    // mixed case 2
            "%2e%2e/SECRET",    // path case doesn't matter
            "%2E%2E/SECRET"
        };
        
        for (String path : blockedPaths) {
            SecurityException exception = assertThrows(SecurityException.class, 
                () -> ClassUtilities.loadResourceAsBytes(path),
                "Should block case variant: " + path);
            assertTrue(exception.getMessage().contains("encoded traversal"));
        }
    }
    
    @Test
    @DisplayName("Should block double-encoded sequences")
    void testDoubleEncodedSequences() {
        // Test double-encoding where % itself is encoded as %25
        String[] blockedPaths = {
            "%252e%252e/etc/passwd",       // %25 = %, so %252e = %2e
            "%252E%252E/etc/passwd",       // uppercase
            "path/%252e%252e/../secret"    // mixed with literal
        };
        
        for (String path : blockedPaths) {
            SecurityException exception = assertThrows(SecurityException.class, 
                () -> ClassUtilities.loadResourceAsBytes(path),
                "Should block double-encoded: " + path);
            assertTrue(exception.getMessage().contains("encoded traversal"));
        }
    }
    
    @Test
    @DisplayName("Encoded traversal check happens before other normalizations")
    void testEncodedCheckBeforeNormalization() {
        // Verify that encoded traversal is checked BEFORE backslash normalization
        // This ensures we catch attempts that might try to bypass via backslashes
        String pathWithBackslash = "%2e%2e\\etc\\passwd";
        
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> ClassUtilities.loadResourceAsBytes(pathWithBackslash));
        assertTrue(exception.getMessage().contains("encoded traversal"),
            "Should detect encoded traversal before converting backslashes");
    }
    
    @Test
    @DisplayName("Should work with loadResourceAsString as well")
    void testLoadResourceAsStringAlsoProtected() {
        // Verify both loadResourceAsBytes and loadResourceAsString are protected
        String encodedTraversal = "%2e%2e/etc/passwd";
        
        SecurityException bytesException = assertThrows(SecurityException.class, 
            () -> ClassUtilities.loadResourceAsBytes(encodedTraversal));
        assertTrue(bytesException.getMessage().contains("encoded traversal"));
        
        SecurityException stringException = assertThrows(SecurityException.class, 
            () -> ClassUtilities.loadResourceAsString(encodedTraversal));
        assertTrue(stringException.getMessage().contains("encoded traversal"));
    }
}