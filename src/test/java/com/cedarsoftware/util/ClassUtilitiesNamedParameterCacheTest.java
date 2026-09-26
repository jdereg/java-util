package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * newInstance() with a Map of named values (json-io's path for every object it builds) looked up the class's
 * constructors, copied each one's Parameter[] and scanned their names for synthetic {@code argN} spellings on
 * EVERY call -- facts fixed for the class. The ReflectionUtils lookup also read a system property and wrote an
 * LRU timestamp per call, a shared-memory write under concurrency. Those facts are now computed once per class.
 * <p>
 * The first test pins that; the rest pin that caching them changed no outcome. Needs the test classes compiled
 * with {@code -parameters} (the pom's setting), or named matching never applies.
 */
class ClassUtilitiesNamedParameterCacheTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    static class Point {
        final int x;
        final int y;
        final String via;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
            this.via = "xy";
        }

        Point(int x) {
            this.x = x;
            this.y = -1;
            this.via = "x";
        }
    }

    /** One constructor has real names, the other a name that looks synthetic. */
    static class Mixed {
        final String label;
        final int size;
        final String via;

        Mixed(String label, int size) {
            this.label = label;
            this.size = size;
            this.via = "labelSize";
        }

        Mixed(String arg0) {
            this.label = arg0;
            this.size = -1;
            this.via = "arg0";
        }
    }

    static class Tagged {
        final String name;
        final String[] tags;

        Tagged(String name, String... tags) {
            this.name = name;
            this.tags = tags;
        }
    }

    private static final class Hidden {
        final String secret;

        private Hidden(String secret) {
            this.secret = secret;
        }
    }

    /** Counts lookups of one class in ReflectionUtils' sorted-constructor cache. */
    private static final class CountingCache extends ConcurrentHashMap<Object, Constructor<?>[]> {
        final Class<?> watched;
        final AtomicInteger lookups = new AtomicInteger();

        CountingCache(Class<?> watched) {
            this.watched = watched;
        }

        @Override
        public Constructor<?>[] get(Object key) {
            if (key == watched) {
                lookups.incrementAndGet();
            }
            return super.get(key);
        }

        @Override
        public Constructor<?>[] computeIfAbsent(Object key, Function<? super Object, ? extends Constructor<?>[]> fn) {
            if (key == watched) {
                lookups.incrementAndGet();
            }
            return super.computeIfAbsent(key, fn);
        }
    }

    private Map<Object, Constructor<?>[]> originalSortedCache;

    @SuppressWarnings("unchecked")
    private static AtomicReference<Map<Object, Constructor<?>[]>> sortedConstructorsCache() throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField("SORTED_CONSTRUCTORS_CACHE");
        f.setAccessible(true);
        return (AtomicReference<Map<Object, Constructor<?>[]>>) f.get(null);
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue("x".equals(Point.class.getDeclaredConstructor(int.class).getParameters()[0].getName()),
                "test classes must be compiled with -parameters");
        originalSortedCache = sortedConstructorsCache().get();
        ClassUtilities.clearCaches();
    }

    @AfterEach
    void tearDown() {
        ReflectionUtils.setSortedConstructorsCache(originalSortedCache);
        ClassUtilities.clearCaches();
    }

    @Test
    void theConstructorsAreLookedUpOncePerClassNotOncePerObject() {
        CountingCache counting = new CountingCache(Point.class);
        ReflectionUtils.setSortedConstructorsCache(counting);
        ClassUtilities.clearCaches();

        ClassUtilities.newInstance(converter, Point.class, map("x", 1, "y", 2));
        int afterFirst = counting.lookups.get();
        assertTrue(afterFirst > 0, "the first construction looks the constructors up");

        for (int i = 0; i < 100; i++) {
            Point p = (Point) ClassUtilities.newInstance(converter, Point.class, map("x", i, "y", i + 1));
            assertEquals(i, p.x);
            assertEquals(i + 1, p.y);
        }
        assertEquals(afterFirst, counting.lookups.get(), "later constructions must not look them up again");

        ClassUtilities.clearCaches();
        ClassUtilities.newInstance(converter, Point.class, map("x", 1, "y", 2));
        assertTrue(counting.lookups.get() > afterFirst, "clearCaches() starts over");
    }

    /**
     * A ClassValue's value lives inside its Class. Were it a java-util type, computing it for a JDK class would
     * pin java-util's class loader (a webapp's, say) for the life of the JVM.
     */
    @Test
    void theCachedFactsHoldOnlyJdkTypes() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("namedConstructors");
        f.setAccessible(true);
        ClassValue<?> cache = (ClassValue<?>) f.get(null);

        for (Class<?> c : new Class<?>[]{Point.class, Tagged.class, java.util.ArrayList.class, java.awt.Point.class}) {
            Object plan = cache.get(c);
            assertNull(plan.getClass().getClassLoader(), c + ": " + plan.getClass());
            for (Object element : (Object[]) plan) {
                assertNull(element.getClass().getClassLoader(), c + ": " + element.getClass());
            }
        }
    }

    @Test
    void eachCallStillMatchesItsOwnValues() {
        for (int i = 0; i < 3; i++) {
            Point both = (Point) ClassUtilities.newInstance(converter, Point.class, map("x", "3", "y", 4));
            assertEquals("xy", both.via);
            assertEquals(3, both.x);
            assertEquals(4, both.y);

            Point onlyX = (Point) ClassUtilities.newInstance(converter, Point.class, map("x", 5));
            assertEquals("x", onlyX.via, "y is missing: the one-argument constructor");
            assertEquals(5, onlyX.x);
        }
    }

    @Test
    void aConstructorWithSyntheticLookingNamesIsNotMatchedByName() {
        Mixed full = (Mixed) ClassUtilities.newInstance(converter, Mixed.class, map("label", "a", "size", 2));
        assertEquals("labelSize", full.via);
        assertEquals("a", full.label);
        assertEquals(2, full.size);

        // "arg0" names the one-argument constructor's parameter, but a name spelled argN is not trusted: named
        // matching skips that constructor, and the positional fallback takes the values in key order.
        Mixed notByName = (Mixed) ClassUtilities.newInstance(converter, Mixed.class, map("arg0", "b", "size", 3));
        assertEquals("labelSize", notByName.via);
        assertEquals("b", notByName.label);
        assertEquals(3, notByName.size);
    }

    @Test
    void varargsAreMatchedByName() {
        Tagged t = (Tagged) ClassUtilities.newInstance(converter, Tagged.class,
                map("name", "n", "tags", Arrays.asList("a", "b")));
        assertEquals("n", t.name);
        assertArrayEquals(new String[]{"a", "b"}, t.tags);

        Tagged none = (Tagged) ClassUtilities.newInstance(converter, Tagged.class, map("name", "m"));
        assertEquals("m", none.name);
        assertArrayEquals(new String[0], none.tags);
    }

    @Test
    void aPrivateConstructorIsStillUsed() {
        for (int i = 0; i < 2; i++) {
            Hidden h = (Hidden) ClassUtilities.newInstance(converter, Hidden.class, map("secret", "s" + i));
            assertEquals("s" + i, h.secret);
        }
    }

    @Test
    void aNullForAPrimitiveGetsItsDefault() {
        Point p = (Point) ClassUtilities.newInstance(converter, Point.class, map("x", null, "y", 7));
        assertEquals("xy", p.via);
        assertEquals(0, p.x);
        assertEquals(7, p.y);
    }

    @Test
    void anUnconvertibleValueFallsThroughToTheNextConstructor() {
        Point p = (Point) ClassUtilities.newInstance(converter, Point.class, map("x", 9, "y", "not a number"));
        assertEquals("x", p.via, "y will not convert: the one-argument constructor");
        assertEquals(9, p.x);
    }
}
