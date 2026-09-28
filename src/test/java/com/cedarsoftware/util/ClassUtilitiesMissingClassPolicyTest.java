package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import groovy.lang.GroovyClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * How long forName() keeps a "not found" answer depends on the class loader: a class a loader defines after its name
 * was looked up is found. Every loader kept its misses for good, so such a class was never found (until
 * clearCaches()) -- n-cube looks classes up in its GroovyClassLoader and defines them there.
 * <ul>
 *     <li>The JDK's own loaders keep them: their classes never change.</li>
 *     <li>A Groovy class loader keeps none: it defines classes at any moment.</li>
 *     <li>Any other loader keeps them for classutilities.class.not.found.cache.millis (1 second by default), and
 *         forgetMissingClasses() drops them at once.</li>
 * </ul>
 * The Groovy loader here is a test-scope stand-in (groovy.lang.GroovyClassLoader); java-util does not depend on
 * Groovy.
 */
class ClassUtilitiesMissingClassPolicyTest {

    private static final String PROPERTY = "classutilities.class.not.found.cache.millis";

    /** Defined late, by the loaders below; its bytes come from the test class path. */
    public static class Late {
    }

    private static final String LATE = Late.class.getName();

    private static byte[] lateBytes() {
        String resource = LATE.replace('.', '/') + ".class";
        try (InputStream in = ClassUtilitiesMissingClassPolicyTest.class.getClassLoader().getResourceAsStream(resource)) {
            return IOUtilities.inputStreamToBytes(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Knows no class named LATE until told to define it, and counts the times it is asked for it. */
    static final class LateLoader extends ClassLoader {
        final AtomicInteger asked = new AtomicInteger();
        private volatile Class<?> late;

        LateLoader() {
            super(ClassUtilitiesMissingClassPolicyTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (LATE.equals(name)) {
                asked.incrementAndGet();
                if (late == null) {
                    throw new ClassNotFoundException(name);
                }
                return late;
            }
            return super.loadClass(name, resolve);
        }

        Class<?> defineLate() {
            byte[] bytes = lateBytes();
            late = defineClass(LATE, bytes, 0, bytes.length);
            return late;
        }
    }

    /** The same, as a Groovy class loader: a subclass of it, as n-cube's are. */
    static final class LateGroovyLoader extends GroovyClassLoader {
        final AtomicInteger asked = new AtomicInteger();
        private volatile Class<?> late;

        LateGroovyLoader() {
            super(ClassUtilitiesMissingClassPolicyTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (LATE.equals(name)) {
                asked.incrementAndGet();
                if (late == null) {
                    throw new ClassNotFoundException(name);
                }
                return late;
            }
            return super.loadClass(name, resolve);
        }

        Class<?> defineLate() {
            late = defineClass(LATE, lateBytes());
            return late;
        }
    }

    @BeforeEach
    @AfterEach
    void reset() {
        System.clearProperty(PROPERTY);
        ClassUtilities.clearCaches();
    }

    @Test
    void aGroovyLoaderKeepsNoMiss() {
        LateGroovyLoader loader = new LateGroovyLoader();
        assertNull(ClassUtilities.forName(LATE, loader));
        assertNull(ClassUtilities.forName(LATE, loader));
        assertEquals(2, loader.asked.get(), "asked each time: its misses are not kept");

        Class<?> defined = loader.defineLate();
        assertSame(defined, ClassUtilities.forName(LATE, loader), "found as soon as it is defined");
    }

    @Test
    void anotherLoadersMissLastsItsLifetime() throws Exception {
        System.setProperty(PROPERTY, "200");            // read when the loader's cache is made
        LateLoader loader = new LateLoader();
        assertNull(ClassUtilities.forName(LATE, loader));
        assertNull(ClassUtilities.forName(LATE, loader));
        assertEquals(1, loader.asked.get(), "the second answer came from the cache");

        Class<?> defined = loader.defineLate();
        TimeUnit.MILLISECONDS.sleep(400);
        assertSame(defined, ClassUtilities.forName(LATE, loader), "found once the miss has expired");
        assertEquals(2, loader.asked.get());
    }

    @Test
    void forgetMissingClassesDropsALoadersMissesAtOnce() {
        LateLoader loader = new LateLoader();              // the default lifetime: 1 second
        assertNull(ClassUtilities.forName(LATE, loader));
        Class<?> defined = loader.defineLate();
        assertNull(ClassUtilities.forName(LATE, loader), "still the cached miss");

        ClassUtilities.forgetMissingClasses(loader);
        assertSame(defined, ClassUtilities.forName(LATE, loader));
        assertSame(defined, ClassUtilities.forName(LATE, loader));
        assertEquals(2, loader.asked.get(), "once for the miss, once after it was forgotten; then cached as found");
    }

    @Test
    void aLifetimeOfZeroKeepsNoMiss() {
        System.setProperty(PROPERTY, "0");
        LateLoader loader = new LateLoader();
        assertNull(ClassUtilities.forName(LATE, loader));
        assertNull(ClassUtilities.forName(LATE, loader));
        assertEquals(2, loader.asked.get());
    }

    /** Which policy each loader's cache was given, read from the cache itself. */
    @Test
    void theJdksOwnLoadersKeepTheirMisses() throws Exception {
        ClassLoader system = ClassLoader.getSystemClassLoader();
        assertEquals(Long.MAX_VALUE, lifetimeOf(system), "the system class loader");
        ClassLoader parent = system.getParent();
        if (parent != null) {
            assertEquals(Long.MAX_VALUE, lifetimeOf(parent), "the platform (extension) class loader");
        }
        try (URLClassLoader mine = new URLClassLoader(new URL[0], system)) {
            assertEquals(TimeUnit.SECONDS.toNanos(1), lifetimeOf(mine), "a loader the application made");
        }
        assertEquals(0L, lifetimeOf(new LateGroovyLoader()), "a Groovy class loader");
    }

    @SuppressWarnings("unchecked")
    private static long lifetimeOf(ClassLoader loader) throws Exception {
        assertNull(ClassUtilities.forName("com.cedarsoftware.nowhere.Missing", loader));    // makes its cache
        Field nameCache = ClassUtilities.class.getDeclaredField("NAME_CACHE");
        nameCache.setAccessible(true);
        Map<ClassLoader, Object> byLoader = (Map<ClassLoader, Object>) nameCache.get(null);
        Object holder;
        synchronized (byLoader) {
            holder = byLoader.get(loader);
        }
        assertNotNull(holder);
        Field lifetime = holder.getClass().getDeclaredField("missLifetimeNanos");
        lifetime.setAccessible(true);
        return lifetime.getLong(holder);
    }
}
