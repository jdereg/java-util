package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A member that cannot be made accessible does not keep its class loader alive. trySetAccessible() remembers such
 * members so it does not fail on them again; it kept them in one static map, on the reasoning that only JDK internals
 * refuse. A named application module that does not open its package refuses too (and a SecurityManager), and its
 * loader -- a module layer's, a plugin's -- then stayed in memory for good. Here the refusing member's class is
 * defined by a throwaway loader.
 */
class ClassUtilitiesInaccessibleMemberLeakTest {

    /** A member whose setAccessible(true) is refused, as a sealed module's is. Defined by a throwaway loader. */
    @SuppressWarnings("deprecation")
    public static class RefusingMember extends AccessibleObject implements Member {
        public RefusingMember() {
        }

        @Override
        public void setAccessible(boolean flag) {
            throw new IllegalStateException("module does not open this package");
        }

        @Override
        public Class<?> getDeclaringClass() {
            return RefusingMember.class;
        }

        @Override
        public String getName() {
            return "refusing";
        }

        @Override
        public int getModifiers() {
            return Modifier.PRIVATE;
        }

        @Override
        public boolean isSynthetic() {
            return false;
        }
    }

    /** Defines RefusingMember itself; everything else comes from the parent. */
    private static final class ThrowawayLoader extends ClassLoader {
        private static final String NAME = RefusingMember.class.getName();

        ThrowawayLoader() {
            super(ClassUtilitiesInaccessibleMemberLeakTest.class.getClassLoader());
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!NAME.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                String resource = name.replace('.', '/') + ".class";
                try (InputStream in = getParent().getResourceAsStream(resource)) {
                    byte[] bytes = IOUtilities.inputStreamToBytes(in);
                    c = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            return c;
        }
    }

    @Test
    void aRefusedMemberDoesNotKeepItsLoaderAlive() throws Exception {
        WeakReference<ClassLoader> loader = refuseOnce();
        for (int i = 0; i < 50 && loader.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(loader.get(), "the throwaway loader was kept alive by the inaccessible-member cache");
    }

    /** Loads the member through a throwaway loader, has it refused (and remembered), and lets go of both. */
    private static WeakReference<ClassLoader> refuseOnce() throws Exception {
        ThrowawayLoader loader = new ThrowawayLoader();
        Class<?> c = loader.loadClass(RefusingMember.class.getName());
        if (c.getClassLoader() != loader) {
            throw new IllegalStateException("fixture not defined by the throwaway loader");
        }
        AccessibleObject member = (AccessibleObject) c.getConstructor().newInstance();
        ClassUtilities.trySetAccessible(member);       // refused, and remembered
        ClassUtilities.trySetAccessible(member);       // answered from what was remembered
        return new WeakReference<>(loader);
    }

    @Test
    void aRefusalIsRememberedAndASecurityRefusalStillThrows() {
        CountingMember member = new CountingMember();
        ClassUtilities.trySetAccessible(member);
        ClassUtilities.trySetAccessible(member);
        ClassUtilities.trySetAccessible(member);
        if (member.attempts != 1) {
            throw new AssertionError("setAccessible tried " + member.attempts + " times, expected once");
        }
        assertThrows(SecurityException.class, () -> ClassUtilities.trySetAccessible(new SecurityRefusingMember()));
    }

    @SuppressWarnings("deprecation")
    static class CountingMember extends RefusingMember {
        int attempts;

        @Override
        public void setAccessible(boolean flag) {
            attempts++;
            super.setAccessible(flag);
        }

        @Override
        public Class<?> getDeclaringClass() {
            return CountingMember.class;
        }
    }

    static class SecurityRefusingMember extends RefusingMember {
        @Override
        public void setAccessible(boolean flag) {
            throw new SecurityException("denied");
        }

        @Override
        public Class<?> getDeclaringClass() {
            return SecurityRefusingMember.class;
        }
    }
}
