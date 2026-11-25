package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security tests for ArrayUtilities: the array-size limit. (Until 4.119.0 also a "dangerous class" check, which is
 * gone: see testNoClassIsRefusedAndTheOldPropertiesAreIgnored.)
 */
public class ArrayUtilitiesSecurityTest {
    
    private String originalSecurityEnabled;
    private String originalComponentTypeValidationEnabled;
    private String originalDangerousClassValidationEnabled;
    private String originalMaxArraySize;
    private String originalDangerousClassPatterns;
    
    @BeforeEach
    public void setUp() {
        // Save original system property values
        originalSecurityEnabled = System.getProperty("arrayutilities.security.enabled");
        originalComponentTypeValidationEnabled = System.getProperty("arrayutilities.component.type.validation.enabled");
        originalDangerousClassValidationEnabled = System.getProperty("arrayutilities.dangerous.classes.validation.enabled");
        originalMaxArraySize = System.getProperty("arrayutilities.max.array.size");
        originalDangerousClassPatterns = System.getProperty("arrayutilities.dangerous.class.patterns");
        
        // Enable security features for testing
        System.setProperty("arrayutilities.security.enabled", "true");
        System.setProperty("arrayutilities.component.type.validation.enabled", "true");
        System.setProperty("arrayutilities.dangerous.classes.validation.enabled", "true");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original system property values
        restoreProperty("arrayutilities.security.enabled", originalSecurityEnabled);
        restoreProperty("arrayutilities.component.type.validation.enabled", originalComponentTypeValidationEnabled);
        restoreProperty("arrayutilities.dangerous.classes.validation.enabled", originalDangerousClassValidationEnabled);
        restoreProperty("arrayutilities.max.array.size", originalMaxArraySize);
        restoreProperty("arrayutilities.dangerous.class.patterns", originalDangerousClassPatterns);
    }
    
    private void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }
    
    // Test component type validation
                        
    @Test
    public void testNullToEmpty_safeClass_works() {
        String[] result = ArrayUtilities.nullToEmpty(String.class, null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }
    
    // Test integer overflow protection in addAll
    
    @Test
    public void testAddAll_integerOverflow_throwsException() {
        // Test the validation logic directly instead of creating large arrays
        long overflowSize = (long) Integer.MAX_VALUE + 100;
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.validateArraySize(overflowSize);
        });
        
        assertTrue(exception.getMessage().contains("Array size too large"),
                  "Should prevent integer overflow in array combination");
    }
    
    @Test
    public void testAddAll_maxSizeArray_throwsException() {
        // Test the validation logic directly  
        long maxSize = Integer.MAX_VALUE - 7;
        long tooLarge = maxSize + 100;
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.validateArraySize(tooLarge);
        });
        
        assertTrue(exception.getMessage().contains("Array size too large"),
                  "Should prevent creation of arrays larger than max size");
    }
        
    @Test
    public void testAddAll_safeArrays_works() {
        String[] array1 = {"a", "b"};
        String[] array2 = {"c", "d"};
        
        String[] result = ArrayUtilities.addAll(array1, array2);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, result);
    }
    
    // Test integer overflow protection in addItem
    
    @Test
    public void testAddItem_maxSizeArray_throwsException() {
        // Test the validation logic directly instead of creating huge arrays
        long maxSize = Integer.MAX_VALUE - 8;
        long tooLarge = maxSize + 1;
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.validateArraySize(tooLarge);
        });
        
        assertTrue(exception.getMessage().contains("Array size too large"),
                  "Should prevent adding item to max-sized array");
    }
        
    @Test
    public void testAddItem_safeClass_works() {
        String[] array = {"a", "b"};
        String[] result = ArrayUtilities.addItem(String.class, array, "c");
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }
    
    // Test removeItem security
        
    @Test
    public void testRemoveItem_invalidIndex_genericError() {
        String[] array = {"a", "b", "c"};
        
        Exception exception = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            ArrayUtilities.removeItem(array, -1);
        });
        
        // Security: Error message should not expose array details
        assertEquals("Invalid array index", exception.getMessage(),
                    "Error message should be generic for security");
    }
    
    @Test
    public void testRemoveItem_safeArray_works() {
        String[] array = {"a", "b", "c"};
        String[] result = ArrayUtilities.removeItem(array, 1);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertArrayEquals(new String[]{"a", "c"}, result);
    }
    
    // Test toArray security
        
    @Test
    public void testToArray_largeCollection_throwsException() {
        // Create a collection that claims to be too large
        Collection<String> largeCollection = new ArrayList<String>() {
            @Override
            public int size() {
                return Integer.MAX_VALUE; // Return max int to trigger size validation
            }
        };
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.toArray(String.class, largeCollection);
        });
        
        assertTrue(exception.getMessage().contains("Array size too large"),
                  "Should prevent creation of oversized arrays from collections");
    }
    
    @Test
    public void testToArray_safeCollection_works() {
        List<String> list = Arrays.asList("x", "y", "z");
        String[] result = ArrayUtilities.toArray(String.class, list);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new String[]{"x", "y", "z"}, result);
    }
    
    // Test boundary conditions
    
    @Test
    public void testSecurity_maxAllowedArraySize() {
        // Test that we can create arrays up to the security limit
        int maxAllowed = Integer.MAX_VALUE - 8;
        
        // This should NOT throw an exception (though it may cause OutOfMemoryError)
        assertDoesNotThrow(() -> {
            ArrayUtilities.validateArraySize(maxAllowed);
        }, "Should allow arrays up to max size");
        
        // This SHOULD throw an exception
        assertThrows(SecurityException.class, () -> {
            ArrayUtilities.validateArraySize(maxAllowed + 1);
        }, "Should reject arrays larger than max size");
    }
    
    @Test
    public void testSecurity_negativeArraySize() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.validateArraySize(-1);
        });
        
        assertTrue(exception.getMessage().contains("cannot be negative"),
                  "Should reject negative array sizes");
    }
    
    // Test thread safety of security controls
        
    // Test comprehensive dangerous class coverage
        
    // Test that safe classes are allowed
    
    @Test
    public void testSecurity_safeClassesAllowed() {
        // Test various safe classes are allowed
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(String.class, null);
        }, "String should be allowed");
        
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(Integer.class, null);
        }, "Integer should be allowed");
        
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(Object.class, null);
        }, "Object should be allowed");
        
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(java.util.List.class, null);
        }, "List should be allowed");
    }
    
    // Test backward compatibility (security disabled by default)
    
    @Test
    public void testSecurity_disabledByDefault() {
        // Clear security properties to test defaults
        System.clearProperty("arrayutilities.security.enabled");
        System.clearProperty("arrayutilities.component.type.validation.enabled");
        
        // Dangerous classes should be allowed when security is disabled
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(Runtime.class, null);
        }, "Runtime should be allowed when security is disabled");
        
        assertDoesNotThrow(() -> {
            ArrayUtilities.nullToEmpty(System.class, null);
        }, "System should be allowed when security is disabled");
        
        // Large arrays should be allowed when security is disabled
        assertDoesNotThrow(() -> {
            ArrayUtilities.validateArraySize(Long.MAX_VALUE);
        }, "Large arrays should be allowed when security is disabled");
    }
    
    // Test configurable dangerous class patterns
    
    /**
     * getArraySubset() is held to the array-size limit like every other method that creates an array. It was the
     * one that allocates from a number the caller passes: an end past the array's length pads the result with nulls
     * (as Arrays.copyOfRange does), so with a limit of 1,000 getArraySubset(oneElement, 0, 10_000_000) still made a
     * 10 million element array.
     */
    @Test
    public void testGetArraySubsetIsHeldToTheArraySizeLimit() {
        System.setProperty("arrayutilities.max.array.size", "1000");
        String[] small = {"a", "b"};
        SecurityException e = assertThrows(SecurityException.class,
                () -> ArrayUtilities.getArraySubset(small, 0, 10_000_000));
        assertTrue(e.getMessage().contains("Array size too large"), e.getMessage());
        assertArrayEquals(new String[]{"b", null, null}, ArrayUtilities.getArraySubset(small, 1, 4));   // padded
        assertEquals(1000, ArrayUtilities.getArraySubset(small, 0, 1000).length);                       // at the limit
        assertArrayEquals(new String[]{"a"}, ArrayUtilities.getArraySubset(small, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.getArraySubset(small, 2, 1)); // as before
        assertThrows(NullPointerException.class, () -> ArrayUtilities.getArraySubset((String[]) null, 0, 1));
    }

    /**
     * 4.119.0: no class is refused. ArrayUtilities could be told to refuse creating an array of a "dangerous"
     * class (Runtime, ProcessBuilder, Class, anything in java.security or sun), with three properties. An array of
     * a class grants nothing -- new Runtime[3] is three nulls, and any caller can write that -- and the check stood
     * on 5 of the 12 paths that create an array. It is gone, and its properties (which setUp() still sets) are
     * ignored. Fourteen tests here pinned the refusals.
     */
    @Test
    public void testNoClassIsRefusedAndTheOldPropertiesAreIgnored() {
        System.setProperty("arrayutilities.dangerous.class.patterns", "java.lang.Runtime,java.lang.String,java.");
        Runtime runtime = Runtime.getRuntime();
        assertEquals(0, ArrayUtilities.nullToEmpty(Runtime.class, null).length);
        assertEquals(0, ArrayUtilities.nullToEmpty(ProcessBuilder.class, null).length);
        assertEquals(0, ArrayUtilities.nullToEmpty(Class.class, null).length);
        assertEquals(0, ArrayUtilities.nullToEmpty(java.security.MessageDigest.class, null).length);
        assertEquals(0, ArrayUtilities.nullToEmpty(Runtime[].class, null).length);
        assertEquals(0, ArrayUtilities.nullToEmpty(String.class, null).length);
        Runtime[] existing = {runtime};
        assertSame(existing, ArrayUtilities.nullToEmpty(Runtime.class, existing));
        assertEquals(2, ArrayUtilities.addAll(existing, existing).length);
        assertEquals(2, ArrayUtilities.addItem(Runtime.class, existing, runtime).length);
        assertEquals(1, ArrayUtilities.addItem(System.class, null, null).length);
        assertEquals(0, ArrayUtilities.removeItem(existing, 0).length);
        assertEquals(1, ArrayUtilities.toArray(Runtime.class, java.util.Collections.singletonList(runtime)).length);
    }

    /**
     * The size limit, which is what remains, applies with its property unset, and nothing is written: ArrayUtilities
     * wrote its defaults into the system properties when the class was loaded, which nothing needed and which failed
     * class initialization under a SecurityManager that does not let the library write properties.
     */
    @Test
    public void testTheSizeLimitAppliesWithoutItsPropertyAndNoneIsWritten() {
        System.clearProperty("arrayutilities.dangerous.class.patterns");
        System.clearProperty("arrayutilities.max.array.size");
        assertEquals(3, ArrayUtilities.toArray(String.class, java.util.Arrays.asList("a", "b", "c")).length);
        assertEquals(1000, ArrayUtilities.getArraySubset(new String[]{"a"}, 0, 1000).length);
        assertThrows(SecurityException.class, () -> ArrayUtilities.validateArraySize(Integer.MAX_VALUE));   // the default limit
        assertNull(System.getProperty("arrayutilities.dangerous.class.patterns"));
        assertNull(System.getProperty("arrayutilities.max.array.size"));
    }

    /** The settings are read on every call, from any thread: two threads see the limit one of them is refused by. */
    @Test
    public void testSecurity_threadSafety() throws InterruptedException {
        System.setProperty("arrayutilities.max.array.size", "100");
        final boolean[] refused = new boolean[2];
        final Exception[] unexpected = new Exception[2];
        Thread[] threads = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int n = i;
            threads[n] = new Thread(() -> {
                try {
                    for (int round = 0; round < 2000; round++) {
                        ArrayUtilities.getArraySubset(new String[]{"a"}, 0, 100);
                    }
                    ArrayUtilities.getArraySubset(new String[]{"a"}, 0, 101);
                } catch (SecurityException e) {
                    refused[n] = true;
                } catch (Exception e) {
                    unexpected[n] = e;
                }
            });
            threads[n].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertNull(unexpected[0]);
        assertNull(unexpected[1]);
        assertTrue(refused[0] && refused[1]);
    }
}
