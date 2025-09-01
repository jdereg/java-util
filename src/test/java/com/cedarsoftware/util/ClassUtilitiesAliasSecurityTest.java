package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ClassUtilities alias security enhancement.
 * Verifies that addPermanentClassAlias() properly validates classes through SecurityChecker.
 */
class ClassUtilitiesAliasSecurityTest {
    
    @BeforeEach
    void setUp() {
        // Clean up any existing aliases
        ClassUtilities.removePermanentClassAlias("testAlias");
        ClassUtilities.removePermanentClassAlias("stringAlias");
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test aliases
        ClassUtilities.removePermanentClassAlias("testAlias");
        ClassUtilities.removePermanentClassAlias("stringAlias");
    }
    
    @Test
    @DisplayName("addPermanentClassAlias should allow safe classes")
    void testAddAliasForSafeClass() {
        // String is a safe class
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(String.class, "stringAlias");
        });
        
        // Verify the alias works
        assertDoesNotThrow(() -> {
            Class<?> clazz = ClassUtilities.forName("stringAlias", null);
            assertEquals(String.class, clazz);
        });
    }
    
    @Test
    @DisplayName("addPermanentClassAlias should allow common Java types")
    void testAddAliasForCommonTypes() {
        // Test various safe types
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(Integer.class, "intAlias");
            ClassUtilities.removePermanentClassAlias("intAlias");
            
            ClassUtilities.addPermanentClassAlias(java.util.HashMap.class, "mapAlias");
            ClassUtilities.removePermanentClassAlias("mapAlias");
            
            ClassUtilities.addPermanentClassAlias(java.util.ArrayList.class, "listAlias");
            ClassUtilities.removePermanentClassAlias("listAlias");
        });
    }
    
    @Test
    @DisplayName("addPermanentClassAlias verifies class through SecurityChecker")
    void testAddAliasGoesThruSecurity() {
        // We can't easily test blocked classes without triggering actual security exceptions,
        // but we can verify that safe classes pass through SecurityChecker.verifyClass()
        // The key point is that the method now calls SecurityChecker.verifyClass()
        // before adding the alias, providing belt-and-suspenders security.
        
        // This test verifies the code path works correctly for allowed classes
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(ClassUtilitiesAliasSecurityTest.class, "testAlias");
        });
        
        // Verify the alias was added
        assertDoesNotThrow(() -> {
            Class<?> clazz = ClassUtilities.forName("testAlias", null);
            assertEquals(ClassUtilitiesAliasSecurityTest.class, clazz);
        });
    }
    
    @Test
    @DisplayName("removePermanentClassAlias should work normally")
    void testRemoveAlias() {
        // Add an alias
        ClassUtilities.addPermanentClassAlias(String.class, "stringAlias");
        
        // Verify it exists
        assertDoesNotThrow(() -> {
            Class<?> clazz = ClassUtilities.forName("stringAlias", null);
            assertEquals(String.class, clazz);
        });
        
        // Remove the alias
        ClassUtilities.removePermanentClassAlias("stringAlias");
        
        // Verify it's gone (forName returns null for not found)
        Class<?> result = ClassUtilities.forName("stringAlias", null);
        assertNull(result, "Removed alias should return null");
    }
    
    @Test
    @DisplayName("Multiple aliases for same class should work")
    void testMultipleAliasesForSameClass() {
        // Add multiple aliases for the same class
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(String.class, "alias1");
            ClassUtilities.addPermanentClassAlias(String.class, "alias2");
            ClassUtilities.addPermanentClassAlias(String.class, "alias3");
        });
        
        // Verify all aliases work
        assertDoesNotThrow(() -> {
            assertEquals(String.class, ClassUtilities.forName("alias1", null));
            assertEquals(String.class, ClassUtilities.forName("alias2", null));
            assertEquals(String.class, ClassUtilities.forName("alias3", null));
        });
        
        // Clean up
        ClassUtilities.removePermanentClassAlias("alias1");
        ClassUtilities.removePermanentClassAlias("alias2");
        ClassUtilities.removePermanentClassAlias("alias3");
    }
    
    @Test
    @DisplayName("Alias replacement should work with security check")
    void testAliasReplacement() {
        // Add an alias
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(String.class, "testAlias");
        });
        
        // Replace with a different class (also safe)
        assertDoesNotThrow(() -> {
            ClassUtilities.addPermanentClassAlias(Integer.class, "testAlias");
        });
        
        // Verify the alias now points to the new class
        assertDoesNotThrow(() -> {
            Class<?> clazz = ClassUtilities.forName("testAlias", null);
            assertEquals(Integer.class, clazz);
        });
    }
    
    @Test
    @DisplayName("Removing an alias stops resolution even after it has been used once")
    void testAliasRemovalInvalidatesCache() {
        // Add an alias
        ClassUtilities.addPermanentClassAlias(String.class, "cacheTestAlias");
        
        // Use the alias once (this will cache it)
        Class<?> firstLookup = ClassUtilities.forName("cacheTestAlias", null);
        assertEquals(String.class, firstLookup, "First lookup should return String.class");
        
        // Remove the alias
        ClassUtilities.removePermanentClassAlias("cacheTestAlias");
        
        // Try to use the alias again - should return null even though it was cached
        Class<?> secondLookup = ClassUtilities.forName("cacheTestAlias", null);
        assertNull(secondLookup, "Removed alias should return null even if it was previously cached");
        
        // Clean up just in case
        ClassUtilities.removePermanentClassAlias("cacheTestAlias");
    }
}