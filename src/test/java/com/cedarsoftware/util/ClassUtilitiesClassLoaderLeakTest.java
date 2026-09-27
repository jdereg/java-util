package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class that ClassUtilities has seen must not keep its ClassLoader alive once the application lets go of both.
 * Five of its per-class caches were ClassValueMaps, whose backing map holds every class ever put in it: a class
 * passed to forName(), newInstance() or a security check -- and its ClassLoader, and every class that loader loaded
 * -- could never be unloaded, without an explicit clearCaches(). A redeployed webapp, a plugin, a recompiled script:
 * each discarded loader stayed in memory. The caches are ClassValues now, whose entries live inside the class.
 * <p>
 * No clearCaches() here. ReflectionUtils' constructor caches are swapped out after each operation: they are bounded
 * LRU caches that hold a class only until newer entries evict it, and are not what this test is about.
 */
class ClassUtilitiesClassLoaderLeakTest {

    private static final String FIXTURE = "com.cedarsoftware.util.ClassUtilitiesClassLoaderLeakTest$LoaderFixture";
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private Map<Object, Constructor<?>[]> originalSorted;
    private Map<Object, Constructor<?>> originalConstructors;

    @SuppressWarnings("unchecked")
    private static <T> T reflectionUtilsCache(String name) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) ((AtomicReference<?>) f.get(null)).get();
    }

    @BeforeEach
    void saveReflectionUtilsCaches() throws Exception {
        originalSorted = reflectionUtilsCache("SORTED_CONSTRUCTORS_CACHE");
        originalConstructors = reflectionUtilsCache("CONSTRUCTOR_CACHE");
    }

    @AfterEach
    void restoreReflectionUtilsCaches() {
        ReflectionUtils.setSortedConstructorsCache(originalSorted);
        ReflectionUtils.setConstructorCache(originalConstructors);
    }

    /** Loads the fixture in a throwaway loader, runs {@code use} on it, drops both, and reports whether the loader went. */
    private void assertLoaderIsCollectedAfter(String what, Consumer<Class<?>> use) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        assertNotSame(LoaderFixture.class, fixture, "the fixture must be defined by the throwaway loader");

        // The bounded LRU caches in ReflectionUtils are not under test (see the class comment): the operation gets
        // throwaway ones, dropped afterwards
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());
        use.accept(fixture);
        ReflectionUtils.setSortedConstructorsCache(new ConcurrentHashMap<>());
        ReflectionUtils.setConstructorCache(new ConcurrentHashMap<>());

        WeakReference<ClassLoader> loaderRef = new WeakReference<>(loader);
        fixture = null;
        loader.close();
        loader = null;
        assertTrue(gcUntilCleared(loaderRef), "after " + what + ", something still holds the class loader");
    }

    @Test
    void forNameDoesNotPinTheLoader() throws Exception {
        assertLoaderIsCollectedAfter("forName()", c -> assertNotNull(ClassUtilities.forName(FIXTURE, c.getClassLoader())));
    }

    @Test
    void theHierarchyCacheDoesNotPinTheLoader() throws Exception {
        assertLoaderIsCollectedAfter("getClassHierarchyInfo()", c -> assertNotNull(ClassUtilities.getClassHierarchyInfo(c)));
    }

    @Test
    void newInstanceWithoutArgumentsDoesNotPinTheLoader() throws Exception {
        assertLoaderIsCollectedAfter("newInstance() with no arguments",
                c -> assertNotNull(ClassUtilities.newInstance(converter, c, null)));
    }

    @Test
    void newInstanceWithPositionalArgumentsDoesNotPinTheLoader() throws Exception {
        assertLoaderIsCollectedAfter("newInstance() with a List",
                c -> assertNotNull(ClassUtilities.newInstance(converter, c, Collections.singletonList("a name"))));
    }

    @Test
    void newInstanceWithNamedArgumentsDoesNotPinTheLoader() throws Exception {
        assertLoaderIsCollectedAfter("newInstance() with a Map", c -> {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("name", "a name");
            assertNotNull(ClassUtilities.newInstance(converter, c, args));
        });
    }

    private static boolean gcUntilCleared(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            // Allocation pressure encourages a full collection, class unloading included
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        return ref.get() == null;
    }

    /** Loaded in a throwaway ClassLoader whose parent is the bootstrap loader: may depend only on java.lang. */
    public static class LoaderFixture {
        public final String name;

        public LoaderFixture() {
            this.name = null;
        }

        public LoaderFixture(String name) {
            this.name = name;
        }
    }
}
