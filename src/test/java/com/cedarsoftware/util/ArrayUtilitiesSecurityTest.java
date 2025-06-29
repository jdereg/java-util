package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for ArrayUtilities.
 * Verifies that security controls prevent memory exhaustion, reflection attacks,
 * and other array-related security vulnerabilities.
 */
public class ArrayUtilitiesSecurityTest {
    
    // Test component type validation
    
    @Test
    public void testNullToEmpty_dangerousClass_throwsException() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.nullToEmpty(Runtime.class, null);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block dangerous class array creation");
    }
    
    @Test
    public void testNullToEmpty_systemClass_throwsException() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.nullToEmpty(System.class, null);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block System class array creation");
    }
    
    @Test
    public void testNullToEmpty_processBuilderClass_throwsException() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.nullToEmpty(ProcessBuilder.class, null);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block ProcessBuilder class array creation");
    }
    
    @Test
    public void testNullToEmpty_securityClass_throwsException() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.nullToEmpty(java.security.Provider.class, null);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block security package class array creation");
    }
    
    @Test
    public void testNullToEmpty_sunClass_throwsException() {
        // Test sun.* package blocking (if available)
        try {
            Class<?> sunClass = Class.forName("sun.misc.Unsafe");
            Exception exception = assertThrows(SecurityException.class, () -> {
                ArrayUtilities.nullToEmpty(sunClass, null);
            });
            
            assertTrue(exception.getMessage().contains("Array creation denied"),
                      "Should block sun package class array creation");
        } catch (ClassNotFoundException e) {
            // sun.misc.Unsafe not available in this JVM, skip test
            assertTrue(true, "sun.misc.Unsafe not available, test skipped");
        }
    }
    
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
    public void testAddAll_dangerousComponentType_throwsException() {
        Runtime[] array1 = new Runtime[1];
        Runtime[] array2 = new Runtime[1];
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.addAll(array1, array2);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block dangerous class array operations");
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
    public void testAddItem_dangerousClass_throwsException() {
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.addItem(Runtime.class, null, null);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block dangerous class array creation");
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
    public void testRemoveItem_dangerousClass_throwsException() {
        Runtime[] array = new Runtime[3];
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.removeItem(array, 1);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block dangerous class array operations");
    }
    
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
    public void testToArray_dangerousClass_throwsException() {
        List<String> list = Arrays.asList("a", "b");
        
        Exception exception = assertThrows(SecurityException.class, () -> {
            ArrayUtilities.toArray(Runtime.class, list);
        });
        
        assertTrue(exception.getMessage().contains("Array creation denied"),
                  "Should block dangerous class array creation");
    }
    
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
    
    @Test
    public void testSecurity_threadSafety() throws InterruptedException {
        final Exception[] exceptions = new Exception[2];
        final boolean[] results = new boolean[2];
        
        Thread thread1 = new Thread(() -> {
            try {
                ArrayUtilities.nullToEmpty(Runtime.class, null);
                results[0] = false; // Should not reach here
            } catch (SecurityException e) {
                results[0] = true; // Expected
            } catch (Exception e) {
                exceptions[0] = e;
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                ArrayUtilities.addItem(System.class, null, null);
                results[1] = false; // Should not reach here
            } catch (SecurityException e) {
                results[1] = true; // Expected
            } catch (Exception e) {
                exceptions[1] = e;
            }
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        assertNull(exceptions[0], "Thread 1 should not have thrown unexpected exception");
        assertNull(exceptions[1], "Thread 2 should not have thrown unexpected exception");
        assertTrue(results[0], "Thread 1 should have caught SecurityException");
        assertTrue(results[1], "Thread 2 should have caught SecurityException");
    }
    
    // Test comprehensive dangerous class coverage
    
    @Test
    public void testSecurity_comprehensiveDangerousClassBlocking() {
        // Test various dangerous classes are blocked
        String[] dangerousClasses = {
            "java.lang.Runtime",
            "java.lang.ProcessBuilder", 
            "java.lang.System",
            "java.security.Provider",
            "javax.script.ScriptEngine",
            "java.lang.Class"
        };
        
        for (String className : dangerousClasses) {
            try {
                Class<?> dangerousClass = Class.forName(className);
                Exception exception = assertThrows(SecurityException.class, () -> {
                    ArrayUtilities.nullToEmpty(dangerousClass, null);
                }, "Should block " + className);
                
                assertTrue(exception.getMessage().contains("Array creation denied"),
                          "Should block " + className + " with appropriate message");
            } catch (ClassNotFoundException e) {
                // Class not available in this JVM, skip
                assertTrue(true, className + " not available, test skipped");
            }
        }
    }
    
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
}