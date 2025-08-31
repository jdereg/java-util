package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for areAllConstructorsPrivate handling of implicit constructors.
 */
class ClassUtilitiesImplicitConstructorTest {
    
    // Class with no declared constructors - gets implicit public no-arg constructor
    static class NoConstructorsClass {
        public String value = "test";
    }
    
    // Class with explicit public constructor
    static class PublicConstructorClass {
        public PublicConstructorClass() {}
    }
    
    // Class with all private constructors
    static class AllPrivateConstructorsClass {
        private AllPrivateConstructorsClass() {}
        private AllPrivateConstructorsClass(String arg) {}
    }
    
    // Class with mixed visibility constructors
    static class MixedConstructorsClass {
        private MixedConstructorsClass() {}
        public MixedConstructorsClass(String arg) {}
    }
    
    @Test
    @DisplayName("Class with no declared constructors has implicit public constructor")
    void testNoConstructorsClass() {
        // Class with no declared constructors gets implicit public no-arg constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(NoConstructorsClass.class),
                "Class with no declared constructors has implicit public constructor");
        
        // Verify we can actually instantiate it
        assertDoesNotThrow(() -> {
            NoConstructorsClass instance = new NoConstructorsClass();
            assertNotNull(instance);
        });
    }
    
    @Test
    @DisplayName("Class with explicit public constructor returns false")
    void testPublicConstructorClass() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicConstructorClass.class),
                "Class with public constructor should return false");
    }
    
    @Test
    @DisplayName("Class with all private constructors returns true")
    void testAllPrivateConstructorsClass() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivateConstructorsClass.class),
                "Class with all private constructors should return true");
    }
    
    @Test
    @DisplayName("Class with mixed visibility constructors returns false")
    void testMixedConstructorsClass() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedConstructorsClass.class),
                "Class with at least one non-private constructor should return false");
    }
    
    @Test
    @DisplayName("Interface has no constructors but should be handled correctly")
    void testInterface() {
        // Interfaces don't have constructors
        assertFalse(ClassUtilities.areAllConstructorsPrivate(Runnable.class),
                "Interface should return false (no constructors)");
    }
    
    @Test
    @DisplayName("Abstract class with no constructors")
    void testAbstractClassNoConstructors() {
        abstract class AbstractNoConstructors {
            abstract void doSomething();
        }
        
        // Abstract class with no declared constructors gets implicit public constructor
        assertFalse(ClassUtilities.areAllConstructorsPrivate(AbstractNoConstructors.class),
                "Abstract class with no declared constructors has implicit public constructor");
    }
}