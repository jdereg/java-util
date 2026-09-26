package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cached constructor plan must make newInstance() produce exactly what a fresh call does: the cache may save
 * work, never change an answer. Randomized (fixed seed) over constructors, argument classes, values -- strings
 * that do and do not convert, numbers, booleans, nulls -- and Converters that differ in which conversions
 * matching may use. The constructors accept anything, so only argument matching can make results differ.
 */
class ClassUtilitiesPlanEquivalenceTest {

    static class P1 { final Object[] v; P1(String a, Object b) { v = new Object[]{a, b}; } }
    static class P2 { final Object[] v; P2(Object a, String b) { v = new Object[]{a, b}; } }
    static class P3 { final Object[] v; P3(long a, String b) { v = new Object[]{a, b}; } }
    static class P4 { final Object[] v; P4(Long a, String b, int c) { v = new Object[]{a, b, c}; } }
    static class P5 { final Object[] v; P5(Number a, CharSequence b) { v = new Object[]{a, b}; } }
    static class P6 { final Object[] v; P6(double a, Integer b, String c) { v = new Object[]{a, b, c}; } }
    static class P7 { final Object[] v; P7(String a, String b) { v = new Object[]{a, b}; } }
    static class P8 { final Object[] v; P8(BigDecimal a, Boolean b, boolean c) { v = new Object[]{a, b, c}; } }
    static class P9 { final Object[] v; P9(Integer a, Long b, Object c, String d) { v = new Object[]{a, b, c, d}; } }

    private static final Class<?>[] CLASSES = {P1.class, P2.class, P3.class, P4.class, P5.class, P6.class,
            P7.class, P8.class, P9.class};

    /** Each kind is one argument class; its values differ, some converting where others do not. */
    private static final Object[][] KINDS = {
            {"42", "7", "x", "abc", "3.5", "true", "", "2020-01-01"},     // String
            {1L, 42L, -5L},                                                 // Long
            {2, 0, 99},                                                     // Integer
            {1.5d, 0.0d, -2.25d},                                           // Double
            {new BigDecimal("1.10"), BigDecimal.ONE},                       // BigDecimal
            {true, false},                                                  // Boolean
            {null},                                                         // null
    };

    private final Converter plain = new Converter(new DefaultConverterOptions());
    private final Converter overriding;

    ClassUtilitiesPlanEquivalenceTest() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getConverterOverrides().put(Converter.pair(String.class, Long.class),
                (from, conv) -> Long.valueOf(((String) from).length()));
        options.getConverterOverrides().put(Converter.pair(String.class, Integer.class),
                (from, conv) -> ((String) from).length());
        overriding = new Converter(options);
    }

    @AfterEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    @Test
    void aCachedCallProducesWhatAFreshCallDoes() throws Exception {
        Random random = new Random(20260926L);
        List<String> mismatches = new ArrayList<>();
        int trials = 3000;
        for (int t = 0; t < trials; t++) {
            Class<?> c = CLASSES[random.nextInt(CLASSES.length)];
            int count = random.nextInt(5);
            int[] kinds = new int[count];
            for (int i = 0; i < count; i++) {
                kinds[i] = random.nextInt(KINDS.length);
            }
            List<Object> first = values(random, kinds);
            List<Object> second = values(random, kinds);    // same argument classes, other values
            Converter recordWith = random.nextBoolean() ? plain : overriding;
            Converter callWith = random.nextBoolean() ? plain : overriding;

            ClassUtilities.clearCaches();
            outcome(recordWith, c, first);                  // caches a plan for this argument shape
            String cached = outcome(callWith, c, second);
            ClassUtilities.clearCaches();
            String fresh = outcome(callWith, c, second);

            if (!cached.equals(fresh) && mismatches.size() < 10) {
                mismatches.add(c.getSimpleName() + " first=" + first + " second=" + second
                        + (recordWith == callWith ? "" : " (converter changed)")
                        + "\n    cached: " + cached + "\n    fresh:  " + fresh);
            }
        }
        assertTrue(mismatches.isEmpty(), "cached and fresh calls differ:\n" + String.join("\n", mismatches));
    }

    /** Insertion order, and not a LinkedHashMap or SortedMap -- as json-io's JsonObject. */
    static final class InsertionOrderMap extends AbstractMap<String, Object> {
        private final Map<String, Object> inner = new LinkedHashMap<>();

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return inner.entrySet();
        }

        @Override
        public Object put(String key, Object value) {
            return inner.put(key, value);
        }
    }

    /**
     * Keys that match no parameter name send a Map down the positional path, where its values go in the
     * alphabetical order of their keys -- exactly what a sorted copy of the map passes. Random key sets and
     * insertion orders, repeated, so the remembered order is both reused and replaced.
     */
    @Test
    void aMapIsReadAsItsSortedCopyWouldBe() throws Exception {
        Random random = new Random(926L);
        List<String> mismatches = new ArrayList<>();
        for (int t = 0; t < 2000; t++) {
            Class<?> c = CLASSES[random.nextInt(CLASSES.length)];
            List<String> keys = new ArrayList<>();
            for (int k = 0; k < 12; k++) {
                if (random.nextInt(3) == 0) {
                    keys.add("k" + k);      // "k10" sorts before "k2"
                }
            }
            Collections.shuffle(keys, random);
            InsertionOrderMap map = new InsertionOrderMap();
            for (String key : keys) {
                Object[] pool = KINDS[random.nextInt(KINDS.length)];
                map.put(key, pool[random.nextInt(pool.length)]);
            }
            Converter converter = random.nextBoolean() ? plain : overriding;
            if (random.nextInt(4) == 0) {
                ClassUtilities.clearCaches();
            }

            String viaMap = outcome(converter, c, map);
            String viaSorted = outcome(converter, c, new TreeMap<>(map));
            if (!viaMap.equals(viaSorted) && mismatches.size() < 10) {
                mismatches.add(c.getSimpleName() + " keys=" + keys + " map=" + map
                        + "\n    map:    " + viaMap + "\n    sorted: " + viaSorted);
            }
        }
        assertTrue(mismatches.isEmpty(), "a map and its sorted copy differ:\n" + String.join("\n", mismatches));
    }

    private static List<Object> values(Random random, int[] kinds) {
        List<Object> values = new ArrayList<>();
        for (int kind : kinds) {
            Object[] pool = KINDS[kind];
            values.add(pool[random.nextInt(pool.length)]);
        }
        return values;
    }

    /** The constructed values, or the failure. A default Object is shown by its class: each is a new one. */
    private static String outcome(Converter converter, Class<?> c, Object args) throws Exception {
        Object instance;
        try {
            instance = ClassUtilities.newInstance(converter, c, args);
        } catch (Exception e) {
            return "threw " + e.getClass().getSimpleName();
        }
        Field f = c.getDeclaredField("v");
        Object[] v = (Object[]) f.get(instance);
        StringBuilder sb = new StringBuilder("[");
        for (Object x : v) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(x != null && x.getClass() == Object.class ? "new Object()"
                    : x == null ? "null" : x.getClass().getSimpleName() + ":" + Objects.toString(x));
        }
        return sb.append(']').toString();
    }
}
