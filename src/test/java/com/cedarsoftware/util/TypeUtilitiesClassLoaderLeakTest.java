package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private static final String PLUGIN_TYPE = "com.cedarsoftware.util.TypeUtilitiesClassLoaderLeakTest$PluginListType";

    /** A plugin's own ParameterizedType, List<String>: its arguments are JDK classes, the object is the plugin's. */
    public static class PluginListType implements ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    /** Loaded in throwaway ClassLoaders whose parent is the bootstrap loader: may depend only on java.base. */
    public static class Fixture {
        public List<String> names;
        public Map<String, List<Fixture>> byName;
        public List<Fixture> selves;
    }

    private static URLClassLoader throwawayLoader() throws Exception {
        return new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
    }

    /**
     * Both cases, sharing one forced collection (one costs about a second late in a JDK 8 run): a class's own generic
     * types, and a type mixing classes of two unrelated loaders -- which has no class to be kept on, so it is resolved
     * and kept nowhere.
     */
    @Test
    void resolvingTypesDoesNotKeepTheirLoadersAlive() throws Exception {
        WeakReference<ClassLoader> loader = resolveFixtureTypes();
        WeakReference<?>[] unrelated = resolveAcrossTwoLoaders();
        WeakReference<ClassLoader> javaUtil = resolveJdkTypesWithJavaUtilInAThrowawayLoader();
        WeakReference<ClassLoader> plugin = resolveAPluginsOwnTypeObject();
        gcUntilCleared(loader, unrelated[0], unrelated[1], javaUtil, plugin);
        assertNull(loader.get(), "resolveType() kept the class loader alive");
        assertNull(unrelated[0].get(), "a type of unrelated loaders kept the first loader alive");
        assertNull(unrelated[1].get(), "a type of unrelated loaders kept the second loader alive");
        assertNull(javaUtil.get(), "java-util's own Type object, kept on a JDK class, kept java-util's loader");
        assertNull(plugin.get(), "a plugin's own ParameterizedType, kept on java.util.List, kept the plugin's loader");
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

    /**
     * java-util itself in a throwaway loader (a webapp's WEB-INF/lib, a plugin), resolving a type made only of JDK
     * classes: AbstractMap<K,V> against HashMap. The answer is one of TypeUtilities' own Type implementations; kept
     * in a slot of a JDK class, it held java-util's loader for good.
     */
    private static WeakReference<ClassLoader> resolveJdkTypesWithJavaUtilInAThrowawayLoader() throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/classes").toURI().toURL()}, null);
        Class<?> typeUtilities = Class.forName(TypeUtilities.class.getName(), true, loader);
        assertNotSame(TypeUtilities.class, typeUtilities);
        Method resolveType = typeUtilities.getMethod("resolveType", Type.class, Type.class);
        Type abstractMap = HashMap.class.getGenericSuperclass();
        for (int i = 0; i < 2; i++) {
            Object resolved = resolveType.invoke(null, HashMap.class, abstractMap);
            assertSame(loader, resolved.getClass().getClassLoader(), "an object of java-util's own Type class");
        }
        loader.close();
        return new WeakReference<>(loader);
    }

    /** A plugin's own ParameterizedType object whose arguments are JDK classes, resolved against itself and List. */
    private static WeakReference<ClassLoader> resolveAPluginsOwnTypeObject() throws Exception {
        URLClassLoader loader = throwawayLoader();
        Type pluginType = (Type) Class.forName(PLUGIN_TYPE, true, loader).getConstructor().newInstance();
        assertNotSame(PluginListType.class, pluginType.getClass());
        String listOfString = "java.util.List<java.lang.String>";
        for (int i = 0; i < 2; i++) {
            assertEquals(listOfString, TypeUtilities.resolveType(pluginType, pluginType).getTypeName());
            assertEquals(listOfString, TypeUtilities.resolveType(List.class, pluginType).getTypeName());
        }
        loader.close();
        return new WeakReference<>(loader);
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

    private static void gcUntilCleared(WeakReference<?>... refs) throws InterruptedException {
        for (int i = 0; i < 100 && java.util.Arrays.stream(refs).anyMatch(r -> r.get() != null); i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
    }
}
