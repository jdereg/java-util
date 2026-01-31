package com.cedarsoftware.util;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test cases for ClassLoader-scoped caching in ClassUtilities.
 * Verifies that class names are cached per ClassLoader to prevent
 * cross-loader collisions in multi-classloader environments.
 */
class ClassUtilitiesClassLoaderCacheTest {
    
    @Test
    @DisplayName("Classes loaded from different ClassLoaders are cached separately")
    void testClassLoaderScopedCaching() throws Exception {
        // Load a class using the system classloader
        // Note: ArrayList is loaded by bootstrap loader (null) not system loader
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        Class<?> class1 = ClassUtilities.forName("java.util.ArrayList", systemLoader);
        assertNotNull(class1);
        assertEquals("java.util.ArrayList", class1.getName());
        // ArrayList is loaded by bootstrap classloader
        assertNull(class1.getClassLoader());
        
        // Load the same class name using a different classloader
        // Note: ArrayList will still come from bootstrap loader, but the cache key differs
        URL[] urls = new URL[0];
        URLClassLoader customLoader = new URLClassLoader(urls, systemLoader);
        Class<?> class2 = ClassUtilities.forName("java.util.ArrayList", customLoader);
        assertNotNull(class2);
        assertEquals("java.util.ArrayList", class2.getName());
        
        // Both should resolve to the same class (ArrayList is a system class)
        assertSame(class1, class2);
        
        // Test with a custom class that would truly differ between loaders
        // For this test, we'll use a test class from java-util itself
        Class<?> testClass = ClassUtilities.forName("com.cedarsoftware.util.ClassUtilities", systemLoader);
        assertNotNull(testClass);
        assertEquals("com.cedarsoftware.util.ClassUtilities", testClass.getName());
        // This class is loaded by the app classloader
        assertNotNull(testClass.getClassLoader());
        
        customLoader.close();
    }
    
    @Test
    @DisplayName("Global aliases are accessible from all ClassLoaders")
    void testGlobalAliases() {
        // Test primitive type aliases
        Class<?> intType1 = ClassUtilities.forName("int", null);
        Class<?> intType2 = ClassUtilities.forName("int", ClassLoader.getSystemClassLoader());
        
        assertNotNull(intType1);
        assertNotNull(intType2);
        assertSame(int.class, intType1);
        assertSame(int.class, intType2);
        assertSame(intType1, intType2);
        
        // Test common aliases
        Class<?> stringAlias1 = ClassUtilities.forName("string", null);
        Class<?> stringAlias2 = ClassUtilities.forName("string", ClassLoader.getSystemClassLoader());
        
        assertNotNull(stringAlias1);
        assertNotNull(stringAlias2);
        assertSame(String.class, stringAlias1);
        assertSame(String.class, stringAlias2);
    }
    
    @Test
    @DisplayName("User-defined aliases are global across ClassLoaders")
    void testUserDefinedAliases() {
        String alias = "mySpecialList";
        
        try {
            // Add a custom alias
            ClassUtilities.addPermanentClassAlias(java.util.LinkedList.class, alias);
            
            // Should be accessible from different classloaders
            Class<?> class1 = ClassUtilities.forName(alias, null);
            Class<?> class2 = ClassUtilities.forName(alias, ClassLoader.getSystemClassLoader());
            
            assertNotNull(class1);
            assertNotNull(class2);
            assertSame(java.util.LinkedList.class, class1);
            assertSame(java.util.LinkedList.class, class2);
            assertSame(class1, class2);
            
        } finally {
            // Clean up
            ClassUtilities.removePermanentClassAlias(alias);
        }
        
        // After removal, should not be found
        Class<?> afterRemoval = ClassUtilities.forName(alias, null);
        assertNull(afterRemoval);
    }
    
    @Test
    @DisplayName("Cache is cleared properly with clearCaches()")
    void testClearCaches() {
        // Load a class to populate cache
        Class<?> beforeClear = ClassUtilities.forName("java.util.HashMap", null);
        assertNotNull(beforeClear);
        
        // Clear caches
        ClassUtilities.clearCaches();
        
        // Should still work after clearing (will re-cache)
        Class<?> afterClear = ClassUtilities.forName("java.util.HashMap", null);
        assertNotNull(afterClear);
        assertSame(beforeClear, afterClear);
        
        // Primitive aliases should be restored
        Class<?> intType = ClassUtilities.forName("int", null);
        assertSame(int.class, intType);
        
        Class<?> stringAlias = ClassUtilities.forName("string", null);
        assertSame(String.class, stringAlias);
    }
}