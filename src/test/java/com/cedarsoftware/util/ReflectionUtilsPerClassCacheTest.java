package com.cedarsoftware.util;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ReflectionUtils keeps what it finds on each class (a ClassValue) instead of in bounded LRU caches:
 * <ul>
 *   <li>A hit writes nothing and allocates nothing (the LRUs allocated a key -- 40 to 168 bytes -- and wrote a
 *       timestamp or took a lock on every hit, so threads reading the same entry fought over it).</li>
 *   <li>A name the class does not have stores nothing, however many are asked for.</li>
 *   <li>A caller's field filter is held weakly: a class never keeps it, or its class loader, alive.</li>
 *   <li>Every value is a JDK type: a ClassValue's value lives inside its Class, and a java-util type stored in a JDK
 *       class would keep java-util's own class loader (a webapp's, say) alive for the life of the JVM.</li>
 *   <li>The lookups answer as they always have: the rules below are restated from the documented behavior.</li>
 * </ul>
 */
class ReflectionUtilsPerClassCacheTest {

    private static volatile Object sink;

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface Marker {
        String value();
    }

    public interface Named {
        String name();
    }

    @Marker("iface")
    public interface MarkedIface {
    }

    public interface Deep1 {
        default int deep() {
            return 1;
        }
    }

    public interface Deep2 extends Deep1, Named {
    }

    @Marker("base")
    public static class Base {
        protected int id;
        private String secret;
        public static int count;

        public Number size() {
            return 1;
        }

        protected Object copy() {
            return this;
        }
    }

    public static class Child extends Base implements Named, MarkedIface {
        private String secret;
        public Integer extra;

        @Override
        public Integer size() {             // covariant: javac adds a bridge size() returning Number
            return 2;
        }

        @Override
        public Child copy() {
            return this;
        }

        @Override
        public String name() {
            return "child";
        }
    }

    public static class GrandChild extends Child {
        public int id;
    }

    public static class OnlyMarkedByInterface implements MarkedIface {
    }

    public static class DeepImpl implements Deep2 {
        @Override
        public String name() {
            return "deep";
        }
    }

    /** Asked about only by aNameTheClassDoesNotHaveStoresNothing(). */
    public static class Probe {
        private int only;

        public int value() {
            return only;
        }
    }

    public static class Sample {
        public String alpha;
        private int beta;
        protected long gamma;
        String delta;
        public static int epsilon;
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    void theLookupsFollowTheirDocumentedRules() throws Exception {
        // getMethod: of a class's methods with the same parameters, the one with the most specific return type
        // (the covariant override, not its bridge) -- as Class.getDeclaredMethod() picks
        assertEquals(Integer.class, ReflectionUtils.getMethod(Child.class, "size").getReturnType());
        assertEquals(Child.class, ReflectionUtils.getMethod(Child.class, "copy").getReturnType());
        assertEquals(Child.class.getDeclaredMethod("size"), ReflectionUtils.getMethod(GrandChild.class, "size"));
        // ...a default method is found through the interfaces, breadth-first
        assertEquals(Deep1.class.getDeclaredMethod("deep"), ReflectionUtils.getMethod(DeepImpl.class, "deep"));
        assertNull(ReflectionUtils.getMethod(Child.class, "size", int.class));
        assertNull(ReflectionUtils.getMethod(Child.class, "noSuchMethod"));

        // getMethod by argument count: the non-bridge method declared in the most specific class
        assertEquals(Integer.class, ReflectionUtils.getMethod(new Child(), "size", 0).getReturnType());
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(new Child(), "size", 1));

        // getField: the first field of that name, the class's own before its superclasses'
        assertEquals(GrandChild.class, ReflectionUtils.getField(GrandChild.class, "id").getDeclaringClass());
        assertEquals(Base.class, ReflectionUtils.getField(Child.class, "id").getDeclaringClass());
        assertEquals(Child.class, ReflectionUtils.getField(GrandChild.class, "secret").getDeclaringClass());
        assertNull(ReflectionUtils.getField(Child.class, "count"), "static fields are not included");

        // getClassAnnotation: a class's own annotations (@Inherited ones from its superclass among them) come
        // before its interfaces'
        assertEquals("base", ReflectionUtils.getClassAnnotation(Child.class, Marker.class).value());
        assertEquals("iface", ReflectionUtils.getClassAnnotation(OnlyMarkedByInterface.class, Marker.class).value());
        assertNull(ReflectionUtils.getClassAnnotation(DeepImpl.class, Marker.class));

        // getAllConstructors and getConstructor hand out the same instances
        Object viaAll = ReflectionUtils.getAllConstructors(Sample.class)[0];
        assertSame(viaAll, ReflectionUtils.getConstructor(Sample.class));
    }

    @Test
    void theSameFilterGetsTheSameListAndManyFiltersAllAnswer() {
        List<Predicate<Field>> filters = new ArrayList<>();
        for (char first = 'a'; first <= 'z'; first++) {
            final char c = first;
            filters.add(f -> f.getName().charAt(0) == c);
        }
        for (int round = 0; round < 3; round++) {
            for (Predicate<Field> filter : filters) {
                List<String> expected = new ArrayList<>();
                for (Field f : Sample.class.getDeclaredFields()) {
                    if (filter.test(f)) {
                        expected.add(f.getName());
                    }
                }
                List<Field> got = ReflectionUtils.getDeclaredFields(Sample.class, filter);
                assertEquals(expected, names(got));
                assertSame(got, ReflectionUtils.getDeclaredFields(Sample.class, filter), "asked again at once");
                assertEquals(expected, names(ReflectionUtils.getAllDeclaredFields(Sample.class, filter)));
            }
        }
    }

    @Test
    void aCallersFilterIsHeldWeakly() throws InterruptedException {
        WeakReference<Object> shallow = askWithANewFilter(false);
        WeakReference<Object> deep = askWithANewFilter(true);
        for (int i = 0; i < 100 && (shallow.get() != null || deep.get() != null); i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        assertNull(shallow.get(), "getDeclaredFields() kept the caller's filter alive");
        assertNull(deep.get(), "getAllDeclaredFields() kept the caller's filter alive");
    }

    private static WeakReference<Object> askWithANewFilter(boolean deep) {
        Predicate<Field> filter = new Predicate<Field>() {
            private final byte[] state = new byte[16];

            @Override
            public boolean test(Field field) {
                return state.length > 0;
            }
        };
        List<Field> fields = deep ? ReflectionUtils.getAllDeclaredFields(Child.class, filter)
                : ReflectionUtils.getDeclaredFields(Child.class, filter);
        assertFalse(fields.isEmpty());
        return new WeakReference<>(filter);
    }

    @Test
    void aNameTheClassDoesNotHaveStoresNothing() throws Exception {
        for (int i = 0; i < 2_000; i++) {
            assertNull(ReflectionUtils.getField(Probe.class, "missing" + i));
            assertNull(ReflectionUtils.getMethod(Probe.class, "missing" + i));
            assertNull(ReflectionUtils.getMethod(Probe.class, "value", int.class));
            final int n = i;
            assertThrows(IllegalArgumentException.class,
                    () -> ReflectionUtils.getMethod(Probe.class, "missing" + n, 2));
        }
        assertEquals(1, perClass("FIELDS_BY_NAME", Probe.class, Map.class).size(), "only the real field");
        assertTrue(perClass("METHODS_FOUND", Probe.class, Map.class).isEmpty());
        assertTrue(perClass("METHODS_BY_ARG_COUNT", Probe.class, Map.class).isEmpty());

        ReflectionUtils.getMethod(Probe.class, "value");
        assertEquals(1, perClass("METHODS_FOUND", Probe.class, Map.class).size(), "what is found is kept");
    }

    @Test
    void aHitAllocatesNothing() {
        ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean, "needs per-thread allocation counters");
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) base;
        assumeTrue(mx.isThreadAllocatedMemorySupported() && mx.isThreadAllocatedMemoryEnabled());

        Class<?>[] noTypes = new Class<?>[0];
        Class<?>[] objectType = {Object.class};
        Predicate<Field> filter = f -> !Modifier.isStatic(f.getModifiers());
        Method name = ReflectionUtils.getMethod(Child.class, "name");
        Map<String, Supplier<Object>> hits = new LinkedHashMap<>();
        hits.put("getField", () -> ReflectionUtils.getField(Child.class, "extra"));
        hits.put("getMethod", () -> ReflectionUtils.getMethod(Child.class, "equals", objectType));
        hits.put("getMethod(argCount)", () -> ReflectionUtils.getMethod(Child.class, "name", 0));
        hits.put("getDeclaredFields", () -> ReflectionUtils.getDeclaredFields(Child.class));
        hits.put("getAllDeclaredFields", () -> ReflectionUtils.getAllDeclaredFields(Child.class));
        hits.put("getAllDeclaredFields(filter)", () -> ReflectionUtils.getAllDeclaredFields(Child.class, filter));
        hits.put("getConstructor", () -> ReflectionUtils.getConstructor(Child.class, noTypes));
        hits.put("getAllConstructors", () -> ReflectionUtils.getAllConstructors(Child.class));
        hits.put("getClassAnnotation", () -> ReflectionUtils.getClassAnnotation(Child.class, Marker.class));
        hits.put("getMethodAnnotation", () -> ReflectionUtils.getMethodAnnotation(name, Deprecated.class));

        long threadId = Thread.currentThread().getId();
        int calls = 20_000;
        for (Map.Entry<String, Supplier<Object>> hit : hits.entrySet()) {
            for (int i = 0; i < calls; i++) {
                sink = hit.getValue().get();
            }
            long before = mx.getThreadAllocatedBytes(threadId);
            for (int i = 0; i < calls; i++) {
                sink = hit.getValue().get();
            }
            double bytesPerCall = (mx.getThreadAllocatedBytes(threadId) - before) / (double) calls;
            // The LRU caches allocated 40-168 bytes per hit (a key object, often boxed hash parts and a copy of the
            // parameter types)
            assertTrue(bytesPerCall < 8, hit.getKey() + " allocated " + bytesPerCall + " bytes per hit");
        }
    }

    /**
     * Every per-class value, computed for JDK classes, holds only JDK types. A reference is not followed: what it
     * refers to (a caller's field filter) is held weakly and keeps nothing alive.
     */
    @Test
    void theCachedValuesHoldOnlyJdkTypes() throws Exception {
        Predicate<Field> callersFilter = f -> true;
        Class<?>[] jdkClasses = {String.class, java.util.ArrayList.class, HashMap.class, Thread.class, Object.class,
                int[].class, Runnable.class, java.util.concurrent.ConcurrentHashMap.class};
        for (Class<?> c : jdkClasses) {
            ReflectionUtils.getField(c, "value");
            ReflectionUtils.getAllDeclaredFields(c);
            ReflectionUtils.getAllDeclaredFields(c, callersFilter);
            ReflectionUtils.getAllDeclaredFieldsMap(c);
            ReflectionUtils.getMethod(c, "toString");
            ReflectionUtils.getMethod(c, "hashCode");
            ReflectionUtils.getAllConstructors(c);
            ReflectionUtils.getConstructor(c);
            ReflectionUtils.getClassAnnotation(c, Deprecated.class);
            ReflectionUtils.getClassAnnotation(c, FunctionalInterface.class);
            for (Method m : c.getDeclaredMethods()) {
                ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
            }
            try {
                ReflectionUtils.getMethod(c, "toString", 0);
                ReflectionUtils.getNonOverloadedMethod(c, "toString");
            } catch (IllegalArgumentException ignored) {
                // not every class has one; the lookup is what matters
            }
        }

        int caches = 0;
        for (Field f : ReflectionUtils.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != ClassValue.class) {
                continue;
            }
            caches++;
            f.setAccessible(true);
            ClassValue<?> cache = (ClassValue<?>) f.get(null);
            for (Class<?> c : jdkClasses) {
                assertOnlyJdkTypes(cache.get(c), f.getName() + "(" + c.getName() + ")",
                        Collections.newSetFromMap(new IdentityHashMap<>()));
            }
        }
        assertTrue(caches >= 10, "expected the per-class caches, found " + caches);
    }

    private static void assertOnlyJdkTypes(Object value, String path, Set<Object> seen) {
        if (value == null || !seen.add(value)) {
            return;
        }
        ClassLoader loader = value.getClass().getClassLoader();
        assertTrue(loader == null || loader == ClassLoader.getSystemClassLoader().getParent(),
                path + " holds a " + value.getClass().getName());
        if (value instanceof Reference) {
            return;
        }
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                assertOnlyJdkTypes(array[i], path + "[" + i + "]", seen);
            }
        } else if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                assertOnlyJdkTypes(element, path + "[]", seen);
            }
        } else if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                assertOnlyJdkTypes(entry.getKey(), path + ".key", seen);
                assertOnlyJdkTypes(entry.getValue(), path + "." + entry.getKey(), seen);
            }
        } else if (value instanceof AtomicReference) {
            assertOnlyJdkTypes(((AtomicReference<?>) value).get(), path + ".get()", seen);
        }
    }

    private static <T> T perClass(String cacheName, Class<?> c, Class<T> type) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(cacheName);
        f.setAccessible(true);
        return type.cast(((ClassValue<?>) f.get(null)).get(c));
    }

    private static List<String> names(List<Field> fields) {
        List<String> names = new ArrayList<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        return names;
    }
}
