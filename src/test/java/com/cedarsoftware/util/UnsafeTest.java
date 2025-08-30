package com.cedarsoftware.util;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

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
}