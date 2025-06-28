package com.cedarsoftware.util;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsafeTest {
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
        Unsafe unsafe = new Unsafe();
        Example.ctorCalled = false;

        Object obj = unsafe.allocateInstance(Example.class);
        assertNotNull(obj);
        assertTrue(obj instanceof Example);
        Example ex = (Example) obj;
        assertFalse(Example.ctorCalled, "constructor should not run");
        assertEquals(0, ex.value, "field initialization should be skipped");
    }

    @Test
    void allocateInstanceRejectsInterface() throws InvocationTargetException {
        Unsafe unsafe = new Unsafe();
        assertThrows(IllegalArgumentException.class, () -> unsafe.allocateInstance(Runnable.class));
    }

    @Test
    void allocateInstanceRejectsNull() throws InvocationTargetException {
        Unsafe unsafe = new Unsafe();
        assertThrows(IllegalArgumentException.class, () -> unsafe.allocateInstance(null));
    }
}