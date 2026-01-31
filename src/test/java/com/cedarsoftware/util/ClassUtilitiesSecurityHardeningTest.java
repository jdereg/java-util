package com.cedarsoftware.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for enhanced security hardening in ClassUtilities based on GPT-5 review.
 * Tests blocking of Nashorn JavaScript engine and MethodHandles$Lookup.
 */
class ClassUtilitiesSecurityHardeningTest {
    
    @Test
    @DisplayName("Should block jdk.nashorn package classes")
    void testNashornPackageBlocked() {
        // Test various Nashorn classes that should be blocked
        String[] nashornClasses = {
            "jdk.nashorn.api.scripting.NashornScriptEngine",
            "jdk.nashorn.api.scripting.NashornScriptEngineFactory",
            "jdk.nashorn.internal.runtime.Context",
            "jdk.nashorn.internal.runtime.ScriptRuntime",
            "jdk.nashorn.api.tree.Parser",
            "jdk.nashorn.internal.objects.Global"
        };
        
        for (String className : nashornClasses) {
            // forName throws SecurityException for blocked classes
            SecurityException exception = assertThrows(SecurityException.class,
                () -> ClassUtilities.forName(className, null),
                "Should throw SecurityException for Nashorn class: " + className);
            assertTrue(exception.getMessage().contains("cannot load"),
                "Exception should indicate class cannot be loaded");
            
            // Verify the name is identified as blocked
            assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className),
                "Should identify " + className + " as security blocked");
        }
    }
    
    @Test
    @DisplayName("Should block MethodHandles$Lookup class")
    void testMethodHandlesLookupBlocked() {
        String lookupClass = "java.lang.invoke.MethodHandles$Lookup";
        
        // The actual class exists in the JVM, but we should block loading it by name
        SecurityException exception = assertThrows(SecurityException.class,
            () -> ClassUtilities.forName(lookupClass, null),
            "Should throw SecurityException for MethodHandles$Lookup");
        assertTrue(exception.getMessage().contains("cannot load"),
            "Exception should indicate class cannot be loaded");
        
        // Verify the name is identified as blocked
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(lookupClass),
            "Should identify MethodHandles$Lookup as security blocked");
    }
    
    @Test
    @DisplayName("Should continue to block javax.script package")
    void testJavaxScriptStillBlocked() {
        // Ensure existing javax.script blocking still works
        String[] scriptClasses = {
            "javax.script.ScriptEngine",
            "javax.script.ScriptEngineManager",
            "javax.script.ScriptEngineFactory",
            "javax.script.Invocable",
            "javax.script.Compilable"
        };
        
        for (String className : scriptClasses) {
            // forName throws SecurityException for blocked classes
            SecurityException exception = assertThrows(SecurityException.class,
                () -> ClassUtilities.forName(className, null),
                "Should throw SecurityException for javax.script class: " + className);
            assertTrue(exception.getMessage().contains("cannot load"),
                "Exception should indicate class cannot be loaded");
            
            assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className),
                "Should identify " + className + " as security blocked");
        }
    }
    
    @Test
    @DisplayName("Should not block legitimate java.lang.invoke classes")
    void testLegitimateInvokeClassesNotBlocked() {
        // These classes in java.lang.invoke should NOT be blocked
        // Only MethodHandles$Lookup should be blocked
        String[] allowedClasses = {
            "java.lang.invoke.MethodHandle",
            "java.lang.invoke.MethodType",
            "java.lang.invoke.CallSite",
            "java.lang.invoke.VolatileCallSite",
            "java.lang.invoke.MutableCallSite",
            "java.lang.invoke.ConstantCallSite"
        };
        
        for (String className : allowedClasses) {
            assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName(className),
                "Should NOT block legitimate invoke class: " + className);
            
            // These classes should be loadable (they're part of core Java)
            Class<?> clazz = ClassUtilities.forName(className, null);
            assertNotNull(clazz, "Should allow loading of legitimate invoke class: " + className);
        }
    }
    
    @Test
    @DisplayName("Should not block classes with similar but different names")
    void testSimilarNamesNotBlocked() {
        // These should NOT be blocked despite similar names
        String[] allowedNames = {
            "com.example.jdk.nashorn.MyClass",  // Not actually in jdk.nashorn package
            "javax.scriptlet.Something",        // Similar but different package
            "my.app.NashornHelper",             // Contains "nashorn" but not in the package
            "java.lang.invoke.MyHelper"         // In invoke package but not the Lookup class
        };
        
        for (String className : allowedNames) {
            assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName(className),
                "Should NOT block class with similar name: " + className);
        }
    }
    
    @Test
    @DisplayName("Existing security blocks should still work")
    void testExistingSecurityBlocksStillWork() {
        // Verify that our new changes didn't break existing security
        String[] existingBlocked = {
            "java.lang.ProcessImpl",
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "javax.script.ScriptEngine",
            "javax.script.ScriptEngineManager"
        };
        
        for (String className : existingBlocked) {
            assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className),
                "Existing security block should still work for: " + className);
        }
    }
}