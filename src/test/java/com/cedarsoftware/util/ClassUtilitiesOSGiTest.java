package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for OSGi-related functionality in ClassUtilities.
 * These tests verify that OSGi detection and classloader resolution
 * work correctly in both OSGi and non-OSGi environments.
 */
class ClassUtilitiesOSGiTest {
    
    @Test
    @DisplayName("getClassLoader should handle non-OSGi environment gracefully")
    void testGetClassLoader_nonOSGi() {
        // In a non-OSGi environment, getClassLoader should fall back to
        // context classloader or the class's own classloader
        ClassLoader loader = ClassUtilities.getClassLoader(ClassUtilitiesOSGiTest.class);
        assertNotNull(loader, "Should return a classloader in non-OSGi environment");
        
        // Should be either context classloader or our class's loader
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = ClassUtilitiesOSGiTest.class.getClassLoader();
        
        assertTrue(loader == contextLoader || loader == classLoader || loader == ClassLoader.getSystemClassLoader(),
                  "Should be one of the standard classloaders");
    }
    
    @Test
    @DisplayName("getClassLoader should not throw when OSGi classes are not available")
    void testGetClassLoader_noOSGiClasses() {
        // This test verifies that the OSGi detection code doesn't throw
        // when OSGi framework classes are not on the classpath
        assertDoesNotThrow(() -> {
            ClassLoader loader = ClassUtilities.getClassLoader(String.class);
            assertNotNull(loader);
        }, "Should handle missing OSGi classes gracefully");
    }
    
    @Test
    @DisplayName("getClassLoader should be consistent for same class")
    void testGetClassLoader_consistency() {
        ClassLoader loader1 = ClassUtilities.getClassLoader(ClassUtilitiesOSGiTest.class);
        ClassLoader loader2 = ClassUtilities.getClassLoader(ClassUtilitiesOSGiTest.class);
        
        assertSame(loader1, loader2, "Should return same classloader for same class");
    }
    
    @Test
    @DisplayName("getClassLoader should handle null anchor class")
    void testGetClassLoader_nullAnchor() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtilities.getClassLoader(null);
        }, "Should throw for null anchor class");
    }
    
    @Test
    @DisplayName("getClassLoader should handle bootstrap classes")
    void testGetClassLoader_bootstrapClass() {
        // String.class is loaded by bootstrap classloader (returns null)
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertNotNull(loader, "Should return a non-null loader even for bootstrap classes");
        
        // Should fall back to context or system loader
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        assertTrue(loader == contextLoader || loader == ClassLoader.getSystemClassLoader(),
                  "Should use context or system loader for bootstrap classes");
    }
}