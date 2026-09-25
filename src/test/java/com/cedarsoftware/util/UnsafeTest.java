package com.cedarsoftware.util;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UnsafeTest {
    private static Unsafe unsafeInstance = null;
    
    @BeforeAll
    static void checkUnsafeAvailability() {
        try {
            // Try to create an Unsafe instance to see if it's available
            unsafeInstance = new Unsafe();
        } catch (Exception e) {
            // Unsafe is not available on this JDK (likely due to JPMS restrictions)
            unsafeInstance = null;
        }
    }
    
    static class Example {
        static boolean ctorCalled = false;
        int value = 5;

        Example() {
            ctorCalled = true;
            value = 10;
        }
    }

    @Test
    void allocateInstanceBypassesConstructor() throws InvocationTargetException {
        assumeTrue(unsafeInstance != null, "Unsafe is not available on this JDK");
        Example.ctorCalled = false;

        Object obj = unsafeInstance.allocateInstance(Example.class);
        assertNotNull(obj);
        assertTrue(obj instanceof Example);
        Example ex = (Example) obj;
        assertFalse(Example.ctorCalled, "constructor should not run");
        assertEquals(0, ex.value, "field initialization should be skipped");
    }

    @Test
    void allocateInstanceRejectsInterface() throws InvocationTargetException {
        assumeTrue(unsafeInstance != null, "Unsafe is not available on this JDK");
        assertThrows(IllegalArgumentException.class, () -> unsafeInstance.allocateInstance(Runnable.class));
    }

    @Test
    void allocateInstanceRejectsNull() throws InvocationTargetException {
        assumeTrue(unsafeInstance != null, "Unsafe is not available on this JDK");
        assertThrows(IllegalArgumentException.class, () -> unsafeInstance.allocateInstance(null));
    }

    /**
     * Strategy 2 (sun.misc.Unsafe) is the fallback whenever ReflectionFactory cannot build a class. It
     * was loaded through ClassUtilities.forName, whose block list refuses sun.misc.Unsafe, so it was
     * never available. Force strategy 1 off and require strategy 2 to do the work.
     */
    @Test
    void sunMiscUnsafeFallbackIsAvailable() throws Exception {
        assumeTrue(unsafeInstance != null, "Unsafe is not available on this JDK");
        boolean present;
        try {
            Class.forName("sun.misc.Unsafe");
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        assumeTrue(present, "sun.misc.Unsafe exists on this JDK");

        Unsafe onlyFallback = new Unsafe();
        java.lang.reflect.Field rf = Unsafe.class.getDeclaredField("reflectionFactory");
        rf.setAccessible(true);
        rf.set(onlyFallback, null);

        Example.ctorCalled = false;
        Object obj = onlyFallback.allocateInstance(Example.class);
        assertTrue(obj instanceof Example);
        assertFalse(Example.ctorCalled, "constructor should not run");
        assertEquals(0, ((Example) obj).value);
    }
}