package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ClassValueMap no longer keeps its keys -- or their ClassLoaders -- alive. It kept every class it mapped in a backing
 * ConcurrentHashMap, so a class from a discarded loader (a redeployed webapp, a plugin, a recompiled script) stayed in
 * memory, with its loader and every class that loader loaded, until removed. Each class's value now lives in a slot
 * stored in the class itself, and the mapped classes are listed only weakly: as in a WeakHashMap, an entry goes away
 * when its class is unloaded, even when the value refers back to the class. Everything else about the map is as before,
 * which the differential test below holds to a LinkedHashMap.
 */
class ClassValueMapWeakKeysTest {

    private static final String FIXTURE = "com.cedarsoftware.util.ClassValueMapWeakKeysTest$LoaderFixture";

    /** Loaded in a throwaway ClassLoader whose parent is the bootstrap loader: may depend only on java.lang. */
    public static class LoaderFixture {
        public LoaderFixture() {
        }
    }

    private static Class<?> loadFixture() throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        assertNotSame(LoaderFixture.class, fixture, "the fixture must be defined by the throwaway loader");
        return fixture;
    }

    @Test
    void aMappedClassIsNotKeptAliveAndItsEntryGoesAway() throws Exception {
        ClassValueMap<Object> map = new ClassValueMap<>();
        map.put(String.class, "stays");
        WeakReference<ClassLoader> loader = mapAFixture(map);
        assertEquals(3, map.size());

        for (int i = 0; i < 100 && (loader.get() != null || map.size() != 1); i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        assertNull(loader.get(), "the map kept the class loader alive");
        assertEquals(1, map.size(), "the unloaded classes' entries must go");
        assertEquals(1, map.entrySet().size());
        assertEquals("stays", map.get(String.class));
        assertEquals(1, map.keySet().size());
        assertTrue(map.containsKey(String.class));
    }

    /** Maps two classes of a throwaway loader, each to something that refers back to it, and drops the loader. */
    private static WeakReference<ClassLoader> mapAFixture(ClassValueMap<Object> map) throws Exception {
        Class<?> fixture = loadFixture();
        map.put(fixture, fixture.getConstructor());                     // the value refers to its class
        Class<?> array = java.lang.reflect.Array.newInstance(fixture, 0).getClass();
        map.computeIfAbsent(array, c -> c.getComponentType());           // so does this one
        assertSame(fixture, map.get(array));
        ClassLoader loader = fixture.getClassLoader();
        ((URLClassLoader) loader).close();
        return new WeakReference<>(loader);
    }

    /**
     * The slot a map keeps in a class is a JDK type: a ClassValue's value lives inside its Class, and a java-util type
     * stored in a JDK class would keep java-util's own class loader alive for the life of the JVM.
     */
    @Test
    void theSlotKeptInAClassIsAJdkType() throws Exception {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "value");
        map.get(Integer.class);
        Field mappingsField = ClassValueMap.class.getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        Object mappings = mappingsField.get(map);
        Field slotsField = mappings.getClass().getDeclaredField("slots");
        slotsField.setAccessible(true);
        ClassValue<?> slots = (ClassValue<?>) slotsField.get(mappings);
        for (Class<?> c : new Class<?>[]{String.class, Integer.class}) {
            Object slot = slots.get(c);
            assertNull(slot.getClass().getClassLoader(), c + " holds a " + slot.getClass());
            Object held = ((java.util.concurrent.atomic.AtomicReference<?>) slot).get();
            assertNull(held.getClass().getClassLoader(), c + "'s slot holds a " + held.getClass());
        }
    }

    /**
     * Random operations -- every Map and ConcurrentMap method, with a null key and null values -- against a
     * LinkedHashMap that stands for what a Map must answer. Fixed seed.
     */
    @Test
    void randomOperationsAgreeWithALinkedHashMap() {
        List<Class<?>> pool = new ArrayList<>(Arrays.asList(String.class, Integer.class, Long.class, Double.class,
                Object.class, List.class, Map.class, HashMap.class, ArrayList.class, Thread.class, int.class,
                int[].class, String[].class, Class.class, Number.class, Runnable.class, Enum.class, Character.class));
        pool.add(null);
        Object[] values = {1, 2, 3, "a", new String("a"), null, 1L};
        Random random = new Random(20260927L);
        for (int round = 0; round < 40; round++) {
            ClassValueMap<Object> map = new ClassValueMap<>();
            Map<Class<?>, Object> model = new LinkedHashMap<>();
            for (int step = 0; step < 600; step++) {
                Class<?> key = pool.get(random.nextInt(pool.size()));
                Object value = values[random.nextInt(values.length)];
                Object other = values[random.nextInt(values.length)];
                int op = random.nextInt(19);
                String where = "round " + round + " step " + step + " op " + op + " key " + key + " value " + value;
                switch (op) {
                    case 0:
                    case 1:
                        assertEquals(model.put(key, value), map.put(key, value), where);
                        break;
                    case 2:
                        assertEquals(model.remove(key), map.remove(key), where);
                        break;
                    case 3:
                        assertEquals(model.remove(key, value), map.remove(key, value), where);
                        break;
                    case 4:
                        assertEquals(model.putIfAbsent(key, value), map.putIfAbsent(key, value), where);
                        break;
                    case 5:
                        assertEquals(model.computeIfAbsent(key, k -> value), map.computeIfAbsent(key, k -> value), where);
                        break;
                    case 6:
                        assertEquals(model.compute(key, (k, v) -> v == null ? value : null),
                                map.compute(key, (k, v) -> v == null ? value : null), where);
                        break;
                    case 7:
                        assertEquals(model.replace(key, value), map.replace(key, value), where);
                        break;
                    case 8:
                        assertEquals(model.replace(key, value, other), map.replace(key, value, other), where);
                        break;
                    case 9:
                        assertEquals(model.getOrDefault(key, "default"), map.getOrDefault(key, "default"), where);
                        break;
                    case 10:
                        assertEquals(model.containsValue(value), map.containsValue(value), where);
                        break;
                    case 11:
                        assertEquals(model.computeIfPresent(key, (k, v) -> other),
                                map.computeIfPresent(key, (k, v) -> other), where);
                        break;
                    case 12:
                        if (value != null) {
                            assertEquals(model.merge(key, value, (a, b) -> other), map.merge(key, value, (a, b) -> other),
                                    where);
                        }
                        break;
                    case 13:
                        if (random.nextInt(20) == 0) {
                            model.clear();
                            map.clear();
                        }
                        break;
                    case 14:                                                // remove through the entry iterator
                        java.util.Iterator<Map.Entry<Class<?>, Object>> it = map.entrySet().iterator();
                        while (it.hasNext()) {
                            if (Objects.equals(it.next().getKey(), key)) {
                                it.remove();
                                model.remove(key);
                            }
                        }
                        break;
                    case 15:
                        assertEquals(model.get(key), key == null ? map.get(null) : map.getByClass(key), where);
                        break;
                    default:
                        assertEquals(model.get(key), map.get(key), where);
                        assertEquals(model.containsKey(key), map.containsKey(key), where);
                }
                assertEquals(model.size(), map.size(), where);
            }
            assertEquals(model, map, "round " + round);
            assertEquals(map, model, "round " + round);
            assertEquals(model.hashCode(), map.hashCode(), "round " + round);
            assertEquals(model.keySet(), map.keySet(), "round " + round);
            assertEquals(new ArrayList<>(model.values()).size(), map.values().size(), "round " + round);
            for (Object value : values) {
                assertEquals(model.containsValue(value), map.values().contains(value), "round " + round);
            }
            assertEquals(model, new ClassValueMap<>(model), "copy constructor, round " + round);
        }
    }

    @Test
    void concurrentComputeIfAbsentRunsTheFunctionOncePerKey() throws Exception {
        ClassValueMap<Object> map = new ClassValueMap<>();
        Class<?>[] keys = {String.class, Integer.class, Long.class, Double.class, Object.class, List.class, Map.class,
                Thread.class, Number.class, Runnable.class};
        Map<Class<?>, AtomicInteger> calls = new ConcurrentHashMap<>();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Map<Class<?>, Object>>> seen = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            seen.add(pool.submit(() -> {
                start.await();
                Map<Class<?>, Object> got = new HashMap<>();
                for (int round = 0; round < 200; round++) {
                    for (Class<?> key : keys) {
                        got.put(key, map.computeIfAbsent(key, k -> {
                            calls.computeIfAbsent(k, x -> new AtomicInteger()).incrementAndGet();
                            return new Object();
                        }));
                    }
                }
                return got;
            }));
        }
        start.countDown();
        List<Map<Class<?>, Object>> results = new ArrayList<>();
        for (Future<Map<Class<?>, Object>> f : seen) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        for (Class<?> key : keys) {
            assertEquals(1, calls.get(key).get(), key + ": the mapping function ran more than once");
            for (Map<Class<?>, Object> got : results) {
                assertSame(map.get(key), got.get(key), key + ": threads saw different values");
            }
        }
        assertEquals(keys.length, map.size());
    }

    @Test
    void concurrentPutsAndRemovesLeaveAConsistentMap() throws Exception {
        ClassValueMap<Integer> map = new ClassValueMap<>();
        Class<?>[] keys = {String.class, Integer.class, Long.class, Double.class, Object.class, List.class, Map.class,
                Thread.class, Number.class, Runnable.class, HashMap.class, ArrayList.class};
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> done = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int seed = t;
            done.add(pool.submit(() -> {
                start.await();
                Random random = new Random(seed);
                for (int i = 0; i < 20_000; i++) {
                    Class<?> key = keys[random.nextInt(keys.length)];
                    if (random.nextBoolean()) {
                        map.put(key, i);
                    } else {
                        map.remove(key);
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : done) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        int listed = 0;
        for (Map.Entry<Class<?>, Integer> entry : map.entrySet()) {
            listed++;
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
        int present = 0;
        for (Class<?> key : keys) {
            if (map.containsKey(key)) {
                present++;
            }
        }
        assertEquals(present, listed, "iteration and containsKey() disagree");
        assertEquals(present, map.size(), "size() and containsKey() disagree");
        assertFalse(map.size() > keys.length);
    }
}
