package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolving a generic type must not keep the classes it mentions -- or their ClassLoaders -- alive. resolveType()
 * cached its answers in a static, bounded LRU cache whose keys held the types, so a class with a generic field (a
 * redeployed webapp's, a plugin's, a recompiled script's) stayed in memory, with its loader and every class that
 * loader loaded, until newer entries pushed it out. Answers are now kept on a class the types mention -- the one
 * whose loader has all the others' as ancestors, so an answer never outlives what it refers to -- and types mixing
 * classes of unrelated loaders are resolved without being kept at all.
 */
class TypeUtilitiesClassLoaderLeakTest {

    private static final String FIXTURE = "com.cedarsoftware.util.TypeUtilitiesClassLoaderLeakTest$Fixture";

    /** Loaded in throwaway ClassLoaders whose parent is the bootstrap loader: may depend only on java.base. */
    public static class Fixture {
        public List<String> names;
        public Map<String, List<Fixture>> byName;
        public List<Fixture> selves;
    }

    private static URLClassLoader throwawayLoader() throws Exception {
        return new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
    }

    @Test
    void resolvingAClassesGenericTypesDoesNotKeepItsLoaderAlive() throws Exception {
        WeakReference<ClassLoader> loader = resolveFixtureTypes();
        gcUntilCleared(loader);
        assertNull(loader.get(), "resolveType() kept the class loader alive");
    }

    private static WeakReference<ClassLoader> resolveFixtureTypes() throws Exception {
        URLClassLoader loader = throwawayLoader();
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        assertNotSame(Fixture.class, fixture);
        for (String field : new String[]{"names", "byName", "selves"}) {
            Type declared = fixture.getField(field).getGenericType();
            assertEquals(declared, TypeUtilities.resolveType(fixture, declared));
            assertEquals(declared, TypeUtilities.resolveType(fixture, declared), "asked again");
        }
        // List<Fixture>, resolved against itself: it mentions a JDK class and the fixture, and must be kept with
        // the fixture, not with java.util.List (which lives as long as the JVM)
        Type selves = fixture.getField("selves").getGenericType();
        assertEquals(selves, TypeUtilities.resolveType(selves, selves));
        loader.close();
        return new WeakReference<>(loader);
    }

    /** A type mixing classes of two unrelated loaders has no class to be kept on: it is resolved, and kept nowhere. */
    @Test
    void typesOfUnrelatedLoadersAreResolvedAndNotKept() throws Exception {
        WeakReference<?>[] loaders = resolveAcrossTwoLoaders();
        gcUntilCleared(loaders[0]);
        gcUntilCleared(loaders[1]);
        assertNull(loaders[0].get(), "the first loader was kept alive");
        assertNull(loaders[1].get(), "the second loader was kept alive");
    }

    private static WeakReference<?>[] resolveAcrossTwoLoaders() throws Exception {
        URLClassLoader first = throwawayLoader();
        URLClassLoader second = throwawayLoader();
        Class<?> a = Class.forName(FIXTURE, false, first);
        Class<?> b = Class.forName(FIXTURE, false, second);
        assertNotSame(a, b);
        ParameterizedType mixed = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{a, b};
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        for (int i = 0; i < 2; i++) {
            Type resolved = TypeUtilities.resolveType(mixed, mixed);
            assertTrue(resolved instanceof ParameterizedType, String.valueOf(resolved));
            assertEquals(Arrays.asList(a, b), Arrays.asList(((ParameterizedType) resolved).getActualTypeArguments()));
        }
        first.close();
        second.close();
        return new WeakReference<?>[]{new WeakReference<>(first), new WeakReference<>(second)};
    }

    private static void gcUntilCleared(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
    }
}
