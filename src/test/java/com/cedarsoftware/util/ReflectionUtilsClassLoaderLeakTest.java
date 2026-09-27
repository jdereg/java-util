package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A class that ReflectionUtils has been asked about must not keep its ClassLoader alive once the application lets go
 * of both. Its caches were bounded LRUs that held the class -- in their keys, and in the Field, Method and Constructor
 * values -- until newer entries pushed it out, so a discarded loader (a redeployed webapp, a plugin, a recompiled
 * script) and every class it loaded stayed in memory meanwhile. What ReflectionUtils finds is now kept on the class
 * itself (a ClassValue), and goes away with it.
 */
class ReflectionUtilsClassLoaderLeakTest {

    private static final String FIXTURE = "com.cedarsoftware.util.ReflectionUtilsClassLoaderLeakTest$LoaderFixture";
    private static final Predicate<Field> NON_STATIC = f -> !Modifier.isStatic(f.getModifiers());

    /** Loads the fixture in a throwaway loader, runs {@code use} on it, drops both, and returns the loader weakly. */
    private static WeakReference<ClassLoader> useInAThrowawayLoader(Consumer<Class<?>> use) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        assertNotSame(LoaderFixture.class, fixture, "the fixture must be defined by the throwaway loader");
        use.accept(fixture);
        loader.close();
        return new WeakReference<>(loader);
    }

    /**
     * Each group of lookups runs on the fixture in its own loader; one collection then serves them all (a forced full
     * collection late in a JDK 8 test run takes about a second).
     */
    @Test
    void noLookupPinsTheLoader() throws Exception {
        Map<String, WeakReference<ClassLoader>> loaders = new LinkedHashMap<>();
        loaders.put("the field lookups", useInAThrowawayLoader(c -> {
            assertNotNull(ReflectionUtils.getField(c, "name"));
            assertNull(ReflectionUtils.getField(c, "noSuchField"));
            assertEquals(1, ReflectionUtils.getDeclaredFields(c).size());
            assertEquals(1, ReflectionUtils.getAllDeclaredFields(c).size());
            assertEquals(1, ReflectionUtils.getAllDeclaredFieldsMap(c).size());
            assertEquals(1, ReflectionUtils.getAllDeclaredFields(c, NON_STATIC).size());
            assertEquals(1, ReflectionUtils.getDeclaredFields(c, NON_STATIC).size());
        }));
        loaders.put("the method lookups", useInAThrowawayLoader(c -> {
            assertNotNull(ReflectionUtils.getMethod(c, "greet", String.class));
            assertNull(ReflectionUtils.getMethod(c, "noSuchMethod"));
            assertNotNull(ReflectionUtils.getMethod(c, "greet", 1));
            assertNotNull(ReflectionUtils.getNonOverloadedMethod(c, "describe"));
        }));
        loaders.put("the constructor lookups", useInAThrowawayLoader(c -> {
            assertNotNull(ReflectionUtils.getConstructor(c));
            assertNotNull(ReflectionUtils.getConstructor(c, String.class));
            assertNull(ReflectionUtils.getConstructor(c, Integer.class));
            assertEquals(2, ReflectionUtils.getAllConstructors(c).length);
        }));
        loaders.put("the annotation lookups", useInAThrowawayLoader(c -> {
            assertNotNull(ReflectionUtils.getClassAnnotation(c, Deprecated.class));
            assertNull(ReflectionUtils.getClassAnnotation(c, FunctionalInterface.class));
            Method greet = ReflectionUtils.getMethod(c, "greet", String.class);
            assertNotNull(ReflectionUtils.getMethodAnnotation(greet, Deprecated.class));
            assertNull(ReflectionUtils.getMethodAnnotation(greet, FunctionalInterface.class));
        }));

        gcUntilCleared(loaders.values());
        for (Map.Entry<String, WeakReference<ClassLoader>> loader : loaders.entrySet()) {
            assertNull(loader.getValue().get(),
                    "after " + loader.getKey() + ", something still holds the class loader");
        }
    }

    private static void gcUntilCleared(Collection<? extends WeakReference<?>> refs) throws InterruptedException {
        for (int i = 0; i < 100 && refs.stream().anyMatch(ref -> ref.get() != null); i++) {
            System.gc();
            // Allocation pressure encourages a full collection, class unloading included
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
    }

    /** Loaded in a throwaway ClassLoader whose parent is the bootstrap loader: may depend only on java.lang. */
    @Deprecated
    public static class LoaderFixture {
        private static int count;
        public String name;

        public LoaderFixture() {
        }

        public LoaderFixture(String name) {
            this.name = name;
        }

        @Deprecated
        public String greet(String who) {
            count++;
            return who;
        }

        public String describe() {
            return name;
        }
    }
}
