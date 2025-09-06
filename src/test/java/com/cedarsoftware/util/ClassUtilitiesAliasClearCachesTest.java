package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies user-added aliases are preserved when clearCaches() is called.
 * This addresses the GPT-5 security review concern about permanent aliases being
 * inadvertently removed by clearCaches().
 */
class ClassUtilitiesAliasClearCachesTest {

    @BeforeEach
    void setUp() {
        // Clean state before each test
        ClassUtilities.clearCaches();
    }

    @AfterEach
    void tearDown() {
        // Clean up any test aliases
        ClassUtilities.removePermanentClassAlias("testAlias1");
        ClassUtilities.removePermanentClassAlias("testAlias2");
        ClassUtilities.removePermanentClassAlias("testAlias3");
        ClassUtilities.removePermanentClassAlias("userAlias");
        ClassUtilities.removePermanentClassAlias("myCustomClass");
    }

    @Test
    @DisplayName("User-added aliases should be preserved after clearCaches()")
    void testUserAliasesPreservedAfterClearCaches() {
        // Add user aliases
        ClassUtilities.addPermanentClassAlias(String.class, "testAlias1");
        ClassUtilities.addPermanentClassAlias(Integer.class, "testAlias2");
        ClassUtilities.addPermanentClassAlias(java.util.HashMap.class, "testAlias3");

        // Verify aliases work before clearing
        assertEquals(String.class, ClassUtilities.forName("testAlias1", null));
        assertEquals(Integer.class, ClassUtilities.forName("testAlias2", null));
        assertEquals(java.util.HashMap.class, ClassUtilities.forName("testAlias3", null));

        // Clear caches
        ClassUtilities.clearCaches();

        // Verify user aliases are still present after clearing
        assertEquals(String.class, ClassUtilities.forName("testAlias1", null), 
            "User alias 'testAlias1' should be preserved after clearCaches()");
        assertEquals(Integer.class, ClassUtilities.forName("testAlias2", null),
            "User alias 'testAlias2' should be preserved after clearCaches()");
        assertEquals(java.util.HashMap.class, ClassUtilities.forName("testAlias3", null),
            "User alias 'testAlias3' should be preserved after clearCaches()");
    }

    @Test
    @DisplayName("Built-in aliases should always be available after clearCaches()")
    void testBuiltinAliasesAlwaysPresent() {
        // Clear caches
        ClassUtilities.clearCaches();

        // Verify built-in aliases are present
        assertEquals(Boolean.TYPE, ClassUtilities.forName("boolean", null));
        assertEquals(Character.TYPE, ClassUtilities.forName("char", null));
        assertEquals(Byte.TYPE, ClassUtilities.forName("byte", null));
        assertEquals(Short.TYPE, ClassUtilities.forName("short", null));
        assertEquals(Integer.TYPE, ClassUtilities.forName("int", null));
        assertEquals(Long.TYPE, ClassUtilities.forName("long", null));
        assertEquals(Float.TYPE, ClassUtilities.forName("float", null));
        assertEquals(Double.TYPE, ClassUtilities.forName("double", null));
        assertEquals(Void.TYPE, ClassUtilities.forName("void", null));
        assertEquals(String.class, ClassUtilities.forName("string", null));
        assertEquals(java.util.Date.class, ClassUtilities.forName("date", null));
        assertEquals(Class.class, ClassUtilities.forName("class", null));
    }

    @Test
    @DisplayName("User aliases should override built-in aliases and survive clearCaches()")
    void testUserAliasOverridesBuiltin() {
        // Override a built-in alias
        ClassUtilities.addPermanentClassAlias(Integer.class, "string"); // Override "string" -> String.class

        // Verify override works
        assertEquals(Integer.class, ClassUtilities.forName("string", null),
            "User should be able to override built-in aliases");

        // Clear caches
        ClassUtilities.clearCaches();

        // Verify user override is preserved
        assertEquals(Integer.class, ClassUtilities.forName("string", null),
            "User override of built-in alias should be preserved after clearCaches()");

        // Clean up the override
        ClassUtilities.removePermanentClassAlias("string");
        
        // After removing user override, built-in should be restored
        assertEquals(String.class, ClassUtilities.forName("string", null),
            "Built-in alias should be restored after removing user override");
    }

    @Test
    @DisplayName("Multiple clearCaches() calls should not affect user aliases")
    void testMultipleClearCachesPreservesUserAliases() {
        // Add a user alias
        ClassUtilities.addPermanentClassAlias(java.util.ArrayList.class, "userAlias");
        
        // Verify it works
        assertEquals(java.util.ArrayList.class, ClassUtilities.forName("userAlias", null));

        // Clear caches multiple times
        for (int i = 0; i < 5; i++) {
            ClassUtilities.clearCaches();
            assertEquals(java.util.ArrayList.class, ClassUtilities.forName("userAlias", null),
                "User alias should survive clearCaches() call #" + (i + 1));
        }
    }

    @Test
    @DisplayName("User aliases added, then clearCaches(), then more user aliases added")
    void testAddAliasesBeforeAndAfterClearCaches() {
        // Add first user alias
        ClassUtilities.addPermanentClassAlias(String.class, "testAlias1");
        assertEquals(String.class, ClassUtilities.forName("testAlias1", null));

        // Clear caches
        ClassUtilities.clearCaches();

        // First alias should still work
        assertEquals(String.class, ClassUtilities.forName("testAlias1", null));

        // Add second user alias after clearCaches
        ClassUtilities.addPermanentClassAlias(Integer.class, "testAlias2");
        assertEquals(Integer.class, ClassUtilities.forName("testAlias2", null));

        // Clear caches again
        ClassUtilities.clearCaches();

        // Both aliases should still work
        assertEquals(String.class, ClassUtilities.forName("testAlias1", null),
            "First alias should survive second clearCaches()");
        assertEquals(Integer.class, ClassUtilities.forName("testAlias2", null),
            "Second alias should survive clearCaches()");
    }

    @Test
    @DisplayName("Removing user alias after clearCaches() should work correctly")
    void testRemoveUserAliasAfterClearCaches() {
        // Add user alias
        ClassUtilities.addPermanentClassAlias(java.util.LinkedList.class, "myCustomClass");
        assertEquals(java.util.LinkedList.class, ClassUtilities.forName("myCustomClass", null));

        // Clear caches
        ClassUtilities.clearCaches();

        // Alias should still work
        assertEquals(java.util.LinkedList.class, ClassUtilities.forName("myCustomClass", null));

        // Remove the alias
        ClassUtilities.removePermanentClassAlias("myCustomClass");

        // Alias should no longer work
        assertNull(ClassUtilities.forName("myCustomClass", null),
            "Removed user alias should return null");

        // Clear caches again
        ClassUtilities.clearCaches();

        // Alias should still be gone
        assertNull(ClassUtilities.forName("myCustomClass", null),
            "Removed user alias should stay removed after clearCaches()");
    }

    @Test
    @DisplayName("clearCaches() should not affect other cache clearing functionality")
    void testClearCachesStillClearsOtherCaches() {
        // This test verifies that clearCaches() still does its primary job
        // of clearing other internal caches, not just preserving aliases.
        // We can't directly test internal cache state, but we can verify
        // the method runs without error and basic functionality still works.
        
        // Force some caching to occur
        ClassUtilities.forName("java.lang.String", null);
        ClassUtilities.forName("java.util.HashMap", null);
        
        // Add a user alias
        ClassUtilities.addPermanentClassAlias(java.util.TreeMap.class, "userAlias");
        
        // Clear caches - should clear internal caches but preserve aliases
        assertDoesNotThrow(ClassUtilities::clearCaches);
        
        // Verify basic functionality still works
        assertEquals(String.class, ClassUtilities.forName("java.lang.String", null));
        assertEquals(java.util.HashMap.class, ClassUtilities.forName("java.util.HashMap", null));
        
        // Verify user alias is preserved
        assertEquals(java.util.TreeMap.class, ClassUtilities.forName("userAlias", null));
    }
}