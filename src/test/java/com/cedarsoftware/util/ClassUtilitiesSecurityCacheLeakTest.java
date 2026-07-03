package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that classes passing through the SecurityChecker verdict cache do not leak.
 * <p>
 * The security verdict is memoized in a {@code ClassValue} (SECURITY_CHECK_CACHE), whose
 * entries live inside the {@code Class} object itself and therefore never pin the class or
 * its ClassLoader. Historically, {@code computeValue} ALSO recorded every checked class in
 * two static strong-reference ClassValueSets (INHERITS_FROM_BLOCKED / VERIFIED_SAFE_CLASSES).
 * Those sets could never produce a cache hit the outer ClassValue wouldn't (the verdict is
 * computed at most once per Class lifetime, and a re-loaded class is a different identity),
 * but they pinned every verified class — and its ClassLoader — for the JVM's lifetime, with
 * no release path: even {@link ClassUtilities#clearCaches()} did not clear them.
 * <p>
 * This test loads a fixture class in a disposable ClassLoader, runs it through the security
 * check, clears the (clearable) static caches, and asserts the loader becomes GC-reclaimable.
 */
class ClassUtilitiesSecurityCacheLeakTest {

    @Test
    void testVerifiedClassDoesNotPinItsClassLoaderAfterClearCaches() throws Exception {
        URLClassLoader loader = newTestClassesLoader();
        Class<?> fixture = Class.forName(
                "com.cedarsoftware.util.ClassUtilitiesSecurityCacheLeakTest$LeakFixture", false, loader);
        assertNotSame(LeakFixture.class, fixture, "Fixture must be defined by the disposable loader");
        assertNotSame(getClass().getClassLoader(), fixture.getClassLoader());

        // Route the foreign class through the security verdict path (safe → cached verdict FALSE).
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(fixture));

        WeakReference<ClassLoader> loaderRef = new WeakReference<>(loader);
        WeakReference<Class<?>> classRef = new WeakReference<>(fixture);
        fixture = null;
        loader.close();
        loader = null;

        // Release the per-user-class static caches that legitimately hold strong Class refs
        // (CLASS_HIERARCHY_CACHE etc.). After this, nothing in java-util may pin the fixture.
        ClassUtilities.clearCaches();

        assertTrue(gcUntilCleared(loaderRef), "Disposable ClassLoader should be GC-reclaimable after "
                + "clearCaches(); a surviving reference means a static cache still pins verified classes");
        assertNull(classRef.get(), "Fixture Class should be unloadable once its loader is reclaimable");
    }

    @Test
    void testSecurityVerdictsRemainCorrectAndStable() {
        // Safe class: repeated calls exercise the cached-verdict path.
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class));

        // Directly blocked class.
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(Runtime.class));

        // Blocked via inheritance: any ClassLoader subclass must stay blocked (hierarchy walk).
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(URLClassLoader.class));
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(URLClassLoader.class));
    }

    private static URLClassLoader newTestClassesLoader() throws Exception {
        URL testClasses = new File("target/test-classes").toURI().toURL();
        // parent = null (bootstrap): forces the child to define the fixture class itself,
        // giving it a distinct Class identity from the surefire-loaded copy.
        return new URLClassLoader(new URL[]{testClasses}, null);
    }

    private static boolean gcUntilCleared(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            // Allocation pressure encourages a full collection incl. class unloading.
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        return ref.get() == null;
    }

    /** Loaded reflectively in a disposable ClassLoader; must depend only on java.lang. */
    public static class LeakFixture {
    }
}
