package com.cedarsoftware.util;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * getClassLoader() looks for OSGi only once when it is absent. With no context class loader and an anchor class from
 * the bootstrap loader, every call tried to load org.osgi.framework.FrameworkUtil and failed with a
 * ClassNotFoundException; the answer (the system class loader) is unchanged.
 */
class ClassUtilitiesOsgiAbsentTest {

    @Test
    void anAbsentOsgiIsLookedForOnce() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader context = thread.getContextClassLoader();
        thread.setContextClassLoader(null);
        try {
            for (int call = 1; call <= 3; call++) {
                assertSame(ClassLoader.getSystemClassLoader(), ClassUtilities.getClassLoader(String.class));
            }
        } finally {
            thread.setContextClassLoader(context);
        }
        Field present = ClassUtilities.class.getDeclaredField("osgiPresent");
        present.setAccessible(true);
        assertFalse((Boolean) present.get(null), "FrameworkUtil is not on this class path: remembered as absent");
    }
}
