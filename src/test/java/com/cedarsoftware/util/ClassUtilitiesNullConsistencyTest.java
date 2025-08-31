package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for consistent null handling in primitive/wrapper conversion methods.
 * Verifies that all methods throw IllegalArgumentException with descriptive messages.
 */
class ClassUtilitiesNullConsistencyTest {
    
    @Test
    @DisplayName("toPrimitiveWrapperClass should throw IllegalArgumentException for null")
    void testToPrimitiveWrapperClassNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            ClassUtilities.toPrimitiveWrapperClass(null)
        );
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
        assertTrue(ex.getMessage().contains("primitiveClass"));
    }
    
    @Test
    @DisplayName("getPrimitiveFromWrapper should throw IllegalArgumentException for null")
    void testGetPrimitiveFromWrapperNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            ClassUtilities.getPrimitiveFromWrapper(null)
        );
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
        assertTrue(ex.getMessage().contains("toType"));
    }
    
    @Test
    @DisplayName("toPrimitiveClass should throw IllegalArgumentException for null")
    void testToPrimitiveClassNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            ClassUtilities.toPrimitiveClass(null)
        );
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
    }
    
    @Test
    @DisplayName("All three methods should throw same exception type for null")
    void testConsistentExceptionType() {
        // All three should throw IllegalArgumentException (not NPE or other exceptions)
        assertThrows(IllegalArgumentException.class, () -> 
            ClassUtilities.toPrimitiveWrapperClass(null));
        assertThrows(IllegalArgumentException.class, () -> 
            ClassUtilities.getPrimitiveFromWrapper(null));
        assertThrows(IllegalArgumentException.class, () -> 
            ClassUtilities.toPrimitiveClass(null));
    }
    
    @Test
    @DisplayName("Verify normal operation still works after null checks")
    void testNormalOperationAfterNullChecks() {
        // toPrimitiveWrapperClass
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
        
        // getPrimitiveFromWrapper
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
        
        // toPrimitiveClass
        assertEquals(int.class, ClassUtilities.toPrimitiveClass(Integer.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveClass(String.class));
    }
}