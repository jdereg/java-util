package com.cedarsoftware.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * trySetAccessible() called setAccessible(true) on every call, even on an object that was already
 * accessible. That WRITES the object's override flag, and the Constructor/Field/Method objects involved
 * are shared by every thread (ReflectionUtils caches them), so under concurrent construction each call
 * invalidated the cache line on every other core: 8 threads ran >10x slower per operation than one.
 * An object that is already accessible is now left alone.
 */
@SuppressWarnings("deprecation")
class ClassUtilitiesTrySetAccessibleTest {

    /** Counts the writes trySetAccessible() makes. */
    static final class CountingAccessible extends AccessibleObject {
        int setCalls;

        @Override
        public void setAccessible(boolean flag) {
            setCalls++;
            super.setAccessible(flag);
        }
    }

    static class Target {
        private final String secret;

        private Target(String secret) {
            this.secret = secret;
        }
    }

    @Test
    void anAlreadyAccessibleObjectIsNotWrittenAgain() {
        CountingAccessible obj = new CountingAccessible();
        ClassUtilities.trySetAccessible(obj);
        assertEquals(1, obj.setCalls);
        assertTrue(obj.isAccessible());

        for (int i = 0; i < 10; i++) {
            ClassUtilities.trySetAccessible(obj);
        }
        assertEquals(1, obj.setCalls, "setAccessible(true) must not be repeated on an accessible object");
    }

    @Test
    void privateMembersStillBecomeAccessible() throws Exception {
        Constructor<Target> ctor = Target.class.getDeclaredConstructor(String.class);
        ClassUtilities.trySetAccessible(ctor);
        assertTrue(ctor.isAccessible());
        Target t = ctor.newInstance("x");

        Field f = Target.class.getDeclaredField("secret");
        ClassUtilities.trySetAccessible(f);
        assertTrue(f.isAccessible());
        assertEquals("x", f.get(t));
    }

    /** The check is per instance: another Field object for the same field still gets set. */
    @Test
    void eachFieldInstanceIsMadeAccessible() throws Exception {
        Field first = Target.class.getDeclaredField("secret");
        Field second = Target.class.getDeclaredField("secret");
        assertNotSame(first, second);

        ClassUtilities.trySetAccessible(first);
        ClassUtilities.trySetAccessible(second);
        assertTrue(first.isAccessible());
        assertTrue(second.isAccessible());
        assertEquals("y", second.get(new Target("y")));
    }
}
