package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for Windows absolute drive path blocking in resource loading.
 */
class ClassUtilitiesWindowsDrivePathTest {
    
    @Test
    @DisplayName("loadResourceAsBytes should reject Windows absolute drive paths")
    void testWindowsAbsoluteDrivePathsBlocked() {
        // Test various Windows absolute path patterns
        String[] blockedPaths = {
            "C:/windows/system32/config.sys",
            "D:/Users/secret.txt",
            "E:/Program Files/app.exe",
            "c:/temp/file.txt",  // lowercase drive letter
            "Z:/network/share.doc",
            "A:/floppy.dat",
            "C:\\windows\\system32\\config.sys"  // backslashes (will be normalized)
        };
        
        for (String path : blockedPaths) {
            SecurityException exception = assertThrows(SecurityException.class, 
                () -> ClassUtilities.loadResourceAsBytes(path),
                "Should block Windows absolute path: " + path);
            assertTrue(exception.getMessage().contains("Absolute/UNC paths not allowed"),
                "Exception message should indicate absolute paths are blocked");
        }
    }
    
    @Test
    @DisplayName("loadResourceAsBytes should allow legitimate resource paths that might look like drive paths")
    void testLegitimatePathsNotBlocked() {
        // These paths should NOT be blocked as they don't match the pattern
        String[] allowedPaths = {
            "com/example/C:/notreally.txt",  // C: not at start
            "resources/D:notadrive.txt",     // No slash after colon
            "C:relative.txt",                // No slash (relative to C: drive in Windows, but not absolute)
            "CC:/twocolons.txt",             // Two letters before colon
            "1:/numeric.txt",                // Numeric, not letter
            "/C:/stillnotabsolute.txt"       // Leading slash makes it not match
        };
        
        for (String path : allowedPaths) {
            // These should not throw SecurityException for the drive path check
            // (they might fail for other reasons like resource not found)
            try {
                ClassUtilities.loadResourceAsBytes(path);
                // If we get here, the resource was actually found (unlikely in test)
            } catch (SecurityException e) {
                if (e.getMessage().contains("Absolute/UNC paths not allowed")) {
                    fail("Should not block legitimate path as Windows drive path: " + path);
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
    @DisplayName("loadResourceAsString should also reject Windows absolute drive paths")
    void testLoadResourceAsStringBlocksWindowsPaths() {
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> ClassUtilities.loadResourceAsString("C:/windows/system.ini"));
        assertTrue(exception.getMessage().contains("Absolute/UNC paths not allowed"));
    }
}